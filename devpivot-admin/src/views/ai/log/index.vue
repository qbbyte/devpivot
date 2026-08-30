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
      <el-form-item label="并行任务ID" prop="taskId">
        <el-input
          v-model="queryParams.taskId"
          placeholder="请输入并行任务ID"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="模型ID" prop="modelId">
        <el-input
          v-model="queryParams.modelId"
          placeholder="请输入模型ID"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="输入token" prop="reqTokens">
        <el-input
          v-model="queryParams.reqTokens"
          placeholder="请输入输入token"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="输出token" prop="respTokens">
        <el-input
          v-model="queryParams.respTokens"
          placeholder="请输入输出token"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="费用" prop="cost">
        <el-input
          v-model="queryParams.cost"
          placeholder="请输入费用"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="命中缓存" prop="cacheHit">
        <el-select v-model="queryParams.cacheHit" placeholder="请选择命中缓存" clearable style="width: 160px">
          <el-option
            v-for="dict in sys_yes_no"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="耗时(毫秒)" prop="consumeMs">
        <el-input
          v-model="queryParams.consumeMs"
          placeholder="请输入耗时(毫秒)"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="开始时间" prop="startTime">
        <el-date-picker clearable
          v-model="queryParams.startTime"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择开始时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="结束时间" prop="endTime">
        <el-date-picker clearable
          v-model="queryParams.endTime"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择结束时间">
        </el-date-picker>
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
          v-hasPermi="['system:log:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['system:log:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['system:log:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['system:log:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="logList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="日志ID" align="center" prop="logId" />
      <el-table-column label="项目ID" align="center" prop="projectId" />
      <el-table-column label="并行任务ID" align="center" prop="taskId" />
      <el-table-column label="场景类型" align="center" prop="taskType" />
      <el-table-column label="模型ID" align="center" prop="modelId" />
      <el-table-column label="输入token" align="center" prop="reqTokens" />
      <el-table-column label="输出token" align="center" prop="respTokens" />
      <el-table-column label="费用" align="center" prop="cost" />
      <el-table-column label="命中缓存" align="center" prop="cacheHit">
        <template #default="scope">
          <dict-tag :options="sys_yes_no" :value="scope.row.cacheHit"/>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status">
        <template #default="scope">
          <dict-tag :options="ai_call_status" :value="scope.row.status"/>
        </template>
      </el-table-column>
      <el-table-column label="错误信息" align="center" prop="errorMsg" />
      <el-table-column label="耗时(毫秒)" align="center" prop="consumeMs" />
      <el-table-column label="开始时间" align="center" prop="startTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.startTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="结束时间" align="center" prop="endTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.endTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:log:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:log:remove']">删除</el-button>
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

    <!-- 添加或修改AI模型调用日志对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="logRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="项目ID" prop="projectId">
              <el-input v-model="form.projectId" placeholder="请输入项目ID" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="并行任务ID" prop="taskId">
              <el-input v-model="form.taskId" placeholder="请输入并行任务ID" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="模型ID" prop="modelId">
              <el-input v-model="form.modelId" placeholder="请输入模型ID" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="输入token" prop="reqTokens">
              <el-input v-model="form.reqTokens" placeholder="请输入输入token" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="输出token" prop="respTokens">
              <el-input v-model="form.respTokens" placeholder="请输入输出token" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="费用" prop="cost">
              <el-input v-model="form.cost" placeholder="请输入费用" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="命中缓存" prop="cacheHit">
              <el-input v-model="form.cacheHit" placeholder="请输入命中缓存" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="错误信息" prop="errorMsg">
              <el-input v-model="form.errorMsg" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="耗时(毫秒)" prop="consumeMs">
              <el-input v-model="form.consumeMs" placeholder="请输入耗时(毫秒)" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="开始时间" prop="startTime">
              <el-date-picker clearable
                v-model="form.startTime"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择开始时间">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="结束时间" prop="endTime">
              <el-date-picker clearable
                v-model="form.endTime"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择结束时间">
              </el-date-picker>
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

<script setup name="Log">
import { listLog, getLog, delLog, addLog, updateLog } from "@/api/ai/log"

const { proxy } = getCurrentInstance()
const { ai_call_status, sys_yes_no } = useDict('ai_call_status', 'sys_yes_no')

const logList = ref([])
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
    taskId: undefined,
    taskType: undefined,
    modelId: undefined,
    reqTokens: undefined,
    respTokens: undefined,
    cost: undefined,
    cacheHit: undefined,
    status: undefined,
    errorMsg: undefined,
    consumeMs: undefined,
    startTime: undefined,
    endTime: undefined,
  },
  rules: {
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询AI模型调用日志列表 */
function getList() {
  loading.value = true
  listLog(queryParams.value).then(response => {
    logList.value = response.rows
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
    logId: null,
    projectId: null,
    taskId: null,
    taskType: null,
    modelId: null,
    reqTokens: null,
    respTokens: null,
    cost: null,
    cacheHit: null,
    status: null,
    errorMsg: null,
    consumeMs: null,
    startTime: null,
    endTime: null,
    createBy: null,
    createTime: null,
    remark: null
  }
  proxy.resetForm("logRef")
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
  ids.value = selection.map(item => item.logId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加AI模型调用日志"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _logId = row.logId || ids.value
  getLog(_logId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改AI模型调用日志"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["logRef"].validate(valid => {
    if (valid) {
      if (form.value.logId != null) {
        updateLog(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addLog(form.value).then(() => {
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
  const _logIds = row.logId || ids.value
  proxy.$modal.confirm('是否确认删除AI模型调用日志编号为"' + _logIds + '"的数据项？').then(function() {
    return delLog(_logIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('system/log/export', {
    ...queryParams.value
  }, `log_${new Date().getTime()}.xlsx`)
}

getList()
</script>
