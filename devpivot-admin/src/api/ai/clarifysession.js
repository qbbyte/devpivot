import request from '@/utils/request'

// 查询澄清会话列表
export function listClarifysession(query) {
  return request({
    url: '/system/clarifysession/list',
    method: 'get',
    params: query
  })
}

// 查询澄清会话详细
export function getClarifysession(sessionId) {
  return request({
    url: '/system/clarifysession/' + sessionId,
    method: 'get'
  })
}
