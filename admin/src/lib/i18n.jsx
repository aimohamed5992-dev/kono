import { createContext, useContext, useEffect, useMemo, useRef, useState } from 'react'
import { EN } from './strings'

const I18nCtx = createContext(null)

/** Walk the DOM and swap Arabic text for English using the phrase dictionary. */
function translate(root) {
  if (!root) return
  const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT)
  const texts = []
  while (walker.nextNode()) texts.push(walker.currentNode)
  for (const node of texts) {
    const raw = node.nodeValue
    if (!raw) continue
    const key = raw.trim()
    if (key && EN[key] && EN[key] !== key) {
      node.nodeValue = raw.replace(key, EN[key])
    }
  }
  root.querySelectorAll?.('[placeholder],[title],[aria-label]').forEach((el) => {
    for (const attr of ['placeholder', 'title', 'aria-label']) {
      const v = el.getAttribute(attr)
      if (v && EN[v.trim()]) el.setAttribute(attr, EN[v.trim()])
    }
  })
}

export function I18nProvider({ children }) {
  const [lang, setLangState] = useState(() => localStorage.getItem('kono.lang') || 'ar')
  const observer = useRef(null)

  useEffect(() => {
    document.documentElement.lang = lang
    document.documentElement.dir = lang === 'ar' ? 'rtl' : 'ltr'

    observer.current?.disconnect()
    if (lang === 'en') {
      const run = () => translate(document.body)
      run()
      // re-translate after React re-renders
      observer.current = new MutationObserver(() => {
        observer.current.disconnect()
        run()
        observer.current.observe(document.body, { childList: true, subtree: true, characterData: true })
      })
      observer.current.observe(document.body, { childList: true, subtree: true, characterData: true })
      const iv = setInterval(run, 800)
      return () => { clearInterval(iv); observer.current?.disconnect() }
    }
  }, [lang])

  const setLang = (l) => {
    localStorage.setItem('kono.lang', l)
    if (l === 'ar' && lang === 'en') {
      window.location.reload() // simplest way to restore original text
      return
    }
    setLangState(l)
  }
  const toggle = () => setLang(lang === 'ar' ? 'en' : 'ar')

  const t = useMemo(() => (s) => (lang === 'ar' || typeof s !== 'string' ? s : EN[s] ?? EN[s?.trim()] ?? s), [lang])

  const value = useMemo(() => ({ lang, setLang, toggle, t, isRTL: lang === 'ar' }), [lang, t])
  return <I18nCtx.Provider value={value}>{children}</I18nCtx.Provider>
}

export function useI18n() {
  return useContext(I18nCtx) ?? { lang: 'ar', t: (s) => s, toggle: () => {}, isRTL: true }
}
