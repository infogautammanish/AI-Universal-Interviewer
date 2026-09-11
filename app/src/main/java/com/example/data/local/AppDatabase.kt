package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.entities.AssessmentReportEntity
import com.example.data.local.entities.AuditLogEntity
import com.example.data.local.entities.CandidateEntity
import com.example.data.local.entities.InterviewQuestionEntity
import com.example.data.local.entities.InterviewSessionEntity

@Database(
    entities = [
        CandidateEntity::class,
        InterviewSessionEntity::class,
        InterviewQuestionEntity::class,
        AssessmentReportEntity::class,
        AuditLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun candidateDao(): CandidateDao
    abstract fun interviewDao(): InterviewDao
    abstract fun assessmentDao(): AssessmentDao
    abstract fun auditDao(): AuditDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "universal_interviewer_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
