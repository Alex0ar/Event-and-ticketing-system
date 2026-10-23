/** <input type="datetime-local"> works in local time without a zone; convert both ways. */
export function toLocalInput(d: Date) {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

export const localInputToIso = (value: string) => new Date(value).toISOString()
