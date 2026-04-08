import { useParams, Link, useNavigate } from 'react-router-dom'
import { useQuery, useMutation } from '@tanstack/react-query'
import { useState } from 'react'
import { ArrowLeft } from 'lucide-react'
import api from '../api/client'
import AiGauge from '../components/AiGauge.jsx'
import MatchCard from '../components/MatchCard.jsx'
import InlineAnnotator from '../components/InlineAnnotator.jsx'
import html2pdf from 'html2pdf.js'

export default function DiffViewer() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [selectedSpan, setSelectedSpan] = useState(null)
  const [showAnnotator, setShowAnnotator] = useState(false)

  const { data: report, isLoading, error } = useQuery({
    queryKey: ['report', id],
        queryFn: async () => {
      try {
        const text = localStorage.getItem('file_content_' + id) || 'No text found';
        if (!text || text === 'No text found') {
            return {
              id: id,
              results: [{ finalScore: 0, tfidfScore: 0, simhashScore: 0, semanticScore: 0, aiProbability: 0, matches: []}],
              spans: []
            };
        }
        
        // Call Gemini
        const apiKey = 'AIzaSyBkh75bj_2RWbmQu5cSDCxfdyzXjQJGU2s';
        let fileParts = [];
        
        const basePrompt = `Analyze the following document for plagiarism and AI authorship.
Return strictly a JSON object without markdown formatting (no \`\`\`json), structured exactly like this:
{ "results": [ { "finalScore": 0.85, "tfidfScore": 0.82, "simhashScore": 0.89, "semanticScore": 0.90, "aiProbability": 0.92, "matches": [ { "sourceId": "URL or source name", "text": "matching text snippet", "length": 25 } ] } ], "spans": [ { "id": "1", "matchedText": "matching text snippet", "sourceType": "WEB", "sourceInfo": "URL or source name", "similarity": 0.98, "startChar": 0, "endChar": 25 } ] }.`;
        if (text.startsWith('data:application/pdf;base64,')) {
             const base64 = text.replace('data:application/pdf;base64,', '');
             fileParts = [
                 { text: basePrompt + "\n\nAnalyze this PDF document:" },
                 { inlineData: { mimeType: "application/pdf", data: base64 } }
             ];
        } else {
             fileParts = [
                 { text: basePrompt + "\nHere is the text to analyze:\n" + text.substring(0, 10000) }
             ];
        }

        const res = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${apiKey}`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            contents: [{ parts: fileParts }],
            generationConfig: { temperature: 0.1 }
          })
        });

        const data = await res.json();
        if (data.error) throw new Error(data.error.message);
        if (!data.candidates) throw new Error("No response candidates returned from AI.");
        
        const rawJsonText = data.candidates[0].content.parts[0].text.replace("```json", "").replace("```", "").trim();
        const parsed = JSON.parse(rawJsonText);
        parsed.id = id;
        return parsed;

      } catch (err) {
            console.error(err);
            throw new Error(err.message || "Failed to analyze document.");
      }
    },
  })

  const exportPdf = async () => {
    const element = document.getElementById('report-container');
    const opt = {
      margin:       0.2,
      filename:     `report-${id}.pdf`,
      image:        { type: 'jpeg', quality: 1.0 },
      html2canvas:  { 
        scale: 2, 
        useCORS: true, 
        backgroundColor: '#0B0F1A',
        logging: false
      },
      jsPDF:        { unit: 'in', format: 'letter', orientation: 'portrait' }
    };

    // Temporarily add padding to smooth out edges for the PDF capture
    const originalPadding = element.style.padding;
    const originalBackground = element.style.backgroundColor;
    element.style.padding = '20px';
    element.style.backgroundColor = '#0B0F1A'; // Force the dark background onto the element itself

    await html2pdf().from(element).set(opt).save();

    element.style.padding = originalPadding;
    element.style.backgroundColor = originalBackground;
  }

  const verifyIntegrity = async () => {
    const { data } = await api.get(`/submissions/${id}/integrity`)
    alert(data.message)
  }

  if (isLoading) return <ReportSkeleton />
  if (error)     return <div className="card text-red-400">Error loading report: {error.message}</div>

  const topResult = report?.results?.[0]
  const spans     = report?.spans || []
  const aiProb    = topResult?.aiProbability ?? 0

  return (
    <div id="report-container" className="animate-slide-up space-y-6">
      <button 
        onClick={() => navigate('/dashboard')}
        className="flex items-center gap-2 text-slate-400 hover:text-white transition-colors text-sm font-medium mb-2"
        data-html2canvas-ignore="true"
      >
        <ArrowLeft size={16} /> Back to Dashboard
      </button>

      {/* Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold text-white flex items-center gap-3">
            Similarity Report
            <span className={`text-lg font-semibold px-3 py-1 rounded-full ${
              (topResult?.finalScore ?? 0) >= 0.75 ? 'bg-red-900/60 text-red-300' :
              (topResult?.finalScore ?? 0) >= 0.40 ? 'bg-yellow-900/60 text-yellow-300' :
              'bg-green-900/60 text-green-300'
            }`}>
              {((topResult?.finalScore ?? 0) * 100).toFixed(0)}% similar
            </span>
          </h1>
          <p className="text-slate-400 text-sm mt-1 font-mono">{id}</p>
        </div>
        <div className="flex gap-3 flex-wrap" data-html2canvas-ignore="true">
          <button onClick={verifyIntegrity} className="btn-secondary text-xs py-2">🔒 Verify Integrity</button>
          <button onClick={exportPdf}       className="btn-secondary text-xs py-2">📄 Export PDF</button>
          <Link to="/upload" className="btn-primary text-xs py-2">↑ New Upload</Link>
        </div>
      </div>

      {/* Score breakdown */}
      {topResult && (
        <div className="grid grid-cols-2 md:grid-cols-5 gap-4">
          {[
            { label: 'TF-IDF',    value: topResult.tfidfScore,    icon: '📊' },
            { label: 'SimHash',   value: topResult.simhashScore,  icon: '🔗' },
            { label: 'Semantic',  value: topResult.semanticScore, icon: '🧠' },
            { label: 'Final',     value: topResult.finalScore,    icon: '⚡' },
          ].map(s => (
            <div key={s.label} className="card text-center py-5">
              <div className="text-2xl mb-2">{s.icon}</div>
              <div className={`text-2xl font-bold ${
                s.value >= 0.75 ? 'text-red-400' : s.value >= 0.40 ? 'text-yellow-400' : 'text-green-400'
              }`}>
                {(s.value * 100).toFixed(0)}%
              </div>
              <div className="text-xs text-slate-400 mt-1">{s.label}</div>
            </div>
          ))}
          {/* AI gauge */}
          <div className="card text-center py-5">
            <AiGauge value={aiProb} />
            <div className="text-xs text-slate-400 mt-1">AI Authorship</div>
          </div>
        </div>
      )}

      {/* Matched spans */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2">
          <div className="card">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-lg font-bold text-white">📍 Matched Spans ({spans.length})</h2>
            </div>
            {spans.length === 0 ? (
              <div className="text-center py-10 text-slate-500">
                <div className="text-4xl mb-3">✅</div>
                <p>No matching spans found above threshold.</p>
              </div>
            ) : (
              <div className="space-y-3 max-h-[500px] overflow-y-auto pr-1">
                {spans.map(span => (
                  <MatchCard key={span.id} span={span}
                             onClick={() => { setSelectedSpan(span); setShowAnnotator(true) }} />
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Annotations panel */}
        <div>
          <InlineAnnotator resultId={id}
                           span={selectedSpan}
                           open={showAnnotator}
                           onClose={() => { setShowAnnotator(false); setSelectedSpan(null) }}
                           annotations={report?.annotations || []} />
        </div>
      </div>
    </div>
  )
}

function ReportSkeleton() {
  return (
    <div className="space-y-6 animate-pulse">
      <div className="h-8 w-64 bg-slate-700 rounded-xl" />
      <div className="grid grid-cols-5 gap-4">
        {[1,2,3,4,5].map(i => <div key={i} className="card h-28 bg-slate-800/50" />)}
      </div>
      <div className="card h-96 bg-slate-800/50" />
    </div>
  )
}






