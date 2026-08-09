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

/* ===================== 门户·PRD 读写（/ai/doc，仅校验登录态，门户用户可用） ===================== */

// 获取可用模型列表与最大对比数（来自后端 ai_model_config 启用项）
export function getDocModels() {
  return request({ url: '/ai/doc/models', method: 'get' })
}

// 提交 PRD：落库 status=1 并推进项目阶段到 PROTO（后端统一处理阶段推进）
export function submitPrd(projectId, data) {
  return request({ url: '/ai/doc/submit/' + projectId, method: 'post', data })
}

// 按项目读取当前 PRD（返回 AiPrdDoc 或 null）
export function getPrdDoc(projectId) {
  return request({
    url: '/ai/doc/get',
    method: 'post',
    data: { projectId }
  })
}

// 按项目 upsert PRD（编辑保存 / 生成后落库），返回主键 docId
export function savePrdDoc(data) {
  return request({
    url: '/ai/doc/save',
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
 * 说明（重要）：
 *  - 当前为前端 mock 数据，便于页面先行联调，无需后端。
 *  - 后端就绪后，删除 mock 分支，改为调用流式生成接口；
 *    注意接口前缀不要带 /api（例如 /ai/doc/generate），
 *    并用原生 fetch 解析 SSE（参考 clarify.js 的 sendMessage 实现）。
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
  // 优先调用真实流式接口 /ai/doc/generate；后端不可用时回退前端 mock，保证页面始终可用
  return streamGenerate('/ai/doc/generate', body, handlers, () => mockStreamGenerate(buildPrdMarkdown(params), handlers))
}

/* 原生 fetch 解析 SSE（text/event-stream），逐 token 累计后回调；
   网络/HTTP 异常时调用 onFallback 回退本地 mock。返回 { stop } 兼容旧调用方。 */
function streamGenerate(url, body, handlers = {}, onFallback) {
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
    // 真实接口不可用 → 回退 mock
    console.warn('[prd] /ai/doc/generate 不可用，回退本地 mock：', err.message)
    if (onFallback) onFallback()
  })
  return ctrl
}

/* ===================== 以下为前端 mock，后端就绪后整段删除 ===================== */

function tplLabel(t) {
  return t === 'SIMPLE' ? '精简' : t === 'DETAIL' ? '详细' : '标准'
}

function formatDate(d) {
  const p = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}/${p(d.getMonth() + 1)}/${p(d.getDate())}`
}

function buildPrdMarkdown(p = {}) {
  const name = p.projectName || '产品'
  const industry = p.industryType || '通用行业'
  const target = p.targetUser || '目标用户'
  const t = p.templateType || 'STANDARD'
  const mode = p.mode === 'multi' ? '多模型协同' : '单模型'
  const date = formatDate(new Date())
  const tpl = tplLabel(t)
  // 注意：澄清结论仅作为生成上下文（后端接入时喂给模型），绝不渲染进文档
  const head = `# ${name} 产品需求文档（PRD）\n\n> 版本：v1.0 ｜ 状态：草稿 ｜ 生成日期：${date} ｜ 模板：${tpl} ｜ 生成模式：${mode} ｜ 行业：${industry}\n`

  const revision = `## 1. 修订记录\n\n| 版本 | 日期 | 作者 | 变更说明 |\n| --- | --- | --- | --- |\n| v1.0 | ${date} | 产品团队 | 基于需求澄清生成首版 |\n`

  const overview = `## 2. 产品概述\n\n### 2.1 背景与机遇\n**${name}** 面向${industry}领域的${target}，旨在解决用户在核心业务场景中的关键痛点，通过标准化、可落地的产品方案提升业务效率与体验。\n\n### 2.2 产品目标\n- 明确并收敛核心需求，形成可研发拆解的产品定义\n- 提升${target}在关键流程中的效率与满意度\n- 建立可追溯的需求基线，支撑原型、技术方案与数据设计联动\n\n### 2.3 目标用户\n- 核心用户：${target}\n- 次级用户：运营、管理与协作角色\n`

  const scope = `## 3. 范围\n\n### 3.1 范围内（In Scope）\n- 需求的结构化采集与 AI 澄清\n- 产品需求文档（PRD）的生成与在线编辑\n- 原型 / 技术方案 / 数据库设计的串联推导\n\n### 3.2 范围外（Out of Scope）\n- 底层模型训练与算力基础设施建设\n- 与既有第三方系统的深度集成（本期不纳入）\n`

  const fr = `## 4. 功能需求\n\n### 4.1 功能清单\n| 编号 | 功能模块 | 描述 | 优先级 |\n| --- | --- | --- | --- |\n| FR-01 | 需求采集 | 支持结构化表单与自由描述录入 | P0 |\n| FR-02 | AI 澄清 | 多模型并行提问，自动收敛需求边界 | P0 |\n| FR-03 | PRD 生成 | 一键产出结构化产品需求文档 | P0 |\n| FR-04 | 文档编辑 | 支持在线编辑、修订与版本保存 | P1 |\n| FR-05 | 设计联动 | 原型 / 技术 / 库表推导串联 | P1 |\n\n### 4.2 关键用户故事\n- 作为${target}，我希望能快速录入需求，以便减少沟通成本\n- 作为产品负责人，我希望 AI 自动澄清模糊点，以便需求更完整\n- 作为研发，我希望获得结构化 PRD，以便直接拆解任务\n`

  const nfr = `## 5. 非功能需求\n- 性能：关键接口 P95 < 300ms\n- 可用性：核心链路可用性 ≥ 99.9%\n- 安全：敏感数据加密存储、最小权限访问\n- 兼容性：支持主流浏览器与移动端访问\n`

  const acceptance = `## 6. 验收标准\n- 需求澄清覆盖率 ≥ 90%\n- PRD 关键章节完整率 100%\n- 上下游环节数据可溯源\n`

  const data = `## 7. 数据需求（草稿）\n- 项目主表 project：projectId, projectName, step\n- 澄清会话 clarify_session：sessionId, conclusion\n- PRD 文档 prd_doc：docId, templateType, content\n`

  const risk = `## 8. 风险与依赖\n- 依赖 AI 模型服务的稳定性与响应时延\n- 需求变更频繁时须保证 PRD 与下游产物一致\n`

  const milestone = `## 9. 里程碑\n- M1：采集 + 澄清闭环\n- M2：PRD 生成与编辑\n- M3：原型 / 技术 / 库表联动\n`

  if (t === 'SIMPLE') {
    return head + overview + '\n' + scope + '\n' + fr + '\n' + acceptance
  }
  if (t === 'DETAIL') {
    return head + revision + overview + '\n' + scope + '\n' + fr + '\n' + nfr + acceptance + data + risk + milestone
  }
  // 标准（默认）
  return head + revision + overview + '\n' + scope + '\n' + fr + '\n' + nfr + acceptance + milestone
}

function mockStreamGenerate(fullText, handlers = {}) {
  let i = 0
  const stepSize = 8
  const timer = setInterval(() => {
    i += stepSize
    if (i >= fullText.length) {
      handlers.onChunk && handlers.onChunk(fullText)
      clearInterval(timer)
      handlers.onDone && handlers.onDone()
      return
    }
    handlers.onChunk && handlers.onChunk(fullText.slice(0, i))
  }, 35)
  return { stop() { clearInterval(timer) } }
}
