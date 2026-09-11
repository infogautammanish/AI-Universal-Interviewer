package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
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
import com.example.data.local.entities.AssessmentReportEntity
import com.example.data.local.entities.AuditLogEntity
import com.example.data.local.entities.CandidateEntity
import com.example.ui.components.DomainBadge
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RecruiterDashboardScreen(
    candidates: List<CandidateEntity>,
    reports: List<AssessmentReportEntity>,
    auditLogs: List<AuditLogEntity>,
    onSelectCandidate: (CandidateEntity) -> Unit,
    onViewCandidateReport: (AssessmentReportEntity) -> Unit,
    onStartNewCandidate: () -> Unit
) {
    var selectedDomainFilter by remember { mutableStateOf("All") }
    var selectedTab by remember { mutableStateOf(0) } // 0: Candidates, 1: Assessment Reports, 2: Audit Logs

    val domains = remember(candidates) {
        listOf("All") + candidates.map { it.detectedDomain }.distinct().filter { it.isNotBlank() && it != "Pending Evaluation" }
    }

    val filteredCandidates = remember(candidates, selectedDomainFilter) {
        if (selectedDomainFilter == "All") candidates
        else candidates.filter { it.detectedDomain.equals(selectedDomainFilter, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Recruiter Dashboard Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Recruiter Command Center",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Cross-Domain Talent Pipelines & AI Verification",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FilledTonalButton(
                    onClick = onStartNewCandidate,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("recruiter_new_candidate_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Candidate", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Navigation Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = BrandBlue,
                divider = {}
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Candidates (${candidates.size})", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Reports (${reports.size})", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Audit Trail (${auditLogs.size})", fontWeight = FontWeight.SemiBold) }
                )
            }
        }

        HorizontalDivider(color = Slate200)

        // Domain Filters Bar (if on Candidates or Reports tab)
        if (selectedTab != 2 && domains.size > 1) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(domains) { dom ->
                    val isSelected = selectedDomainFilter == dom
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedDomainFilter = dom },
                        label = { Text(dom) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandBlue,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // Tab Content
        Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            when (selectedTab) {
                0 -> CandidatesListView(
                    candidates = filteredCandidates,
                    onSelect = onSelectCandidate
                )
                1 -> ReportsListView(
                    reports = reports,
                    onSelectReport = onViewCandidateReport
                )
                2 -> AuditLogsListView(logs = auditLogs)
            }
        }
    }
}

@Composable
private fun CandidatesListView(
    candidates: List<CandidateEntity>,
    onSelect: (CandidateEntity) -> Unit
) {
    if (candidates.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.PersonSearch, contentDescription = null, tint = Slate400, modifier = Modifier.size(56.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("No candidates found", style = MaterialTheme.typography.titleMedium, color = Slate600)
                Text("Upload a resume to begin cross-domain evaluation.", style = MaterialTheme.typography.bodySmall, color = Slate500)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(candidates) { candidate ->
                CandidateRowCard(candidate = candidate, onClick = { onSelect(candidate) })
            }
        }
    }
}

@Composable
private fun CandidateRowCard(
    candidate: CandidateEntity,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("candidate_row_${candidate.id}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(BrandBlueLight),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = candidate.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BrandBlueDark
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = candidate.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = candidate.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DomainBadge(candidate.detectedDomain)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = candidate.experienceLevel,
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate500
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (candidate.status) {
                        "REPORT_GENERATED" -> SuccessGreenLight
                        "INTERVIEW_IN_PROGRESS" -> BrandBlueLight
                        else -> Slate100
                    }
                ) {
                    Text(
                        text = candidate.status.replace("_", " "),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (candidate.status) {
                            "REPORT_GENERATED" -> SuccessGreen
                            "INTERVIEW_IN_PROGRESS" -> BrandBlue
                            else -> Slate700
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Slate400)
            }
        }
    }
}

@Composable
private fun ReportsListView(
    reports: List<AssessmentReportEntity>,
    onSelectReport: (AssessmentReportEntity) -> Unit
) {
    if (reports.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Assessment, contentDescription = null, tint = Slate400, modifier = Modifier.size(56.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("No assessment reports yet", style = MaterialTheme.typography.titleMedium, color = Slate600)
                Text("Reports are automatically generated when an interview concludes.", style = MaterialTheme.typography.bodySmall, color = Slate500)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(reports) { rep ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectReport(rep) }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (rep.overallScore >= 80) SuccessGreenLight
                                    else if (rep.overallScore >= 70) BrandBlueLight
                                    else WarningAmberLight
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${rep.overallScore}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (rep.overallScore >= 80) SuccessGreen
                                else if (rep.overallScore >= 70) BrandBlue
                                else WarningAmber
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = rep.candidateName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Recommendation: ${rep.recommendation}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            DomainBadge(rep.domain)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = rep.recruiterStatus,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = BrandBlue
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Slate400)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditLogsListView(logs: List<AuditLogEntity>) {
    if (logs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No audit log records found.", color = Slate500)
        }
    } else {
        val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(logs) { log ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = BrandBlueLight
                            ) {
                                Text(
                                    text = log.actionStage,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandBlueDark,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = dateFormat.format(Date(log.timestamp)),
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate500
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = log.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = log.details,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
