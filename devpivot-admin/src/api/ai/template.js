import request from '@/utils/request'

// 查询Prompt模板列表
export function listTemplate(query) {
  return request({
    url: '/system/template/list',
    method: 'get',
    params: query
  })
}

// 查询Prompt模板详细
export function getTemplate(templateId) {
  return request({
    url: '/system/template/' + templateId,
    method: 'get'
  })
}

// 新增Prompt模板
export function addTemplate(data) {
  return request({
    url: '/system/template',
    method: 'post',
    data: data
  })
}

// 修改Prompt模板
export function updateTemplate(data) {
  return request({
    url: '/system/template',
    method: 'put',
    data: data
  })
}

// 删除Prompt模板
export function delTemplate(templateId) {
  return request({
    url: '/system/template/' + templateId,
    method: 'delete'
  })
}
