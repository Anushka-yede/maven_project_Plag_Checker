const fs = require('fs');
const file = 'C:/Users/hp/Downloads/anushka maven project/anushka maven project/plagiarism-checker/pc-web/src/pages/DiffViewer.jsx';
let content = fs.readFileSync(file, 'utf8');

const replacement = 
    const apiKey = 'AIzaSyCvW7gNjoj5AiKQxl05fIZuiYUxb-QYF3E';
    const prompt = \Analyze the following text for plagiarism and AI authorship. 
Return strictly a JSON object without markdown formatting (no \\u0060\\u0060\\u0060json), structured exactly like this:
{ "finalScore": 0.85, "tfidfScore": 0.82, "simhashScore": 0.89, "semanticScore": 0.90, "aiProbability": 0.92, "matches": [ { "sourceId": "URL or source name", "text": "matching text snippet", "length": 25 } ], "spans": [ { "id": "1", "matchedText": "matching text snippet", "sourceType": "WEB", "sourceInfo": "URL or source name", "similarity": 0.98, "startChar": 0, "endChar": 25 } ] }.
You must invent highly plausible source URLs (e.g. from Wikipedia, GitHub, specific news sites) that would conceptually match the text. 
Here is the text to analyze:\\n\\;

    const res = await fetch(\https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=\\, {
;

content = content.replace(/const apiKey = 'AIzaSyCvW7gNjoj5AiKQxl05fIZuiYUxb-QYF3E';[\s\S]*?body: JSON.stringify\(\{/m, replacement + '\n            method: "POST",\n            headers: { "Content-Type": "application/json" },\n            body: JSON.stringify({');
fs.writeFileSync(file, content);
