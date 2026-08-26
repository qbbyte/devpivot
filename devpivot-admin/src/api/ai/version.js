import request from '@/utils/request'

/* =========================================================================
 * 结果物版本 · 门户接口层（/portal/version，纯数据，无 mock）
 * 各阶段（CLARIFY/REQ/PRD/PROTO/TECH/DB）结果物多版本管理：
 * 保存新版本 / 发布 / 还原 / 删除 / 对比 / 列表 / 详情
 * 鉴权：service 层（Reader 查看 · Writer 保存还原发布 · Manager 删除）
 * ========================================================================= */

/** 版本列表（不含快照） */
export function listVersions(params) {
  return request({ url: '/portal/version/list', method: 'get', params }).then(res => res.rows || [])
}

/** 版本详情（含快照内容） */
export function getVersion(versionId) {
  return request({ url: `/portal/version/detail/${versionId}`, method: 'get' }).then(res => res.data || {})
}

/** 两版本结构化 diff */
export function diffVersions(fromId, toId) {
  return request({ url: '/portal/version/diff', method: 'get', params: { fromId, toId } }).then(res => res.data || {})
}

/**
 * 保存新版本
 * @param {Object} data { projectId, stage, artifactType, versionName, snapshot, sourceType, sourceModel, changeRemark }
 */
export function saveVersion(data) {
  return request({ url: `/portal/version/save/${data.projectId}`, method: 'post', data }).then(res => res.data || {})
}

/** 发布版本（DRAFT -> RELEASED） */
export function releaseVersion(versionId) {
  return request({ url: `/portal/version/release/${versionId}`, method: 'post' }).then(res => res.data || {})
}

/** 还原版本（快照写回业务表并生成新版本） */
export function restoreVersion(versionId) {
  return request({ url: `/portal/version/restore/${versionId}`, method: 'post' }).then(res => res.data || {})
}

/** 删除版本（仅 Manager） */
export function deleteVersion(versionId) {
  return request({ url: `/portal/version/${versionId}`, method: 'delete' })
}
