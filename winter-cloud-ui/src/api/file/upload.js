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

export function uploadFile(data) {
  return request({
    url: '/file/upload',
    method: 'post',
    data: data,
    ...multipartConfig({ timeout: 120000 })
  })
}

export function uploadFiles(data) {
  return request({
    url: '/file/uploads',
    method: 'post',
    data: data,
    ...multipartConfig({ timeout: 180000 })
  })
}

export function deleteFile(fileUrl) {
  return request({
    url: '/file/delete',
    method: 'delete',
    params: { fileUrl }
  })
}

// re-export big-file APIs for existing import paths
export {
  bigFileApply,
  bigFileStatus,
  bigFileUploadPart,
  bigFileCancel,
  bigFileMerge
} from './bigFile'