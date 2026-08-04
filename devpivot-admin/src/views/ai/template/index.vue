<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="模板编码" prop="templateCode">
        <el-input
          v-model="queryParams.templateCode"
          placeholder="请输入模板编码"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="场景类型(CLARIFY/PRD/TECH/DB/CHECK/POLISH)" prop="sceneType">
        <el-select v-model="queryParams.sceneType" placeholder="请选择场景类型(CLARIFY/PRD/TECH/DB/CHECK/POLISH)" clearable>
          <el-option
            v-for="dict in ai_scene_type"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="模板名称" prop="templateName">
        <el-input
          v-model="queryParams.templateName"
          placeholder="请输入模板名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="是否默认(Y/N)" prop="isDefault">
        <el-select v-model="queryParams.isDefault" placeholder="请选择是否默认(Y/N)" clearable>
          <el-option
            v-for="dict in sys_yes_no"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="是否启用(0启用 1停用)" prop="isEnabled">
        <el-select v-model="queryParams.isEnabled" placeholder="请选择是否启用(0启用 1停用)" clearable>
          <el-option
            v-for="dict in sys_normal_disable"
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
          v-hasPermi="['system:template:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['system:template:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['system:template:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['system:template:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="templateList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="模板ID" align="center" prop="templateId" />
      <el-table-column label="模板编码" align="center" prop="templateCode" />
      <el-table-column label="场景类型(CLARIFY/PRD/TECH/DB/CHECK/POLISH)" align="center" prop="sceneType">
        <template #default="scope">
          <dict-tag :options="ai_scene_type" :value="scope.row.sceneType"/>
        </template>
      </el-table-column>
      <el-table-column label="模板名称" align="center" prop="templateName" />
      <el-table-column label="模板内容" align="center" prop="templateContent" />
      <el-table-column label="多模型差异化Prompt(JSON)" align="center" prop="modelSpecific" />
      <el-table-column label="是否默认(Y/N)" align="center" prop="isDefault">
        <template #default="scope">
          <dict-tag :options="sys_yes_no" :value="scope.row.isDefault"/>
        </template>
      </el-table-column>
      <el-table-column label="是否启用(0启用 1停用)" align="center" prop="isEnabled">
        <template #default="scope">
          <dict-tag :options="sys_normal_disable" :value="scope.row.isEnabled"/>
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:template:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:template:remove']">删除</el-button>
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

    <!-- 添加或修改Prompt模板对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="templateRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="模板编码" prop="templateCode">
              <el-input v-model="form.templateCode" placeholder="请输入模板编码" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="场景类型(CLARIFY/PRD/TECH/DB/CHECK/POLISH)" prop="sceneType">
              <el-select v-model="form.sceneType" placeholder="请选择场景类型(CLARIFY/PRD/TECH/DB/CHECK/POLISH)">
                <el-option
                  v-for="dict in ai_scene_type"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="模板名称" prop="templateName">
              <el-input v-model="form.templateName" placeholder="请输入模板名称" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="模板内容">
              <editor v-model="form.templateContent" :min-height="192"/>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="是否默认(Y/N)" prop="isDefault">
              <el-radio-group v-model="form.isDefault">
                <el-radio
                  v-for="dict in sys_yes_no"
                  :key="dict.value"
                  :label="dict.value"
                >{{dict.label}}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="是否启用(0启用 1停用)" prop="isEnabled">
              <el-radio-group v-model="form.isEnabled">
                <el-radio
                  v-for="dict in sys_normal_disable"
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

<script setup name="Template">
import { listTemplate, getTemplate, delTemplate, addTemplate, updateTemplate } from "@/api/ai/template"

const { proxy } = getCurrentInstance()
const { sys_yes_no, ai_scene_type, sys_normal_disable } = useDict('sys_yes_no', 'ai_scene_type', 'sys_normal_disable')

const templateList = ref([])
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
    templateCode: undefined,
    sceneType: undefined,
    templateName: undefined,
    templateContent: undefined,
    modelSpecific: undefined,
    isDefault: undefined,
    isEnabled: undefined,
  },
  rules: {
    templateCode: [
      { required: true, message: "模板编码不能为空", trigger: "blur" }
    ],
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询Prompt模板列表 */
function getList() {
  loading.value = true
  listTemplate(queryParams.value).then(response => {
    templateList.value = response.rows
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
    templateId: null,
    templateCode: null,
    sceneType: null,
    templateName: null,
    templateContent: null,
    modelSpecific: null,
    isDefault: null,
    isEnabled: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
    remark: null
  }
  proxy.resetForm("templateRef")
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
  ids.value = selection.map(item => item.templateId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加Prompt模板"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _templateId = row.templateId || ids.value
  getTemplate(_templateId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改Prompt模板"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["templateRef"].validate(valid => {
    if (valid) {
      if (form.value.templateId != null) {
        updateTemplate(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addTemplate(form.value).then(() => {
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
  const _templateIds = row.templateId || ids.value
  proxy.$modal.confirm('是否确认删除Prompt模板编号为"' + _templateIds + '"的数据项？').then(function() {
    return delTemplate(_templateIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('system/template/export', {
    ...queryParams.value
  }, `template_${new Date().getTime()}.xlsx`)
}

getList()
</script>
