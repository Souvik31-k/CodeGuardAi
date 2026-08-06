# CodeGuard AI Platform

> **An AI-powered Pull Request review platform that automatically analyzes GitHub Pull Requests using specialized AI agents.**

CodeGuard AI Platform is a backend-first intelligent code review system built with **Spring Boot**, **PostgreSQL**, **LangGraph4j**, and **Groq LLMs**. The platform listens to GitHub webhooks, retrieves Pull Request changes, orchestrates AI agents using a workflow graph, and produces intelligent code review classifications that will later be routed to specialist review agents.

---

# 🏗️ Architecture


                        GitHub Pull Request
                               │
                               ▼
                      GitHub Webhook Event
                               │
                               ▼
                     Webhook Verification
                    (HMAC SHA-256 Signature)
                               │
                               ▼
                    Repository Registration
                               │
                               ▼
                    ReviewRun Creation
                               │
                               ▼
                  GitHub REST API Client
              Fetch Pull Request Changed Files
                               │
                               ▼
                      Review Graph Service
                         (LangGraph4j)
                               │
                               ▼
                       Supervisor Agent
                         (Groq LLM)
                               │
                               ▼
             File Classification (AI Decision)
                               │
                               ▼
                 ReviewState Updated

---

# 🚀 Features

## GitHub Integration

- GitHub Webhook Processing
- HMAC SHA-256 Signature Verification
- Repository Registration
- Pull Request Event Handling
- GitHub REST API Integration
- Pull Request Changed File Retrieval

## AI Review Pipeline

- LangGraph4j Workflow Orchestration
- Supervisor Agent
- AI-powered File Classification
- Prompt Engineering
- Structured JSON Response Parsing
- Workflow State Management

## Backend

- Spring Boot
- Spring Data JPA
- PostgreSQL
- Flyway Database Migrations
- Layered Architecture
- DTO Mapping
- Exception Handling
- REST API
- Configuration Properties
- Logging

---

# 🛠️ Technology Stack

| Category | Technology |
|----------|------------|
| Language | Java 24 |
| Framework | Spring Boot |
| Database | PostgreSQL |
| ORM | Spring Data JPA |
| Database Migration | Flyway |
| AI Workflow | LangGraph4j |
| LLM | Groq (Llama 3.3 70B Versatile) |
| Version Control | Git |
| Repository Hosting | GitHub |
| Build Tool | Maven |
| API Testing | Postman |
| Webhook Tunneling | ngrok |

---

# 📁 Project Structure

```text
backend
│
├── controller
├── service
├── github
│   ├── client
│   ├── dto
│   ├── mapper
│   ├── config
│   └── service
│
├── orchestration
│   ├── configuration
│   ├── nodes
│   ├── prompt
│   ├── service
│   ├── state
│   ├── model
│   └── dto
│
├── llm
│   ├── groq
│   ├── dto
│   └── mapper
│
├── model
├── repository
├── exception
├── config
└── security
```

---

# 🔄 Current Workflow

1. Register a GitHub repository.
2. Configure a GitHub webhook.
3. Receive Pull Request webhook events.
4. Verify webhook authenticity.
5. Create a new `ReviewRun`.
6. Fetch changed files using the GitHub REST API.
7. Invoke the LangGraph workflow.
8. Generate the Supervisor prompt.
9. Send the prompt to the Groq LLM.
10. Classify every changed file.
11. Store the classifications in the workflow state.

---

# 🤖 AI Classification Categories

The Supervisor Agent classifies every changed file into exactly one of the following categories:

- SECURITY
- PERFORMANCE
- TEST
- DOCUMENTATION

These classifications will later determine which specialist AI agent is responsible for reviewing each file.

---

# 📌 Project Status

## 🚧 Upcoming Phases

- Security Agent
- Performance Agent
- Test Agent
- Documentation Agent
- Multi-agent orchestration
- Retrieval-Augmented Generation (RAG)
- pgvector integration
- Embedding generation
- Findings persistence
- GitHub Pull Request review comments
- Dashboard & Analytics
- Asynchronous review execution

---

# ⚙️ Running the Project

## Prerequisites

- Java 24+
- Maven
- PostgreSQL
- Git
- ngrok

---

## Environment Variables

Create a `.env` file:

```env
POSTGRES_DB=your_database
POSTGRES_USER=your_username
POSTGRES_PASSWORD=your_password

ENCRYPTION_KEY=your_encryption_key

GROQ_API_KEY=your_groq_api_key

ACCESS_TOKEN=your_github_personal_access_token
```

---

## Start the Application

```bash
mvn spring-boot:run
```

---

## Configure GitHub Webhook

Configure your GitHub repository webhook with:

### Payload URL

```text
https://<your-ngrok-url>/webhook
```

### Content Type

```text
application/json
```

### Secret

Use the webhook secret generated during repository registration.

### Events

- Pull Requests

---

# 🎯 Design Goals

- Modular AI Agent Architecture
- Extensible Workflow Engine
- Domain-Driven Design
- Clean Layered Architecture
- Separation of Concerns
- Provider-independent LLM Integration
- Production-oriented Backend Design

---

# 🔮 Future Vision

CodeGuard AI Platform is being developed as an AI-powered Pull Request review platform where multiple specialist agents collaborate to review code changes, retrieve repository-specific coding standards using Retrieval-Augmented Generation (RAG), generate actionable findings, and publish intelligent review comments directly back to GitHub Pull Requests.

---
