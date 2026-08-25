<template>
  <div class="invite-page">
    <PortalHeader />
    <main class="invite-main">
      <div class="invite-card">
        <template v-if="state === 'loading'">
          <el-skeleton :rows="3" animated />
        </template>

        <el-result v-else-if="state === 'invalid'" icon="error" title="邀请链接无效" sub-title="邀请码不存在或团队已解散，请联系团队管理员获取新的邀请码">
          <template #extra>
            <el-button type="primary" @click="goHome">返回工作台</el-button>
          </template>
        </el-result>

        <el-result v-else-if="state === 'joined'" icon="info" :title="`你已加入团队「${teamName}」`" sub-title="无需重复加入，可直接进入团队查看">
          <template #extra>
            <el-button type="primary" @click="goTeam">进入团队</el-button>
          </template>
        </el-result>

        <el-result v-else-if="state === 'ready'" icon="success" :title="`加入团队「${teamName}」？`" sub-title="加入后可参与团队项目与讨论">
          <template #extra>
            <el-button @click="cancel">暂不加入</el-button>
            <el-button type="primary" :loading="joining" @click="doJoin">确认加入</el-button>
          </template>
        </el-result>
      </div>
    </main>
  </div>
</template>

<script setup name="InviteJoin">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import useUserStore from '@/store/modules/user'
import PortalHeader from './components/PortalHeader.vue'
import { getInviteInfo, joinTeamByCode } from '@/api/ai/team'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const state = ref('loading') // loading | invalid | joined | ready
const teamName = ref('')
const joining = ref(false)

onMounted(() => {
  const code = route.params.code
  // 未登录：先登录再回跳（登录页已支持 redirect 参数）
  if (!userStore.token) {
    router.replace({ path: '/login', query: { redirect: '/invite/' + code } })
    return
  }
  loadInviteInfo(code)
})

function loadInviteInfo(code) {
  getInviteInfo(code)
    .then(res => {
      const info = res.data
      if (!info) {
        state.value = 'invalid'
        return
      }
      teamName.value = info.teamName || ''
      state.value = info.joined ? 'joined' : 'ready'
    })
    .catch(() => {
      state.value = 'invalid'
    })
}

function doJoin() {
  joining.value = true
  joinTeamByCode(route.params.code)
    .then(() => {
      ElMessage.success(`已加入团队「${teamName.value}」`)
      router.replace('/portal/team')
    })
    .finally(() => {
      joining.value = false
    })
}

function goTeam() {
  router.replace('/portal/team')
}

function goHome() {
  router.replace('/portal')
}

function cancel() {
  router.replace('/portal/team')
}
</script>

<style scoped>
.invite-page {
  min-height: 100vh;
  background: var(--c-bg);
}
.invite-main {
  max-width: 720px;
  margin: 0 auto;
  padding: 72px 24px 60px;
}
.invite-card {
  background: var(--c-surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md, 0 4px 16px rgba(37, 99, 235, 0.08));
  padding: 12px 24px 8px;
}
.invite-card :deep(.el-result__title p) {
  font-size: 18px;
  color: var(--c-text);
}
.invite-card :deep(.el-result__subtitle p) {
  font-size: 13px;
  color: var(--c-text-muted);
}
</style>
