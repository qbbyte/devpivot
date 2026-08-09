<template>
  <div class="team-page">
    <header class="portal-header">
      <div class="portal-header-inner">
        <div class="portal-brand">
          <div class="brand-logo">
            <el-icon :size="20"><Opportunity /></el-icon>
          </div>
          <span class="brand-name">AI 智能需求设计</span>
        </div>
        <nav class="portal-nav">
          <router-link to="/portal" class="pn-item" :class="{ active: route.path === '/portal' }">工作台</router-link>
          <router-link to="/portal/team" class="pn-item" :class="{ active: route.path.startsWith('/portal/team') }">我的团队</router-link>
          <router-link v-hasRole="['admin']" to="/index" class="pn-item pn-admin">
            <el-icon><Setting /></el-icon>
            <span>进入管理后台</span>
          </router-link>
        </nav>
      </div>
    </header>

    <main class="portal-main">
      <div class="tp-body">
      <!-- 左侧：团队列表 -->
      <aside class="tp-list">
        <div class="tp-list-head">
          <span>团队（{{ teams.length }}）</span>
          <el-button type="primary" size="small" @click="openCreate">
            <el-icon><Plus /></el-icon>
            <span>创建团队</span>
          </el-button>
        </div>
        <div
          v-for="t in teams"
          :key="t.teamId"
          class="tp-team-item"
          :class="{ active: activeTeam && activeTeam.teamId === t.teamId }"
          @click="selectTeam(t)"
        >
          <div class="tti-name">{{ t.teamName }}</div>
          <div class="tti-meta">
            <el-tag size="small" :type="t.myRole === 'OWNER' ? 'warning' : 'info'">
              {{ roleLabel(t.myRole) }}
            </el-tag>
            <span class="tti-count">{{ t.members.length }} 人</span>
          </div>
        </div>
        <el-empty v-if="!teams.length" description="还没有团队，点击右上角创建" :image-size="70" />
      </aside>

      <!-- 右侧：团队详情 -->
      <section v-if="activeTeam" class="tp-detail">
        <div class="td-head">
          <div class="td-head-info">
            <h2 class="td-name">{{ activeTeam.teamName }}</h2>
            <p class="td-desc">{{ activeTeam.description || '暂无简介' }}</p>
          </div>
          <div v-if="canManage" class="td-ops">
            <el-button @click="openEdit">编辑</el-button>
            <el-button type="danger" plain @click="dissolveTeam">解散团队</el-button>
          </div>
        </div>

        <el-tabs v-model="activeTab" class="td-tabs">
          <!-- 项目 -->
          <el-tab-pane label="项目" name="projects">
            <div class="td-toolbar">
              <span class="td-tab-count">{{ activeTeam.projects.length }} 个项目</span>
              <el-button v-if="canManage" type="primary" size="small" @click="openBindProject">
                <el-icon><Plus /></el-icon>
                <span>关联项目</span>
              </el-button>
            </div>
            <el-table :data="activeTeam.projects" border stripe>
              <el-table-column label="项目名称" prop="projectName" min-width="200" />
              <el-table-column label="当前阶段" width="120">
                <template #default="{ row }">
                  <el-tag size="small">{{ stepLabel(row.step) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="90" align="center">
                <template #default="{ row }">
                  <el-button type="danger" link size="small" :disabled="!canManage" @click="unbindProject(row)">解绑</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-if="!activeTeam.projects.length" description="暂无关联项目" :image-size="70" />
          </el-tab-pane>

          <!-- 成员 -->
          <el-tab-pane label="成员" name="members">
            <div class="td-toolbar">
              <span class="td-tab-count">{{ activeTeam.members.length }} 名成员</span>
              <el-button v-if="canManage" type="primary" size="small" @click="openAddMember">
                <el-icon><Plus /></el-icon>
                <span>添加成员</span>
              </el-button>
            </div>
            <el-table :data="activeTeam.members" border stripe>
              <el-table-column label="成员" min-width="170">
                <template #default="{ row }">
                  <div class="member-cell">
                    <el-avatar :size="30">{{ row.nickName.charAt(0) }}</el-avatar>
                    <div class="mc-text">
                      <div class="mc-name">{{ row.nickName }}</div>
                      <div class="mc-sub">@{{ row.userName }}</div>
                    </div>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="邮箱" prop="email" min-width="190" />
              <el-table-column label="职务" prop="title" min-width="120" />
              <el-table-column label="角色" width="150">
                <template #default="{ row }">
                  <el-select
                    :model-value="row.role"
                    size="small"
                    :disabled="!canManage || row.userId === currentUserId || row.role === 'OWNER'"
                    @change="(val) => changeRole(row, val)"
                  >
                    <el-option label="创建者" value="OWNER" />
                    <el-option label="管理员" value="ADMIN" />
                    <el-option label="成员" value="MEMBER" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="90" align="center">
                <template #default="{ row }">
                  <el-button
                    type="danger"
                    link
                    size="small"
                    :disabled="!canManage || row.userId === currentUserId || row.role === 'OWNER'"
                    @click="removeMember(row)"
                  >移除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <!-- 讨论 -->
          <el-tab-pane label="讨论" name="chat">
            <div class="td-toolbar">
              <span class="td-tab-count">{{ activeTeam.messages.length }} 条消息</span>
            </div>
            <div ref="chatListRef" class="chat-list">
              <div
                v-for="m in activeTeam.messages"
                :key="m.msgId"
                class="chat-item"
                :class="{ 'chat-mine': m.userId === currentUserId }"
              >
                <el-avatar :size="34" class="chat-avatar">{{ m.nickName.charAt(0) }}</el-avatar>
                <div class="chat-bubble-wrap">
                  <div class="chat-meta">
                    <span class="chat-name">{{ m.nickName }}</span>
                    <span class="chat-time">{{ m.time }}</span>
                  </div>
                  <div class="chat-bubble">{{ m.content }}</div>
                </div>
              </div>
              <el-empty v-if="!activeTeam.messages.length" description="还没有消息，来发第一条吧" :image-size="70" />
            </div>
            <div class="chat-input">
              <el-input
                v-model="chatDraft"
                type="textarea"
                :rows="2"
                resize="none"
                maxlength="500"
                placeholder="输入消息，按 Enter 发送 / Shift+Enter 换行"
                @keydown.enter.exact.prevent="sendMessage"
              />
              <el-button type="primary" :disabled="!chatDraft.trim()" @click="sendMessage">发送</el-button>
            </div>
          </el-tab-pane>
        </el-tabs>
      </section>

      <el-empty v-else description="选择一个团队查看详情，或创建新团队" class="tp-detail-empty" />
    </div>
    </main>

    <!-- 创建 / 编辑团队 -->
    <el-dialog v-model="teamDialogVisible" :title="teamDialogTitle" width="460px">
      <el-form :model="teamForm" label-width="80px">
        <el-form-item label="团队名称" required>
          <el-input v-model="teamForm.teamName" maxlength="30" placeholder="例如：平台研发组" />
        </el-form-item>
        <el-form-item label="团队简介">
          <el-input
            v-model="teamForm.description"
            type="textarea"
            :rows="3"
            maxlength="200"
            placeholder="选填，简要描述团队职责"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="teamDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitTeam">确定</el-button>
      </template>
    </el-dialog>

    <!-- 添加成员 -->
    <el-dialog v-model="memberDialogVisible" title="添加成员" width="480px">
      <el-input v-model="memberKeyword" placeholder="搜索昵称 / 用户名 / 邮箱" clearable @input="filterDirectory">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <div class="member-directory">
        <div
          v-for="u in filteredDirectory"
          :key="u.userId"
          class="md-item"
          :class="{ disabled: isTeamMember(u.userId) }"
          @click="!isTeamMember(u.userId) && addMember(u)"
        >
          <el-avatar :size="28">{{ u.nickName.charAt(0) }}</el-avatar>
          <div class="md-info">
            <div class="md-name">{{ u.nickName }}</div>
            <div class="md-sub">@{{ u.userName }} · {{ u.email }}</div>
          </div>
          <el-tag v-if="isTeamMember(u.userId)" size="small" type="success">已在团队</el-tag>
          <el-icon v-else class="md-add"><Plus /></el-icon>
        </div>
        <el-empty v-if="!filteredDirectory.length" description="无匹配用户" :image-size="60" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Opportunity, Plus, Search, Setting, User, SwitchButton } from '@element-plus/icons-vue'
import useUserStore from '@/store/modules/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const currentUserId = computed(() => userStore.id || userStore.userId || 1)

/* ===================== 模拟数据（后续替换为 /ai/team 真实接口） ===================== */
// 平台用户目录：添加成员时从中检索
const userDirectory = [
  { userId: 1, nickName: '张管理员', userName: 'admin', email: 'admin@devpivot.com', title: '技术负责人' },
  { userId: 2, nickName: '李开发', userName: 'devli', email: 'li@devpivot.com', title: '后端工程师' },
  { userId: 3, nickName: '王设计', userName: 'wang', email: 'wang@devpivot.com', title: 'UI 设计师' },
  { userId: 4, nickName: '赵测试', userName: 'zhao', email: 'zhao@devpivot.com', title: '测试工程师' },
  { userId: 5, nickName: '钱产品', userName: 'qian', email: 'qian@devpivot.com', title: '产品经理' },
  { userId: 6, nickName: '孙运维', userName: 'sun', email: 'sun@devpivot.com', title: '运维工程师' }
]

const teams = ref([
  {
    teamId: 1,
    teamName: '平台研发组',
    description: '负责 devPivot 平台核心功能开发',
    ownerId: 1,
    myRole: 'OWNER',
    members: [
      { userId: 1, nickName: '张管理员', userName: 'admin', role: 'OWNER', email: 'admin@devpivot.com', title: '技术负责人' },
      { userId: 2, nickName: '李开发', userName: 'devli', role: 'ADMIN', email: 'li@devpivot.com', title: '后端工程师' },
      { userId: 3, nickName: '王设计', userName: 'wang', role: 'MEMBER', email: 'wang@devpivot.com', title: 'UI 设计师' }
    ],
    projects: [
      { projectId: 101, projectName: '智能客服系统', step: 'DB' },
      { projectId: 102, projectName: '数据中台', step: 'PRD' }
    ],
    messages: [
      { msgId: 1, userId: 2, nickName: '李开发', content: '智能客服系统这版 DB 设计我提交了一版，大家有空帮忙看下索引', time: '09:32' },
      { msgId: 2, userId: 3, nickName: '王设计', content: '收到，原型那边我同步更新了对话流', time: '09:40' }
    ]
  },
  {
    teamId: 2,
    teamName: '创新孵化小组',
    description: '探索 AI 辅助研发的新场景',
    ownerId: 5,
    myRole: 'MEMBER',
    members: [
      { userId: 5, nickName: '钱产品', userName: 'qian', role: 'OWNER', email: 'qian@devpivot.com', title: '产品经理' },
      { userId: 4, nickName: '赵测试', userName: 'zhao', role: 'MEMBER', email: 'zhao@devpivot.com', title: '测试工程师' },
      { userId: 1, nickName: '张管理员', userName: 'admin', role: 'MEMBER', email: 'admin@devpivot.com', title: '技术负责人' }
    ],
    projects: [
      { projectId: 103, projectName: 'AI 绘画工具', step: 'PROTO' }
    ],
    messages: [
      { msgId: 1, userId: 5, nickName: '钱产品', content: 'AI 绘画工具进入原型阶段了，本周先打磨生图参数面板', time: '14:05' }
    ]
  }
])
/* ====================================================================================== */

const activeTeam = ref(null)
const activeTab = ref('projects')

const canManage = computed(() =>
  activeTeam.value && (activeTeam.value.myRole === 'OWNER' || activeTeam.value.myRole === 'ADMIN')
)

const ROLE_LABELS = { OWNER: '我创建', ADMIN: '管理员', MEMBER: '我加入' }
const STEP_LABELS = {
  REQ: '需求采集', CLARIFY: 'AI 澄清', PRD: 'PRD 文档',
  PROTO: '原型设计', TECH: '技术方案', DB: '数据库设计', DONE: '已完成'
}
function roleLabel(r) { return ROLE_LABELS[r] || r }
function stepLabel(s) { return STEP_LABELS[s] || s }

function selectTeam(t) {
  activeTeam.value = t
  activeTab.value = 'projects'
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

/* 创建 / 编辑团队 */
const teamDialogVisible = ref(false)
const teamDialogTitle = ref('创建团队')
const teamForm = ref({ teamId: null, teamName: '', description: '' })

function openCreate() {
  teamDialogTitle.value = '创建团队'
  teamForm.value = { teamId: null, teamName: '', description: '' }
  teamDialogVisible.value = true
}
function openEdit() {
  teamDialogTitle.value = '编辑团队'
  teamForm.value = {
    teamId: activeTeam.value.teamId,
    teamName: activeTeam.value.teamName,
    description: activeTeam.value.description || ''
  }
  teamDialogVisible.value = true
}
function submitTeam() {
  if (!teamForm.value.teamName.trim()) {
    ElMessage.warning('请输入团队名称')
    return
  }
  if (teamForm.value.teamId == null) {
    const newTeam = {
      teamId: Date.now(),
      teamName: teamForm.value.teamName.trim(),
      description: teamForm.value.description.trim(),
      ownerId: currentUserId.value,
      myRole: 'OWNER',
      members: [{
        userId: currentUserId.value,
        nickName: userStore.nickName || '我',
        userName: userStore.userName || 'me',
        role: 'OWNER',
        email: userStore.email || ''
      }],
      projects: []
    }
    teams.value.unshift(newTeam)
    activeTeam.value = newTeam
  } else {
    const t = teams.value.find(x => x.teamId === teamForm.value.teamId)
    if (t) {
      t.teamName = teamForm.value.teamName.trim()
      t.description = teamForm.value.description.trim()
    }
  }
  teamDialogVisible.value = false
  ElMessage.success('保存成功')
}
function dissolveTeam() {
  ElMessageBox.confirm('解散后团队及成员关系将被清除，确定吗？', '解散团队', { type: 'warning' })
    .then(() => {
      teams.value = teams.value.filter(x => x.teamId !== activeTeam.value.teamId)
      activeTeam.value = teams.value[0] || null
      ElMessage.success('已解散团队')
    })
    .catch(() => {})
}

/* 成员管理 */
function changeRole(row, val) {
  row.role = val
  ElMessage.success('角色已更新')
}
function removeMember(row) {
  activeTeam.value.members = activeTeam.value.members.filter(m => m.userId !== row.userId)
  ElMessage.success('已移除成员')
}

/* 添加成员 */
const memberDialogVisible = ref(false)
const memberKeyword = ref('')
const filteredDirectory = ref([...userDirectory])

function openAddMember() {
  memberKeyword.value = ''
  filteredDirectory.value = userDirectory.slice()
  memberDialogVisible.value = true
}
function filterDirectory() {
  const k = memberKeyword.value.trim().toLowerCase()
  filteredDirectory.value = userDirectory.filter(u =>
    !k ||
    u.nickName.toLowerCase().includes(k) ||
    u.userName.toLowerCase().includes(k) ||
    u.email.toLowerCase().includes(k)
  )
}
function isTeamMember(uid) {
  return activeTeam.value && activeTeam.value.members.some(m => m.userId === uid)
}
function addMember(u) {
  if (isTeamMember(u.userId)) return
  activeTeam.value.members.push({ ...u, role: 'MEMBER' })
  ElMessage.success(`已添加 ${u.nickName}`)
}

/* 项目关联（演示） */
function unbindProject(row) {
  activeTeam.value.projects = activeTeam.value.projects.filter(p => p.projectId !== row.projectId)
  ElMessage.success('已解绑项目')
}
function openBindProject() {
  ElMessage.info('演示环境：关联项目将在接入真实接口后开放')
}

/* 群聊（演示：消息存内存，刷新即重置） */
const chatDraft = ref('')
const chatListRef = ref(null)
let chatSeq = 1000

function nowTime() {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  return `${p(d.getHours())}:${p(d.getMinutes())}`
}

function scrollChatToBottom() {
  nextTick(() => {
    const el = chatListRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

function sendMessage() {
  const text = chatDraft.value.trim()
  if (!text || !activeTeam.value) return
  activeTeam.value.messages.push({
    msgId: ++chatSeq,
    userId: currentUserId.value,
    nickName: userStore.nickName || '我',
    content: text,
    time: nowTime()
  })
  chatDraft.value = ''
  scrollChatToBottom()
}

onMounted(() => {
  if (!userStore.nickName) userStore.getInfo().catch(() => {})
  if (teams.value.length) activeTeam.value = teams.value[0]
})
</script>

<style scoped>
.team-page {
  min-height: 100vh;
  background: #f5f6f9;
  display: flex;
  flex-direction: column;
}
/* ===== 顶部品牌导航条（与工作台首页一致） ===== */
.portal-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid #eef0f3;
}
.portal-header-inner {
  max-width: 1440px;
  margin: 0 auto;
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
.portal-nav {
  display: flex;
  align-items: center;
  gap: 6px;
}
.pn-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border-radius: 8px;
  color: #4e5969;
  font-size: 14px;
  text-decoration: none;
  transition: background 0.18s, color 0.18s;
}
.pn-item:hover {
  background: #f2f5f9;
  color: #1d2129;
}
.pn-item.active {
  color: #3370ff;
  font-weight: 600;
}
.pn-item.active:hover {
  background: transparent;
}
.pn-admin {
  color: #86909c;
  margin-left: 4px;
}

/* ===== Hero 问候区（与工作台首页一致） ===== */
.portal-main {
  flex: 1;
  width: 100%;
  max-width: 1440px;
  margin: 0 auto;
  padding: 24px 24px 36px;
}
.tp-list-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
  font-weight: 600;
  color: #1d2129;
  margin-bottom: 10px;
}
.tp-body {
  flex: 1;
  display: flex;
  gap: 16px;
  padding: 24px 0 0;
  align-items: flex-start;
}
.tp-list {
  width: 260px;
  flex-shrink: 0;
  background: #fff;
  border-radius: 12px;
  padding: 14px;
  box-shadow: 0 2px 10px rgba(31, 45, 61, 0.05);
  max-height: calc(100vh - 120px);
  overflow: auto;
}
.tp-team-item {
  padding: 12px;
  border-radius: 10px;
  cursor: pointer;
  margin-bottom: 8px;
  border: 1px solid transparent;
  transition: all 0.15s;
}
.tp-team-item:hover {
  background: #f5f9ff;
}
.tp-team-item.active {
  background: #ecf5ff;
  border-color: #b3d8ff;
}
.tti-name {
  font-weight: 600;
  font-size: 14px;
  color: #1f2d3d;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.tti-meta {
  display: flex;
  align-items: center;
  gap: 10px;
}
.tti-count {
  font-size: 12px;
  color: #8a96a3;
}
.tp-detail {
  flex: 1;
  background: #fff;
  border-radius: 12px;
  padding: 20px 22px;
  box-shadow: 0 2px 10px rgba(31, 45, 61, 0.05);
  min-height: calc(100vh - 120px);
}
.tp-detail-empty {
  flex: 1;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(31, 45, 61, 0.05);
  display: flex;
  align-items: center;
  justify-content: center;
}
.td-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 8px;
}
.td-name {
  margin: 0 0 8px;
  font-size: 26px;
  color: #1f2d3d;
}
.td-desc {
  margin: 0;
  color: #8a96a3;
  font-size: 15px;
}
.td-ops {
  display: flex;
  gap: 10px;
}
.td-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 6px 0 12px;
}
.td-tab-count {
  font-size: 13px;
  color: #8a96a3;
}
.member-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}
.mc-name {
  font-weight: 500;
  font-size: 13px;
  color: #1f2d3d;
}
.mc-sub {
  font-size: 12px;
  color: #a0aab5;
}
.member-directory {
  margin-top: 14px;
  max-height: 340px;
  overflow: auto;
}
.md-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s;
}
.md-item:hover:not(.disabled) {
  background: #f5f9ff;
}
.md-item.disabled {
  cursor: not-allowed;
  opacity: 0.6;
}
.md-info {
  flex: 1;
  min-width: 0;
}
.md-name {
  font-size: 13px;
  font-weight: 500;
  color: #1f2d3d;
}
.md-sub {
  font-size: 12px;
  color: #a0aab5;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.md-add {
  color: #409eff;
  font-size: 18px;
}

/* ===== 群聊 ===== */
.chat-list {
  height: calc(100vh - 360px);
  min-height: 280px;
  overflow-y: auto;
  padding: 8px 4px 4px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.chat-item {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}
.chat-item.chat-mine {
  flex-direction: row-reverse;
}
.chat-avatar {
  flex-shrink: 0;
  background: linear-gradient(135deg, #3370ff, #6e52ff);
  color: #fff;
  font-size: 14px;
}
.chat-bubble-wrap {
  max-width: 70%;
  display: flex;
  flex-direction: column;
}
.chat-mine .chat-bubble-wrap {
  align-items: flex-end;
}
.chat-meta {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 4px;
}
.chat-name {
  font-size: 13px;
  font-weight: 600;
  color: #1f2d3d;
}
.chat-time {
  font-size: 11px;
  color: #a0aab5;
}
.chat-bubble {
  display: inline-block;
  padding: 9px 13px;
  border-radius: 10px;
  background: #f2f5f9;
  color: #1f2d3d;
  font-size: 14px;
  line-height: 1.55;
  word-break: break-word;
  white-space: pre-wrap;
}
.chat-mine .chat-bubble {
  background: linear-gradient(135deg, #3370ff, #5b8bff);
  color: #fff;
}
.chat-input {
  display: flex;
  gap: 10px;
  align-items: flex-end;
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid #eef0f3;
}
.chat-input .el-button {
  flex-shrink: 0;
  height: 54px;
}
</style>
