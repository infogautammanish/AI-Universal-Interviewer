package com.example.data.repository

import com.example.domain.model.*
import org.json.JSONArray
import org.json.JSONObject

object ModelJsonSerializer {

    fun profileToJson(profile: CandidateProfile): String {
        val root = JSONObject()
        root.put("name", profile.name)
        root.put("email", profile.email)
        root.put("phone", profile.phone)
        root.put("location", profile.location)
        root.put("professionalSummary", profile.professionalSummary)
        root.put("careerObjective", profile.careerObjective)

        val eduArray = JSONArray()
        for (e in profile.education) {
            val obj = JSONObject()
            obj.put("degree", e.degree)
            obj.put("institution", e.institution)
            obj.put("year", e.year)
            obj.put("fieldOfStudy", e.fieldOfStudy)
            eduArray.put(obj)
        }
        root.put("education", eduArray)

        val expArray = JSONArray()
        for (exp in profile.experience) {
            val obj = JSONObject()
            obj.put("jobTitle", exp.jobTitle)
            obj.put("company", exp.company)
            obj.put("duration", exp.duration)
            val respArr = JSONArray()
            for (r in exp.responsibilities) respArr.put(r)
            obj.put("responsibilities", respArr)
            expArray.put(obj)
        }
        root.put("experience", expArray)

        val skillsArray = JSONArray()
        for (s in profile.skills) {
            val obj = JSONObject()
            obj.put("name", s.name)
            obj.put("category", s.category)
            obj.put("depth", s.depth)
            obj.put("source", s.source.name)
            skillsArray.put(obj)
        }
        root.put("skills", skillsArray)

        val projArray = JSONArray()
        for (p in profile.projects) {
            val obj = JSONObject()
            obj.put("title", p.title)
            obj.put("description", p.description)
            obj.put("roleOrContribution", p.roleOrContribution)
            val toolsArr = JSONArray()
            for (t in p.toolsUsed) toolsArr.put(t)
            obj.put("toolsUsed", toolsArr)
            obj.put("outcomesOrMetrics", p.outcomesOrMetrics)
            projArray.put(obj)
        }
        root.put("projects", projArray)

        val certsArr = JSONArray()
        for (c in profile.certifications) certsArr.put(c)
        root.put("certifications", certsArr)

        return root.toString()
    }

    fun jsonToProfile(jsonStr: String): CandidateProfile {
        if (jsonStr.isBlank()) return CandidateProfile()
        return try {
            val root = JSONObject(jsonStr)
            val eduList = mutableListOf<EducationItem>()
            val eduArray = root.optJSONArray("education")
            if (eduArray != null) {
                for (i in 0 until eduArray.length()) {
                    val obj = eduArray.getJSONObject(i)
                    eduList.add(
                        EducationItem(
                            degree = obj.optString("degree"),
                            institution = obj.optString("institution"),
                            year = obj.optString("year"),
                            fieldOfStudy = obj.optString("fieldOfStudy")
                        )
                    )
                }
            }

            val expList = mutableListOf<ExperienceItem>()
            val expArray = root.optJSONArray("experience")
            if (expArray != null) {
                for (i in 0 until expArray.length()) {
                    val obj = expArray.getJSONObject(i)
                    val respList = mutableListOf<String>()
                    val respArr = obj.optJSONArray("responsibilities")
                    if (respArr != null) {
                        for (j in 0 until respArr.length()) respList.add(respArr.getString(j))
                    }
                    expList.add(
                        ExperienceItem(
                            jobTitle = obj.optString("jobTitle"),
                            company = obj.optString("company"),
                            duration = obj.optString("duration"),
                            responsibilities = respList
                        )
                    )
                }
            }

            val skillsList = mutableListOf<SkillItem>()
            val skillsArray = root.optJSONArray("skills")
            if (skillsArray != null) {
                for (i in 0 until skillsArray.length()) {
                    val obj = skillsArray.getJSONObject(i)
                    skillsList.add(
                        SkillItem(
                            name = obj.optString("name"),
                            category = obj.optString("category", "Domain"),
                            depth = obj.optString("depth", "Demonstrated"),
                            source = try {
                                SkillSource.valueOf(obj.optString("source", "EXPLICIT"))
                            } catch (e: Exception) {
                                SkillSource.EXPLICIT
                            }
                        )
                    )
                }
            }

            val projList = mutableListOf<ProjectItem>()
            val projArray = root.optJSONArray("projects")
            if (projArray != null) {
                for (i in 0 until projArray.length()) {
                    val obj = projArray.getJSONObject(i)
                    val toolsList = mutableListOf<String>()
                    val toolsArr = obj.optJSONArray("toolsUsed")
                    if (toolsArr != null) {
                        for (j in 0 until toolsArr.length()) toolsList.add(toolsArr.getString(j))
                    }
                    projList.add(
                        ProjectItem(
                            title = obj.optString("title"),
                            description = obj.optString("description"),
                            roleOrContribution = obj.optString("roleOrContribution"),
                            toolsUsed = toolsList,
                            outcomesOrMetrics = obj.optString("outcomesOrMetrics")
                        )
                    )
                }
            }

            val certsList = mutableListOf<String>()
            val certsArr = root.optJSONArray("certifications")
            if (certsArr != null) {
                for (i in 0 until certsArr.length()) certsList.add(certsArr.getString(i))
            }

            CandidateProfile(
                name = root.optString("name"),
                email = root.optString("email"),
                phone = root.optString("phone"),
                location = root.optString("location"),
                professionalSummary = root.optString("professionalSummary"),
                careerObjective = root.optString("careerObjective"),
                education = eduList,
                experience = expList,
                skills = skillsList,
                projects = projList,
                certifications = certsList
            )
        } catch (e: Exception) {
            CandidateProfile()
        }
    }

    fun reportToJson(report: FinalAssessmentReport): String {
        val root = JSONObject()
        root.put("id", report.id)
        root.put("candidateId", report.candidateId)
        root.put("candidateName", report.candidateName)
        root.put("interviewId", report.interviewId)
        root.put("overallScore", report.overallScore)
        root.put("domain", report.domain)
        root.put("experienceLevel", report.experienceLevel)
        root.put("summary", report.summary)
        root.put("recommendation", report.recommendation.name)
        root.put("projectUnderstanding", report.projectUnderstanding)
        root.put("technicalOrDomainKnowledge", report.technicalOrDomainKnowledge)
        root.put("problemSolving", report.problemSolving)
        root.put("communication", report.communication)
        root.put("resumeConsistency", report.resumeConsistency)
        root.put("recruiterNotes", report.recruiterNotes)
        root.put("recruiterStatus", report.recruiterStatus)

        val rolesArr = JSONArray()
        for (r in report.likelyRoles) rolesArr.put(r)
        root.put("likelyRoles", rolesArr)

        val strengthsArr = JSONArray()
        for (s in report.strengths) strengthsArr.put(s)
        root.put("strengths", strengthsArr)

        val weaknessesArr = JSONArray()
        for (w in report.weaknesses) weaknessesArr.put(w)
        root.put("weaknesses", weaknessesArr)

        val verifiedArr = JSONArray()
        for (v in report.verifiedSkills) verifiedArr.put(v)
        root.put("verifiedSkills", verifiedArr)

        val unverifiedArr = JSONArray()
        for (u in report.skillsNeedingVerification) unverifiedArr.put(u)
        root.put("skillsNeedingVerification", unverifiedArr)

        val flagsArr = JSONArray()
        for (f in report.potentialInconsistencies) {
            val fObj = JSONObject()
            fObj.put("claim", f.claim)
            fObj.put("candidateResponse", f.candidateResponse)
            fObj.put("concern", f.concern)
            fObj.put("severity", f.severity)
            flagsArr.put(fObj)
        }
        root.put("potentialInconsistencies", flagsArr)

        return root.toString()
    }

    fun jsonToReport(jsonStr: String): FinalAssessmentReport? {
        if (jsonStr.isBlank()) return null
        return try {
            val root = JSONObject(jsonStr)

            val roles = mutableListOf<String>()
            val rolesArr = root.optJSONArray("likelyRoles")
            if (rolesArr != null) {
                for (i in 0 until rolesArr.length()) roles.add(rolesArr.getString(i))
            }

            val strengths = mutableListOf<String>()
            val strengthsArr = root.optJSONArray("strengths")
            if (strengthsArr != null) {
                for (i in 0 until strengthsArr.length()) strengths.add(strengthsArr.getString(i))
            }

            val weaknesses = mutableListOf<String>()
            val weaknessesArr = root.optJSONArray("weaknesses")
            if (weaknessesArr != null) {
                for (i in 0 until weaknessesArr.length()) weaknesses.add(weaknessesArr.getString(i))
            }

            val verified = mutableListOf<String>()
            val verifiedArr = root.optJSONArray("verifiedSkills")
            if (verifiedArr != null) {
                for (i in 0 until verifiedArr.length()) verified.add(verifiedArr.getString(i))
            }

            val unverified = mutableListOf<String>()
            val unverifiedArr = root.optJSONArray("skillsNeedingVerification")
            if (unverifiedArr != null) {
                for (i in 0 until unverifiedArr.length()) unverified.add(unverifiedArr.getString(i))
            }

            val flags = mutableListOf<InconsistencyFlag>()
            val flagsArr = root.optJSONArray("potentialInconsistencies")
            if (flagsArr != null) {
                for (i in 0 until flagsArr.length()) {
                    val obj = flagsArr.getJSONObject(i)
                    flags.add(
                        InconsistencyFlag(
                            claim = obj.optString("claim"),
                            candidateResponse = obj.optString("candidateResponse"),
                            concern = obj.optString("concern"),
                            severity = obj.optString("severity", "Medium")
                        )
                    )
                }
            }

            val rec = try {
                RecommendationTier.valueOf(root.optString("recommendation", "POTENTIAL_CANDIDATE"))
            } catch (e: Exception) {
                RecommendationTier.POTENTIAL_CANDIDATE
            }

            FinalAssessmentReport(
                id = root.optString("id"),
                candidateId = root.optString("candidateId"),
                candidateName = root.optString("candidateName"),
                interviewId = root.optString("interviewId"),
                overallScore = root.optInt("overallScore"),
                domain = root.optString("domain"),
                likelyRoles = roles,
                experienceLevel = root.optString("experienceLevel"),
                strengths = strengths,
                weaknesses = weaknesses,
                verifiedSkills = verified,
                skillsNeedingVerification = unverified,
                projectUnderstanding = root.optInt("projectUnderstanding"),
                technicalOrDomainKnowledge = root.optInt("technicalOrDomainKnowledge"),
                problemSolving = root.optInt("problemSolving"),
                communication = root.optInt("communication"),
                resumeConsistency = root.optInt("resumeConsistency"),
                potentialInconsistencies = flags,
                recommendation = rec,
                summary = root.optString("summary"),
                recruiterNotes = root.optString("recruiterNotes"),
                recruiterStatus = root.optString("recruiterStatus", "Under Recruiter Review")
            )
        } catch (e: Exception) {
            null
        }
    }
}
