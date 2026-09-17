# SpeakUp

> **Think quick. Speak better.**

A full-stack speaking-practice web application to improve impromptu speaking, communication, argumentation, research, and storytelling skills.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | React + Vite + JavaScript |
| Backend | Java 25 + Spring Boot 4.1.1 + Maven |
| Database | MySQL |
| API | REST (JSON) |

## Modes

- **Off the Cuff** — Random topics, no prep, timed speaking
- **Research** — Research a topic, then explain it (coming soon)
- **Debate** — Pick a side, make your case (coming soon)
- **Story** — Get a scenario, tell the story (coming soon)

## Local Development Setup

### Prerequisites

- Java 25 (LTS)
- Node.js 20+
- MySQL 8+

### 1. Database

Create the MySQL database:

```sql
CREATE DATABASE speakup_dev;
```

### 2. Backend

```bash
cd speakup-server

# Set your MySQL credentials (or edit application-dev.yml)
set SPEAKUP_DB_USER=root
set SPEAKUP_DB_PASS=yourpassword

# Build and run (Maven Wrapper — no global Maven install needed)
mvnw.cmd compile
mvnw.cmd spring-boot:run
```

Backend runs at `http://localhost:8080`

### 3. Frontend

```bash
cd speakup-client
npm install
npm run dev
```

Frontend runs at `http://localhost:5173`

### 4. Verify

- Health check: `http://localhost:8080/api/v1/health`
- App: `http://localhost:5173`

## Project Structure

```
SpeakUp/
├── speakup-client/    # React + Vite frontend
│   └── src/
│       ├── api/           # API client layer
│       ├── components/    # Reusable UI components
│       ├── features/      # Feature-based pages
│       ├── hooks/         # Custom React hooks
│       ├── styles/        # Global styles + design tokens
│       └── utils/         # Utility functions
│
├── speakup-server/    # Spring Boot backend
│   └── src/main/java/com/speakup/
│       ├── config/        # CORS, web config
│       ├── controller/    # REST controllers
│       ├── service/       # Business logic
│       ├── repository/    # Data access (JPA)
│       ├── model/         # JPA entities + enums
│       ├── dto/           # Data transfer objects
│       ├── mapper/        # Entity ↔ DTO conversion
│       └── exception/     # Error handling
│
└── docs/              # Documentation
```

## Running Tests

```bash
# Backend
cd speakup-server
mvnw.cmd test -Dspring.profiles.active=test

# Frontend
cd speakup-client
npm run build
```

## Git Workflow

- `main` — stable, working code
- `feature/*` — individual feature branches
