import request from '@/utils/request'

// 查询数据库设计文档列表
export function listDbdoc(query) {
  return request({
    url: '/system/dbdoc/list',
    method: 'get',
    params: query
  })
}

// 查询数据库设计文档详细
export function getDbdoc(docId) {
  return request({
    url: '/system/dbdoc/' + docId,
    method: 'get'
  })
}

// 新增数据库设计文档
export function addDbdoc(data) {
  return request({
    url: '/system/dbdoc',
    method: 'post',
    data: data
  })
}

// 修改数据库设计文档
export function updateDbdoc(data) {
  return request({
    url: '/system/dbdoc',
    method: 'put',
    data: data
  })
}

// 删除数据库设计文档
export function delDbdoc(docId) {
  return request({
    url: '/system/dbdoc/' + docId,
    method: 'delete'
  })
}
