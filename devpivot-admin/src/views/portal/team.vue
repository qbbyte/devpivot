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
            <span class="tti-count">{{ t.memberCount || 0 }} 人</span>
            <span v-if="t.unreadCount > 0" class="tti-unread" :title="t.unreadCount + ' 条未读'">
              {{ t.unreadCount > 99 ? '99+' : t.unreadCount }}
            </span>
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
          <div class="td-ops">
            <el-button v-if="canManage" @click="openEdit">编辑</el-button>
            <el-button v-if="!isOwner" type="warning" plain @click="handleLeave">退出团队</el-button>
            <el-button v-if="isOwner" type="danger" plain @click="handleDissolve">解散团队</el-button>
          </div>
        </div>

        <el-tabs v-model="activeTab" class="td-tabs">
          <!-- 项目 -->
          <el-tab-pane label="项目" name="projects">
            <div class="tab-pane-inner">
              <div class="td-toolbar">
                <span class="td-tab-count">{{ projectTotal }} 个项目</span>
                <el-button v-if="canManage" type="primary" size="small" @click="openBindProject">
                  <el-icon><Plus /></el-icon>
                  <span>关联项目</span>
                </el-button>
              </div>
              <div class="tab-pane-body">
                <el-table v-if="projectRows.length" v-loading="loadingProjects" :data="projectRows" border stripe @row-dblclick="openProject">
                  <el-table-column label="项目名称" min-width="200">
                    <template #default="{ row }">
                      <span class="project-link" title="点击查看项目" @click="openProject(row)">{{ row.projectName }}</span>
                    </template>
                  </el-table-column>
                  <el-table-column label="当前阶段" width="120">
                    <template #default="{ row }">
                      <el-tag size="small">{{ stepLabel(row.step) }}</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column label="操作" width="150" align="center">
                    <template #default="{ row }">
                      <el-button type="primary" link size="small" @click="openArtifacts(row)">产物</el-button>
                      <el-button type="danger" link size="small" :disabled="!canManage" @click="unbindProject(row)">解绑</el-button>
                    </template>
                  </el-table-column>
                </el-table>
                <div v-else class="empty-fill">
                  <el-empty description="暂无关联项目" :image-size="70" />
                </div>
              </div>
              <el-pagination
                class="tab-pane-pager"
                layout="total, sizes, prev, pager, next, jumper"
                :total="projectTotal"
                :page-size="projectQuery.pageSize"
                :current-page="projectQuery.pageNum"
                :page-sizes="[10, 20, 50]"
                background
                @current-change="handleProjectPageChange"
                @size-change="handleProjectSizeChange"
              />
            </div>
          </el-tab-pane>

          <!-- 成员 -->
          <el-tab-pane label="成员" name="members">
            <div class="tab-pane-inner">
              <div class="td-toolbar">
                <span class="td-tab-count">{{ memberTotal }} 名成员</span>
                <el-button v-if="canManage" type="primary" size="small" @click="openAddMember">
                  <el-icon><Plus /></el-icon>
                  <span>添加成员</span>
                </el-button>
              </div>
              <div class="tab-pane-body">
                <el-table v-if="memberRows.length" v-loading="loadingMembers" :data="memberRows" border stripe>
                  <el-table-column label="成员" min-width="170">
                    <template #default="{ row }">
                      <div class="member-cell">
                        <el-avatar :size="30" :src="avatarUrl(row.avatar)">{{ row.nickName?.charAt(0) }}</el-avatar>
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
                <div v-else class="empty-fill">
                  <el-empty description="暂无成员" :image-size="70" />
                </div>
              </div>
              <el-pagination
                class="tab-pane-pager"
                layout="total, sizes, prev, pager, next, jumper"
                :total="memberTotal"
                :page-size="memberQuery.pageSize"
                :current-page="memberQuery.pageNum"
                :page-sizes="[10, 20, 50]"
                background
                @current-change="handleMemberPageChange"
                @size-change="handleMemberSizeChange"
              />
            </div>
          </el-tab-pane>

          <!-- 讨论 -->
          <el-tab-pane label="讨论" name="chat">
            <div class="chat-pane">
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
                  <el-avatar :size="34" class="chat-avatar" :src="avatarUrl(m.avatar)">{{ m.nickName?.charAt(0) }}</el-avatar>
                  <div class="chat-bubble-wrap">
                    <div class="chat-meta">
                      <span class="chat-name">{{ m.nickName }}</span>
                      <span class="chat-time">{{ m.time }}</span>
                    </div>
                    <div class="chat-bubble">{{ m.content }}</div>
                    <div v-if="m.readUsers && m.readUsers.length" class="chat-read">
                      已读 {{ m.readUsers.length }} 人：{{ m.readUsers.map(r => r.nickName).join('、') }}
                    </div>
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
          <el-avatar :size="28" :src="avatarUrl(u.avatar)">{{ u.nickName?.charAt(0) }}</el-avatar>
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

    <!-- 关联项目 -->
    <el-dialog v-model="bindDialogVisible" title="关联项目" width="460px">
      <el-select
        v-model="bindForm.projectId"
        filterable
        clearable
        placeholder="选择要关联的项目（已关联的将禁用）"
        style="width: 100%"
      >
        <el-option
          v-for="p in projectOptions"
          :key="p.projectId"
          :label="p.projectName"
          :value="p.projectId"
          :disabled="isProjectBound(p.projectId)"
        />
      </el-select>
      <template #footer>
        <el-button @click="bindDialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!bindForm.projectId" @click="submitBind">确定</el-button>
      </template>
    </el-dialog>

    <!-- 项目阶段概览（点击项目名弹出，展示所有阶段与实现人） -->
    <el-dialog
      v-model="projectDialogVisible"
      :title="dialogProject.projectName || '项目阶段概览'"
      width="620px"
      top="7vh"
      class="phase-dialog-el"
    >
      <div v-loading="loadingPhases" class="phase-dialog">
        <div class="pd-head">
          <div class="pd-head-row">
            <div class="pd-info">
              <span class="pd-label">当前阶段</span>
              <el-tag
                size="small"
                :type="dialogProject.step === 'DONE' ? 'success' : 'primary'"
                effect="light"
                round
              >{{ stepLabel(dialogProject.step) }}</el-tag>
            </div>
            <div class="pd-info">
              <span class="pd-label">项目负责人</span>
              <span class="pd-value">{{ dialogProject.assigneeName || '未指派' }}</span>
            </div>
          </div>

          <div class="pd-progress">
            <span class="pd-progress-label">阶段进度</span>
            <div class="pd-progress-track">
              <div
                class="pd-progress-bar"
                :style="{ width: phaseProgressPercent + '%', background: dialogProject.step === 'DONE' ? '#67c23a' : '#409eff' }"
              />
            </div>
            <span class="pd-progress-text" :class="{ done: dialogProject.step === 'DONE' }">
              {{ phaseProgressText }}
            </span>
          </div>
        </div>

        <div class="pd-timeline" :style="{ '--done-percent': phaseProgressPercent + '%' }">
          <div
            v-for="(p, idx) in dialogProject.phases"
            :key="p.step"
            class="pd-phase"
            :class="[p.status, { last: idx === dialogProject.phases.length - 1 }]"
            @click="gotoPhase(p.step)"
          >
            <div class="pd-phase-index">
              <el-icon v-if="p.status === 'done'"><Check /></el-icon>
              <span v-else>{{ idx + 1 }}</span>
            </div>
            <div class="pd-phase-body">
              <div class="pd-phase-top">
                <span class="pd-phase-name">{{ p.label }}</span>
                <el-tag
                  size="small"
                  :type="p.status === 'done' ? 'success' : p.status === 'current' ? 'warning' : 'info'"
                  effect="light"
                  round
                >{{ p.status === 'done' ? '已完成' : p.status === 'current' ? '进行中' : '未开始' }}</el-tag>
              </div>
              <div class="pd-phase-owner" :class="{ empty: !p.implementer }">
                <el-icon><User /></el-icon>
                <span>{{ p.implementer ? '实现人：' + phaseImplementerName(p.implementer) : '实现人未记录' }}</span>
              </div>
            </div>
            <el-icon class="pd-enter"><Right /></el-icon>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="projectDialogVisible = false">关闭</el-button>
        <el-button type="primary" :icon="Right" @click="gotoProject(dialogProject)">进入项目</el-button>
      </template>
    </el-dialog>

    <!-- 项目产物弹窗：列出各阶段产物并支持下载 -->
    <el-dialog
      v-model="artifactDialogVisible"
      :title="artifactDialog.projectName ? artifactDialog.projectName + ' · 项目产物' : '项目产物'"
      width="600px"
      top="8vh"
      class="artifact-dialog-el"
    >
      <div v-loading="loadingArtifacts" class="artifact-dialog">
        <div v-if="artifactDialog.artifacts.length" class="art-list">
          <div
            v-for="(a, idx) in artifactDialog.artifacts"
            :key="a.step"
            class="art-item"
            :class="{ 'art-empty': !a.hasData }"
          >
            <div class="art-item-head">
              <span class="art-index">{{ idx + 1 }}</span>
              <span class="art-name">{{ a.label }}</span>
              <el-tag size="small" effect="plain" round>{{ a.type === 'json' ? 'JSON' : 'Markdown' }}</el-tag>
            </div>
            <div class="art-item-body">
              <span v-if="a.hasData" class="art-tip">可下载为 {{ a.fileName }}</span>
              <span v-else class="art-tip empty">本阶段暂无产物</span>
              <div class="art-actions">
                <el-button
                  type="primary"
                  size="small"
                  plain
                  :disabled="!a.hasData"
                  :icon="Download"
                  @click="downloadArtifact(a)"
                >下载</el-button>
                <el-button
                  v-if="a.step === 'PROTO'"
                  type="success"
                  size="small"
                  plain
                  :disabled="!a.hasData"
                  :icon="Download"
                  @click="downloadProtoHtml(a)"
                >下载 HTML</el-button>
              </div>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无产物数据" :image-size="80" />
      </div>
      <template #footer>
        <el-button @click="artifactDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Opportunity, Plus, Search, Setting, User, SwitchButton, Check, Right, Download } from '@element-plus/icons-vue'
import useUserStore from '@/store/modules/user'
import defAva from '@/assets/images/profile.jpg'
import {
  listMyTeams, getTeamDetail, createTeam, updateTeam, dissolveTeam as dissolveTeamApi,
  addTeamMember, removeTeamMember, changeTeamMemberRole,
  bindTeamProject, unbindTeamProject, sendTeamMessage, markTeamRead, searchTeamUsers,
  listProjectOptions, leaveTeam,   listTeamMembers, listTeamProjects, getProjectPhases, getProjectArtifacts
} from '@/api/ai/team'
import { getProtoPages } from '@/api/ai/proto'
import { protoToHtml } from '@/utils/protoHtml'
import { subscribeTeam, unsubscribeTeam, disconnectWs } from '@/api/ai/teamWs'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const currentUserId = computed(() => userStore.id || userStore.userId)

/* ===================== 团队数据(真实接口 /team) ===================== */
const teams = ref([])          // 我的团队列表(摘要)
const activeTeam = ref(null)   // 当前选中团队详情(含 members/projects/messages)
const activeTab = ref('projects')

/* ===== 成员/项目分页状态(后端若依分页) ===== */
const memberQuery = reactive({ pageNum: 1, pageSize: 10 })
const memberRows = ref([])
const memberTotal = ref(0)
const loadingMembers = ref(false)
const projectQuery = reactive({ pageNum: 1, pageSize: 10 })
const projectRows = ref([])
const projectTotal = ref(0)
const loadingProjects = ref(false)

/* ===== 项目阶段概览弹窗 ===== */
const projectDialogVisible = ref(false)
const dialogProject = ref({ projectName: '', assigneeName: '', step: '', phases: [] })
const loadingPhases = ref(false)

// 阶段 → 项目门户路由段（DONE 无独立页，落到最后一个可用阶段 db）
const STEP_ROUTE = {
  REQ: 'req', CLARIFY: 'clarify', PRD: 'prd',
  PROTO: 'proto', TECH: 'tech', DB: 'db', DONE: 'db'
}
// 团队成员 userName → nickName 映射，用于把实现人登录名转成昵称
const userNameToNick = computed(() => {
  const map = {}
  ;(activeTeam.value?.members || []).forEach(m => { map[m.userName] = m.nickName })
  return map
})
function phaseImplementerName(username) {
  if (!username) return '—'
  return userNameToNick.value[username] || username
}
function openProject(row) {
  if (!row || !row.projectId) return
  projectDialogVisible.value = true
  loadingPhases.value = true
  dialogProject.value = { projectId: row.projectId, projectName: row.projectName, assigneeName: '', step: row.step, phases: [] }
  getProjectPhases(row.projectId).then(res => {
    const d = res.data || {}
    dialogProject.value = {
      projectId: row.projectId,
      projectName: d.projectName || row.projectName,
      assigneeName: d.assigneeName || '未指派',
      step: d.step,
      phases: d.phases || []
    }
  }).catch(() => {
    dialogProject.value.phases = []
  }).finally(() => {
    loadingPhases.value = false
  })
}
// 弹窗内：进入项目总览页（应用内跳转，不新开标签页）
function gotoProject(row) {
  if (!row || !row.projectId) return
  projectDialogVisible.value = false
  router.push({ path: '/portal/project/' + row.projectId })
}
// 弹窗内：进入某具体阶段（应用内跳转，不新开标签页）
function gotoPhase(step) {
  const id = dialogProject.value.projectId
  if (!id) return
  const seg = STEP_ROUTE[step] || 'clarify'
  projectDialogVisible.value = false
  router.push({ path: '/portal/project/' + id + '/' + seg })
}

/* ===================== 项目产物弹窗 ===================== */
const artifactDialogVisible = ref(false)
const loadingArtifacts = ref(false)
const artifactDialog = reactive({ projectId: null, projectName: '', artifacts: [] })

// 项目行「产物」按钮：打开项目产物弹窗
function openArtifacts(row) {
  if (!row || !row.projectId) return
  artifactDialogVisible.value = true
  loadingArtifacts.value = true
  artifactDialog.projectId = row.projectId
  artifactDialog.projectName = row.projectName || ''
  artifactDialog.artifacts = []
  getProjectArtifacts(row.projectId).then(res => {
    const d = res.data || {}
    artifactDialog.projectName = d.projectName || row.projectName || ''
    artifactDialog.artifacts = d.artifacts || []
  }).catch(() => {
    artifactDialog.artifacts = []
  }).finally(() => {
    loadingArtifacts.value = false
  })
}

// 前端按内容类型生成文件并触发下载（产物均为库内文本，无需后端落盘）
function downloadArtifact(a) {
  if (!a || !a.hasData) return
  const content = a.content || ''
  const mime = a.type === 'json' ? 'application/json' : 'text/markdown'
  const blob = new Blob([content], { type: mime + ';charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = a.fileName || 'artifact'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

// 原型设计额外支持导出为可离线打开的静态 HTML
async function downloadProtoHtml(a) {
  if (!a || !a.hasData || !artifactDialog.projectId) return
  const pages = await getProtoPages(artifactDialog.projectId)
  const html = protoToHtml(pages, artifactDialog.projectName || '原型设计')
  const blob = new Blob([html], { type: 'text/html;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = '原型设计.html'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

const phaseProgressPercent = computed(() => {
  const list = dialogProject.value.phases || []
  if (!list.length) return 0
  const done = list.filter(p => p.status === 'done').length
  return Math.round((done / list.length) * 100)
})
const phaseProgressText = computed(() => {
  const total = (dialogProject.value.phases || []).length
  const done = (dialogProject.value.phases || []).filter(p => p.status === 'done').length
  if (dialogProject.value.step === 'DONE') return `全部 ${total} 个阶段已完成`
  return `已完成 ${done} / ${total} 个阶段`
})

async function loadMembers() {
  if (!activeTeam.value) return
  loadingMembers.value = true
  try {
    const res = await listTeamMembers(activeTeam.value.teamId, { ...memberQuery })
    memberRows.value = res.rows || []
    memberTotal.value = res.total || 0
  } finally {
    loadingMembers.value = false
  }
}

async function loadProjects() {
  if (!activeTeam.value) return
  loadingProjects.value = true
  try {
    const res = await listTeamProjects(activeTeam.value.teamId, { ...projectQuery })
    projectRows.value = res.rows || []
    projectTotal.value = res.total || 0
  } finally {
    loadingProjects.value = false
  }
}

function handleMemberPageChange(page) {
  memberQuery.pageNum = page
  loadMembers()
}
function handleMemberSizeChange(size) {
  memberQuery.pageNum = 1
  memberQuery.pageSize = size
  loadMembers()
}
function handleProjectPageChange(page) {
  projectQuery.pageNum = page
  loadProjects()
}
function handleProjectSizeChange(size) {
  projectQuery.pageNum = 1
  projectQuery.pageSize = size
  loadProjects()
}

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

/** 拼接头像完整 URL
 *  - 空值 → 默认头像 defAva（与全局导航一致）
 *  - 已完整地址（默认头像 / 已带 /dev-api / /assets / /src / data: / http）→ 直接返回，避免二次拼接
 *  - 后端原始相对路径（如 /profile/avatar/xxx.png）→ 拼 /dev-api 前缀
 */
function avatarUrl(avatar) {
  if (!avatar) return defAva
  if (/^(https?:\/\/|\/dev-api\/|\/assets\/|\/src\/assets\/|\/src\/|data:)/.test(avatar)) return avatar
  return import.meta.env.VITE_APP_BASE_API + avatar
}

async function loadTeams() {
  const res = await listMyTeams()
  teams.value = res.data || []
  if (teams.value.length) {
    await selectTeam(teams.value[0])
  } else {
    activeTeam.value = null
  }
}

async function selectTeam(t) {
  if (activeTeam.value && activeTeam.value.teamId !== t.teamId) {
    unsubscribeTeam(activeTeam.value.teamId)
  }
  const res = await getTeamDetail(t.teamId)
  activeTeam.value = res.data
  activeTab.value = 'projects'
  // 切换团队后重置分页并加载成员/项目第一页
  memberQuery.pageNum = 1
  projectQuery.pageNum = 1
  await Promise.all([loadMembers(), loadProjects()])
  reinitSeen()
  await subscribeTeam(t.teamId, onWsMessage, onWsRead)
  scrollChatToBottom()
}

async function refreshDetail() {
  if (!activeTeam.value) return
  const res = await getTeamDetail(activeTeam.value.teamId)
  activeTeam.value = res.data
  reinitSeen()
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
async function submitTeam() {
  if (!teamForm.value.teamName.trim()) {
    ElMessage.warning('请输入团队名称')
    return
  }
  const payload = {
    teamName: teamForm.value.teamName.trim(),
    description: teamForm.value.description.trim()
  }
  if (teamForm.value.teamId == null) {
    const res = await createTeam(payload)
    const createdId = res.data && res.data.teamId
    ElMessage.success('创建成功')
    await loadTeams()
    if (createdId) await selectTeam({ teamId: createdId })
  } else {
    await updateTeam({ teamId: teamForm.value.teamId, ...payload })
    ElMessage.success('保存成功')
    await loadTeams()
  }
  teamDialogVisible.value = false
}
function handleDissolve() {
  ElMessageBox.confirm('解散后团队及成员关系将被清除，确定吗？', '解散团队', { type: 'warning' })
    .then(async () => {
      await dissolveTeamApi(activeTeam.value.teamId)
      ElMessage.success('已解散团队')
      await loadTeams()
    })
    .catch(() => {})
}

/* 成员管理 */
async function changeRole(row, val) {
  await changeTeamMemberRole(activeTeam.value.teamId, row.userId, val)
  ElMessage.success('角色已更新')
  await refreshDetail()
  await loadMembers()
}
async function removeMember(row) {
  await removeTeamMember(activeTeam.value.teamId, row.userId)
  ElMessage.success('已移除成员')
  await refreshDetail()
  await loadMembers()
}

/* 添加成员 */
const memberDialogVisible = ref(false)
const memberKeyword = ref('')
const filteredDirectory = ref([])

function openAddMember() {
  memberKeyword.value = ''
  memberDialogVisible.value = true
  loadDirectory('')
}
async function loadDirectory(k) {
  try {
    const res = await searchTeamUsers(k || '')
    filteredDirectory.value = res.data || []
  } catch (e) {
    filteredDirectory.value = []
  }
}
function filterDirectory() {
  loadDirectory(memberKeyword.value)
}
function isTeamMember(uid) {
  return activeTeam.value && activeTeam.value.members.some(m => m.userId === uid)
}
async function addMember(u) {
  if (isTeamMember(u.userId)) return
  await addTeamMember(activeTeam.value.teamId, u.userId, 'MEMBER')
  ElMessage.success(`已添加 ${u.nickName}`)
  memberDialogVisible.value = false
  await refreshDetail()
  await loadMembers()
}

/* 项目关联 */
async function unbindProject(row) {
  await unbindTeamProject(activeTeam.value.teamId, row.projectId)
  ElMessage.success('已解绑项目')
  await refreshDetail()
  await loadProjects()
}
async function openBindProject() {
  bindForm.value = { projectId: null }
  try {
    const res = await listProjectOptions()
    projectOptions.value = res.data || []
  } catch (e) {
    projectOptions.value = []
  }
  bindDialogVisible.value = true
}

/* 群聊 */
const chatDraft = ref('')
const chatListRef = ref(null)

function scrollChatToBottom() {
  nextTick(() => {
    const el = chatListRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

async function sendMessage() {
  const text = chatDraft.value.trim()
  if (!text || !activeTeam.value) return
  const res = await sendTeamMessage(activeTeam.value.teamId, text)
  const msg = res.data
  // sendMessage 后端未回传 nickName/avatar，用当前登录用户真实信息补全
  msg.nickName = userStore.nickName
  msg.avatar = userStore.avatar || ''
  if (!msg.readUsers) msg.readUsers = []
  activeTeam.value.messages.push(msg)
  // 本地发送的消息先入 seen，避免随后收到的服务端广播重复追加
  seenMsgIds.value.add(msg.msgId)
  chatDraft.value = ''
  scrollChatToBottom()
}

/* 角色派生：是否创建者(创建者不可退出,用解散) */
const isOwner = computed(() => activeTeam.value && activeTeam.value.myRole === 'OWNER')

/* 清除本地未读红点 */
function clearUnread(teamId) {
  const t = teams.value.find(x => x.teamId === teamId)
  if (t) t.unreadCount = 0
}

/* 退出团队(非创建者) */
async function handleLeave() {
  const tid = activeTeam.value && activeTeam.value.teamId
  if (!tid) return
  try {
    await ElMessageBox.confirm('确定退出该团队吗？退出后需重新被邀请才能加入。', '退出团队', { type: 'warning' })
  } catch (e) { return }
  await leaveTeam(tid)
  ElMessage.success('已退出团队')
  unsubscribeTeam(tid)
  await loadTeams()
}

/* 关联项目(选择器数据源 + 弹窗状态) */
const bindDialogVisible = ref(false)
const projectOptions = ref([])
const bindForm = ref({ projectId: null })
function isProjectBound(pid) {
  return activeTeam.value && activeTeam.value.projects.some(p => p.projectId === pid)
}
async function submitBind() {
  const pid = bindForm.value.projectId
  if (!pid || !activeTeam.value) return
  await bindTeamProject(activeTeam.value.teamId, pid)
  ElMessage.success('已关联项目')
  bindDialogVisible.value = false
  await refreshDetail()
  await loadProjects()
}

/* 讨论区实时刷新(WebSocket 推送) */
const seenMsgIds = ref(new Set())

/** 以当前激活团队已加载的消息重置去重集合，避免重订阅/刷新后重复追加 */
function reinitSeen() {
  seenMsgIds.value = new Set((activeTeam.value?.messages || []).map(m => m.msgId))
}

/** 收到新消息：仅处理/归属当前激活团队，按 msgId 去重增量追加 */
function onWsMessage(msg) {
  if (!msg || !msg.msgId) return
  if (!activeTeam.value || activeTeam.value.teamId !== msg.teamId) {
    bumpUnread(msg.teamId)
    return
  }
  if (seenMsgIds.value.has(msg.msgId)) return
  seenMsgIds.value.add(msg.msgId)
  activeTeam.value.messages.push(msg)
  if (activeTab.value === 'chat') {
    scrollChatToBottom()
    markTeamRead(msg.teamId, []).then(() => clearUnread(msg.teamId)).catch(() => {})
  } else {
    bumpUnread(msg.teamId)
  }
}

/** 收到已读事件：更新本地面板中对应消息的"已读 N 人" */
function onWsRead(ev) {
  if (!ev || !activeTeam.value || activeTeam.value.teamId !== ev.teamId) return
  const ids = ev.msgIds || []
  for (const m of (activeTeam.value.messages || [])) {
    if (ids.includes(m.msgId)) {
      if (!m.readUsers) m.readUsers = []
      if (!m.readUsers.some(r => r.userId === ev.readerUserId) && ev.readerUserId !== currentUserId.value) {
        m.readUsers.push({ userId: ev.readerUserId, nickName: ev.readerNickName })
      }
    }
  }
}

/** 非激活团队(或不在 chat 标签页)收到消息时，本地累加未读红点 */
function bumpUnread(teamId) {
  const t = teams.value.find(x => x.teamId === teamId)
  if (t) t.unreadCount = (t.unreadCount || 0) + 1
}

// 切换到讨论标签页时，标记已读并清除本地红点
watch(activeTab, (val) => {
  if (val === 'chat' && activeTeam.value) {
    markTeamRead(activeTeam.value.teamId, []).then(() => clearUnread(activeTeam.value.teamId)).catch(() => {})
  } else if (val === 'members') {
    // 切到成员 tab 时刷新当前分页数据
    loadMembers()
  } else if (val === 'projects') {
    loadProjects()
  }
})
onUnmounted(() => disconnectWs())

onMounted(async () => {
  if (!userStore.nickName) {
    try { await userStore.getInfo() } catch (e) { }
  }
  await loadTeams()
})
</script>

<style scoped>
.team-page {
  height: 100vh;
  overflow: hidden;
  background: #f5f6f9;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
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
  padding: 16px 24px 36px;
  min-height: 0;
  overflow: hidden;
  box-sizing: border-box;
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
  padding: 0;
  align-items: stretch;
  min-height: 0;
  box-sizing: border-box;
}
.tp-list {
  width: 260px;
  flex-shrink: 0;
  background: #fff;
  border-radius: 12px;
  padding: 14px;
  box-shadow: 0 2px 10px rgba(31, 45, 61, 0.05);
  height: calc(100vh - 112px);
  max-height: calc(100vh - 112px);
  overflow: auto;
  box-sizing: border-box;
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
.tti-unread {
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 8px;
  background: #f53f3f;
  color: #fff;
  font-size: 11px;
  line-height: 16px;
  text-align: center;
}
.tp-detail {
  flex: 1;
  background: #fff;
  border-radius: 12px;
  padding: 20px 22px;
  box-shadow: 0 2px 10px rgba(31, 45, 61, 0.05);
  min-height: 0;
  height: calc(100vh - 112px);
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
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

/* 标签页自适应高度：内容区撑满，单个 tab 超出时内部滚动 */
/* 注意：Element Plus 内部 DOM（.el-tabs__content / .el-tab-pane）在 <style scoped> 下
   不带当前组件的 data-v 属性，普通后代选择器匹配不到，必须用 :deep() 才能命中 */
.td-tabs {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.td-tabs :deep(.el-tabs__header) {
  flex-shrink: 0;
}
.td-tabs :deep(.el-tabs__content) {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.td-tabs :deep(.el-tab-pane) {
  flex: 1;
  min-height: 0;
  overflow: auto;
  box-sizing: border-box;
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

/* 项目/成员 tab 内容撑满 pane */
.project-link {
  color: #409eff;
  cursor: pointer;
  font-weight: 500;
}
.project-link:hover {
  text-decoration: underline;
}
.tab-pane-inner {
  height: 100%;
  display: flex;
  flex-direction: column;
}
.tab-pane-body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: auto;
}
.tab-pane-body > .el-table {
  flex: 1;
}
.empty-fill {
  flex: 1;
  min-height: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}
/* 分页条：固定在内容区底部，不随表格滚动 */
.tab-pane-pager {
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
  padding-top: 10px;
}

/* ===== 项目阶段概览弹窗 ===== */
.phase-dialog-el :deep(.el-dialog__body) {
  padding: 18px 22px 10px;
}
.phase-dialog-el :deep(.el-dialog__header) {
  margin-right: 0;
  padding: 16px 22px 14px;
  border-bottom: 1px solid #f0f1f3;
}
.phase-dialog-el :deep(.el-dialog__title) {
  font-weight: 600;
  font-size: 17px;
  color: #1f2329;
}
.phase-dialog-el :deep(.el-dialog__footer) {
  padding: 12px 22px 18px;
  border-top: 1px solid #f0f1f3;
}
.phase-dialog {
  min-height: 120px;
}
.pd-head {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding-bottom: 18px;
  border-bottom: 1px solid #f0f1f3;
  margin-bottom: 18px;
}
.pd-head-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}
.pd-info {
  display: flex;
  align-items: center;
  gap: 8px;
}
.pd-label {
  font-size: 12px;
  color: #909399;
}
.pd-value {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}
.pd-progress {
  display: flex;
  align-items: center;
  gap: 12px;
}
.pd-progress-label {
  flex-shrink: 0;
  font-size: 12px;
  color: #909399;
}
.pd-progress-track {
  flex: 1;
  height: 8px;
  background: #ebedf0;
  border-radius: 4px;
  overflow: hidden;
}
.pd-progress-bar {
  height: 100%;
  border-radius: 4px;
  transition: width 0.5s ease;
}
.pd-progress-text {
  flex-shrink: 0;
  font-size: 12px;
  color: #606266;
  white-space: nowrap;
}
.pd-progress-text.done {
  color: #67c23a;
  font-weight: 500;
}
.pd-timeline {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 0;
  padding-left: 0;
}
.pd-timeline::before {
  content: '';
  position: absolute;
  left: 14px;
  top: 18px;
  bottom: 18px;
  width: 2px;
  background: linear-gradient(180deg, #67c23a var(--done-percent, 0%), #ebedf0 var(--done-percent, 0%));
  border-radius: 1px;
  z-index: 0;
}
.pd-phase {
  position: relative;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 16px 12px 44px;
  margin-bottom: 10px;
  border: 1px solid #ebedf0;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.25s ease;
  background: #fff;
  box-shadow: 0 1px 3px rgba(31, 45, 61, 0.04);
}
.pd-phase:last-child,
.pd-phase.last {
  margin-bottom: 0;
}
.pd-phase:hover {
  border-color: #409eff;
  box-shadow: 0 6px 18px rgba(64, 158, 255, 0.12);
  transform: translateY(-2px);
}
.pd-phase.done:hover {
  border-color: #67c23a;
  box-shadow: 0 6px 18px rgba(103, 194, 58, 0.12);
}
.pd-phase.current {
  border-color: #e6a23c;
  background: linear-gradient(135deg, #fffbf5, #fdf6ec);
  box-shadow: 0 4px 12px rgba(230, 162, 60, 0.1);
}
.pd-phase.current:hover {
  border-color: #e6a23c;
  box-shadow: 0 6px 18px rgba(230, 162, 60, 0.14);
}
.pd-phase.current .pd-phase-index {
  background: #e6a23c;
  color: #fff;
  box-shadow: 0 0 0 4px rgba(230, 162, 60, 0.15);
}
.pd-phase.done .pd-phase-index {
  background: #67c23a;
  color: #fff;
  box-shadow: 0 0 0 4px rgba(103, 194, 58, 0.12);
}
.pd-phase.todo .pd-phase-index {
  background: #f2f3f5;
  color: #909399;
}
.pd-phase.todo {
  background: #fafbfc;
}
.pd-phase.todo .pd-phase-name {
  color: #8a9099;
}
.pd-phase-index {
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 30px;
  height: 30px;
  flex-shrink: 0;
  border-radius: 50%;
  background: #e5e7eb;
  color: #86909c;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  z-index: 1;
  transition: all 0.25s ease;
}
.pd-phase-body {
  flex: 1;
  min-width: 0;
}
.pd-phase-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}
.pd-phase-name {
  font-size: 14px;
  font-weight: 600;
  color: #1f2329;
}
.pd-phase-owner {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #646a73;
}
.pd-phase-owner.empty {
  color: #c0c4cc;
}
.pd-phase-owner .el-icon {
  color: #a0aab5;
}
.pd-enter {
  flex-shrink: 0;
  color: #c0c4cc;
  opacity: 0;
  transform: translateX(-4px);
  transition: all 0.25s ease;
}
.pd-phase:hover .pd-enter {
  opacity: 1;
  transform: translateX(2px);
  color: #409eff;
}
.pd-phase.done:hover .pd-enter {
  color: #67c23a;
}
.pd-phase.current:hover .pd-enter {
  color: #e6a23c;
}

/* ===== 项目产物弹窗 ===== */
.artifact-dialog-el :deep(.el-dialog__body) {
  padding: 18px 22px 12px;
}
.artifact-dialog {
  min-height: 120px;
}
.art-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.art-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 16px;
  border: 1px solid #ebedf0;
  border-radius: 10px;
  background: #fff;
  box-shadow: 0 1px 3px rgba(31, 45, 61, 0.04);
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}
.art-item:hover {
  border-color: #409eff;
  box-shadow: 0 4px 14px rgba(64, 158, 255, 0.1);
}
.art-item.art-empty {
  background: #fafbfc;
  border-style: dashed;
}
.art-item-head {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}
.art-index {
  width: 24px;
  height: 24px;
  flex-shrink: 0;
  border-radius: 50%;
  background: #ecf5ff;
  color: #409eff;
  font-size: 13px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}
.art-name {
  font-size: 14px;
  font-weight: 600;
  color: #1f2329;
}
.art-item.art-empty .art-name {
  color: #909399;
}
.art-item-body {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-shrink: 0;
  flex-wrap: wrap;
}
.art-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
.art-tip {
  font-size: 12px;
  color: #646a73;
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.art-tip.empty {
  color: #c0c4cc;
}

/* ===== 群聊 ===== */
.chat-pane {
  height: 100%;
  display: flex;
  flex-direction: column;
}
.chat-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 8px 4px 4px;
  display: flex;
  flex-direction: column;
  gap: 0;
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
.chat-read {
  margin-top: 4px;
  font-size: 11px;
  color: #a0aab5;
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
