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

export function testApiDatasource(data) {
  return request({
    url: '/datasource/api/test',
    method: 'post',
    data: data
  })
}
