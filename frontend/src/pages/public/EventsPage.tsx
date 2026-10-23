import { useState, type FormEvent } from 'react'
import { useSearchParams } from 'react-router'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { ChevronLeft, ChevronRight, Search, SlidersHorizontal, X } from 'lucide-react'
import { eventsApi } from '@/api/events'
import type { EventSearchParams } from '@/api/types'
import { Container } from '@/components/layout/AppLayout'
import { EventCard } from '@/components/EventCard'
import { CATEGORIES } from '@/lib/categories'
import { Button } from '@/components/ui/Button'
import { Input, Select } from '@/components/ui/Field'
import { EmptyState } from '@/components/ui/EmptyState'
import { ErrorAlert } from '@/components/ui/ErrorAlert'
import { PageHeader } from '@/components/ui/PageHeader'
import { Spinner } from '@/components/ui/Spinner'
import { titleCase } from '@/lib/format'

const FILTER_KEYS = ['query', 'city', 'category', 'from', 'to', 'minPrice', 'maxPrice'] as const
const PAGE_SIZE = 9

/** Filters live in the URL so searches are shareable and survive back/forward. */
function toApiParams(sp: URLSearchParams): EventSearchParams {
  const from = sp.get('from')
  const to = sp.get('to')
  return {
    query: sp.get('query') || undefined,
    city: sp.get('city') || undefined,
    category: sp.get('category') || undefined,
    startsAfter: from ? new Date(`${from}T00:00:00`).toISOString() : undefined,
    startsBefore: to ? new Date(`${to}T23:59:59`).toISOString() : undefined,
    minPrice: sp.get('minPrice') || undefined,
    maxPrice: sp.get('maxPrice') || undefined,
    page: Number(sp.get('page') ?? 0),
    size: PAGE_SIZE,
    sort: 'startsAt',
  }
}

/** Keyed on the URL, so it re-initialises from the query string whenever the search changes. */
function FilterForm({ sp, setSp, searching }: {
  sp: URLSearchParams; setSp: (sp: URLSearchParams) => void; searching: boolean
}) {
  const [draft, setDraft] = useState(() => Object.fromEntries(FILTER_KEYS.map((k) => [k, sp.get(k) ?? ''])))
  const [showMore, setShowMore] = useState(() => FILTER_KEYS.slice(3).some((k) => sp.get(k)))

  const apply = (next: Record<string, string>) => {
    const out = new URLSearchParams()
    Object.entries(next).forEach(([k, v]) => { if (v) out.set(k, v) })
    setSp(out)
  }
  const submit = (e: FormEvent) => { e.preventDefault(); apply(draft) }
  const set = (k: string) => (e: { target: { value: string } }) => setDraft((d) => ({ ...d, [k]: e.target.value }))
  const activeFilters = FILTER_KEYS.filter((k) => sp.get(k))

  return (
    <form onSubmit={submit} className="mb-8 rounded-2xl bg-white p-4 ring-1 ring-slate-200/80 shadow-sm">
      <div className="flex flex-col gap-3 md:flex-row">
        <div className="relative flex-1">
          <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400" />
          <Input aria-label="Search by title" placeholder="Search by title…" value={draft.query} onChange={set('query')} className="pl-9" />
        </div>
        <Input aria-label="City" placeholder="City" value={draft.city} onChange={set('city')} className="md:w-44" />
        <Select aria-label="Category" value={draft.category} onChange={(e) => apply({ ...draft, category: e.target.value })} className="md:w-44">
          <option value="">All categories</option>
          {CATEGORIES.map((c) => <option key={c} value={c}>{titleCase(c)}</option>)}
        </Select>
        <Button type="button" variant="secondary" onClick={() => setShowMore((s) => !s)}>
          <SlidersHorizontal className="size-4" /> More
        </Button>
        <Button type="submit" loading={searching}>Search</Button>
      </div>
      {showMore && (
        <div className="mt-3 grid gap-3 border-t border-slate-100 pt-3 sm:grid-cols-2 lg:grid-cols-4">
          <label className="text-xs font-medium text-slate-600">From
            <Input type="date" value={draft.from} onChange={set('from')} className="mt-1" /></label>
          <label className="text-xs font-medium text-slate-600">To
            <Input type="date" value={draft.to} onChange={set('to')} className="mt-1" /></label>
          <label className="text-xs font-medium text-slate-600">Min price (€)
            <Input type="number" min={0} value={draft.minPrice} onChange={set('minPrice')} className="mt-1" /></label>
          <label className="text-xs font-medium text-slate-600">Max price (€)
            <Input type="number" min={0} value={draft.maxPrice} onChange={set('maxPrice')} className="mt-1" /></label>
        </div>
      )}
      {activeFilters.length > 0 && (
        <div className="mt-3 flex flex-wrap items-center gap-2">
          {activeFilters.map((k) => (
            <button key={k} type="button" onClick={() => apply({ ...draft, [k]: '' })}
              className="inline-flex items-center gap-1 rounded-full bg-brand-50 px-3 py-1 text-xs font-medium text-brand-700 hover:bg-brand-100">
              {k}: {sp.get(k)} <X className="size-3" />
            </button>
          ))}
          <button type="button" onClick={() => setSp(new URLSearchParams())} className="text-xs font-medium text-slate-500 hover:text-slate-800">Clear all</button>
        </div>
      )}
    </form>
  )
}

export function EventsPage() {
  const [sp, setSp] = useSearchParams()
  const params = toApiParams(sp)
  const { data, isLoading, isFetching, error } = useQuery({
    queryKey: ['events', params],
    queryFn: () => eventsApi.search(params),
    placeholderData: keepPreviousData,
  })

  const page = params.page ?? 0

  const goTo = (p: number) => {
    const next = new URLSearchParams(sp)
    if (p === 0) next.delete('page'); else next.set('page', String(p))
    setSp(next)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  return (
    <Container>
      <PageHeader title="Events" description="Filters are combined on the server into one dynamic query (JPA Specifications), paginated and cached." />

      <FilterForm key={sp.toString()} sp={sp} setSp={setSp} searching={isFetching && !isLoading} />

      {error ? <ErrorAlert error={error} /> : isLoading ? (
        <div className="py-20 text-center"><Spinner className="size-8" /></div>
      ) : data && data.content.length > 0 ? (
        <>
          <p className="mb-4 text-sm text-slate-500">{data.totalElements} event{data.totalElements === 1 ? '' : 's'}</p>
          <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {data.content.map((e) => <EventCard key={e.id} event={e} />)}
          </div>
          {data.totalPages > 1 && (
            <div className="mt-10 flex items-center justify-center gap-3">
              <Button variant="secondary" size="sm" disabled={page === 0} onClick={() => goTo(page - 1)}><ChevronLeft className="size-4" /> Prev</Button>
              <span className="text-sm text-slate-600">Page {page + 1} of {data.totalPages}</span>
              <Button variant="secondary" size="sm" disabled={data.last} onClick={() => goTo(page + 1)}>Next <ChevronRight className="size-4" /></Button>
            </div>
          )}
        </>
      ) : (
        <EmptyState title="No events match" description="Try removing a filter or searching another city."
          action={<Button variant="secondary" onClick={() => setSp(new URLSearchParams())}>Clear filters</Button>} />
      )}
    </Container>
  )
}
