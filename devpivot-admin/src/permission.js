import router from './router'
import { ElMessage } from 'element-plus'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { getToken } from '@/utils/auth'
import { isHttp, isPathMatch } from '@/utils/validate'
import { isRelogin } from '@/utils/request'
import useUserStore from '@/store/modules/user'
import useLockStore from '@/store/modules/lock'
import useSettingsStore from '@/store/modules/settings'
import usePermissionStore from '@/store/modules/permission'

NProgress.configure({ showSpinner: false })

const whiteList = ['/login', '/register']

const isWhiteList = (path) => {
  return whiteList.some(pattern => isPathMatch(pattern, path))
}

/**
 * 递归保证动态路由 name 全局唯一：
 * 若与已注册路由（含前端常量路由 Index/Profile/Data 等）重名，则自动重命名并告警。
 * 正常情况后端 getRouteName 已用 menuId 生成唯一名，此处仅作兜底防御。
 * @param {Object} route 路由对象（含 children）
 */
function ensureUniqueRouteName(route) {
  if (route && route.name) {
    if (router.hasRoute(route.name)) {
      const renamed = `${route.name}__${route.path || Date.now()}`
      console.warn(
        `[路由冲突] 动态路由 name="${route.name}" 与已注册路由重名，` +
        `已自动重命名为 "${renamed}" 以避免被静默覆盖（原路由可能被挤掉）。` +
        `请检查 sys_menu 配置或后端 SysMenuServiceImpl.getRouteName 生成规则。`
      )
      route.name = renamed
    }
  }
  if (route && Array.isArray(route.children)) {
    route.children.forEach(ensureUniqueRouteName)
  }
}

router.beforeEach(async (to, from) => {
  NProgress.start()
  if (getToken()) {
    to.meta.title && useSettingsStore().setTitle(to.meta.title)
    const isLock = useLockStore().isLock
    if (to.path === '/login') {
      NProgress.done()
      return { path: '/' }
    }
    if (isWhiteList(to.path)) {
      return true
    }
    if (isLock && to.path !== '/lock') {
      NProgress.done()
      return { path: '/lock' }
    }
    if (!isLock && to.path === '/lock') {
      NProgress.done()
      return { path: '/' }
    }
    if (useUserStore().roles.length === 0) {
      isRelogin.show = true
      try {
        // 拉取user_info信息
        await useUserStore().getInfo()
        isRelogin.show = false
        // 根据roles权限生成可访问的路由
        const accessRoutes = await usePermissionStore().generateRoutes()
        accessRoutes.forEach(route => {
          if (!isHttp(route.path)) {
            // 防御：动态路由的 name 若与已注册路由（含前端常量路由如 Index/Profile/Data）重名，
            // vue-router 会以「后者覆盖前者」的方式静默顶掉原路由导致 404。
            // 递归检测并自动重命名，同时告警以便从 sys_menu / 后端 getRouteName 根因修复。
            ensureUniqueRouteName(route)
            router.addRoute(route)
          }
        })
        // 重新导航到目标路由，确保动态路由已注册
        return { ...to, replace: true }
      } catch (err) {
        await useUserStore().logOut()
        ElMessage.error(err)
        return { path: '/' }
      }
    }
    return true
  } else {
    // 没有token
    if (isWhiteList(to.path)) {
      // 在免登录白名单，直接进入
      return true
    }
    NProgress.done()
    return `/login?redirect=${to.fullPath}` // 否则全部重定向到登录页
  }
})

router.afterEach(() => {
  NProgress.done()
})
