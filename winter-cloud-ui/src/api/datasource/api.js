import request from '@/utils/request'

export function listApiDatasource(query) {
  return request({
    url: '/datasource/api/list',
    method: 'get',
    params: query
  })
}

export function getApiDatasource(id) {
  return request({
    url: '/datasource/api/' + id,
    method: 'get'
  })
}

export function listRelyCandidates(params) {
  return request({
    url: '/datasource/api/rely/list',
    method: 'get',
    params: params
  })
}

export function listApiOutputFields(id) {
  return request({
    url: '/datasource/api/fields/' + id,
    method: 'get'
  })
}

export function addApiDatasource(data) {
  return request({
    url: '/datasource/api',
    method: 'post',
    data: data
  })
}

export function updateApiDatasource(data) {
  return request({
    url: '/datasource/api',
    method: 'put',
    data: data
  })
}

export function delApiDatasource(ids) {
  return request({
    url: '/datasource/api/' + ids,
    method: 'delete'
  })
}

/** 批量修改状态：ids 为字符串数组，status 0停用/1启用 */
export function changeApiDatasourceStatus(ids, status) {
  return request({
    url: '/datasource/api/changeStatus',
    method: 'put',
    data: { ids, status }
  })
}

export function testApiDatasource(data) {
  return request({
    url: '/datasource/api/test',
    method: 'post',
    data: data,
    // 测试可重复点，关闭防重复提交（否则同参 1 秒内会被静默拦截）
    headers: { repeatSubmit: false }
  })
}
