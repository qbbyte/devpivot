<template>
  <div class="clarify-page">
    <header class="page-header">
      <div class="header-inner">
        <div class="header-left">
          <button class="back-btn" @click="goBack" title="返回">
            <el-icon>
              <ArrowLeft />
            </el-icon>
          </button>
          <div class="header-titles">
            <span class="header-title">{{ project.projectName || 'AI 需求澄清' }}</span>
            <span class="stage-pill">
              <span class="stage-dot"></span>
              {{ stepLabel }}
            </span>
          </div>
        </div>
        <div class="header-actions">
          <button class="history-btn" @click="showConclusion = true">
            <el-icon>
              <Document />
            </el-icon>
            <span>结论</span>
          </button>
          <button class="history-btn" @click="showHistory = true">
            <el-icon>
              <Clock />
            </el-icon>
            <span>历史</span>
          </button>
          <button class="history-btn" @click="showRetain = true">
            <el-icon>
              <Collection />
            </el-icon>
            <span>摘录</span>
            <span class="badge" v-if="retainedSnippets.length">{{ retainedSnippets.length }}</span>
          </button>
        </div>
      </div>
    </header>

    <main class="page-main">
      <div class="layout">
        <aside class="panel">
          <div class="panel-block">
            <span class="block-label">项目名称</span>
            <span class="block-value">{{ project.projectName || '—' }}</span>
          </div>

          <div class="panel-block">
            <span class="block-label">选型模型</span>
            <div class="chips" v-if="selectedModels.length">
              <span v-for="model in selectedModels" :key="model.id" class="chip">{{ model.name }}</span>
            </div>
            <span class="chip-empty" v-else>尚未选择</span>
            <button class="link-btn" @click="showModelDialog = true">切换模型</button>
          </div>

          <div class="panel-block progress-block">
            <div class="progress-row">
              <span class="block-label">澄清进度</span>
              <span class="progress-count">{{ answeredCount }}<i>/{{ totalQuestions }}</i></span>
            </div>
            <div class="progress-track">
              <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
            </div>
          </div>
        </aside>

        <section class="chat-card">
          <div class="chat-scroll" ref="chatContainer">
            <div class="chat-welcome" v-if="messages.length === 0">
              <div class="welcome-icon">
                <el-icon :size="26">
                  <MagicStick />
                </el-icon>
              </div>
              <div class="welcome-title">开始需求澄清</div>
              <div class="welcome-desc">AI 将根据您的需求基线提出针对性问题，帮您完善需求细节</div>
            </div>

            <template v-for="msg in visibleMessages" :key="msg.id">
              <div class="chat-message ai-message" v-if="msg.type === 'ai_question'">
                <div class="message-avatar">
                  <el-icon :size="18">
                    <Monitor />
                  </el-icon>
                </div>
                <div class="message-content">
                  <div class="message-bubble" v-html="formatMessage(msg.content)"></div>
                  <div class="message-time"><span class="message-author">{{ msg.author }}</span> · {{ msg.timestamp }}
                  </div>
                  <div class="message-options" v-if="msg.options">
                    <div v-for="opt in msg.options" :key="opt.value" class="option-item"
                      :class="{ selected: msg.selectedOption === opt.value }" @click="selectOption(opt, msg)">
                      {{ opt.label }}
                    </div>
                  </div>
                  <div class="message-time">{{ msg.timestamp }}</div>
                </div>
              </div>

              <div class="chat-message user-message" v-else-if="msg.type === 'user_answer'">
                <div class="message-content">
                  <div class="message-bubble">{{ msg.content }}</div>
                  <div class="message-time"><span class="message-author">{{ msg.author }}</span> · {{ msg.timestamp }}
                  </div>
                </div>
                <div class="message-avatar user-avatar">
                  <el-icon :size="18">
                    <User />
                  </el-icon>
                </div>
              </div>

              <div class="chat-message user-message" v-else-if="msg.type === 'user_supplement'">
                <div class="message-content">
                  <div class="message-caption">需求补充</div>
                  <div class="message-bubble">{{ msg.content }}</div>
                  <div class="message-time"><span class="message-author">{{ msg.author }}</span> · {{ msg.timestamp }}
                  </div>
                </div>
                <div class="message-avatar user-avatar">
                  <el-icon :size="18">
                    <User />
                  </el-icon>
                </div>
              </div>

              <div class="chat-message ai-multi-message" v-else-if="msg.type === 'ai_multi_response'">
                <div class="message-avatar">
                  <el-icon :size="18">
                    <Monitor />
                  </el-icon>
                </div>
                <div class="multi-content">
                  <div class="multi-wrap">
                    <div class="multi-responses" :data-id="msg.id" @wheel="onMultiWheel" @mousedown="onMultiDown"
                      @mousemove="onMultiMove" @mouseup="onMultiUp" @mouseleave="onMultiUp" @scroll="onMultiScroll"
                      @click.capture="onMultiClick">
                      <div v-for="resp in msg.modelResponses" :key="resp.respId" class="response-card">
                        <div class="response-header">
                          <span class="model-name">{{ resp.modelName }}</span>
                          <span class="response-time" v-if="resp.latency">{{ resp.latency }}ms</span>
                        </div>
                        <div class="resp-body" v-overflow>
                          <div class="response-content">
                            <span v-if="resp.status === 'loading' && !resp.content" class="thinking">思考中…</span>
                            <span v-else v-html="formatMessage(resp.content)"></span>
                          </div>
                          <div class="resp-fade"></div>
                          <button class="view-more" @click="openResponseDetail(resp)">查看完整回答 ›</button>
                        </div>
                        <div class="response-actions">
                          <el-button size="small" type="primary" :disabled="msg.adoptedModel !== null"
                            @click="adoptResponse(resp, msg)">
                            采纳
                          </el-button>
                        </div>
                      </div>
                    </div>
                    <div class="multi-empty" v-if="!msg.modelResponses || msg.modelResponses.length === 0">所选模型暂无回答内容
                    </div>
                    <div class="multi-fade" v-show="multiState[msg.id] && multiState[msg.id].overflow"></div>
                    <button class="multi-arrow"
                      v-show="multiState[msg.id] && multiState[msg.id].overflow && !multiState[msg.id].atEnd"
                      @click="scrollMultiRight(msg.id)" title="向右滑动">
                      <el-icon>
                        <ArrowRight />
                      </el-icon>
                    </button>
                  </div>
                  <div class="multi-hint" v-show="multiState[msg.id] && multiState[msg.id].overflow">
                    <el-icon>
                      <InfoFilled />
                    </el-icon>
                    横向滑动查看更多模型回答
                  </div>
                </div>
              </div>

              <div class="chat-message user-message" v-else-if="msg.type === 'user_adopt'">
                <div class="message-content">
                  <div class="message-bubble adopt-bubble">
                    <el-icon>
                      <CircleCheck />
                    </el-icon>
                    {{ msg.content }}
                  </div>
                  <div class="message-time"><span class="message-author">{{ msg.author }}</span> · {{ msg.timestamp }}
                  </div>
                </div>
                <div class="message-avatar user-avatar">
                  <el-icon :size="18">
                    <User />
                  </el-icon>
                </div>
              </div>
            </template>

            <div class="chat-message ai-message" v-if="showTypingIndicator">
              <div class="message-avatar">
                <el-icon :size="18">
                  <Monitor />
                </el-icon>
              </div>
              <div class="message-content">
                <div class="message-bubble typing">
                  <span class="dot"></span>
                  <span class="dot"></span>
                  <span class="dot"></span>
                </div>
              </div>
            </div>
          <!-- 聊天区底部：进入下一题按钮（在消息流末尾，透明背景显示聊天区浅灰） -->
          <div class="next-question-btn-wrap" v-if="showNextQuestionButton">
            <button class="next-question-btn" @click="goNextQuestion">进入到下一个问题</button>
          </div>
          </div>

          <div class="chat-input">
            <div class="input-box" :class="{ active: inputFocused }">
              <el-input v-model="inputMessage" type="textarea" :autosize="{ minRows: 1, maxRows: 5 }" resize="none"
                placeholder="输入消息，AI 将为你解答..." :disabled="isTyping" @keyup.enter="handleKeyEnter"
                @focus="inputFocused = true" @blur="inputFocused = false" />
              <div class="input-actions">
                <span class="input-tip">Enter 发送 · Shift+Enter 换行</span>
                <el-button class="send-btn" :type="canSend ? 'primary' : 'info'" circle :disabled="!canSend"
                  @click="sendMessage">
                  <el-icon v-if="!isTyping">
                    <Promotion />
                  </el-icon>
                  <div v-else class="send-loading"></div>
                </el-button>
              </div>
            </div>
          </div>
        </section>
      </div>

    </main>

    <el-dialog v-model="showModelDialog" title="选择 AI 模型" width="480px" :close-on-click-modal="false" align-center>
      <div class="model-dialog-body">
        <p class="model-dialog-tip">请选择用于需求澄清的 AI 模型（最多 {{ maxCompareCount }} 个）</p>
        <div class="model-list" v-if="allModels.length">
          <div v-for="model in allModels" :key="model.id" class="model-item" :class="{
            selected: tempSelectedIds.includes(model.id),
            disabled: !tempSelectedIds.includes(model.id) && tempSelectedIds.length >= maxCompareCount
          }" @click="toggleModel(model)">
            <el-checkbox :model-value="tempSelectedIds.includes(model.id)"
              :disabled="!tempSelectedIds.includes(model.id) && tempSelectedIds.length >= maxCompareCount" @click.stop
              @change="toggleModel(model)" />
            <div class="model-info">
              <span class="model-name">{{ model.name }}</span>
              <span class="model-desc">{{ model.description }}</span>
            </div>
            <el-tag v-if="model.isDefault" size="small" type="success" effect="light">推荐</el-tag>
          </div>
        </div>
        <el-empty v-else description="没有可用模型，请在后台配置 ai_model_config" />
        <div class="model-dialog-footer">
          <span class="selected-count">已选 {{ tempSelectedIds.length }}/{{ maxCompareCount }} 个模型</span>
          <div class="model-dialog-actions">
            <el-button @click="showModelDialog = false">取消</el-button>
            <el-button type="primary" @click="confirmModels">确认并进入澄清</el-button>
          </div>
        </div>
      </div>
    </el-dialog>

    <el-drawer v-model="showHistory" title="历史版本" direction="rtl" size="420px" :with-header="true">
      <div class="history-drawer">
        <div v-for="ver in historyVersions" :key="ver.version" class="history-item">
          <div class="history-head" @click="toggleVersion(ver.version)">
            <div class="history-head-top">
              <span class="history-version">{{ ver.version }}</span>
              <span class="history-status" :class="statusClass(ver.status)">{{ ver.status }}</span>
              <el-icon class="history-caret" :class="{ open: expandedVersion === ver.version }">
                <ArrowDown />
              </el-icon>
            </div>
            <div class="history-sub">
              <span>{{ ver.time }}</span>
              <span class="history-sep">·</span>
              <span>{{ ver.author }}</span>
            </div>
            <div class="history-summary">{{ ver.summary }}</div>
            <div class="history-file-count">{{ ver.files.length }} 个文件</div>
          </div>

          <div class="history-files" v-show="expandedVersion === ver.version">
            <div v-for="file in ver.files" :key="file.name" class="file-row">
              <span class="file-badge" :class="'ft-' + file.type">{{ file.type.toUpperCase() }}</span>
              <span class="file-name" :title="file.name">{{ file.name }}</span>
              <span class="file-size">{{ file.size }}</span>
              <button class="file-view" @click="viewFile(ver, file)">查看</button>
            </div>
          </div>
        </div>
      </div>
    </el-drawer>

    <el-drawer v-model="showRetain" title="已保留要点" direction="rtl" size="420px" :with-header="true">
      <div class="retain-drawer">
        <div class="retain-hint" v-if="!retainedSnippets.length">
          在「查看详情」中选中文字并点「保留」，要点会汇集到这里。
        </div>
        <div v-else>
          <div v-for="s in retainedSnippets" :key="s.id" class="retain-item">
            <div class="retain-item-head">
              <span class="retain-model">{{ s.model }}</span>
              <span class="retain-time">{{ s.time }}</span>
              <button class="retain-del" @click="removeSnippet(s.id)" title="删除">×</button>
            </div>
            <div class="retain-text">{{ s.text }}</div>
          </div>
          <button class="retain-fill" @click="fillSnippetsToInput">全部填入输入框</button>
        </div>
      </div>
    </el-drawer>

    <el-drawer v-model="responseDetail.visible" :title="responseDetail.title" direction="rtl" size="480px"
      :with-header="true" @close="onDetailClose">
      <div class="response-detail" @mouseup="onDetailMouseup" @scroll="retainBtn.visible = false">
        <div class="response-detail-content" v-html="formatMessage(responseDetail.content)"></div>
      </div>
      <button v-show="retainBtn.visible" class="retain-floating"
        :style="{ top: retainBtn.y + 'px', left: retainBtn.x + 'px' }" @mousedown.prevent
        @click="confirmRetain">保留</button>
    </el-drawer>

    <el-drawer v-model="showConclusion" title="需求澄清结论" direction="rtl" size="520px" :with-header="true"
      class="conclusion-drawer-root" @opened="onConclusionOpened">
      <div class="conclusion-drawer">
        <div class="conclusion-scroll" ref="conclusionScrollRef" @scroll="onConclusionScroll">
          <div class="conclusion-summary">{{ clarifyConclusion.summary }}</div>

          <section class="conclusion-section" v-if="clarifyConclusion.adopted.length">
            <h4 class="conclusion-h">采纳结论</h4>
            <div v-for="(a, i) in clarifyConclusion.adopted" :key="i" class="conclusion-adopt">
              <div class="conclusion-meta">
                <span class="conclusion-model">{{ a.modelName }}</span>
                <span class="conclusion-q" v-if="a.question">针对：{{ a.question }}</span>
                <span class="conclusion-time" v-if="a.time">{{ a.time }}</span>
              </div>
              <div class="conclusion-content" v-html="formatMessage(a.content)"></div>
            </div>
          </section>

          <section class="conclusion-section" v-if="retainedSnippets.length">
            <h4 class="conclusion-h">已保留要点（{{ retainedSnippets.length }}）</h4>
            <div v-for="s in retainedSnippets" :key="s.id" class="conclusion-snippet">
              <div class="conclusion-meta">
                <span class="conclusion-model">{{ s.model }}</span>
                <span class="conclusion-time">{{ s.time }}</span>
              </div>
              <div class="conclusion-content">{{ s.text }}</div>
            </div>
          </section>

          <section class="conclusion-section">
            <h4 class="conclusion-h" v-if="clarifyConclusion.freeInputs.length">用户补充（{{
              clarifyConclusion.freeInputs.length
              }}）</h4>
            <div v-for="(f, i) in clarifyConclusion.freeInputs" :key="i" class="conclusion-snippet">
              <div class="conclusion-meta">
                <span class="conclusion-time">{{ f.time }}</span>
              </div>
              <div class="conclusion-content">{{ f.content }}</div>
            </div>
          </section>

          <section class="conclusion-section" v-if="clarifyConclusion.openQuestions.length">
            <h4 class="conclusion-h open">待定问题（{{ clarifyConclusion.openQuestions.length }}）</h4>
            <div v-for="(o, i) in clarifyConclusion.openQuestions" :key="i" class="conclusion-open">{{ o }}</div>
          </section>

          <section class="conclusion-section">
            <h4 class="conclusion-h">参与模型</h4>
            <div class="conclusion-models">{{ clarifyConclusion.modelNames }}</div>
          </section>
          <div class="conclusion-supplement">
            <div class="conclusion-supp-head">
              <el-icon class="conclusion-supp-icon">
                <EditPen />
              </el-icon>
              <span class="conclusion-supp-label">补充说明（提交前可追加）</span>
              <span class="conclusion-supp-tag">可选</span>
            </div>
            <el-input v-model="conclusionSupplement" type="textarea" :autosize="{ minRows: 3, maxRows: 6 }"
              resize="none" placeholder="可在此补充需求背景、目标、约束或其他说明，将一并纳入澄清结论" />
          </div>
        </div>
        <div class="conclusion-actions">
          <span class="conclusion-hint" v-if="!hasReadAll">
            <el-icon>
              <WarningFilled />
            </el-icon>
            请先浏览完整结论内容后再提交
          </span>
          <el-button @click="showConclusion = false">关闭</el-button>
          <el-button type="primary" :disabled="!hasReadAll" @click="handleSubmit">确认并提交澄清结果</el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup name="StepClarify">
import { ref, computed, onMounted, nextTick, getCurrentInstance, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import useUserStore from '@/store/modules/user'
import { getProject } from '@/api/ai/project'
import {
  submitClarify,
  getModels,
  getModelConfig,
  getClarifySession,
  saveSession,
  sendMessage as sendMessageApi,
  adoptAnswer
} from '@/api/ai/clarify'
import { mockHistoryVersions } from './mockData'

// 澄清访谈问卷（引导式结构化访谈，属产品设计，非 AI 假数据；AI 回答由后端真实返回）
const questionScript = [
  {
    content: '您好！我是 AI 需求澄清助手。为了更准确地理清需求，我们先确认几个关键点。\n\n**"系统预计需要支持多少并发用户？"**',
    options: [
      { label: '10人以内', value: '10' },
      { label: '10-100人', value: '100' },
      { label: '100-500人', value: '500' },
      { label: '500人以上', value: '500+' },
      { label: '其他（请说明）', value: 'other' }
    ]
  },
  {
    content: '明白了。接下来关于部署方式：\n\n**"您倾向于哪种部署方式？"**',
    options: [
      { label: '本地服务器', value: 'local' },
      { label: '云服务（推荐）', value: 'cloud' },
      { label: '混合部署', value: 'hybrid' }
    ]
  },
  {
    content: '好的。最后关于终端形态：\n\n**"是否需要移动端支持？"**',
    options: [
      { label: '仅 Web 端', value: 'web' },
      { label: '需要移动端', value: 'mobile' },
      { label: '多端统一', value: 'multi' }
    ]
  }
]

const { proxy } = getCurrentInstance()
const router = useRouter()
const route = useRoute()
const projectId = computed(() => route.params.id)

// 方案C：当前登录用户，作为消息作者标记，便于多人协作时追溯“谁说的”
const userStore = useUserStore()
const currentUser = computed(() => userStore.name || '我')

const stepOrder = [
  { value: 'REQ', label: '需求采集' },
  { value: 'CLARIFY', label: 'AI 澄清' },
  { value: 'PRD', label: 'PRD 文档' },
  { value: 'PROTO', label: '原型设计' },
  { value: 'TECH', label: '技术方案' },
  { value: 'DB', label: '数据库' },
  { value: 'DONE', label: '完成' }
]

const project = ref({})
const currentStep = ref('CLARIFY')

const stepIndex = computed(() => stepOrder.findIndex(s => s.value === currentStep.value))
const stepLabel = computed(() => {
  const hit = stepOrder.find(s => s.value === currentStep.value)
  return hit ? hit.label : '未开始'
})

// 模型选择相关
// 注意：初始必须为 false，避免组件挂载瞬间就弹出选择框、再被 loadSession 异步关闭造成「闪现」。
// 只有「全新会话（无历史对话）」时才在 loadSession 中主动打开，让用户先选模型。
const showModelDialog = ref(false)
const conversationStarted = ref(false)
const allModels = ref([])
const selectedModels = ref([])
const tempSelectedIds = ref([])
const maxCompareCount = ref(4)

// 访谈问卷进度：已提出的问题数（用于恢复会话后续问 & 判断是否还有下一题）
const currentQuestionIndex = ref(0)
// 最近一次提问内容（用于采纳时记录上下文）
const lastQuestionContent = ref('')

// 生成全局唯一 id（自增计数器 + 时间戳），杜绝 v-for :key 因 Date.now() 同毫秒碰撞
// 或 selectedModels 含重复模型导致的 "Cannot set properties of null (setting '__vnode')" 渲染崩溃
let _idSeq = 0
function genId(prefix) {
  _idSeq += 1
  return `${prefix}_${Date.now()}_${_idSeq}`
}

// 把模型标识里的中文/英文括号后缀（如「（完整）」、「(lite)」）去掉，
// 避免历史脏数据把同一模型存成多个 id 导致出现重复卡片。
function normalizeModelId(id) {
  if (!id) return ''
  return String(id).replace(/\s*[\uff08\u0028][^\uff09\u0029]*[\uff09\u0029]\s*$/g, '').trim()
}

// 对话相关
const messages = ref([])
// 渲染时过滤掉“空壳”AI消息：旧数据/model调用失败时可能落库空 modelResponses，
// 避免页面上出现孤立的 AI 头像或“所选模型暂无回答内容”堆积。
function isBlankContent(c) {
  if (!c) return true
  const text = String(c).replace(/<[^>]+>/g, '').replace(/&nbsp;/g, ' ').trim()
  return text.length === 0
}
const visibleMessages = computed(() => messages.value.filter(msg => {
  if (msg.type === 'ai_multi_response') {
    return Array.isArray(msg.modelResponses) && msg.modelResponses.length > 0
  }
  if (msg.type === 'ai_question') {
    return !isBlankContent(msg.content)
  }
  return true
}))
// 输入指示器（"..."）只在还没有 AI 占位气泡时才显示。
// 已有 ai_multi_response 时其卡片自带「思考中…」状态，再叠一个 typing 气泡会出现上下两个 AI 头像的冗余。
const showTypingIndicator = computed(() => {
  if (!isTyping.value) return false
  const last = messages.value[messages.value.length - 1]
  return !(last && last.type === 'ai_multi_response')
})
const inputMessage = ref('')
const inputFocused = ref(false)
const isTyping = ref(false)
const chatContainer = ref(null)

// 统计：进度基于真实问答，而非写死数值
const totalQuestions = computed(() =>
  messages.value.filter(m => m.type === 'ai_question').length
)
// 已回答的问题数：每个 ai_question 之后存在用户回应(user_answer/user_text/user_adopt)即计为已回答
const answeredCount = computed(() => {
  const msgs = messages.value
  let count = 0
  let open = false
  for (const m of msgs) {
    if (m.type === 'ai_question') open = true
    else if (open && (m.type === 'user_answer' || m.type === 'user_text' || m.type === 'user_adopt')) {
      count++
      open = false
    }
  }
  return count
})
const progressPercent = computed(() =>
  totalQuestions.value ? Math.round((answeredCount.value / totalQuestions.value) * 100) : 0
)
const canSend = computed(() => inputMessage.value.trim() && !isTyping.value)

function handleKeyEnter(e) {
  if (e.shiftKey) return
  e.preventDefault()
  sendMessage()
}

// 历史相关
const showHistory = ref(false)
const showRetain = ref(false)
const historyVersions = ref(mockHistoryVersions)
const expandedVersion = ref(mockHistoryVersions[0]?.version || '')

function toggleVersion(version) {
  expandedVersion.value = expandedVersion.value === version ? '' : version
}

function viewFile(ver, file) {
  proxy.$modal.msgInfo(`正在预览 ${ver.version} · ${file.name}`)
}

function statusClass(status) {
  return status === '草稿' ? 'is-draft' : 'is-archived'
}

function goBack() {
  router.push('/portal')
}

function toggleModel(model) {
  const idx = tempSelectedIds.value.indexOf(model.id)
  if (idx > -1) {
    tempSelectedIds.value.splice(idx, 1)
  } else if (tempSelectedIds.value.length < maxCompareCount.value) {
    tempSelectedIds.value.push(model.id)
  }
}

// 把临时勾选的模型应用到实际选用列表（侧栏与发送均读取 selectedModels）
function applyTempSelectedModels() {
  selectedModels.value = allModels.value.filter(m => tempSelectedIds.value.includes(m.id))
}
function confirmModels() {
  applyTempSelectedModels()
  showModelDialog.value = false
  // 会话初始化与首题播种统一由 onMounted 的 loadSession 负责，这里仅更新所选模型
}

// 加载模型列表与系统配置（真实接口）
async function loadModels() {
  try {
    const [mRes, cRes] = await Promise.all([getModels(), getModelConfig()])
    if (mRes && mRes.data && mRes.data.length) {
      allModels.value = mRes.data
    } else {
      // 未获取到任何已配置模型：清空并提示用户，不写死兜底清单
      allModels.value = []
      proxy.$modal.msgWarning('没有可用模型，请在后台配置 ai_model_config')
    }
    if (cRes && cRes.data && cRes.data.maxCompareCount) {
      maxCompareCount.value = cRes.data.maxCompareCount
    }
  } catch (e) {
    // 请求失败（后端未就绪等）：同样提示没有可用模型，不回退到写死清单
    allModels.value = []
    proxy.$modal.msgWarning('没有可用模型，请在后台配置 ai_model_config')
  }
}

// 恢复已有会话（真实接口）：若后端已有对话则还原，否则播种第一题
async function loadSession() {
  try {
    const res = await getClarifySession(projectId.value)
    const session = res.data
    let conv = session && session.conversation
    // 关键修复：库中 conversation 以 JSON 字符串存储，需解析为数组；
    // 之前直接 Array.isArray(字符串) 恒为 false，导致每次刷新都重新播种、丢失全部记录。
    if (typeof conv === 'string' && conv.trim()) {
      try { conv = JSON.parse(conv) } catch (e) { conv = null }
    }
    if (Array.isArray(conv) && conv.length) {
      // 兼容旧数据：
      // 1. 后端早期落库的 ai_multi_response.modelResponses 无 respId，前端模板 key 依赖 respId；
      //    缺 key 会导致 v-for 重复 key 渲染异常，因此为每条缺失 respId 的回答补一个唯一 key。
      // 2. 历史脏数据/模型配置里可能把同一模型存成两个 modelId（如 qwen3.7-max 与 qwen3.7-max（完整）），
      //    按归一化后的 modelId 去重，避免同一模型出现两条卡片。
      //    注意：务必保留原始的 modelId/modelName（不要重写写入库的 id），否则会与后端回传的
      //    token/done 事件里的 modelId 对不上，导致 content/status 无法更新、刷新后变「思考中」。
      conv.forEach(m => {
        if (m.type === 'ai_multi_response' && Array.isArray(m.modelResponses)) {
          const seenNorm = new Set()
          m.modelResponses = m.modelResponses.filter(r => {
            if (!r || !r.modelId) return false
            const norm = normalizeModelId(r.modelId)
            if (!norm || seenNorm.has(norm)) return false
            seenNorm.add(norm)
            if (!r.respId) r.respId = genId('resp')
            return true
          })
        }
      })
      // 防御性修复：任何残留的 loading 状态（如上次 SSE 被中断/Access Denied 未收到 done 事件）
      // 在恢复时一律视为 failed，避免刷新后卡片长期卡在「思考中...」。
      conv.forEach(m => {
        if (m.type === 'ai_multi_response' && Array.isArray(m.modelResponses)) {
          m.modelResponses.forEach(r => {
            if (r.status === 'loading') r.status = 'failed'
          })
        }
      })
      messages.value = conv
      restoreSelectedModels(conv)
      // 已恢复出历史所选模型则保持弹窗关闭，避免刷新后重复弹出（初始值已是 false，无需再显式置 false）
      currentQuestionIndex.value = messages.value.filter(m => m.type === 'ai_question').length
      // 恢复保留要点
      let retained = session && session.retained
      if (typeof retained === 'string' && retained.trim()) {
        try { retainedSnippets.value = JSON.parse(retained) } catch (e) { retainedSnippets.value = [] }
      }
      conversationStarted.value = true
      nextTick(refreshMultiStates)
      // 进入页面自动定位到底部，看到最新消息（nextTick 后仍可能有异步布局/图片，
      // 故再补一次短延时滚动兜底）
      scrollToBottom()
      setTimeout(scrollToBottom, 200)
      return
    }
  } catch (e) {
    // 拉取失败则本地新建会话
  }
  // 无历史：自动采用已预选模型（默认/首个），直接进入对话，
  // 避免进入时弹出/闪现模型选择弹窗，满足「进入即直接展示对话界面」的要求（onMounted 已 await loadModels 完成预选）
  applyTempSelectedModels()
  // 无历史：播种第一题
  askNextQuestion()
  conversationStarted.value = true
  // 进入时也定位到底部，展示最新内容（新会话首题即最新）
  scrollToBottom()
}

// 从已落库对话中反推已选模型（用于侧栏/模型弹窗展示）
function restoreSelectedModels(conv) {
  const seen = new Map()
  conv.forEach(m => {
    if (m.type === 'ai_multi_response' && Array.isArray(m.modelResponses)) {
      m.modelResponses.forEach(r => {
        if (r.modelId && r.modelId !== 'unknown' && !seen.has(r.modelId)) {
          seen.set(r.modelId, r.modelName)
        }
      })
    }
  })
  selectedModels.value = Array.from(seen, ([id, name]) => ({ id, name }))
}

// 持久化完整对话（前端为权威源）：把整个 messages 数组与保留要点深拷贝后原样落库，
// 供刷新页面后 loadSession 原样恢复。fire-and-forget，失败静默不影响交互。
function persistSession() {
  const payload = {
    conversation: JSON.parse(JSON.stringify(messages.value)),
    retained: JSON.parse(JSON.stringify(retainedSnippets.value))
  }
  saveSession(projectId.value, payload).catch(() => { })
}

// 根据问卷进度提出下一题
// 注意：questionScript 仅作初始播种的示例/兜底（3道预设题），
// 实际澄清流程由后端 AI 动态驱动，不应受 questionScript.length 硬限制。
// 当 questionScript 用尽后仍可继续 askNextQuestion（由后端决定是否还有下一题）。
function askNextQuestion() {
  // 已超出预设脚本范围：生成一个通用的"继续澄清"提示，让后端 AI 接管后续问题
  let q
  if (currentQuestionIndex.value < questionScript.length) {
    q = questionScript[currentQuestionIndex.value]
  } else {
    q = {
      content: '为了进一步明确需求细节，请继续描述您的想法或回答以下问题：\n\n**"关于刚才讨论的需求点，您还有什么补充或需要调整的地方吗？"**',
      options: [
        { label: '没有补充，进入下一步', value: 'done' },
        { label: '我有补充说明', value: 'supplement' }
      ]
    }
  }
  lastQuestionContent.value = q.content
  messages.value.push({
    id: genId('q'),
    type: 'ai_question',
    content: q.content,
    options: q.options,
    author: 'AI助手',
    timestamp: new Date().toLocaleString()
  })
  currentQuestionIndex.value++
  scrollToBottom()
  nextTick(refreshMultiStates)
  // 把前端播种的问题也落库，确保刷新后对话完整（含 ai_question）
  persistSession()
}

// 用户主动点击「进入下一题」时调用，避免流结束后自动跳题
function goNextQuestion() {
  if (isTyping.value) return
  askNextQuestion()
}

// 输入框上方「进入到下一个问题」按钮的显示条件（宽松：对话已开始就展示，
// 不依赖 questionScript.length——实际澄清由后端 AI 驱动，不受前端 3 题预设限制。
// 点击时 goNextQuestion 自身有 isTyping 防护）
const showNextQuestionButton = computed(() => {
  return conversationStarted.value
})

// 发送用户回答到后端，流式获取真实模型回答（逐 token 实时渲染）
async function sendToBackend(text) {
  if (!selectedModels.value.length) {
    proxy.$modal.msgWarning('请先选择至少一个 AI 模型')
    return
  }
  isTyping.value = true
  scrollToBottom()

  // 本地先建一个占位 AI 多模型消息，后端逐 token 推送时实时填充。
  // 保留 reactive()：流式回调闭包直接 mutate 这个代理对象，才能驱动 UI 实时更新；
  // 推入 ref([]) 时同一代理会被复用，不会双重包装。每条 response 分配唯一 respId 作为 v-for key，
  // 并对 selectedModels 按 modelId（归一化后）去重，杜绝 modelResponses 出现重复 key 引发的渲染崩溃。
  const seenModel = new Set()
  const aiMsg = reactive({
    id: genId('ai'),
    type: 'ai_multi_response',
    modelResponses: selectedModels.value
      .filter(m => {
        const normId = normalizeModelId(m.id)
        if (!normId || seenModel.has(normId)) return false
        seenModel.add(normId)
        return true
      })
      .map(m => ({
        respId: genId('resp'),
        modelId: m.id,
        modelName: normalizeModelId(m.name),
        content: '',
        status: 'loading',
        latency: 0
      })),
    adoptedModel: null,
    author: 'AI'
  })
  messages.value.push(aiMsg)
  scrollToBottom()

  let streamDone = false
  try {
    await sendMessageApi({
      projectId: projectId.value,
      message: text,
      selectedModels: selectedModels.value.map(m => ({ id: m.id, name: m.name }))
    }, (ev) => {
      const d = ev.data || {}
      if (d.type === 'token') {
        const resp = aiMsg.modelResponses.find(r => r.modelId === d.modelId)
        if (resp) {
          resp.content += d.delta || ''
          scrollToBottom()
        }
      } else if (d.type === 'done') {
        const resp = aiMsg.modelResponses.find(r => r.modelId === d.modelId)
        if (resp) {
          resp.status = d.status || 'completed'
          resp.latency = d.latency || 0
        }
      } else if (d.type === 'error') {
        const resp = aiMsg.modelResponses.find(r => r.modelId === d.modelId)
        if (resp) {
          resp.status = 'failed'
          resp.content = (resp.content || '') + (d.content || '')
        }
      } else if (d.type === 'done-all') {
        streamDone = true
        // 兜底：若后端 done 事件丢失或没按模型下发，确保所有仍在 loading 的卡片变为完成
        aiMsg.modelResponses.forEach(r => { if (r.status === 'loading') r.status = 'completed' })
      }
    }
    )
  } catch (e) {
    proxy.$modal.msgError('AI 回复获取失败，请稍后重试')
    aiMsg.modelResponses.forEach(r => { if (r.status === 'loading') r.status = 'failed' })
  } finally {
    isTyping.value = false
    // 流完成后仅同步当前问卷进度；下一题不再自动推送，由用户点击「进入下一题」按钮触发
    if (streamDone) {
      currentQuestionIndex.value = messages.value.filter(m => m.type === 'ai_question').length
    }
    scrollToBottom()
    nextTick(refreshMultiStates)
  }
  // 流结束（无论成功/失败）都全量落库，保证刷新可恢复
  persistSession()
}

function selectOption(opt, msg) {
  if (msg) msg.selectedOption = opt.value
  const userMsg = {
    id: genId('msg'),
    type: 'user_answer',
    content: opt.label,
    author: currentUser.value,
    timestamp: new Date().toLocaleString()
  }
  messages.value.push(userMsg)
  sendToBackend(opt.label)
}

function sendMessage() {
  if (!inputMessage.value.trim() || isTyping.value) return

  const text = inputMessage.value
  const userMsg = {
    id: genId('msg'),
    type: 'user_answer',
    content: text,
    author: currentUser.value,
    timestamp: new Date().toLocaleString()
  }
  messages.value.push(userMsg)
  inputMessage.value = ''
  sendToBackend(text)
}

function adoptResponse(resp, msg) {
  if (msg) msg.adoptedModel = resp.modelId
  const adoptMsg = {
    id: genId('msg'),
    type: 'user_adopt',
    content: `采纳 ${resp.modelName}`,
    adoptedModel: resp.modelId,
    author: currentUser.value,
    timestamp: new Date().toLocaleString()
  }
  messages.value.push(adoptMsg)
  proxy.$modal.msgSuccess(`已采纳 ${resp.modelName} 的回答`)
  // 持久化采纳（含最新对话，确保刷新后可恢复采纳状态）
  adoptAnswer({
    projectId: projectId.value,
    modelId: resp.modelId,
    modelName: resp.modelName,
    content: resp.content,
    question: lastQuestionContent.value,
    timestamp: adoptMsg.timestamp,
    conversation: JSON.parse(JSON.stringify(messages.value))
  }).catch(() => { })
}

// 查看完整回答（右侧抽屉）+ 选中片段保留
const responseDetail = ref({ visible: false, title: '', modelName: '', content: '' })
function openResponseDetail(resp) {
  responseDetail.value = {
    visible: true,
    title: resp.modelName + (resp.latency ? ` · ${resp.latency}ms` : ''),
    modelName: resp.modelName,
    content: resp.content
  }
}
// 抽屉内选中片段 → 浮动「保留」按钮 → 存入侧边栏要点面板
const retainedSnippets = ref([])
const retainBtn = ref({ visible: false, x: 0, y: 0, text: '' })
function onDetailMouseup() {
  setTimeout(() => {
    const sel = window.getSelection()
    const text = sel ? sel.toString().trim() : ''
    if (!text) { retainBtn.value.visible = false; return }
    const range = sel.getRangeAt(0)
    const content = document.querySelector('.response-detail-content')
    if (!content || !content.contains(range.commonAncestorContainer)) {
      retainBtn.value.visible = false
      return
    }
    const rect = range.getBoundingClientRect()
    retainBtn.value = {
      visible: true,
      x: rect.left + rect.width / 2,
      y: rect.top - 38,
      text
    }
  }, 0)
}
function confirmRetain() {
  if (!retainBtn.value.text) return
  retainedSnippets.value.push({
    id: genId('snip'),
    text: retainBtn.value.text,
    model: responseDetail.value.modelName,
    author: currentUser.value,
    time: new Date().toLocaleTimeString()
  })
  retainBtn.value.visible = false
  retainBtn.value.text = ''
  window.getSelection().removeAllRanges()
  proxy.$modal.msgSuccess('已保留要点')
  persistSession()
}
function removeSnippet(id) {
  retainedSnippets.value = retainedSnippets.value.filter(s => s.id !== id)
  persistSession()
}
function fillSnippetsToInput() {
  const joined = retainedSnippets.value.map(s => s.text).join('\n')
  if (!joined) return
  inputMessage.value += (inputMessage.value ? '\n' : '') + joined
  proxy.$modal.msgSuccess('已填入输入框')
}
function onDetailClose() {
  retainBtn.value.visible = false
}

// 需求澄清结论（核心产物）：把采纳结论、保留要点、用户补充聚合成一份可读文档
const showConclusion = ref(false)
// 结论抽屉内的「补充说明」：提交前由用户自行追加，纳入澄清结论
const conclusionSupplement = ref('')
// 结论抽屉「阅读门禁」：滚动浏览完所有内容后，才能提交（内容不足一屏时直接放行）
const conclusionScrollRef = ref(null)
const hasReadAll = ref(false)

function checkConclusionRead() {
  const el = conclusionScrollRef.value
  if (!el) return
  // 内容不足一屏：无需滚动即视为已读，直接解锁
  if (el.scrollHeight - el.clientHeight <= 4) {
    hasReadAll.value = true
    return
  }
  if (el.scrollTop + el.clientHeight >= el.scrollHeight - 4) {
    hasReadAll.value = true
  }
}

function onConclusionScroll() {
  if (hasReadAll.value) return
  checkConclusionRead()
}

function onConclusionOpened() {
  hasReadAll.value = false
  nextTick(() => checkConclusionRead())
}

const clarifyConclusion = computed(() => {
  const msgs = messages.value
  let q = ''
  const questionStates = []
  const adopted = []
  const freeInputs = []
  msgs.forEach(m => {
    if (m.type === 'ai_question') {
      q = m.content
      questionStates.push({ question: q, answered: false, adopted: false })
    } else if (m.type === 'ai_multi_response') {
      if (m.adoptedModel) {
        const resp = (m.modelResponses || []).find(r => r.modelId === m.adoptedModel)
        if (resp) {
          adopted.push({ question: q, modelName: resp.modelName, content: resp.content, time: m.timestamp })
          const last = questionStates[questionStates.length - 1]
          if (last) { last.adopted = true; last.answered = true }
        }
      }
    } else if (m.type === 'user_adopt') {
      // mock 静态数据中采纳信息在 user_adopt 上，回查其前的多模型回答与问题
      let mr = null
      let qq = q
      const idx = msgs.indexOf(m)
      for (let j = idx - 1; j >= 0; j--) {
        if (msgs[j].type === 'ai_multi_response' && !mr) mr = msgs[j]
        if (msgs[j].type === 'ai_question') { qq = msgs[j].content; break }
      }
      const modelId = m.adoptedModel || (mr && mr.adoptedModel)
      const resp = mr && (mr.modelResponses || []).find(r => r.modelId === modelId)
      if (resp) {
        adopted.push({ question: qq, modelName: resp.modelName, content: resp.content, time: m.timestamp })
        const last = questionStates[questionStates.length - 1]
        if (last) { last.adopted = true; last.answered = true }
      }
    } else if (m.type === 'user_answer' || m.type === 'user_text' || m.type === 'user_supplement') {
      freeInputs.push({ content: m.content, time: m.timestamp })
      const last = questionStates[questionStates.length - 1]
      if (last && !last.adopted) last.answered = true
    }
  })
  const openQuestions = questionStates.filter(s => !s.answered).map(s => s.question)
  const modelNames = selectedModels.value.map(mm => mm.name).join('、') || '—'
  const suppCount = conclusionSupplement.value.trim() ? 1 : 0
  const summary =
    `本次澄清共梳理 ${questionStates.length} 个问题，已采纳 ${adopted.length} 个模型结论，` +
    `保留 ${retainedSnippets.value.length} 条要点，留存 ${freeInputs.length + suppCount} 条用户补充；` +
    `尚有 ${openQuestions.length} 个待定问题需后续确认。`
  return { adopted, freeInputs, openQuestions, modelNames, summary }
})

// 提交澄清结果：submitClarify 已改为 @/api/ai/clarify 的真实接口（落库 ai_clarify_session 并将项目 step 推进到 PRD）

function buildClarifyResult() {
  const freeInputs = [...clarifyConclusion.value.freeInputs]
  const supp = conclusionSupplement.value.trim()
  if (supp) {
    freeInputs.push({ content: supp, time: new Date().toLocaleString(), fromConclusion: true })
  }
  return {
    projectId: projectId.value,
    projectName: project.value.projectName || 'AI 需求澄清',
    generatedAt: new Date().toLocaleString(),
    selectedModels: selectedModels.value.map(mm => ({ id: mm.id, name: mm.name })),
    adopted: clarifyConclusion.value.adopted,
    retained: retainedSnippets.value,
    freeInputs,
    openQuestions: clarifyConclusion.value.openQuestions,
    summary: clarifyConclusion.value.summary,
    conversation: messages.value
  }
}

// 局部指令：内容溢出时给容器加 is-overflow，进而显示渐隐遮罩与“查看完整回答”
const vOverflow = {
  mounted(el) {
    el.classList.toggle('is-overflow', el.scrollHeight > el.clientHeight + 1)
  },
  updated(el) {
    el.classList.toggle('is-overflow', el.scrollHeight > el.clientHeight + 1)
  }
}

function formatMessage(content) {
  if (!content) return ''
  return content
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\n/g, '<br>')
}

function scrollToBottom() {
  nextTick(() => {
    if (chatContainer.value) {
      chatContainer.value.scrollTop = chatContainer.value.scrollHeight
    }
  })
}

// 多模型回答卡片：鼠标滚轮转横向滚动 + 拖拽滑动
function onMultiWheel(e) {
  const el = e.currentTarget
  if (el.scrollWidth <= el.clientWidth) return
  const delta = Math.abs(e.deltaY) >= Math.abs(e.deltaX) ? e.deltaY : e.deltaX
  const atStart = el.scrollLeft <= 0
  const atEnd = el.scrollLeft + el.clientWidth >= el.scrollWidth - 1
  // 到达左右边界时放行，让页面可以继续纵向滚动
  if ((delta < 0 && atStart) || (delta > 0 && atEnd)) return
  e.preventDefault()
  el.scrollLeft += delta
  syncMulti(el)
}

const dragState = { active: false, startX: 0, startLeft: 0, moved: false }
function onMultiDown(e) {
  const el = e.currentTarget
  if (el.scrollWidth <= el.clientWidth) return
  // 在正文或按钮上按下时不触发拖拽，以便正常选中文字 / 点击按钮
  if (e.target.closest('.response-content') || e.target.closest('button')) return
  dragState.active = true
  dragState.startX = e.pageX
  dragState.startLeft = el.scrollLeft
  dragState.moved = false
  el.classList.add('grabbing')
}
function onMultiMove(e) {
  if (!dragState.active) return
  const el = e.currentTarget
  const dx = e.pageX - dragState.startX
  if (Math.abs(dx) > 3) dragState.moved = true
  el.scrollLeft = dragState.startLeft - dx
  syncMulti(el)
}
function onMultiUp(e) {
  const el = e.currentTarget
  dragState.active = false
  el.classList.remove('grabbing')
}
function onMultiClick(e) {
  // 拖拽后松手不触发卡片内按钮点击
  if (dragState.moved) {
    e.preventDefault()
    e.stopPropagation()
    dragState.moved = false
  }
}

// 多卡片滚动状态：是否溢出、是否已到最右端（驱动箭头显隐）
const multiState = reactive({})
function syncMulti(el) {
  if (!el) return
  const id = el.dataset.id
  if (!id) return
  const overflow = el.scrollWidth > el.clientWidth + 1
  const atEnd = el.scrollLeft + el.clientWidth >= el.scrollWidth - 1
  const s = multiState[id] || (multiState[id] = { overflow: false, atEnd: false })
  s.overflow = overflow
  s.atEnd = atEnd
}
function onMultiScroll(e) {
  syncMulti(e.currentTarget)
}
function refreshMultiStates() {
  document.querySelectorAll('.multi-responses').forEach(syncMulti)
}
function scrollMultiRight(id) {
  const el = document.querySelector(`.multi-responses[data-id="${id}"]`)
  if (el) el.scrollBy({ left: 340, behavior: 'smooth' })
}

async function handleSubmit() {
  const result = buildClarifyResult()
  proxy.$modal.confirm('确认提交澄清结果？提交后将生成「需求澄清结论」并推进到 PRD 阶段。', '提示', {
    confirmButtonText: '确认提交',
    cancelButtonText: '取消',
    type: 'info'
  }).then(async () => {
    try {
      await submitClarify(projectId.value, result)
      proxy.$modal.msgSuccess('澄清结果已提交，已生成需求澄清结论并推进到 PRD 阶段')
      // 提交后即退出本流程，回到门户首页（澄清负责人可能不负责后续阶段）
      showConclusion.value = false
      router.push('/portal')
    } catch (e) {
      proxy.$modal.msgError('提交失败，请稍后重试')
    }
  }).catch(() => { })
}

function getProjectInfo() {
  getProject(projectId.value).then(response => {
    project.value = response.data
    currentStep.value = response.data.step || 'CLARIFY'
  }).catch(() => { })
}

onMounted(async () => {
  getProjectInfo()
  await loadModels()
  // 默认预选：优先 isDefault，否则取第一个，避免初始无模型
  if (!tempSelectedIds.value.length) {
    const defaults = allModels.value.filter(m => m.isDefault)
    tempSelectedIds.value = (defaults.length ? defaults : (allModels.value[0] ? [allModels.value[0]] : [])).map(m => m.id)
  }
  await loadSession()
})
</script>

<style scoped>
.clarify-page {
  --primary: #3370ff;
  --primary-hover: #2860e1;
  --primary-soft: #f0f7ff;
  --bg: #f5f6f8;
  --surface: #ffffff;
  --text-1: #1d2129;
  --text-2: #4e5969;
  --text-3: #86909c;
  --text-4: #c9cdd4;
  --border: #eef0f2;
  --radius-lg: 16px;
  --radius-md: 12px;
  --radius-sm: 9px;
  --shadow-sm: 0 1px 2px rgba(20, 23, 28, 0.04);
  --shadow-md: 0 6px 20px rgba(20, 23, 28, 0.06);

  height: 100vh;
  background: var(--bg);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  font-family: -apple-system, BlinkMacSystemFont, "PingFang SC", "Microsoft YaHei", "Segoe UI", Roboto, sans-serif;
}

/* ---------- Header ---------- */
.page-header {
  flex-shrink: 0;
  height: 58px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: saturate(180%) blur(12px);
  border-bottom: 1px solid var(--border);
}

.header-inner {
  max-width: 1480px;
  height: 100%;
  margin: 0 auto;
  padding: 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.back-btn {
  width: 34px;
  height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  color: var(--text-2);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all 0.18s ease;
}

.back-btn:hover {
  background: rgba(51, 112, 255, 0.08);
  color: var(--primary);
}

.header-titles {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.header-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-1);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.stage-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 10px;
  border-radius: 999px;
  background: var(--primary-soft);
  color: var(--primary);
  font-size: 12px;
  font-weight: 500;
  flex-shrink: 0;
}

.stage-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--primary);
}

.history-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 34px;
  padding: 0 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--surface);
  color: var(--text-2);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.18s ease;
}

.history-btn:hover {
  color: var(--primary);
  border-color: var(--primary);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 17px;
  height: 17px;
  padding: 0 4px;
  margin-left: 2px;
  font-size: 11px;
  font-weight: 600;
  color: #fff;
  background: var(--primary);
  border-radius: 9px;
  line-height: 1;
}

/* ---------- Layout ---------- */
.page-main {
  flex: 1;
  min-height: 0;
  padding: 22px 20px 0;
  display: flex;
  flex-direction: column;
}

.layout {
  flex: 1;
  min-height: 0;
  max-width: 1480px;
  width: 100%;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 256px 1fr;
  gap: 20px;
  align-items: stretch;
}

/* ---------- Sidebar panel ---------- */
.panel {
  background: var(--surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
  align-self: start;
}

.panel-block {
  padding: 18px 20px;
}

.panel-block+.panel-block {
  border-top: 1px solid var(--border);
}

.block-label {
  display: block;
  font-size: 12px;
  color: var(--text-3);
  letter-spacing: 0.2px;
}

.block-value {
  display: block;
  margin-top: 7px;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-1);
}

.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  margin-top: 10px;
}

.chip {
  padding: 4px 11px;
  border-radius: 999px;
  background: var(--primary-soft);
  color: var(--primary);
  font-size: 12px;
  font-weight: 500;
}

.chip-empty {
  display: block;
  margin-top: 9px;
  font-size: 13px;
  color: var(--text-4);
}

.link-btn {
  margin-top: 12px;
  padding: 0;
  border: none;
  background: none;
  color: var(--primary);
  font-size: 13px;
  cursor: pointer;
  transition: color 0.18s ease;
}

.link-btn:hover {
  color: var(--primary-hover);
}

.progress-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}

.progress-count {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-1);
}

.progress-count i {
  font-style: normal;
  font-size: 12px;
  font-weight: 400;
  color: var(--text-4);
  margin-left: 1px;
}

.progress-track {
  margin-top: 11px;
  height: 6px;
  border-radius: 999px;
  background: #eef0f3;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #4d82ff, #3370ff);
  transition: width 0.4s ease;
}

/* ---------- Chat ---------- */
.chat-card {
  background: var(--surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.chat-scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 26px 24px;
  background: #fafbfc;
}

.chat-scroll::-webkit-scrollbar,
.multi-responses::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.chat-scroll::-webkit-scrollbar-thumb,
.multi-responses::-webkit-scrollbar-thumb {
  background: #dfe3e8;
  border-radius: 999px;
}

.chat-scroll::-webkit-scrollbar-thumb:hover,
.multi-responses::-webkit-scrollbar-thumb:hover {
  background: #c6ccd4;
}

/* welcome */
.chat-welcome {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  gap: 6px;
  color: var(--text-3);
}

.welcome-icon {
  width: 58px;
  height: 58px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--primary-soft);
  color: var(--primary);
  margin-bottom: 12px;
}

.welcome-title {
  font-size: 17px;
  font-weight: 600;
  color: var(--text-1);
}

.welcome-desc {
  font-size: 13px;
  color: var(--text-3);
  max-width: 320px;
  line-height: 1.6;
}

/* messages */
.chat-message {
  display: flex;
  gap: 12px;
  margin-bottom: 22px;
  animation: msg-in 0.32s ease both;
}

@keyframes msg-in {
  from {
    opacity: 0;
    transform: translateY(8px);
  }

  to {
    opacity: 1;
    transform: none;
  }
}

.user-message {
  justify-content: flex-end;
}

.message-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  flex-shrink: 0;
  background: linear-gradient(135deg, #3370ff, #5b8cff);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.user-avatar {
  background: linear-gradient(135deg, #6b7785, #8a96a3);
}

.message-content {
  max-width: 72%;
  min-width: 0;
}

.message-bubble {
  padding: 11px 15px;
  border-radius: 14px;
  font-size: 14px;
  line-height: 1.65;
  word-break: break-word;
  box-shadow: var(--shadow-sm);
}

.ai-message .message-bubble {
  background: var(--surface);
  color: var(--text-1);
  border-top-left-radius: 5px;
}

.user-message .message-bubble {
  background: linear-gradient(135deg, #3370ff, #4d82ff);
  color: #fff;
  border-top-right-radius: 5px;
}

.adopt-bubble {
  background: #f0f9eb !important;
  color: #52a824 !important;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  box-shadow: none !important;
}

.message-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.option-item {
  padding: 7px 16px;
  border: 1px solid #e4e7ed;
  border-radius: 999px;
  font-size: 13px;
  color: var(--text-2);
  cursor: pointer;
  transition: all 0.18s ease;
}

.option-item:hover {
  border-color: var(--primary);
  color: var(--primary);
}

.option-item.selected {
  background: var(--primary);
  color: #fff;
  border-color: var(--primary);
}

.message-time {
  font-size: 11px;
  color: var(--text-4);
  margin-top: 5px;
}

.message-author {
  color: var(--primary);
  font-weight: 600;
}

.message-caption {
  font-size: 12px;
  font-weight: 500;
  color: var(--primary);
  margin-bottom: 5px;
}

/* typing */
.typing {
  display: inline-flex;
  gap: 5px;
  align-items: center;
  padding: 14px 18px;
}

.dot {
  width: 7px;
  height: 7px;
  background: var(--text-4);
  border-radius: 50%;
  animation: typing 1.4s infinite;
}

.dot:nth-child(2) {
  animation-delay: 0.2s;
}

.dot:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {

  0%,
  60%,
  100% {
    transform: translateY(0);
    opacity: 0.5;
  }

  30% {
    transform: translateY(-4px);
    opacity: 1;
  }
}

/* multi responses */
.ai-multi-message {
  align-items: flex-start;
}

.multi-content {
  flex: 1;
  min-width: 0;
}

.multi-wrap {
  position: relative;
  min-width: 0;
}

.multi-responses {
  display: flex;
  gap: 14px;
  overflow-x: auto;
  padding-bottom: 6px;
  cursor: grab;
  user-select: none;
}

.multi-responses.grabbing {
  cursor: grabbing;
}

.multi-fade {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 6px;
  width: 64px;
  background: linear-gradient(to right, rgba(250, 251, 252, 0), #fafbfc);
  pointer-events: none;
  border-radius: 0 12px 12px 0;
}

.multi-arrow {
  position: absolute;
  top: 50%;
  right: 6px;
  transform: translateY(-50%);
  width: 30px;
  height: 30px;
  padding: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--border);
  border-radius: 50%;
  background: var(--surface);
  color: var(--text-2);
  box-shadow: 0 2px 8px rgba(20, 23, 28, 0.12);
  cursor: pointer;
  transition: all 0.18s ease;
}

.multi-arrow:hover {
  color: var(--primary);
  border-color: var(--primary);
  box-shadow: 0 3px 12px rgba(51, 112, 255, 0.2);
}

.multi-arrow:active {
  transform: translateY(-50%) scale(0.92);
}

.response-card {
  min-width: 300px;
  flex: 1 0 300px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  padding: 16px;
  box-shadow: var(--shadow-sm);
  cursor: default;
  display: flex;
  flex-direction: column;
}

.response-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.model-name {
  font-weight: 600;
  color: var(--primary);
  font-size: 13px;
}

.response-time {
  font-size: 12px;
  color: var(--text-4);
}

.response-content {
  font-size: 14px;
  line-height: 1.65;
  color: var(--text-1);
  user-select: text;
}

.thinking {
  color: #909399;
  font-style: italic;
}

.resp-body {
  position: relative;
  max-height: 220px;
  overflow: hidden;
  flex: 1;
  min-height: 0;
  margin-bottom: 14px;
}

.resp-body.is-overflow .resp-fade {
  display: block;
}

.resp-body.is-overflow .view-more {
  display: inline-flex;
}

.resp-fade {
  display: none;
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 44px;
  background: linear-gradient(to bottom, rgba(255, 255, 255, 0), var(--surface));
  pointer-events: none;
  border-bottom-left-radius: var(--radius-md);
  border-bottom-right-radius: var(--radius-md);
}

.view-more {
  display: none;
  position: absolute;
  right: 10px;
  bottom: 8px;
  align-items: center;
  gap: 2px;
  padding: 4px 10px;
  font-size: 12px;
  color: var(--primary);
  background: var(--surface);
  border: 1px solid var(--primary);
  border-radius: 14px;
  cursor: pointer;
  box-shadow: 0 2px 6px rgba(51, 112, 255, 0.15);
  transition: all 0.18s ease;
}

.view-more:hover {
  background: var(--primary);
  color: #fff;
}

.response-detail {
  height: 100%;
  overflow-y: auto;
}

.response-detail-content {
  font-size: 14px;
  line-height: 1.7;
  color: var(--text-1);
  padding-right: 4px;
}

/* 抽屉：已保留要点 */
.retain-drawer {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.retain-hint {
  font-size: 13px;
  color: var(--text-4);
  line-height: 1.6;
  padding: 20px 4px;
}

.retain-item {
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  padding: 12px 14px;
  background: #fafbfc;
}

.retain-item-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.retain-model {
  font-size: 12px;
  font-weight: 600;
  color: var(--primary);
}

.retain-time {
  font-size: 11px;
  color: var(--text-4);
  flex: 1;
}

.retain-del {
  border: none;
  background: none;
  color: var(--text-3);
  cursor: pointer;
  font-size: 16px;
  line-height: 1;
  padding: 0 2px;
  margin-left: auto;
}

.retain-del:hover {
  color: #f53f3f;
}

.retain-text {
  font-size: 13px;
  line-height: 1.6;
  color: var(--text-1);
  white-space: pre-wrap;
  word-break: break-word;
}

.retain-fill {
  margin-top: 16px;
  width: 100%;
  font-size: 13px;
  font-weight: 500;
  color: var(--primary);
  background: var(--surface);
  border: 1px solid var(--primary);
  border-radius: var(--radius-md);
  padding: 9px 0;
  cursor: pointer;
  transition: all 0.18s ease;
}

.retain-fill:hover {
  background: var(--primary);
  color: #fff;
}

/* 抽屉内选区保留浮动按钮 */
.retain-floating {
  position: fixed;
  transform: translate(-50%, 0);
  z-index: 3000;
  padding: 5px 14px;
  font-size: 12px;
  color: #fff;
  background: var(--primary);
  border: none;
  border-radius: 14px;
  cursor: pointer;
  box-shadow: 0 4px 14px rgba(51, 112, 255, 0.35);
}

.retain-floating:hover {
  background: #245bdb;
}

/* 结论抽屉（核心产物） */
.conclusion-drawer {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.conclusion-scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 18px 20px 10px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.conclusion-summary {
  font-size: 13px;
  line-height: 1.7;
  color: var(--text-1);
  background: rgba(51, 112, 255, 0.06);
  border: 1px solid rgba(51, 112, 255, 0.18);
  border-radius: var(--radius-md);
  padding: 12px 14px;
}

.conclusion-section {}

.conclusion-h {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-1);
  margin: 0 0 10px;
  padding-left: 9px;
  border-left: 3px solid var(--primary);
}

.conclusion-h.open {
  border-left-color: #f59e0b;
}

.conclusion-adopt,
.conclusion-snippet {
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  padding: 12px 14px;
  margin-bottom: 10px;
  background: var(--surface);
}

.conclusion-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

.conclusion-model {
  font-size: 12px;
  font-weight: 600;
  color: var(--primary);
}

.conclusion-time {
  font-size: 12px;
  color: var(--text-4);
}

.conclusion-q {
  font-size: 12px;
  color: var(--text-3);
  flex: 1;
  min-width: 0;
}

.conclusion-content {
  font-size: 13px;
  line-height: 1.7;
  color: var(--text-1);
  word-break: break-word;
}

.conclusion-content :deep(strong) {
  color: var(--text-0);
}

.conclusion-open {
  font-size: 13px;
  line-height: 1.7;
  color: #b45309;
  background: rgba(245, 158, 11, 0.08);
  border-radius: var(--radius-sm);
  padding: 8px 12px;
  margin-bottom: 8px;
}

.conclusion-models {
  font-size: 13px;
  color: var(--text-1);
}

.response-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: auto;
}

.multi-empty {
  padding: 22px;
  text-align: center;
  font-size: 13px;
  color: var(--text-4);
  background: #fafbfc;
  border: 1px dashed var(--border);
  border-radius: var(--radius-md);
}

.multi-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  justify-content: center;
  font-size: 12px;
  color: var(--text-3);
  margin-top: 10px;
}

/* 输入框上方「进入到下一个问题」按钮 */
.next-question-btn-wrap {
  display: flex;
  justify-content: center;
  padding: 10px 0 10px;
  background: transparent;
}

.next-question-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: fit-content;
  padding: 8px 22px;
  border: 1px solid var(--border);
  border-radius: 20px;
  background: transparent;
  color: var(--primary);
  font-size: 13px;
  font-weight: 500;
  line-height: 1;
  cursor: pointer;
  transition: all 0.2s ease;
  user-select: none;
}

.next-question-btn:hover {
  border-color: var(--primary);
  background: var(--primary-soft);
  box-shadow: var(--shadow-md);
}

.next-question-btn:active {
  transform: scale(0.97);
}

/* ---------- Input ---------- */
.chat-input {
  flex-shrink: 0;
  padding: 14px 20px 18px;
  border-top: 1px solid var(--border);
  background: var(--surface);
}

.input-box {
  border: 1px solid #e6e8eb;
  border-radius: var(--radius-md);
  background: #f7f8fa;
  transition: all 0.2s ease;
}

.input-box.active {
  border-color: var(--primary);
  background: var(--surface);
  box-shadow: 0 0 0 3px rgba(51, 112, 255, 0.08);
}

.input-box :deep(.el-textarea__inner) {
  box-shadow: none;
  background: transparent;
  border: none;
  padding: 12px 16px 2px;
  font-size: 14px;
  line-height: 1.6;
  resize: none;
}

.input-box :deep(.el-textarea__inner:focus) {
  box-shadow: none;
}

.input-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 2px 10px 8px 16px;
}

.input-tip {
  font-size: 12px;
  color: var(--text-4);
}

.send-btn {
  width: 34px;
  height: 34px;
  box-shadow: var(--shadow-sm);
}

.send-loading {
  width: 14px;
  height: 14px;
  border: 2px solid #fff;
  border-top-color: transparent;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* ---------- Action bar ---------- */

/* ---------- Dialog ---------- */
.model-dialog-body {
  padding: 4px 0;
}

.model-dialog-tip {
  margin: 0 0 16px;
  font-size: 13px;
  color: var(--text-3);
}

.model-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.model-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 13px 14px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.18s ease;
}

.model-item:hover {
  border-color: var(--primary);
}

.model-item.selected {
  border-color: var(--primary);
  background: var(--primary-soft);
}

.model-item.disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.model-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.model-info .model-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-1);
}

.model-info .model-desc {
  font-size: 12px;
  color: var(--text-3);
}

.conclusion-supplement {
  margin-top: 14px;
  padding: 14px 14px 16px;
  background: var(--primary-soft);
  border: 1px solid rgba(51, 112, 255, 0.22);
  border-radius: var(--radius-md);
}

.conclusion-supp-head {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 10px;
}

.conclusion-supp-icon {
  color: var(--primary);
  font-size: 15px;
}

.conclusion-supp-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-1);
}

.conclusion-supp-tag {
  margin-left: auto;
  font-size: 11px;
  color: var(--primary);
  background: var(--surface);
  border: 1px solid rgba(51, 112, 255, 0.25);
  border-radius: 10px;
  padding: 1px 8px;
}

.conclusion-supplement :deep(.el-textarea__inner) {
  background: var(--surface);
  border-color: rgba(51, 112, 255, 0.3);
  box-shadow: none;
}

.model-dialog-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid var(--border);
}

.selected-count {
  font-size: 13px;
  color: var(--text-2);
}

.model-dialog-actions {
  display: flex;
  gap: 10px;
}

.conclusion-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
  padding: 14px 20px 18px;
  border-top: 1px solid var(--border);
  background: var(--surface);
}

.conclusion-hint {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  margin-right: auto;
  font-size: 12.5px;
  color: var(--text-3);
}

.conclusion-hint .el-icon {
  color: #e6a23c;
  font-size: 14px;
}

/* ---------- Responsive ---------- */
@media (max-width: 900px) {
  .layout {
    grid-template-columns: 1fr;
    overflow-y: auto;
  }

  .panel {
    align-self: stretch;
  }

  .multi-responses {
    flex-direction: column;
  }

  .response-card {
    min-width: 100%;
    flex-basis: auto;
  }

  .stage-pill {
    display: none;
  }
}

/* ---------- History Drawer ---------- */
.history-drawer {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.history-item {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  overflow: hidden;
  transition: border-color 0.18s ease;
}

.history-item:hover {
  border-color: #d9dde3;
}

.history-head {
  padding: 14px 16px;
  cursor: pointer;
}

.history-head-top {
  display: flex;
  align-items: center;
  gap: 8px;
}

.history-version {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-1);
}

.history-status {
  font-size: 11px;
  padding: 1px 8px;
  border-radius: 999px;
  font-weight: 500;
}

.history-status.is-draft {
  background: var(--primary-soft);
  color: var(--primary);
}

.history-status.is-archived {
  background: #f2f3f5;
  color: var(--text-3);
}

.history-caret {
  margin-left: auto;
  color: var(--text-4);
  transition: transform 0.2s ease;
}

.history-caret.open {
  transform: rotate(180deg);
}

.history-sub {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 6px;
  font-size: 12px;
  color: var(--text-3);
}

.history-sep {
  color: var(--text-4);
}

.history-summary {
  margin-top: 8px;
  font-size: 13px;
  line-height: 1.5;
  color: var(--text-2);
}

.history-file-count {
  margin-top: 10px;
  font-size: 12px;
  color: var(--text-4);
}

.history-files {
  border-top: 1px solid var(--border);
  background: #fafbfc;
  padding: 6px 16px 10px;
}

.file-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 0;
  border-bottom: 1px dashed var(--border);
}

.file-row:last-child {
  border-bottom: none;
}

.file-badge {
  flex-shrink: 0;
  width: 34px;
  height: 34px;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.3px;
  background: var(--primary-soft);
  color: var(--primary);
}

.file-badge.ft-pdf {
  background: #fdeeee;
  color: #f5654a;
}

.file-badge.ft-doc {
  background: #eaf2fd;
  color: #2f7de0;
}

.file-badge.ft-xls {
  background: #eaf7ee;
  color: #1fa463;
}

.file-badge.ft-dia {
  background: #f3edfd;
  color: #8b5cf6;
}

.file-name {
  flex: 1;
  min-width: 0;
  font-size: 13px;
  color: var(--text-1);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.file-size {
  font-size: 12px;
  color: var(--text-4);
  flex-shrink: 0;
}

.file-view {
  flex-shrink: 0;
  border: none;
  background: none;
  cursor: pointer;
  color: var(--primary);
  font-size: 13px;
  padding: 4px 8px;
  border-radius: 6px;
  transition: background 0.18s ease;
}

.file-view:hover {
  background: var(--primary-soft);
}
</style>
