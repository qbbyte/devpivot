import request from '@/utils/request'

// 查询原型组件清单列表
export function listComponent(query) {
  return request({
    url: '/system/component/list',
    method: 'get',
    params: query
  })
}

// 查询原型组件清单详细
export function getComponent(compId) {
  return request({
    url: '/system/component/' + compId,
    method: 'get'
  })
}

// 新增原型组件清单
export function addComponent(data) {
  return request({
    url: '/system/component',
    method: 'post',
    data: data
  })
}

// 修改原型组件清单
export function updateComponent(data) {
  return request({
    url: '/system/component',
    method: 'put',
    data: data
  })
}

// 删除原型组件清单
export function delComponent(compId) {
  return request({
    url: '/system/component/' + compId,
    method: 'delete'
  })
}
