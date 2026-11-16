package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.example.data.model.GameType
import com.example.data.repository.WePlayRepository
import com.example.ui.components.HostRoomBottomSheet
import com.example.ui.components.JoinRoomDialog
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.LobbyScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.RoomScreen
import com.example.ui.screens.SoloGameScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.theme.ArcadeBg
import com.example.ui.theme.ArcadeSurface
import com.example.ui.theme.WePlayClubTheme
import com.example.ui.viewmodel.UiState
import com.example.ui.viewmodel.WePlayViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

object Routes {
  const val LOBBY = "lobby"
  const val STUDIO = "studio"
  const val HISTORY = "history"
  const val PROFILE = "profile"
  const val SOLO_GAME = "solo_game/{gameType}?packId={packId}"
  const val ROOM = "room/{roomId}"

  fun soloGame(gameType: GameType, packId: String? = null): String {
    return if (packId.isNullOrBlank()) {
      "solo_game/${gameType.id}"
    } else {
      "solo_game/${gameType.id}?packId=$packId"
    }
  }

  fun room(roomId: String): String = "room/$roomId"
}

private data class NavTab(
  val route: String,
  val label: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
)

private val TopTabs = listOf(
  NavTab(Routes.LOBBY, "Lobby", Icons.Filled.Home, Icons.Outlined.Home),
  NavTab(Routes.STUDIO, "Studio", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
  NavTab(Routes.HISTORY, "History", Icons.Filled.History, Icons.Outlined.History),
  NavTab(Routes.PROFILE, "Profile", Icons.Filled.Person, Icons.Outlined.Person),
)

@Composable
fun WePlayApp(auth: FirebaseAuth = Firebase.auth) {
  val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
  val user = currentUser

  if (user == null) {
    WePlayClubTheme {
      AuthScreen(onAuthSuccess = {})
    }
  } else {
    androidx.compose.runtime.key(user.uid) {
      AuthenticatedWePlayShell(
        currentUserId = user.uid,
        defaultDisplayName = user.displayName,
      )
    }
  }
}

@Composable
private fun AuthenticatedWePlayShell(
  currentUserId: String,
  defaultDisplayName: String?,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  val viewModel: WePlayViewModel =
    viewModel(
      key = currentUserId,
      factory =
        viewModelFactory {
          initializer {
            val app = checkNotNull(this[APPLICATION_KEY])
            val databaseId = app.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(databaseId)
            WePlayViewModel(WePlayRepository(db), currentUserId, defaultDisplayName)
          }
        },
    )

  val profileState by viewModel.userProfile.collectAsStateWithLifecycle()
  val profile = (profileState as? UiState.Success)?.data
  val accentIndex = (profile?.accentIndex ?: 0L).toInt()

  val matchHistoryState by viewModel.matchHistory.collectAsStateWithLifecycle()
  val customPacksState by viewModel.customPacks.collectAsStateWithLifecycle()
  val publicRoomsState by viewModel.publicRooms.collectAsStateWithLifecycle()
  val joinedRoomsState by viewModel.joinedRooms.collectAsStateWithLifecycle()
  val activeRoom by viewModel.activeRoom.collectAsStateWithLifecycle()
  val activeMessages by viewModel.activeRoomMessages.collectAsStateWithLifecycle()
  val activeStrokes by viewModel.activeRoomStrokes.collectAsStateWithLifecycle()
  val bannerMessage by viewModel.bannerMessage.collectAsStateWithLifecycle()

  val snackbarHostState = remember { SnackbarHostState() }
  LaunchedEffect(bannerMessage) {
    val msg = bannerMessage
    if (msg != null) {
      snackbarHostState.showSnackbar(msg)
      viewModel.clearBanner()
    }
  }

  val navController = rememberNavController()
  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = backStackEntry?.destination?.route ?: Routes.LOBBY
  val showTopLevelNav = TopTabs.any { it.route == currentRoute }

  var hostSheetInitialGame by remember { mutableStateOf<GameType?>(null) }
  var showJoinDialog by remember { mutableStateOf(false) }

  WePlayClubTheme(accentIndex = accentIndex) {
    androidx.compose.foundation.layout.BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val useNavRail = maxWidth >= 700.dp

      Scaffold(
        containerColor = ArcadeBg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
          if (showTopLevelNav && !useNavRail) {
            NavigationBar(containerColor = ArcadeSurface) {
              TopTabs.forEach { tab ->
                val selected = currentRoute == tab.route
                NavigationBarItem(
                  selected = selected,
                  onClick = {
                    navController.navigate(tab.route) {
                      popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                      launchSingleTop = true
                      restoreState = true
                    }
                  },
                  icon = {
                    Icon(
                      imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                      contentDescription = tab.label,
                    )
                  },
                  label = { Text(tab.label) },
                  modifier = Modifier.testTag("nav_tab_${tab.route}"),
                )
              }
            }
          }
        },
      ) { innerPadding ->
        Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
          if (showTopLevelNav && useNavRail) {
            NavigationRail(containerColor = ArcadeSurface) {
              TopTabs.forEach { tab ->
                val selected = currentRoute == tab.route
                NavigationRailItem(
                  selected = selected,
                  onClick = {
                    navController.navigate(tab.route) {
                      popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                      launchSingleTop = true
                      restoreState = true
                    }
                  },
                  icon = {
                    Icon(
                      imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                      contentDescription = tab.label,
                    )
                  },
                  label = { Text(tab.label) },
                )
              }
            }
          }

          Box(modifier = Modifier.weight(1f).fillMaxSize()) {
            NavHost(
              navController = navController,
              startDestination = Routes.LOBBY,
            ) {
              composable(Routes.LOBBY) {
                LobbyScreen(
                  profile = profile,
                  publicRoomsState = publicRoomsState,
                  joinedRoomsState = joinedRoomsState,
                  onPlaySolo = { game -> navController.navigate(Routes.soloGame(game)) },
                  onOpenHostSheet = { game -> hostSheetInitialGame = game },
                  onOpenJoinDialog = { showJoinDialog = true },
                  onJoinOrEnterRoom = { roomCode ->
                    viewModel.joinRoom(roomCode) { code ->
                      navController.navigate(Routes.room(code))
                    }
                  },
                  modifier = Modifier.statusBarsPadding().padding(top = 4.dp),
                )
              }

              composable(Routes.STUDIO) {
                StudioScreen(
                  profile = profile,
                  customPacksState = customPacksState,
                  onSaveHouseRules = { newAccent, roundSec, maxPlayers ->
                    viewModel.updateProfile(
                      displayName = profile?.displayName ?: defaultDisplayName ?: "PartyPlayer",
                      bio = profile?.bio ?: "Ready for party games!",
                      avatarIndex = profile?.avatarIndex ?: 0L,
                      accentIndex = newAccent,
                      favoriteGame = GameType.fromId(profile?.favoriteGame),
                      defaultRoundSeconds = roundSec,
                      defaultMaxPlayers = maxPlayers,
                    )
                  },
                  onSaveCustomPack = { title, game, items ->
                    viewModel.saveCustomPack(title = title, gameType = game, items = items)
                  },
                  onDeleteCustomPack = { packId -> viewModel.deleteCustomPack(packId) },
                  onPlayWithPack = { game, packId ->
                    navController.navigate(Routes.soloGame(game, packId))
                  },
                  modifier = Modifier.statusBarsPadding().padding(top = 4.dp),
                )
              }

              composable(Routes.HISTORY) {
                HistoryScreen(
                  profile = profile,
                  matchesState = matchHistoryState,
                  modifier = Modifier.statusBarsPadding().padding(top = 4.dp),
                )
              }

              composable(Routes.PROFILE) {
                ProfileScreen(
                  profile = profile,
                  onSaveProfile = { name, bio, avatarIdx, favGame ->
                    viewModel.updateProfile(
                      displayName = name,
                      bio = bio,
                      avatarIndex = avatarIdx,
                      accentIndex = profile?.accentIndex ?: 0L,
                      favoriteGame = favGame,
                      defaultRoundSeconds = profile?.defaultRoundSeconds ?: 45L,
                      defaultMaxPlayers = profile?.defaultMaxPlayers ?: 6L,
                    )
                  },
                  onSignOut = {
                    signOutUser(credentialManager = credentialManager, scope = scope)
                  },
                  modifier = Modifier.statusBarsPadding().padding(top = 4.dp),
                )
              }

              composable(
                route = Routes.SOLO_GAME,
                arguments =
                  listOf(
                    navArgument("gameType") { type = NavType.StringType },
                    navArgument("packId") {
                      type = NavType.StringType
                      nullable = true
                      defaultValue = null
                    },
                  ),
              ) { entry ->
                val gameArg = entry.arguments?.getString("gameType")
                val packIdArg = entry.arguments?.getString("packId")
                val game = GameType.fromId(gameArg)
                val packs = (customPacksState as? UiState.Success)?.data.orEmpty()
                val pack = packs.firstOrNull { it.id == packIdArg }

                SoloGameScreen(
                  gameType = game,
                  customPack = pack,
                  roundSeconds = (profile?.defaultRoundSeconds ?: 45L).toInt(),
                  playerName = profile?.displayName ?: "You",
                  onRecordMatch = { gt, mode, title, score, coins, xp, won, players ->
                    viewModel.recordMatch(gt, mode, title, score, coins, xp, won, players)
                  },
                  onBack = { navController.popBackStack() },
                )
              }

              composable(
                route = Routes.ROOM,
                arguments = listOf(navArgument("roomId") { type = NavType.StringType }),
              ) { entry ->
                val roomId = entry.arguments?.getString("roomId") ?: ""
                LaunchedEffect(roomId) { viewModel.setActiveRoom(roomId) }
                val packs = (customPacksState as? UiState.Success)?.data.orEmpty()

                RoomScreen(
                  room = activeRoom,
                  currentUserId = currentUserId,
                  messages = activeMessages,
                  strokes = activeStrokes,
                  customPacks = packs,
                  onLeaveRoom = { r ->
                    viewModel.leaveRoom(r) { navController.popBackStack() }
                  },
                  onUpdateHostState = { rId, gt, st, round, prompt ->
                    viewModel.updateRoomHostState(rId, gt, st, round, prompt)
                  },
                  onSendMessage = { r, text, type -> viewModel.sendRoomMessage(r, text, type) },
                  onAddStroke = { r, hex, width, encoded -> viewModel.addRoomStroke(r, hex, width, encoded) },
                  onRecordMatch = { gt, mode, title, score, coins, xp, won, players ->
                    viewModel.recordMatch(gt, mode, title, score, coins, xp, won, players)
                  },
                  onNotify = { viewModel.showBanner(it) },
                  onBack = { navController.popBackStack() },
                )
              }
            }
          }
        }
      }
    }

    val hostGame = hostSheetInitialGame
    if (hostGame != null) {
      HostRoomBottomSheet(
        initialGameType = hostGame,
        defaultRoundSeconds = profile?.defaultRoundSeconds ?: 45L,
        defaultMaxPlayers = profile?.defaultMaxPlayers ?: 6L,
        onDismiss = { hostSheetInitialGame = null },
        onCreateRoom = { title, game, isPublic, roundSec, maxPlayers ->
          hostSheetInitialGame = null
          viewModel.createRoom(title, game, isPublic, roundSec, maxPlayers) { roomCode ->
            navController.navigate(Routes.room(roomCode))
          }
        },
      )
    }

    if (showJoinDialog) {
      JoinRoomDialog(
        onDismiss = { showJoinDialog = false },
        onJoin = { code ->
          showJoinDialog = false
          viewModel.joinRoom(code) { roomCode ->
            navController.navigate(Routes.room(roomCode))
          }
        },
      )
    }
  }
}
