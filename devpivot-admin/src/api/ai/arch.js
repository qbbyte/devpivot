import request from '@/utils/request'
import { getToken } from '@/utils/auth'

/* =========================================================================
 * 系统架构设计 · 门户接口层
 * 数据读写：/portal/arch（doc / save / submit）
 * AI 生成 / 对话：/ai/arch（models / generate / chat）
 * 架构文档为含 Mermaid 图的 Markdown，生成后落库 ai_arch_doc
 * ========================================================================= */

// 可用模型列表（单模型模式）
export function getArchModels() {
  return request({ url: '/ai/arch/models', method: 'get' })
}

// 按项目读取当前系统架构设计（返 AiArchDoc 或 null）
export function getArchDoc(projectId) {
  return request({ url: '/portal/arch/doc', method: 'post', data: { projectId } })
}

// 按项目 upsert 系统架构设计，返回主键 docId
export function saveArchDoc(data) {
  return request({ url: '/portal/arch/save', method: 'post', data })
}

// 提交系统架构设计：落库 status=1 并推进项目阶段到 TECH
export function submitArch(projectId, data) {
  return request({ url: '/portal/arch/submit/' + projectId, method: 'post', data })
}

/**
 * 生成系统架构设计（流式 SSE，单模型）
 * @param {Object} params { projectId, projectName, model, extraReq }
 * @param {Object} handlers { onChunk(fullText), onDone(), onError(err) }
 * @returns {{ stop: Function }}
 */
export function generateArch(params, handlers = {}) {
  const base = import.meta.env.VITE_APP_BASE_API || ''
  const ctrl = new AbortController()
  let full = ''
  fetch(base + '/ai/arch/generate', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': 'Bearer ' + (getToken() || '')
    },
    body: JSON.stringify({
      projectId: params.projectId,
      projectName: params.projectName || '',
      model: params.model || '',
      extraReq: params.extraReq || ''
    }),
    signal: ctrl.signal
  }).then(resp => {
    if (!resp.ok || !resp.body) {
      handlers.onError && handlers.onError(new Error('HTTP ' + resp.status))
      return
    }
    const reader = resp.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''
    const pump = () => reader.read().then(({ done, value }) => {
      if (done) { handlers.onDone && handlers.onDone(); return }
      buffer += decoder.decode(value, { stream: true })
      let idx
      while ((idx = buffer.indexOf('\n\n')) >= 0) {
        const raw = buffer.slice(0, idx)
        buffer = buffer.slice(idx + 2)
        const ev = parseArchSse(raw)
        if (!ev || !ev.data) continue
        if (ev.data.type === 'token') {
          full += (ev.data.delta || '')
          handlers.onChunk && handlers.onChunk(full)
        } else if (ev.data.type === 'error') {
          handlers.onError && handlers.onError(new Error(ev.data.content || '生成失败'))
        }
      }
      pump()
    })
    pump()
  }).catch(err => {
    if (err.name !== 'AbortError') handlers.onError && handlers.onError(err)
  })
  return { stop() { ctrl.abort() } }
}

/**
 * 架构设计 AI 对话（流式 SSE）
 * @param {Object} params { message, model }
 * @param {Object} handlers { onChunk(fullText), onDone(), onError(err) }
 * @returns {{ stop: Function }}
 */
export function sendArchChat(params, handlers = {}) {
  const base = import.meta.env.VITE_APP_BASE_API || ''
  const ctrl = new AbortController()
  let full = ''
  fetch(base + '/ai/arch/chat', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': 'Bearer ' + (getToken() || '')
    },
    body: JSON.stringify({
      message: params.message || '',
      model: params.model || ''
    }),
    signal: ctrl.signal
  }).then(resp => {
    if (!resp.ok || !resp.body) {
      handlers.onError && handlers.onError(new Error('HTTP ' + resp.status))
      return
    }
    const reader = resp.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''
    const pump = () => reader.read().then(({ done, value }) => {
      if (done) { handlers.onDone && handlers.onDone(); return }
      buffer += decoder.decode(value, { stream: true })
      let idx
      while ((idx = buffer.indexOf('\n\n')) >= 0) {
        const raw = buffer.slice(0, idx)
        buffer = buffer.slice(idx + 2)
        const ev = parseArchSse(raw)
        if (!ev || !ev.data) continue
        if (ev.data.type === 'token') {
          full += (ev.data.delta || '')
          handlers.onChunk && handlers.onChunk(full)
        } else if (ev.data.type === 'done-all') {
          handlers.onDone && handlers.onDone()
        }
      }
      pump()
    })
    pump()
  }).catch(err => {
    if (err.name !== 'AbortError') handlers.onError && handlers.onError(err)
  })
  return { stop() { ctrl.abort() } }
}

// 解析单个 SSE 事件块（event:/data: 行），返回 { event, data }
function parseArchSse(raw) {
  let eventName = ''
  let dataStr = ''
  raw.split('\n').forEach(line => {
    if (line.startsWith('event:')) eventName = line.slice(6).trim()
    else if (line.startsWith('data:')) dataStr += line.slice(5).trim()
  })
  if (!dataStr) return null
  try {
    return { event: eventName, data: JSON.parse(dataStr) }
  } catch (e) {
    return null
  }
}
