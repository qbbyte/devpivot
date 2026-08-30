<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="项目名称" prop="projectName">
        <el-input
          v-model="queryParams.projectName"
          placeholder="请输入项目名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="目标用户群体" prop="targetUser">
        <el-input
          v-model="queryParams.targetUser"
          placeholder="请输入目标用户群体"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="数据库类型" prop="dbType">
        <el-select v-model="queryParams.dbType" placeholder="请选择数据库类型" clearable style="width: 160px">
          <el-option
            v-for="dict in ai_db_type"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="进度阶段" prop="step">
        <el-select v-model="queryParams.step" placeholder="请选择进度阶段" clearable style="width: 160px">
          <el-option
            v-for="dict in ai_project_step"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="是否置顶" prop="isTop">
        <el-select v-model="queryParams.isTop" placeholder="请选择是否置顶" clearable style="width: 120px">
          <el-option
            v-for="dict in sys_yes_no"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="项目状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择项目状态" clearable style="width: 120px">
          <el-option
            v-for="dict in ai_project_status"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
          v-hasPermi="['system:project:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['system:project:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['system:project:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['system:project:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="projectList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="项目ID" align="center" prop="projectId" width="80" />
      <el-table-column label="项目名称" align="left" prop="projectName" show-overflow-tooltip />
      <el-table-column label="行业分类" align="center" prop="industryType" width="110" show-overflow-tooltip />
      <el-table-column label="目标用户群体" align="center" prop="targetUser" width="120" show-overflow-tooltip />
      <el-table-column label="数据库类型" align="center" prop="dbType" width="100">
        <template #default="scope">
          <dict-tag :options="ai_db_type" :value="scope.row.dbType"/>
        </template>
      </el-table-column>
      <el-table-column label="进度阶段" align="center" prop="step" width="100">
        <template #default="scope">
          <dict-tag :options="ai_project_step" :value="scope.row.step"/>
        </template>
      </el-table-column>
      <el-table-column label="置顶" align="center" prop="isTop" width="80">
        <template #default="scope">
          <dict-tag :options="sys_yes_no" :value="scope.row.isTop"/>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="90">
        <template #default="scope">
          <dict-tag :options="ai_project_status" :value="scope.row.status"/>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="200">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleView(scope.row)" v-hasPermi="['system:project:query']">查看</el-button>
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:project:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:project:remove']">删除</el-button>
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

    <!-- 添加或修改AI项目对话框 -->
    <el-dialog :title="title" v-model="open" width="780px" append-to-body>
      <el-form ref="projectRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="项目名称" prop="projectName">
              <el-input v-model="form.projectName" placeholder="请输入项目名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="目标用户群体" prop="targetUser">
              <el-input v-model="form.targetUser" placeholder="请输入目标用户群体" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="数据库类型" prop="dbType">
              <el-select v-model="form.dbType" placeholder="请选择数据库类型" style="width: 100%">
                <el-option
                  v-for="dict in ai_db_type"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="进度阶段" prop="step">
              <el-select v-model="form.step" placeholder="请选择进度阶段" style="width: 100%">
                <el-option
                  v-for="dict in ai_project_step"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="项目简介" prop="projectIntro">
              <el-input v-model="form.projectIntro" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否置顶" prop="isTop">
              <el-radio-group v-model="form.isTop">
                <el-radio
                  v-for="dict in sys_yes_no"
                  :key="dict.value"
                  :value="dict.value"
                >{{dict.label}}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="项目状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio
                  v-for="dict in ai_project_status"
                  :key="dict.value"
                  :value="dict.value"
                >{{dict.label}}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 查看项目详情对话框 -->
    <el-dialog title="项目详情" v-model="viewOpen" width="860px" append-to-body>
      <div class="view-meta">
        <span>项目ID：{{ viewForm.projectId }}</span>
        <span>行业分类：{{ viewForm.industryType || '-' }}</span>
        <span>目标用户群体：{{ viewForm.targetUser || '-' }}</span>
        <span>创建时间：{{ viewForm.createTime }}</span>
      </div>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="项目简介" name="intro">
          <el-input v-model="viewForm.projectIntro" type="textarea" :rows="16" readonly placeholder="暂无项目简介" />
        </el-tab-pane>
        <el-tab-pane label="模型策略" name="strategy">
          <el-input v-model="prettyStrategy" type="textarea" :rows="16" readonly placeholder="暂无模型策略" />
        </el-tab-pane>
        <el-tab-pane label="备注" name="remark">
          <el-input v-model="viewForm.remark" type="textarea" :rows="16" readonly placeholder="暂无备注" />
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
  </div>
</template>

<script setup name="Project">
import { listProject, getProject, delProject, addProject, updateProject } from "@/api/ai/project"

const { proxy } = getCurrentInstance()
const { ai_db_type, ai_project_step, ai_project_status, sys_yes_no } = useDict('ai_db_type', 'ai_project_step', 'ai_project_status', 'sys_yes_no')

const projectList = ref([])
const open = ref(false)
const viewOpen = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")
const viewForm = ref({})
const activeTab = ref("intro")

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    projectName: undefined,
    targetUser: undefined,
    dbType: undefined,
    step: undefined,
    isTop: undefined,
    status: undefined,
  },
  rules: {
    projectName: [
      { required: true, message: "项目名称不能为空", trigger: "blur" }
    ],
  }
})

const { queryParams, form, rules } = toRefs(data)

/** JSON 美化展示（非 JSON 原样返回） */
function prettyJson(text) {
  if (!text) return ''
  try {
    return JSON.stringify(JSON.parse(text), null, 2)
  } catch (e) {
    return text
  }
}

const prettyStrategy = computed(() => prettyJson(viewForm.value.modelStrategy))

/** 查询AI项目列表 */
function getList() {
  loading.value = true
  listProject(queryParams.value).then(response => {
    projectList.value = response.rows
    total.value = response.total
    loading.value = false
  }).catch(() => {
    loading.value = false
  })
}

/** 取消按钮 */
function cancel() {
  open.value = false
  reset()
}

/** 表单重置 */
function reset() {
  form.value = {
    projectId: null,
    projectName: null,
    industryType: null,
    projectIntro: null,
    targetUser: null,
    dbType: null,
    modelStrategy: null,
    step: null,
    isTop: null,
    status: null,
    delFlag: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
    remark: null
  }
  proxy.resetForm("projectRef")
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

/** 多选框选中数据 */
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.projectId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加AI项目"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _projectId = row.projectId || ids.value
  getProject(_projectId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改AI项目"
  })
}

/** 查看详情按钮操作 */
function handleView(row) {
  getProject(row.projectId).then(response => {
    viewForm.value = response.data
    activeTab.value = "intro"
    viewOpen.value = true
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["projectRef"].validate(valid => {
    if (valid) {
      if (form.value.projectId != null) {
        updateProject(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addProject(form.value).then(() => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _projectIds = row.projectId || ids.value
  proxy.$modal.confirm('是否确认删除AI项目编号为"' + _projectIds + '"的数据项？').then(function() {
    return delProject(_projectIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('system/project/export', {
    ...queryParams.value
  }, `project_${new Date().getTime()}.xlsx`)
}

getList()
</script>

<style lang="scss" scoped>
.view-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 12px;
  font-size: 13px;
  color: #909399;
}
</style>
