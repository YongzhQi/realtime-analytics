import React, { useEffect, useRef, useState } from 'react'

const PROCESSOR_BASE = import.meta.env.VITE_PROCESSOR_BASE || 'http://localhost:8082'
const INGEST_BASE = import.meta.env.VITE_INGEST_BASE || 'http://localhost:8081'

function App() {
  const [metrics, setMetrics] = useState({
    receivedTotal: 0,
    writtenTotal: 0,
    avgProcessingMs: 0,
    lag: -1,
    ts: ''
  })
  const [status, setStatus] = useState('disconnected')
  const esRef = useRef(null)

  useEffect(() => {
    const url = `${PROCESSOR_BASE}/metrics/stream`
    const es = new EventSource(url)
    esRef.current = es
    setStatus('connecting')

    es.addEventListener('metrics', (ev) => {
      try {
        const data = JSON.parse(ev.data)
        setMetrics(data)
        setStatus('connected')
      } catch (err) {
        console.error('parse error', err)
      }
    })
    es.onerror = (e) => {
      console.error('SSE error', e)
      setStatus('error')
    }

    return () => {
      es.close()
      setStatus('disconnected')
    }
  }, [])

  const [form, setForm] = useState({
    sessionId: 's-1',
    eventType: 'page_view',
    payload: '{"path":"/"}'
  })
  const [sending, setSending] = useState(false)
  const [sendMsg, setSendMsg] = useState('')

  const sendEvent = async (e) => {
    e.preventDefault()
    setSending(true)
    setSendMsg('')
    try {
      const body = {
        sessionId: form.sessionId,
        eventType: form.eventType,
        payload: form.payload
      }
      const res = await fetch(`${INGEST_BASE}/events`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
      })
      if (!res.ok) throw new Error(`HTTP ${res.status}`)
      setSendMsg('Event accepted')
    } catch (err) {
      setSendMsg(`Send failed: ${err.message}`)
    } finally {
      setSending(false)
    }
  }

  return (
    <div style={{ fontFamily: 'system-ui, Arial', padding: 20, maxWidth: 900, margin: '0 auto' }}>
      <h1>Real-Time Analytics Dashboard</h1>
      <p>SSE status: <strong>{status}</strong></p>

      <section style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 12 }}>
        <Card title="Received total" value={metrics.receivedTotal} />
        <Card title="Written total" value={metrics.writtenTotal} />
        <Card title="Avg processing (ms)" value={metrics.avgProcessingMs?.toFixed ? metrics.avgProcessingMs.toFixed(2) : metrics.avgProcessingMs} />
        <Card title="Lag" value={metrics.lag} />
      </section>

      <p style={{ color: '#666', marginTop: 8 }}>Last update: {metrics.ts}</p>

      <h2 style={{ marginTop: 28 }}>Send Test Event</h2>
      <form onSubmit={sendEvent} style={{ display: 'grid', gap: 8, maxWidth: 600 }}>
        <label>
          Session ID
          <input value={form.sessionId} onChange={e => setForm(f => ({ ...f, sessionId: e.target.value }))} />
        </label>
        <label>
          Event Type
          <input value={form.eventType} onChange={e => setForm(f => ({ ...f, eventType: e.target.value }))} />
        </label>
        <label>
          Payload (JSON string)
          <textarea rows={4} value={form.payload} onChange={e => setForm(f => ({ ...f, payload: e.target.value }))} />
        </label>
        <div>
          <button type="submit" disabled={sending}>{sending ? 'Sending...' : 'Send'}</button>
          <span style={{ marginLeft: 10 }}>{sendMsg}</span>
        </div>
      </form>

      <h3 style={{ marginTop: 28 }}>Config</h3>
      <pre style={{ background: '#f7f7f7', padding: 10 }}>
        PROCESSOR_BASE = {PROCESSOR_BASE}{'\n'}
        INGEST_BASE = {INGEST_BASE}
      </pre>
      <p>Override via .env: VITE_PROCESSOR_BASE and VITE_INGEST_BASE</p>
    </div>
  )
}

function Card({ title, value }) {
  return (
    <div style={{ border: '1px solid #ddd', borderRadius: 8, padding: 12 }}>
      <div style={{ fontSize: 12, color: '#666' }}>{title}</div>
      <div style={{ fontSize: 22, fontWeight: 600 }}>{String(value)}</div>
    </div>
  )
}

export default App