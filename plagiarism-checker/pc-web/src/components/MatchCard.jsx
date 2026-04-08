import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import api from '../api/client'

/**
 * MatchCard — displays a single matched text span with:
 * - Character range
 * - Source type badge (CORPUS | WEB)
 * - Matched text preview
 * - "Suggest rewrites" button (Feature 17)
 */
export default function MatchCard({ span, onClick }) {
  const [rewrites, setRewrites] = useState(null)
  const [showRewrites, setShowRewrites] = useState(false)

  const suggestMutation = useMutation({
    mutationFn: () => api.post('/suggestions', {
      matchedSpan: span.matchedText,
      context: span.matchedText?.substring(0, 200) ?? '',
    }).then(r => r.data),
    onSuccess: (data) => {
      setRewrites(data.rewrites)
      setShowRewrites(true)
    },
  })

  const src = span.sourceType === 'WEB' ? 'badge-red' : 'badge-yellow'

  return (
    <div className="rounded-xl border border-slate-700/50 p-4 hover:border-amber-500/40
                    hover:bg-slate-800/30 transition-all duration-200 group cursor-pointer"
         onClick={onClick}>
      {/* Header */}
      <div className="flex items-center justify-between mb-3">
        <div className="flex items-center gap-2">
          <span className={src}>{span.sourceType}</span>
          <span className="text-xs text-slate-500 font-mono">
            chars {span.startChar}–{span.endChar}
          </span>
        </div>
        <button
          className="text-xs text-blue-400 hover:text-blue-300 opacity-0 group-hover:opacity-100
                     transition-all duration-200 border border-blue-700/50 rounded-lg px-2.5 py-1"
          onClick={(e) => { e.stopPropagation(); suggestMutation.mutate() }}
          disabled={suggestMutation.isPending}>
          {suggestMutation.isPending ? (
            <span className="flex items-center gap-1">
              <svg className="animate-spin w-3 h-3" fill="none" viewBox="0 0 24 24">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"/>
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z"/>
              </svg>
              Loading…
            </span>
          ) : '✦ Suggest rewrites'}
        </button>
      </div>

      {/* Matched text */}
      <p className="text-sm text-slate-300 leading-relaxed line-clamp-3 span-match px-2 py-1 rounded">
        {span.matchedText || '(empty span)'}
      </p>
      {span.sourceUrl && (
        <p className="text-xs text-blue-400 mt-2 truncate">🌐 {span.sourceUrl}</p>
      )}

      {/* Rewrite suggestions (Feature 17) */}
      {showRewrites && rewrites && (
        <div className="mt-4 space-y-2" onClick={e => e.stopPropagation()}>
          <div className="flex items-center justify-between">
            <p className="text-xs font-semibold text-slate-400 uppercase tracking-wider">✦ Rewrite Suggestions</p>
            <button onClick={() => setShowRewrites(false)} className="text-xs text-slate-500 hover:text-white">✕</button>
          </div>
          {rewrites.map((r, i) => (
            <div key={i} className="rounded-lg bg-blue-900/30 border border-blue-800/40 px-3 py-2">
              <p className="text-xs text-blue-400 font-semibold mb-1">Option {i + 1}</p>
              <p className="text-xs text-slate-300 leading-relaxed">{r}</p>
              <button onClick={() => navigator.clipboard.writeText(r)}
                      className="text-xs text-slate-500 hover:text-white mt-1.5">📋 Copy</button>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
