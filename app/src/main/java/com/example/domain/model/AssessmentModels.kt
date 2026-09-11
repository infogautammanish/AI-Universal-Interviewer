package com.example.domain.model

enum class RecommendationTier(val label: String, val badgeColorHex: String) {
    STRONG_CANDIDATE("Strong Candidate", "#10B981"),
    POTENTIAL_CANDIDATE("Potential Candidate", "#0284C7"),
    NEEDS_FURTHER_EVALUATION("Needs Further Evaluation", "#F59E0B"),
    INSUFFICIENT_EVIDENCE("Insufficient Evidence", "#64748B")
}

data class FinalAssessmentReport(
    val id: String,
    val candidateId: String,
    val candidateName: String,
    val interviewId: String,
    val overallScore: Int,
    val domain: String,
    val likelyRoles: List<String>,
    val experienceLevel: String,
    val strengths: List<String>,
    val weaknesses: List<String>,
    val verifiedSkills: List<String>,
    val skillsNeedingVerification: List<String>,
    val projectUnderstanding: Int,
    val technicalOrDomainKnowledge: Int,
    val problemSolving: Int,
    val communication: Int,
    val resumeConsistency: Int,
    val potentialInconsistencies: List<InconsistencyFlag>,
    val recommendation: RecommendationTier,
    val summary: String,
    val recruiterNotes: String = "",
    val recruiterStatus: String = "Under Recruiter Review", // Shortlisted, Next Round, Archived, Offer Extended
    val createdAt: Long = System.currentTimeMillis()
)

data class AuditLog(
    val id: String,
    val candidateId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actionStage: String, // RESUME_PARSING, DOMAIN_DETECTION, INTERVIEW_PLANNING, QUESTION_EVALUATION, INCONSISTENCY_CHECK, FINAL_ASSESSMENT
    val title: String,
    val details: String
)
