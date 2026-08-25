import request from '@/utils/request'

// 知识库文档列表（按项目 + 可选阶段过滤）
export function listKbDocs(params) {
  return request({
    url: '/portal/kb/list',
    method: 'get',
    params
  })
}

// 门户：上传文档到自己的项目库（仅登录态 + 项目写权限，后端校验归属；不支持共享库）
export function uploadPortalKbDoc(data) {
  return request({
    url: '/portal/kb/upload',
    method: 'post',
    data
  })
}

// 上传并索引一篇知识库文档（后台管理，需 admin 角色；支持共享库）
export function uploadKbDoc(data) {
  return request({
    url: '/system/kb/upload',
    method: 'post',
    data
  })
}

// 门户：删除自己项目库的文档（仅登录态 + 项目写权限，后端校验归属；共享库文档不可删）
export function deletePortalKbDoc(docId) {
  return request({
    url: '/portal/kb/' + docId,
    method: 'delete'
  })
}

// 删除知识库文档（后台管理，需 admin 角色；可删共享库）
export function deleteKbDoc(docId) {
  return request({
    url: '/system/kb/' + docId,
    method: 'delete'
  })
}

// 检索预览/调试（返回 context 字符串）
export function previewKbRetrieve(params) {
  return request({
    url: '/portal/kb/retrieve',
    method: 'get',
    params
  })
}

// 检索日志查询（admin；按时间倒序，可选 projectId/stage 过滤）
export function listKbLogs(params) {
  return request({
    url: '/system/kb/logs',
    method: 'get',
    params
  })
}

// 清理检索日志（admin；保留天数由 kb.retrieval-log.keep-days 控制）
export function clearKbLogs() {
  return request({
    url: '/system/kb/logs',
    method: 'delete'
  })
}
