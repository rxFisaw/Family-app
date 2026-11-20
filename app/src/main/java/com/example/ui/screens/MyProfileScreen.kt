package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.ui.components.BLOOD_GROUPS
import com.example.ui.components.BirthdayDatePickerDialog
import com.example.ui.components.PhotoPickerAvatar
import com.example.util.BirthdayUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MyProfileScreen(
    userProfileState: UiState<UserProfile?>,
    defaultDisplayName: String,
    userEmail: String,
    isSaving: Boolean,
    onSaveProfile: (UserProfile) -> Unit,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val existing = (userProfileState as? UiState.Success)?.data

    var fullName by remember { mutableStateOf(defaultDisplayName) }
    var nickname by remember { mutableStateOf("") }
    var relationLabel by remember { mutableStateOf("আমি") }
    var bio by remember { mutableStateOf("") }
    var birthDay by remember { mutableLongStateOf(0L) }
    var birthMonth by remember { mutableLongStateOf(0L) }
    var birthYear by remember { mutableLongStateOf(0L) }
    var hideYear by remember { mutableStateOf(false) }
    var profession by remember { mutableStateOf("") }
    var bloodGroup by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var hobbies by remember { mutableStateOf("") }
    var favoriteFood by remember { mutableStateOf("") }
    var favoriteColor by remember { mutableStateOf("") }
    var favoriteQuote by remember { mutableStateOf("") }
    var photoBase64 by remember { mutableStateOf("") }
    var notifyEnabled by remember { mutableStateOf(true) }
    var notifyDaysBefore by remember { mutableLongStateOf(1L) }
    var notifyHour by remember { mutableLongStateOf(9L) }

    var hydratedOnce by remember { mutableStateOf(false) }
    LaunchedEffect(existing) {
        if (existing != null && !hydratedOnce) {
            hydratedOnce = true
            fullName = existing.fullName.ifBlank { defaultDisplayName }
            nickname = existing.nickname
            relationLabel = existing.relationLabel.ifBlank { "আমি" }
            bio = existing.bio
            birthDay = existing.birthDay
            birthMonth = existing.birthMonth
            birthYear = existing.birthYear
            hideYear = existing.birthDay > 0 && existing.birthYear == 0L
            profession = existing.profession
            bloodGroup = existing.bloodGroup
            phone = existing.phone
            address = existing.address
            hobbies = existing.hobbies
            favoriteFood = existing.favoriteFood
            favoriteColor = existing.favoriteColor
            favoriteQuote = existing.favoriteQuote
            photoBase64 = existing.photoBase64
            notifyEnabled = existing.notifyEnabled
            notifyDaysBefore = existing.notifyDaysBefore
            notifyHour = existing.notifyHour
        }
    }

    var isProcessingPhoto by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    if (showDatePicker) {
        BirthdayDatePickerDialog(
            initialDay = birthDay,
            initialMonth = birthMonth,
            initialYear = birthYear,
            onDismiss = { showDatePicker = false },
            onDateSelected = { d, m, y ->
                birthDay = d
                birthMonth = m
                birthYear = if (hideYear) 0L else y
            }
        )
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("সাইন-আউট করবেন?") },
            text = { Text("আপনি কি বর্তমান গুগল অ্যাকাউন্ট থেকে লগআউট করতে চান?") },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutDialog = false
                        onSignOutClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("সাইন-আউট")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "header") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "আমার প্রোফাইল",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    if (userEmail.isNotBlank()) {
                        Text(
                            text = userEmail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                OutlinedButton(
                    onClick = { showSignOutDialog = true },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("sign_out_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("সাইন-আউট")
                }
            }
        }

        item(key = "photo") {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    PhotoPickerAvatar(
                        photoBase64 = photoBase64,
                        name = fullName,
                        isProcessing = isProcessingPhoto,
                        onProcessingChanged = { isProcessingPhoto = it },
                        onPhotoSelected = { photoBase64 = it }
                    )
                    Text(
                        text = "আপনার প্রোফাইল সংরক্ষণ করলে পরিবারের তালিকায়ও সুন্দরভাবে যুক্ত থাকবে",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (validationError != null) {
            item(key = "error") {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = validationError!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        item(key = "personal_fields") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { if (it.length <= 80) fullName = it },
                    label = { Text("নিজের পূর্ণ নাম *") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("my_name_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = nickname,
                        onValueChange = { if (it.length <= 40) nickname = it },
                        label = { Text("ডাকনাম") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = relationLabel,
                        onValueChange = { if (it.length <= 40) relationLabel = it },
                        label = { Text("পরিচয় (যেমন: আমি / বড় ছেলে)") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = bio,
                    onValueChange = { if (it.length <= 500) bio = it },
                    label = { Text("নিজের সম্পর্কে বর্ণনা") },
                    placeholder = { Text("আপনার সম্পর্কে কিছু কথা লিখুন...") },
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("my_bio_input")
                )
            }
        }

        item(key = "my_birthday") {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "নিজের জন্মদিন",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("my_birthday_picker_button")
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (BirthdayUtils.hasBirthday(birthDay, birthMonth)) {
                                BirthdayUtils.formatBirthdayBn(
                                    birthDay,
                                    birthMonth,
                                    if (hideYear) 0L else birthYear
                                )
                            } else {
                                "নিজের জন্মদিন বেছে নিন"
                            }
                        )
                    }
                    if (BirthdayUtils.hasBirthday(birthDay, birthMonth)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Checkbox(
                                checked = hideYear,
                                onCheckedChange = { checked ->
                                    hideYear = checked
                                    if (checked) birthYear = 0L
                                }
                            )
                            Text("জন্মসাল দেখাবেন না", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        Text(
                            text = "জন্মদিনের নোটিফিকেশন চালু রাখুন",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = notifyEnabled,
                            onCheckedChange = { notifyEnabled = it }
                        )
                    }
                }
            }
        }

        item(key = "extra_details") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "আরও তথ্য (রক্তের গ্রুপ, পেশা, পছন্দ)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BLOOD_GROUPS.forEach { bg ->
                        FilterChip(
                            selected = bloodGroup == bg,
                            onClick = { bloodGroup = if (bloodGroup == bg) "" else bg },
                            label = { Text(bg) }
                        )
                    }
                }

                OutlinedTextField(
                    value = profession,
                    onValueChange = { if (it.length <= 80) profession = it },
                    label = { Text("পেশা / পড়াশোনা") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { if (it.length <= 30) phone = it },
                    label = { Text("মোবাইল নম্বর") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { if (it.length <= 150) address = it },
                    label = { Text("বর্তমান শহর / ঠিকানা") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = hobbies,
                    onValueChange = { if (it.length <= 200) hobbies = it },
                    label = { Text("শখ ও আগ্রহ") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = favoriteFood,
                        onValueChange = { if (it.length <= 100) favoriteFood = it },
                        label = { Text("প্রিয় খাবার") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = favoriteColor,
                        onValueChange = { if (it.length <= 60) favoriteColor = it },
                        label = { Text("প্রিয় রং") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = favoriteQuote,
                    onValueChange = { if (it.length <= 250) favoriteQuote = it },
                    label = { Text("প্রিয় উক্তি বা জীবনের লক্ষ্য") },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item(key = "save_profile_btn") {
            Button(
                onClick = {
                    if (fullName.isBlank()) {
                        validationError = "অনুগ্রহ করে আপনার নাম লিখুন।"
                    } else {
                        validationError = null
                        onSaveProfile(
                            UserProfile(
                                fullName = fullName.trim(),
                                nickname = nickname.trim(),
                                relationLabel = relationLabel.trim().ifEmpty { "আমি" },
                                bio = bio.trim(),
                                birthDay = birthDay,
                                birthMonth = birthMonth,
                                birthYear = if (hideYear) 0L else birthYear,
                                profession = profession.trim(),
                                bloodGroup = bloodGroup.trim(),
                                phone = phone.trim(),
                                address = address.trim(),
                                hobbies = hobbies.trim(),
                                favoriteFood = favoriteFood.trim(),
                                favoriteColor = favoriteColor.trim(),
                                favoriteQuote = favoriteQuote.trim(),
                                photoBase64 = photoBase64,
                                notifyEnabled = notifyEnabled,
                                notifyDaysBefore = notifyDaysBefore,
                                notifyHour = notifyHour
                            )
                        )
                    }
                },
                enabled = !isSaving && !isProcessingPhoto,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("save_my_profile_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("প্রোফাইল সংরক্ষণ করুন", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
