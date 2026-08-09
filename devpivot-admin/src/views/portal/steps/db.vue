<template>
  <div class="project-page">
    <header class="project-header">
      <div class="header-left">
        <button class="back-link" @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          <span>返回</span>
        </button>
        <span class="header-divider"></span>
        <span class="header-title">{{ project.projectName || '数据库设计' }}</span>
        <span class="stage-pill"><span class="stage-dot"></span>数据库设计</span>
      </div>
      <div class="header-right">
        <el-button class="header-btn" @click="openSettings">
          <el-icon><Setting /></el-icon>
          <span>生成设置</span>
        </el-button>
        <el-button class="header-btn" @click="handleSaveDraft">
          <el-icon><DocumentChecked /></el-icon>
          <span>保存草稿</span>
        </el-button>
        <el-button type="primary" class="header-btn submit-header-btn" :loading="submitting" :disabled="!canSubmit" @click="handleSubmit">
          <span>确认数据库设计，进入下一阶段</span>
          <el-icon class="el-icon--right"><ArrowRight /></el-icon>
        </el-button>
      </div>
    </header>

    <main class="project-main">
      <div class="project-content">
        <!-- 左侧：数据库设计文档 -->
        <div class="doc-pane" :style="{ width: splitPercent + '%' }">
          <div class="main-content">
            <section class="db-section">
              <div class="section-header-row">
                <div class="section-title-left">
                  <h3 class="section-title">
                    <el-icon><Coin /></el-icon>
                    <span>数据库设计</span>
                    <el-tag v-if="isEditing" size="small" type="warning" effect="light" class="edit-tag">编辑中</el-tag>
                  </h3>
                  <div class="selected-models" v-if="sourceModelName">
                    <span class="sm-label">生成模型</span>
                    <span class="sm-chip">{{ sourceModelName }}</span>
                  </div>
                </div>
                <div class="doc-actions">
                  <template v-if="!isEditing">
                    <el-button text class="doc-action-btn" @click="enterEdit">
                      <el-icon><EditPen /></el-icon><span>编辑</span>
                    </el-button>
                    <el-button text class="doc-action-btn" :loading="isGenerating" @click="openSettings">
                      <el-icon><Refresh /></el-icon><span>重新生成</span>
                    </el-button>
                  </template>
                  <template v-else>
                    <el-button text class="doc-action-btn" @click="cancelEdit">
                      <span>取消</span>
                    </el-button>
                    <el-button text class="doc-action-btn" type="primary" @click="saveEdit">
                      <el-icon><Select /></el-icon><span>保存</span>
                    </el-button>
                  </template>
                </div>
              </div>

              <div class="doc-content" :class="{ 'is-editing': isEditing }">
                <div v-if="!finalContent.trim() && !isGenerating" class="db-empty">
                  <el-icon :size="48" color="#c9cdd4"><Coin /></el-icon>
                  <p class="db-empty-title">数据库设计尚未生成</p>
                  <p class="db-empty-desc">点击右上角「生成设置」配置数据库类型与补充要求，AI 将基于 PRD 与技术方案生成库表设计。</p>
                  <el-button type="primary" class="db-empty-btn" @click="openSettings">
                    <el-icon><MagicStick /></el-icon><span>开始生成</span>
                  </el-button>
                </div>
                <div v-else-if="isGenerating && !finalContent.trim()" class="generating-tip">
                  <el-icon class="rotating"><Loading /></el-icon>
                  <span>正在生成数据库设计…</span>
                </div>
                <div v-show="!isEditing && finalContent.trim()" ref="previewRef" class="doc-markdown markdown-body" v-html="renderMarkdown(finalContent)"></div>
                <div v-show="isEditing" class="db-editor">
                  <el-input v-model="finalContent" type="textarea" :rows="20" resize="none" placeholder="在此编辑数据库设计文档（Markdown）…" />
                </div>
                <div v-if="isGenerating && finalContent.trim()" class="generating-tip">
                  <el-icon class="rotating"><Loading /></el-icon>
                  <span>AI 正在生成…</span>
                </div>
              </div>
            </section>
          </div>
        </div>

        <!-- 中间分隔条 -->
        <div class="split-divider" :class="{ dragging }" @mousedown="startDrag"></div>

        <!-- 右侧：AI 对话 -->
        <div class="chat-pane">
          <div class="chat-card">
            <div class="chat-header">
              <div class="chat-header-left">
                <div class="chat-header-title">
                  <el-icon><ChatLineRound /></el-icon>
                  <span>AI 对话</span>
                </div>
                <span class="chat-header-sub">针对数据库设计提问、补充约束或修改建议</span>
              </div>
              <div class="chat-model" @click="openModelDialog">
                <div class="model-chip">
                  <span class="model-dot" :class="{ on: chatModel.value }"></span>
                  <span>{{ chatModel.label }}</span>
                  <el-icon class="model-caret"><ArrowDown /></el-icon>
                </div>
              </div>
            </div>

            <div ref="chatScrollRef" class="chat-scroll">
              <div v-if="!chatMessages.length" class="chat-empty">
                <el-icon :size="40" color="#c9cdd4"><ChatLineRound /></el-icon>
                <p class="chat-empty-title">AI 设计助手</p>
                <p class="chat-empty-desc">可以问：「给用户表加手机号的唯一索引」「订单表需要哪些字段」「把主键改成雪花 ID」等。</p>
              </div>
              <div v-for="(msg, idx) in chatMessages" :key="idx" class="chat-msg" :class="msg.role">
                <div class="msg-avatar" :class="msg.role === 'ai' ? 'msg-avatar-ai' : 'msg-avatar-user'">
                  <el-icon><component :is="msg.role === 'ai' ? 'Cpu' : 'UserFilled'" /></el-icon>
                </div>
                <div class="msg-bubble">
                  <div class="msg-text" v-html="msg.role === 'ai' ? renderMarkdown(msg.content) : escapeHtml(msg.content)"></div>
                </div>
              </div>
              <div v-if="chatGenerating" class="chat-msg ai">
                <div class="msg-avatar msg-avatar-ai"><el-icon><Cpu /></el-icon></div>
                <div class="msg-bubble">
                  <span class="thinking-inline"><el-icon class="rotating"><Loading /></el-icon>思考中…</span>
                </div>
              </div>
            </div>

            <div class="chat-input">
              <div class="input-row">
                <div class="input-wrap">
                  <el-input v-model="chatInput" type="textarea" :rows="2" resize="none" placeholder="输入你的问题…" @keydown.enter.prevent="sendChat" />
                </div>
                <el-button type="primary" class="chat-send" :disabled="!chatInput.trim() || chatGenerating" @click="sendChat">
                  <el-icon><Promotion /></el-icon>
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- 生成设置弹窗 -->
    <el-dialog v-model="showSettingsDialog" title="数据库设计生成设置" width="560px" :close-on-click-modal="false" align-center>
      <div class="settings-body">
        <div class="settings-section">
          <div class="section-label">
            <span class="label-dot db"></span>
            <span class="label-text required">目标数据库类型</span>
          </div>
          <div class="db-type-list">
            <div v-for="item in dbTypeOptions" :key="item.value" class="db-type-card" :class="{ active: settingsForm.dbType === item.value }" @click="settingsForm.dbType = item.value">
              <div class="db-type-info">
                <span class="db-type-name">{{ item.label }}</span>
                <span class="db-type-desc">{{ item.desc }}</span>
              </div>
              <el-icon v-if="settingsForm.dbType === item.value" class="stack-check"><Select /></el-icon>
            </div>
          </div>
        </div>
        <div class="settings-section">
          <div class="section-label">
            <span class="label-dot constraint"></span>
            <span class="label-text">补充要求 / 约束</span>
          </div>
          <el-input v-model="settingsForm.extraReq" type="textarea" :rows="3" resize="none" placeholder="如：必须支持分库分表、字段命名采用下划线、敏感字段加密、统一使用 bigint 主键等" />
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showSettingsDialog = false">取消</el-button>
          <el-button type="primary" :disabled="!settingsForm.dbType" @click="confirmSettings">
            确认设置并生成
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 模型选择弹窗 -->
    <el-dialog v-model="showModelDialog" title="选择 AI 模型" width="480px" :close-on-click-modal="false" align-center>
      <div class="model-dialog-body">
        <p class="model-dialog-tip">请选择用于数据库设计对话的 AI 模型</p>
        <div class="model-list" v-if="modelOptions.length">
          <div v-for="model in modelOptions" :key="model.modelId" class="model-item" :class="{ selected: tempSelectedIds.includes(model.modelId) }" @click="toggleModelItem(model)">
            <el-checkbox :model-value="tempSelectedIds.includes(model.modelId)" @click.stop @change="toggleModelItem(model)" />
            <div class="model-info">
              <span class="model-name">{{ model.modelName }}</span>
              <span class="model-desc">{{ model.description || '' }}</span>
            </div>
          </div>
        </div>
        <el-empty v-else description="没有可用模型，请在后台配置 ai_model_config" />
        <div class="model-dialog-footer">
          <span class="selected-count">已选 {{ tempSelectedIds.length }} 个模型</span>
          <div class="model-dialog-actions">
            <el-button @click="showModelDialog = false">取消</el-button>
            <el-button type="primary" @click="confirmModelDialog">确认</el-button>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup name="StepDb">
import { ref, reactive, computed, onMounted, nextTick, getCurrentInstance } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getProject } from '@/api/ai/project'
import { getDbModels, getDbDoc, saveDbDoc, generateDb, submitDb } from '@/api/ai/db'
import { sendChatMessage } from '@/api/ai/chat'

const { proxy } = getCurrentInstance()
const router = useRouter()
const route = useRoute()
const projectId = computed(() => route.params.id)

const loading = ref(false)
const project = ref({})
const currentStep = ref('DB')
const submitting = ref(false)

const dbType = ref('MySQL')
const modelOptions = ref([])
const selectedModels = ref([])
const extraReq = ref('')
const isGenerating = ref(false)
const hasGenerated = ref(false)
const mainModelId = ref('')
const finalContent = ref('')
const previewRef = ref(null)
const isEditing = ref(false)
const editBackup = ref('')

const showSettingsDialog = ref(false)
const settingsForm = reactive({ dbType: 'MySQL', extraReq: '' })

const dbTypeOptions = [
  { value: 'MySQL', label: 'MySQL', desc: '关系型数据库，Web 应用主流选择' },
  { value: 'PostgreSQL', label: 'PostgreSQL', desc: '高级关系型数据库，适合复杂查询与 JSON' },
  { value: 'Redis', label: 'Redis', desc: '内存键值存储，缓存 / 会话 / 排行榜' },
  { value: 'MongoDB', label: 'MongoDB', desc: '文档型数据库，灵活 Schema' },
  { value: 'TiDB', label: 'TiDB', desc: '分布式 HTAP 数据库，水平扩展' },
  { value: 'OTHER', label: '其他', desc: '自定义数据库类型' }
]

const showModelDialog = ref(false)
const tempSelectedIds = ref([])

// 拖拽分栏
const splitPercent = ref(52)
const dragging = ref(false)

const canSubmit = computed(() => {
  return finalContent.value.trim().length > 0 && !isGenerating.value
})

function goBack() {
  router.push('/portal')
}

function getProjectInfo() {
  loading.value = true
  getProject(projectId.value).then(response => {
    project.value = response.data || {}
    currentStep.value = response.data?.step || 'DB'
    settingsForm.dbType = project.value.dbType || 'MySQL'
    dbType.value = settingsForm.dbType
    loading.value = false
  }).catch(() => { loading.value = false })
}

function loadModels() {
  getDbModels().then(res => {
    const data = res?.data ?? res
    modelOptions.value = data?.models || []
    selectedModels.value = (data?.models || [])[0] ? [data.models[0].modelId] : []
    if (modelOptions.value.length && !chatModel.value.value) {
      chatModel.value = { value: modelOptions.value[0].modelId, label: modelOptions.value[0].modelName }
    }
  }).catch(() => {})
}

function loadDoc() {
  getDbDoc(projectId.value).then(res => {
    const doc = res?.data ?? res
    if (doc && doc.content) {
      finalContent.value = doc.content
      dbType.value = doc.dbType || dbType.value
      mainModelId.value = doc.sourceModel || ''
      hasGenerated.value = true
      settingsForm.dbType = dbType.value
    }
  }).catch(() => {})
}

function modelName(id) {
  const m = modelOptions.value.find(o => o.modelId === id)
  return m ? m.modelName : id
}

const sourceModelName = computed(() => {
  return mainModelId.value ? modelName(mainModelId.value) : ''
})

// ===== 设置弹窗 =====
function openSettings() {
  settingsForm.dbType = dbType.value || 'MySQL'
  settingsForm.extraReq = extraReq.value
  showSettingsDialog.value = true
}

function confirmSettings() {
  if (!settingsForm.dbType) {
    proxy.$modal.msgWarning('请选择目标数据库类型')
    return
  }
  dbType.value = settingsForm.dbType
  extraReq.value = settingsForm.extraReq
  showSettingsDialog.value = false
  if (!selectedModels.value.length) {
    if (modelOptions.value.length) {
      selectedModels.value = [modelOptions.value[0].modelId]
    } else {
      openModelDialog()
      return
    }
  }
  startGenerate()
}

// ===== 模型弹窗 =====
function openModelDialog() {
  tempSelectedIds.value = [...selectedModels.value]
  showModelDialog.value = true
}

function toggleModelItem(model) {
  if (tempSelectedIds.value.includes(model.modelId)) return
  tempSelectedIds.value = [model.modelId]
}

function confirmModelDialog() {
  if (!tempSelectedIds.value.length) {
    proxy.$modal.msgWarning('请至少选择一个模型')
    return
  }
  selectedModels.value = [...tempSelectedIds.value]
  showModelDialog.value = false
}

let genController = null
function startGenerate() {
  if (isGenerating.value || !selectedModels.value.length) {
    if (!selectedModels.value.length) proxy.$modal.msgWarning('请先选择模型')
    return
  }
  finalContent.value = ''
  hasGenerated.value = true
  isGenerating.value = true
  genController = generateDb(
    {
      projectId: projectId.value,
      projectName: project.value.projectName,
      industryType: project.value.industryType,
      targetUser: project.value.targetUser,
      dbType: dbType.value,
      models: selectedModels.value,
      extraReq: extraReq.value
    },
    {
      onModelChunk: (modelId, text) => {
        finalContent.value = text
        mainModelId.value = modelId
        scrollPreview()
      },
      onModelDone: () => {},
      onAllDone: () => {
        isGenerating.value = false
        genController = null
        persistDb()
      },
      onError: (err) => {
        isGenerating.value = false
        genController = null
        proxy.$modal.msgError('生成失败，请稍后重试')
        console.error(err)
      }
    }
  )
}

function persistDb() {
  const payload = {
    projectId: projectId.value,
    docName: (project.value.projectName || '产品') + ' 数据库设计',
    dbType: dbType.value,
    content: finalContent.value,
    status: '0',
    sourceModel: mainModelId.value
  }
  saveDbDoc(payload).catch(() => {})
}

function handleSaveDraft() {
  persistDb()
  proxy.$modal.msgSuccess('草稿已保存')
}

function handleSubmit() {
  if (!canSubmit.value) {
    proxy.$modal.msgWarning('请先生成并完善数据库设计')
    return
  }
  submitting.value = true
  const payload = {
    docName: (project.value.projectName || '产品') + ' 数据库设计',
    dbType: dbType.value,
    content: finalContent.value,
    sourceModel: mainModelId.value,
    status: '1'
  }
  submitDb(projectId.value, payload).then(() => {
    submitting.value = false
    proxy.$modal.msgSuccess('已提交，项目完成')
    router.push('/portal')
  }).catch(() => { submitting.value = false })
}

function enterEdit() { isEditing.value = true; editBackup.value = finalContent.value }
function cancelEdit() { isEditing.value = false; finalContent.value = editBackup.value }
function saveEdit() { isEditing.value = false; persistDb(); proxy.$modal.msgSuccess('已更新') }

// ===== 拖拽分隔条 =====
function startDrag(e) {
  dragging.value = true
  const container = e.currentTarget.parentElement
  const rect = container.getBoundingClientRect()
  const startX = e.clientX
  const startPercent = splitPercent.value
  const onMove = (ev) => {
    const dx = ev.clientX - startX
    let np = startPercent + (dx / rect.width) * 100
    np = Math.min(80, Math.max(20, np))
    splitPercent.value = np
  }
  const onUp = () => {
    dragging.value = false
    document.removeEventListener('mousemove', onMove)
    document.removeEventListener('mouseup', onUp)
    document.body.style.cursor = ''
    document.body.style.userSelect = ''
  }
  document.addEventListener('mousemove', onMove)
  document.addEventListener('mouseup', onUp)
  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
  if (e.preventDefault) e.preventDefault()
}

// ===== 右侧 AI 对话 =====
const chatMessages = ref([])
const chatInput = ref('')
const chatGenerating = ref(false)
const chatModel = ref({ value: '', label: '默认模型' })
const chatScrollRef = ref(null)

function scrollChatToBottom() {
  nextTick(() => {
    const el = chatScrollRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

function sendChat() {
  const q = (chatInput.value || '').trim()
  if (!q || chatGenerating.value) return
  chatMessages.value.push({ role: 'user', content: q })
  chatInput.value = ''
  const aiMsg = reactive({ role: 'ai', content: '' })
  chatMessages.value.push(aiMsg)
  chatGenerating.value = true
  scrollChatToBottom()
  sendChatMessage(
    {
      projectId: projectId.value,
      projectName: project.value.projectName,
      question: q,
      docContent: finalContent.value,
      model: chatModel.value.value
    },
    {
      onChunk: (text) => { aiMsg.content = text; scrollChatToBottom() },
      onDone: () => { chatGenerating.value = false; scrollChatToBottom() },
      onError: () => {
        chatGenerating.value = false
        aiMsg.content = '抱歉，对话服务暂不可用，请稍后重试。'
      }
    }
  )
}

function scrollPreview() {
  requestAnimationFrame(() => {
    const el = previewRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

/* 轻量 Markdown 渲染 */
function escapeHtml(s) { return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;') }
function inlineMd(s) {
  return s
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
}
function renderMarkdown(md) {
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

onMounted(() => {
  getProjectInfo()
  loadModels()
  loadDoc()
  setTimeout(() => {
    if (!hasGenerated.value) {
      showSettingsDialog.value = true
    }
  }, 200)
})
</script>

<style scoped>
.project-page {
  height: 100vh;
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
.stage-pill { display: inline-flex; align-items: center; gap: 6px; font-size: 12px; color: #3370ff; background: rgba(51,112,255,0.1); padding: 3px 10px; border-radius: 999px; }
.stage-dot { width: 6px; height: 6px; border-radius: 50%; background: #3370ff; }
.header-right { display: flex; align-items: center; gap: 8px; }
.header-btn {
  border-radius: 8px;
  font-size: 13px;
  color: #4e5969;
  border-color: #c9cdd4;
  transition: all 0.22s ease;
}
.header-btn:hover { color: #3370ff; border-color: #3370ff; background: rgba(51, 112, 255, 0.04); }

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

.doc-pane {
  flex: 0 0 auto;
  min-width: 0;
  height: 100%;
  align-self: stretch;
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

.db-section {
  flex: 1;
  min-height: 0;
  padding: 0 22px;
  background: #fff;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.section-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: 0 0 10px;
  padding: 14px 0;
  border-bottom: 1px solid #f2f3f5;
}
.section-title-left { display: flex; align-items: center; gap: 18px; min-width: 0; flex-wrap: wrap; }
.section-title {
  display: flex;
  align-items: center;
  gap: 9px;
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #1d2129;
  flex-shrink: 0;
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
.edit-tag { margin-left: 2px; transform: translateY(-1px); }
.selected-models {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  padding-left: 12px;
  border-left: 1px solid #ebedf0;
}
.sm-label { font-size: 12px; color: #86909c; }
.sm-chip {
  font-size: 12px;
  padding: 3px 9px;
  border-radius: 999px;
  background: rgba(51,112,255,0.1);
  color: #3370ff;
}
.doc-actions { display: flex; align-items: center; gap: 4px; flex-shrink: 0; }
.doc-action-btn { font-size: 13px; color: #4e5969; padding: 6px 10px; border-radius: 8px; transition: all 0.2s ease; }
.doc-action-btn:hover { color: #3370ff; background: rgba(51, 112, 255, 0.06); }

.submit-header-btn {
  color: #fff;
  background: linear-gradient(135deg, #3370ff 0%, #4880ff 100%);
  border-color: transparent;
  box-shadow: 0 2px 8px rgba(51, 112, 255, 0.25);
}
.submit-header-btn:hover {
  color: #fff;
  background: linear-gradient(135deg, #2b66f5 0%, #4075ff 100%);
  border-color: transparent;
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(51, 112, 255, 0.35);
}
.submit-header-btn.is-disabled,
.submit-header-btn.is-disabled:hover {
  color: #fff;
  background: #a0c0ff;
  border-color: transparent;
  box-shadow: none;
  transform: none;
}

.doc-content { flex: 1; min-height: 0; position: relative; padding-top: 12px; display: flex; flex-direction: column; }
.doc-content > .markdown-body:only-child,
.doc-content > .db-editor:only-child { flex: 1; min-height: 0; }
.doc-content.is-editing { padding-top: 0; }
.db-editor { flex: 1; min-height: 0; width: 100%; }
.db-editor :deep(.el-textarea) { height: 100%; }
.db-editor :deep(.el-textarea__inner) {
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
.doc-markdown { flex: 1; min-height: 0; overflow-y: auto; }

.db-empty {
  position: absolute;
  inset: 12px 4px 12px 4px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 32px 20px;
  background: radial-gradient(ellipse at 50% 40%, rgba(51, 112, 255, 0.04) 0%, transparent 70%);
  border: 1px dashed #e5e6eb;
  border-radius: 12px;
}
.db-empty :deep(.el-icon) { opacity: 0.45; }
.db-empty-title { font-size: 14px; color: #4e5969; margin: 0; font-weight: 500; }
.db-empty-desc { font-size: 12px; color: #86909c; margin: 0; max-width: 320px; text-align: center; line-height: 1.65; }
.db-empty-actions { margin-top: 10px; display: flex; gap: 10px; }

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
.rotating { animation: db-spin 1s linear infinite; }
@keyframes db-spin { to { transform: rotate(360deg); } }

/* Markdown 渲染 */
.markdown-body { padding: 2px 4px; font-size: 14px; line-height: 1.75; color: #1d2129; }
.markdown-body h1 { font-size: 20px; margin: 4px 0 10px; padding-bottom: 8px; border-bottom: 2px solid #eef0f3; color: #1d2129; }
.markdown-body h2 { font-size: 17px; margin: 18px 0 8px; color: #272e3b; font-weight: 600; }
.markdown-body h3 { font-size: 15px; margin: 14px 0 6px; color: #333d4d; font-weight: 600; }
.markdown-body p { margin: 6px 0; }
.markdown-body ul, .markdown-body ol { margin: 6px 0; padding-left: 20px; }
.markdown-body li { margin: 3px 0; }
.markdown-body blockquote { margin: 10px 0; padding: 8px 14px; background: linear-gradient(135deg, #f7f8fb 0%, #f0f2f5 100%); border-left: 3px solid #3370ff; color: #4e5969; border-radius: 0 8px 8px 0; }
.markdown-body code { background: #f0f1f4; padding: 1px 6px; border-radius: 4px; font-size: 12.5px; color: #d6326e; font-family: 'SF Mono', Consolas, monospace; }
.markdown-body strong { color: #1d2129; font-weight: 600; }

/* 中间可拖拽分隔条 */
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

/* 右侧对话区域 */
.chat-pane { flex: 1 1 0; min-width: 0; display: flex; margin: 0; padding: 0; }
.chat-card {
  flex: 1;
  width: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
  overflow: hidden;
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
.chat-header-title { display: flex; align-items: center; gap: 8px; font-size: 14px; font-weight: 600; color: #1d2129; }
.chat-header-title .el-icon {
  width: 24px; height: 24px; border-radius: 7px;
  background: linear-gradient(135deg, #00b42a 0%, #36c455 100%);
  color: #fff; padding: 5px; box-shadow: 0 2px 8px rgba(0, 180, 42, 0.25);
}
.chat-header-sub { font-size: 11.5px; color: #c9cdd4; font-weight: 400; }
.chat-header-left { display: flex; align-items: center; gap: 12px; min-width: 0; }
.chat-model { flex-shrink: 0; }
.model-chip {
  display: inline-flex; align-items: center; gap: 4px; max-width: 180px;
  padding: 5px 10px; font-size: 12.5px; font-weight: 500; color: #4e5969;
  background: linear-gradient(135deg, #f7f8fa 0%, #f2f3f5 100%);
  border: 1px solid #e5e6eb; border-radius: 8px; cursor: pointer;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis; transition: all 0.18s ease;
}
.model-chip:hover { color: #3370ff; border-color: #bcd0ff; background: linear-gradient(135deg, #f0f5ff 0%, #e8f0ff 100%); box-shadow: 0 2px 8px rgba(51, 112, 255, 0.12); }
.model-caret { width: 13px; height: 13px; opacity: 0.6; }
.model-dot { display: inline-block; width: 6px; height: 6px; margin-right: 8px; border-radius: 50%; background: #c9cdd4; vertical-align: middle; transition: background 0.2s ease; }
.model-dot.on { background: #3370ff; box-shadow: 0 0 0 3px rgba(51, 112, 255, 0.15); }
.chat-scroll {
  flex: 1; min-height: 0; overflow-y: auto; padding: 14px 14px 8px;
  display: flex; flex-direction: column; gap: 12px; background: #fff;
}
.chat-empty { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 6px; padding: 24px 16px; }
.chat-empty :deep(.el-icon) { opacity: 0.35; }
.chat-empty-title { font-size: 13.5px; color: #4e5969; margin: 0; font-weight: 500; }
.chat-empty-desc { font-size: 11.5px; color: #86909c; margin: 0; max-width: 240px; text-align: center; line-height: 1.65; }
.chat-msg { display: flex; gap: 8px; align-items: flex-start; }
.chat-msg.user { justify-content: flex-end; }
.msg-avatar { width: 28px; height: 28px; border-radius: 8px; color: #fff; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.msg-avatar-ai { background: linear-gradient(135deg, #00b42a 0%, #36c455 100%); box-shadow: 0 2px 6px rgba(0, 180, 42, 0.2); }
.msg-avatar-user { background: linear-gradient(135deg, #3370ff 0%, #5b8cff 100%); box-shadow: 0 2px 6px rgba(51, 112, 255, 0.2); }
.msg-avatar .el-icon { width: 16px; height: 16px; }
.msg-bubble { max-width: 82%; padding: 9px 13px; border-radius: 14px; font-size: 13.5px; line-height: 1.68; word-break: break-word; }
.chat-msg.ai .msg-bubble { background: #eeeef0; border: 1px solid #e2e4e8; color: #1d2129; border-top-left-radius: 5px; box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04); }
.chat-msg.user .msg-bubble { background: linear-gradient(135deg, #3370ff 0%, #5b8cff 100%); color: #fff; border-top-right-radius: 5px; box-shadow: 0 2px 8px rgba(51, 112, 255, 0.2); }
.chat-msg.user .msg-text { white-space: pre-wrap; }
.thinking-inline { display: inline-flex; align-items: center; gap: 6px; font-size: 12.5px; color: #86909c; }
.thinking-inline .rotating { color: #3370ff; font-size: 13px; }
.chat-input { display: flex; flex-direction: column; gap: 8px; padding: 10px 14px 12px; border-top: 1px solid #f2f3f5; background: #fff; }
.input-row { display: flex; gap: 8px; align-items: flex-end; }
.input-wrap { flex: 1; min-width: 0; }
.chat-send { flex-shrink: 0; }

/* 设置弹窗 */
.settings-body { padding: 6px 4px; }
.settings-section { margin-bottom: 18px; }
.settings-section:last-of-type { margin-bottom: 6px; }
.section-label { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.label-dot { width: 6px; height: 6px; border-radius: 50%; background: #c9cdd4; }
.label-dot.db { background: #3370ff; }
.label-dot.constraint { background: #f5a623; }
.label-text { font-size: 14px; font-weight: 500; color: #1d2129; }
.label-text.required::after { content: '*'; color: #d54941; margin-left: 4px; }
.db-type-list { display: flex; flex-direction: column; gap: 8px; }
.db-type-card {
  position: relative;
  display: flex; align-items: center; gap: 8px;
  padding: 10px 12px;
  border: 1px solid #e5e6eb;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  transition: all 0.18s ease;
}
.db-type-card:hover { border-color: #d7dae0; background: #f9fafb; }
.db-type-card.active { border-color: #3370ff; background: #f5f8ff; }
.db-type-info { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 1px; }
.db-type-name { font-size: 13.5px; font-weight: 500; color: #1d2129; }
.db-type-desc { font-size: 11.5px; color: #86909c; line-height: 1.35; }
.stack-check {
  position: absolute; top: 8px; right: 8px;
  width: 15px; height: 15px; border-radius: 50%;
  background: #fff; color: #3370ff; font-size: 11px;
  border: 1px solid #3370ff;
  display: flex; align-items: center; justify-content: center;
}
.settings-section :deep(.el-textarea__inner) {
  border-radius: 8px;
  border-color: #e5e6eb;
  padding: 10px 12px;
  font-size: 13px;
  line-height: 1.7;
  color: #1d2129;
}
.settings-section :deep(.el-textarea__inner:focus) { border-color: #3370ff; }
.dialog-footer { display: flex; justify-content: flex-end; gap: 10px; }

/* 模型选择弹窗 */
.model-dialog-body { padding: 6px 4px; }
.model-dialog-tip { font-size: 13px; color: #646a73; margin: 0 0 14px; }
.model-list { display: flex; flex-direction: column; gap: 8px; max-height: 320px; overflow-y: auto; }
.model-item {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 12px; border: 1px solid #ebedf0; border-radius: 8px;
  cursor: pointer; transition: all 0.18s ease; background: #fff;
}
.model-item:hover { border-color: #bcd0ff; background: rgba(51,112,255,0.03); }
.model-item.selected { border-color: #3370ff; background: rgba(51,112,255,0.06); }
.model-info { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.model-info .model-name { font-size: 13.5px; font-weight: 500; color: #1d2129; }
.model-info .model-desc { font-size: 12px; color: #86909c; line-height: 1.4; }
.model-dialog-footer {
  display: flex; align-items: center; justify-content: space-between;
  margin-top: 16px; padding-top: 14px; border-top: 1px solid #f0f1f4;
}
.selected-count { font-size: 12px; color: #646a73; }
.model-dialog-actions { display: flex; gap: 10px; }

@media (max-width: 900px) {
  .project-content { flex-direction: column; overflow-y: auto; }
  .doc-pane { width: 100% !important; height: auto; }
  .chat-pane { height: 60vh; flex: none; }
  .split-divider { display: none; }
  .db-section { overflow: visible; }
  .section-title-left { gap: 10px; }
  .selected-models { border-left: none; padding-left: 0; width: 100%; }
}
</style>
