import { Client } from '@stomp/stompjs'
import { getToken } from '@/utils/auth'

/**
 * 团队讨论区 WebSocket 客户端(原生 WebSocket + STOMP)
 *
 * - 全页面共享单条连接；按 teamId 订阅 /topic/team/{teamId}(新消息) 与
 *   /topic/team/{teamId}/read(已读事件)。
 * - 断线自动重连(STOMP 内置)，重连后自动重订阅。
 * - 鉴权 token 通过 STOMP CONNECT 帧的 Authorization 头下发(服务端 TeamWsAuthInterceptor 校验)。
 */

let client = null
let connectPromise = null
let pendingResolve = null
// teamId -> { onMessage, onRead, msgSub, readSub }
const subs = new Map()
// 标记当前 client 是否已进入销毁流程，用于抑制销毁/重连竞态期间服务端回送的噪声 ERROR 帧
let disposed = false
// 标记是否处于断线→重连过渡期(后端重启/网络抖动触发)，过渡期内的 ERROR 帧属良性噪声
let transitional = false
// 重连成功后的业务回调（由 team.vue 注册）：用于从 DB 重新同步当前团队消息，
// 补偿"连接断开期间推送被内存 broker 丢弃"导致的丢失，避免必须手动刷新整页。
let onReconnectCb = null

// HMR：模块热替换时先干净断开旧连接，避免孤儿 WebSocket 连接与重连竞态
if (import.meta.hot) {
  import.meta.hot.dispose(() => disconnectWs())
}

function buildWsUrl() {
  const baseApi = import.meta.env.VITE_APP_BASE_API || ''
  const wsProto = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  // 绝对地址(如 http(s)://host/api) -> 取其 host + path
  if (/^https?:\/\//.test(baseApi)) {
    const u = new URL(baseApi)
    const path = u.pathname.replace(/\/$/, '')
    return `${u.protocol === 'https:' ? 'wss:' : 'ws:'}//${u.host}${path}/ws/team`
  }
  // 相对前缀(如 /dev-api) -> 用当前页面源拼接
  const path = baseApi.replace(/\/$/, '')
  return `${wsProto}//${window.location.host}${path}/ws/team`
}

function buildClient() {
  disposed = false
  transitional = false
  client = new Client({
    brokerURL: buildWsUrl(),
    reconnectDelay: 3000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    onConnect: () => {
      transitional = false
      if (disposed) return
      const r = pendingResolve
      pendingResolve = null
      connectPromise = null
      if (r) r()
      resubscribeAll()
      // 重连/首连成功后，通知业务层从 DB 重新同步当前团队消息，
      // 补偿断连窗口内被内存 broker 丢弃的推送，无需手动刷新整页。
      if (onReconnectCb && !disposed) {
        try { onReconnectCb() } catch (e) { /* 业务回调异常不影响 WS 心跳 */ }
      }
    },
    onStompError: (frame) => {
      const msg = frame.headers?.message || frame.body || ''
      // 良性噪声：连接销毁/重连过渡期，或服务端在重启窗口内拒绝入站帧(ExecutorSubscribableChannel/clientInboundChannel)。
      // stomp.js 会自动重连并由 onConnect 重建订阅，消息收发不受影响；HMR 销毁/重连过渡期一律静音，不刷屏。
      // 真正的鉴权/越权错误(WebSocket 鉴权失败 / 无权订阅)不在良性范围，仍按 error 输出以便排查。
      const benign = disposed || transitional || /ExecutorSubscribableChannel|clientInboundChannel|Failed to send message/.test(msg)
      if (benign) return
      console.error('[teamWs] STOMP 错误:', msg)
    },
    onWebSocketError: (event) => {
      // 底层 WebSocket 连接错误（握手失败 / 后端未起 / 代理 WS 升级失败）。
      // 销毁/重连过渡期静音，避免 HMR 或重连抖动刷屏；其余情况打印便于定位。
      if (disposed || transitional) return
      console.warn('[teamWs] WebSocket 底层连接错误:', event?.message || event)
    },
    onWebSocketClose: () => {
      // 连接断开进入过渡期：订阅引用失效，待重连 onConnect 时重建；过渡期内 ERROR 帧视为良性噪声
      transitional = true
    }
  })
}

function resubscribeAll() {
  if (!client || !client.connected) return
  for (const [teamId, cbs] of subs) {
    cbs.msgSub = client.subscribe('/topic/team/' + teamId, (frame) => {
      try { cbs.onMessage(JSON.parse(frame.body)) } catch (e) { console.error('[teamWs] 消息解析失败', e) }
    })
    cbs.readSub = client.subscribe('/topic/team/' + teamId + '/read', (frame) => {
      try { cbs.onRead(JSON.parse(frame.body)) } catch (e) { console.error('[teamWs] 已读事件解析失败', e) }
    })
  }
}

/** 确保客户端已激活并返回连接就绪的 Promise */
function activate() {
  if (client && client.connected) return Promise.resolve()
  if (connectPromise) return connectPromise
  if (!client) buildClient()
  // 每次激活前刷新 token（避免 token 过期后用旧值重连）
  client.connectHeaders = { Authorization: 'Bearer ' + (getToken() || '') }
  connectPromise = new Promise((resolve) => {
    pendingResolve = resolve
    client.activate()
  })
  return connectPromise
}

/**
 * 订阅某团队的实时消息与已读事件
 * @param {number|string} teamId
 * @param {(msg:object)=>void} onMessage 新消息回调
 * @param {(ev:object)=>void} onRead 已读事件回调
 */
/**
 * 注册"重连成功后"的业务回调（如从 DB 重新同步当前团队消息）。
 * 用于补偿 WebSocket 断连期间被内存 broker 丢弃的推送，避免必须手动刷新整页。
 * @param {()=>void} cb
 */
export function setOnReconnect(cb) {
  onReconnectCb = typeof cb === 'function' ? cb : null
}

export async function subscribeTeam(teamId, onMessage, onRead) {
  await activate()
  if (disposed) return
  if (subs.has(teamId)) return
  const cbs = { onMessage, onRead, msgSub: null, readSub: null }
  cbs.msgSub = client.subscribe('/topic/team/' + teamId, (frame) => {
    try { onMessage(JSON.parse(frame.body)) } catch (e) { console.error('[teamWs] 消息解析失败', e) }
  })
  cbs.readSub = client.subscribe('/topic/team/' + teamId + '/read', (frame) => {
    try { onRead(JSON.parse(frame.body)) } catch (e) { console.error('[teamWs] 已读事件解析失败', e) }
  })
  subs.set(teamId, cbs)
}

/** 取消订阅某团队（切换团队或离开时调用） */
export function unsubscribeTeam(teamId) {
  const cbs = subs.get(teamId)
  if (cbs) {
    if (cbs.msgSub) cbs.msgSub.unsubscribe()
    if (cbs.readSub) cbs.readSub.unsubscribe()
    subs.delete(teamId)
  }
}

/** 断开并清理（页面卸载时调用） */
export function disconnectWs() {
  disposed = true
  if (client && client.active) {
    client.deactivate()
  }
  client = null
  connectPromise = null
  pendingResolve = null
  subs.clear()
}
