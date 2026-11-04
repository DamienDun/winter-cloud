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
      <!-- Office：docx / xlsx 前端解析预览 -->
      <div v-else-if="kind === 'office'" class="preview-office">
        <el-skeleton v-if="officeLoading" :rows="10" animated />
        <div v-else-if="officeError" class="preview-fallback">
          <p>{{ officeError }}</p>
          <p class="preview-name">{{ displayName }}</p>
        </div>
        <div v-show="!officeLoading && !officeError && officeSubKind === 'docx'" ref="docxContainer" class="preview-docx" />
        <div
          v-show="!officeLoading && !officeError && officeSubKind === 'excel'"
          class="preview-excel"
          v-html="excelHtml"
        />
      </div>
      <!-- 其他 -->
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
 * Office：从 public/libs 动态加载 docx-preview / xlsx（避免 npm+webpack4 解析失败）；旧版 doc/ppt 外开。
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
      txtContent: null,
      officeLoading: false,
      officeError: null,
      officeSubKind: '',
      excelHtml: ''
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
      if (this.kind === 'image' || this.kind === 'video' || this.kind === 'pdf' || this.kind === 'html' || this.kind === 'office') {
        return '80%'
      }
      if (this.kind === 'audio') {
        return '520px'
      }
      return '640px'
    },
    fileExt() {
      const name = this.fileName || this.pickNameFromUrl(this.url) || ''
      if (name.includes('.')) {
        return name.split('.').pop().toLowerCase()
      }
      if (this.fileType !== undefined && this.fileType !== null && this.fileType !== '') {
        const token = String(this.fileType).toLowerCase().replace(/^\./, '')
        if (/^[a-z0-9]+$/.test(token) && TYPE_CODE_MAP[this.fileType] === undefined) {
          return token
        }
      }
      return ''
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
      this.officeError = null
      this.officeSubKind = ''
      this.excelHtml = ''
      this.visible = true
      this.$nextTick(() => {
        if (this.kind === 'txt') {
          this.loadText()
        } else if (this.kind === 'office') {
          this.loadOffice()
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
          this.txtContent = text.length > 200000 ? text.slice(0, 200000) + '\n\n...内容过长已截断' : text
        })
        .catch(() => {
          this.txtContent = null
        })
        .finally(() => {
          this.txtLoading = false
        })
    },

    loadScript(src) {
      return new Promise((resolve, reject) => {
        const existed = document.querySelector('script[data-preview-lib="' + src + '"]')
        if (existed) {
          resolve()
          return
        }
        const script = document.createElement('script')
        script.src = src
        script.async = true
        script.setAttribute('data-preview-lib', src)
        script.onload = () => resolve()
        script.onerror = () => reject(new Error('脚本加载失败: ' + src))
        document.head.appendChild(script)
      })
    },
    async ensureOfficeLibs(kind) {
      const base = (process.env.BASE_URL || '/').replace(/\/?$/, '/')
      if (kind === 'docx') {
        if (!window.JSZip) {
          await this.loadScript(base + 'libs/jszip.min.js')
        }
        if (!window.docx || !window.docx.renderAsync) {
          await this.loadScript(base + 'libs/docx-preview.min.js')
        }
        return
      }
      if (kind === 'excel') {
        if (!window.XLSX) {
          await this.loadScript(base + 'libs/xlsx.full.min.js')
        }
      }
    },
    async loadOffice() {
      this.officeLoading = true
      this.officeError = null
      this.excelHtml = ''
      this.officeSubKind = ''
      const ext = this.fileExt
      // 旧版二进制格式与 PPT 无法在浏览器内可靠解析
      if (/^(doc|ppt|pptx|rtf|odt|ods|odp|wps|et|dps)$/.test(ext)) {
        this.officeError = '当前格式暂不支持页内预览，请新窗口打开或下载后查看（docx/xlsx/xls/csv 可页内预览）。'
        this.officeLoading = false
        return
      }
      try {
        const res = await fetch(this.url)
        if (!res.ok) {
          throw new Error('文件拉取失败 HTTP ' + res.status)
        }
        const buffer = await res.arrayBuffer()
        if (ext === 'docx' || this.fileType === 5) {
          this.officeSubKind = 'docx'
          await this.$nextTick()
          const container = this.$refs.docxContainer
          if (container) {
            container.innerHTML = ''
          }
          await this.ensureOfficeLibs('docx')
          await window.docx.renderAsync(buffer, container, null, {
            className: 'docx-preview-body',
            inWrapper: true,
            ignoreWidth: false,
            ignoreHeight: false,
            breakPages: true
          })
        } else if (/^(xls|xlsx|xlsm|csv)$/.test(ext) || this.fileType === 6) {
          this.officeSubKind = 'excel'
          await this.ensureOfficeLibs('excel')
          const XLSX = window.XLSX
          const workbook = XLSX.read(buffer, { type: 'array' })
          const sheetName = workbook.SheetNames[0]
          if (!sheetName) {
            throw new Error('工作簿为空')
          }
          // 多 sheet 时拼接展示，避免只能看第一页
          const parts = workbook.SheetNames.map(name => {
            const sheet = workbook.Sheets[name]
            const html = XLSX.utils.sheet_to_html(sheet, { header: '', footer: '' })
            return '<h4 class="excel-sheet-title">' + name + '</h4>' + html
          })
          this.excelHtml = parts.join('')
        } else {
          this.officeError = '无法识别的 Office 扩展名：' + (ext || '-') + '，请新窗口打开。'
        }
      } catch (e) {
        const msg = (e && e.message) ? e.message : String(e)
        // 跨域时浏览器会拦 fetch，提示走外开
        this.officeError = '页内预览失败（' + msg + '）。若为跨域，请配置对象存储 CORS 或使用「新窗口打开」。'
      } finally {
        this.officeLoading = false
      }
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
      this.officeLoading = false
      this.officeError = null
      this.officeSubKind = ''
      this.excelHtml = ''
      if (this.$refs.docxContainer) {
        this.$refs.docxContainer.innerHTML = ''
      }
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
.preview-office {
  min-height: 200px;
}
.preview-docx,
.preview-excel {
  max-height: 70vh;
  overflow: auto;
  background: #f5f7fa;
  padding: 12px;
}
.preview-excel {
  ::v-deep table {
    border-collapse: collapse;
    width: max-content;
    min-width: 100%;
    background: #fff;
  }
  ::v-deep td,
  ::v-deep th {
    border: 1px solid #dcdfe6;
    padding: 4px 8px;
    font-size: 13px;
    white-space: pre-wrap;
  }
  ::v-deep .excel-sheet-title {
    margin: 12px 0 8px;
    color: #303133;
  }
}
.preview-docx {
  ::v-deep .docx-wrapper {
    background: transparent;
    padding: 0;
  }
  ::v-deep .docx-wrapper > section.docx {
    margin-bottom: 16px;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  }
}
</style>
