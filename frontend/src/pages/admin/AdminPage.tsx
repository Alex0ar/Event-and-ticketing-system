import { useForm } from 'react-hook-form'
import { useMutation } from '@tanstack/react-query'
import { toast } from 'sonner'
import { authApi } from '@/api/auth'
import type { Role } from '@/api/types'
import { Container } from '@/components/layout/AppLayout'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Card } from '@/components/ui/Card'
import { ErrorAlert } from '@/components/ui/ErrorAlert'
import { Field, Input, Select } from '@/components/ui/Field'
import { PageHeader } from '@/components/ui/PageHeader'
import { roleLabel } from '@/lib/format'

export function AdminPage() {
  const { register, handleSubmit, formState: { errors } } = useForm<{ userId: string; role: Role }>({ defaultValues: { role: 'ORGANIZER' } })
  const grant = useMutation({
    mutationFn: (v: { userId: string; role: Role }) => authApi.grantRole(Number(v.userId), v.role),
    onSuccess: (u) => toast.success(`${u.fullName} now has: ${u.roles.map((r) => roleLabel[r]).join(', ')}`),
  })

  return (
    <Container className="max-w-2xl">
      <PageHeader eyebrow="Admin" title="Grant a role"
        description="Role escalation is admin-only and enforced server-side with @PreAuthorize. The user picks up the new role on their next login or token refresh." />
      <Card className="p-6">
        <form onSubmit={handleSubmit((v) => grant.mutate(v))} className="grid gap-4 sm:grid-cols-[1fr_1fr_auto]" noValidate>
          <Field label="User ID" error={errors.userId?.message} hint="Demo customer is user #2">
            <Input type="number" min={1} {...register('userId', { required: 'Required' })} />
          </Field>
          <Field label="Role">
            <Select {...register('role')}>
              {(Object.keys(roleLabel) as Role[]).map((r) => <option key={r} value={r}>{roleLabel[r]}</option>)}
            </Select>
          </Field>
          <div className="flex items-start pt-7"><Button type="submit" loading={grant.isPending}>Grant</Button></div>
        </form>
        {grant.error && <div className="mt-4"><ErrorAlert error={grant.error} /></div>}
        {grant.data && (
          <div className="mt-4 flex flex-wrap items-center gap-2 text-sm">
            <span className="font-semibold">{grant.data.email}</span>
            {grant.data.roles.map((r) => <Badge key={r} tone="brand">{roleLabel[r]}</Badge>)}
          </div>
        )}
      </Card>
    </Container>
  )
}
