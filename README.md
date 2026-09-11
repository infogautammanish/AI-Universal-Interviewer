# 🤖 Universal AI Interviewer

> **AI-powered, domain-agnostic resume evaluation and adaptive interview platform**

Universal AI Interviewer is an AI-powered recruitment platform that evaluates candidates based on their **resume, experience, projects, skills, and interview responses**.

The platform is designed to work across **any professional domain**, including Software Engineering, Data Science, Finance, Healthcare, Civil Engineering, HR, Digital Marketing, Supply Chain, Legal, and more.

---

## 🚀 Overview

Traditional interview systems often use the same predefined questions for every candidate.

**Universal AI Interviewer** takes a different approach.

It analyzes the candidate's resume, understands their professional background, generates personalized questions, conducts an adaptive interview, evaluates responses, detects inconsistencies, and produces a detailed assessment report.

### Core Principle

> **The candidate's resume is the source of truth for evaluating their claimed experience.**

AI helps understand the resume, generate relevant questions, evaluate responses, and identify areas that require further verification.

---

## ✨ Key Features

### 📄 AI Resume Analysis

* Resume parsing and structured profile extraction
* Automatic domain identification
* Seniority-level detection
* Role identification
* Skills and technology extraction
* Project and experience extraction
* Certification extraction
* Explicit vs inferred skill classification
* Identification of verifiable claims

### 👤 Candidate Profile Confirmation

Candidates can review AI-extracted information before starting the interview.

They can verify or edit:

* Professional domain
* Seniority level
* Skills
* Projects
* Experience
* Core competencies

This confirmed profile is used for personalized question generation.

---

## 🎯 Interview Modes

### Resume Interview

Evaluates the candidate strictly around their resume and claimed experience.

The interview focuses on:

* Skills
* Projects
* Technologies
* Responsibilities
* Achievements
* Practical knowledge
* Professional experience

### Job-Specific Interview

Allows recruiters to provide a Job Description and evaluate the candidate against the target role.

The system analyzes:

* Resume-to-job alignment
* Required skills
* Relevant experience
* Skill gaps
* Strengths
* Areas requiring verification

---

## 🧠 Adaptive AI Interview

Questions are dynamically generated based on the candidate's actual background.

### Interview Categories

1. Introduction
2. Resume Claim Verification
3. Project Deep Dive
4. Domain Knowledge
5. Scenario Handling
6. Behavioral Questions

### Adaptive Follow-Ups

The AI analyzes each response and can generate contextual follow-up questions to investigate:

* Practical involvement
* Technical depth
* Problem-solving ability
* Decision-making
* Project ownership
* Understanding of claimed work
* Potential inconsistencies

This creates a more natural and personalized interview experience.

---

## 🎙️ AI Voice Support

The Android application includes Text-to-Speech functionality.

Candidates can:

* Listen to interview questions
* Control question narration
* Follow the interview through an audio-assisted experience

---

## 🔍 Resume Consistency Analysis

The AI continuously compares interview responses with the information contained in the resume.

Potential issues can include:

* Contradictory statements
* Timeline inconsistencies
* Technology mismatches
* Lack of practical understanding
* Project knowledge gaps
* Experience-depth mismatches

These findings are presented as **verification signals**, not automatic proof of dishonesty.

---

## 📊 AI Assessment Report

After completing the interview, the system generates a comprehensive candidate assessment.

### Overall Evaluation

* Overall Score: **0–100**
* Recommendation Tier
* Executive Summary

### Competency Evaluation

* Technical / Domain Knowledge
* Project Understanding
* Problem Solving
* Communication Clarity
* Resume Consistency

### Additional Insights

* Verified Skills
* Skills Requiring Verification
* Key Strengths
* Weaknesses
* Resume Inconsistencies
* Interview Evidence

### Recommendation Levels

| Recommendation                | Meaning                                                  |
| ----------------------------- | -------------------------------------------------------- |
| 🟢 Strong Candidate           | Strong evidence of role capability                       |
| 🔵 Potential Fit              | Good potential with some areas to verify                 |
| 🟠 Needs Further Verification | Important claims or skills require additional validation |
| 🔴 Insufficient Fit           | Insufficient evidence for the target role                |

---

## 👨‍💼 Recruiter Dashboard

Recruiters can manage candidates through a centralized command center.

### Features

* Candidate pipeline
* Domain filtering
* Candidate status tracking
* Resume/profile review
* AI assessment reports
* Interview results
* Candidate recommendations
* Recruiter actions

### Candidate Status Workflow

```text
New
 ↓
Under Review
 ↓
Shortlisted
 ↓
Interview Scheduled
 ↓
Selected / Rejected
```

Recruiters can also:

* Shortlist candidates
* Recommend candidates for offer
* Archive candidates

---

## 🧾 AI Audit Trail

The platform provides an AI transparency layer that records important evaluation steps.

The audit trail can contain:

* Resume extraction
* Domain detection
* Domain confidence
* Profile generation
* Question-generation strategy
* Answer evaluation
* Follow-up generation
* Consistency checks
* Final assessment generation

---

## 🌍 Domain-Agnostic Architecture

The platform is not restricted to a particular profession or technology.

It can evaluate candidates from different backgrounds, such as:

```text
Software Engineering
Data Science
Data Analytics
Finance
Healthcare
Civil Engineering
Human Resources
Digital Marketing
Supply Chain
Legal
Product Management
And more...
```

The interview strategy is generated according to the candidate's professional context.

---

## 🏗️ Application Architecture

The application follows a modular architecture separating:

```text
UI / Presentation
        ↓
Domain / Business Logic
        ↓
AI Services
        ↓
Data Layer
        ↓
Local Persistence
```

This makes the AI service layer easier to extend or replace without redesigning the entire application.

---

## 🛠️ Tech Stack

### Android

* Kotlin
* Jetpack Compose
* Material Design
* Kotlin Coroutines
* Android Text-to-Speech

### Data

* Room Database
* Local persistence

### AI

* AI-powered resume analysis
* AI-powered question generation
* AI-powered answer evaluation
* Adaptive follow-up generation
* Consistency analysis

---

## ⚙️ Setup & Installation

### Prerequisites

Make sure you have:

* **Android Studio** latest stable version
* **JDK 17+**
* **Android SDK**
* **Git**
* Android Emulator or physical Android device

Check Git:

```bash
git --version
```

Check Java:

```bash
java -version
```

---

### 1. Clone the Repository

```bash
git clone https://github.com/YOUR_USERNAME/universal-ai-interviewer.git
cd universal-ai-interviewer
```

Replace `YOUR_USERNAME` with your GitHub username.

---

### 2. Open the Project

Open **Android Studio** and:

1. Select **Open**
2. Select the cloned `universal-ai-interviewer` folder
3. Allow Gradle Sync to complete
4. Wait for indexing and dependency installation

---

### 3. Configure AI API Key

The AI functionality requires an API key for features such as:

* Resume analysis
* Candidate profile extraction
* Question generation
* Adaptive follow-up questions
* Answer evaluation
* Assessment report generation

**Never hardcode API keys in the source code.**

Example:

```text
OPENAI_API_KEY=your_api_key_here
```

Use a secure secrets/environment configuration appropriate for your development setup.

> ⚠️ Never commit API keys, passwords, tokens, or other secrets to GitHub.

---

### 4. Configure Android SDK

In Android Studio:

```text
File
 → Settings
 → Languages & Frameworks
 → Android SDK
```

Make sure the required SDK and build tools are installed.

---

### 5. Build the Project

#### Windows

```bash
gradlew.bat build
```

#### macOS / Linux

```bash
chmod +x gradlew
./gradlew build
```

Or use Android Studio:

```text
Build → Make Project
```

---

### 6. Run the Application

Start an Android Emulator or connect a physical Android device.

#### Windows

```bash
gradlew.bat installDebug
```

#### macOS / Linux

```bash
./gradlew installDebug
```

Or run directly from Android Studio:

```text
Run → Run 'app'
```

---

### 7. Run Tests

#### Windows

```bash
gradlew.bat test
```

#### macOS / Linux

```bash
./gradlew test
```

For Android instrumented tests:

#### Windows

```bash
gradlew.bat connectedAndroidTest
```

#### macOS / Linux

```bash
./gradlew connectedAndroidTest
```

---

## 🚀 Quick Start

### Windows

```bash
git clone https://github.com/YOUR_USERNAME/universal-ai-interviewer.git
cd universal-ai-interviewer
gradlew.bat build
gradlew.bat installDebug
```

### macOS / Linux

```bash
git clone https://github.com/YOUR_USERNAME/universal-ai-interviewer.git
cd universal-ai-interviewer
chmod +x gradlew
./gradlew build
./gradlew installDebug
```

> Make sure an Android device or emulator is connected before running `installDebug`.

---

## 🔐 Security

Before publishing or deploying:

* Never commit API keys
* Never store secrets directly in source code
* Use secure environment/secrets management
* Restrict API keys to required services
* Rotate credentials if they are exposed

Recommended `.gitignore` entries:

```gitignore
# Secrets
.env
*.key
*.pem
local.properties

# Android
.gradle/
build/
*/build/

# IDE
.idea/
*.iml
```

---

## 🧪 Testing

The project includes automated tests for core domain and application functionality.

The application can be built, tested, and run through the Android development environment.

---

## 🔄 End-to-End Workflow

```text
Candidate Registration
        ↓
Resume Upload
        ↓
AI Resume Parsing
        ↓
Domain & Seniority Detection
        ↓
Skills / Projects / Experience Extraction
        ↓
Candidate Profile Confirmation
        ↓
Interview Configuration
        ↓
Personalized Question Generation
        ↓
AI Interview
        ↓
Adaptive Follow-Up Questions
        ↓
Answer Evaluation
        ↓
Consistency Analysis
        ↓
Final AI Assessment
        ↓
Recruiter Dashboard
        ↓
Candidate Decision
```

---

## 🐛 Troubleshooting

### Gradle Sync Failed

Try:

```bash
gradlew.bat clean
gradlew.bat build
```

On macOS/Linux:

```bash
./gradlew clean
./gradlew build
```

### SDK Not Found

Check:

```text
Android Studio
 → Settings
 → Android SDK
```

and ensure the required SDK is installed.

### Emulator Not Starting

Verify:

* Android Emulator is installed
* A virtual device has been created
* Hardware virtualization is enabled
* The emulator has sufficient RAM and storage

### AI Features Not Working

Check:

* API key configuration
* API provider availability
* API permissions
* Network connectivity
* Secret configuration
* That no API key has been committed to GitHub

---

## 🔮 Future Improvements

* 🎤 Real-time Speech-to-Text
* 🗣️ Full AI voice interviews
* 🌐 Multi-language interviews
* 🎥 Video interviews
* 📈 Advanced recruiter analytics
* 👥 Candidate comparison
* 🔗 ATS integrations
* 📅 Interview scheduling
* ☁️ Cloud-based candidate management
* 🔌 Multiple AI provider support
* 🛡️ Advanced interview integrity features
* 🏢 Organization-level recruiter accounts

---

## 📌 Project Status

**MVP / Functional Prototype**

Universal AI Interviewer is designed as a foundation for a scalable AI-powered recruitment platform capable of evaluating candidates across different industries, domains, and professional backgrounds.

---

## 👨‍💻 Author

**Manish Kumar**

AI & Data Science | Data Analytics | Software Development

---

## 📄 License

This project is currently intended for development and demonstration purposes.
