package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "candidates")
data class CandidateEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val rawResumeText: String,
    val parsedProfileJson: String,
    val correctedProfileJson: String?,
    val detectedDomain: String,
    val likelyRolesJson: String,
    val experienceLevel: String,
    val status: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "interview_sessions")
data class InterviewSessionEntity(
    @PrimaryKey val id: String,
    val candidateId: String,
    val interviewType: String,
    val jobDescription: String,
    val configJson: String,
    val status: String,
    val currentQuestionIndex: Int,
    val totalQuestions: Int,
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

@Entity(tableName = "interview_questions")
data class InterviewQuestionEntity(
    @PrimaryKey val id: String,
    val interviewId: String,
    val questionNumber: Int,
    val totalQuestions: Int,
    val category: String,
    val difficulty: String,
    val questionText: String,
    val relatedClaim: String?,
    val candidateAnswer: String,
    val followUpPrompt: String?,
    val followUpAnswer: String?,
    val isAnswered: Boolean,
    val evaluationJson: String?
)

@Entity(tableName = "assessment_reports")
data class AssessmentReportEntity(
    @PrimaryKey val id: String,
    val candidateId: String,
    val candidateName: String,
    val interviewId: String,
    val domain: String,
    val overallScore: Int,
    val recommendation: String,
    val reportJson: String,
    val recruiterStatus: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey val id: String,
    val candidateId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actionStage: String,
    val title: String,
    val details: String
)
