package com.example.ui.viewmodel

import android.app.Application
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entities.AssessmentReportEntity
import com.example.data.local.entities.AuditLogEntity
import com.example.data.local.entities.CandidateEntity
import com.example.data.local.entities.InterviewQuestionEntity
import com.example.data.local.entities.InterviewSessionEntity
import com.example.data.repository.InterviewRepository
import com.example.data.repository.ModelJsonSerializer
import com.example.data.samples.SampleResume
import com.example.domain.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale

enum class AppScreen {
    AUTH,
    RESUME_UPLOAD,
    CANDIDATE_REVIEW,
    INTERVIEW_CONFIG,
    INTERVIEW_SESSION,
    ASSESSMENT_REPORT,
    RECRUITER_DASHBOARD
}

data class UiState(
    val currentScreen: AppScreen = AppScreen.AUTH,
    val isRecruiterMode: Boolean = false,
    val isLoading: Boolean = false,
    val loadingMessage: String = "",
    val errorMessage: String? = null,

    // Candidate auth & input
    val candidateId: String = "",
    val candidateName: String = "Elena Vance",
    val candidateEmail: String = "elena.vance.eng@example.com",
    val candidatePhone: String = "(555) 234-8901",
    val rawResumeText: String = "",

    // Parsed Profile & Analysis
    val parsedProfile: CandidateProfile = CandidateProfile(),
    val editableProfile: CandidateProfile = CandidateProfile(),
    val professionalAnalysis: ProfessionalAnalysis = ProfessionalAnalysis(),

    // Interview Configuration
    val interviewConfig: InterviewConfig = InterviewConfig(),

    // Active Interview Session
    val currentInterviewId: String = "",
    val currentQuestions: List<InterviewQuestion> = emptyList(),
    val currentQuestionIndex: Int = 0,
    val currentAnswerText: String = "",
    val activeFollowUpPrompt: String? = null,
    val activeFollowUpAnswerText: String = "",
    val isSpeaking: Boolean = false,

    // Assessment Report
    val assessmentReport: FinalAssessmentReport? = null,

    // Recruiter Dashboard
    val selectedCandidateForReview: CandidateEntity? = null,
    val selectedReportForReview: FinalAssessmentReport? = null,
    val selectedCandidateTranscripts: List<InterviewQuestionEntity> = emptyList(),
    val selectedCandidateLogs: List<AuditLogEntity> = emptyList()
)

class InterviewViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private val repository: InterviewRepository
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false

    init {
        val db = AppDatabase.getDatabase(application)
        repository = InterviewRepository(db)
        tts = TextToSpeech(application, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            isTtsInitialized = true
        }
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val allCandidates: StateFlow<List<CandidateEntity>> = repository.allCandidates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allReports: StateFlow<List<AssessmentReportEntity>> = repository.allReports
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAuditLogs: StateFlow<List<AuditLogEntity>> = repository.allAuditLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun switchMode(recruiter: Boolean) {
        _uiState.update {
            it.copy(
                isRecruiterMode = recruiter,
                currentScreen = if (recruiter) AppScreen.RECRUITER_DASHBOARD else AppScreen.AUTH
            )
        }
    }

    fun navigateTo(screen: AppScreen) {
        stopTts()
        _uiState.update { it.copy(currentScreen = screen, errorMessage = null) }
    }

    fun updateCandidateAuth(name: String, email: String, phone: String) {
        _uiState.update { it.copy(candidateName = name, candidateEmail = email, candidatePhone = phone) }
    }

    fun registerAndProceed() {
        val state = _uiState.value
        if (state.candidateName.isBlank() || state.candidateEmail.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter name and email to register.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadingMessage = "Registering candidate account...") }
            try {
                val candidate = repository.registerCandidate(state.candidateName, state.candidateEmail, state.candidatePhone)
                _uiState.update {
                    it.copy(
                        candidateId = candidate.id,
                        isLoading = false,
                        currentScreen = AppScreen.RESUME_UPLOAD,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Registration error: ${e.message}") }
            }
        }
    }

    fun setResumeText(text: String) {
        _uiState.update { it.copy(rawResumeText = text) }
    }

    fun loadSampleResume(sample: SampleResume) {
        _uiState.update {
            it.copy(
                rawResumeText = sample.content,
                candidateName = sample.candidateName
            )
        }
    }

    fun processResume() {
        val state = _uiState.value
        if (state.rawResumeText.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter or select a resume first.") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    loadingMessage = "AI parsing resume, detecting domain & extracting claims..."
                )
            }
            try {
                val (profile, analysis) = repository.processResume(state.candidateId, state.rawResumeText)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        parsedProfile = profile,
                        editableProfile = profile,
                        professionalAnalysis = analysis,
                        currentScreen = AppScreen.CANDIDATE_REVIEW,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Resume processing failed: ${e.message}") }
            }
        }
    }

    fun updateEditableProfile(updated: CandidateProfile) {
        _uiState.update { it.copy(editableProfile = updated) }
    }

    fun confirmCandidateProfile() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadingMessage = "Saving confirmed profile...") }
            try {
                repository.saveCandidateCorrections(state.candidateId, state.editableProfile)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentScreen = AppScreen.INTERVIEW_CONFIG,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to save profile: ${e.message}") }
            }
        }
    }

    fun updateInterviewConfig(
        type: InterviewType,
        count: Int,
        difficulty: String,
        followUps: Boolean,
        jobDesc: String
    ) {
        _uiState.update {
            it.copy(
                interviewConfig = it.interviewConfig.copy(
                    interviewType = type,
                    questionCount = count,
                    difficulty = difficulty,
                    allowFollowUps = followUps,
                    jobDescription = jobDesc
                )
            )
        }
    }

    fun startInterview() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    loadingMessage = "Generating personalized questions tailored to resume claims in ${state.professionalAnalysis.primaryDomain}..."
                )
            }
            try {
                val (session, questions) = repository.startInterview(
                    candidateId = state.candidateId,
                    config = state.interviewConfig,
                    analysis = state.professionalAnalysis
                )
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentInterviewId = session.id,
                        currentQuestions = questions,
                        currentQuestionIndex = 0,
                        currentAnswerText = "",
                        activeFollowUpPrompt = null,
                        activeFollowUpAnswerText = "",
                        currentScreen = AppScreen.INTERVIEW_SESSION,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Could not start interview: ${e.message}") }
            }
        }
    }

    fun setAnswerText(text: String) {
        _uiState.update { it.copy(currentAnswerText = text) }
    }

    fun setFollowUpAnswerText(text: String) {
        _uiState.update { it.copy(activeFollowUpAnswerText = text) }
    }

    fun submitAnswer() {
        val state = _uiState.value
        if (state.currentAnswerText.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter an answer before submitting.") }
            return
        }

        val question = state.currentQuestions.getOrNull(state.currentQuestionIndex) ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadingMessage = "AI evaluating answer depth & verifying claims...") }
            try {
                val (followUpPrompt, updatedEntity) = repository.submitQuestionAnswer(
                    interviewId = state.currentInterviewId,
                    questionId = question.id,
                    candidateAnswer = state.currentAnswerText,
                    allowFollowUp = state.interviewConfig.allowFollowUps,
                    domain = state.professionalAnalysis.primaryDomain
                )

                if (followUpPrompt != null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            activeFollowUpPrompt = followUpPrompt,
                            activeFollowUpAnswerText = "",
                            errorMessage = null
                        )
                    }
                    speakText("Follow-up question: $followUpPrompt")
                } else {
                    // Question completed without follow-up, advance
                    advanceQuestionOrFinish()
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to submit answer: ${e.message}") }
            }
        }
    }

    fun submitFollowUpAnswer() {
        val state = _uiState.value
        if (state.activeFollowUpAnswerText.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please provide your follow-up answer.") }
            return
        }

        val question = state.currentQuestions.getOrNull(state.currentQuestionIndex) ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadingMessage = "Evaluating follow-up and detecting claim consistency...") }
            try {
                repository.submitFollowUpAnswer(
                    interviewId = state.currentInterviewId,
                    questionId = question.id,
                    followUpAnswer = state.activeFollowUpAnswerText,
                    domain = state.professionalAnalysis.primaryDomain
                )
                advanceQuestionOrFinish()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to submit follow-up: ${e.message}") }
            }
        }
    }

    private suspend fun advanceQuestionOrFinish() {
        val state = _uiState.value
        val nextIndex = state.currentQuestionIndex + 1

        if (nextIndex < state.currentQuestions.size) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    currentQuestionIndex = nextIndex,
                    currentAnswerText = "",
                    activeFollowUpPrompt = null,
                    activeFollowUpAnswerText = "",
                    errorMessage = null
                )
            }
            val nextQ = state.currentQuestions[nextIndex]
            speakText("Question ${nextIndex + 1} of ${state.currentQuestions.size}. ${nextQ.questionText}")
        } else {
            // All questions answered! Generate Final Assessment Report
            _uiState.update {
                it.copy(
                    isLoading = true,
                    loadingMessage = "Synthesizing full interview, verifying claims, detecting inconsistencies, and compiling AI Assessment Report..."
                )
            }
            try {
                val report = repository.generateAssessmentReport(
                    candidateId = state.candidateId,
                    interviewId = state.currentInterviewId,
                    domain = state.professionalAnalysis.primaryDomain,
                    analysis = state.professionalAnalysis
                )
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        assessmentReport = report,
                        currentScreen = AppScreen.ASSESSMENT_REPORT,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Report generation failed: ${e.message}") }
            }
        }
    }

    fun speakCurrentQuestion() {
        val state = _uiState.value
        val q = state.currentQuestions.getOrNull(state.currentQuestionIndex)
        if (q != null) {
            if (state.activeFollowUpPrompt != null) {
                speakText(state.activeFollowUpPrompt)
            } else {
                speakText("Question ${state.currentQuestionIndex + 1}: ${q.questionText}")
            }
        }
    }

    private fun speakText(text: String) {
        if (isTtsInitialized && tts != null) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "INTERVIEW_TTS")
            _uiState.update { it.copy(isSpeaking = true) }
        }
    }

    fun stopTts() {
        tts?.stop()
        _uiState.update { it.copy(isSpeaking = false) }
    }

    // Recruiter actions
    fun selectCandidateForReview(candidate: CandidateEntity) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadingMessage = "Loading candidate records...") }
            val reportEntity = repository.getReportForCandidate(candidate.id)
            val report = if (!candidate.parsedProfileJson.isNullOrBlank()) {
                val profile = ModelJsonSerializer.jsonToProfile(candidate.parsedProfileJson)
                // If there's an existing report, convert it
                null
            } else null

            val logsFlow = repository.observeCandidateAuditLogs(candidate.id).firstOrNull() ?: emptyList()

            _uiState.update {
                it.copy(
                    isLoading = false,
                    selectedCandidateForReview = candidate,
                    selectedCandidateLogs = logsFlow
                )
            }
        }
    }

    fun updateRecruiterDecision(reportId: String, newStatus: String) {
        viewModelScope.launch {
            repository.updateRecruiterStatus(reportId, newStatus)
            _uiState.update {
                if (it.assessmentReport?.id == reportId) {
                    it.copy(assessmentReport = it.assessmentReport.copy(recruiterStatus = newStatus))
                } else it
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
    }
}
