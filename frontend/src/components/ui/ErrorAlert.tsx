import { AlertCircle } from 'lucide-react'
import { apiError, errorMessage } from '@/api/client'

/** Renders the backend's ApiError (message + field errors) or a network failure. */
export function ErrorAlert({ error, title }: { error: unknown; title?: string }) {
  if (!error) return null
  const api = apiError(error)
  return (
    <div role="alert" className="flex gap-3 rounded-xl bg-rose-50 p-4 text-sm text-rose-800 ring-1 ring-rose-200">
      <AlertCircle className="mt-0.5 size-4 shrink-0" />
      <div>
        <p className="font-semibold">{title ?? errorMessage(error)}</p>
        {title && <p>{errorMessage(error)}</p>}
        {api && api.fieldErrors.length > 0 && (
          <ul className="mt-1 list-disc pl-4">
            {api.fieldErrors.map((f) => (
              <li key={f.field + f.message}><span className="font-medium">{f.field}</span>: {f.message}</li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}
