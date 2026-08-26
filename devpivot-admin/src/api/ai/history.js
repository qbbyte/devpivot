import request from '@/utils/request'

/* =========================================================================
 * 成员修改记录 · 门户接口层（/portal/history）
 * 腾讯文档式操作轨迹：时间线查询 / 最近记录（头像组入口）/ 成员贡献聚合
 * ========================================================================= */

/** 修改记录时间线（分页，按时间倒序） */
export function listHistory(params) {
  return request({ url: '/portal/history/list', method: 'get', params }).then(res => res.rows || [])
}

/** 最近 N 条记录（历史入口头像组数据源，后端默认取 3 条） */
export function recentHistory(projectId) {
  return request({ url: '/portal/history/recent', method: 'get', params: { projectId } }).then(res => res.data || [])
}

/** 成员贡献聚合（按人：操作次数/版本操作/最近活跃） */
export function aggregateHistory(projectId, days) {
  return request({ url: '/portal/history/aggregate', method: 'get', params: { projectId, days } }).then(res => res.data || [])
}
