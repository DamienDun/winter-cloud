import request from '@/utils/request'

/** FormData 上传时去掉默认 JSON Content-Type，由浏览器补 boundary */
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

/**
 * 普通文件上传
 * @param {FormData} data 含 file 字段
 */
export function uploadFile(data) {
  return request({
    url: '/file/upload',
    method: 'post',
    data: data,
    ...multipartConfig({ timeout: 120000 })
  })
}

/**
 * 批量上传
 * @param {FormData} data 含多个 file
 */
export function uploadFiles(data) {
  return request({
    url: '/file/uploads',
    method: 'post',
    data: data,
    ...multipartConfig({ timeout: 180000 })
  })
}

/**
 * 删除文件
 * @param {string} fileUrl
 */
export function deleteFile(fileUrl) {
  return request({
    url: '/file/delete',
    method: 'delete',
    params: { fileUrl }
  })
}

/**
 * 大文件分片：申请上传任务
 */
export function bigFileApply(data) {
  return request({
    url: '/file/bigFile/upload/apply',
    method: 'post',
    data: data
  })
}

/**
 * 大文件分片：查询上传状态
 */
export function bigFileStatus(data) {
  return request({
    url: '/file/bigFile/upload/status',
    method: 'post',
    data: data
  })
}

/**
 * 大文件分片：上传单个分片（FormData: file, uploadId, partNumber）
 */
export function bigFileUploadPart(data) {
  return request({
    url: '/file/bigFile/upload',
    method: 'post',
    data: data,
    ...multipartConfig({ timeout: 5 * 60 * 1000 })
  })
}

/**
 * 大文件分片：取消上传
 */
export function bigFileCancel(data) {
  return request({
    url: '/file/bigFile/upload/cancel',
    method: 'post',
    data: data
  })
}

/**
 * 大文件分片：手动合并（一般自动合并，兜底用）
 */
export function bigFileMerge(data) {
  return request({
    url: '/file/bigFile/merge',
    method: 'post',
    data: data
  })
}
