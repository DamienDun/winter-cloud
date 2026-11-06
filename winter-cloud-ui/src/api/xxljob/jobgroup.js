import request from '@/utils/request'

export function listJobGroup(query) {
  return request({ url: '/xxljob/jobgroup/list', method: 'get', params: query })
}
export function listAllJobGroup() {
  return request({ url: '/xxljob/jobgroup/all', method: 'get' })
}
export function addJobGroup(data) {
  return request({ url: '/xxljob/jobgroup', method: 'post', data })
}
export function updateJobGroup(data) {
  return request({ url: '/xxljob/jobgroup', method: 'put', data })
}
export function delJobGroup(id) {
  return request({ url: '/xxljob/jobgroup/' + id, method: 'delete' })
}
