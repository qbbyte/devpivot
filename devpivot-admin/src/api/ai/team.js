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

// 轻量拉取团队消息(供讨论区轮询刷新)
export function getTeamMessages(teamId) {
  return request({
    url: '/team/' + teamId + '/messages',
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
