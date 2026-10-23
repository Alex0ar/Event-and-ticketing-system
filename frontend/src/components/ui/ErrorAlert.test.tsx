import { render, screen } from '@testing-library/react'
import { AxiosError, AxiosHeaders } from 'axios'
import type { ApiError } from '@/api/types'
import { ErrorAlert } from './ErrorAlert'

function axiosErrorWith(body: ApiError) {
  const err = new AxiosError('Request failed')
  err.response = { status: body.status, statusText: '', headers: {}, config: { headers: new AxiosHeaders() }, data: body }
  return err
}

describe('ErrorAlert', () => {
  it("renders the backend's message and field errors", () => {
    render(<ErrorAlert error={axiosErrorWith({
      timestamp: '', path: '/api/v1/auth/register', status: 400, errorCode: 'VALIDATION_ERROR',
      message: 'Request validation failed',
      fieldErrors: [{ field: 'password', message: 'password must contain a digit' }],
    })} />)
    expect(screen.getByRole('alert')).toHaveTextContent('Request validation failed')
    expect(screen.getByText(/password must contain a digit/)).toBeInTheDocument()
  })

  it('explains network failures', () => {
    render(<ErrorAlert error={new AxiosError('Network Error')} />)
    expect(screen.getByRole('alert')).toHaveTextContent(/Cannot reach the server/)
  })
})
