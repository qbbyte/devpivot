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
        <HistoryEntry :project-id="projectId" stage="PRD" :snapshot="docContent" />
        <el-button v-if="!readOnly" class="save-btn" @click="handleSave">
          <el-icon><DocumentChecked /></el-icon>
          <span>保存草稿</span>
        </el-button>
        <el-tag v-else type="info" effect="plain" size="small" class="ro-tag">
          <el-icon><Lock /></el-icon>
          <span>只读</span>
        </el-tag>
      </div>
    </header>

    <main class="project-main">
      <div class="project-content" :class="{ 'is-reversed': reversed }">
        <!-- 左侧：文档区域 -->
        <div class="doc-pane" :style="{ width: splitPercent + '%' }"
          :class="{ 'swap-active': isSwapMode && draggingPane === 'doc' }">
          <div class="main-content">
            <section class="prd-section" @scroll="onDocScroll">
              <div class="section-header-row"
                @mousedown="onHeaderPointerDown($event, 'doc')"
                @touchstart.prevent="onHeaderPointerDown($event, 'doc')">
                <h3 class="section-title" :class="{ 'swap-hint': isSwapMode }">
                  <el-icon><Notebook /></el-icon>
                  <span>PRD 文档</span>
                  <el-tag v-if="isEditing" size="small" type="warning" effect="light" class="edit-tag">编辑中</el-tag>
                </h3>
                <div class="doc-actions">
                  <template v-if="readOnly">
                    <el-tag type="info" effect="plain" size="small" class="ro-tag">
                      <el-icon><Lock /></el-icon>
                      <span>只读 · 该阶段已完成</span>
                    </el-tag>
                  </template>
                  <template v-else-if="!isEditing">
                    <el-button text class="doc-action-btn" @click="enterEdit">
                      <el-icon><EditPen /></el-icon><span>编辑</span>
                    </el-button>
                    <el-button text class="doc-action-btn" :loading="isGenerating" @click="regenerate">
                      <el-icon><Refresh /></el-icon><span>重新生成</span>
                    </el-button>
                  </template>
                  <template v-else>
                    <el-button text class="doc-action-btn" @click="cancelEdit">
                      <el-icon><Close /></el-icon><span>取消</span>
                    </el-button>
                    <el-button text type="primary" class="doc-action-btn" @click="saveEdit">
                      <el-icon><Select /></el-icon><span>保存</span>
                    </el-button>
                  </template>
                  <el-button v-if="!readOnly" type="primary" size="default" class="submit-btn-inline" :loading="submitting" @click="handleSubmit">
                    <span>确认 PRD，进入下一阶段</span>
                    <el-icon class="el-icon--right"><ArrowRight /></el-icon>
                  </el-button>
                </div>
              </div>

              <div class="prd-content" :class="{ 'is-editing': isEditing }" @mouseup="onDocMouseUp">
                <el-input
                  v-if="isEditing"
                  v-model="docContent"
                  type="textarea"
                  class="prd-editor"
                  resize="none"
                  placeholder="在此编辑 PRD 文档（支持 Markdown）…"
                />
                <div v-else ref="previewRef" class="markdown-body" v-html="renderedContent"></div>
                <div v-if="isGenerating" class="generating-tip">
                  <el-icon class="rotating"><Loading /></el-icon>
                  <span>AI 正在撰写中…</span>
                </div>
                <div v-if="!docContent && !isGenerating && !isEditing" class="prd-empty">
                  <el-icon :size="32" color="#c0c4cc"><DocumentAdd /></el-icon>
                  <p class="prd-empty-title">PRD 生成失败</p>
                  <p class="prd-empty-desc">未能基于需求澄清生成文档，请点击「重新生成」重试</p>
                  <el-button class="prd-empty-btn" :loading="isGenerating" @click="regenerate">
                    <el-icon><Refresh /></el-icon><span>重新生成</span>
                  </el-button>
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
                    <!-- 引用区：多条折叠 / 少条平铺 -->
                    <div v-if="msg.quotes && msg.quotes.length" class="msg-quotes-wrap">
                      <!-- 折叠态：摘要卡片 -->
                      <div
                        v-if="!msg._quotesExpanded && msg.quotes.length >= 3"
                        class="msg-quotes-collapsed"
                        @click.stop="toggleMsgQuotes(msg)"
                      >
                        <el-icon class="msg-quotes-collapse-icon"><Document /></el-icon>
                        <span>已引用 <b>{{ msg.quotes.length }}</b> 段 PRD 内容</span>
                        <el-icon class="msg-quotes-arrow"><ArrowDown /></el-icon>
                      </div>
                      <!-- 展开态：限高滚动列表 -->
                      <div v-else class="msg-quotes-expanded">
                        <div class="msg-quotes-head" @click.stop="toggleMsgQuotes(msg)" v-if="msg.quotes.length >= 3">
                          <span>{{ msg.quotes.length }} 条引用</span>
                          <el-icon class="msg-quotes-arrow up"><ArrowDown /></el-icon>
                        </div>
                        <div class="msg-quotes-list">
                          <div v-for="(q, qi) in msg.quotes" :key="qi" class="msg-quote" :title="q">
                            <span class="msg-quote-index">{{ qi + 1 }}</span>{{ q }}
                          </div>
                        </div>
                      </div>
                    </div>
                    <div class="msg-text">{{ msg.content }}</div>
                  </div>
                  <div class="msg-avatar msg-avatar-user">
                    <el-icon><UserFilled /></el-icon>
                  </div>
                </template>
              </div>
            </div>

            <div class="chat-input" @mousedown="onChatInputMouseDown">
              <div v-if="readOnly" class="chat-locked-note">
                <el-icon><Lock /></el-icon>
                <span>该阶段已锁定，仅可查看历史对话</span>
              </div>
              <div v-if="chatQuotes.length" class="quote-zone">
              <div class="quote-zone-header">
                <span class="quote-count"><el-icon><Document /></el-icon> 已引用 {{ chatQuotes.length }} 段内容</span>
                <button class="quote-clear" type="button" @click.stop="clearQuotes">清空</button>
              </div>
              <div class="quote-list">
                <div v-for="(q, qi) in chatQuotes" :key="qi" class="quote-bar" :title="q">
                  <span class="quote-index">{{ qi + 1 }}</span>
                  <span class="quote-text">{{ q }}</span>
                  <button class="quote-remove" type="button" @click.stop="removeQuote(qi)">
                    <el-icon><Close /></el-icon>
                  </button>
                </div>
              </div>
            </div>
              <div class="input-row">
              <div class="input-wrap">
                <el-input
                  v-model="chatInput"
                  type="textarea"
                  :rows="2"
                  resize="none"
                  :disabled="chatGenerating || readOnly"
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
                  :disabled="chatGenerating || readOnly || !chatInput.trim()"
                  @click="sendChat"
                >
                  <el-icon><Promotion /></el-icon>
                  <span>发送</span>
                </el-button>
              </div>
              </div>
            </div>

            <!-- 文档选区浮出的「引用到对话」入口 -->
            <div
              v-if="quotePopup.visible"
              class="quote-popup"
              :style="quotePopup.style"
              @mousedown.prevent="applyQuote"
            >
              引用到对话
            </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup name="StepPrd">
import { ref, reactive, computed, onMounted, nextTick, getCurrentInstance } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  ArrowLeft,
  DocumentChecked,
  Document,
  Notebook,
  Loading,
  DocumentAdd,
  EditPen,
  Refresh,
  Close,
  Select,
  ArrowRight,
  ChatDotRound,
  ChatLineSquare,
  Cpu,
  Promotion,
  ArrowDown,
  UserFilled,
  MagicStick,
  Lock
} from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'
import { getProject } from '@/api/ai/project'
import { getClarifySession } from '@/api/ai/clarify'
import { generatePrd, getPrdDoc, savePrdDoc, getDocModels, submitPrd } from '@/api/ai/doc'
import HistoryEntry from '@/views/portal/components/HistoryEntry.vue'
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
  { value: 'ARCH', label: '系统架构' },
  { value: 'TECH', label: '技术方案' },
  { value: 'DB', label: '数据库' },
  { value: 'DONE', label: '完成' }
]

const loading = ref(false)
const submitting = ref(false)
const project = ref({})
const currentStep = ref('PRD')

const stepIndex = computed(() => stepOrder.findIndex(s => s.value === currentStep.value))

// 阶段已"过去"判定：项目当前阶段在我这一阶之后 → 整页只读锁定
const readOnly = computed(() => {
  const cur = stepOrder.findIndex(s => s.value === currentStep.value)
  const mine = stepOrder.findIndex(s => s.value === 'PRD')
  return cur > mine
})

// ---- PRD 生成（调用真实接口 @/api/ai/doc 的 generatePrd）----
const templateType = ref('STANDARD')
const genMode = ref('single')
const docContent = ref('')
const isGenerating = ref(false)
const previewRef = ref(null)
let genController = null

// 内联编辑 / 重新生成状态
const isEditing = ref(false)
const editBackup = ref('')
const clarifySummary = ref('')   // 上一阶段（AI 澄清）的结论，作为 PRD 生成上下文
const prdDocId = ref(null)        // 后端 ai_prd_doc 主键（落库用），null 表示尚未入库

// ---- 右侧 AI 对话（调用真实流式接口 @/api/ai/chat 的 sendChatMessage）----
const chatMessages = ref([])
const chatInput = ref('')
const chatGenerating = ref(false)
const enhancing = ref(false)
const chatScrollRef = ref(null)
let chatController = null

// ---- PRD 文本选中 → 引用到对话 ----
const quotePopup = reactive({ visible: false, text: '', style: {} })
const chatQuotes = ref([])   // 当前待发送的 PRD 引用列表

// 当前对话模型（进入页面从 /ai/clarify/models 拉取真实模型列表，value 为 modelCode）
const modelOptions = ref([
  { value: 'deepseek', label: 'DeepSeek（默认）' }
])
const chatModel = ref(modelOptions.value[0])
function onSelectModel(val) {
  const m = modelOptions.value.find(o => o.value === val)
  if (m) chatModel.value = m
}
// 取当前用于真实模型调用的 modelCode（无配置时回退 deepseek，与后端一致）
function currentModelCode() {
  return (chatModel.value && chatModel.value.value) || 'deepseek'
}
// 拉取真实可用模型列表（/ai/doc/models）
async function loadModels() {
  try {
    const res = await getDocModels()
    const data = res?.data ?? res
    const list = data?.models || (Array.isArray(data) ? data : [])
    if (Array.isArray(list) && list.length) {
      modelOptions.value = list.map(m => ({ value: m.modelId, label: m.modelName }))
      chatModel.value = modelOptions.value[0]
      return
    }
  } catch (e) { /* 拉取失败用默认 */ }
  modelOptions.value = [{ value: 'deepseek', label: 'DeepSeek（默认）' }]
  chatModel.value = modelOptions.value[0]
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

const renderedContent = computed(() => highlightQuotes(formatMarkdown(docContent.value)))
const wordCount = computed(() => docContent.value.replace(/[\s#*>`\-|]/g, '').length)

// 正则转义
function escapeRegExp(s) {
  return s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}
// 在文档预览 HTML 中高亮当前引用条里的内容（黄色背景），便于直观看到"引用了文档哪段"
function highlightQuotes(html) {
  let out = html
  for (const q of chatQuotes.value) {
    const t = (q || '').trim()
    if (!t || t.length < 2) continue
    try {
      const re = new RegExp(escapeRegExp(t), 'g')
      out = out.replace(re, m => '<mark class="quote-highlight">' + m + '</mark>')
    } catch (e) { /* 非法正则忽略 */ }
  }
  return out
}

function draftKey() { return `prd_draft_${projectId.value}` }

function goBack() {
  router.push('/portal')
}

function getProjectInfo() {
  loading.value = true
  return getProject(projectId.value).then(response => {
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
  persistPrd()
  proxy.$modal.msgSuccess('PRD 已保存' + (prdDocId.value ? '' : '（已落库）'))
}

// 从后端加载已有 PRD（/portal/prd/get）；存在则作为权威源覆盖本地草稿
async function loadPrdFromBackend() {
  try {
    const res = await getPrdDoc(projectId.value)
    const doc = res?.data ?? res
    if (doc && doc.content) {
      prdDocId.value = doc.docId
      docContent.value = doc.content
      if (doc.templateType) templateType.value = doc.templateType
      return true
    }
  } catch (e) { /* 后端不可用忽略，走本地/生成 */ }
  return false
}

// 将当前 PRD 落库 ai_prd_doc（/portal/prd/save upsert）；失败不影响本地草稿
async function persistPrd() {
  if (!docContent.value.trim()) return
  const payload = {
    projectId: projectId.value,
    docName: (project.value.projectName || '产品') + ' PRD',
    templateType: templateType.value,
    content: docContent.value,
    status: '0',
    sourceModel: currentModelCode()
  }
  try {
    const res = await savePrdDoc(payload)
    const id = res?.data ?? res
    if (id) prdDocId.value = id
  } catch (e) {
    console.warn('[prd] 落库失败（不影响本地草稿）：', e)
  }
}

function handleSubmit() {
  if (!docContent.value.trim()) {
    proxy.$modal.msgWarning('请先生成 PRD 文档再提交')
    return
  }
  submitting.value = true
  const payload = {
    docName: (project.value.projectName || '产品') + ' PRD',
    templateType: templateType.value,
    content: docContent.value,
    sourceModel: currentModelCode()
  }
  submitPrd(projectId.value, payload).then(() => {
    proxy.$modal.msgSuccess('已提交，进入原型设计阶段')
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
      if (d.content) {
        docContent.value = d.content
        return true
      }
    }
  } catch (e) { /* ignore */ }
  return false
}

// 进入 PRD：后端已有文档则优先加载（权威源）；否则本地草稿；都没有则基于澄清自动生成
async function initPrd() {
  if (await loadPrdFromBackend()) return
  const restored = loadDraft()
  if (restored && docContent.value.trim()) return
  generateFromClarify()
}

// 从澄清会话抽取干净的"需求要点"文本（绝不 dump 原始 JSON）；仅作为生成上下文
function extractRequirementSummary(d) {
  const parts = []
  if (d && d.projectName) parts.push('项目：' + d.projectName)
  const reqs = Array.isArray(d && d.freeInputs)
    ? [...new Set(d.freeInputs.map(x => (x && (x.content || x.text) || '').trim()).filter(Boolean))]
    : []
  if (reqs.length) parts.push('需求要点：' + reqs.join('；'))
  const adopted = Array.isArray(d && d.adopted)
    ? d.adopted.map(x => typeof x === 'string' ? x : (x && (x.content || x.text || x.conclusion) || '')).filter(Boolean)
    : []
  if (adopted.length) parts.push('已采纳结论：' + adopted.join('；'))
  return parts.join('\n')
}

// 尽力拉取 AI 澄清结论作为上下文（后端不可用时静默跳过），随后生成 PRD
async function generateFromClarify() {
  try {
    const res = await getClarifySession(projectId.value)
    const d = res?.data ?? res
    clarifySummary.value = d ? extractRequirementSummary(d) : ''
  } catch (e) {
    clarifySummary.value = ''
  }
  startGenerate()
}

// ---- 内联编辑 ----
function enterEdit() {
  editBackup.value = docContent.value
  isEditing.value = true
}
function cancelEdit() {
  docContent.value = editBackup.value
  isEditing.value = false
}
function saveEdit() {
  handleSave()
  isEditing.value = false
  proxy.$modal.msgSuccess('已保存 PRD 修改')
}

// ---- 重新生成（覆盖前确认）----
function regenerate() {
  if (isGenerating.value) return
  if (docContent.value.trim()) {
    ElMessageBox.confirm(
      '重新生成将覆盖当前 PRD 文档（包含你的手动修改），是否继续？',
      '提示',
      { confirmButtonText: '重新生成', cancelButtonText: '取消', type: 'warning' }
    ).then(() => {
      generateFromClarify()
    }).catch(() => {})
  } else {
    generateFromClarify()
  }
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
  genController = generatePrd(
    {
      projectId: projectId.value,
      projectName: project.value.projectName,
      industryType: project.value.industryType,
      targetUser: project.value.targetUser,
      templateType: templateType.value,
      mode: genMode.value,
      model: currentModelCode(),
      clarifySummary: clarifySummary.value
    },
    {
      onChunk: (txt) => { docContent.value = txt; scrollPreview() },
      onDone: () => { isGenerating.value = false; genController = null; persistPrd() },
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

// 增强提示词：把简短/模糊输入扩写为结构化提问（本地启发式构造，仅包装用户自身输入，最终仍由真实后端 /ai/chat/send 处理）
function enhancePrompt() {
  const raw = chatInput.value.trim()
  if (!raw || enhancing.value || chatGenerating.value) return
  enhancing.value = true
  // 本地构造增强提示词（当前为前端启发式；若后端提供 /ai/chat/enhance 可在此替换为真实调用）
  setTimeout(() => {
    chatInput.value = buildEnhancedPrompt(raw)
    enhancing.value = false
    proxy.$modal.msgSuccess('已增强提示词')
  }, 450)
}

function buildEnhancedPrompt(raw) {
  // 启发式规则：补充角色、上下文与目标，让提问更具可执行性
  const projectName = project.value.projectName || '本产品'
  return `作为资深产品经理，请结合「${projectName}」的 PRD，针对以下问题给出具体、可执行的建议：\n\n${raw}\n\n请重点回应：① 目标用户与核心场景；② 关键约束（性能/安全/兼容性）；③ 可量化的验收标准。`
}

// 文档区 mouseup：检测是否有文本被选中，浮出「引用到对话」
function onDocMouseUp() {
  requestAnimationFrame(() => {
    const sel = window.getSelection && window.getSelection()
    const text = sel ? sel.toString().trim() : ''
    if (text) {
      const range = sel.getRangeAt(0)
      const rect = range.getBoundingClientRect()
      quotePopup.visible = true
      quotePopup.text = text
      quotePopup.style = {
        position: 'fixed',
        left: (rect.left + rect.width / 2) + 'px',
        top: Math.max(8, rect.top - 42) + 'px',
        transform: 'translateX(-50%)'
      }
      return
    }
    // 编辑态：textarea 内的选区
    if (isEditing.value) {
      const ta = document.querySelector('.prd-editor textarea')
      if (ta && ta.selectionStart !== ta.selectionEnd) {
        const t = ta.value.slice(ta.selectionStart, ta.selectionEnd).trim()
        if (t) {
          const r = ta.getBoundingClientRect()
          quotePopup.visible = true
          quotePopup.text = t
          quotePopup.style = {
            position: 'fixed',
            left: (r.right - 58) + 'px',
            top: (r.top + 8) + 'px',
            transform: 'none'
          }
          return
        }
      }
    }
    quotePopup.visible = false
  })
}
// 点击「引用到对话」：把选中文本加入引用列表，并聚焦聊天输入框
function applyQuote() {
  const t = quotePopup.text.trim()
  if (t && !chatQuotes.value.includes(t)) chatQuotes.value.push(t)
  quotePopup.visible = false
  nextTick(() => {
    const ta = document.querySelector('.chat-input textarea')
    if (ta) ta.focus()
  })
}
function removeQuote(i) { chatQuotes.value.splice(i, 1) }
function clearQuotes() { chatQuotes.value = [] }
function toggleMsgQuotes(msg) {
  msg._quotesExpanded = !msg._quotesExpanded
}
function onChatInputMouseDown() { quotePopup.visible = false }
function onDocScroll() { quotePopup.visible = false }

function sendChat() {
  const q = chatInput.value.trim()
  if ((!q && !chatQuotes.value.length) || chatGenerating.value) return
  const quotes = chatQuotes.value.slice()
  chatMessages.value.push({ role: 'user', content: q || '请针对以上引用的 PRD 内容给出修改建议', quotes })
  chatInput.value = ''
  clearQuotes()
  scrollChatToBottom()

  chatGenerating.value = true
  const aiMsg = { role: 'ai', content: '' }
  chatMessages.value.push(aiMsg)
  // 把引用内容拼入发给 AI 的 question（后端接 SSE 后模型即可感知上下文）
  let question = q
  if (quotes.length) {
    const block = quotes.map(x => '> ' + x).join('\n')
    question = (q ? block + '\n\n' + q : block)
  }
  // 调用真实流式接口 /ai/chat/send（由 @/api/ai/chat 的 sendChatMessage 发起 SSE）
  chatController = sendChatMessage(
    {
      projectId: projectId.value,
      projectName: project.value.projectName,
      question,
      quotes,
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

onMounted(async () => {
  loadModels()
  await getProjectInfo()
  initPrd()
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
.prd-empty-btn { margin-top: 10px; }

/* 文档操作按钮（编辑 / 重新生成 / 保存 / 取消） */
.doc-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}
.doc-action-btn {
  font-size: 13px;
  color: #4e5969;
  padding: 6px 10px;
  border-radius: 8px;
  transition: all 0.2s ease;
}
.doc-action-btn:hover {
  color: #3370ff;
  background: rgba(51, 112, 255, 0.06);
}
.edit-tag {
  margin-left: 8px;
  transform: translateY(-1px);
}

/* 只读锁定标记 */
.ro-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: auto;
  padding: 5px 12px;
  font-size: 13px;
  font-weight: 500;
  color: #3370ff;
  white-space: nowrap;
  vertical-align: middle;
  background: linear-gradient(180deg, #f5f9ff 0%, #eef4ff 100%);
  border: 1px solid #c5d9ff;
  border-radius: 20px;
  box-shadow: 0 1px 2px rgba(51, 112, 255, 0.06);
}
.ro-tag .el-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  padding: 0;
  border-radius: 50%;
  background: #3370ff;
  color: #fff;
  flex-shrink: 0;
}
.ro-tag .el-icon svg {
  width: 12px;
  height: 12px;
}
.chat-locked-note {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  font-size: 13px;
  font-weight: 500;
  color: #3370ff;
  white-space: nowrap;
  vertical-align: middle;
  background: linear-gradient(180deg, #f5f9ff 0%, #eef4ff 100%);
  border: 1px solid #c5d9ff;
  border-radius: 20px;
  box-shadow: 0 1px 2px rgba(51, 112, 255, 0.06);
}
.chat-locked-note .el-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  padding: 0;
  border-radius: 50%;
  background: #3370ff;
  color: #fff;
  flex-shrink: 0;
}
.chat-locked-note .el-icon svg {
  width: 12px;
  height: 12px;
}

/* 内联编辑模式 */
.prd-content.is-editing {
  padding-top: 0;
}
.prd-editor {
  height: 100%;
  width: 100%;
}
.prd-editor :deep(.el-textarea) {
  height: 100%;
}
.prd-editor :deep(.el-textarea__inner) {
  height: 100% !important;
  min-height: 100%;
  border: none;
  border-radius: 0;
  resize: none;
  font-size: 13.5px;
  line-height: 1.7;
  font-family: 'SF Mono', Consolas, 'PingFang SC', 'Microsoft YaHei', monospace;
  color: #1d2129;
  padding: 4px 8px;
  box-shadow: none;
}

.markdown-body {
  padding: 2px 4px;
  font-size: 14px;
  line-height: 1.75;
  color: #1d2129;
}
/* 文档中当前被引用（黄色高亮）的内容 */
mark.quote-highlight {
  background-color: #fff3a3;
  color: inherit;
  border-radius: 2px;
  padding: 0 1px;
  box-shadow: 0 0 0 1px rgba(255, 193, 7, 0.4);
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
  flex-direction: column;
  gap: 8px;
  padding: 10px 14px 12px;
  border-top: 1px solid #f2f3f5;
  background: #fff;
}
.input-row {
  display: flex;
  gap: 8px;
  align-items: flex-end;
}
.input-wrap {
  flex: 1;
  min-width: 0;
  position: relative;
}

/* 引用条（对话输入框上方）—— 紧凑限高风格 */
.quote-zone {
  display: flex;
  flex-direction: column;
  gap: 0;
  background: #f7f8fb;
  border: 1px solid #e5e6eb;
  border-radius: 10px;
  overflow: hidden;
}

/* 引用计数头 */
.quote-zone-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 10px;
  background: linear-gradient(135deg, #f0f2f7 0%, #f7f8fb 100%);
  border-bottom: 1px solid #ebeef3;
}
.quote-count {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 11.5px;
  font-weight: 600;
  color: #4e5969;
}
.quote-count .el-icon {
  width: 14px;
  height: 14px;
  color: #3370ff;
}
.quote-clear {
  border: none;
  background: transparent;
  color: #86909c;
  font-size: 11.5px;
  cursor: pointer;
  padding: 2px 6px;
  border-radius: 4px;
  transition: all 0.18s ease;
}
.quote-clear:hover { color: #f53f3f; background: rgba(245,63,63,0.08); }

/* 引用列表（可滚动） */
.quote-list {
  max-height: 90px;
  overflow-y: auto;
  padding: 4px 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.quote-list::-webkit-scrollbar { width: 4px; }
.quote-list::-webkit-scrollbar-thumb { background: #d0d3d9; border-radius: 2px; }

/* 单条引用 —— 紧凑单行 */
.quote-bar {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 5px 10px;
  margin: 0 6px;
  border-radius: 7px;
  transition: background 0.15s ease;
  cursor: default;
}
.quote-bar:hover { background: #fff; }

.quote-text {
  flex: 1;
  min-width: 0;
  font-size: 12px;
  line-height: 1.45;
  color: #4e5969;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 序号徽标 */
.quote-index {
  flex-shrink: 0;
  width: 18px;
  height: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 600;
  color: #3370ff;
  background: rgba(51, 112, 255, 0.1);
  border-radius: 5px;
}

/* 删除按钮 */
.quote-remove {
  flex-shrink: 0;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  color: #c9cdd4;
  cursor: pointer;
  border-radius: 5px;
  opacity: 0;
  transition: all 0.16s ease;
}
.quote-bar:hover .quote-remove { opacity: 1; }
.quote-remove:hover {
  color: #f53f3f;
  background: rgba(245, 63, 63, 0.08);
}
.quote-remove .el-icon { width: 13px; height: 13px; }

/* 文档选区浮出的「引用到对话」入口 */
.quote-popup {
  position: fixed;
  z-index: 1000;
  padding: 6px 12px;
  background: linear-gradient(135deg, #3370ff 0%, #4880ff 100%);
  color: #fff;
  font-size: 12px;
  font-weight: 500;
  border-radius: 8px;
  cursor: pointer;
  box-shadow: 0 4px 14px rgba(51, 112, 255, 0.35);
  user-select: none;
  white-space: nowrap;
  transition: filter 0.18s ease;
}
.quote-popup:hover { filter: brightness(1.06); }

/* ===== 用户气泡内引用：折叠/展开 ===== */
.msg-quotes-wrap { margin-bottom: 6px; }

/* 折叠态摘要卡片 */
.msg-quotes-collapsed {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 7px 12px;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.22);
  border-radius: 10px;
  cursor: pointer;
  font-size: 12.5px;
  color: rgba(255, 255, 255, 0.85);
  transition: all 0.2s ease;
  user-select: none;
}
.msg-quotes-collapsed:hover {
  background: rgba(255, 255, 255, 0.24);
  border-color: rgba(255, 255, 255, 0.35);
}
.msg-quotes-collapsed b { color: #fff; font-weight: 700; }
.msg-quotes-collapse-icon { width: 15px; height: 15px; opacity: 0.75; flex-shrink: 0; }
.msg-quotes-arrow { width: 14px; height: 14px; margin-left: auto; opacity: 0.6; flex-shrink: 0; transition: transform 0.25s ease; }

/* 展开态容器 */
.msg-quotes-expanded {
  border-radius: 8px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.13);
}
.msg-quotes-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 9px;
  font-size: 10.5px;
  color: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  user-select: none;
}
.msg-quotes-head .msg-quotes-arrow.up { transform: rotate(180deg); }

/* 引用列表：紧凑限高滚动 */
.msg-quotes-list {
  max-height: 85px;
  overflow-y: auto;
  padding: 4px 6px;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

/* 单条引用 —— 严格单行截断 */
.msg-quote {
  padding: 4px 8px 4px 7px;
  background: rgba(255, 255, 255, 0.09);
  border-radius: 6px;
  font-size: 11.5px;
  line-height: 1.4;
  color: rgba(255, 255, 255, 0.8);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  transition: background 0.15s;
}
.msg-quote:hover { background: rgba(255, 255, 255, 0.17); }

/* 序号徽标 —— 更小 */
.msg-quote-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 16px;
  height: 16px;
  margin-right: 5px;
  padding: 0 3px;
  font-size: 10px;
  font-weight: 700;
  color: #fff;
  background: rgba(51, 112, 255, 0.45);
  border-radius: 3px;
  vertical-align: -1px;
  flex-shrink: 0;
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