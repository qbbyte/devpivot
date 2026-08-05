<template>
  <div class="project-page">
    <header class="project-header">
      <div class="header-left">
        <button class="back-link" @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          <span>返回</span>
        </button>
        <span class="header-divider"></span>
        <span class="header-title">{{ project.projectName || '需求采集' }}</span>
      </div>
      <div class="header-right">
        <el-button class="save-btn" @click="handleSave">
          <el-icon><DocumentChecked /></el-icon>
          <span>保存草稿</span>
        </el-button>
      </div>
    </header>

    <main class="project-main">
      <div class="project-content">
        <section class="step-section">
          <div class="step-track">
            <template v-for="(s, idx) in stepOrder" :key="s.value">
              <div class="step-node" :class="{ active: stepIndex >= idx, current: stepIndex === idx }">
                <div class="step-dot">
                  <el-icon v-if="stepIndex > idx"><Check /></el-icon>
                  <span v-else>{{ idx + 1 }}</span>
                </div>
                <div class="step-text">{{ s.label }}</div>
              </div>
              <div v-if="idx < stepOrder.length - 1" class="step-line" :class="{ active: stepIndex > idx }"></div>
            </template>
          </div>
        </section>

        <div class="main-grid">
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
              </div>
            </section>
          </aside>

          <div class="main-content">
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
                  <el-input v-model="baseline.businessContext" type="textarea" :rows="4" placeholder="例如：当前企业使用Excel管理客户信息，效率低下且易出错，需要一套CRM系统来提升销售团队的工作效率..." />
                </div>
                <div class="form-block">
                  <div class="block-header">
                    <span class="block-label">核心功能点</span>
                    <span class="block-hint">列出系统的主要功能模块</span>
                  </div>
                  <el-input v-model="baseline.coreFeatures" type="textarea" :rows="4" placeholder="例如：&#10;1. 客户信息管理 - 支持客户的增删改查和分类管理&#10;2. 销售机会跟踪 - 记录销售线索和跟进状态&#10;3. 数据报表分析 - 可视化销售数据统计..." />
                </div>
                <div class="form-block">
                  <div class="block-header">
                    <span class="block-label">用户故事</span>
                    <span class="block-hint">作为[角色]，我希望[功能]，以便[价值]</span>
                  </div>
                  <el-input v-model="baseline.userStories" type="textarea" :rows="4" placeholder="例如：&#10;- 作为销售经理，我希望查看团队的客户跟进情况，以便及时调整销售策略&#10;- 作为销售人员，我希望快速记录客户沟通内容，以便后续跟进..." />
                </div>
                <div class="form-block">
                  <div class="block-header">
                    <span class="block-label">非功能性需求</span>
                    <span class="block-hint">性能、安全、可用性、兼容性等要求</span>
                  </div>
                  <el-input v-model="baseline.nonFunctional" type="textarea" :rows="3" placeholder="例如：&#10;- 支持100人同时在线使用&#10;- 数据备份每日自动执行&#10;- 支持Chrome、Edge浏览器..." />
                </div>
              </div>
            </section>

            <section class="action-section">
              <el-button type="primary" size="large" class="submit-btn" :loading="submitting" @click="handleSubmit">
                <span>提交需求，进入下一阶段</span>
                <el-icon class="el-icon--right"><ArrowRight /></el-icon>
              </el-button>
            </section>
          </div>
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
import { getProject, updateProject } from '@/api/ai/project'
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

function handleSave() {
  proxy.$modal.msgSuccess('草稿已保存')
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
  const updateData = { projectId: projectId.value, step: nextStep }
  if (nextAssignee.value) {
    updateData.assigneeId = nextAssignee.value
  }
  updateProject(updateData).then(() => {
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
.project-page { min-height: 100vh; background: #f7f8fa; display: flex; flex-direction: column; }
.project-header { position: sticky; top: 0; z-index: 10; display: flex; align-items: center; justify-content: space-between; height: 52px; padding: 0 24px; background: rgba(255, 255, 255, 0.92); backdrop-filter: blur(8px); border-bottom: 1px solid #ebedf0; }
.header-left { display: flex; align-items: center; gap: 12px; }
.back-link { display: inline-flex; align-items: center; gap: 4px; border: none; background: none; color: #646a73; font-size: 13px; cursor: pointer; padding: 4px 8px; border-radius: 6px; transition: color 0.2s, background 0.2s; }
.back-link:hover { color: #3370ff; background: rgba(51, 112, 255, 0.06); }
.header-divider { width: 1px; height: 16px; background: #e5e7eb; }
.header-title { font-size: 14px; font-weight: 600; color: #1f2329; }
.save-btn { border-radius: 8px; color: #646a73; border-color: #dee0e3; }
.save-btn:hover { color: #3370ff; border-color: #3370ff; }
.project-main { flex: 1; padding: 32px 24px 80px; }
.project-content { max-width: 1280px; margin: 0 auto; }
.step-section { margin-bottom: 24px; padding: 20px 24px; background: #fff; border-radius: 12px; box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04); }
.step-track { display: flex; align-items: flex-start; justify-content: space-between; }
.step-node { display: flex; flex-direction: column; align-items: center; gap: 8px; flex-shrink: 0; }
.step-dot { width: 32px; height: 32px; border-radius: 50%; background: #e5e7eb; color: #86909c; display: flex; align-items: center; justify-content: center; font-size: 13px; font-weight: 600; transition: all 0.3s; }
.step-node.active .step-dot { background: #3370ff; color: #fff; box-shadow: 0 2px 8px rgba(51, 112, 255, 0.3); }
.step-node.current .step-dot { transform: scale(1.1); }
.step-text { font-size: 12px; color: #86909c; white-space: nowrap; }
.step-node.active .step-text { color: #3370ff; font-weight: 500; }
.step-line { flex: 1; height: 2px; background: #e5e7eb; margin: 15px -4px 0; transition: background 0.3s; }
.step-line.active { background: #3370ff; }
.main-grid { display: grid; grid-template-columns: 280px 1fr; gap: 24px; align-items: start; }
.sidebar { position: sticky; top: 84px; }
.info-section { padding: 24px; background: #fff; border-radius: 12px; }
.section-title { display: flex; align-items: center; gap: 8px; margin: 0 0 20px; font-size: 15px; font-weight: 600; color: #1f2329; padding-bottom: 16px; border-bottom: 1px solid #f5f6f8; }
.section-title .el-icon { width: 28px; height: 28px; border-radius: 8px; background: linear-gradient(135deg, #3370ff, #5b8cff); color: #fff; padding: 6px; box-shadow: 0 2px 6px rgba(51, 112, 255, 0.25); }
.info-list { display: flex; flex-direction: column; gap: 0; }
.info-item { display: flex; flex-direction: column; gap: 6px; padding: 14px 0; border-bottom: 1px solid #f5f6f8; }
.info-item:last-child { border-bottom: none; padding-bottom: 0; }
.info-item:first-child { padding-top: 0; }
.info-label { font-size: 12px; color: #86909c; text-transform: uppercase; letter-spacing: 0.5px; }
.info-value { font-size: 14px; color: #1f2329; font-weight: 500; line-height: 1.5; word-break: break-all; }
.main-content { display: flex; flex-direction: column; gap: 24px; }
.baseline-section { padding: 24px; background: #fff; border-radius: 12px; box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04); }
.baseline-form { display: flex; flex-direction: column; gap: 20px; }
.form-block { display: flex; flex-direction: column; gap: 8px; }
.block-header { display: flex; align-items: baseline; gap: 8px; }
.block-label { font-size: 14px; font-weight: 500; color: #1f2329; }
.block-hint { font-size: 12px; color: #a0a4ad; }
.form-block :deep(.el-textarea__inner) { border-radius: 8px; box-shadow: 0 0 0 1px #dee0e3 inset; transition: box-shadow 0.25s ease; }
.form-block :deep(.el-textarea__inner:hover) { box-shadow: 0 0 0 1px #c9cdd4 inset; }
.form-block :deep(.el-textarea__inner:focus) { box-shadow: 0 0 0 1px #3370ff inset, 0 0 0 3px rgba(51, 112, 255, 0.15); }
.action-section { display: flex; justify-content: center; }
.submit-btn { min-width: 200px; border-radius: 8px; padding: 12px 32px; font-size: 15px; font-weight: 500; }
.submit-btn:active { transform: scale(0.97); }
.assign-dialog-body { padding: 8px 0; }
.assign-tip { margin: 0 0 16px; font-size: 14px; color: #646a73; line-height: 1.6; }
.assign-select { width: 100%; }
@media (max-width: 900px) { .main-grid { grid-template-columns: 1fr; } .sidebar { position: static; } }
@media (max-width: 640px) { .project-main { padding: 20px 16px 64px; } .step-section { padding: 16px; } .baseline-section { padding: 16px; } .step-text { font-size: 10px; } }
</style>