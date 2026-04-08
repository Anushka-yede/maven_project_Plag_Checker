/**
 * AiGauge — radial gauge showing 0–100% AI authorship probability.
 * Feature 02 — AI Authorship Detector.
 */
export default function AiGauge({ value = 0, size = 64 }) {
  const pct     = Math.round(value * 100)
  const radius  = size / 2 - 6
  const circ    = 2 * Math.PI * radius
  const offset  = circ - (pct / 100) * circ

  const color = pct >= 75 ? '#ef4444' : pct >= 50 ? '#f59e0b' : '#10b981'

  return (
    <div className="flex flex-col items-center gap-1">
      <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`}>
        {/* Track */}
        <circle cx={size/2} cy={size/2} r={radius}
                fill="none" stroke="#1e293b" strokeWidth={6} />
        {/* Progress */}
        <circle cx={size/2} cy={size/2} r={radius}
                fill="none" stroke={color} strokeWidth={6}
                strokeDasharray={circ}
                strokeDashoffset={offset}
                strokeLinecap="round"
                transform={`rotate(-90 ${size/2} ${size/2})`}
                style={{ transition: 'stroke-dashoffset 0.6s ease, stroke 0.4s ease' }} />
        {/* Label */}
        <text x={size/2} y={size/2 + 5}
              textAnchor="middle" fill={color}
              fontSize={size < 50 ? 10 : 13} fontWeight="700">
          {pct}%
        </text>
      </svg>
    </div>
  )
}
