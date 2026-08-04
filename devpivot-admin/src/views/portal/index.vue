<template>
  <div class="portal">
    <header class="portal-header">
      <div class="portal-header-inner">
        <div class="portal-brand">
          <div class="brand-logo">
            <el-icon :size="20"><Opportunity /></el-icon>
          </div>
          <span class="brand-name">AI 智能需求设计</span>
        </div>
        <div class="portal-actions">
          <el-button v-hasRole="['admin']" class="admin-btn" @click="goAdmin">
            <el-icon><Setting /></el-icon>
            <span>进入管理后台</span>
          </el-button>
        </div>
      </div>
    </header>

    <main class="portal-main">
      <section class="portal-hero">
        <div class="hero-left">
          <h1>AI 项目工作台</h1>
          <p>统一查看所有 AI 需求设计项目的进度与状态</p>
        </div>
        <el-button type="primary" class="create-btn" @click="goCreate">
          <el-icon><Plus /></el-icon>
          <span>新建项目</span>
        </el-button>
      </section>

      <section class="portal-stats" v-if="stats.total > 0">
        <div class="stat-card">
          <div class="stat-icon stat-total">
            <el-icon :size="20"><FolderOpened /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-num">{{ stats.total }}</div>
            <div class="stat-label">项目总数</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon stat-doing">
            <el-icon :size="20"><Loading /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-num">{{ stats.doing }}</div>
            <div class="stat-label">进行中</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon stat-done">
            <el-icon :size="20"><CircleCheck /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-num">{{ stats.done }}</div>
            <div class="stat-label">已完成</div>
          </div>
        </div>
      </section>

      <section class="portal-body">
        <template v-if="!loading && projectList.length > 0">
          <div class="project-list">
            <div class="project-card" v-for="item in projectList" :key="item.projectId" @click="goProject(item.projectId)">
              <div class="project-card-left">
                <div class="project-card-top">
                  <div class="project-name">
                    <el-tag effect="dark" size="small" type="danger" v-if="item.isTop === 'Y'">置顶</el-tag>
                    <span class="name-text">{{ item.projectName }}</span>
                  </div>
                  <dict-tag :options="ai_project_status" :value="item.status" />
                </div>
                <p class="project-intro">{{ item.projectIntro || '暂无项目简介' }}</p>
                <div class="project-meta">
                  <span v-if="item.assigneeName"><el-icon><User /></el-icon>{{ item.assigneeName }}</span>
                  <span v-if="item.updateTime"><el-icon><Clock /></el-icon>{{ formatTime(item.updateTime) }}</span>
                </div>
              </div>
              <div class="project-card-right">
                <div class="step-percent">{{ stepPercent(item.step) }}%</div>
                <el-progress :percentage="stepPercent(item.step)" :stroke-width="6" :show-text="false" />
                <dict-tag :options="ai_project_step" :value="item.step" />
              </div>
            </div>
          </div>
          <div class="pagination-wrap" v-if="total > 10">
            <el-pagination
              v-model:current-page="queryParams.pageNum"
              :page-size="10"
              :total="total"
              layout="prev, pager, next"
              background
              @current-change="getList"
            />
          </div>
        </template>

        <el-empty v-else-if="!loading" description="暂无项目数据" />
        <div v-loading="loading" class="loading-mask"></div>
      </section>
    </main>

    <footer class="portal-footer">
      <span>AI 智能需求设计与数据库生成系统</span>
    </footer>
  </div>
</template>

<script setup name="Portal">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listProject } from '@/api/ai/project'
import { useDict } from '@/utils/dict'

const router = useRouter()
const { ai_project_step, ai_project_status, ai_db_type } = useDict('ai_project_step', 'ai_project_status', 'ai_db_type')

const stepOrder = [
  { value: 'REQ', color: '#409eff' },
  { value: 'CLARIFY', color: '#909399' },
  { value: 'PRD', color: '#67c23a' },
  { value: 'PROTO', color: '#e6a23c' },
  { value: 'TECH', color: '#909399' },
  { value: 'DB', color: '#f56c6c' },
  { value: 'DONE', color: '#67c23a' }
]

const loading = ref(false)
const projectList = ref([])
const total = ref(0)
const allProjectList = ref([])
const queryParams = ref({
  pageNum: 1,
  pageSize: 10
})

const stats = computed(() => {
  const t = total.value
  const done = allProjectList.value.filter(item => item.step === 'DONE').length
  return { total: t, doing: t - done, done }
})

function stepIndex(value) {
  const idx = stepOrder.findIndex(s => s.value === value)
  return idx === -1 ? -1 : idx
}

function stepPercent(value) {
  const idx = stepOrder.findIndex(s => s.value === value)
  if (idx === -1) return 0
  return Math.round(((idx + 1) / stepOrder.length) * 100)
}

function stepColor(value) {
  const hit = stepOrder.find(s => s.value === value)
  return hit ? hit.color : '#c0c4cc'
}

function dbTypeLabel(value) {
  const hit = ai_db_type.value.find(d => d.value === value)
  return hit ? hit.label : value
}

function stepLabel(value) {
  const hit = ai_project_step.value.find(d => d.value === value)
  return hit ? hit.label : value || '未开始'
}

function formatTime(value) {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 16)
}

function goAdmin() {
  router.push('/index')
}

function goCreate() {
  router.push('/portal/create')
}

function goProject(id) {
  router.push(`/portal/project/${id}`)
}

function getList() {
  loading.value = true
  listProject(queryParams.value).then(response => {
    projectList.value = response.rows || []
    total.value = response.total || 0
    loading.value = false
  }).catch(() => {
    loading.value = false
  })
}

function getAllList() {
  listProject({ pageNum: 1, pageSize: 1000 }).then(response => {
    allProjectList.value = response.rows || []
  })
}

onMounted(() => {
  getList()
  getAllList()
})
</script>

<style scoped>
.portal {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f7f8fa;
}

/* ===== Header ===== */
.portal-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.portal-header-inner {
  max-width: 1080px;
  margin: 0 auto;
  height: 64px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.portal-brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-logo {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: linear-gradient(135deg, #409eff, #6a5cff);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 10px rgba(64, 158, 255, 0.25);
}

.brand-name {
  font-size: 17px;
  font-weight: 600;
  color: #1f2329;
  letter-spacing: 0.3px;
}

.admin-btn {
  border-radius: 8px;
  padding: 8px 16px;
  color: #4b5565;
  border-color: #e5e7eb;

  &:hover {
    color: #409eff;
    border-color: #409eff;
    background: rgba(64, 158, 255, 0.06);
  }

  .el-icon {
    margin-right: 6px;
  }
}

/* ===== Main ===== */
.portal-main {
  flex: 1;
  width: 100%;
  max-width: 1080px;
  margin: 0 auto;
  padding: 56px 24px 40px;
}

.portal-hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 40px;
}

.hero-left h1 {
  margin: 0 0 12px;
  font-size: 32px;
  font-weight: 700;
  color: #1f2329;
  letter-spacing: 0.5px;
}

.hero-left p {
  margin: 0;
  font-size: 15px;
  color: #86909c;
}

.create-btn {
  border-radius: 8px;
  padding: 10px 20px;
  font-size: 14px;

  .el-icon {
    margin-right: 6px;
  }
}

/* ===== Stats ===== */
.portal-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 40px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  background: #fff;
  border-radius: 12px;
  padding: 18px 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04), 0 4px 12px rgba(0, 0, 0, 0.04);
  transition: transform 0.2s, box-shadow 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.06), 0 8px 24px rgba(0, 0, 0, 0.06);
}

.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.stat-total {
  background: linear-gradient(135deg, #409eff, #6a5cff);
}

.stat-doing {
  background: linear-gradient(135deg, #ffb64d, #ff8f4d);
}

.stat-done {
  background: linear-gradient(135deg, #34d399, #10b981);
}

.stat-num {
  font-size: 26px;
  font-weight: 700;
  color: #1f2329;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: #86909c;
  margin-top: 2px;
}

/* ===== Project List ===== */
.project-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.project-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  border: 1px solid #f0f1f3;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.03);
  transition: transform 0.2s, box-shadow 0.2s, border-color 0.2s;
  display: flex;
  gap: 24px;
  cursor: pointer;
}

.project-card:hover {
  transform: translateY(-2px);
  border-color: #e4ecfb;
  box-shadow: 0 6px 16px rgba(64, 158, 255, 0.08);
}

.project-card-left {
  flex: 1;
  min-width: 0;
}

.project-card-right {
  width: 120px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: center;
  gap: 6px;
  padding-left: 20px;
  border-left: 1px solid #f0f1f3;
}

.project-card-right .step-percent {
  font-size: 18px;
  font-weight: 700;
  color: #1f2329;
}

@media (max-width: 768px) {
  .portal-stats {
    grid-template-columns: 1fr;
  }
  .portal-hero {
    flex-direction: column;
    align-items: flex-start;
  }
  .project-card {
    flex-direction: column;
    gap: 16px;
  }
  .project-card-right {
    width: 100%;
    padding-left: 0;
    border-left: none;
    border-top: 1px solid #f0f1f3;
    padding-top: 16px;
  }
}

.project-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.project-name {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.name-text {
  font-size: 16px;
  font-weight: 600;
  color: #1f2329;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.project-intro {
  margin: 0 0 10px;
  font-size: 13px;
  color: #4e5969;
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.project-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  color: #86909c;
  font-size: 12px;
}

.project-meta span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.project-card-right .step-percent {
  font-size: 18px;
  font-weight: 700;
  color: #1f2329;
}

.project-card-right .el-tag {
  align-self: flex-end;
}

.project-step :deep(.el-progress-bar__outer) {
  background-color: #f0f1f3;
}

.project-step :deep(.el-progress-bar__inner) {
  border-radius: 4px;
}

.loading-mask {
  min-height: 120px;
}

/* ===== Pagination ===== */
.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

/* ===== Footer ===== */
.portal-footer {
  text-align: center;
  padding: 20px 16px;
  color: #c0c4cc;
  font-size: 12px;
  background: #fff;
  border-top: 1px solid #f0f1f3;
}
</style>
