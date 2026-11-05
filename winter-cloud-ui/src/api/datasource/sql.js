import request from '@/utils/request'

/** ÷¥––÷ª∂¡ SQL ≤È—Ø */
export function runSqlQuery(data) {
  return request({
    url: '/datasource/sql/query',
    method: 'post',
    data: data,
    timeout: 60000
  })
}
