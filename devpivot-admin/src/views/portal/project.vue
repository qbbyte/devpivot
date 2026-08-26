<template>
  <div class="project-page">
    <header class="project-header">
      <div class="header-left">
        <button class="back-link" @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          <span>返回</span>
        </button>
        <span class="header-divider"></span>
        <span class="header-title">{{ project.projectName || '项目详情' }}</span>
      </div>
      <div class="header-right">
        <el-button class="save-btn" @click="goKnowledge" v-hasRole="['admin']">
          <el-icon><Collection /></el-icon>
          <span>知识库</span>
        </el-button>
        <el-button class="save-btn" @click="handleSave">
          <el-icon><DocumentChecked /></el-icon>
          <span>保存草稿</span>
        </el-button>
      </div>
    </header>

    <main class="project-main">
      <div class="project-content">

        <!-- 左右分栏布局 -->
        <div class="main-grid">

          <!-- 左侧：基本信息 -->
          <aside class="sidebar">
            <section class="info-section">
              <h3 class="section-title">
                <el-icon><Document /></el-icon>
                <span>基本信息</span>
              </h3>
              <div class="info-list">
                <div class="info-item">
                  <span class="info-label">项目名称</span>
                  <span class="info-value">{{ project.projectName }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">行业分类</span>
                  <span class="info-value">{{ project.industryType || '-' }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">目标用户群体</span>
                  <span class="info-value">{{ project.targetUser || '-' }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">当前阶段</span>
                  <el-tag size="small" type="primary">{{ stepLabel }}</el-tag>
                </div>
                <div class="info-item">
                  <span class="info-label">负责人</span>
                  <span class="info-value">{{ project.assigneeName || '-' }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">更新时间</span>
                  <span class="info-value">{{ project.updateTime || '-' }}</span>
                </div>
              </div>
            </section>
          </aside>

          <!-- 右侧：阶段感知内容 -->
          <div class="main-content">

            <!-- REQ：需求基线表单 -->
            <template v-if="currentStep === 'REQ'">
              <section class="baseline-section">
                <h3 class="section-title">
                  <el-icon><EditPen /></el-icon>
                  <span>需求基线</span>
                </h3>
                <div class="baseline-form">
                  <div class="form-block">
                    <div class="block-header">
                      <span class="block-label">业务背景</span>
                      <span class="block-hint">描述项目要解决的业务问题和目标</span>
                    </div>
                    <el-input
                      v-model="baseline.businessContext"
                      type="textarea"
                      :rows="4"
                      placeholder="例如：当前企业使用Excel管理客户信息，效率低下且易出错，需要一套CRM系统来提升销售团队的工作效率..."
                    />
                  </div>
                  <div class="form-block">
                    <div class="block-header">
                      <span class="block-label">核心功能点</span>
                      <span class="block-hint">列出系统的主要功能模块</span>
                    </div>
                    <el-input
                      v-model="baseline.coreFeatures"
                      type="textarea"
                      :rows="4"
                      placeholder="例如：&#10;1. 客户信息管理 - 支持客户的增删改查和分类管理&#10;2. 销售机会跟踪 - 记录销售线索和跟进状态&#10;3. 数据报表分析 - 可视化销售数据统计..."
                    />
                  </div>
                  <div class="form-block">
                    <div class="block-header">
                      <span class="block-label">用户故事</span>
                      <span class="block-hint">作为[角色]，我希望[功能]，以便[价值]</span>
                    </div>
                    <el-input
                      v-model="baseline.userStories"
                      type="textarea"
                      :rows="4"
                      placeholder="例如：&#10;- 作为销售经理，我希望查看团队的客户跟进情况，以便及时调整销售策略&#10;- 作为销售人员，我希望快速记录客户沟通内容，以便后续跟进..."
                    />
                  </div>
                  <div class="form-block">
                    <div class="block-header">
                      <span class="block-label">非功能性需求</span>
                      <span class="block-hint">性能、安全、可用性、兼容性等要求</span>
                    </div>
                    <el-input
                      v-model="baseline.nonFunctional"
                      type="textarea"
                      :rows="3"
                      placeholder="例如：&#10;- 支持100人同时在线使用&#10;- 数据备份每日自动执行&#10;- 支持Chrome、Edge浏览器..."
                    />
                  </div>
                </div>
              </section>
              <section class="action-section">
                <el-button type="primary" size="large" class="submit-btn" :loading="submitting" @click="handleSubmit">
                  <span>提交需求，进入下一阶段</span>
                  <el-icon class="el-icon--right"><ArrowRight /></el-icon>
                </el-button>
              </section>
            </template>

            <!-- DONE：完成摘要 + 交付物 -->
            <template v-else-if="currentStep === 'DONE'">
              <section class="done-section">
                <div class="done-banner">
                  <el-icon class="done-icon"><CircleCheck /></el-icon>
                  <div>
                    <h3 class="done-title">项目已完成</h3>
                    <p class="done-sub">所有阶段已交付，可查看各阶段产出物</p>
                  </div>
                </div>
                <div class="done-actions">
                  <el-dropdown trigger="click" @command="handleExport" :disabled="exporting">
                    <el-button type="primary" :loading="exporting">
                      <el-icon><Download /></el-icon>
                      <span>导出到开发工具</span>
                      <el-icon class="el-icon--right"><ArrowDown /></el-icon>
                    </el-button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item command="agents">通用 / AGENTS.md</el-dropdown-item>
                        <el-dropdown-item command="cursor">Cursor</el-dropdown-item>
                        <el-dropdown-item command="trae">Trae</el-dropdown-item>
                        <el-dropdown-item command="vscode">VS Code (Copilot)</el-dropdown-item>
                        <el-dropdown-item command="claudecode">Claude Code</el-dropdown-item>
                        <el-dropdown-item divided command="copy">复制 Markdown（粘进终端/聊天框）</el-dropdown-item>
                        <el-dropdown-item command="curl">复制 curl 命令（服务器终端拉取）</el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                  <span class="done-actions-hint">导出项目上下文与 IDE 规则文件，拖入项目根目录即可使用；或用下方两个选项直接喂给终端 AI</span>
                </div>
                <div class="deliver-grid">
                  <div
                    v-for="d in deliverables"
                    :key="d.stage"
                    class="deliver-card"
                    role="button"
                    tabindex="0"
                    @click="goStage(d.stage)"
                    @keyup.enter="goStage(d.stage)"
                  >
                    <div class="deliver-icon">{{ d.icon }}</div>
                    <div class="deliver-meta">
                      <div class="deliver-name">{{ d.label }}</div>
                      <div class="deliver-state">{{ deliverStateOf(d.stage) }}</div>
                    </div>
                    <span class="deliver-go">查看 →</span>
                  </div>
                </div>
              </section>
            </template>

            <!-- 其它阶段：进入当前阶段 -->
            <template v-else>
              <section class="stage-entry-section">
                <div class="stage-entry-banner">
                  <el-icon class="entry-icon"><Promotion /></el-icon>
                  <div>
                    <h3 class="entry-title">当前阶段：{{ stepLabel }}</h3>
                    <p class="entry-sub">{{ currentStageDesc }}</p>
                  </div>
                </div>
                <el-button type="primary" size="large" class="entry-btn" @click="goStage(currentStep)">
                  <span>进入「{{ stepLabel }}」</span>
                  <el-icon class="el-icon--right"><ArrowRight /></el-icon>
                </el-button>
              </section>
            </template>

          </div>
        </div>
      </div>
    </main>

    <!-- 选择下一阶段负责人弹窗 -->
    <el-dialog v-model="assignDialogVisible" title="选择下一阶段负责人" width="420px" :close-on-click-modal="false">
      <div class="assign-dialog-body">
        <p class="assign-tip">请选择下一阶段的负责人，不选择则默认由自己负责</p>
        <el-select v-model="nextAssignee" placeholder="请选择负责人（可不选）" clearable class="assign-select">
          <el-option
            v-for="item in userList"
            :key="item.userId"
            :label="item.nickName"
            :value="item.userId"
          />
        </el-select>
      </div>
      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="confirmSubmit">确认提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="ProjectDetail">
import { ref, reactive, computed, onMounted, getCurrentInstance } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getProject, updateProject } from '@/api/ai/project'
import { getBaselineByProject, saveBaseline } from '@/api/ai/baseline'
import { listUser } from '@/api/system/user'
import { exportDevContext, copyDevContextMarkdown, copyDevContextCurl } from '@/utils/exportDevContext'

const { proxy } = getCurrentInstance()
const router = useRouter()
const route = useRoute()

const projectId = computed(() => route.params.id)

const stepOrder = [
  { value: 'REQ', label: '需求采集', short: '需求' },
  { value: 'CLARIFY', label: 'AI 澄清', short: '澄清' },
  { value: 'PRD', label: 'PRD 文档', short: 'PRD' },
  { value: 'PROTO', label: '原型设计', short: '原型' },
  { value: 'ARCH', label: '系统架构', short: '架构' },
  { value: 'TECH', label: '技术方案', short: '技术' },
  { value: 'DB', label: '数据库', short: '库表' },
  { value: 'DONE', label: '完成', short: '完成' }
]

// 阶段 -> 路由后缀（不存在 done 路由，DONE 仅展示）
const stepRouteMap = {
  REQ: 'req',
  CLARIFY: 'clarify',
  PRD: 'prd',
  PROTO: 'proto',
  ARCH: 'arch',
  TECH: 'tech',
  DB: 'db'
}

const stageDescMap = {
  CLARIFY: 'AI 将针对需求进行澄清问答，补齐模糊点后进入 PRD。',
  PRD: '基于需求与澄清，生成结构化的产品需求文档（PRD）。',
  PROTO: '基于 PRD 生成可交互的原型界面。',
  ARCH: '基于 PRD 与原型生成系统架构设计（模块划分/核心流程/接口契约/部署架构/非功能）。',
  TECH: '基于原型生成落地技术方案。',
  DB: '基于技术方案生成数据库表结构设计。'
}

const deliverables = [
  { stage: 'PRD', label: 'PRD 文档', icon: 'P' },
  { stage: 'PROTO', label: '原型设计', icon: '◳' },
  { stage: 'ARCH', label: '系统架构设计', icon: '⌬' },
  { stage: 'TECH', label: '技术方案', icon: '⌘' },
  { stage: 'DB', label: '数据库设计', icon: '⛁' }
]

// 阶段产物状态：项目当前阶段索引 >= 产物所在阶段索引 → 已生成（含 DONE）；否则未生成
function deliverStateOf(stage)
{
  if (currentStep.value === 'DONE') return '已生成'
  const myIdx = stepOrder.findIndex(s => s.value === stage)
  return myIdx >= 0 && myIdx <= stepIndex.value ? '已生成' : '未生成'
}

const loading = ref(false)
const submitting = ref(false)
const assignDialogVisible = ref(false)
const nextAssignee = ref(null)
const userList = ref([])

const project = ref({})
const currentStep = ref('REQ')

const stepLabel = computed(() => {
  const hit = stepOrder.find(s => s.value === currentStep.value)
  return hit ? hit.label : '未开始'
})

const stepIndex = computed(() => stepOrder.findIndex(s => s.value === currentStep.value))

const currentStageDesc = computed(() => stageDescMap[currentStep.value] || '')

const baseline = reactive({
  businessContext: '',
  coreFeatures: '',
  userStories: '',
  nonFunctional: ''
})

function goBack() {
  router.push('/portal')
}

function goKnowledge() {
  router.push('/portal/project/' + projectId.value + '/kb')
}

function goStage(stageValue) {
  const routeName = stepRouteMap[stageValue]
  if (!routeName) return
  router.push(`/portal/project/${projectId.value}/${routeName}`)
}

function getProjectInfo() {
  loading.value = true
  getProject(projectId.value).then(response => {
    project.value = response.data
    currentStep.value = response.data.step || 'REQ'
    loading.value = false
    // 处于 REQ 阶段时，加载已保存的需求基线回填表单（与 req.vue 同源，避免双入口不一致）
    if (currentStep.value === 'REQ') {
      loadBaseline()
    }
  }).catch(() => {
    loading.value = false
  })
}

// 从 baseline API 加载已保存的需求基线，回填到表单（content 为 JSON 字符串，需解析）
function loadBaseline() {
  getBaselineByProject(projectId.value).then(res => {
    const data = res.data
    if (data && data.content) {
      try {
        const c = JSON.parse(data.content)
        baseline.businessContext = c.businessContext || ''
        baseline.coreFeatures = c.coreFeatures || ''
        baseline.userStories = c.userStories || ''
        baseline.nonFunctional = c.nonFunctional || ''
      } catch (e) {
        // 历史数据非 JSON 时忽略，避免页面崩溃
      }
    }
  }).catch(() => { })
}

// 组装需求基线保存载荷（与 req.vue 一致：content 为四字段 JSON，status 草稿0/已确认1）
function buildBaselinePayload(status) {
  return {
    projectId: projectId.value,
    status: status,
    content: JSON.stringify({
      businessContext: baseline.businessContext,
      coreFeatures: baseline.coreFeatures,
      userStories: baseline.userStories,
      nonFunctional: baseline.nonFunctional
    })
  }
}

function handleSave() {
  // 仅 REQ 阶段存在需求基线表单；其余阶段无基线可存，仅给中性反馈
  if (currentStep.value !== 'REQ') {
    proxy.$modal.msgSuccess('已保存')
    return
  }
  saveBaseline(buildBaselinePayload('0')).then(() => {
    proxy.$modal.msgSuccess('草稿已保存')
  }).catch(() => { })
}

const exporting = ref(false)

function handleExport(command) {
  exporting.value = true
  if (command === 'copy') {
    copyDevContextMarkdown(projectId.value, project.value)
      .then(() => {
        proxy.$modal.msgSuccess('已复制项目上下文 Markdown，直接粘贴到终端 / 聊天框即可')
      })
      .catch(() => {
        proxy.$modal.msgError('复制失败，请重试')
      })
      .finally(() => {
        exporting.value = false
      })
    return
  }
  if (command === 'curl') {
    copyDevContextCurl(projectId.value, 'agents')
      .then(() => {
        proxy.$modal.msgSuccess('已复制 curl 命令，在服务器终端粘贴执行即可拉取 AGENTS.md')
      })
      .catch(() => {
        proxy.$modal.msgError('生成失败，请重试')
      })
      .finally(() => {
        exporting.value = false
      })
    return
  }
  exportDevContext({ projectId: projectId.value, project: project.value, target: command })
    .then(() => {
      proxy.$modal.msgSuccess('已导出开发上下文')
    })
    .catch(() => {
      proxy.$modal.msgError('导出失败，请重试')
    })
    .finally(() => {
      exporting.value = false
    })
}

function handleSubmit() {
  if (!baseline.businessContext) {
    proxy.$modal.msgWarning('请填写业务背景')
    return
  }
  assignDialogVisible.value = true
}

function confirmSubmit() {
  submitting.value = true
  const nextStep = stepOrder[stepIndex.value + 1]?.value || 'DONE'
  // 先落库需求基线（状态置为已确认），再推进项目阶段 + 指派负责人（与 req.vue 一致）
  saveBaseline(buildBaselinePayload('1')).then(() => {
    const updateData = {
      projectId: projectId.value,
      step: nextStep
    }
    if (nextAssignee.value) {
      updateData.assigneeId = nextAssignee.value
    }
    return updateProject(updateData)
  }).then(() => {
    proxy.$modal.msgSuccess('需求已提交')
    assignDialogVisible.value = false
    router.push('/portal')
  }).catch(() => {
    submitting.value = false
  })
}

function getUserList() {
  listUser({ pageNum: 1, pageSize: 100 }).then(response => {
    userList.value = response.rows || []
  })
}

onMounted(() => {
  getProjectInfo()
  getUserList()
})
</script>

<style scoped>
.project-page {
  min-height: 100vh;
  background: #f7f8fa;
  display: flex;
  flex-direction: column;
}

/* ===== Header ===== */
.project-header {
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

.save-btn {
  border-radius: 8px;
  color: #646a73;
  border-color: #dee0e3;
}

.save-btn:hover {
  color: #3370ff;
  border-color: #3370ff;
}

/* ===== Main ===== */
.project-main {
  flex: 1;
  padding: 32px 24px 80px;
}

.project-content {
  max-width: 1280px;
  margin: 0 auto;
}

/* ===== Grid Layout ===== */
.main-grid {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 24px;
  align-items: start;
}

/* ===== Sidebar ===== */
.sidebar {
  position: sticky;
  top: 84px;
  background: transparent !important;
  padding: 0 !important;
  margin-bottom: 0 !important;
  border-radius: 0 !important;
  line-height: normal !important;
  font-size: normal !important;
  color: inherit !important;
}

.info-section {
  padding: 24px;
  background: #fff;
  border-radius: 12px;
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

.info-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px 0;
  border-bottom: 1px solid #f5f6f8;
}

.info-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.info-item:first-child {
  padding-top: 0;
}

.info-label {
  font-size: 12px;
  color: #86909c;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.info-value {
  font-size: 14px;
  color: #1f2329;
  font-weight: 500;
  line-height: 1.5;
  word-break: break-all;
}

/* ===== Main Content ===== */
.main-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.baseline-section {
  padding: 24px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

/* ===== Baseline Form ===== */
.baseline-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.block-header {
  display: flex;
  align-items: baseline;
  gap: 8px;
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

.form-block :deep(.el-textarea__inner) {
  border-radius: 8px;
  box-shadow: 0 0 0 1px #dee0e3 inset;
  transition: box-shadow 0.25s ease;
}

.form-block :deep(.el-textarea__inner:hover) {
  box-shadow: 0 0 0 1px #c9cdd4 inset;
}

.form-block :deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 1px #3370ff inset, 0 0 0 3px rgba(51, 112, 255, 0.15);
}

/* ===== Action Section ===== */
.action-section {
  display: flex;
  justify-content: center;
}

.submit-btn {
  min-width: 200px;
  border-radius: 8px;
  padding: 12px 32px;
  font-size: 15px;
  font-weight: 500;
}

.submit-btn:active {
  transform: scale(0.97);
}

/* ===== DONE 完成摘要 ===== */
.done-section {
  padding: 24px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.done-banner {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
  background: #eaf3de;
  border-radius: 10px;
  margin-bottom: 20px;
}

.done-icon {
  font-size: 28px;
  color: #3b6d11;
}

.done-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #27500a;
}

.done-sub {
  margin: 4px 0 0;
  font-size: 13px;
  color: #4a6b22;
}

.done-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.done-actions-hint {
  font-size: 12px;
  color: #8a8f99;
}

.deliver-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.deliver-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  cursor: pointer;
  background: #fff;
  transition: border-color 0.2s, box-shadow 0.2s, transform 0.15s;
}

.deliver-card:hover {
  border-color: #3370ff;
  box-shadow: 0 4px 14px rgba(51, 112, 255, 0.12);
}

.deliver-card:focus-visible {
  outline: 2px solid #3370ff;
  outline-offset: 2px;
}

.deliver-icon {
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  border-radius: 10px;
  background: #e6f1fb;
  color: #185fa5;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 600;
}

.deliver-meta {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.deliver-name {
  font-size: 14px;
  font-weight: 500;
  color: #1f2329;
}

.deliver-state {
  font-size: 12px;
  color: #639922;
}

.deliver-go {
  font-size: 12px;
  color: #3370ff;
  white-space: nowrap;
}

/* ===== 其它阶段：进入当前阶段 ===== */
.stage-entry-section {
  padding: 24px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.stage-entry-banner {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 22px 20px;
  background: #e6f1fb;
  border-radius: 10px;
  margin-bottom: 20px;
}

.entry-icon {
  font-size: 28px;
  color: #185fa5;
}

.entry-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #0c447c;
}

.entry-sub {
  margin: 4px 0 0;
  font-size: 13px;
  color: #2b6aa8;
  line-height: 1.6;
}

.entry-btn {
  min-width: 200px;
  border-radius: 8px;
  padding: 12px 32px;
  font-size: 15px;
  font-weight: 500;
}

.entry-btn:active {
  transform: scale(0.97);
}

/* ===== Assign Dialog ===== */
.assign-dialog-body {
  padding: 8px 0;
}

.assign-tip {
  margin: 0 0 16px;
  font-size: 14px;
  color: #646a73;
  line-height: 1.6;
}

.assign-select {
  width: 100%;
}

/* ===== Responsive ===== */
@media (max-width: 900px) {
  .main-grid {
    grid-template-columns: 1fr;
  }

  .sidebar {
    position: static;
  }

  .info-section {
    margin-bottom: 0;
  }
}

@media (max-width: 640px) {
  .project-main {
    padding: 20px 16px 64px;
  }

  .baseline-section,
  .done-section,
  .stage-entry-section {
    padding: 16px;
  }

  .deliver-grid {
    grid-template-columns: 1fr;
  }
}
</style>
