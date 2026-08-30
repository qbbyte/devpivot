import request from '@/utils/request'

// 查询产物版本列表
export function listArtifact(query) {
  return request({
    url: '/system/artifact/list',
    method: 'get',
    params: query
  })
}

// 查询产物版本详细
export function getArtifact(versionId) {
  return request({
    url: '/system/artifact/' + versionId,
    method: 'get'
  })
}

// 发布产物版本
export function releaseArtifact(versionId) {
  return request({
    url: '/system/artifact/' + versionId + '/release',
    method: 'put'
  })
}

// 恢复产物版本
export function restoreArtifact(versionId) {
  return request({
    url: '/system/artifact/' + versionId + '/restore',
    method: 'put'
  })
}

// 删除产物版本
export function delArtifact(versionId) {
  return request({
    url: '/system/artifact/' + versionId,
    method: 'delete'
  })
}
