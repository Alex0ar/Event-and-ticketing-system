import { useEffect, useRef, useState, type FormEvent } from 'react'
import { useMutation } from '@tanstack/react-query'
import { BrowserQRCodeReader, type IScannerControls } from '@zxing/browser'
import { Camera, CameraOff, CheckCircle2, Keyboard, XCircle } from 'lucide-react'
import { ticketsApi } from '@/api/tickets'
import { apiError, errorMessage } from '@/api/client'
import type { CheckInResponse } from '@/api/types'
import { Container } from '@/components/layout/AppLayout'
import { Button } from '@/components/ui/Button'
import { Card } from '@/components/ui/Card'
import { Input } from '@/components/ui/Field'
import { PageHeader } from '@/components/ui/PageHeader'
import { formatDateTime } from '@/lib/format'

const reasons: Record<string, string> = {
  TICKET_ALREADY_USED: 'Already checked in',
  TICKET_REFUNDED: 'Ticket was refunded',
  TICKET_VOID: 'Ticket is void — event cancelled',
  NOT_FOUND: 'Unknown ticket',
}

type Result = { ok: true; data: CheckInResponse } | { ok: false; reason: string; detail: string }

export function ScannerPage() {
  const videoRef = useRef<HTMLVideoElement>(null)
  const controlsRef = useRef<IScannerControls | null>(null)
  const lastCodeRef = useRef<{ code: string; at: number } | null>(null)
  const [cameraOn, setCameraOn] = useState(false)
  const [cameraError, setCameraError] = useState<string | null>(null)
  const [manual, setManual] = useState('')
  const [result, setResult] = useState<Result | null>(null)
  const [history, setHistory] = useState<{ code: string; ok: boolean; label: string; at: Date }[]>([])

  const checkIn = useMutation({
    mutationFn: ticketsApi.checkIn,
    onSuccess: (data) => {
      setResult({ ok: true, data })
      setHistory((h) => [{ code: data.ticketCode, ok: true, label: `${data.holderName} · ${data.tierName}`, at: new Date() }, ...h].slice(0, 8))
    },
    onError: (err, code) => {
      const api = apiError(err)
      const reason = (api && reasons[api.errorCode]) ?? 'Rejected'
      setResult({ ok: false, reason, detail: errorMessage(err) })
      setHistory((h) => [{ code, ok: false, label: reason, at: new Date() }, ...h].slice(0, 8))
    },
  })

  const submitCode = (code: string) => {
    const trimmed = code.trim()
    if (!trimmed) return
    // The camera reports the same QR many times a second; ignore repeats for 3 s.
    const last = lastCodeRef.current
    if (last && last.code === trimmed && Date.now() - last.at < 3000) return
    lastCodeRef.current = { code: trimmed, at: Date.now() }
    checkIn.mutate(trimmed)
  }
  const submitRef = useRef(submitCode)
  useEffect(() => { submitRef.current = submitCode })

  useEffect(() => {
    if (!cameraOn || !videoRef.current) return
    const reader = new BrowserQRCodeReader()
    let stopped = false
    reader.decodeFromVideoDevice(undefined, videoRef.current, (res) => {
      if (res && !stopped) submitRef.current(res.getText())
    })
      .then((controls) => { controlsRef.current = controls; if (stopped) controls.stop() })
      .catch(() => { setCameraError('Camera unavailable — allow camera access or type the code instead.'); setCameraOn(false) })
    return () => { stopped = true; controlsRef.current?.stop(); controlsRef.current = null }
  }, [cameraOn])

  const onManual = (e: FormEvent) => { e.preventDefault(); lastCodeRef.current = null; submitCode(manual) }

  return (
    <Container className="max-w-5xl">
      <PageHeader eyebrow="Gate staff" title="Ticket scanner"
        description="Scan a QR from the customer's My tickets page, or paste a ticket code. Every ticket is accepted exactly once — even if two gates scan it at the same instant." />

      <div className="grid gap-6 lg:grid-cols-2">
        <Card className="p-6">
          <div className="relative aspect-square overflow-hidden rounded-xl bg-slate-900">
            <video ref={videoRef} className={cameraOn ? 'h-full w-full object-cover' : 'hidden'} muted playsInline />
            {cameraOn ? <div className="pointer-events-none absolute inset-10 rounded-2xl border-2 border-white/70" /> : (
              <div className="grid h-full place-items-center p-6 text-center text-slate-300">
                <div><Camera className="mx-auto size-10" /><p className="mt-3 text-sm">Camera is off</p></div>
              </div>
            )}
          </div>
          {cameraError && <p className="mt-3 text-sm text-rose-600">{cameraError}</p>}
          <Button className="mt-4 w-full" variant={cameraOn ? 'secondary' : 'primary'}
            onClick={() => { setCameraError(null); setCameraOn((on) => !on) }}>
            {cameraOn ? <><CameraOff className="size-4" /> Stop camera</> : <><Camera className="size-4" /> Start camera</>}
          </Button>
          <form onSubmit={onManual} className="mt-4 flex gap-2">
            <div className="relative flex-1">
              <Keyboard className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400" />
              <Input aria-label="Ticket code" placeholder="Paste a ticket code" value={manual} onChange={(e) => setManual(e.target.value)} className="pl-9 font-mono text-xs" />
            </div>
            <Button type="submit" variant="secondary" loading={checkIn.isPending}>Check in</Button>
          </form>
        </Card>

        <div className="space-y-6">
          {result ? (
            <div role="status" className={`rounded-2xl p-8 text-center text-white shadow-lg ${result.ok ? 'bg-emerald-600' : 'bg-rose-600'}`}>
              {result.ok ? <CheckCircle2 className="mx-auto size-16" /> : <XCircle className="mx-auto size-16" />}
              <p className="mt-3 text-3xl font-extrabold">{result.ok ? 'Welcome in' : result.reason}</p>
              {result.ok ? (
                <div className="mt-3 space-y-0.5 text-emerald-50">
                  <p className="text-lg font-semibold">{result.data.holderName}</p>
                  <p>{result.data.eventTitle} · {result.data.tierName}</p>
                  <p className="text-sm opacity-80">{formatDateTime(result.data.checkedInAt)}</p>
                </div>
              ) : <p className="mt-2 text-rose-50">{result.detail}</p>}
            </div>
          ) : (
            <Card className="grid min-h-60 place-items-center p-8 text-center text-slate-500">Waiting for a ticket…</Card>
          )}
          {history.length > 0 && (
            <Card className="p-5">
              <h2 className="mb-3 text-sm font-semibold text-slate-700">Recent scans</h2>
              <ul className="space-y-2 text-sm">
                {history.map((h, i) => (
                  <li key={i} className="flex items-center gap-2">
                    {h.ok ? <CheckCircle2 className="size-4 text-emerald-600" /> : <XCircle className="size-4 text-rose-600" />}
                    <span className="flex-1">{h.label}</span>
                    <span className="font-mono text-xs text-slate-400">{h.code.slice(0, 8)}</span>
                  </li>
                ))}
              </ul>
            </Card>
          )}
        </div>
      </div>
    </Container>
  )
}
