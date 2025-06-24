package com.rawello.mapar.presentation.scenePage

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.view.MotionEvent
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.ar.core.Frame
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.rawello.mapar.R
import com.rawello.mapar.data.TokenManager
import com.rawello.mapar.data.database.NodeViewModel
import com.rawello.mapar.data.models.LoginRequest
import com.rawello.mapar.data.models.NodeDb
import com.rawello.mapar.data.models.QrBuildingInfo
import com.rawello.mapar.data.models.SignUpRequest
import com.rawello.mapar.data.network.ApiFactory
import com.rawello.mapar.presentation.Routes
import com.rawello.mapar.presentation.helpers.PathHelper
import com.rawello.mapar.presentation.login.LoginPage
import com.rawello.mapar.presentation.nodesList.NodesListContent
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.rememberARCameraNode
import io.github.sceneview.node.Node
import io.github.sceneview.rememberCollisionSystem
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.rememberView
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArPage(
    navController: NavController,
    buildingId: Int,
    buildingName: String,
    destinationNodeId: Int? = null,
    editMode: Boolean = false,
    arPageViewModel: ArPageViewModel = viewModel(),
    nodeViewModel: NodeViewModel = viewModel()
) {
    val state by arPageViewModel.state.collectAsState()
    val context = LocalContext.current
    val tokenManager = remember { TokenManager.getInstance(context) }

    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val cameraNode = rememberARCameraNode(engine)
    val childNodes = rememberNodes()
    val view = rememberView(engine)
    val collisionSystem = rememberCollisionSystem(view)

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val handler = remember {
        ArPageHandler(
            viewModel = arPageViewModel,
            nodeViewModel = nodeViewModel,
            context = context,
            engine = engine,
            modelLoader = modelLoader,
            childNodes = state.childNodes
        )
    }

    var lastArFrame by remember { mutableStateOf<Frame?>(null) }

    LaunchedEffect(buildingId, editMode) {
        try {
            Log.d("ArPage", "[UNIFIED] Начало инициализации для здания $buildingId, editMode=$editMode")
            arPageViewModel.handleIntent(ArPageIntent.SetLoading(true))
            arPageViewModel.handleIntent(ArPageIntent.SetBuildingId(buildingId))
            arPageViewModel.handleIntent(ArPageIntent.SetBuildingName(buildingName))

            arPageViewModel.fetchNodesForBuildingOnInit(buildingId)

            delay(1000)
            val nodes = arPageViewModel.state.value.nodeList
            Log.d("ArPage", "[UNIFIED] Загружено ${nodes.size} точек (state)")

            if (nodes.isNotEmpty()) {
                nodes.forEach { node ->
                    Log.d("ArPage", "[UNIFIED] Точка ${node.id}: type=${node.type}, title=${node.title}, translation=${node.translation.contentToString()}")
                }
                if (!editMode) {
                    destinationNodeId?.let { destId ->
                        val sourceNode = nodes.find { it.type == "view" }
                        val destNode = nodes.find { it.id == destId }
                        if (sourceNode != null && destNode != null) {
                            Log.d("ArPage", "[UNIFIED] Построение маршрута от ${sourceNode.id} до $destId")
                            arPageViewModel.handleIntent(ArPageIntent.SelectPoint(sourceNode.id.toString(), destId.toString()))
                            arPageViewModel.handleIntent(ArPageIntent.SetButtonText(sourceNode.title, destNode.title))
                            arPageViewModel.handleIntent(ArPageIntent.BuildPath)
                        } else {
                            Log.e("ArPage", "[UNIFIED] Не удалось найти начальную или конечную точку для автоматического построения маршрута")
                        }
                    }
                }
            } else {
                Log.e("ArPage", "[UNIFIED] Нет загруженных точек для здания $buildingId")
            }
        } catch (e: Exception) {
            Log.e("ArPage", "[UNIFIED] Ошибка загрузки точек", e)
            snackbarHostState.showSnackbar("Ошибка при загрузке точек: ${e.message}")
        } finally {
            arPageViewModel.handleIntent(ArPageIntent.SetLoading(false))
            handler.initializeNodes(state, isEditMode = editMode, isQrMode = state.isQrMode)
            if (arPageViewModel.state.value.nodeList.isEmpty()) {
                snackbarHostState.showSnackbar("Нет точек для отображения (возможно, нет локальных данных)")
            }
        }
    }

    var isAnonymous by remember { mutableStateOf(true) }
    var isBuildingOwner by remember { mutableStateOf(false) }
    var isLoggedIn by remember { mutableStateOf(false) }
    var canEdit by remember { mutableStateOf(false) }
    var canLogin by remember { mutableStateOf(true) }
    var canLogout by remember { mutableStateOf(false) }
    var currentUser by remember { mutableStateOf<String?>(null) }
    var showAccountInfo by remember { mutableStateOf(false) }
    var showNewBuildingDialog by remember { mutableStateOf(false) }
    var newBuildingName by remember { mutableStateOf("") }

    var showNewNodeDialog by remember { mutableStateOf(false) }
    var newNodeTitle by rememberSaveable { mutableStateOf("") }
    var newNodeType by rememberSaveable { mutableStateOf("view") }

    var isLoginDialogOpen by remember { mutableStateOf(false) }
    var isRegisterDialogOpen by remember { mutableStateOf(false) }
    var isChangeBuildingDialogOpen by remember { mutableStateOf(false) }

    var showLocationSelectionDialog by remember { mutableStateOf(false) }

    var showLoginDialog by remember { mutableStateOf(false) }
    var loginErrorMessage by remember { mutableStateOf<String?>(null) }

    val systemUiController = rememberSystemUiController()
    val useDarkIcons = MaterialTheme.colorScheme.surface.luminance() > 0.5f
    val color = MaterialTheme.colorScheme.background

    LaunchedEffect(state.isQrMode, buildingId, state.isEditMode) {
        currentUser = tokenManager.getCurrentUsername()
        isLoggedIn = currentUser != null
        isAnonymous = !isLoggedIn
        canLogin = !isLoggedIn
        canLogout = isLoggedIn
        Log.d("ArPage", "Проверка владельца: user=$currentUser, buildingId=$buildingId, isLoggedIn=$isLoggedIn, isEditMode=${state.isEditMode}")
        if (isLoggedIn) {
            scope.launch {
                try {
                    Log.d("ArPage", "Проверка владельца здания через TokenManager для пользователя $currentUser и buildingId $buildingId")
                    val owner = tokenManager.isBuildingOwner(buildingId.toLong())
                    isBuildingOwner = owner
                    canEdit = owner && !state.isEditMode
                    Log.d("ArPage", "Результат isOwner (TokenManager): $owner для пользователя $currentUser, canEdit=$canEdit")
                } catch (e: Exception) {
                    isBuildingOwner = false
                    canEdit = false
                    Log.e("ArPage", "Ошибка проверки владельца здания через TokenManager: ${e.message}")
                }
            }
        } else {
            isBuildingOwner = false
            canEdit = false
            Log.d("ArPage", "Пользователь не залогинен, кнопка редактирования скрыта")
        }
    }

    var showAlertDialog by remember { mutableStateOf(false) }
    var inputTitle by remember { mutableStateOf("") }

    var motionEvent: MotionEvent? = null
    var node: Node? = null

    var showQrDialog by remember { mutableStateOf(false) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var hasUnsavedChanges by remember { mutableStateOf(false) }
    var showExitEditModeDialog by remember { mutableStateOf(false) }

    var selectedNodeId by remember { mutableStateOf<Int?>(null) }
    var selectedEdge by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var showLinkDialog by remember { mutableStateOf(false) }
    var linkTargetId by remember { mutableStateOf<Int?>(null) }

    fun tryExitEditMode() {
        if (hasUnsavedChanges) {
            showExitEditModeDialog = true
        } else {
            arPageViewModel.handleIntent(ArPageIntent.ToggleEditMode(false))
        }
    }

    var editNodeTitleDialogId by remember { mutableStateOf<Int?>(null) }

    var readyToPlaceNode by remember { mutableStateOf(false) }
    var nodeToPlaceId by remember { mutableStateOf<Int?>(null) }
    var pendingNodeType by remember { mutableStateOf<String?>(null) }
    var pendingNodeTitle by remember { mutableStateOf<String?>(null) }

    var linkMode by remember { mutableStateOf(false) }
    var linkSourceId by remember { mutableStateOf<Int?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            state.arSession?.pause()
            arPageViewModel.resetState()
        }
    }

    SideEffect {
        systemUiController.setStatusBarColor(
            color = color,
            darkIcons = useDarkIcons
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .imePadding(),
        topBar = {
            if (buildingName.isNotEmpty()) {
                TopAppBar(
                    title = {
                        val displayName = state.buildingName.replace("_", " ").trim()
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { showLocationSelectionDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Установить местоположение"
                            )
                        }
                    }
                )
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                if (state.isQrMode) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ElevatedButton(
                                onClick = { navController.navigate(Routes.Second.string) },
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .height(56.dp),
                                colors = ButtonDefaults.elevatedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                elevation = ButtonDefaults.elevatedButtonElevation(
                                    defaultElevation = 6.dp,
                                    pressedElevation = 8.dp
                                )
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Home,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = "Выбор здания",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                            ElevatedButton(
                                onClick = { navController.navigate(Routes.QrAlpha.string) },
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .height(56.dp),
                                colors = ButtonDefaults.elevatedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                elevation = ButtonDefaults.elevatedButtonElevation(
                                    defaultElevation = 6.dp,
                                    pressedElevation = 8.dp
                                )
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = "Сканировать QR-код",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                            Spacer(Modifier.height(24.dp))
                            if (isLoggedIn) {
                                ElevatedButton(
                                    onClick = { showAccountInfo = true },
                                    modifier = Modifier
                                        .fillMaxWidth(0.8f)
                                        .height(56.dp),
                                    colors = ButtonDefaults.elevatedButtonColors(),
                                    elevation = ButtonDefaults.elevatedButtonElevation()
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Info,
                                            contentDescription = "Информация об аккаунте",
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(
                                            "Аккаунт",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                }
                            }
                            if (canEdit) {
                                ElevatedButton(
                                    onClick = {
                                        arPageViewModel.handleIntent(
                                            ArPageIntent.ToggleEditMode(
                                                true
                                            )
                                        )
                                        arPageViewModel.handleIntent(ArPageIntent.ToggleQrMode(false))
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth(0.8f)
                                        .height(76.dp),
                                    colors = ButtonDefaults.elevatedButtonColors(),
                                    elevation = ButtonDefaults.elevatedButtonElevation()
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Home,
                                            contentDescription = "Войти в режим редактирования",
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(
                                            "Войти в режим редактирования",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                }
                            }
                            if (state.isEditMode && state.isQrMode) {
                                ElevatedButton(
                                    onClick = {
                                        arPageViewModel.handleIntent(
                                            ArPageIntent.ToggleEditMode(
                                                false
                                            )
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth(0.8f)
                                        .height(64.dp),
                                    colors = ButtonDefaults.elevatedButtonColors(),
                                    elevation = ButtonDefaults.elevatedButtonElevation()
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.LocationOn,
                                            contentDescription = "Выйти в режим просмотра",
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(
                                            "Выйти в режим просмотра",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                }
                            }
                            if (canLogin) {
                                ElevatedButton(
                                    onClick = { showLoginDialog = true },
                                    modifier = Modifier
                                        .fillMaxWidth(0.8f)
                                        .height(56.dp),
                                    colors = ButtonDefaults.elevatedButtonColors(),
                                    elevation = ButtonDefaults.elevatedButtonElevation()
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Info,
                                            contentDescription = "Войти в аккаунт",
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(
                                            "Войти в аккаунт",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                }
                            }
                            if (canLogout) {
                                ElevatedButton(
                                    onClick = {
                                        scope.launch {
                                            tokenManager.logout()
                                            isAnonymous = true
                                            isBuildingOwner = false
                                            currentUser = null
                                            isLoggedIn = false
                                            canLogin = true
                                            canLogout = false
                                            canEdit = false
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth(0.8f)
                                        .height(56.dp),
                                    colors = ButtonDefaults.elevatedButtonColors(),
                                    elevation = ButtonDefaults.elevatedButtonElevation()
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Share,
                                            contentDescription = "Выйти из аккаунта",
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(
                                            "Выйти из аккаунта",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                }
                            }
                        }
                        if (showAccountInfo && currentUser != null) {
                            AlertDialog(
                                onDismissRequest = { showAccountInfo = false },
                                title = { Text("Аккаунт") },
                                text = { Text("Вы вошли как $currentUser") },
                                confirmButton = {
                                    Button(onClick = {
                                        scope.launch {
                                            tokenManager.logout()
                                            isAnonymous = true
                                            isBuildingOwner = false
                                            currentUser = null
                                            isLoggedIn = false
                                            canLogin = true
                                            canLogout = false
                                            canEdit = false
                                            showAccountInfo = false
                                        }
                                    }) { Text("Выйти") }
                                },
                                dismissButton = {
                                    TextButton(onClick = {
                                        showAccountInfo = false
                                    }) { Text("Назад") }
                                }
                            )
                        }
                        if (showLoginDialog) {
                            val loginScope = rememberCoroutineScope()
                            LoginPage(
                                onDismiss = { showLoginDialog = false },
                                onLogin = { username, password ->
                                    val trimmedUsername = username.trim()
                                    val trimmedPassword = password.trim()
                                    loginScope.launch {
                                        try {
                                            val response = ApiFactory.apiService.login(
                                                LoginRequest(
                                                    trimmedUsername,
                                                    trimmedPassword
                                                )
                                            )
                                            tokenManager.login(trimmedUsername, response.token)
                                            isAnonymous = false
                                            currentUser = trimmedUsername
                                            isLoggedIn = true
                                            canLogin = false
                                            canLogout = true
                                            isBuildingOwner =
                                                ApiFactory.apiService.isBuildingOwner(buildingId.toLong())
                                            canEdit = isBuildingOwner && !state.isEditMode
                                            showLoginDialog = false
                                            loginErrorMessage = null
                                        } catch (e: Exception) {
                                            val isForbidden = e.message?.contains("403") == true
                                            loginErrorMessage =
                                                if (isForbidden) "Неверный логин или пароль" else (e.message
                                                    ?: "Неизвестная ошибка")
                                        }
                                    }
                                },
                                onRegister = { username, password ->
                                    val trimmedUsername = username.trim()
                                    val trimmedPassword = password.trim()
                                    loginScope.launch {
                                        try {
                                            ApiFactory.apiService.register(
                                                SignUpRequest(
                                                    trimmedUsername,
                                                    trimmedPassword
                                                )
                                            )
                                            val response = ApiFactory.apiService.login(
                                                LoginRequest(
                                                    trimmedUsername,
                                                    trimmedPassword
                                                )
                                            )
                                            tokenManager.login(trimmedUsername, response.token)
                                            isAnonymous = false
                                            currentUser = trimmedUsername
                                            isLoggedIn = true
                                            canLogin = false
                                            canLogout = true
                                            isBuildingOwner =
                                                ApiFactory.apiService.isBuildingOwner(buildingId.toLong())
                                            canEdit = isBuildingOwner && !state.isEditMode
                                            showLoginDialog = false
                                            loginErrorMessage = null
                                        } catch (e: Exception) {
                                            val isForbidden = e.message?.contains("403") == true
                                            loginErrorMessage =
                                                if (isForbidden) "Неверный логин или пароль" else (e.message
                                                    ?: "Неизвестная ошибка")
                                        }
                                    }
                                },
                                snackbarHostState = remember { SnackbarHostState() },
                                scope = loginScope,
                                context = context,
                                errorMessage = loginErrorMessage
                            )
                        }
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            val barWidth = 320.dp
                            val bottomBarHeight = 64.dp
                            val indicatorColor =
                                if (state.isQrMode) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                            val activeColor = indicatorColor.copy(alpha = 0.18f)
                            val inactiveColor =
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.10f)
                            Card(
                                modifier = Modifier
                                    .width(barWidth)
                                    .height(bottomBarHeight)
                                    .align(Alignment.BottomCenter)
                                    .offset(y = (-32).dp)
                                    .zIndex(1f),
                                shape = RoundedCornerShape(32.dp),
                                elevation = CardDefaults.cardElevation(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                        alpha = 0.95f
                                    )
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(
                                            start = 20.dp,
                                            end = 20.dp,
                                            top = 8.dp,
                                            bottom = 8.dp
                                        ),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .padding(end = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val isActive = !state.isQrMode
                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .background(
                                                    color = if (isActive) activeColor else inactiveColor,
                                                    shape = RoundedCornerShape(24.dp)
                                                )
                                        )
                                        IconButton(
                                            onClick = {
                                                arPageViewModel.handleIntent(
                                                    ArPageIntent.ToggleQrMode(false)
                                                )
                                            },
                                            enabled = state.isQrMode,
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.LocationOn,
                                                contentDescription = "AR режим",
                                                tint = if (isActive) indicatorColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .padding(start = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val isActive = state.isQrMode
                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .background(
                                                    color = if (isActive) activeColor else inactiveColor,
                                                    shape = RoundedCornerShape(24.dp)
                                                )
                                        )
                                        IconButton(
                                            onClick = {
                                                arPageViewModel.handleIntent(
                                                    ArPageIntent.ToggleQrMode(true)
                                                )
                                            },
                                            enabled = !state.isQrMode,
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Info,
                                                contentDescription = "QR режим",
                                                tint = if (isActive) indicatorColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        TopAppBar(
                            modifier = Modifier.align(Alignment.TopCenter),
                            title = {
                                val displayName = state.buildingName.replace("_", " ").trim()
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = displayName,
                                        style = MaterialTheme.typography.titleLarge,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                            },
                            actions = {
                                IconButton(onClick = { showLocationSelectionDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = "Установить местоположение"
                                    )
                                }
                            }
                        )

                        if (showLocationSelectionDialog) {
                            AlertDialog(
                                onDismissRequest = { showLocationSelectionDialog = false },
                                title = { Text("Выберите ваше местоположение") },
                                text = {
                                    LazyColumn {
                                        items(state.nodeList.filter { it.type == "view" }) { node ->
                                            Text(
                                                text = node.title,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        arPageViewModel.handleIntent(
                                                            ArPageIntent.SetUserLocation(
                                                                node.id,
                                                                navController
                                                            )
                                                        )
                                                        showLocationSelectionDialog = false
                                                    }
                                                    .padding(16.dp)
                                            )
                                        }
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = { showLocationSelectionDialog = false }) {
                                        Text("Отмена")
                                    }
                                }
                            )
                        }

                        val arNodes = state.childNodes.mapNotNull { node ->
                            val id = node.name?.toIntOrNull()
                            val title = node.name ?: "Точка"
                            val type = if (node is io.github.sceneview.node.ModelNode) "linked" else "view"
                            if (id != null) NodeDb(
                                id = id,
                                buildingId = state.buildingId.toLong() ?: 0L,
                                nameNode = node.name ?: "node_$id",
                                translation = node.position.toFloatArray(),
                                quaternion = node.quaternion.toFloatArray(),
                                type = type,
                                title = title,
                                neighbors = ""
                            ) else null
                        }
                        var localNodeList by remember {
                            mutableStateOf(
                                if (state.nodeList.isNotEmpty()) state.nodeList else arNodes
                            )
                        }
                        var localEdges by remember(localNodeList) {
                            mutableStateOf(
                                buildList {
                                    val added = mutableSetOf<Pair<Int, Int>>()
                                    localNodeList.forEach { node ->
                                        node.neighbors.split(",").mapNotNull { it.toIntOrNull() }.forEach { neighborId ->
                                            val pair = if (node.id < neighborId) node.id to neighborId else neighborId to node.id
                                            if (pair.first != pair.second && pair !in added) {
                                                add(pair)
                                                added.add(pair)
                                            }
                                        }
                                    }
                                }
                            )
                        }
                        var localSelectedNodeId by remember { mutableStateOf<Int?>(null) }
                        var localSelectedEdge by remember { mutableStateOf<Pair<Int, Int>?>(null) }
                        var localHasUnsavedChanges by remember { mutableStateOf(false) }
                        var showLocalLinkDialog by remember { mutableStateOf(false) }
                        var localLinkTargetId by remember { mutableStateOf<Int?>(null) }
                        var showNodeMenu by remember { mutableStateOf(false) }
                        var showEdgeMenu by remember { mutableStateOf(false) }
                        var nodeTypeDropdownExpanded by remember { mutableStateOf(false) }
                        val nodeTypes = listOf("view" to "Точка с названием", "linked" to "Связная точка")

                        IconButton(onClick = {
                            if (newNodeType.isBlank()) newNodeType = "view"
                            newNodeTitle = ""
                            showNewNodeDialog = true
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Добавить точку")
                        }

                        if (showNewNodeDialog) {
                            AlertDialog(
                                onDismissRequest = {
                                    showNewNodeDialog = false
                                    newNodeType = "view"
                                    newNodeTitle = ""
                                },
                                title = { Text("Создать новую точку") },
                                text = {
                                    Column {
                                        OutlinedTextField(
                                            value = newNodeTitle,
                                            onValueChange = { newNodeTitle = it },
                                            label = { Text("Название точки") },
                                            singleLine = true
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        ExposedDropdownMenuBox(
                                            expanded = nodeTypeDropdownExpanded,
                                            onExpandedChange = {
                                                nodeTypeDropdownExpanded = !nodeTypeDropdownExpanded
                                            }
                                        ) {
                                            OutlinedTextField(
                                                value = nodeTypes.find { it.first == newNodeType }?.second
                                                    ?: "Точка с названием",
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text("Тип точки") },
                                                trailingIcon = {
                                                    ExposedDropdownMenuDefaults.TrailingIcon(
                                                        expanded = nodeTypeDropdownExpanded
                                                    )
                                                },
                                                modifier = Modifier.menuAnchor()
                                            )
                                            ExposedDropdownMenu(
                                                expanded = nodeTypeDropdownExpanded,
                                                onDismissRequest = {
                                                    nodeTypeDropdownExpanded = false
                                                }
                                            ) {
                                                nodeTypes.forEach { (type, label) ->
                                                    DropdownMenuItem(
                                                        text = { Text(label) },
                                                        onClick = {
                                                            newNodeType = type
                                                            nodeTypeDropdownExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            "После выбора типа и названия нажмите на сцену для размещения точки",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                },
                                confirmButton = {
                                    Button(onClick = {
                                        val newId = (localNodeList.maxOfOrNull { it.id } ?: 0) + 1
                                        nodeToPlaceId = newId
                                        pendingNodeType = newNodeType
                                        pendingNodeTitle = newNodeTitle
                                        readyToPlaceNode = true
                                        showNewNodeDialog = false
                                        newNodeTitle = ""
                                        newNodeType = nodeTypes.first().first
                                        nodeTypeDropdownExpanded = false
                                    }, enabled = newNodeTitle.isNotBlank()) {
                                        Text("Выбрать место на сцене")
                                    }
                                },
                                dismissButton = {
                                    Button(onClick = {
                                        showNewNodeDialog = false
                                        newNodeType = "view"
                                        newNodeTitle = ""
                                    }) { Text("Отмена") }
                                }
                            )
                        }

                        if (editNodeTitleDialogId != null) {
                            val nodeSource = if (localNodeList.isNotEmpty()) localNodeList else state.nodeList
                            val node = nodeSource.find { it.id == editNodeTitleDialogId }
                            var editTitle by remember(editNodeTitleDialogId, node?.title) { mutableStateOf(node?.title ?: "") }
                            AlertDialog(
                                onDismissRequest = { editNodeTitleDialogId = null },
                                title = { Text("Редактировать: ${node?.title ?: node?.id?.toString() ?: ""}") },
                                text = {
                                    OutlinedTextField(
                                        value = editTitle,
                                        onValueChange = { editTitle = it },
                                        label = { Text("Новое название") },
                                        singleLine = true
                                    )
                                },
                                confirmButton = {
                                    Button(onClick = {
                                        if (node != null && editTitle.isNotBlank()) {
                                            localNodeList = nodeSource.map {
                                                if (it.id == node.id) it.copy(title = editTitle) else it
                                            }
                                            editNodeTitleDialogId = null
                                            localHasUnsavedChanges = true
                                        }
                                    }) { Text("Сохранить") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { editNodeTitleDialogId = null }) { Text("Отмена") }
                                }
                            )
                        }

                        val onPlaceNode: (FloatArray, FloatArray) -> Unit = { position, quaternion ->
                            if (readyToPlaceNode && nodeToPlaceId != null && pendingNodeType != null && pendingNodeTitle != null) {
                                val newNode = NodeDb(
                                    id = nodeToPlaceId!!,
                                    buildingId = buildingId.toLong(),
                                    nameNode = "node_${nodeToPlaceId!!}",
                                    translation = position,
                                    quaternion = quaternion,
                                    type = pendingNodeType!!,
                                    title = pendingNodeTitle!!,
                                    neighbors = ""
                                )
                                arPageViewModel.handleIntent(ArPageIntent.SetNodeList(state.nodeList + newNode))
                                handler.initializeNodes(state.copy(nodeList = state.nodeList + newNode), isEditMode = state.isEditMode, isQrMode = state.isQrMode)
                                readyToPlaceNode = false
                                nodeToPlaceId = null
                                pendingNodeType = null
                                pendingNodeTitle = null
                                localHasUnsavedChanges = true
                            }
                        }

                        val onMoveNode: (Int, FloatArray, FloatArray) -> Unit =
                            { nodeId, position, quaternion ->
                                localNodeList = localNodeList.map {
                                    if (it.id == nodeId) it.copy(
                                        translation = position,
                                        quaternion = quaternion
                                    ) else it
                                }
                            }

                        ARScene(
                            modifier = Modifier.fillMaxSize(),
                            childNodes = state.childNodes,
                            engine = engine,
                            view = view,
                            modelLoader = modelLoader,
                            collisionSystem = collisionSystem,
                            cameraNode = cameraNode,
                            onGestureListener = rememberOnGestureListener(
                                onSingleTapConfirmed = { event, tappedNode ->
                                    if (readyToPlaceNode && nodeToPlaceId != null) {
                                        val hitResult = lastArFrame?.hitTest(event.x, event.y)
                                            ?.firstOrNull { it.trackable != null }
                                        if (hitResult != null) {
                                            val pose = hitResult.hitPose
                                            val position =
                                                floatArrayOf(pose.tx(), pose.ty(), pose.tz())
                                            val quaternion = floatArrayOf(
                                                pose.qx(),
                                                pose.qy(),
                                                pose.qz(),
                                                pose.qw()
                                            )
                                            onPlaceNode(position, quaternion)
                                        } else {

                                        }
                                    } else if (state.isEditMode) {
                                        val tappedId = tappedNode?.name?.toIntOrNull()
                                        if (tappedId != null) {
                                            selectedNodeId = tappedId
                                            selectedEdge = null
                                        } else if (tappedNode?.name?.contains("_") == true) {
                                            val parts = tappedNode.name!!.split("_")
                                            val id1 = parts.getOrNull(0)?.toIntOrNull()
                                            val id2 = parts.getOrNull(1)?.toIntOrNull()
                                            if (id1 != null && id2 != null) {
                                                selectedEdge = id1 to id2
                                                selectedNodeId = null
                                            }
                                        } else {
                                            selectedNodeId = null
                                            selectedEdge = null
                                        }
                                    }
                                },
                                onLongPress = { _, tappedNode ->
                                    if (state.isEditMode) {
                                        val tappedId = tappedNode?.name?.toIntOrNull()
                                        if (tappedId != null) {
                                            editNodeTitleDialogId = tappedId
                                        }
                                    }
                                }
                            ),
                            onSessionUpdated = { session, updatedFrame ->
                                lastArFrame = updatedFrame
                                scope.launch {
                                    state.context = context
                                    handler.handleSessionUpdate(session, updatedFrame, state)
                                }
                            },
                            onViewCreated = {
                                handler.onViewCreated(this, state)
                            },
                            onViewUpdated = {
                                handler.onViewUpdated(this)
                            }
                        )

                        if (state.showBottomSheet) {
                            ModalBottomSheet(
                                onDismissRequest = {
                                    arPageViewModel.handleIntent(
                                        ArPageIntent.SetShowBottomSheet(
                                            false
                                        )
                                    )
                                },
                                sheetState = rememberModalBottomSheetState(),
                            ) {
                                NodesListContent(
                                    nodes = state.nodeList,
                                    onPointsSelected = { point1, point2 ->
                                        scope.launch {
                                            arPageViewModel.handleIntent(
                                                ArPageIntent.SelectPoint(
                                                    point1,
                                                    point2
                                                )
                                            )
                                            arPageViewModel.handleIntent(
                                                ArPageIntent.SetButtonText(
                                                    point1,
                                                    point2
                                                )
                                            )
                                            arPageViewModel.handleIntent(
                                                ArPageIntent.SetShowBottomSheet(
                                                    false
                                                )
                                            )
                                        }
                                    },
                                )
                            }
                        }

                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            val barWidth = 320.dp
                            val bottomBarHeight = 64.dp
                            val dragHandlerHeight = 16.dp
                            val minSheetHeight =
                                dragHandlerHeight + 32.dp
                            val maxSheetHeight = 160.dp
                            val sheetHeight = remember { mutableStateOf(minSheetHeight) }
                            val isCollapsed = sheetHeight.value <= minSheetHeight + 8.dp
                            val animatedSheetHeight by animateDpAsState(
                                sheetHeight.value,
                                tween(220)
                            )
                            val sheetShape = RoundedCornerShape(
                                topStart = 32.dp,
                                topEnd = 32.dp,
                                bottomStart = 32.dp,
                                bottomEnd = 32.dp
                            )
                            val indicatorColor =
                                if (state.isQrMode) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                            val indicatorWidth = barWidth / 2
                            val indicatorOffset by animateDpAsState(
                                targetValue = if (state.isQrMode) indicatorWidth else 0.dp,
                                label = "indicatorOffset"
                            )

                            val totalHeight = animatedSheetHeight + bottomBarHeight - 16.dp

                            Card(
                                modifier = Modifier
                                    .width(barWidth)
                                    .height(totalHeight)
                                    .align(Alignment.BottomCenter)
                                    .offset(y = (-32).dp)
                                    .zIndex(1f),
                                shape = sheetShape,
                                elevation = CardDefaults.cardElevation(if (!isCollapsed) 24.dp else 8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                        alpha = 0.98f
                                    )
                                )
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = bottomBarHeight),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 8.dp)
                                                .width(48.dp)
                                                .height(dragHandlerHeight)
                                                .background(
                                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                        alpha = 0.22f
                                                    ), RoundedCornerShape(8.dp)
                                                )
                                                .clickable {
                                                    sheetHeight.value =
                                                        if (isCollapsed) maxSheetHeight else minSheetHeight
                                                }
                                        )
                                        AnimatedVisibility(visible = !isCollapsed) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(
                                                        top = 8.dp,
                                                        start = 24.dp,
                                                        end = 24.dp,
                                                        bottom = 8.dp
                                                    ),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(16.dp)
                                            ) {
                                                Button(
                                                    onClick = {
                                                        arPageViewModel.handleIntent(
                                                            ArPageIntent.SelectPoint(
                                                                state.buttonText1,
                                                                state.buttonText2
                                                            )
                                                        )
                                                        arPageViewModel.handleIntent(ArPageIntent.BuildPath)
                                                        arPageViewModel.handleIntent(
                                                            ArPageIntent.SetShowBottomSheet(
                                                                true
                                                            )
                                                        )
                                                        arPageViewModel.handleIntent(
                                                            ArPageIntent.SetShowBottomSheet(
                                                                false
                                                            )
                                                        )
                                                    },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(24.dp)
                                                ) { Text("Построить маршрут") }
                                                Button(
                                                    onClick = {
                                                        scope.launch {
                                                            arPageViewModel.handleIntent(
                                                                ArPageIntent.SelectPoint(null, null)
                                                            )
                                                            arPageViewModel.handleIntent(
                                                                ArPageIntent.SetShowBottomSheet(true)
                                                            )
                                                        }
                                                    },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(24.dp)
                                                ) {
                                                    val label =
                                                        if (state.buttonText1.isNotEmpty() && state.buttonText2.isNotEmpty()) {
                                                            val fromNode =
                                                                state.nodeList.find { it.id.toString() == state.buttonText1 }
                                                            val toNode =
                                                                state.nodeList.find { it.id.toString() == state.buttonText2 }
                                                            val fromTitle =
                                                                fromNode?.title ?: state.buttonText1
                                                            val toTitle =
                                                                toNode?.title ?: state.buttonText2
                                                            val full = "$fromTitle -> $toTitle"
                                                            val maxLen = 32
                                                            if (full.length > maxLen) full.take(
                                                                maxLen - 3
                                                            ) + "..." else full
                                                        } else {
                                                            "Выбор маршрута"
                                                        }
                                                    Text(label, maxLines = 1)
                                                }
                                            }
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(bottomBarHeight)
                                            .align(Alignment.BottomCenter)
                                            .padding(bottom = 8.dp),
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(
                                                    start = 20.dp,
                                                    end = 20.dp,
                                                    top = 8.dp,
                                                    bottom = 8.dp
                                                ),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceEvenly
                                        ) {
                                            val activeColor = indicatorColor.copy(alpha = 0.18f)
                                            val inactiveColor =
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                    alpha = 0.10f
                                                )
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight()
                                                    .padding(end = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                val isActive = !state.isQrMode
                                                Box(
                                                    modifier = Modifier
                                                        .matchParentSize()
                                                        .background(
                                                            color = if (isActive) activeColor else inactiveColor,
                                                            shape = RoundedCornerShape(24.dp)
                                                        )
                                                )
                                                IconButton(
                                                    onClick = {
                                                        arPageViewModel.handleIntent(
                                                            ArPageIntent.ToggleQrMode(false)
                                                        )
                                                    },
                                                    enabled = state.isQrMode,
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.LocationOn,
                                                        contentDescription = "AR режим",
                                                        tint = if (isActive) indicatorColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(28.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight()
                                                    .padding(start = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                val isActive = state.isQrMode
                                                Box(
                                                    modifier = Modifier
                                                        .matchParentSize()
                                                        .background(
                                                            color = if (isActive) activeColor else inactiveColor,
                                                            shape = RoundedCornerShape(24.dp)
                                                        )
                                                )
                                                IconButton(
                                                    onClick = {
                                                        arPageViewModel.handleIntent(
                                                            ArPageIntent.ToggleQrMode(true)
                                                        )
                                                    },
                                                    enabled = !state.isQrMode,
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Info,
                                                        contentDescription = "QR режим",
                                                        tint = if (isActive) indicatorColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(28.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                AnimatedVisibility(visible = state.isEditMode && isBuildingOwner && !isAnonymous) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 24.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        Button(onClick = { showNewNodeDialog = true }) {
                                            Icon(
                                                Icons.Default.Add,
                                                contentDescription = "Добавить точку"
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text("Добавить точку")
                                        }
                                        Button(onClick = { showNewBuildingDialog = true }) {
                                            Icon(
                                                Icons.Default.Add,
                                                contentDescription = "Добавить здание"
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text("Добавить здание")
                                        }
                                        Button(onClick = { arPageViewModel.saveBuilding() }) {
                                            Icon(
                                                Icons.Default.Home,
                                                contentDescription = "Сохранить здание"
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text("Сохранить здание")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showNewBuildingDialog) {
            var newBuildingAddress by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showNewBuildingDialog = false },
                title = { Text("Введите данные нового здания") },
                text = {
                    Column {
                        TextField(
                            value = newBuildingName,
                            onValueChange = { newBuildingName = it },
                            label = { Text("Название здания") }
                        )
                        Spacer(Modifier.height(12.dp))
                        TextField(
                            value = newBuildingAddress,
                            onValueChange = { newBuildingAddress = it },
                            label = { Text("Адрес здания") }
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            arPageViewModel.createBuilding(newBuildingName, newBuildingAddress)
                            showNewBuildingDialog = false
                        },
                        enabled = newBuildingName.isNotBlank() && newBuildingAddress.isNotBlank()
                    ) {
                        Text("Создать")
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { showNewBuildingDialog = false }
                    ) {
                        Text("Отмена")
                    }
                }
            )
        }

        if (isChangeBuildingDialogOpen) {
            ChangeBuildingDialog(
                onDismiss = { isChangeBuildingDialogOpen = false },
                onChangeBuilding = { newBuildingId, newBuildingName ->
                    arPageViewModel.resetState()
                    navController.navigate("arPage/$newBuildingId/$newBuildingName?editMode=true")
                }
            )
        }

        LaunchedEffect(Unit) {
            while (true) {
                delay(250)
                if (state.path.isNotEmpty()) {
                    val currentNode = state.path.first()
                    val nextNode = state.path.getOrNull(1)
                    val currentPosition = handler.getCurrentCameraPosition(state.frame!!)
                    val distanceToTurn = PathHelper().distance(
                        currentPosition,
                        nextNode?.position ?: currentNode.position
                    )
                    val totalDistance = PathHelper().sumOfDistances(state.path)
                    val turnDirection = calculateTurnDirection(
                        currentPosition,
                        nextNode?.position ?: currentNode.position
                    )

                    arPageViewModel.handleIntent(ArPageIntent.SetDistanceToTurn(distanceToTurn))
                    arPageViewModel.handleIntent(ArPageIntent.SetTurnDirection(turnDirection))

                    if (!state.showRouteInfoBar) {
                        arPageViewModel.handleIntent(ArPageIntent.SetShowRouteInfoBar(true))
                    }
                }
            }
        }

        val showRouteInfoBar = state.showRouteInfoBar && !state.isQrMode && (
                (state.path.isNotEmpty() && state.turnDirection != "Вы дошли до места назначения") ||
                        (state.turnDirection == "Вы дошли до места назначения" && state.wasRouteActive)
                )
        if (showRouteInfoBar) {
            val currentNode = state.path.firstOrNull()
            val nextNode = state.path.getOrNull(1)
            val currentPosition = handler.getCurrentCameraPosition(state.frame!!)
            val distanceToTurn = if (currentNode != null) PathHelper().distance(
                currentPosition,
                nextNode?.position ?: currentNode.position
            ) else 0f
            val turnDirection = state.turnDirection

            RouteInfoBar(
                distanceToTurn = distanceToTurn,
                turnDirection = turnDirection,
                modifier = Modifier.statusBarsPadding()
            )
        }

        if (state.isEditMode && !state.isQrMode) {
            var localNodeList by remember {
                mutableStateOf(
                    state.nodeList
                )
            }
            var localEdges by remember(localNodeList) {
                mutableStateOf(
                    buildList {
                        val added = mutableSetOf<Pair<Int, Int>>()
                        localNodeList.forEach { node ->
                            node.neighbors.split(",").mapNotNull { it.toIntOrNull() }.forEach { neighborId ->
                                val pair = if (node.id < neighborId) node.id to neighborId else neighborId to node.id
                                if (pair.first != pair.second && pair !in added) {
                                    add(pair)
                                    added.add(pair)
                                }
                            }
                        }
                    }
                )
            }
            var localSelectedNodeId by remember { mutableStateOf<Int?>(null) }
            var localSelectedEdge by remember { mutableStateOf<Pair<Int, Int>?>(null) }
            var localHasUnsavedChanges by remember { mutableStateOf(false) }
            var showLocalLinkDialog by remember { mutableStateOf(false) }
            var localLinkTargetId by remember { mutableStateOf<Int?>(null) }
            var showNodeMenu by remember { mutableStateOf(false) }
            var showEdgeMenu by remember { mutableStateOf(false) }

            LaunchedEffect(state.nodeList) {
                if (!localHasUnsavedChanges) {
                    localNodeList = state.nodeList
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                AnimatedVisibility(
                    visible = true,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 140.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .widthIn(min = 180.dp, max = 320.dp)
                            .padding(8.dp),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { showNewNodeDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = "Добавить точку")
                            }
                            IconButton(onClick = {
                                linkMode = !linkMode
                                linkSourceId = null
                                localSelectedNodeId = null
                            }) {
                                Icon(Icons.Default.Share, contentDescription = if (linkMode) "Выйти из режима связывания" else "Связать точки")
                            }
                            IconButton(onClick = {
                                if (localSelectedNodeId != null) editNodeTitleDialogId =
                                    localSelectedNodeId
                            }, enabled = localSelectedNodeId != null) {
                                Icon(Icons.Default.Edit, contentDescription = "Редактировать точку")
                            }
                            IconButton(onClick = { showQrDialog = true }) {
                                Icon(Icons.Default.Place, contentDescription = "QR-код точки")
                            }
                            IconButton(onClick = {
                                if (localSelectedNodeId != null) {
                                    val id = localSelectedNodeId!!
                                    localNodeList = localNodeList.filter { it.id != id }
                                    localNodeList = localNodeList.map { n ->
                                        n.copy(
                                            neighbors = n.neighbors.split(",")
                                                .filter { it.toIntOrNull() != id }.joinToString(",")
                                        )
                                    }
                                    localSelectedNodeId = null
                                    localSelectedEdge = null
                                    localHasUnsavedChanges = true
                                }
                                if (localSelectedEdge != null) {
                                    val (id1, id2) = localSelectedEdge!!
                                    localNodeList = localNodeList.map { n ->
                                        when (n.id) {
                                            id1 -> n.copy(
                                                neighbors = n.neighbors.split(",")
                                                    .filter { it.toIntOrNull() != id2 }
                                                    .joinToString(",")
                                            )

                                            id2 -> n.copy(
                                                neighbors = n.neighbors.split(",")
                                                    .filter { it.toIntOrNull() != id1 }
                                                    .joinToString(",")
                                            )

                                            else -> n
                                        }
                                    }
                                    localSelectedEdge = null
                                    localHasUnsavedChanges = true
                                }
                            }, enabled = localSelectedNodeId != null || localSelectedEdge != null) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Удалить точку/связь"
                                )
                            }
                            IconButton(onClick = {
                                arPageViewModel.handleIntent(ArPageIntent.SetNodeList(localNodeList))
                                localHasUnsavedChanges = false
                            }) {
                                Icon(Icons.Default.Check, contentDescription = "Сохранить")
                            }
                            IconButton(onClick = {
                                localNodeList = state.nodeList
                                localSelectedNodeId = null
                                localSelectedEdge = null
                                localHasUnsavedChanges = false
                                arPageViewModel.handleIntent(ArPageIntent.ToggleEditMode(false))
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Выйти")
                            }
                        }
                    }
                }
                FloatingActionButton(
                    onClick = { showNodeMenu = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 120.dp, end = 0.dp),
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        Icons.Default.List,
                        contentDescription = "Список точек/связей",
                        tint = Color.White
                    )
                }
                if (showNodeMenu) {
                    LaunchedEffect(showNodeMenu, state.nodeList) {
                        if (showNodeMenu && state.nodeList.isNotEmpty()) {
                            localNodeList = state.nodeList
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.25f))
                            .clickable { showNodeMenu = false }
                            .zIndex(10f)
                    ) {
                        Card(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 16.dp, bottom = 120.dp)
                                .width(260.dp)
                                .heightIn(max = 420.dp),
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.cardElevation(12.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    "Список точек",
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Divider()
                                val points = localNodeList.filter { it.type != "arrow" && it.type != "line" && it.type != "route" }
                                val scrollState = rememberLazyListState()
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 320.dp),
                                    state = scrollState
                                ) {
                                    if (points.isEmpty()) {
                                        item {
                                            Text(
                                                "Нет точек для отображения",
                                                modifier = Modifier.padding(16.dp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    } else {
                                        items(points) { node ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        localSelectedNodeId = node.id
                                                        localSelectedEdge = null
                                                        showNodeMenu = false
                                                    }
                                                    .background(
                                                        if (localSelectedNodeId == node.id)
                                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                                                        else Color.Transparent
                                                    )
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.Place,
                                                    contentDescription = null,
                                                    tint = if (localSelectedNodeId == node.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                if (localSelectedNodeId == node.id) {
                                                    Spacer(Modifier.width(4.dp))
                                                    Icon(
                                                        Icons.Default.Star,
                                                        contentDescription = "Фокус",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    node.title,
                                                    fontWeight = if (localSelectedNodeId == node.id) FontWeight.Bold else FontWeight.Normal,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                IconButton(
                                                    onClick = {
                                                        editNodeTitleDialogId = node.id
                                                        showNodeMenu = false
                                                    },
                                                    modifier = Modifier.size(20.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Edit,
                                                        contentDescription = "Редактировать",
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                Divider()
                                Button(
                                    onClick = {
                                        showEdgeMenu = true
                                        showNodeMenu = false
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(6.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Связи")
                                }
                                Button(
                                    onClick = { showNodeMenu = false },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("Закрыть")
                                }
                            }
                        }
                    }
                }
                if (showEdgeMenu) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.25f))
                            .clickable { showEdgeMenu = false }
                            .zIndex(10f)
                    ) {
                        Card(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 16.dp, bottom = 120.dp)
                                .width(260.dp)
                                .heightIn(max = 340.dp),
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.cardElevation(12.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    "Список связей",
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Divider()
                                val pointIds = localNodeList.filter { it.type != "arrow" && it.type != "line" && it.type != "route" }.map { it.id }
                                val edges = localEdges.filter { edge ->
                                    pointIds.contains(edge.first) && pointIds.contains(edge.second)
                                }
                                val scrollState = rememberLazyListState()
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 220.dp),
                                    state = scrollState
                                ) {
                                    if (edges.isEmpty()) {
                                        item {
                                            Text(
                                                "Нет связей для отображения",
                                                modifier = Modifier.padding(16.dp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    } else {
                                        items(edges) { edge ->
                                            val node1 = localNodeList.find { it.id == edge.first }
                                            val node2 = localNodeList.find { it.id == edge.second }
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        localSelectedEdge = edge
                                                        localSelectedNodeId = null
                                                        showEdgeMenu = false
                                                    }
                                                    .background(
                                                        if (localSelectedEdge == edge)
                                                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
                                                        else Color.Transparent
                                                    )
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.Share,
                                                    contentDescription = null,
                                                    tint = if (localSelectedEdge == edge) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    "${node1?.title ?: edge.first} ↔ ${node2?.title ?: edge.second}",
                                                    fontWeight = if (localSelectedEdge == edge) FontWeight.Bold else FontWeight.Normal,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }
                                }
                                Divider()
                                Button(
                                    onClick = { showEdgeMenu = false },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp)
                                ) {
                                    Text("Закрыть")
                                }
                            }
                        }
                    }
                }
                if (linkMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.07f))
                            .zIndex(20f)
                    ) {
                        Card(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(start = 12.dp, top = 220.dp)
                                .widthIn(max = 270.dp)
                                .wrapContentHeight(),
                            shape = RoundedCornerShape(14.dp),
                            elevation = CardDefaults.cardElevation(6.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (linkSourceId == null)
                                        "Выберите первую точку для связи\nили связанную для удаления"
                                    else
                                        "Теперь выберите вторую точку: связь будет создана или удалена",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 3
                                )
                            }
                        }
                        Card(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(start = 12.dp, top = 300.dp)
                                .widthIn(max = 200.dp)
                                .heightIn(max = 260.dp),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column {
                                Text(
                                    "Связывание точек",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 4.dp)
                                )
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f, fill = false),
                                    contentPadding = PaddingValues(vertical = 2.dp)
                                ) {
                                    items(localNodeList) { node ->
                                        val isSource = linkSourceId == node.id
                                        val isLinked = linkSourceId != null && localNodeList.find { it.id == linkSourceId }?.neighbors?.split(",")?.mapNotNull { it.toIntOrNull() }?.contains(node.id) == true
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    if (linkSourceId == null) {
                                                        linkSourceId = node.id
                                                    } else if (linkSourceId == node.id) {
                                                        linkSourceId = null
                                                    } else {
                                                        val id1 = linkSourceId!!
                                                        val id2 = node.id
                                                        val node1 = localNodeList.find { it.id == id1 }!!
                                                        val node2 = localNodeList.find { it.id == id2 }!!
                                                        val linked = node1.neighbors.split(",").mapNotNull { it.toIntOrNull() }.contains(id2)
                                                        if (!linked) {
                                                            arPageViewModel.handleIntent(ArPageIntent.LinkNodes(id1, id2))
                                                        } else {
                                                            arPageViewModel.handleIntent(ArPageIntent.DeleteEdge(id1, id2))
                                                        }
                                                        linkSourceId = null
                                                        localNodeList = arPageViewModel.state.value.nodeList
                                                    }
                                                }
                                                .background(
                                                    when {
                                                        isSource -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                                        isLinked -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                                                        else -> Color.Transparent
                                                    }
                                                )
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                node.title,
                                                maxLines = 1,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (isSource) {
                                                Spacer(Modifier.width(6.dp))
                                                Text("(выбрано)", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                                            }
                                            if (isLinked) {
                                                Spacer(Modifier.width(6.dp))
                                                Text("(связано)", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }
                                }
                                Button(
                                    onClick = { linkMode = false; linkSourceId = null },
                                    modifier = Modifier
                                        .align(Alignment.CenterHorizontally)
                                        .padding(vertical = 6.dp)
                                ) { Text("Выйти") }
                            }
                        }
                    }
                }
            }
            if (showLocalLinkDialog && localSelectedNodeId != null) {
                AlertDialog(
                    onDismissRequest = { showLocalLinkDialog = false },
                    title = { Text("Выберите вторую точку для связи") },
                    text = {
                        Column {
                            localNodeList.filter { it.id != localSelectedNodeId }.forEach { node ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { localLinkTargetId = node.id }
                                        .background(
                                            if (localLinkTargetId == node.id) MaterialTheme.colorScheme.primary.copy(
                                                alpha = 0.18f
                                            ) else MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Place, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(node.title)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            if (localLinkTargetId != null) {
                                val id1 = localSelectedNodeId!!
                                val id2 = localLinkTargetId!!
                                localNodeList = localNodeList.map { n ->
                                    when (n.id) {
                                        id1 -> if (n.neighbors.split(",")
                                                .any { it.toIntOrNull() == id2 }
                                        ) n else n.copy(
                                            neighbors = (n.neighbors + "," + id2).trim(
                                                ','
                                            )
                                        )

                                        id2 -> if (n.neighbors.split(",")
                                                .any { it.toIntOrNull() == id1 }
                                        ) n else n.copy(
                                            neighbors = (n.neighbors + "," + id1).trim(
                                                ','
                                            )
                                        )

                                        else -> n
                                    }
                                }
                                localHasUnsavedChanges = true
                                showLocalLinkDialog = false
                                localLinkTargetId = null
                            }
                        }, enabled = localLinkTargetId != null) {
                            Text("Связать")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showLocalLinkDialog = false; localLinkTargetId = null
                        }) {
                            Text("Отмена")
                        }
                    }
                )
            }
            if (showNewNodeDialog) {
                var nodeTypeDropdownExpanded by remember(showNewNodeDialog) { mutableStateOf(false) }
                val nodeTypes = listOf("view" to "Точка с названием", "linked" to "Связная точка")
                AlertDialog(
                    onDismissRequest = {
                        showNewNodeDialog = false
                        nodeTypeDropdownExpanded = false
                    },
                    title = { Text("Создать новую точку") },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = newNodeTitle,
                                onValueChange = { newNodeTitle = it },
                                label = { Text("Название точки") },
                                singleLine = true
                            )
                            Spacer(Modifier.height(8.dp))
                            ExposedDropdownMenuBox(
                                expanded = nodeTypeDropdownExpanded,
                                onExpandedChange = {
                                    nodeTypeDropdownExpanded = !nodeTypeDropdownExpanded
                                }
                            ) {
                                OutlinedTextField(
                                    value = nodeTypes.find { it.first == newNodeType }?.second
                                        ?: "Точка с названием",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Тип точки") },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(
                                            expanded = nodeTypeDropdownExpanded
                                        )
                                    },
                                    modifier = Modifier.menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = nodeTypeDropdownExpanded,
                                    onDismissRequest = { nodeTypeDropdownExpanded = false }
                                ) {
                                    nodeTypes.forEach { (type, label) ->
                                        DropdownMenuItem(
                                            text = { Text(label) },
                                            onClick = {
                                                newNodeType = type
                                                nodeTypeDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "После выбора типа и названия нажмите на сцену для размещения точки",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val newId = (localNodeList.maxOfOrNull { it.id } ?: 0) + 1
                                nodeToPlaceId = newId
                                pendingNodeType = newNodeType
                                pendingNodeTitle = newNodeTitle
                                readyToPlaceNode = true
                                showNewNodeDialog = false
                                newNodeTitle = ""
                                newNodeType = nodeTypes.first().first
                                nodeTypeDropdownExpanded = false
                            },
                            enabled = newNodeTitle.isNotBlank()
                        ) {
                            Text("Выбрать место на сцене")
                        }
                    },
                    dismissButton = {
                        Button(
                            onClick = {
                                showNewNodeDialog = false
                                nodeTypeDropdownExpanded = false
                            }
                        ) {
                            Text("Отмена")
                        }
                    }
                )
            }

            if (showQrDialog) {
                var dropdownExpanded by remember { mutableStateOf(false) }
                val viewNodes = localNodeList.filter { it.type == "view" }
                var selectedQrNodeId by remember { mutableStateOf(viewNodes.firstOrNull()?.id ?: -1) }
                val selectedQrNode = viewNodes.find { it.id == selectedQrNodeId } ?: viewNodes.firstOrNull()
                val qrData = remember(selectedQrNodeId) {
                    "{" +
                            "\"type\":\"mapar_location\"," +
                            "\"buildingId\":${buildingId}," +
                            "\"nodeId\":${selectedQrNode?.id}," +
                            "\"nodeTitle\":\"${selectedQrNode?.title ?: ""}\"" +
                            "}"
                }
                val qrBitmap = remember(qrData) {
                    val size = 220
                    val writer = QRCodeWriter()
                    val bitMatrix = writer.encode(
                        qrData,
                        BarcodeFormat.QR_CODE,
                        size,
                        size
                    )
                    val bmp = Bitmap.createBitmap(
                        size,
                        size,
                        Bitmap.Config.RGB_565
                    )
                    for (x in 0 until size) {
                        for (y in 0 until size) {
                            bmp.setPixel(
                                x,
                                y,
                                if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                            )
                        }
                    }
                    bmp
                }
                val context = LocalContext.current
                AlertDialog(
                    onDismissRequest = { showQrDialog = false },
                    title = { Text("Генерация QR-кода для точки") },
                    text = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (viewNodes.isNotEmpty()) {
                                Text("Выберите точку:", style = MaterialTheme.typography.bodyMedium)
                                ExposedDropdownMenuBox(
                                    expanded = dropdownExpanded,
                                    onExpandedChange = { dropdownExpanded = !dropdownExpanded },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = viewNodes.find { it.id == selectedQrNodeId }?.title ?: "-",
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Точка") },
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                                        },
                                        modifier = Modifier.menuAnchor().fillMaxWidth()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = dropdownExpanded,
                                        onDismissRequest = { dropdownExpanded = false }
                                    ) {
                                        viewNodes.forEach { node ->
                                            DropdownMenuItem(
                                                text = { Text("${node.title} (${node.id})") },
                                                onClick = {
                                                    selectedQrNodeId = node.id
                                                    dropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                                AndroidView(
                                    factory = { ctx ->
                                        ImageView(ctx).apply { setImageBitmap(qrBitmap) }
                                    },
                                    modifier = Modifier.size(180.dp).align(Alignment.CenterHorizontally)
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    Button(onClick = {
                                        val size = 512
                                        val writer = QRCodeWriter()
                                        val bitMatrix = writer.encode(
                                            qrData,
                                            BarcodeFormat.QR_CODE,
                                            size,
                                            size
                                        )
                                        val bmp = Bitmap.createBitmap(
                                            size,
                                            size,
                                            Bitmap.Config.RGB_565
                                        )
                                        for (x in 0 until size) {
                                            for (y in 0 until size) {
                                                bmp.setPixel(
                                                    x,
                                                    y,
                                                    if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                                                )
                                            }
                                        }
                                        val filename = "qr-node-${selectedQrNode?.id ?: "unknown"}.png"
                                        val fos = context.openFileOutput(filename, Context.MODE_PRIVATE)
                                        bmp.compress(Bitmap.CompressFormat.PNG, 100, fos)
                                        fos.close()
                                        Toast.makeText(context, "QR-код сохранён: $filename", Toast.LENGTH_LONG).show()
                                    }) {
                                        Text("Сохранить QR-код")
                                    }
                                    OutlinedButton(onClick = { showQrDialog = false }) {
                                        Text("Назад")
                                    }
                                }
                            } else {
                                Text(
                                    "Нет точек типа 'view' для генерации QR-кода",
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(Modifier.height(8.dp))
                                OutlinedButton(onClick = { showQrDialog = false }) {
                                    Text("Назад")
                                }
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {}
                )
            }
        }
    }

    LaunchedEffect(state.isEditMode) {
        if (state.isEditMode) {
            try {
                Log.d("ArPage", "[EDITMODE] Загружаем все точки для режима редактирования, buildingId=$buildingId")
                arPageViewModel.handleIntent(ArPageIntent.SetLoading(true))
                val nodes = try {
                    nodeViewModel.getNodesForBuilding(buildingId.toLong())
                } catch (e: Exception) {
                    Log.e("ArPage", "[EDITMODE] Ошибка загрузки точек из БД", e)
                    emptyList()
                }
                Log.d("ArPage", "[EDITMODE] Загружено ${nodes.size} точек для редактирования")
                arPageViewModel.handleIntent(ArPageIntent.SetNodeList(nodes))
            } catch (e: Exception) {
                Log.e("ArPage", "[EDITMODE] Ошибка загрузки точек", e)
                snackbarHostState.showSnackbar("Ошибка при загрузке точек: ${e.message}")
            } finally {
                arPageViewModel.handleIntent(ArPageIntent.SetLoading(false))
            }
        }
    }

    LaunchedEffect(state.isEditMode, state.isQrMode) {
        if (state.isEditMode || state.isQrMode) {
            Log.d("ArPage", "Сброс состояния AR сцены: isEditMode=${state.isEditMode}, isQrMode=${state.isQrMode}")
            handler.resetSceneState(state)
        }
    }

    LaunchedEffect(state.isEditMode, arPageViewModel.hasUnsavedChanges) {
        hasUnsavedChanges = arPageViewModel.hasUnsavedChanges
        if (!state.isEditMode) {
            hasUnsavedChanges = false
            arPageViewModel.resetUnsavedFlag()
        }
    }

    if (showExitEditModeDialog) {
        AlertDialog(
            onDismissRequest = { showExitEditModeDialog = false },
            title = { Text("Несохраненные изменения") },
            text = { Text("У вас есть несохраненные изменения. Сохранить перед выходом из режима редактирования?") },
            confirmButton = {
                Button(
                    onClick = {
                        arPageViewModel.saveBuilding()
                        arPageViewModel.resetUnsavedFlag()
                        arPageViewModel.handleIntent(ArPageIntent.ToggleEditMode(false))
                        showExitEditModeDialog = false
                    }
                ) {
                    Text("Сохранить и выйти")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        arPageViewModel.resetUnsavedFlag()
                        arPageViewModel.handleIntent(ArPageIntent.ToggleEditMode(false))
                        showExitEditModeDialog = false
                    }
                ) {
                    Text("Выйти без сохранения")
                }
            }
        )
    }

    BackHandler(enabled = state.isEditMode && hasUnsavedChanges) {
        tryExitEditMode()
    }
}

@Composable
fun RouteInfoBar(distanceToTurn: Float, turnDirection: String, modifier: Modifier = Modifier) {
    val isDestination = turnDirection == "Вы дошли до места назначения"
    val turnIcon = when {
        isDestination -> R.drawable.ic_check_circle
        turnDirection == "Вправо" -> R.drawable.turn_right
        turnDirection == "Влево" -> R.drawable.turn_left
        turnDirection == "Вперед" -> R.drawable.turn_forward
        turnDirection == "Назад" -> R.drawable.turn_back
        else -> R.drawable.turn_forward
    }
    val bgColor = if (isDestination) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isDestination) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 72.dp, start = 16.dp, end = 16.dp)
            .offset(y = (-16).dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopEnd)
                .width(300.dp)
                .height(120.dp),
            shape = RoundedCornerShape(16.dp),
            color = bgColor,
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    painter = painterResource(id = turnIcon),
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .padding(8.dp),
                    tint = if (isDestination) MaterialTheme.colorScheme.primary else LocalContentColor.current
                )
                Column(
                    modifier = Modifier
                      .weight(1f)
                        .padding(start = 8.dp)
                ) {
                    Text(
                        text = if (isDestination) "Вы дошли до места назначения" else "Поворот: $turnDirection",
                        style = MaterialTheme.typography.bodyLarge,
                        fontSize = 16.sp,
                        color = textColor
                    )
                    if (!isDestination) {
                        Text(
                            text = "До поворота: ${"%.2f".format(distanceToTurn)} м",
                            style = MaterialTheme.typography.bodyLarge,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }
                }
            }
        }
    }
}

fun calculateTurnDirection(currentPosition: FloatArray, nextPosition: FloatArray): String {
    val dx = nextPosition[0] - currentPosition[0]
    val dz = nextPosition[2] - currentPosition[2]

    val threshold = 0.2f

    return when {
        abs(dx) < threshold && abs(dz) < threshold -> "Вперед"
        dx > 0 && dz > 0 -> "Вправо"
        dx > 0 && dz < 0 -> "Влево"
        dx < 0 && dz > 0 -> "Влево"
        dx < 0 && dz < 0 -> "Вправо"
        dx > 0 -> "Вперед"
        dx < 0 -> "Назад"
        dz > 0 -> "Вправо"
        dz < 0 -> "Влево"
        else -> "Вперед"
    }
}

@Composable
fun ChangeBuildingDialog(
    onDismiss: () -> Unit,
    onChangeBuilding: (Int, String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Выберите здание") },
        text = {
        },
        confirmButton = {
            Button(
                onClick = {
                    val newBuildingId = 1
                    val newBuildingName = "Новое здание"
                    onChangeBuilding(newBuildingId, newBuildingName)
                }
            ) {
                Text("Сменить")
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss
            ) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun QrCodeDialog(
    onDismiss: () -> Unit,
    qrBitmap: Bitmap,
    qrInfo: QrBuildingInfo,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.fillMaxWidth(0.95f),
        properties = DialogProperties(dismissOnClickOutside = true),
        title = {
            Column {
                Text(
                    text = "QR-код локации",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Divider()
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .size(256.dp)
                        .padding(8.dp),
                    shape = RoundedCornerShape(8.dp),
                    tonalElevation = 8.dp,
                    shadowElevation = 4.dp
                ) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "QR код",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    )
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    tonalElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Информация о локации",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Divider(modifier = Modifier.padding(vertical = 4.dp))

                        InfoRow(
                            label = "Здание:",
                            value = qrInfo.buildingName
                        )
                        InfoRow(
                            label = "Адрес:",
                            value = qrInfo.buildingAddress
                        )
                        InfoRow(
                            label = "Точка:",
                            value = qrInfo.nodeTitle
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FilledTonalButton(
                        onClick = onSave,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text("Сохранить")
                        }
                    }

                    FilledTonalButton(
                        onClick = { },
                        modifier = Modifier.weight(1f).padding(start = 8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text("Поделиться")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun QrDialogForNode(
    showQrDialog: Boolean,
    onDismiss: () -> Unit,
    localNodeList: List<NodeDb>,
    buildingId: Int
) {
    val viewNodes = localNodeList.filter { it.type == "view" }
    var selectedQrNodeId by remember {
        mutableStateOf(viewNodes.firstOrNull()?.id ?: -1)
    }
    val selectedQrNode = viewNodes.find { it.id == selectedQrNodeId } ?: viewNodes.firstOrNull()
    val qrData = remember(selectedQrNode) {
        "{" +
                "\"type\":\"mapar_location\"," +
                "\"buildingId\":${buildingId}," +
                "\"nodeId\":${selectedQrNode?.id}," +
                "\"nodeTitle\":\"${selectedQrNode?.title ?: ""}\"" +
                "}"
    }
    val context = LocalContext.current
    if (showQrDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Генерация QR-кода для точки") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (viewNodes.isNotEmpty()) {
                        DropdownMenu(
                            expanded = true,
                            onDismissRequest = {},
                            modifier = Modifier.fillMaxWidth().height(200.dp)
                        ) {
                            viewNodes.forEach { node ->
                                DropdownMenuItem(
                                    text = { Text("${node.title} (${node.id})") },
                                    onClick = { selectedQrNodeId = node.id }
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        AndroidView(
                            factory = { ctx ->
                                val size = 320
                                val writer = QRCodeWriter()
                                val bitMatrix = writer.encode(
                                    qrData,
                                    BarcodeFormat.QR_CODE,
                                    size,
                                    size
                                )
                                val bmp = Bitmap.createBitmap(
                                    size,
                                    size,
                                    Bitmap.Config.RGB_565
                                )
                                for (x in 0 until size) {
                                    for (y in 0 until size) {
                                        bmp.setPixel(
                                            x,
                                            y,
                                            if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                                        )
                                    }
                                }
                                ImageView(ctx)
                                    .apply { setImageBitmap(bmp) }
                            },
                            modifier = Modifier.size(220.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = {
                            val size = 512
                            val writer = QRCodeWriter()
                            val bitMatrix = writer.encode(
                                qrData,
                                BarcodeFormat.QR_CODE,
                                size,
                                size
                            )
                            val bmp = Bitmap.createBitmap(
                                size,
                                size,
                                Bitmap.Config.RGB_565
                            )
                            for (x in 0 until size) {
                                for (y in 0 until size) {
                                    bmp.setPixel(
                                        x,
                                        y,
                                        if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                                    )
                                }
                            }
                            val filename =
                                "qr-node-${selectedQrNode?.id ?: "unknown"}.png"
                            val fos = context.openFileOutput(
                                filename,
                                Context.MODE_PRIVATE
                            )
                            bmp.compress(
                                Bitmap.CompressFormat.PNG,
                                100,
                                fos
                            )
                            fos.close()
                            Toast.makeText(
                                context,
                                "QR-код сохранён: $filename",
                                Toast.LENGTH_LONG
                            ).show()
                        }) {
                            Text("Сохранить QR-код")
                        }
                    } else {
                        Text(
                            "Нет точек типа 'view' для генерации QR-кода",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = onDismiss) { Text("Закрыть") }
            }
        )
    }
}