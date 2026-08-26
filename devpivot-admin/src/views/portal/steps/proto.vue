<template>
  <div class="project-page">
    <header class="project-header">
      <div class="header-left">
        <button class="back-link" @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          <span>返回</span>
        </button>
        <span class="header-divider"></span>
        <span class="header-title">{{ project.projectName || '原型设计' }}</span>
      </div>
      <div class="header-right">
        <HistoryEntry
          ref="historyEntryRef"
          :project-id="projectId"
          stage="PROTO"
          :snapshot="pages"
          @restored="onHistoryRestored"
        />
        <el-radio-group v-model="currentDevice" size="small" class="device-select" :disabled="readOnly" @change="onDeviceChange">
          <el-radio-button v-for="d in DEVICE_OPTIONS" :key="d.value" :value="d.value">{{ d.label }}</el-radio-button>
        </el-radio-group>
        <el-select v-if="isMobile" v-model="currentModel" size="small" class="model-select" style="width:150px" :disabled="readOnly">
          <el-option v-for="m in DEVICE_MODELS" :key="m.value" :label="m.label" :value="m.value" />
          <el-option label="自定义尺寸" value="custom" />
        </el-select>
        <el-select v-model="chatModelCode" size="small" class="model-select" :disabled="readOnly" @change="onSelectModel">
          <el-option v-for="m in modelOptions" :key="m.value" :label="m.label" :value="m.value" />
        </el-select>
        <template v-if="!readOnly">
          <el-button class="save-btn" @click="handleSave">
            <el-icon><DocumentChecked /></el-icon><span>保存草稿</span>
          </el-button>
          <el-button class="gen-btn" type="primary" plain :loading="generating" @click="onGenerate">
            <el-icon><MagicStick /></el-icon><span>AI 生成原型</span>
          </el-button>
          <el-button type="primary" class="submit-btn" :loading="submitting" @click="handleSubmit">
            <span>确认原型</span>
            <el-icon class="el-icon--right"><ArrowRight /></el-icon>
          </el-button>
        </template>
        <el-tag v-else type="info" effect="plain" size="small" class="ro-tag">
          <el-icon><Lock /></el-icon>
          <span>只读 · 该阶段已完成</span>
        </el-tag>
      </div>
    </header>

    <main class="project-main">
      <div class="project-content">
        <div class="main-grid" :style="{ '--left-width': leftWidth + 'px', '--right-width': rightWidth + 'px' }">
          <!-- 最左图标切换栏（墨刀式） -->
          <nav class="icon-rail">
            <button class="rail-btn" :class="{ active: leftTab === 'page' }" title="页面" @click="setLeftTab('page')"><el-icon><Files /></el-icon></button>
            <button class="rail-btn" :class="{ active: leftTab === 'component' }" title="组件" @click="setLeftTab('component')"><el-icon><Grid /></el-icon></button>
            <button class="rail-btn" :class="{ active: leftTab === 'icon' }" title="图标" @click="setLeftTab('icon')"><el-icon><Picture /></el-icon></button>
            <button class="rail-btn" :class="{ active: leftTab === 'mine' }" title="我的" @click="setLeftTab('mine')"><el-icon><User /></el-icon></button>
          </nav>

          <!-- 左栏：随图标切换的单一面板 -->
          <aside class="sidebar" :style="{ width: leftWidth + 'px' }">
            <section class="panel" v-show="leftTab === 'page'">
              <div class="panel-head">
                <span class="panel-title"><el-icon><Files /></el-icon> 页面</span>
                <el-button text size="small" @click="addPage"><el-icon><Plus /></el-icon></el-button>
              </div>
              <div class="page-list">
                <div
                  v-for="p in pages"
                  :key="p.uid"
                  class="page-item"
                  :class="{ active: p.uid === currentPageUid }"
                  @click="selectPage(p.uid)"
                >
                  <template v-if="editingPageUid === p.uid">
                    <el-input
                      v-model="p.pageName"
                      size="small"
                      @click.stop
                      @blur="editingPageUid = ''"
                      @keyup.enter="editingPageUid = ''"
                    />
                  </template>
                  <template v-else>
                    <span class="page-name" :title="p.pageName">{{ p.pageName }}</span>
                    <el-tag size="small" :type="p.status === '1' ? 'success' : 'info'" effect="plain" class="page-status">
                      {{ p.status === '1' ? '已确认' : '草稿' }}
                    </el-tag>
                    <span class="page-actions" @click.stop>
                      <el-icon class="pa-ico" @click="editingPageUid = p.uid"><Edit /></el-icon>
                      <el-icon class="pa-ico" @click="deletePage(p)"><Delete /></el-icon>
                    </span>
                  </template>
                </div>
                <div v-if="!pages.length" class="empty-hint">尚无页面，点右上「AI 生成原型」或「＋」</div>
              </div>
            </section>

            <section class="panel palette-panel" v-show="leftTab === 'component'">
              <div class="panel-head">
                <span class="panel-title"><el-icon><Grid /></el-icon> 组件库</span>
                <span class="panel-tip">点击或拖入画布</span>
              </div>
              <div class="palette">
                <div v-for="g in PALETTE" :key="g.group" class="palette-group">
                  <div class="palette-group-title" @click="toggleGroup(g.group)">
                    <span class="pg-arrow" :class="{ collapsed: collapsedGroups[g.group] }"></span>
                    {{ g.label }}
                  </div>
                  <div v-show="!collapsedGroups[g.group]" class="palette-items">
                    <div
                      v-for="it in g.items"
                      :key="it.type"
                      class="palette-item"
                      draggable="true"
                      :title="`拖入或点击添加：${it.label}`"
                      @click="addComponent(it)"
                      @dragstart="onPaletteDragStart($event, it)"
                    >
                      <el-icon><component :is="it.icon" /></el-icon>
                      <span>{{ it.label }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </section>

            <!-- 图标面板 -->
            <section class="panel icon-panel" v-show="leftTab === 'icon'">
              <div class="panel-head">
                <span class="panel-title"><el-icon><Picture /></el-icon> 图标</span>
                <span class="panel-tip">点击应用到选中图标组件</span>
              </div>
              <div class="icon-grid">
                <div v-for="ic in iconList" :key="ic" class="icon-cell" :title="ic" @click="pickIcon(ic)">
                  <el-icon><component :is="ic" /></el-icon>
                </div>
              </div>
            </section>

            <!-- 我的面板 -->
            <section class="panel mine-panel" v-show="leftTab === 'mine'">
              <div class="panel-head">
                <span class="panel-title"><el-icon><User /></el-icon> 我的</span>
              </div>
              <div class="empty-hint">暂无收藏组件<br/>（后续支持收藏常用元件）</div>
            </section>
          </aside>

          <!-- 左分隔条 -->
          <div class="resize-divider" @pointerdown="startResizeLeft" title="左右拖动调整左栏宽度"></div>

          <!-- 中栏：画布 -->
          <section class="canvas-section">
            <div class="canvas-toolbar">
              <div class="ct-left">
                <span class="cur-page">{{ currentPage && currentPage.pageName }}</span>
                <span class="comp-count">{{ currentPage ? currentPage.components.length : 0 }} 个组件</span>
              </div>
              <div class="ct-right">
                <el-select v-if="isMobile" v-model="currentModel" size="small" class="toolbar-model" style="width:128px" title="切换机型" :disabled="readOnly">
                  <el-option v-for="m in DEVICE_MODELS" :key="m.value" :label="m.label" :value="m.value" />
                  <el-option label="自定义尺寸" value="custom" />
                </el-select>
                <template v-if="isMobile && currentModel === 'custom'">
                  <el-input-number v-model="customSize.width" :min="200" :max="1400" :controls="false" size="small" style="width:84px" title="宽度" />
                  <span class="size-x">×</span>
                  <el-input-number v-model="customSize.height" :min="300" :max="2400" :controls="false" size="small" style="width:84px" title="高度" />
                </template>
                <div class="zoom-ctrl">
                  <button class="zc-btn" title="缩小" @click="zoomOut">−</button>
                  <span class="zc-val" title="点击适应屏幕" @click="zoomFit">{{ Math.round(canvasScale * 100) }}%</span>
                  <button class="zc-btn" title="放大" @click="zoomIn">＋</button>
                  <button class="zc-btn" :class="{ active: showGrid }" title="网格" @click="toggleGrid">#</button>
                  <button class="zc-btn" title="适应屏幕" @click="zoomFit">适应</button>
                </div>
                <el-radio-group v-model="mode" size="small" :disabled="readOnly">
                  <el-radio-button value="edit"><el-icon><Edit /></el-icon> 编辑</el-radio-button>
                  <el-radio-button value="preview"><el-icon><View /></el-icon> 预览走查</el-radio-button>
                </el-radio-group>
                <el-button size="small" @click="openHistoryPanel"><el-icon><Clock /></el-icon> 历史版本</el-button>
                <el-button size="small" :loading="generating" @click="onGenerate"><el-icon><Refresh /></el-icon> 重新生成</el-button>
              </div>
            </div>

            <!-- 空状态 -->
            <div v-if="!pages.length" class="canvas-empty">
              <el-icon :size="56" color="#c0c4cc"><MagicStick /></el-icon>
              <p class="empty-title">还没有原型</p>
              <p class="empty-sub">点击「AI 生成原型」基于 PRD 一键产出页面与组件</p>
              <el-button type="primary" :loading="generating" @click="onGenerate">
                <el-icon><MagicStick /></el-icon> AI 生成原型
              </el-button>
              <p v-if="genProgress" class="gen-progress">{{ genProgress }}</p>
            </div>

            <!-- 画布 -->
            <div
              v-else
              ref="canvasRef"
              class="canvas-scroll"
              @dragover="onCanvasDragOver"
              @drop="onCanvasDrop"
              @click.self="selectedCompUid = ''"
            >
              <div class="device-shell" :class="{ 'is-mobile': isMobile }" :style="{ zoom: canvasScale, ...(isMobile ? { width: modelObj.width + 'px', borderRadius: modelObj.radius + 'px' } : {}) }">
                <div v-if="!isMobile" class="web-bar">
                  <span class="web-dots"><i></i><i></i><i></i></span>
                  <span class="web-url">{{ project.projectName || '原型' }} · {{ currentPage && currentPage.pageName }}</span>
                </div>
                <div v-if="isMobile" class="phone-statusbar" :class="'notch-' + modelObj.notch">
                  <span class="sb-time">9:41</span>
                  <span v-if="modelObj.notch === 'notch'" class="sb-notch"></span>
                  <span v-else-if="modelObj.notch === 'island'" class="sb-island"></span>
                  <span class="sb-icons"><i></i><i></i><i></i></span>
                </div>
                <div ref="frameRef" class="device-frame" :class="{ 'mobile-frame': isMobile }" :style="isMobile ? { minHeight: modelObj.height + 'px' } : {}">
                <div v-if="!currentPage.components.length" class="canvas-hint">
                  当前页面为空，从左侧「组件库」点击或拖入组件
                </div>
                <div class="canvas-grid" :class="{ 'grid-on': showGrid }">
                  <div
                    v-for="(c, i) in currentPage.components"
                    :key="c.uid"
                    class="comp-wrapper"
                    :class="[
                      { selected: c.uid === selectedCompUid && mode === 'edit' },
                      { 'drop-before': dragFrom !== null && dragOverPos === i },
                      { 'drop-after': dragFrom !== null && dragOverPos === i + 1 }
                    ]"
                    :style="{ gridColumn: 'span ' + c.widthSpan }"
                    :draggable="mode === 'edit'"
                    @click.stop="selectComp(c.uid)"
                    @dragstart="onCompDragStart($event, i)"
                    @dragover="onCompDragOver($event, i)"
                    @drop="onCompDrop($event, i)"
                    @dragend="onCompDragEnd"
                  >
                    <div v-if="mode === 'edit'" class="comp-bar">
                      <div class="comp-meta">
                        <span class="comp-name">{{ c.compName || c.compType }}</span>
                        <span v-if="c.fieldName" class="comp-field">· {{ c.fieldName }}</span>
                        <span v-if="c.required === 'Y'" class="comp-req">必填</span>
                      </div>
                      <div class="comp-tools">
                        <el-icon class="ct-ico" @click.stop="deleteComp(c)"><Delete /></el-icon>
                      </div>
                    </div>
                    <ProtoComponent :comp="c" :preview="mode === 'preview'" :pages="pages" :device-type="currentPage.deviceType || 'WEB'" @navigate="navigate" />
                    <template v-if="mode === 'edit'">
                      <span class="handle h-nw" @pointerdown.stop="onResizeStart($event, c, 'nw')"></span>
                      <span class="handle h-n" @pointerdown.stop="onResizeStart($event, c, 'n')"></span>
                      <span class="handle h-ne" @pointerdown.stop="onResizeStart($event, c, 'ne')"></span>
                      <span class="handle h-e" @pointerdown.stop="onResizeStart($event, c, 'e')"></span>
                      <span class="handle h-se" @pointerdown.stop="onResizeStart($event, c, 'se')"></span>
                      <span class="handle h-s" @pointerdown.stop="onResizeStart($event, c, 's')"></span>
                      <span class="handle h-sw" @pointerdown.stop="onResizeStart($event, c, 'sw')"></span>
                      <span class="handle h-w" @pointerdown.stop="onResizeStart($event, c, 'w')"></span>
                    </template>
                  </div>
                </div>
                <div v-if="isMobile" class="phone-home"></div>
                </div>
              </div>
            </div>
          </section>

          <!-- 右分隔条 -->
          <div class="resize-divider" @pointerdown="startResizeRight" title="左右拖动调整右栏宽度"></div>

          <!-- 右栏：属性 + AI 对话 -->
          <aside class="right-panel" :style="{ width: rightWidth + 'px' }">
            <el-tabs v-model="rightTab" class="right-tabs">
              <el-tab-pane label="属性" name="inspector">
                <div v-if="!selectedComp" class="insp-empty">
                  <el-icon :size="32" color="#c0c4cc"><Setting /></el-icon>
                  <p>在画布中点选一个组件<br/>即可在此编辑属性</p>
                </div>
                <div v-else class="insp-body">
                  <div class="insp-ai-bar">
                    <div class="insp-ai-title"><el-icon><MagicStick /></el-icon> 使用 AI 修改</div>
                    <div class="insp-ai-row">
                      <el-input
                        v-model="compAiInput"
                        type="textarea"
                        :rows="1"
                        resize="none"
                        :disabled="readOnly"
                        placeholder="告诉 AI 如何修改这个组件"
                        @keydown.enter.exact.prevent="sendCompAi"
                      />
                      <el-button type="primary" class="insp-ai-send" :loading="chatGenerating" :disabled="!compAiInput.trim() || chatGenerating || readOnly" @click="sendCompAi">
                        <el-icon><Top /></el-icon>
                      </el-button>
                    </div>
                  </div>

                  <div class="insp-row">
                    <label>组件类型</label>
                    <el-tag size="small" effect="plain">{{ selectedComp.compType }}</el-tag>
                  </div>
                  <div class="insp-row">
                    <label>组件名称</label>
                    <el-input v-model="selectedComp.compName" size="small" />
                  </div>
                  <div class="insp-row">
                    <label>字段名</label>
                    <el-input v-model="selectedComp.fieldName" size="small" placeholder="如 userName" />
                  </div>
                  <div class="insp-row">
                    <label>字段类型</label>
                    <el-select v-model="selectedComp.fieldType" size="small" clearable placeholder="无">
                      <el-option v-for="t in fieldTypes" :key="t" :label="t" :value="t" />
                    </el-select>
                  </div>
                  <div class="insp-row">
                    <label>是否必填</label>
                    <el-switch v-model="selectedComp.required" active-value="Y" inactive-value="N" />
                  </div>
                  <div class="insp-row">
                    <label>栅格宽度 {{ selectedComp.widthSpan }}/12</label>
                    <el-slider v-model="selectedComp.widthSpan" :min="1" :max="12" :marks="spanMarks" />
                  </div>

                  <el-divider>排列</el-divider>
                  <div class="insp-row">
                    <label>尺寸 W × H（px，留空自适应）</label>
                    <div class="insp-two">
                      <el-input-number v-model="selectedComp.style.width" :min="0" :controls="false" size="small" placeholder="宽" />
                      <el-input-number v-model="selectedComp.style.height" :min="0" :controls="false" size="small" placeholder="高" />
                    </div>
                  </div>
                  <div class="insp-row">
                    <label>水平对齐</label>
                    <el-radio-group v-model="selectedComp.style.align" size="small">
                      <el-radio-button value="stretch">撑满</el-radio-button>
                      <el-radio-button value="left">左</el-radio-button>
                      <el-radio-button value="center">中</el-radio-button>
                      <el-radio-button value="right">右</el-radio-button>
                    </el-radio-group>
                  </div>
                  <div class="insp-row">
                    <label>垂直对齐（需设固定高度）</label>
                    <el-radio-group v-model="selectedComp.style.valign" size="small">
                      <el-radio-button value="start">上</el-radio-button>
                      <el-radio-button value="middle">中</el-radio-button>
                      <el-radio-button value="bottom">下</el-radio-button>
                    </el-radio-group>
                  </div>
                  <el-divider>样式</el-divider>
                  <div class="insp-row">
                    <label>旋转 {{ selectedComp.style.rotate }}°</label>
                    <el-slider v-model="selectedComp.style.rotate" :min="-180" :max="180" />
                  </div>
                  <div class="insp-row">
                    <label>不透明度 {{ selectedComp.style.opacity }}%</label>
                    <el-slider v-model="selectedComp.style.opacity" :min="0" :max="100" />
                  </div>
                  <div class="insp-row">
                    <label>圆角 {{ selectedComp.style.borderRadius }} px</label>
                    <el-slider v-model="selectedComp.style.borderRadius" :min="0" :max="50" />
                  </div>
                  <div class="insp-row">
                    <label>填充 / 描边</label>
                    <div class="insp-two">
                      <el-color-picker v-model="selectedComp.style.backgroundColor" />
                      <el-color-picker v-model="selectedComp.style.borderColor" />
                    </div>
                  </div>
                  <div class="insp-row" v-if="selectedComp.style.borderColor">
                    <label>描边宽 {{ selectedComp.style.borderWidth }} px</label>
                    <el-slider v-model="selectedComp.style.borderWidth" :min="0" :max="20" />
                  </div>
                  <div class="insp-row">
                    <label>阴影</label>
                    <el-switch v-model="selectedComp.style.shadowEnabled" />
                  </div>
                  <template v-if="selectedComp.style.shadowEnabled">
                    <div class="insp-row">
                      <label>阴影颜色 / 模糊</label>
                      <div class="insp-two">
                        <el-color-picker v-model="selectedComp.style.shadowColor" />
                        <el-input-number v-model="selectedComp.style.shadowBlur" :min="0" :controls="false" size="small" placeholder="模糊" />
                      </div>
                    </div>
                    <div class="insp-two">
                      <div class="insp-col">
                        <label>X {{ selectedComp.style.shadowX }}</label>
                        <el-slider v-model="selectedComp.style.shadowX" :min="-30" :max="30" />
                      </div>
                      <div class="insp-col">
                        <label>Y {{ selectedComp.style.shadowY }}</label>
                        <el-slider v-model="selectedComp.style.shadowY" :min="-30" :max="30" />
                      </div>
                    </div>
                  </template>
                  <el-divider>交互</el-divider>
                  <div class="insp-row">
                    <label>点击行为</label>
                    <el-select v-model="selectedComp.interaction.action" size="small">
                      <el-option label="无" value="none" />
                      <el-option label="跳转页面" value="navigate" />
                    </el-select>
                  </div>
                  <div class="insp-row" v-if="selectedComp.interaction.action === 'navigate'">
                    <label>跳转目标</label>
                    <el-select v-model="selectedComp.interaction.linkTo" size="small" clearable placeholder="选择页面">
                      <el-option v-for="p in pages" :key="p.uid" :label="p.pageName" :value="p.uid" />
                    </el-select>
                  </div>

                  <el-divider>文本样式</el-divider>
                  <div class="insp-row">
                    <label>字号（px，留空默认）</label>
                    <el-input-number v-model="selectedComp.style.fontSize" :min="10" :max="48" :controls="false" size="small" placeholder="默认" />
                  </div>
                  <div class="insp-row">
                    <label>字重</label>
                    <el-select v-model="selectedComp.style.fontWeight" size="small" clearable placeholder="默认">
                      <el-option label="常规" value="400" />
                      <el-option label="中等" value="500" />
                      <el-option label="加粗" value="600" />
                      <el-option label="特粗" value="700" />
                    </el-select>
                  </div>
                  <div class="insp-row">
                    <label>对齐</label>
                    <el-radio-group v-model="selectedComp.style.textAlign" size="small">
                      <el-radio-button value="left">左</el-radio-button>
                      <el-radio-button value="center">中</el-radio-button>
                      <el-radio-button value="right">右</el-radio-button>
                    </el-radio-group>
                  </div>
                  <div class="insp-row">
                    <label>文本颜色</label>
                    <el-color-picker v-model="selectedComp.style.color" />
                  </div>

                  <template v-for="f in inspectorExtras" :key="f.key">
                    <div class="insp-row insp-field">
                      <label>{{ f.label }}</label>
                      <el-input
                        v-if="f.control === 'text'"
                        :model-value="getVal(f.key)"
                        size="small"
                        @update:model-value="v => setVal(f.key, v)"
                      />
                      <el-input
                        v-else-if="f.control === 'textarea'"
                        type="textarea"
                        :rows="3"
                        :model-value="getVal(f.key)"
                        @update:model-value="v => setVal(f.key, v)"
                      />
                      <el-input-number
                        v-else-if="f.control === 'number'"
                        :model-value="Number(getVal(f.key) != null ? getVal(f.key) : (f.min != null ? f.min : 0))"
                        :min="f.min != null ? f.min : 0"
                        :max="f.max != null ? f.max : 9999"
                        size="small"
                        @update:model-value="v => setVal(f.key, v)"
                      />
                      <el-select
                        v-else-if="f.control === 'select'"
                        :model-value="getVal(f.key)"
                        size="small"
                        @update:model-value="v => setVal(f.key, v)"
                      >
                        <el-option v-for="o in f.options" :key="o" :label="o" :value="o" />
                      </el-select>
                      <el-select
                        v-else-if="f.control === 'page'"
                        :model-value="getVal(f.key)"
                        size="small"
                        clearable
                        placeholder="不跳转"
                        @update:model-value="v => setVal(f.key, v)"
                      >
                        <el-option v-for="p in pages" :key="p.uid" :label="p.pageName" :value="p.uid" />
                      </el-select>
                      <el-input
                        v-else-if="f.control === 'csv'"
                        :model-value="csvDisplay(f.key)"
                        size="small"
                        placeholder="逗号分隔"
                        @update:model-value="v => setCsv(f.key, v)"
                      />
                      <el-select
                        v-else-if="f.control === 'tags'"
                        multiple filterable allow-create default-first-option
                        :model-value="getVal(f.key) || []"
                        size="small"
                        placeholder="可输入新增"
                        @update:model-value="v => setVal(f.key, v)"
                      >
                        <el-option v-for="o in (getVal(f.key) || [])" :key="o" :label="o" :value="o" />
                      </el-select>
                      <el-switch
                        v-else-if="f.control === 'switch'"
                        :model-value="!!getVal(f.key)"
                        @update:model-value="v => setVal(f.key, v)"
                      />
                      <div v-else-if="f.control === 'kv'" class="kv-editor">
                        <div v-for="(row, i) in (getVal(f.key) || [])" :key="i" class="kv-row">
                          <el-input
                            v-for="s in (f.shape || [])" :key="s.k"
                            v-model="row[s.k]"
                            size="small"
                            :placeholder="s.label"
                          />
                          <el-button size="small" text type="danger" @click="removeKv(f.key, i)">
                            <el-icon><Close /></el-icon>
                          </el-button>
                        </div>
                        <el-button size="small" plain class="kv-add" @click="addKv(f.key, f.shape)">+ 添加一项</el-button>
                      </div>
                    </div>
                  </template>

                  <el-divider>说明</el-divider>
                  <div class="insp-row">
                    <label>业务说明</label>
                    <el-input v-model="selectedComp.bizDesc" type="textarea" :rows="2" size="small" placeholder="该组件承载的业务含义" />
                  </div>
                  <div class="insp-row">
                    <label>交互说明</label>
                    <el-input v-model="selectedComp.interactDesc" type="textarea" :rows="2" size="small" placeholder="点击/联动等行为" />
                  </div>
                </div>
              </el-tab-pane>

              <el-tab-pane label="AI 对话" name="chat">
                <div class="chat-body">
                  <div ref="chatScrollRef" class="chat-messages">
                    <div v-for="(m, i) in chatMessages" :key="i" class="chat-msg" :class="m.role">
                      <div class="bubble" v-html="renderMarkdown(m.content)"></div>
                    </div>
                    <div v-if="!chatMessages.length" class="chat-empty">
                      <div class="chat-empty-icon"><el-icon><ChatDotRound /></el-icon></div>
                      <div class="chat-empty-title">AI 原型助手</div>
                      <div class="chat-empty-desc">描述你想调整的原型，AI 会生成或修改当前页面</div>
                    </div>
                  </div>
                  <div class="chat-input">
                    <div v-if="readOnly" class="chat-locked-note">
                      <el-icon><Lock /></el-icon>
                      <span>该阶段已锁定，仅可查看历史对话</span>
                    </div>
                    <div class="chat-input-card">
                      <el-input
                        v-model="chatInput"
                        type="textarea"
                        :rows="2"
                        resize="none"
                        :disabled="chatGenerating || readOnly"
                        placeholder="输入需求，回车发送"
                        @keydown.enter.exact.prevent="sendChat"
                      />
                      <el-button type="primary" class="chat-send-btn" :loading="chatGenerating" :disabled="!chatInput.trim() || chatGenerating || readOnly" @click="sendChat">
                        <el-icon><Top /></el-icon>
                      </el-button>
                    </div>
                  </div>
                </div>
              </el-tab-pane>
            </el-tabs>
          </aside>

        </div>

        <footer class="status-bar">
          <span class="sb-item">缩放 {{ Math.round(canvasScale * 100) }}%</span>
          <span class="sb-item">{{ selectedComp ? ('已选：' + (selectedComp.compName || selectedComp.compType)) : '未选中组件' }}</span>
          <span class="sb-item sb-right">{{ currentDevice }} · 共 {{ pages.length }} 页 · {{ currentPage ? currentPage.components.length : 0 }} 组件</span>
        </footer>

      </div>
    </main>
  </div>
</template>

<script setup name="StepProto">
import { ref, computed, onMounted, getCurrentInstance, nextTick, reactive, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getProject } from '@/api/ai/project'
import { Lock } from '@element-plus/icons-vue'
import {
  PALETTE, uid, buildComponent, generateProto, DEVICE_OPTIONS, DEVICE_MODELS, defaultStyle, ICON_LIST,
  sendProtoChat, getProtoPages, saveProto, confirmProto, getProtoModels,
  applyProtoPatch
} from '@/api/ai/proto'
import ProtoComponent from '@/components/proto/ProtoComponent.vue'
import HistoryEntry from '@/views/portal/components/HistoryEntry.vue'

const { proxy } = getCurrentInstance()
const router = useRouter()
const route = useRoute()
const projectId = computed(() => route.params.id)

const stepOrder = [
  { value: 'REQ', label: '需求采集' },
  { value: 'CLARIFY', label: 'AI 澄清' },
  { value: 'PRD', label: 'PRD 文档' },
  { value: 'PROTO', label: '原型设计' },
  { value: 'ARCH', label: '系统架构' },
  { value: 'TECH', label: '技术方案' },
  { value: 'DB', label: '数据库' },
  { value: 'DONE', label: '完成' }
]

const loading = ref(false)
const submitting = ref(false)
const project = ref({})
const currentStep = ref('PROTO')

const stepIndex = computed(() => stepOrder.findIndex(s => s.value === currentStep.value))
const stepLabel = computed(() => {
  const hit = stepOrder.find(s => s.value === currentStep.value)
  return hit ? hit.label : '未开始'
})

// 阶段已"过去"判定：项目当前阶段在我这一阶之后 → 整页只读锁定
const readOnly = computed(() => {
  const order = ['REQ', 'CLARIFY', 'PRD', 'PROTO', 'ARCH', 'TECH', 'DB', 'DONE']
  const cur = order.indexOf(currentStep.value)
  const mine = order.indexOf('PROTO')
  return cur > mine
})

/* ---------------- 模型选择（复用澄清接口） ---------------- */
const modelOptions = ref([{ value: 'deepseek', label: 'DeepSeek（默认）' }])
const chatModel = ref(modelOptions.value[0])
const chatModelCode = computed({
  get: () => (chatModel.value && chatModel.value.value) || 'deepseek',
  set: (v) => { const m = modelOptions.value.find(o => o.value === v); if (m) chatModel.value = m }
})
function onSelectModel(val) {
  const m = modelOptions.value.find(o => o.value === val)
  if (m) chatModel.value = m
}
function currentModelCode() { return chatModelCode.value || 'deepseek' }
async function loadModels() {
  try {
    const res = await getProtoModels()
    const data = res?.data ?? res
    const list = data?.models || (Array.isArray(data) ? data : [])
    if (Array.isArray(list) && list.length) {
      modelOptions.value = list.map(m => ({ value: m.modelId, label: m.modelName }))
      chatModel.value = modelOptions.value[0]
      return
    }
  } catch (e) { /* 用默认 */ }
  modelOptions.value = [{ value: 'deepseek', label: 'DeepSeek（默认）' }]
  chatModel.value = modelOptions.value[0]
}

/* ---------------- 页面 / 组件状态 ---------------- */
const pages = ref([])
const currentPageUid = ref('')
const selectedCompUid = ref('')
const mode = ref('edit') // 'edit' | 'preview'
const rightTab = ref('inspector')
const collapsedGroups = reactive({})

const currentDevice = ref('WEB') // 当前设备类型：WEB / H5 / MINI（生成默认 + 当前页设备）
const frameRef = ref(null)
const isMobile = computed(() => !!currentPage.value && !!currentPage.value.deviceType && currentPage.value.deviceType !== 'WEB')
const currentModel = ref('iphone-13') // 移动端机型
const customSize = reactive({ width: 390, height: 844 }) // 自定义机型尺寸
const modelObj = computed(() => {
  if (currentModel.value === 'custom') return { value: 'custom', label: '自定义尺寸', width: customSize.width, height: customSize.height, radius: 30, notch: 'none' }
  return DEVICE_MODELS.find(m => m.value === currentModel.value) || DEVICE_MODELS[2]
})
function onDeviceChange(v) {
  currentDevice.value = v
  if (currentPage.value) currentPage.value.deviceType = v
}

const currentPage = computed(() => pages.value.find(p => p.uid === currentPageUid.value) || null)
const selectedComp = computed(() => {
  if (!currentPage.value) return null
  return currentPage.value.components.find(c => c.uid === selectedCompUid.value) || null
})
watch(selectedCompUid, () => { compAiInput.value = '' })

const fieldTypes = ['STRING', 'NUMBER', 'DATE', 'BOOLEAN', 'ENUM', 'JSON']
const spanMarks = { 1: '1', 6: '6', 12: '12' }

/* ---------------- 墨刀式：左栏切换 / 画布缩放 / 图标库 ---------------- */
const leftTab = ref('page') // page | component | icon | mine
const canvasScale = ref(1)
const showGrid = ref(false)
const iconList = ICON_LIST
function setLeftTab(t) { leftTab.value = t }
function zoomIn() { canvasScale.value = Math.min(2, +(canvasScale.value + 0.1).toFixed(2)) }
function zoomOut() { canvasScale.value = Math.max(0.4, +(canvasScale.value - 0.1).toFixed(2)) }
function zoomFit() { canvasScale.value = 1 }
function toggleGrid() { showGrid.value = !showGrid.value }

/* ---------------- 三栏可拖拽宽度 ---------------- */
const LEFT_MIN_WIDTH = 180
const RIGHT_MIN_WIDTH = 420
const MAX_PANEL_WIDTH = 520
const leftWidth = ref(300)
const rightWidth = ref(360)
const dragging = ref(false)
let dragState = null // { side: 'left'|'right', startX: number, startWidth: number, minWidth, targetRef }
function startResizeLeft(e) { startResize(e, 'left', leftWidth, LEFT_MIN_WIDTH) }
function startResizeRight(e) { startResize(e, 'right', rightWidth, RIGHT_MIN_WIDTH) }
function startResize(e, side, targetRef, minWidth) {
  e.preventDefault()
  dragState = { side, startX: e.clientX, startWidth: targetRef.value, minWidth, targetRef }
  dragging.value = true
  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
  document.addEventListener('pointermove', onResizeMove)
  document.addEventListener('pointerup', onResizeEnd)
  document.addEventListener('pointercancel', onResizeEnd)
}
function onResizeMove(e) {
  if (!dragState) return
  const { side, startX, startWidth, minWidth, targetRef } = dragState
  const dx = side === 'left' ? e.clientX - startX : startX - e.clientX
  const next = Math.max(minWidth, Math.min(MAX_PANEL_WIDTH, startWidth + dx))
  targetRef.value = next
}
function onResizeEnd() {
  dragState = null
  dragging.value = false
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
  document.removeEventListener('pointermove', onResizeMove)
  document.removeEventListener('pointerup', onResizeEnd)
  document.removeEventListener('pointercancel', onResizeEnd)
}

function pickIcon(name) {
  if (selectedComp.value && selectedComp.value.type === 'icon') {
    selectedComp.value.props.name = name
    return
  }
  if (!currentPage.value) addPage()
  const c = buildComponent(findPalette('icon'), { props: { name } })
  currentPage.value.components.push(c)
  selectedCompUid.value = c.uid
  rightTab.value = 'inspector'
}

/* ---------------- 加载 / 生成 ---------------- */
async function loadPages() {
  // 从后端读取已存原型（权威数据源）
  let backendPages = null
  try {
    backendPages = await getProtoPages(projectId.value)
  } catch (e) {
    backendPages = null
  }
  if (backendPages && backendPages.length) {
    pages.value = backendPages
    currentPageUid.value = backendPages[0].uid
    currentDevice.value = backendPages[0]?.deviceType || 'WEB'
    // 机型 / 自定义尺寸为前端 UI 偏好，后端未存储，使用默认偏好
    currentModel.value = 'iphone-13'
    customSize.width = 390
    customSize.height = 844
  } else {
    pages.value = []
    currentPageUid.value = ''
  }
}

// 机型 / 设备类型 / 自定义尺寸变化自动持久化进草稿
watch([currentModel, currentDevice, customSize], () => {
  if (readOnly.value) return
  if (pages.value && pages.value.length) {
    saveProto(projectId.value, pages.value, '人工')
  }
})

// 只读时强制预览模式：隐藏编辑栏 / 拖拽柄 / 选中框等可写交互
watch(readOnly, (ro) => { if (ro) mode.value = 'preview' })

const generating = ref(false)
const genProgress = ref('')
function onGenerate() {
  if (generating.value) return
  const regen = pages.value.length > 0
  const doGen = () => {
    pages.value = []
    currentPageUid.value = ''
    selectedCompUid.value = ''
    generating.value = true
    genProgress.value = '正在初始化生成…'
    generateProto(
      {
        projectId: projectId.value,
        projectName: project.value.projectName,
        industryType: project.value.industryType,
        targetUser: project.value.targetUser,
        model: currentModelCode(),
        deviceType: currentDevice.value
      },
      {
        onProgress: (t) => { genProgress.value = t },
        onPage: (page) => {
          pages.value.push(page)
          if (!currentPageUid.value) currentPageUid.value = page.uid
        },
        onDone: () => {
          generating.value = false
          genProgress.value = ''
          saveProto(projectId.value, pages.value, '人工')
          proxy.$modal.msgSuccess('原型已生成并保存')
        },
        onError: () => { generating.value = false; genProgress.value = '' }
      }
    )
  }
  if (regen) {
    proxy.$modal.confirm('重新生成将覆盖当前所有页面与组件，确定继续？').then(doGen).catch(() => {})
  } else {
    doGen()
  }
}

/* ---------------- 页面操作 ---------------- */
function selectPage(uid) {
  currentPageUid.value = uid
  selectedCompUid.value = ''
  const p = pages.value.find(x => x.uid === uid)
  if (p) currentDevice.value = p.deviceType || 'WEB'
}
function addPage() {
  const p = { uid: uid('p'), pageName: `新页面 ${pages.value.length + 1}`, pageDesc: '', status: '0', deviceType: currentDevice.value, components: [] }
  pages.value.push(p)
  currentPageUid.value = p.uid
}
function deletePage(p) {
  proxy.$modal.confirm(`确认删除页面「${p.pageName}」？`).then(() => {
    const i = pages.value.findIndex(x => x.uid === p.uid)
    if (i >= 0) pages.value.splice(i, 1)
    if (currentPageUid.value === p.uid) currentPageUid.value = pages.value[0]?.uid || ''
  }).catch(() => {})
}
const editingPageUid = ref('')

/* ---------------- 组件添加 / 选择 / 删除 ---------------- */
function toggleGroup(group) {
  collapsedGroups[group] = !collapsedGroups[group]
}
function addComponent(item) {
  if (!currentPage.value) addPage()
  const mobile = currentPage.value.deviceType && currentPage.value.deviceType !== 'WEB'
  const c = buildComponent(item, mobile ? { widthSpan: 12 } : {})
  currentPage.value.components.push(c)
  selectedCompUid.value = c.uid
  rightTab.value = 'inspector'
}
function selectComp(uid) { selectedCompUid.value = uid; rightTab.value = 'inspector' }
function deleteComp(c) {
  const arr = currentPage.value.components
  const i = arr.findIndex(x => x.uid === c.uid)
  if (i >= 0) arr.splice(i, 1)
  if (selectedCompUid.value === c.uid) selectedCompUid.value = ''
}

/* ---------------- 拖拽：组件库 → 画布 ---------------- */
const dragPaletteType = ref('')
function onPaletteDragStart(e, item) {
  dragPaletteType.value = item.type
  e.dataTransfer.effectAllowed = 'copy'
  try { e.dataTransfer.setData('text/plain', item.type) } catch (err) {}
}
function onCanvasDragOver(e) { e.preventDefault(); e.dataTransfer.dropEffect = 'copy' }
function onCanvasDrop(e) {
  e.preventDefault()
  if (dragPaletteType.value) {
    const item = findPaletteItem(dragPaletteType.value)
    if (item) addComponent(item)
    dragPaletteType.value = ''
  }
}
function findPaletteItem(type) {
  for (const g of PALETTE) { const hit = g.items.find(it => it.type === type); if (hit) return hit }
  return null
}

// 兼容别名：部分历史调用点使用了 findPalette，统一指向 findPaletteItem
function findPalette(type) {
  return findPaletteItem(type)
}

/* ---------------- 拖拽：画布内重排 ---------------- */
const dragFrom = ref(null)
const dragOverPos = ref(null)
function onCompDragStart(e, i) {
  dragFrom.value = i
  e.dataTransfer.effectAllowed = 'move'
  try { e.dataTransfer.setData('text/plain', String(i)) } catch (err) {}
}
function onCompDragOver(e, i) {
  if (dragFrom.value === null) return
  e.preventDefault()
  const rect = e.currentTarget.getBoundingClientRect()
  const before = (e.clientX - rect.left) < rect.width / 2
  dragOverPos.value = before ? i : i + 1
}
function onCompDrop(e, i) {
  e.preventDefault()
  if (dragFrom.value === null) return
  const arr = currentPage.value.components
  const from = dragFrom.value
  if (from === i) { dragFrom.value = null; dragOverPos.value = null; return }
  const [moved] = arr.splice(from, 1)
  let target = i > from ? i - 1 : i
  arr.splice(target, 0, moved)
  dragFrom.value = null
  dragOverPos.value = null
}
function onCompDragEnd() { dragFrom.value = null; dragOverPos.value = null }

/* ---------------- 缩放：拖角柄改 widthSpan ---------------- */
function onResizeStart(e, comp, dir) {
  const canvas = frameRef.value
  if (!canvas) return
  const canvasWidth = canvas.clientWidth
  const colWidth = canvasWidth / 12
  const startX = e.clientX
  const startY = e.clientY
  const startSpan = comp.widthSpan
  const startH = Number(comp.style.height) || 0
  const clamp = (v, a, b) => Math.max(a, Math.min(b, v))
  const move = (ev) => {
    const dx = ev.clientX - startX
    const dy = ev.clientY - startY
    const horiz = dir.indexOf('e') >= 0 ? 1 : (dir.indexOf('w') >= 0 ? -1 : 0)
    if (horiz !== 0) {
      comp.widthSpan = clamp(Math.round(startSpan + horiz * dx / colWidth), 1, 12)
    }
    if (dir.indexOf('n') >= 0 || dir.indexOf('s') >= 0) {
      comp.style.height = Math.max(0, Math.round(startH + dy))
    }
  }
  const up = () => {
    window.removeEventListener('pointermove', move)
    window.removeEventListener('pointerup', up)
  }
  window.addEventListener('pointermove', move)
  window.addEventListener('pointerup', up)
}

/* ---------------- 预览走查跳转 ---------------- */
function navigate(targetUid) {
  if (mode.value !== 'preview') return
  currentPageUid.value = targetUid
  selectedCompUid.value = ''
}

/* ---------------- 属性面板：动态扩展字段 ---------------- */
const inspectorExtrasMap = {
  nav: [{ key: 'props.menus', label: '菜单(逗号分隔)', control: 'csv' }],
  input: [{ key: 'props.label', label: '标签', control: 'text' }, { key: 'props.placeholder', label: '占位提示', control: 'text' }],
  select: [{ key: 'props.label', label: '标签', control: 'text' }, { key: 'props.options', label: '选项(逗号分隔)', control: 'csv' }],
  date: [{ key: 'props.label', label: '标签', control: 'text' }],
  switch: [{ key: 'props.label', label: '标签', control: 'text' }],
  number: [{ key: 'props.label', label: '标签', control: 'text' }, { key: 'props.placeholder', label: '占位提示', control: 'text' }],
  textarea: [{ key: 'props.label', label: '标签', control: 'text' }, { key: 'props.rows', label: '行数', control: 'number' }],
  submit: [{ key: 'props.text', label: '按钮文字', control: 'text' }],
  table: [{ key: 'props.columns', label: '列(逗号分隔)', control: 'csv' }, { key: 'props.rows', label: '示例行数', control: 'number' }],
  list: [{ key: 'props.items', label: '项(逗号分隔)', control: 'csv' }],
  card: [{ key: 'props.title', label: '标题', control: 'text' }, { key: 'props.desc', label: '描述', control: 'text' }],
  chart: [{ key: 'props.chartType', label: '图表类型', control: 'text' }],
  text: [{ key: 'props.text', label: '文本内容', control: 'textarea' }],
  image: [{ key: 'props.ratio', label: '比例', control: 'select', options: ['16:9', '4:3', '1:1'] }],
  button: [
    { key: 'props.text', label: '按钮文字', control: 'text' },
    { key: 'props.type', label: '样式', control: 'select', options: ['default', 'primary', 'success', 'warning', 'danger', 'info'] }
  ],
  icon: [{ key: 'props.name', label: '图标名', control: 'select', options: ['Star', 'Setting', 'User', 'Search', 'Bell'] }],
  container: [{ key: 'props.title', label: '标题', control: 'text' }, { key: 'props.columns', label: '分栏数', control: 'number' }],
  divider: [{ key: 'props.text', label: '文字(可选)', control: 'text' }]
}
const inspectorExtras = computed(() => {
  if (!selectedComp.value) return []
  const item = findPalette(selectedComp.value.type)
  if (item && item.fields && item.fields.length) return item.fields
  return inspectorExtrasMap[selectedComp.value.type] || []
})
function getVal(key) {
  if (!selectedComp.value) return ''
  return key.split('.').reduce((o, k) => (o == null ? o : o[k]), selectedComp.value)
}
function setVal(key, val) {
  if (!selectedComp.value) return
  const parts = key.split('.')
  const last = parts.pop()
  const obj = parts.reduce((o, k) => (o[k] = o[k] || {}), selectedComp.value)
  obj[last] = val
}
function csvDisplay(key) {
  const v = getVal(key)
  return Array.isArray(v) ? v.join(', ') : ''
}
function setCsv(key, val) {
  const arr = String(val).split(',').map(s => s.trim()).filter(Boolean)
  setVal(key, arr)
}
/* kv：对象数组编辑（如描述列表/折叠面板），按 shape 定义子字段 */
function getArr(key) {
  let v = getVal(key)
  if (!Array.isArray(v)) { v = []; setVal(key, v) }
  return v
}
function addKv(key, shape) {
  const arr = getArr(key)
  const obj = {}
  ;(shape || []).forEach(s => { obj[s.k] = '' })
  arr.push(obj)
}
function removeKv(key, i) {
  const arr = getArr(key)
  if (i >= 0 && i < arr.length) arr.splice(i, 1)
}

/* ---------------- 保存 / 提交 ---------------- */
function handleSave() {
  // 后端落库：成功才算真正保存，失败明确提示（避免「以为存了其实没存」）
  saveProto(projectId.value, pages.value, '人工')
    .then(() => { proxy.$modal.msgSuccess('已保存到服务器') })
    .catch(() => { proxy.$modal.msgError('保存失败，请稍后重试') })
}
function handleSubmit() {
  const total = pages.value.reduce((s, p) => s + p.components.length, 0)
  if (!pages.value.length || total === 0) {
    proxy.$modal.msgWarning('请先生成/添加至少一个页面与组件')
    return
  }
  submitting.value = true
  const nextStep = stepOrder[stepIndex.value + 1]?.value || 'DONE'
  // 标记所有页面为已确认
  pages.value.forEach(p => { p.status = '1' })
  // 后端：先保存最新页面/组件，再确认推进 step=TECH
  saveProto(projectId.value, pages.value, '人工')
    .then(() => confirmProto(projectId.value))
    .then(() => updateProject({ projectId: projectId.value, step: nextStep }))
    .then(() => {
      proxy.$modal.msgSuccess('已提交')
      submitting.value = false
      router.push('/portal')
    })
    .catch(() => {
      submitting.value = false
      proxy.$modal.msgError('提交失败：原型保存到服务器出错，请重试')
    })
}

/* ---------------- AI 对话（流式） ---------------- */
const chatMessages = ref([])
const chatInput = ref('')
const compAiInput = ref('')
const chatGenerating = ref(false)
const chatScrollRef = ref(null)
let chatController = null

/* 历史记录入口（Header 头像组 + 抽屉双 Tab；旧版本 dialog 已下线，统一走 HistoryPanel） */
const historyEntryRef = ref(null)
function openHistoryPanel() {
  historyEntryRef.value && historyEntryRef.value.open()
}
/* 版本还原完成：HistoryPanel 已写回业务表，此处刷新画布展示 */
function onHistoryRestored(payload) {
  if (!payload || !payload.content) { proxy.$modal.msgWarning('还原内容为空'); return }
  try {
    const pagesArr = JSON.parse(payload.content)
    if (!Array.isArray(pagesArr)) { proxy.$modal.msgWarning('还原内容格式不正确'); return }
    pages.value = pagesArr
    currentPageUid.value = pages.value.length ? pages.value[0].uid : ''
    selectedCompUid.value = ''
    if (payload.artifactType) currentDevice.value = payload.artifactType
    proxy.$modal.msgSuccess('已还原版本')
  } catch (e) {
    proxy.$modal.msgWarning('还原内容解析失败')
  }
}

function renderMarkdown(text) {
  // 轻量渲染：换行 + 加粗，足够预览
  return String(text || '')
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\n/g, '<br/>')
}
function scrollChat() {
  nextTick(() => {
    if (chatScrollRef.value) chatScrollRef.value.scrollTop = chatScrollRef.value.scrollHeight
  })
}
function sendChat() {
  const msg = chatInput.value.trim()
  if (!msg || chatGenerating.value) return
  chatInput.value = ''

  chatMessages.value.push({ role: 'user', content: msg })
  chatGenerating.value = true
  const assistant = { role: 'assistant', content: '' }
  chatMessages.value.push(assistant)
  scrollChat()
  const ctrl = sendProtoChat(
    { projectId: projectId.value, message: msg, pages: pages.value, model: currentModelCode() },
    {
      onChunk: (t) => { assistant.content = t; scrollChat() },
      onDone: () => { chatGenerating.value = false; saveProto(projectId.value, pages.value, '人工') },
      onError: () => { chatGenerating.value = false }
    }
  )
  chatController = ctrl
}

function sendCompAi() {
  const comp = selectedComp.value
  if (!comp || readOnly.value) return
  const raw = compAiInput.value.trim()
  if (!raw || chatGenerating.value) return
  const instruction = `请修改当前页面中名为「${comp.compName}」的 ${comp.compType} 组件（类型：${comp.type || '未知'}）：${raw}`
  compAiInput.value = ''
  rightTab.value = 'chat'
  chatGenerating.value = true
  chatMessages.value.push({ role: 'user', content: `修改「${comp.compName}」：${raw}` })
  const assistant = { role: 'assistant', content: '（正在应用修改，请稍候…）' }
  chatMessages.value.push(assistant)
  scrollChat()
  let replacedOnce = false
  const ctrl = applyProtoPatch(
    { projectId: projectId.value, instruction, pages: pages.value, model: currentModelCode() },
    {
      onProgress: (t) => { assistant.content = t; scrollChat() },
      onPage: (page) => {
        if (!replacedOnce) { pages.value = []; replacedOnce = true }
        pages.value.push(page)
        if (!currentPageUid.value) currentPageUid.value = page.uid
      },
      onDone: () => {
        chatGenerating.value = false
        replacedOnce = false
        saveProto(projectId.value, pages.value, '人工')
        proxy.$modal.msgSuccess('已应用 AI 修改')
      },
      onError: () => { chatGenerating.value = false; replacedOnce = false }
    }
  )
  chatController = ctrl
}

/* ---------------- 初始化 ---------------- */
function getProjectInfo() {
  loading.value = true
  getProject(projectId.value).then(response => {
    project.value = response.data
    currentStep.value = response.data.step || 'PROTO'
    loading.value = false
  }).catch(() => { loading.value = false })
}
function goBack() { router.push('/portal') }

const canvasRef = ref(null)

onMounted(() => {
  getProjectInfo()
  loadModels()
  loadPages()
})
</script>

<style scoped>
.project-page { height: 100vh; overflow: hidden; background: #f7f8fa; display: flex; flex-direction: column; }
.project-header { position: sticky; top: 0; z-index: 30; display: flex; align-items: center; justify-content: space-between; height: 52px; padding: 0 24px; background: rgba(255,255,255,0.92); backdrop-filter: blur(8px); border-bottom: 1px solid #ebedf0; }
.header-left { display: flex; align-items: center; gap: 12px; }
.back-link { display: inline-flex; align-items: center; gap: 4px; border: none; background: none; color: #646a73; font-size: 13px; cursor: pointer; padding: 4px 8px; border-radius: 6px; transition: .2s; }
.back-link:hover { color: #3370ff; background: rgba(51,112,255,.06); }
.header-divider { width: 1px; height: 16px; background: #e5e7eb; }
.header-title { font-size: 14px; font-weight: 600; color: #1f2329; }
.header-right { display: flex; align-items: center; gap: 10px; }
.model-select { width: 150px; }
.save-btn { border-radius: 8px; color: #646a73; border-color: #dee0e3; }
.save-btn:hover { color: #3370ff; border-color: #3370ff; }
.gen-btn { border-radius: 8px; }

.project-main { flex: 1; min-height: 0; padding: 0; margin: 0; overflow: hidden; position: relative; }
.project-content { height: 100%; width: 100%; margin: 0; padding: 0; max-width: none; display: flex; flex-direction: column; }
.main-grid { display: flex; flex-direction: row; flex: 1; min-height: 0; align-items: stretch; }

/* 左栏 */
.sidebar { position: relative; top: 0; display: flex; flex-direction: column; gap: 0; height: 100%; max-height: none; overflow: hidden; background: #fff; border-right: 1px solid #ebedf0; flex-shrink: 0; }
.resize-divider { width: 8px; margin: 0 -2px; flex-shrink: 0; cursor: col-resize; background: transparent; transition: background .15s; position: relative; z-index: 10; touch-action: none; user-select: none; }
.resize-divider:hover, .resize-divider:active { background: rgba(51,112,255,.22); }
.resize-divider::after { content: ''; position: absolute; top: 0; bottom: 0; left: 50%; width: 1px; background: #ebedf0; transform: translateX(-50%); pointer-events: none; }
.resize-divider:hover::after { background: #3370ff; }
.panel { background: #fff; border-radius: 0; box-shadow: none; border-bottom: 1px solid #f2f3f5; display: flex; flex-direction: column; min-height: 0; }
.panel-head { display: flex; align-items: center; justify-content: space-between; padding: 14px 16px; border-bottom: 1px solid #f5f6f8; }
.panel-title { display: flex; align-items: center; gap: 6px; font-size: 14px; font-weight: 600; color: #1f2329; }
.panel-tip { font-size: 11px; color: #a8abb2; }
.page-list { padding: 8px; overflow-y: auto; max-height: 260px; }
.page-item { display: flex; align-items: center; gap: 8px; padding: 9px 10px; border-radius: 8px; cursor: pointer; transition: .15s; }
.page-item:hover { background: #f2f3f5; }
.page-item.active { background: rgba(51,112,255,.08); }
.page-name { flex: 1; font-size: 13px; color: #1f2329; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.page-status { transform: scale(.85); }
.page-actions { display: none; gap: 6px; }
.page-item:hover .page-actions { display: flex; }
.pa-ico { color: #8a919f; cursor: pointer; font-size: 14px; }
.pa-ico:hover { color: #3370ff; }
.empty-hint { padding: 16px; font-size: 12px; color: #a8abb2; text-align: center; line-height: 1.6; }

.palette-panel { flex: 1; min-height: 0; }
.palette { padding: 12px 10px 18px; overflow-y: auto; }
.palette-group { margin-bottom: 14px; }
.palette-group:first-child { margin-top: 2px; }
.palette-group-title { display: flex; align-items: center; font-size: 12px; font-weight: 600; color: #4e5969; margin-bottom: 8px; padding: 2px 0; cursor: pointer; user-select: none; }
.pg-arrow { display: inline-block; width: 0; height: 0; border-left: 4px solid transparent; border-right: 4px solid transparent; border-top: 5px solid #86909c; margin-right: 6px; transition: transform .15s; }
.pg-arrow.collapsed { transform: rotate(-90deg); }
.palette-items { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; }
.palette-item { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 3px; height: 64px; padding: 6px 4px; border: 1px solid #ebedf0; border-radius: 8px; font-size: 11px; color: #4e5969; cursor: grab; background: #fff; transition: all .15s ease; user-select: none; text-align: center; line-height: 1.3; }
.palette-item .el-icon { font-size: 18px; color: #646a73; transition: color .15s; }
.palette-item:hover { border-color: #3370ff; color: #3370ff; background: rgba(51,112,255,.04); box-shadow: 0 4px 12px rgba(51,112,255,.1); transform: translateY(-1px); }
.palette-item:hover .el-icon { color: #3370ff; }
.palette-item:active { cursor: grabbing; transform: translateY(0); box-shadow: 0 2px 6px rgba(51,112,255,.06); }

/* 中栏画布 */
.canvas-section { background: #fff; border-radius: 0; box-shadow: none; display: flex; flex-direction: column; height: 100%; min-height: 0; min-width: 0; flex: 1 1 0; }
.canvas-toolbar { display: flex; align-items: center; justify-content: space-between; padding: 12px 16px; border-bottom: 1px solid #f5f6f8; }
.ct-left { display: flex; align-items: center; gap: 12px; }
.cur-page { font-size: 14px; font-weight: 600; color: #1f2329; }
.comp-count { font-size: 12px; color: #a8abb2; }
.ct-right { display: flex; align-items: center; gap: 10px; }

.canvas-empty { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px; padding: 40px; text-align: center; }
.empty-title { font-size: 16px; font-weight: 600; color: #4e5969; margin: 0; }
.empty-sub { font-size: 13px; color: #a8abb2; margin: 0; }
.gen-progress { font-size: 12px; color: #3370ff; }

.canvas-scroll { flex: 1; padding: 16px; overflow: hidden; background: #fff; }
.device-frame { max-width: none; margin: 0; background: #fff; border-radius: 0; box-shadow: none; padding: 0; min-height: 100%; }
.device-select { margin-right: 2px; }
.device-shell { max-width: none; margin: 0; }
.device-shell.is-mobile { margin: 0 auto; border: 10px solid #1f2329; overflow: hidden; box-shadow: 0 12px 40px rgba(0,0,0,.18); background: #1f2329; display: flex; flex-direction: column; max-height: calc(100% - 32px); }
.phone-statusbar { display: flex; align-items: center; justify-content: space-between; height: 30px; background: #1f2329; color: #fff; padding: 0 20px; font-size: 12px; }
.sb-notch { width: 90px; height: 6px; background: #3a3f47; border-radius: 4px; }
.sb-island { width: 92px; height: 26px; background: #000; border-radius: 14px; }
.sb-icons { display: flex; align-items: center; gap: 5px; }
.sb-icons i { width: 6px; height: 6px; border-radius: 50%; background: #fff; display: inline-block; }
.mobile-frame { max-width: none; border-radius: 0; box-shadow: none; padding: 12px; flex: 1; min-height: 0; overflow-y: auto; background: #fff; }
.canvas-hint { color: #a8abb2; font-size: 13px; text-align: center; padding: 60px 0; border: 1px dashed #d9dde3; border-radius: 10px; }
.canvas-grid { display: grid; grid-template-columns: repeat(12, 1fr); gap: 14px; align-items: start; }
.comp-wrapper { position: relative; border: 1px solid transparent; border-radius: 10px; padding: 10px; transition: border-color .15s, box-shadow .15s; cursor: pointer; }
.comp-wrapper:hover { border-color: #c9d3e0; }
.comp-wrapper.selected { border-color: #3370ff; box-shadow: 0 0 0 2px rgba(51,112,255,.15); }
.comp-bar { position: absolute; top: -26px; left: 0; right: 0; display: flex; align-items: center; justify-content: space-between; opacity: 0; transition: opacity .15s; z-index: 2; padding: 0 2px; font-size: 12px; pointer-events: none; }
.comp-wrapper:hover .comp-bar, .comp-wrapper.selected .comp-bar { opacity: 1; }
.comp-meta { display: flex; align-items: center; gap: 6px; background: #3370ff; color: #fff; border-radius: 6px; padding: 3px 8px; pointer-events: auto; }
.comp-name { font-weight: 600; }
.comp-field { opacity: .9; font-family: monospace; }
.comp-req { opacity: .95; font-size: 11px; border: 1px solid rgba(255,255,255,.6); border-radius: 4px; padding: 0 4px; }
.comp-tools { display: flex; align-items: center; gap: 4px; background: #3370ff; border-radius: 6px; padding: 3px 5px; pointer-events: auto; }
.comp-wrapper:hover .comp-tools, .comp-wrapper.selected .comp-tools { display: flex; }
.ct-ico { color: #fff; font-size: 13px; cursor: pointer; }
.resize-handle { position: absolute; right: 2px; bottom: 2px; width: 14px; height: 14px; cursor: nwse-resize; background: linear-gradient(135deg, transparent 50%, #3370ff 50%, #3370ff 60%, transparent 60%, transparent 70%, #3370ff 70%); border-radius: 0 0 6px 0; opacity: 0; }
.comp-wrapper:hover .resize-handle, .comp-wrapper.selected .resize-handle { opacity: 1; }
/* 拖拽重排插入提示：用伪元素绝对定位，不占网格空间，避免强制换行 */
.comp-wrapper.drop-before::before,
.comp-wrapper.drop-after::after {
  content: '';
  position: absolute;
  top: -4px; bottom: -4px;
  width: 3px;
  background: #3370ff;
  border-radius: 2px;
  z-index: 5;
}
.comp-wrapper.drop-before::before { left: -3px; }
.comp-wrapper.drop-after::after { right: -3px; }

/* 右栏 */
.right-panel { position: relative; top: 0; background: #fff; border-radius: 0; box-shadow: none; border-left: 1px solid #ebedf0; height: 100%; display: flex; flex-direction: column; overflow: hidden; flex-shrink: 0; }
.right-tabs { display: flex; flex-direction: column; height: 100%; }
.right-tabs :deep(.el-tabs__header) { margin: 0; padding: 0 12px; }
.right-tabs :deep(.el-tabs__content) { flex: 1; overflow: hidden; }
.right-tabs :deep(.el-tab-pane) { height: 100%; }

.insp-empty { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px; height: 100%; color: #a8abb2; font-size: 13px; text-align: center; padding: 40px; line-height: 1.7; }
.insp-body { padding: 16px; overflow-y: auto; height: 100%; }
.insp-ai-bar { margin: -4px -4px 12px; padding: 12px; background: linear-gradient(135deg, rgba(51,112,255,.06), rgba(51,112,255,.03)); border: 1px solid rgba(51,112,255,.12); border-radius: 10px; }
.insp-ai-title { font-size: 12px; font-weight: 600; color: #3370ff; margin-bottom: 8px; display: flex; align-items: center; gap: 4px; }
.insp-ai-row { display: flex; align-items: flex-end; gap: 8px; }
.insp-ai-row .el-textarea { flex: 1; }
.insp-ai-row .el-textarea__inner { border: 1px solid #d9e1ff; border-radius: 8px; padding: 6px 10px; background: #fff; resize: none; box-shadow: none; font-size: 12px; line-height: 1.6; min-height: 32px; }
.insp-ai-row .el-textarea__inner::placeholder { color: #a8abb2; }
.insp-ai-row .el-textarea__inner:focus { border-color: #3370ff; }
.insp-ai-send { height: 30px; width: 30px; border-radius: 50%; padding: 0; flex-shrink: 0; font-size: 13px; }
.insp-ai-send.is-disabled { opacity: .5; }
.insp-row { display: flex; flex-direction: column; gap: 6px; margin-bottom: 14px; }
.insp-row > label { font-size: 12px; color: #4e5969; }
.insp-two { display: flex; gap: 10px; align-items: center; }
.insp-col { flex: 1; display: flex; flex-direction: column; gap: 6px; min-width: 0; }
.insp-col > label { font-size: 12px; color: #4e5969; }
.insp-two :deep(.el-color-picker), .insp-row :deep(.el-color-picker) { vertical-align: middle; }
.insp-two :deep(.el-input-number), .insp-two :deep(.el-select) { width: 100%; }
.insp-field :deep(.el-select), .insp-field :deep(.el-input), .insp-field :deep(.el-input-number) { width: 100%; }
.kv-editor { display: flex; flex-direction: column; gap: 8px; }
.kv-row { display: flex; gap: 6px; align-items: center; }
.kv-row :deep(.el-input) { flex: 1; min-width: 0; }
.kv-add { align-self: flex-start; }

.chat-body { display: flex; flex-direction: column; height: 100%; background: #fff; }
.chat-messages { flex: 1; overflow-y: auto; padding: 16px; display: flex; flex-direction: column; gap: 14px; }
.chat-empty { margin: auto; padding: 24px; text-align: center; }
.chat-empty-icon { font-size: 40px; color: #c9cdd4; margin-bottom: 12px; }
.chat-empty-title { font-size: 15px; font-weight: 600; color: #1f2329; margin-bottom: 6px; }
.chat-empty-desc { font-size: 12px; color: #86909c; line-height: 1.6; }
.chat-msg { display: flex; }
.chat-msg.user { justify-content: flex-end; }
.chat-msg.assistant { justify-content: flex-start; }
.bubble { max-width: 88%; padding: 10px 12px; border-radius: 10px; font-size: 13px; line-height: 1.7; word-break: break-word; }
.chat-msg.user .bubble { background: #3370ff; color: #fff; border-bottom-right-radius: 2px; }
.chat-msg.assistant .bubble { background: #f2f3f5; color: #1f2329; border-bottom-left-radius: 2px; }
.chat-input { padding: 10px 12px 12px; border-top: 1px solid #f2f3f5; background: #f7f8fa; display: flex; flex-direction: column; gap: 8px; }
.chat-input-card { display: flex; align-items: flex-end; gap: 8px; background: #fff; border: 1px solid #e5e6eb; border-radius: 12px; padding: 8px 10px; box-shadow: 0 1px 4px rgba(0,0,0,.03); }
.chat-input-card .el-textarea { flex: 1; }
.chat-input-card .el-textarea__inner { border: none; padding: 6px 8px; background: transparent; resize: none; box-shadow: none; font-size: 13px; line-height: 1.6; min-height: 36px; }
.chat-input-card .el-textarea__inner::placeholder { color: #a8abb2; }
.chat-send-btn { height: 34px; width: 34px; border-radius: 50%; padding: 0; flex-shrink: 0; font-size: 14px; }
.chat-send-btn.is-disabled { opacity: .5; }
.ver-toolbar { display: flex; align-items: center; gap: 10px; }

/* 最左图标切换栏（墨刀式） */
.icon-rail { width: 54px; flex-shrink: 0; display: flex; flex-direction: column; align-items: center; gap: 8px; padding: 12px 0; background: #fff; border-right: 1px solid #ebedf0; }
.rail-btn { width: 38px; height: 38px; border: none; background: transparent; border-radius: 8px; color: #646a73; cursor: pointer; display: inline-flex; align-items: center; justify-content: center; font-size: 18px; transition: .15s; }
.rail-btn:hover { background: #f2f3f5; color: #3370ff; }
.rail-btn.active { background: rgba(51,112,255,.1); color: #3370ff; }

/* 图标面板 */
.icon-panel { flex: 1; min-height: 0; }
.icon-grid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 8px; padding: 12px; overflow-y: auto; }
.icon-cell { display: flex; align-items: center; justify-content: center; height: 38px; border: 1px solid #ebedf0; border-radius: 8px; color: #4e5969; cursor: pointer; font-size: 18px; transition: .15s; }
.icon-cell:hover { border-color: #3370ff; color: #3370ff; background: rgba(51,112,255,.06); }

/* 画布缩放控制 */
.zoom-ctrl { display: flex; align-items: center; gap: 2px; background: #f2f3f5; border-radius: 8px; padding: 2px; }
.toolbar-model { margin-right: 4px; }
.toolbar-model .el-input__wrapper { box-shadow: 0 0 0 1px #e5e6eb inset; }
.size-x { color: #86909c; font-size: 12px; padding: 0 2px; }
.zc-btn { border: none; background: transparent; color: #4e5969; cursor: pointer; height: 26px; min-width: 26px; border-radius: 6px; font-size: 14px; padding: 0 6px; transition: .15s; }
.zc-btn:hover { background: #e6e8eb; color: #1f2329; }
.zc-btn.active { background: rgba(51,112,255,.15); color: #3370ff; }
.zc-val { font-size: 12px; color: #4e5969; cursor: pointer; min-width: 44px; text-align: center; }

/* 网页地址栏 */
.web-bar { display: flex; align-items: center; gap: 10px; height: 36px; background: #f2f3f5; border-bottom: 1px solid #ebedf0; padding: 0 14px; }
.web-dots { display: flex; gap: 6px; }
.web-dots i { width: 10px; height: 10px; border-radius: 50%; background: #d0d3d9; display: inline-block; }
.web-url { flex: 1; font-size: 12px; color: #646a73; background: #fff; border: 1px solid #e5e7eb; border-radius: 14px; padding: 4px 12px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

/* 移动端 Home Indicator */
.phone-home { width: 120px; height: 5px; background: #c9cdd4; border-radius: 3px; margin: 8px auto 4px; }

/* 网格 */
.canvas-grid.grid-on { background-image: linear-gradient(#eef1f6 1px, transparent 1px), linear-gradient(90deg, #eef1f6 1px, transparent 1px); background-size: 28px 28px; background-position: -1px -1px; }

/* 选中包围框 8 控制点（墨刀式） */
.handle { position: absolute; width: 9px; height: 9px; background: #fff; border: 1.5px solid #3370ff; border-radius: 50%; z-index: 6; opacity: 0; transition: opacity .15s; }
.comp-wrapper:hover .handle, .comp-wrapper.selected .handle { opacity: 1; }
.handle.h-nw { top: -5px; left: -5px; cursor: nwse-resize; }
.handle.h-n  { top: -5px; left: 50%; transform: translateX(-50%); cursor: ns-resize; }
.handle.h-ne { top: -5px; right: -5px; cursor: nesw-resize; }
.handle.h-e  { top: 50%; right: -5px; transform: translateY(-50%); cursor: ew-resize; }
.handle.h-se { bottom: -5px; right: -5px; cursor: nwse-resize; }
.handle.h-s  { bottom: -5px; left: 50%; transform: translateX(-50%); cursor: ns-resize; }
.handle.h-sw { bottom: -5px; left: -5px; cursor: nesw-resize; }
.handle.h-w  { top: 50%; left: -5px; transform: translateY(-50%); cursor: ew-resize; }

/* 底部状态栏 */
.status-bar { height: 28px; display: flex; align-items: center; gap: 18px; padding: 0 16px; background: #fff; border-top: 1px solid #ebedf0; font-size: 12px; color: #646a73; }
.status-bar .sb-item.sb-right { margin-left: auto; }

.submit-btn { border-radius: 6px; padding: 8px 16px; font-size: 13px; font-weight: 500; }

@media (max-width: 1200px) { .main-grid { flex-wrap: wrap; height: auto; } .sidebar, .right-panel { position: static; height: auto; max-height: none; width: 100% !important; } .resize-divider { display: none; } .canvas-section { width: 100%; order: -1; } }

/* 只读锁定态（阶段已过去） */
.ro-tag { display:inline-flex; align-items:center; gap:6px; height:auto; padding:5px 12px; font-size:13px; font-weight:500; color:#3370ff; white-space:nowrap; vertical-align:middle; background:linear-gradient(180deg,#f5f9ff 0%,#eef4ff 100%); border:1px solid #c5d9ff; border-radius:20px; box-shadow:0 1px 2px rgba(51,112,255,0.06); }
.ro-tag .el-icon { display:inline-flex; align-items:center; justify-content:center; width:18px; height:18px; padding:0; border-radius:50%; background:#3370ff; color:#fff; flex-shrink:0; }
.ro-tag .el-icon svg { width:12px; height:12px; }
.chat-locked-note { display:inline-flex; align-items:center; gap:8px; padding:10px 14px; font-size:13px; font-weight:500; color:#3370ff; white-space:nowrap; vertical-align:middle; background:linear-gradient(180deg,#f5f9ff 0%,#eef4ff 100%); border:1px solid #c5d9ff; border-radius:20px; box-shadow:0 1px 2px rgba(51,112,255,0.06); }
.chat-locked-note .el-icon { display:inline-flex; align-items:center; justify-content:center; width:18px; height:18px; padding:0; border-radius:50%; background:#3370ff; color:#fff; flex-shrink:0; }
.chat-locked-note .el-icon svg { width:12px; height:12px; }
</style>
