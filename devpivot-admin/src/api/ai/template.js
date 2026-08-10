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

// 刷新提示词渲染缓存（编辑/置默认后立即生效）
export function clearTemplateCache() {
  return request({
    url: '/system/template/clearCache',
    method: 'post'
  })
}

// 试跑提示词：用渲染后的 system/user 直接调模型，返回输出
export function tryRunTemplate(data) {
  return request({
    url: '/system/template/tryRun',
    method: 'post',
    data: data
  })
}

// 克隆模板为新版本（复制为「非默认+停用」副本）
export function cloneTemplate(data) {
  return request({
    url: '/system/template/clone',
    method: 'post',
    data: data
  })
}

// 互斥设为默认（版本回滚）
export function setDefaultTemplate(data) {
  return request({
    url: '/system/template/setDefault',
    method: 'post',
    data: data
  })
}
