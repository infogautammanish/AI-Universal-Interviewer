package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.repository.ModelJsonSerializer
import com.example.ui.components.LoadingOverlay
import com.example.ui.components.UniversalTopBar
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.InterviewViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp(viewModel: InterviewViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val allCandidates by viewModel.allCandidates.collectAsStateWithLifecycle()
    val allReports by viewModel.allReports.collectAsStateWithLifecycle()
    val allAuditLogs by viewModel.allAuditLogs.collectAsStateWithLifecycle()

    // Handle back button navigation
    BackHandler(enabled = uiState.currentScreen != AppScreen.AUTH && uiState.currentScreen != AppScreen.RECRUITER_DASHBOARD) {
        when (uiState.currentScreen) {
            AppScreen.RESUME_UPLOAD -> viewModel.navigateTo(AppScreen.AUTH)
            AppScreen.CANDIDATE_REVIEW -> viewModel.navigateTo(AppScreen.RESUME_UPLOAD)
            AppScreen.INTERVIEW_CONFIG -> viewModel.navigateTo(AppScreen.CANDIDATE_REVIEW)
            AppScreen.ASSESSMENT_REPORT -> viewModel.navigateTo(AppScreen.INTERVIEW_CONFIG)
            else -> {}
        }
    }

    val screenTitle = when (uiState.currentScreen) {
        AppScreen.AUTH -> "Universal AI Interviewer"
        AppScreen.RESUME_UPLOAD -> "Resume Processing"
        AppScreen.CANDIDATE_REVIEW -> "Verify Profile Details"
        AppScreen.INTERVIEW_CONFIG -> "Interview Setup"
        AppScreen.INTERVIEW_SESSION -> "AI Interview Session"
        AppScreen.ASSESSMENT_REPORT -> "Assessment Evaluation"
        AppScreen.RECRUITER_DASHBOARD -> "Recruiter Dashboard"
    }

    val screenSubtitle = when (uiState.currentScreen) {
        AppScreen.AUTH -> "Domain-Agnostic Talent Intelligence"
        AppScreen.RESUME_UPLOAD -> "Step 1 of 3: Resume Input"
        AppScreen.CANDIDATE_REVIEW -> "Step 2 of 3: AI Verification"
        AppScreen.INTERVIEW_CONFIG -> "Step 3 of 3: Session Rigor"
        AppScreen.INTERVIEW_SESSION -> "Adaptive Conversational Flow"
        AppScreen.ASSESSMENT_REPORT -> "Verified Claims & Scorecard"
        AppScreen.RECRUITER_DASHBOARD -> "Cross-Domain Talent Pipelines"
    }

    val canGoBack = when (uiState.currentScreen) {
        AppScreen.RESUME_UPLOAD, AppScreen.CANDIDATE_REVIEW, AppScreen.INTERVIEW_CONFIG -> true
        else -> false
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            UniversalTopBar(
                title = screenTitle,
                subtitle = screenSubtitle,
                isRecruiterMode = uiState.isRecruiterMode,
                onToggleMode = { viewModel.switchMode(it) },
                onBack = if (canGoBack) {
                    {
                        when (uiState.currentScreen) {
                            AppScreen.RESUME_UPLOAD -> viewModel.navigateTo(AppScreen.AUTH)
                            AppScreen.CANDIDATE_REVIEW -> viewModel.navigateTo(AppScreen.RESUME_UPLOAD)
                            AppScreen.INTERVIEW_CONFIG -> viewModel.navigateTo(AppScreen.CANDIDATE_REVIEW)
                            else -> {}
                        }
                    }
                } else null
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.currentScreen) {
                AppScreen.AUTH -> AuthScreen(
                    name = uiState.candidateName,
                    email = uiState.candidateEmail,
                    phone = uiState.candidatePhone,
                    errorMessage = uiState.errorMessage,
                    onNameChange = { viewModel.updateCandidateAuth(it, uiState.candidateEmail, uiState.candidatePhone) },
                    onEmailChange = { viewModel.updateCandidateAuth(uiState.candidateName, it, uiState.candidatePhone) },
                    onPhoneChange = { viewModel.updateCandidateAuth(uiState.candidateName, uiState.candidateEmail, it) },
                    onRegister = { viewModel.registerAndProceed() },
                    onSwitchToRecruiter = { viewModel.switchMode(true) }
                )

                AppScreen.RESUME_UPLOAD -> ResumeUploadScreen(
                    candidateName = uiState.candidateName,
                    resumeText = uiState.rawResumeText,
                    errorMessage = uiState.errorMessage,
                    onResumeTextChange = { viewModel.setResumeText(it) },
                    onSelectSample = { viewModel.loadSampleResume(it) },
                    onProcessResume = { viewModel.processResume() }
                )

                AppScreen.CANDIDATE_REVIEW -> CandidateReviewScreen(
                    profile = uiState.editableProfile,
                    analysis = uiState.professionalAnalysis,
                    onProfileChange = { viewModel.updateEditableProfile(it) },
                    onConfirmProfile = { viewModel.confirmCandidateProfile() }
                )

                AppScreen.INTERVIEW_CONFIG -> InterviewConfigScreen(
                    domain = uiState.professionalAnalysis.primaryDomain,
                    candidateName = uiState.editableProfile.name.ifBlank { uiState.candidateName },
                    config = uiState.interviewConfig,
                    onConfigChange = { type, count, diff, followUps, jobDesc ->
                        viewModel.updateInterviewConfig(type, count, diff, followUps, jobDesc)
                    },
                    onStartInterview = { viewModel.startInterview() }
                )

                AppScreen.INTERVIEW_SESSION -> InterviewSessionScreen(
                    domain = uiState.professionalAnalysis.primaryDomain,
                    candidateName = uiState.editableProfile.name.ifBlank { uiState.candidateName },
                    questions = uiState.currentQuestions,
                    currentIndex = uiState.currentQuestionIndex,
                    answerText = uiState.currentAnswerText,
                    followUpPrompt = uiState.activeFollowUpPrompt,
                    followUpAnswerText = uiState.activeFollowUpAnswerText,
                    isSpeaking = uiState.isSpeaking,
                    errorMessage = uiState.errorMessage,
                    onAnswerChange = { viewModel.setAnswerText(it) },
                    onFollowUpAnswerChange = { viewModel.setFollowUpAnswerText(it) },
                    onSubmitAnswer = { viewModel.submitAnswer() },
                    onSubmitFollowUp = { viewModel.submitFollowUpAnswer() },
                    onSpeakQuestion = { viewModel.speakCurrentQuestion() },
                    onStopSpeech = { viewModel.stopTts() }
                )

                AppScreen.ASSESSMENT_REPORT -> {
                    val report = uiState.assessmentReport
                    if (report != null) {
                        AssessmentReportScreen(
                            report = report,
                            onStatusChange = { newStatus ->
                                viewModel.updateRecruiterDecision(report.id, newStatus)
                            },
                            onStartNew = {
                                viewModel.navigateTo(AppScreen.AUTH)
                            }
                        )
                    }
                }

                AppScreen.RECRUITER_DASHBOARD -> RecruiterDashboardScreen(
                    candidates = allCandidates,
                    reports = allReports,
                    auditLogs = allAuditLogs,
                    onSelectCandidate = { candidate ->
                        // Inspect candidate
                        viewModel.selectCandidateForReview(candidate)
                    },
                    onViewCandidateReport = { rep ->
                        val parsed = ModelJsonSerializer.jsonToReport(rep.reportJson)
                        if (parsed != null) {
                            // Can view the report
                        }
                    },
                    onStartNewCandidate = {
                        viewModel.switchMode(false)
                        viewModel.navigateTo(AppScreen.AUTH)
                    }
                )
            }

            if (uiState.isLoading) {
                LoadingOverlay(message = uiState.loadingMessage.ifBlank { "Processing..." })
            }
        }
    }
}
