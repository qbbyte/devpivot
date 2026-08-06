<template>
  <div class="project-page" :class="{ 'is-swap-mode': isSwapMode }">
    <header class="project-header">
      <div class="header-left">
        <button class="back-link" @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          <span>返回</span>
        </button>
        <span class="header-divider"></span>
        <span class="header-title">{{ project.projectName || 'PRD 文档' }}</span>
      </div>
      <div class="header-right">
        <el-button class="save-btn" @click="handleSave">
          <el-icon><DocumentChecked /></el-icon>
          <span>保存草稿</span>
        </el-button>
      </div>
    </header>

    <main class="project-main">
      <div class="project-content" :class="{ 'is-reversed': reversed }">
        <!-- 左侧：文档区域 -->
        <div class="doc-pane" :style="{ width: splitPercent + '%' }"
          :class="{ 'swap-active': isSwapMode && draggingPane === 'doc' }">
          <div class="main-content">
            <section class="prd-section">
              <div class="section-header-row"
                @mousedown="onHeaderPointerDown($event, 'doc')"
                @touchstart.prevent="onHeaderPointerDown($event, 'doc')">
                <h3 class="section-title" :class="{ 'swap-hint': isSwapMode }">
                  <el-icon><Notebook /></el-icon>
                  <span>PRD 文档</span>
                </h3>
                <el-button type="primary" size="default" class="submit-btn-inline" :loading="submitting" @click="handleSubmit">
                  <span>确认 PRD，进入下一阶段</span>
                  <el-icon class="el-icon--right"><ArrowRight /></el-icon>
                </el-button>
              </div>

              <div class="prd-content">
                <div v-if="!docContent && !isGenerating" class="prd-empty">
                  <el-icon :size="32" color="#c0c4cc"><DocumentAdd /></el-icon>
                  <p class="prd-empty-title">尚未生成 PRD 文档</p>
                  <p class="prd-empty-desc">选择模板与生成模式后，点击「开始生成」，AI 将基于需求基线撰写产品需求文档</p>
                </div>
                <div v-else ref="previewRef" class="markdown-body" v-html="renderedContent"></div>
                <div v-if="isGenerating" class="generating-tip">
                  <el-icon class="rotating"><Loading /></el-icon>
                  <span>AI 正在撰写中…</span>
                </div>
              </div>
            </section>
          </div>
        </div>

        <!-- 中间：可拖拽分隔条 -->
        <div class="split-divider" @mousedown="startDrag">
        </div>

        <!-- 右侧：对话区域 -->
        <div class="chat-pane"
          :class="{ 'swap-active': isSwapMode && draggingPane === 'chat' }">
          <div class="chat-card">
            <div class="chat-header"
              @mousedown="onHeaderPointerDown($event, 'chat')"
              @touchstart.prevent="onHeaderPointerDown($event, 'chat')">
              <div class="chat-header-left">
                <div class="chat-header-title" :class="{ 'swap-hint': isSwapMode }">
                  <el-icon><ChatDotRound /></el-icon>
                  <span>AI 对话</span>
                </div>
                <span class="chat-header-sub">针对 PRD 提问、补充或修订</span>
              </div>
              <el-dropdown class="chat-model" trigger="click" @command="onSelectModel">
                <span class="model-chip" @mousedown.stop @touchstart.stop>
                  {{ chatModel.label }}
                  <el-icon class="model-caret"><ArrowDown /></el-icon>
                </span>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item
                      v-for="m in modelOptions"
                      :key="m.value"
                      :command="m.value"
                      :class="{ 'is-selected': m.value === chatModel.value }"
                    >
                      <span class="model-dot" :class="{ on: m.value === chatModel.value }"></span>
                      {{ m.label }}
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>

            <div ref="chatScrollRef" class="chat-scroll">
              <div v-if="!chatMessages.length && !chatGenerating" class="chat-empty">
                <el-icon :size="28" color="#c0c4cc"><ChatLineSquare /></el-icon>
                <p class="chat-empty-title">开始与 AI 讨论 PRD</p>
                <p class="chat-empty-desc">提出需求疑问、补充约束或修改建议，AI 会结合当前文档给出建议</p>
              </div>
              <div v-for="(msg, idx) in chatMessages" :key="idx" :class="['chat-msg', msg.role]">
                <!-- AI：头像在左 -->
                <template v-if="msg.role === 'ai'">
                  <div class="msg-avatar msg-avatar-ai">
                    <el-icon><Cpu /></el-icon>
                  </div>
                  <div class="msg-bubble md">
                    <!-- 内容为空且仍在生成中 → 显示思考提示 -->
                    <div v-if="!msg.content && chatGenerating" class="thinking-inline">
                      <el-icon class="rotating"><Loading /></el-icon>
                      <span>AI 正在思考…</span>
                    </div>
                    <div v-else class="markdown-body" v-html="formatMarkdown(msg.content)"></div>
                  </div>
                </template>
                <!-- 用户：气泡在左、头像在右 -->
                <template v-else>
                  <div class="msg-bubble">
                    <div class="msg-text">{{ msg.content }}</div>
                  </div>
                  <div class="msg-avatar msg-avatar-user">
                    <el-icon><UserFilled /></el-icon>
                  </div>
                </template>
              </div>
            </div>

            <div class="chat-input">
              <div class="input-wrap">
                <el-input
                  v-model="chatInput"
                  type="textarea"
                  :rows="2"
                  resize="none"
                  :disabled="chatGenerating"
                  placeholder="针对 PRD 提问、补充需求或修改建议…"
                  @keydown.enter.exact.prevent="sendChat"
                />
                <!-- 输入框右下角：增强提示词 -->
                <el-tooltip content="增强提示词" placement="top">
                  <button
                    class="enhance-btn"
                    type="button"
                    :class="{ active: enhancing }"
                    :disabled="enhancing || chatGenerating || !chatInput.trim()"
                    @click="enhancePrompt"
                  >
                    <el-icon class="enhance-icon" :class="{ rotating: enhancing }">
                      <MagicStick />
                    </el-icon>
                  </button>
                </el-tooltip>
              </div>
              <el-button
                type="primary"
                class="chat-send"
                :disabled="chatGenerating || !chatInput.trim()"
                @click="sendChat"
              >
                <el-icon><Promotion /></el-icon>
                <span>发送</span>
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup name="StepPrd">
import { ref, computed, onMounted, getCurrentInstance } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  ArrowLeft,
  DocumentChecked,
  Notebook,
  Loading,
  DocumentAdd,
  ArrowRight,
  ChatDotRound,
  ChatLineSquare,
  Cpu,
  Promotion,
  ArrowDown,
  UserFilled,
  MagicStick
} from '@element-plus/icons-vue'
import { getProject, updateProject } from '@/api/ai/project'
import { generatePrd } from '@/api/ai/doc'
import { sendChatMessage } from '@/api/ai/chat'

const { proxy } = getCurrentInstance()
const router = useRouter()
const route = useRoute()
const projectId = computed(() => route.params.id)

const stepOrder = [
  { value: 'REQ', label: '需求采集' },
  { value: 'CLARIFY', label: 'AI 澄清' },
  { value: 'PRD', label: 'PRD 文档' },
  { value: 'PROTO', label: '原型设计' },
  { value: 'TECH', label: '技术方案' },
  { value: 'DB', label: '数据库' },
  { value: 'DONE', label: '完成' }
]

const loading = ref(false)
const submitting = ref(false)
const project = ref({})
const currentStep = ref('PRD')

const stepIndex = computed(() => stepOrder.findIndex(s => s.value === currentStep.value))

// ---- PRD 生成（mock 实现与预留接口均在 @/api/ai/doc 的 generatePrd）----
const templateType = ref('STANDARD')
const genMode = ref('single')
const docContent = ref('')
const isGenerating = ref(false)
const previewRef = ref(null)
let genController = null

// ---- 右侧 AI 对话（mock 实现与预留接口均在 @/api/ai/chat 的 sendChatMessage）----
const chatMessages = ref([])
const chatInput = ref('')
const chatGenerating = ref(false)
const enhancing = ref(false)
const chatScrollRef = ref(null)
let chatController = null

// 当前对话模型（mock 占位，后端就绪后从接口返回真实值）
const modelOptions = [
  { value: 'gpt-4o', label: 'GPT-4o' },
  { value: 'claude-3.5-sonnet', label: 'Claude 3.5 Sonnet' },
  { value: 'deepseek-chat', label: 'DeepSeek Chat' },
  { value: 'qwen-max', label: '通义千问 Max' }
]
const chatModel = ref(modelOptions[0])
function onSelectModel(val) {
  const m = modelOptions.find(o => o.value === val)
  if (m) chatModel.value = m
}

// ---- 左右分栏拖拽 + 长按交换 ----
const splitPercent = ref(50)
const reversed = ref(false)

// 交换模式状态
const isSwapMode = ref(false)       // 是否处于可交换模式
const draggingPane = ref(null)      // 当前正在拖动的面板 ('doc' | 'chat')
const longPressTimer = ref(null)    // 长按计时器
const LONG_PRESS_MS = 350           // 长按触发阈值(ms)

/** 标题栏按下 → 开始长按检测 */
function onHeaderPointerDown(e, pane) {
  // 如果已经在交换模式下，直接开始拖拽
  if (isSwapMode.value) {
    startPaneDrag(e, pane)
    return
  }

  const startX = e.clientX ?? (e.touches && e.touches[0].clientX)
  const startY = e.clientY ?? (e.touches && e.touches[0].clientY)

  longPressTimer.value = setTimeout(() => {
    isSwapMode.value = true
    draggingPane.value = pane
    document.body.style.cursor = 'grabbing'
    startPaneDrag(e, pane)
  }, LONG_PRESS_MS)

  // 用户提前松开 → 取消长按
  function onUp(ev) {
    clearTimeout(longPressTimer.value)
    longPressTimer.value = null
    document.removeEventListener('mouseup', onUp)
    document.removeEventListener('touchend', onUp)
    document.removeEventListener('touchmove', onMove)
  }
  // 用户移动超过阈值 → 取消长按（视为滚动/普通操作）
  function onMove(ev) {
    const cx = ev.clientX ?? (ev.touches && ev.touches[0].clientX)
    const cy = ev.clientY ?? (ev.touches && ev.touches[0].clientY)
    if (Math.abs(cx - startX) > 8 || Math.abs(cy - startY) > 8) {
      clearTimeout(longPressTimer.value)
      longPressTimer.value = null
      document.removeEventListener('mousemove', onMove)
    }
  }
  document.addEventListener('mouseup', onUp)
  document.addEventListener('touchend', onUp)
  document.addEventListener('mousemove', onMove)
  document.addEventListener('touchmove', onMove)
}

/** 开始拖动面板进行位置交换 */
function startPaneDrag(e, pane) {
  draggingPane.value = pane
  const containerEl = document.querySelector('.project-content')
  const rect = containerEl.getBoundingClientRect()
  const startX = e.clientX ?? (e.touches && e.touches[0].clientX)

  function onMove(ev) {
    const cx = ev.clientX ?? (ev.touches && ev.touches[0].clientX)
    const relX = cx - rect.left
    const mid = rect.width / 2

    // 拖过中线 → 切换顺序
    const shouldReverse = (pane === 'doc' ? relX > mid : relX < mid)
    if (shouldReverse !== reversed.value) {
      reversed.value = shouldReverse
    }
  }

  function onUp() {
    draggingPane.value = null
    isSwapMode.value = false
    document.body.style.cursor = ''
    document.body.style.userSelect = ''
    document.removeEventListener('mousemove', onMove)
    document.removeEventListener('mouseup', onUp)
    document.removeEventListener('touchmove', onMove)
    document.removeEventListener('touchend', onUp)
  }

  document.addEventListener('mousemove', onMove)
  document.addEventListener('mouseup', onUp)
  document.addEventListener('touchmove', onMove)
  document.addEventListener('touchend', onUp)
  document.body.style.cursor = 'grabbing'
  document.body.style.userSelect = 'none'
}

/** 分隔条拖拽调整宽度 */
function startDrag(e) {
  if (isSwapMode.value) return   // 交换模式下禁用宽度调整
  e.preventDefault()
  const divider = e.currentTarget
  const container = divider.parentElement
  const rect = container.getBoundingClientRect()
  const startX = e.clientX
  const startPercent = splitPercent.value
  const reverse = reversed.value
  divider.classList.add('dragging')
  function onMove(ev) {
    let delta = ((ev.clientX - startX) / rect.width) * 100
    if (reverse) delta = -delta
    let np = startPercent + delta
    np = Math.max(20, Math.min(80, np))
    splitPercent.value = np
  }
  function onUp() {
    document.removeEventListener('mousemove', onMove)
    document.removeEventListener('mouseup', onUp)
    document.body.style.cursor = ''
    document.body.style.userSelect = ''
    divider.classList.remove('dragging')
  }
  document.addEventListener('mousemove', onMove)
  document.addEventListener('mouseup', onUp)
  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
}

const renderedContent = computed(() => formatMarkdown(docContent.value))
const wordCount = computed(() => docContent.value.replace(/[\s#*>`\-|]/g, '').length)

function draftKey() { return `prd_draft_${projectId.value}` }

function goBack() {
  router.push('/portal')
}

function getProjectInfo() {
  loading.value = true
  getProject(projectId.value).then(response => {
    project.value = response.data
    currentStep.value = response.data.step || 'PRD'
    loading.value = false
  }).catch(() => {
    loading.value = false
  })
}

function handleSave() {
  localStorage.setItem(draftKey(), JSON.stringify({
    templateType: templateType.value,
    genMode: genMode.value,
    content: docContent.value
  }))
  proxy.$modal.msgSuccess('草稿已保存到本地（后端就绪后将落库 ai_prd_doc）')
}

function handleSubmit() {
  if (!docContent.value.trim()) {
    proxy.$modal.msgWarning('请先生成 PRD 文档再提交')
    return
  }
  submitting.value = true
  const nextStep = stepOrder[stepIndex.value + 1]?.value || 'DONE'
  updateProject({ projectId: projectId.value, step: nextStep }).then(() => {
    proxy.$modal.msgSuccess('已提交')
    submitting.value = false
    router.push('/portal')
  }).catch(() => {
    submitting.value = false
  })
}

function loadDraft() {
  try {
    const raw = localStorage.getItem(draftKey())
    if (raw) {
      const d = JSON.parse(raw)
      if (d.templateType) templateType.value = d.templateType
      if (d.genMode) genMode.value = d.genMode
      if (d.content) docContent.value = d.content
    }
  } catch (e) { /* ignore */ }
}

// 轻量 Markdown 渲染（与项目现有 formatMessage 风格一致，扩展标题/列表/引用）
function escapeHtml(s) { return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;') }
function inlineMd(s) {
  return s
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
}
function formatMarkdown(md) {
  if (!md) return ''
  const lines = escapeHtml(md).split('\n')
  let html = ''
  let inUl = false, inOl = false
  const closeLists = () => {
    if (inUl) { html += '</ul>'; inUl = false }
    if (inOl) { html += '</ol>'; inOl = false }
  }
  for (const line of lines) {
    const h = line.match(/^(#{1,3})\s+(.*)$/)
    if (h) { closeLists(); const lvl = h[1].length; html += `<h${lvl}>${inlineMd(h[2])}</h${lvl}>`; continue }
    const bq = line.match(/^>\s?(.*)$/)
    if (bq) { closeLists(); html += `<blockquote>${inlineMd(bq[1])}</blockquote>`; continue }
    if (/^\s*[-*]\s+/.test(line)) {
      if (!inUl) { closeLists(); html += '<ul>'; inUl = true }
      if (inOl) { html += '</ol>'; inOl = false }
      html += `<li>${inlineMd(line.replace(/^\s*[-*]\s+/, ''))}</li>`; continue
    }
    if (/^\s*\d+\.\s+/.test(line)) {
      if (!inOl) { closeLists(); html += '<ol>'; inOl = true }
      if (inUl) { html += '</ul>'; inUl = false }
      html += `<li>${inlineMd(line.replace(/^\s*\d+\.\s+/, ''))}</li>`; continue
    }
    if (line.trim() === '') { closeLists(); continue }
    closeLists(); html += `<p>${inlineMd(line)}</p>`
  }
  closeLists()
  return html
}

function startGenerate() {
  if (isGenerating.value) return
  docContent.value = ''
  isGenerating.value = true
  // 预留接口：当前用前端 mock，后端就绪后 generatePrd 内部切换为真实流式请求（接口前缀不带 /api）
  genController = generatePrd(
    {
      projectId: projectId.value,
      projectName: project.value.projectName,
      industryType: project.value.industryType,
      targetUser: project.value.targetUser,
      templateType: templateType.value,
      mode: genMode.value
    },
    {
      onChunk: (txt) => { docContent.value = txt; scrollPreview() },
      onDone: () => { isGenerating.value = false; genController = null },
      onError: (err) => {
        isGenerating.value = false
        genController = null
        proxy.$modal.msgError('生成失败，请稍后重试')
        console.error(err)
      }
    }
  )
}

function stopGenerate() {
  if (genController) genController.stop()
  isGenerating.value = false
  genController = null
}

function scrollPreview() {
  requestAnimationFrame(() => {
    const el = previewRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

function scrollChatToBottom() {
  requestAnimationFrame(() => {
    const el = chatScrollRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

// 增强提示词：把简短/模糊输入扩写为结构化提问（当前前端 mock，后端就绪后改为调用 /ai/chat/enhance 真实接口，不加 /api 前缀）
function enhancePrompt() {
  const raw = chatInput.value.trim()
  if (!raw || enhancing.value || chatGenerating.value) return
  enhancing.value = true
  // 预留接口：后端就绪后改为 const res = await fetch('/ai/chat/enhance', {...}) 取增强结果
  setTimeout(() => {
    chatInput.value = buildEnhancedPrompt(raw)
    enhancing.value = false
    proxy.$modal.msgSuccess('已增强提示词')
  }, 450)
}

function buildEnhancedPrompt(raw) {
  // mock 规则：补充角色、上下文与目标，让提问更具可执行性
  const projectName = project.value.projectName || '本产品'
  return `作为资深产品经理，请结合「${projectName}」的 PRD，针对以下问题给出具体、可执行的建议：\n\n${raw}\n\n请重点回应：① 目标用户与核心场景；② 关键约束（性能/安全/兼容性）；③ 可量化的验收标准。`
}

function sendChat() {
  const q = chatInput.value.trim()
  if (!q || chatGenerating.value) return
  chatMessages.value.push({ role: 'user', content: q })
  chatInput.value = ''
  scrollChatToBottom()

  chatGenerating.value = true
  const aiMsg = { role: 'ai', content: '' }
  chatMessages.value.push(aiMsg)
  // 预留接口：当前用前端 mock，后端就绪后 sendChatMessage 内部切换为真实流式请求（接口前缀不带 /api）
  chatController = sendChatMessage(
    {
      projectId: projectId.value,
      projectName: project.value.projectName,
      question: q,
      docContent: docContent.value,
      model: chatModel.value.value
    },
    {
      onChunk: (txt) => { aiMsg.content = txt; scrollChatToBottom() },
      onDone: () => { chatGenerating.value = false; chatController = null; scrollChatToBottom() },
      onError: (err) => {
        chatGenerating.value = false
        chatController = null
        aiMsg.content = '抱歉，处理失败，请稍后重试。'
        proxy.$modal.msgError('对话请求失败，请稍后重试')
        console.error(err)
      }
    }
  )
}

onMounted(() => {
  getProjectInfo()
  loadDraft()
})
</script>

<style scoped>
.project-page {
  height: 100%;
  width: 100%;
  display: flex;
  flex-direction: column;
  background: #eff1f4;
  box-sizing: border-box;
  overflow: hidden;
}
.project-header {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 48px;
  padding: 0 12px;
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(228, 231, 235, 0.7);
  flex-shrink: 0;
}
.header-left { display: flex; align-items: center; gap: 10px; }
.back-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: none;
  background: none;
  color: #86909c;
  font-size: 13px;
  cursor: pointer;
  padding: 5px 10px;
  border-radius: 8px;
  transition: all 0.2s ease;
}
.back-link:hover { color: #3370ff; background: rgba(51, 112, 255, 0.07); }
.header-divider { width: 1px; height: 14px; background: #e5e6eb; }
.header-title { font-size: 14px; font-weight: 600; color: #1d2129; letter-spacing: 0.01em; }
.save-btn {
  border-radius: 8px;
  font-size: 13px;
  color: #4e5969;
  border-color: #c9cdd4;
  transition: all 0.22s ease;
}
.save-btn:hover { color: #3370ff; border-color: #3370ff; background: rgba(51, 112, 255, 0.04); }

/* ===== 主内容区：左右双栏（铺满整屏，各占 50%） ===== */
.project-main {
  flex: 1;
  min-height: 0;
  padding: 0;
  margin: 0;
  overflow: hidden;
  position: relative;
}
.project-content {
  height: 100%;
  width: 100%;
  margin: 0;
  padding: 0;
  display: flex;
  align-items: stretch;
  position: relative;
}
.project-content.is-reversed {
  flex-direction: row-reverse;
}

/* ===== 左侧文档面板 ===== */
.doc-pane {
  flex: 0 0 auto;
  min-width: 0;
  display: flex;
  flex-direction: column;
  overflow: visible;
  margin: 0;
  padding: 0;
}
.main-content {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.prd-section {
  flex: 1;
  min-height: 0;
  padding: 0 22px;
  background: #fff;
  border-radius: 0;
  box-shadow: none;
  border: none;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  transition: box-shadow 0.28s ease, transform 0.28s ease;
}
.section-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 0 0 14px;
  padding: 14px 0;
  border-bottom: 1px solid #f2f3f5;
}
.section-title {
  display: flex;
  align-items: center;
  gap: 9px;
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #1d2129;
}
.section-title .el-icon {
  width: 26px;
  height: 26px;
  border-radius: 8px;
  background: linear-gradient(135deg, #3370ff 0%, #5b8cff 100%);
  color: #fff;
  padding: 5px;
  box-shadow: 0 2px 8px rgba(51, 112, 255, 0.28);
}

/* 文档内容区 */
.prd-content {
  flex: 1;
  min-height: 0;
  position: relative;
  padding-top: 12px;
}
.prd-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 32px 20px;
  /* 空态背景装饰 */
  background: radial-gradient(ellipse at 50% 40%, rgba(51, 112, 255, 0.04) 0%, transparent 70%);
  border: 1px dashed #e5e6eb;
  border-radius: 12px;
}
.prd-empty :deep(.el-icon) { opacity: 0.45; }
.prd-empty-title {
  font-size: 14px;
  color: #4e5969;
  margin: 0;
  font-weight: 500;
}
.prd-empty-desc {
  font-size: 12px;
  color: #86909c;
  margin: 0;
  max-width: 300px;
  text-align: center;
  line-height: 1.65;
}

.markdown-body {
  padding: 2px 4px;
  font-size: 14px;
  line-height: 1.75;
  color: #1d2129;
}
.markdown-body h1 {
  font-size: 20px;
  margin: 4px 0 10px;
  padding-bottom: 8px;
  border-bottom: 2px solid #eef0f3;
  color: #1d2129;
}
.markdown-body h2 {
  font-size: 17px;
  margin: 18px 0 8px;
  color: #272e3b;
  font-weight: 600;
}
.markdown-body h3 {
  font-size: 15px;
  margin: 14px 0 6px;
  color: #333d4d;
  font-weight: 600;
}
.markdown-body p { margin: 6px 0; }
.markdown-body ul, .markdown-body ol { margin: 6px 0; padding-left: 20px; }
.markdown-body li { margin: 3px 0; }
.markdown-body blockquote {
  margin: 10px 0;
  padding: 8px 14px;
  background: linear-gradient(135deg, #f7f8fb 0%, #f0f2f5 100%);
  border-left: 3px solid #3370ff;
  color: #4e5969;
  border-radius: 0 8px 8px 0;
}
.markdown-body code {
  background: #f0f1f4;
  padding: 1px 6px;
  border-radius: 4px;
  font-size: 12.5px;
  color: #d6326e;
  font-family: 'SF Mono', Consolas, monospace;
}
.markdown-body strong { color: #1d2129; font-weight: 600; }

.generating-tip {
  position: absolute;
  right: 10px;
  bottom: 10px;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: #3370ff;
  background: rgba(51, 112, 255, 0.08);
  padding: 4px 12px;
  border-radius: 14px;
  font-weight: 500;
}
.rotating { animation: prd-spin 1s linear infinite; }
@keyframes prd-spin { to { transform: rotate(360deg); } }

/* 标题行内提交按钮 */
.submit-btn-inline {
  border-radius: 8px;
  padding: 8px 20px;
  font-size: 13px;
  font-weight: 500;
  background: linear-gradient(135deg, #3370ff 0%, #4880ff 100%);
  border: none;
  box-shadow: 0 2px 8px rgba(51, 112, 255, 0.25);
  flex-shrink: 0;
  transition: all 0.22s ease;
}
.submit-btn-inline:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(51, 112, 255, 0.35);
}

/* ===== 右侧对话区域 ===== */
.chat-pane {
  flex: 1 1 0;
  min-width: 0;
  display: flex;
  margin: 0;
  padding: 0;
}

/* ===== 中间可拖拽分隔条（精致窄版） ===== */
.split-divider {
  flex: 0 0 3px;
  width: 3px;
  cursor: col-resize;
  background: transparent;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s ease;
}
.split-divider::before {
  content: '';
  position: absolute;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 2px;
  height: 100%;
  border-radius: 2px;
  background: linear-gradient(180deg, #e3e4e9, #eaecf1 40%, #e3e4e9);
  transition: all 0.22s ease;
}
.split-divider:hover::before,
.split-divider.dragging::before {
  width: 3px;
  background: linear-gradient(180deg, #3370ff, #5b8cff);
  box-shadow: 0 0 8px rgba(51, 112, 255, 0.18), 0 0 16px rgba(51, 112, 255, 0.08);
}

/* ===== 长按交换模式 ===== */
.section-header-row,
.chat-header {
  cursor: default;
  user-select: none;
  -webkit-user-select: none;
}

/* 被拖动的整张卡片：发光 + 蓝色描边 + 微缩放 */
.doc-pane.swap-active,
.chat-pane.swap-active {
  z-index: 5;
}
.doc-pane.swap-active .prd-section,
.chat-pane.swap-active .chat-card {
  box-shadow:
    0 0 0 2px rgba(51, 112, 255, 0.35),
    0 0 0 6px rgba(51, 112, 255, 0.1),
    0 8px 36px rgba(51, 112, 255, 0.22);
  border-radius: 0;
  transform: scale(1.012);
  transition: all 0.28s cubic-bezier(0.34, 1.56, 0.64, 1);
}

/* 标题文字在交换模式下变色提示（仅作文字引导，不发光） */
.section-title.swap-hint,
.chat-header-title.swap-hint {
  color: #3370ff;
}
.section-title.swap-hint .el-icon,
.chat-header-title.swap-hint .el-icon {
  animation: swapIconPulse 0.8s ease-in-out infinite alternate;
}
@keyframes swapIconPulse {
  from { transform: scale(1); opacity: 0.9; }
  to   { transform: scale(1.1); opacity: 1; }
}

/* 整页交换模式遮罩提示 */
.project-page.is-swap-mode .project-main::after {
  content: '拖动卡片以交换位置';
  position: absolute;
  top: 12px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 12px;
  color: #86909c;
  background: rgba(255, 255, 255, 0.85);
  padding: 4px 14px;
  border-radius: 14px;
  pointer-events: none;
  z-index: 20;
  backdrop-filter: blur(8px);
  border: 1px solid rgba(230, 232, 235, 0.6);
}
.chat-card {
  flex: 1;
  width: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 0;
  box-shadow: none;
  border: none;
  overflow: hidden;
  transition: box-shadow 0.28s ease, transform 0.28s ease;
}
.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 14px 18px;
  border-bottom: 1px solid #f2f3f5;
  background: linear-gradient(to bottom, #fafbfc, #fff);
}
.chat-header-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #1d2129;
}
.chat-header-title .el-icon {
  width: 24px;
  height: 24px;
  border-radius: 7px;
  background: linear-gradient(135deg, #00b42a 0%, #36c455 100%);
  color: #fff;
  padding: 5px;
  box-shadow: 0 2px 8px rgba(0, 180, 42, 0.25);
}
.chat-header-sub {
  font-size: 11.5px;
  color: #c9cdd4;
  font-weight: 400;
}
.chat-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

/* 模型标识（可点击切换） */
.chat-model {
  flex-shrink: 0;
}
.model-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  max-width: 180px;
  padding: 5px 10px;
  font-size: 12.5px;
  font-weight: 500;
  color: #4e5969;
  background: linear-gradient(135deg, #f7f8fa 0%, #f2f3f5 100%);
  border: 1px solid #e5e6eb;
  border-radius: 8px;
  cursor: pointer;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  transition: all 0.18s ease;
}
.model-chip:hover {
  color: #3370ff;
  border-color: #bcd0ff;
  background: linear-gradient(135deg, #f0f5ff 0%, #e8f0ff 100%);
  box-shadow: 0 2px 8px rgba(51, 112, 255, 0.12);
}
.model-caret {
  width: 13px;
  height: 13px;
  opacity: 0.6;
}
/* 下拉项选中态 */
.model-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  margin-right: 8px;
  border-radius: 50%;
  background: #c9cdd4;
  vertical-align: middle;
  transition: background 0.2s ease;
}
.model-dot.on {
  background: #3370ff;
  box-shadow: 0 0 0 3px rgba(51, 112, 255, 0.15);
}
.el-dropdown-menu__item.is-selected {
  color: #3370ff;
  font-weight: 600;
}

/* 对话滚动区 */
.chat-scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 14px 14px 8px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: #fff;
}

/* 空态 */
.chat-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 24px 16px;
}
.chat-empty :deep(.el-icon) { opacity: 0.35; }
.chat-empty-title {
  font-size: 13.5px;
  color: #4e5969;
  margin: 0;
  font-weight: 500;
}
.chat-empty-desc {
  font-size: 11.5px;
  color: #86909c;
  margin: 0;
  max-width: 240px;
  text-align: center;
  line-height: 1.65;
}

/* 消息气泡 */
.chat-msg { display: flex; gap: 8px; align-items: flex-start; }
.chat-msg.user { justify-content: flex-end; }
.msg-avatar {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.msg-avatar-ai {
  background: linear-gradient(135deg, #00b42a 0%, #36c455 100%);
  box-shadow: 0 2px 6px rgba(0, 180, 42, 0.2);
}
.msg-avatar-user {
  background: linear-gradient(135deg, #3370ff 0%, #5b8cff 100%);
  box-shadow: 0 2px 6px rgba(51, 112, 255, 0.2);
}
.msg-avatar .el-icon { width: 16px; height: 16px; }
.msg-bubble {
  max-width: 82%;
  padding: 9px 13px;
  border-radius: 14px;
  font-size: 13.5px;
  line-height: 1.68;
  word-break: break-word;
}
.chat-msg.ai .msg-bubble {
  background: #eeeef0;
  border: 1px solid #e2e4e8;
  color: #1d2129;
  border-top-left-radius: 5px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}
.chat-msg.user .msg-bubble {
  background: linear-gradient(135deg, #3370ff 0%, #5b8cff 100%);
  color: #fff;
  border-top-right-radius: 5px;
  box-shadow: 0 2px 8px rgba(51, 112, 255, 0.2);
}
.chat-msg.user .msg-text { white-space: pre-wrap; }
.chat-msg.ai .msg-bubble .markdown-body { padding: 0; font-size: 13.5px; }

/* AI 思考中（气泡内行内显示） */
.thinking-inline {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12.5px;
  color: #86909c;
}
.thinking-inline .rotating {
  color: #3370ff;
  font-size: 13px;
}

/* 输入区 */
.chat-input {
  display: flex;
  gap: 8px;
  padding: 10px 14px 12px;
  border-top: 1px solid #f2f3f5;
  background: #fff;
  align-items: flex-end;
}
.input-wrap {
  flex: 1;
  min-width: 0;
  position: relative;
}
/* 输入框右下角：增强提示词按钮 */
.enhance-btn {
  position: absolute;
  right: 8px;
  bottom: 7px;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: #1d2129;
  cursor: pointer;
  transition: all 0.18s ease;
  z-index: 2;
}
.enhance-btn:hover:not(:disabled) {
  color: #3370ff;
  background: rgba(51, 112, 255, 0.08);
}
.enhance-btn.active {
  color: #3370ff;
}
.enhance-btn:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}
.enhance-icon {
  width: 15px;
  height: 15px;
}
.chat-input :deep(.el-textarea__inner) {
  border-radius: 10px;
  font-size: 13px;
  line-height: 1.55;
  padding: 8px 36px 8px 12px;
  border-color: #e5e6eb;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.chat-input :deep(.el-textarea__inner:focus) {
  border-color: #3370ff;
  box-shadow: 0 0 0 3px rgba(51, 112, 255, 0.1);
}
.chat-send {
  flex-shrink: 0;
  border-radius: 10px !important;
  font-size: 13px;
  padding: 8px 16px;
  height: auto;
}

/* 响应式 */
@media (max-width: 980px) {
  .project-main { overflow: visible; }
  .project-content { flex-direction: column; height: auto; }
  .project-content.is-reversed { flex-direction: column; }
  .doc-pane { overflow: visible; width: 100% !important; }
  .main-content { overflow: visible; }
  .chat-pane { height: 60vh; }
  .split-divider { display: none; }
  .chat-card { border-top: 1px solid #e5e6eb; }
}
</style>