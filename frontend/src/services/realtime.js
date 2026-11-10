import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'
import { getAccessToken } from './api.js'

let client = null
let connected = false
const subs = new Map()

function wsUrl() {
  // Dev (vite proxy) và prod (nginx /ws/) đều dùng cùng origin
  return `${location.protocol}//${location.host}/ws`
}

export function connectRealtime() {
  if (client && connected) return client
  const token = getAccessToken()
  client = new Client({
    webSocketFactory: () => new SockJS(token ? `${wsUrl()}?token=${encodeURIComponent(token)}` : wsUrl()),
    connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
    reconnectDelay: 5000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    onConnect: () => {
      connected = true
      // Đăng ký lại các topic còn dang dở
      subs.forEach(({ topic, cb }, id) => {
        subs.set(id, { topic, cb, sub: client.subscribe(topic, (m) => cb(JSON.parse(m.body))) })
      })
    },
    onDisconnect: () => { connected = false },
    onStompError: () => { connected = false },
  })
  client.activate()
  return client
}

export function subscribe(topic, cb) {
  connectRealtime()
  const id = `${topic}::${Date.now()}::${Math.random().toString(36).slice(2)}`
  const entry = { topic, cb, sub: null }
  // Nếu đã connect thì sub ngay, chưa thì onConnect sẽ sub lại
  const trySub = () => {
    if (client && client.connected) {
      entry.sub = client.subscribe(topic, (m) => cb(JSON.parse(m.body)))
    } else {
      setTimeout(trySub, 500)
    }
  }
  trySub()
  subs.set(id, entry)
  return () => {
    try { entry.sub?.unsubscribe() } catch { /* bỏ qua */ }
    subs.delete(id)
  }
}

export function subscribeOrder(orderId, cb) {
  return subscribe(`/topic/orders/${orderId}`, cb)
}

export function subscribeMerchant(restaurantId, cb) {
  return subscribe(`/topic/merchant/${restaurantId}`, cb)
}
