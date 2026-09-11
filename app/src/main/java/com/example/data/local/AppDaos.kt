package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.AssessmentReportEntity
import com.example.data.local.entities.AuditLogEntity
import com.example.data.local.entities.CandidateEntity
import com.example.data.local.entities.InterviewQuestionEntity
import com.example.data.local.entities.InterviewSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CandidateDao {
    @Query("SELECT * FROM candidates ORDER BY createdAt DESC")
    fun getAllCandidates(): Flow<List<CandidateEntity>>

    @Query("SELECT * FROM candidates WHERE id = :id LIMIT 1")
    suspend fun getCandidateById(id: String): CandidateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCandidate(candidate: CandidateEntity)

    @Update
    suspend fun updateCandidate(candidate: CandidateEntity)

    @Query("DELETE FROM candidates WHERE id = :id")
    suspend fun deleteCandidate(id: String)
}

@Dao
interface InterviewDao {
    @Query("SELECT * FROM interview_sessions WHERE candidateId = :candidateId ORDER BY startedAt DESC LIMIT 1")
    fun getActiveInterviewForCandidate(candidateId: String): Flow<InterviewSessionEntity?>

    @Query("SELECT * FROM interview_sessions WHERE id = :id LIMIT 1")
    suspend fun getInterviewById(id: String): InterviewSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInterview(interview: InterviewSessionEntity)

    @Update
    suspend fun updateInterview(interview: InterviewSessionEntity)

    @Query("SELECT * FROM interview_questions WHERE interviewId = :interviewId ORDER BY questionNumber ASC")
    fun getQuestionsForInterview(interviewId: String): Flow<List<InterviewQuestionEntity>>

    @Query("SELECT * FROM interview_questions WHERE interviewId = :interviewId ORDER BY questionNumber ASC")
    suspend fun getQuestionsList(interviewId: String): List<InterviewQuestionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<InterviewQuestionEntity>)

    @Update
    suspend fun updateQuestion(question: InterviewQuestionEntity)
}

@Dao
interface AssessmentDao {
    @Query("SELECT * FROM assessment_reports ORDER BY createdAt DESC")
    fun getAllReports(): Flow<List<AssessmentReportEntity>>

    @Query("SELECT * FROM assessment_reports WHERE candidateId = :candidateId ORDER BY createdAt DESC LIMIT 1")
    fun getReportForCandidate(candidateId: String): Flow<AssessmentReportEntity?>

    @Query("SELECT * FROM assessment_reports WHERE id = :id LIMIT 1")
    suspend fun getReportById(id: String): AssessmentReportEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: AssessmentReportEntity)

    @Update
    suspend fun updateReport(report: AssessmentReportEntity)

    @Query("UPDATE assessment_reports SET recruiterStatus = :status WHERE id = :reportId")
    suspend fun updateRecruiterStatus(reportId: String, status: String)
}

@Dao
interface AuditDao {
    @Query("SELECT * FROM audit_logs WHERE candidateId = :candidateId ORDER BY timestamp DESC")
    fun getLogsForCandidate(candidateId: String): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 100")
    fun getAllLogs(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AuditLogEntity)
}
