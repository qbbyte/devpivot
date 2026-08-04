import request from '@/utils/request'

// 查询AI澄清问题记录列表
export function listClarify(query) {
  return request({
    url: '/system/clarify/list',
    method: 'get',
    params: query
  })
}

// 查询AI澄清问题记录详细
export function getClarify(recordId) {
  return request({
    url: '/system/clarify/' + recordId,
    method: 'get'
  })
}

// 新增AI澄清问题记录
export function addClarify(data) {
  return request({
    url: '/system/clarify',
    method: 'post',
    data: data
  })
}

// 修改AI澄清问题记录
export function updateClarify(data) {
  return request({
    url: '/system/clarify',
    method: 'put',
    data: data
  })
}

// 删除AI澄清问题记录
export function delClarify(recordId) {
  return request({
    url: '/system/clarify/' + recordId,
    method: 'delete'
  })
}
