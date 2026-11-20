package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.FamilyMember
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.notifications.BirthdayNotificationHelper
import com.example.ui.components.BirthdayCountdownBadge
import com.example.ui.components.MemberAvatar
import com.example.ui.components.shareBirthdayWish
import com.example.util.BirthdayUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BirthdaysScreen(
    membersState: UiState<List<FamilyMember>>,
    userProfileState: UiState<UserProfile?>,
    onMemberClick: (String) -> Unit,
    onToggleMemberReminder: (String, Boolean) -> Unit,
    onUpdateNotificationPrefs: (Boolean, Long, Long) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasNotificationPermission by remember {
        mutableStateOf(BirthdayNotificationHelper.hasNotificationPermission(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasNotificationPermission = granted
            if (granted) {
                onShowMessage("নোটিফিকেশন অনুমতি দেওয়া হয়েছে")
            } else {
                onShowMessage("নোটিফিকেশন পেতে অ্যাপের সেটিংস থেকে অনুমতি দিন")
            }
        }
    )

    val members = (membersState as? UiState.Success)?.data ?: emptyList()
    val profile = (userProfileState as? UiState.Success)?.data
    val notifyEnabled = profile?.notifyEnabled ?: true
    val notifyDaysBefore = profile?.notifyDaysBefore ?: 1L
    val notifyHour = profile?.notifyHour ?: 9L

    val birthdayMembers = members
        .filter { BirthdayUtils.hasBirthday(it.birthDay, it.birthMonth) }
        .sortedBy { BirthdayUtils.daysUntilBirthday(it.birthDay, it.birthMonth) ?: Int.MAX_VALUE }

    val todaysBirthdays = birthdayMembers.filter {
        BirthdayUtils.daysUntilBirthday(it.birthDay, it.birthMonth) == 0
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "header") {
            Column {
                Text(
                    text = "জন্মদিন ও নোটিফিকেশন",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "প্রিয়জনের জন্মদিন মনে রাখুন ও সময়মতো নোটিফিকেশন পান",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (todaysBirthdays.isNotEmpty()) {
            item(key = "todays_celebration") {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.tertiaryContainer
                                    )
                                )
                            )
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Celebration,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "আজকের শুভ জন্মদিন!",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        todaysBirthdays.forEach { celebrant ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                MemberAvatar(
                                    photoBase64 = celebrant.photoBase64,
                                    name = celebrant.fullName,
                                    size = 50.dp,
                                    highlightBorder = true
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = celebrant.fullName,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = celebrant.relation,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Button(
                                    onClick = { shareBirthdayWish(context, celebrant) },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("শুভেচ্ছা")
                                }
                            }
                        }
                    }
                }
            }
        }

        item(key = "notification_settings_card") {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (notifyEnabled) MaterialTheme.colorScheme.secondaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (notifyEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                contentDescription = null,
                                tint = if (notifyEnabled) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "জন্মদিনের নোটিফিকেশন",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = if (notifyEnabled) "প্রতিদিন নির্ধারিত সময়ে চেক করা হবে" else "নোটিফিকেশন বন্ধ আছে",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = notifyEnabled,
                            onCheckedChange = { enabled ->
                                onUpdateNotificationPrefs(enabled, notifyDaysBefore, notifyHour)
                            },
                            modifier = Modifier.testTag("master_notification_switch")
                        )
                    }

                    if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = "নোটিফিকেশন দেখাতে অনুমতির প্রয়োজন",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.tertiary
                                    ),
                                    modifier = Modifier.testTag("grant_notification_permission_button")
                                ) {
                                    Text("অনুমতি দিন")
                                }
                            }
                        }
                    }

                    if (notifyEnabled) {
                        Text(
                            text = "কখন মনে করিয়ে দেবে?",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                0L to "জন্মদিনের দিন",
                                1L to "১ দিন আগে",
                                3L to "৩ দিন আগে"
                            ).forEach { (days, label) ->
                                FilterChip(
                                    selected = notifyDaysBefore == days,
                                    onClick = {
                                        onUpdateNotificationPrefs(true, days, notifyHour)
                                    },
                                    label = { Text(label) }
                                )
                            }
                        }

                        Text(
                            text = "নোটিফিকেশনের সময়",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                0L to "রাত ১২:০০",
                                8L to "সকাল ৮:০০",
                                9L to "সকাল ৯:০০",
                                10L to "সকাল ১০:০০"
                            ).forEach { (hr, label) ->
                                FilterChip(
                                    selected = notifyHour == hr,
                                    onClick = {
                                        onUpdateNotificationPrefs(true, notifyDaysBefore, hr)
                                    },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                val sent = BirthdayNotificationHelper.sendTestNotification(
                                    context = context,
                                    nextMember = birthdayMembers.firstOrNull()
                                )
                                if (sent) {
                                    onShowMessage("টেস্ট নোটিফিকেশন পাঠানো হয়েছে!")
                                } else {
                                    onShowMessage("নোটিফিকেশন অনুমতি পাওয়া যায়নি")
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("test_notification_button")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("টেস্ট নোটিফিকেশন পাঠান")
                    }
                }
            }
        }

        item(key = "upcoming_title") {
            Text(
                text = "আসন্ন জন্মদিনসমূহ (${BirthdayUtils.toBengaliDigits(birthdayMembers.size)})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (birthdayMembers.isEmpty()) {
            item(key = "no_birthdays") {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cake,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "এখনো কারও জন্মদিন যোগ করা হয়নি",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "পরিবারের সদস্যদের বা নিজের প্রোফাইলে জন্মদিন যোগ করলে এখানে কাউন্টডাউন দেখতে পাবেন।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(birthdayMembers, key = { it.id }) { member ->
                val formattedDate = BirthdayUtils.formatBirthdayBn(
                    member.birthDay,
                    member.birthMonth,
                    member.birthYear
                )
                val upcomingAge = BirthdayUtils.upcomingAge(
                    member.birthDay,
                    member.birthMonth,
                    member.birthYear
                )

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onMemberClick(member.id) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        MemberAvatar(
                            photoBase64 = member.photoBase64,
                            name = member.fullName,
                            size = 56.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${member.fullName} (${member.relation})",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = formattedDate + if (upcomingAge != null) {
                                    " • ${BirthdayUtils.toBengaliDigits(upcomingAge)} বছরে পা দেবেন"
                                } else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            BirthdayCountdownBadge(
                                birthDay = member.birthDay,
                                birthMonth = member.birthMonth
                            )
                        }
                        Switch(
                            checked = member.birthdayReminder,
                            onCheckedChange = { enabled ->
                                onToggleMemberReminder(member.id, enabled)
                            }
                        )
                    }
                }
            }
        }
    }
}
