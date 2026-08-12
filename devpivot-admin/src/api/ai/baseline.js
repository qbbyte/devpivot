import request from '@/utils/request'

// 查询需求基线列表
export function listBaseline(query) {
  return request({
    url: '/system/baseline/list',
    method: 'get',
    params: query
  })
}

// 查询需求基线详细
export function getBaseline(baselineId) {
  return request({
    url: '/system/baseline/' + baselineId,
    method: 'get'
  })
}

// 新增需求基线
export function addBaseline(data) {
  return request({
    url: '/system/baseline',
    method: 'post',
    data: data
  })
}

// 门户：按项目ID获取需求基线（无需后台权限，仅登录态）
export function getBaselineByProject(projectId) {
  return request({
    url: '/system/baseline/byProject/' + projectId,
    method: 'get'
  })
}

// 门户：保存（新增或更新）需求基线，按 projectId upsert（无需后台权限，仅登录态）
export function saveBaseline(data) {
  return request({
    url: '/system/baseline/save',
    method: 'post',
    data: data
  })
}

// 修改需求基线
export function updateBaseline(data) {
  return request({
    url: '/system/baseline',
    method: 'put',
    data: data
  })
}

// 删除需求基线
export function delBaseline(baselineId) {
  return request({
    url: '/system/baseline/' + baselineId,
    method: 'delete'
  })
}
