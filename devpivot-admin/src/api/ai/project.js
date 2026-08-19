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
