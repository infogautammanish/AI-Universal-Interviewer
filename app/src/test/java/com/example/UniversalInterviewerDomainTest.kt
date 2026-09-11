package com.example

import com.example.data.samples.SampleResumes
import com.example.domain.ai.AIService
import com.example.domain.model.InterviewCategory
import com.example.domain.model.InterviewConfig
import com.example.domain.model.InterviewQuestion
import com.example.domain.model.InterviewType
import com.example.domain.model.RecommendationTier
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class UniversalInterviewerDomainTest {

    private val aiService = AIService()

    @Test
    fun `test domain agnostic parsing on multiple industries`() = runBlocking {
        // 1. Civil Engineering
        val civilSample = SampleResumes.samples.first { it.domain == "Civil Engineering" }
        val (civilProfile, civilAnalysis) = aiService.resumeParser.parseResume(civilSample.content)
        assertEquals("Civil Engineering", civilAnalysis.primaryDomain)
        assertTrue(civilProfile.experience.isNotEmpty() || civilProfile.projects.isNotEmpty())
        assertTrue(civilProfile.skills.any { it.name.contains("STAAD", ignoreCase = true) || it.name.contains("AutoCAD", ignoreCase = true) || it.name.contains("Structural", ignoreCase = true) })

        // 2. Human Resources
        val hrSample = SampleResumes.samples.first { it.domain == "Human Resources" }
        val (hrProfile, hrAnalysis) = aiService.resumeParser.parseResume(hrSample.content)
        assertEquals("Human Resources", hrAnalysis.primaryDomain)
        assertTrue(hrProfile.skills.any { it.name.contains("HRMS", ignoreCase = true) || it.name.contains("Workday", ignoreCase = true) || it.name.contains("Recruitment", ignoreCase = true) })

        // 3. Digital Marketing
        val mktgSample = SampleResumes.samples.first { it.domain == "Digital Marketing" }
        val (_, mktgAnalysis) = aiService.resumeParser.parseResume(mktgSample.content)
        assertEquals("Digital Marketing", mktgAnalysis.primaryDomain)

        // 4. Healthcare & Nursing
        val healthSample = SampleResumes.samples.first { it.domain.contains("Healthcare") }
        val (_, healthAnalysis) = aiService.resumeParser.parseResume(healthSample.content)
        assertEquals("Healthcare & Nursing", healthAnalysis.primaryDomain)

        // 5. Finance
        val finSample = SampleResumes.samples.first { it.domain.contains("Finance") }
        val (_, finAnalysis) = aiService.resumeParser.parseResume(finSample.content)
        assertEquals("Finance & Investment", finAnalysis.primaryDomain)
    }

    @Test
    fun `test personalized question generation anchored in resume claims`() = runBlocking {
        val civilSample = SampleResumes.samples.first { it.domain == "Civil Engineering" }
        val (profile, analysis) = aiService.resumeParser.parseResume(civilSample.content)

        val config = InterviewConfig(
            interviewType = InterviewType.RESUME_ONLY,
            questionCount = 6,
            difficulty = "Intermediate",
            allowFollowUps = true
        )

        val questions = aiService.questionGenerator.generateQuestions(profile, analysis, config)
        assertEquals(6, questions.size)

        // Verify questions reference domain context
        val allQuestionsText = questions.joinToString(" ") { it.questionText }
        assertTrue(
            "Questions should reference candidate or civil domain",
            allQuestionsText.contains("Civil Engineering", ignoreCase = true) ||
            allQuestionsText.contains("Harbor Overpass", ignoreCase = true) ||
            allQuestionsText.contains("Elena", ignoreCase = true) ||
            allQuestionsText.contains("STAAD", ignoreCase = true)
        )
    }

    @Test
    fun `test adaptive follow up generation`() = runBlocking {
        val question = InterviewQuestion(
            id = "q1",
            questionNumber = 2,
            totalQuestions = 6,
            category = InterviewCategory.PROJECTS,
            difficulty = "Intermediate",
            questionText = "Explain your role on the Harbor Overpass Replacement using STAAD.Pro."
        )

        val followUp = aiService.followUpGenerator.generateFollowUp(
            question = question,
            candidateAnswer = "I led the team and modeled the concrete pier jackets in STAAD.Pro.",
            domain = "Civil Engineering"
        )

        assertNotNull(followUp)
        assertTrue("Follow up should be meaningful question", followUp.length > 10)
    }

    @Test
    fun `test inconsistency detection when candidate contradicts claim`() = runBlocking {
        val question = InterviewQuestion(
            id = "q_claim",
            questionNumber = 3,
            totalQuestions = 6,
            category = InterviewCategory.RESUME_VERIFICATION,
            difficulty = "Advanced",
            questionText = "Your resume claims you led the 1.4-mile Harbor Overpass Replacement. Walk me through how you implemented this.",
            relatedClaim = "Led structural analysis and 3D modeling for the 1.4-mile Harbor Overpass"
        )

        // Candidate gives contradictory answer
        val contradictoryAnswer = "To be honest, I did not actually lead it, someone else did and I only watched."
        val evaluation = aiService.answerEvaluator.evaluateAnswer(
            question = question,
            candidateAnswer = contradictoryAnswer,
            followUpAnswer = null,
            domain = "Civil Engineering",
            resumeClaims = emptyList()
        )

        assertFalse("Evaluation should detect inconsistency", evaluation.isConsistentWithResume)
        assertNotNull(evaluation.inconsistencyConcern)
    }

    @Test
    fun `test final assessment report generation`() = runBlocking {
        val civilSample = SampleResumes.samples.first { it.domain == "Civil Engineering" }
        val (profile, analysis) = aiService.resumeParser.parseResume(civilSample.content)

        val questions = listOf(
            InterviewQuestion(
                id = "q1",
                questionNumber = 1,
                totalQuestions = 2,
                category = InterviewCategory.INTRODUCTION,
                difficulty = "Introductory",
                questionText = "Tell me about your background in Civil Engineering.",
                candidateAnswer = "I have 6 years of experience in structural engineering focusing on seismic retrofitting.",
                isAnswered = true,
                targetSkillOrTopic = "Structural Engineering"
            ),
            InterviewQuestion(
                id = "q2",
                questionNumber = 2,
                totalQuestions = 2,
                category = InterviewCategory.PROJECTS,
                difficulty = "Intermediate",
                questionText = "Describe your STAAD.Pro modeling for the Harbor Overpass.",
                candidateAnswer = "I built the 3D finite element model for the bridge piers and validated load distributions according to AASHTO LRFD specifications.",
                isAnswered = true,
                targetSkillOrTopic = "STAAD.Pro"
            )
        )

        val report = aiService.finalAssessmentGenerator.generateAssessment(
            candidateId = "cand_123",
            candidateName = "Elena Vance",
            interviewId = "int_123",
            domain = "Civil Engineering",
            analysis = analysis,
            questions = questions
        )

        assertNotNull(report)
        assertEquals("Civil Engineering", report.domain)
        assertTrue(report.overallScore in 50..100)
        assertTrue(report.strengths.isNotEmpty())
        assertTrue(report.verifiedSkills.isNotEmpty())
    }
}
