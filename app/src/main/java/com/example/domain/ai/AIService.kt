package com.example.domain.ai

import com.example.data.remote.GeminiApiClient
import com.example.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class AIService(
    private val geminiClient: GeminiApiClient = GeminiApiClient()
) {
    val resumeParser = ResumeParser(geminiClient)
    val domainDetector = DomainDetector(geminiClient)
    val roleDetector = RoleDetector(geminiClient)
    val skillExtractor = SkillExtractor(geminiClient)
    val experienceAnalyzer = ExperienceAnalyzer(geminiClient)
    val projectAnalyzer = ProjectAnalyzer(geminiClient)
    val interviewPlanner = InterviewPlanner(geminiClient)
    val questionGenerator = QuestionGenerator(geminiClient)
    val followUpGenerator = FollowUpGenerator(geminiClient)
    val answerEvaluator = AnswerEvaluator(geminiClient)
    val consistencyAnalyzer = ConsistencyAnalyzer(geminiClient)
    val finalAssessmentGenerator = FinalAssessmentGenerator(geminiClient)
}

class ResumeParser(private val geminiClient: GeminiApiClient) {

    suspend fun parseResume(rawResumeText: String): Pair<CandidateProfile, ProfessionalAnalysis> = withContext(Dispatchers.Default) {
        val geminiPrompt = """
            You are a universal, domain-agnostic resume understanding engine.
            Read and understand this resume regardless of profession, format, or industry.
            Distinguish between explicitly mentioned information and inferences. Do NOT hallucinate.
            
            Return a JSON object with:
            {
              "candidate": {
                "name": "...",
                "email": "...",
                "phone": "...",
                "location": "...",
                "professional_summary": "...",
                "career_objective": "...",
                "education": [
                  {"degree": "...", "institution": "...", "year": "...", "fieldOfStudy": "..."}
                ],
                "experience": [
                  {"jobTitle": "...", "company": "...", "duration": "...", "responsibilities": ["..."]}
                ],
                "skills": [
                  {"name": "...", "category": "Technical|Domain|Soft|Tool", "depth": "Demonstrated", "source": "EXPLICIT"}
                ],
                "projects": [
                  {"title": "...", "description": "...", "roleOrContribution": "...", "toolsUsed": ["..."], "outcomesOrMetrics": "..."}
                ],
                "certifications": ["..."],
                "achievements": ["..."],
                "tools_and_technologies": ["..."],
                "languages": ["..."]
              },
              "professional_analysis": {
                "primary_domain": "...",
                "possible_roles": ["..."],
                "experience_level": "Entry-Level|Intermediate|Senior|Lead/Executive",
                "core_skills": ["..."],
                "tools_and_technologies": ["..."],
                "areas_of_expertise": ["..."],
                "claims_to_verify": [
                  {"claimText": "...", "sourceSection": "...", "confidence": "High"}
                ]
              }
            }
            
            Resume text:
            $rawResumeText
        """.trimIndent()

        val geminiResponse = geminiClient.generateContent(
            prompt = geminiPrompt,
            systemInstruction = "You are a precise, objective, domain-agnostic HR and talent intelligence parser. Always output valid raw JSON."
        )

        if (!geminiResponse.isNullOrBlank()) {
            val parsed = parseJsonResult(geminiResponse)
            if (parsed != null) return@withContext parsed
        }

        // Heuristic Domain-Agnostic Parser Fallback
        fallbackHeuristicParser(rawResumeText)
    }

    private fun parseJsonResult(jsonString: String): Pair<CandidateProfile, ProfessionalAnalysis>? {
        return try {
            val cleanJson = jsonString.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
            val root = JSONObject(cleanJson)
            val candidateObj = root.getJSONObject("candidate")
            val analysisObj = root.getJSONObject("professional_analysis")

            val eduList = mutableListOf<EducationItem>()
            val eduArray = candidateObj.optJSONArray("education")
            if (eduArray != null) {
                for (i in 0 until eduArray.length()) {
                    val item = eduArray.getJSONObject(i)
                    eduList.add(
                        EducationItem(
                            degree = item.optString("degree"),
                            institution = item.optString("institution"),
                            year = item.optString("year"),
                            fieldOfStudy = item.optString("fieldOfStudy")
                        )
                    )
                }
            }

            val expList = mutableListOf<ExperienceItem>()
            val expArray = candidateObj.optJSONArray("experience")
            if (expArray != null) {
                for (i in 0 until expArray.length()) {
                    val item = expArray.getJSONObject(i)
                    val respList = mutableListOf<String>()
                    val respArray = item.optJSONArray("responsibilities")
                    if (respArray != null) {
                        for (j in 0 until respArray.length()) respList.add(respArray.getString(j))
                    }
                    expList.add(
                        ExperienceItem(
                            jobTitle = item.optString("jobTitle"),
                            company = item.optString("company"),
                            duration = item.optString("duration"),
                            responsibilities = respList
                        )
                    )
                }
            }

            val skillList = mutableListOf<SkillItem>()
            val skillsArray = candidateObj.optJSONArray("skills")
            if (skillsArray != null) {
                for (i in 0 until skillsArray.length()) {
                    val item = skillsArray.optJSONObject(i)
                    if (item != null) {
                        skillList.add(
                            SkillItem(
                                name = item.optString("name"),
                                category = item.optString("category", "Domain"),
                                depth = item.optString("depth", "Demonstrated"),
                                source = if (item.optString("source").equals("INFERRED", true)) SkillSource.INFERRED else SkillSource.EXPLICIT
                            )
                        )
                    } else {
                        val skillStr = skillsArray.optString(i)
                        if (skillStr.isNotBlank()) {
                            skillList.add(SkillItem(name = skillStr, source = SkillSource.EXPLICIT))
                        }
                    }
                }
            }

            val projectList = mutableListOf<ProjectItem>()
            val projArray = candidateObj.optJSONArray("projects")
            if (projArray != null) {
                for (i in 0 until projArray.length()) {
                    val item = projArray.getJSONObject(i)
                    val toolsList = mutableListOf<String>()
                    val toolsArray = item.optJSONArray("toolsUsed")
                    if (toolsArray != null) {
                        for (j in 0 until toolsArray.length()) toolsList.add(toolsArray.getString(j))
                    }
                    projectList.add(
                        ProjectItem(
                            title = item.optString("title"),
                            description = item.optString("description"),
                            roleOrContribution = item.optString("roleOrContribution"),
                            toolsUsed = toolsList,
                            outcomesOrMetrics = item.optString("outcomesOrMetrics")
                        )
                    )
                }
            }

            val certs = mutableListOf<String>()
            val certArray = candidateObj.optJSONArray("certifications")
            if (certArray != null) {
                for (i in 0 until certArray.length()) certs.add(certArray.getString(i))
            }

            val profile = CandidateProfile(
                name = candidateObj.optString("name", "Candidate"),
                email = candidateObj.optString("email"),
                phone = candidateObj.optString("phone"),
                location = candidateObj.optString("location"),
                professionalSummary = candidateObj.optString("professional_summary"),
                careerObjective = candidateObj.optString("career_objective"),
                education = eduList,
                experience = expList,
                skills = skillList,
                projects = projectList,
                certifications = certs
            )

            val roles = mutableListOf<String>()
            val rolesArray = analysisObj.optJSONArray("possible_roles")
            if (rolesArray != null) {
                for (i in 0 until rolesArray.length()) roles.add(rolesArray.getString(i))
            }

            val coreSkills = mutableListOf<String>()
            val coreSkillsArray = analysisObj.optJSONArray("core_skills")
            if (coreSkillsArray != null) {
                for (i in 0 until coreSkillsArray.length()) coreSkills.add(coreSkillsArray.getString(i))
            }

            val claims = mutableListOf<ResumeClaim>()
            val claimsArray = analysisObj.optJSONArray("claims_to_verify")
            if (claimsArray != null) {
                for (i in 0 until claimsArray.length()) {
                    val item = claimsArray.getJSONObject(i)
                    claims.add(
                        ResumeClaim(
                            claimText = item.optString("claimText"),
                            sourceSection = item.optString("sourceSection"),
                            confidence = item.optString("confidence", "High")
                        )
                    )
                }
            }

            val analysis = ProfessionalAnalysis(
                primaryDomain = analysisObj.optString("primary_domain", "Professional"),
                possibleRoles = roles,
                experienceLevel = analysisObj.optString("experience_level", "Intermediate"),
                coreSkills = coreSkills,
                toolsAndTechnologies = profile.toolsAndTechnologies,
                areasOfExpertise = coreSkills,
                claimsToVerify = claims
            )

            Pair(profile, analysis)
        } catch (e: Exception) {
            null
        }
    }

    private fun fallbackHeuristicParser(text: String): Pair<CandidateProfile, ProfessionalAnalysis> {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        var name = "Candidate"
        var email = ""
        var phone = ""
        var location = ""
        var summary = ""
        val education = mutableListOf<EducationItem>()
        val experience = mutableListOf<ExperienceItem>()
        val skills = mutableListOf<SkillItem>()
        val projects = mutableListOf<ProjectItem>()
        val certifications = mutableListOf<String>()
        val claims = mutableListOf<ResumeClaim>()

        // 1. Name & contact
        if (lines.isNotEmpty()) {
            val firstLine = lines.first()
            if (!firstLine.contains(":") && firstLine.length < 50) {
                name = firstLine
            }
        }

        val emailRegex = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
        val phoneRegex = Regex("(\\+?\\d{1,3}[-.\\s]?)?\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}")
        email = emailRegex.find(text)?.value ?: ""
        phone = phoneRegex.find(text)?.value ?: ""

        // Section recognition (agnostic of headers)
        var currentSection = ""
        val sectionBuffer = mutableListOf<String>()

        fun flushSection() {
            if (currentSection.isBlank() || sectionBuffer.isEmpty()) return
            when (currentSection.uppercase()) {
                "SUMMARY", "PROFESSIONAL SUMMARY", "PROFILE", "ABOUT", "OBJECTIVE" -> {
                    summary = sectionBuffer.joinToString(" ")
                }
                "EDUCATION", "ACADEMIC BACKGROUND", "STUDIES" -> {
                    for (line in sectionBuffer) {
                        if (line.contains("degree", true) || line.contains("bachelor", true) ||
                            line.contains("master", true) || line.contains("phd", true) ||
                            line.contains("university", true) || line.contains("college", true)
                        ) {
                            val parts = line.split("|", "-").map { it.trim() }
                            education.add(
                                EducationItem(
                                    degree = parts.getOrNull(0) ?: line,
                                    institution = parts.getOrNull(1) ?: "",
                                    year = parts.getOrNull(2) ?: ""
                                )
                            )
                        }
                    }
                }
                "EXPERIENCE", "WORK EXPERIENCE", "EMPLOYMENT", "CAREER HISTORY", "WORK HISTORY" -> {
                    var currentTitle = ""
                    var currentCompany = ""
                    var currentDuration = ""
                    val currentResp = mutableListOf<String>()

                    for (line in sectionBuffer) {
                        if (line.startsWith("-") || line.startsWith("•") || line.startsWith("*")) {
                            val bullet = line.removePrefix("-").removePrefix("•").removePrefix("*").trim()
                            currentResp.add(bullet)
                            if (bullet.contains("%") || bullet.contains("$") || bullet.length > 30) {
                                claims.add(ResumeClaim(bullet, "Experience", "High"))
                            }
                        } else if (line.contains("|") || line.contains("–") || line.contains("- 20") || line.contains("Present")) {
                            if (currentTitle.isNotBlank()) {
                                experience.add(ExperienceItem(currentTitle, currentCompany, currentDuration, currentResp.toList()))
                                currentResp.clear()
                            }
                            val parts = line.split("|", "–", "-").map { it.trim() }
                            currentTitle = parts.getOrNull(0) ?: line
                            currentCompany = parts.getOrNull(1) ?: ""
                            currentDuration = parts.getOrNull(2) ?: ""
                        } else if (currentTitle.isBlank()) {
                            currentTitle = line
                        }
                    }
                    if (currentTitle.isNotBlank()) {
                        experience.add(ExperienceItem(currentTitle, currentCompany, currentDuration, currentResp))
                    }
                }
                "SKILLS", "TECHNICAL SKILLS", "COMPETENCIES", "CORE SKILLS", "SKILLS & TOOLS" -> {
                    for (line in sectionBuffer) {
                        val clean = line.removePrefix("-").removePrefix("•").removePrefix("*")
                        val tokens = clean.split(",", ";", "•", "|").map { it.trim() }.filter { it.isNotBlank() && it.length < 40 }
                        for (token in tokens) {
                            if (!token.contains("skills", true) && !token.contains("tools", true)) {
                                skills.add(SkillItem(name = token, category = "Domain", depth = "Demonstrated", source = SkillSource.EXPLICIT))
                            }
                        }
                    }
                }
                "PROJECTS", "KEY PROJECTS", "FEATURED PROJECTS", "INITIATIVES" -> {
                    var pTitle = ""
                    var pDesc = ""
                    for (line in sectionBuffer) {
                        if (line.contains(":") && pTitle.isBlank()) {
                            val parts = line.split(":", limit = 2)
                            pTitle = parts[0].trim()
                            pDesc = parts[1].trim()
                        } else if (pTitle.isNotBlank()) {
                            pDesc += " " + line.removePrefix("-").removePrefix("•").trim()
                            projects.add(ProjectItem(title = pTitle, description = pDesc.trim()))
                            claims.add(ResumeClaim("Project claim: $pTitle - $pDesc", "Project", "High"))
                            pTitle = ""
                            pDesc = ""
                        } else if (line.length < 50 && !line.startsWith("-")) {
                            pTitle = line
                        }
                    }
                }
                "CERTIFICATIONS", "LICENSES", "CREDENTIALS" -> {
                    for (line in sectionBuffer) {
                        val c = line.removePrefix("-").removePrefix("•").removePrefix("*").trim()
                        if (c.isNotBlank()) {
                            certifications.add(c)
                            claims.add(ResumeClaim("Certified: $c", "Certification", "High"))
                        }
                    }
                }
            }
            sectionBuffer.clear()
        }

        val sectionHeaders = listOf(
            "SUMMARY", "PROFESSIONAL SUMMARY", "CAREER OBJECTIVE", "EDUCATION",
            "EXPERIENCE", "WORK EXPERIENCE", "EMPLOYMENT", "WORK HISTORY",
            "SKILLS", "TECHNICAL SKILLS", "CORE SKILLS", "PROJECTS", "KEY PROJECTS",
            "CERTIFICATIONS", "LICENSES", "ACHIEVEMENTS"
        )

        for (line in lines) {
            val upper = line.uppercase().removeSuffix(":")
            val matchedHeader = sectionHeaders.firstOrNull { upper == it || upper.startsWith("$it ") }
            if (matchedHeader != null) {
                flushSection()
                currentSection = matchedHeader
            } else {
                sectionBuffer.add(line)
            }
        }
        flushSection()

        // Detect Domain Dynamically from vocabulary
        val detectedDomain = detectDomainFromText(text)
        val likelyRoles = detectRolesFromText(text, detectedDomain, experience)
        val coreSkillNames = skills.take(8).map { it.name }.ifEmpty { listOf("Domain Execution", "Industry Standards") }

        val profile = CandidateProfile(
            name = name,
            email = email,
            phone = phone,
            location = location,
            professionalSummary = summary,
            education = education,
            experience = experience,
            skills = skills,
            projects = projects,
            certifications = certifications
        )

        val analysis = ProfessionalAnalysis(
            primaryDomain = detectedDomain,
            possibleRoles = likelyRoles,
            experienceLevel = if (experience.size > 2) "Senior" else if (experience.isNotEmpty()) "Intermediate" else "Entry-Level",
            coreSkills = coreSkillNames,
            toolsAndTechnologies = skills.filter { it.category == "Tool" }.map { it.name },
            areasOfExpertise = coreSkillNames,
            claimsToVerify = claims.take(5)
        )

        return Pair(profile, analysis)
    }

    private fun detectDomainFromText(text: String): String {
        val lower = text.lowercase()
        return when {
            lower.contains("civil") || lower.contains("structural") || lower.contains("staad") || lower.contains("autocad") || lower.contains("concrete") || lower.contains("bridge") -> "Civil Engineering"
            lower.contains("human resource") || lower.contains("talent acquisition") || lower.contains("hrms") || lower.contains("workday") || lower.contains("employee relations") -> "Human Resources"
            lower.contains("seo") || lower.contains("google ads") || lower.contains("marketing") || lower.contains("campaign") || lower.contains("roas") || lower.contains("conversion") -> "Digital Marketing"
            lower.contains("dcf") || lower.contains("lbo") || lower.contains("financial model") || lower.contains("valuation") || lower.contains("bloomberg") || lower.contains("cfa") -> "Finance & Investment"
            lower.contains("nurse") || lower.contains("nursing") || lower.contains("icu") || lower.contains("triage") || lower.contains("ehr") || lower.contains("patient care") || lower.contains("clinical") -> "Healthcare & Nursing"
            lower.contains("supply chain") || lower.contains("logistics") || lower.contains("freight") || lower.contains("warehouse") || lower.contains("procurement") -> "Supply Chain & Logistics"
            lower.contains("microservices") || lower.contains("kubernetes") || lower.contains("golang") || lower.contains("python") || lower.contains("developer") || lower.contains("software") -> "Software Engineering"
            lower.contains("mechanical") || lower.contains("cad") || lower.contains("thermodynamics") || lower.contains("manufacturing") -> "Mechanical Engineering"
            lower.contains("legal") || lower.contains("compliance") || lower.contains("contract law") || lower.contains("litigation") -> "Legal & Compliance"
            else -> "Domain Professional"
        }
    }

    private fun detectRolesFromText(text: String, domain: String, experience: List<ExperienceItem>): List<String> {
        val roles = mutableListOf<String>()
        for (exp in experience) {
            if (exp.jobTitle.isNotBlank() && !roles.contains(exp.jobTitle)) {
                roles.add(exp.jobTitle)
            }
        }
        if (roles.isEmpty()) {
            roles.add("$domain Specialist")
            roles.add("$domain Lead")
        }
        return roles.take(3)
    }
}

class DomainDetector(private val geminiClient: GeminiApiClient) {
    suspend fun detectDomain(resumeText: String): String = withContext(Dispatchers.Default) {
        val prompt = "Identify the single primary professional domain for this resume (e.g. Civil Engineering, Digital Marketing, Human Resources, Finance, Healthcare, Software Engineering, Architecture, Supply Chain, etc.). Respond with just the domain name:\n\n$resumeText"
        val resp = geminiClient.generateContent(prompt)
        if (!resp.isNullOrBlank() && resp.length < 50 && !resp.contains("\n")) {
            return@withContext resp.trim()
        }
        // Heuristic
        val lower = resumeText.lowercase()
        when {
            lower.contains("civil") || lower.contains("staad") || lower.contains("structural") -> "Civil Engineering"
            lower.contains("human resource") || lower.contains("hrms") || lower.contains("talent acquisition") -> "Human Resources"
            lower.contains("seo") || lower.contains("google ads") || lower.contains("marketing") -> "Digital Marketing"
            lower.contains("dcf") || lower.contains("lbo") || lower.contains("valuation") || lower.contains("bloomberg") -> "Finance & Investment"
            lower.contains("icu") || lower.contains("nurse") || lower.contains("clinical") || lower.contains("triage") -> "Healthcare & Nursing"
            lower.contains("supply chain") || lower.contains("freight") || lower.contains("logistics") -> "Supply Chain & Logistics"
            lower.contains("software") || lower.contains("kubernetes") || lower.contains("golang") -> "Software Engineering"
            else -> "Professional Domain"
        }
    }
}

class RoleDetector(private val geminiClient: GeminiApiClient) {
    suspend fun detectRoles(resumeText: String, domain: String): List<String> = withContext(Dispatchers.Default) {
        val prompt = "Based on this resume in $domain, identify 2-3 likely professional roles. Output as a JSON list of strings [\"Role 1\", \"Role 2\"]:\n\n$resumeText"
        val resp = geminiClient.generateContent(prompt)
        if (!resp.isNullOrBlank()) {
            try {
                val clean = resp.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                val array = JSONArray(clean)
                val list = mutableListOf<String>()
                for (i in 0 until array.length()) list.add(array.getString(i))
                if (list.isNotEmpty()) return@withContext list
            } catch (ignored: Exception) {}
        }
        listOf("$domain Specialist", "$domain Consultant", "$domain Project Lead")
    }
}

class SkillExtractor(private val geminiClient: GeminiApiClient)
class ExperienceAnalyzer(private val geminiClient: GeminiApiClient)
class ProjectAnalyzer(private val geminiClient: GeminiApiClient)

class InterviewPlanner(private val geminiClient: GeminiApiClient) {
    fun planInterviewStructure(
        profile: CandidateProfile,
        analysis: ProfessionalAnalysis,
        config: InterviewConfig
    ): List<InterviewCategory> {
        val categories = mutableListOf<InterviewCategory>()
        categories.add(InterviewCategory.INTRODUCTION)

        if (analysis.claimsToVerify.isNotEmpty()) {
            categories.add(InterviewCategory.RESUME_VERIFICATION)
        }
        if (profile.projects.isNotEmpty()) {
            categories.add(InterviewCategory.PROJECTS)
        }
        categories.add(InterviewCategory.TECHNICAL_SKILLS)
        categories.add(InterviewCategory.DOMAIN_KNOWLEDGE)
        categories.add(InterviewCategory.PROBLEM_SOLVING)
        categories.add(InterviewCategory.SCENARIO_BASED)
        categories.add(InterviewCategory.BEHAVIORAL)

        return categories.take(config.questionCount)
    }
}

class QuestionGenerator(private val geminiClient: GeminiApiClient) {

    suspend fun generateQuestions(
        profile: CandidateProfile,
        analysis: ProfessionalAnalysis,
        config: InterviewConfig
    ): List<InterviewQuestion> = withContext(Dispatchers.Default) {
        val geminiPrompt = """
            You are an expert, domain-agnostic executive interviewer.
            Generate ${config.questionCount} dynamic, high-fidelity interview questions tailored strictly to this candidate's resume claims, projects, skills, and domain.
            
            Domain: ${analysis.primaryDomain}
            Experience Level: ${analysis.experienceLevel}
            Interview Mode: ${config.interviewType}
            ${if (config.jobDescription.isNotBlank()) "Job Description: " + config.jobDescription else ""}
            
            Candidate Name: ${profile.name}
            Summary: ${profile.professionalSummary}
            Projects: ${profile.projects.map { "${it.title}: ${it.description} (Tools: ${it.toolsUsed.joinToString()})" }}
            Experience: ${profile.experience.map { "${it.jobTitle} at ${it.company}: ${it.responsibilities.joinToString("; ")}" }}
            Skills: ${profile.skills.map { it.name }}
            Claims to Verify: ${analysis.claimsToVerify.map { it.claimText }}
            
            CRITICAL RULES:
            1. Every question must be anchored in what the candidate ACTUALLY claimed in their resume.
            2. Do NOT ask generic boilerplate questions. If they built a bridge, ask about the bridge and calculations. If they managed HR, ask about their HRMS migration and labor compliance.
            3. Balance categories: Resume Verification, Projects, Domain Knowledge, Problem Solving, Scenario Based.
            
            Return JSON array:
            [
              {
                "questionNumber": 1,
                "category": "RESUME_VERIFICATION",
                "difficulty": "Intermediate",
                "questionText": "...",
                "relatedClaim": "...",
                "targetSkillOrTopic": "..."
              }
            ]
        """.trimIndent()

        val geminiResp = geminiClient.generateContent(
            prompt = geminiPrompt,
            systemInstruction = "You are a professional technical and behavioral AI interviewer. Return valid JSON array only."
        )

        if (!geminiResp.isNullOrBlank()) {
            val questions = parseQuestionsJson(geminiResp, config.questionCount)
            if (questions.isNotEmpty()) return@withContext questions
        }

        // Domain-Agnostic Heuristic Question Generator
        fallbackGenerateQuestions(profile, analysis, config)
    }

    private fun parseQuestionsJson(jsonString: String, total: Int): List<InterviewQuestion> {
        return try {
            val clean = jsonString.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val array = JSONArray(clean)
            val list = mutableListOf<InterviewQuestion>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val catStr = obj.optString("category", "DOMAIN_KNOWLEDGE")
                val category = try {
                    InterviewCategory.valueOf(catStr)
                } catch (e: Exception) {
                    InterviewCategory.DOMAIN_KNOWLEDGE
                }
                list.add(
                    InterviewQuestion(
                        id = UUID.randomUUID().toString(),
                        questionNumber = i + 1,
                        totalQuestions = total,
                        category = category,
                        difficulty = obj.optString("difficulty", "Intermediate"),
                        questionText = obj.getString("questionText"),
                        relatedClaim = obj.optString("relatedClaim").takeIf { it.isNotBlank() },
                        targetSkillOrTopic = obj.optString("targetSkillOrTopic")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun fallbackGenerateQuestions(
        profile: CandidateProfile,
        analysis: ProfessionalAnalysis,
        config: InterviewConfig
    ): List<InterviewQuestion> {
        val total = config.questionCount
        val questions = mutableListOf<InterviewQuestion>()
        var qNum = 1

        // 1. Introduction tailored to domain
        questions.add(
            InterviewQuestion(
                id = UUID.randomUUID().toString(),
                questionNumber = qNum++,
                totalQuestions = total,
                category = InterviewCategory.INTRODUCTION,
                difficulty = "Introductory",
                questionText = "Welcome, ${profile.name}. Based on your resume in ${analysis.primaryDomain}, could you give an overview of your career journey and what you consider your most defining professional achievement?",
                targetSkillOrTopic = "Career Overview"
            )
        )

        // 2. Project Question 1
        if (profile.projects.isNotEmpty() && qNum <= total) {
            val proj = profile.projects.first()
            val toolsStr = if (proj.toolsUsed.isNotEmpty()) " utilizing ${proj.toolsUsed.joinToString(", ")}" else ""
            questions.add(
                InterviewQuestion(
                    id = UUID.randomUUID().toString(),
                    questionNumber = qNum++,
                    totalQuestions = total,
                    category = InterviewCategory.PROJECTS,
                    difficulty = "Intermediate",
                    questionText = "In your resume, you highlighted the project '${proj.title}'$toolsStr. Could you detail the structural architecture, your specific contribution, and how you evaluated its success?",
                    relatedClaim = proj.title,
                    targetSkillOrTopic = proj.title
                )
            )
        }

        // 3. Claim Verification Question
        if (analysis.claimsToVerify.isNotEmpty() && qNum <= total) {
            val claim = analysis.claimsToVerify.first()
            questions.add(
                InterviewQuestion(
                    id = UUID.randomUUID().toString(),
                    questionNumber = qNum++,
                    totalQuestions = total,
                    category = InterviewCategory.RESUME_VERIFICATION,
                    difficulty = "Advanced",
                    questionText = "Your resume claims: \"${claim.claimText}\". Walk me step-by-step through how you implemented this. What technical or operational challenges arose, and how did you resolve them?",
                    relatedClaim = claim.claimText,
                    targetSkillOrTopic = "Claim Verification"
                )
            )
        }

        // 4. Experience Deep Dive
        if (profile.experience.isNotEmpty() && qNum <= total) {
            val exp = profile.experience.first()
            val resp = exp.responsibilities.firstOrNull() ?: "your core responsibilities"
            questions.add(
                InterviewQuestion(
                    id = UUID.randomUUID().toString(),
                    questionNumber = qNum++,
                    totalQuestions = total,
                    category = InterviewCategory.CAREER_EXPERIENCE,
                    difficulty = "Intermediate",
                    questionText = "During your tenure as ${exp.jobTitle} at ${exp.company}, you focused on: \"$resp\". How did you prioritize deliverables and measure the practical business or technical impact of this work?",
                    relatedClaim = resp,
                    targetSkillOrTopic = exp.jobTitle
                )
            )
        }

        // 5. Technical / Domain Tool question
        val coreSkill = analysis.coreSkills.firstOrNull() ?: "${analysis.primaryDomain} Methodologies"
        if (qNum <= total) {
            questions.add(
                InterviewQuestion(
                    id = UUID.randomUUID().toString(),
                    questionNumber = qNum++,
                    totalQuestions = total,
                    category = InterviewCategory.TECHNICAL_SKILLS,
                    difficulty = "Advanced",
                    questionText = "How do you apply '$coreSkill' when tackling complex constraints in ${analysis.primaryDomain}? What edge cases or failure modes do you routinely guard against?",
                    targetSkillOrTopic = coreSkill
                )
            )
        }

        // 6. Problem Solving & Scenario
        if (qNum <= total) {
            questions.add(
                InterviewQuestion(
                    id = UUID.randomUUID().toString(),
                    questionNumber = qNum++,
                    totalQuestions = total,
                    category = InterviewCategory.PROBLEM_SOLVING,
                    difficulty = "Advanced",
                    questionText = "Describe a high-stakes scenario in your ${analysis.primaryDomain} work where standard assumptions proved incorrect or a sudden roadblock occurred. How did you diagnose the root cause and adapt?",
                    targetSkillOrTopic = "Root Cause Analysis & Resilience"
                )
            )
        }

        // 7. Scenario / Job alignment
        if (qNum <= total) {
            val scenarioPrompt = if (config.jobDescription.isNotBlank()) {
                "In the context of the target job requirements: \"${config.jobDescription.take(120)}...\", how would you approach the first 90 days to drive measurable progress?"
            } else {
                "Imagine you are leading a new initiative in ${analysis.primaryDomain} with tight deadlines and cross-functional stakeholders who disagree on the technical approach. How do you build consensus and deliver?"
            }
            questions.add(
                InterviewQuestion(
                    id = UUID.randomUUID().toString(),
                    questionNumber = qNum++,
                    totalQuestions = total,
                    category = InterviewCategory.SCENARIO_BASED,
                    difficulty = "Advanced",
                    questionText = scenarioPrompt,
                    targetSkillOrTopic = "Strategic Leadership & Consensus"
                )
            )
        }

        // 8. Behavioral
        if (qNum <= total) {
            questions.add(
                InterviewQuestion(
                    id = UUID.randomUUID().toString(),
                    questionNumber = qNum++,
                    totalQuestions = total,
                    category = InterviewCategory.BEHAVIORAL,
                    difficulty = "Intermediate",
                    questionText = "Can you share an instance where you mentored a junior colleague or advocated for higher quality standards in your ${analysis.primaryDomain} team despite pushback?",
                    targetSkillOrTopic = "Culture & Quality Advocacy"
                )
            )
        }

        // Pad if needed
        while (qNum <= total) {
            val extraSkill = analysis.coreSkills.getOrNull(qNum % analysis.coreSkills.size.coerceAtLeast(1)) ?: "Industry Best Practices"
            questions.add(
                InterviewQuestion(
                    id = UUID.randomUUID().toString(),
                    questionNumber = qNum++,
                    totalQuestions = total,
                    category = InterviewCategory.DOMAIN_KNOWLEDGE,
                    difficulty = "Intermediate",
                    questionText = "What emerging trends or regulatory/technological shifts in ${analysis.primaryDomain} are currently influencing your approach to $extraSkill?",
                    targetSkillOrTopic = extraSkill
                )
            )
        }

        return questions.take(total)
    }
}

class FollowUpGenerator(private val geminiClient: GeminiApiClient) {

    suspend fun generateFollowUp(
        question: InterviewQuestion,
        candidateAnswer: String,
        domain: String
    ): String = withContext(Dispatchers.Default) {
        val prompt = """
            You are an adaptive AI interviewer conducting a deep resume verification interview in $domain.
            The original question was: "${question.questionText}"
            Candidate answered: "$candidateAnswer"
            
            Rules:
            1. Generate ONE focused, probing follow-up question.
            2. If the candidate answered with high confidence, probe deeper into practical details, metrics, trade-offs, or error handling.
            3. If the candidate was vague or brief, ask for a concrete example, numbers, or specific methodologies.
            4. Keep the question crisp, respectful, and strictly relevant.
            
            Return ONLY the follow-up question text:
        """.trimIndent()

        val resp = geminiClient.generateContent(prompt)
        if (!resp.isNullOrBlank() && resp.length > 15) {
            return@withContext resp.trim().removePrefix("\"").removeSuffix("\"")
        }

        // Heuristic adaptive follow-up
        heuristicFollowUp(question, candidateAnswer, domain)
    }

    private fun heuristicFollowUp(question: InterviewQuestion, answer: String, domain: String): String {
        val lower = answer.lowercase()
        return when {
            lower.contains("team") || lower.contains("colleague") ->
                "Can you pinpoint exactly which part of that solution was your individual contribution versus what the broader team executed?"
            lower.contains("tool") || lower.contains("used") || lower.contains("software") || lower.contains("system") ->
                "What specific limitations or bottlenecks did you encounter with that tooling, and how did you circumvent them?"
            lower.contains("led") || lower.contains("managed") || lower.contains("architected") ->
                "How did you validate that your design or management strategy was successful, and what metric best reflects that outcome?"
            answer.length < 70 ->
                "Could you elaborate on that with a concrete real-world example from your day-to-day experience?"
            else ->
                "Looking back at that experience, if you had to do it again from scratch, what key trade-off would you handle differently?"
        }
    }
}

class AnswerEvaluator(private val geminiClient: GeminiApiClient) {

    suspend fun evaluateAnswer(
        question: InterviewQuestion,
        candidateAnswer: String,
        followUpAnswer: String?,
        domain: String,
        resumeClaims: List<ResumeClaim>
    ): AnswerEvaluation = withContext(Dispatchers.Default) {
        val fullAnswer = candidateAnswer + (if (!followUpAnswer.isNullOrBlank()) " [Follow-up: $followUpAnswer]" else "")

        val prompt = """
            Evaluate the candidate's interview response in $domain.
            Question: ${question.questionText}
            Candidate Response: $fullAnswer
            Related Resume Claim: ${question.relatedClaim ?: "None"}
            
            Analyze:
            1. Domain Relevance & Depth (0-100)
            2. Practical knowledge vs theoretical buzzwords (0-100)
            3. Problem Solving & Communication (0-100)
            4. Resume Consistency: Does the answer contradict the resume claims? (e.g. claims 5 years experience but says started recently)
            
            Return JSON:
            {
              "score": 85,
              "domainRelevanceScore": 88,
              "depthOfKnowledgeScore": 82,
              "communicationScore": 84,
              "problemSolvingScore": 80,
              "feedback": "...",
              "isConsistentWithResume": true,
              "inconsistencyConcern": null
            }
        """.trimIndent()

        val resp = geminiClient.generateContent(prompt)
        if (!resp.isNullOrBlank()) {
            try {
                val clean = resp.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                val obj = JSONObject(clean)
                return@withContext AnswerEvaluation(
                    score = obj.optInt("score", 80),
                    domainRelevanceScore = obj.optInt("domainRelevanceScore", 80),
                    depthOfKnowledgeScore = obj.optInt("depthOfKnowledgeScore", 80),
                    communicationScore = obj.optInt("communicationScore", 80),
                    problemSolvingScore = obj.optInt("problemSolvingScore", 80),
                    feedback = obj.optString("feedback", "Good practical demonstration."),
                    isConsistentWithResume = obj.optBoolean("isConsistentWithResume", true),
                    inconsistencyConcern = obj.optString("inconsistencyConcern").takeIf { it.isNotBlank() && it != "null" }
                )
            } catch (ignored: Exception) {}
        }

        // Heuristic Evaluation
        heuristicEvaluation(question, fullAnswer, resumeClaims)
    }

    private fun heuristicEvaluation(question: InterviewQuestion, answer: String, claims: List<ResumeClaim>): AnswerEvaluation {
        val wordCount = answer.split("\\s+".toRegex()).size
        val score = when {
            wordCount > 60 -> 88
            wordCount > 30 -> 82
            wordCount > 15 -> 74
            else -> 60
        }

        var isConsistent = true
        var concern: String? = null

        // Check for inconsistency flags
        val lowerAnswer = answer.lowercase()
        if (question.relatedClaim != null) {
            val lowerClaim = question.relatedClaim.lowercase()
            if (lowerClaim.contains("year") && (lowerAnswer.contains("just started") || lowerAnswer.contains("recently learned") || lowerAnswer.contains("last month"))) {
                isConsistent = false
                concern = "Resume indicates extended experience, but response suggests recent onboarding."
            } else if ((lowerClaim.contains("led") || lowerClaim.contains("spearheaded") || lowerClaim.contains("architected")) &&
                (lowerAnswer.contains("did not actually") || lowerAnswer.contains("only watched") || lowerAnswer.contains("someone else did"))) {
                isConsistent = false
                concern = "Resume claims leadership/architecture role, whereas interview response states secondary involvement."
            }
        }

        return AnswerEvaluation(
            score = score,
            domainRelevanceScore = score + 2,
            depthOfKnowledgeScore = score,
            communicationScore = if (wordCount > 25) 85 else 70,
            problemSolvingScore = score - 2,
            feedback = if (isConsistent) "Demonstrates relevant practical knowledge and coherent domain articulation." else "Requires recruiter review due to divergence from stated resume claim.",
            isConsistentWithResume = isConsistent,
            inconsistencyConcern = concern
        )
    }
}

class ConsistencyAnalyzer(private val geminiClient: GeminiApiClient) {
    fun extractInconsistencies(questions: List<InterviewQuestion>): List<InconsistencyFlag> {
        val flags = mutableListOf<InconsistencyFlag>()
        for (q in questions) {
            val eval = q.evaluation
            if (eval != null && !eval.isConsistentWithResume && eval.inconsistencyConcern != null) {
                flags.add(
                    InconsistencyFlag(
                        claim = q.relatedClaim ?: q.questionText,
                        candidateResponse = q.candidateAnswer,
                        concern = eval.inconsistencyConcern,
                        severity = "Medium"
                    )
                )
            }
        }
        return flags
    }
}

class FinalAssessmentGenerator(private val geminiClient: GeminiApiClient) {

    suspend fun generateAssessment(
        candidateId: String,
        candidateName: String,
        interviewId: String,
        domain: String,
        analysis: ProfessionalAnalysis,
        questions: List<InterviewQuestion>
    ): FinalAssessmentReport = withContext(Dispatchers.Default) {
        val answeredQuestions = questions.filter { it.isAnswered }
        val evaluations = answeredQuestions.mapNotNull { it.evaluation }

        val avgScore = if (evaluations.isNotEmpty()) evaluations.map { it.score }.average().toInt() else 75
        val avgDomain = if (evaluations.isNotEmpty()) evaluations.map { it.domainRelevanceScore }.average().toInt() else 75
        val avgDepth = if (evaluations.isNotEmpty()) evaluations.map { it.depthOfKnowledgeScore }.average().toInt() else 75
        val avgComm = if (evaluations.isNotEmpty()) evaluations.map { it.communicationScore }.average().toInt() else 78
        val avgProb = if (evaluations.isNotEmpty()) evaluations.map { it.problemSolvingScore }.average().toInt() else 76

        val inconsistencies = mutableListOf<InconsistencyFlag>()
        for (q in answeredQuestions) {
            val ev = q.evaluation
            if (ev != null && !ev.isConsistentWithResume && !ev.inconsistencyConcern.isNullOrBlank()) {
                inconsistencies.add(
                    InconsistencyFlag(
                        claim = q.relatedClaim ?: q.questionText,
                        candidateResponse = q.candidateAnswer,
                        concern = ev.inconsistencyConcern,
                        severity = "Medium"
                    )
                )
            }
        }

        val consistencyScore = (100 - (inconsistencies.size * 15)).coerceIn(40, 100)
        val overallScore = ((avgScore * 0.4) + (avgDepth * 0.25) + (avgProb * 0.15) + (avgComm * 0.1) + (consistencyScore * 0.1)).toInt()

        val recommendation = when {
            overallScore >= 82 && inconsistencies.isEmpty() -> RecommendationTier.STRONG_CANDIDATE
            overallScore >= 70 && inconsistencies.size <= 1 -> RecommendationTier.POTENTIAL_CANDIDATE
            overallScore >= 55 || inconsistencies.isNotEmpty() -> RecommendationTier.NEEDS_FURTHER_EVALUATION
            else -> RecommendationTier.INSUFFICIENT_EVIDENCE
        }

        val verifiedSkills = mutableListOf<String>()
        val unverifiedSkills = mutableListOf<String>()
        for (q in answeredQuestions) {
            if (q.targetSkillOrTopic.isNotBlank()) {
                if ((q.evaluation?.score ?: 0) >= 75) {
                    verifiedSkills.add(q.targetSkillOrTopic)
                } else {
                    unverifiedSkills.add(q.targetSkillOrTopic)
                }
            }
        }
        for (skill in analysis.coreSkills) {
            if (!verifiedSkills.contains(skill) && !unverifiedSkills.contains(skill)) {
                unverifiedSkills.add(skill)
            }
        }

        val strengths = mutableListOf<String>()
        if (avgDomain >= 80) strengths.add("Strong technical command of $domain principles and methodologies.")
        if (avgComm >= 80) strengths.add("Articulate communication with structured real-world problem breakdowns.")
        if (avgDepth >= 80) strengths.add("Practical hands-on execution depth evident in project discussions.")
        if (strengths.isEmpty()) strengths.add("Familiarity with core $domain workflows and terminology.")

        val weaknesses = mutableListOf<String>()
        if (inconsistencies.isNotEmpty()) weaknesses.add("Discrepancies identified between resume statements and verbal answers.")
        if (avgDepth < 75) weaknesses.add("Responses occasionally leaned on conceptual descriptions rather than detailed operational metrics.")
        if (weaknesses.isEmpty()) weaknesses.add("Could benefit from deeper elaboration on long-term risk mitigation strategies.")

        val summary = "$candidateName completed a comprehensive $domain evaluation. The candidate demonstrated an overall competency score of $overallScore/100, displaying solid grasp in ${verifiedSkills.take(3).joinToString(", ")}. Recommended as '${recommendation.label}' for recruiter consideration."

        FinalAssessmentReport(
            id = UUID.randomUUID().toString(),
            candidateId = candidateId,
            candidateName = candidateName,
            interviewId = interviewId,
            overallScore = overallScore,
            domain = domain,
            likelyRoles = analysis.possibleRoles,
            experienceLevel = analysis.experienceLevel,
            strengths = strengths,
            weaknesses = weaknesses,
            verifiedSkills = verifiedSkills.distinct(),
            skillsNeedingVerification = unverifiedSkills.distinct(),
            projectUnderstanding = avgDepth,
            technicalOrDomainKnowledge = avgDomain,
            problemSolving = avgProb,
            communication = avgComm,
            resumeConsistency = consistencyScore,
            potentialInconsistencies = inconsistencies,
            recommendation = recommendation,
            summary = summary
        )
    }
}
