package com.example.domain.model

data class CandidateProfile(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val location: String = "",
    val professionalSummary: String = "",
    val careerObjective: String = "",
    val education: List<EducationItem> = emptyList(),
    val experience: List<ExperienceItem> = emptyList(),
    val skills: List<SkillItem> = emptyList(),
    val projects: List<ProjectItem> = emptyList(),
    val certifications: List<String> = emptyList(),
    val achievements: List<String> = emptyList(),
    val toolsAndTechnologies: List<String> = emptyList(),
    val languages: List<String> = emptyList()
)

data class EducationItem(
    val degree: String = "",
    val institution: String = "",
    val year: String = "",
    val fieldOfStudy: String = ""
)

data class ExperienceItem(
    val jobTitle: String = "",
    val company: String = "",
    val duration: String = "",
    val responsibilities: List<String> = emptyList()
)

data class SkillItem(
    val name: String,
    val category: String = "Technical", // Technical, Domain, Soft, Tool
    val depth: String = "Demonstrated", // Beginner, Intermediate, Advanced, Demonstrated
    val source: SkillSource = SkillSource.EXPLICIT // EXPLICIT, INFERRED, NOT_FOUND
)

enum class SkillSource {
    EXPLICIT,
    INFERRED,
    NOT_FOUND
}

data class ProjectItem(
    val title: String = "",
    val description: String = "",
    val roleOrContribution: String = "",
    val toolsUsed: List<String> = emptyList(),
    val outcomesOrMetrics: String = ""
)

data class ProfessionalAnalysis(
    val primaryDomain: String = "General Professional",
    val possibleRoles: List<String> = emptyList(),
    val experienceLevel: String = "Intermediate", // Entry-Level, Intermediate, Senior, Lead/Executive
    val coreSkills: List<String> = emptyList(),
    val toolsAndTechnologies: List<String> = emptyList(),
    val areasOfExpertise: List<String> = emptyList(),
    val claimsToVerify: List<ResumeClaim> = emptyList()
)

data class ResumeClaim(
    val claimText: String,
    val sourceSection: String, // Project, Experience, Certification, Skill
    val confidence: String = "High",
    val status: ClaimStatus = ClaimStatus.UNVERIFIED // UNVERIFIED, VERIFIED, INCONSISTENT
)

enum class ClaimStatus {
    UNVERIFIED,
    VERIFIED,
    INCONSISTENT
}
