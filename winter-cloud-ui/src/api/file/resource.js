import request from '@/utils/request'

export function listResourceFile(query) {
  return request({
    url: '/file/list',
    method: 'get',
    params: query
  })
}

export function getResourceFile(id) {
  return request({
    url: '/file/' + id,
    method: 'get'
  })
}

export function delResourceFile(ids) {
  return request({
    url: '/file/' + ids,
    method: 'delete'
  })
}