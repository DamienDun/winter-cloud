/**
 * 任务参数的纯逻辑，和 xxl-job-admin 的 executor.param.editor.js 对齐。
 * 文本模式原样保存；JSON 表单只填一层键值，值可以是简单数组。
 * 超过 15 位的整数不能交给 JSON.parse，否则钉钉这类 id 末尾会被改掉。
 */
export const MAX_LEN = 512

const JSON_NUMBER = /^-?(?:0|[1-9]\d*)(?:\.\d+)?(?:[eE][+-]?\d+)?$/
const LONG_PREFIX = '\u0001LONG:'

export const MSG = {
  tipText: '普通文字会原样提交。要填 JSON 请点 JSON，用表单逐项填写，不用手写引号和逗号。',
  tipForm: '每行一个参数。要填数组，类型选「数组」，一个元素一行，不用写中括号和逗号。',
  tipSource: '直接修改 JSON。使用英文双引号，最后一项后面不要逗号。',
  formHint: '值可以是文本、数字、是/否、空，也可以是数组。数组里再套对象时用源码。带引号的 "[1,2]" 仍然是文本，不是数组。',
  textPh: '普通文字直接填写，原样传给任务',
  sourcePh: '在这里粘贴或修改 JSON',
  keyPh: '例如 type',
  valuePh: '直接填写，不用加引号',
  numberPh: '例如 5',
  nullPh: '空值，不用填写',
  countTitle: '最多 512 个字符，与数据库字段一致，超长会拦住，避免保存时被截断',
  okJson: 'JSON 格式正确。',
  formReady: '可以保存，引号会自动加上。',
  emptySave: '没有填写参数，保存后为空。',
  emptyObjectSave: '没有参数项，保存后是空对象 {}。',
  textLooksJson: '这是合法 JSON，可以直接保存。要按项修改请点 JSON。',
  needConfirmIntro: '这段内容像 JSON，但格式不合法：',
  needConfirmOutro: '点「去修改」到 JSON 模式里改。如果本来就要传这段原文，点「仍按原文保存」。',
  tooLong: '压缩后仍有 {0} 个字符，超过 {1} 的上限。请缩短内容，否则数据库会截断。',
  lengthText: '已有 {0} 个字符，最多 {1} 个。',
  prettyWillCompact: '排版后有 {0} 个字符，保存时会自动压成一行（{1} 个字符）。',
  rowNoKey: '第 {0} 行没有填写参数名。',
  rowDup: '第 {0} 行参数名「{1}」重复了。',
  rowNumber: '第 {0} 行不是合法数字。填 0、5、1.5 这种，不要加引号，也不要写成 01。',
  rowArrayNumber: '第 {0} 行的数组，第 {1} 个不是合法数字。填 0、5 或长编号，不要加引号，也不要写成 01。',
  notFlat: '表单要填的是带参数名的对象，值可以是数组。当前内容有嵌套对象，或者数组里又套了数组/对象，请在源码里改。',
  fixNone: '没能自动修好，原文没有改动。自动修复只处理中文引号、中文逗号冒号括号，以及括号前多余的逗号。',
  fixOk: '已修好：{0}。现在是合法 JSON。',
  alreadyJson: '已经是合法 JSON，不用修复。',
  where: '第 {0} 行，第 {1} 个字符附近。',
  hintQuote: '用了中文双引号，JSON 必须用英文双引号 " 。可点「修复标点」。',
  hintComma: '用了中文逗号，JSON 里要写成英文逗号 , 。可点「修复标点」。',
  hintColon: '用了中文冒号，JSON 里要写成英文冒号 : 。可点「修复标点」。',
  hintBracket: '用了中文括号，要改成英文括号 {} [] 。可点「修复标点」。',
  hintTrailing: '最后一项后面多了逗号，JSON 不允许这样写。可点「修复标点」。',
  hintSingle: '键或值用了单引号。JSON 只能用英文双引号。SQL 里的单引号请放到表单的「文本」里，不要在源码里裸写。',
  hintComment: 'JSON 不能写 // 或 /* 注释。',
  hintKey: '键名也要加英文双引号，例如 "type": 5 。',
  hintGeneric: '格式不对。检查括号是否成对、项与项之间是否有英文逗号、文字是否用英文双引号包起来。',
  snippet: '这一行：{0}'
}

export function fmt(str) {
  const args = Array.prototype.slice.call(arguments, 1)
  return String(str).replace(/\{(\d+)\}/g, function (_, n) {
    const value = args[parseInt(n, 10)]
    return value == null ? '' : String(value)
  })
}

function ownKeys(obj) {
  const keys = []
  for (const key in obj) {
    if (Object.prototype.hasOwnProperty.call(obj, key)) keys.push(key)
  }
  return keys
}

function isArrayValue(value) {
  return Object.prototype.toString.call(value) === '[object Array]'
}

function longDigitsOf(value) {
  if (typeof value !== 'string' || value.indexOf(LONG_PREFIX) !== 0) return null
  return value.substring(LONG_PREFIX.length)
}

function isPrimitiveValue(item) {
  if (item === null || longDigitsOf(item) != null) return true
  const kind = typeof item
  return kind === 'string' || kind === 'number' || kind === 'boolean'
}

export function isSimpleArray(value) {
  if (!isArrayValue(value)) return false
  for (let i = 0; i < value.length; i++) {
    if (!isPrimitiveValue(value[i])) return false
    if (typeof value[i] === 'number' && !isFinite(value[i])) return false
  }
  return true
}

export function isFlatObject(value) {
  if (!value || Object.prototype.toString.call(value) !== '[object Object]') return false
  const keys = ownKeys(value)
  for (let i = 0; i < keys.length; i++) {
    const item = value[keys[i]]
    if (isSimpleArray(item)) continue
    if (!isPrimitiveValue(item)) return false
    if (typeof item === 'number' && !isFinite(item)) return false
  }
  return true
}

export function detectType(value) {
  if (value === null) return 'null'
  if (isArrayValue(value)) return 'array'
  if (typeof value === 'boolean') return 'boolean'
  if (typeof value === 'number' || longDigitsOf(value) != null) return 'number'
  return 'string'
}

export function numberText(value) {
  const digits = longDigitsOf(value)
  if (digits != null) return digits
  return value == null ? '' : String(value)
}

/** 超过 15 位的整数保留成标记字符串，写回 JSON 时再输出成不带引号的数字。 */
export function parseNumberToken(token) {
  if (/^-?\d+$/.test(token) && token.replace('-', '').length > 15) return LONG_PREFIX + token
  return JSON.parse(token)
}

export function looksLikeJson(trimmed) {
  if (!trimmed) return false
  const ch = trimmed.charAt(0)
  return ch === '{' || ch === '['
}

function posFromLineCol(text, line, col) {
  const lines = text.split('\n')
  let pos = 0
  for (let i = 0; i < line - 1 && i < lines.length; i++) pos += lines[i].length + 1
  return pos + Math.max(col - 1, 0)
}

function locate(text, err) {
  const msg = String((err && err.message) || '')
  let line = 0
  let col = 0
  let pos = -1
  const lineCol = msg.match(/line\s+(\d+)\s+column\s+(\d+)/i)
  if (lineCol) {
    line = parseInt(lineCol[1], 10)
    col = parseInt(lineCol[2], 10)
    pos = posFromLineCol(text, line, col)
  } else {
    const posMatch = msg.match(/position\s+(\d+)/i)
    if (posMatch) {
      pos = parseInt(posMatch[1], 10)
      const head = text.slice(0, pos)
      line = head.split('\n').length
      col = pos - head.lastIndexOf('\n')
    }
  }
  let lineText = ''
  if (line > 0) {
    lineText = (text.split('\n')[line - 1] || '').trim()
    if (lineText.length > 120) lineText = lineText.substring(0, 120) + '...'
  }
  return { line, col, pos, lineText }
}

function stripTrailingCommas(text) {
  let out = ''
  let inString = false
  let escaped = false
  let removed = 0
  for (let i = 0; i < text.length; i++) {
    const ch = text.charAt(i)
    if (inString) {
      out += ch
      if (escaped) {
        escaped = false
        continue
      }
      if (ch === '\\') {
        escaped = true
        continue
      }
      if (ch === '"') inString = false
      continue
    }
    if (ch === '"') {
      inString = true
      out += ch
      continue
    }
    if (ch === ',') {
      let j = i + 1
      while (j < text.length && /\s/.test(text.charAt(j))) j++
      if (j < text.length && (text.charAt(j) === '}' || text.charAt(j) === ']')) {
        removed++
        continue
      }
    }
    out += ch
  }
  return { text: out, removed }
}

function replaceOutsideStrings(text) {
  let out = ''
  let inString = false
  let escaped = false
  let comma = 0
  let colon = 0
  let brace = 0
  for (let i = 0; i < text.length; i++) {
    const ch = text.charAt(i)
    if (inString) {
      out += ch
      if (escaped) {
        escaped = false
        continue
      }
      if (ch === '\\') {
        escaped = true
        continue
      }
      if (ch === '"') inString = false
      continue
    }
    if (ch === '"') {
      inString = true
      out += ch
      continue
    }
    if (ch === '，') { comma++; out += ','; continue }
    if (ch === '：') { colon++; out += ':'; continue }
    if (ch === '｛') { brace++; out += '{'; continue }
    if (ch === '｝') { brace++; out += '}'; continue }
    if (ch === '［') { brace++; out += '['; continue }
    if (ch === '］') { brace++; out += ']'; continue }
    out += ch
  }
  return { text: out, comma, colon, brace }
}

function countMatches(text, pattern) {
  const matched = text.match(pattern)
  return matched ? matched.length : 0
}

/** 只接受修完后能 parse 的结果。中文标点只替换双引号外面的，避免改掉 SQL 正文。 */
export function tryFixPunctuation(text) {
  const steps = []
  let next = text
  const quoteCount = countMatches(next, /[“”＂]/g)
  if (quoteCount) {
    next = next.replace(/[“”＂]/g, '"')
    steps.push('中文双引号 ' + quoteCount)
  }
  const outside = replaceOutsideStrings(next)
  next = outside.text
  if (outside.comma) steps.push('中文逗号 ' + outside.comma)
  if (outside.colon) steps.push('中文冒号 ' + outside.colon)
  if (outside.brace) steps.push('中文括号 ' + outside.brace)
  const stripped = stripTrailingCommas(next)
  next = stripped.text
  if (stripped.removed) steps.push('多余尾逗号 ' + stripped.removed)
  return { changed: next !== text, text: next, summary: steps.join('，') }
}

function detectHint(text) {
  if (/[“”＂]/.test(text)) return MSG.hintQuote
  if (/，/.test(text)) return MSG.hintComma
  if (/：/.test(text)) return MSG.hintColon
  if (/[｛｝［］]/.test(text)) return MSG.hintBracket
  if (stripTrailingCommas(text).removed > 0) return MSG.hintTrailing
  if (/'\s*:/.test(text) || /:\s*'/.test(text)) return MSG.hintSingle
  if (/\/\/|\/\*/.test(text)) return MSG.hintComment
  if (/[{,]\s*[A-Za-z_][\w]*\s*:/.test(text)) return MSG.hintKey
  return MSG.hintGeneric
}

function explain(text, loc) {
  const parts = []
  if (loc.line > 0) parts.push(fmt(MSG.where, loc.line, loc.col))
  parts.push(detectHint(text))
  if (loc.lineText) parts.push(fmt(MSG.snippet, loc.lineText))
  return parts.join('\n')
}

/**
 * 字符串外面的超长整数换成标记。必须写成 JSON 转义 \\u0001，裸控制字符 JSON.parse 不接受。
 * 字符串里面的数字不动，避免把 SQL 或说明文字里的长数字改掉。
 */
function protectLongInts(text) {
  let out = ''
  let inString = false
  let escaped = false
  let i = 0
  while (i < text.length) {
    const ch = text.charAt(i)
    if (inString) {
      out += ch
      if (escaped) escaped = false
      else if (ch === '\\') escaped = true
      else if (ch === '"') inString = false
      i++
      continue
    }
    if (ch === '"') {
      inString = true
      out += ch
      i++
      continue
    }
    if (ch === '-' || (ch >= '0' && ch <= '9')) {
      const start = i
      let j = i
      if (text.charAt(j) === '-') j++
      const digitStart = j
      if (!(text.charAt(j) >= '0' && text.charAt(j) <= '9')) {
        out += ch
        i++
        continue
      }
      while (j < text.length && text.charAt(j) >= '0' && text.charAt(j) <= '9') j++
      const intEnd = j
      let pureInt = true
      if (j < text.length && text.charAt(j) === '.') {
        pureInt = false
        j++
        while (j < text.length && text.charAt(j) >= '0' && text.charAt(j) <= '9') j++
      }
      if (j < text.length && (text.charAt(j) === 'e' || text.charAt(j) === 'E')) {
        pureInt = false
        j++
        if (text.charAt(j) === '+' || text.charAt(j) === '-') j++
        while (j < text.length && text.charAt(j) >= '0' && text.charAt(j) <= '9') j++
      }
      const digits = text.slice(digitStart, intEnd)
      if (pureInt && digits.length > 15 && digits.charAt(0) !== '0') {
        out += '"\\u0001LONG:' + text.slice(start, intEnd) + '"'
        i = intEnd
        continue
      }
      out += text.slice(start, pureInt ? intEnd : j)
      i = pureInt ? intEnd : j
      continue
    }
    out += ch
    i++
  }
  return out
}

export function tryParse(text) {
  try {
    JSON.parse(text)
  } catch (err) {
    const loc = locate(text, err)
    return { ok: false, message: explain(text, loc), pos: loc.pos }
  }
  try {
    return { ok: true, value: JSON.parse(protectLongInts(text)) }
  } catch (err2) {
    return { ok: true, value: JSON.parse(text) }
  }
}

function encodeJson(value, space, depth) {
  const digits = longDigitsOf(value)
  if (digits != null) return digits
  if (value === null || typeof value !== 'object') return JSON.stringify(value)
  const pretty = space > 0
  const gap = pretty ? new Array(space + 1).join(' ') : ''
  if (isArrayValue(value)) {
    if (!value.length) return '[]'
    if (!pretty) {
      const compactItems = []
      for (let i = 0; i < value.length; i++) compactItems.push(encodeJson(value[i], 0, 0))
      return '[' + compactItems.join(',') + ']'
    }
    const inner = new Array(depth + 2).join(gap)
    const base = new Array(depth + 1).join(gap)
    const items = []
    for (let a = 0; a < value.length; a++) items.push('\n' + inner + encodeJson(value[a], space, depth + 1))
    return '[' + items.join(',') + '\n' + base + ']'
  }
  const keys = ownKeys(value)
  if (!keys.length) return '{}'
  if (!pretty) {
    const compactProps = []
    for (let k = 0; k < keys.length; k++) {
      compactProps.push(JSON.stringify(keys[k]) + ':' + encodeJson(value[keys[k]], 0, 0))
    }
    return '{' + compactProps.join(',') + '}'
  }
  const innerObj = new Array(depth + 2).join(gap)
  const baseObj = new Array(depth + 1).join(gap)
  const props = []
  for (let p = 0; p < keys.length; p++) {
    props.push('\n' + innerObj + JSON.stringify(keys[p]) + ': ' + encodeJson(value[keys[p]], space, depth + 1))
  }
  return '{' + props.join(',') + '\n' + baseObj + '}'
}

/** 不能用 JSON.stringify，否则长整数标记会被写成带引号的字符串。 */
export function stringifyJson(value, space) {
  return encodeJson(value, space || 0, 0)
}

export function chooseSaved(value, maxLen) {
  const pretty = stringifyJson(value, 2)
  if (pretty.length <= maxLen) return pretty
  return stringifyJson(value, 0)
}

export function blankRow() {
  return { key: '', type: 'string', text: '', bool: true, items: [], _prev: 'string' }
}

export function toItem(value) {
  const type = detectType(value)
  if (type === 'boolean') return { type, text: '', bool: value !== false }
  if (type === 'null') return { type: 'null', text: '', bool: true }
  if (type === 'number') return { type: 'number', text: numberText(value), bool: true }
  return { type: 'string', text: value == null ? '' : String(value), bool: true }
}

export function toRow(key, value) {
  const type = detectType(value)
  const row = { key: key || '', type, text: '', bool: true, items: [], _prev: type }
  if (type === 'array') {
    row.items = value.map(toItem)
    return row
  }
  if (type === 'boolean') {
    row.bool = value !== false
    return row
  }
  if (type === 'null') return row
  row.text = type === 'number' ? numberText(value) : (value == null ? '' : String(value))
  return row
}

export function valueToRows(obj) {
  return ownKeys(obj || {}).map(key => toRow(key, obj[key]))
}

function rowTouched(row) {
  if (String(row.key || '').trim()) return true
  if (row.type === 'number' || row.type === 'boolean' || row.type === 'null' || row.type === 'array') return true
  return !!(row.type === 'string' && row.text)
}

function readItems(row, rowIndex) {
  const items = []
  const list = row.items || []
  for (let i = 0; i < list.length; i++) {
    const item = list[i]
    if (item.type === 'null') {
      items.push(null)
      continue
    }
    if (item.type === 'boolean') {
      items.push(item.bool === true || item.bool === 'true')
      continue
    }
    if (item.type === 'number') {
      const token = String(item.text || '').trim()
      if (!JSON_NUMBER.test(token)) {
        return { ok: false, message: fmt(MSG.rowArrayNumber, rowIndex, i + 1) }
      }
      items.push(parseNumberToken(token))
      continue
    }
    items.push(item.text || '')
  }
  return { ok: true, value: items }
}

export function rowsToObject(rows) {
  const obj = {}
  const seen = {}
  const list = rows || []
  for (let i = 0; i < list.length; i++) {
    const row = list[i]
    if (!rowTouched(row)) continue
    const index = i + 1
    const key = String(row.key || '').trim()
    if (!key) return { ok: false, message: fmt(MSG.rowNoKey, index), rowIndex: i }
    if (Object.prototype.hasOwnProperty.call(seen, key)) {
      return { ok: false, message: fmt(MSG.rowDup, index, key), rowIndex: i }
    }
    seen[key] = true
    if (row.type === 'null') {
      obj[key] = null
      continue
    }
    if (row.type === 'boolean') {
      obj[key] = row.bool === true || row.bool === 'true'
      continue
    }
    if (row.type === 'number') {
      const token = String(row.text || '').trim()
      if (!JSON_NUMBER.test(token)) return { ok: false, message: fmt(MSG.rowNumber, index), rowIndex: i }
      obj[key] = parseNumberToken(token)
      continue
    }
    if (row.type === 'array') {
      const items = readItems(row, index)
      if (!items.ok) return { ok: false, message: items.message, rowIndex: i }
      obj[key] = items.value
      continue
    }
    obj[key] = row.text || ''
  }
  if (!ownKeys(obj).length) return { ok: true, empty: true, value: null }
  return { ok: true, empty: false, value: obj }
}

/** 类型改成数组时，尽量把原来的单个值收成一个元素，已经是简单数组的文本则展开。 */
export function coerceArraySeed(prevType, prev) {
  if (isArrayValue(prev)) return prev
  if (prevType === 'number' && typeof prev === 'string' && JSON_NUMBER.test(prev.trim())) {
    return [parseNumberToken(prev.trim())]
  }
  if (prevType === 'boolean') return [prev === true]
  if (prevType === 'null') return [null]
  if (typeof prev === 'string') {
    const parsed = tryParse(prev)
    if (parsed.ok && isSimpleArray(parsed.value)) return parsed.value
  }
  return prev
}

export { JSON_NUMBER }
