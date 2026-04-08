import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend } from 'recharts'

/**
 * RiskTrendChart — Recharts line chart showing similarity scores over time.
 * Feature 19 — Risk Trend Dashboard.
 */
export default function RiskTrendChart({ submissions = [] }) {
  if (!submissions.length) return null

  // Build chart data from submissions (sorted by date)
  const sorted = [...submissions]
    .filter(s => s.createdAt)
    .sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt))

  const data = sorted.map((s, i) => ({
    name: new Date(s.createdAt).toLocaleDateString('en-US', { month: 'short', day: 'numeric' }),
    submission: i + 1,
    status: s.status,
  }))

  const CustomTooltip = ({ active, payload, label }) => {
    if (!active || !payload?.length) return null
    return (
      <div className="card py-2 px-3 text-xs space-y-1 border-blue-700/50">
        <p className="text-white font-semibold">{label}</p>
        <p className="text-blue-300">Submission #{payload[0]?.value}</p>
        <p className="text-slate-400">Status: {payload[0]?.payload?.status}</p>
      </div>
    )
  }

  return (
    <div className="h-48">
      <ResponsiveContainer width="100%" height="100%">
        <LineChart data={data} margin={{ top: 5, right: 20, left: -20, bottom: 5 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
          <XAxis dataKey="name" tick={{ fill: '#94a3b8', fontSize: 11 }} axisLine={false} tickLine={false} />
          <YAxis tick={{ fill: '#94a3b8', fontSize: 11 }} axisLine={false} tickLine={false} />
          <Tooltip content={<CustomTooltip />} />
          <Line type="monotone" dataKey="submission" stroke="#6366f1" strokeWidth={2}
                dot={{ fill: '#6366f1', r: 4 }} activeDot={{ r: 6, fill: '#818cf8' }} />
        </LineChart>
      </ResponsiveContainer>
    </div>
  )
}
