import request from '@/utils/request'

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
  // TODO(后端就绪): 替换为真实流式请求，例如：
  // const res = await fetch('/ai/doc/generate', { method: 'POST', headers: {...}, body: JSON.stringify(params) })
  // 再用原生 fetch 解析 SSE 逐块调用 handlers.onChunk(text)；结束时 handlers.onDone()，异常时 handlers.onError(err)
  return mockStreamGenerate(buildPrdMarkdown(params), handlers)
}

/* ===================== 以下为前端 mock，后端就绪后整段删除 ===================== */

function tplLabel(t) {
  return t === 'SIMPLE' ? '精简' : t === 'DETAIL' ? '详细' : '标准'
}

function buildPrdMarkdown(p = {}) {
  const name = p.projectName || '产品'
  const industry = p.industryType || '通用行业'
  const target = p.targetUser || '目标用户'
  const t = p.templateType || 'STANDARD'
  const mode = p.mode === 'multi' ? '多模型对比' : '单模型'
  const head = `# ${name} 产品需求文档（PRD）\n\n> 模板：${tplLabel(t)} ｜ 生成模式：${mode} ｜ 行业：${industry}\n\n`
  const overview = `## 1. 产品概述\n\n**${name}** 面向${industry}的${target}，旨在解决用户在核心场景下的关键痛点。本文档基于已确认的需求基线，明确产品目标、功能范围与关键约束。\n\n### 1.1 产品目标\n- 提升${target}在核心流程中的效率\n- 降低操作门槛，缩短任务完成时间\n- 通过数据沉淀支撑后续精细化运营\n\n### 1.2 用户群体\n- 核心用户：${target}\n- 次级用户：运营与管理角色\n`
  const scope = `## 2. 功能范围\n\n### 2.1 核心功能\n1. 需求采集：支持结构化表单与自由描述录入\n2. AI 澄清：多模型并行提问，自动收敛需求边界\n3. PRD 生成：一键产出结构化产品需求文档\n4. 原型 / 技术 / 库表推导：串联设计、研发与数据层\n\n### 2.2 非功能需求\n- 性能：关键接口 P95 < 300ms\n- 可用性：核心链路可用性 ≥ 99.9%\n- 安全：敏感数据加密存储、最小权限访问\n`
  let body = overview + '\n' + scope
  if (t === 'STANDARD' || t === 'DETAIL') {
    body += `\n## 3. 用户故事\n\n- 作为${target}，我希望能快速录入需求，以便减少沟通成本\n- 作为产品负责人，我希望 AI 自动澄清模糊点，以便需求更完整\n- 作为研发，我希望获得结构化 PRD，以便直接拆解任务\n`
  }
  if (t === 'DETAIL') {
    body += `\n## 4. 关键流程\n\n1. 用户进入需求采集页，填写 / 描述需求\n2. 系统调用多模型进行澄清并汇总结论\n3. 基于结论生成 PRD，支持在线编辑与定向修改\n4. 确认后流转至原型、技术方案与数据库设计\n\n## 5. 数据模型（草稿）\n\n- 项目主表 project：projectId, projectName, step\n- 澄清会话 clarify_session：sessionId, conclusion\n- PRD 文档 prd_doc：docId, templateType, content\n\n## 6. 里程碑\n\n- M1：采集 + 澄清闭环\n- M2：PRD 生成与编辑\n- M3：原型 / 技术 / 库表联动\n`
  } else if (t === 'STANDARD') {
    body += `\n## 4. 验收标准\n\n- 需求澄清覆盖率 ≥ 90%\n- PRD 关键章节完整率 100%\n- 上下游环节数据可溯源\n`
  } else {
    body += `\n## 3. 验收标准\n\n- 关键需求点均有明确结论\n- 文档可直接用于研发拆解\n`
  }
  return head + body
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
