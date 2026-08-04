import request from '@/utils/request'

// 查询数据库字段定义列表
export function listColumn(query) {
  return request({
    url: '/system/column/list',
    method: 'get',
    params: query
  })
}

// 查询数据库字段定义详细
export function getColumn(columnId) {
  return request({
    url: '/system/column/' + columnId,
    method: 'get'
  })
}

// 新增数据库字段定义
export function addColumn(data) {
  return request({
    url: '/system/column',
    method: 'post',
    data: data
  })
}

// 修改数据库字段定义
export function updateColumn(data) {
  return request({
    url: '/system/column',
    method: 'put',
    data: data
  })
}

// 删除数据库字段定义
export function delColumn(columnId) {
  return request({
    url: '/system/column/' + columnId,
    method: 'delete'
  })
}
