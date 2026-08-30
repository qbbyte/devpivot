import request from '@/utils/request'

// 查询全量团队列表
export function listTeam(query) {
  return request({
    url: '/system/team/list',
    method: 'get',
    params: query
  })
}

// 查询团队成员列表
export function listTeamMembers(teamId) {
  return request({
    url: '/system/team/' + teamId + '/members',
    method: 'get'
  })
}

// 查询团队关联项目列表
export function listTeamProjects(teamId) {
  return request({
    url: '/system/team/' + teamId + '/projects',
    method: 'get'
  })
}

// 解散团队
export function dissolveTeam(teamId) {
  return request({
    url: '/system/team/' + teamId,
    method: 'delete'
  })
}
