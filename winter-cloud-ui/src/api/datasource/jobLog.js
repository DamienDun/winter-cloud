import request from '@/utils/request'

export function listJobLog(query) {
  return request({
    url: '/datasource/jobLog/list',
    method: 'get',
    params: query
  })
}

export function getJobLog(id) {
  return request({
    url: '/datasource/jobLog/' + id,
    method: 'get'
  })
}

export function delJobLog(ids) {
  return request({
    url: '/datasource/jobLog/' + ids,
    method: 'delete'
  })
}
