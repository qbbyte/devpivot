<template>
  <div class="kb-page">
    <header class="kb-header">
      <div class="header-left">
        <button class="back-link" @click="goProject">
          <el-icon><ArrowLeft /></el-icon>
          <span>返回项目</span>
        </button>
        <span class="header-divider"></span>
        <span class="header-title">{{ scopeTitle }}</span>
        <span class="header-sub">{{ scopeSub }}</span>
      </div>
      <div class="header-right">
        <el-button class="save-btn" @click="goProject">
          <el-icon><Document /></el-icon>
          <span>项目详情</span>
        </el-button>
      </div>
    </header>

    <main class="kb-main">
      <div class="kb-content">
        <!-- 范围切换 + 阶段切换 -->
        <section class="stage-section">
          <div class="scope-bar">
            <el-radio-group v-model="scope" @change="onScopeChange">
              <el-radio-button label="project">本项目知识库</el-radio-button>
              <el-radio-button label="shared">组织共享库</el-radio-button>
            </el-radio-group>
            <span class="scope-hint">{{ scopeHint }}</span>
          </div>
          <el-tabs v-model="activeStage" @tab-change="onStageChange">
            <el-tab-pane label="全局" name="" />
            <el-tab-pane label="需求" name="REQ" />
            <el-tab-pane label="澄清" name="CLARIFY" />
            <el-tab-pane label="PRD" name="PRD" />
            <el-tab-pane label="原型" name="PROTO" />
            <el-tab-pane label="技术方案" name="TECH" />
            <el-tab-pane label="数据库" name="DB" />
          </el-tabs>
        </section>

        <div class="main-grid">
          <!-- 左栏：上传 + 检索预览 -->
          <aside class="left-col">
            <section class="card upload-card">
              <h3 class="section-title">
                <el-icon><UploadFilled /></el-icon>
                <span>上传知识文档</span>
              </h3>
              <el-alert
                class="stage-tip"
                :title="stageTip"
                type="info"
                :closable="false"
                show-icon
              />
              <div class="form-block">
                <span class="block-label">文档标题</span>
                <el-input v-model="uploadForm.title" placeholder="例如：支付模块业务规则说明" maxlength="200" />
              </div>
              <div class="form-block">
                <span class="block-label">文档内容</span>
                <span class="block-hint">将按段落切片并建立全文索引，支持自然语言检索</span>
                <el-input
                  v-model="uploadForm.content"
                  type="textarea"
                  :rows="10"
                  placeholder="粘贴或输入领域知识、业务规则、术语表、历史需求等内容..."
                />
              </div>
              <div class="upload-actions">
                <el-button type="primary" :loading="uploading" @click="handleUpload">
                  <el-icon><Check /></el-icon>
                  <span>上传并索引</span>
                </el-button>
              </div>
            </section>

            <section class="card retrieve-card">
              <h3 class="section-title">
                <el-icon><Search /></el-icon>
                <span>检索预览</span>
              </h3>
              <div class="form-block">
                <span class="block-label">检索语句</span>
                <el-input
                  v-model="retrieveForm.query"
                  type="textarea"
                  :rows="3"
                  placeholder="模拟 AI 生成时的检索意图，例如：支付流程需要支持哪些渠道？"
                />
              </div>
              <div class="upload-actions">
                <el-button :loading="retrieving" @click="handleRetrieve">
                  <el-icon><Search /></el-icon>
                  <span>检索</span>
                </el-button>
              </div>
              <div v-if="retrieveResult" class="retrieve-result">
                <div class="retrieve-label">将注入提示词的知识上下文：</div>
                <pre class="retrieve-text">{{ retrieveResult }}</pre>
              </div>
            </section>
          </aside>

          <!-- 右栏：文档列表 -->
          <div class="right-col">
            <section class="card list-card">
              <div class="list-head">
                <h3 class="section-title" style="margin-bottom:0;border:none;padding-bottom:0;">
                  <el-icon><Files /></el-icon>
                  <span>已索引文档</span>
                </h3>
                <span class="list-count">共 {{ docList.length }} 篇</span>
              </div>

              <div v-if="loading" class="list-loading">
                <el-icon class="is-loading"><Loading /></el-icon>
                <span>加载中...</span>
              </div>
              <div v-else-if="docList.length === 0" class="list-empty">
                <el-icon><DocumentAdd /></el-icon>
                <p>当前阶段暂无知识文档，上传后 AI 生成时将自动参考</p>
              </div>
              <div v-else class="doc-list">
                <div v-for="doc in docList" :key="doc.docId" class="doc-item">
                  <div class="doc-main">
                    <div class="doc-title-row">
                      <span class="doc-title">{{ doc.title }}</span>
                      <el-tag v-if="doc.projectId === -1" size="small" type="success">共享</el-tag>
                      <el-tag size="small" :type="doc.sourceType === 'pipeline' ? 'warning' : 'info'">
                        {{ doc.sourceType === 'pipeline' ? '流水线' : '手动' }}
                      </el-tag>
                    </div>
                    <div class="doc-meta">
                      <span class="doc-stage">{{ stageLabel(doc.stage) }}</span>
                      <span class="doc-dot">·</span>
                      <span>{{ doc.chunkCount }} 切片</span>
                      <span class="doc-dot">·</span>
                      <span>{{ formatTime(doc.createTime) }}</span>
                    </div>
                    <div v-if="doc.originalText" class="doc-snippet">{{ snippet(doc.originalText) }}</div>
                  </div>
                  <el-button
                    class="doc-del"
                    type="danger"
                    link
                    :icon="Delete"
                    @click="handleDelete(doc)"
                  >删除</el-button>
                </div>
              </div>
            </section>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup name="ProjectKnowledge">
import { ref, reactive, computed, onMounted, getCurrentInstance } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import { listKbDocs, uploadKbDoc, deleteKbDoc, previewKbRetrieve } from '@/api/ai/kb'

const router = useRouter()
const route = useRoute()
const { proxy } = getCurrentInstance()

const projectId = computed(() => route.params.id)

const stageOptions = [
  { value: '', label: '全局' },
  { value: 'REQ', label: '需求' },
  { value: 'CLARIFY', label: '澄清' },
  { value: 'PRD', label: 'PRD' },
  { value: 'PROTO', label: '原型' },
  { value: 'TECH', label: '技术方案' },
  { value: 'DB', label: '数据库' }
]

const activeStage = ref('')
const scope = ref('project') // project = 本项目知识库；shared = 组织共享库
const loading = ref(false)
const uploading = ref(false)
const retrieving = ref(false)
const docList = ref([])
const retrieveResult = ref('')

const uploadForm = reactive({ title: '', content: '' })
const retrieveForm = reactive({ query: '' })

const stageTip = computed(() => {
  if (!activeStage.value) {
    return '当前为「全局」库：所有阶段生成时都会参考此处知识。'
  }
  return `当前为「${stageLabel(activeStage.value)}」阶段库：仅该阶段生成时优先参考，同时也会参考全局库。`
})

function stageLabel(stage) {
  const hit = stageOptions.find(s => s.value === stage)
  return hit ? hit.label : '全局'
}

const scopeTitle = computed(() => scope.value === 'shared' ? '组织共享知识库' : '项目知识库')
const scopeSub = computed(() => scope.value === 'shared'
  ? '组织级通用知识，对所有项目的对应阶段生效'
  : '按阶段沉淀领域知识，让每个阶段的 AI 生成更精准')
const scopeHint = computed(() => scope.value === 'shared'
  ? '共享库对所有项目生效，请谨慎维护'
  : '仅当前项目可见')

function onScopeChange() {
  retrieveResult.value = ''
  loadDocs()
}

function goProject() {
  router.push('/portal/project/' + projectId.value)
}

function onStageChange() {
  retrieveResult.value = ''
  loadDocs()
}

function loadDocs() {
  loading.value = true
  const params = { stage: activeStage.value || undefined }
  if (scope.value === 'shared') {
    params.shared = true
  } else {
    params.projectId = projectId.value
  }
  listKbDocs(params)
    .then(res => {
      docList.value = res.data || []
    })
    .finally(() => {
      loading.value = false
    })
}

function handleUpload() {
  if (!uploadForm.content.trim()) {
    proxy?.$modal.msgWarning('请填写文档内容')
    return
  }
  uploading.value = true
  const payload = {
    stage: activeStage.value || null,
    title: uploadForm.title.trim() || '未命名文档',
    content: uploadForm.content
  }
  if (scope.value === 'shared') {
    payload.shared = true
  } else {
    payload.projectId = projectId.value
  }
  uploadKbDoc(payload).then(() => {
    proxy?.$modal.msgSuccess('索引成功')
    uploadForm.title = ''
    uploadForm.content = ''
    loadDocs()
  }).finally(() => {
    uploading.value = false
  })
}

function handleRetrieve() {
  if (!retrieveForm.query.trim()) {
    proxy?.$modal.msgWarning('请填写检索语句')
    return
  }
  retrieving.value = true
  previewKbRetrieve({
    projectId: scope.value === 'shared' ? -1 : projectId.value,
    stage: activeStage.value || undefined,
    query: retrieveForm.query
  }).then(res => {
    const ctx = res.data && res.data.context
    retrieveResult.value = ctx && ctx.trim() ? ctx : '（无命中，该阶段生成时将不注入知识上下文）'
  }).finally(() => {
    retrieving.value = false
  })
}

function handleDelete(doc) {
  ElMessageBox.confirm('确认删除文档「' + doc.title + '」？该文档的切片将一并删除。', '删除确认', {
    type: 'warning'
  }).then(() => {
    deleteKbDoc(doc.docId).then(() => {
      proxy?.$modal.msgSuccess('已删除')
      loadDocs()
    })
  }).catch(() => {})
}

function snippet(text) {
  const t = text.replace(/\s+/g, ' ').trim()
  return t.length > 80 ? t.slice(0, 80) + '...' : t
}

function formatTime(t) {
  if (!t) return '-'
  return String(t).replace('T', ' ').slice(0, 19)
}

onMounted(() => {
  loadDocs()
})
</script>

<style scoped>
.kb-page {
  min-height: 100vh;
  background: #f7f8fa;
  display: flex;
  flex-direction: column;
}

.kb-header {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 52px;
  padding: 0 24px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(8px);
  border-bottom: 1px solid #ebedf0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: none;
  background: none;
  color: #646a73;
  font-size: 13px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: color 0.2s, background 0.2s;
}

.back-link:hover {
  color: #3370ff;
  background: rgba(51, 112, 255, 0.06);
}

.header-divider {
  width: 1px;
  height: 16px;
  background: #e5e7eb;
}

.header-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2329;
}

.header-sub {
  font-size: 12px;
  color: #a0a4ad;
}

.save-btn {
  border-radius: 8px;
  color: #646a73;
  border-color: #dee0e3;
}

.save-btn:hover {
  color: #3370ff;
  border-color: #3370ff;
}

.kb-main {
  flex: 1;
  padding: 24px 24px 80px;
}

.kb-content {
  max-width: 1280px;
  margin: 0 auto;
}

.stage-section {
  margin-bottom: 20px;
  padding: 4px 24px 0;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.scope-bar {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 0 4px;
  flex-wrap: wrap;
}

.scope-hint {
  font-size: 12px;
  color: #a0a4ad;
}

.main-grid {
  display: grid;
  grid-template-columns: 460px 1fr;
  gap: 24px;
  align-items: start;
}

.card {
  padding: 24px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.left-col {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 20px;
  font-size: 15px;
  font-weight: 600;
  color: #1f2329;
  padding-bottom: 16px;
  border-bottom: 1px solid #f5f6f8;
}

.section-title .el-icon {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: linear-gradient(135deg, #3370ff, #5b8cff);
  color: #fff;
  padding: 6px;
  box-shadow: 0 2px 6px rgba(51, 112, 255, 0.25);
}

.stage-tip {
  margin-bottom: 20px;
  border-radius: 8px;
}

.form-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}

.block-label {
  font-size: 14px;
  font-weight: 500;
  color: #1f2329;
}

.block-hint {
  font-size: 12px;
  color: #a0a4ad;
}

.upload-actions {
  display: flex;
  justify-content: flex-end;
}

.retrieve-result {
  margin-top: 16px;
  padding: 14px;
  background: #f7f9ff;
  border: 1px solid #e1e9ff;
  border-radius: 8px;
}

.retrieve-label {
  font-size: 12px;
  color: #646a73;
  margin-bottom: 8px;
}

.retrieve-text {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
  font-family: inherit;
  font-size: 13px;
  line-height: 1.7;
  color: #1f2329;
  max-height: 280px;
  overflow: auto;
}

.list-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f5f6f8;
}

.list-count {
  font-size: 12px;
  color: #a0a4ad;
}

.list-loading,
.list-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 48px 0;
  color: #a0a4ad;
  font-size: 13px;
}

.list-empty .el-icon {
  font-size: 40px;
  color: #c9cdd4;
}

.doc-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.doc-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 16px;
  border: 1px solid #f0f1f3;
  border-radius: 10px;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.doc-item:hover {
  border-color: #c9d6ff;
  box-shadow: 0 2px 10px rgba(51, 112, 255, 0.08);
}

.doc-main {
  flex: 1;
  min-width: 0;
}

.doc-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.doc-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2329;
}

.doc-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #a0a4ad;
  margin-bottom: 8px;
}

.doc-stage {
  color: #3370ff;
}

.doc-dot {
  color: #d4d7de;
}

.doc-snippet {
  font-size: 12px;
  color: #86909c;
  line-height: 1.6;
}

.doc-del {
  flex-shrink: 0;
}

@media (max-width: 900px) {
  .main-grid {
    grid-template-columns: 1fr;
  }
}
</style>
