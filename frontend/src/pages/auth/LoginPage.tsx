import { Link, useLocation, useNavigate } from 'react-router'
import { useForm } from 'react-hook-form'
import { toast } from 'sonner'
import { useMutation } from '@tanstack/react-query'
import { useAuth } from '@/auth/useAuth'
import { DemoLoginButtons } from '@/components/DemoLoginButtons'
import { Button } from '@/components/ui/Button'
import { Card } from '@/components/ui/Card'
import { Field, Input } from '@/components/ui/Field'
import { ErrorAlert } from '@/components/ui/ErrorAlert'

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const from = (useLocation().state as { from?: string } | null)?.from ?? '/events'
  const { register, handleSubmit, formState: { errors } } = useForm<{ email: string; password: string }>()

  const mutation = useMutation({
    mutationFn: ({ email, password }: { email: string; password: string }) => login(email, password),
    onSuccess: (user) => {
      toast.success(`Welcome back, ${user.fullName.split(' ')[0]}`)
      navigate(from, { replace: true })
    },
  })

  return (
    <div className="mx-auto grid max-w-5xl gap-8 px-4 py-12 sm:px-6 lg:grid-cols-2">
      <Card className="p-8">
        <h1 className="text-2xl font-bold">Sign in</h1>
        <p className="mt-1 text-sm text-slate-600">New here? <Link to="/register" className="font-semibold text-brand-600">Create an account</Link></p>
        <form className="mt-6 space-y-4" onSubmit={handleSubmit((v) => mutation.mutate(v))} noValidate>
          <Field label="Email" error={errors.email?.message}>
            <Input type="email" autoComplete="email" invalid={!!errors.email}
              {...register('email', { required: 'Email is required' })} />
          </Field>
          <Field label="Password" error={errors.password?.message}>
            <Input type="password" autoComplete="current-password" invalid={!!errors.password}
              {...register('password', { required: 'Password is required' })} />
          </Field>
          {mutation.error && <ErrorAlert error={mutation.error} />}
          <Button type="submit" className="w-full" loading={mutation.isPending}>Sign in</Button>
        </form>
      </Card>
      <div>
        <h2 className="font-semibold">Or use a demo account</h2>
        <p className="mb-4 mt-1 text-sm text-slate-600">Each role sees a different part of the app. Password for all: <code className="rounded bg-slate-100 px-1.5">demo1234</code></p>
        <DemoLoginButtons />
      </div>
    </div>
  )
}
