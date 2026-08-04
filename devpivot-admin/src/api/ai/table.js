import request from '@/utils/request'

// 查询数据库结构列表
export function listTable(query) {
  return request({
    url: '/system/table/list',
    method: 'get',
    params: query
  })
}

// 查询数据库结构详细
export function getTable(tableId) {
  return request({
    url: '/system/table/' + tableId,
    method: 'get'
  })
}

// 新增数据库结构
export function addTable(data) {
  return request({
    url: '/system/table',
    method: 'post',
    data: data
  })
}

// 修改数据库结构
export function updateTable(data) {
  return request({
    url: '/system/table',
    method: 'put',
    data: data
  })
}

// 删除数据库结构
export function delTable(tableId) {
  return request({
    url: '/system/table/' + tableId,
    method: 'delete'
  })
}
