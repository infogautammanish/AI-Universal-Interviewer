package com.example.domain.model

enum class InterviewType {
    RESUME_ONLY,
    JOB_SPECIFIC
}

enum class InterviewCategory(val label: String) {
    INTRODUCTION("Introduction & Career Context"),
    RESUME_VERIFICATION("Resume Claim Verification"),
    PROJECTS("Project Deep-Dive & Execution"),
    TECHNICAL_SKILLS("Technical & Functional Skills"),
    DOMAIN_KNOWLEDGE("Industry & Domain Knowledge"),
    TOOLS("Tools & Methodologies"),
    PROBLEM_SOLVING("Problem Solving & Analysis"),
    SCENARIO_BASED("Real-world Scenario Assessment"),
    BEHAVIORAL("Behavioral & Collaboration"),
    ROLE_SPECIFIC("Role-Specific Competencies"),
    CAREER_EXPERIENCE("Career Experience & Impact")
}

data class InterviewConfig(
    val interviewType: InterviewType = InterviewType.RESUME_ONLY,
    val questionCount: Int = 8,
    val difficulty: String = "Intermediate", // Entry, Intermediate, Advanced, Expert
    val allowFollowUps: Boolean = true,
    val resumeQuestionsPct: Int = 40,
    val domainQuestionsPct: Int = 40,
    val behavioralQuestionsPct: Int = 20,
    val jobDescription: String = ""
)

data class InterviewQuestion(
    val id: String,
    val questionNumber: Int,
    val totalQuestions: Int,
    val category: InterviewCategory,
    val difficulty: String,
    val questionText: String,
    val relatedClaim: String? = null,
    val targetSkillOrTopic: String = "",
    val candidateAnswer: String = "",
    val followUpPrompt: String? = null,
    val followUpAnswer: String? = null,
    val isAnswered: Boolean = false,
    val evaluation: AnswerEvaluation? = null
)

data class AnswerEvaluation(
    val score: Int = 80, // 0 - 100
    val domainRelevanceScore: Int = 80,
    val depthOfKnowledgeScore: Int = 80,
    val communicationScore: Int = 80,
    val problemSolvingScore: Int = 80,
    val feedback: String = "",
    val isConsistentWithResume: Boolean = true,
    val inconsistencyConcern: String? = null
)

data class InconsistencyFlag(
    val claim: String,
    val candidateResponse: String,
    val concern: String,
    val severity: String = "Medium" // Low, Medium, High
)
