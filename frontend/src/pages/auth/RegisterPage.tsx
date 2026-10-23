import { Link, useNavigate } from 'react-router'
import { useForm } from 'react-hook-form'
import { useMutation } from '@tanstack/react-query'
import { toast } from 'sonner'
import { authApi } from '@/api/auth'
import { apiError } from '@/api/client'
import { useAuth } from '@/auth/useAuth'
import { Button } from '@/components/ui/Button'
import { Card } from '@/components/ui/Card'
import { Field, Input } from '@/components/ui/Field'
import { ErrorAlert } from '@/components/ui/ErrorAlert'

interface Form { fullName: string; email: string; password: string }

export function RegisterPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const { register, handleSubmit, setError, formState: { errors } } = useForm<Form>()

  const mutation = useMutation({
    mutationFn: async (v: Form) => {
      await authApi.register(v.fullName, v.email, v.password)
      return login(v.email, v.password)
    },
    onSuccess: () => {
      toast.success('Account created — you can buy tickets now')
      navigate('/events')
    },
    onError: (err) => {
      // Map the backend's field errors straight onto the inputs.
      apiError(err)?.fieldErrors.forEach((f) => setError(f.field as keyof Form, { message: f.message }))
    },
  })

  return (
    <div className="mx-auto max-w-md px-4 py-12">
      <Card className="p-8">
        <h1 className="text-2xl font-bold">Create an account</h1>
        <p className="mt-1 text-sm text-slate-600">Already registered? <Link to="/login" className="font-semibold text-brand-600">Sign in</Link></p>
        <form className="mt-6 space-y-4" onSubmit={handleSubmit((v) => mutation.mutate(v))} noValidate>
          <Field label="Full name" error={errors.fullName?.message}>
            <Input autoComplete="name" invalid={!!errors.fullName} {...register('fullName', { required: 'Name is required' })} />
          </Field>
          <Field label="Email" error={errors.email?.message}>
            <Input type="email" autoComplete="email" invalid={!!errors.email} {...register('email', {
              required: 'Email is required',
              pattern: { value: /^[^\s@]+@[^\s@]+\.[^\s@]+$/, message: 'Enter a valid email' },
            })} />
          </Field>
          <Field label="Password" error={errors.password?.message} hint="At least 8 characters, with a letter and a digit">
            <Input type="password" autoComplete="new-password" invalid={!!errors.password} {...register('password', {
              required: 'Password is required',
              minLength: { value: 8, message: 'Password must be at least 8 characters' },
              validate: {
                letter: (v) => /[A-Za-z]/.test(v) || 'Password must contain a letter',
                digit: (v) => /\d/.test(v) || 'Password must contain a digit',
              },
            })} />
          </Field>
          {mutation.error && !apiError(mutation.error)?.fieldErrors.length && <ErrorAlert error={mutation.error} />}
          <Button type="submit" className="w-full" loading={mutation.isPending}>Create account</Button>
        </form>
      </Card>
    </div>
  )
}
