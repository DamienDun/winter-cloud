<template>
  <el-dialog
    :title="dialogTitle"
    :visible.sync="visible"
    :width="dialogWidth"
    append-to-body
    destroy-on-close
    class="file-preview-dialog"
    @closed="onClosed"
  >
    <div v-if="!url" class="preview-empty">无可预览地址</div>
    <div v-else class="preview-body">
      <!-- 图片 -->
      <div v-if="kind === 'image'" class="preview-image">
        <el-image :src="url" fit="contain" :preview-src-list="[url]">
          <div slot="error" class="preview-fallback">图片加载失败，可新窗口打开</div>
        </el-image>
      </div>
      <!-- 音频 -->
      <audio v-else-if="kind === 'audio'" class="preview-audio" :src="url" controls controlsList="nodownload" />
      <!-- 视频 -->
      <video v-else-if="kind === 'video'" class="preview-video" :src="url" controls controlsList="nodownload" />
      <!-- PDF / HTML：iframe 内嵌 -->
      <iframe
        v-else-if="kind === 'pdf' || kind === 'html'"
        class="preview-frame"
        :src="url"
        frameborder="0"
      />
      <!-- 纯文本：尝试拉取展示，跨域失败则提示新开 -->
      <div v-else-if="kind === 'txt'" class="preview-txt">
        <el-skeleton v-if="txtLoading" :rows="8" animated />
        <pre v-else-if="txtContent !== null">{{ txtContent }}</pre>
        <div v-else class="preview-fallback">
          文本无法直接读取（可能跨域），请新窗口打开
        </div>
      </div>
      <!-- Office / 其他：不强制伪造在线预览 -->
      <div v-else class="preview-fallback">
        <p>当前类型不支持页内预览，请新窗口打开或下载后查看。</p>
        <p class="preview-name">{{ displayName }}</p>
      </div>
    </div>
    <div slot="footer" class="dialog-footer">
      <el-button v-if="url" type="primary" plain icon="el-icon-link" @click="openExternal">新窗口打开</el-button>
      <el-button @click="visible = false">关 闭</el-button>
    </div>
  </el-dialog>
</template>

<script>
/**
 * 通用文件预览弹窗。
 * fileType 可为 ResourceFileTypeEnum 数字码，或扩展名/MIME 字符串。
 */
const TYPE_CODE_MAP = {
  1: 'image',
  2: 'audio',
  3: 'video',
  4: 'pdf',
  5: 'office',
  6: 'office',
  7: 'office',
  8: 'txt',
  9: 'html'
}

export default {
  name: 'FilePreview',
  data() {
    return {
      visible: false,
      url: '',
      fileName: '',
      fileType: undefined,
      txtLoading: false,
      txtContent: null
    }
  },
  computed: {
    kind() {
      return this.resolveKind(this.fileType, this.url, this.fileName)
    },
    displayName() {
      return this.fileName || this.pickNameFromUrl(this.url) || '-'
    },
    dialogTitle() {
      return this.displayName !== '-' ? ('预览 - ' + this.displayName) : '文件预览'
    },
    dialogWidth() {
      if (this.kind === 'image' || this.kind === 'video' || this.kind === 'pdf' || this.kind === 'html') {
        return '80%'
      }
      if (this.kind === 'audio') {
        return '520px'
      }
      return '640px'
    }
  },
  methods: {
    /**
     * 打开预览
     * @param {string} url 可访问地址
     * @param {{ fileType?: number|string, fileName?: string }} [options]
     */
    open(url, options = {}) {
      if (!url) {
        this.$modal.msgWarning('暂无访问地址，无法预览')
        return
      }
      this.url = url
      this.fileName = options.fileName || ''
      this.fileType = options.fileType
      this.txtContent = null
      this.visible = true
      this.$nextTick(() => {
        if (this.kind === 'txt') {
          this.loadText()
        }
      })
    },
    resolveKind(fileType, url, fileName) {
      if (fileType !== undefined && fileType !== null && fileType !== '') {
        if (TYPE_CODE_MAP[fileType] !== undefined) {
          return TYPE_CODE_MAP[fileType]
        }
        const byToken = this.kindFromToken(String(fileType))
        if (byToken) {
          return byToken
        }
      }
      const name = fileName || this.pickNameFromUrl(url) || ''
      const ext = name.includes('.') ? name.split('.').pop().toLowerCase() : ''
      return this.kindFromToken(ext) || 'other'
    },
    kindFromToken(token) {
      if (!token) return null
      const t = token.toLowerCase().replace(/^\./, '')
      if (/^(jpg|jpeg|png|gif|bmp|webp|svg|tif|tiff|ico|heic|heif|apng|dib|image\/)/.test(t)) return 'image'
      if (/^(mp3|wav|flac|aac|ogg|m4a|wma|mid|midi|opus|amr|audio\/)/.test(t)) return 'audio'
      if (/^(mp4|avi|mkv|mov|wmv|flv|webm|m4v|mpeg|mpg|3gp|ts|rmvb|rm|video\/)/.test(t)) return 'video'
      if (t === 'pdf' || t === 'ofd' || t.startsWith('application/pdf')) return 'pdf'
      if (/^(txt|md|markdown|log|ini|conf|properties|sql|json|xml|yml|yaml)$/.test(t) || t.startsWith('text/plain')) return 'txt'
      if (/^(html|htm)$/.test(t) || t.startsWith('text/html')) return 'html'
      if (/^(doc|docx|xls|xlsx|xlsm|csv|ppt|pptx|rtf|odt|ods|odp|wps|et|dps)$/.test(t)) return 'office'
      return null
    },
    pickNameFromUrl(url) {
      if (!url) return ''
      try {
        const path = url.split('?')[0]
        return decodeURIComponent(path.substring(path.lastIndexOf('/') + 1))
      } catch (e) {
        return ''
      }
    },
    loadText() {
      this.txtLoading = true
      this.txtContent = null
      fetch(this.url)
        .then(res => {
          if (!res.ok) throw new Error('fetch failed')
          return res.text()
        })
        .then(text => {
          // 过大文本截断，避免弹窗卡死
          this.txtContent = text.length > 200000 ? text.slice(0, 200000) + '\n\n...内容过长已截断' : text
        })
        .catch(() => {
          this.txtContent = null
        })
        .finally(() => {
          this.txtLoading = false
        })
    },
    openExternal() {
      if (this.url) {
        window.open(this.url, '_blank')
      }
    },
    onClosed() {
      this.url = ''
      this.fileName = ''
      this.fileType = undefined
      this.txtContent = null
      this.txtLoading = false
    }
  }
}
</script>

<style lang="scss" scoped>
.preview-body {
  min-height: 120px;
}
.preview-empty,
.preview-fallback {
  color: #909399;
  text-align: center;
  padding: 40px 16px;
  line-height: 1.6;
}
.preview-name {
  word-break: break-all;
  color: #606266;
}
.preview-image {
  text-align: center;
  ::v-deep .el-image {
    max-width: 100%;
    max-height: 70vh;
  }
  ::v-deep .el-image__inner {
    max-height: 70vh;
  }
}
.preview-audio {
  width: 100%;
  margin: 24px 0;
}
.preview-video {
  display: block;
  width: 100%;
  max-height: 70vh;
  background: #000;
}
.preview-frame {
  width: 100%;
  height: 70vh;
  border: none;
}
.preview-txt {
  max-height: 70vh;
  overflow: auto;
  pre {
    margin: 0;
    white-space: pre-wrap;
    word-break: break-word;
    font-family: Consolas, Monaco, monospace;
    font-size: 13px;
    line-height: 1.5;
    color: #303133;
  }
}
</style>
