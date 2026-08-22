import request from '@/utils/request'

// 查询AI项目列表
export function listProject(query) {
  return request({
    url: '/system/project/list',
    method: 'get',
    params: query
  })
}

// 查询「我的项目」列表（我创建的 ∪ 我参与团队关联的项目，仅登录态）
export function listMyProject(query) {
  return request({
    url: '/system/project/my',
    method: 'get',
    params: query
  })
}

// 查询AI项目详细
export function getProject(projectId) {
  return request({
    url: '/system/project/' + projectId,
    method: 'get'
  })
}

// 查询AI项目产物概览（聚合各阶段产物文本，用于导出开发上下文）
export function getProjectContext(projectId) {
  return request({
    url: '/system/project/' + projectId + '/artifacts',
    method: 'get'
  })
}

// 生成项目上下文导出 token（24h 只读），用于服务器终端 curl 拉取约定文件
export function createExportToken(projectId) {
  return request({
    url: '/system/project/' + projectId + '/export-token',
    method: 'post'
  })
}

// 新增AI项目
export function addProject(data) {
  return request({
    url: '/system/project',
    method: 'post',
    data: data
  })
}

// 修改AI项目
export function updateProject(data) {
  return request({
    url: '/system/project',
    method: 'put',
    data: data
  })
}

// 删除AI项目
export function delProject(projectId) {
  return request({
    url: '/system/project/' + projectId,
    method: 'delete'
  })
}
