import JSZip from 'jszip'
import { saveAs } from 'file-saver'
import { getProjectContext, createExportToken } from '@/api/ai/project'

/**
 * 导出项目开发上下文到各类 AI 编程工具。
 *
 * 设计要点（一源多投）：
 * - 后端 /portal/project/{id}/artifacts 已聚合 PRD/原型/技术/DB 等各阶段产物文本
 * - AGENTS.md 作为跨工具权威文件（2026 行业事实标准，Linux Foundation 托管）
 * - 选中具体工具时，再补一份该工具专属规则文件（内容镜像 AGENTS.md）
 * - 各阶段产物独立成文件，AGENTS.md 仅做索引，避免撑爆上下文
 */

// 各阶段产物 -> 解压后的文件名（英文，便于 AGENTS.md 引用）
const STEP_FILES = {
  REQ: { file: 'requirements.md', title: '需求基线' },
  CLARIFY: { file: 'clarify.md', title: 'AI 澄清记录' },
  PRD: { file: 'prd.md', title: 'PRD 文档' },
  PROTO: { file: 'proto.json', title: '原型设计' },
  TECH: { file: 'tech.md', title: '技术方案' },
  DB: { file: 'db.md', title: '数据库设计' }
}

// 取出某阶段产物文本；无数据则给占位说明
function artifactText(artifacts, step) {
  const info = STEP_FILES[step]
  const a = (artifacts || []).find(x => x.step === step)
  if (a && a.hasData && a.content) return a.content
  return `# ${info.title}\n\n（本项目尚未生成该阶段产物）\n`
}

// 构建 AGENTS.md（项目宪法：概览 + 约束 + 产物索引）
function buildAgentsMd(project, artifacts) {
  const name = project.projectName || '未命名项目'
  const lines = []
  lines.push(`# ${name}`)
  lines.push('')
  lines.push('> 由 devPivot 协同研发平台导出 · 项目开发上下文')
  lines.push('')
  lines.push('## 项目概览')
  lines.push(`- 行业分类：${project.industryType || '未填写'}`)
  lines.push(`- 目标用户：${project.targetUser || '未填写'}`)
  lines.push(`- 当前阶段：${project.step || '未开始'}`)
  lines.push(`- 负责人：${project.assigneeName || '未指定'}`)
  lines.push('')
  lines.push('## 技术约束')
  lines.push('- 严格按照下方各阶段产物文档进行开发，不要偏离已确认的需求与设计')
  lines.push('- 修改代码前，先阅读对应阶段的产物文档')
  lines.push('- 不要引入未在「技术方案」中列出的新技术栈或依赖')
  lines.push('')
  lines.push('## 阶段产物（位于仓库根目录）')
  Object.keys(STEP_FILES).forEach(step => {
    const s = STEP_FILES[step]
    lines.push(`- ${s.title} → \`${s.file}\``)
  })
  lines.push('')
  lines.push('## 协作约定')
  lines.push('- 数据库变更必须同步更新 `db.md`')
  lines.push('- 接口变更必须同步更新 `prd.md` 与 `tech.md`')
  lines.push('')
  return lines.join('\n')
}

/**
 * 导出开发上下文
 * @param {Object} opts
 * @param {string|number} opts.projectId 项目ID
 * @param {Object} opts.project 项目基本信息（projectName/industryType/targetUser/step/assigneeName）
 * @param {string} [opts.target='agents'] 目标工具：agents|cursor|trae|vscode|claudecode
 */
export async function exportDevContext({ projectId, project, target = 'agents' }) {
  const res = await getProjectContext(projectId)
  const data = res.data || {}
  const artifacts = data.artifacts || []
  const projectName = (project && project.projectName) || data.projectName || 'project'
  const folder = `${projectName}-dev-context`

  const zip = new JSZip()
  const root = zip.folder(folder)

  // 1) 各阶段产物独立成文件
  Object.keys(STEP_FILES).forEach(step => {
    root.file(STEP_FILES[step].file, artifactText(artifacts, step))
  })

  // 2) 跨工具权威文件 AGENTS.md（始终包含）
  const agentsMd = buildAgentsMd(project || {}, artifacts)
  root.file('AGENTS.md', agentsMd)

  // 3) 选中具体工具时，补对应专属规则文件（内容镜像 AGENTS.md）
  if (target === 'cursor') {
    const mdc = '---\ndescription: devPivot 项目「' + projectName + '」开发上下文\nalwaysApply: true\n---\n\n' + agentsMd
    root.file('.cursor/rules/devpivot.mdc', mdc)
  } else if (target === 'trae') {
    root.file('.trae/rules/project_rules.md', agentsMd)
  } else if (target === 'vscode') {
    root.file('.github/copilot-instructions.md', agentsMd)
  } else if (target === 'claudecode') {
    root.file('CLAUDE.md', agentsMd)
  }

  const blob = await zip.generateAsync({ type: 'blob' })
  saveAs(blob, `${folder}.zip`)
}

// 各 fmt -> 约定文件名（与后端 contextFileName / 菜单映射保持一致）
const FMT_FILE = {
  agents: 'AGENTS.md',
  claude: 'CLAUDE.md',
  cursor: '.cursor/rules/devpivot.mdc',
  trae: '.trae/rules/project_rules.md',
  vscode: '.github/copilot-instructions.md'
}

/**
 * 构建可粘贴的单文件 Markdown：内联全部阶段产物，供终端/聊天框（Claude Code / Codex）直接粘贴
 * 与 zip 导出不同，此处把内容全部拼进一份文本，便于一键 Ctrl+V
 */
export async function buildPasteMarkdown(projectId, project) {
  const res = await getProjectContext(projectId)
  const data = res.data || {}
  const artifacts = data.artifacts || []
  const name = (project && project.projectName) || data.projectName || '未命名项目'
  const lines = []
  lines.push(`# ${name}`)
  lines.push('')
  lines.push('> 由 devPivot 协同研发平台导出 · 项目开发上下文（可直接粘贴给 Claude Code / Codex / 任意终端 AI）')
  lines.push('')
  lines.push('## 项目概览')
  lines.push(`- 行业分类：${project && project.industryType ? project.industryType : '未填写'}`)
  lines.push(`- 目标用户：${project && project.targetUser ? project.targetUser : '未填写'}`)
  lines.push(`- 当前阶段：${project && project.step ? project.step : '未开始'}`)
  lines.push(`- 负责人：${project && project.assigneeName ? project.assigneeName : '未指定'}`)
  lines.push('')
  lines.push('## 各阶段产物')
  Object.keys(STEP_FILES).forEach(step => {
    const info = STEP_FILES[step]
    lines.push('')
    lines.push(`### ${info.title}`)
    lines.push('')
    lines.push(artifactText(artifacts, step))
  })
  return lines.join('\n')
}

/**
 * 复制可粘贴 Markdown 到剪贴板（终端/聊天框直接 Ctrl+V）
 * @returns {Promise<string>} 复制的文本内容
 */
export async function copyDevContextMarkdown(projectId, project) {
  const md = await buildPasteMarkdown(projectId, project)
  await copyText(md)
  return md
}

/**
 * 生成「服务器终端一行拉取约定文件」的 curl 命令，并复制到剪贴板
 * 命令形态：curl -s "{origin}{baseApi}/portal/project/{id}/context?fmt=agents&token=xxx" -o AGENTS.md
 * @param {string|number} projectId
 * @param {string} [fmt='agents'] 目标格式：agents|claude|cursor|trae|vscode
 * @returns {Promise<string>} 复制的 curl 命令
 */
export async function copyDevContextCurl(projectId, fmt = 'agents') {
  const res = await createExportToken(projectId)
  const token = (res.data && res.data.token) || res.token
  const cmd = buildCurlCommand(projectId, fmt, token)
  await copyText(cmd)
  return cmd
}

// 拼接 curl 命令（baseApi 取自 Vite 环境变量，与前端请求前缀一致）
function buildCurlCommand(projectId, fmt, token) {
  const baseApi = (import.meta.env && import.meta.env.VITE_APP_BASE_API) || ''
  const origin = (typeof window !== 'undefined' && window.location.origin) || ''
  const file = FMT_FILE[fmt] || 'AGENTS.md'
  const url = `${origin}${baseApi}/portal/project/${projectId}/context?fmt=${fmt}&token=${encodeURIComponent(token)}`
  return `curl -s "${url}" -o ${file}`
}

// 复制到剪贴板（优先 Clipboard API，非安全上下文回退到 execCommand）
async function copyText(text) {
  if (typeof navigator !== 'undefined' && navigator.clipboard && window.isSecureContext) {
    await navigator.clipboard.writeText(text)
    return
  }
  const ta = document.createElement('textarea')
  ta.value = text
  ta.style.position = 'fixed'
  ta.style.top = '-9999px'
  ta.style.opacity = '0'
  document.body.appendChild(ta)
  ta.focus()
  ta.select()
  try {
    document.execCommand('copy')
  } finally {
    document.body.removeChild(ta)
  }
}
