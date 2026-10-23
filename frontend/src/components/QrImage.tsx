import { useQuery } from '@tanstack/react-query'
import { useEffect, useMemo } from 'react'
import { ticketsApi } from '@/api/tickets'
import { Spinner } from './ui/Spinner'

/** QR PNGs require the bearer token, so they're fetched as blobs and shown via object URLs. */
export function QrImage({ code, className = 'size-40' }: { code: string; className?: string }) {
  const { data: blob, isError } = useQuery({
    queryKey: ['qr', code],
    queryFn: () => ticketsApi.qrBlob(code),
    staleTime: Infinity,
  })
  const url = useMemo(() => (blob ? URL.createObjectURL(blob) : null), [blob])
  useEffect(() => () => { if (url) URL.revokeObjectURL(url) }, [url])

  if (isError) return <div className={`${className} grid place-items-center text-xs text-slate-500`}>QR unavailable</div>
  if (!url) return <div className={`${className} grid place-items-center`}><Spinner /></div>
  return <img src={url} alt={`QR code for ticket ${code}`} className={`${className} rounded-lg bg-white`} />
}
