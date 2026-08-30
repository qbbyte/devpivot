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
          <!-- 左栏：上传 + 检索预览（共享库仅管理员维护，门户隐藏上传卡片） -->
          <aside class="left-col">
            <section
              v-if="scope !== 'shared'"
              class="card upload-card"
              :class="{ 'drag-over': dragActive }"
              @dragover.prevent="dragActive = true"
              @dragleave.prevent="dragActive = false"
              @drop.prevent="onDropFile"
            >
              <div v-if="dragActive" class="drag-mask">
                <el-icon><UploadFilled /></el-icon>
                <span>松开导入 .txt / .md 文件</span>
              </div>
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
                <span class="block-label">文档标题<span class="label-optional">选填，默认"未命名文档"</span></span>
                <el-input v-model="uploadForm.title" placeholder="例如：支付模块业务规则说明" maxlength="200" />
              </div>
              <div class="form-block">
                <span class="block-label">文档内容</span>
                <span class="block-hint">
                  将按段落切片并建立全文索引，支持自然语言检索
                  <span v-if="chunkEstimate" class="chunk-estimate">{{ chunkEstimate }}</span>
                </span>
                <el-input
                  v-model="uploadForm.content"
                  type="textarea"
                  :rows="10"
                  :maxlength="20000"
                  show-word-limit
                  placeholder="粘贴或输入领域知识、业务规则、术语表、历史需求等内容，也可直接拖入 .txt / .md 文件..."
                  @keydown.ctrl.enter="handleUpload"
                  @keydown.meta.enter="handleUpload"
                />
              </div>
              <div class="upload-actions">
                <el-button type="primary" :loading="uploading" @click="handleUpload">
                  <el-icon><Promotion /></el-icon>
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
                  @keydown.ctrl.enter="handleRetrieve"
                  @keydown.meta.enter="handleRetrieve"
                />
              </div>
              <div class="upload-actions">
                <el-button type="primary" plain :loading="retrieving" @click="handleRetrieve">
                  <el-icon><Search /></el-icon>
                  <span>检索</span>
                </el-button>
              </div>
              <div v-if="retrieveSegments.length" class="retrieve-result">
                <div class="retrieve-label-row">
                  <span class="retrieve-label">将注入提示词的知识上下文（{{ retrieveSegments.length }} 个切片）：</span>
                  <el-button link type="primary" size="small" :icon="DocumentCopy" @click="copyContext">复制</el-button>
                </div>
                <div v-for="(seg, i) in retrieveSegments" :key="i" class="retrieve-seg">
                  <span class="seg-tag">{{ seg.label }}</span>
                  <pre class="retrieve-text">{{ seg.text }}</pre>
                </div>
              </div>
              <div v-else-if="retrieveResult" class="retrieve-result">
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
                <el-button v-if="scope !== 'shared'" type="primary" plain size="small" @click="scrollToUpload">
                  <el-icon><UploadFilled /></el-icon>
                  <span>去上传文档</span>
                </el-button>
              </div>
              <template v-else>
                <TransitionGroup name="doc" tag="div" class="doc-list">
                  <div v-for="doc in shownDocs" :key="doc.docId" class="doc-item">
                    <div class="doc-main">
                      <div class="doc-title-row">
                        <span class="doc-title doc-title-link" @click="openView(doc)">{{ doc.title }}</span>
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
                </TransitionGroup>
                <div v-if="docList.length > PAGE_SIZE" class="list-more">
                  <el-button link type="primary" @click="expanded = !expanded">
                    {{ expanded ? '收起列表' : '显示全部 ' + docList.length + ' 篇' }}
                  </el-button>
                </div>
              </template>
            </section>
          </div>
        </div>
      </div>
    </main>

    <!-- 文档全文查看 -->
    <el-dialog :title="viewDoc ? viewDoc.title : '文档详情'" v-model="viewOpen" width="720px" append-to-body>
      <div v-if="viewDoc" class="view-meta">
        <el-tag size="small" type="info">{{ stageLabel(viewDoc.stage) }}</el-tag>
        <el-tag v-if="viewDoc.projectId === -1" size="small" type="success">共享</el-tag>
        <el-tag size="small" :type="viewDoc.sourceType === 'pipeline' ? 'warning' : 'info'">
          {{ viewDoc.sourceType === 'pipeline' ? '流水线' : '手动' }}
        </el-tag>
        <span class="view-meta-text">{{ viewDoc.chunkCount }} 切片 · {{ formatTime(viewDoc.createTime) }}</span>
      </div>
      <pre class="view-text">{{ viewDoc && viewDoc.originalText ? viewDoc.originalText : '（无正文内容）' }}</pre>
    </el-dialog>
  </div>
</template>

<script setup name="ProjectKnowledge">
import { ref, reactive, computed, onMounted, getCurrentInstance } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { Delete, DocumentCopy } from '@element-plus/icons-vue'
import { listKbDocs, uploadPortalKbDoc, deletePortalKbDoc, previewKbRetrieve } from '@/api/ai/kb'

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

// 文档全文查看
const viewOpen = ref(false)
const viewDoc = ref(null)

// 列表分页展示（默认最多 20 条，可展开全部）
const PAGE_SIZE = 20
const expanded = ref(false)
const shownDocs = computed(() => (expanded.value ? docList.value : docList.value.slice(0, PAGE_SIZE)))

// 检索结果按切片分段（后端以 --- 分隔，段首为【参考资料-阶段】）
const retrieveSegments = computed(() => {
  const ctx = retrieveResult.value
  if (!ctx) return []
  return ctx
    .split(/\n-{3,}\n/)
    .map(s => s.trim())
    .filter(Boolean)
    .map(seg => {
      const m = seg.match(/^【参考资料-([^】]*)】\s*\n?/)
      return m
        ? { label: m[1] || '全局', text: seg.slice(m[0].length).trim() }
        : { label: '片段', text: seg }
    })
})

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
  expanded.value = false
  loadDocs()
}

function goProject() {
  router.push('/portal/project/' + projectId.value)
}

function onStageChange() {
  retrieveResult.value = ''
  expanded.value = false
  loadDocs()
}

/** 打开文档全文查看弹窗 */
function openView(doc) {
  viewDoc.value = doc
  viewOpen.value = true
}

/** 滚动定位到上传卡片（空状态引导） */
function scrollToUpload() {
  const el = document.querySelector('.upload-card')
  if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

// 拖拽文件导入
const dragActive = ref(false)

function onDropFile(e) {
  dragActive.value = false
  const file = e.dataTransfer && e.dataTransfer.files && e.dataTransfer.files[0]
  if (!file) return
  if (!/\.(txt|md|markdown)$/i.test(file.name)) {
    proxy?.$modal.msgWarning('仅支持 .txt / .md 文本文件')
    return
  }
  if (file.size > 200 * 1024) {
    proxy?.$modal.msgWarning('文件过大，请控制在 200KB 内或分段粘贴')
    return
  }
  const reader = new FileReader()
  reader.onload = () => {
    uploadForm.content = String(reader.result || '').slice(0, 20000)
    if (!uploadForm.title.trim()) {
      uploadForm.title = file.name.replace(/\.(txt|md|markdown)$/i, '')
    }
    proxy?.$modal.msgSuccess('已读取文件：' + file.name)
  }
  reader.readAsText(file, 'utf-8')
}

// 切片数预估（后端约 500 字/段切片）
const chunkEstimate = computed(() => {
  const len = uploadForm.content.trim().length
  if (!len) return ''
  return '预计生成约 ' + Math.max(1, Math.ceil(len / 500)) + ' 个切片'
})

// 复制检索上下文
function copyContext() {
  const text = retrieveResult.value || ''
  if (!text) return
  const done = () => proxy?.$modal.msgSuccess('已复制到剪贴板')
  if (navigator.clipboard && window.isSecureContext) {
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
  try {
    document.execCommand('copy')
    done()
  } catch (e) {
    proxy?.$modal.msgError('复制失败，请手动选择复制')
  }
  document.body.removeChild(ta)
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
  uploadPortalKbDoc(payload).then(() => {
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
    deletePortalKbDoc(doc.docId).then(() => {
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
  if (typeof t === 'number') {
    const d = new Date(t)
    const p = n => (n < 10 ? '0' + n : '' + n)
    return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate()) + ' ' + p(d.getHours()) + ':' + p(d.getMinutes()) + ':' + p(d.getSeconds())
  }
  return String(t).replace('T', ' ').slice(0, 19)
}

onMounted(() => {
  loadDocs()
})
</script>

<style scoped>
.kb-page {
  min-height: 100vh;
  background: #f5f7fa;
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
  padding: 0 20px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(8px);
  border-bottom: 1px solid #eceef1;
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
  color: #4e8abe;
  background: rgba(115, 169, 216, 0.12);
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

.kb-main {
  flex: 1;
  padding: 0 0 64px;
}

.kb-content {
  width: 100%;
  margin: 0;
}

.stage-section {
  position: sticky;
  top: 52px;
  z-index: 9;
  margin-bottom: 0;
  padding: 4px 20px;
  background: #fff;
  border-radius: 0;
  border: none;
  border-bottom: 1px solid #eceef1;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.02);
}

.scope-bar {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 10px 0 4px;
  flex-wrap: wrap;
}

.scope-hint {
  font-size: 12px;
  color: #a0a4ad;
}

.main-grid {
  display: grid;
  grid-template-columns: 460px 1fr;
  gap: 16px;
  align-items: start;
  padding-top: 16px;
}

.card {
  padding: 20px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #edf0f3;
}

/* 左栏作为统一白色面板：两卡合并为一张，用细分隔线替代卡片间隔 */
.left-col {
  position: sticky;
  top: 68px;
  max-height: calc(100vh - 84px);
  overflow-y: auto;
  scrollbar-width: thin;
  display: flex;
  flex-direction: column;
  gap: 0;
  background: #fff;
  border: 1px solid #edf0f3;
  border-radius: 8px;
}

/* 左栏内部卡片取消独立外框：继承面板白背景，只保留上下 padding */
.left-col .card {
  padding-left: 0;
  padding-right: 0;
  background: transparent;
  border: none;
  border-radius: 0;
  box-shadow: none;
}

/* 上传卡片与检索卡片之间的细分隔线 */
.left-col .upload-card + .retrieve-card {
  border-top: 1px solid #eef1f4;
  padding-top: 16px;
  margin-top: 4px;
}

/* 上传卡片顶部贴住面板圆角，避免被面板边框裁掉一点点 */
.left-col .upload-card {
  border-radius: 8px 8px 0 0;
}

/* 左栏卡片贴紧左右，内容区各自负责小边距 */
.left-col .section-title {
  padding-left: 8px;
  padding-right: 12px;
}

.left-col .stage-tip {
  margin-left: 8px;
  margin-right: 12px;
}

.left-col .form-block {
  padding-left: 8px;
  padding-right: 12px;
}

.left-col .upload-actions {
  padding-left: 8px;
  padding-right: 12px;
}

.left-col .retrieve-result {
  margin-left: 8px;
  margin-right: 12px;
}

.left-col .retrieve-label-row {
  padding-left: 8px;
  padding-right: 12px;
  margin-left: 0;
  margin-right: 0;
}

.left-col .drag-mask {
  margin: 0;
  border-radius: 7px 7px 0 0;
}

/* 拖拽文件导入反馈 */
.upload-card {
  position: relative;
}

.upload-card.drag-over {
  border-color: #73a9d8;
  background: #f8fbfe;
}

.drag-mask {
  position: absolute;
  inset: 0;
  z-index: 5;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: rgba(245, 250, 253, 0.94);
  border-radius: 8px;
  color: #4e8abe;
  font-size: 13px;
  pointer-events: none;
}

.drag-mask .el-icon {
  font-size: 28px;
}

.chunk-estimate {
  color: #4e8abe;
  margin-left: 8px;
}

.retrieve-label-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}

.retrieve-label-row .retrieve-label {
  margin-bottom: 0;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 16px;
  font-size: 14px;
  font-weight: 600;
  color: #1f2329;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f2f5;
}

.section-title .el-icon {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  background: linear-gradient(135deg, #73a9d8, #a9cbe6);
  color: #fff;
  padding: 5px;
  box-shadow: 0 1px 3px rgba(115, 169, 216, 0.25);
}

.stage-tip {
  margin-bottom: 16px;
  border-radius: 6px;
}

.form-block {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 14px;
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
  margin-top: 12px;
  padding: 12px;
  background: #f8fbfd;
  border: 1px solid #e3eff6;
  border-radius: 6px;
}

.retrieve-label {
  font-size: 12px;
  color: #646a73;
  margin-bottom: 8px;
}

.retrieve-seg {
  padding: 8px 0;
  border-bottom: 1px dashed #e4eef5;
}

.retrieve-seg:first-of-type {
  padding-top: 0;
}

.retrieve-seg:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.seg-tag {
  display: inline-block;
  font-size: 12px;
  color: #4e8abe;
  background: rgba(115, 169, 216, 0.1);
  border-radius: 4px;
  padding: 1px 8px;
  margin-bottom: 6px;
}

.retrieve-text {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
  font-family: inherit;
  font-size: 13px;
  line-height: 1.7;
  color: #3b3f46;
  max-height: 280px;
  overflow: auto;
}

.list-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f2f5;
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
  gap: 10px;
  padding: 40px 0;
  color: #a0a4ad;
  font-size: 13px;
}

.list-empty .el-icon {
  font-size: 36px;
  color: #c9cdd4;
}

.doc-list {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.doc-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  border: 1px solid #edf0f3;
  border-radius: 6px;
  transition: border-color 0.18s, background 0.18s;
}

.doc-item:hover {
  border-color: #c5dbea;
  background: #f8fbfd;
}

/* 列表进出动效（适度） */
.doc-enter-active,
.doc-leave-active,
.doc-move {
  transition: all 0.25s ease;
}

.doc-enter-from,
.doc-leave-to {
  opacity: 0;
  transform: translateY(8px);
}

.doc-leave-active {
  position: absolute;
  width: 100%;
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

.doc-title-link {
  cursor: pointer;
  transition: color 0.2s;
}

.doc-title-link:hover {
  color: #4e8abe;
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
  color: #4e8abe;
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
  opacity: 0;
  transition: opacity 0.2s;
}

.doc-item:hover .doc-del {
  opacity: 1;
}

/* 触屏设备无 hover，删除按钮常显 */
@media (hover: none) {
  .doc-del {
    opacity: 1;
  }
}

.label-optional {
  font-size: 12px;
  font-weight: 400;
  color: #a0a4ad;
  margin-left: 6px;
}

.list-more {
  display: flex;
  justify-content: center;
  padding-top: 4px;
}

.view-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
}

.view-meta-text {
  font-size: 12px;
  color: #a0a4ad;
  margin-left: 4px;
}

.view-text {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
  font-family: inherit;
  font-size: 13px;
  line-height: 1.8;
  color: #3b3f46;
  background: #f8f9fb;
  border-radius: 6px;
  padding: 16px;
  max-height: 60vh;
  overflow: auto;
}

@media (max-width: 900px) {
  .main-grid {
    grid-template-columns: 1fr;
  }

  .left-col {
    position: static;
    max-height: none;
    overflow: visible;
  }
}
</style>
