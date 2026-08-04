import request from '@/utils/request'

// 查询原型页面列表
export function listPage(query) {
  return request({
    url: '/system/page/list',
    method: 'get',
    params: query
  })
}

// 查询原型页面详细
export function getPage(pageId) {
  return request({
    url: '/system/page/' + pageId,
    method: 'get'
  })
}

// 新增原型页面
export function addPage(data) {
  return request({
    url: '/system/page',
    method: 'post',
    data: data
  })
}

// 修改原型页面
export function updatePage(data) {
  return request({
    url: '/system/page',
    method: 'put',
    data: data
  })
}

// 删除原型页面
export function delPage(pageId) {
  return request({
    url: '/system/page/' + pageId,
    method: 'delete'
  })
}
