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
        <HistoryEntry :project-id="projectId" stage="DB" :snapshot="finalContent" />
        <template v-if="!readOnly">
          <el-button class="header-btn" @click="handleSaveDraft">
            <el-icon><DocumentChecked /></el-icon>
            <span>保存草稿</span>
          </el-button>
          <el-button type="primary" class="header-btn submit-header-btn" :loading="submitting" :disabled="!canSubmit" @click="handleSubmit">
            <span>确认数据库设计，进入下一阶段</span>
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
                  <div class="selected-models" v-if="sourceModelName && activeTab === 'md' && !isEditing">
                    <span class="sm-label">生成模型</span>
                    <span class="sm-chip">{{ sourceModelName }}</span>
                  </div>
                </div>
                <div class="doc-actions" v-if="!readOnly">
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
                      <span>取消</span>
                    </el-button>
                    <el-button text class="doc-action-btn" type="primary" @click="saveEdit">
                      <el-icon><Select /></el-icon><span>保存</span>
                    </el-button>
                  </template>
                </div>
              </div>

              <!-- 视图切换标签 -->
              <div class="tab-bar" v-if="finalContent.trim() && !isGenerating">
                <button class="tab-btn" :class="{ on: activeTab === 'tables' }" :disabled="!hasStructuredData" @click="activeTab = 'tables'">
                  <el-icon><Grid /></el-icon><span>表结构</span>
                </button>
                <button class="tab-btn" :class="{ on: activeTab === 'er' }" :disabled="!hasStructuredData" @click="activeTab = 'er'">
                  <el-icon><Share /></el-icon><span>ER 图</span>
                </button>
                <button class="tab-btn" :class="{ on: activeTab === 'ddl' }" :disabled="!ddlBlocks.length" @click="activeTab = 'ddl'">
                  <el-icon><Document /></el-icon><span>DDL 预览</span>
                </button>
                <button class="tab-btn" :class="{ on: activeTab === 'md' }" @click="activeTab = 'md'">
                  <el-icon><Memo /></el-icon><span>原文</span>
                </button>
                <span class="tab-count" v-if="hasStructuredData">{{ parsedTables.length }} 张表</span>
              </div>

              <div class="doc-content" :class="{ 'is-editing': isEditing }">
                <div v-if="!finalContent.trim() && !isGenerating" class="db-empty">
                  <el-icon :size="48" color="#c9cdd4"><Coin /></el-icon>
                  <p class="db-empty-title">数据库设计尚未生成</p>
                  <p class="db-empty-desc">AI 将基于 PRD 与技术方案生成数据库设计；生成后在「表结构 / DDL 预览」中复制 DDL 时可选择目标数据库方言（默认 MySQL）。</p>
                  <el-button v-if="!readOnly" type="primary" class="db-empty-btn" @click="startGenerate">
                    <el-icon><MagicStick /></el-icon><span>开始生成</span>
                  </el-button>
                </div>
                <div v-else-if="isGenerating && !finalContent.trim()" class="generating-tip">
                  <el-icon class="rotating"><Loading /></el-icon>
                  <span>正在生成数据库设计…</span>
                </div>

                <!-- 表结构视图 -->
                <div v-show="activeTab === 'tables' && finalContent.trim() && hasStructuredData" class="tables-view">
                  <div class="table-list">
                    <div class="table-list-head">表列表 ({{ parsedTables.length }})</div>
                    <div
                      v-for="t in parsedTables"
                      :key="t.name"
                      class="table-item"
                      :class="{ on: selectedTable === t.name }"
                      @click="selectedTable = t.name"
                    >
                      <span class="ti-dot" :class="tableColor(t.name)"></span>
                      <span class="ti-name">{{ t.name }}</span>
                      <span class="ti-count">{{ t.columns.length }}</span>
                    </div>
                  </div>
                  <div class="table-detail" v-if="selectedTableObj">
                    <div class="td-head">
                      <div class="td-head-left">
                        <span class="td-name">{{ selectedTableObj.name }}</span>
                        <span class="td-comment" v-if="selectedTableObj.comment">· {{ selectedTableObj.comment }}</span>
                        <span class="td-engine" v-if="dbType">{{ dbType }}</span>
                      </div>
                      <div class="td-actions">
                        <el-button text size="small" class="td-add" v-if="!readOnly" @click="openAddField">
                          <el-icon><Plus /></el-icon><span>添加字段</span>
                        </el-button>
                        <el-button v-if="ddlBlocks.length" text size="small" class="td-copy" @click="copyDdl(selectedTableObj)">
                          <el-icon><CopyDocument /></el-icon><span>复制 DDL</span>
                        </el-button>
                      </div>
                    </div>
                    <div class="td-scroll">
                      <div class="td-tip" v-if="!selectedTableObj.columns.length">该表暂无可解析的字段（可能来自 Markdown 章节而非 DDL）。</div>
                      <table v-else class="td-table">
                        <thead>
                          <tr><th>字段名</th><th>类型</th><th>可空</th><th>键</th><th>默认值</th><th>注释</th></tr>
                        </thead>
                        <tbody>
                          <tr v-for="(c, i) in selectedTableObj.columns" :key="i">
                            <td class="col-name">{{ c.name }}</td>
                            <td><span class="col-type">{{ c.type }}</span></td>
                            <td><span class="badge" :class="c.nullable === 'N' ? 'badge-nn' : 'badge-null'">{{ c.nullable }}</span></td>
                            <td>
                              <span v-if="c.isPk" class="badge badge-pk">PK</span>
                              <span v-else-if="c.isUnique" class="badge badge-uq">UQ</span>
                              <span v-else-if="isFkCol(selectedTableObj, c.name)" class="badge badge-fk">FK</span>
                            </td>
                            <td class="col-default">{{ c.default || '—' }}</td>
                            <td class="col-comment">{{ c.comment || '—' }}</td>
                          </tr>
                        </tbody>
                      </table>

                      <div class="td-sec" v-if="selectedTableObj.indexes && selectedTableObj.indexes.length">
                        <div class="td-sec-title">索引</div>
                        <div v-for="(idx, i) in selectedTableObj.indexes" :key="'i' + i" class="idx-row">
                          <span class="badge" :class="idx.type === 'UNIQUE' ? 'badge-uq' : 'badge-idx'">{{ idx.type === 'UNIQUE' ? 'UQ' : 'IDX' }}</span>
                          <span class="idx-name">{{ idx.name }}</span>
                          <span class="idx-cols">{{ (idx.columns || []).join(', ') }}</span>
                        </div>
                      </div>

                      <div class="td-sec" v-if="selectedTableObj.foreignKeys && selectedTableObj.foreignKeys.length">
                        <div class="td-sec-title">外键</div>
                        <div v-for="(fk, i) in selectedTableObj.foreignKeys" :key="'f' + i" class="fk-row">
                          <span class="fk-col">{{ fk.column }}</span>
                          <span class="fk-arrow">→</span>
                          <span class="fk-ref" @click="selectedTable = fk.refTable">{{ fk.refTable }}.{{ fk.refColumn }}</span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- ER 图视图 -->
                <div v-show="activeTab === 'er' && finalContent.trim() && hasStructuredData" ref="erRef" class="er-view"></div>
                <div v-show="activeTab === 'er' && finalContent.trim() && !hasStructuredData" class="er-empty-tip">
                  未检测到 CREATE TABLE 语句，无法生成 ER 图。
                </div>

                <!-- DDL 预览视图 -->
                <div v-show="activeTab === 'ddl' && finalContent.trim() && ddlBlocks.length" class="ddl-view">
                  <div v-for="(sql, i) in ddlBlocks" :key="i" class="ddl-block">
                    <div class="ddl-toolbar">
                      <span class="ddl-fname">{{ ddlTableName(sql) }}.sql</span>
                      <el-button text size="small" class="ddl-copy" @click="requestCopyDdl(sql)">
                        <el-icon><CopyDocument /></el-icon><span>复制</span>
                      </el-button>
                    </div>
                    <pre class="ddl-code" v-html="highlightSql(sql)"></pre>
                  </div>
                </div>
                <div v-show="activeTab === 'ddl' && finalContent.trim() && !ddlBlocks.length" class="er-empty-tip">
                  未检测到 DDL 代码块（```sql ... ```）。
                </div>

                <!-- 原文视图 -->
                <div v-show="activeTab === 'md' && !isEditing && finalContent.trim()" ref="previewRef" class="doc-markdown markdown-body md-body" v-html="renderMarkdown(finalContent)"></div>
                <div v-show="activeTab === 'md' && isEditing" class="db-editor">
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
              <div class="chat-model" :class="{ 'is-locked': readOnly }" @click="!readOnly && openModelDialog()">
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
                  <div class="msg-text md-body" v-html="msg.role === 'ai' ? renderMarkdown(msg.content) : escapeHtml(msg.content)"></div>
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
              <div v-if="readOnly" class="chat-locked-note">
                <el-icon><Lock /></el-icon>
                <span>该阶段已锁定，仅可查看历史对话</span>
              </div>
              <div class="input-row">
                <div class="input-wrap">
                  <el-input v-model="chatInput" type="textarea" :rows="2" resize="none" :disabled="chatGenerating || readOnly" placeholder="输入你的问题…" @keydown.enter.prevent="sendChat" />
                </div>
                <el-button type="primary" class="chat-send" :disabled="!chatInput.trim() || chatGenerating || readOnly" @click="sendChat">
                  <el-icon><Promotion /></el-icon>
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- 复制 DDL 方言选择弹窗 -->
    <el-dialog v-model="showCopyDialog" title="复制 DDL" width="480px" :close-on-click-modal="false" align-center>
      <div class="copy-dialect-body">
        <p class="copy-dialect-tip">选择目标数据库方言，复制时会自动转换为对应语法（默认 MySQL）。</p>
        <div class="db-type-list">
          <div v-for="item in copyDialectOptions" :key="item.value" class="db-type-card" :class="{ active: copyDialect === item.value }" @click="copyDialect = item.value">
            <div class="db-type-info">
              <span class="db-type-name">{{ item.label }}</span>
            </div>
            <el-icon v-if="copyDialect === item.value" class="stack-check"><Select /></el-icon>
          </div>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showCopyDialog = false">取消</el-button>
          <el-button type="primary" @click="confirmCopyDdl">复制</el-button>
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

    <!-- 添加字段弹窗 -->
    <el-dialog v-model="showAddField" title="添加字段" width="520px" :close-on-click-modal="false" align-center>
      <div class="add-field-body">
        <div class="af-target">目标表：<b class="af-table">{{ addTargetTable }}</b></div>
        <el-form :model="fieldForm" label-width="80px" class="af-form">
          <el-form-item label="字段名" required>
            <el-input v-model="fieldForm.name" placeholder="如 creator" />
          </el-form-item>
          <el-form-item label="类型" required>
            <el-select v-model="fieldForm.type" filterable allow-create default-first-option placeholder="选择或输入类型" style="width:100%">
              <el-option v-for="t in fieldTypeOptions" :key="t" :label="t" :value="t" />
            </el-select>
          </el-form-item>
          <el-form-item label="可空">
            <el-switch v-model="fieldForm.nullable" active-text="允许为空" inactive-text="NOT NULL" />
          </el-form-item>
          <el-form-item label="默认值">
            <el-input v-model="fieldForm.default" placeholder="可选，如 '' / 0 / CURRENT_TIMESTAMP" />
          </el-form-item>
          <el-form-item label="注释">
            <el-input v-model="fieldForm.comment" placeholder="字段说明" />
          </el-form-item>
          <el-form-item label="约束">
            <el-checkbox v-model="fieldForm.isPk">主键 PK</el-checkbox>
            <el-checkbox v-model="fieldForm.isUnique">唯一 UQ</el-checkbox>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showAddField = false">取消</el-button>
          <el-button type="primary" @click="confirmAddField">确认添加</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="StepDb">
import { ref, reactive, computed, onMounted, nextTick, watch, getCurrentInstance } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getProject } from '@/api/ai/project'
import { Lock } from '@element-plus/icons-vue'
import { getDbModels, getDbDoc, saveDbDoc, generateDb, submitDb } from '@/api/ai/db'
import HistoryEntry from '@/views/portal/components/HistoryEntry.vue'
import { sendChatMessage } from '@/api/ai/chat'
import { renderMarkdown } from '@/utils/markdown'

const { proxy } = getCurrentInstance()
const router = useRouter()
const route = useRoute()
const projectId = computed(() => route.params.id)

const loading = ref(false)
const project = ref({})
const currentStep = ref('DB')
const submitting = ref(false)

// 阶段已"过去"判定：项目当前阶段在我这一阶之后 → 整页只读锁定
const readOnly = computed(() => {
  const order = ['REQ', 'CLARIFY', 'PRD', 'PROTO', 'ARCH', 'TECH', 'DB', 'DONE']
  const cur = order.indexOf(currentStep.value)
  const mine = order.indexOf('DB')
  return cur > mine
})

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

// 只读时强制退出编辑态（隐藏可写工具栏 / 编辑器）
watch(readOnly, (ro) => { if (ro) isEditing.value = false })

// 复制 DDL 时可选的目标方言（仅 SQL 方言，Redis/MongoDB 等无 DDL 故不列入）
const copyDialectOptions = [
  { value: 'MySQL', label: 'MySQL' },
  { value: 'PostgreSQL', label: 'PostgreSQL' },
  { value: 'SQLServer', label: 'SQL Server' },
  { value: 'Oracle', label: 'Oracle' },
  { value: 'SQLite', label: 'SQLite' }
]

const showModelDialog = ref(false)
const tempSelectedIds = ref([])

// 拖拽分栏
const splitPercent = ref(52)
const dragging = ref(false)

// 文档视图标签：tables / er / ddl / md
const activeTab = ref('tables')
const selectedTable = ref('')

// ER 图容器
const erRef = ref(null)
let mermaidLib = null

// ===== 结构化解析 =====
const ddlBlocks = computed(() => extractDdlBlocks(finalContent.value))
const parsedTables = computed(() => parseDbDoc(finalContent.value))
const hasStructuredData = computed(() => parsedTables.value.length > 0)
const selectedTableObj = computed(() => {
  const t = parsedTables.value.find(x => x.name === selectedTable.value)
  return t || parsedTables.value[0] || null
})

watch(parsedTables, (list) => {
  if (list.length && (!selectedTable.value || !list.find(t => t.name === selectedTable.value))) {
    selectedTable.value = list[0].name
  }
})
watch(activeTab, (tab) => {
  if (tab === 'er') renderEr()
})
watch(finalContent, () => {
  if (activeTab.value === 'er') renderEr()
})

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
    loading.value = false
  }).catch(() => { loading.value = false })
}

function loadModels() {
  getDbModels().then(res => {
    const data = res?.data ?? res
    const models = data?.models || []
    if (models.length) {
      modelOptions.value = models
      selectedModels.value = [models[0].modelId]
      if (!chatModel.value.value) chatModel.value = { value: models[0].modelId, label: models[0].modelName }
    }
    // 后端无启用模型时不做本地兜底，由「开始生成」的「请先选择模型」提示引导用户配置
  }).catch(() => {})
}

function loadDoc() {
  return getDbDoc(projectId.value).then(res => {
    const doc = res?.data ?? res
    if (doc && doc.content) {
      finalContent.value = doc.content
      dbType.value = doc.dbType || dbType.value
      mainModelId.value = doc.sourceModel || ''
      hasGenerated.value = true
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
// ===== 模型弹窗 =====
function openModelDialog() {
  if (readOnly.value) return
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
  if (readOnly.value) return
  if (isGenerating.value || !selectedModels.value.length) {
    if (!selectedModels.value.length) proxy.$modal.msgWarning('请先选择模型')
    return
  }
  finalContent.value = ''
  hasGenerated.value = true
  isGenerating.value = true
  activeTab.value = 'md'
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
        if (activeTab.value === 'md') scrollPreview()
      },
      onModelDone: () => {},
      onAllDone: () => {
        isGenerating.value = false
        genController = null
        persistDb()
        if (parsedTables.value.length) activeTab.value = 'tables'
        else if (ddlBlocks.value.length) activeTab.value = 'ddl'
        else activeTab.value = 'md'
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
  saveDbDoc(payload).catch((e) => { if (import.meta.env.DEV) console.warn('保存数据库设计失败', e) })
}

function handleSaveDraft() {
  if (readOnly.value) return
  persistDb()
  proxy.$modal.msgSuccess('草稿已保存')
}

function handleSubmit() {
  if (readOnly.value) return
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

function enterEdit() { if (readOnly.value) return; isEditing.value = true; editBackup.value = finalContent.value }
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
  if (!q || chatGenerating.value || readOnly.value) return
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

/* HTML 转义（SQL 高亮等非 Markdown 场景使用） */
function escapeHtml(s) { return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;') }

/* ============ DDL / 结构化解析 ============ */

// 提取 ```sql ... ``` 代码块
function extractDdlBlocks(md) {
  if (!md) return []
  const re = /```sql\s*([\s\S]*?)```/gi
  const blocks = []
  let m
  while ((m = re.exec(md))) {
    const sql = m[1].trim()
    if (sql) blocks.push(sql)
  }
  return blocks
}

// 将一个 DDL 块按 CREATE TABLE 语句拆分为多条（每条以 ; 结尾）
// 注意：模型常在 CREATE TABLE 前加 `-- 注释`，故需先裁掉行首注释再判定
function splitCreateTables(sql) {
  return sql.split(';')
    .map(s => s.trim())
    .map(s => {
      // 去掉语句前的注释/空行，定位 CREATE TABLE 真正起点
      const idx = s.search(/CREATE\s+TABLE/i)
      return idx >= 0 ? s.slice(idx) : ''
    })
    .filter(s => /^CREATE\s+TABLE/i.test(s))
    .map(s => s + ';')
}

// 解析整篇文档为表数组（优先 DDL，回退 Markdown 表格）
function parseDbDoc(md) {
  if (!md) return []
  const tables = []
  extractDdlBlocks(md).forEach(b => {
    splitCreateTables(b).forEach(stmt => {
      const t = parseCreateTable(stmt)
      if (t) tables.push(t)
    })
  })
  if (tables.length) return tables
  return parseMarkdownTables(md)
}

// 解析单个 CREATE TABLE
function parseCreateTable(sql) {
  const createMatch = sql.match(/CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?[`"]?([A-Za-z0-9_]+)[`"]?\s*\(/i)
  if (!createMatch) return null
  const tableName = createMatch[1]

  const tblComment = sql.match(/\)[^;]*COMMENT\s*=\s*'([^']*)'/i)
  const tableComment = tblComment ? tblComment[1] : ''

  const startIdx = sql.indexOf('(', createMatch.index) + 1
  let depth = 1
  let endIdx = startIdx
  for (let i = startIdx; i < sql.length; i++) {
    if (sql[i] === '(') depth++
    else if (sql[i] === ')') {
      depth--
      if (depth === 0) { endIdx = i; break }
    }
  }
  const inner = sql.slice(startIdx, endIdx)
  const parts = splitTopLevel(inner, ',')

  const columns = []
  const indexes = []
  const foreignKeys = []
  const pkColumns = []
  const uniqueColumns = {}

  for (const raw of parts) {
    const line = raw.trim()
    if (!line) continue
    const upper = line.toUpperCase()
    if (/^(ENGINE|DEFAULT CHARSET|CHARSET|COLLATE|AUTO_INCREMENT|COMMENT)\b/.test(upper)) continue
    if (upper.startsWith('PRIMARY KEY')) { pkColumns.push(...extractParenCols(line)); continue }
    if (upper.startsWith('UNIQUE KEY') || upper.startsWith('UNIQUE INDEX')) {
      const idxName = (line.match(/KEY\s+[`"]?([A-Za-z0-9_]+)/i) || [])[1] || ''
      const cols = extractParenCols(line)
      indexes.push({ name: idxName, type: 'UNIQUE', columns: cols })
      cols.forEach(c => { uniqueColumns[c] = true })
      continue
    }
    if (/^(KEY|INDEX|FULLTEXT|SPATIAL)\b/.test(upper)) {
      const idxName = (line.match(/(?:KEY|INDEX)\s+[`"]?([A-Za-z0-9_]+)/i) || [])[1] || ''
      indexes.push({ name: idxName, type: 'INDEX', columns: extractParenCols(line) })
      continue
    }
    if (upper.startsWith('CONSTRAINT') || upper.startsWith('FOREIGN KEY')) {
      const fk = parseForeignKey(line)
      if (fk) foreignKeys.push(fk)
      continue
    }
    const col = parseColumn(line)
    if (col) columns.push(col)
  }

  columns.forEach(c => {
    if (pkColumns.includes(c.name)) c.isPk = true
    if (uniqueColumns[c.name]) c.isUnique = true
  })

  return { name: tableName, comment: tableComment, columns, indexes, foreignKeys }
}

// 按顶层逗号切分（忽略括号内的逗号）
function splitTopLevel(str, sep) {
  const parts = []
  let depth = 0
  let cur = ''
  for (let i = 0; i < str.length; i++) {
    const ch = str[i]
    if (ch === '(') depth++
    else if (ch === ')') depth--
    if (ch === sep && depth === 0) { parts.push(cur); cur = '' }
    else cur += ch
  }
  if (cur.trim()) parts.push(cur)
  return parts
}

// 提取 (a, b, c) 中的列名
function extractParenCols(line) {
  const m = line.match(/\(([^)]*)\)/)
  if (!m) return []
  return m[1].split(',').map(s => {
    s = s.trim()
    const n = s.match(/[`"]?([A-Za-z0-9_]+)/)
    return n ? n[1] : s
  })
}

// 解析外键行
function parseForeignKey(line) {
  const colM = line.match(/FOREIGN\s+KEY\s+\(?[`"]?([A-Za-z0-9_]+)[`"]?\)?/i)
  const refM = line.match(/REFERENCES\s+[`"]?([A-Za-z0-9_]+)[`"]?\s*\(?[`"]?([A-Za-z0-9_]+)/i)
  if (colM && refM) {
    return { column: colM[1], refTable: refM[1], refColumn: refM[2] }
  }
  return null
}

// 解析列定义
function parseColumn(line) {
  const nameM = line.match(/^[`"]?([A-Za-z0-9_]+)[`"]?\s+/)
  if (!nameM) return null
  const name = nameM[1]
  const rest = line.slice(nameM[0].length)
  const typeM = rest.match(/^([A-Za-z0-9_]+(?:\s*\([^)]*\))?)/i)
  const type = typeM ? typeM[1].replace(/\s+/g, ' ').toUpperCase() : ''
  const remainder = typeM ? rest.slice(typeM[0].length) : rest
  const upper = remainder.toUpperCase()
  const nullable = /\bNOT\s+NULL\b/.test(upper) ? 'N' : 'Y'
  const autoInc = /\bAUTO_INCREMENT\b/.test(upper)
  const defaultM = remainder.match(/DEFAULT\s+([^\s,]+)/i)
  let defaultVal = defaultM ? defaultM[1].replace(/^['"`]|['"`]$/g, '') : ''
  if (autoInc) defaultVal = defaultVal || 'AUTO_INC'
  const commentM = remainder.match(/COMMENT\s+'((?:[^'\\]|\\.)*)'/i)
  const comment = commentM ? commentM[1].replace(/\\'/g, "'") : ''
  const beforeComment = remainder.split(/\bCOMMENT\b/i)[0]
  const upc = beforeComment.toUpperCase()
  const isPkInline = /\bPRIMARY\s+KEY\b/.test(upc)
  const isUniqueInline = /\bUNIQUE\b/.test(upc)
  return { name, type, nullable, default: defaultVal, comment, isPk: isPkInline, isUnique: isUniqueInline }
}

// Markdown 表格回退解析（第 5 章核心表结构）
function parseMarkdownTables(md) {
  const lines = md.split('\n')
  const tables = []
  let cur = null
  let headers = []
  for (const rawLine of lines) {
    const line = rawLine.trim()
    const h = line.match(/^#{3,4}\s+.*?([A-Za-z0-9_]+)[）)]\s*(.*)$/i)
    if (h) {
      if (cur) tables.push(cur)
      cur = { name: h[1], comment: (h[2] || '').replace(/^[（(]/, '').trim(), columns: [], indexes: [], foreignKeys: [] }
      headers = []
      continue
    }
    if (cur && line.startsWith('|')) {
      const cells = line.replace(/^\|/, '').replace(/\|$/, '').split('|').map(s => s.trim())
      if (!cells.length) continue
      if (cells.every(c => /^[-:]+$/.test(c))) { headers = []; continue }
      if (headers.length === 0) { headers = cells; continue }
      const get = (...keys) => {
        for (const k of keys) {
          const idx = headers.findIndex(hh => hh.replace(/\s/g, '').includes(k.replace(/\s/g, '')))
          if (idx >= 0) return cells[idx] || ''
        }
        return ''
      }
      const colName = get('字段名', '字段', '列名', '列')
      if (!colName) continue
      const key = get('键', '主键', '约束')
      const note = get('说明', '注释')
      cur.columns.push({
        name: colName,
        type: (get('类型') || '').toUpperCase(),
        nullable: (get('可空') || '').toUpperCase().includes('N') ? 'N' : 'Y',
        default: get('默认值') || '',
        comment: note,
        isPk: (key.toUpperCase().includes('PK') || note.includes('主键')),
        isUnique: (key.toUpperCase().includes('U') || note.includes('唯一'))
      })
    }
  }
  if (cur) tables.push(cur)
  return tables.filter(t => t.columns.length)
}

// 表名 → 颜色（确定性哈希）
function tableColor(name) {
  let h = 0
  for (let i = 0; i < name.length; i++) h = (h * 31 + name.charCodeAt(i)) >>> 0
  return 'ti-dot-' + (h % 6)
}

function isFkCol(table, colName) {
  return (table.foreignKeys || []).some(fk => fk.column === colName)
}

// ===== 手动添加字段 =====
const showAddField = ref(false)
const addTargetTable = ref('')
const fieldForm = reactive({ name: '', type: 'VARCHAR(64)', nullable: true, default: '', comment: '', isPk: false, isUnique: false })
const fieldTypeOptions = ['BIGINT(20)', 'INT', 'TINYINT', 'VARCHAR(64)', 'VARCHAR(255)', 'CHAR(1)', 'TEXT', 'DATETIME', 'TIMESTAMP', 'DATE', 'DECIMAL(10,2)', 'BOOLEAN', 'JSON']

function escapeRegExp(s) { return s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&') }

// 将 CREATE TABLE 内部按顶层逗号拆为带偏移的片段（忽略引号内的逗号）
function splitTopLevelParts(str) {
  const parts = []
  let depth = 0, cur = '', start = 0, inQuote = null
  for (let i = 0; i < str.length; i++) {
    const ch = str[i]
    if (inQuote) {
      cur += ch
      if (ch === inQuote) inQuote = null
      continue
    }
    if (ch === "'" || ch === '"' || ch === '`') { inQuote = ch; cur += ch; continue }
    if (ch === '(') depth++
    else if (ch === ')') depth--
    if (ch === ',' && depth === 0) { parts.push({ text: cur.trim(), start, end: i }); cur = ''; start = i + 1 }
    else cur += ch
  }
  if (cur.trim()) parts.push({ text: cur.trim(), start, end: str.length })
  return parts
}

// 构建一行列定义的 DDL
function buildColumnDdlLine(f) {
  let line = `${f.name} ${f.type}`
  if (!f.nullable) line += ' NOT NULL'
  if (f.isPk) line += ' PRIMARY KEY'
  else if (f.isUnique) line += ' UNIQUE'
  if (f.default !== '' && f.default != null) {
    const d = String(f.default).trim()
    const upper = d.toUpperCase()
    const noQuote = /^-?\d+(\.\d+)?$/.test(d) || ['NULL', 'TRUE', 'FALSE', 'CURRENT_TIMESTAMP', 'NOW()'].includes(upper)
    line += ' DEFAULT ' + (noQuote ? d : `'${d.replace(/'/g, "''")}'`)
  }
  if (f.comment) line += ` COMMENT '${f.comment.replace(/'/g, "''")}'`
  return line
}

// 在指定 CREATE TABLE 的最后一列后插入新字段，返回新文档；找不到返回 null
function insertColumnIntoDdl(md, tableName, colLine) {
  const re = new RegExp('CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?[`"]?' + escapeRegExp(tableName) + '[`"]?\\s*\\(', 'i')
  const m = md.match(re)
  if (!m) return null
  const startParen = m.index + m[0].length
  let depth = 1, endParen = -1
  for (let i = startParen; i < md.length; i++) {
    if (md[i] === '(') depth++
    else if (md[i] === ')') { depth--; if (depth === 0) { endParen = i; break } }
  }
  if (endParen < 0) return null
  const inner = md.slice(startParen, endParen)
  const parts = splitTopLevelParts(inner)
  let lastColIdx = -1
  for (let i = 0; i < parts.length; i++) {
    const up = parts[i].text.toUpperCase()
    const isConstraint = /^(PRIMARY KEY|UNIQUE KEY|UNIQUE INDEX|KEY|INDEX|FULLTEXT|SPATIAL|CONSTRAINT|FOREIGN KEY|ENGINE|DEFAULT CHARSET|CHARSET|COLLATE|AUTO_INCREMENT|COMMENT)\b/.test(up)
    if (!isConstraint) lastColIdx = i
  }
  if (lastColIdx < 0) return null
  const last = parts[lastColIdx]
  // 插入点 = 该列定义最后一个非空白字符之后（避免把逗号插进注释/类型里）
  const partRaw = inner.slice(last.start, last.end)
  const colEndInInner = last.start + partRaw.trimEnd().length
  const insertAt = startParen + colEndInInner
  const insertText = ',\n  ' + colLine
  return md.slice(0, insertAt) + insertText + md.slice(insertAt)
}

function openAddField() {
  if (readOnly.value) return
  if (!selectedTableObj.value) return
  addTargetTable.value = selectedTableObj.value.name
  Object.assign(fieldForm, { name: '', type: 'VARCHAR(64)', nullable: true, default: '', comment: '', isPk: false, isUnique: false })
  showAddField.value = true
}

function confirmAddField() {
  const name = (fieldForm.name || '').trim()
  const type = (fieldForm.type || '').trim()
  if (!name) { proxy.$modal.msgWarning('请填写字段名'); return }
  if (!/^[A-Za-z0-9_]+$/.test(name)) { proxy.$modal.msgWarning('字段名仅支持字母、数字、下划线'); return }
  if (!type) { proxy.$modal.msgWarning('请选择或填写字段类型'); return }
  const target = addTargetTable.value
  const colLine = buildColumnDdlLine({ name, type, nullable: fieldForm.nullable, default: fieldForm.default, comment: fieldForm.comment, isPk: fieldForm.isPk, isUnique: fieldForm.isUnique })
  const updated = insertColumnIntoDdl(finalContent.value, target, colLine)
  if (updated == null) {
    proxy.$modal.msgWarning('未在该表 DDL 中找到 CREATE TABLE，请到「原文」视图手动添加')
    return
  }
  finalContent.value = updated
  showAddField.value = false
  proxy.$modal.msgSuccess(`已为 ${target} 添加字段：${name}`)
}

// ER 图源（mermaid erDiagram）
function buildErSource() {
  let src = 'erDiagram\n'
  parsedTables.value.forEach(t => {
    src += `  ${t.name} {\n`
    t.columns.slice(0, 8).forEach(c => {
      const tag = c.isPk ? 'PK' : (c.isUnique ? 'UK' : (isFkCol(t, c.name) ? 'FK' : ''))
      const tp = (c.type || 'VARCHAR').replace(/[^A-Za-z0-9]/g, '')
      src += `    ${tp} ${c.name} ${tag}\n`
    })
    src += '  }\n'
  })
  parsedTables.value.forEach(t => {
    ;(t.foreignKeys || []).forEach(fk => {
      src += `  ${t.name} ||--o{ ${fk.refTable} : "${fk.column}"\n`
    })
  })
  return src
}

function loadMermaidScript() {
  return new Promise((resolve, reject) => {
    if (window.mermaid) return resolve(window.mermaid)
    const s = document.createElement('script')
    s.src = 'https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.min.js'
    s.onload = () => resolve(window.mermaid)
    s.onerror = () => reject(new Error('mermaid 脚本加载失败'))
    document.head.appendChild(s)
  })
}

async function renderEr() {
  if (!erRef.value) return
  if (!parsedTables.value.length) {
    erRef.value.innerHTML = ''
    return
  }
  try {
    const m = await loadMermaidScript()
    m.initialize({
      startOnLoad: false,
      theme: 'base',
      securityLevel: 'loose',
      themeVariables: {
        primaryColor: '#e8f3ff',
        primaryTextColor: '#1d2129',
        primaryBorderColor: '#3370ff',
        lineColor: '#86909c',
        secondaryColor: '#fafbfc',
        tertiaryColor: '#fff',
        fontSize: '12px'
      },
      er: { entityPadding: 6, strokeWidth: 1, diagramPadding: 16 }
    })
    const src = buildErSource()
    const { svg } = await m.render('er-' + Date.now(), src)
    erRef.value.innerHTML = svg
  } catch (e) {
    erRef.value.innerHTML = '<div class="er-empty-tip">ER 图渲染失败：' + (e && e.message ? e.message : e) + '</div>'
  }
}

// SQL 语法高亮
function highlightSql(sql) {
  if (!sql) return ''
  const kw = /\b(CREATE|TABLE|IF|NOT|EXISTS|NULL|PRIMARY|KEY|UNIQUE|INDEX|FOREIGN|REFERENCES|DEFAULT|CONSTRAINT|ENGINE|CHARSET|COLLATE|AUTO_INCREMENT|COMMENT|ON|UPDATE|CURRENT_TIMESTAMP|INNODB|UNSIGNED|ZEROFILL)\b/gi
  const tp = /\b(BIGINT|INT|INTEGER|TINYINT|SMALLINT|MEDIUMINT|DECIMAL|NUMERIC|FLOAT|DOUBLE|VARCHAR|CHAR|TEXT|LONGTEXT|MEDIUMTEXT|TINYTEXT|DATE|DATETIME|TIMESTAMP|TIME|YEAR|BOOLEAN|BOOL|JSON|BLOB|ENUM)\b/gi
  let out = escapeHtml(sql)
  const slots = []
  out = out.replace(/'[^']*'/g, m => { slots.push(m); return `\u0000${slots.length - 1}\u0000` })
  out = out.replace(/--[^\n]*/g, m => { slots.push(m); return `\u0000${slots.length - 1}\u0000` })
  out = out.replace(kw, m => `<span class="sql-kw">${m}</span>`)
  out = out.replace(tp, m => `<span class="sql-tp">${m}</span>`)
  out = out.replace(/\u0000(\d+)\u0000/g, (_, i) => {
    const s = slots[+i]
    if (s.startsWith('--')) return `<span class="sql-cm">${s}</span>`
    return `<span class="sql-st">${s}</span>`
  })
  return out
}

function ddlTableName(sql) {
  const m = sql.match(/CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?[`"]?([A-Za-z0-9_]+)/i)
  return m ? m[1] : 'schema'
}

function copyText(text) {
  const done = () => proxy.$modal.msgSuccess('已复制')
  if (navigator.clipboard && navigator.clipboard.writeText) {
    navigator.clipboard.writeText(text).then(done).catch(() => fallbackCopy(text, done))
  } else {
    fallbackCopy(text, done)
  }
}

function fallbackCopy(text, done) {
  const ta = document.createElement('textarea')
  ta.value = text
  ta.style.position = 'fixed'
  ta.style.opacity = '0'
  document.body.appendChild(ta)
  ta.select()
  try { document.execCommand('copy'); done() } catch (e) { proxy.$modal.msgError('复制失败') }
  document.body.removeChild(ta)
}

// 复制 DDL 前先让用户选择目标方言（默认 MySQL），再做最佳努力的方言转换
const showCopyDialog = ref(false)
const copyTargetText = ref('')
const copyDialect = ref('MySQL')

function requestCopyDdl(text) {
  copyTargetText.value = text || ''
  copyDialect.value = 'MySQL'
  showCopyDialog.value = true
}

function confirmCopyDdl() {
  const out = convertDdlToDialect(copyTargetText.value, copyDialect.value)
  showCopyDialog.value = false
  copyText(out)
}

function copyDdl(table) {
  const block = ddlBlocks.value.find(sql => ddlTableName(sql) === table.name)
  if (block) requestCopyDdl(block)
  else proxy.$modal.msgWarning('未找到该表 DDL')
}

// 将 MySQL 风格 CREATE TABLE 转换为目标方言（最佳努力，覆盖常见生成形态）
function convertDdlToDialect(sql, target) {
  if (!target || target === 'MySQL') return sql
  const t = String(target).toLowerCase()
  const isPg = t === 'postgresql'
  const isMs = t === 'sqlserver' || t === 'mssql'
  const isOra = t === 'oracle'
  const isSqlite = t === 'sqlite'
  const needsQuote = isPg || isMs || isOra
  const stripInlineComment = isPg || isMs || isOra
  const commentOn = []

  const nameM = sql.match(/CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?[`"]?([A-Za-z0-9_]+)/i)
  const tableName = nameM ? nameM[1] : null

  const re = /CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?[`"]?[A-Za-z0-9_]+[`"]?\s*\(/i
  const m = sql.match(re)
  if (!m) return sql
  const startParen = m.index + m[0].length
  let depth = 1, endParen = -1
  for (let i = startParen; i < sql.length; i++) {
    if (sql[i] === '(') depth++
    else if (sql[i] === ')') { depth--; if (depth === 0) { endParen = i; break } }
  }
  if (endParen < 0) return sql
  const head = sql.slice(0, startParen)
  const inner = sql.slice(startParen, endParen)
  const tail = sql.slice(endParen)

  const parts = splitTopLevelParts(inner)
  const converted = parts.map(p => {
    let line = p.text
    const up = line.toUpperCase()
    const isConstraint = /^(PRIMARY KEY|UNIQUE KEY|UNIQUE INDEX|KEY|INDEX|FULLTEXT|SPATIAL|CONSTRAINT|FOREIGN KEY)\b/.test(up)
    if (isConstraint) return line

    // AUTO_INCREMENT → 各方言自增写法
    if (/AUTO_INCREMENT/i.test(line)) {
      const isPk = /\bPRIMARY\s+KEY\b/i.test(line)
      line = line.replace(/\s*AUTO_INCREMENT\b/i, '')
      if (isPg) line = line.replace(/\b(BIGINT|INT|SMALLINT|INTEGER|MEDIUMINT|TINYINT)\b(\(\d+\))?/i, '$1 GENERATED BY DEFAULT AS IDENTITY')
      else if (isOra) line = line.replace(/\b(BIGINT|INT|SMALLINT|INTEGER|MEDIUMINT|TINYINT)\b(\(\d+\))?/i, 'NUMBER(19) GENERATED BY DEFAULT AS IDENTITY')
      else if (isMs) line = line.replace(/\b(BIGINT|INT|SMALLINT|INTEGER|MEDIUMINT|TINYINT)\b(\(\d+\))?/i, '$1 IDENTITY(1,1)')
      else if (isSqlite && isPk) {
        line = line.replace(/\b(BIGINT|INT|SMALLINT|INTEGER|MEDIUMINT|TINYINT)\b(\(\d+\))?/i, 'INTEGER PRIMARY KEY AUTOINCREMENT')
        line = line.replace(/\bNOT\s+NULL\b/i, '')
      }
    }
    // 类型映射
    if (isPg || isMs || isOra) line = line.replace(/\bDATETIME\b/i, 'TIMESTAMP')
    if (isPg || isMs || isOra) line = line.replace(/\bTINYINT\(1\)\b/i, 'BOOLEAN')
    line = line.replace(/\bJSON\b/i, isPg ? 'JSONB' : isOra ? 'CLOB' : isMs ? 'NVARCHAR(MAX)' : isSqlite ? 'TEXT' : 'JSON')
    if (isOra) line = line.replace(/\bTEXT\b/i, 'CLOB')
    if (isMs) line = line.replace(/\bTEXT\b/i, 'NVARCHAR(MAX)')
    // 列内联 COMMENT → 方言改为 COMMENT ON COLUMN
    const cm = line.match(/COMMENT\s+'((?:[^'\\]|\\.)*)'/i)
    if (cm && stripInlineComment && tableName) {
      const colM = line.match(/^[`"]?([A-Za-z0-9_]+)[`"]?\s/)
      const colName = colM ? colM[1] : null
      line = line.replace(/\s*COMMENT\s+'((?:[^'\\]|\\.)*)'/i, '')
      if (colName) {
        const qcol = needsQuote ? `"${tableName}"."${colName}"` : `${tableName}.${colName}`
        commentOn.push(`COMMENT ON COLUMN ${qcol} IS '${cm[1].replace(/'/g, "''")}';`)
      }
    }
    return line
  })

  // 清理 MySQL 表级选项
  let tailClean = tail
  tailClean = tailClean.replace(/\s*ENGINE\s*=\s*[A-Za-z0-9_]+/i, '')
  tailClean = tailClean.replace(/\s*DEFAULT\s+CHARSET\s*=\s*[A-Za-z0-9_]+/i, '')
  tailClean = tailClean.replace(/\s*CHARSET\s*=\s*[A-Za-z0-9_]+/i, '')
  tailClean = tailClean.replace(/\s*COLLATE\s*=\s*[A-Za-z0-9_]+/i, '')
  tailClean = tailClean.replace(/\s*AUTO_INCREMENT\s*=\s*\d+/i, '')
  tailClean = tailClean.replace(/\s*COMMENT\s*=\s*'((?:[^'\\]|\\.)*)'/i, (mm, c) => {
    if (stripInlineComment && tableName) {
      const qt = needsQuote ? `"${tableName}"` : tableName
      commentOn.push(`COMMENT ON TABLE ${qt} IS '${c.replace(/'/g, "''")}';`)
    }
    return ''
  })
  tailClean = tailClean.replace(/^\s*;\s*$/, '')

  let result = head.trimEnd() + '\n  ' + converted.join(',\n  ') + tailClean.trim()
  if (!result.trim().endsWith(';')) result = result.trim() + ';'
  // 统一反引号：PG/MSSQL/Oracle 用双引号，SQLite 去掉
  result = result.replace(/`/g, needsQuote ? '"' : '')
  if (commentOn.length) result += '\n\n' + commentOn.join('\n')
  return result
}

onMounted(() => {
  getProjectInfo()
  loadModels()
  loadDoc().then(() => {
    if (finalContent.value.trim()) {
      activeTab.value = parsedTables.value.length ? 'tables' : (ddlBlocks.value.length ? 'ddl' : 'md')
    } else {
      activeTab.value = 'tables'
    }
  })
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
.doc-action-btn .el-icon { margin-right: 3px; }

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

/* 视图标签栏 */
.tab-bar {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 0 0 8px;
  border-bottom: 1px solid #f2f3f5;
  flex-shrink: 0;
}
.tab-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 12px;
  font-size: 13px;
  color: #86909c;
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  cursor: pointer;
  transition: all 0.18s ease;
}
.tab-btn .el-icon { font-size: 14px; }
.tab-btn:hover { color: #3370ff; }
.tab-btn.on { color: #3370ff; border-bottom-color: #3370ff; font-weight: 500; }
.tab-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.tab-btn:disabled:hover { color: #86909c; }
.tab-count {
  margin-left: auto;
  font-size: 11px;
  color: #86909c;
  background: #f2f3f5;
  padding: 2px 8px;
  border-radius: 999px;
}

.doc-content { flex: 1; min-height: 0; position: relative; padding-top: 12px; display: flex; flex-direction: column; }
.doc-content.is-editing { padding-top: 0; }

/* 表结构视图 */
.tables-view { flex: 1; min-height: 0; display: flex; gap: 0; }
.table-list {
  width: 168px;
  flex-shrink: 0;
  border-right: 1px solid #f2f3f5;
  background: #fafbfc;
  padding: 8px 6px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.table-list-head {
  font-size: 11px;
  color: #c9cdd4;
  padding: 2px 8px 6px;
  letter-spacing: 0.04em;
}
.table-item {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 7px 10px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 12.5px;
  color: #4e5969;
  transition: all 0.15s ease;
}
.table-item:hover { background: #f2f3f5; }
.table-item.on { background: #e8f3ff; color: #3370ff; }
.ti-dot { width: 7px; height: 7px; border-radius: 50%; flex-shrink: 0; background: #c9cdd4; }
.ti-dot-0 { background: #3370ff; }
.ti-dot-1 { background: #00b42a; }
.ti-dot-2 { background: #ba7517; }
.ti-dot-3 { background: #a32d2d; }
.ti-dot-4 { background: #534ab7; }
.ti-dot-5 { background: #0f6e56; }
.ti-name { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-family: var(--font-mono, 'SF Mono', Consolas, monospace); }
.ti-count { font-size: 10px; color: #c9cdd4; background: #f2f3f5; padding: 1px 5px; border-radius: 4px; flex-shrink: 0; }
.table-item.on .ti-count { background: #cce0ff; color: #185fa5; }

.table-detail { flex: 1; min-width: 0; display: flex; flex-direction: column; overflow: hidden; }
.td-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 4px 4px 12px;
}
.td-head-left { display: flex; align-items: baseline; gap: 8px; min-width: 0; flex-wrap: wrap; }
.td-name { font-size: 16px; font-weight: 500; color: #1d2129; font-family: var(--font-mono, 'SF Mono', Consolas, monospace); }
.td-comment { font-size: 12px; color: #86909c; }
.td-engine { font-size: 11px; color: #185fa5; background: #e8f3ff; padding: 2px 7px; border-radius: 4px; }
.td-copy { font-size: 12px; color: #4e5969; flex-shrink: 0; }
.td-copy:hover { color: #3370ff; }
.td-actions { display: flex; align-items: center; gap: 4px; flex-shrink: 0; }
.td-add { font-size: 12px; color: #3370ff; flex-shrink: 0; }
.td-add:hover { background: rgba(51,112,255,0.06); }
.td-add .el-icon { margin-right: 3px; }
.add-field-body { padding: 4px 2px; }
.af-target { font-size: 13px; color: #4e5969; margin-bottom: 14px; }
.af-table { font-family: var(--font-mono, 'SF Mono', Consolas, monospace); color: #3370ff; }
.af-form :deep(.el-input__inner), .af-form :deep(.el-textarea__inner) { border-radius: 8px; }
.af-form .el-checkbox { margin-right: 18px; }
.td-copy .el-icon { margin-right: 3px; }
.td-scroll { flex: 1; min-height: 0; overflow-y: auto; padding-right: 4px; }
.td-tip { font-size: 12px; color: #86909c; padding: 8px 4px; }

.td-table { width: 100%; border-collapse: collapse; }
.td-table th {
  padding: 6px 8px;
  font-size: 11px;
  color: #86909c;
  font-weight: 400;
  text-align: left;
  border-bottom: 1px solid #e5e6eb;
  text-transform: uppercase;
  letter-spacing: 0.03em;
  position: sticky;
  top: 0;
  background: #fff;
}
.td-table td { padding: 6px 8px; font-size: 12.5px; border-bottom: 0.5px solid #f2f3f5; color: #1d2129; }
.col-name { font-family: var(--font-mono, 'SF Mono', Consolas, monospace); font-weight: 500; }
.col-type {
  font-family: var(--font-mono, 'SF Mono', Consolas, monospace);
  font-size: 11.5px;
  color: #534ab7;
  background: #eeedfe;
  padding: 1px 6px;
  border-radius: 3px;
}
.col-default { color: #86909c; font-family: var(--font-mono, 'SF Mono', Consolas, monospace); font-size: 11.5px; }
.col-comment { color: #4e5969; }
.badge { font-size: 10px; padding: 1px 5px; border-radius: 3px; font-weight: 500; }
.badge-nn { background: #fcebeb; color: #a32d2d; }
.badge-null { background: #f2f3f5; color: #86909c; }
.badge-pk { background: #fff3e0; color: #ba7517; }
.badge-uq { background: #e8f3ff; color: #185fa5; }
.badge-fk { background: #eaf3de; color: #3b6d11; }
.badge-idx { background: #eaf3de; color: #3b6d11; }

.td-sec { margin-top: 14px; }
.td-sec-title { font-size: 12px; font-weight: 500; color: #4e5969; margin-bottom: 6px; }
.idx-row { display: flex; align-items: center; gap: 8px; font-size: 12px; color: #86909c; padding: 2px 0; }
.idx-name { font-family: var(--font-mono, 'SF Mono', Consolas, monospace); color: #4e5969; }
.idx-cols { font-family: var(--font-mono, 'SF Mono', Consolas, monospace); }
.fk-row { display: flex; align-items: center; gap: 6px; font-size: 12px; color: #86909c; padding: 2px 0; }
.fk-col { font-family: var(--font-mono, 'SF Mono', Consolas, monospace); color: #4e5969; }
.fk-arrow { color: #c9cdd4; }
.fk-ref { font-family: var(--font-mono, 'SF Mono', Consolas, monospace); color: #3370ff; cursor: pointer; }
.fk-ref:hover { text-decoration: underline; }

/* ER 图视图 */
.er-view { flex: 1; min-height: 0; overflow: auto; display: flex; align-items: center; justify-content: center; background: #fafbfc; }
.er-view :deep(svg) { max-width: 100%; height: auto; }
.er-empty-tip { flex: 1; display: flex; align-items: center; justify-content: center; font-size: 13px; color: #86909c; padding: 24px; text-align: center; }

/* DDL 预览视图 */
.ddl-view { flex: 1; min-height: 0; overflow-y: auto; display: flex; flex-direction: column; gap: 14px; }
.ddl-block { border: 0.5px solid #e5e6eb; border-radius: 8px; overflow: hidden; }
.ddl-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 12px;
  background: #fafbfc;
  border-bottom: 0.5px solid #eee;
}
.ddl-fname { font-size: 12px; color: #4e5969; font-family: var(--font-mono, 'SF Mono', Consolas, monospace); display: flex; align-items: center; gap: 6px; }
.ddl-fname::before { content: 'S'; display: inline-flex; align-items: center; justify-content: center; width: 15px; height: 15px; border-radius: 3px; background: #3370ff; color: #fff; font-size: 9px; font-weight: 600; }
.ddl-copy { font-size: 12px; color: #4e5969; }
.ddl-copy:hover { color: #3370ff; }
.ddl-copy .el-icon { margin-right: 3px; }
.ddl-code {
  margin: 0;
  padding: 12px 16px;
  background: #fff;
  font-family: var(--font-mono, 'SF Mono', Consolas, monospace);
  font-size: 12px;
  line-height: 1.75;
  white-space: pre;
  color: #1d2129;
  overflow-x: auto;
}
.sql-kw { color: #534ab7; font-weight: 500; }
.sql-tp { color: #ba7517; }
.sql-st { color: #3b6d11; }
.sql-cm { color: #c9cdd4; font-style: italic; }

/* 原文 / 编辑器 / 空态 / 生成中 */
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
/* v-html 插入的内容不带 data-v 属性，scoped 下必须用 :deep() 才能命中 */
.markdown-body { padding: 2px 4px; font-size: 14px; line-height: 1.75; color: #1d2129; }
.markdown-body :deep(h1) { font-size: 20px; margin: 4px 0 10px; padding-bottom: 8px; border-bottom: 2px solid #eef0f3; color: #1d2129; }
.markdown-body :deep(h2) { font-size: 17px; margin: 18px 0 8px; color: #272e3b; font-weight: 600; }
.markdown-body :deep(h3) { font-size: 15px; margin: 14px 0 6px; color: #333d4d; font-weight: 600; }
.markdown-body :deep(p) { margin: 6px 0; }
.markdown-body :deep(ul), .markdown-body :deep(ol) { margin: 6px 0; padding-left: 20px; }
.markdown-body :deep(li) { margin: 3px 0; }
.markdown-body :deep(blockquote) { margin: 10px 0; padding: 8px 14px; background: linear-gradient(135deg, #f7f8fb 0%, #f0f2f5 100%); border-left: 3px solid #3370ff; color: #4e5969; border-radius: 0 8px 8px 0; }
.markdown-body :deep(code) { background: #f0f1f4; padding: 1px 6px; border-radius: 4px; font-size: 12.5px; color: #d6326e; font-family: 'SF Mono', Consolas, monospace; }
.markdown-body :deep(strong) { color: #1d2129; font-weight: 600; }
.markdown-body :deep(table) { width: 100%; border-collapse: collapse; margin: 10px 0; font-size: 13.5px; }
.markdown-body :deep(th), .markdown-body :deep(td) { border: 1px solid #e5e6eb; padding: 7px 12px; text-align: left; }
.markdown-body :deep(th) { background: #f7f8fa; font-weight: 600; color: #1d2129; }
.markdown-body :deep(tr:nth-child(even) td) { background: #fafbfc; }
.markdown-body :deep(pre) { background: #f7f8fa; border-radius: 6px; padding: 12px 14px; overflow-x: auto; margin: 10px 0; }
.markdown-body :deep(pre code) { background: none; padding: 0; color: #1d2129; font-size: 13px; line-height: 1.55; }

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
.copy-dialect-body { padding: 4px 2px; }
.copy-dialect-tip { font-size: 13px; color: #4e5969; margin: 0 0 14px; line-height: 1.5; }
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
  .tables-view { flex-direction: column; }
  .table-list { width: 100%; flex-direction: row; flex-wrap: wrap; border-right: none; border-bottom: 1px solid #f2f3f5; }
}

/* 只读锁定态（阶段已过去） */
.ro-tag { display:inline-flex; align-items:center; gap:6px; height:auto; padding:5px 12px; font-size:13px; font-weight:500; color:#3370ff; white-space:nowrap; vertical-align:middle; background:linear-gradient(180deg,#f5f9ff 0%,#eef4ff 100%); border:1px solid #c5d9ff; border-radius:20px; box-shadow:0 1px 2px rgba(51,112,255,0.06); }
.ro-tag .el-icon { display:inline-flex; align-items:center; justify-content:center; width:18px; height:18px; padding:0; border-radius:50%; background:#3370ff; color:#fff; flex-shrink:0; }
.ro-tag .el-icon svg { width:12px; height:12px; }
.chat-locked-note { display:inline-flex; align-items:center; gap:8px; padding:10px 14px; font-size:13px; font-weight:500; color:#3370ff; white-space:nowrap; vertical-align:middle; background:linear-gradient(180deg,#f5f9ff 0%,#eef4ff 100%); border:1px solid #c5d9ff; border-radius:20px; box-shadow:0 1px 2px rgba(51,112,255,0.06); }
.chat-locked-note .el-icon { display:inline-flex; align-items:center; justify-content:center; width:18px; height:18px; padding:0; border-radius:50%; background:#3370ff; color:#fff; flex-shrink:0; }
.chat-locked-note .el-icon svg { width:12px; height:12px; }
.chat-model.is-locked { opacity: 0.5; cursor: not-allowed; }
</style>
