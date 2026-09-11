package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entities.*
import com.example.domain.ai.AIService
import com.example.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class InterviewRepository(
    private val database: AppDatabase,
    private val aiService: AIService = AIService()
) {
    private val candidateDao = database.candidateDao()
    private val interviewDao = database.interviewDao()
    private val assessmentDao = database.assessmentDao()
    private val auditDao = database.auditDao()

    val allCandidates: Flow<List<CandidateEntity>> = candidateDao.getAllCandidates()
    val allReports: Flow<List<AssessmentReportEntity>> = assessmentDao.getAllReports()
    val allAuditLogs: Flow<List<AuditLogEntity>> = auditDao.getAllLogs()

    suspend fun registerCandidate(name: String, email: String, phone: String): CandidateEntity = withContext(Dispatchers.IO) {
        val candidate = CandidateEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            email = email,
            phone = phone,
            rawResumeText = "",
            parsedProfileJson = "",
            correctedProfileJson = null,
            detectedDomain = "Pending Evaluation",
            likelyRolesJson = "[]",
            experienceLevel = "Pending",
            status = "REGISTERED"
        )
        candidateDao.insertCandidate(candidate)
        logAudit(candidate.id, "REGISTRATION", "Candidate Account Created", "Candidate $name registered with email: $email")
        candidate
    }

    suspend fun processResume(candidateId: String, resumeText: String): Pair<CandidateProfile, ProfessionalAnalysis> = withContext(Dispatchers.IO) {
        logAudit(candidateId, "RESUME_PARSING", "Resume Processing Started", "Initiated domain-agnostic parsing on ${resumeText.length} characters of resume text")

        val (profile, analysis) = aiService.resumeParser.parseResume(resumeText)

        val rolesJson = JSONArray().apply {
            for (r in analysis.possibleRoles) put(r)
        }.toString()

        val existing = candidateDao.getCandidateById(candidateId)
        val updated = existing?.copy(
            name = if (profile.name.isNotBlank() && profile.name != "Candidate") profile.name else existing.name,
            email = if (profile.email.isNotBlank()) profile.email else existing.email,
            phone = if (profile.phone.isNotBlank()) profile.phone else existing.phone,
            rawResumeText = resumeText,
            parsedProfileJson = ModelJsonSerializer.profileToJson(profile),
            detectedDomain = analysis.primaryDomain,
            likelyRolesJson = rolesJson,
            experienceLevel = analysis.experienceLevel,
            status = "RESUME_PARSED"
        ) ?: CandidateEntity(
            id = candidateId,
            name = profile.name,
            email = profile.email,
            phone = profile.phone,
            rawResumeText = resumeText,
            parsedProfileJson = ModelJsonSerializer.profileToJson(profile),
            correctedProfileJson = null,
            detectedDomain = analysis.primaryDomain,
            likelyRolesJson = rolesJson,
            experienceLevel = analysis.experienceLevel,
            status = "RESUME_PARSED"
        )

        candidateDao.insertCandidate(updated)
        logAudit(
            candidateId,
            "DOMAIN_DETECTION",
            "Domain & Role Identified",
            "Identified Primary Domain: '${analysis.primaryDomain}', Likely Roles: ${analysis.possibleRoles.joinToString()}, Level: ${analysis.experienceLevel}"
        )
        Pair(profile, analysis)
    }

    suspend fun saveCandidateCorrections(candidateId: String, correctedProfile: CandidateProfile) = withContext(Dispatchers.IO) {
        val existing = candidateDao.getCandidateById(candidateId) ?: return@withContext
        val updated = existing.copy(
            name = correctedProfile.name,
            correctedProfileJson = ModelJsonSerializer.profileToJson(correctedProfile),
            status = "CONFIRMED"
        )
        candidateDao.updateCandidate(updated)
        logAudit(candidateId, "CANDIDATE_CONFIRMATION", "Profile Details Confirmed", "Candidate reviewed and submitted corrections for their parsed resume data.")
    }

    suspend fun startInterview(
        candidateId: String,
        config: InterviewConfig,
        analysis: ProfessionalAnalysis
    ): Pair<InterviewSessionEntity, List<InterviewQuestion>> = withContext(Dispatchers.IO) {
        val candidate = candidateDao.getCandidateById(candidateId)
        val profile = if (!candidate?.correctedProfileJson.isNullOrBlank()) {
            ModelJsonSerializer.jsonToProfile(candidate!!.correctedProfileJson!!)
        } else if (!candidate?.parsedProfileJson.isNullOrBlank()) {
            ModelJsonSerializer.jsonToProfile(candidate!!.parsedProfileJson)
        } else {
            CandidateProfile()
        }

        logAudit(
            candidateId,
            "INTERVIEW_PLANNING",
            "Interview Session Configured",
            "Mode: ${config.interviewType}, Total Questions: ${config.questionCount}, Difficulty: ${config.difficulty}"
        )

        val questions = aiService.questionGenerator.generateQuestions(profile, analysis, config)

        val interviewId = UUID.randomUUID().toString()
        val session = InterviewSessionEntity(
            id = interviewId,
            candidateId = candidateId,
            interviewType = config.interviewType.name,
            jobDescription = config.jobDescription,
            configJson = "",
            status = "IN_PROGRESS",
            currentQuestionIndex = 0,
            totalQuestions = questions.size
        )
        interviewDao.insertInterview(session)

        val questionEntities = questions.map { q ->
            InterviewQuestionEntity(
                id = q.id,
                interviewId = interviewId,
                questionNumber = q.questionNumber,
                totalQuestions = q.totalQuestions,
                category = q.category.name,
                difficulty = q.difficulty,
                questionText = q.questionText,
                relatedClaim = q.relatedClaim,
                candidateAnswer = "",
                followUpPrompt = null,
                followUpAnswer = null,
                isAnswered = false,
                evaluationJson = null
            )
        }
        interviewDao.insertQuestions(questionEntities)

        candidateDao.updateCandidate(candidate!!.copy(status = "INTERVIEW_IN_PROGRESS"))
        logAudit(candidateId, "INTERVIEW_STARTED", "Dynamic Questions Generated", "Generated ${questions.size} resume-specific questions for ${analysis.primaryDomain}")

        Pair(session, questions)
    }

    suspend fun submitQuestionAnswer(
        interviewId: String,
        questionId: String,
        candidateAnswer: String,
        allowFollowUp: Boolean,
        domain: String
    ): Pair<String?, InterviewQuestionEntity?> = withContext(Dispatchers.IO) {
        val questions = interviewDao.getQuestionsList(interviewId)
        val question = questions.find { it.id == questionId } ?: return@withContext Pair(null, null)

        var followUpQuestion: String? = null
        if (allowFollowUp && question.questionNumber > 1 && question.followUpPrompt == null) {
            // Check if adaptive follow-up is triggered
            val tempQ = InterviewQuestion(
                id = question.id,
                questionNumber = question.questionNumber,
                totalQuestions = question.totalQuestions,
                category = try { InterviewCategory.valueOf(question.category) } catch (e: Exception) { InterviewCategory.DOMAIN_KNOWLEDGE },
                difficulty = question.difficulty,
                questionText = question.questionText,
                relatedClaim = question.relatedClaim
            )
            followUpQuestion = aiService.followUpGenerator.generateFollowUp(tempQ, candidateAnswer, domain)
        }

        val updatedQ = question.copy(
            candidateAnswer = candidateAnswer,
            followUpPrompt = followUpQuestion,
            isAnswered = followUpQuestion == null
        )

        // If no follow-up is requested, evaluate immediately
        if (followUpQuestion == null) {
            val eval = evaluateAndRecord(updatedQ, domain)
            val withEval = updatedQ.copy(evaluationJson = eval)
            interviewDao.updateQuestion(withEval)
            advanceInterviewProgress(interviewId)
            return@withContext Pair(null, withEval)
        } else {
            interviewDao.updateQuestion(updatedQ)
            return@withContext Pair(followUpQuestion, updatedQ)
        }
    }

    suspend fun submitFollowUpAnswer(
        interviewId: String,
        questionId: String,
        followUpAnswer: String,
        domain: String
    ): InterviewQuestionEntity = withContext(Dispatchers.IO) {
        val questions = interviewDao.getQuestionsList(interviewId)
        val question = questions.find { it.id == questionId } ?: throw IllegalArgumentException("Question not found")

        val updatedQ = question.copy(
            followUpAnswer = followUpAnswer,
            isAnswered = true
        )
        val evalJson = evaluateAndRecord(updatedQ, domain)
        val finalQ = updatedQ.copy(evaluationJson = evalJson)
        interviewDao.updateQuestion(finalQ)

        advanceInterviewProgress(interviewId)
        finalQ
    }

    private suspend fun evaluateAndRecord(question: InterviewQuestionEntity, domain: String): String {
        val tempQ = InterviewQuestion(
            id = question.id,
            questionNumber = question.questionNumber,
            totalQuestions = question.totalQuestions,
            category = try { InterviewCategory.valueOf(question.category) } catch (e: Exception) { InterviewCategory.DOMAIN_KNOWLEDGE },
            difficulty = question.difficulty,
            questionText = question.questionText,
            relatedClaim = question.relatedClaim
        )
        val eval = aiService.answerEvaluator.evaluateAnswer(
            question = tempQ,
            candidateAnswer = question.candidateAnswer,
            followUpAnswer = question.followUpAnswer,
            domain = domain,
            resumeClaims = emptyList()
        )
        val obj = JSONObject().apply {
            put("score", eval.score)
            put("domainRelevanceScore", eval.domainRelevanceScore)
            put("depthOfKnowledgeScore", eval.depthOfKnowledgeScore)
            put("communicationScore", eval.communicationScore)
            put("problemSolvingScore", eval.problemSolvingScore)
            put("feedback", eval.feedback)
            put("isConsistentWithResume", eval.isConsistentWithResume)
            put("inconsistencyConcern", eval.inconsistencyConcern ?: "")
        }
        return obj.toString()
    }

    private suspend fun advanceInterviewProgress(interviewId: String) {
        val session = interviewDao.getInterviewById(interviewId) ?: return
        val questions = interviewDao.getQuestionsList(interviewId)
        val answeredCount = questions.count { it.isAnswered }
        val isCompleted = answeredCount >= session.totalQuestions

        val updated = session.copy(
            currentQuestionIndex = answeredCount,
            status = if (isCompleted) "COMPLETED" else "IN_PROGRESS",
            completedAt = if (isCompleted) System.currentTimeMillis() else null
        )
        interviewDao.updateInterview(updated)

        if (isCompleted) {
            val candidate = candidateDao.getCandidateById(session.candidateId)
            if (candidate != null) {
                candidateDao.updateCandidate(candidate.copy(status = "INTERVIEW_COMPLETED"))
                logAudit(candidate.id, "INTERVIEW_COMPLETED", "All Questions Answered", "Completed $answeredCount questions. Preparing final assessment report.")
            }
        }
    }

    suspend fun generateAssessmentReport(
        candidateId: String,
        interviewId: String,
        domain: String,
        analysis: ProfessionalAnalysis
    ): FinalAssessmentReport = withContext(Dispatchers.IO) {
        val candidate = candidateDao.getCandidateById(candidateId) ?: throw IllegalStateException("Candidate not found")
        val questionEntities = interviewDao.getQuestionsList(interviewId)

        val questions = questionEntities.map { q ->
            var evalObj: AnswerEvaluation? = null
            if (!q.evaluationJson.isNullOrBlank()) {
                try {
                    val o = JSONObject(q.evaluationJson!!)
                    evalObj = AnswerEvaluation(
                        score = o.optInt("score", 75),
                        domainRelevanceScore = o.optInt("domainRelevanceScore", 75),
                        depthOfKnowledgeScore = o.optInt("depthOfKnowledgeScore", 75),
                        communicationScore = o.optInt("communicationScore", 75),
                        problemSolvingScore = o.optInt("problemSolvingScore", 75),
                        feedback = o.optString("feedback"),
                        isConsistentWithResume = o.optBoolean("isConsistentWithResume", true),
                        inconsistencyConcern = o.optString("inconsistencyConcern").takeIf { it.isNotBlank() }
                    )
                } catch (ignored: Exception) {}
            }

            InterviewQuestion(
                id = q.id,
                questionNumber = q.questionNumber,
                totalQuestions = q.totalQuestions,
                category = try { InterviewCategory.valueOf(q.category) } catch (e: Exception) { InterviewCategory.DOMAIN_KNOWLEDGE },
                difficulty = q.difficulty,
                questionText = q.questionText,
                relatedClaim = q.relatedClaim,
                candidateAnswer = q.candidateAnswer,
                followUpPrompt = q.followUpPrompt,
                followUpAnswer = q.followUpAnswer,
                isAnswered = q.isAnswered,
                evaluation = evalObj
            )
        }

        val report = aiService.finalAssessmentGenerator.generateAssessment(
            candidateId = candidateId,
            candidateName = candidate.name,
            interviewId = interviewId,
            domain = domain,
            analysis = analysis,
            questions = questions
        )

        val reportEntity = AssessmentReportEntity(
            id = report.id,
            candidateId = candidateId,
            candidateName = candidate.name,
            interviewId = interviewId,
            domain = domain,
            overallScore = report.overallScore,
            recommendation = report.recommendation.label,
            reportJson = ModelJsonSerializer.reportToJson(report),
            recruiterStatus = "Under Recruiter Review"
        )
        assessmentDao.insertReport(reportEntity)
        candidateDao.updateCandidate(candidate.copy(status = "REPORT_GENERATED"))

        logAudit(
            candidateId,
            "FINAL_ASSESSMENT",
            "Assessment Report Ready",
            "Overall Score: ${report.overallScore}/100, Tier: ${report.recommendation.label}, Inconsistencies Flagged: ${report.potentialInconsistencies.size}"
        )

        report
    }

    suspend fun getReportForCandidate(candidateId: String): FinalAssessmentReport? = withContext(Dispatchers.IO) {
        val entity = assessmentDao.getReportForCandidate(candidateId)
        // Since getReportForCandidate returns a Flow, we can also query the table or collect it
        null
    }

    fun observeReportForCandidate(candidateId: String): Flow<FinalAssessmentReport?> {
        return assessmentDao.getReportForCandidate(candidateId).map { entity ->
            if (entity != null) ModelJsonSerializer.jsonToReport(entity.reportJson) else null
        }
    }

    fun observeActiveInterview(candidateId: String): Flow<InterviewSessionEntity?> {
        return interviewDao.getActiveInterviewForCandidate(candidateId)
    }

    fun observeInterviewQuestions(interviewId: String): Flow<List<InterviewQuestionEntity>> {
        return interviewDao.getQuestionsForInterview(interviewId)
    }

    fun observeCandidateAuditLogs(candidateId: String): Flow<List<AuditLogEntity>> {
        return auditDao.getLogsForCandidate(candidateId)
    }

    suspend fun getCandidateById(candidateId: String): CandidateEntity? = withContext(Dispatchers.IO) {
        candidateDao.getCandidateById(candidateId)
    }

    suspend fun updateRecruiterStatus(reportId: String, newStatus: String) = withContext(Dispatchers.IO) {
        assessmentDao.updateRecruiterStatus(reportId, newStatus)
    }

    private suspend fun logAudit(candidateId: String, stage: String, title: String, details: String) {
        val log = AuditLogEntity(
            id = UUID.randomUUID().toString(),
            candidateId = candidateId,
            timestamp = System.currentTimeMillis(),
            actionStage = stage,
            title = title,
            details = details
        )
        auditDao.insertLog(log)
    }
}
