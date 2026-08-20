// ============================================================
// Git 数据前端内存缓存（模块级，同会话内避免重复请求）
// 说明：后端已有 Redis 缓存（git:contributors/commits/branches/heatmap，
//       TTL 300s，仓库增删改时自动清理）——但每次进入页面仍会发 HTTP 请求。
//       本层缓存让「同一次会话内反复进入页面 / 切换仓库 / 翻页」命中内存，零请求。
// 注意：模块级 Map，刷新浏览器即失效（不同会话靠后端 Redis 兜底）。
// ============================================================
const CACHE_TTL = 5 * 60 * 1000 // 与后端 Redis TTL 保持一致
const store = new Map()

/** 读取缓存，命中且未过期返回数据，否则返回 null（过期项顺手清理） */
export function gitCacheGet(key) {
  const hit = store.get(key)
  if (hit && Date.now() - hit.t < CACHE_TTL) return hit.data
  if (hit) store.delete(key)
  return null
}

/** 写入缓存 */
export function gitCacheSet(key, data) {
  store.set(key, { t: Date.now(), data })
}

/** 按前缀批量失效（如 'repo:'、'commits:123'），用于仓库增删改后主动刷新 */
export function gitCacheClear(prefix) {
  for (const k of Array.from(store.keys())) {
    if (k.startsWith(prefix)) store.delete(k)
  }
}

/** 清空全部 Git 缓存（一般用于退出登录等场景，预留） */
export function gitCacheClearAll() {
  store.clear()
}
