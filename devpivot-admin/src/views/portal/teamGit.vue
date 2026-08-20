<template>
  <div class="tg-page">
    <!-- ====== 顶部导航 ====== -->
    <header class="tg-topbar">
      <button class="tg-back" @click="goBack" aria-label="返回">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 12H5M12 19l-7-7 7-7"/></svg>
        <span>返回团队</span>
      </button>
      <div class="tg-topbar-center">
        <h1 class="tg-title">Git 提交统计</h1>
        <div class="tg-breadcrumb" v-if="teamName || projectName">
          {{ teamName ? teamName + ' · ' : '' }}{{ projectName || ('项目 #' + projectId) }}
        </div>
      </div>
      <div class="tg-topbar-right" v-if="repos.length">
        <span class="tg-status-dot" :class="{ 'dot-active': !!activeRepo }"></span>
        <span class="tg-status-text">{{ repos.length }} 个仓库</span>
      </div>
    </header>

    <div v-loading="gitLoading" class="tg-body">
      <!-- ============ 左侧：仓库导航 + 概览 ============ -->
      <aside class="tg-side">
        <section class="tg-panel">
          <div class="tg-panel-head">
            <h2 class="tg-panel-title">仓库</h2>
            <button v-if="canManage" class="tg-btn-add" @click="openAddRepo">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
              添加仓库
            </button>
          </div>

          <!-- 仓库列表 -->
          <div v-if="repos.length" class="tg-repo-list">
            <button
              v-for="r in repos"
              :key="r.id"
              class="tg-repo-item"
              :class="{ 'item-active': activeRepoId === r.id }"
              @click="selectRepo(r.id)"
            >
              <span class="tg-repo-item-name">
                <span class="tg-repo-item-icon">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 19c-5 1.5-5-2.5-7-3m14 6v-3.87a3.37 3.37 0 0 0-.94-2.61c3.14-.35 6.44-1.54 6.44-7A5.44 5.44 0 0 0 20 4.77 5.07 5.07 0 0 0 19.91 1S18.73.65 16 2.48a13.38 13.38 0 0 0-7 0C6.27.65 5.09 1 5.09 1A5.07 5.07 0 0 0 5 4.77a5.44 5.44 0 0 0-1.5 3.78c0 5.42 3.3 6.61 6.44 7A3.37 3.37 0 0 0 9 18.13V22"/></svg>
                </span>
                <span class="tg-repo-item-text">
                  <span class="tg-repo-item-title">{{ repoDisplayName(r) }}</span>
                  <span class="tg-repo-item-sub tg-mono">{{ r.repoFullName }}</span>
                </span>
              </span>
              <span class="tg-repo-item-tag">{{ PLATFORM_LABEL[r.platform] || r.platform }}</span>
              <span v-if="canManage" class="tg-repo-item-del" @click.stop="removeRepo(r)">
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
              </span>
            </button>
          </div>
          <div v-else class="tg-repo-empty">
            <p>还没有关联仓库</p>
            <p class="tg-repo-empty-sub">点击「添加仓库」关联第一个 Git 仓库</p>
          </div>

          <div v-if="canManage && activeRepo" class="tg-panel-foot">
            <button class="tg-btn-ghost" @click="openEditRepo(activeRepo)">编辑当前仓库</button>
          </div>
        </section>

        <!-- 统计概览 -->
        <section class="tg-panel" v-if="activeRepo">
          <div class="tg-panel-head">
            <h2 class="tg-panel-title">统计概览</h2>
          </div>
          <div class="tg-stats-grid">
            <div class="tg-stat tg-stat--primary">
              <div class="tg-stat-num">{{ totalCommits }}</div>
              <div class="tg-stat-label">总提交</div>
            </div>
            <div class="tg-stat tg-stat--green">
              <div class="tg-stat-num">{{ gitContributors.length }}</div>
              <div class="tg-stat-label">贡献者</div>
            </div>
            <div class="tg-stat tg-stat--indigo">
              <div class="tg-stat-num tg-stat-num--sm">{{ currentBranch }}</div>
              <div class="tg-stat-label">当前分支</div>
            </div>
            <div class="tg-stat tg-stat--neutral">
              <div class="tg-stat-num tg-stat-num--sm">{{ platformLabel }}</div>
              <div class="tg-stat-label">平台</div>
            </div>
          </div>
        </section>
      </aside>

      <!-- ============ 中间列：仓库信息 + 提交历史 ============ -->
      <main class="tg-main">
        <!-- 零仓库引导 -->
        <div v-if="!repos.length && !gitLoading" class="tg-onboard">
          <div class="tg-onboard-icon">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="6" cy="6" r="3"/><circle cx="6" cy="18" r="3"/><line x1="6" y1="9" x2="6" y2="15"/><line x1="18" y1="6" x2="18" y2="6"/><path d="M18 9v3a3 3 0 0 1-3 3H9"/></svg>
          </div>
          <h3>关联你的 Git 仓库</h3>
          <p>一个项目可以关联多个仓库（如前端、后端、移动端），在左侧添加后即可查看各仓库的提交统计</p>
          <button v-if="canManage" class="tg-btn-primary tg-onboard-btn" @click="openAddRepo">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
            添加第一个仓库
          </button>
        </div>

        <template v-if="activeRepo">
          <!-- 仓库信息栏 -->
          <div class="tg-repo-bar">
            <div class="tg-repo-info">
              <span class="tg-repo-icon">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 19c-5 1.5-5-2.5-7-3m14 6v-3.87a3.37 3.37 0 0 0-.94-2.61c3.14-.35 6.44-1.54 6.44-7A5.44 5.44 0 0 0 20 4.77 5.07 5.07 0 0 0 19.91 1S18.73.65 16 2.48a13.38 13.38 0 0 0-7 0C6.27.65 5.09 1 5.09 1A5.07 5.07 0 0 0 5 4.77a5.44 5.44 0 0 0-1.5 3.78c0 5.42 3.3 6.61 6.44 7A3.37 3.37 0 0 0 9 18.13V22"/></svg>
              </span>
              <span class="tg-repo-fullname tg-mono">{{ repoDisplayName(activeRepo) }}</span>
              <span class="tg-tag-sm">{{ platformLabel }}</span>
            </div>
            <div class="tg-branch-wrap" v-if="gitConfig.configured">
              <button class="tg-branch-chip" :class="{ 'chip-open': branchMenuOpen }" @click.stop="toggleBranchMenu">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="6" cy="6" r="3"/><circle cx="6" cy="18" r="3"/><line x1="6" y1="9" x2="6" y2="15"/><line x1="18" y1="9" x2="18" y2="9"/><path d="M18 9v3M9 18h6a3 3 0 0 0 3-3V9"/></svg>
                <span>{{ currentBranch }}</span>
                <svg class="chip-caret" width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="6 9 12 15 18 9"/></svg>
              </button>
              <ul v-if="branchMenuOpen" class="tg-branch-menu" @click.stop>
                <li :class="{ active: !gitConfig.repoBranch }" @click="pickBranch('')">
                  <span class="menu-name">默认分支</span>
                  <svg v-if="!gitConfig.repoBranch" class="menu-check" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
                </li>
                <li v-if="gitBranchLoading" class="menu-empty">加载中...</li>
                <li v-else-if="!gitBranches.length" class="menu-empty" @click.stop="loadBranches(true)">暂无分支，点击刷新</li>
                <li v-for="b in gitBranches" :key="b" :class="{ active: gitConfig.repoBranch === b }" @click="pickBranch(b)">
                  <span class="menu-name tg-mono">{{ b }}</span>
                  <svg v-if="gitConfig.repoBranch === b" class="menu-check" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
                </li>
                <li class="menu-refresh" @click.stop="loadBranches(true)">
                  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/></svg>
                  刷新分支
                </li>
              </ul>
            </div>
          </div>

          <!-- 提交热力图（Gitee/GitHub 风格） -->
          <section class="tg-card tg-heat-card">
            <div class="tg-card-head">
              <h2 class="tg-card-title">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2"/><rect x="7" y="7" width="3" height="3"/><rect x="12" y="7" width="3" height="3" fill="currentColor" stroke="none"/><rect x="17" y="7" width="3" height="3"/><rect x="7" y="12" width="3" height="3"/><rect x="12" y="12" width="3" height="3"/><rect x="17" y="12" width="3" height="3"/><rect x="7" y="17" width="3" height="3"/><rect x="12" y="17" width="3" height="3"/><rect x="17" y="17" width="3" height="3"/></svg>
                提交热力图
              </h2>
              <span class="tg-card-sub" v-if="gitHeatmap">近 {{ gitHeatmap.days || 365 }} 天 · {{ gitHeatmap.total }} 次提交</span>
            </div>
            <div class="tg-card-body tg-heat-body">
              <div v-if="gitHeatLoading" class="tg-heat-placeholder">
                <span class="tg-heat-loading-dot"></span> 正在统计提交分布...
              </div>
              <div v-else-if="gitHeatError" class="tg-heat-placeholder">
                <span>{{ gitHeatError }}</span>
                <button class="tg-btn-ghost" @click="loadHeatmap">重试</button>
              </div>
              <div v-else-if="!gitHeatmap || !gitHeatmap.total" class="tg-heat-placeholder">近一年暂无提交</div>
              <div v-else class="tg-heat-scroll">
                <div class="tg-heat">
                  <div class="tg-heat-months">
                    <span class="tg-heat-months-spacer"></span>
                    <div class="tg-heat-months-track">
                      <span v-for="(label, i) in heatMonthLabels" :key="i" class="tg-heat-month">{{ label }}</span>
                    </div>
                  </div>
                  <div class="tg-heat-main">
                    <div class="tg-heat-weekdays">
                      <span v-for="(l, i) in WEEKDAY_LABELS" :key="i" class="tg-heat-weekday">{{ l }}</span>
                    </div>
                    <div class="tg-heat-grid">
                      <div v-for="(week, wi) in heatGrid" :key="wi" class="tg-heat-week">
                        <span
                          v-for="(cell, ci) in week"
                          :key="ci"
                          class="tg-heat-cell"
                          :class="{ 'cell-off': !cell.inRange }"
                          :style="cell.inRange ? { background: HEAT_COLORS[heatLevel(cell.count)] } : null"
                          :title="heatCellTitle(cell)"
                        ></span>
                      </div>
                    </div>
                  </div>
                  <div class="tg-heat-foot">
                    <span class="tg-heat-label-sm">少</span>
                    <span v-for="c in HEAT_COLORS" :key="c" class="tg-heat-legend" :style="{ background: c }"></span>
                    <span class="tg-heat-label-sm">多</span>
                  </div>
                </div>
              </div>
            </div>
          </section>

          <!-- 提交历史 -->
          <section class="tg-card">
            <div class="tg-card-head">
              <h2 class="tg-card-title">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
                提交历史
              </h2>
              <span class="tg-card-sub">{{ currentBranch }}</span>
            </div>
            <div class="tg-card-body">
              <div v-if="!gitCommitLoading && !gitCommits.length" class="tg-empty">暂无提交记录</div>
              <div v-else class="tg-timeline">
                <div v-for="(cm, idx) in gitCommits" :key="cm.sha || idx" class="tg-tl-item">
                  <div class="tg-tl-dot" :class="{ 'tl-dot-team': cm.memberName }"></div>
                  <div class="tg-tl-content">
                    <div class="tg-tl-msg">{{ cm.message }}</div>
                    <div class="tg-tl-meta">
                      <span class="tg-tl-author" :class="{ 'is-team': cm.memberName }">
                        {{ cm.memberName || cm.authorName || cm.authorLogin }}
                      </span>
                      <span class="tg-tl-date">{{ formatGitDate(cm.date) }}</span>
                      <span v-if="cm.additions || cm.deletions" class="tg-tl-diff">
                        <span class="add">+{{ cm.additions }}</span> <span class="del">-{{ cm.deletions }}</span>
                      </span>
                    </div>
                  </div>
                </div>
              </div>
              <div v-loading="gitCommitLoading" style="min-height:60px"></div>

              <!-- 触底自动加载哨兵 -->
              <div v-if="gitCommits.length && !gitCommitNoMore" id="tg-commit-sentinel" class="tg-commit-sentinel"></div>
              <p v-if="gitCommits.length && gitCommitNoMore" class="tg-commit-end">已显示全部提交</p>
            </div>
          </section>
        </template>
      </main>

      <!-- ============ 右列：成员提交排名 ============ -->
      <aside class="tg-side-right">
        <template v-if="activeRepo">
          <section class="tg-card">
            <div class="tg-card-head">
              <h2 class="tg-card-title">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
                成员提交排名
              </h2>
              <span class="tg-card-sub">按提交数排序</span>
            </div>
            <div class="tg-card-body">
              <div v-if="!gitContribLoading && !sortedContributors.length" class="tg-empty">暂无贡献者数据</div>
              <div v-else class="tg-ranks">
                <div
                  v-for="(c, i) in sortedContributors"
                  :key="(c.login||'') + (c.email||'')"
                  class="tg-rank"
                >
                  <span class="tg-rank-num" :class="rankClass(i)">{{ i + 1 }}</span>
                  <div class="tg-rank-body">
                    <div class="tg-rank-head">
                      <span v-if="c.memberName" class="tg-rank-name">{{ c.memberName }}</span>
                      <span v-else class="tg-rank-name">{{ c.name || c.login }}</span>
                      <span v-if="c.memberName" class="tg-tag-team">团队</span>
                      <span class="tg-rank-stats">
                        <span class="tg-rank-count">{{ c.contributions }}</span> 次
                        <span v-if="c.additions || c.deletions" class="tg-rank-diff">
                          <span class="add">+{{ c.additions }}</span><span class="del">-{{ c.deletions }}</span>
                        </span>
                      </span>
                    </div>
                    <div class="tg-rank-bar">
                      <span :style="{ width: barWidth(c.contributions), background: barColor(i) }"></span>
                    </div>
                  </div>
                </div>
              </div>
              <div v-loading="gitContribLoading" style="min-height:60px"></div>
            </div>
          </section>
        </template>
      </aside>
    </div>

    <!-- ============ 添加/编辑仓库弹窗 ============ -->
    <div v-if="repoDialog.visible" class="tg-modal-mask" @click.self="closeRepoModal">
      <div class="tg-modal">
        <div class="tg-modal-head">
          <h3 class="tg-modal-title">{{ repoDialog.mode === 'edit' ? '编辑仓库' : '添加仓库' }}</h3>
          <button class="tg-modal-close" @click="closeRepoModal" aria-label="关闭">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </div>

        <div class="tg-modal-body">
          <div class="tg-field">
            <label class="tg-label">仓库别名 <span class="tg-optional">可选</span></label>
            <input v-model="repoDialog.name" :disabled="!canManage" class="tg-input" maxlength="64" placeholder="如：前端 / 后端 / API 服务" />
            <p class="tg-field-hint">用于区分同一项目的多个仓库；留空时显示仓库全名</p>
          </div>

          <div class="tg-field">
            <label class="tg-label">仓库地址 <span class="tg-req">*</span></label>
            <input
              v-model="repoDialog.repoUrl"
              :disabled="!canManage"
              class="tg-input"
              maxlength="512"
              placeholder="https://github.com/owner/repo"
              @change="onRepoUrlChange"
            />
            <p class="tg-field-hint">支持 GitHub / GitLab / Gitee / Gitea，自动解析平台与仓库名</p>
          </div>

          <div class="tg-field">
            <label class="tg-label">平台</label>
            <select v-model="repoDialog.platform" :disabled="!canManage" class="tg-input tg-select" @change="onPlatformChange">
              <option value="github">GitHub</option>
              <option value="gitlab">GitLab</option>
              <option value="gitee">Gitee</option>
              <option value="gitea">Gitea（自托管）</option>
              <option value="self-hosted">自托管(GitLab兼容)</option>
            </select>
          </div>

          <div class="tg-field">
            <label class="tg-label">仓库全名</label>
            <input v-model="repoDialog.repoFullName" :disabled="!canManage" class="tg-input tg-mono" maxlength="255" placeholder="由地址自动解析" />
          </div>

          <div class="tg-field" v-if="repoDialog.platform === 'self-hosted' || repoDialog.platform === 'gitea'">
            <label class="tg-label">API 地址 <span class="tg-req">*</span></label>
            <input
              v-model="repoDialog.repoApiBase"
              :disabled="!canManage"
              class="tg-input tg-mono"
              maxlength="255"
              :placeholder="repoDialog.platform === 'gitea' ? 'https://gitea.xxx.com/api/v1' : 'https://gitlab.xxx.com/api/v4'"
            />
          </div>

          <div class="tg-field">
            <label class="tg-label">默认分支</label>
            <input v-model="repoDialog.repoBranch" :disabled="!canManage" class="tg-input tg-mono" maxlength="128" placeholder="留空 = 仓库默认分支" />
          </div>

          <div class="tg-field">
            <label class="tg-label">访问令牌 <span v-if="repoDialog.mode === 'edit'" class="tg-optional">（留空不修改）</span></label>
            <input
              v-model="repoDialog.accessToken"
              :disabled="!canManage"
              type="password"
              class="tg-input"
              maxlength="600"
              :placeholder="repoDialog.maskedToken ? ('已配置：' + repoDialog.maskedToken) : '只读 PAT / 项目令牌'"
            />
            <p class="tg-field-hint">令牌仅加密存储，不会回传浏览器</p>
          </div>
        </div>

        <div class="tg-modal-foot">
          <button class="tg-btn-cancel" @click="closeRepoModal">取消</button>
          <button class="tg-btn-primary tg-btn-save" :loading="repoDialog.saving" @click="saveRepoModal">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"/><polyline points="17 21 17 13 7 13 7 21"/><polyline points="7 3 7 8 15 8"/></svg>
            {{ repoDialog.mode === 'edit' ? '保存修改' : '添加仓库' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
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
import { gitCacheGet, gitCacheSet, gitCacheClear } from '@/utils/gitCache'

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
  'linear-gradient(90deg, #2563eb, #60a5fa)',
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
  // 后端返回有提交的日期明细列表(缺省视为 0)
  const counts = {}
  ;(hm.list || []).forEach(d => { counts[d.date] = d.count })
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
    const targetRepoId = repoDialog.mode === 'edit' ? repoDialog.editingRepoId : null
    if (repoDialog.mode === 'edit') {
      await updateTeamProjectRepo(teamId.value, repoDialog.editingRepoId, payload)
      ElMessage.success('仓库配置已更新')
    } else {
      const res = await addTeamProjectRepo(teamId.value, projectId.value, payload)
      ElMessage.success('仓库已添加')
      activeRepoId.value = Number(res.data)
    }
    // 仓库配置/令牌变更后，主动失效该仓库的前端与后端缓存
    if (targetRepoId) clearFrontCache(targetRepoId)
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
    clearFrontCache(r.id)
    await loadRepos()
    // 删除后自动切到第一个仓库并重新加载数据
    await selectRepo(activeRepoId.value || null)
  } catch (e) {
    // 响应拦截器已统一提示
  }
}

/** 仓库配置/令牌/增删后，失效该仓库的前端内存缓存（后端 Redis 由接口侧自行清理） */
function clearFrontCache(repoId) {
  if (repoId == null) return
  ;['repo:', 'branches:', 'contrib:', 'commits:', 'heat:'].forEach(p => gitCacheClear(p + repoId))
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
  const cacheKey = 'repo:' + activeRepoId.value
  try {
    let d = gitCacheGet(cacheKey)
    if (!d) {
      const res = await getTeamProjectRepo(teamId.value, activeRepoId.value)
      d = res.data || {}
      gitCacheSet(cacheKey, d)
    }
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
    gitCacheClear(cacheKey)
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
  const cacheKey = 'contrib:' + activeRepoId.value
  try {
    let list = gitCacheGet(cacheKey)
    if (!list) {
      const res = await getTeamProjectContributors(teamId.value, activeRepoId.value)
      list = res.data || []
      gitCacheSet(cacheKey, list)
    }
    gitContributors.value = list
  } catch (e) {
    gitCacheClear(cacheKey)
    gitContributors.value = []
  } finally {
    gitContribLoading.value = false
  }
}
async function loadHeatmap() {
  if (!activeRepoId.value || !teamId.value || !gitConfig.configured) return
  gitHeatLoading.value = true
  gitHeatError.value = ''
  const cacheKey = 'heat:' + activeRepoId.value + ':' + (gitConfig.repoBranch || '')
  try {
    let hm = gitCacheGet(cacheKey)
    if (!hm) {
      const res = await getTeamProjectHeatmap(teamId.value, activeRepoId.value, { branch: gitConfig.repoBranch || '' })
      hm = res.data || null
      if (hm) gitCacheSet(cacheKey, hm)
    }
    gitHeatmap.value = hm
  } catch (e) {
    gitCacheClear(cacheKey)
    gitHeatmap.value = null
    gitHeatError.value = '热力图加载失败'
  } finally {
    gitHeatLoading.value = false
  }
}
async function loadBranches(force) {
  if (!activeRepoId.value || !teamId.value) return
  gitBranchLoading.value = true
  const cacheKey = 'branches:' + activeRepoId.value
  try {
    let d = force ? null : gitCacheGet(cacheKey)
    if (!d) {
      const res = await getTeamProjectBranches(teamId.value, activeRepoId.value)
      d = res.data || {}
      gitCacheSet(cacheKey, d)
    }
    gitBranches.value = d.branches || []
    if (!gitConfig.repoBranch && d.defaultBranch) {
      gitConfig.repoBranch = d.defaultBranch
    }
  } catch (e) {
    // 响应拦截器已统一提示
    gitCacheClear(cacheKey)
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
  const cacheKey = 'commits:' + activeRepoId.value + ':' + (gitConfig.repoBranch || '') + ':' + page
  try {
    let list = gitCacheGet(cacheKey)
    if (!list) {
      const res = await getTeamProjectCommits(teamId.value, activeRepoId.value, { page, branch: gitConfig.repoBranch || '' })
      list = res.data || []
      gitCacheSet(cacheKey, list)
    }
    if (reset) gitCommits.value = []
    gitCommits.value.push(...list)
    gitCommitPage.value = page
    gitCommitNoMore.value = list.length < 20
  } catch (e) {
    gitCacheClear(cacheKey)
    if (reset) gitCommits.value = []
  } finally {
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
</script>

<style scoped>
/* ========== Design Tokens (Soft UI Evolution · Indigo) ========== */
.tg-page {
  --c-primary: #2563eb;
  --c-primary-light: #60a5fa;
  --c-primary-bg: #eff4ff;
  --c-accent: #059669;
  --c-accent-bg: #ecfdf5;
  --c-bg: #f5f8fd;
  --c-surface: #ffffff;
  --c-border: #e2e8f0;
  --c-border-light: #f1f5f9;
  --c-text: #1e293b;
  --c-text-muted: #64748b;
  --c-text-subtle: #94a3b8;
  --shadow-sm: 0 1px 3px rgba(37, 99, 235, 0.06), 0 1px 2px rgba(0, 0, 0, 0.04);
  --shadow-md: 0 4px 12px rgba(37, 99, 235, 0.08), 0 2px 4px rgba(0, 0, 0, 0.04);
  --shadow-lg: 0 10px 30px rgba(37, 99, 235, 0.1), 0 4px 8px rgba(0, 0, 0, 0.04);
  --radius-sm: 8px;
  --radius-md: 12px;
  --radius-lg: 16px;
  --transition: 200ms ease;

  height: 100vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background: var(--c-bg);
  color: var(--c-text);
  font-family: -apple-system, 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.tg-mono {
  font-family: 'JetBrains Mono', 'Fira Code', 'Consolas', 'Cascadia Code', monospace;
}

/* ========== 顶部导航 ========== */
.tg-topbar {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  background: var(--c-surface);
  border-bottom: 1px solid var(--c-border);
  z-index: 10;
}
.tg-back {
  display: flex;
  align-items: center;
  gap: 6px;
  border: none;
  background: transparent;
  color: var(--c-text-muted);
  cursor: pointer;
  font-size: 13px;
  padding: 6px 10px;
  border-radius: var(--radius-sm);
  transition: all var(--transition);
}
.tg-back:hover {
  background: var(--c-primary-bg);
  color: var(--c-primary);
}
.tg-topbar-center {
  text-align: center;
}
.tg-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--c-text);
  margin: 0;
}
.tg-breadcrumb {
  font-size: 12px;
  color: var(--c-text-subtle);
  margin-top: 2px;
}
.tg-topbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}
.tg-status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--c-text-subtle);
}
.dot-active {
  background: var(--c-accent);
  box-shadow: 0 0 0 3px var(--c-accent-bg);
}
.tg-status-text {
  font-size: 12px;
  color: var(--c-text-muted);
  font-weight: 500;
}

/* ========== 布局 ========== */
.tg-body {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  gap: 16px;
  padding: 16px 20px;
  align-items: stretch;
  overflow: hidden;
}
.tg-side {
  width: 380px;
  flex: 0 0 380px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  background-color: var(--c-bg);
  overflow-y: auto;
  min-height: 0;
}
.tg-main {
  flex: 1 1 auto;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
  overflow: hidden;
}
.tg-side-right {
  width: 390px;
  flex: 0 0 390px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  background-color: var(--c-bg);
  overflow-y: auto;
  min-height: 0;
}

/* 中间列：仓库信息栏固定，提交历史卡片撑满并内部滚动 */
.tg-repo-bar {
  flex-shrink: 0;
}
.tg-main > .tg-card:last-child {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.tg-main > .tg-card:last-child .tg-card-head {
  flex-shrink: 0;
}
.tg-main > .tg-card:last-child .tg-card-body {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
}

/* ========== 面板 ========== */
.tg-panel {
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: var(--radius-md);
  padding: 18px;
  box-shadow: var(--shadow-sm);
  transition: box-shadow var(--transition);
}
.tg-panel:hover {
  box-shadow: var(--shadow-md);
}
.tg-panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}
.tg-panel-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--c-text);
  margin: 0;
}
.tg-panel-foot {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--c-border-light);
  display: flex;
  justify-content: center;
}

/* ========== 仓库列表 ========== */
.tg-btn-add {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: none;
  background: var(--c-primary-bg);
  color: var(--c-primary);
  font-size: 12px;
  font-weight: 600;
  padding: 5px 10px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all var(--transition);
}
.tg-btn-add:hover {
  background: var(--c-primary);
  color: #fff;
}
.tg-repo-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 380px;
  overflow-y: auto;
}
.tg-repo-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  border: 1px solid var(--c-border-light);
  background: var(--c-surface);
  border-radius: var(--radius-sm);
  padding: 9px 10px;
  cursor: pointer;
  transition: all var(--transition);
  text-align: left;
}
.tg-repo-item:hover {
  border-color: var(--c-primary-light);
  background: #f5f9ff;
}
.tg-repo-item.item-active {
  border-color: var(--c-primary);
  background: var(--c-primary-bg);
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.12);
}
.tg-repo-item-name {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}
.tg-repo-item-icon {
  display: flex;
  align-items: center;
  color: var(--c-primary);
  flex-shrink: 0;
}
.tg-repo-item-text {
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.tg-repo-item-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--c-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.tg-repo-item-sub {
  font-size: 11px;
  color: var(--c-text-subtle);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.tg-repo-item-tag {
  font-size: 10px;
  font-weight: 600;
  padding: 2px 6px;
  border-radius: 4px;
  background: #f1f5f9;
  color: var(--c-text-muted);
  flex-shrink: 0;
}
.tg-repo-item.item-active .tg-repo-item-tag {
  background: #fff;
  color: var(--c-primary);
}
.tg-repo-item-del {
  display: none;
  align-items: center;
  color: var(--c-text-subtle);
  padding: 2px;
  border-radius: 4px;
  flex-shrink: 0;
}
.tg-repo-item:hover .tg-repo-item-del {
  display: inline-flex;
}
.tg-repo-item-del:hover {
  color: #ef4444;
  background: #fef2f2;
}
.tg-repo-empty {
  text-align: center;
  padding: 20px 8px;
  border: 1.5px dashed var(--c-border);
  border-radius: var(--radius-sm);
}
.tg-repo-empty p {
  margin: 0;
  font-size: 13px;
  color: var(--c-text-muted);
}
.tg-repo-empty .tg-repo-empty-sub {
  font-size: 11px;
  color: var(--c-text-subtle);
  margin-top: 4px;
}
.tg-btn-ghost {
  border: none;
  background: transparent;
  color: var(--c-primary);
  font-size: 12px;
  font-weight: 600;
  padding: 6px 14px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all var(--transition);
}
.tg-btn-ghost:hover {
  background: var(--c-primary-bg);
}

/* ========== 统计概览 ========== */
.tg-stats-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}
.tg-stat {
  border-radius: var(--radius-sm);
  padding: 14px 10px;
  text-align: center;
  border: 1px solid transparent;
  transition: transform var(--transition);
}
.tg-stat:hover {
  transform: translateY(-2px);
}
.tg-stat--primary { background: var(--c-primary-bg); border-color: #bfdbfe; }
.tg-stat--green { background: var(--c-accent-bg); border-color: #a7f3d0; }
.tg-stat--indigo { background: #f0f9ff; border-color: #bae6fd; }
.tg-stat--neutral { background: #f8fafc; border-color: var(--c-border-light); }

.tg-stat-num {
  font-size: 24px;
  font-weight: 800;
  color: var(--c-text);
  line-height: 1.2;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.tg-stat-num--sm {
  font-size: 14px;
  font-weight: 700;
}
.tg-stat-label {
  font-size: 11px;
  color: var(--c-text-muted);
  margin-top: 6px;
  font-weight: 500;
}

/* ========== 引导空态 ========== */
.tg-onboard {
  background: var(--c-surface);
  border: 2px dashed var(--c-border);
  border-radius: var(--radius-lg);
  padding: 60px 40px;
  text-align: center;
}
.tg-onboard-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: var(--c-primary-bg);
  color: var(--c-primary);
}
.tg-onboard h3 {
  font-size: 18px;
  font-weight: 700;
  color: var(--c-text);
  margin: 0 0 8px;
}
.tg-onboard p {
  font-size: 14px;
  color: var(--c-text-muted);
  max-width: 420px;
  margin: 0 auto 20px;
  line-height: 1.6;
}
.tg-onboard-btn {
  width: auto;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 22px;
}

/* ========== 仓库信息栏 ========== */
.tg-repo-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  padding: 12px 16px;
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
}
.tg-repo-info {
  display: flex;
  align-items: center;
  gap: 8px;
}
.tg-repo-icon {
  color: var(--c-primary);
  display: flex;
  align-items: center;
}
.tg-repo-fullname {
  font-size: 15px;
  font-weight: 700;
  color: var(--c-text);
}
.tg-tag-sm {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 20px;
  background: var(--c-primary-bg);
  color: var(--c-primary);
}
.tg-branch-wrap {
  position: relative;
}
.tg-branch-chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  background: #f0f9ff;
  color: #0284c7;
  border: 1px solid #bae6fd;
  border-radius: 20px;
  padding: 4px 12px;
  font-size: 12px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  transition: all var(--transition);
}
.tg-branch-chip:hover {
  background: #e0f2fe;
  border-color: #7dd3fc;
}
.tg-branch-chip.chip-open {
  background: #e0f2fe;
  border-color: #38bdf8;
  box-shadow: 0 0 0 3px rgba(56, 189, 248, 0.15);
}
.tg-branch-chip .chip-caret {
  transition: transform var(--transition);
}
.tg-branch-chip.chip-open .chip-caret {
  transform: rotate(180deg);
}
.tg-branch-menu {
  position: absolute;
  top: calc(100% + 6px);
  right: 0;
  z-index: 20;
  min-width: 200px;
  max-width: 320px;
  max-height: 280px;
  overflow-y: auto;
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: var(--radius-sm);
  box-shadow: var(--shadow-lg);
  padding: 4px;
  list-style: none;
  margin: 0;
  animation: tg-menu-in 140ms ease;
}
@keyframes tg-menu-in {
  from { opacity: 0; transform: translateY(-4px); }
  to { opacity: 1; transform: translateY(0); }
}
.tg-branch-menu li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 7px 10px;
  border-radius: 6px;
  font-size: 13px;
  color: var(--c-text);
  cursor: pointer;
  transition: background 120ms ease;
}
.tg-branch-menu li:hover {
  background: var(--c-primary-bg);
}
.tg-branch-menu li.active {
  background: var(--c-primary-bg);
  color: var(--c-primary);
  font-weight: 600;
}
.tg-branch-menu li .menu-name {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.tg-branch-menu li .menu-check {
  color: var(--c-primary);
  flex-shrink: 0;
}
.tg-branch-menu .menu-empty {
  color: var(--c-text-subtle);
  font-size: 12px;
  cursor: default;
  text-align: center;
  justify-content: center;
}
.tg-branch-menu .menu-empty:hover {
  background: transparent;
}
.tg-branch-menu .menu-refresh {
  margin-top: 4px;
  padding-top: 8px;
  border-top: 1px solid var(--c-border-light);
  color: var(--c-text-muted);
  justify-content: center;
  font-size: 12px;
}
.tg-branch-menu .menu-refresh:hover {
  color: var(--c-primary);
  background: transparent;
}

/* ========== 数据卡片 ========== */
.tg-card {
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
  transition: box-shadow var(--transition);
}
.tg-card:hover {
  box-shadow: var(--shadow-md);
}
.tg-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  border-bottom: 1px solid var(--c-border-light);
}
.tg-card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 700;
  color: var(--c-text);
  margin: 0;
}
.tg-card-title svg {
  color: var(--c-primary);
}
.tg-card-sub {
  font-size: 12px;
  color: var(--c-text-subtle);
  font-weight: 500;
}
.tg-card-body {
  padding: 8px 18px 16px;
}
.tg-empty {
  text-align: center;
  padding: 32px 0;
  color: var(--c-text-subtle);
  font-size: 13px;
}

/* ========== 提交热力图（Gitee/GitHub 风格 7×N 网格） ========== */
.tg-heat-card {
  flex-shrink: 0;
}
.tg-heat-body {
  padding: 14px 18px 16px;
}
.tg-heat-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 20px 0;
  color: var(--c-text-subtle);
  font-size: 13px;
}
.tg-heat-loading-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--c-primary-light);
  animation: tg-heat-pulse 1s ease-in-out infinite;
}
@keyframes tg-heat-pulse {
  0%, 100% { opacity: 0.35; transform: scale(0.85); }
  50% { opacity: 1; transform: scale(1.15); }
}
.tg-heat-scroll {
  overflow-x: auto;
  padding-bottom: 2px;
}
.tg-heat {
  min-width: 830px;
}
.tg-heat-months {
  display: flex;
  margin-bottom: 4px;
}
.tg-heat-months-spacer {
  width: 30px;
  flex-shrink: 0;
}
.tg-heat-months-track {
  display: flex;
  flex: 1;
  overflow: hidden;
}
.tg-heat-month {
  width: 15px;
  flex-shrink: 0;
  font-size: 9px;
  color: var(--c-text-subtle);
  line-height: 14px;
  white-space: nowrap;
}
.tg-heat-main {
  display: flex;
}
.tg-heat-weekdays {
  width: 30px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.tg-heat-weekday {
  height: 12px;
  font-size: 9px;
  color: var(--c-text-subtle);
  line-height: 12px;
}
.tg-heat-grid {
  display: flex;
  gap: 3px;
}
.tg-heat-week {
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.tg-heat-cell {
  width: 12px;
  height: 12px;
  border-radius: 2.5px;
  transition: transform var(--transition);
}
.tg-heat-cell:not(.cell-off):hover {
  transform: scale(1.3);
  box-shadow: var(--shadow-sm);
}
.tg-heat-cell.cell-off {
  visibility: hidden;
}
.tg-heat-foot {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 3px;
  margin-top: 8px;
}
.tg-heat-label-sm {
  font-size: 10px;
  color: var(--c-text-subtle);
  margin: 0 2px;
}
.tg-heat-legend {
  width: 11px;
  height: 11px;
  border-radius: 2px;
}

/* ========== 贡献者排名 ========== */
.tg-ranks {
  display: flex;
  flex-direction: column;
}
.tg-rank {
  display: flex;
  align-items: stretch;
  gap: 12px;
  padding: 12px 8px;
  border-bottom: 1px solid var(--c-border-light);
  transition: background var(--transition);
  border-radius: var(--radius-sm);
}
.tg-rank:hover {
  background: var(--c-primary-bg);
}
.tg-rank:last-child {
  border-bottom: none;
}
.tg-rank-num {
  width: 28px;
  height: 28px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  font-size: 12px;
  font-weight: 800;
}
.rank-gold { background: #fef3c7; color: #d97706; }
.rank-silver { background: #f1f5f9; color: #64748b; }
.rank-bronze { background: #fed7aa; color: #c2410c; }
.rank-default { background: var(--c-primary-bg); color: var(--c-primary); }

.tg-rank-body {
  flex: 1;
  min-width: 0;
}
.tg-rank-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 6px;
}
.tg-rank-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--c-text);
}
.tg-tag-team {
  font-size: 10px;
  font-weight: 600;
  padding: 2px 6px;
  border-radius: 4px;
  background: var(--c-accent-bg);
  color: var(--c-accent);
  margin-left: 6px;
}
.tg-rank-stats {
  font-size: 12px;
  color: var(--c-text-muted);
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}
.tg-rank-count {
  font-weight: 700;
  color: var(--c-text);
  font-size: 14px;
}
.tg-rank-diff {
  font-size: 11px;
  display: inline-flex;
  gap: 4px;
}
.tg-rank-bar {
  height: 6px;
  border-radius: 3px;
  background: var(--c-border-light);
  overflow: hidden;
}
.tg-rank-bar > span {
  display: block;
  height: 100%;
  border-radius: 3px;
  transition: width 0.5s cubic-bezier(0.4, 0, 0.2, 1);
}

/* ========== 提交历史时间线 ========== */
.tg-timeline {
  position: relative;
  padding-left: 4px;
}
.tg-tl-item {
  display: flex;
  gap: 14px;
  padding: 10px 0;
  position: relative;
}
.tg-tl-item:not(:last-child)::before {
  content: '';
  position: absolute;
  left: 5px;
  top: 24px;
  bottom: -4px;
  width: 2px;
  background: var(--c-border);
}
.tg-tl-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: 2.5px solid var(--c-surface);
  background: var(--c-primary);
  box-shadow: 0 0 0 2px var(--c-border);
  flex-shrink: 0;
  margin-top: 4px;
  z-index: 1;
}
.tl-dot-team {
  background: var(--c-accent);
  box-shadow: 0 0 0 2px #a7f3d0;
}
.tg-tl-content {
  flex: 1;
  min-width: 0;
}
.tg-tl-msg {
  font-size: 13px;
  color: var(--c-text);
  font-weight: 500;
  line-height: 1.5;
  word-break: break-word;
}
.tg-tl-meta {
  margin-top: 4px;
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 11px;
  color: var(--c-text-subtle);
  flex-wrap: wrap;
}
.tg-tl-author.is-team {
  color: var(--c-accent);
  font-weight: 600;
}
.tg-tl-date {
  font-family: 'JetBrains Mono', 'Consolas', monospace;
  font-size: 11px;
}
.tg-tl-diff {
  display: inline-flex;
  gap: 4px;
}

.add { color: #10b981; font-weight: 600; }
.del { color: #ef4444; font-weight: 600; }

/* ========== 触底加载 ========== */
.tg-commit-sentinel {
  height: 1px;
}
.tg-commit-end {
  text-align: center;
  margin: 16px 0 4px;
  color: var(--c-text-subtle);
  font-size: 12px;
}

/* ========== 表单（弹窗内复用） ========== */
.tg-field {
  margin-bottom: 14px;
}
.tg-label {
  display: block;
  font-size: 12px;
  font-weight: 600;
  color: var(--c-text-muted);
  margin-bottom: 5px;
}
.tg-req { color: #ef4444; }
.tg-optional {
  font-size: 11px;
  font-weight: 400;
  color: var(--c-text-subtle);
}
.tg-input {
  width: 100%;
  border: 1.5px solid var(--c-border);
  border-radius: var(--radius-sm);
  padding: 8px 12px;
  font-size: 13px;
  color: var(--c-text);
  background: var(--c-surface);
  transition: all var(--transition);
  outline: none;
  box-sizing: border-box;
}
.tg-input:focus {
  border-color: var(--c-primary);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
}
.tg-input:disabled {
  background: #f8fafc;
  color: var(--c-text-subtle);
  cursor: not-allowed;
}
.tg-input::placeholder {
  color: var(--c-text-subtle);
}
.tg-select {
  cursor: pointer;
  appearance: auto;
}
.tg-field-hint {
  font-size: 11px;
  color: var(--c-text-subtle);
  margin: 4px 0 0;
  line-height: 1.5;
}

/* ========== 按钮 ========== */
.tg-btn-primary {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: none;
  background: linear-gradient(135deg, var(--c-primary), var(--c-primary-light));
  color: #fff;
  border-radius: var(--radius-sm);
  padding: 10px 16px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition);
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.25);
}
.tg-btn-primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(37, 99, 235, 0.35);
}
.tg-btn-primary:active {
  transform: translateY(0);
}

/* ========== 弹窗 ========== */
.tg-modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(30, 41, 59, 0.45);
  backdrop-filter: blur(2px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 16px;
}
.tg-modal {
  width: 480px;
  max-width: 100%;
  max-height: 90vh;
  overflow-y: auto;
  background: var(--c-surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  animation: tg-modal-in 180ms ease;
}
@keyframes tg-modal-in {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}
.tg-modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px 0;
}
.tg-modal-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--c-text);
  margin: 0;
}
.tg-modal-close {
  border: none;
  background: transparent;
  color: var(--c-text-subtle);
  padding: 4px;
  border-radius: 6px;
  cursor: pointer;
  transition: all var(--transition);
  display: flex;
  align-items: center;
}
.tg-modal-close:hover {
  background: var(--c-border-light);
  color: var(--c-text);
}
.tg-modal-body {
  padding: 16px 20px 4px;
}
.tg-modal-foot {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 12px 20px 18px;
}
.tg-btn-cancel {
  border: 1.5px solid var(--c-border);
  background: var(--c-surface);
  color: var(--c-text-muted);
  border-radius: var(--radius-sm);
  padding: 9px 18px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition);
}
.tg-btn-cancel:hover {
  border-color: var(--c-text-subtle);
  color: var(--c-text);
}
.tg-btn-save {
  min-width: 120px;
}

/* ========== 响应式 ========== */
/* 三列 → 单列堆叠（左仓库导航 / 中提交历史 / 右成员排名 依次排列），恢复整页滚动 */
@media (max-width: 1320px) {
  .tg-page {
    height: auto;
    overflow: visible;
    display: block;
  }
  .tg-body {
    flex-direction: column;
    padding: 12px;
    overflow: visible;
    height: auto;
  }
  .tg-side,
  .tg-side-right {
    width: 100%;
    flex: 1 1 auto;
    overflow: visible;
  }
  .tg-main {
    overflow: visible;
  }
  .tg-main > .tg-card:last-child {
    flex: none;
    display: block;
  }
  .tg-main > .tg-card:last-child .tg-card-body {
    overflow: visible;
  }
}
@media (max-width: 480px) {
  .tg-topbar {
    padding: 10px 12px;
  }
  .tg-body {
    padding: 10px;
  }
  .tg-stats-grid {
    grid-template-columns: 1fr;
  }
  .tg-topbar-center {
    display: none;
  }
}

/* ========== 减弱动画偏好 ========== */
@media (prefers-reduced-motion: reduce) {
  * {
    transition: none !important;
    animation: none !important;
  }
}
</style>
