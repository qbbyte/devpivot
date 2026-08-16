<template>
  <div class="project-page">
    <header class="project-header">
      <div class="header-left">
        <button class="back-link" @click="goBack">
          <el-icon>
            <ArrowLeft />
          </el-icon>
          <span>返回工作台</span>
        </button>
        <span class="header-divider"></span>
        <span class="header-title">{{ project.projectName || '需求采集' }}</span>
      </div>
      <div class="header-center">
        <h1 class="page-title">需求采集</h1>
        <el-tag size="small" class="stage-tag" effect="light">{{ stepLabel }}</el-tag>
      </div>
      <div class="header-right">
        <template v-if="!readOnly">
          <el-button class="save-btn" @click="handleSave">
            <el-icon>
              <DocumentChecked />
            </el-icon>
            <span>保存草稿</span>
          </el-button>
          <el-button type="primary" class="header-submit-btn" :loading="submitting" @click="handleSubmit">
            <span>提交需求，进入下一阶段</span>
            <el-icon class="el-icon--right">
              <ArrowRight />
            </el-icon>
          </el-button>
        </template>
        <el-tag v-else type="info" effect="plain" size="small" class="ro-tag">
          <el-icon>
            <Lock />
          </el-icon>
          <span>只读</span>
        </el-tag>
      </div>
    </header>

    <main class="project-main">
      <div class="project-layout">
        <aside class="project-sidebar">
          <div class="sidebar-card">
            <div class="sidebar-header">
              <div class="sidebar-header-line"></div>
              <span class="sidebar-title">项目信息</span>
            </div>
            <div class="sidebar-meta-list">
              <div class="meta-item">
                <div class="meta-icon">
                  <el-icon>
                    <OfficeBuilding />
                  </el-icon>
                </div>
                <div class="meta-body">
                  <span class="meta-label">行业分类</span>
                  <span class="meta-value">{{ project.industryType || '-' }}</span>
                </div>
              </div>
              <div class="meta-divider"></div>
              <div class="meta-item">
                <div class="meta-icon">
                  <el-icon>
                    <User />
                  </el-icon>
                </div>
                <div class="meta-body">
                  <span class="meta-label">目标用户</span>
                  <span class="meta-value">{{ project.targetUser || '-' }}</span>
                </div>
              </div>
            </div>
            <div class="sidebar-tip">
              <div class="tip-icon">
                <el-icon>
                  <InfoFilled />
                </el-icon>
              </div>
              <span>填写完整的需求基线，有助于 AI 在后续阶段生成更贴合的方案。</span>
            </div>
          </div>
        </aside>

        <div class="project-main-area">
          <section class="baseline-section">
            <div class="section-header">
              <div class="section-icon">
                <el-icon>
                  <EditPen />
                </el-icon>
              </div>
              <div class="section-text">
                <h3 class="section-title">需求基线</h3>
                <p class="section-subtitle">填写项目背景、功能范围与约束，作为后续 AI 澄清与 PRD 生成的基础</p>
              </div>
            </div>

            <div class="baseline-form">
              <div class="form-block">
                <div class="block-header">
                  <div class="block-icon"><el-icon>
                      <Briefcase />
                    </el-icon></div>
                  <div class="block-title-wrap">
                    <span class="block-label">业务背景</span>
                    <span class="block-hint">描述项目要解决的业务问题和目标</span>
                  </div>
                </div>
                <el-input v-model="baseline.businessContext" type="textarea" :rows="4" :disabled="readOnly"
                  placeholder="例如：当前企业使用 Excel 管理客户信息，效率低下且易出错，需要一套 CRM 系统来提升销售团队的工作效率..." />
              </div>

              <div class="form-block">
                <div class="block-header">
                  <div class="block-icon"><el-icon>
                      <List />
                    </el-icon></div>
                  <div class="block-title-wrap">
                    <span class="block-label">核心功能点</span>
                    <span class="block-hint">列出系统的主要功能模块</span>
                  </div>
                </div>
                <el-input v-model="baseline.coreFeatures" type="textarea" :rows="4" :disabled="readOnly"
                  placeholder="例如：&#10;1. 客户信息管理 - 支持客户的增删改查和分类管理&#10;2. 销售机会跟踪 - 记录销售线索和跟进状态&#10;3. 数据报表分析 - 可视化销售数据统计..." />
              </div>

              <div class="form-block">
                <div class="block-header">
                  <div class="block-icon"><el-icon>
                      <UserFilled />
                    </el-icon></div>
                  <div class="block-title-wrap">
                    <span class="block-label">用户故事</span>
                    <span class="block-hint">作为[角色]，我希望[功能]，以便[价值]</span>
                  </div>
                </div>
                <el-input v-model="baseline.userStories" type="textarea" :rows="4" :disabled="readOnly"
                  placeholder="例如：&#10;- 作为销售经理，我希望查看团队的客户跟进情况，以便及时调整销售策略&#10;- 作为销售人员，我希望快速记录客户沟通内容，以便后续跟进..." />
              </div>

              <div class="form-block">
                <div class="block-header">
                  <div class="block-icon"><el-icon>
                      <SetUp />
                    </el-icon></div>
                  <div class="block-title-wrap">
                    <span class="block-label">非功能性需求</span>
                    <span class="block-hint">性能、安全、可用性、兼容性等要求</span>
                  </div>
                </div>
                <el-input v-model="baseline.nonFunctional" type="textarea" :rows="3" :disabled="readOnly"
                  placeholder="例如：&#10;- 支持 100 人同时在线使用&#10;- 数据备份每日自动执行&#10;- 支持 Chrome、Edge 浏览器..." />
              </div>
            </div>
          </section>

        </div>
      </div>
    </main>

    <el-dialog v-model="assignDialogVisible" title="选择下一阶段负责人" width="420px" :close-on-click-modal="false">
      <div class="assign-dialog-body">
        <p class="assign-tip">请选择下一阶段的负责人，不选择则默认由自己负责</p>
        <el-select v-model="nextAssignee" placeholder="请选择负责人（可不选）" clearable class="assign-select">
          <el-option v-for="item in userList" :key="item.userId" :label="item.nickName" :value="item.userId" />
        </el-select>
      </div>
      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="confirmSubmit">确认提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="StepReq">
import { ref, reactive, computed, onMounted, getCurrentInstance } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Lock, InfoFilled } from '@element-plus/icons-vue'
import { getProject, updateProject } from '@/api/ai/project'
import { getBaselineByProject, saveBaseline } from '@/api/ai/baseline'
import { listUser } from '@/api/system/user'

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
const assignDialogVisible = ref(false)
const nextAssignee = ref(null)
const userList = ref([])
const project = ref({})
const currentStep = ref('REQ')

const stepIndex = computed(() => stepOrder.findIndex(s => s.value === currentStep.value))
const stepLabel = computed(() => {
  const hit = stepOrder.find(s => s.value === currentStep.value)
  return hit ? hit.label : '未开始'
})

// 阶段已"过去"判定：项目当前阶段在我这一阶之后 → 整页只读锁定
const readOnly = computed(() => {
  const order = ['REQ', 'CLARIFY', 'PRD', 'PROTO', 'TECH', 'DB', 'DONE']
  const cur = order.indexOf(currentStep.value)
  const mine = order.indexOf('REQ')
  return cur > mine
})

const baseline = reactive({
  businessContext: '',
  coreFeatures: '',
  userStories: '',
  nonFunctional: ''
})

function goBack() {
  router.push('/portal')
}

function getProjectInfo() {
  loading.value = true
  getProject(projectId.value).then(response => {
    project.value = response.data
    currentStep.value = response.data.step || 'REQ'
    loading.value = false
  }).catch(() => {
    loading.value = false
  })
}

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
  saveBaseline(buildBaselinePayload('0')).then(() => {
    proxy.$modal.msgSuccess('草稿已保存')
  }).catch(() => { })
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
  // 先落库需求基线（状态置为已确认），再推进项目阶段
  saveBaseline(buildBaselinePayload('1')).then(() => {
    const nextStep = stepOrder[stepIndex.value + 1]?.value || 'DONE'
    const updateData = { projectId: projectId.value, step: nextStep }
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
  loadBaseline()
  getUserList()
})
</script>

<style scoped>
.project-page {
  min-height: 100vh;
  background: #ffffff;
  display: flex;
  flex-direction: column;
}

.project-header {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 24px;
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid #ebedf0;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.02);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: none;
  background: none;
  color: #646a73;
  font-size: 13px;
  cursor: pointer;
  padding: 6px 10px;
  border-radius: 8px;
  transition: all 0.2s;
}

.back-link:hover {
  color: #3370ff;
  background: rgba(51, 112, 255, 0.06);
}

.back-link .el-icon {
  font-size: 14px;
}

.header-divider {
  width: 1px;
  height: 16px;
  background: #e5e7eb;
}

.header-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2329;
}

.header-center {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-center .page-title {
  margin: 0;
  font-size: 17px;
  font-weight: 600;
  color: #1f2329;
  line-height: 1.3;
}

.header-center .stage-tag {
  --el-tag-bg-color: #f0f6ff;
  --el-tag-border-color: #c5d9ff;
  --el-tag-text-color: #3370ff;
  font-weight: 500;
  border-radius: 6px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.save-btn {
  border-radius: 8px;
  color: #3370ff;
  border-color: #d6e4ff;
  padding: 8px 16px;
}

.save-btn:hover {
  color: #fff;
  border-color: #3370ff;
  background: #3370ff;
}

.header-submit-btn {
  border-radius: 8px;
  padding: 8px 18px;
  font-weight: 500;
  box-shadow: 0 2px 8px rgba(51, 112, 255, 0.2);
  transition: all 0.2s;
}

.header-submit-btn:hover {
  box-shadow: 0 4px 12px rgba(51, 112, 255, 0.3);
  transform: translateY(-1px);
}

.header-submit-btn:active {
  transform: scale(0.98);
}

.project-main {
  flex: 1;
  padding: 28px 24px 80px;
}

.project-layout {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  align-items: flex-start;
  gap: 24px;
}

.project-sidebar {
  width: 300px;
  flex-shrink: 0;
  background-color: #ffffff;
  position: sticky;
  top: 80px;
}

.sidebar-card {
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 12px;
  background: #fff;
  border-radius: 16px;
  border: 1px solid #f0f0f0;
}

.sidebar-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.sidebar-header-line {
  width: 4px;
  height: 16px;
  border-radius: 2px;
  background: linear-gradient(180deg, #3370ff, #5b8bff);
}

.sidebar-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2329;
}

.sidebar-meta-list {
  display: flex;
  flex-direction: column;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 4px;
  border-radius: 10px;
  transition: all 0.2s;
}

.meta-item:hover {
  background: #f7f9fc;
}

.meta-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: linear-gradient(135deg, #3370ff, #5b8bff);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 17px;
  flex-shrink: 0;
  box-shadow: 0 4px 10px rgba(51, 112, 255, 0.18);
}

.meta-divider {
  height: 1px;
  background: linear-gradient(90deg, transparent, #f0f0f0, transparent);
  margin: 4px 0;
}

.meta-body {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.meta-label {
  font-size: 12px;
  color: #86909c;
}

.meta-value {
  font-size: 15px;
  color: #1f2329;
  font-weight: 600;
}

.sidebar-tip {
  display: flex;
  gap: 10px;
  padding: 14px;
  background: linear-gradient(135deg, #f5f9ff 0%, #eef4ff 100%);
  border: 1px solid #c5d9ff;
  border-radius: 12px;
  color: #3370ff;
  font-size: 12px;
  line-height: 1.6;
}

.tip-icon {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #3370ff;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  flex-shrink: 0;
  margin-top: 1px;
}

.project-main-area {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.baseline-section {
  padding: 32px;
  background: #fff;
  border-radius: 16px;
  border: 1px solid #f0f0f0;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 28px;
  padding-bottom: 24px;
  border-bottom: 1px solid #f2f3f5;
}

.section-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: linear-gradient(135deg, #3370ff, #5b8bff);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  box-shadow: 0 4px 12px rgba(51, 112, 255, 0.18);
}

.section-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.section-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #1f2329;
}

.section-subtitle {
  margin: 0;
  font-size: 13px;
  color: #86909c;
  line-height: 1.5;
}

.baseline-form {
  display: flex;
  flex-direction: column;
  gap: 28px;
}

.form-block {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.block-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.block-icon {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: #f0f6ff;
  color: #3370ff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
}

.block-title-wrap {
  display: flex;
  align-items: baseline;
  gap: 8px;
  flex-wrap: wrap;
}

.block-label {
  font-size: 15px;
  font-weight: 600;
  color: #1f2329;
}

.block-hint {
  font-size: 12px;
  color: #a0a4ad;
}

.form-block :deep(.el-textarea__inner) {
  border-radius: 10px;
  padding: 14px 16px;
  background: #fafbfc;
  border: 1px solid transparent;
  box-shadow: none;
  transition: all 0.25s ease;
  font-size: 14px;
  line-height: 1.7;
  resize: vertical;
}

.form-block :deep(.el-textarea__inner:hover) {
  background: #f5f6f7;
}

.form-block :deep(.el-textarea__inner:focus) {
  background: #fff;
  border-color: #3370ff;
  box-shadow: 0 0 0 3px rgba(51, 112, 255, 0.12);
}

.form-block :deep(.el-textarea__inner:disabled) {
  background: #f5f6f7;
  color: #8f959e;
  cursor: not-allowed;
}

.form-block :deep(.el-textarea__inner::placeholder) {
  color: #b1b6bd;
}

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

@media (max-width: 992px) {
  .project-layout {
    flex-direction: column;
    align-items: stretch;
  }

  .project-sidebar {
    width: 100%;
    position: static;
  }

  .sidebar-card {
    width: 100%;
    box-sizing: border-box;
  }

  .sidebar-meta-list {
    flex-direction: row;
    align-items: stretch;
  }

  .meta-item {
    flex: 1;
  }

  .meta-divider {
    width: 1px;
    height: auto;
    background: linear-gradient(180deg, transparent, #f0f0f0, transparent);
    margin: 0 8px;
  }

  .sidebar-tip {
    margin-top: 0;
  }
}

@media (max-width: 768px) {
  .project-main {
    padding: 20px 16px 64px;
  }

  .project-sidebar {
    flex-direction: column;
  }

  .sidebar-meta-list {
    flex-direction: column;
  }

  .sidebar-tip {
    min-width: 0;
  }

  .baseline-section {
    padding: 20px;
  }

  .section-header {
    margin-bottom: 20px;
    padding-bottom: 18px;
  }

  .block-title-wrap {
    flex-direction: column;
    gap: 2px;
  }
}

@media (max-width: 768px) {
  .header-center {
    display: none;
  }
}

@media (max-width: 480px) {
  .header-title {
    display: none;
  }
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
</style>
