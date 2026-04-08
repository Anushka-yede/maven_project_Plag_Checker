// Content script — injected into every web page
// Adds text selection listener and communicates with background worker

let tooltip = null

document.addEventListener('mouseup', () => {
  const selected = window.getSelection().toString().trim()
  if (!selected || selected.length < 20) {
    removeTooltip()
    return
  }
  showTooltip(selected)
})

document.addEventListener('mousedown', (e) => {
  if (tooltip && !tooltip.contains(e.target)) removeTooltip()
})

function showTooltip(text) {
  removeTooltip()
  const selection = window.getSelection()
  if (!selection.rangeCount) return

  const range = selection.getRangeAt(0).getBoundingClientRect()

  tooltip = document.createElement('div')
  tooltip.id = 'pc-tooltip'
  tooltip.style.cssText = `
    position: fixed;
    top: ${Math.max(range.top - 44, 0)}px;
    left: ${range.left}px;
    z-index: 2147483647;
    background: #1e1b4b;
    border: 1px solid #4f46e5;
    border-radius: 10px;
    padding: 6px 14px;
    display: flex;
    gap: 8px;
    align-items: center;
    box-shadow: 0 8px 24px rgba(0,0,0,0.5);
    font-family: system-ui, -apple-system, sans-serif;
    font-size: 12px;
    cursor: pointer;
    transition: opacity 0.15s;
    animation: pcFadeIn 0.15s ease-out;
  `

  // Add CSS animation
  if (!document.getElementById('pc-style')) {
    const style = document.createElement('style')
    style.id = 'pc-style'
    style.textContent = `
      @keyframes pcFadeIn { from { opacity:0; transform:translateY(4px); } to { opacity:1; transform:translateY(0); } }
    `
    document.head.appendChild(style)
  }

  const btn = document.createElement('button')
  btn.textContent = '🛡 Check Text'
  btn.style.cssText = `
    background: none; border: none; color: #a5b4fc; cursor: pointer;
    font-size: 12px; font-weight: 600; padding: 0; font-family: inherit;
  `
  btn.addEventListener('click', async () => {
    btn.textContent = '⟳ Checking…'
    btn.style.color = '#94a3b8'
    try {
      // Try to get token from background
      const token = await new Promise(resolve => {
        chrome.runtime.sendMessage({ type: 'GET_TOKEN' }, res => resolve(res?.token || null))
      })

      const res = await fetch('http://localhost:8080/api/check-preview', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        body: JSON.stringify({ text: text.slice(0, 5000) }),
      })

      const data = await res.json()
      const risk  = data.aiRisk || 'UNKNOWN'
      const pct   = Math.round((data.aiProbability || 0) * 100)

      const color = risk === 'HIGH' ? '#fca5a5' : risk === 'MEDIUM' ? '#fcd34d' : '#86efac'
      const emoji = risk === 'HIGH' ? '⚠️' : risk === 'MEDIUM' ? '⚡' : '✅'

      btn.textContent = `${emoji} ${risk}: ${pct}% AI risk`
      btn.style.color  = color

      // Auto-remove after 4 seconds
      setTimeout(removeTooltip, 4000)
    } catch (err) {
      btn.textContent = '⚠️ Server offline'
      btn.style.color = '#fca5a5'
      setTimeout(removeTooltip, 3000)
    }
  })

  tooltip.appendChild(btn)
  document.body.appendChild(tooltip)
}

function removeTooltip() {
  if (tooltip) { tooltip.remove(); tooltip = null }
}
