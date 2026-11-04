import request from '@/utils/request'

export function listTables(query) {
  return request({
    url: '/datasource/data/tables',
    method: 'get',
    params: query
  })
}

export function listColumns(query) {
  return request({
    url: '/datasource/data/columns',
    method: 'get',
    params: query
  })
}

export function listRows(query) {
  return request({
    url: '/datasource/data/rows',
    method: 'get',
    params: query
  })
}

export function addRow(data) {
  return request({
    url: '/datasource/data/rows',
    method: 'post',
    data: data
  })
}

export function updateRow(data) {
  return request({
    url: '/datasource/data/rows',
    method: 'put',
    data: data
  })
}

export function delRow(data) {
  return request({
    url: '/datasource/data/rows',
    method: 'delete',
    data: data
  })
}
