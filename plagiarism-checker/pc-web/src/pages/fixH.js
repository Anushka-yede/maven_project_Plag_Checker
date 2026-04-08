const fs = require('fs');
const file = 'C:/Users/hp/Downloads/anushka maven project/anushka maven project/plagiarism-checker/pc-web/src/pages/Humanizer.jsx';
let content = fs.readFileSync(file, 'utf8');

const replacement1 = 
    mutationFn: async (text) => {
        try {
          const apiKey = 'AIzaSyCvW7gNjoj5AiKQxl05fIZuiYUxb-QYF3E';
          const res = await fetch(\https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=\\, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
              contents: [{ parts: [{ text: "Humanize this text to sound more natural, engaging, and less like AI. Only return the final rewritten text, with no extra commentary or markdown brackets:\\n" + text }] }],
              generationConfig: { temperature: 0.7 }
            })
          });
          const data = await res.json();
          return {
            original: text,
            humanized: data.candidates[0].content.parts[0].text.trim(),
            scoreImprovement: 0.85
          };
        } catch (e) {
          console.error(e);
          return { original: text, humanized: text + ' (Error connected to AI fallback)', scoreImprovement: 0 };
        }
    },
;

const replacement2 = 
    mutationFn: async (text) => {
        try {
          const apiKey = 'AIzaSyCvW7gNjoj5AiKQxl05fIZuiYUxb-QYF3E';
          const res = await fetch(\https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=\\, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
              contents: [{ parts: [{ text: "Analyze the following text and determine the probability it was AI generated. Return strictly JSON: { \\"aiProbability\\": 0.85, \\"verdict\\": \\"HIGHLY LIKELY AI\\" }. Text:\\n" + text }] }],
              generationConfig: { temperature: 0.1 }
            })
          });
          const data = await res.json();
          const raw = data.candidates[0].content.parts[0].text.replace(/\\\\\\\\\json/g, '').replace(/\\\\\\\\\/g, '').trim();
          return JSON.parse(raw);
        } catch (e) {
          console.error(e);
          return { aiProbability: 0, verdict: 'ERROR' };
        }
      },
;

content = content.replace(/mutationFn: async \(text\) => \{[\s\S]*?scoreImprovement: 0\.85\r?\n\s*\};\r?\n\s*\}\r?\n\s*\}/m, replacement1.trim());
content = content.replace(/mutationFn: async \(text\) => \{[\s\S]*?verdict: 'HIGHLY LIKELY AI'\r?\n\s*\};\r?\n\s*\}\r?\n\s*\}/m, replacement2.trim());

fs.writeFileSync(file, content);
