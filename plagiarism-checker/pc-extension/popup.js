const API_BASE = 'http://localhost:8080/api'

const textInput = document.getElementById('textInput')
const checkBtn  = document.getElementById('checkBtn')
const grabBtn   = document.getElementById('grabBtn')
const resultEl  = document.getElementById('result')

// Grab selected text from the active tab
grabBtn.addEventListener('click', async () => {
  try {
    const [tab] = await chrome.tabs.query({ active: true, currentWindow: true })
    const [{ result }] = await chrome.scripting.executeScript({
      target: { tabId: tab.id },
      func: () => window.getSelection().toString(),
    })
    if (result && result.trim()) {
      textInput.value = result.trim().slice(0, 10000)
    } else {
      showResult('medium', 'No Text Selected', 'Please select text on the page first.')
    }
  } catch (e) {
    showResult('medium', 'Permission Needed', 'Cannot access this page. Try a different tab.')
  }
})

// Check the text
checkBtn.addEventListener('click', async () => {
  const text = textInput.value.trim()
  if (!text || text.length < 10) {
    showResult('medium', 'Too Short', 'Please enter at least 10 characters.')
    return
  }

  checkBtn.disabled = true
  checkBtn.innerHTML = '<span class="spinner">⟳</span> Checking…'
  resultEl.style.display = 'none'

  try {
    const token = await getToken()
    const res = await fetch(`${API_BASE}/check-preview`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: JSON.stringify({ text }),
    })

    if (!res.ok) throw new Error(`Server error ${res.status}`)
    const data = await res.json()

    const risk  = data.aiRisk || 'LOW'
    const pct   = Math.round((data.aiProbability || 0) * 100)
    const label = risk === 'HIGH'   ? `⚠️ HIGH RISK — ${pct}% AI probability` :
                  risk === 'MEDIUM' ? `⚡ MEDIUM RISK — ${pct}% AI probability` :
                                     `✅ LOW RISK — ${pct}% AI probability`

    showResult(risk.toLowerCase(), label, data.message || 'Check complete.')
  } catch (err) {
    showResult('medium', '⚠️ Connection Error',
      'Cannot reach the server. Make sure Plagiarism Checker Pro is running on localhost:8080.')
  } finally {
    checkBtn.disabled = false
    checkBtn.textContent = '🔍 Check Now'
  }
})

function showResult(level, risk, msg) {
  resultEl.className = `result ${level}`
  resultEl.style.display = 'block'
  resultEl.innerHTML = `<div class="risk">${risk}</div><div class="msg">${msg}</div>`
}

async function getToken() {
  return new Promise(resolve =>
    chrome.storage.local.get(['pc_token'], r => resolve(r.pc_token || null))
  )
}
