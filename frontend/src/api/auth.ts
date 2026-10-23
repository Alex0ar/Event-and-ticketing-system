import { http } from './client'
import type { TokenResponse, UserResponse } from './types'

export const authApi = {
  login: (email: string, password: string) =>
    http.post<TokenResponse>('/auth/login', { email, password }).then((r) => r.data),
  register: (fullName: string, email: string, password: string) =>
    http.post<UserResponse>('/auth/register', { fullName, email, password }).then((r) => r.data),
  me: () => http.get<UserResponse>('/users/me').then((r) => r.data),
  logout: () => http.post('/users/logout'),
  grantRole: (userId: number, role: string) =>
    http.post<UserResponse>(`/users/${userId}/roles/${role}`).then((r) => r.data),
}
