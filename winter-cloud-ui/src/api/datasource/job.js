import request from '@/utils/request'

export function listJob(query) {
  return request({
    url: '/datasource/job/list',
    method: 'get',
    params: query
  })
}

export function getJob(id) {
  return request({
    url: '/datasource/job/' + id,
    method: 'get'
  })
}

export function addJob(data) {
  return request({
    url: '/datasource/job',
    method: 'post',
    data: data
  })
}

export function updateJob(data) {
  return request({
    url: '/datasource/job',
    method: 'put',
    data: data
  })
}

export function delJob(ids) {
  return request({
    url: '/datasource/job/' + ids,
    method: 'delete'
  })
}

export function startJob(id) {
  return request({
    url: '/datasource/job/start/' + id,
    method: 'put'
  })
}

export function stopJob(id) {
  return request({
    url: '/datasource/job/stop/' + id,
    method: 'put'
  })
}

/** 批量开启/停用：ids 为字符串数组，status 0停用/1开启 */
export function changeJobStatus(ids, status) {
  return request({
    url: '/datasource/job/changeStatus',
    method: 'put',
    data: { ids, status }
  })
}

export function executeJob(id) {
  return request({
    url: '/datasource/job/execute/' + id,
    method: 'post'
  })
}

export function nextTriggerTime(cron) {
  return request({
    url: '/datasource/job/nextTriggerTime',
    method: 'get',
    params: { cron }
  })
}
