import request from '@/utils/request'

// 我的团队列表(摘要)
export function listMyTeams() {
  return request({
    url: '/team/mine',
    method: 'get'
  })
}

// 团队详情(成员/项目/消息/已读)
export function getTeamDetail(teamId) {
  return request({
    url: '/team/' + teamId,
    method: 'get'
  })
}

// 创建团队
export function createTeam(data) {
  return request({
    url: '/team',
    method: 'post',
    data: data
  })
}

// 编辑团队
export function updateTeam(data) {
  return request({
    url: '/team',
    method: 'put',
    data: data
  })
}

// 解散团队
export function dissolveTeam(teamId) {
  return request({
    url: '/team/' + teamId,
    method: 'delete'
  })
}

// 添加成员
export function addTeamMember(teamId, userId, role) {
  return request({
    url: '/team/' + teamId + '/member',
    method: 'post',
    params: { userId, role }
  })
}

// 移除成员
export function removeTeamMember(teamId, userId) {
  return request({
    url: '/team/' + teamId + '/member/' + userId,
    method: 'delete'
  })
}

// 修改成员角色
export function changeTeamMemberRole(teamId, userId, role) {
  return request({
    url: '/team/' + teamId + '/member/' + userId + '/role',
    method: 'put',
    params: { role }
  })
}

// 关联项目
export function bindTeamProject(teamId, projectId) {
  return request({
    url: '/team/' + teamId + '/project',
    method: 'post',
    params: { projectId }
  })
}

// 解绑项目
export function unbindTeamProject(teamId, projectId) {
  return request({
    url: '/team/' + teamId + '/project/' + projectId,
    method: 'delete'
  })
}

// 发送讨论消息
export function sendTeamMessage(teamId, content) {
  return request({
    url: '/team/' + teamId + '/message',
    method: 'post',
    params: { content }
  })
}

// 标记已读(传空数组=标记全部未读)
export function markTeamRead(teamId, msgIds) {
  return request({
    url: '/team/' + teamId + '/message/read',
    method: 'post',
    data: msgIds || []
  })
}

// 检索平台用户目录
export function searchTeamUsers(keyword) {
  return request({
    url: '/team/user-search',
    method: 'get',
    params: { keyword }
  })
}

// 项目下拉选项(供关联项目选择器)
export function listProjectOptions() {
  return request({
    url: '/team/project-options',
    method: 'get'
  })
}

// 退出团队
export function leaveTeam(teamId) {
  return request({
    url: '/team/' + teamId + '/leave',
    method: 'delete'
  })
}

// 凭邀请码加入团队(门户侧,仅登录态)
export function joinTeamByCode(code) {
  return request({
    url: '/team/join',
    method: 'post',
    params: { code }
  })
}

// 重新生成团队邀请码(仅 OWNER/ADMIN)
export function refreshInviteCode(teamId) {
  return request({
    url: '/team/' + teamId + '/invite-code/refresh',
    method: 'post'
  })
}

// 团队成员分页列表(若依 TableDataInfo: { rows, total })
export function listTeamMembers(teamId, params) {
  return request({
    url: '/team/' + teamId + '/members',
    method: 'get',
    params: params
  })
}

// 团队关联项目分页列表
export function listTeamProjects(teamId, params) {
  return request({
    url: '/team/' + teamId + '/projects',
    method: 'get',
    params: params
  })
}

// 项目阶段概览(含每阶段状态与实现人)，弹窗展示用
export function getProjectPhases(projectId) {
  return request({
    url: '/portal/project/' + projectId + '/phases',
    method: 'get'
  })
}

// 项目产物概览(聚合各阶段产物文本)，「产物」按钮弹窗展示/下载用
export function getProjectArtifacts(projectId) {
  return request({
    url: '/portal/project/' + projectId + '/artifacts',
    method: 'get'
  })
}
