import { useEffect, useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import api from '../api/client'

/**
 * InlineAnnotator — Reviewer annotation drawer with real-time STOMP updates.
 * Feature 09 — Inline Annotator.
 * Feature 18 — Live Collaborative Review via WebSocket.
 */
export default function InlineAnnotator({ resultId, span, open, onClose, annotations = [] }) {
  const qc = useQueryClient()
  const [verdict,  setVerdict]  = useState('confirmed')
  const [note,     setNote]     = useState('')
  const [liveAnns, setLiveAnns] = useState([])

  // STOMP WebSocket subscription for live annotations
  useEffect(() => {
    if (!resultId) return
    let client
    try {
      client = new Client({
        webSocketFactory: () => new SockJS('/ws'),
        onConnect: () => {
          client.subscribe(`/topic/annotations/${resultId}`, (msg) => {
            try {
              const ann = JSON.parse(msg.body)
              setLiveAnns(prev => [...prev, ann])
            } catch {}
          })
        },
        reconnectDelay: 3000,
      })
      client.activate()
    } catch (e) {
      // WebSocket not available in dev without backend
    }
    return () => { try { client?.deactivate() } catch {} }
  }, [resultId])

  const saveMutation = useMutation({
    mutationFn: () => api.post('/annotations', {
      resultId,
      spanStart: span?.startChar ?? 0,
      verdict,
      note,
    }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['report'] })
      setNote('')
    },
  })

  if (!open) return (
    <div className="card h-full flex flex-col items-center justify-center text-center text-slate-600 py-20">
      <div className="text-5xl mb-4">💬</div>
      <p className="text-sm">Click a matched span to annotate it.</p>
    </div>
  )

  const allAnnotations = [...annotations, ...liveAnns]
  const verdictStyles = {
    confirmed: 'badge-red',
    dismissed: 'badge-green',
    grey_area: 'badge-yellow',
  }

  return (
    <div className="card h-full flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <h3 className="text-lg font-bold text-white">💬 Annotate Span</h3>
        <button onClick={onClose} className="text-slate-500 hover:text-white transition-colors text-xl">✕</button>
      </div>

      {/* Span preview */}
      {span && (
        <div className="rounded-xl bg-amber-900/20 border border-amber-700/30 p-3">
          <p className="text-xs text-amber-400 font-semibold mb-1">
            Selected span: chars {span.startChar}–{span.endChar}
          </p>
          <p className="text-xs text-slate-300 line-clamp-4 leading-relaxed">
            {span.matchedText || '(empty)'}
          </p>
        </div>
      )}

      {/* Verdict selector */}
      <div>
        <label className="block text-xs text-slate-400 mb-2 font-semibold uppercase tracking-wider">Verdict</label>
        <div className="flex gap-2">
          {['confirmed', 'dismissed', 'grey_area'].map(v => (
            <button key={v} onClick={() => setVerdict(v)}
                    className={`flex-1 py-2 rounded-xl text-xs font-semibold capitalize border transition-all ${
                      verdict === v
                        ? v === 'confirmed' ? 'bg-red-900/60 text-red-300 border-red-600' :
                          v === 'dismissed' ? 'bg-green-900/60 text-green-300 border-green-600' :
                                             'bg-yellow-900/60 text-yellow-300 border-yellow-600'
                        : 'text-slate-500 border-slate-700 hover:border-slate-500'
                    }`}>
              {v.replace('_', ' ')}
            </button>
          ))}
        </div>
      </div>

      {/* Note */}
      <div>
        <label className="block text-xs text-slate-400 mb-1.5 font-medium">Note</label>
        <textarea className="input resize-none text-xs" rows={4}
                  placeholder="Add reviewer note…"
                  value={note} onChange={e => setNote(e.target.value)} />
      </div>

      <button onClick={() => saveMutation.mutate()} disabled={saveMutation.isPending || !resultId}
              className="btn-primary w-full">
        {saveMutation.isPending ? 'Saving…' : '💾 Save Annotation'}
      </button>

      {/* Existing annotations + live updates */}
      {allAnnotations.length > 0 && (
        <div className="flex-1 overflow-y-auto space-y-2 border-t border-slate-700/50 pt-4">
          <p className="text-xs text-slate-400 font-semibold uppercase tracking-wider">
            Annotations ({allAnnotations.length})
            {liveAnns.length > 0 && <span className="ml-2 badge-blue">● LIVE</span>}
          </p>
          {allAnnotations.map((a, i) => (
            <div key={i} className="rounded-xl bg-slate-800/50 border border-slate-700/30 p-3">
              <div className="flex items-center justify-between mb-1">
                <span className={verdictStyles[a.verdict] || 'badge-gray'}>{a.verdict}</span>
                <span className="text-xs text-slate-500">{a.reviewer || a.createdAt?.substring(0,10)}</span>
              </div>
              {a.note && <p className="text-xs text-slate-300">{a.note}</p>}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
