<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="项目ID" prop="projectId">
        <el-input
          v-model="queryParams.projectId"
          placeholder="请输入项目ID"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态(0草稿 1已确认)" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态(0草稿 1已确认)" clearable>
          <el-option
            v-for="dict in ai_doc_status"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="生成模型" prop="sourceModel">
        <el-input
          v-model="queryParams.sourceModel"
          placeholder="请输入生成模型"
          clearable
          @keyup.enter="handleQuery"
        />
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
          v-hasPermi="['system:baseline:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['system:baseline:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['system:baseline:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['system:baseline:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="baselineList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="基线ID" align="center" prop="baselineId" />
      <el-table-column label="项目ID" align="center" prop="projectId" />
      <el-table-column label="结构化需求内容(JSON: 功能点/业务规则/角色权限/字段信息)" align="center" prop="content" />
      <el-table-column label="状态(0草稿 1已确认)" align="center" prop="status">
        <template #default="scope">
          <dict-tag :options="ai_doc_status" :value="scope.row.status"/>
        </template>
      </el-table-column>
      <el-table-column label="生成模型" align="center" prop="sourceModel" />
      <el-table-column label="生成参数(JSON)" align="center" prop="modelParams" />
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:baseline:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:baseline:remove']">删除</el-button>
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

    <!-- 添加或修改需求基线对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="baselineRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="项目ID" prop="projectId">
              <el-input v-model="form.projectId" placeholder="请输入项目ID" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="结构化需求内容(JSON: 功能点/业务规则/角色权限/字段信息)">
              <editor v-model="form.content" :min-height="192"/>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="状态(0草稿 1已确认)" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio
                  v-for="dict in ai_doc_status"
                  :key="dict.value"
                  :label="dict.value"
                >{{dict.label}}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="生成模型" prop="sourceModel">
              <el-input v-model="form.sourceModel" placeholder="请输入生成模型" />
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
  </div>
</template>

<script setup name="Baseline">
import { listBaseline, getBaseline, delBaseline, addBaseline, updateBaseline } from "@/api/ai/baseline"

const { proxy } = getCurrentInstance()
const { ai_doc_status } = useDict('ai_doc_status')

const baselineList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    projectId: undefined,
    content: undefined,
    status: undefined,
    sourceModel: undefined,
    modelParams: undefined,
  },
  rules: {
    projectId: [
      { required: true, message: "项目ID不能为空", trigger: "blur" }
    ],
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询需求基线列表 */
function getList() {
  loading.value = true
  listBaseline(queryParams.value).then(response => {
    baselineList.value = response.rows
    total.value = response.total
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
    baselineId: null,
    projectId: null,
    content: null,
    status: null,
    sourceModel: null,
    modelParams: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
    remark: null
  }
  proxy.resetForm("baselineRef")
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
  ids.value = selection.map(item => item.baselineId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加需求基线"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _baselineId = row.baselineId || ids.value
  getBaseline(_baselineId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改需求基线"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["baselineRef"].validate(valid => {
    if (valid) {
      if (form.value.baselineId != null) {
        updateBaseline(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addBaseline(form.value).then(() => {
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
  const _baselineIds = row.baselineId || ids.value
  proxy.$modal.confirm('是否确认删除需求基线编号为"' + _baselineIds + '"的数据项？').then(function() {
    return delBaseline(_baselineIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('system/baseline/export', {
    ...queryParams.value
  }, `baseline_${new Date().getTime()}.xlsx`)
}

getList()
</script>
