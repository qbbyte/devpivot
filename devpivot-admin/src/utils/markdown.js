/**
 * 统一 Markdown 渲染工具
 *
 * 为什么需要它：
 * 门户各步骤页（prd/db/tech/arch/clarify）曾各自维护一份手写的极简 Markdown 解析器，
 * 仅支持标题/列表/引用/粗体，导致表格、HTML 实体（&gt;）、代码块、链接等语法原样输出。
 * 此处收敛为单一真源，后续修 bug / 换引擎只改这一个文件。
 *
 * 技术选型：
 * - markdown-it：完整 GFM 支持（表格 / 删除线 / 任务列表 / 自动链接），插件生态好
 * - DOMPurify：因渲染结果通过 v-html 插入 DOM，且内容来自 AI 生成，必须做 XSS 过滤
 *
 * 关于 html: true
 * 设为 true 才能让 `&gt;` 这类实体被保留为实体、由浏览器正确解码显示为 `>`；
 * 若设为 false，源码里的 `&gt;` 会被二次转义成 `&amp;gt;`，界面上显示成字面量 `&gt;`。
 * 开启原始 HTML 的安全风险由 DOMPurify.sanitize() 兜底。
 */
import MarkdownIt from 'markdown-it'
import DOMPurify from 'dompurify'

/** 转义为 HTML 实体（纯文本/属性场景使用） */
export function escapeHtml(s) {
  return String(s ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function createEngine() {
  return new MarkdownIt({
    html: true, // 保留原始 HTML 与实体，安全由 DOMPurify 兜底
    linkify: true, // 自动把裸 URL 识别为链接
    typographer: false,
    breaks: false
  })
}

const baseEngine = createEngine()

/**
 * 支持 mermaid 的引擎：拦截 ```mermaid 代码块，
 * 输出占位 div（.arch-mermaid + data-mermaid），供 arch.vue 的 mermaid 初始化逻辑消费。
 * 占位结构必须与原手写实现保持一致，否则架构图的后续渲染会失效。
 */
const mermaidEngine = createEngine()
const defaultFence = mermaidEngine.renderer.rules.fence
mermaidEngine.renderer.rules.fence = function (tokens, idx, options, env, self) {
  const token = tokens[idx]
  if (token && token.info && token.info.trim() === 'mermaid') {
    const code = (token.content || '').trim()
    if (!code) return ''
    const safe = escapeHtml(code)
    return (
      '<div class="arch-mermaid" data-mermaid="' + safe + '">' +
      '<div class="mermaid-fallback">```mermaid\n' + safe + '\n```</div>' +
      '</div>'
    )
  }
  return defaultFence ? defaultFence(tokens, idx, options, env, self) : self.renderToken(tokens, idx, options)
}

/**
 * 渲染 Markdown 为可安全用于 v-html 的 HTML
 * @param {string} text Markdown 源文本
 * @param {{ mermaid?: boolean }} options mermaid=true 时把 ```mermaid 块转为占位 div
 * @returns {string} 已过滤的安全 HTML
 */
export function renderMarkdown(text, options = {}) {
  if (text === null || text === undefined || text === '') return ''
  try {
    const engine = options.mermaid ? mermaidEngine : baseEngine
    const raw = engine.render(String(text))
    return DOMPurify.sanitize(raw)
  } catch (e) {
    // 解析异常时降级为转义纯文本，避免整块内容白屏
    return escapeHtml(text)
  }
}

export default renderMarkdown
