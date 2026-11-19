<template>
  <!-- 对齐 xxl-job-admin 任务参数编辑器：文本原样保存，JSON 用表单填一层键值（含简单数组） -->
  <div class="executor-param-editor" :class="'is-' + view">
    <div class="param-toolbar">
      <el-radio-group v-model="mode" size="mini" @change="onModeChange">
        <el-radio-button label="text">文本</el-radio-button>
        <el-radio-button label="json">JSON</el-radio-button>
      </el-radio-group>
      <span v-show="mode === 'json'" class="param-tools">
        <el-button size="mini" :type="view === 'form' ? 'primary' : 'default'" plain @click="showForm">表单填写</el-button>
        <el-button size="mini" :type="view === 'source' ? 'primary' : 'default'" plain @click="showSource">源码</el-button>
        <el-button v-show="view === 'source'" size="mini" @click="formatJson">格式化</el-button>
        <el-button v-show="view === 'source'" size="mini" @click="fixPunctuation">修复标点</el-button>
      </span>
      <span class="param-count" :class="{ 'is-over': over }" :title="msg.countTitle">{{ countText }}</span>
    </div>
    <span class="param-tip">{{ tip }}</span>

    <div v-show="view === 'form'" class="param-form">
      <table class="param-table">
        <thead>
          <tr>
            <th class="col-key">参数名</th>
            <th class="col-type">类型</th>
            <th>参数值</th>
            <th class="col-op" />
          </tr>
        </thead>
        <tbody>
          <tr v-for="(row, index) in rows" :key="index" :class="{ 'has-error': errorRow === index }">
            <td>
              <el-input v-model="row.key" size="mini" :placeholder="msg.keyPh" @input="markDirty" @keydown.enter.native.prevent />
            </td>
            <td>
              <el-select v-model="row.type" size="mini" @change="onRowType(row)">
                <el-option label="文本" value="string" />
                <el-option label="数字" value="number" />
                <el-option label="是/否" value="boolean" />
                <el-option label="空" value="null" />
                <el-option label="数组" value="array" />
              </el-select>
            </td>
            <td>
              <div v-if="row.type === 'array'" class="array-box">
                <div v-for="(item, idx) in row.items" :key="idx" class="array-item">
                  <el-select v-model="item.type" size="mini" @change="markDirty">
                    <el-option label="文本" value="string" />
                    <el-option label="数字" value="number" />
                    <el-option label="是/否" value="boolean" />
                    <el-option label="空" value="null" />
                  </el-select>
                  <el-select v-if="item.type === 'boolean'" v-model="item.bool" size="mini" class="item-input" @change="markDirty">
                    <el-option label="是" :value="true" />
                    <el-option label="否" :value="false" />
                  </el-select>
                  <el-input v-else-if="item.type === 'null'" size="mini" class="item-input" disabled :placeholder="msg.nullPh" />
                  <el-input
                    v-else
                    v-model="item.text"
                    size="mini"
                    class="item-input"
                    :placeholder="item.type === 'number' ? msg.numberPh : msg.valuePh"
                    @input="markDirty"
                    @keydown.enter.native.prevent
                  />
                  <el-button type="text" size="mini" @click="removeItem(row, idx)">删除</el-button>
                </div>
                <el-button size="mini" @click="addItem(row)">添加元素</el-button>
              </div>
              <el-select v-else-if="row.type === 'boolean'" v-model="row.bool" size="mini" @change="markDirty">
                <el-option label="是" :value="true" />
                <el-option label="否" :value="false" />
              </el-select>
              <el-input v-else-if="row.type === 'null'" size="mini" disabled :placeholder="msg.nullPh" />
              <el-input
                v-else-if="row.type === 'string' && isLongText(row.text)"
                v-model="row.text"
                type="textarea"
                :rows="3"
                size="mini"
                :placeholder="msg.valuePh"
                @input="markDirty"
              />
              <el-input
                v-else
                v-model="row.text"
                size="mini"
                :placeholder="row.type === 'number' ? msg.numberPh : msg.valuePh"
                @input="markDirty"
                @keydown.enter.native.prevent
              />
            </td>
            <td>
              <el-button type="text" size="mini" @click="removeRow(index)">删除</el-button>
            </td>
          </tr>
        </tbody>
      </table>
      <el-button size="mini" @click="addRow">添加一项</el-button>
      <p class="param-form-hint">{{ msg.formHint }}</p>
    </div>

    <el-input
      v-show="view !== 'form'"
      v-model="sourceText"
      class="param-source"
      type="textarea"
      :rows="view === 'source' ? 8 : 3"
      :placeholder="view === 'source' ? msg.sourcePh : msg.textPh"
      @input="onSourceInput"
    />
    <div class="param-msg" :class="msgClass">{{ message }}</div>
  </div>
</template>

<script>
import {
  MAX_LEN, MSG, fmt, tryParse, stringifyJson, isFlatObject, looksLikeJson, chooseSaved,
  blankRow, toItem, valueToRows, rowsToObject, coerceArraySeed, tryFixPunctuation, numberText
} from './executorParam'

export default {
  name: 'ExecutorParamEditor',
  data() {
    return {
      msg: MSG,
      maxLen: MAX_LEN,
      mode: 'text',
      view: 'text',
      sourceText: '',
      rows: [],
      originalValue: '',
      dirty: false,
      blankMeansEmpty: true,
      forceText: false,
      loading: false,
      message: '',
      msgLevel: '',
      countText: '0/' + MAX_LEN,
      over: false,
      errorPos: -1,
      errorRow: -1
    }
  },
  computed: {
    tip() {
      if (this.view === 'form') return MSG.tipForm
      if (this.view === 'source') return MSG.tipSource
      return MSG.tipText
    },
    msgClass() {
      return this.msgLevel === 'ok' ? 'is-ok' : (this.msgLevel === 'err' ? 'is-err' : '')
    }
  },
  methods: {
    isLongText(text) {
      return !!text && (text.length > 60 || text.indexOf('\n') >= 0)
    },
    setMsg(level, text) {
      this.msgLevel = level || ''
      this.message = text || ''
    },
    showCount(length, over) {
      this.countText = length + '/' + this.maxLen
      this.over = !!over
    },
    /** 打开弹窗时调用。没改过再保存，会原样交回这次载入的字符串。 */
    load(value) {
      this.loading = true
      value = value == null ? '' : String(value)
      this.originalValue = value
      this.dirty = false
      this.blankMeansEmpty = true
      this.forceText = false
      this.errorPos = -1
      this.errorRow = -1
      this.sourceText = value
      const trimmed = value.trim()
      if (looksLikeJson(trimmed)) {
        const parsed = tryParse(trimmed)
        if (parsed.ok && isFlatObject(parsed.value)) {
          this.blankMeansEmpty = false
          this.rows = valueToRows(parsed.value)
          this.view = 'form'
          this.mode = 'json'
        } else {
          this.rows = []
          this.view = 'source'
          this.mode = 'json'
        }
      } else {
        this.rows = []
        this.view = 'text'
        this.mode = 'text'
      }
      this.loading = false
      this.dirty = false
      this.refreshStatus()
    },
    markDirty() {
      if (this.loading) return
      this.dirty = true
      this.refreshStatus()
    },
    onSourceInput() {
      this.markDirty()
    },
    onModeChange(next) {
      if (next === 'text') this.enterText()
      else this.enterJson()
    },
    enterText() {
      if (this.view === 'form' && this.dirty) {
        const built = rowsToObject(this.rows)
        if (!built.ok) {
          this.mode = 'json'
          this.showBuiltError(built)
          return
        }
        const written = this.writeBuilt(built)
        if (!written.ok) {
          this.mode = 'json'
          return
        }
      } else if (!this.dirty) {
        this.sourceText = this.originalValue
      }
      this.view = 'text'
      this.rows = []
      this.errorRow = -1
      this.refreshStatus()
    },
    enterJson() {
      if (this.view !== 'text') {
        this.mode = 'json'
        return
      }
      const trimmed = this.sourceText.trim()
      if (!trimmed) {
        this.blankMeansEmpty = true
        this.rows = [blankRow()]
        this.view = 'form'
        this.refreshStatus()
        return
      }
      const parsed = tryParse(trimmed)
      if (!parsed.ok) {
        this.errorPos = parsed.pos
        this.view = 'source'
        this.refreshStatus()
        return
      }
      this.blankMeansEmpty = false
      if (isFlatObject(parsed.value)) {
        this.rows = valueToRows(parsed.value)
        this.view = 'form'
      } else {
        this.sourceText = stringifyJson(parsed.value, 2)
        this.view = 'source'
      }
      this.refreshStatus()
    },
    showForm() {
      if (this.view === 'form') return
      const trimmed = this.sourceText.trim()
      if (!trimmed) {
        this.blankMeansEmpty = true
        this.rows = [blankRow()]
        this.view = 'form'
        this.mode = 'json'
        this.refreshStatus()
        return
      }
      const parsed = tryParse(trimmed)
      if (!parsed.ok) {
        this.errorPos = parsed.pos
        this.setMsg('err', parsed.message)
        return
      }
      if (!isFlatObject(parsed.value)) {
        this.setMsg('err', MSG.notFlat)
        return
      }
      this.blankMeansEmpty = false
      this.rows = valueToRows(parsed.value)
      this.view = 'form'
      this.mode = 'json'
      this.refreshStatus()
    },
    showSource() {
      if (this.view === 'form') {
        if (this.dirty) {
          const built = rowsToObject(this.rows)
          if (!built.ok) {
            this.showBuiltError(built)
            return
          }
          if (built.empty) this.sourceText = this.blankMeansEmpty ? '' : '{}'
          else this.sourceText = stringifyJson(built.value, 2)
        } else {
          const parsed = tryParse(this.originalValue.trim())
          this.sourceText = parsed.ok ? stringifyJson(parsed.value, 2) : this.originalValue
        }
      }
      this.view = 'source'
      this.mode = 'json'
      this.errorRow = -1
      this.refreshStatus()
    },
    formatJson() {
      if (this.view !== 'source') return
      const raw = this.sourceText
      const trimmed = raw.trim()
      if (!trimmed) return
      const parsed = tryParse(trimmed)
      if (!parsed.ok) {
        this.errorPos = parsed.pos
        this.setMsg('err', parsed.message)
        return
      }
      const pretty = stringifyJson(parsed.value, 2)
      if (pretty !== raw) {
        this.sourceText = pretty
        this.dirty = true
      }
      this.refreshStatus()
    },
    fixPunctuation() {
      if (this.view !== 'source') return
      const raw = this.sourceText
      if (tryParse(raw.trim()).ok) {
        this.setMsg('ok', MSG.alreadyJson)
        return
      }
      const fixed = tryFixPunctuation(raw)
      if (!fixed.changed || !tryParse(fixed.text.trim()).ok) {
        const parsed = tryParse(raw)
        this.errorPos = parsed.pos
        this.setMsg('err', MSG.fixNone + '\n' + (parsed.message || ''))
        return
      }
      this.sourceText = fixed.text
      this.dirty = true
      this.refreshStatus()
      this.setMsg('ok', fmt(MSG.fixOk, fixed.summary))
    },
    addRow() {
      this.rows.push(blankRow())
      this.markDirty()
    },
    removeRow(index) {
      this.rows.splice(index, 1)
      this.markDirty()
    },
    addItem(row) {
      const last = row.items.length ? row.items[row.items.length - 1] : null
      const type = last ? last.type : 'string'
      const item = toItem(type === 'boolean' ? true : (type === 'null' ? null : ''))
      item.type = type
      row.items.push(item)
      this.markDirty()
    },
    removeItem(row, index) {
      row.items.splice(index, 1)
      this.markDirty()
    },
    onRowType(row) {
      const prevType = row._prev || 'string'
      const next = row.type
      let prev
      if (prevType === 'array') {
        const current = rowsToObject([{ key: '_', type: 'array', text: '', bool: true, items: row.items }])
        prev = current.ok ? current.value._ : ''
      } else if (prevType === 'boolean') {
        prev = row.bool === true || row.bool === 'true'
      } else {
        prev = row.text || ''
      }
      if (next === 'array') prev = coerceArraySeed(prevType, prev)
      this.applyRowValue(row, next, prev)
      row._prev = next
      this.markDirty()
    },
    applyRowValue(row, type, value) {
      row.type = type
      if (type !== 'array' && Object.prototype.toString.call(value) === '[object Array]') {
        value = stringifyJson(value, 0)
      }
      if (type === 'array') {
        let items
        if (Object.prototype.toString.call(value) === '[object Array]') items = value
        else if (value != null && String(value) !== '') items = [String(value)]
        else items = ['']
        row.items = items.map(toItem)
        row.text = ''
        return
      }
      row.items = []
      if (type === 'boolean') {
        row.bool = !(value === false || value === 'false')
        row.text = ''
        return
      }
      if (type === 'null') {
        row.text = ''
        return
      }
      row.text = type === 'number' ? numberText(value) : (value == null ? '' : String(value))
    },
    showBuiltError(built) {
      this.errorRow = typeof built.rowIndex === 'number' ? built.rowIndex : -1
      this.setMsg('err', built.message)
    },
    writeBuilt(built) {
      if (built.empty) {
        const emptyValue = this.blankMeansEmpty ? '' : '{}'
        this.sourceText = emptyValue
        return { ok: true, value: emptyValue }
      }
      const saved = chooseSaved(built.value, this.maxLen)
      if (saved.length > this.maxLen) {
        const message = fmt(MSG.tooLong, saved.length, this.maxLen)
        this.setMsg('err', message)
        return { ok: false, message }
      }
      this.sourceText = saved
      return { ok: true, value: saved }
    },
    refreshStatus() {
      if (this.loading) return
      this.errorPos = -1
      this.errorRow = -1
      if (this.view === 'form') {
        const built = rowsToObject(this.rows)
        if (!built.ok) {
          this.showBuiltError(built)
          return
        }
        const saved = built.empty
          ? (this.blankMeansEmpty ? '' : '{}')
          : chooseSaved(built.value, this.maxLen)
        const over = saved.length > this.maxLen
        this.showCount(saved.length, over)
        if (over) this.setMsg('err', fmt(MSG.tooLong, saved.length, this.maxLen))
        else if (built.empty && this.blankMeansEmpty) this.setMsg('', MSG.emptySave)
        else if (built.empty) this.setMsg('ok', MSG.emptyObjectSave)
        else this.setMsg('ok', MSG.formReady)
        return
      }
      const raw = this.sourceText
      const trimmed = raw.trim()
      if (this.view === 'text') {
        const overText = raw.length > this.maxLen
        this.showCount(raw.length, overText)
        if (overText) this.setMsg('err', fmt(MSG.lengthText, raw.length, this.maxLen))
        else if (looksLikeJson(trimmed)) {
          const parsedText = tryParse(trimmed)
          if (parsedText.ok) this.setMsg('ok', MSG.textLooksJson)
          else {
            this.errorPos = parsedText.pos
            this.setMsg('err', parsedText.message)
          }
        } else this.setMsg('', '')
        return
      }
      if (!trimmed) {
        this.showCount(0, false)
        this.setMsg('', MSG.emptySave)
        return
      }
      const parsed = tryParse(trimmed)
      if (!parsed.ok) {
        this.errorPos = parsed.pos
        this.showCount(raw.length, raw.length > this.maxLen)
        this.setMsg('err', parsed.message)
        return
      }
      const compact = stringifyJson(parsed.value, 0)
      const shown = raw.length <= this.maxLen ? raw.length : compact.length
      const overSource = shown > this.maxLen
      this.showCount(shown, overSource)
      if (overSource) this.setMsg('err', fmt(MSG.tooLong, compact.length, this.maxLen))
      else if (this.dirty && raw.length > this.maxLen) this.setMsg('ok', fmt(MSG.prettyWillCompact, raw.length, compact.length))
      else this.setMsg('ok', MSG.okJson)
    },
    commitText() {
      let raw = this.sourceText
      if (!this.dirty) {
        raw = this.originalValue
        this.sourceText = raw
      } else if (raw.length > this.maxLen) {
        const lengthMessage = fmt(MSG.lengthText, raw.length, this.maxLen)
        this.setMsg('err', lengthMessage)
        return { ok: false, message: lengthMessage }
      }
      const trimmed = raw.trim()
      if (!this.forceText && looksLikeJson(trimmed)) {
        const parsed = tryParse(trimmed)
        if (!parsed.ok) {
          this.errorPos = parsed.pos
          return {
            ok: false,
            needConfirm: true,
            message: MSG.needConfirmIntro + '\n' + parsed.message + '\n' + MSG.needConfirmOutro
          }
        }
      }
      this.forceText = false
      return { ok: true, value: raw }
    },
    commitSource() {
      const raw = this.sourceText
      const trimmed = raw.trim()
      if (!trimmed) {
        this.sourceText = ''
        return { ok: true, value: '' }
      }
      const parsed = tryParse(trimmed)
      if (!parsed.ok) {
        this.errorPos = parsed.pos
        this.setMsg('err', parsed.message)
        return { ok: false, message: parsed.message }
      }
      if (raw.length <= this.maxLen) return { ok: true, value: raw }
      const compact = stringifyJson(parsed.value, 0)
      if (compact.length <= this.maxLen) {
        this.sourceText = compact
        return { ok: true, value: compact }
      }
      const message = fmt(MSG.tooLong, compact.length, this.maxLen)
      this.setMsg('err', message)
      return { ok: false, message }
    },
    /** 提交前收口。ok 时 value 才是要写入 executorParam 的字符串。 */
    commit() {
      if (this.view === 'text') return this.commitText()
      // 打开后没改过，按载入时的原文提交，避免只点了源码排版就把参数改写
      if (!this.dirty) {
        const original = this.originalValue
        const trimmed = original.trim()
        if (!trimmed || tryParse(trimmed).ok) {
          this.sourceText = original
          return { ok: true, value: original }
        }
      }
      if (this.view === 'form') {
        const built = rowsToObject(this.rows)
        if (!built.ok) {
          this.showBuiltError(built)
          return { ok: false, message: built.message }
        }
        return this.writeBuilt(built)
      }
      return this.commitSource()
    },
    forceRaw() {
      this.forceText = true
    },
    focusProblem() {
      if (this.view === 'text') {
        this.view = 'source'
        this.mode = 'json'
        this.refreshStatus()
      }
      this.$nextTick(() => {
        if (this.view === 'form' && this.errorRow >= 0) {
          const trs = this.$el.querySelectorAll('tbody tr')
          const tr = trs[this.errorRow]
          const input = tr && tr.querySelector('input, textarea')
          if (input) input.focus()
          return
        }
        const ta = this.$el.querySelector('.param-source textarea')
        if (!ta) return
        ta.focus()
        if (typeof this.errorPos === 'number' && this.errorPos >= 0 && ta.setSelectionRange) {
          const end = Math.min(this.errorPos + 1, ta.value.length)
          ta.setSelectionRange(this.errorPos, end)
        }
      })
    }
  }
}
</script>

<style scoped>
.executor-param-editor { width: 100%; }
.param-toolbar { margin-bottom: 4px; min-height: 28px; }
.param-toolbar:after { content: ""; display: block; clear: both; }
.param-tools { margin-left: 8px; }
.param-tools .el-button { margin-right: 4px; }
.param-count { float: right; color: #999; font-size: 12px; line-height: 28px; }
.param-count.is-over { color: #f56c6c; font-weight: 600; }
.param-tip { display: block; margin: 0 0 6px; color: #909399; font-size: 12px; line-height: 1.4; }
.param-form { max-height: 320px; overflow: auto; border: 1px solid #dcdfe6; background: #fafafa; padding: 6px 8px 8px; }
.param-table { width: 100%; border-collapse: collapse; background: #fff; margin-bottom: 6px; }
.param-table th { text-align: left; font-size: 12px; color: #606266; font-weight: 600; padding: 4px; }
.param-table td { vertical-align: top; padding: 4px; }
.param-table .col-key { width: 28%; }
.param-table .col-type { width: 108px; }
.param-table .col-op { width: 48px; }
.array-item { display: flex; align-items: flex-start; margin-bottom: 4px; }
.array-item > .el-select { width: 88px; flex: none; margin-right: 4px; }
.array-item > .item-input { flex: 1; margin-right: 4px; }
.param-form-hint { margin: 6px 0 0; color: #909399; font-size: 12px; }
.param-msg { margin-top: 4px; font-size: 12px; line-height: 1.45; white-space: pre-wrap; word-break: break-all; min-height: 0; }
.param-msg.is-ok { color: #67c23a; }
.param-msg.is-err { color: #f56c6c; }
.is-source >>> textarea { font-family: Consolas, Monaco, "Courier New", monospace; font-size: 12px; line-height: 1.45; }
tr.has-error >>> .el-input__inner,
tr.has-error >>> .el-textarea__inner { border-color: #f56c6c; }
</style>
