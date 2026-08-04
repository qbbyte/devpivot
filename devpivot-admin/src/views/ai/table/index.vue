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
      <el-form-item label="表名" prop="tableName">
        <el-input
          v-model="queryParams.tableName"
          placeholder="请输入表名"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="表说明" prop="tableComment">
        <el-input
          v-model="queryParams.tableComment"
          placeholder="请输入表说明"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="数据库类型(MySQL/PostgreSQL)" prop="dbType">
        <el-select v-model="queryParams.dbType" placeholder="请选择数据库类型(MySQL/PostgreSQL)" clearable>
          <el-option
            v-for="dict in ai_db_type"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
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
          v-hasPermi="['system:table:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['system:table:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['system:table:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['system:table:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="tableList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="表结构ID" align="center" prop="tableId" />
      <el-table-column label="项目ID" align="center" prop="projectId" />
      <el-table-column label="表名" align="center" prop="tableName" />
      <el-table-column label="表说明" align="center" prop="tableComment" />
      <el-table-column label="数据库类型(MySQL/PostgreSQL)" align="center" prop="dbType">
        <template #default="scope">
          <dict-tag :options="ai_db_type" :value="scope.row.dbType"/>
        </template>
      </el-table-column>
      <el-table-column label="表关系说明(JSON)" align="center" prop="relationDesc" />
      <el-table-column label="完整DDL脚本" align="center" prop="ddlSql" />
      <el-table-column label="规范校验结果(JSON)" align="center" prop="checkReport" />
      <el-table-column label="状态(0草稿 1已确认)" align="center" prop="status">
        <template #default="scope">
          <dict-tag :options="ai_doc_status" :value="scope.row.status"/>
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:table:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:table:remove']">删除</el-button>
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

    <!-- 添加或修改数据库结构对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="tableRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="项目ID" prop="projectId">
              <el-input v-model="form.projectId" placeholder="请输入项目ID" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="表名" prop="tableName">
              <el-input v-model="form.tableName" placeholder="请输入表名" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="表说明" prop="tableComment">
              <el-input v-model="form.tableComment" placeholder="请输入表说明" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="数据库类型(MySQL/PostgreSQL)" prop="dbType">
              <el-select v-model="form.dbType" placeholder="请选择数据库类型(MySQL/PostgreSQL)">
                <el-option
                  v-for="dict in ai_db_type"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="表关系说明(JSON)" prop="relationDesc">
              <el-input v-model="form.relationDesc" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="完整DDL脚本" prop="ddlSql">
              <el-input v-model="form.ddlSql" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="规范校验结果(JSON)" prop="checkReport">
              <el-input v-model="form.checkReport" type="textarea" placeholder="请输入内容" />
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

<script setup name="Table">
import { listTable, getTable, delTable, addTable, updateTable } from "@/api/ai/table"

const { proxy } = getCurrentInstance()
const { ai_db_type, ai_doc_status } = useDict('ai_db_type', 'ai_doc_status')

const tableList = ref([])
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
    tableName: undefined,
    tableComment: undefined,
    dbType: undefined,
    relationDesc: undefined,
    ddlSql: undefined,
    checkReport: undefined,
    status: undefined,
  },
  rules: {
    projectId: [
      { required: true, message: "项目ID不能为空", trigger: "blur" }
    ],
    tableName: [
      { required: true, message: "表名不能为空", trigger: "blur" }
    ],
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询数据库结构列表 */
function getList() {
  loading.value = true
  listTable(queryParams.value).then(response => {
    tableList.value = response.rows
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
    tableId: null,
    projectId: null,
    tableName: null,
    tableComment: null,
    dbType: null,
    relationDesc: null,
    ddlSql: null,
    checkReport: null,
    status: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
    remark: null
  }
  proxy.resetForm("tableRef")
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
  ids.value = selection.map(item => item.tableId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加数据库结构"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _tableId = row.tableId || ids.value
  getTable(_tableId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改数据库结构"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["tableRef"].validate(valid => {
    if (valid) {
      if (form.value.tableId != null) {
        updateTable(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addTable(form.value).then(() => {
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
  const _tableIds = row.tableId || ids.value
  proxy.$modal.confirm('是否确认删除数据库结构编号为"' + _tableIds + '"的数据项？').then(function() {
    return delTable(_tableIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('system/table/export', {
    ...queryParams.value
  }, `table_${new Date().getTime()}.xlsx`)
}

getList()
</script>
