package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.auth.authStateFlow
import com.example.auth.signOutUser
import com.example.data.FamilyRepository
import com.example.model.UiState
import com.example.notifications.BirthdayNotificationHelper
import com.example.ui.screens.AddEditMemberScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BirthdaysScreen
import com.example.ui.screens.FamilyDirectoryScreen
import com.example.ui.screens.MemberDetailScreen
import com.example.ui.screens.MyProfileScreen
import com.example.viewmodel.FamilyViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

const val ROUTE_FAMILY = "family"
const val ROUTE_BIRTHDAYS = "birthdays"
const val ROUTE_MY_PROFILE = "my_profile"
const val ROUTE_DETAIL = "detail/{memberId}"
const val ROUTE_ADD_EDIT = "add_edit?memberId={memberId}"

private data class TopTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val TOP_TABS = listOf(
    TopTab(ROUTE_FAMILY, "পরিবার", Icons.Filled.Groups, Icons.Outlined.Groups, "tab_family"),
    TopTab(ROUTE_BIRTHDAYS, "জন্মদিন", Icons.Filled.Cake, Icons.Outlined.Cake, "tab_birthdays"),
    TopTab(ROUTE_MY_PROFILE, "আমি", Icons.Filled.Person, Icons.Outlined.Person, "tab_my_profile")
)

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        val uid = user.uid
        val displayName = user.displayName.orEmpty().ifBlank { "আমি" }
        val email = user.email.orEmpty()
        key(uid) {
            AuthenticatedFamilyShell(
                currentUserId = uid,
                defaultDisplayName = displayName,
                userEmail = email
            )
        }
    }
}

@Composable
private fun AuthenticatedFamilyShell(
    currentUserId: String,
    defaultDisplayName: String,
    userEmail: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: FamilyViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                FamilyViewModel(
                    repository = FamilyRepository(db),
                    currentUserId = currentUserId,
                    defaultDisplayName = defaultDisplayName
                )
            }
        }
    )

    val membersState by viewModel.membersState.collectAsStateWithLifecycle()
    val filteredMembers by viewModel.filteredMembers.collectAsStateWithLifecycle()
    val userProfileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()

    // Keep cached offline birthday notifications synchronized with Firestore data
    LaunchedEffect(membersState, userProfileState) {
        val members = (membersState as? UiState.Success)?.data ?: return@LaunchedEffect
        val profile = (userProfileState as? UiState.Success)?.data
        BirthdayNotificationHelper.syncBirthdaysAndSchedule(
            context = context.applicationContext,
            members = members,
            notifyEnabled = profile?.notifyEnabled ?: true,
            daysBefore = (profile?.notifyDaysBefore ?: 1L).toInt(),
            notifyHour = (profile?.notifyHour ?: 9L).toInt()
        )
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(actionMessage) {
        val msg = actionMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearActionMessage()
        }
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showTopLevelChrome = currentRoute in setOf(ROUTE_FAMILY, ROUTE_BIRTHDAYS, ROUTE_MY_PROFILE)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (showTopLevelChrome && !useNavRail) {
                    NavigationBar {
                        TOP_TABS.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (currentRoute == ROUTE_FAMILY || currentRoute == ROUTE_BIRTHDAYS) {
                    FloatingActionButton(
                        onClick = { navController.navigate("add_edit") },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("add_member_fab")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "সদস্য যোগ করুন"
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showTopLevelChrome && useNavRail) {
                    NavigationRail {
                        TOP_TABS.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_FAMILY
                    ) {
                        composable(ROUTE_FAMILY) {
                            FamilyDirectoryScreen(
                                membersState = membersState,
                                filteredMembers = filteredMembers,
                                searchQuery = searchQuery,
                                selectedCategory = selectedCategory,
                                onSearchQueryChange = viewModel::updateSearchQuery,
                                onCategorySelect = viewModel::selectCategory,
                                onMemberClick = { memberId ->
                                    navController.navigate("detail/$memberId")
                                },
                                onAddMemberClick = {
                                    navController.navigate("add_edit")
                                },
                                onOpenMyProfileClick = {
                                    navController.navigate(ROUTE_MY_PROFILE) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }

                        composable(ROUTE_BIRTHDAYS) {
                            BirthdaysScreen(
                                membersState = membersState,
                                userProfileState = userProfileState,
                                onMemberClick = { memberId ->
                                    navController.navigate("detail/$memberId")
                                },
                                onToggleMemberReminder = viewModel::toggleMemberReminder,
                                onUpdateNotificationPrefs = viewModel::updateNotificationPreferences,
                                onShowMessage = viewModel::showMessage
                            )
                        }

                        composable(ROUTE_MY_PROFILE) {
                            MyProfileScreen(
                                userProfileState = userProfileState,
                                defaultDisplayName = defaultDisplayName,
                                userEmail = userEmail,
                                isSaving = isSaving,
                                onSaveProfile = { profile ->
                                    viewModel.saveUserProfile(profile)
                                },
                                onSignOutClick = {
                                    signOutUser(credentialManager, scope)
                                }
                            )
                        }

                        composable(
                            route = ROUTE_DETAIL,
                            arguments = listOf(navArgument("memberId") { type = NavType.StringType })
                        ) { entry ->
                            val memberId = entry.arguments?.getString("memberId").orEmpty()
                            val allMembers = (membersState as? UiState.Success)?.data ?: emptyList()
                            val member = allMembers.firstOrNull { it.id == memberId }

                            MemberDetailScreen(
                                member = member,
                                onBack = { navController.popBackStack() },
                                onEditClick = { target ->
                                    if (target.selfProfile) {
                                        navController.navigate(ROUTE_MY_PROFILE) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    } else {
                                        navController.navigate("add_edit?memberId=${target.id}")
                                    }
                                },
                                onToggleReminder = viewModel::toggleMemberReminder,
                                onDeleteClick = { id ->
                                    viewModel.deleteFamilyMember(id) {
                                        navController.popBackStack()
                                    }
                                }
                            )
                        }

                        composable(
                            route = ROUTE_ADD_EDIT,
                            arguments = listOf(
                                navArgument("memberId") {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null
                                }
                            )
                        ) { entry ->
                            val memberId = entry.arguments?.getString("memberId")
                            val allMembers = (membersState as? UiState.Success)?.data ?: emptyList()
                            val existingMember = if (memberId != null) {
                                allMembers.firstOrNull { it.id == memberId }
                            } else {
                                null
                            }

                            AddEditMemberScreen(
                                existingMember = existingMember,
                                isSaving = isSaving,
                                onBack = { navController.popBackStack() },
                                onSave = { member, existingId ->
                                    viewModel.saveFamilyMember(member, existingId) {
                                        navController.popBackStack()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
