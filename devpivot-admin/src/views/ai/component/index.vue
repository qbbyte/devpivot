<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="所属页面ID" prop="pageId">
        <el-input
          v-model="queryParams.pageId"
          placeholder="请输入所属页面ID"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="组件类型" prop="compType">
        <el-select v-model="queryParams.compType" placeholder="请选择组件类型" clearable>
          <el-option
            v-for="dict in ai_comp_type"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="组件显示名称" prop="compName">
        <el-input
          v-model="queryParams.compName"
          placeholder="请输入组件显示名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="绑定字段名" prop="fieldName">
        <el-input
          v-model="queryParams.fieldName"
          placeholder="请输入绑定字段名"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="字段类型" prop="fieldType">
        <el-select v-model="queryParams.fieldType" placeholder="请选择字段类型" clearable>
          <el-option
            v-for="dict in ai_field_type"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="是否必填" prop="required">
        <el-select v-model="queryParams.required" placeholder="请选择是否必填" clearable>
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
      <el-form-item label="栅格宽度" prop="widthSpan">
        <el-input
          v-model="queryParams.widthSpan"
          placeholder="请输入栅格宽度"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="父组件ID" prop="parentId">
        <el-input
          v-model="queryParams.parentId"
          placeholder="请输入父组件ID"
          clearable
          @keyup.enter="handleQuery"
        />
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
          v-hasPermi="['system:component:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['system:component:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['system:component:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['system:component:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="componentList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="组件ID" align="center" prop="compId" />
      <el-table-column label="所属页面ID" align="center" prop="pageId" />
      <el-table-column label="组件类型" align="center" prop="compType">
        <template #default="scope">
          <dict-tag :options="ai_comp_type" :value="scope.row.compType"/>
        </template>
      </el-table-column>
      <el-table-column label="组件显示名称" align="center" prop="compName" />
      <el-table-column label="绑定字段名" align="center" prop="fieldName" />
      <el-table-column label="字段类型" align="center" prop="fieldType">
        <template #default="scope">
          <dict-tag :options="ai_field_type" :value="scope.row.fieldType"/>
        </template>
      </el-table-column>
      <el-table-column label="是否必填" align="center" prop="required">
        <template #default="scope">
          <dict-tag :options="sys_yes_no" :value="scope.row.required"/>
        </template>
      </el-table-column>
      <el-table-column label="默认值" align="center" prop="defaultValue" />
      <el-table-column label="校验规则" align="center" prop="validateRule" width="120" show-overflow-tooltip />
      <el-table-column label="栅格宽度" align="center" prop="widthSpan" />
      <el-table-column label="业务说明" align="center" prop="bizDesc" show-overflow-tooltip />
      <el-table-column label="交互说明" align="center" prop="interactDesc" show-overflow-tooltip />
      <el-table-column label="父组件ID" align="center" prop="parentId" />
      <el-table-column label="排序" align="center" prop="sort" />
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:component:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:component:remove']">删除</el-button>
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

    <!-- 添加或修改原型组件清单对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="componentRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="所属页面ID" prop="pageId">
              <el-input v-model="form.pageId" placeholder="请输入所属页面ID" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="组件类型" prop="compType">
              <el-select v-model="form.compType" placeholder="请选择组件类型">
                <el-option
                  v-for="dict in ai_comp_type"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="组件显示名称" prop="compName">
              <el-input v-model="form.compName" placeholder="请输入组件显示名称" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="绑定字段名" prop="fieldName">
              <el-input v-model="form.fieldName" placeholder="请输入绑定字段名" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="字段类型" prop="fieldType">
              <el-select v-model="form.fieldType" placeholder="请选择字段类型">
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
            <el-form-item label="是否必填" prop="required">
              <el-radio-group v-model="form.required">
                <el-radio
                  v-for="dict in sys_yes_no"
                  :key="dict.value"
                  :value="dict.value"
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
            <el-form-item label="栅格宽度" prop="widthSpan">
              <el-input v-model="form.widthSpan" placeholder="请输入栅格宽度" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="业务说明" prop="bizDesc">
              <el-input v-model="form.bizDesc" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="交互说明" prop="interactDesc">
              <el-input v-model="form.interactDesc" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="父组件ID" prop="parentId">
              <el-input v-model="form.parentId" placeholder="请输入父组件ID" />
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

<script setup name="Component">
import { listComponent, getComponent, delComponent, addComponent, updateComponent } from "@/api/ai/component"

const { proxy } = getCurrentInstance()
const { ai_comp_type, sys_yes_no, ai_field_type } = useDict('ai_comp_type', 'sys_yes_no', 'ai_field_type')

const componentList = ref([])
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
    pageId: undefined,
    compType: undefined,
    compName: undefined,
    fieldName: undefined,
    fieldType: undefined,
    required: undefined,
    defaultValue: undefined,
    validateRule: undefined,
    widthSpan: undefined,
    bizDesc: undefined,
    interactDesc: undefined,
    parentId: undefined,
    sort: undefined,
  },
  rules: {
    pageId: [
      { required: true, message: "所属页面ID不能为空", trigger: "blur" }
    ],
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询原型组件清单列表 */
function getList() {
  loading.value = true
  listComponent(queryParams.value).then(response => {
    componentList.value = response.rows
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
    compId: null,
    pageId: null,
    compType: null,
    compName: null,
    fieldName: null,
    fieldType: null,
    required: null,
    defaultValue: null,
    validateRule: null,
    widthSpan: null,
    bizDesc: null,
    interactDesc: null,
    parentId: null,
    sort: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
    remark: null
  }
  proxy.resetForm("componentRef")
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
  ids.value = selection.map(item => item.compId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加原型组件清单"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _compId = row.compId || ids.value
  getComponent(_compId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改原型组件清单"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["componentRef"].validate(valid => {
    if (valid) {
      if (form.value.compId != null) {
        updateComponent(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addComponent(form.value).then(() => {
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
  const _compIds = row.compId || ids.value
  proxy.$modal.confirm('是否确认删除原型组件清单编号为"' + _compIds + '"的数据项？').then(function() {
    return delComponent(_compIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('system/component/export', {
    ...queryParams.value
  }, `component_${new Date().getTime()}.xlsx`)
}

getList()
</script>
