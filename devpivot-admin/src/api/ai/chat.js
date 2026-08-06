import request from '@/utils/request'

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
 * 说明（重要）：
 *  - 当前为前端 mock 数据，便于页面先行联调，无需后端。
 *  - 后端就绪后，删除 mock 分支，改为调用流式对话接口；
 *    注意接口前缀不要带 /api（例如 /ai/chat/send），
 *    并用原生 fetch 解析 SSE（参考 clarify.js 的 sendMessage 实现）。
 */
export function sendChatMessage(params, handlers = {}) {
  // TODO(后端就绪): 替换为真实流式请求，例如：
  // const res = await fetch('/ai/chat/send', { method: 'POST', headers: {...}, body: JSON.stringify(params) })
  // 再用原生 fetch 解析 SSE 逐块调用 handlers.onChunk(text)；结束时 handlers.onDone()，异常时 handlers.onError(err)
  return mockStreamGenerate(buildChatReply(params), handlers)
}

/* ===================== 以下为前端 mock，后端就绪后整段删除 ===================== */

function buildChatReply(p = {}) {
  const q = (p.question || '').trim() || '你的需求'
  const name = p.projectName || '产品'
  const docHint = p.docContent && p.docContent.length > 40
    ? '我已读到当前 PRD 内容，可据此定向修订。'
    : '当前 PRD 尚未生成，建议先产出文档再细化。'
  const reply = [
    `收到，关于「${q}」我有以下建议：`,
    '',
    '### 关联 PRD 要点',
    `- 该需求可归入「功能范围 / 关键流程」章节，建议补充明确的验收口径`,
    `- 需要与 ${name} 的目标用户诉求对齐，避免范围蔓延`,
    `- 如涉及性能或安全约束，应在非功能需求中单独列项`,
    '',
    '### 处理建议',
    `1. ${docHint}`,
    '2. 若需改动具体章节，可直接告诉我「修订第 X 章 …」，我会给出修订后的内容',
    '3. 确认后点击左侧「重新生成」或手动粘贴到文档区',
    '',
    '你可以继续补充其他需求点，或对某条建议追问细节。'
  ].join('\n')
  return reply
}

function mockStreamGenerate(fullText, handlers = {}) {
  let i = 0
  const stepSize = 6
  const timer = setInterval(() => {
    i += stepSize
    if (i >= fullText.length) {
      handlers.onChunk && handlers.onChunk(fullText)
      clearInterval(timer)
      handlers.onDone && handlers.onDone()
      return
    }
    handlers.onChunk && handlers.onChunk(fullText.slice(0, i))
  }, 30)
  return { stop() { clearInterval(timer) } }
}
