import request from '@/utils/request'
import { getToken } from '@/utils/auth'

/* ===================== 门户·数据库设计读写（/ai/db，仅校验登录态） ===================== */

// 获取可用模型列表与最大对比数（来自后端 ai_model_config 启用项）
export function getDbModels() {
  return request({ url: '/ai/db/models', method: 'get' })
}

// 按项目读取当前数据库设计（返 AiDbDoc 或 null），已对接后端 /ai/db/doc
export function getDbDoc(projectId) {
  return request({ url: '/ai/db/doc', method: 'post', data: { projectId } })
}

// 按项目 upsert 数据库设计（编辑保存 / 生成后落库），返回主键 docId，已对接后端 /ai/db/save
export function saveDbDoc(data) {
  return request({ url: '/ai/db/save', method: 'post', data })
}

// 提交数据库设计：落库 status=1 并推进项目阶段到 DONE（后端统一处理），已对接 /ai/db/submit/{projectId}
export function submitDb(projectId, data) {
  return request({ url: '/ai/db/submit/' + projectId, method: 'post', data })
}

/**
 * 生成数据库设计（单模型流式输出）
 * @param {Object} params
 *   - projectId    项目ID
 *   - projectName  项目名称
 *   - industryType 行业
 *   - targetUser   目标用户
 *   - dbType       数据库类型（MySQL/PostgreSQL/...）
 *   - models       模型ID数组（单模型）
 *   - extraReq     补充要求（可选）
 * @param {Object} handlers
 *   - onModelChunk(modelId, fullText)
 *   - onModelDone(modelId)
 *   - onAllDone()
 *   - onError(err)
 * @returns {{ stop: Function }}
 */
export function generateDb(params, handlers = {}) {
  const body = {
    projectId: params.projectId,
    projectName: params.projectName,
    industryType: params.industryType,
    targetUser: params.targetUser,
    dbType: params.dbType,
    models: params.models,
    extraReq: params.extraReq || ''
  }
  return streamGenerateDb('/ai/db/generate', body, handlers)
}

/* 原生 fetch 解析 SSE（text/event-stream），按 modelId 多路复用逐 token 累计后回调；
   网络/HTTP 异常时通过 handlers.onError 上报错误。 */
function streamGenerateDb(url, body, handlers = {}) {
  const base = import.meta.env.VITE_APP_BASE_API || ''
  const ctrl = { stopped: false, stop() { this.stopped = true } }
  const fullByModel = {}
  fetch(base + url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + (getToken() || '') },
    body: JSON.stringify(body)
  }).then(resp => {
    const ct = resp.headers ? (resp.headers.get('content-type') || '') : ''
    if (!resp.ok || !resp.body || !ct.includes('text/event-stream')) {
      throw new Error('非 SSE 响应：HTTP ' + resp.status + ' ' + ct)
    }
    const reader = resp.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''
    const pump = () => reader.read().then(({ done, value }) => {
      if (done) {
        if (buffer.trim()) handleSse(buffer)
        handlers.onAllDone && handlers.onAllDone()
        return
      }
      if (ctrl.stopped) { try { reader.cancel() } catch (e) {} handlers.onAllDone && handlers.onAllDone(); return }
      buffer += decoder.decode(value, { stream: true })
      let idx
      while ((idx = buffer.indexOf('\n\n')) >= 0) {
        const raw = buffer.slice(0, idx)
        buffer = buffer.slice(idx + 2)
        handleSse(raw)
      }
      pump()
    })
    const handleSse = (raw) => {
      let type = ''
      let modelId = ''
      let dataStr = ''
      raw.split('\n').forEach(line => {
        if (line.startsWith('event:')) type = line.slice(6).trim()
        else if (line.startsWith('model:')) modelId = line.slice(6).trim()
        else if (line.startsWith('data:')) dataStr += line.slice(5).trim()
      })
      if (!dataStr) return
      let data
      try { data = JSON.parse(dataStr) } catch (e) { return }
      const mid = data.modelId || modelId || ''
      if (data.type === 'token' && data.delta) {
        fullByModel[mid] = (fullByModel[mid] || '') + data.delta
        handlers.onModelChunk && handlers.onModelChunk(mid, fullByModel[mid])
      } else if (data.type === 'done') {
        handlers.onModelDone && handlers.onModelDone(mid)
      } else if (data.type === 'error') {
        fullByModel[mid] = (fullByModel[mid] || '') + (data.content || '生成失败')
        handlers.onModelChunk && handlers.onModelChunk(mid, fullByModel[mid])
        handlers.onModelDone && handlers.onModelDone(mid)
      }
    }
    pump()
  }).catch(err => {
    console.error('[db] /ai/db/generate 请求失败：', err.message)
    handlers.onError && handlers.onError(err)
  })
  return ctrl
}
