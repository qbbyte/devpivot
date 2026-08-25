import request from '@/utils/request'
import { getToken } from '@/utils/auth'

// 查询PRD需求文档列表
export function listDoc(query) {
  return request({
    url: '/system/doc/list',
    method: 'get',
    params: query
  })
}

// 查询PRD需求文档详细
export function getDoc(docId) {
  return request({
    url: '/system/doc/' + docId,
    method: 'get'
  })
}

// 新增PRD需求文档
export function addDoc(data) {
  return request({
    url: '/system/doc',
    method: 'post',
    data: data
  })
}

// 修改PRD需求文档
export function updateDoc(data) {
  return request({
    url: '/system/doc',
    method: 'put',
    data: data
  })
}

// 删除PRD需求文档
export function delDoc(docId) {
  return request({
    url: '/system/doc/' + docId,
    method: 'delete'
  })
}

/* ===================== 门户·PRD 读写（/system/prd 数据 + /ai/doc 生成，仅校验登录态，门户用户可用） ===================== */

// 获取可用模型列表与最大对比数（来自后端 ai_model_config 启用项）
export function getDocModels() {
  return request({ url: '/ai/doc/models', method: 'get' })
}

// 提交 PRD：落库 status=1 并推进项目阶段到 PROTO（后端统一处理阶段推进），已对接 /portal/prd/submit/{projectId}
export function submitPrd(projectId, data) {
  return request({ url: '/portal/prd/submit/' + projectId, method: 'post', data })
}

// 按项目读取当前 PRD（返回 AiPrdDoc 或 null），已对接后端 /portal/prd/get
export function getPrdDoc(projectId) {
  return request({
    url: '/portal/prd/get',
    method: 'post',
    data: { projectId }
  })
}

// 按项目 upsert PRD（编辑保存 / 生成后落库），返回主键 docId，已对接后端 /portal/prd/save
export function savePrdDoc(data) {
  return request({
    url: '/portal/prd/save',
    method: 'post',
    data
  })
}

/**
 * 生成 PRD（流式输出）
 * @param {Object} params
 *   - projectId    项目ID（预留，后端用于聚合需求上下文）
 *   - projectName  项目名称
 *   - industryType 行业
 *   - targetUser   目标用户
 *   - templateType 'SIMPLE' | 'STANDARD' | 'DETAIL'
 *   - mode         'single' | 'multi'
 *   - modelIds     模型ID列表（多模型时使用）
 * @param {Object} handlers { onChunk(text), onDone(), onError(err) }
 * @returns {{ stop: Function }} 调用 stop() 可中断生成
 *
 * 说明：调用后端流式生成接口 /ai/doc/generate，原生 fetch 解析 SSE。
 */
export function generatePrd(params, handlers = {}) {
  const body = {
    projectId: params.projectId,
    projectName: params.projectName,
    industryType: params.industryType,
    targetUser: params.targetUser,
    templateType: params.templateType,
    mode: params.mode,
    model: params.model,
    clarifySummary: params.clarifySummary
  }
  return streamGenerate('/ai/doc/generate', body, handlers)
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
        full += (full ? '\n' : '') + (data.content || '生成失败')
        handlers.onChunk && handlers.onChunk(full)
      }
    }
    pump()
  }).catch(err => {
    console.error('[prd] /ai/doc/generate 请求失败：', err.message)
    handlers.onError && handlers.onError(err)
  })
  return ctrl
}
