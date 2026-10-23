import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Building2, Pencil, Plus, Trash2 } from 'lucide-react'
import { toast } from 'sonner'
import { organizerApi } from '@/api/organizer'
import { errorMessage } from '@/api/client'
import type { VenueRequest, VenueResponse } from '@/api/types'
import { Container } from '@/components/layout/AppLayout'
import { Button } from '@/components/ui/Button'
import { Card } from '@/components/ui/Card'
import { EmptyState } from '@/components/ui/EmptyState'
import { ErrorAlert } from '@/components/ui/ErrorAlert'
import { Field, Input } from '@/components/ui/Field'
import { PageHeader } from '@/components/ui/PageHeader'
import { PageSpinner } from '@/components/ui/Spinner'

function VenueForm({ initial, onDone }: { initial?: VenueResponse; onDone: () => void }) {
  const queryClient = useQueryClient()
  const { register, handleSubmit, formState: { errors } } = useForm<VenueRequest>({ defaultValues: initial })
  const save = useMutation({
    mutationFn: (v: VenueRequest) => {
      const body = { ...v, capacity: Number(v.capacity) }
      return initial ? organizerApi.updateVenue(initial.id, body) : organizerApi.createVenue(body)
    },
    onSuccess: () => {
      toast.success(initial ? 'Venue updated' : 'Venue created')
      queryClient.invalidateQueries({ queryKey: ['organizer', 'venues'] })
      onDone()
    },
  })
  const req = { required: 'Required' }
  return (
    <form onSubmit={handleSubmit((v) => save.mutate(v))} className="grid gap-4 sm:grid-cols-2" noValidate>
      <Field label="Name" error={errors.name?.message}><Input {...register('name', req)} /></Field>
      <Field label="Address" error={errors.address?.message}><Input {...register('address', req)} /></Field>
      <Field label="City" error={errors.city?.message}><Input {...register('city', req)} /></Field>
      <Field label="Country" error={errors.country?.message}><Input {...register('country', req)} /></Field>
      <Field label="Capacity" error={errors.capacity?.message} hint="Total tier quantities can't exceed this">
        <Input type="number" min={1} {...register('capacity', { required: 'Required', min: { value: 1, message: 'At least 1' } })} />
      </Field>
      <div className="flex items-end gap-2">
        <Button type="submit" loading={save.isPending}>{initial ? 'Save changes' : 'Create venue'}</Button>
        <Button type="button" variant="ghost" onClick={onDone}>Cancel</Button>
      </div>
      {save.error && <div className="sm:col-span-2"><ErrorAlert error={save.error} /></div>}
    </form>
  )
}

export function VenuesPage() {
  const queryClient = useQueryClient()
  const [editing, setEditing] = useState<VenueResponse | 'new' | null>(null)
  const { data, isLoading, error } = useQuery({ queryKey: ['organizer', 'venues'], queryFn: organizerApi.venues })
  const remove = useMutation({
    mutationFn: organizerApi.deleteVenue,
    onSuccess: () => { toast.success('Venue deleted'); queryClient.invalidateQueries({ queryKey: ['organizer', 'venues'] }) },
    onError: (err) => toast.error(errorMessage(err, 'Venue is in use by an event')),
  })

  if (isLoading) return <PageSpinner />
  return (
    <Container className="max-w-5xl">
      <PageHeader eyebrow="Organizer" title="Venues" description="Events are created at one of your venues."
        actions={editing === null && <Button onClick={() => setEditing('new')}><Plus className="size-4" /> Add venue</Button>} />
      {editing && (
        <Card className="mb-6 p-6">
          <h2 className="mb-4 font-bold">{editing === 'new' ? 'New venue' : `Edit ${editing.name}`}</h2>
          <VenueForm key={editing === 'new' ? 'new' : editing.id} initial={editing === 'new' ? undefined : editing} onDone={() => setEditing(null)} />
        </Card>
      )}
      {error ? <ErrorAlert error={error} /> : !data?.length ? (
        <EmptyState icon={<Building2 className="size-7" />} title="No venues yet" description="Add the place your event happens." />
      ) : (
        <div className="grid gap-4 sm:grid-cols-2">
          {data.map((v) => (
            <Card key={v.id} className="flex items-start justify-between gap-4 p-5">
              <div>
                <h3 className="font-bold">{v.name}</h3>
                <p className="text-sm text-slate-600">{v.address}, {v.city}, {v.country}</p>
                <p className="mt-2 text-sm"><span className="font-semibold">{v.capacity.toLocaleString()}</span> <span className="text-slate-500">capacity</span></p>
              </div>
              <div className="flex gap-1">
                <Button variant="ghost" size="sm" aria-label={`Edit ${v.name}`} onClick={() => setEditing(v)}><Pencil className="size-4" /></Button>
                <Button variant="ghost" size="sm" aria-label={`Delete ${v.name}`}
                  onClick={() => window.confirm(`Delete ${v.name}?`) && remove.mutate(v.id)}><Trash2 className="size-4 text-rose-600" /></Button>
              </div>
            </Card>
          ))}
        </div>
      )}
    </Container>
  )
}
