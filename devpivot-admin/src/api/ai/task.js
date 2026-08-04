import request from '@/utils/request'

// 查询多模型并行任务列表
export function listTask(query) {
  return request({
    url: '/system/task/list',
    method: 'get',
    params: query
  })
}

// 查询多模型并行任务详细
export function getTask(taskId) {
  return request({
    url: '/system/task/' + taskId,
    method: 'get'
  })
}

// 新增多模型并行任务
export function addTask(data) {
  return request({
    url: '/system/task',
    method: 'post',
    data: data
  })
}

// 修改多模型并行任务
export function updateTask(data) {
  return request({
    url: '/system/task',
    method: 'put',
    data: data
  })
}

// 删除多模型并行任务
export function delTask(taskId) {
  return request({
    url: '/system/task/' + taskId,
    method: 'delete'
  })
}
