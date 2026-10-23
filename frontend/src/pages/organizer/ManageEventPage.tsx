import { Link, useParams } from 'react-router'
import { useForm } from 'react-hook-form'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { BarChart3, ExternalLink, Rocket, XCircle } from 'lucide-react'
import { toast } from 'sonner'
import { organizerApi } from '@/api/organizer'
import { errorMessage } from '@/api/client'
import type { TierRequest } from '@/api/types'
import { Container } from '@/components/layout/AppLayout'
import { EventBanner } from '@/components/EventBanner'
import { StatusBadge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Card } from '@/components/ui/Card'
import { ErrorAlert } from '@/components/ui/ErrorAlert'
import { Field, Input } from '@/components/ui/Field'
import { PageSpinner } from '@/components/ui/Spinner'
import { formatDateTime, formatMoney, titleCase } from '@/lib/format'

function AddTierForm({ eventId }: { eventId: number }) {
  const queryClient = useQueryClient()
  const { register, handleSubmit, reset, formState: { errors } } = useForm<TierRequest>({
    defaultValues: { priceCurrency: 'EUR', maxPerOrder: 4 },
  })
  const add = useMutation({
    mutationFn: (v: TierRequest) => organizerApi.addTier(eventId, {
      ...v, priceAmount: Number(v.priceAmount), totalQuantity: Number(v.totalQuantity), maxPerOrder: Number(v.maxPerOrder),
    }),
    onSuccess: () => {
      toast.success('Tier added')
      reset({ priceCurrency: 'EUR', maxPerOrder: 4 })
      queryClient.invalidateQueries({ queryKey: ['organizer', 'event', eventId] })
    },
  })
  return (
    <form onSubmit={handleSubmit((v) => add.mutate(v))} className="grid gap-4 sm:grid-cols-6" noValidate>
      <Field label="Name" error={errors.name?.message} className="sm:col-span-2">
        <Input placeholder="e.g. Front Row" {...register('name', { required: 'Required' })} />
      </Field>
      <Field label="Price (€)" error={errors.priceAmount?.message}>
        <Input type="number" step="0.01" min={0} {...register('priceAmount', { required: 'Required', min: { value: 0, message: '≥ 0' } })} />
      </Field>
      <Field label="Quantity" error={errors.totalQuantity?.message}>
        <Input type="number" min={1} {...register('totalQuantity', { required: 'Required', min: { value: 1, message: '≥ 1' } })} />
      </Field>
      <Field label="Max / order" error={errors.maxPerOrder?.message}>
        <Input type="number" min={1} {...register('maxPerOrder', { required: 'Required', min: { value: 1, message: '≥ 1' } })} />
      </Field>
      <div className="flex items-end"><Button type="submit" className="w-full" loading={add.isPending}>Add tier</Button></div>
      <Field label="Description (optional)" className="sm:col-span-6"><Input {...register('description')} /></Field>
      {add.error && <div className="sm:col-span-6"><ErrorAlert error={add.error} /></div>}
    </form>
  )
}

export function ManageEventPage() {
  const id = Number(useParams().id)
  const queryClient = useQueryClient()
  const { data: event, isLoading, error } = useQuery({ queryKey: ['organizer', 'event', id], queryFn: () => organizerApi.event(id) })

  const onChanged = (message: string) => {
    toast.success(message)
    queryClient.invalidateQueries({ queryKey: ['organizer'] })
    queryClient.invalidateQueries({ queryKey: ['events'] })
  }
  const publish = useMutation({ mutationFn: () => organizerApi.publish(id), onSuccess: () => onChanged('Published — it is now in the public catalog') })
  const cancel = useMutation({
    mutationFn: () => organizerApi.cancel(id),
    onSuccess: () => onChanged('Event cancelled — all paid orders were refunded and tickets voided'),
    onError: (err) => toast.error(errorMessage(err)),
  })

  if (isLoading) return <PageSpinner />
  if (error || !event) return <Container><ErrorAlert error={error} /></Container>

  const isDraft = event.status === 'DRAFT'
  const tierTotal = event.tiers.reduce((s, t) => s + t.totalQuantity, 0)

  return (
    <Container className="max-w-5xl">
      <Card className="overflow-hidden">
        <EventBanner category={event.category} className="h-32" iconClass="size-16" />
        <div className="flex flex-col gap-4 p-6 sm:flex-row sm:items-start sm:justify-between">
          <div>
            <div className="flex items-center gap-2"><StatusBadge status={event.status} /><span className="text-sm text-slate-500">{titleCase(event.category)}</span></div>
            <h1 className="mt-2 text-2xl font-bold">{event.title}</h1>
            <p className="mt-1 text-sm text-slate-600">{formatDateTime(event.startsAt)} · {event.venue?.name}, {event.venue?.city}</p>
            <p className="text-sm text-slate-500">Sales {formatDateTime(event.salesStartsAt)} → {formatDateTime(event.salesEndsAt)}</p>
          </div>
          <div className="flex flex-wrap gap-2">
            {isDraft && <Button loading={publish.isPending} onClick={() => publish.mutate()}><Rocket className="size-4" /> Publish</Button>}
            {!isDraft && <Link to={`/organizer/events/${id}/stats`}><Button variant="secondary"><BarChart3 className="size-4" /> Stats</Button></Link>}
            {event.status === 'PUBLISHED' && <Link to={`/events/${id}`}><Button variant="secondary"><ExternalLink className="size-4" /> Public page</Button></Link>}
            {event.status !== 'CANCELLED' && (
              <Button variant="danger" loading={cancel.isPending}
                onClick={() => window.confirm('Cancel this event? Every paid order will be refunded and all tickets voided.') && cancel.mutate()}>
                <XCircle className="size-4" /> Cancel event
              </Button>
            )}
          </div>
        </div>
        {publish.error && <div className="px-6 pb-6"><ErrorAlert error={publish.error} title="Can't publish yet" /></div>}
      </Card>

      <Card className="mt-6 p-6">
        <div className="flex items-baseline justify-between">
          <h2 className="font-bold">Ticket tiers</h2>
          <span className="text-sm text-slate-500">{tierTotal.toLocaleString()} seats in total</span>
        </div>
        {event.tiers.length === 0 ? (
          <p className="mt-3 text-sm text-slate-600">No tiers yet. An event needs at least one before it can be published.</p>
        ) : (
          <div className="mt-4 overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="text-xs uppercase tracking-wide text-slate-500">
                <tr><th className="py-2">Tier</th><th>Price</th><th>Quantity</th><th>Available</th><th>Max / order</th></tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {event.tiers.map((t) => (
                  <tr key={t.id}>
                    <td className="py-3 font-semibold">{t.name}{t.description && <div className="text-xs font-normal text-slate-500">{t.description}</div>}</td>
                    <td>{formatMoney(t.priceAmount, t.priceCurrency)}</td>
                    <td>{t.totalQuantity.toLocaleString()}</td>
                    <td>{t.available.toLocaleString()}</td>
                    <td>{t.maxPerOrder}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        {isDraft ? (
          <div className="mt-6 border-t border-slate-100 pt-6">
            <h3 className="mb-4 text-sm font-semibold text-slate-700">Add a tier</h3>
            <AddTierForm eventId={id} />
          </div>
        ) : (
          <p className="mt-4 text-xs text-slate-500">Tiers are locked once an event is published, so prices can't change under buyers.</p>
        )}
      </Card>
    </Container>
  )
}
