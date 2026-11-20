package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FamilyRepository
import com.example.model.FamilyCategory
import com.example.model.FamilyMember
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.util.BirthdayUtils
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FamilyViewModel(
    private val repository: FamilyRepository,
    private val currentUserId: String,
    private val defaultDisplayName: String
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<FamilyCategory?>(null)
    val selectedCategory: StateFlow<FamilyCategory?> = _selectedCategory.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    val userProfileState: StateFlow<UiState<UserProfile?>> = repository.observeUserProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing user profile", rootError)
            emit(UiState.Error(rootError.message ?: "প্রোফাইল লোড করতে সমস্যা হয়েছে"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val membersState: StateFlow<UiState<List<FamilyMember>>> = repository.observeFamilyMembers(currentUserId)
        .map<List<FamilyMember>, UiState<List<FamilyMember>>> { members ->
            val sorted = members.sortedWith(
                compareBy<FamilyMember>(
                    { !it.selfProfile },
                    { BirthdayUtils.daysUntilBirthday(it.birthDay, it.birthMonth) ?: Int.MAX_VALUE },
                    { it.fullName }
                )
            )
            UiState.Success(sorted)
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing family members", rootError)
            emit(UiState.Error(rootError.message ?: "পরিবারের তালিকা লোড করতে সমস্যা হয়েছে"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val filteredMembers: StateFlow<List<FamilyMember>> = combine(
        membersState,
        _searchQuery,
        _selectedCategory
    ) { state, query, category ->
        val list = (state as? UiState.Success)?.data ?: emptyList()
        val trimmed = query.trim().lowercase()
        list.filter { member ->
            val matchesCategory = category == null || FamilyCategory.fromRaw(member.category) == category
            val matchesSearch = trimmed.isEmpty() ||
                member.fullName.lowercase().contains(trimmed) ||
                member.nickname.lowercase().contains(trimmed) ||
                member.relation.lowercase().contains(trimmed) ||
                member.bio.lowercase().contains(trimmed) ||
                member.profession.lowercase().contains(trimmed) ||
                member.bloodGroup.lowercase().contains(trimmed)
            matchesCategory && matchesSearch
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptyList()
    )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: FamilyCategory?) {
        _selectedCategory.value = category
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    fun showMessage(message: String) {
        _actionMessage.value = message
    }

    fun saveUserProfile(profile: UserProfile, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.saveUserProfile(currentUserId, profile)
            _isSaving.value = false
            result.onSuccess {
                _actionMessage.value = "আপনার প্রোফাইল সফলভাবে সংরক্ষিত হয়েছে"
                onSuccess()
            }.onFailure { err ->
                _actionMessage.value = err.message ?: "প্রোফাইল সেভ করা যায়নি"
            }
        }
    }

    fun updateNotificationPreferences(
        notifyEnabled: Boolean,
        notifyDaysBefore: Long,
        notifyHour: Long
    ) {
        viewModelScope.launch {
            val currentName = (userProfileState.value as? UiState.Success)?.data?.fullName
                ?.ifBlank { defaultDisplayName }
                ?: defaultDisplayName
            val result = repository.updateNotificationPreferences(
                userId = currentUserId,
                fallbackName = currentName,
                notifyEnabled = notifyEnabled,
                notifyDaysBefore = notifyDaysBefore,
                notifyHour = notifyHour
            )
            result.onSuccess {
                _actionMessage.value = if (notifyEnabled) {
                    "জন্মদিনের নোটিফিকেশন সেটিংস আপডেট হয়েছে"
                } else {
                    "জন্মদিনের নোটিফিকেশন বন্ধ করা হয়েছে"
                }
            }.onFailure { err ->
                _actionMessage.value = err.message ?: "সেটিংস সেভ করা যায়নি"
            }
        }
    }

    fun saveFamilyMember(
        member: FamilyMember,
        existingId: String?,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.saveFamilyMember(currentUserId, member, existingId)
            _isSaving.value = false
            result.onSuccess {
                _actionMessage.value = if (existingId == null) {
                    "${member.fullName}-কে পরিবারে যোগ করা হয়েছে"
                } else {
                    "${member.fullName}-এর তথ্য আপডেট হয়েছে"
                }
                onSuccess()
            }.onFailure { err ->
                _actionMessage.value = err.message ?: "তথ্য সংরক্ষণ করা যায়নি"
            }
        }
    }

    fun toggleMemberReminder(memberId: String, enabled: Boolean) {
        viewModelScope.launch {
            val result = repository.toggleMemberReminder(currentUserId, memberId, enabled)
            result.onFailure { err ->
                _actionMessage.value = err.message ?: "রিমাইন্ডার পরিবর্তন করা যায়নি"
            }
        }
    }

    fun deleteFamilyMember(memberId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.deleteFamilyMember(currentUserId, memberId)
            _isSaving.value = false
            result.onSuccess {
                _actionMessage.value = "সদস্যের তথ্য মুছে ফেলা হয়েছে"
                onSuccess()
            }.onFailure { err ->
                _actionMessage.value = err.message ?: "মুছে ফেলা যায়নি"
            }
        }
    }

    private companion object {
        const val TAG = "FamilyVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
