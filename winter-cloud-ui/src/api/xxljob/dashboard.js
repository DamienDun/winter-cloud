import request from '@/utils/request'

export function dashboardInfo() {
  return request({ url: '/xxljob/dashboard/info', method: 'get' })
}

export function chartInfo(query) {
  return request({ url: '/xxljob/dashboard/chart', method: 'get', params: query })
}
