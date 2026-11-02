import request from '@/utils/request'

/** FormData upload: drop default JSON Content-Type so browser sets boundary */
function multipartConfig(extra = {}) {
  return {
    headers: { repeatSubmit: false },
    transformRequest: [
      (data, headers) => {
        if (typeof FormData !== 'undefined' && data instanceof FormData) {
          delete headers['Content-Type']
        }
        return data
      }
    ],
    ...extra
  }
}

export function listBigFileTask(query) {
  return request({
    url: '/file/bigFile/list',
    method: 'get',
    params: query
  })
}

export function getBigFileTask(id) {
  return request({
    url: '/file/bigFile/' + id,
    method: 'get'
  })
}

export function delBigFileTask(ids) {
  return request({
    url: '/file/bigFile/' + ids,
    method: 'delete'
  })
}

export function bigFileApply(data) {
  return request({
    url: '/file/bigFile/upload/apply',
    method: 'post',
    data: data
  })
}

export function bigFileStatus(data) {
  return request({
    url: '/file/bigFile/upload/status',
    method: 'post',
    data: data
  })
}

export function bigFileUploadPart(data) {
  return request({
    url: '/file/bigFile/upload',
    method: 'post',
    data: data,
    ...multipartConfig({ timeout: 5 * 60 * 1000 })
  })
}

export function bigFileCancel(data) {
  return request({
    url: '/file/bigFile/upload/cancel',
    method: 'post',
    data: data
  })
}

export function bigFileMerge(data) {
  return request({
    url: '/file/bigFile/merge',
    method: 'post',
    data: data
  })
}