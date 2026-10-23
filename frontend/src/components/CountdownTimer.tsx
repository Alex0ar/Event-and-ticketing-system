import { useEffect, useState } from 'react'
import { Timer } from 'lucide-react'
import { cn } from '@/lib/cn'

/** Counts down to `expiresAt`; calls onExpire once when it reaches zero. */
export function CountdownTimer({ expiresAt, onExpire }: { expiresAt: string; onExpire?: () => void }) {
  const target = new Date(expiresAt).getTime()
  const [remaining, setRemaining] = useState(() => Math.max(0, target - Date.now()))

  useEffect(() => {
    const id = setInterval(() => {
      const left = Math.max(0, target - Date.now())
      setRemaining(left)
      if (left === 0) {
        clearInterval(id)
        onExpire?.()
      }
    }, 250)
    return () => clearInterval(id)
  }, [target, onExpire])

  const totalSeconds = Math.ceil(remaining / 1000)
  const mm = String(Math.floor(totalSeconds / 60)).padStart(2, '0')
  const ss = String(totalSeconds % 60).padStart(2, '0')
  const urgent = totalSeconds <= 60

  return (
    <div className={cn(
      'inline-flex items-center gap-2 rounded-xl px-4 py-2 font-mono text-2xl font-bold tabular-nums ring-1',
      urgent ? 'bg-rose-50 text-rose-700 ring-rose-200' : 'bg-brand-50 text-brand-700 ring-brand-100',
    )}>
      <Timer className="size-5" /> {mm}:{ss}
    </div>
  )
}
