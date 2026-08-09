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
  return streamGenerateDb('/ai/db/generate', body, handlers, () =>
    mockGenerateDb(params, handlers)
  )
}

/* 原生 fetch 解析 SSE（text/event-stream），按 modelId 多路复用逐 token 累计后回调；
   网络/HTTP 异常时调用 onFallback 回退本地 mock。 */
function streamGenerateDb(url, body, handlers = {}, onFallback) {
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
    console.warn('[db] /ai/db/generate 不可用，回退本地 mock：', err.message)
    if (onFallback) onFallback()
  })
  return ctrl
}

/* ===================== 前端 mock，后端就绪后整段删除 ===================== */

function formatDate(d) {
  const p = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}/${p(d.getMonth() + 1)}/${p(d.getDate())}`
}

function mockGenerateDb(params, handlers = {}) {
  const modelId = (params.models && params.models[0]) || 'deepseek'
  const fullText = buildDbMarkdown(params)
  let i = 0
  const stepSize = 10
  const timer = setInterval(() => {
    i += stepSize
    if (i >= fullText.length) {
      handlers.onModelChunk && handlers.onModelChunk(modelId, fullText)
      clearInterval(timer)
      handlers.onModelDone && handlers.onModelDone(modelId)
      handlers.onAllDone && handlers.onAllDone()
      return
    }
    handlers.onModelChunk && handlers.onModelChunk(modelId, fullText.slice(0, i))
  }, 30)
  return { stop() { clearInterval(timer) } }
}

function buildDbMarkdown(p = {}) {
  const name = p.projectName || '产品'
  const industry = p.industryType || '通用行业'
  const target = p.targetUser || '目标用户'
  const db = p.dbType || 'MySQL'
  const date = formatDate(new Date())
  const extra = p.extraReq ? `\n> 补充要求：${p.extraReq}\n` : ''

  const head = `# ${name} 数据库设计文档\n\n> 版本：v1.0 ｜ 状态：草稿 ｜ 生成日期：${date} ｜ 目标数据库：${db} ｜ 行业：${industry} ｜ 目标用户：${target}\n${extra}`

  const goal = `## 1. 设计目标与约束\n- 支持${target}的核心业务数据持久化与查询。\n- 满足未来 1-3 年数据增长与并发需求。\n- 优先保证数据一致性与可维护性。\n`

  const choice = `## 2. 数据库选型与部署架构\n- 主数据库：${db}。\n- 缓存：Redis（热点数据、会话、分布式锁）。\n- 部署：主从架构，定期全量 + 增量备份。\n`

  const spec = `## 3. 全局命名与字段规范\n- 表名统一小写，下划线分隔，业务前缀如 \`biz_\`。\n- 主键使用 \`bigint\` 自增或雪花 ID。\n- 必有审计字段：\`create_by\`、\`create_time\`、\`update_by\`、\`update_time\`、\`remark\`、\`del_flag\`。\n- 软删除统一使用 \`del_flag\`（0 存在 / 2 删除）。\n`

  const er = `## 4. 实体-关系总览\n核心实体：用户、项目、PRD、原型页面、原型组件、技术方案、数据库设计、澄清会话。\n主要关系：一个项目拥有多条 PRD/技术方案/库表记录；一个页面包含多个组件。\n`

  const tables = `## 5. 核心表结构\n\n### 5.1 项目主表（ai_project）\n| 字段 | 类型 | 长度 | 可空 | 默认值 | 说明 |\n| --- | --- | --- | --- | --- | --- |\n| project_id | bigint | 20 | N | auto_increment | 主键 |\n| project_name | varchar | 64 | N | - | 项目名称 |\n| industry_type | varchar | 32 | Y | '' | 行业分类 |\n| step | varchar | 16 | Y | 'REQ' | 当前阶段 |\n| status | char | 1 | Y | '0' | 项目状态 |\n\n### 5.2 PRD 文档表（ai_prd_doc）\n| 字段 | 类型 | 长度 | 可空 | 默认值 | 说明 |\n| --- | --- | --- | --- | --- | --- |\n| doc_id | bigint | 20 | N | auto_increment | 主键 |\n| project_id | bigint | 20 | N | - | 项目ID |\n| content | longtext | - | Y | - | Markdown 内容 |\n| status | char | 1 | Y | '0' | 0草稿 1已确认 |\n`

  const dict = `## 6. 关键业务字段字典\n- 项目阶段：REQ / CLARIFY / PRD / PROTO / TECH / DB / DONE。\n- 项目状态：0 正常 / 1 归档。\n`

  const idx = `## 7. 索引与性能设计\n- ai_prd_doc.project_id 建立普通索引。\n- ai_project.step 建立普通索引便于阶段统计。\n- 列表查询优先走覆盖索引与缓存。\n`

  const sec = `## 8. 安全与合规\n- 敏感字段（如手机号）加密存储。\n- 数据库账号按最小权限原则分配。\n- 开启审计日志记录关键变更。\n`

  const ddl = `## 9. 可执行 DDL（${db}）\n\`\`\`sql\nCREATE TABLE ai_project (\n  project_id BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '项目ID',\n  project_name VARCHAR(64) NOT NULL COMMENT '项目名称',\n  step VARCHAR(16) DEFAULT 'REQ' COMMENT '当前阶段',\n  status CHAR(1) DEFAULT '0' COMMENT '项目状态',\n  PRIMARY KEY (project_id),\n  INDEX idx_step (step)\n) ENGINE=InnoDB COMMENT='AI项目表';\n\`\`\``

  return head + goal + choice + spec + er + tables + dict + idx + sec + ddl
}
