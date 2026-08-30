import request from '@/utils/request'

// 查询系统架构设计文档列表
export function listArchdoc(query) {
  return request({
    url: '/system/archdoc/list',
    method: 'get',
    params: query
  })
}

// 查询系统架构设计文档详细
export function getArchdoc(docId) {
  return request({
    url: '/system/archdoc/' + docId,
    method: 'get'
  })
}

// 新增系统架构设计文档
export function addArchdoc(data) {
  return request({
    url: '/system/archdoc',
    method: 'post',
    data: data
  })
}

// 修改系统架构设计文档
export function updateArchdoc(data) {
  return request({
    url: '/system/archdoc',
    method: 'put',
    data: data
  })
}

// 删除系统架构设计文档
export function delArchdoc(docId) {
  return request({
    url: '/system/archdoc/' + docId,
    method: 'delete'
  })
}
