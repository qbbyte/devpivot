import request from '@/utils/request'

// 查询版本全链路记录列表
export function listRecord(query) {
  return request({
    url: '/system/record/list',
    method: 'get',
    params: query
  })
}

// 查询版本全链路记录详细
export function getRecord(recordId) {
  return request({
    url: '/system/record/' + recordId,
    method: 'get'
  })
}

// 新增版本全链路记录
export function addRecord(data) {
  return request({
    url: '/system/record',
    method: 'post',
    data: data
  })
}

// 修改版本全链路记录
export function updateRecord(data) {
  return request({
    url: '/system/record',
    method: 'put',
    data: data
  })
}

// 删除版本全链路记录
export function delRecord(recordId) {
  return request({
    url: '/system/record/' + recordId,
    method: 'delete'
  })
}
