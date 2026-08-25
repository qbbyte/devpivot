import request from '@/utils/request'
import { getToken } from '@/utils/auth'

// ===================== 门户·AI澄清对话链路（/ai/clarify） =====================

// 获取可用模型列表
export function getModels() {
  return request({
    url: '/ai/clarify/models',
    method: 'get'
  })
}

// 获取系统配置（最大对比模型数）
export function getModelConfig() {
  return request({
    url: '/ai/clarify/models/config',
    method: 'get'
  })
}

// 获取澄清会话
export function getClarifySession(projectId) {
  return request({
    url: `/portal/clarify/session/${projectId}`,
    method: 'get'
  })
}

// 持久化完整对话（前端为权威源）：传入 { conversation, retained }，原样落库供刷新恢复
export function saveSession(projectId, data) {
  return request({
    url: `/portal/clarify/save/${projectId}`,
    method: 'post',
    data
  })
}

// 发送消息（流式 SSE）：后端并发调用多个大模型并逐 token 推送。
// onEvent({ event, data }) 在收到每个 SSE 事件时回调；Promise 在流正常结束时 resolve。
// 使用原生 fetch（axios 不擅长读流），并自行解析 text/event-stream。
export function sendMessage(data, onEvent) {
  const base = import.meta.env.VITE_APP_BASE_API || ''
  return new Promise((resolve, reject) => {
    fetch(base + '/ai/clarify/send', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + getToken()
      },
      body: JSON.stringify(data)
    }).then(resp => {
      if (!resp.ok || !resp.body) {
        reject(new Error('HTTP ' + resp.status))
        return
      }
      const reader = resp.body.getReader()
      const decoder = new TextDecoder('utf-8')
      let buffer = ''
      const pump = () => reader.read().then(({ done, value }) => {
        if (done) {
          if (buffer.trim()) {
            const ev = parseSseEvent(buffer)
            if (ev) onEvent(ev)
          }
          resolve()
          return
        }
        buffer += decoder.decode(value, { stream: true })
        let idx
        while ((idx = buffer.indexOf('\n\n')) >= 0) {
          const raw = buffer.slice(0, idx)
          buffer = buffer.slice(idx + 2)
          const ev = parseSseEvent(raw)
          if (ev) onEvent(ev)
        }
        pump()
      })
      pump()
    }).catch(err => reject(err))
  })
}

// 解析单个 SSE 事件块（含 event:/data: 行），返回 { event, data }
function parseSseEvent(raw) {
  let eventName = ''
  let dataStr = ''
  raw.split('\n').forEach(line => {
    if (line.startsWith('event:')) {
      eventName = line.slice(6).trim()
    } else if (line.startsWith('data:')) {
      dataStr += line.slice(5).trim()
    }
  })
  if (!dataStr) return null
  try {
    return { event: eventName, data: JSON.parse(dataStr) }
  } catch (e) {
    return null
  }
}

// 获取澄清进度
export function getClarifyProgress(projectId) {
  return request({
    url: `/portal/clarify/progress/${projectId}`,
    method: 'get'
  })
}

// 动态生成下一题：后端 AI 依据需求基线 + 澄清对话历史生成针对性问题，返回 { content, options }
export function nextClarifyQuestion(data) {
  return request({
    url: '/ai/clarify/nextQuestion',
    method: 'post',
    data
  })
}

// 提交澄清结果（conclusion 为完整结论对象，将落库并推进项目阶段到 PRD）
export function submitClarify(projectId, conclusion) {
  return request({
    url: `/portal/clarify/submit/${projectId}`,
    method: 'post',
    data: conclusion
  })
}

// 采纳模型回答
export function adoptAnswer(data) {
  return request({
    url: '/portal/clarify/adopt',
    method: 'post',
    data
  })
}

// ===================== 历史版本（/portal/clarify/version，复用后端 ai_version_record，bizType=CLARIFY） =====================

// 保存当前澄清结论为历史版本：snapshot=完整澄清结论对象；versionName/remark/sourceModel 可选
export function saveClarifyVersion(projectId, snapshot, versionName = '', remark = '', sourceModel = '') {
  return request({
    url: `/portal/clarify/version/${projectId}`,
    method: 'post',
    data: { snapshot, versionName, remark, sourceModel }
  })
}

// 历史版本列表（含派生文件清单，不含大快照正文）
export function listClarifyVersions(projectId) {
  return request({
    url: `/portal/clarify/versions/${projectId}`,
    method: 'get'
  }).then(res => res.data || [])
}

// 获取单个版本（含快照正文，供查看/还原）
export function getClarifyVersion(versionId) {
  return request({
    url: `/portal/clarify/version/${versionId}`,
    method: 'get'
  }).then(res => res.data || {})
}

// 还原历史版本（把快照写回当前会话）
export function restoreClarifyVersion(versionId) {
  return request({
    url: `/portal/clarify/version/restore/${versionId}`,
    method: 'post'
  })
}

// ===================== 后台管理·澄清问题记录 CRUD（/system/clarify） =====================
// 注：以下接口与上面门户对话链路相互独立，对应后端 AiClarifyRecordController 的标准 CRUD。

// 查询AI澄清问题记录列表
export function listClarify(query) {
  return request({
    url: '/system/clarify/list',
    method: 'get',
    params: query
  })
}

// 获取AI澄清问题记录详细信息
export function getClarify(recordId) {
  return request({
    url: `/system/clarify/${recordId}`,
    method: 'get'
  })
}

// 删除AI澄清问题记录
export function delClarify(recordIds) {
  return request({
    url: `/system/clarify/${recordIds}`,
    method: 'delete'
  })
}

// 新增AI澄清问题记录
export function addClarify(data) {
  return request({
    url: '/system/clarify',
    method: 'post',
    data
  })
}

// 修改AI澄清问题记录
export function updateClarify(data) {
  return request({
    url: '/system/clarify',
    method: 'put',
    data
  })
}
