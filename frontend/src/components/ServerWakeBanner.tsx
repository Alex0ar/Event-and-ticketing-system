import { useEffect, useState } from 'react'
import { Loader2 } from 'lucide-react'
import { API_URL } from '@/api/client'

/**
 * The demo backend runs on a free tier that sleeps when idle. Ping /actuator/health on load and,
 * if it's slow to answer, tell the visitor what's happening instead of showing a broken page.
 */
export function ServerWakeBanner() {
  const [state, setState] = useState<'checking' | 'waking' | 'up'>('checking')

  useEffect(() => {
    let done = false
    const slow = setTimeout(() => { if (!done) setState('waking') }, 1500)
    const ping = async () => {
      for (let attempt = 0; attempt < 40 && !done; attempt++) {
        try {
          const res = await fetch(`${API_URL}/actuator/health`, { cache: 'no-store' })
          if (res.ok) { done = true; setState('up'); return }
        } catch { /* still waking */ }
        await new Promise((r) => setTimeout(r, 3000))
      }
    }
    ping()
    return () => { done = true; clearTimeout(slow) }
  }, [])

  if (state !== 'waking') return null
  return (
    <div className="bg-amber-50 text-amber-900 ring-1 ring-amber-200">
      <div className="mx-auto flex max-w-7xl items-center gap-3 px-4 py-2.5 text-sm sm:px-6">
        <Loader2 className="size-4 shrink-0 animate-spin" />
        <span><strong>Waking up the demo server…</strong> It runs on free hosting and sleeps when idle — this takes up to a minute, once.</span>
      </div>
    </div>
  )
}
