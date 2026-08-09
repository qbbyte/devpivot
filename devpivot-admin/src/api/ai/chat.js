import request from '@/utils/request'
import { getToken } from '@/utils/auth'

// 查询对话历史（后台管理，预留）
export function listChat(query) {
  return request({
    url: '/system/chat/list',
    method: 'get',
    params: query
  })
}

// 新增对话记录（后台管理，预留）
export function addChat(data) {
  return request({
    url: '/system/chat',
    method: 'post',
    data: data
  })
}

/**
 * 发送对话消息（流式输出）
 * @param {Object} params
 *   - projectId    项目ID
 *   - projectName  项目名称
 *   - question     用户本轮提问
 *   - docContent   当前 PRD 文档内容（作为上下文，后端可据此定向修订）
 * @param {Object} handlers { onChunk(text), onDone(), onError(err) }
 * @returns {{ stop: Function }} 调用 stop() 可中断生成
 *
 * 说明：调用后端流式对话接口 /ai/chat/send，原生 fetch 解析 SSE。
 */
export function sendChatMessage(params, handlers = {}) {
  const body = {
    projectId: params.projectId,
    projectName: params.projectName,
    question: params.question,
    docContent: params.docContent,
    quotes: params.quotes || [],
    model: params.model
  }
  return streamGenerate('/ai/chat/send', body, handlers)
}

/* 原生 fetch 解析 SSE（text/event-stream），逐 token 累计后回调；
   网络/HTTP 异常时通过 handlers.onError 上报错误。返回 { stop } 兼容旧调用方。 */
function streamGenerate(url, body, handlers = {}) {
  const base = import.meta.env.VITE_APP_BASE_API || ''
  const ctrl = { stopped: false, stop() { this.stopped = true } }
  fetch(base + url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + (getToken() || '') },
    body: JSON.stringify(body)
  }).then(resp => {
    if (!resp.ok || !resp.body) throw new Error('HTTP ' + resp.status)
    const reader = resp.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''
    let full = ''
    const pump = () => reader.read().then(({ done, value }) => {
      if (done) {
        if (buffer.trim()) handleSse(buffer)
        handlers.onDone && handlers.onDone()
        return
      }
      if (ctrl.stopped) { try { reader.cancel() } catch (e) {} handlers.onDone && handlers.onDone(); return }
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
      let dataStr = ''
      raw.split('\n').forEach(line => {
        if (line.startsWith('event:')) type = line.slice(6).trim()
        else if (line.startsWith('data:')) dataStr += line.slice(5).trim()
      })
      if (!dataStr) return
      let data
      try { data = JSON.parse(dataStr) } catch (e) { return }
      if (data.type === 'token' && data.delta) {
        full += data.delta
        handlers.onChunk && handlers.onChunk(full)
      } else if (data.type === 'error') {
        full += (full ? '\n' : '') + (data.content || '对话失败')
        handlers.onChunk && handlers.onChunk(full)
      }
    }
    pump()
  }).catch(err => {
    console.error('[chat] /ai/chat/send 请求失败：', err.message)
    handlers.onError && handlers.onError(err)
  })
  return ctrl
}
