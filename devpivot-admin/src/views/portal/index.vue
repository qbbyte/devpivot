<template>
  <div class="portal">
    <PortalHeader />

    <main class="portal-main">
      <section class="portal-hero">
        <div class="hero-left">
          <h1>你好，{{ userStore.nickName || '访客' }} <span class="hero-wave">👋</span></h1>
          <p>{{ stats.doing ? `当前有 ${stats.doing} 个项目进行中，欢迎回来继续推进` : '还没有进行中的项目，点击右上角创建你的第一个 AI 需求设计' }}</p>
        </div>
        <el-button type="primary" class="create-btn" @click="goCreate">
          <el-icon><Plus /></el-icon>
          <span>新建项目</span>
        </el-button>
      </section>

      <div class="portal-layout">
        <!-- ===== 左主列：筛选 + 项目卡片网格 ===== -->
        <section class="portal-main-col">
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
              <div class="project-grid">
                <ContinueCard
                  v-for="item in visibleProjects"
                  :key="item.projectId"
                  :project="item"
                  @open="onOpenProject"
                  @continue="onContinueProject"
                />
              </div>
              <div v-if="hasMore" class="load-more-wrap">
                <el-button class="load-more-btn" @click="loadMore">
                  加载更多<el-icon class="load-more-icon"><ArrowDown /></el-icon>
                </el-button>
              </div>
              <p v-else-if="filteredProjects.length > pageSize" class="list-end">
                已显示全部 {{ filteredProjects.length }} 个项目
              </p>
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

        <!-- ===== 右侧栏：统计 + 快捷入口 + 最近更新 ===== -->
        <aside class="portal-aside">
          <div class="aside-block">
            <h3 class="aside-title">项目统计</h3>
            <div class="stat-list">
              <div v-for="s in statItems" :key="s.label" class="stat-item">
                <span class="stat-item-icon" :class="s.cls">
                  <el-icon :size="16"><component :is="s.icon" /></el-icon>
                </span>
                <div class="stat-item-body">
                  <div class="stat-item-num">{{ s.value }}</div>
                  <div class="stat-item-label">{{ s.label }}</div>
                </div>
              </div>
            </div>
          </div>

          <div class="aside-block">
            <h3 class="aside-title">快捷入口</h3>
            <div class="quick-list">
              <button class="quick-item" @click="goCreate">
                <span class="quick-icon qi-primary"><el-icon><Plus /></el-icon></span>
                <span class="quick-text"><strong>新建项目</strong><em>开启新一轮 AI 需求设计</em></span>
              </button>
              <button class="quick-item" @click="router.push('/portal/team')">
                <span class="quick-icon qi-indigo"><el-icon><User /></el-icon></span>
                <span class="quick-text"><strong>我的团队</strong><em>成员协作与 Git 仓库统计</em></span>
              </button>
              <button v-hasRole="['admin']" class="quick-item" @click="router.push('/index')">
                <span class="quick-icon qi-neutral"><el-icon><Setting /></el-icon></span>
                <span class="quick-text"><strong>管理后台</strong><em>系统配置与全局模型设置</em></span>
              </button>
            </div>
          </div>

          <div v-if="recentProjects.length" class="aside-block">
            <h3 class="aside-title">最近更新</h3>
            <div class="recent-list">
              <button
                v-for="p in recentProjects"
                :key="p.projectId"
                class="recent-item"
                @click="onOpenProject(p)"
              >
                <span class="recent-name">{{ p.projectName }}</span>
                <span class="recent-meta">
                  <el-tag size="small" type="primary" class="recent-tag">{{ stepLabelOf(p.step) }}</el-tag>
                  <span class="recent-time">{{ relativeTime(p.updateTime) }}</span>
                </span>
              </button>
            </div>
          </div>
        </aside>
      </div>
    </main>

    <footer class="portal-footer">
      <span>AI 智能需求设计与数据库生成系统</span>
    </footer>
  </div>
</template>

<script setup name="Portal">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Loading, CircleCheck, FolderOpened, Plus, User, Setting, ArrowDown } from '@element-plus/icons-vue'
import { listMyProject } from '@/api/ai/project'
import { useDict } from '@/utils/dict'
import useUserStore from '@/store/modules/user'
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
const pageSize = 10
const visibleCount = ref(pageSize)

const stats = computed(() => {
  const list = allProjectList.value
  const total = list.length
  const done = list.filter(item => item.step === 'DONE').length
  return { total, doing: total - done, done }
})

// 侧栏统计条目（图标/配色/数值）
const statItems = computed(() => [
  { label: '项目总数', value: stats.value.total, icon: FolderOpened, cls: 'st-total' },
  { label: '进行中', value: stats.value.doing, icon: Loading, cls: 'st-doing' },
  { label: '已完成', value: stats.value.done, icon: CircleCheck, cls: 'st-done' }
])

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

// 主区可见项目：前 N 条，点「加载更多」递增
const visibleProjects = computed(() => filteredProjects.value.slice(0, visibleCount.value))
const hasMore = computed(() => visibleCount.value < filteredProjects.value.length)

// 侧栏「最近更新」：按更新时间倒序取前 5
const recentProjects = computed(() =>
  [...allProjectList.value]
    .sort((a, b) => String(b.updateTime || '').localeCompare(String(a.updateTime || '')))
    .slice(0, 5)
)

const hasFilter = computed(() =>
  !!searchKeyword.value.trim() || !!filterStatus.value || !!filterStep.value
)

function stepLabelOf(step) {
  const hit = ai_project_step.value.find(o => o.value === step)
  return hit ? hit.label : '项目'
}
function relativeTime(value) {
  if (!value) return ''
  const t = new Date(String(value).replace(' ', 'T'))
  if (isNaN(t.getTime())) return String(value).slice(0, 16)
  const diff = Date.now() - t.getTime()
  const m = Math.floor(diff / 60000)
  if (m < 1) return '刚刚'
  if (m < 60) return m + ' 分钟前'
  const h = Math.floor(m / 60)
  if (h < 24) return h + ' 小时前'
  const d = Math.floor(h / 24)
  if (d < 30) return d + ' 天前'
  return String(value).slice(0, 10)
}

function loadMore() {
  visibleCount.value += pageSize
}

function resetFilter() {
  searchKeyword.value = ''
  filterStatus.value = ''
  filterStep.value = ''
}

function goCreate() {
  router.push('/portal/create')
}

// 卡片点击：进入项目总览页（/portal/project/:id）
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
  listMyProject({ pageNum: 1, pageSize: 1000 }).then(response => {
    allProjectList.value = response.rows || []
    loading.value = false
  }).catch(() => {
    loading.value = false
  })
}

watch([searchKeyword, filterStatus, filterStep], () => {
  visibleCount.value = pageSize
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
  background: var(--c-bg);
  padding: 0;
}

/* ===== Main ===== */
.portal-main {
  flex: 1;
  width: 100%;
  max-width: 1440px;
  margin: 0 auto;
  padding: 40px 24px 36px;
}

/* ----- Hero ----- */
.portal-hero {
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 28px;
  padding: 30px 34px;
  border-radius: var(--radius-lg);
  border: 1px solid var(--c-border);
  background: linear-gradient(120deg, var(--c-primary-bg) 0%, var(--c-bg) 45%, #eff6ff 100%);
}

/* 柔和装饰光斑（纯装饰，不干扰内容层级） */
.portal-hero::before,
.portal-hero::after {
  content: "";
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
}
.portal-hero::before {
  right: -70px;
  top: -90px;
  width: 260px;
  height: 260px;
  background: radial-gradient(circle, rgba(37, 99, 235, 0.16) 0%, rgba(37, 99, 235, 0) 70%);
}
.portal-hero::after {
  right: 130px;
  bottom: -110px;
  width: 220px;
  height: 220px;
  background: radial-gradient(circle, rgba(96, 165, 250, 0.14) 0%, rgba(96, 165, 250, 0) 70%);
}

.hero-left {
  position: relative;
  z-index: 1;
}

.hero-left h1 {
  margin: 0 0 8px;
  font-size: 26px;
  font-weight: 700;
  color: var(--c-text);
  letter-spacing: 0.5px;
  line-height: 1.3;
}

.hero-left p {
  margin: 0;
  font-size: 14px;
  color: var(--c-text-muted);
  line-height: 1.6;
}

.hero-wave {
  font-size: 24px;
  display: inline-block;
}

.create-btn {
  position: relative;
  z-index: 1;
  border-radius: var(--radius-sm);
  padding: 10px 22px;
  font-size: 14px;
  font-weight: 500;
  background: linear-gradient(135deg, var(--c-primary), var(--c-primary-light));
  border: none;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.28);
  transition: transform 0.18s ease, box-shadow 0.18s ease;

  &:hover {
    background: linear-gradient(135deg, var(--c-primary), var(--c-primary-light));
    transform: translateY(-1px);
    box-shadow: 0 6px 18px rgba(37, 99, 235, 0.34);
  }

  .el-icon { margin-right: 6px; }
}

/* ----- 双栏布局 ----- */
.portal-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 20px;
  align-items: start;
}
.portal-main-col {
  min-width: 0;
}

/* ----- 侧栏 ----- */
.portal-aside {
  position: sticky;
  top: 84px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.aside-block {
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: var(--radius-md);
  padding: 16px 18px;
  box-shadow: var(--shadow-sm);
}
.aside-title {
  margin: 0 0 12px;
  font-size: 14px;
  font-weight: 600;
  color: var(--c-text);
}

/* 统计条目（竖排） */
.stat-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.stat-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--c-border-light);
  background: var(--c-bg);
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}
.stat-item:hover {
  transform: translateY(-1px);
  box-shadow: var(--shadow-sm);
  border-color: var(--c-primary-light);
}
.stat-item-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
  box-shadow: 0 3px 8px rgba(0, 0, 0, 0.1);
}
.st-total { background: linear-gradient(135deg, var(--c-primary), var(--c-primary-light)); }
.st-doing  { background: linear-gradient(135deg, #f59e0b, #fbbf24); }
.st-done  { background: linear-gradient(135deg, #10b981, #34d399); }
.stat-item-num {
  font-size: 20px;
  font-weight: 700;
  color: var(--c-text);
  line-height: 1.2;
  letter-spacing: -0.3px;
}
.stat-item-label {
  font-size: 12px;
  color: var(--c-text-muted);
  margin-top: 1px;
}

/* 快捷入口 */
.quick-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.quick-item {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  text-align: left;
  border: 1px solid var(--c-border-light);
  background: var(--c-surface);
  border-radius: var(--radius-sm);
  padding: 10px 12px;
  cursor: pointer;
  transition: border-color 0.18s ease, background 0.18s ease;
}
.quick-item:hover {
  border-color: var(--c-primary-light);
  background: var(--c-primary-bg);
}
.quick-icon {
  width: 34px;
  height: 34px;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.qi-primary { background: var(--c-primary-bg); color: var(--c-primary); }
.qi-indigo { background: #e8f4ff; color: #0284c7; }
.qi-neutral { background: #f1f5f9; color: var(--c-text-muted); }
.quick-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.quick-text strong {
  font-size: 13px;
  font-weight: 600;
  color: var(--c-text);
}
.quick-text em {
  font-style: normal;
  font-size: 11px;
  color: var(--c-text-subtle);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 最近更新 */
.recent-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.recent-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  width: 100%;
  text-align: left;
  border: none;
  background: transparent;
  border-radius: var(--radius-sm);
  padding: 8px 10px;
  cursor: pointer;
  transition: background 0.18s ease;
}
.recent-item:hover {
  background: var(--c-primary-bg);
}
.recent-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--c-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.recent-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.recent-tag {
  flex-shrink: 0;
}
.recent-time {
  font-size: 11px;
  color: var(--c-text-subtle);
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
  color: var(--c-text);
}

.section-count {
  font-size: 13px;
  color: var(--c-text-muted);
}

/* ----- Project Grid ----- */
.project-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.load-more-wrap {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
.load-more-btn {
  font-size: 13px;
  padding: 8px 24px;
  border-radius: var(--radius-sm);
}
.load-more-icon {
  margin-left: 4px;
}
.list-end {
  text-align: center;
  margin: 18px 0 4px;
  font-size: 12px;
  color: var(--c-text-subtle);
}

/* ----- Loading / Empty ----- */
.loading-mask {
  min-height: 120px;
}

/* ----- Filter Bar ----- */
.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 18px;
  flex-wrap: wrap;
  padding: 14px 18px;
  background: var(--c-surface);
  border-radius: var(--radius-md);
  border: 1px solid var(--c-border);
  box-shadow: var(--shadow-sm);
}
.filter-search { width: 320px; max-width: 100%; }
.filter-select { width: 150px; flex-shrink: 0; }
.filter-bar-spacer { flex: 1; min-width: 40px; }
.filter-reset {
  font-size: 13px;
  padding: 8px 16px;
  border-radius: var(--radius-sm);
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

/* ----- Footer ----- */
.portal-footer {
  text-align: center;
  padding: 18px 16px;
  color: var(--c-text-subtle);
  font-size: 12px;
  background: transparent;
  border-top: none;
  margin-top: auto;
}

/* ===== Responsive ===== */
@media (max-width: 1200px) {
  .portal-layout {
    grid-template-columns: 1fr;
  }
  .portal-aside {
    position: static;
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
    gap: 16px;
  }
  .project-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .portal-main {
    padding: 24px 16px 28px;
  }
  .portal-hero {
    flex-direction: column;
    align-items: flex-start;
    margin-bottom: 24px;
    padding: 24px 20px;
  }

  .project-grid {
    grid-template-columns: 1fr;
  }

  .filter-bar { padding: 12px 14px; gap: 10px; }
  .filter-search { width: 100%; flex: 1 1 100%; }
  .filter-select { flex: 1; min-width: 120px; }
  .filter-bar-spacer { display: none; }
}

@media (prefers-reduced-motion: reduce) {
  .create-btn,
  .stat-item {
    transition: none;
  }
  .create-btn:hover,
  .stat-item:hover {
    transform: none;
  }
}
</style>
