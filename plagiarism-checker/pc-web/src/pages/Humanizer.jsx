import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import api from '../api/client'
import { useNavigate } from 'react-router-dom'
import { ArrowLeft } from 'lucide-react'

export default function Humanizer() {
  const navigate = useNavigate()
  const [input,    setInput]    = useState('')
  const [result,   setResult]   = useState(null)
  const [mode,     setMode]     = useState('humanize') // humanize | detect
  const [showDiff, setShowDiff] = useState(false)

  const humanizeMutation = useMutation({
    mutationFn: async (text) => {
        try {
          const apiKey = 'AIzaSyBkh75bj_2RWbmQu5cSDCxfdyzXjQJGU2s';
          const res = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${apiKey}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
              contents: [{ parts: [{ text: "Humanize this text to sound more natural, engaging, and less like AI. REWRITE THE ENTIRE TEXT from start to finish. Change words, vary sentence lengths and structures, use synonyms, and adjust tone significantly to bypass AI detectors. Do not just return the same text back. Ensure the rewritten text is fully complete and covers all points of the original. Only return the final rewritten text, with no extra commentary, no markdown, and do NOT start with 'Abstract-'. Return raw text ONLY:\n\n" + text }] }],
              generationConfig: { temperature: 0.9 }
            })
          });
          const data = await res.json();
          if (data.error) throw new Error(data.error.message);
          if (!data.candidates) throw new Error("No response candidates returned from AI.");
          
          return {
            original: text,
            humanized: data.candidates[0].content.parts[0].text.trim(),
            scoreImprovement: 0.85
          };
        } catch (e) {
          console.error(e);
          throw new Error(e.message || "Failed to humanize text.");
        }
    },
    onSuccess: (data) => setResult(data),
  })

  const detectMutation = useMutation({
    mutationFn: async (text) => {
        try {
          const apiKey = 'AIzaSyBkh75bj_2RWbmQu5cSDCxfdyzXjQJGU2s';
          const res = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${apiKey}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
              contents: [{ parts: [{ text: "Analyze the following text and determine the probability it was AI generated. You must format your answer strictly as a raw JSON object with NO markdown formatting, NO backticks, NO \`\`\`json, just start with { and end with }. Structure it exactly like this: { \"probabilityPct\": 85, \"riskLabel\": \"HIGHLY LIKELY AI\", \"reasoning\": \"It lacks human variance...\", \"indicators\": [\"low burstiness\", \"repetitive phrasing\"] }. Text:\n" + text }] }],
              generationConfig: { temperature: 0.1 }
            })
          });
          const data = await res.json();
          if (data.error) throw new Error(data.error.message);
          if (!data.candidates) throw new Error("No response candidates returned from AI.");
          const raw = data.candidates[0].content.parts[0].text.replace("```json", "").replace("```", "").trim();
          return JSON.parse(raw);
        } catch (e) {
          console.error(e);
          throw new Error(e.message || "Failed to analyze text.");
        }
    },
    onSuccess: (data) => setResult(data),
  })

  const handleSubmit = () => {
    if (!input.trim()) return
    setResult(null)
    if (mode === 'humanize') humanizeMutation.mutate(input)
    else detectMutation.mutate(input)
  }

  const isLoading = humanizeMutation.isPending || detectMutation.isPending
  const errorMsg  = humanizeMutation.error?.response?.data?.detail
                 || detectMutation.error?.response?.data?.detail

  // Inline diff between original and humanized
  const renderDiff = (original, humanized) => {
    const origWords = original.split(' ')
    const humWords  = humanized.split(' ')
    return humWords.map((w, i) => (
      <span key={i} className={origWords[i] !== w ? 'bg-green-900/50 text-green-300 rounded px-0.5' : ''}>
        {w}{' '}
      </span>
    ))
  }

  return (
    <div className="max-w-5xl mx-auto animate-slide-up space-y-6">
      <button 
        onClick={() => navigate('/dashboard')}
        className="flex items-center gap-2 text-slate-400 hover:text-white transition-colors text-sm font-medium"
      >
        <ArrowLeft size={16} /> Back to Dashboard
      </button>

      {/* Header */}
      <div>
        <h1 className="text-3xl font-bold text-white flex items-center gap-3">
          <span className="text-4xl">✦</span> AI Studio
        </h1>
        <p className="text-slate-400 mt-1">Humanize AI text or detect AI authorship using Claude API.</p>
      </div>

      {/* Mode tabs */}
      <div className="flex rounded-xl p-1 gap-1 w-fit" style={{ background: 'rgba(0,0,0,0.3)' }}>
        {[
          { id: 'humanize', label: '✦ Humanize Text',   icon: '✦' },
          { id: 'detect',   label: '🔍 Detect AI',       icon: '🔍' },
        ].map(m => (
          <button key={m.id} onClick={() => { setMode(m.id); setResult(null) }}
                  className={`px-5 py-2 rounded-lg text-sm font-semibold transition-all duration-200
                    ${mode === m.id ? 'bg-blue-600 text-white shadow-lg' : 'text-slate-400 hover:text-white'}`}>
            {m.label}
          </button>
        ))}
      </div>

      {/* Two-panel layout */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Input */}
        <div className="card space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-sm font-bold text-slate-300 uppercase tracking-wider">
              {mode === 'humanize' ? '📥 AI-Generated Text' : '📥 Text to Analyze'}
            </h2>
            <span className="text-xs text-slate-500">{input.length} / 10,000</span>
          </div>
          <textarea
            className="input resize-none font-mono text-xs leading-relaxed"
            rows={16}
            placeholder={mode === 'humanize'
              ? "Paste AI-generated text here to humanize it…\n\nExample: 'Furthermore, it is important to note that the implementation of sustainable practices…'"
              : "Paste text here to detect if it was written by AI or a human…"}
            value={input}
            onChange={e => setInput(e.target.value.slice(0, 10000))}
          />
          <button onClick={handleSubmit} disabled={isLoading || !input.trim()} className="btn-primary w-full py-3">
            {isLoading ? (
              <span className="flex items-center justify-center gap-2">
                <svg className="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"/>
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z"/>
                </svg>
                Processing with Claude…
              </span>
            ) : mode === 'humanize' ? '✦ Humanize Text' : '🔍 Detect AI Authorship'}
          </button>
          {errorMsg && (
            <div className="text-xs text-red-300 bg-red-900/30 rounded-xl px-4 py-3 border border-red-800/50">
              {errorMsg}
            </div>
          )}
        </div>

        {/* Output */}
        <div className="card space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-sm font-bold text-slate-300 uppercase tracking-wider">
              {mode === 'humanize' ? '📤 Humanized Output' : '🔬 Detection Result'}
            </h2>
            {mode === 'humanize' && result?.humanized && (
              <div className="flex gap-2">
                <button onClick={() => setShowDiff(d => !d)}
                        className="text-xs text-blue-400 hover:text-blue-300">
                  {showDiff ? 'Hide Diff' : 'Show Diff'}
                </button>
                <button onClick={() => navigator.clipboard.writeText(result.humanized)}
                        className="text-xs text-slate-400 hover:text-white">📋 Copy</button>
              </div>
            )}
          </div>

          {/* Humanize output */}
          {mode === 'humanize' && (
            <div className="min-h-[340px]">
              {(humanizeMutation.isError || detectMutation.isError) && (
                <div className="p-4 bg-red-900/40 text-red-300 border border-red-700/50 rounded-xl mb-4 text-sm font-semibold">
                  Error: {(humanizeMutation.error || detectMutation.error)?.message}
                </div>
              )}
              {!result && !isLoading && !humanizeMutation.isError && !detectMutation.isError && (
                <div className="flex flex-col items-center justify-center h-64 text-slate-600">
                  <div className="text-5xl mb-4">✦</div>
                  <p className="text-sm">Humanized text will appear here</p>
                </div>
              )}
              {isLoading && (
                <div className="flex flex-col items-center justify-center h-64 text-slate-400 gap-4">
                  <svg className="animate-spin w-10 h-10 text-blue-500" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"/>
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z"/>
                  </svg>
                  <p className="text-sm">Claude is rewriting your text…</p>
                </div>
              )}
              {result?.humanized && (
                <div className="font-mono text-xs leading-relaxed text-slate-200 overflow-y-auto max-h-[400px]">
                  {showDiff ? renderDiff(result.original, result.humanized) : result.humanized}
                </div>
              )}
            </div>
          )}

          {/* Detection output */}
          {mode === 'detect' && (
            <div className="min-h-[340px]">
              {!result && !isLoading && (
                <div className="flex flex-col items-center justify-center h-64 text-slate-600">
                  <div className="text-5xl mb-4">🔍</div>
                  <p className="text-sm">Detection results will appear here</p>
                </div>
              )}
              {isLoading && (
                <div className="flex flex-col items-center justify-center h-64 text-slate-400 gap-4">
                  <svg className="animate-spin w-10 h-10 text-blue-500" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"/>
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z"/>
                  </svg>
                  <p className="text-sm">Analyzing with Claude + statistics…</p>
                </div>
              )}
              {result && (
                <div className="space-y-5">
                  {/* Probability meter */}
                  <div>
                    <div className="flex justify-between text-sm mb-2">
                      <span className="text-white font-semibold">AI Probability</span>
                      <span className={`font-bold ${
                        result.probabilityPct >= 75 ? 'text-red-400' :
                        result.probabilityPct >= 50 ? 'text-yellow-400' : 'text-green-400'
                      }`}>{result.probabilityPct}%</span>
                    </div>
                    <div className="h-4 rounded-full bg-slate-700 overflow-hidden">
                      <div className={`h-full rounded-full transition-all duration-700 ${
                        result.probabilityPct >= 75 ? 'bg-red-500' :
                        result.probabilityPct >= 50 ? 'bg-yellow-500' : 'bg-green-500'
                      }`} style={{ width: `${result.probabilityPct}%` }} />
                    </div>
                  </div>
                  <div className={`rounded-xl px-4 py-3 text-sm font-semibold border ${
                    result.probabilityPct >= 75 ? 'bg-red-900/40 text-red-300 border-red-700/50' :
                    result.probabilityPct >= 50 ? 'bg-yellow-900/40 text-yellow-300 border-yellow-700/50' :
                    'bg-green-900/40 text-green-300 border-green-700/50'
                  }`}>{result.riskLabel}</div>
                  <div>
                    <p className="text-xs text-slate-400 font-semibold mb-2 uppercase tracking-wider">Reasoning</p>
                    <p className="text-sm text-slate-300">{result.reasoning}</p>
                  </div>
                  {result.indicators?.length > 0 && (
                    <div>
                      <p className="text-xs text-slate-400 font-semibold mb-2 uppercase tracking-wider">Indicators</p>
                      <div className="flex flex-wrap gap-2">
                        {result.indicators.map((ind, i) => (
                          <span key={i} className="badge-yellow text-xs">{ind}</span>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}








