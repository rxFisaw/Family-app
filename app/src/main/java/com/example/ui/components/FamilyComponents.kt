package com.example.ui.components

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.FamilyMember
import com.example.util.BirthdayUtils
import com.example.util.ImageUtils
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone

val BLOOD_GROUPS = listOf("A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-")

@Composable
fun MemberAvatar(
    photoBase64: String,
    name: String,
    size: Dp = 64.dp,
    modifier: Modifier = Modifier,
    highlightBorder: Boolean = false
) {
    val imageBitmap = remember(photoBase64) {
        ImageUtils.decodeBase64ToBitmap(photoBase64)?.asImageBitmap()
    }
    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: ""
    val borderMod = if (highlightBorder) {
        Modifier.border(
            width = 2.5.dp,
            brush = Brush.linearGradient(
                listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.tertiary
                )
            ),
            shape = CircleShape
        )
    } else {
        Modifier.border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            shape = CircleShape
        )
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .then(borderMod)
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.tertiaryContainer
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = name.ifBlank { "সদস্যের ছবি" },
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (initial.isNotEmpty()) {
            Text(
                text = initial,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "সদস্যের ছবি",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(size * 0.5f)
            )
        }
    }
}

@Composable
fun PhotoPickerAvatar(
    photoBase64: String,
    name: String,
    isProcessing: Boolean,
    onProcessingChanged: (Boolean) -> Unit,
    onPhotoSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                scope.launch {
                    onProcessingChanged(true)
                    val encoded = ImageUtils.encodePhotoUriToBase64(context, uri)
                    onProcessingChanged(false)
                    if (encoded != null) {
                        onPhotoSelected(encoded)
                    }
                }
            }
        }
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.BottomEnd,
            modifier = Modifier
                .size(116.dp)
                .clickable(enabled = !isProcessing) {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
                .testTag("pick_photo_button")
        ) {
            MemberAvatar(
                photoBase64 = photoBase64,
                name = name,
                size = 112.dp,
                highlightBorder = true
            )
            if (isProcessing) {
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = Color.White,
                        strokeWidth = 3.dp
                    )
                }
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 4.dp,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "ছবি পরিবর্তন করুন",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                enabled = !isProcessing
            ) {
                Text(if (photoBase64.isBlank()) "গ্যালারি থেকে ছবি যোগ করুন" else "ছবি পরিবর্তন করুন")
            }
            if (photoBase64.isNotBlank()) {
                IconButton(
                    onClick = { onPhotoSelected("") },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "ছবি মুছুন",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BirthdayCountdownBadge(
    birthDay: Long,
    birthMonth: Long,
    modifier: Modifier = Modifier
) {
    val days = BirthdayUtils.daysUntilBirthday(birthDay, birthMonth) ?: return
    val isToday = days == 0
    val isSoon = days in 1..7

    val containerColor = when {
        isToday -> MaterialTheme.colorScheme.primary
        isSoon -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = when {
        isToday -> MaterialTheme.colorScheme.onPrimary
        isSoon -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = containerColor,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Cake,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = BirthdayUtils.formatCountdownBn(days),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun InfoTagPill(
    label: String,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = containerColor,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BirthdayDatePickerDialog(
    initialDay: Long,
    initialMonth: Long,
    initialYear: Long,
    onDismiss: () -> Unit,
    onDateSelected: (day: Long, month: Long, year: Long) -> Unit
) {
    val initialUtcMillis = remember(initialDay, initialMonth, initialYear) {
        if (BirthdayUtils.hasBirthday(initialDay, initialMonth)) {
            Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                val yr = if (initialYear > 1900L) initialYear.toInt() else 1995
                set(yr, (initialMonth - 1).toInt(), initialDay.toInt(), 12, 0, 0)
            }.timeInMillis
        } else {
            Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                set(1995, Calendar.JANUARY, 15, 12, 0, 0)
            }.timeInMillis
        }
    }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialUtcMillis)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                            timeInMillis = millis
                        }
                        val d = cal.get(Calendar.DAY_OF_MONTH).toLong()
                        val m = (cal.get(Calendar.MONTH) + 1).toLong()
                        val y = cal.get(Calendar.YEAR).toLong()
                        onDateSelected(d, m, y)
                    }
                    onDismiss()
                }
            ) {
                Text("নিশ্চিত করুন")
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = {
                        onDateSelected(0L, 0L, 0L)
                        onDismiss()
                    }
                ) {
                    Text("তারিখ মুছুন")
                }
                TextButton(onClick = onDismiss) {
                    Text("বাতিল")
                }
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

fun shareBirthdayWish(context: Context, member: FamilyMember) {
    val dateStr = BirthdayUtils.formatBirthdayBn(member.birthDay, member.birthMonth, 0L)
    val message = buildString {
        append("শুভ জন্মদিন, ${member.fullName}")
        if (member.nickname.isNotBlank()) append(" (${member.nickname})")
        append("! 🎉🎂\n")
        append("আপনার জীবন সুস্থতা, আনন্দ আর ভালোবাসায় ভরে উঠুক।")
        if (BirthdayUtils.hasBirthday(member.birthDay, member.birthMonth)) {
            append("\n— জন্মদিন: $dateStr")
        }
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "শুভ জন্মদিন ${member.fullName}!")
        putExtra(Intent.EXTRA_TEXT, message)
    }
    context.startActivity(Intent.createChooser(intent, "শুভেচ্ছা পাঠান"))
}
