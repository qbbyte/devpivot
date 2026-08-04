import request from '@/utils/request'

// 查询PRD需求文档列表
export function listDoc(query) {
  return request({
    url: '/system/doc/list',
    method: 'get',
    params: query
  })
}

// 查询PRD需求文档详细
export function getDoc(docId) {
  return request({
    url: '/system/doc/' + docId,
    method: 'get'
  })
}

// 新增PRD需求文档
export function addDoc(data) {
  return request({
    url: '/system/doc',
    method: 'post',
    data: data
  })
}

// 修改PRD需求文档
export function updateDoc(data) {
  return request({
    url: '/system/doc',
    method: 'put',
    data: data
  })
}

// 删除PRD需求文档
export function delDoc(docId) {
  return request({
    url: '/system/doc/' + docId,
    method: 'delete'
  })
}
