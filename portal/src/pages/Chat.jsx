import { useEffect, useRef, useState } from 'react'
import { Send, ImagePlus, Phone, MapPin } from 'lucide-react'
import { useThreads } from '../lib/hooks'
import { api, apiError } from '../lib/api'
import { ensureHub, chatHub, joinConversation } from '../lib/chat'
import { useToast } from '../lib/toast'
import { PageHeader, Spinner } from '../components/ui'
import { clsx } from '../components/clsx'

const roleAr = (r) => ({ customer: 'العميل', courier: 'المندوب', merchant: 'المتجر' }[r] || r)

export default function Chat() {
  const { data: threads, isLoading, refetch } = useThreads()
  const [activeId, setActiveId] = useState(null)
  const [conv, setConv] = useState(null)
  const [draft, setDraft] = useState('')
  const [sending, setSending] = useState(false)
  const toast = useToast()
  const fileRef = useRef(null)
  const scrollRef = useRef(null)

  // realtime: subscribe once
  useEffect(() => {
    let mounted = true
    ensureHub().then((c) => {
      c.off('message')
      c.on('message', (msg) => {
        if (!mounted) return
        setConv((cur) => (cur && msg.conversationId === cur.conversationId
          ? { ...cur, messages: [...cur.messages.filter((m) => m.id !== msg.id), msg] }
          : cur))
        refetch()
      })
    })
    return () => { mounted = false; chatHub().off('message') }
  }, [refetch])

  async function openThread(t) {
    setActiveId(t.conversationId)
    const { data } = await api.get(`/chat/${t.conversationId}`)
    setConv(data)
    await api.post(`/chat/${t.conversationId}/read`).catch(() => {})
    joinConversation(t.conversationId)
    refetch()
  }

  useEffect(() => {
    if (scrollRef.current) scrollRef.current.scrollTop = scrollRef.current.scrollHeight
  }, [conv?.messages?.length])

  async function send(body) {
    if (!conv) return
    setSending(true)
    try {
      const { data } = await api.post(`/chat/${conv.conversationId}/messages`, body)
      setConv((cur) => ({ ...cur, messages: [...cur.messages, data] }))
      setDraft('')
    } catch (e) {
      toast.error(apiError(e))
    } finally {
      setSending(false)
    }
  }

  async function onFile(e) {
    const file = e.target.files?.[0]
    if (!file) return
    const reader = new FileReader()
    reader.onload = async () => {
      try {
        const { data } = await api.post('/chat/upload', { dataUri: reader.result, kind: 'image' })
        send({ kind: 'image', mediaUrl: data.url })
      } catch (err) {
        toast.error(apiError(err))
      }
    }
    reader.readAsDataURL(file)
    e.target.value = ''
  }

  async function call() {
    try {
      const { data } = await api.post(`/chat/${conv.conversationId}/call`, { kind: 'voice' })
      window.location.href = `tel:${data.otherPartyPhone}`
    } catch (e) {
      toast.error(apiError(e))
    }
  }

  return (
    <div className="flex h-[calc(100vh-7rem)] flex-col">
      <PageHeader title="المحادثات" subtitle="مراسلة العملاء والمناديب بخصوص الطلبات" />
      <div className="grid flex-1 gap-4 overflow-hidden lg:grid-cols-[320px_1fr]">
        {/* threads */}
        <div className="flex flex-col overflow-y-auto rounded-xl border border-line bg-white">
          {isLoading ? (
            <div className="grid h-40 place-items-center"><Spinner className="size-6" /></div>
          ) : (threads ?? []).length === 0 ? (
            <p className="p-6 text-center text-sm text-ink-soft">لا توجد محادثات بعد</p>
          ) : (
            (threads ?? []).map((t) => (
              <button
                key={t.conversationId}
                onClick={() => openThread(t)}
                className={clsx(
                  'border-b border-line px-4 py-3 text-right transition hover:bg-cream',
                  activeId === t.conversationId && 'bg-brand/10'
                )}
              >
                <div className="flex items-center justify-between">
                  <span className="font-bold text-ink">{t.otherPartyName}</span>
                  {t.unreadCount > 0 && (
                    <span className="rounded-full bg-danger px-1.5 text-xs font-black text-white">{t.unreadCount}</span>
                  )}
                </div>
                <div className="truncate text-xs text-ink-soft">{t.lastMessagePreview || `طلب ${t.orderShortCode}`}</div>
                <div className="mt-0.5 text-[11px] text-ink-soft/70">{roleAr(t.otherPartyRole)} · {t.orderShortCode}</div>
              </button>
            ))
          )}
        </div>

        {/* conversation */}
        <div className="flex flex-col overflow-hidden rounded-xl border border-line bg-canvas">
          {!conv ? (
            <div className="grid flex-1 place-items-center text-sm text-ink-soft">اختر محادثة</div>
          ) : (
            <>
              <div className="flex items-center justify-between border-b border-line bg-white px-4 py-3">
                <div>
                  <div className="font-bold text-ink">{conv.otherPartyName}</div>
                  <div className="text-xs text-ink-soft">{roleAr(conv.otherPartyRole)} · طلب {conv.orderShortCode}</div>
                </div>
                <button onClick={call} className="grid size-9 place-items-center rounded-full bg-brown text-cream">
                  <Phone className="size-4" />
                </button>
              </div>

              <div ref={scrollRef} className="flex-1 space-y-2 overflow-y-auto p-4">
                {conv.messages.map((m) => (
                  <Bubble key={m.id} m={m} />
                ))}
              </div>

              <div className="flex items-center gap-2 border-t border-line bg-white p-3">
                <button onClick={() => fileRef.current?.click()} className="grid size-9 place-items-center rounded-full bg-canvas text-brown">
                  <ImagePlus className="size-4" />
                </button>
                <input ref={fileRef} type="file" accept="image/*" hidden onChange={onFile} />
                <input
                  value={draft}
                  onChange={(e) => setDraft(e.target.value)}
                  onKeyDown={(e) => e.key === 'Enter' && draft.trim() && send({ kind: 'text', text: draft })}
                  placeholder="اكتب رسالة…"
                  className="h-10 flex-1 rounded-full border border-line bg-white px-4 text-sm outline-none focus:border-brown"
                />
                <button
                  disabled={sending || !draft.trim()}
                  onClick={() => send({ kind: 'text', text: draft })}
                  className="grid size-10 place-items-center rounded-full bg-brown text-cream disabled:opacity-40"
                >
                  <Send className="size-4" />
                </button>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  )
}

function Bubble({ m }) {
  const mine = m.mine
  return (
    <div className={clsx('flex', mine ? 'justify-start' : 'justify-end')}>
      <div
        className={clsx(
          'max-w-[70%] rounded-2xl px-3 py-2 text-sm',
          mine ? 'bg-brand text-brown' : 'border border-line bg-white text-ink'
        )}
      >
        {m.kind === 'image' && <img src={m.mediaUrl} alt="" className="mb-1 max-h-56 rounded-lg" />}
        {m.kind === 'voice' && (
          <audio controls src={m.mediaUrl} className="h-9" />
        )}
        {m.kind === 'location' && (
          <a
            className="flex items-center gap-1 font-semibold text-info"
            href={`https://maps.google.com/?q=${m.lat},${m.lng}`}
            target="_blank"
            rel="noreferrer"
          >
            <MapPin className="size-4" /> موقع مُشارَك
          </a>
        )}
        {m.text && <div className="whitespace-pre-wrap">{m.text}</div>}
        <div className={clsx('mt-0.5 text-[10px]', mine ? 'text-brown/60' : 'text-ink-soft')}>
          {new Date(m.createdAt).toLocaleTimeString('ar-SA', { hour: '2-digit', minute: '2-digit' })}
          {mine && (m.read ? ' ✓✓' : ' ✓')}
        </div>
      </div>
    </div>
  )
}
