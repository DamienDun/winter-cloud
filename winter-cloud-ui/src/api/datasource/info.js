import request from '@/utils/request'

export function listDatasource(query) {
  return request({
    url: '/datasource/info/list',
    method: 'get',
    params: query
  })
}

export function getDatasource(id) {
  return request({
    url: '/datasource/info/' + id,
    method: 'get'
  })
}

export function listAllDatasource() {
  return request({
    url: '/datasource/info/all',
    method: 'get'
  })
}

export function addDatasource(data) {
  return request({
    url: '/datasource/info',
    method: 'post',
    data: data
  })
}

export function updateDatasource(data) {
  return request({
    url: '/datasource/info',
    method: 'put',
    data: data
  })
}

export function delDatasource(ids) {
  return request({
    url: '/datasource/info/' + ids,
    method: 'delete'
  })
}

/** 批量修改状态：ids 为字符串数组，status 0停用/1启用 */
export function changeDatasourceStatus(ids, status) {
  return request({
    url: '/datasource/info/changeStatus',
    method: 'put',
    data: { ids, status }
  })
}

export function testDatasource(data) {
  return request({
    url: '/datasource/info/test',
    method: 'post',
    data: data
  })
}
