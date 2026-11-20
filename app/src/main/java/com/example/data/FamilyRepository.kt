package com.example.data

import android.content.Context
import com.example.R
import com.example.model.FamilyCategory
import com.example.model.FamilyMember
import com.example.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FamilyRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    companion object {
        const val SELF_MEMBER_DOC_ID = "self_profile"
    }

    private fun requireUserId(expectedUid: String): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("পরিবারের তথ্য দেখতে বা সেভ করতে প্রথমে সাইন-ইন করুন।")
        check(uid == expectedUid) { "ব্যবহারকারীর সেশন পরিবর্তিত হয়েছে।" }
        return uid
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId(userId)
        val docRef = db.collection("users").document(uid)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (!snapshot.exists()) null
                    else snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.GET, docRef.path)
                    }
                    throw error
                }
        )
    }

    fun observeFamilyMembers(userId: String): Flow<List<FamilyMember>> = flow {
        val uid = requireUserId(userId)
        val colRef = db.collection("users").document(uid).collection("members")
        emitAll(
            colRef.snapshots()
                .map { querySnapshot ->
                    querySnapshot.toObjects(
                        FamilyMember::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.LIST, colRef.path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveUserProfile(userId: String, profile: UserProfile): Result<Unit> {
        var docPath = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            val fullName = profile.fullName.trim().take(80)
            require(fullName.isNotEmpty()) { "অনুগ্রহ করে আপনার নাম লিখুন।" }
            require(profile.photoBase64.length <= 250_000) { "ছবির আকার অনেক বড়, অনুগ্রহ করে অন্য ছবি বেছে নিন।" }

            val docRef = db.collection("users").document(uid)
            docPath = docRef.path
            val exists = docRef.get().await().exists()

            val mutableFields = mapOf(
                "fullName" to fullName,
                "nickname" to profile.nickname.trim().take(40),
                "bio" to profile.bio.trim().take(500),
                "birthDay" to profile.birthDay.coerceIn(0L, 31L),
                "birthMonth" to profile.birthMonth.coerceIn(0L, 12L),
                "birthYear" to profile.birthYear.coerceIn(0L, 2100L),
                "relationLabel" to profile.relationLabel.trim().ifEmpty { "আমি" }.take(40),
                "profession" to profile.profession.trim().take(80),
                "bloodGroup" to profile.bloodGroup.trim().take(10),
                "phone" to profile.phone.trim().take(30),
                "address" to profile.address.trim().take(150),
                "hobbies" to profile.hobbies.trim().take(200),
                "favoriteFood" to profile.favoriteFood.trim().take(100),
                "favoriteColor" to profile.favoriteColor.trim().take(60),
                "favoriteQuote" to profile.favoriteQuote.trim().take(250),
                "photoBase64" to profile.photoBase64,
                "notifyEnabled" to profile.notifyEnabled,
                "notifyDaysBefore" to profile.notifyDaysBefore.coerceIn(0L, 7L),
                "notifyHour" to profile.notifyHour.coerceIn(0L, 23L),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            if (exists) {
                docRef.update(mutableFields).await()
            } else {
                val createFields = mutableFields + mapOf(
                    "userId" to uid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                docRef.set(createFields).await()
            }

            // Sync self card into family directory so user appears with everyone else
            val selfMemberRef = db.collection("users").document(uid)
                .collection("members").document(SELF_MEMBER_DOC_ID)
            val selfExists = selfMemberRef.get().await().exists()
            val favCombined = listOf(profile.favoriteFood.trim(), profile.favoriteColor.trim())
                .filter { it.isNotEmpty() }
                .joinToString(" • ")
                .take(120)

            val selfMutableFields = mapOf(
                "fullName" to fullName,
                "nickname" to profile.nickname.trim().take(40),
                "relation" to profile.relationLabel.trim().ifEmpty { "আমি" }.take(40),
                "category" to FamilyCategory.SELF.name,
                "bio" to profile.bio.trim().take(500),
                "birthDay" to profile.birthDay.coerceIn(0L, 31L),
                "birthMonth" to profile.birthMonth.coerceIn(0L, 12L),
                "birthYear" to profile.birthYear.coerceIn(0L, 2100L),
                "profession" to profile.profession.trim().take(80),
                "bloodGroup" to profile.bloodGroup.trim().take(10),
                "phone" to profile.phone.trim().take(30),
                "address" to profile.address.trim().take(150),
                "hobbies" to profile.hobbies.trim().take(200),
                "favoriteThing" to favCombined,
                "specialNote" to profile.favoriteQuote.trim().take(300),
                "photoBase64" to profile.photoBase64,
                "birthdayReminder" to profile.notifyEnabled,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            if (selfExists) {
                selfMemberRef.update(selfMutableFields).await()
            } else {
                val selfCreateFields = selfMutableFields + mapOf(
                    "userId" to uid,
                    "selfProfile" to true,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                selfMemberRef.set(selfCreateFields).await()
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateNotificationPreferences(
        userId: String,
        fallbackName: String,
        notifyEnabled: Boolean,
        notifyDaysBefore: Long,
        notifyHour: Long
    ): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            val docRef = db.collection("users").document(uid)
            val exists = docRef.get().await().exists()
            if (exists) {
                docRef.update(
                    mapOf(
                        "notifyEnabled" to notifyEnabled,
                        "notifyDaysBefore" to notifyDaysBefore.coerceIn(0L, 7L),
                        "notifyHour" to notifyHour.coerceIn(0L, 23L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val safeName = fallbackName.trim().ifEmpty { "আমি" }.take(80)
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "fullName" to safeName,
                        "nickname" to "",
                        "bio" to "",
                        "birthDay" to 0L,
                        "birthMonth" to 0L,
                        "birthYear" to 0L,
                        "relationLabel" to "আমি",
                        "profession" to "",
                        "bloodGroup" to "",
                        "phone" to "",
                        "address" to "",
                        "hobbies" to "",
                        "favoriteFood" to "",
                        "favoriteColor" to "",
                        "favoriteQuote" to "",
                        "photoBase64" to "",
                        "notifyEnabled" to notifyEnabled,
                        "notifyDaysBefore" to notifyDaysBefore.coerceIn(0L, 7L),
                        "notifyHour" to notifyHour.coerceIn(0L, 23L),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveFamilyMember(
        userId: String,
        member: FamilyMember,
        existingId: String?
    ): Result<Unit> {
        var docPath = "users/$userId/members"
        return try {
            val uid = requireUserId(userId)
            val fullName = member.fullName.trim().take(80)
            val relation = member.relation.trim().take(40)
            require(fullName.isNotEmpty()) { "সদস্যের নাম লিখুন।" }
            require(relation.isNotEmpty()) { "সম্পর্ক উল্লেখ করুন।" }
            require(member.photoBase64.length <= 250_000) { "ছবির আকার অনেক বড়, অনুগ্রহ করে অন্য ছবি বেছে নিন।" }

            val safeCategory = FamilyCategory.fromRaw(member.category).name
            val mutableFields = mapOf(
                "fullName" to fullName,
                "nickname" to member.nickname.trim().take(40),
                "relation" to relation,
                "category" to safeCategory,
                "bio" to member.bio.trim().take(500),
                "birthDay" to member.birthDay.coerceIn(0L, 31L),
                "birthMonth" to member.birthMonth.coerceIn(0L, 12L),
                "birthYear" to member.birthYear.coerceIn(0L, 2100L),
                "profession" to member.profession.trim().take(80),
                "bloodGroup" to member.bloodGroup.trim().take(10),
                "phone" to member.phone.trim().take(30),
                "address" to member.address.trim().take(150),
                "hobbies" to member.hobbies.trim().take(200),
                "favoriteThing" to member.favoriteThing.trim().take(120),
                "specialNote" to member.specialNote.trim().take(300),
                "photoBase64" to member.photoBase64,
                "birthdayReminder" to member.birthdayReminder,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val colRef = db.collection("users").document(uid).collection("members")
            if (!existingId.isNullOrBlank()) {
                val docRef = colRef.document(existingId)
                docPath = docRef.path
                docRef.update(mutableFields).await()
            } else {
                val docRef = colRef.document()
                docPath = docRef.path
                val createFields = mutableFields + mapOf(
                    "userId" to uid,
                    "selfProfile" to member.selfProfile,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                docRef.set(createFields).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleMemberReminder(
        userId: String,
        memberId: String,
        enabled: Boolean
    ): Result<Unit> {
        val docPath = "users/$userId/members/$memberId"
        return try {
            val uid = requireUserId(userId)
            require(memberId.isNotBlank()) { "সদস্যের আইডি সঠিক নয়।" }
            db.collection("users").document(uid)
                .collection("members").document(memberId)
                .update(
                    mapOf(
                        "birthdayReminder" to enabled,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteFamilyMember(userId: String, memberId: String): Result<Unit> {
        val docPath = "users/$userId/members/$memberId"
        return try {
            val uid = requireUserId(userId)
            require(memberId.isNotBlank()) { "সদস্যের আইডি সঠিক নয়।" }
            db.collection("users").document(uid)
                .collection("members").document(memberId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
