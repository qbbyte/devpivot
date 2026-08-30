<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="团队名称" prop="teamName">
        <el-input
          v-model="queryParams.teamName"
          placeholder="请输入团队名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 160px">
          <el-option label="正常" value="0" />
          <el-option label="已解散" value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="teamList">
      <el-table-column label="团队ID" align="center" prop="teamId" width="80" />
      <el-table-column label="团队名称" align="left" prop="teamName" show-overflow-tooltip />
      <el-table-column label="描述" align="left" prop="description" show-overflow-tooltip />
      <el-table-column label="队长ID" align="center" prop="ownerId" width="90" />
      <el-table-column label="成员数" align="center" prop="memberCount" width="80" />
      <el-table-column label="关联项目" align="center" prop="projectCount" width="90" />
      <el-table-column label="邀请码" align="center" prop="inviteCode" width="120" />
      <el-table-column label="状态" align="center" width="90">
        <template #default="scope">
          <el-tag :type="scope.row.status === '0' ? 'success' : 'danger'" size="small">
            {{ scope.row.status === '0' ? '正常' : '已解散' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="220">
        <template #default="scope">
          <el-button link type="primary" icon="User" @click="handleMembers(scope.row)" v-hasPermi="['system:team:query']">成员</el-button>
          <el-button link type="primary" icon="Folder" @click="handleProjects(scope.row)" v-hasPermi="['system:team:query']">项目</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDissolve(scope.row)" v-if="scope.row.status === '0'" v-hasPermi="['system:team:remove']">解散</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total>0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 团队成员对话框 -->
    <el-dialog :title="'团队成员 - ' + currentTeam.teamName" v-model="membersOpen" width="720px" append-to-body>
      <el-table v-loading="membersLoading" :data="members" size="small">
        <el-table-column label="用户ID" align="center" prop="userId" width="80" />
        <el-table-column label="昵称" align="center" prop="nickName" width="120" />
        <el-table-column label="账号" align="center" prop="userName" width="120" />
        <el-table-column label="角色" align="center" width="90">
          <template #default="scope">
            <el-tag :type="scope.row.role === 'owner' ? 'warning' : 'info'" size="small">
              {{ roleLabel(scope.row.role) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="职位" align="center" prop="title" width="120">
          <template #default="scope">{{ scope.row.title || '-' }}</template>
        </el-table-column>
        <el-table-column label="邮箱" align="left" prop="email" show-overflow-tooltip>
          <template #default="scope">{{ scope.row.email || '-' }}</template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 团队关联项目对话框 -->
    <el-dialog :title="'关联项目 - ' + currentTeam.teamName" v-model="projectsOpen" width="760px" append-to-body>
      <el-table v-loading="projectsLoading" :data="projects" size="small">
        <el-table-column label="项目ID" align="center" prop="projectId" width="80" />
        <el-table-column label="项目名称" align="left" prop="projectName" show-overflow-tooltip />
        <el-table-column label="阶段" align="center" prop="step" width="110">
          <template #default="scope">{{ scope.row.step || '-' }}</template>
        </el-table-column>
        <el-table-column label="Git 仓库" align="left" width="200">
          <template #default="scope">
            {{ scope.row.repoFullName ? scope.row.repoFullName + ' (' + (scope.row.repoBranch || '-') + ')' : '-' }}
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup name="TeamAdmin">
import { listTeam, listTeamMembers, listTeamProjects, dissolveTeam } from "@/api/ai/teamAdmin"

const { proxy } = getCurrentInstance()

const teamList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const currentTeam = ref({})
const membersOpen = ref(false)
const membersLoading = ref(false)
const members = ref([])
const projectsOpen = ref(false)
const projectsLoading = ref(false)
const projects = ref([])

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    teamName: undefined,
    status: undefined,
  }
})

const { queryParams } = toRefs(data)

/** 角色标签 */
function roleLabel(role) {
  if (role === 'owner') return '队长'
  if (role === 'admin') return '管理员'
  return '成员'
}

/** 查询团队列表 */
function getList() {
  loading.value = true
  listTeam(queryParams.value).then(response => {
    teamList.value = response.rows
    total.value = response.total
    loading.value = false
  }).catch(() => {
    loading.value = false
  })
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

/** 查看团队成员 */
function handleMembers(row) {
  currentTeam.value = row
  membersOpen.value = true
  membersLoading.value = true
  listTeamMembers(row.teamId).then(response => {
    members.value = response.rows || []
  }).finally(() => {
    membersLoading.value = false
  })
}

/** 查看关联项目 */
function handleProjects(row) {
  currentTeam.value = row
  projectsOpen.value = true
  projectsLoading.value = true
  listTeamProjects(row.teamId).then(response => {
    projects.value = response.rows || []
  }).finally(() => {
    projectsLoading.value = false
  })
}

/** 解散团队 */
function handleDissolve(row) {
  proxy.$modal.confirm('是否确认解散团队"' + row.teamName + '"？该操作不可恢复（软删除，团队将无法再被访问）。').then(function() {
    return dissolveTeam(row.teamId)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("解散成功")
  }).catch(() => {})
}

getList()
</script>
