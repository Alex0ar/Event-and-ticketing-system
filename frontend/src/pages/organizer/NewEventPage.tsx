import { Link, useNavigate } from 'react-router'
import { useForm } from 'react-hook-form'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { organizerApi } from '@/api/organizer'
import { Container } from '@/components/layout/AppLayout'
import { CATEGORIES } from '@/lib/categories'
import { Button } from '@/components/ui/Button'
import { Card } from '@/components/ui/Card'
import { EmptyState } from '@/components/ui/EmptyState'
import { ErrorAlert } from '@/components/ui/ErrorAlert'
import { Field, Input, Select, Textarea } from '@/components/ui/Field'
import { PageHeader } from '@/components/ui/PageHeader'
import { PageSpinner } from '@/components/ui/Spinner'
import { localInputToIso, toLocalInput } from '@/lib/datetimeLocal'
import { titleCase } from '@/lib/format'

interface Form {
  venueId: string; title: string; description: string; category: string
  startsAt: string; endsAt: string; salesStartsAt: string; salesEndsAt: string
}

function defaults(): Partial<Form> {
  const start = new Date(); start.setDate(start.getDate() + 30); start.setHours(19, 0, 0, 0)
  const end = new Date(start); end.setHours(23)
  const salesEnd = new Date(start); salesEnd.setHours(18)
  return { category: 'CONCERT', startsAt: toLocalInput(start), endsAt: toLocalInput(end),
    salesStartsAt: toLocalInput(new Date()), salesEndsAt: toLocalInput(salesEnd) }
}

export function NewEventPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const venues = useQuery({ queryKey: ['organizer', 'venues'], queryFn: organizerApi.venues })
  const { register, handleSubmit, getValues, formState: { errors } } = useForm<Form>({ defaultValues: defaults() })

  const create = useMutation({
    mutationFn: (v: Form) => organizerApi.createEvent({
      venueId: Number(v.venueId), title: v.title, description: v.description || undefined, category: v.category,
      startsAt: localInputToIso(v.startsAt), endsAt: localInputToIso(v.endsAt),
      salesStartsAt: localInputToIso(v.salesStartsAt), salesEndsAt: localInputToIso(v.salesEndsAt),
    }),
    onSuccess: (event) => {
      queryClient.invalidateQueries({ queryKey: ['organizer', 'events'] })
      toast.success('Draft created — now add ticket tiers')
      navigate(`/organizer/events/${event.id}`)
    },
  })

  if (venues.isLoading) return <PageSpinner />
  if (!venues.data?.length) {
    return <EmptyState title="Add a venue first" description="Every event takes place at one of your venues."
      action={<Link to="/organizer/venues"><Button>Go to venues</Button></Link>} />
  }

  const after = (field: keyof Form, label: string) => (v: string) =>
    !getValues(field) || new Date(v) > new Date(getValues(field)) || `Must be after ${label}`

  return (
    <Container className="max-w-3xl">
      <PageHeader eyebrow="Organizer" title="New event" description="Events start as drafts. You'll add ticket tiers and publish on the next screen." />
      <Card className="p-6 sm:p-8">
        <form onSubmit={handleSubmit((v) => create.mutate(v))} className="grid gap-5 sm:grid-cols-2" noValidate>
          <Field label="Title" error={errors.title?.message} className="sm:col-span-2">
            <Input {...register('title', { required: 'Title is required' })} />
          </Field>
          <Field label="Venue" error={errors.venueId?.message}>
            <Select {...register('venueId', { required: 'Pick a venue' })}>
              <option value="">Choose…</option>
              {venues.data.map((v) => <option key={v.id} value={v.id}>{v.name} — {v.city} ({v.capacity.toLocaleString()})</option>)}
            </Select>
          </Field>
          <Field label="Category">
            <Select {...register('category')}>{CATEGORIES.map((c) => <option key={c} value={c}>{titleCase(c)}</option>)}</Select>
          </Field>
          <Field label="Description" className="sm:col-span-2"><Textarea {...register('description')} /></Field>
          <Field label="Starts" error={errors.startsAt?.message}>
            <Input type="datetime-local" {...register('startsAt', { required: 'Required' })} />
          </Field>
          <Field label="Ends" error={errors.endsAt?.message}>
            <Input type="datetime-local" {...register('endsAt', { required: 'Required', validate: after('startsAt', 'the start') })} />
          </Field>
          <Field label="Sales open" error={errors.salesStartsAt?.message}>
            <Input type="datetime-local" {...register('salesStartsAt', { required: 'Required' })} />
          </Field>
          <Field label="Sales close" error={errors.salesEndsAt?.message} hint="Must be on or before the event start">
            <Input type="datetime-local" {...register('salesEndsAt', {
              required: 'Required',
              validate: (v) => (after('salesStartsAt', 'sales open')(v) === true
                && new Date(v) <= new Date(getValues('startsAt'))) || 'Must be after sales open and not after the start',
            })} />
          </Field>
          {create.error && <div className="sm:col-span-2"><ErrorAlert error={create.error} /></div>}
          <div className="flex gap-2 sm:col-span-2">
            <Button type="submit" loading={create.isPending}>Create draft</Button>
            <Link to="/organizer"><Button type="button" variant="ghost">Cancel</Button></Link>
          </div>
        </form>
      </Card>
    </Container>
  )
}
