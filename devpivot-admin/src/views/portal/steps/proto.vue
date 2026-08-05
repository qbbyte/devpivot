<template>
  <div class="project-page">
    <header class="project-header">
      <div class="header-left">
        <button class="back-link" @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          <span>返回</span>
        </button>
        <span class="header-divider"></span>
        <span class="header-title">{{ project.projectName || '原型设计' }}</span>
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
                  <el-tag size="small" type="warning">{{ stepLabel }}</el-tag>
                </div>
              </div>
            </section>
          </aside>

          <div class="main-content">
            <section class="proto-section">
              <h3 class="section-title">
                <el-icon><Monitor /></el-icon>
                <span>原型设计</span>
              </h3>
              <div class="proto-content">
                <el-empty description="原型设计生成中，请稍候...">
                  <template #image>
                    <el-icon :size="64" color="#e6a23c"><Loading /></el-icon>
                  </template>
                </el-empty>
              </div>
            </section>

            <section class="action-section">
              <el-button type="primary" size="large" class="submit-btn" :loading="submitting" @click="handleSubmit">
                <span>确认原型，进入下一阶段</span>
                <el-icon class="el-icon--right"><ArrowRight /></el-icon>
              </el-button>
            </section>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup name="StepProto">
import { ref, computed, onMounted, getCurrentInstance } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getProject, updateProject } from '@/api/ai/project'

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
const project = ref({})
const currentStep = ref('PROTO')

const stepIndex = computed(() => stepOrder.findIndex(s => s.value === currentStep.value))
const stepLabel = computed(() => {
  const hit = stepOrder.find(s => s.value === currentStep.value)
  return hit ? hit.label : '未开始'
})

function goBack() {
  router.push('/portal')
}

function getProjectInfo() {
  loading.value = true
  getProject(projectId.value).then(response => {
    project.value = response.data
    currentStep.value = response.data.step || 'PROTO'
    loading.value = false
  }).catch(() => {
    loading.value = false
  })
}

function handleSave() {
  proxy.$modal.msgSuccess('草稿已保存')
}

function handleSubmit() {
  submitting.value = true
  const nextStep = stepOrder[stepIndex.value + 1]?.value || 'DONE'
  updateProject({ projectId: projectId.value, step: nextStep }).then(() => {
    proxy.$modal.msgSuccess('已提交')
    submitting.value = false
    router.push('/portal')
  }).catch(() => {
    submitting.value = false
  })
}

onMounted(() => {
  getProjectInfo()
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
.proto-section { padding: 24px; background: #fff; border-radius: 12px; box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04); }
.proto-content { min-height: 400px; display: flex; align-items: center; justify-content: center; }
.action-section { display: flex; justify-content: center; }
.submit-btn { min-width: 200px; border-radius: 8px; padding: 12px 32px; font-size: 15px; font-weight: 500; }
@media (max-width: 900px) { .main-grid { grid-template-columns: 1fr; } .sidebar { position: static; } }
</style>