import request from '@/utils/request'

// 创建并同步执行多模型并行任务（返回含各模型结果的任务对象）
export function runParallelTask(data) {
  return request({ url: '/ai/parallel/run', method: 'post', data })
}

// 查询并行任务详情（resultSummary=各模型结果 JSON Map）
export function getParallelTask(taskId) {
  return request({ url: '/portal/parallel/' + taskId, method: 'get' })
}
