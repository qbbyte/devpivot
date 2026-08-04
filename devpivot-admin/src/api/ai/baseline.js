import request from '@/utils/request'

// 查询需求基线列表
export function listBaseline(query) {
  return request({
    url: '/system/baseline/list',
    method: 'get',
    params: query
  })
}

// 查询需求基线详细
export function getBaseline(baselineId) {
  return request({
    url: '/system/baseline/' + baselineId,
    method: 'get'
  })
}

// 新增需求基线
export function addBaseline(data) {
  return request({
    url: '/system/baseline',
    method: 'post',
    data: data
  })
}

// 修改需求基线
export function updateBaseline(data) {
  return request({
    url: '/system/baseline',
    method: 'put',
    data: data
  })
}

// 删除需求基线
export function delBaseline(baselineId) {
  return request({
    url: '/system/baseline/' + baselineId,
    method: 'delete'
  })
}
