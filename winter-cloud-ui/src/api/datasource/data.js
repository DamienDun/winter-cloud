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

export function createTable(data) {
  return request({
    url: '/datasource/data/schema/table',
    method: 'post',
    data: data
  })
}

export function dropTable(data) {
  return request({
    url: '/datasource/data/schema/table',
    method: 'delete',
    data: data
  })
}

export function alterColumn(data) {
  return request({
    url: '/datasource/data/schema/column',
    method: 'post',
    data: data
  })
}

export function listIndexes(query) {
  return request({
    url: '/datasource/data/schema/indexes',
    method: 'get',
    params: query
  })
}

export function createIndex(data) {
  return request({
    url: '/datasource/data/schema/index',
    method: 'post',
    data: data
  })
}

export function updateIndex(data) {
  return request({
    url: '/datasource/data/schema/index',
    method: 'put',
    data: data
  })
}

export function dropIndex(data) {
  return request({
    url: '/datasource/data/schema/index',
    method: 'delete',
    data: data
  })
}
