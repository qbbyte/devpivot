import request from '@/utils/request'

// 系统全局已启用的模型列表（脱敏：仅 maskedApiKey，apiKey 不回传）
export function getGlobalModels() {
  return request({ url: '/portal/model/global', method: 'get' })
}
