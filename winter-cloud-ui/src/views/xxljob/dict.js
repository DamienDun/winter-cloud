/**
 * xxl-job-admin 2.3.0 中文文案（对齐 message_zh_CN.properties）
 */
export const scheduleTypeLabel = {
  NONE: '无',
  CRON: 'CRON',
  FIX_RATE: '固定速度',
  FIX_DELAY: '固定延迟'
}

export const misfireLabel = {
  DO_NOTHING: '忽略',
  FIRE_ONCE_NOW: '立即执行一次'
}

export const routeLabel = {
  FIRST: '第一个',
  LAST: '最后一个',
  ROUND: '轮询',
  RANDOM: '随机',
  CONSISTENT_HASH: '一致性HASH',
  LEAST_FREQUENTLY_USED: '最不经常使用',
  LEAST_RECENTLY_USED: '最近最久未使用',
  FAILOVER: '故障转移',
  BUSYOVER: '忙碌转移',
  SHARDING_BROADCAST: '分片广播'
}

export const blockLabel = {
  SERIAL_EXECUTION: '单机串行',
  DISCARD_LATER: '丢弃后续调度',
  COVER_EARLY: '覆盖之前调度'
}

export const glueTypeLabel = {
  BEAN: 'BEAN',
  GLUE_GROOVY: 'GLUE(Java)',
  GLUE_SHELL: 'GLUE(Shell)',
  GLUE_PYTHON: 'GLUE(Python)',
  GLUE_PHP: 'GLUE(PHP)',
  GLUE_NODEJS: 'GLUE(Nodejs)',
  GLUE_POWERSHELL: 'GLUE(PowerShell)'
}

export const logCleanTypes = [
  { value: 1, label: '清理一个月之前日志数据' },
  { value: 2, label: '清理三个月之前日志数据' },
  { value: 3, label: '清理六个月之前日志数据' },
  { value: 4, label: '清理一年之前日志数据' },
  { value: 5, label: '清理一千条以前日志数据' },
  { value: 6, label: '清理一万条以前日志数据' },
  { value: 7, label: '清理三万条以前日志数据' },
  { value: 8, label: '清理十万条以前日志数据' },
  { value: 9, label: '清理所有日志数据' }
]

export function labelOf(map, key) {
  if (key == null || key === '') return ''
  return map[key] != null ? map[key] : key
}

export function optionsOf(map, keys) {
  return (keys || Object.keys(map)).map(k => ({ value: k, label: map[k] || k }))
}
