<template>
  <div class="project-page">
    <header class="project-header">
      <div class="header-left">
        <button class="back-link" @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          <span>返回</span>
        </button>
        <span class="header-divider"></span>
        <span class="header-title">{{ project.projectName || '技术方案' }}</span>
        <span class="stage-pill"><span class="stage-dot"></span>技术方案</span>
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
          <span>确认技术方案，进入下一阶段</span>
          <el-icon class="el-icon--right"><ArrowRight /></el-icon>
        </el-button>
      </div>
    </header>

    <main class="project-main">
      <div class="project-content" :class="{ 'is-reversed': reversed }">
        <!-- 左侧：技术方案文档 -->
        <div class="doc-pane" :style="{ width: splitPercent + '%' }">
          <div class="main-content">
            <!-- 文档区 -->
            <section class="prd-section">
              <div class="section-header-row">
                <div class="section-title-left">
                  <h3 class="section-title">
                    <el-icon><Notebook /></el-icon>
                    <span>技术方案</span>
                    <el-tag v-if="isEditing" size="small" type="warning" effect="light" class="edit-tag">编辑中</el-tag>
                  </h3>
                  <div class="selected-models" v-if="selectedModels.length">
                    <span class="sm-label">已选模型</span>
                    <span class="sm-chip" v-for="id in selectedModels.slice(0, 3)" :key="id">{{ modelName(id) }}</span>
                    <span v-if="selectedModels.length > 3" class="sm-more">+{{ selectedModels.length - 3 }}</span>
                    <button class="link-btn sm-change" @click="openModelDialog">切换模型</button>
                  </div>
                </div>
                <div class="doc-actions">
                  <template v-if="!isEditing">
                    <el-button text class="doc-action-btn" @click="enterEdit">
                      <el-icon><EditPen /></el-icon><span>编辑</span>
                    </el-button>
                    <el-button text class="doc-action-btn" :loading="isGenerating" @click="startGenerate">
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
                </div>
              </div>

              <div class="doc-content" :class="{ 'is-editing': isEditing }">
                <!-- 最终稿 -->
                <template v-if="activeView === 'final'">
                  <el-input
                    v-if="isEditing"
                    v-model="finalContent"
                    type="textarea"
                    class="prd-editor"
                    resize="none"
                    placeholder="在此编辑技术方案（支持 Markdown）…"
                  />
                  <div v-else-if="finalContent.trim()" ref="previewRef" class="markdown-body doc-markdown" v-html="renderMarkdown(finalContent)"></div>
                  <div v-if="isGenerating && activeView === 'final'" class="generating-tip">
                    <el-icon class="rotating"><Loading /></el-icon>
                    <span>AI 正在撰写中…</span>
                  </div>
                  <div v-if="!finalContent.trim() && !isGenerating && !isEditing" class="prd-empty">
                    <el-icon :size="32" color="#c0c4cc"><DocumentAdd /></el-icon>
                    <p class="prd-empty-title">技术方案尚未生成</p>
                    <p class="prd-empty-desc">{{ settingsDone ? '已配置技术栈与模型，点击生成技术方案' : '请先完成生成设置，再生成技术方案' }}</p>
                    <div class="prd-empty-actions">
                      <el-button v-if="!settingsDone" class="prd-empty-btn" type="primary" @click="openSettings">
                        <el-icon><Setting /></el-icon><span>生成设置</span>
                      </el-button>
                      <el-button v-else class="prd-empty-btn" type="primary" :loading="isGenerating" @click="startGenerate">
                        <el-icon><MagicStick /></el-icon><span>AI 生成技术方案</span>
                      </el-button>
                    </div>
                  </div>
                </template>

              </div>
            </section>
          </div>
        </div>

        <!-- 中间：可拖拽分隔条 -->
        <div class="split-divider" :class="{ dragging }" @mousedown="startDrag"></div>

        <!-- 右侧：AI 对话 -->
        <div class="chat-pane">
          <div class="chat-card">
            <div class="chat-header">
              <div class="chat-header-left">
                <div class="chat-header-title">
                  <el-icon><ChatDotRound /></el-icon>
                  <span>AI 对话</span>
                </div>
                <span class="chat-header-sub">针对技术方案提问、补充或修订</span>
              </div>
              <el-dropdown class="chat-model" trigger="click" @command="onSelectChatModel">
                <span class="model-chip">
                  {{ chatModel.label }}
                  <el-icon class="model-caret"><ArrowDown /></el-icon>
                </span>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item
                      v-for="m in modelOptions"
                      :key="m.modelId"
                      :command="m.modelId"
                      :class="{ 'is-selected': m.modelId === chatModel.value }"
                    >
                      <span class="model-dot" :class="{ on: m.modelId === chatModel.value }"></span>
                      {{ m.modelName }}
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>

            <div ref="chatScrollRef" class="chat-scroll">
              <div v-if="!chatMessages.length && !chatGenerating" class="chat-empty">
                <el-icon :size="28" color="#c0c4cc"><ChatLineSquare /></el-icon>
                <p class="chat-empty-title">开始与 AI 讨论技术方案</p>
                <p class="chat-empty-desc">提出技术选型疑问、补充约束或修改建议，AI 会结合当前方案给出建议</p>
              </div>
              <div v-for="(msg, idx) in chatMessages" :key="idx" :class="['chat-msg', msg.role]">
                <template v-if="msg.role === 'ai'">
                  <div class="msg-avatar msg-avatar-ai"><el-icon><Cpu /></el-icon></div>
                  <div class="msg-bubble md">
                    <div v-if="!msg.content && chatGenerating" class="thinking-inline">
                      <el-icon class="rotating"><Loading /></el-icon>
                      <span>AI 正在思考…</span>
                    </div>
                    <div v-else class="markdown-body" v-html="renderMarkdown(msg.content)"></div>
                  </div>
                </template>
                <template v-else>
                  <div class="msg-bubble"><div class="msg-text">{{ msg.content }}</div></div>
                  <div class="msg-avatar msg-avatar-user"><el-icon><UserFilled /></el-icon></div>
                </template>
              </div>
            </div>

            <div class="chat-input">
              <div class="input-row">
                <div class="input-wrap">
                  <el-input
                    v-model="chatInput"
                    type="textarea"
                    :rows="2"
                    resize="none"
                    :disabled="chatGenerating"
                    placeholder="针对技术方案提问、补充约束或修改建议…"
                    @keydown.enter.exact.prevent="sendChat"
                  />
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
      </div>
    </main>

    <!-- 进入页面/生成设置：前后端技术栈 + 技术约束 -->
    <el-dialog v-model="showSettingsDialog" title="" width="720px" :close-on-click-modal="false" :show-close="settingsClosable" align-center class="tech-settings-dialog">
      <div class="settings-body">
        <div class="settings-header">
          <h3 class="settings-title">技术方案生成设置</h3>
          <p class="settings-subtitle">选择前后端技术栈并补充约束，AI 将据此生成针对性的工程蓝图。</p>
        </div>

        <div class="settings-row">
          <!-- 后端 -->
          <div class="settings-col">
            <div class="section-label">
              <span class="label-dot backend"></span>
              <span class="label-text required">后端技术栈</span>
              <span class="label-hint">建议 {{ project.dbType === 'PG' ? 'Python' : 'Java' }}</span>
            </div>
            <div class="stack-list">
              <div
                v-for="item in backendOptions"
                :key="item.value"
                class="stack-card"
                :class="{ active: settingsForm.backendStack === item.value }"
                @click="settingsForm.backendStack = item.value"
              >
                <div class="stack-info">
                  <span class="stack-name">{{ item.label }}</span>
                  <span class="stack-desc">{{ item.desc }}</span>
                </div>
                <el-icon v-if="settingsForm.backendStack === item.value" class="stack-check"><Select /></el-icon>
              </div>
            </div>
            <el-input
              v-if="settingsForm.backendStack === 'OTHER'"
              v-model="settingsForm.backendOther"
              class="stack-other-input"
              size="default"
              placeholder="请填写其他后端技术栈，如 Rust / Scala / PHP 等"
            />
          </div>

          <!-- 前端 -->
          <div class="settings-col">
            <div class="section-label">
              <span class="label-dot frontend"></span>
              <span class="label-text required">前端技术栈</span>
              <span class="label-hint">建议 Vue3</span>
            </div>
            <div class="stack-list">
              <div
                v-for="item in frontendOptions"
                :key="item.value"
                class="stack-card"
                :class="{ active: settingsForm.frontendStack === item.value }"
                @click="settingsForm.frontendStack = item.value"
              >
                <div class="stack-info">
                  <span class="stack-name">{{ item.label }}</span>
                  <span class="stack-desc">{{ item.desc }}</span>
                </div>
                <el-icon v-if="settingsForm.frontendStack === item.value" class="stack-check"><Select /></el-icon>
              </div>
            </div>
            <el-input
              v-if="settingsForm.frontendStack === 'OTHER'"
              v-model="settingsForm.frontendOther"
              class="stack-other-input"
              size="default"
              placeholder="请填写其他前端技术栈，如 Svelte / SolidJS 等"
            />
          </div>
        </div>

        <div class="settings-section">
          <div class="section-label">
            <span class="label-dot constraint"></span>
            <span class="label-text">技术约束 / 偏好</span>
          </div>
          <el-input
            v-model="settingsForm.extraReq"
            type="textarea"
            :rows="2"
            resize="none"
            placeholder="如：优先国产化组件、要求前后端分离、必须支持 MySQL 主从、限 Spring Boot 3.x、需要 SSR 等"
          />
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button v-if="settingsClosable" @click="showSettingsDialog = false">取消</el-button>
          <el-button type="primary" :disabled="!canConfirmSettings" @click="confirmSettings">
            确认设置并生成
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 模型选择弹窗（clarify 同款） -->
    <el-dialog v-model="showModelDialog" title="选择 AI 模型" width="480px" :close-on-click-modal="false" align-center>
      <div class="model-dialog-body">
        <p class="model-dialog-tip">请选择用于生成技术方案的 AI 模型</p>
        <div class="model-list" v-if="modelOptions.length">
          <div v-for="model in modelOptions" :key="model.modelId" class="model-item" :class="{
            selected: tempSelectedIds.includes(model.modelId)
          }" @click="toggleModelItem(model)">
            <el-checkbox :model-value="tempSelectedIds.includes(model.modelId)" @click.stop
              @change="toggleModelItem(model)" />
            <div class="model-info">
              <span class="model-name">{{ model.modelName }}</span>
              <span class="model-desc">{{ model.description }}</span>
            </div>
            <el-tag v-if="model.isDefault" size="small" type="success" effect="light">推荐</el-tag>
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

<script setup name="StepTech">
import { ref, reactive, computed, onMounted, nextTick, getCurrentInstance } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getProject } from '@/api/ai/project'
import { getTechModels, getTechDoc, saveTechDoc, generateTech, submitTech } from '@/api/ai/tech'
import { sendChatMessage } from '@/api/ai/chat'

const { proxy } = getCurrentInstance()
const router = useRouter()
const route = useRoute()
const projectId = computed(() => route.params.id)

const loading = ref(false)
const project = ref({})
const currentStep = ref('TECH')
const submitting = ref(false)

const techStack = ref('JAVA')
const modelOptions = ref([])
const selectedModels = ref([])
const maxCompare = ref(4)
const extraReq = ref('')
const activeView = ref('final')
const isGenerating = ref(false)
const hasGenerated = ref(false)
const mainModelId = ref('')
const finalContent = ref('')
const previewRef = ref(null)
const isEditing = ref(false)

const modelResults = ref([]) // { modelId, modelName, content, status, latency }
let genStart = 0
let genController = null

const settingsDone = ref(false) // 是否已配置过技术栈/约束（本次会话）
const showSettingsDialog = ref(false)
const settingsClosable = ref(true)
const settingsForm = reactive({ backendStack: 'JAVA', backendOther: '', frontendStack: 'VUE3', frontendOther: '', extraReq: '' })

const backendOptions = [
  { value: 'JAVA', label: 'Java', desc: 'Spring Boot / Spring Cloud' },
  { value: 'PYTHON', label: 'Python', desc: 'Django / FastAPI / Flask' },
  { value: 'NODE', label: 'Node.js', desc: 'NestJS / Express / Koa' },
  { value: 'GO', label: 'Go', desc: 'Gin / Echo / Go-zero' },
  { value: 'NET', label: '.NET', desc: '.NET Core / ASP.NET Core' },
  { value: 'OTHER', label: '其他', desc: '自定义后端技术栈' }
]
const frontendOptions = [
  { value: 'VUE3', label: 'Vue 3', desc: 'Element Plus / Pinia / Vite' },
  { value: 'REACT', label: 'React', desc: 'Ant Design / Redux / Next.js' },
  { value: 'ANGULAR', label: 'Angular', desc: 'Material / RxJS / TypeScript' },
  { value: 'HTML', label: 'HTML5', desc: '原生 / jQuery / Bootstrap' },
  { value: 'OTHER', label: '其他', desc: '自定义前端技术栈' }
]

const showModelDialog = ref(false)
const tempSelectedIds = ref([])

// 拖拽分栏
const splitPercent = ref(52)
const reversed = ref(false)
const dragging = ref(false)

const doneCount = computed(() => modelResults.value.filter(r => r.status === 'done').length)
const canSubmit = computed(() => {
  const anyDone = modelResults.value.some(r => r.status === 'done')
  return finalContent.value.trim().length > 0 && anyDone
})
const canConfirmSettings = computed(() => {
  const bOk = settingsForm.backendStack && (settingsForm.backendStack !== 'OTHER' || settingsForm.backendOther.trim())
  const fOk = settingsForm.frontendStack && (settingsForm.frontendStack !== 'OTHER' || settingsForm.frontendOther.trim())
  return bOk && fOk
})

function goBack() {
  router.push('/portal')
}

function getProjectInfo() {
  loading.value = true
  getProject(projectId.value).then(response => {
    project.value = response.data || {}
    currentStep.value = response.data?.step || 'TECH'
    settingsForm.backendStack = project.value.dbType === 'PG' ? 'PYTHON' : 'JAVA'
    settingsForm.frontendStack = 'VUE3'
    techStack.value = `${stackLabel(settingsForm.backendStack, backendOptions, settingsForm.backendOther)} + ${stackLabel(settingsForm.frontendStack, frontendOptions, settingsForm.frontendOther)}`
    loading.value = false
  }).catch(() => { loading.value = false })
}

function loadModels() {
  getTechModels().then(res => {
    modelOptions.value = res.models || []
    maxCompare.value = 1
    selectedModels.value = (res.models || [])[0] ? [res.models[0].modelId] : []
    if (modelOptions.value.length && !chatModel.value.value) {
      chatModel.value = { value: modelOptions.value[0].modelId, label: modelOptions.value[0].modelName }
    }
  }).catch(() => {})
}

function loadDoc() {
  getTechDoc(projectId.value).then(res => {
    const doc = res
    if (doc && doc.content) {
      finalContent.value = doc.content
      techStack.value = doc.techStack || techStack.value
      mainModelId.value = doc.sourceModel || ''
      hasGenerated.value = true
      if (doc.multiSource) {
        try {
          const arr = JSON.parse(doc.multiSource)
          modelResults.value = Array.isArray(arr) ? arr : []
        } catch (e) {}
      }
      activeView.value = 'final'
    }
  }).catch(() => {})
}

function modelName(id) {
  const m = modelOptions.value.find(o => o.modelId === id)
  return m ? m.modelName : id
}

function stackLabel(value, list, customText) {
  if (value === 'OTHER') return (customText && customText.trim()) || '其他'
  const item = list.find(o => o.value === value)
  return item ? item.label : value
}

// ===== 设置弹窗 =====
function openSettings() {
  const parts = techStack.value.split(' + ')
  const b = backendOptions.find(o => o.label === parts[0])
  const f = frontendOptions.find(o => o.label === parts[1])
  if (b) {
    settingsForm.backendStack = b.value
    settingsForm.backendOther = ''
  } else if (parts[0]) {
    settingsForm.backendStack = 'OTHER'
    settingsForm.backendOther = parts[0]
  } else {
    settingsForm.backendStack = 'JAVA'
    settingsForm.backendOther = ''
  }
  if (f) {
    settingsForm.frontendStack = f.value
    settingsForm.frontendOther = ''
  } else if (parts[1]) {
    settingsForm.frontendStack = 'OTHER'
    settingsForm.frontendOther = parts[1]
  } else {
    settingsForm.frontendStack = 'VUE3'
    settingsForm.frontendOther = ''
  }
  settingsForm.extraReq = extraReq.value
  settingsClosable.value = true
  showSettingsDialog.value = true
}

function confirmSettings() {
  if (!canConfirmSettings.value) {
    proxy.$modal.msgWarning('请选择完整的前后端技术栈，选择「其他」时需填写具体内容')
    return
  }
  techStack.value = `${stackLabel(settingsForm.backendStack, backendOptions, settingsForm.backendOther)} + ${stackLabel(settingsForm.frontendStack, frontendOptions, settingsForm.frontendOther)}`
  extraReq.value = settingsForm.extraReq
  settingsDone.value = true
  showSettingsDialog.value = false
  if (!selectedModels.value.length) {
    if (modelOptions.value.length) {
      // 未选模型时自动默认选中首个，点击即直接生成
      const count = Math.min(1, maxCompare.value, modelOptions.value.length)
      selectedModels.value = modelOptions.value.slice(0, count).map(m => m.modelId)
      startGenerate()
    } else {
      openModelDialog()
    }
  } else {
    startGenerate()
  }
}

// ===== 模型弹窗（clarify 同款） =====
function openModelDialog() {
  tempSelectedIds.value = [...selectedModels.value]
  showModelDialog.value = true
}

function toggleModelItem(model) {
  // 单模型：点击即选中该模型（始终保留一个）
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

function startGenerate() {
  if (isGenerating.value || !selectedModels.value.length) {
    if (!selectedModels.value.length) proxy.$modal.msgWarning('请先选择模型')
    return
  }
  modelResults.value = selectedModels.value.map(id => {
    const m = modelOptions.value.find(o => o.modelId === id)
    return { modelId: id, modelName: m ? m.modelName : id, content: '', status: 'streaming', latency: 0 }
  })
  hasGenerated.value = true
  activeView.value = 'final'
  isGenerating.value = true
  activeView.value = 'final'
  genStart = performance.now()
  genController = generateTech(
    {
      projectId: projectId.value,
      projectName: project.value.projectName,
      industryType: project.value.industryType,
      targetUser: project.value.targetUser,
      techStack: techStack.value,
      models: selectedModels.value,
      extraReq: extraReq.value,
      upstream: 'PRD 文档、原型设计'
    },
    {
      onModelChunk: (modelId, text) => {
        const r = modelResults.value.find(x => x.modelId === modelId)
        if (r) { r.content = text; r.status = 'streaming' }
      },
      onModelDone: (modelId) => {
        const r = modelResults.value.find(x => x.modelId === modelId)
        if (r) { r.status = 'done'; r.latency = Math.round(performance.now() - genStart) }
      },
      onAllDone: () => {
        isGenerating.value = false
        genController = null
        if (!finalContent.value.trim() && modelResults.value.length) {
          handleSelectMain(modelResults.value[0].modelId)
        } else {
          activeView.value = 'final'
        }
      },
      onError: (err) => {
        isGenerating.value = false
        proxy.$modal.msgError('生成失败，请稍后重试')
        console.error(err)
      }
    }
  )
}

function retryModel(modelId) {
  if (isGenerating.value) return
  const r = modelResults.value.find(x => x.modelId === modelId)
  if (!r) return
  r.content = ''
  r.status = 'streaming'
  isGenerating.value = true
  activeView.value = 'final'
  genStart = performance.now()
  genController = generateTech(
    {
      projectId: projectId.value,
      projectName: project.value.projectName,
      industryType: project.value.industryType,
      targetUser: project.value.targetUser,
      techStack: techStack.value,
      models: [modelId],
      extraReq: extraReq.value,
      upstream: 'PRD 文档、原型设计'
    },
    {
      onModelChunk: (mid, text) => { if (mid === modelId) r.content = text },
      onModelDone: (mid) => { if (mid === modelId) { r.status = 'done'; r.latency = Math.round(performance.now() - genStart) } },
      onAllDone: () => { isGenerating.value = false; genController = null },
      onError: () => { r.status = 'error'; isGenerating.value = false }
    }
  )
}

function handleSelectMain(modelId) {
  const r = modelResults.value.find(x => x.modelId === modelId)
  if (!r) return
  finalContent.value = r.content
  mainModelId.value = modelId
  activeView.value = 'final'
  nextTick(scrollPreview)
}

function handleSaveDraft() {
  const payload = {
    projectId: projectId.value,
    techStack: techStack.value,
    content: finalContent.value,
    multiSource: JSON.stringify(modelResults.value),
    sourceModel: mainModelId.value,
    status: 0
  }
  saveTechDoc(payload).then(() => {
    proxy.$modal.msgSuccess('草稿已保存')
  }).catch(() => {
    proxy.$modal.msgWarning('草稿已暂存于本地（后端接口待接入）')
  })
}

function handleSubmit() {
  if (!canSubmit.value) {
    proxy.$modal.msgWarning('请先生成并完善技术方案主稿')
    return
  }
  submitting.value = true
  const payload = {
    docName: '技术方案',
    techStack: techStack.value,
    content: finalContent.value,
    multiSource: JSON.stringify(modelResults.value),
    sourceModel: mainModelId.value,
    status: 1
  }
  submitTech(projectId.value, payload).then(() => {
    submitting.value = false
    proxy.$modal.msgSuccess('已提交，进入数据库阶段')
    router.push('/portal')
  }).catch(() => { submitting.value = false })
}

function statusText(r) {
  return { pending: '等待中', streaming: '生成中', done: '已完成', error: '失败' }[r.status] || r.status
}

function enterEdit() { isEditing.value = true }
function cancelEdit() { isEditing.value = false }
function saveEdit() { isEditing.value = false; proxy.$modal.msgSuccess('已更新本地内容') }

// ===== 拖拽分隔条（复用 prd 范式） =====
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

// ===== 右侧 AI 对话（复用 chat.js） =====
const chatMessages = ref([])
const chatInput = ref('')
const chatGenerating = ref(false)
const chatModel = ref({ value: '', label: '默认模型' })
const chatScrollRef = ref(null)

function onSelectChatModel(val) {
  const m = modelOptions.value.find(o => o.modelId === val)
  if (m) chatModel.value = { value: m.modelId, label: m.modelName }
}

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

/* 轻量 Markdown 渲染（与 prd.vue 一致） */
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
  // 无技术方案记录时自动弹出设置窗，引导用户配置
  setTimeout(() => {
    if (!hasGenerated.value) {
      settingsClosable.value = false
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

/* 主内容区：左右双栏（铺满整屏） */
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
.project-content.is-reversed { flex-direction: row-reverse; }

/* 左侧文档面板 */
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

/* 文档区 */
.prd-section {
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
.view-tabs { display: flex; gap: 18px; align-items: center; flex-shrink: 0; }
.tab { font-size: 13px; color: #646a73; padding-bottom: 4px; cursor: pointer; border-bottom: 2px solid transparent; display: inline-flex; align-items: center; gap: 5px; }
.tab.active { color: #3370ff; font-weight: 500; border-bottom-color: #3370ff; }
.tab-count { font-size: 11px; color: #3370ff; background: rgba(51,112,255,0.1); border-radius: 999px; padding: 0 6px; }

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
.sm-more { font-size: 11px; color: #86909c; }
.sm-change { margin-left: 4px; }

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

/* 文档内容区 */
.doc-content { flex: 1; min-height: 0; position: relative; padding-top: 12px; display: flex; flex-direction: column; }
.doc-content > .markdown-body:only-child,
.doc-content > .prd-editor:only-child { flex: 1; min-height: 0; }
.doc-content.is-editing { padding-top: 0; }
.prd-editor { flex: 1; min-height: 0; width: 100%; }
.prd-editor :deep(.el-textarea) { height: 100%; }
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
.doc-markdown { flex: 1; min-height: 0; overflow-y: auto; }

.prd-empty {
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
.prd-empty :deep(.el-icon) { opacity: 0.45; }
.prd-empty-title { font-size: 14px; color: #4e5969; margin: 0; font-weight: 500; }
.prd-empty-desc { font-size: 12px; color: #86909c; margin: 0; max-width: 320px; text-align: center; line-height: 1.65; }
.prd-empty-actions { margin-top: 10px; display: flex; gap: 10px; }
.prd-empty-btn { }

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
.chat-msg.ai .msg-bubble .markdown-body { padding: 0; font-size: 13.5px; }

/* 对比视图（多模型卡片） */
.model-cards {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 14px;
  align-content: start;
  overflow-y: auto;
  padding-bottom: 8px;
}
.model-card { border: 1px solid #ebedf0; border-radius: 10px; padding: 14px; display: flex; flex-direction: column; min-height: 360px; background: #fff; }
.model-card.main { border-color: #3370ff; box-shadow: 0 0 0 2px rgba(51,112,255,0.12); }
.mc-head { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.mc-name { font-size: 13px; font-weight: 600; color: #1f2329; }
.mc-main-tag { font-size: 11px; color: #3370ff; background: rgba(51,112,255,0.1); padding: 1px 7px; border-radius: 999px; }
.mc-status { display: inline-flex; align-items: center; gap: 4px; font-size: 11px; margin-left: auto; }
.mc-status .dot { width: 6px; height: 6px; border-radius: 50%; }
.mc-status.streaming { color: #3370ff; } .mc-status.streaming .dot { background: #3370ff; animation: pulse 1s infinite; }
.mc-status.done { color: #2ba471; } .mc-status.done .dot { background: #2ba471; }
.mc-status.error { color: #d54941; } .mc-status.error .dot { background: #d54941; }
.mc-status.pending { color: #a8abb2; } .mc-status.pending .dot { background: #a8abb2; }
.mc-body { flex: 1; min-height: 0; overflow-y: auto; font-size: 13px; }
.mc-foot { display: flex; gap: 14px; margin-top: 10px; padding-top: 10px; border-top: 1px solid #f5f6f8; }
.link-btn { font-size: 12px; color: #3370ff; background: none; border: none; cursor: pointer; padding: 0; }
.link-btn:disabled { color: #c0c4cc; cursor: not-allowed; }
.link-btn.danger { color: #d54941; }

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

/* ===== 设置弹窗（简约版） ===== */
.tech-settings-dialog :deep(.el-dialog__header) { display: none; }
.tech-settings-dialog :deep(.el-dialog__body) { padding: 0; }
.tech-settings-dialog :deep(.el-dialog__footer) { padding: 12px 24px 20px; border-top: 1px solid #f2f3f5; }

.settings-body { padding: 22px 24px 4px; }
.settings-header {
  margin-bottom: 20px;
}
.settings-title { font-size: 17px; font-weight: 600; color: #1d2129; margin: 0 0 5px; }
.settings-subtitle { font-size: 13px; color: #86909c; margin: 0; line-height: 1.55; }

.settings-row { display: grid; grid-template-columns: 1fr 1fr; gap: 22px; }
.settings-col { min-width: 0; display: flex; flex-direction: column; }
.settings-col + .settings-col { border-left: 1px solid #f2f3f5; padding-left: 22px; }

.settings-section { margin-bottom: 18px; }
.settings-section:last-of-type { margin-bottom: 6px; }
.section-label { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.label-dot { width: 6px; height: 6px; border-radius: 50%; background: #c9cdd4; }
.label-text { font-size: 14px; font-weight: 500; color: #1d2129; }
.label-text.required::after { content: '*'; color: #d54941; margin-left: 4px; }
.label-hint { font-size: 12px; color: #a8abb2; margin-left: auto; }

.stack-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.stack-card {
  position: relative;
  display: flex; align-items: center; gap: 8px;
  padding: 10px 12px;
  border: 1px solid #e5e6eb;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  transition: all 0.18s ease;
}
.stack-card:hover {
  border-color: #d7dae0;
  background: #f9fafb;
}
.stack-card.active {
  border-color: #3370ff;
  background: #f5f8ff;
}
.stack-info { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 1px; }
.stack-name { font-size: 13.5px; font-weight: 500; color: #1d2129; }
.stack-desc { font-size: 11.5px; color: #86909c; line-height: 1.35; }
.stack-check {
  position: absolute; top: 8px; right: 8px;
  width: 15px; height: 15px; border-radius: 50%;
  background: #fff; color: #3370ff; font-size: 11px;
  border: 1px solid #3370ff;
  display: flex; align-items: center; justify-content: center;
}
.stack-other-input { margin-top: 10px; }
.stack-other-input :deep(.el-input__wrapper) {
  border-radius: 8px;
  box-shadow: 0 0 0 1px #e5e6eb inset;
}
.stack-other-input :deep(.el-input__wrapper.is-focus) { box-shadow: 0 0 0 1px #3370ff inset; }

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

/* ===== 模型选择弹窗（clarify 同款） ===== */
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
.model-item.disabled { opacity: 0.5; cursor: not-allowed; }
.model-info { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.model-info .model-name { font-size: 13.5px; font-weight: 500; color: #1d2129; }
.model-info .model-desc { font-size: 12px; color: #86909c; line-height: 1.4; }
.model-dialog-footer {
  display: flex; align-items: center; justify-content: space-between;
  margin-top: 16px; padding-top: 14px; border-top: 1px solid #f0f1f4;
}
.selected-count { font-size: 12px; color: #646a73; }
.model-dialog-actions { display: flex; gap: 10px; }

@keyframes pulse { 0%,100% { opacity: 1; } 50% { opacity: 0.3; } }

@media (max-width: 900px) {
  .project-content { flex-direction: column; overflow-y: auto; }
  .doc-pane { width: 100% !important; height: auto; }
  .chat-pane { height: 60vh; flex: none; }
  .split-divider { display: none; }
  .prd-section { overflow: visible; }
  .section-title-left { gap: 10px; }
  .selected-models { border-left: none; padding-left: 0; width: 100%; }
}
</style>
