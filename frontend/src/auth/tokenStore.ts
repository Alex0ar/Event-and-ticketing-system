// Access token lives in memory only (gone on reload, never readable by other tabs' scripts after
// close); the refresh token is persisted so a reload can silently mint a new access token.
const REFRESH_KEY = 'ticketflow.refreshToken'

let accessToken: string | null = null

export const tokenStore = {
  getAccess: () => accessToken,
  getRefresh: () => localStorage.getItem(REFRESH_KEY),
  set(access: string, refresh: string) {
    accessToken = access
    localStorage.setItem(REFRESH_KEY, refresh)
  },
  clear() {
    accessToken = null
    localStorage.removeItem(REFRESH_KEY)
  },
}
