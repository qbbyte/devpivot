import request from '@/utils/request'
import { getToken } from '@/utils/auth'

/* ===================== 后台管理·技术方案 CRUD（/system/techdoc，已有，备用） ===================== */
export function listTechdoc(query) {
  return request({ url: '/system/techdoc/list', method: 'get', params: query })
}
export function getTechdoc(docId) {
  return request({ url: '/system/techdoc/' + docId, method: 'get' })
}
export function addTechdoc(data) {
  return request({ url: '/system/techdoc', method: 'post', data })
}
export function updateTechdoc(data) {
  return request({ url: '/system/techdoc', method: 'put', data })
}
export function delTechdoc(docId) {
  return request({ url: '/system/techdoc/' + docId, method: 'delete' })
}

/* ===================== 门户·技术方案读写（/system/tech 数据 + /ai/tech 生成，仅校验登录态） ===================== */

// 获取可用模型列表与最大对比数（来自后端 ai_model_config 启用项）
export function getTechModels() {
  return request({ url: '/ai/tech/models', method: 'get' })
}

// 按项目读取当前技术方案（返 AiTechDoc 或 null），已对接后端 /portal/tech/doc
export function getTechDoc(projectId) {
  return request({ url: '/portal/tech/doc', method: 'post', data: { projectId } })
}

// 按项目 upsert 技术方案（编辑保存 / 生成后落库），返回主键 docId，已对接后端 /portal/tech/save
export function saveTechDoc(data) {
  return request({ url: '/portal/tech/save', method: 'post', data })
}

// 提交技术方案：落库 status=1 并推进项目阶段到 DB（后端统一处理），已对接 /portal/tech/submit/{projectId}
export function submitTech(projectId, data) {
  return request({ url: '/portal/tech/submit/' + projectId, method: 'post', data })
}

/**
 * 生成技术方案（多模型并行，流式输出）
 * @param {Object} params
 *   - projectId    项目ID
 *   - projectName  项目名称
 *   - industryType 行业
 *   - targetUser   目标用户
 *   - techStack    'JAVA' | 'PYTHON'
 *   - models       模型ID数组（单/多模型）
 *   - extraReq     补充要求（可选）
 *   - upstream     上游产物摘要（PRD/原型/基线，可选，作为生成上下文）
 * @param {Object} handlers
 *   - onModelChunk(modelId, fullText)  某模型流式增量（fullText 为该模型累计全文）
 *   - onModelDone(modelId)             某模型完成
 *   - onAllDone()                      全部模型完成
 *   - onError(err)                     整体出错
 * @returns {{ stop: Function }} 调用 stop() 可中断生成
 *
 * 说明：调用后端流式生成接口 /ai/tech/generate，原生 fetch 解析 SSE 多路复用协议
 *   （事件 data 含 { type:'token'|'done'|'error', modelId, delta/content }）。
 */
export function generateTech(params, handlers = {}) {
  const body = {
    projectId: params.projectId,
    projectName: params.projectName,
    industryType: params.industryType,
    targetUser: params.targetUser,
    techStack: params.techStack,
    models: params.models,
    extraReq: params.extraReq || '',
    upstream: params.upstream || ''
  }
  return streamGenerateTech('/ai/tech/generate', body, handlers)
}

/* 原生 fetch 解析 SSE（text/event-stream），按 modelId 多路复用逐 token 累计后回调；
   网络/HTTP 异常时通过 handlers.onError 上报错误。返回 { stop } 兼容旧调用方。 */
function streamGenerateTech(url, body, handlers = {}) {
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
    console.error('[tech] /ai/tech/generate 请求失败：', err.message)
    handlers.onError && handlers.onError(err)
  })
  return ctrl
}
