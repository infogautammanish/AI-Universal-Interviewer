package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.samples.SampleResume
import com.example.data.samples.SampleResumes
import com.example.ui.components.DomainBadge
import com.example.ui.theme.*

@Composable
fun ResumeUploadScreen(
    candidateName: String,
    resumeText: String,
    errorMessage: String?,
    onResumeTextChange: (String) -> Unit,
    onSelectSample: (SampleResume) -> Unit,
    onProcessResume: () -> Unit
) {
    val scrollState = rememberScrollState()

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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Step 1 of 3: Resume Input",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = BrandBlue
                )
                Text(
                    text = "Upload or Paste Resume",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = "Candidate: $candidateName",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Diverse Samples Header
        Text(
            text = "Explore Diverse Professional Domains",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Select any sample below to see the AI dynamically adapt to that exact industry:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Horizontal Carousel of Sample Resumes
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(SampleResumes.samples) { sample ->
                SampleResumeCard(
                    sample = sample,
                    isSelected = resumeText.contains(sample.candidateName),
                    onClick = { onSelectSample(sample) }
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Resume Text Input Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Resume Content",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (resumeText.isNotBlank()) {
                        TextButton(
                            onClick = { onResumeTextChange("") },
                            modifier = Modifier.testTag("clear_resume_button")
                        ) {
                            Text("Clear", color = DangerRed, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                Text(
                    text = "Paste complete resume text. The AI extracts education, experience, skills, and claims without preset formats.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = resumeText,
                    onValueChange = onResumeTextChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .testTag("resume_text_area"),
                    placeholder = {
                        Text("Paste your resume text here, or tap any of the domain sample cards above...")
                    },
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${resumeText.length} characters",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate500
                    )
                    Text(
                        text = "${resumeText.lines().filter { it.isNotBlank() }.size} content lines",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate500
                    )
                }
            }
        }

        if (!errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = DangerRed,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Action Button
        Button(
            onClick = onProcessResume,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("process_resume_button"),
            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Analyze Resume with AI",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SampleResumeCard(
    sample: SampleResume,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) BrandBlueLight.copy(alpha = 0.8f) else MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .width(220.dp)
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) BrandBlue else Slate200,
                shape = RoundedCornerShape(14.dp)
            )
            .testTag("sample_card_${sample.domain.lowercase().replace(" ", "_")}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            DomainBadge(sample.domain)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = sample.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = sample.candidateName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isSelected) "Active Sample" else "Use Sample",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) BrandBlueDark else BrandBlue
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = if (isSelected) BrandBlueDark else BrandBlue
                )
            }
        }
    }
}
