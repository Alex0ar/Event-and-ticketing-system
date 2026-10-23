import { Drama, Laugh, Mic2, Music, Presentation, Sparkles, Trophy, type LucideIcon } from 'lucide-react'
import { cn } from '@/lib/cn'

const styles: Record<string, { gradient: string; icon: LucideIcon }> = {
  CONCERT: { gradient: 'from-fuchsia-600 via-purple-600 to-indigo-700', icon: Music },
  FESTIVAL: { gradient: 'from-orange-500 via-pink-500 to-rose-600', icon: Sparkles },
  CONFERENCE: { gradient: 'from-sky-500 via-blue-600 to-indigo-700', icon: Presentation },
  SPORTS: { gradient: 'from-emerald-500 via-teal-600 to-cyan-700', icon: Trophy },
  THEATRE: { gradient: 'from-rose-700 via-red-700 to-amber-700', icon: Drama },
  COMEDY: { gradient: 'from-amber-400 via-orange-500 to-red-500', icon: Laugh },
}
const fallback = { gradient: 'from-slate-600 via-slate-700 to-slate-900', icon: Mic2 }

/** Generated artwork per category, so the catalog looks finished without hosting images. */
export function EventBanner({ category, bannerUrl, className, iconClass = 'size-14' }: {
  category: string; bannerUrl?: string | null; className?: string; iconClass?: string
}) {
  if (bannerUrl) {
    return <img src={bannerUrl} alt="" className={cn('h-full w-full object-cover', className)} />
  }
  const { gradient, icon: Icon } = styles[category] ?? fallback
  return (
    <div className={cn('relative overflow-hidden bg-gradient-to-br', gradient, className)}>
      <div className="absolute -right-6 -top-6 size-32 rounded-full bg-white/10" />
      <div className="absolute -bottom-10 left-6 size-40 rounded-full bg-black/10" />
      <Icon className={cn('absolute bottom-4 right-4 text-white/80', iconClass)} strokeWidth={1.5} />
    </div>
  )
}
