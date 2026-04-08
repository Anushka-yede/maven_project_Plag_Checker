// Background service worker — Manifest V3
// Handles right-click context menu for quick text checks

chrome.runtime.onInstalled.addListener(() => {
  chrome.contextMenus.create({
    id: 'pc-check',
    title: '🛡 Check for plagiarism/AI',
    contexts: ['selection'],
  })
})

chrome.contextMenus.onClicked.addListener(async (info, tab) => {
  if (info.menuItemId !== 'pc-check') return
  const text = info.selectionText?.trim()
  if (!text) return

  // Store selected text and open popup
  await chrome.storage.local.set({ pendingCheck: text })

  // Inject a quick visual indicator into the page
  try {
    await chrome.scripting.executeScript({
      target: { tabId: tab.id },
      func: (selectedText) => {
        const div = document.createElement('div')
        div.style.cssText = `
          position: fixed; top: 20px; right: 20px; z-index: 999999;
          background: #4f46e5; color: white; padding: 12px 20px;
          border-radius: 12px; font-family: system-ui; font-size: 14px;
          font-weight: 600; box-shadow: 0 8px 32px rgba(0,0,0,0.4);
          animation: slideIn 0.3s ease-out;`
        div.innerHTML = '🛡 Checking with Plagiarism Checker Pro…'
        document.body.appendChild(div)

        // Remove after 2.5s
        setTimeout(() => div.remove(), 2500)

        // Store selected text
        window.__pcSelectedText = selectedText
      },
      args: [text],
    })
  } catch (e) {}
})

// Listen for messages from content script
chrome.runtime.onMessage.addListener((msg, sender, sendResponse) => {
  if (msg.type === 'GET_TOKEN') {
    chrome.storage.local.get(['pc_token'], r => sendResponse({ token: r.pc_token }))
    return true
  }
})
