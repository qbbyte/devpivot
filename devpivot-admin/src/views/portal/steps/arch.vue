<template>
  <div class="project-page">
    <header class="project-header">
      <div class="header-left">
        <button class="back-link" @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          <span>返回</span>
        </button>
        <span class="header-divider"></span>
        <span class="header-title">{{ project.projectName || '系统架构设计' }}</span>
        <span class="stage-pill"><span class="stage-dot"></span>系统架构</span>
      </div>
      <div class="header-right">
        <HistoryEntry :project-id="projectId" stage="ARCH" :snapshot="finalContent" />
        <template v-if="!readOnly">
          <el-button class="header-btn" @click="openGenerate">
            <el-icon><MagicStick /></el-icon>
            <span>{{ hasGenerated ? '重新生成' : 'AI 生成架构设计' }}</span>
          </el-button>
          <el-button class="header-btn" @click="handleSaveDraft">
            <el-icon><DocumentChecked /></el-icon>
            <span>保存草稿</span>
          </el-button>
          <el-button type="primary" class="header-btn submit-header-btn" :loading="submitting" :disabled="!canSubmit" @click="handleSubmit">
            <span>确认架构设计，进入技术方案</span>
            <el-icon class="el-icon--right"><ArrowRight /></el-icon>
          </el-button>
        </template>
        <el-tag v-else type="info" effect="plain" size="small" class="ro-tag">
          <el-icon><Lock /></el-icon>
          <span>只读 · 该阶段已完成</span>
        </el-tag>
      </div>
    </header>

    <main class="project-main">
      <div class="project-content" :class="{ 'is-reversed': reversed }">
        <!-- 左侧：架构设计文档 -->
        <div class="doc-pane" :style="{ width: splitPercent + '%' }"
          :class="{ 'swap-active': isSwapMode && draggingPane === 'doc' }">
          <div class="main-content">
            <section class="arch-section" @scroll="onDocScroll">
              <div class="section-header-row"
                @mousedown="onHeaderPointerDown($event, 'doc')"
                @touchstart.prevent="onHeaderPointerDown($event, 'doc')">
                <h3 class="section-title" :class="{ 'swap-hint': isSwapMode }">
                  <el-icon><Monitor /></el-icon>
                  <span>系统架构设计</span>
                  <el-tag v-if="isEditing" size="small" type="warning" effect="light" class="edit-tag">编辑中</el-tag>
                </h3>
                <div class="doc-actions">
                  <template v-if="readOnly">
                    <el-button text class="doc-action-btn" @click="toggleSwap">
                      <el-icon><Sort /></el-icon> 换位
                    </el-button>
                  </template>
                  <template v-else-if="isEditing">
                    <el-button text class="doc-action-btn" @click="cancelEdit">
                      <el-icon><Close /></el-icon> 取消
                    </el-button>
                    <el-button text type="primary" class="doc-action-btn" @click="saveEdit">
                      <el-icon><Check /></el-icon> 保存修改
                    </el-button>
                  </template>
                  <template v-else>
                    <el-button text class="doc-action-btn" @click="enterEdit">
                      <el-icon><EditPen /></el-icon> 编辑
                    </el-button>
                    <el-button text class="doc-action-btn" @click="toggleSwap">
                      <el-icon><Sort /></el-icon> 换位
                    </el-button>
                  </template>
                </div>
              </div>

              <div class="doc-content" :class="{ 'is-editing': isEditing }">
                <el-input
                  v-if="isEditing"
                  v-model="editContent"
                  type="textarea"
                  class="doc-editor"
                  resize="none"
                  spellcheck="false"
                  @keydown.ctrl.enter.prevent="saveEdit"
                />
                <div v-else-if="isGenerating && !finalContent" class="arch-generating">
                  <el-icon class="gen-spin" :size="26"><Loading /></el-icon>
                  <p class="gen-title">AI 正在生成系统架构设计…</p>
                  <p class="gen-sub">依据 PRD 与原型页面，产出模块划分 / 核心流程 / 接口契约 / 部署架构</p>
                </div>
                <div v-else-if="!finalContent.trim()" class="arch-empty">
                  <el-icon :size="52" color="var(--c-text-subtle, #94a3b8)"><Monitor /></el-icon>
                  <p class="empty-title">还没有系统架构设计</p>
                  <p class="empty-sub">点击「AI 生成架构设计」，基于 PRD 与原型一键产出系统架构（含架构图）</p>
                  <el-button v-if="!readOnly" type="primary" :loading="isGenerating" @click="openGenerate">
                    <el-icon><MagicStick /></el-icon> AI 生成架构设计
                  </el-button>
                </div>
                <div v-else ref="previewRef" class="markdown-body arch-markdown" v-html="renderedContent"></div>
                <div v-if="isGenerating && finalContent" class="gen-streaming-tip">
                  <el-icon class="gen-spin"><Loading /></el-icon>
                  <span>正在生成，架构图将在完成后渲染…</span>
                </div>
              </div>
            </section>
          </div>
        </div>

        <!-- 左右分隔条 -->
        <div class="divider-bar" :class="{ 'is-swap': isSwapMode }"
          @mousedown="onDividerDown($event)"
          @touchstart.prevent="onDividerDown($event)">
          <span class="divider-grip"></span>
        </div>

        <!-- 右侧：AI 对话 -->
        <aside class="chat-pane" :style="{ width: (100 - splitPercent) + '%' }"
          :class="{ 'swap-active': isSwapMode && draggingPane === 'chat' }">
          <div class="chat-header">
            <span class="chat-title"><el-icon><ChatDotRound /></el-icon> 架构咨询</span>
            <el-select v-model="chatModelCode" size="small" class="chat-model" :disabled="readOnly" @change="onSelectModel">
              <el-option v-for="m in modelOptions" :key="m.modelId" :label="m.modelName" :value="m.modelId" />
            </el-select>
          </div>
          <div ref="chatScrollRef" class="chat-messages">
            <div v-if="!chatMessages.length" class="chat-empty">
              <el-empty description="就架构设计问题咨询 AI，例如：模块怎么拆分？核心流程怎么设计？接口契约怎么定？" :image-size="72" />
            </div>
            <div v-for="(msg, i) in chatMessages" :key="i" class="chat-msg" :class="msg.role">
              <div class="msg-bubble" v-html="renderMsg(msg)"></div>
            </div>
            <div v-if="chatGenerating" class="chat-msg assistant">
              <div class="msg-bubble typing"><span class="dot"></span><span class="dot"></span><span class="dot"></span></div>
            </div>
          </div>
          <div class="chat-input-row">
            <el-input
              v-model="chatInput"
              type="textarea"
              :rows="2"
              resize="none"
              placeholder="输入架构问题，Enter 发送，Shift+Enter 换行"
              :disabled="chatGenerating || readOnly"
              @keydown.enter.exact.prevent="sendChat"
            />
            <el-button type="primary" class="chat-send" :loading="chatGenerating" :disabled="!chatInput.trim() || chatGenerating || readOnly" @click="sendChat">
              发送
            </el-button>
          </div>
        </aside>
      </div>
    </main>

    <!-- 生成参数弹窗 -->
    <el-dialog v-model="genDialogVisible" title="AI 生成系统架构设计" width="520px" append-to-body>
      <el-form label-position="top">
        <el-form-item label="生成模型">
          <el-select v-model="genModel" style="width:100%">
            <el-option v-for="m in modelOptions" :key="m.modelId" :label="m.modelName" :value="m.modelId" />
          </el-select>
        </el-form-item>
        <el-form-item label="补充要求（可选，如：需支持多租户 / 高并发 / 微服务拆分约束）">
          <el-input v-model="genExtraReq" type="textarea" :rows="3" maxlength="4000" placeholder="可留空，将依据 PRD 与原型自动生成" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="genDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="isGenerating" @click="startGenerate">开始生成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="StepArch">
import { ref, computed, watch, nextTick, onMounted, onBeforeUnmount, getCurrentInstance } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getProject } from '@/api/ai/project'
import { getArchModels, getArchDoc, saveArchDoc, submitArch, generateArch, sendArchChat } from '@/api/ai/arch'
import HistoryEntry from '@/views/portal/components/HistoryEntry.vue'

const { proxy } = getCurrentInstance()
const router = useRouter()
const route = useRoute()
const projectId = computed(() => route.params.id)

const loading = ref(false)
const project = ref({})
const currentStep = ref('ARCH')
const submitting = ref(false)

// 阶段已"过去"判定：项目当前阶段在我这一阶之后 → 整页只读锁定
const readOnly = computed(() => {
  const order = ['REQ', 'CLARIFY', 'PRD', 'PROTO', 'ARCH', 'TECH', 'DB', 'DONE']
  const cur = order.indexOf(currentStep.value)
  const mine = order.indexOf('ARCH')
  return cur > mine
})

const modelOptions = ref([])
const finalContent = ref('')
const isGenerating = ref(false)
const hasGenerated = ref(false)
const isEditing = ref(false)
const editContent = ref('')
const previewRef = ref(null)

const genDialogVisible = ref(false)
const genModel = ref('')
const genExtraReq = ref('')

// 拖拽分栏
const splitPercent = ref(52)
const reversed = ref(false)
const dragging = ref(false)
const isSwapMode = ref(false)
const draggingPane = ref('')

// 对话
const chatModelCode = ref('')
const chatInput = ref('')
const chatMessages = ref([])
const chatGenerating = ref(false)
const chatScrollRef = ref(null)
let chatController = null

const canSubmit = computed(() => finalContent.value.trim().length > 0 && !isGenerating.value)

const renderedContent = computed(() => renderMarkdown(finalContent.value, true))

watch(readOnly, (ro) => { if (ro) isEditing.value = false })

watch(finalContent, () => {
  if (!isGenerating.value) {
    nextTick(() => renderMermaidBlocks())
  }
})

/* ============================ 初始化 ============================ */
function goBack() { router.push('/portal') }

function getProjectInfo() {
  loading.value = true
  getProject(projectId.value).then(response => {
    project.value = response.data || {}
    currentStep.value = response.data?.step || 'ARCH'
    loading.value = false
  }).catch(() => { loading.value = false })
}

function loadModels() {
  getArchModels().then(res => {
    modelOptions.value = res.models || []
    genModel.value = modelOptions.value.length ? modelOptions.value[0].modelId : ''
    if (!chatModelCode.value && modelOptions.value.length) {
      chatModelCode.value = modelOptions.value[0].modelId
    }
  }).catch(() => {})
}

function loadDoc() {
  getArchDoc(projectId.value).then(res => {
    const doc = res
    if (doc && doc.content) {
      finalContent.value = doc.content
      hasGenerated.value = true
    }
  }).catch(() => {})
}

onMounted(() => {
  getProjectInfo()
  loadModels()
  loadDoc()
})
onBeforeUnmount(() => { if (chatController) chatController.stop() })

/* ============================ 生成 ============================ */
function openGenerate() {
  if (readOnly.value) return
  genExtraReq.value = ''
  genDialogVisible.value = true
}

function startGenerate() {
  if (readOnly.value || isGenerating.value) return
  if (!genModel.value) { proxy.$modal.msgWarning('请选择生成模型'); return }
  genDialogVisible.value = false
  hasGenerated.value = true
  isGenerating.value = true
  finalContent.value = ''
  generateArch(
    {
      projectId: projectId.value,
      projectName: project.value.projectName,
      model: genModel.value,
      extraReq: genExtraReq.value
    },
    {
      onChunk: text => { finalContent.value = text },
      onDone: () => {
        isGenerating.value = false
        if (finalContent.value.trim()) {
          saveArchDoc({ projectId: projectId.value, content: finalContent.value, docName: '系统架构设计', sourceModel: genModel.value, status: '0' })
            .then(() => proxy.$modal.msgSuccess('已生成并保存草稿'))
            .catch(() => {})
          nextTick(() => renderMermaidBlocks())
        }
      },
      onError: err => {
        isGenerating.value = false
        proxy.$modal.msgError('生成失败：' + (err && err.message ? err.message : '未知错误'))
      }
    }
  )
}

/* ============================ 编辑 / 保存 / 提交 ============================ */
function enterEdit() {
  if (readOnly.value) return
  editContent.value = finalContent.value
  isEditing.value = true
}
function cancelEdit() { isEditing.value = false }
function saveEdit() {
  finalContent.value = editContent.value
  isEditing.value = false
  handleSaveDraft()
}

function handleSaveDraft() {
  if (!finalContent.value.trim()) { proxy.$modal.msgWarning('暂无可保存的内容'); return }
  saveArchDoc({ projectId: projectId.value, content: finalContent.value, docName: '系统架构设计', sourceModel: genModel.value || '', status: '0' })
    .then(() => proxy.$modal.msgSuccess('草稿已保存'))
    .catch(() => {})
}

function handleSubmit() {
  if (!canSubmit.value) return
  proxy.$modal.confirm('确认后架构设计将锁定并进入技术方案阶段，确定提交？').then(() => {
    submitting.value = true
    submitArch(projectId.value, { content: finalContent.value, docName: '系统架构设计', sourceModel: genModel.value || '' })
      .then(() => {
        proxy.$modal.msgSuccess('已确认系统架构设计')
        router.push('/portal')
      })
      .catch(() => {})
      .finally(() => { submitting.value = false })
  }).catch(() => {})
}

/* ============================ AI 对话 ============================ */
function onSelectModel(val) {
  chatModelCode.value = val || chatModelCode.value
}

function sendChat() {
  const msg = chatInput.value.trim()
  if (!msg || chatGenerating.value) return
  chatInput.value = ''
  chatMessages.value.push({ role: 'user', content: msg })
  chatGenerating.value = true
  scrollChat()
  const assistant = { role: 'assistant', content: '' }
  chatMessages.value.push(assistant)
  chatController = sendArchChat(
    { message: msg, model: chatModelCode.value },
    {
      onChunk: text => { assistant.content = text; scrollChat() },
      onDone: () => { chatGenerating.value = false; scrollChat() },
      onError: err => {
        chatGenerating.value = false
        assistant.content = '（对话失败：' + (err && err.message ? err.message : '未知错误') + '）'
        scrollChat()
      }
    }
  )
}

function renderMsg(msg) {
  if (msg.role === 'assistant') return renderMarkdown(msg.content, false)
  return String(msg.content || '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/\n/g, '<br/>')
}

function scrollChat() {
  nextTick(() => {
    if (chatScrollRef.value) chatScrollRef.value.scrollTop = chatScrollRef.value.scrollHeight
  })
}

/* ============================ 分栏 / 换位 ============================ */
function onDividerDown(e) {
  dragging.value = true
  draggingPane.value = ''
  const move = (ev) => {
    const rect = document.querySelector('.project-content')
    if (!rect) return
    const r = rect.getBoundingClientRect()
    const pct = ((ev.clientX - r.left) / r.width) * 100
    splitPercent.value = Math.min(70, Math.max(30, pct))
  }
  const up = () => {
    dragging.value = false
    document.removeEventListener('mousemove', move)
    document.removeEventListener('mouseup', up)
  }
  document.addEventListener('mousemove', move)
  document.addEventListener('mouseup', up)
}
function onHeaderPointerDown(e, pane) {
  isSwapMode.value = true
  draggingPane.value = pane
}
function onDocScroll() { }
function toggleSwap() { reversed.value = !reversed.value; isSwapMode.value = false }

/* ============================ Markdown + Mermaid 渲染 ============================ */
let mermaidInstance = null
let mermaidReady = null
function ensureMermaid() {
  if (!mermaidReady) {
    mermaidReady = import('mermaid').then(m => {
      const mm = m.default || m
      mm.initialize({
        startOnLoad: false,
        theme: 'neutral',
        securityLevel: 'loose',
        fontFamily: 'inherit'
      })
      return mm
    }).catch(err => {
      console.warn('[arch] mermaid 加载失败：', err && err.message)
      return null
    })
  }
  return mermaidReady
}

function escapeHtml(s) {
  return String(s || '')
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

/** 轻量 Markdown 渲染：标题/列表/表格/代码块/粗体/行内码；```mermaid 块转为占位 div 待渲染 */
function renderMarkdown(text, withMermaid) {
  let t = String(text || '')
  // mermaid 代码块优先提取为占位
  if (withMermaid) {
    t = t.replace(/```mermaid\s*\n?([\s\S]*?)```/g, (all, code) => {
      return `<div class="arch-mermaid" data-mermaid="${escapeHtml(code.trim())}"><div class="mermaid-fallback">\`\`\`mermaid\n${escapeHtml(code.trim())}\n\`\`\`</div></div>`
    })
  }
  const lines = t.split('\n')
  let html = ''
  let inCode = false
  let codeBuf = []
  let inTable = false
  let tableBuf = []
  const flushCode = () => {
    if (codeBuf.length) {
      html += `<pre class="arch-code">${escapeHtml(codeBuf.join('\n'))}</pre>`
      codeBuf = []
    }
  }
  const flushTable = () => {
    if (tableBuf.length) {
      const rows = tableBuf
      html += '<table class="arch-table">'
      rows.forEach((row, i) => {
        const cells = row.split('|').map(c => c.trim()).filter((_, idx, arr) => !(idx === 0 && arr.length > 2 && !c))
        const tag = i === 0 ? 'th' : 'td'
        html += '<tr>' + cells.map(c => `<${tag}>${inlineMd(c)}</${tag}>`).join('') + '</tr>'
      })
      html += '</table>'
      tableBuf = []
    }
  }
  for (const line of lines) {
    if (line.trim().startsWith('```') && !inCode) {
      flushTable()
      inCode = true
      codeBuf = [line.replace(/^```\w*/, '')]
      continue
    }
    if (line.trim() === '```' && inCode) {
      flushCode()
      inCode = false
      continue
    }
    if (inCode) { codeBuf.push(line); continue }
    if (line.trim().startsWith('|')) {
      if (!inTable) { inTable = true; tableBuf = [] }
      tableBuf.push(line)
      continue
    }
    if (inTable) { flushTable(); inTable = false }
    if (line.trim() === '') { html += ''; continue }
    if (/^#{1,6}\s/.test(line)) {
      const level = line.match(/^#{1,6}/)[0].length
      html += `<h${Math.min(level, 4)} class="arch-h${level}">${inlineMd(line.replace(/^#{1,6}\s*/, ''))}</h${Math.min(level, 4)}>`
    } else if (/^\s*[-*]\s+/.test(line)) {
      html += `<li class="arch-li">${inlineMd(line.replace(/^\s*[-*]\s+/, ''))}</li>`
    } else if (/^\s*\d+\.\s+/.test(line)) {
      html += `<li class="arch-li">${inlineMd(line.replace(/^\s*\d+\.\s+/, ''))}</li>`
    } else {
      html += `<p class="arch-p">${inlineMd(line)}</p>`
    }
  }
  flushCode()
  flushTable()
  return html
}

function inlineMd(s) {
  return escapeHtml(s)
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
}

/** 渲染所有未渲染的 mermaid 占位块；失败保留代码块回退 */
async function renderMermaidBlocks() {
  const root = previewRef.value
  if (!root) return
  const blocks = root.querySelectorAll('.arch-mermaid')
  if (!blocks.length) return
  const mm = await ensureMermaid()
  let seq = 0
  for (const el of blocks) {
    const code = el.getAttribute('data-mermaid')
    if (!code || el.querySelector('svg')) continue
    if (!mm) continue
    const id = 'arch-mmd-' + Date.now() + '-' + seq++
    try {
      const { svg } = await mm.render(id, code)
      el.innerHTML = svg
    } catch (e) {
      console.warn('[arch] mermaid 渲染失败，保留代码块', e)
    }
  }
}
</script>

<style scoped>
.project-page { height: 100vh; display: flex; flex-direction: column; background: var(--c-bg, #f5f8fd); }
.project-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 20px; height: 56px; flex-shrink: 0;
  background: var(--c-surface, #fff); border-bottom: 1px solid var(--c-border, #e2e8f0);
}
.header-left { display: flex; align-items: center; gap: 10px; min-width: 0; }
.back-link { display: inline-flex; align-items: center; gap: 4px; border: none; background: none; cursor: pointer; font-size: 13px; color: var(--c-text-muted, #64748b); padding: 4px 6px; border-radius: 6px; }
.back-link:hover { color: var(--c-primary, #2563eb); background: var(--c-primary-bg, #eff4ff); }
.header-divider { width: 1px; height: 18px; background: var(--c-border, #e2e8f0); }
.header-title { font-size: 15px; font-weight: 500; color: var(--c-text, #1e293b); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.stage-pill { display: inline-flex; align-items: center; gap: 5px; font-size: 11px; color: var(--c-primary, #2563eb); background: var(--c-primary-bg, #eff4ff); padding: 3px 10px; border-radius: 999px; }
.stage-dot { width: 6px; height: 6px; border-radius: 50%; background: var(--c-primary, #2563eb); }
.header-right { display: flex; align-items: center; gap: 8px; }
.header-btn { margin-left: 0; }
.ro-tag { margin-left: 8px; }

.project-main { flex: 1; min-height: 0; display: flex; }
.project-content { display: flex; width: 100%; height: 100%; }
.doc-pane { min-width: 0; height: 100%; overflow: hidden; display: flex; flex-direction: column; transition: order 0.2s; }
.chat-pane { min-width: 0; height: 100%; display: flex; flex-direction: column; background: var(--c-surface, #fff); border-left: 1px solid var(--c-border, #e2e8f0); transition: order 0.2s; }
.is-reversed .doc-pane { order: 2; }
.is-reversed .chat-pane { order: 1; border-left: none; border-right: 1px solid var(--c-border, #e2e8f0); }
.divider-bar { width: 6px; cursor: col-resize; background: transparent; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.divider-bar:hover { background: var(--c-primary-bg, #eff4ff); }
.divider-grip { width: 2px; height: 40px; border-radius: 2px; background: var(--c-border, #e2e8f0); }

.main-content { flex: 1; min-height: 0; overflow: auto; padding: 16px 24px; }
.arch-section { max-width: 860px; margin: 0 auto; }
.section-header-row { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; }
.section-title { display: flex; align-items: center; gap: 8px; font-size: 15px; font-weight: 500; color: var(--c-text, #1e293b); margin: 0; }
.doc-actions { display: flex; gap: 4px; }
.edit-tag { margin-left: 4px; }
.doc-content { position: relative; }
.doc-editor { font-family: var(--font-mono, Consolas, monospace); font-size: 13px; line-height: 1.7; min-height: 60vh; }
.markdown-body { padding-bottom: 40px; }
.arch-h1 { font-size: 20px; margin: 18px 0 10px; color: var(--c-text, #1e293b); }
.arch-h2 { font-size: 17px; margin: 16px 0 8px; color: var(--c-text, #1e293b); }
.arch-h3 { font-size: 15px; margin: 14px 0 6px; color: var(--c-text, #1e293b); }
.arch-h4 { font-size: 14px; margin: 12px 0 6px; color: var(--c-text, #1e293b); }
.arch-p { font-size: 13px; line-height: 1.75; color: var(--c-text, #1e293b); margin: 6px 0; }
.arch-li { font-size: 13px; line-height: 1.7; color: var(--c-text, #1e293b); margin-left: 18px; }
.arch-code { background: var(--c-border-light, #f1f5f9); border-radius: 8px; padding: 10px 12px; font-size: 12px; line-height: 1.6; overflow-x: auto; color: var(--c-text, #1e293b); }
.arch-table { border-collapse: collapse; margin: 8px 0; width: 100%; font-size: 12px; }
.arch-table th, .arch-table td { border: 1px solid var(--c-border, #e2e8f0); padding: 6px 10px; text-align: left; }
.arch-table th { background: var(--c-border-light, #f1f5f9); font-weight: 500; }
.arch-mermaid { margin: 12px 0; background: var(--c-surface, #fff); border: 1px solid var(--c-border, #e2e8f0); border-radius: 10px; padding: 12px; overflow-x: auto; }
.mermaid-fallback { font-family: var(--font-mono, Consolas, monospace); font-size: 12px; white-space: pre; color: var(--c-text-muted, #64748b); }

.arch-empty { display: flex; flex-direction: column; align-items: center; gap: 8px; padding: 72px 20px; }
.arch-generating { display: flex; flex-direction: column; align-items: center; gap: 10px; padding: 72px 20px; }
.empty-title { font-size: 15px; font-weight: 500; color: var(--c-text, #1e293b); margin: 6px 0 0; }
.empty-sub { font-size: 12px; color: var(--c-text-muted, #64748b); margin: 0 0 10px; text-align: center; }
.gen-title { font-size: 14px; font-weight: 500; color: var(--c-text, #1e293b); margin: 8px 0 0; }
.gen-sub { font-size: 12px; color: var(--c-text-muted, #64748b); margin: 0; }
.gen-spin { animation: spin 1.2s linear infinite; color: var(--c-primary, #2563eb); }
@keyframes spin { to { transform: rotate(360deg); } }
.gen-streaming-tip { display: inline-flex; align-items: center; gap: 6px; font-size: 12px; color: var(--c-text-muted, #64748b); margin-top: 8px; }

.chat-header { display: flex; align-items: center; justify-content: space-between; padding: 12px 16px; border-bottom: 1px solid var(--c-border, #e2e8f0); }
.chat-title { display: inline-flex; align-items: center; gap: 6px; font-size: 13px; font-weight: 500; color: var(--c-text, #1e293b); }
.chat-model { width: 160px; }
.chat-messages { flex: 1; min-height: 0; overflow-y: auto; padding: 16px; }
.chat-empty { padding-top: 40px; }
.chat-msg { margin-bottom: 12px; display: flex; }
.chat-msg.user { justify-content: flex-end; }
.msg-bubble { max-width: 88%; padding: 9px 12px; border-radius: 10px; font-size: 13px; line-height: 1.65; word-break: break-word; }
.chat-msg.user .msg-bubble { background: var(--c-primary, #2563eb); color: #fff; border-bottom-right-radius: 3px; }
.chat-msg.assistant .msg-bubble { background: var(--c-border-light, #f1f5f9); color: var(--c-text, #1e293b); border-bottom-left-radius: 3px; }
.chat-msg.assistant .msg-bubble :deep(p) { margin: 4px 0; }
.chat-msg.assistant .msg-bubble :deep(pre) { background: var(--c-surface, #fff); border: 1px solid var(--c-border, #e2e8f0); border-radius: 6px; padding: 8px; overflow-x: auto; font-size: 12px; }
.typing { display: inline-flex; gap: 4px; align-items: center; }
.typing .dot { width: 6px; height: 6px; border-radius: 50%; background: var(--c-text-subtle, #94a3b8); animation: blink 1.2s infinite; }
.typing .dot:nth-child(2) { animation-delay: 0.2s; }
.typing .dot:nth-child(3) { animation-delay: 0.4s; }
@keyframes blink { 0%, 80%, 100% { opacity: 0.3; } 40% { opacity: 1; } }
.chat-input-row { display: flex; gap: 8px; padding: 12px 16px; border-top: 1px solid var(--c-border, #e2e8f0); align-items: flex-end; }
.chat-input-row .el-textarea { flex: 1; }
.chat-send { flex-shrink: 0; }
</style>
