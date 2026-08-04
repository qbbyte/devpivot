<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="所属表结构ID" prop="tableId">
        <el-input
          v-model="queryParams.tableId"
          placeholder="请输入所属表结构ID"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="字段名" prop="columnName">
        <el-input
          v-model="queryParams.columnName"
          placeholder="请输入字段名"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="字段注释" prop="columnComment">
        <el-input
          v-model="queryParams.columnComment"
          placeholder="请输入字段注释"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="字段类型" prop="columnType">
        <el-select v-model="queryParams.columnType" placeholder="请选择字段类型" clearable>
          <el-option
            v-for="dict in ai_field_type"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="长度" prop="columnLength">
        <el-input
          v-model="queryParams.columnLength"
          placeholder="请输入长度"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="是否为空(Y/N)" prop="nullable">
        <el-select v-model="queryParams.nullable" placeholder="请选择是否为空(Y/N)" clearable>
          <el-option
            v-for="dict in sys_yes_no"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="默认值" prop="defaultValue">
        <el-input
          v-model="queryParams.defaultValue"
          placeholder="请输入默认值"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="是否主键(Y/N)" prop="isPk">
        <el-select v-model="queryParams.isPk" placeholder="请选择是否主键(Y/N)" clearable>
          <el-option
            v-for="dict in sys_yes_no"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="外键关联表" prop="fkTable">
        <el-input
          v-model="queryParams.fkTable"
          placeholder="请输入外键关联表"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="外键关联字段" prop="fkColumn">
        <el-input
          v-model="queryParams.fkColumn"
          placeholder="请输入外键关联字段"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="是否唯一约束(Y/N)" prop="isUnique">
        <el-select v-model="queryParams.isUnique" placeholder="请选择是否唯一约束(Y/N)" clearable>
          <el-option
            v-for="dict in sys_yes_no"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="索引类型(NORMAL普通/UNIQUE唯一/UNION联合)" prop="indexType">
        <el-select v-model="queryParams.indexType" placeholder="请选择索引类型(NORMAL普通/UNIQUE唯一/UNION联合)" clearable>
          <el-option
            v-for="dict in ai_index_type"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="排序" prop="sort">
        <el-input
          v-model="queryParams.sort"
          placeholder="请输入排序"
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
          v-hasPermi="['system:column:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['system:column:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['system:column:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['system:column:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="columnList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="字段ID" align="center" prop="columnId" />
      <el-table-column label="所属表结构ID" align="center" prop="tableId" />
      <el-table-column label="字段名" align="center" prop="columnName" />
      <el-table-column label="字段注释" align="center" prop="columnComment" />
      <el-table-column label="字段类型" align="center" prop="columnType">
        <template #default="scope">
          <dict-tag :options="ai_field_type" :value="scope.row.columnType"/>
        </template>
      </el-table-column>
      <el-table-column label="长度" align="center" prop="columnLength" />
      <el-table-column label="是否为空(Y/N)" align="center" prop="nullable">
        <template #default="scope">
          <dict-tag :options="sys_yes_no" :value="scope.row.nullable"/>
        </template>
      </el-table-column>
      <el-table-column label="默认值" align="center" prop="defaultValue" />
      <el-table-column label="是否主键(Y/N)" align="center" prop="isPk">
        <template #default="scope">
          <dict-tag :options="sys_yes_no" :value="scope.row.isPk"/>
        </template>
      </el-table-column>
      <el-table-column label="外键关联表" align="center" prop="fkTable" />
      <el-table-column label="外键关联字段" align="center" prop="fkColumn" />
      <el-table-column label="是否唯一约束(Y/N)" align="center" prop="isUnique">
        <template #default="scope">
          <dict-tag :options="sys_yes_no" :value="scope.row.isUnique"/>
        </template>
      </el-table-column>
      <el-table-column label="索引类型(NORMAL普通/UNIQUE唯一/UNION联合)" align="center" prop="indexType">
        <template #default="scope">
          <dict-tag :options="ai_index_type" :value="scope.row.indexType"/>
        </template>
      </el-table-column>
      <el-table-column label="排序" align="center" prop="sort" />
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:column:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:column:remove']">删除</el-button>
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

    <!-- 添加或修改数据库字段定义对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="columnRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="所属表结构ID" prop="tableId">
              <el-input v-model="form.tableId" placeholder="请输入所属表结构ID" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="字段名" prop="columnName">
              <el-input v-model="form.columnName" placeholder="请输入字段名" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="字段注释" prop="columnComment">
              <el-input v-model="form.columnComment" placeholder="请输入字段注释" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="字段类型" prop="columnType">
              <el-select v-model="form.columnType" placeholder="请选择字段类型">
                <el-option
                  v-for="dict in ai_field_type"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="长度" prop="columnLength">
              <el-input v-model="form.columnLength" placeholder="请输入长度" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="是否为空(Y/N)" prop="nullable">
              <el-radio-group v-model="form.nullable">
                <el-radio
                  v-for="dict in sys_yes_no"
                  :key="dict.value"
                  :label="dict.value"
                >{{dict.label}}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="默认值" prop="defaultValue">
              <el-input v-model="form.defaultValue" placeholder="请输入默认值" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="是否主键(Y/N)" prop="isPk">
              <el-radio-group v-model="form.isPk">
                <el-radio
                  v-for="dict in sys_yes_no"
                  :key="dict.value"
                  :label="dict.value"
                >{{dict.label}}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="外键关联表" prop="fkTable">
              <el-input v-model="form.fkTable" placeholder="请输入外键关联表" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="外键关联字段" prop="fkColumn">
              <el-input v-model="form.fkColumn" placeholder="请输入外键关联字段" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="是否唯一约束(Y/N)" prop="isUnique">
              <el-radio-group v-model="form.isUnique">
                <el-radio
                  v-for="dict in sys_yes_no"
                  :key="dict.value"
                  :label="dict.value"
                >{{dict.label}}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="索引类型(NORMAL普通/UNIQUE唯一/UNION联合)" prop="indexType">
              <el-select v-model="form.indexType" placeholder="请选择索引类型(NORMAL普通/UNIQUE唯一/UNION联合)">
                <el-option
                  v-for="dict in ai_index_type"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="排序" prop="sort">
              <el-input v-model="form.sort" placeholder="请输入排序" />
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

<script setup name="Column">
import { listColumn, getColumn, delColumn, addColumn, updateColumn } from "@/api/ai/column"

const { proxy } = getCurrentInstance()
const { ai_index_type, sys_yes_no, ai_field_type } = useDict('ai_index_type', 'sys_yes_no', 'ai_field_type')

const columnList = ref([])
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
    tableId: undefined,
    columnName: undefined,
    columnComment: undefined,
    columnType: undefined,
    columnLength: undefined,
    nullable: undefined,
    defaultValue: undefined,
    isPk: undefined,
    fkTable: undefined,
    fkColumn: undefined,
    isUnique: undefined,
    indexType: undefined,
    sort: undefined,
  },
  rules: {
    tableId: [
      { required: true, message: "所属表结构ID不能为空", trigger: "blur" }
    ],
    columnName: [
      { required: true, message: "字段名不能为空", trigger: "blur" }
    ],
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询数据库字段定义列表 */
function getList() {
  loading.value = true
  listColumn(queryParams.value).then(response => {
    columnList.value = response.rows
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
    columnId: null,
    tableId: null,
    columnName: null,
    columnComment: null,
    columnType: null,
    columnLength: null,
    nullable: null,
    defaultValue: null,
    isPk: null,
    fkTable: null,
    fkColumn: null,
    isUnique: null,
    indexType: null,
    sort: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
    remark: null
  }
  proxy.resetForm("columnRef")
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
  ids.value = selection.map(item => item.columnId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加数据库字段定义"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _columnId = row.columnId || ids.value
  getColumn(_columnId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改数据库字段定义"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["columnRef"].validate(valid => {
    if (valid) {
      if (form.value.columnId != null) {
        updateColumn(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addColumn(form.value).then(() => {
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
  const _columnIds = row.columnId || ids.value
  proxy.$modal.confirm('是否确认删除数据库字段定义编号为"' + _columnIds + '"的数据项？').then(function() {
    return delColumn(_columnIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('system/column/export', {
    ...queryParams.value
  }, `column_${new Date().getTime()}.xlsx`)
}

getList()
</script>
