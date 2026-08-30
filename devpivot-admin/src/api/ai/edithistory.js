import request from '@/utils/request'

// 查询编辑历史列表
export function listEdithistory(query) {
  return request({
    url: '/system/edithistory/list',
    method: 'get',
    params: query
  })
}
