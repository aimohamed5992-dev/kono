import { HubConnectionBuilder, LogLevel } from '@microsoft/signalr'
import { loadTokens } from './api'

/** One shared hub connection for the whole portal session. */
let connection = null
let starting = null

export function chatHub() {
  if (connection) return connection
  connection = new HubConnectionBuilder()
    .withUrl('/hub/chat', { accessTokenFactory: () => loadTokens()?.accessToken ?? '' })
    .withAutomaticReconnect()
    .configureLogging(LogLevel.Warning)
    .build()
  return connection
}

export async function ensureHub() {
  const c = chatHub()
  if (c.state === 'Connected') return c
  starting = starting || c.start()
  try {
    await starting
  } finally {
    starting = null
  }
  return c
}

export async function joinConversation(id) {
  const c = await ensureHub()
  try {
    await c.invoke('JoinConversation', id)
  } catch {
    /* ignore */
  }
}
