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
          <el-dropdown class="user-dropdown" trigger="hover" @command="handleCommand">
            <div class="user-wrapper">
              <img :src="userStore.avatar || defAva" class="user-avatar" />
              <span class="user-name">{{ userStore.nickName || '未登录' }}</span>
              <el-icon class="user-caret"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <router-link to="/user/profile">
                  <el-dropdown-item>
                    <el-icon><User /></el-icon>
                    <span>个人中心</span>
                  </el-dropdown-item>
                </router-link>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  <span>退出登录</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
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

      <section class="portal-stats">
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
        <div class="section-header">
          <h2 class="section-title">项目列表</h2>
          <span class="section-count">共 {{ filteredProjects.length }} 个项目</span>
        </div>
        <div class="filter-bar">
          <el-input
            v-model="searchKeyword"
            class="filter-search"
            placeholder="搜索项目名称 / 简介"
            clearable
            :prefix-icon="Search"
          />
          <el-select v-model="filterStatus" class="filter-select" placeholder="项目状态" clearable>
            <el-option v-for="s in ai_project_status" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
          <el-select v-model="filterStep" class="filter-select" placeholder="当前阶段" clearable>
            <el-option v-for="s in ai_project_step" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
          <div class="filter-bar-spacer"></div>
          <el-button v-if="hasFilter" class="filter-reset" @click="resetFilter">
            <el-icon><RefreshRight /></el-icon>重置
          </el-button>
        </div>

        <template v-if="!loading && filteredProjects.length > 0">
          <div class="project-list">
            <div class="project-card" v-for="item in pagedProjects" :key="item.projectId" @click="goProject(item)">
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
                <div class="step-percent" :style="{ color: stepColor(item.step) }">{{ stepPercent(item.step) }}%</div>
                <el-progress :percentage="stepPercent(item.step)" :color="stepColor(item.step)" :stroke-width="6" :show-text="false" />
                <dict-tag :options="ai_project_step" :value="item.step" />
              </div>
            </div>
          </div>
          <div class="pagination-wrap" v-if="filteredProjects.length > pageSize">
            <el-pagination
              v-model:current-page="currentPage"
              :page-size="pageSize"
              :total="filteredProjects.length"
              layout="prev, pager, next"
              background
            />
          </div>
        </template>

        <el-empty v-else-if="!loading && allProjectList.length === 0" description="暂无项目数据" />
        <el-empty v-else-if="!loading" description="未找到符合条件的项目" />
        <div v-loading="loading" class="loading-mask"></div>
      </section>
    </main>

    <footer class="portal-footer">
      <span>AI 智能需求设计与数据库生成系统</span>
    </footer>

    <!-- 项目阶段概览弹窗 -->
    <el-dialog
      v-model="stageDialogVisible"
      :title="null"
      width="600px"
      class="stage-dialog"
      :close-on-click-modal="true"
      @closed="activeProject = null"
    >
      <div v-if="activeProject" class="stage-dialog-body">
        <div class="sd-header">
          <div class="sd-title-wrap">
            <div class="sd-name">{{ activeProject.projectName }}</div>
            <div class="sd-title-tags">
              <dict-tag :options="ai_project_status" :value="activeProject.status" />
              <span v-if="activeProject.step === 'DONE'" class="sd-done-tag">
                <el-icon><CircleCheck /></el-icon> 已完成
              </span>
            </div>
          </div>
          <p class="sd-intro">{{ activeProject.projectIntro || '暂无项目简介' }}</p>
        </div>

        <div class="sd-steps">
          <div
            v-for="(s, idx) in visibleStages"
            :key="s.value"
            class="sd-step"
            :class="s.status"
            @click="chooseStage(s)"
          >
            <div class="sd-dot">
              <el-icon v-if="s.status === 'done'"><Check /></el-icon>
              <el-icon v-else-if="s.status === 'current'"><Loading /></el-icon>
              <span v-else>{{ idx + 1 }}</span>
            </div>
            <div class="sd-label">{{ s.label }}</div>
            <div class="sd-state">
              <span v-if="s.status === 'done'" class="sd-state-done">已完成</span>
              <span v-else-if="s.status === 'current'" class="sd-state-current">进行中</span>
              <span v-else class="sd-state-pending">未开始</span>
            </div>
            <div v-if="idx < visibleStages.length - 1" class="sd-line" :class="{ on: s.status === 'done' }"></div>
          </div>
        </div>

        <div class="sd-footer">
          <span class="sd-tip">{{ activeProject && activeProject.step === 'DONE' ? '所有阶段已完成，点击上方阶段可查看历史产出' : '点击任意阶段进入，或继续当前阶段' }}</span>
          <el-button v-if="activeProject && activeProject.step !== 'DONE'" type="primary" @click="chooseStage(currentStage)">
            进入「{{ currentStageLabel }}」
            <el-icon class="el-icon--right"><ArrowRight /></el-icon>
          </el-button>
          <span v-else class="sd-done-badge">
            <el-icon><CircleCheck /></el-icon>
            项目已完成
          </span>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup name="Portal">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { Search, Check, Loading, ArrowRight, CircleCheck, ArrowDown, User, SwitchButton } from '@element-plus/icons-vue'
import { listProject } from '@/api/ai/project'
import { useDict } from '@/utils/dict'
import useUserStore from '@/store/modules/user'
import defAva from '@/assets/images/profile.jpg'

const router = useRouter()
const userStore = useUserStore()
const { ai_project_step, ai_project_status } = useDict('ai_project_step', 'ai_project_status')

const stageDefs = [
  { value: 'REQ', label: '需求采集', route: 'req' },
  { value: 'CLARIFY', label: 'AI 澄清', route: 'clarify' },
  { value: 'PRD', label: 'PRD 文档', route: 'prd' },
  { value: 'PROTO', label: '原型设计', route: 'proto' },
  { value: 'TECH', label: '技术方案', route: 'tech' },
  { value: 'DB', label: '数据库设计', route: 'db' },
  { value: 'DONE', label: '完成', route: 'done' }
]

const stepRouteMap = {
  REQ: 'req',
  CLARIFY: 'clarify',
  PRD: 'prd',
  PROTO: 'proto',
  TECH: 'tech',
  DB: 'db',
  DONE: 'done'
}

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
const allProjectList = ref([])
const searchKeyword = ref('')
const filterStatus = ref('')
const filterStep = ref('')
const currentPage = ref(1)
const pageSize = 10

const stageDialogVisible = ref(false)
const activeProject = ref(null)

const activeStages = computed(() => {
  if (!activeProject.value) return []
  // 已完成项目（step=DONE）：所有阶段均视为「已完成」，避免末节点被误标为「进行中」
  const curIdx = activeProject.value.step === 'DONE'
    ? stageDefs.length
    : stageDefs.findIndex(s => s.value === (activeProject.value.step || 'REQ'))
  return stageDefs.map((s, i) => ({
    ...s,
    status: i < curIdx ? 'done' : (i === curIdx ? 'current' : 'pending')
  }))
})

// 弹窗内展示的阶段列表：已完成项目不显示「完成」节点（无对应页面，纯占位）
const visibleStages = computed(() => {
  const stages = activeStages.value
  if (!stages.length) return []
  // DONE 项目：只展示前 6 个真实阶段
  if (activeProject.value?.step === 'DONE') return stages.filter(s => s.value !== 'DONE')
  return stages
})

const currentStage = computed(() => {
  const hit = stageDefs.find(s => s.value === (activeProject.value?.step || 'REQ'))
  return hit || stageDefs[0]
})

const currentStageLabel = computed(() => currentStage.value.label)

const stats = computed(() => {
  const list = allProjectList.value
  const total = list.length
  const done = list.filter(item => item.step === 'DONE').length
  return { total, doing: total - done, done }
})

const filteredProjects = computed(() => {
  const kw = searchKeyword.value.trim().toLowerCase()
  return allProjectList.value.filter(item => {
    if (filterStatus.value && item.status !== filterStatus.value) return false
    if (filterStep.value && item.step !== filterStep.value) return false
    if (kw) {
      const hay = `${item.projectName || ''} ${item.projectIntro || ''}`.toLowerCase()
      if (!hay.includes(kw)) return false
    }
    return true
  })
})

const pagedProjects = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredProjects.value.slice(start, start + pageSize)
})

const hasFilter = computed(() =>
  !!searchKeyword.value.trim() || !!filterStatus.value || !!filterStep.value
)

function stepPercent(value) {
  const idx = stepOrder.findIndex(s => s.value === value)
  if (idx === -1) return 0
  return Math.round(((idx + 1) / stepOrder.length) * 100)
}

function stepColor(value) {
  const hit = stepOrder.find(s => s.value === value)
  return hit ? hit.color : '#c0c4cc'
}

function formatTime(value) {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 16)
}

function resetFilter() {
  searchKeyword.value = ''
  filterStatus.value = ''
  filterStep.value = ''
}

function goAdmin() {
  router.push('/index')
}

function handleCommand(command) {
  if (command === 'logout') {
    logout()
  }
}

function logout() {
  ElMessageBox.confirm('确定注销并退出系统吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    userStore.logOut().then(() => {
      location.href = '/login'
    })
  }).catch(() => { })
}

function goCreate() {
  router.push('/portal/create')
}

function goProject(item) {
  activeProject.value = item
  stageDialogVisible.value = true
}

function chooseStage(stage) {
  if (!activeProject.value || !stage) return
  // 「完成」是终点状态，没有对应可编辑页面，不导航
  if (stage.value === 'DONE') return
  const id = activeProject.value.projectId
  const routeName = stage.route || stepRouteMap[stage.value] || 'req'
  stageDialogVisible.value = false
  router.push(`/portal/project/${id}/${routeName}`)
}

function getAllList() {
  loading.value = true
  listProject({ pageNum: 1, pageSize: 1000 }).then(response => {
    allProjectList.value = response.rows || []
    loading.value = false
  }).catch(() => {
    loading.value = false
  })
}

watch([searchKeyword, filterStatus, filterStep], () => {
  currentPage.value = 1
})

onMounted(() => {
  // 确保用户信息（昵称/头像）已加载，供右上角头像下拉展示
  if (!userStore.nickName) {
    userStore.getInfo().catch(() => {})
  }
  getAllList()
})
</script>

<style scoped>
.portal {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f6f9;
  padding: 0 24px;
  overflow-x: clip;
}

/* ===== Header ===== */
.portal-header {
  position: sticky;
  top: 0;
  z-index: 100;
  margin: 0 -24px;
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(16px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

.portal-header-inner {
  height: 60px;
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
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: linear-gradient(135deg, #3370ff, #6e52ff);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(51, 112, 255, 0.3);
}

.brand-name {
  font-size: 16px;
  font-weight: 600;
  color: #1d2129;
  letter-spacing: 0.3px;
}

.admin-btn {
  border-radius: 8px;
  padding: 8px 16px;
  font-size: 13px;
  color: #4e5969;
  border-color: #e5e6eb;

  &:hover {
    color: #3370ff;
    border-color: #3370ff;
    background: rgba(51, 112, 255, 0.06);
  }

  .el-icon { margin-right: 5px; }
}

/* ===== Main ===== */
.portal-main {
  flex: 1;
  width: 100%;
  max-width: 1100px;
  margin: 0 auto;
  padding: 48px 24px 36px;
}

/* ----- Hero ----- */
.portal-hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 36px;
}

.hero-left h1 {
  margin: 0 0 8px;
  font-size: 28px;
  font-weight: 700;
  color: #1d2129;
  letter-spacing: 0.5px;
  line-height: 1.3;
}

.hero-left p {
  margin: 0;
  font-size: 14px;
  color: #86909c;
  line-height: 1.6;
}

.create-btn {
  border-radius: 8px;
  padding: 10px 22px;
  font-size: 14px;
  font-weight: 500;
  background: linear-gradient(135deg, #3370ff, #5b8bff);
  border: none;

  &:hover {
    background: linear-gradient(135deg, #2563eb, #4a7fff);
  }

  .el-icon { margin-right: 6px; }
}

/* ----- Stats ----- */
.portal-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 32px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  background: #fff;
  border-radius: 14px;
  padding: 20px 22px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03), 0 4px 16px rgba(0, 0, 0, 0.03);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.12);
}

.stat-total { background: linear-gradient(135deg, #3370ff, #5b8bff); }
.stat-doing  { background: linear-gradient(135deg, #ff9500, #ffb340); }
.stat-done  { background: linear-gradient(135deg, #00b42a, #30c46c); }

.stat-num {
  font-size: 28px;
  font-weight: 700;
  color: #1d2129;
  line-height: 1.2;
  letter-spacing: -0.5px;
}

.stat-label {
  font-size: 13px;
  color: #86909c;
  margin-top: 3px;
}

/* ----- Section Header ----- */
.section-header {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 14px;
}

.section-title {
  margin: 0;
  font-size: 17px;
  font-weight: 600;
  color: #1d2129;
}

.section-count {
  font-size: 13px;
  color: #86909c;
}

/* ----- Filter Bar ----- */
.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 18px;
  flex-wrap: wrap;
  padding: 14px 18px;
  background: #fff;
  border-radius: 12px;
  border: 1px solid #eeeef0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02);
}
.filter-search { width: 320px; max-width: 100%; }
.filter-select { width: 150px; flex-shrink: 0; }
.filter-bar-spacer { flex: 1; min-width: 40px; }
.filter-reset {
  font-size: 13px;
  padding: 8px 16px;
  border-radius: 8px;
  flex-shrink: 0;
}

/* ----- Project List ----- */
.project-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.project-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px 22px;
  border: 1px solid #f0f1f3;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02);
  display: flex;
  gap: 24px;
  cursor: pointer;
}

.project-card:hover {
  border-color: #d6e4ff;
  box-shadow: 0 8px 24px rgba(51, 112, 255, 0.08), 0 2px 6px rgba(0, 0, 0, 0.03);
}

.project-card-left {
  flex: 1;
  min-width: 0;
}

.project-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 8px;
}

.project-name {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.name-text {
  font-size: 15px;
  font-weight: 600;
  color: #1d2129;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.project-intro {
  margin: 0 0 12px;
  font-size: 13px;
  color: #4e5969;
  line-height: 1.65;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.project-meta {
  display: flex;
  align-items: center;
  gap: 18px;
  color: #86909c;
  font-size: 12px;
}

.project-meta span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

/* ----- Card Right (Progress) ----- */
.project-card-right {
  width: 110px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: center;
  gap: 7px;
  padding-left: 20px;
  border-left: 1px solid #f2f3f5;
}

.project-card-right .step-percent {
  font-size: 20px;
  font-weight: 700;
  color: var(--step-accent, #1d2129);
  line-height: 1;
}

.project-card-right .el-tag {
  align-self: flex-end;
}

/* ----- Loading / Empty ----- */
.loading-mask {
  min-height: 120px;
}

/* ----- Pagination ----- */
.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

/* ----- Footer ----- */
.portal-footer {
  text-align: center;
  padding: 18px 16px;
  color: #c0c4cc;
  font-size: 12px;
  background: transparent;
  border-top: none;
  margin-top: auto;
}

/* ===== Stage Dialog ===== */
.stage-dialog .el-dialog__header { display: none; }
.stage-dialog .el-dialog__body { padding: 0; }

.stage-dialog-body {
  padding: 24px 26px 20px;
}

.sd-header {
  border-bottom: 1px solid #f2f3f5;
  padding-bottom: 16px;
  margin-bottom: 22px;
}

.sd-title-wrap {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.sd-title-tags {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.sd-done-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 600;
  color: #00b42a;
  background: rgba(0, 180, 42, 0.08);
  border-radius: 10px;
  padding: 2px 10px;
}

.sd-name {
  font-size: 18px;
  font-weight: 700;
  color: #1d2129;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sd-intro {
  margin: 8px 0 0;
  font-size: 13px;
  color: #86909c;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.sd-steps {
  display: flex;
  align-items: flex-start;
}

.sd-step {
  position: relative;
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 2px;
  border-radius: 10px;
  transition: background 0.2s;
}

.sd-step:hover { background: #f5f7ff; }
.sd-step.done:hover { background: rgba(0, 180, 42, 0.06); }

.sd-dot {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: #e5e7eb;
  color: #86909c;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  transition: all 0.3s;
  z-index: 1;
}

.sd-step.done .sd-dot {
  background: #00b42a;
  color: #fff;
  box-shadow: 0 2px 8px rgba(0, 180, 42, 0.3);
}

.sd-step.current .sd-dot {
  background: #3370ff;
  color: #fff;
  transform: scale(1.12);
  box-shadow: 0 2px 10px rgba(51, 112, 255, 0.35);
}

.sd-label {
  font-size: 12px;
  color: #4e5969;
  text-align: center;
  line-height: 1.3;
  white-space: nowrap;
}

.sd-step.done .sd-label { color: #1d2129; font-weight: 500; }
.sd-step.current .sd-label { color: #3370ff; font-weight: 600; }

.sd-state {
  font-size: 11px;
}

.sd-state-done { color: #00b42a; }
.sd-state-current { color: #3370ff; }
.sd-state-pending { color: #c0c4cc; }

.sd-line {
  position: absolute;
  top: 19px;
  left: 50%;
  width: 100%;
  height: 2px;
  background: #e5e7eb;
  z-index: 0;
}

.sd-line.on { background: #00b42a; }

.sd-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 22px;
  padding-top: 16px;
  border-top: 1px solid #f2f3f5;
}

.sd-tip {
  font-size: 12px;
  color: #a0a4ad;
}

.sd-footer .el-button {
  border-radius: 8px;
  padding: 9px 20px;
  font-weight: 500;
}

.sd-done-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 16px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  color: #00b42a;
  background: rgba(0, 180, 42, 0.08);
  border: 1px solid rgba(0, 180, 42, 0.18);
}

/* ===== Responsive ===== */
@media (max-width: 768px) {
  .portal-hero {
    flex-direction: column;
    align-items: flex-start;
    margin-bottom: 28px;
  }

  .portal-stats {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .filter-bar { padding: 12px 14px; gap: 10px; }
  .filter-search { width: 100%; flex: 1 1 100%; }
  .filter-select { flex: 1; min-width: 120px; }
  .filter-bar-spacer { display: none; }

  .project-card {
    flex-direction: column;
    gap: 14px;
    padding: 16px 18px;
  }

  .project-card-right {
    width: 100%;
    padding-left: 0;
    border-left: none;
    border-top: 1px solid #f2f3f5;
    padding-top: 14px;
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
  }

  .project-card-right .step-percent {
    font-size: 18px;
  }
}
</style>
