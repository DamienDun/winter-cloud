import request from '@/utils/request'

export function todayJobStats() {
  return request({
    url: '/datasource/jobStats/today',
    method: 'get'
  })
}
