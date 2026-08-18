<template>
  <div class="portal">
    <PortalHeader />

    <main class="portal-main">
      <section class="portal-hero">
        <div class="hero-left">
          <h1>你好，{{ userStore.nickName || '访客' }} <span class="hero-wave">👋</span></h1>
          <p>欢迎回到 AI 智能需求设计工作台，查看并推进你的项目</p>
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

      <section v-if="continueProjects.length" class="portal-continue">
        <div class="section-header">
          <h2 class="section-title">继续你的项目</h2>
          <span class="section-count">{{ continueProjects.length }} 个进行中</span>
        </div>
        <div class="continue-grid">
          <ContinueCard
            v-for="item in continueProjects"
            :key="item.projectId"
            :project="item"
            @open="onOpenProject"
            @continue="onContinueProject"
          />
        </div>
      </section>

      <section class="portal-body">
        <!-- 全新用户：引导式空态（无任何项目），隐藏筛选栏 -->
        <EmptyState
          v-if="!loading && allProjectList.length === 0"
          variant="onboarding"
          @create="goCreate"
        />

        <template v-else>
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
              <ProjectRow
                v-for="item in pagedProjects"
                :key="item.projectId"
                :project="item"
                @open="onOpenProject"
                @continue="onContinueProject"
              />
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

          <!-- 有项目但筛选无结果 -->
          <EmptyState
            v-else-if="!loading"
            variant="noResult"
            :keyword="searchKeyword"
            @reset="resetFilter"
          />
          <div v-loading="loading" class="loading-mask"></div>
        </template>
      </section>
    </main>

    <footer class="portal-footer">
      <span>AI 智能需求设计与数据库生成系统</span>
    </footer>
  </div>
</template>

<script setup name="Portal">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Loading, CircleCheck } from '@element-plus/icons-vue'
import { listProject } from '@/api/ai/project'
import { useDict } from '@/utils/dict'
import useUserStore from '@/store/modules/user'
import ProjectRow from './components/ProjectRow.vue'
import ContinueCard from './components/ContinueCard.vue'
import EmptyState from './components/EmptyState.vue'
import PortalHeader from './components/PortalHeader.vue'

const router = useRouter()
const userStore = useUserStore()
const { ai_project_step, ai_project_status } = useDict('ai_project_step', 'ai_project_status')

// 阶段 → 路由名（用于「继续」一步直达当前阶段）
const stepRouteMap = {
  REQ: 'req',
  CLARIFY: 'clarify',
  PRD: 'prd',
  PROTO: 'proto',
  TECH: 'tech',
  DB: 'db',
  DONE: 'done'
}

const loading = ref(false)
const allProjectList = ref([])
const searchKeyword = ref('')
const filterStatus = ref('')
const filterStep = ref('')
const currentPage = ref(1)
const pageSize = 10

const stats = computed(() => {
  const list = allProjectList.value
  const total = list.length
  const done = list.filter(item => item.step === 'DONE').length
  return { total, doing: total - done, done }
})

// 「继续你的项目」：进行中项目，置顶优先 + 最近更新，取前 3
const continueProjects = computed(() => {
  return allProjectList.value
    .filter(item => item.step !== 'DONE')
    .sort((a, b) => {
      const ta = a.isTop === 'Y' ? 1 : 0
      const tb = b.isTop === 'Y' ? 1 : 0
      if (ta !== tb) return tb - ta
      return String(b.updateTime || '').localeCompare(String(a.updateTime || ''))
    })
    .slice(0, 3)
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

function resetFilter() {
  searchKeyword.value = ''
  filterStatus.value = ''
  filterStep.value = ''
}

function goCreate() {
  router.push('/portal/create')
}

// 整行 / 卡片点击：进入项目总览页（/portal/project/:id）
function onOpenProject(project) {
  router.push(`/portal/project/${project.projectId}`)
}

// CTA「继续」：一步直达当前阶段（/portal/project/:id/:stage）
function onContinueProject(project) {
  if (!project || project.step === 'DONE') return
  const routeName = stepRouteMap[project.step] || 'req'
  router.push(`/portal/project/${project.projectId}/${routeName}`)
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
  // 确保用户信息（昵称）已加载，供问候区展示
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
  padding: 0;
}

/* ===== Main ===== */
.portal-main {
  flex: 1;
  width: 100%;
  max-width: 1440px;
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

.hero-wave {
  font-size: 24px;
  display: inline-block;
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

/* ----- Continue (精选区) ----- */
.portal-continue {
  margin-bottom: 32px;
}

.continue-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
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

  .continue-grid {
    grid-template-columns: 1fr;
  }

  .filter-bar { padding: 12px 14px; gap: 10px; }
  .filter-search { width: 100%; flex: 1 1 100%; }
  .filter-select { flex: 1; min-width: 120px; }
  .filter-bar-spacer { display: none; }
}
</style>
