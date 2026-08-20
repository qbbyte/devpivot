import request from '@/utils/request'

// ==================== 多仓库：一个项目可关联多个 Git 仓库（以 repoId 定位） ====================

// 新增仓库配置(仅 OWNER/ADMIN;令牌加密存储, 空令牌表示不修改)
export function addTeamProjectRepo(teamId, projectId, data) {
  return request({
    url: '/team/git/' + teamId + '/project/' + projectId + '/repo',
    method: 'post',
    data: data
  })
}

// 更新仓库配置(仅 OWNER/ADMIN;令牌留空表示不修改)
export function updateTeamProjectRepo(teamId, repoId, data) {
  return request({
    url: '/team/git/' + teamId + '/repo/' + repoId,
    method: 'put',
    data: data
  })
}

// 删除仓库配置(仅 OWNER/ADMIN)
export function deleteTeamProjectRepo(teamId, repoId) {
  return request({
    url: '/team/git/' + teamId + '/repo/' + repoId,
    method: 'delete'
  })
}

// 项目下的仓库列表(不含令牌)
export function listTeamProjectRepos(teamId, projectId) {
  return request({
    url: '/team/git/' + teamId + '/project/' + projectId + '/repos',
    method: 'get'
  })
}

// 单个仓库配置(令牌脱敏)
export function getTeamProjectRepo(teamId, repoId) {
  return request({
    url: '/team/git/' + teamId + '/repo/' + repoId,
    method: 'get'
  })
}

// 贡献者统计(每人提交数)
export function getTeamProjectContributors(teamId, repoId) {
  return request({
    url: '/team/git/' + teamId + '/repo/' + repoId + '/contributors',
    method: 'get'
  })
}

// 提交历史(分页)
export function getTeamProjectCommits(teamId, repoId, params) {
  return request({
    url: '/team/git/' + teamId + '/repo/' + repoId + '/commits',
    method: 'get',
    params: params
  })
}

// 分支列表 + 默认分支(用于下拉切换)
export function getTeamProjectBranches(teamId, repoId) {
  return request({
    url: '/team/git/' + teamId + '/repo/' + repoId + '/branches',
    method: 'get'
  })
}

// 提交热力图(过去 365 天按日聚合)
export function getTeamProjectHeatmap(teamId, repoId, params) {
  return request({
    url: '/team/git/' + teamId + '/repo/' + repoId + '/heatmap',
    method: 'get',
    params: params
  })
}
