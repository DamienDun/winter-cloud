import request from '@/utils/request'

export function listJobInfo(query) {
  return request({ url: '/xxljob/jobinfo/list', method: 'get', params: query })
}
export function jobEnums() {
  return request({ url: '/xxljob/jobinfo/enums', method: 'get' })
}
export function addJobInfo(data) {
  return request({ url: '/xxljob/jobinfo', method: 'post', data })
}
export function updateJobInfo(data) {
  return request({ url: '/xxljob/jobinfo', method: 'put', data })
}
export function delJobInfo(id) {
  return request({ url: '/xxljob/jobinfo/' + id, method: 'delete' })
}
export function startJobInfo(id) {
  return request({ url: '/xxljob/jobinfo/start/' + id, method: 'put' })
}
export function stopJobInfo(id) {
  return request({ url: '/xxljob/jobinfo/stop/' + id, method: 'put' })
}
export function triggerJobInfo(data) {
  return request({ url: '/xxljob/jobinfo/trigger', method: 'post', data })
}
export function nextTriggerTime(query) {
  return request({ url: '/xxljob/jobinfo/nextTriggerTime', method: 'get', params: query })
}
export function getJobInfo(id) {
  return request({ url: '/xxljob/jobinfo/' + id, method: 'get' })
}
export function saveJobGlue(data) {
  return request({ url: '/xxljob/jobinfo/glue', method: 'post', data })
}
