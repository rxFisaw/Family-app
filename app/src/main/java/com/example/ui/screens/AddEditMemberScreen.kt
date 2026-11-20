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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import com.example.model.FamilyCategory
import com.example.model.FamilyMember
import com.example.ui.components.BLOOD_GROUPS
import com.example.ui.components.BirthdayDatePickerDialog
import com.example.ui.components.PhotoPickerAvatar
import com.example.util.BirthdayUtils

private val QUICK_RELATIONS = listOf(
    "বাবা" to FamilyCategory.PARENTS,
    "মা" to FamilyCategory.PARENTS,
    "বড় ভাই" to FamilyCategory.SIBLINGS,
    "ছোট ভাই" to FamilyCategory.SIBLINGS,
    "বড় বোন" to FamilyCategory.SIBLINGS,
    "ছোট বোন" to FamilyCategory.SIBLINGS,
    "স্বামী/স্ত্রী" to FamilyCategory.SPOUSE_KIDS,
    "সন্তান" to FamilyCategory.SPOUSE_KIDS,
    "দাদা/নানা" to FamilyCategory.GRANDPARENTS,
    "দাদি/নানি" to FamilyCategory.GRANDPARENTS,
    "চাচা/মামা" to FamilyCategory.RELATIVES,
    "খালা/ফুফু" to FamilyCategory.RELATIVES
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditMemberScreen(
    existingMember: FamilyMember?,
    isSaving: Boolean,
    onBack: () -> Unit,
    onSave: (FamilyMember, String?) -> Unit
) {
    val isEditing = existingMember != null

    var fullName by remember(existingMember?.id) { mutableStateOf(existingMember?.fullName.orEmpty()) }
    var nickname by remember(existingMember?.id) { mutableStateOf(existingMember?.nickname.orEmpty()) }
    var relation by remember(existingMember?.id) { mutableStateOf(existingMember?.relation.orEmpty()) }
    var category by remember(existingMember?.id) {
        mutableStateOf(
            if (existingMember != null) FamilyCategory.fromRaw(existingMember.category)
            else FamilyCategory.PARENTS
        )
    }
    var bio by remember(existingMember?.id) { mutableStateOf(existingMember?.bio.orEmpty()) }
    var birthDay by remember(existingMember?.id) { mutableLongStateOf(existingMember?.birthDay ?: 0L) }
    var birthMonth by remember(existingMember?.id) { mutableLongStateOf(existingMember?.birthMonth ?: 0L) }
    var birthYear by remember(existingMember?.id) { mutableLongStateOf(existingMember?.birthYear ?: 0L) }
    var hideYear by remember(existingMember?.id) {
        mutableStateOf(existingMember != null && existingMember.birthDay > 0 && existingMember.birthYear == 0L)
    }
    var profession by remember(existingMember?.id) { mutableStateOf(existingMember?.profession.orEmpty()) }
    var bloodGroup by remember(existingMember?.id) { mutableStateOf(existingMember?.bloodGroup.orEmpty()) }
    var phone by remember(existingMember?.id) { mutableStateOf(existingMember?.phone.orEmpty()) }
    var address by remember(existingMember?.id) { mutableStateOf(existingMember?.address.orEmpty()) }
    var hobbies by remember(existingMember?.id) { mutableStateOf(existingMember?.hobbies.orEmpty()) }
    var favoriteThing by remember(existingMember?.id) { mutableStateOf(existingMember?.favoriteThing.orEmpty()) }
    var specialNote by remember(existingMember?.id) { mutableStateOf(existingMember?.specialNote.orEmpty()) }
    var photoBase64 by remember(existingMember?.id) { mutableStateOf(existingMember?.photoBase64.orEmpty()) }
    var birthdayReminder by remember(existingMember?.id) { mutableStateOf(existingMember?.birthdayReminder ?: true) }

    var isProcessingPhoto by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "সদস্যের তথ্য সম্পাদনা" else "নতুন সদস্য যোগ করুন") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("add_edit_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "ফিরে যান"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "photo_picker") {
                PhotoPickerAvatar(
                    photoBase64 = photoBase64,
                    name = fullName,
                    isProcessing = isProcessingPhoto,
                    onProcessingChanged = { isProcessingPhoto = it },
                    onPhotoSelected = { photoBase64 = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (validationError != null) {
                item(key = "validation_error") {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = validationError!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            item(key = "basic_info") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { if (it.length <= 80) fullName = it },
                        label = { Text("পূর্ণ নাম *") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("member_name_input")
                    )

                    OutlinedTextField(
                        value = nickname,
                        onValueChange = { if (it.length <= 40) nickname = it },
                        label = { Text("ডাকনাম (ঐচ্ছিক)") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "দ্রুত সম্পর্ক বাছাই করুন",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        QUICK_RELATIONS.forEach { (label, cat) ->
                            FilterChip(
                                selected = relation == label,
                                onClick = {
                                    relation = label
                                    if (existingMember?.selfProfile != true) {
                                        category = cat
                                    }
                                },
                                label = { Text(label) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = relation,
                        onValueChange = { if (it.length <= 40) relation = it },
                        label = { Text("আপনার সাথে সম্পর্ক * (যেমন: বাবা, মামা, বন্ধু)") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("member_relation_input")
                    )

                    Text(
                        text = "পরিবারের বিভাগ",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FamilyCategory.entries.filter {
                            existingMember?.selfProfile == true || it != FamilyCategory.SELF
                        }.forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat.labelBn) }
                            )
                        }
                    }
                }
            }

            item(key = "birthday_section") {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "জন্মদিন ও নোটিফিকেশন",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pick_birthday_button")
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
                                    "জন্মদিন নির্বাচন করুন"
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
                                Text(
                                    text = "জন্মসাল গোপন / জানা নেই (শুধু দিন ও মাস)",
                                    style = MaterialTheme.typography.bodySmall
                                )
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
                                checked = birthdayReminder,
                                onCheckedChange = { birthdayReminder = it }
                            )
                        }
                    }
                }
            }

            item(key = "description_and_more") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = bio,
                        onValueChange = { if (it.length <= 500) bio = it },
                        label = { Text("তাঁর সম্পর্কে বর্ণনা") },
                        placeholder = { Text("তাঁর স্বভাব, গুণাবলি বা সুন্দর কোনো গল্প লিখুন...") },
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("member_bio_input")
                    )

                    Text(
                        text = "রক্তের গ্রুপ",
                        style = MaterialTheme.typography.labelLarge,
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
                        label = { Text("পেশা বা পড়াশোনা") },
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
                        label = { Text("ঠিকানা / বর্তমান শহর") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = hobbies,
                        onValueChange = { if (it.length <= 200) hobbies = it },
                        label = { Text("শখ ও আগ্রহ (যেমন: বাগান করা, বই পড়া)") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = favoriteThing,
                        onValueChange = { if (it.length <= 120) favoriteThing = it },
                        label = { Text("প্রিয় খাবার / প্রিয় রং") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = specialNote,
                        onValueChange = { if (it.length <= 300) specialNote = it },
                        label = { Text("প্রিয় স্মৃতি বা বিশেষ নোট") },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item(key = "save_button") {
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = {
                        when {
                            fullName.isBlank() -> validationError = "অনুগ্রহ করে সদস্যের নাম লিখুন।"
                            relation.isBlank() -> validationError = "অনুগ্রহ করে সম্পর্ক উল্লেখ করুন।"
                            else -> {
                                validationError = null
                                onSave(
                                    FamilyMember(
                                        id = existingMember?.id.orEmpty(),
                                        userId = existingMember?.userId.orEmpty(),
                                        fullName = fullName.trim(),
                                        nickname = nickname.trim(),
                                        relation = relation.trim(),
                                        category = category.name,
                                        bio = bio.trim(),
                                        birthDay = birthDay,
                                        birthMonth = birthMonth,
                                        birthYear = if (hideYear) 0L else birthYear,
                                        profession = profession.trim(),
                                        bloodGroup = bloodGroup.trim(),
                                        phone = phone.trim(),
                                        address = address.trim(),
                                        hobbies = hobbies.trim(),
                                        favoriteThing = favoriteThing.trim(),
                                        specialNote = specialNote.trim(),
                                        photoBase64 = photoBase64,
                                        birthdayReminder = birthdayReminder,
                                        selfProfile = existingMember?.selfProfile ?: false
                                    ),
                                    existingMember?.id
                                )
                            }
                        }
                    },
                    enabled = !isSaving && !isProcessingPhoto,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("save_member_button")
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
                        Text(
                            text = if (isEditing) "পরিবর্তন সংরক্ষণ করুন" else "পরিবারে যোগ করুন",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
