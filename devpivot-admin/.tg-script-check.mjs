import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTeamDetail } from '@/api/ai/team'
import {
  addTeamProjectRepo, updateTeamProjectRepo, deleteTeamProjectRepo,
  listTeamProjectRepos, getTeamProjectRepo,
  getTeamProjectContributors, getTeamProjectCommits, getTeamProjectBranches,
  getTeamProjectHeatmap
} from '@/api/ai/teamGit'

const route = useRoute()
const router = useRouter()

const teamId = computed(() => Number(route.params.teamId))
const projectId = computed(() => Number(route.params.projectId))
const projectName = ref(route.query.projectName || '')
const teamName = ref('')
const teamDetail = ref(null)

const canManage = computed(() => {
  const role = teamDetail.value && teamDetail.value.myRole
  return role === 'OWNER' || role === 'ADMIN'
})

const PLATFORM_LABEL = { github: 'GitHub', gitlab: 'GitLab', gitee: 'Gitee', gitea: 'Gitea', 'self-hosted': '自托管' }
const platformLabel = computed(() => (activeRepo.value ? (PLATFORM_LABEL[activeRepo.value.platform] || activeRepo.value.platform) : '-'))
const currentBranch = computed(() => gitConfig.repoBranch || (activeRepo.value && activeRepo.value.repoBranch) || '默认分支')

/* ===================== 多仓库状态 ===================== */
const repos = ref([])
const activeRepoId = ref(null)
const activeRepo = computed(() => repos.value.find(r => r.id === activeRepoId.value) || null)
function repoDisplayName(r) { return r.name || r.repoFullName }

/* ===================== 数据状态 ===================== */
const gitLoading = ref(false)
const gitConfig = reactive({ configured: false, platform: '', repoFullName: '', repoBranch: '', repoApiBase: '', maskedToken: '' })
const gitBranches = ref([])
const gitBranchLoading = ref(false)
const gitContributors = ref([])
const gitContribLoading = ref(false)
const gitCommits = ref([])
const gitCommitPage = ref(1)
const gitCommitNoMore = ref(false)
const gitCommitLoading = ref(false)
const gitHeatmap = ref(null)
const gitHeatLoading = ref(false)
const gitHeatError = ref('')

const totalCommits = computed(() => gitContributors.value.reduce((s, c) => s + (c.contributions || 0), 0))
const sortedContributors = computed(() =>
  [...gitContributors.value].sort((a, b) => (b.contributions || 0) - (a.contributions || 0))
)
const maxContrib = computed(() => sortedContributors.value.length ? (sortedContributors.value[0].contributions || 0) : 0)
function barWidth(n) {
  if (!maxContrib.value) return '0%'
  return Math.max(6, Math.round((n / maxContrib.value) * 100)) + '%'
}

const RANK_COLORS = [
  'linear-gradient(90deg, #f59e0b, #f97316)',
  'linear-gradient(90deg, #94a3b8, #64748b)',
  'linear-gradient(90deg, #d97706, #b45309)',
  'linear-gradient(90deg, #6366f1, #818cf8)',
]
const RANK_CLASS = ['rank-gold', 'rank-silver', 'rank-bronze', 'rank-default']
function barColor(i) { return RANK_COLORS[Math.min(i, 3)] }
function rankClass(i) { return RANK_CLASS[Math.min(i, 3)] }

/* ===================== 提交热力图（Gitee/GitHub 风格 7×N 网格） ===================== */
const HEAT_COLORS = ['#ebedf0', '#9be9a8', '#40c463', '#30a14e', '#216e39']
// 网格行序: 周日→周六, 标签只标 一/三/五
const WEEKDAY_LABELS = ['', '一', '', '三', '', '五', '']
function heatLevel(count) {
  if (count <= 0) return 0
  if (count <= 3) return 1
  if (count <= 6) return 2
  if (count <= 9) return 3
  return 4
}
function parseHeatDate(s) {
  const [y, m, d] = String(s).split('-').map(Number)
  return new Date(y, m - 1, d)
}
function heatDateKey(d) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return y + '-' + m + '-' + day
}
const heatGrid = computed(() => {
  const hm = gitHeatmap.value
  if (!hm || !hm.startDate || !hm.endDate) return []
  const counts = hm.counts || {}
  const start = parseHeatDate(hm.startDate)
  const end = parseHeatDate(hm.endDate)
  const first = new Date(start)
  first.setDate(start.getDate() - start.getDay()) // 对齐所在周的周日
  const last = new Date(end)
  last.setDate(end.getDate() + (6 - end.getDay())) // 对齐所在周的周六
  const weeks = []
  const cursor = new Date(first)
  while (cursor <= last) {
    const week = []
    for (let i = 0; i < 7; i++) {
      const d = new Date(cursor)
      const inRange = d >= start && d <= end
      const key = heatDateKey(d)
      week.push({ key, inRange, count: inRange ? (counts[key] || 0) : 0 })
      cursor.setDate(cursor.getDate() + 1)
    }
    weeks.push(week)
  }
  return weeks
})
const heatMonthLabels = computed(() =>
  heatGrid.value.map((week, idx) => {
    const day = week.find(c => c.inRange)
    if (!day) return ''
    if (idx > 0) {
      const prev = heatGrid.value[idx - 1].find(c => c.inRange)
      if (prev && prev.key.slice(0, 7) === day.key.slice(0, 7)) return ''
    }
    return day.key.slice(5, 7).replace(/^0/, '') + '月'
  })
)
function heatCellTitle(cell) {
  if (!cell.inRange) return ''
  return cell.count > 0 ? (cell.key + '：' + cell.count + ' 次提交') : (cell.key + '：暂无提交')
}

/* ===================== 添加/编辑弹窗 ===================== */
const repoDialog = reactive({
  visible: false,
  mode: 'add',           // add | edit
  editingRepoId: null,
  saving: false,
  name: '',
  platform: 'github',
  repoFullName: '',
  repoBranch: '',
  repoApiBase: '',
  accessToken: '',
  maskedToken: '',
  repoUrl: '',
  parsedHost: ''
})

function openAddRepo() {
  Object.assign(repoDialog, {
    visible: true, mode: 'add', editingRepoId: null, saving: false,
    name: '', platform: 'github', repoFullName: '', repoBranch: '', repoApiBase: '',
    accessToken: '', maskedToken: '', repoUrl: '', parsedHost: ''
  })
}
function openEditRepo(r) {
  Object.assign(repoDialog, {
    visible: true, mode: 'edit', editingRepoId: r.id, saving: false,
    name: r.name || '', platform: r.platform || 'github', repoFullName: r.repoFullName || '',
    repoBranch: r.repoBranch || '', repoApiBase: r.repoApiBase || '',
    accessToken: '', maskedToken: gitConfig.maskedToken || '',
    repoUrl: reconstructRepoUrl(r.platform, r.repoFullName, r.repoApiBase), parsedHost: ''
  })
}
function closeRepoModal() {
  if (repoDialog.saving) return
  repoDialog.visible = false
}
async function saveRepoModal() {
  if (!repoDialog.repoFullName.trim()) { ElMessage.warning('请填写仓库地址（将自动解析平台与仓库名）'); return }
  if (repoDialog.platform === 'gitea' || repoDialog.platform === 'self-hosted') {
    if (!repoDialog.repoApiBase.trim()) { ElMessage.warning('自托管 / Gitea 需填写 API 地址'); return }
  }
  if (!projectId.value || !teamId.value) return
  repoDialog.saving = true
  const payload = {
    name: repoDialog.name.trim(),
    platform: repoDialog.platform,
    repoFullName: repoDialog.repoFullName.trim(),
    repoBranch: repoDialog.repoBranch.trim(),
    repoApiBase: repoDialog.repoApiBase.trim(),
    accessToken: repoDialog.accessToken
  }
  try {
    if (repoDialog.mode === 'edit') {
      await updateTeamProjectRepo(teamId.value, repoDialog.editingRepoId, payload)
      ElMessage.success('仓库配置已更新')
    } else {
      const res = await addTeamProjectRepo(teamId.value, projectId.value, payload)
      ElMessage.success('仓库已添加')
      activeRepoId.value = Number(res.data)
    }
    repoDialog.visible = false
    await loadRepos()
    await selectRepo(activeRepoId.value || null)
  } catch (e) {
    // 响应拦截器已统一提示
  } finally {
    repoDialog.saving = false
  }
}
async function removeRepo(r) {
  try {
    await ElMessageBox.confirm(
      `确定删除仓库「${repoDisplayName(r)}」吗？关联的提交统计将一并失效。`,
      '删除仓库',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch (e) {
    return // 用户取消
  }
  try {
    await deleteTeamProjectRepo(teamId.value, r.id)
    ElMessage.success('仓库已删除')
    await loadRepos()
    // 删除后自动切到第一个仓库并重新加载数据
    await selectRepo(activeRepoId.value || null)
  } catch (e) {
    // 响应拦截器已统一提示
  }
}

/* ===================== URL 解析（弹窗内复用） ===================== */
function reconstructRepoUrl(platform, fullName, apiBase) {
  if (!fullName) return ''
  if (platform === 'github') return 'https://github.com/' + fullName
  if (platform === 'gitee') return 'https://gitee.com/' + fullName
  if (platform === 'gitlab') return 'https://gitlab.com/' + fullName
  if (apiBase) {
    const base = apiBase.replace(/\/api\/v1\/?$/, '').replace(/\/api\/v4\/?$/, '')
    return base + '/' + fullName
  }
  return ''
}
function onRepoUrlChange() {
  const raw = (repoDialog.repoUrl || '').trim()
  if (!raw) return
  const cleaned = raw.replace(/\.git$/, '')
  const m = cleaned.match(/^https?:\/\/([^/]+)\/(.+)$/)
  if (!m) {
    ElMessage.warning('无法识别仓库地址，请粘贴 https 开头的网页地址')
    return
  }
  const host = m[1].toLowerCase()
  repoDialog.repoFullName = m[2].replace(/\/+$/, '')
  repoDialog.parsedHost = ''
  if (host === 'github.com') {
    repoDialog.platform = 'github'
    repoDialog.repoApiBase = ''
  } else if (host === 'gitee.com') {
    repoDialog.platform = 'gitee'
    repoDialog.repoApiBase = ''
  } else if (host === 'gitlab.com') {
    repoDialog.platform = 'gitlab'
    repoDialog.repoApiBase = ''
  } else {
    repoDialog.parsedHost = host
    onPlatformChange()
  }
}
function onPlatformChange() {
  if (!repoDialog.parsedHost) {
    if (repoDialog.platform === 'github' || repoDialog.platform === 'gitlab' || repoDialog.platform === 'gitee') {
      repoDialog.repoApiBase = ''
    }
    return
  }
  if (repoDialog.platform === 'gitea') {
    repoDialog.repoApiBase = 'https://' + repoDialog.parsedHost + '/api/v1'
  } else if (repoDialog.platform === 'self-hosted') {
    repoDialog.repoApiBase = 'https://' + repoDialog.parsedHost + '/api/v4'
  } else {
    repoDialog.repoApiBase = ''
  }
}

/* ===================== 数据加载 ===================== */
async function loadTeam() {
  if (!teamId.value) return
  try {
    const res = await getTeamDetail(teamId.value)
    teamDetail.value = res.data || null
    teamName.value = (teamDetail.value && teamDetail.value.teamName) || ''
  } catch (e) {
    if (import.meta.env.DEV) console.warn('加载团队详情失败', e)
  }
}

async function loadRepos() {
  if (!projectId.value || !teamId.value) return
  try {
    const res = await listTeamProjectRepos(teamId.value, projectId.value)
    repos.value = res.data || []
    // 若当前选中的仓库已不存在(删除后)，自动选第一个
    if (!repos.value.find(r => r.id === activeRepoId.value)) {
      activeRepoId.value = repos.value.length ? repos.value[0].id : null
    }
  } catch (e) {
    repos.value = []
    activeRepoId.value = null
  }
}

async function selectRepo(repoId) {
  activeRepoId.value = repoId
  gitCommits.value = []
  gitCommitPage.value = 1
  gitCommitNoMore.value = false
  gitContributors.value = []
  gitHeatmap.value = null
  gitHeatError.value = ''
  await loadRepoConfig()
}

async function loadRepoConfig() {
  if (!activeRepoId.value || !teamId.value) {
    resetGitConfig()
    return
  }
  gitLoading.value = true
  try {
    const res = await getTeamProjectRepo(teamId.value, activeRepoId.value)
    const d = res.data || {}
    gitConfig.configured = !!d.configured
    gitConfig.platform = d.platform || activeRepo.value.platform || 'github'
    gitConfig.repoFullName = d.repoFullName || ''
    gitConfig.repoBranch = d.repoBranch || ''
    gitConfig.repoApiBase = d.repoApiBase || ''
    gitConfig.maskedToken = d.maskedToken || ''
    if (gitConfig.configured) {
      await loadBranches()
      await Promise.all([loadContributors(), loadCommits(1, true), loadHeatmap()])
    }
  } catch (e) {
    if (import.meta.env.DEV) console.warn('加载仓库配置失败', e)
    resetGitConfig()
  } finally {
    gitLoading.value = false
  }
}
function resetGitConfig() {
  gitConfig.configured = false
  gitConfig.platform = ''
  gitConfig.repoFullName = ''
  gitConfig.repoBranch = ''
  gitConfig.repoApiBase = ''
  gitConfig.maskedToken = ''
  gitBranches.value = []
  gitContributors.value = []
  gitCommits.value = []
  gitCommitPage.value = 1
  gitCommitNoMore.value = false
  gitHeatmap.value = null
  gitHeatError.value = ''
}

async function loadContributors() {
  if (!activeRepoId.value || !teamId.value) return
  gitContribLoading.value = true
  try {
    const res = await getTeamProjectContributors(teamId.value, activeRepoId.value)
    gitContributors.value = res.data || []
  } catch (e) { gitContributors.value = [] } finally { gitContribLoading.value = false }
}
async function loadHeatmap() {
  if (!activeRepoId.value || !teamId.value || !gitConfig.configured) return
  gitHeatLoading.value = true
  gitHeatError.value = ''
  try {
    const res = await getTeamProjectHeatmap(teamId.value, activeRepoId.value, { branch: gitConfig.repoBranch || '' })
    gitHeatmap.value = res.data || null
  } catch (e) {
    gitHeatmap.value = null
    gitHeatError.value = '热力图加载失败'
  } finally {
    gitHeatLoading.value = false
  }
}
async function loadBranches() {
  if (!activeRepoId.value || !teamId.value) return
  gitBranchLoading.value = true
  try {
    const res = await getTeamProjectBranches(teamId.value, activeRepoId.value)
    const d = res.data || {}
    gitBranches.value = d.branches || []
    if (!gitConfig.repoBranch && d.defaultBranch) {
      gitConfig.repoBranch = d.defaultBranch
    }
  } catch (e) {
    // 响应拦截器已统一提示
  } finally {
    gitBranchLoading.value = false
  }
}
function onBranchChange() {
  if (!gitConfig.configured) return
  gitCommits.value = []
  gitCommitPage.value = 1
  gitCommitNoMore.value = false
  loadCommits(1, true)
  loadHeatmap()
}

/* ===================== 分支下拉（点击 chip 展开） ===================== */
const branchMenuOpen = ref(false)
async function toggleBranchMenu() {
  branchMenuOpen.value = !branchMenuOpen.value
  // 首次打开若分支列表为空，自动拉一次
  if (branchMenuOpen.value && !gitBranches.value.length && !gitBranchLoading.value && gitConfig.configured) {
    await loadBranches()
  }
}
function pickBranch(b) {
  if (gitConfig.repoBranch === b) {
    branchMenuOpen.value = false
    return
  }
  gitConfig.repoBranch = b
  branchMenuOpen.value = false
  onBranchChange()
}
function onDocClick(e) {
  if (!branchMenuOpen.value) return
  if (!e.target.closest('.tg-branch-wrap')) {
    branchMenuOpen.value = false
  }
}
async function loadCommits(page, reset) {
  if (!activeRepoId.value || !teamId.value) return
  gitCommitLoading.value = true
  try {
    const res = await getTeamProjectCommits(teamId.value, activeRepoId.value, { page, branch: gitConfig.repoBranch || '' })
    const list = res.data || []
    if (reset) gitCommits.value = []
    gitCommits.value.push(...list)
    gitCommitPage.value = page
    gitCommitNoMore.value = list.length < 20
  } catch (e) { if (reset) gitCommits.value = [] } finally {
    gitCommitLoading.value = false
    nextTick(setupInfiniteScroll)
  }
}
function loadMoreCommits() {
  if (gitCommitLoading.value || gitCommitNoMore.value) return
  loadCommits(gitCommitPage.value + 1, false)
}

/* ===================== 触底自动加载（无限滚动） ===================== */
let commitObserver = null
function setupInfiniteScroll() {
  disconnectInfiniteScroll()
  // 没有更多或尚无数据时不监听
  if (gitCommitNoMore.value || !gitCommits.value.length) return
  const sentinel = document.getElementById('tg-commit-sentinel')
  if (!sentinel) return
  commitObserver = new IntersectionObserver((entries) => {
    if (entries[0] && entries[0].isIntersecting) {
      loadMoreCommits()
    }
  }, { root: null, rootMargin: '100px 0px' })
  commitObserver.observe(sentinel)
}
function disconnectInfiniteScroll() {
  if (commitObserver) {
    commitObserver.disconnect()
    commitObserver = null
  }
}
function formatGitDate(d) {
  if (!d) return ''
  return String(d).replace('T', ' ').substring(0, 19)
}
function goBack() {
  if (window.history.length > 1) router.back()
  else router.push({ path: '/portal/team' })
}

onMounted(async () => {
  document.addEventListener('click', onDocClick)
  await loadTeam()
  await loadRepos()
  await selectRepo(activeRepoId.value)
})
onUnmounted(() => {
  document.removeEventListener('click', onDocClick)
  disconnectInfiniteScroll()
})
