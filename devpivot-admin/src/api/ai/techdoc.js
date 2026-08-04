import request from '@/utils/request'

// 查询技术方案文档列表
export function listTechdoc(query) {
  return request({
    url: '/system/techdoc/list',
    method: 'get',
    params: query
  })
}

// 查询技术方案文档详细
export function getTechdoc(docId) {
  return request({
    url: '/system/techdoc/' + docId,
    method: 'get'
  })
}

// 新增技术方案文档
export function addTechdoc(data) {
  return request({
    url: '/system/techdoc',
    method: 'post',
    data: data
  })
}

// 修改技术方案文档
export function updateTechdoc(data) {
  return request({
    url: '/system/techdoc',
    method: 'put',
    data: data
  })
}

// 删除技术方案文档
export function delTechdoc(docId) {
  return request({
    url: '/system/techdoc/' + docId,
    method: 'delete'
  })
}
