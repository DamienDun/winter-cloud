import request from '@/utils/request'

export function listJobLog(query) {
  return request({ url: '/xxljob/joblog/list', method: 'get', params: query })
}
export function listJobsByGroup(jobGroup) {
  return request({ url: '/xxljob/joblog/jobs', method: 'get', params: { jobGroup } })
}
export function logDetail(query) {
  return request({ url: '/xxljob/joblog/detail', method: 'get', params: query })
}
export function clearJobLog(query) {
  return request({ url: '/xxljob/joblog/clear', method: 'delete', params: query })
}
export function killJobLog(id) {
  return request({ url: '/xxljob/joblog/kill/' + id, method: 'post' })
}
