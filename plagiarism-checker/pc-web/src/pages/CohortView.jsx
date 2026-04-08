import { useParams, useNavigate } from 'react-router-dom'
import { ArrowLeft } from 'lucide-react'
import { useQuery } from '@tanstack/react-query'
import { useEffect, useRef } from 'react'
import * as d3 from 'd3'
import api from '../api/client'

export default function CohortView() {
  const { id: assignmentId } = useParams()
  const navigate = useNavigate()

  const { data: cells = [], isLoading } = useQuery({
    queryKey: ['cohort', assignmentId],
    queryFn: () => api.get(`/reports/cohort/${assignmentId}`).then(r => r.data),
  })

  return (
    <div className="animate-slide-up space-y-6">
      <button 
        onClick={() => navigate('/dashboard')}
        className="flex items-center gap-2 text-slate-400 hover:text-white transition-colors text-sm font-medium mb-2"
      >
        <ArrowLeft size={16} /> Back to Dashboard
      </button>

      <div>
        <h1 className="text-3xl font-bold text-white flex items-center gap-3">
          <span>⬛</span> Cohort Heatmap
        </h1>
        <p className="text-slate-400 mt-1">
          Assignment: <span className="text-blue-400 font-mono">{assignmentId}</span>
          {' '}— similarity matrix across all submissions.
        </p>
      </div>

      <div className="card">
        {isLoading ? (
          <div className="flex items-center justify-center h-64">
            <svg className="animate-spin w-10 h-10 text-blue-500" fill="none" viewBox="0 0 24 24">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"/>
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z"/>
            </svg>
          </div>
        ) : cells.length === 0 ? (
          <div className="text-center py-20 text-slate-500">
            <div className="text-6xl mb-4">⬛</div>
            <p className="text-lg font-semibold text-white">No cohort data yet</p>
            <p className="text-sm mt-2">Upload multiple submissions for assignment <strong>{assignmentId}</strong> to see the heatmap.</p>
          </div>
        ) : (
          <ScoreHeatmap cells={cells} />
        )}
      </div>

      {/* Legend */}
      <div className="card flex items-center gap-6 flex-wrap">
        <span className="text-xs text-slate-400 font-semibold">Score key:</span>
        {[
          { color: '#10b981', label: '< 40% — Low risk' },
          { color: '#f59e0b', label: '40–75% — Medium risk' },
          { color: '#ef4444', label: '> 75% — High risk' },
        ].map(l => (
          <div key={l.label} className="flex items-center gap-2">
            <div className="w-4 h-4 rounded" style={{ background: l.color }} />
            <span className="text-xs text-slate-300">{l.label}</span>
          </div>
        ))}
      </div>
    </div>
  )
}

function ScoreHeatmap({ cells }) {
  const svgRef = useRef(null)

  useEffect(() => {
    if (!cells.length || !svgRef.current) return

    const docIds = [...new Set([...cells.map(c => c.docAId), ...cells.map(c => c.docBId)])]
    const n = docIds.length
    const cellSize = Math.min(60, Math.floor(800 / (n + 1)))
    const margin = { top: 100, left: 100, right: 20, bottom: 20 }
    const W = cellSize * n + margin.left + margin.right
    const H = cellSize * n + margin.top + margin.bottom

    const svg = d3.select(svgRef.current)
    svg.selectAll('*').remove()
    svg.attr('width', W).attr('height', H)

    const g = svg.append('g').attr('transform', `translate(${margin.left},${margin.top})`)

    const colorScale = d3.scaleLinear()
      .domain([0, 0.4, 0.75, 1])
      .range(['#1e293b', '#10b981', '#f59e0b', '#ef4444'])

    // Build score lookup
    const scoreMap = {}
    cells.forEach(c => { scoreMap[`${c.docAId}|${c.docBId}`] = c.score })

    // Draw cells
    docIds.forEach((rowId, i) => {
      docIds.forEach((colId, j) => {
        const score = scoreMap[`${rowId}|${colId}`] ?? scoreMap[`${colId}|${rowId}`] ?? (i === j ? 1 : 0)
        const label = rowId.substring(0, 6)

        g.append('rect')
          .attr('x', j * cellSize).attr('y', i * cellSize)
          .attr('width', cellSize - 2).attr('height', cellSize - 2)
          .attr('rx', 4).attr('fill', colorScale(score))
          .attr('class', 'cursor-pointer transition-opacity hover:opacity-80')
          .append('title').text(`${rowId.slice(0,8)} × ${colId.slice(0,8)}: ${(score*100).toFixed(0)}%`)

        if (cellSize > 30) {
          g.append('text')
            .attr('x', j * cellSize + cellSize / 2)
            .attr('y', i * cellSize + cellSize / 2 + 5)
            .attr('text-anchor', 'middle').attr('font-size', 11)
            .attr('fill', score > 0.5 ? 'white' : '#94a3b8')
            .text(`${(score * 100).toFixed(0)}%`)
        }
      })
    })

    // X-axis labels
    docIds.forEach((id, j) => {
      g.append('text')
        .attr('x', j * cellSize + cellSize / 2).attr('y', -10)
        .attr('text-anchor', 'middle').attr('font-size', 9)
        .attr('fill', '#94a3b8').attr('transform', `rotate(-45, ${j * cellSize + cellSize / 2}, -10)`)
        .text(id.substring(0, 6) + '…')
    })

    // Y-axis labels
    docIds.forEach((id, i) => {
      g.append('text')
        .attr('x', -8).attr('y', i * cellSize + cellSize / 2 + 4)
        .attr('text-anchor', 'end').attr('font-size', 9).attr('fill', '#94a3b8')
        .text(id.substring(0, 6) + '…')
    })
  }, [cells])

  return (
    <div className="overflow-auto">
      <svg ref={svgRef} className="block mx-auto" />
    </div>
  )
}

