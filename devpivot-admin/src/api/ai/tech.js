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

/* ===================== 门户·技术方案读写（/ai/tech，仅校验登录态） ===================== */

// 获取可用模型列表与最大对比数
// TODO 后端就绪后改为：return request({ url: '/ai/tech/models', method: 'get' })
export function getTechModels() {
  return Promise.resolve({
    models: [
      { modelId: 'gpt4o', modelName: 'GPT-4o' },
      { modelId: 'deepseek', modelName: 'DeepSeek' },
      { modelId: 'claude', modelName: 'Claude' },
      { modelId: 'tongyi', modelName: '通义千问' },
      { modelId: 'wenxin', modelName: '文心一言' }
    ],
    maxCompareCount: 4
  })
}

// 按项目读取当前技术方案（返 AiTechDoc 或 null）
// TODO 后端就绪后改为真实接口；当前后端未实现，失败由调用方兜底为空。
export function getTechDoc(projectId) {
  return request({ url: '/ai/tech/doc', method: 'post', data: { projectId } })
}

// 按项目 upsert 技术方案（编辑保存 / 生成后落库），返回主键 docId
// TODO 后端就绪后改为真实接口；当前后端未实现，失败仅告警，前端保留本地状态。
export function saveTechDoc(data) {
  return request({ url: '/ai/tech/save', method: 'post', data })
}

// 提交技术方案：落库 status=1 并推进项目阶段到 DB（由后端统一处理）
// TODO 后端就绪后改为真实接口；当前页面直接用 updateProject 推进阶段。
export function submitTech(projectId, data) {
  return request({ url: '/ai/tech/submit/' + projectId, method: 'post', data })
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
 * 说明（重要）：
 *  - 当前 generateTech 先尝试真实流式接口 /ai/tech/generate；
 *    该接口后端未实现时自动回退到前端多模型并行 mock，保证页面始终可用。
 *  - 后端就绪后，仅需确保 /ai/tech/generate 按 SSE 多路复用协议推送
 *    （事件 data 含 { type:'token'|'done'|'error', modelId, delta/content }），
 *    前端 streamGenerateTech 已兼容解析，无需改动页面。
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
  return streamGenerateTech('/ai/tech/generate', body, handlers, () =>
    mockGenerateTech(params, handlers)
  )
}

/* 原生 fetch 解析 SSE（text/event-stream），按 modelId 多路复用逐 token 累计后回调；
   网络/HTTP 异常时调用 onFallback 回退本地 mock。返回 { stop } 兼容旧调用方。 */
function streamGenerateTech(url, body, handlers = {}, onFallback) {
  const base = import.meta.env.VITE_APP_BASE_API || ''
  const ctrl = { stopped: false, stop() { this.stopped = true } }
  const fullByModel = {}
  fetch(base + url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + (getToken() || '') },
    body: JSON.stringify(body)
  }).then(resp => {
    const ct = resp.headers ? (resp.headers.get('content-type') || '') : ''
    // 后端未实现该接口时，若依常返回 HTTP 200 + JSON（非 SSE）。
    // 只有明确是 text/event-stream 才走真实解析，否则回退本地 mock，保证页面始终可用。
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
    console.warn('[tech] /ai/tech/generate 不可用，回退本地 mock：', err.message)
    if (onFallback) onFallback()
  })
  return ctrl
}

/* ===================== 以下为前端 mock，后端就绪后整段删除 ===================== */

function formatDate(d) {
  const p = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}/${p(d.getMonth() + 1)}/${p(d.getDate())}`
}

function parseStack(techStack) {
  if (!techStack) return { backend: 'Java', frontend: 'Vue 3' }
  const parts = String(techStack).split(' + ')
  return { backend: parts[0] || 'Java', frontend: parts[1] || 'Vue 3' }
}

// 多模型并行 mock 流：每个模型独立计时器，互不影响（模拟并发）
function mockGenerateTech(params, handlers = {}) {
  const models = (params.models && params.models.length) ? params.models : ['gpt4o']
  const timers = []
  let doneCount = 0
  const checkAll = () => { if (++doneCount >= models.length) handlers.onAllDone && handlers.onAllDone() }

  models.forEach((modelId, idx) => {
    const fullText = buildTechMarkdown(params, modelId)
    let i = 0
    const stepSize = 12
    const timer = setInterval(() => {
      i += stepSize
      if (i >= fullText.length) {
        handlers.onModelChunk && handlers.onModelChunk(modelId, fullText)
        clearInterval(timer)
        handlers.onModelDone && handlers.onModelDone(modelId)
        checkAll()
        return
      }
      handlers.onModelChunk && handlers.onModelChunk(modelId, fullText.slice(0, i))
    }, 30 + idx * 6)
    timers.push(timer)
  })

  return {
    stop() { timers.forEach(t => clearInterval(t)) }
  }
}

// 根据前后端技术栈 + 模型风格生成差异化的技术方案 Markdown（用于对比视图体现多模型差异）
function buildTechMarkdown(p = {}, modelId = 'gpt4o') {
  const { backend, frontend } = parseStack(p.techStack)
  const name = p.projectName || '产品'
  const industry = p.industryType || '通用行业'
  const target = p.targetUser || '目标用户'
  const date = formatDate(new Date())
  const extra = p.extraReq ? `\n> 补充要求：${p.extraReq}\n` : ''

  const flavor = {
    gpt4o: '（强调标准化分层与云原生弹性）',
    deepseek: '（强调成本可控与落地节奏）',
    claude: '（强调可维护性与合规治理）',
    tongyi: '（强调国产化与生态集成）',
    wenxin: '（强调中文业务场景与智能能力复用）'
  }[modelId] || ''

  const backendMap = {
    'Java': { fw: 'Spring Boot 3.x + Spring Cloud Alibaba', orm: 'MyBatis-Plus', pkg: 'Maven 多模块', store: 'MySQL 8 + Redis', why: '成熟的微服务生态与团队储备，适合中大型长期演进', deploy: 'Kubernetes', prefix: 'biz_' },
    'Python': { fw: 'FastAPI + SQLAlchemy 2.0', orm: 'SQLAlchemy', pkg: 'Poetry / uv 虚拟环境', store: 'PostgreSQL 15 + Redis', why: '开发效率高、AI/数据生态丰富，适合快速验证与数据密集型场景', deploy: 'Docker Compose / K8s', prefix: 't_' },
    'Node.js': { fw: 'NestJS + TypeORM / Prisma', orm: 'TypeORM', pkg: 'pnpm 工作区', store: 'MySQL 8 + Redis', why: '前后端同构、事件驱动与 I/O 密集型场景表现优异', deploy: 'Kubernetes', prefix: 't_' },
    'Go': { fw: 'Gin / Echo + GORM', orm: 'GORM', pkg: 'Go Modules', store: 'MySQL 8 + Redis', why: '高并发、编译快、资源占用低，适合高性能服务', deploy: 'Kubernetes', prefix: 't_' },
    '.NET': { fw: 'ASP.NET Core 8 + EF Core', orm: 'EF Core', pkg: 'NuGet + SDK 风格项目', store: 'SQL Server / PostgreSQL + Redis', why: '企业级工具链完善，适合强类型复杂业务系统', deploy: 'Kubernetes', prefix: 'biz_' }
  }
  const feMap = {
    'Vue 3': { fw: 'Vue 3 + Element Plus', state: 'Pinia', build: 'Vite', ssr: '可选 Nuxt 3' },
    'React': { fw: 'React 18 + Ant Design', state: 'Zustand / Redux', build: 'Vite / webpack', ssr: '可选 Next.js' },
    'Angular': { fw: 'Angular 17 + NG-ZORRO', state: 'RxJS 信号', build: 'Angular CLI', ssr: '可选 Angular Universal' },
    'HTML5': { fw: 'HTML5 + 原生 JS', state: 'localStorage / 轻量 Store', build: '静态构建 / CDN', ssr: '无' }
  }

  const b = backendMap[backend] || backendMap['Java']
  const f = feMap[frontend] || feMap['Vue 3']
  const middleware = backend === 'Java' ? 'Nacos 注册配置中心、Sentinel 限流' : (backend === 'Python' ? 'Celery 异步任务、Redis 消息' : '消息队列 / 分布式缓存')

  const head = `# ${name} 技术方案${flavor}\n\n> 版本：v1.0 ｜ 状态：草稿 ｜ 生成日期：${date} ｜ 技术栈：${backend} + ${frontend} ｜ 行业：${industry} ｜ 目标用户：${target}\n${extra}`

  const stackSec = `## 1. 技术栈选型\n- 后端框架：${b.fw}\n- 前端框架：${f.fw}\n- 前端状态：${f.state}\n- 构建工具：${f.build}\n- 数据访问：${b.orm}\n- 工程化：${b.pkg}\n- 存储：${b.store}\n- 鉴权：JWT（无状态）\n- SSR 策略：${f.ssr}\n`

  const arch = `## 2. 系统架构\n采用分层 + 领域驱动设计（DDD）风格：\n- 接入层：API 网关 / 负载均衡\n- 应用层：控制器、应用服务、领域服务\n- 领域层：实体、值对象、聚合根\n- 基础设施层：数据库、缓存、消息、第三方集成\n\n关键中间件：${middleware}。\n`

  const modules = `## 3. 模块划分\n| 模块 | 职责 | 优先级 |\n| --- | --- | --- |\n| 用户与权限 user | 账号、角色、JWT 鉴权 | P0 |\n| 核心业务 core | ${name} 主流程领域逻辑 | P0 |\n| 内容/文档 content | PRD、原型、技术文档管理 | P1 |\n| 前端门户 portal | ${f.fw} 门户页面 | P0 |\n| 通知 notify | 站内信、Webhook | P2 |\n| 运维 observability | 日志、监控、链路追踪 | P1 |\n`

  const decision = `## 4. 关键设计决策\n- 为什么选 ${backend}：${b.why}。\n- 为什么选 ${frontend}：${frontend === 'Vue 3' ? '渐进式框架、国内生态成熟、与 Element Plus 组合开发效率高' : (frontend === 'React' ? '组件生态丰富、灵活度高、适合复杂交互' : (frontend === 'Angular' ? '全栈框架规范统一、适合大型团队协作' : '极简、无构建依赖、适合轻量后台'))}。\n- 数据一致性：核心交易走本地事务 + 最终一致性补偿；非核心走异步消息。\n- 多租户：行级隔离（tenant_id）优先，必要时按库隔离。\n- 可扩展性：无状态服务 + 水平扩容，配置中心动态下发。\n`

  const nfr = `## 5. 非功能设计\n- 性能：核心接口 P95 < 300ms，列表查询走缓存与分页。\n- 可用性：核心链路 ≥ 99.9%，多副本 + 健康探针。\n- 安全：传输 TLS、敏感字段加密、最小权限、审计日志。\n- 可观测：结构化日志 + 指标 + 分布式追踪。\n`

  const deploy = `## 6. 部署与运维\n- 容器化：Docker + ${b.deploy}。\n- CI/CD：流水线含构建、单测、镜像推送、灰度发布。\n- 备份：数据库每日全量 + 增量，保留 30 天并定期演练恢复。\n`

  const dbHint = `## 7. 对数据库阶段（DB）的输入提示\n- 建议主表前缀：${b.prefix}；统一主键 snowflake / UUID。\n- 必建索引：tenant_id、create_time、业务外键。\n- 枚举与字典统一收纳到字典表，避免硬编码。\n`

  const risk = `## 8. 风险与依赖\n- 依赖模型服务稳定性与响应时延。\n- 需求变更须保证 PRD → 技术方案 → 库表一致。\n`

  return head + stackSec + arch + modules + decision + nfr + deploy + dbHint + risk
}
