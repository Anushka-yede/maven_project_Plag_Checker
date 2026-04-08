# 🛡 Plagiarism Checker Pro

> Enterprise-grade AI-powered plagiarism detection platform — multi-module Maven + React 18 + Claude API

[![Java 21](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/)
[![Spring Boot 3.3](https://img.shields.io/badge/Spring%20Boot-3.3-6DB33F)](https://spring.io/projects/spring-boot)
[![React 18](https://img.shields.io/badge/React-18-61DAFB)](https://react.dev)
[![Apache Lucene](https://img.shields.io/badge/Lucene-9.x-blue)](https://lucene.apache.org)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## ✨ Features (All 20 Implemented)

| # | Feature | Module |
|---|---------|--------|
| 01 | AI Text Humanizer (Claude) | pc-ai, pc-api |
| 02 | AI Authorship Detector (Stat + Claude) | pc-ai, pc-api |
| 03 | TF-IDF Cosine Similarity | pc-engine |
| 04 | Citation Fixer (CrossRef API) | pc-ai, pc-api |
| 05 | SimHash Near-Duplicate Detection | pc-engine |
| 06 | Semantic Similarity (GloVe-50d) | pc-engine |
| 07 | Myers Diff Span Matching | pc-engine |
| 08 | Multi-format Parsing (PDF, DOCX, TXT) | pc-parser |
| 09 | Inline Reviewer Annotations | pc-api, pc-web |
| 10 | PDF Audit Report (iText 8) | pc-api, pc-web |
| 11 | Cohort Heatmap (D3.js N×N) | pc-web |
| 12 | Spring Batch Processing Pipeline | pc-batch |
| 13 | MinIO File Storage | pc-api |
| 14 | JWT Auth + RBAC (Student/Instructor/Admin) | pc-api |
| 15 | Role-based Source Name Blurring | pc-api |
| 16 | SHA-256 Tamper-proof Receipt | pc-api, pc-web |
| 17 | Smart Rewrite Suggestions | pc-ai, pc-web |
| 18 | Live Collaborative Review (WebSocket/STOMP) | pc-api, pc-web |
| 19 | Risk Trend Dashboard (Recharts) | pc-web |
| 20 | Chrome Extension (Manifest V3) | pc-extension |

---

## 🏗 Project Structure

```
plagiarism-checker/
├── pom.xml                  # Parent POM — dependency management
├── docker-compose.yml       # Docker Compose (PostgreSQL, MinIO, API, Web)
├── openapi.yaml             # OpenAPI 3.1 specification
│
├── pc-core/                 # Domain models + NLP utilities
├── pc-parser/               # PDF / DOCX / TXT document parsing
├── pc-engine/               # TF-IDF + SimHash + Semantic + Myers Diff
├── pc-ai/                   # Claude API + Citation Fixer integration
├── pc-batch/                # Spring Batch processing pipelines
├── pc-api/                  # Spring Boot REST API (main app)
└── pc-web/                  # React 18 + Vite + Tailwind frontend
    └── pc-extension/        # Chrome Extension (Manifest V3)
```

---

## 🚀 Quick Start

### Prerequisites
- Java 21+, Maven 3.9+, Node.js 20+
- Docker Desktop
- Anthropic API key (for AI features)

### 1. Start infrastructure
```bash
docker compose up -d postgres minio
```

### 2. Configure environment
```bash
# Windows PowerShell
$env:ANTHROPIC_API_KEY = "sk-ant-..."
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/plagiarism"
$env:SPRING_DATASOURCE_PASSWORD = "pc_secret"
$env:MINIO_URL = "http://localhost:9000"
```

### 3. Build & run the API
```bash
cd plagiarism-checker
mvn -pl pc-api -am spring-boot:run
# API available at: http://localhost:8080
```

### 4. Run the frontend
```bash
cd pc-web
npm install
npm run dev
# Web app at: http://localhost:5173
```

### 5. Full Docker stack
```bash
docker compose up --build
# API: http://localhost:8080
# Web: http://localhost:3000
# MinIO Console: http://localhost:9001 (minioadmin/minioadmin)
```

---

## 🔑 Default Credentials

| Role | Email | Password |
|------|-------|----------|
| Admin | admin@pc.com | Admin@123 |

---

## 🧪 Running Tests

```bash
# All unit tests
mvn verify

# Core only
mvn test -pl pc-core

# AI module (uses Mockito, no real API calls)
mvn test -pl pc-ai
```

---

## 📡 API Examples

```bash
# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@pc.com","password":"Admin@123"}'

# Upload document
curl -X POST http://localhost:8080/api/submissions \
  -H "Authorization: Bearer <token>" \
  -F "file=@document.pdf" \
  -F "assignmentId=CS101-HW1"

# Humanize text
curl -X POST http://localhost:8080/api/humanize \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"text":"Furthermore, it is imperative to acknowledge..."}'

# Check AI probability
curl -X POST http://localhost:8080/api/detect-ai \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"text":"Your text here..."}'
```

---

## 🌐 Chrome Extension

1. Open `chrome://extensions/`
2. Enable **Developer Mode**
3. Click **Load unpacked** → select `pc-extension/` folder
4. Right-click any selected text → **"Check for plagiarism/AI"**

---

## 🏛 Architecture

```
Browser (React 18/Vite)
    │ JWT + REST + WebSocket (STOMP)
    ▼
Spring Boot REST API (pc-api)
    │
    ├── Security: JWT filter + RBAC
    ├── Controllers: Auth, Submissions, Reports, AI, Admin
    ├── MinIO: File storage with SHA-256 receipts
    ├── Spring Batch: Parse → Index → Score → AI pipeline
    │
    ├── pc-engine ──── Lucene TF-IDF + SimHash + Semantic
    ├── pc-ai ──────── Claude API + CrossRef
    └── pc-parser ──── PDFBox + Apache POI + TXT
    │
PostgreSQL (Flyway migrations)
```

---

## 📄 License

MIT © Plagiarism Checker Pro
