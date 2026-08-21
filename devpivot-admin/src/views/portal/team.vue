<template>
  <div class="team-page">
    <PortalHeader />

    <main class="portal-main">
      <div class="tp-body">
      <!-- 左侧：团队列表 -->
      <aside class="tp-list">
        <div class="tp-list-head">
          <span>团队（{{ teams.length }}）</span>
          <el-dropdown trigger="click" @command="onHeadAction">
            <el-button circle size="small" type="primary" aria-label="团队操作">
              <el-icon><Plus /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="create">
                  <el-icon style="margin-right: 8px"><Plus /></el-icon>创建团队
                </el-dropdown-item>
                <el-dropdown-item command="join">
                  <el-icon style="margin-right: 8px"><Right /></el-icon>加入团队
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
        <div
          v-for="t in teams"
          :key="t.teamId"
          class="tp-team-item"
          :class="{ active: activeTeam && activeTeam.teamId === t.teamId }"
          @click="selectTeam(t)"
        >
          <div class="tti-name-row">
            <span class="tti-name" :title="t.teamName">{{ t.teamName }}</span>
            <span v-if="t.unreadCount > 0" class="tti-unread" :title="t.unreadCount + ' 条未读'">
              {{ t.unreadCount > 99 ? '99+' : t.unreadCount }}
            </span>
          </div>
          <div class="tti-meta">
            <span class="tti-role" :class="{ 'is-owner': t.myRole === 'OWNER' }">{{ roleLabel(t.myRole) }}</span>
            <span class="tti-count">{{ t.memberCount || 0 }} 人</span>
          </div>
        </div>
        <el-empty v-if="!teams.length" description="还没有团队，点击右上角创建" :image-size="70" />
      </aside>

      <!-- 右侧：团队详情 -->
      <section v-if="activeTeam" class="tp-detail">
        <div class="td-banner">
          <div class="td-banner-content">
            <div class="td-banner-info">
              <div class="td-banner-title-row">
                <h2 class="td-name">{{ activeTeam.teamName }}</h2>
                <el-tag size="small" effect="light" round :type="activeTeam.myRole === 'OWNER' ? 'warning' : 'info'">
                  {{ roleLabel(activeTeam.myRole) }}
                </el-tag>
                <el-tag size="small" effect="plain" round :type="activeTeam.status === '0' ? 'success' : 'info'">
                  {{ activeTeam.status === '0' ? '正常' : '已解散' }}
                </el-tag>
              </div>
              <p class="td-desc">{{ activeTeam.description || '暂无简介' }}</p>
            </div>
            <div class="td-ops">
              <el-button v-if="canManage" size="small" @click="openEdit">编辑</el-button>
              <el-button v-if="!isOwner" size="small" type="warning" plain @click="handleLeave">退出团队</el-button>
              <el-button v-if="isOwner" size="small" type="danger" plain @click="handleDissolve">解散团队</el-button>
            </div>
          </div>
          <div class="td-banner-stats">
            <div class="td-stat" title="查看成员" @click="activeTab = 'members'">
              <span class="td-stat-num">{{ memberTotal }}</span>
              <span class="td-stat-label">成员</span>
            </div>
            <div class="td-stat" title="查看项目" @click="activeTab = 'projects'">
              <span class="td-stat-num">{{ projectTotal }}</span>
              <span class="td-stat-label">项目</span>
            </div>
            <div class="td-stat" title="查看讨论" @click="activeTab = 'chat'">
              <span class="td-stat-num">{{ unreadTotal }}</span>
              <span class="td-stat-label">未读消息</span>
            </div>
          </div>
        </div>
        <el-tabs v-model="activeTab" class="td-tabs">
          <!-- 团队信息 -->
          <el-tab-pane label="团队信息" name="info">
            <div class="tab-pane-inner">
              <div class="td-invite-card">
                <div class="td-invite-card-head">
                  <span class="td-invite-title">邀请码</span>
                  <span class="td-invite-tip">把邀请码发给同事，对方在左侧「加入团队」处输入即可加入</span>
                </div>
                <div class="td-invite-row">
                  <code class="td-invite-code">{{ activeTeam.inviteCode || '—' }}</code>
                  <el-button size="small" @click="copyInviteCode">复制</el-button>
                  <el-button v-if="canManage" size="small" type="primary" plain @click="handleRefreshCode">重新生成</el-button>
                </div>
              </div>

              <el-descriptions :column="1" border class="td-info">
                <el-descriptions-item label="团队ID">{{ activeTeam.teamId }}</el-descriptions-item>
                <el-descriptions-item label="团队名称">{{ activeTeam.teamName }}</el-descriptions-item>
                <el-descriptions-item label="简介">{{ activeTeam.description || '暂无简介' }}</el-descriptions-item>
                <el-descriptions-item label="创建者ID">{{ activeTeam.ownerId }}</el-descriptions-item>
                <el-descriptions-item label="状态">
                  <el-tag size="small" :type="activeTeam.status === '0' ? 'success' : 'info'">
                    {{ activeTeam.status === '0' ? '正常' : '已解散' }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="创建时间">{{ activeTeam.createTime || '—' }}</el-descriptions-item>
              </el-descriptions>
            </div>
          </el-tab-pane>
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
                  <el-table-column
                    type="index"
                    label="序号"
                    width="64"
                    align="center"
                    :index="(projectQuery.pageNum - 1) * projectQuery.pageSize + 1"
                  />
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
                  <el-table-column label="Git仓库" min-width="140">
                    <template #default="{ row }">
                      <span
                        v-if="repoRows(row).length"
                        class="git-repo-link"
                        title="点击进入 Git 统计"
                        @click.stop="gotoGit(row)"
                      >
                        <el-icon class="git-repo-icon"><Connection /></el-icon>
                        <span>Git仓库</span>
                        <span class="git-repo-count">×{{ repoRows(row).length }}</span>
                      </span>
                      <span v-else class="git-repo-empty">还没有绑定Git仓库</span>
                    </template>
                  </el-table-column>
                  <el-table-column label="操作" width="110" align="center">
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
                  <el-table-column
                    type="index"
                    label="序号"
                    width="64"
                    align="center"
                    :index="(memberQuery.pageNum - 1) * memberQuery.pageSize + 1"
                  />
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
                <el-button class="td-bell" link type="primary" size="small" @click="enableDesktopNotify">
                  <el-icon><Bell /></el-icon>
                  <span>开启桌面通知</span>
                </el-button>
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

    <!-- 加入团队(凭邀请码) -->
    <el-dialog v-model="joinDialogVisible" title="加入团队" width="460px">
      <p class="join-tip">输入团队邀请码（在团队「团队信息」页签中可复制），即可加入该团队。</p>
      <el-input
        v-model="joinCode"
        placeholder="请输入邀请码，如 A1B2C3D4"
        maxlength="20"
        clearable
        @keyup.enter="handleJoin"
      >
        <template #prefix><el-icon><Right /></el-icon></template>
      </el-input>
      <template #footer>
        <el-button @click="joinDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="joining" @click="handleJoin">加入</el-button>
      </template>
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
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import { Plus, Search, User, SwitchButton, Check, Right, Download, Connection } from '@element-plus/icons-vue'
import useUserStore from '@/store/modules/user'
import defAva from '@/assets/images/profile.jpg'
import PortalHeader from './components/PortalHeader.vue'
import {
  listMyTeams, getTeamDetail, createTeam, updateTeam, dissolveTeam as dissolveTeamApi,
  addTeamMember, removeTeamMember, changeTeamMemberRole,
  bindTeamProject, unbindTeamProject, sendTeamMessage, markTeamRead, searchTeamUsers,
  listProjectOptions, leaveTeam,   listTeamMembers, listTeamProjects, getProjectPhases, getProjectArtifacts,
  joinTeamByCode, refreshInviteCode
} from '@/api/ai/team'
import { getProtoPages } from '@/api/ai/proto'
import { protoToHtml } from '@/utils/protoHtml'
import { listTeamProjectRepos } from '@/api/ai/teamGit'
import { subscribeTeam, unsubscribeTeam, disconnectWs, setOnReconnect } from '@/api/ai/teamWs'

const router = useRouter()
const userStore = useUserStore()
const currentUserId = computed(() => userStore.id || userStore.userId)

/* ===================== 团队数据(真实接口 /team) ===================== */
const teams = ref([])          // 我的团队列表(摘要)
const activeTeam = ref(null)   // 当前选中团队详情(含 members/projects/messages)
const activeTab = ref('projects')

/* ===== 加入团队(凭邀请码) ===== */
const joinDialogVisible = ref(false)
const joinCode = ref('')
const joining = ref(false)

function openJoin() {
  joinCode.value = ''
  joinDialogVisible.value = true
}
function onHeadAction(command) {
  if (command === 'create') openCreate()
  else if (command === 'join') openJoin()
}
async function handleJoin() {
  const code = (joinCode.value || '').trim().toUpperCase()
  if (!code) {
    ElMessage.warning('请输入邀请码')
    return
  }
  joining.value = true
  try {
    await joinTeamByCode(code)
    ElMessage.success('已加入团队')
    joinDialogVisible.value = false
    await loadTeams()
  } catch (e) {
    // 错误已由响应拦截器统一提示
  } finally {
    joining.value = false
  }
}
async function copyInviteCode() {
  const code = activeTeam.value && activeTeam.value.inviteCode
  if (!code) return
  try {
    await navigator.clipboard.writeText(code)
    ElMessage.success('邀请码已复制')
  } catch (e) {
    ElMessage.warning('复制失败，请手动复制：' + code)
  }
}
async function handleRefreshCode() {
  if (!activeTeam.value) return
  try {
    await refreshInviteCode(activeTeam.value.teamId)
    ElMessage.success('已重新生成邀请码')
    await refreshDetail()
  } catch (e) { if (import.meta.env.DEV) console.warn('重新生成邀请码失败', e) }
}

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
// 进入团队项目 Git 提交统计独立子页面
function gotoGit(row) {
  if (!row || !row.projectId || !activeTeam.value) return
  router.push({
    path: '/portal/team/' + activeTeam.value.teamId + '/project/' + row.projectId + '/git',
    query: { projectName: row.projectName || '' }
  })
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
    const rows = res.rows || []
    projectTotal.value = res.total || 0
    // 聚合每个项目关联的 Git 仓库(多仓库表 ai_team_project_repo，缺省时后端列表行会带旧单仓库字段)
    const enriched = await Promise.all(rows.map(async (r) => {
      try {
        const repoRes = await listTeamProjectRepos(activeTeam.value.teamId, r.projectId)
        r.repos = repoRes.data || []
      } catch (e) {
        r.repos = []
      }
      return r
    }))
    projectRows.value = enriched
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

/* 项目行 → 是否已绑定 Git 仓库：优先多仓库表 repos，缺省回退 ai_team_project 旧单仓库字段 */
function repoRows(row) {
  if (row.repos && row.repos.length) return row.repos
  if (row.repoFullName) return [{ repoFullName: row.repoFullName, platform: row.repoPlatform, repoBranch: row.repoBranch }]
  return []
}

/* 当前团队未读消息数：以左侧列表的 DB 权威未读数为基准（新消息 notifyIfNeeded→bumpUnread +1，进入讨论区 markRead→clearUnread 清零） */
const unreadTotal = computed(() => {
  const t = teams.value.find(x => activeTeam.value && x.teamId === activeTeam.value.teamId)
  return t ? (t.unreadCount || 0) : 0
})

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
    stopMsgPoll()
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
  startMsgPoll() // 全局轮询兜底：团队激活即运行，不依赖 WS 是否连通
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
    // 兜底：等待多帧渲染完成（图片/复杂 DOM 回流可能跨多帧）
    requestAnimationFrame(() => {
      const el2 = chatListRef.value
      if (el2) el2.scrollTop = el2.scrollHeight
    })
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

/* 讨论区实时刷新(WebSocket 推送 + 全局轮询) */
const seenMsgIds = ref(new Set())
// 已弹过提示的消息 id 集合：WS 推送与全局轮询共用，避免同一条消息重复弹 Toast/通知/提示音
const notifiedMsgIds = ref(new Set())

/** 以当前激活团队已加载的消息重置去重集合，避免重订阅/刷新后重复追加或重复提示 */
function reinitSeen() {
  const ids = (activeTeam.value?.messages || []).map(m => m.msgId)
  seenMsgIds.value = new Set(ids)
  notifiedMsgIds.value = new Set(ids)
}

/** 收到新消息：仅处理/归属当前激活团队，按 msgId 去重增量追加 */
function onWsMessage(msg) {
  if (!msg || !msg.msgId) return
  // 同团队
  if (activeTeam.value && activeTeam.value.teamId === msg.teamId) {
    if (seenMsgIds.value.has(msg.msgId)) return
    seenMsgIds.value.add(msg.msgId)
    activeTeam.value.messages.push(msg)
    if (activeTab.value === 'chat') {
      scrollChatToBottom()
      markTeamRead(msg.teamId, []).then(() => clearUnread(msg.teamId)).catch(() => {})
    } else {
      // 同团队但不在讨论 tab：红点 + 提示(统一入口，WS 与轮询共用去重)
      notifyIfNeeded(msg)
    }
    return
  }
  // 非激活团队：红点 + 提示
  notifyIfNeeded(msg)
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

/**
 * 从 DB 重新同步当前激活团队的消息列表（覆盖式）。
 * 用途：补偿 WebSocket 断连期间被内存 broker 丢弃的推送，使接收方无需手动刷新整页即可看到新内容。
 * 以服务端为权威源，按 msgId 去重，避免与已落地的 WS 推送重复。
 */
async function refreshActiveTeamMessages() {
  if (!activeTeam.value) return
  try {
    const res = await getTeamDetail(activeTeam.value.teamId)
    const incoming = (res.data && res.data.messages) || []
    const map = new Map((activeTeam.value.messages || []).map(m => [m.msgId, m]))
    const addedMsgs = []
    for (const m of incoming) {
      if (!map.has(m.msgId)) { map.set(m.msgId, m); addedMsgs.push(m) }
    }
    const merged = Array.from(map.values()).sort((a, b) => (a.msgId || 0) - (b.msgId || 0))
    activeTeam.value.messages = merged
    // 先对新增消息弹提示（在 reinitSeen 重置 notifiedMsgIds 之前，避免被误判为已通知）
    for (const m of addedMsgs) notifyIfNeeded(m)
    reinitSeen()
    // 未读红点不在此重算：DB 权威值(listMyTeams/getTeamDetail 的 unread_count)提供基准，
    // 新增消息已由 notifyIfNeeded → bumpUnread 实时 +1，进入讨论区 markRead → clearUnread 清零。
    if (activeTab.value === 'chat') {
      scrollChatToBottom()
    }
  } catch (e) {
    // 拉取失败静默忽略，下一轮轮询/重连会重试
  }
}

// 全局轻量轮询兜底：只要当前团队处于激活状态就持续运行（不绑定 chat tab）。
// 即使 WebSocket 彻底不通，也能在数秒内通过 DB 拉取看到新消息，无需手动刷新整页。
// 轮询同时负责刷新「未读红点」，使红点不再依赖 WS 推送。
let msgPollTimer = null
function startMsgPoll() {
  stopMsgPoll()
  msgPollTimer = setInterval(() => refreshActiveTeamMessages(), 5000)
}
function stopMsgPoll() {
  if (msgPollTimer) {
    clearInterval(msgPollTimer)
    msgPollTimer = null
  }
}


/** 收到「未正在查看」的消息时，给出多重提示：右上角 Toast(自动隐藏) + 浏览器桌面通知 + 提示音 */
function notifyNewMessage(msg) {
  if (!msg || msg.userId === currentUserId.value) return // 自己的消息不打扰
  const team = teams.value.find(t => t.teamId === msg.teamId)
  const teamName = team ? team.teamName : '团队'
  const sender = msg.nickName || msg.senderName || '成员'
  const raw = msg.content || ''
  const preview = raw.length > 30 ? raw.slice(0, 30) + '…' : raw
  const title = `${sender} 在「${teamName}」`
  const body = preview || '发来了新消息'

  // 1) 右上角 Toast，duration 后自动隐藏，点击直达讨论区
  ElNotification({
    title,
    message: body,
    type: 'info',
    duration: 5000,
    position: 'top-right',
    onClick: () => jumpToTeamChat(msg.teamId)
  })

  // 2) 浏览器桌面通知
  pushDesktopNotify(title, body, msg.teamId)

  // 3) 提示音
  playNotifySound()
}

/**
 * 统一的新消息提示入口：WS 推送与全局轮询共用。
 * - 去重：已弹过的消息(notifyIfNeeded 或 onWsMessage 标记)不再重复弹。
 * - 排除自己发的消息。
 * - 排除「正在该团队讨论 tab 查看」的情况(消息已可见，无需打扰)。
 * - 非讨论 tab 时同步累加未读红点。
 * 这样即使 WebSocket 完全不通，靠全局轮询也能在数秒内弹出右上角 Toast + 桌面通知 + 提示音。
 */
function notifyIfNeeded(msg) {
  if (!msg || !msg.msgId) return
  if (msg.userId === currentUserId.value) return
  if (notifiedMsgIds.value.has(msg.msgId)) return
  const viewing = activeTab.value === 'chat' && activeTeam.value && activeTeam.value.teamId === msg.teamId
  if (viewing) return
  notifiedMsgIds.value.add(msg.msgId)
  notifyNewMessage(msg)
  bumpUnread(msg.teamId)
}

/** 点击 Toast/桌面通知时，定位到对应团队并打开讨论 tab */
function jumpToTeamChat(teamId) {
  const t = teams.value.find(x => x.teamId === teamId)
  if (!t) return
  selectTeam(t)
  activeTab.value = 'chat'
}

/** 浏览器桌面通知（已授权时直接弹；未决定时 lazy 请求权限） */
function pushDesktopNotify(title, body, teamId) {
  if (typeof window === 'undefined' || !('Notification' in window)) return
  if (Notification.permission === 'granted') {
    try {
      const n = new Notification(title, { body, tag: 'team-msg-' + teamId })
      n.onclick = () => { window.focus(); jumpToTeamChat(teamId); n.close() }
    } catch (e) { /* 部分浏览器构造失败，忽略 */ }
  } else if (Notification.permission !== 'denied') {
    Notification.requestPermission().catch(() => {})
  }
}

/** 用户手动开启桌面通知（写在按钮点击里，满足浏览器「用户手势」要求） */
function enableDesktopNotify() {
  if (typeof window === 'undefined' || !('Notification' in window)) {
    ElMessage.info('当前浏览器不支持桌面通知')
    return
  }
  if (Notification.permission === 'granted') {
    ElMessage.success('桌面通知已开启')
    return
  }
  Notification.requestPermission().then(p => {
    if (p === 'granted') ElMessage.success('桌面通知已开启')
    else ElMessage.warning('已拒绝桌面通知，可在浏览器地址栏重新允许')
  })
}

/** 提示音：用 Web Audio 现场合成一声轻「叮」，无需音频资源文件 */
let _audioCtx = null
function playNotifySound() {
  try {
    const Ctx = window.AudioContext || window.webkitAudioContext
    if (!Ctx) return
    _audioCtx = _audioCtx || new Ctx()
    if (_audioCtx.state === 'suspended') _audioCtx.resume()
    const o = _audioCtx.createOscillator()
    const g = _audioCtx.createGain()
    o.type = 'sine'
    o.frequency.value = 880
    g.gain.value = 0.04
    o.connect(g); g.connect(_audioCtx.destination)
    o.start()
    g.gain.exponentialRampToValueAtTime(0.0001, _audioCtx.currentTime + 0.18)
    o.stop(_audioCtx.currentTime + 0.2)
  } catch (e) { /* 自动播放策略限制时忽略 */ }
}

// 切换到讨论标签页时，立即拉取一次并标记已读、清除红点（全局轮询不在此启停，始终运行）
watch(activeTab, (val) => {
  if (val === 'chat' && activeTeam.value) {
    refreshActiveTeamMessages()
    markTeamRead(activeTeam.value.teamId, []).then(() => clearUnread(activeTeam.value.teamId)).catch(() => {})
    scrollChatToBottom()
  } else if (val === 'members') {
    // 切到成员 tab 时刷新当前分页数据
    loadMembers()
  } else if (val === 'projects') {
    loadProjects()
  }
})
onUnmounted(() => {
  stopMsgPoll()
  disconnectWs()
})

onMounted(async () => {
  // 注册 WS 重连回调：连接恢复后从 DB 重新同步当前团队消息，补偿断连期间丢失的推送
  setOnReconnect(() => refreshActiveTeamMessages())
  if (!userStore.nickName) {
    try { await userStore.getInfo() } catch (e) { if (import.meta.env.DEV) console.warn('获取用户信息失败', e) }
  }
  await loadTeams()
})
</script>

<style scoped>
.team-page {
  height: 100vh;
  overflow: hidden;
  background: var(--c-bg);
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}
/* ===== Hero 问候区（与工作台首页一致） ===== */
.portal-main {
  flex: 1;
  width: 100%;
  min-height: 0;
  overflow: hidden;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  padding: 0;
}
.tp-list-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
  font-weight: 600;
  color: var(--c-text);
  margin-bottom: 12px;
  padding: 2px 4px;
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
  width: 280px;
  flex-shrink: 0;
  background: var(--c-surface);
  border-radius: var(--radius-lg);
  padding: 14px;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--c-border-light);
  height: 100%;
  min-height: 0;
  overflow: auto;
  box-sizing: border-box;
}
.tp-team-item {
  padding: 12px;
  border-radius: var(--radius-md);
  cursor: pointer;
  margin-bottom: 8px;
  border: 1px solid transparent;
  transition: all 0.18s;
}
.tp-team-item:hover {
  background: var(--c-primary-bg);
}
.tp-team-item.active {
  background: var(--c-primary-bg);
  border-color: var(--c-primary-light);
  box-shadow: var(--shadow-sm);
}
.tti-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}
.tti-name {
  flex: 1;
  min-width: 0;
  font-weight: 600;
  font-size: 14px;
  color: var(--c-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.tti-meta {
  display: flex;
  align-items: center;
  gap: 10px;
}
.tti-role {
  font-size: 12px;
  color: var(--c-text-muted);
}
.tti-role.is-owner {
  color: var(--c-warning);
  font-weight: 500;
}
.tti-count {
  font-size: 12px;
  color: var(--c-text-subtle);
}
.tti-unread {
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  background: var(--c-danger);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  line-height: 18px;
  text-align: center;
  flex-shrink: 0;
}
.tp-detail {
  flex: 1;
  background: var(--c-surface);
  border-radius: var(--radius-lg);
  padding: 20px 22px;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--c-border-light);
  min-height: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}
.tp-detail-empty {
  flex: 1;
  background: var(--c-surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--c-border-light);
  display: flex;
  align-items: center;
  justify-content: center;
}
/* ===== 团队 Banner ===== */
.td-banner {
  position: relative;
  overflow: hidden;
  border-radius: var(--radius-lg);
  background: linear-gradient(135deg, #dbeafe 0%, #eff4ff 55%, #e0f2fe 100%);
  border: 1px solid var(--c-border-light);
  color: var(--c-text);
  padding: 14px 20px 0;
  margin-bottom: 12px;
  box-shadow: var(--shadow-sm);
}
.td-banner::before,
.td-banner::after {
  content: '';
  position: absolute;
  border-radius: 50%;
  background: rgba(37, 99, 235, 0.06);
  pointer-events: none;
}
.td-banner::before {
  width: 220px;
  height: 220px;
  right: -60px;
  top: -100px;
}
.td-banner::after {
  width: 140px;
  height: 140px;
  right: 120px;
  bottom: -90px;
  background: rgba(37, 99, 235, 0.045);
}
.td-banner-content {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}
.td-banner-info {
  flex: 1;
  min-width: 0;
}
.td-banner-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.td-name {
  margin: 0;
  font-size: 20px;
  color: var(--c-text);
  line-height: 1.2;
}
.td-banner-title-row :deep(.el-tag) {
  border-color: var(--c-border);
}
.td-banner-title-row :deep(.el-tag--warning) {
  background: var(--c-warning-bg);
  color: var(--c-warning);
}
.td-banner-title-row :deep(.el-tag--info) {
  background: var(--c-bg);
  color: var(--c-text-muted);
}
.td-desc {
  margin: 4px 0 0;
  color: var(--c-text-muted);
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.td-ops {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}
.td-banner-stats {
  position: relative;
  z-index: 1;
  display: flex;
  gap: 36px;
  margin-top: 10px;
  padding: 10px 0 12px;
  border-top: 1px solid var(--c-border-light);
}
.td-stat {
  display: flex;
  flex-direction: column;
  gap: 2px;
  cursor: pointer;
  padding: 2px 6px;
  border-radius: var(--radius-sm);
  transition: background 0.15s;
}
.td-stat:hover {
  background: rgba(37, 99, 235, 0.07);
}
.td-stat-num {
  font-size: 18px;
  font-weight: 700;
  line-height: 1;
  color: var(--c-primary);
}
.td-stat-label {
  font-size: 12px;
  color: var(--c-text-muted);
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
  overflow: hidden;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}

.td-tabs :deep(.el-tabs__nav-wrap::after) {
  background-color: var(--c-border-light);
  height: 1px;
}
.td-tabs :deep(.el-tabs__item) {
  font-size: 14px;
  color: var(--c-text-muted);
}
.td-tabs :deep(.el-tabs__item:hover) {
  color: var(--c-primary);
}
.td-tabs :deep(.el-tabs__item.is-active) {
  color: var(--c-primary);
  font-weight: 600;
}
.td-tabs :deep(.el-tabs__active-bar) {
  background-color: var(--c-primary);
  border-radius: 2px;
}

.td-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 6px 0 12px;
}
.td-tab-count {
  font-size: 13px;
  color: var(--c-text-muted);
}
.member-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}
.mc-name {
  font-weight: 500;
  font-size: 13px;
  color: var(--c-text);
}
.mc-sub {
  font-size: 12px;
  color: var(--c-text-subtle);
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
  background: var(--c-primary-bg);
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
  color: var(--c-text);
}
.md-sub {
  font-size: 12px;
  color: var(--c-text-subtle);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.md-add {
  color: var(--c-primary);
  font-size: 18px;
}

/* 项目/成员 tab 内容撑满 pane */
.project-link {
  color: var(--c-primary);
  cursor: pointer;
  font-weight: 500;
}
.project-link:hover {
  text-decoration: underline;
}
/* Git 仓库列 */
.git-repo-link {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: var(--c-primary);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
}
.git-repo-link:hover {
  opacity: 0.85;
  text-decoration: underline;
}
.git-repo-icon {
  font-size: 15px;
}
.git-repo-count {
  font-size: 12px;
  color: var(--c-text-subtle);
  font-weight: 400;
}
.git-repo-empty {
  font-size: 13px;
  color: var(--c-text-subtle);
}
.tab-pane-inner {
  flex: 1;
  min-height: 0;
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
/* 表头统一居中（项目表 / 成员表同用） */
.tab-pane-body :deep(.el-table__header th.el-table__cell) {
  text-align: center;
  background: var(--c-bg);
  color: var(--c-text);
  font-weight: 600;
}
/* 表体内容统一居中 */
.tab-pane-body :deep(.el-table__body td.el-table__cell) {
  text-align: center;
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

/* ===== 团队信息 tab ===== */
.td-info {
  margin-top: 14px;
}
.td-info :deep(.el-descriptions__label) {
  width: 110px;
  color: var(--c-text-muted);
  font-weight: 500;
  background: var(--c-bg);
}
.td-info :deep(.el-descriptions__content) {
  color: var(--c-text);
}
.td-info :deep(.el-descriptions__body) {
  border-color: var(--c-border-light);
}
.td-invite-card {
  position: relative;
  overflow: hidden;
  border: 1px solid var(--c-primary-light);
  border-radius: var(--radius-md);
  padding: 16px 18px;
  background: var(--c-primary-bg);
}
.td-invite-card::after {
  content: '';
  position: absolute;
  right: -30px;
  top: -30px;
  width: 110px;
  height: 110px;
  border-radius: 50%;
  background: rgba(37, 99, 235, 0.08);
  pointer-events: none;
}
.td-invite-card-head {
  position: relative;
  display: flex;
  align-items: baseline;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.td-invite-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--c-primary);
}
.td-invite-tip {
  font-size: 12px;
  color: var(--c-text-muted);
}
.td-invite-row {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.td-invite-code {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 15px;
  letter-spacing: 1px;
  color: var(--c-text);
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: var(--radius-sm);
  padding: 5px 12px;
  min-width: 160px;
  text-align: center;
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
  flex: 1;
  min-height: 0;
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
  background: linear-gradient(135deg, var(--c-primary), #7c3aed);
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
  color: var(--c-text);
}
.chat-time {
  font-size: 11px;
  color: var(--c-text-subtle);
}
.chat-bubble {
  display: inline-block;
  padding: 9px 13px;
  border-radius: 12px;
  border-top-left-radius: 4px;
  background: var(--c-border-light);
  color: var(--c-text);
  font-size: 14px;
  line-height: 1.55;
  word-break: break-word;
  white-space: pre-wrap;
}
.chat-mine .chat-bubble {
  background: linear-gradient(135deg, var(--c-primary), var(--c-primary-light));
  color: #fff;
  border-top-left-radius: 12px;
  border-top-right-radius: 4px;
}
.chat-read {
  margin-top: 4px;
  font-size: 11px;
  color: var(--c-text-subtle);
}
.chat-input {
  display: flex;
  gap: 10px;
  align-items: flex-end;
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid var(--c-border-light);
}
.chat-input .el-button {
  flex-shrink: 0;
  height: 54px;
}
</style>
