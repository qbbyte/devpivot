// P0 修复逻辑级实测：用新版 getRouteName(M{id}) 重建路由树并断言首页不被覆盖
import { createRouter, createMemoryHistory } from 'vue-router'
import { readFileSync } from 'node:fs'

const MENU_ROOT_ID = 0
const TYPE_DIR = 'M'
const TYPE_MENU = 'C'
const NO_FRAME = '1'

// ---- 读取真实菜单数据 ----
const rows = readFileSync('D:/mine-project/devPivot/.workbuddy/tmp/p0_menus.tsv', 'utf8').trim().split('\n')
  .map(l => l.split('\t'))
  .map(([menuId, parentId, path, component, menuType, isFrame, routeName, visible, status]) => ({
    menuId: Number(menuId), parentId: Number(parentId), path, component, menuType, isFrame, routeName, visible, status
  }))
  .filter(r => r.menuType === 'M' || r.menuType === 'C') // 仅 M/C 参与路由重建，F 为按钮不计

// ---- 复刻后端 SysMenuServiceImpl 的关键方法（新版 getRouteName）----
const isHttp = (s) => /^https?:\/\//.test(s || '')
function isMenuFrame(m) {
  return m.parentId === MENU_ROOT_ID && m.menuType === TYPE_MENU && String(m.isFrame) === NO_FRAME
}
function isInnerLink(m) { return isHttp(m.path) }
// ===== 新版：显式 route_name 优先，否则 M{menuId} =====
function getRouteName(m) {
  if (isMenuFrame(m)) return ''
  if (m.routeName && m.routeName.trim() !== '') return m.routeName
  return 'M' + m.menuId
}
function getRouterPath(m) {
  let p = m.path
  if (m.parentId !== MENU_ROOT_ID && isInnerLink(m)) {
    // innerLinkReplaceEach 简化：去掉 http(s)://
    p = (m.path || '').replace(/^https?:\/\//, '')
  } else if (m.parentId === MENU_ROOT_ID && m.menuType === TYPE_DIR && String(m.isFrame) === NO_FRAME) {
    p = '/' + m.path
  } else if (isMenuFrame(m)) {
    p = '/'
  }
  return p
}

// ---- 简化 buildMenus：复刻后端路径拼接（父目录补 /，叶子相对）----
function buildMenus(list) {
  const byParent = new Map()
  list.forEach(m => {
    if (!byParent.has(m.parentId)) byParent.set(m.parentId, [])
    byParent.get(m.parentId).push(m)
  })
  const build = (pid, parentPath) => {
    const children = byParent.get(pid) || []
    return children.map(m => {
      let p = getRouterPath(m)
      // 复刻 filterChildren：子路由 path 拼到父路径后（相对写法）
      if (parentPath) p = parentPath + '/' + p.replace(/^\/+/, '')
      else if (!p.startsWith('/')) p = '/' + p
      const router = { path: p, name: getRouteName(m), meta: { title: m.path } }
      if (m.menuType === TYPE_DIR && byParent.has(m.menuId)) {
        router.children = build(m.menuId, p)
      }
      return router
    })
  }
  return build(MENU_ROOT_ID, '')
}

const dynamicRoutes = buildMenus(rows).filter(r => !isHttp(r.path))

// ---- 常量路由（与 src/router/index.js 一致的关键项）----
const Layout = { render: () => null }
const constantRoutes = [
  { path: '/:pathMatch(.*)*', component: {}, hidden: true },        // 404 catch-all
  { path: '', component: Layout, redirect: '/portal', children: [
    { path: '/index', component: {}, name: 'Index', meta: { title: '首页' } }
  ]},
  { path: '/user', component: Layout, hidden: true, redirect: 'noredirect', children: [
    { path: 'profile/:activeTab?', component: {}, name: 'Profile', meta: { title: '个人中心' } }
  ]}
]

// ---- 注入真实 vue-router 并注册 ----
const router = createRouter({ history: createMemoryHistory(), routes: constantRoutes })
let collisionWarnings = 0
function registerWithDefense(routes) {
  routes.forEach(r => {
    if (!isHttp(r.path)) {
      // 复刻前端 ensureUniqueRouteName 防御
      if (r.name && router.hasRoute(r.name)) {
        collisionWarnings++
        r.name = `${r.name}__${r.path}`
      }
      if (r.children) registerWithDefense(r.children)
      router.addRoute(r)
    }
  })
}
registerWithDefense(dynamicRoutes)

// ---- 断言 ----
const ri = router.resolve('/index')
const matchedNames = ri.matched.map(r => r.name)
const okHome = ri.matched.some(r => r.path === '/index' && r.name === 'Index')

// 收集所有动态路由 name，检查是否均为 M{数字} 或显式 route_name
const allDynNames = []
;(function collect(rs){ rs.forEach(r=>{ if(r.name) allDynNames.push(r.name); if(r.children) collect(r.children) }) })(dynamicRoutes)
const badNames = allDynNames.filter(n => !/^M\d+$/.test(n) && !(rows.some(m => m.routeName === n)))

console.log('=== P0 修复逻辑级实测 ===')
console.log('动态路由总数(顶层):', dynamicRoutes.length)
console.log('动态路由生成的 name 示例:', allDynNames.slice(0, 8).join(', '), '...')
console.log('非 M{id}/非显式route_name 的异常 name:', badNames.length === 0 ? '无 ✓' : badNames)
console.log('前端防御触发告警次数:', collisionWarnings, '(应为 0，说明后端已根除冲突)')
console.log('resolve(/index) 匹配链 name:', JSON.stringify(matchedNames))
console.log('首页 /index 仍解析到常量 Index 路由:', okHome ? '✓ 通过' : '✗ 失败(被覆盖)')

// 额外：模拟「若 KB 菜单 path 又被误改回 index」的回归场景，验证前端防御兜底
const kb = rows.find(m => m.path === 'list' && m.component === 'ai/kb/index')
if (kb) {
  const rogue = [{ path: '/kb', name: '', children: [{ path: '/kb/index', name: 'Index', meta: {} }] }]
  const r2 = createRouter({ history: createMemoryHistory(), routes: constantRoutes })
  let warns = 0
  ;(function reg(rs){ rs.forEach(r=>{ if(r.name && r2.hasRoute(r.name)){warns++; r.name=r.name+'__'+r.path;} if(r.children) reg(r.children); r2.addRoute(r) }) })(rogue)
  const r2i = r2.resolve('/index')
  console.log('\n=== 回归兜底测试：KB 菜单 path 误改回 index ===')
  console.log('前端防御告警:', warns, warns>0?'✓ 已拦截':'✗ 未拦截')
  console.log('首页 /index 仍可用:', r2i.matched.some(r=>r.name==='Index') ? '✓' : '✗')
}

process.exit(okHome ? 0 : 1)
