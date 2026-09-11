package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.InterviewConfig
import com.example.domain.model.InterviewType
import com.example.ui.components.DomainBadge
import com.example.ui.theme.*

@Composable
fun InterviewConfigScreen(
    domain: String,
    candidateName: String,
    config: InterviewConfig,
    onConfigChange: (InterviewType, Int, String, Boolean, String) -> Unit,
    onStartInterview: () -> Unit
) {
    val scrollState = rememberScrollState()

    var selectedType by remember { mutableStateOf(config.interviewType) }
    var questionCount by remember { mutableStateOf(config.questionCount) }
    var difficulty by remember { mutableStateOf(config.difficulty) }
    var allowFollowUps by remember { mutableStateOf(config.allowFollowUps) }
    var jobDescription by remember { mutableStateOf(config.jobDescription) }

    fun notifyChange() {
        onConfigChange(selectedType, questionCount, difficulty, allowFollowUps, jobDescription)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Step Banner
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Step 3 of 3: Session Setup",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = BrandBlue
                )
                Text(
                    text = "Configure AI Interview",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            DomainBadge(domain)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Interview Mode Selection (Resume Only vs Job-Specific)
        Text(
            text = "Interview Assessment Mode",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ModeCard(
                title = "Resume Interview",
                subtitle = "Resume is single source of truth. Evaluates candidate claims and experience.",
                icon = Icons.Default.Description,
                isSelected = selectedType == InterviewType.RESUME_ONLY,
                modifier = Modifier.weight(1f),
                onClick = {
                    selectedType = InterviewType.RESUME_ONLY
                    notifyChange()
                }
            )

            ModeCard(
                title = "Job-Specific",
                subtitle = "Evaluates resume alignment against specific target job requirements.",
                icon = Icons.Default.AssignmentInd,
                isSelected = selectedType == InterviewType.JOB_SPECIFIC,
                modifier = Modifier.weight(1f),
                onClick = {
                    selectedType = InterviewType.JOB_SPECIFIC
                    notifyChange()
                }
            )
        }

        // Optional Job Description Field if Job-Specific is active
        if (selectedType == InterviewType.JOB_SPECIFIC) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Target Job Description (Optional)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Paste responsibilities and required qualifications to test role fit:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = jobDescription,
                        onValueChange = {
                            jobDescription = it
                            notifyChange()
                        },
                        placeholder = { Text("e.g. Senior Structural Engineer responsible for bridge seismic calculations, coordinating with state DOT...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .testTag("job_description_input"),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Question Count
        Text(
            text = "Number of Questions: $questionCount",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Select interview session length",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(5, 8, 10, 12).forEach { count ->
                val isSelected = questionCount == count
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) BrandBlue else MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) BrandBlue else Slate300
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            questionCount = count
                            notifyChange()
                        }
                        .testTag("question_count_$count")
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$count Qs",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Difficulty Level
        Text(
            text = "Interview Depth & Rigor",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Entry", "Intermediate", "Advanced", "Expert").forEach { diff ->
                val isSelected = difficulty == diff
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) BrandBlue else MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) BrandBlue else Slate300
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            difficulty = diff
                            notifyChange()
                        }
                        .testTag("difficulty_${diff.lowercase()}")
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = diff,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Adaptive Follow-Ups Toggle Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DynamicForm,
                            contentDescription = null,
                            tint = BrandTeal,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Adaptive Follow-Up Questions",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "AI analyzes answers in real time and asks conversational follow-up questions to drill into practical execution.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = allowFollowUps,
                    onCheckedChange = {
                        allowFollowUps = it
                        notifyChange()
                    },
                    modifier = Modifier.testTag("follow_up_switch")
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Start Interview Button
        Button(
            onClick = onStartInterview,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("start_interview_button"),
            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Begin AI Interview",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ModeCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) BrandBlueLight.copy(alpha = 0.8f) else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) BrandBlue else Slate300
        ),
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag("mode_${title.lowercase().replace(" ", "_")}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) BrandBlue else Slate600,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) BrandBlueDark else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
