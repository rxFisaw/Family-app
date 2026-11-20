package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

enum class FamilyCategory(val labelBn: String) {
    SELF("আমি"),
    PARENTS("মা-বাবা"),
    SIBLINGS("ভাই-বোন"),
    SPOUSE_KIDS("সঙ্গী ও সন্তান"),
    GRANDPARENTS("দাদা-দাদি / নানা-নানি"),
    RELATIVES("অন্যান্য আত্মীয়");

    companion object {
        fun fromRaw(raw: String?): FamilyCategory =
            entries.firstOrNull { it.name == raw } ?: RELATIVES
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    val userId: String = "",
    val fullName: String = "",
    val nickname: String = "",
    val bio: String = "",
    val birthDay: Long = 0L,
    val birthMonth: Long = 0L,
    val birthYear: Long = 0L,
    val relationLabel: String = "আমি",
    val profession: String = "",
    val bloodGroup: String = "",
    val phone: String = "",
    val address: String = "",
    val hobbies: String = "",
    val favoriteFood: String = "",
    val favoriteColor: String = "",
    val favoriteQuote: String = "",
    val photoBase64: String = "",
    val notifyEnabled: Boolean = true,
    val notifyDaysBefore: Long = 1L,
    val notifyHour: Long = 9L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FamilyMember(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val nickname: String = "",
    val relation: String = "",
    val category: String = FamilyCategory.RELATIVES.name,
    val bio: String = "",
    val birthDay: Long = 0L,
    val birthMonth: Long = 0L,
    val birthYear: Long = 0L,
    val profession: String = "",
    val bloodGroup: String = "",
    val phone: String = "",
    val address: String = "",
    val hobbies: String = "",
    val favoriteThing: String = "",
    val specialNote: String = "",
    val photoBase64: String = "",
    val birthdayReminder: Boolean = true,
    val selfProfile: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
