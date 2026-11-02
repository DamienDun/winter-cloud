<template>
  <div class="app-container">
    <el-card shadow="never">
      <div slot="header" class="clearfix">
        <span>大文件分片上传</span>
        <span class="header-tip">MD5 秒传 / 断点续传，默认分片 5MB，并发 {{ concurrency }}</span>
      </div>

      <el-form label-width="100px" size="small">
        <el-form-item label="存储目录">
          <el-input v-model="uploadFolder" placeholder="对象路径前缀，如 bigfile" style="width: 320px;" />
        </el-form-item>
        <el-form-item label="分片大小">
          <el-select v-model="partSizeMb" style="width: 160px;">
            <el-option :value="5" label="5 MB" />
            <el-option :value="10" label="10 MB" />
            <el-option :value="20" label="20 MB" />
          </el-select>
        </el-form-item>
        <el-form-item label="选择文件">
          <el-upload
            ref="upload"
            action="/"
            :limit="1"
            :auto-upload="false"
            :show-file-list="true"
            :file-list="fileList"
            :on-change="handleFileChange"
            :on-remove="handleRemove"
            :on-exceed="handleExceed"
          >
            <el-button size="small" type="primary" :disabled="uploading">选取文件</el-button>
            <div slot="tip" class="el-upload__tip">支持任意格式，建议大于 20MB 的文件使用本页上传</div>
          </el-upload>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="uploading" :disabled="!currentFile" @click="startUpload">开始上传</el-button>
          <el-button type="danger" plain :disabled="!uploadId || !uploading" @click="cancelUpload">取消上传</el-button>
          <el-button @click="resetAll" :disabled="uploading">重置</el-button>
        </el-form-item>
        <el-form-item label="上传进度" v-if="progressPercent > 0 || uploading || statusText">
          <el-progress :percentage="progressPercent" :status="progressStatus" style="width: 480px;" />
          <div class="status-text">{{ statusText }}</div>
        </el-form-item>
        <el-form-item label="上传结果" v-if="result">
          <el-descriptions :column="1" size="small" border>
            <el-descriptions-item label="访问地址">
              <el-link :href="result.accessPath" target="_blank" type="primary">{{ result.accessPath }}</el-link>
            </el-descriptions-item>
            <el-descriptions-item label="对象路径">{{ result.filePath }}</el-descriptions-item>
            <el-descriptions-item label="桶">{{ result.bucketName }}</el-descriptions-item>
            <el-descriptions-item label="通道">{{ result.channelId }}</el-descriptions-item>
            <el-descriptions-item label="MD5">{{ result.fileMd5 }}</el-descriptions-item>
            <el-descriptions-item label="大小">{{ formatSize(result.totalBytes) }}</el-descriptions-item>
          </el-descriptions>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import { bigFileApply, bigFileUploadPart, bigFileCancel } from '@/api/file/upload'
import { calcFileMd5 } from '@/utils/sparkMd5'

/** 默认分片 5MB，对齐 paperless */
const DEFAULT_PART_MB = 5
/** 分片并发数（参考 paperless Queue(10)，浏览器侧略收敛） */
const DEFAULT_CONCURRENCY = 5

export default {
  name: 'BigFileUploadPage',
  data() {
    return {
      uploadFolder: 'bigfile',
      partSizeMb: DEFAULT_PART_MB,
      concurrency: DEFAULT_CONCURRENCY,
      fileList: [],
      currentFile: null,
      uploading: false,
      aborted: false,
      uploadId: '',
      progressPercent: 0,
      statusText: '',
      progressStatus: undefined,
      result: null,
      activeQueue: null
    }
  },
  methods: {
    handleFileChange(file, fileList) {
      this.fileList = fileList.slice(-1)
      this.currentFile = file.raw || null
      this.result = null
      this.progressPercent = 0
      this.statusText = ''
      this.progressStatus = undefined
      this.uploadId = ''
    },
    handleRemove() {
      this.currentFile = null
      this.fileList = []
    },
    handleExceed() {
      this.$modal.msgWarning('一次仅支持上传 1 个文件')
    },
    resetAll() {
      this.handleRemove()
      this.result = null
      this.progressPercent = 0
      this.statusText = ''
      this.progressStatus = undefined
      this.uploadId = ''
      this.aborted = false
      if (this.$refs.upload) {
        this.$refs.upload.clearFiles()
      }
    },
    formatSize(bytes) {
      if (!bytes && bytes !== 0) {
        return '-'
      }
      const n = Number(bytes)
      if (n < 1024) return n + ' B'
      if (n < 1024 * 1024) return (n / 1024).toFixed(2) + ' KB'
      if (n < 1024 * 1024 * 1024) return (n / 1024 / 1024).toFixed(2) + ' MB'
      return (n / 1024 / 1024 / 1024).toFixed(2) + ' GB'
    },
    getExt(fileName) {
      if (!fileName || fileName.indexOf('.') < 0) {
        return 'bin'
      }
      return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase()
    },
    buildFilePath(fileName) {
      const now = new Date()
      const y = now.getFullYear()
      const m = String(now.getMonth() + 1).padStart(2, '0')
      const d = String(now.getDate()).padStart(2, '0')
      const folder = (this.uploadFolder || 'bigfile').replace(/^\/+|\/+$/g, '')
      // 日期目录：YYYY/MM/DD
      return `/${folder}/${y}/${m}/${d}/${fileName}`
    },
    toUploadedSet(list) {
      if (!list) {
        return new Set()
      }
      if (Array.isArray(list)) {
        return new Set(list.map(Number))
      }
      // 兼容异常序列化形态
      return new Set(Object.values(list).map(Number))
    },
    async startUpload() {
      if (!this.currentFile) {
        this.$modal.msgWarning('请先选择文件')
        return
      }
      this.aborted = false
      this.uploading = true
      this.result = null
      this.progressPercent = 0
      this.progressStatus = undefined
      this.statusText = '正在计算文件 MD5...'

      try {
        const file = this.currentFile
        const fileMd5 = await calcFileMd5(file)
        if (this.aborted) {
          return
        }
        this.statusText = `MD5: ${fileMd5}，正在申请上传任务...`

        const partBytes = this.partSizeMb * 1024 * 1024
        const applyRes = await bigFileApply({
          fileMd5,
          filePath: this.buildFilePath(file.name),
          fileType: this.getExt(file.name),
          totalBytes: file.size,
          partBytes
        })
        const task = applyRes.data
        if (!task) {
          throw new Error(applyRes.msg || '申请上传任务失败')
        }
        this.uploadId = task.uploadId

        // 已全部完成（秒传）
        if (task.complete || Number(task.completeBytes) >= Number(task.totalBytes)) {
          this.progressPercent = 100
          this.progressStatus = 'success'
          this.statusText = '文件已存在，秒传成功'
          this.result = task
          this.$modal.msgSuccess('秒传成功')
          return
        }

        this.statusText = '分片上传中...'
        const failList = await this.uploadChunks(file, task)
        if (this.aborted) {
          this.statusText = '已取消上传'
          this.progressStatus = 'exception'
          return
        }
        if (failList.length > 0) {
          this.progressStatus = 'exception'
          this.statusText = `有 ${failList.length} 个分片上传失败，请重试`
          this.$modal.msgError('部分分片上传失败，请尝试重新上传')
          return
        }
        this.progressPercent = 100
        this.progressStatus = 'success'
        this.statusText = '上传完成'
        if (!this.result) {
          // 最后一片可能已带回结果；否则用申请任务里的路径信息兜底
          this.result = {
            accessPath: task.accessPath,
            filePath: task.filePath,
            bucketName: task.bucketName,
            channelId: task.channelId,
            fileMd5: task.fileMd5 || fileMd5,
            totalBytes: task.totalBytes || file.size
          }
        }
        this.$modal.msgSuccess('上传成功')
      } catch (e) {
        if (!this.aborted) {
          this.progressStatus = 'exception'
          this.statusText = e.message || '上传失败'
          this.$modal.msgError(e.message || '上传失败')
        }
      } finally {
        this.uploading = false
        this.activeQueue = null
      }
    },
    /**
     * 分片并发上传（逻辑对齐 paperless uploadBigVideo）
     */
    uploadChunks(file, task) {
      const partBytes = Number(task.partBytes)
      const partCount = Number(task.partCount)
      const totalBytes = Number(task.totalBytes)
      const uploadId = task.uploadId
      const uploadedSet = this.toUploadedSet(task.uploadedPartNumberList)

      const parts = []
      for (let i = 0; i < partCount; i++) {
        let size = partBytes
        const start = partBytes * i
        let end = start + size
        if (totalBytes < end) {
          size = totalBytes - start
          end = start + size
        }
        parts.push({
          partNumber: i + 1,
          start,
          end,
          size,
          finished: uploadedSet.has(i + 1)
        })
      }

      let uploadedSize = 0
      parts.forEach(p => {
        if (p.finished) {
          uploadedSize += p.size
        }
      })
      this.progressPercent = Math.min(99, Math.round((uploadedSize / totalBytes) * 100))

      const pending = parts.filter(p => !p.finished)
      const failArr = []
      let cursor = 0
      const self = this
      const controller = { stopped: false }
      this.activeQueue = controller

      const runOne = async (part) => {
        if (controller.stopped || self.aborted) {
          return
        }
        const blob = file.slice(part.start, part.end)
        const formData = new FormData()
        formData.append('file', blob, file.name)
        formData.append('uploadId', uploadId)
        formData.append('partNumber', String(part.partNumber))
        try {
          const res = await bigFileUploadPart(formData)
          const data = res.data || {}
          uploadedSize += part.size
          self.progressPercent = Math.min(99, Math.round((uploadedSize / totalBytes) * 100))
          // 最后一片合并完成后后端返回 complete / accessPath
          if (data.complete) {
            self.result = data
            self.progressPercent = 100
          }
        } catch (err) {
          failArr.push({ partNumber: part.partNumber, error: err })
        }
      }

      return new Promise((resolve) => {
        let running = 0
        const next = () => {
          if (controller.stopped || self.aborted) {
            resolve(failArr)
            return
          }
          while (running < self.concurrency && cursor < pending.length) {
            const part = pending[cursor++]
            running++
            runOne(part).finally(() => {
              running--
              if (cursor >= pending.length && running === 0) {
                resolve(failArr)
              } else {
                next()
              }
            })
          }
          if (pending.length === 0) {
            resolve(failArr)
          }
        }
        next()
      })
    },
    async cancelUpload() {
      this.aborted = true
      if (this.activeQueue) {
        this.activeQueue.stopped = true
      }
      if (this.uploadId) {
        try {
          await bigFileCancel({ uploadId: this.uploadId })
        } catch (e) {
          // 取消失败不阻断本地状态
        }
      }
      this.uploading = false
      this.progressStatus = 'exception'
      this.statusText = '已取消上传'
      this.$modal.msgWarning('已取消上传')
    }
  }
}
</script>

<style scoped>
.header-tip {
  margin-left: 12px;
  color: #909399;
  font-size: 12px;
}
.status-text {
  margin-top: 8px;
  color: #606266;
  font-size: 13px;
}
</style>
