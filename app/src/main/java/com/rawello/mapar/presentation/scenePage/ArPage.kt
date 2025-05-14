package com.rawello.mapar.presentation.scenePage

import android.graphics.Bitmap
import android.util.Log
import android.view.MotionEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
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

    LaunchedEffect(buildingId) {
        try {
            Log.d("ArPage", "Starting initialization for building $buildingId")
            arPageViewModel.handleIntent(ArPageIntent.SetLoading(true))
            arPageViewModel.handleIntent(ArPageIntent.SetBuildingId(buildingId))
            arPageViewModel.handleIntent(ArPageIntent.SetBuildingName(buildingName))
            
            // Загружаем точки
            Log.d("ArPage", "Loading nodes for building $buildingId")
            val nodes = nodeViewModel.getNodesForBuilding(buildingId.toLong())
            Log.d("ArPage", "Loaded ${nodes.size} nodes")
            
            if (nodes.isNotEmpty()) {
                nodes.forEach { node ->
                    Log.d("ArPage", "Node ${node.id}: type=${node.type}, title=${node.title}, translation=${node.translation.contentToString()}")
                }
                arPageViewModel.handleIntent(ArPageIntent.SetNodeList(nodes))
                delay(1000) // Даем время на инициализацию и подготовку ресурсов
                
                // Если есть целевая точка, автоматически строим к ней маршрут
                destinationNodeId?.let { destId ->
                    val sourceNode = nodes.find { it.type == "view" }
                    val destNode = nodes.find { it.id == destId }
                    if (sourceNode != null && destNode != null) {
                        Log.d("ArPage", "Building path from ${sourceNode.id} to $destId")
                        arPageViewModel.handleIntent(ArPageIntent.SelectPoint(sourceNode.id.toString(), destId.toString()))
                        arPageViewModel.handleIntent(ArPageIntent.SetButtonText(sourceNode.title, destNode.title))
                        arPageViewModel.handleIntent(ArPageIntent.BuildPath)
                    } else {
                        Log.e("ArPage", "Could not find source or destination node for automatic path building")
                    }
                }
            } else {
                Log.e("ArPage", "No nodes loaded for building $buildingId")
            }
        } catch (e: Exception) {
            Log.e("ArPage", "Error loading nodes", e)
            snackbarHostState.showSnackbar("Ошибка при загрузке точек: ${e.message}")
        } finally {
            arPageViewModel.handleIntent(ArPageIntent.SetLoading(false))
        }
    }

    var isDrawerOpen by remember { mutableStateOf(false) }
    var isAnonymous by remember { mutableStateOf(true) }
    var isBuildingOwner by remember { mutableStateOf(false) }
    var showNewBuildingDialog by remember { mutableStateOf(false) }
    var newBuildingName by remember { mutableStateOf("") }

    var showNewNodeDialog by remember { mutableStateOf(false) }
    var newNodeTitle by remember { mutableStateOf("") }
    var newNodeType by remember { mutableStateOf("") }

    var isLoginDialogOpen by remember { mutableStateOf(false) }
    var isRegisterDialogOpen by remember { mutableStateOf(false) }
    var isChangeBuildingDialogOpen by remember { mutableStateOf(false) }

    var showLocationSelectionDialog by remember { mutableStateOf(false) }

    val systemUiController = rememberSystemUiController()
    val useDarkIcons = MaterialTheme.colorScheme.surface.luminance() > 0.5f
    val color = MaterialTheme.colorScheme.background

    LaunchedEffect(Unit) {
        isAnonymous = tokenManager.isAnonymous()
        isBuildingOwner = !isAnonymous && ApiFactory.apiService.isBuildingOwner(buildingId.toLong())
    }

    var showAlertDialog by remember { mutableStateOf(false) }
    var inputTitle by remember { mutableStateOf("") }

    var motionEvent: MotionEvent? = null
    var node: Node? = null

    var showQrDialog by remember { mutableStateOf(false) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var hasUnsavedChanges by remember { mutableStateOf(false) }
    var showExitEditModeDialog by remember { mutableStateOf(false) }

    // Добавляем обработку выхода из режима редактирования
    fun tryExitEditMode() {
        if (hasUnsavedChanges) {
            showExitEditModeDialog = true
        } else {
            arPageViewModel.handleIntent(ArPageIntent.ToggleEditMode(false))
        }
    }

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
                        Column {
                            Text(buildingName)
                            if (state.isEditMode) {
                                Text(
                                    "Режим редактирования",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { isDrawerOpen = !isDrawerOpen }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Открыть меню"
                            )
                        }
                    },
                    actions = {
                        if (state.isEditMode) {
                            IconButton(onClick = { tryExitEditMode() }) {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = "Выйти из режима редактирования"
                                )
                            }
                        } else {
                            IconButton(
                                onClick = { showLocationSelectionDialog = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = "Установить местоположение"
                                )
                            }
                        }
                    },
                    modifier = Modifier.statusBarsPadding()
                )
            }
        },
        bottomBar = {
            BottomAppBar(
                modifier = Modifier.navigationBarsPadding()
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    //if (!isAnonymous || isBuildingOwner) {
                        IconButton(
                            onClick = { arPageViewModel.handleIntent(ArPageIntent.ToggleQrMode(false)) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Режим AR"
                            )
                        }
                        IconButton(
                            onClick = { arPageViewModel.handleIntent(ArPageIntent.ToggleQrMode(true)) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Режим QR"
                            )
                        }
                    //}
                }
            }
        }
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
                            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        TopAppBar(
                            modifier = Modifier.align(Alignment.TopCenter),
                            title = { Text(text = state.buildingName) },
                            actions = {
                                if (!state.isEditMode) {
                                    IconButton(
                                        onClick = {
                                            showLocationSelectionDialog = true
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = "Установить местоположение"
                                        )
                                    }
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
                                                        arPageViewModel.handleIntent(ArPageIntent.SetUserLocation(node.id, navController))
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
                                    motionEvent = event
                                    node = tappedNode
                                    if (state.model == "view") {
                                        showAlertDialog = true
                                    } else {
                                        scope.launch {
                                            handler.onSingleTapConfirmedWithTitle(event, tappedNode, state, "")
                                        }
                                    }
                                },
                                onLongPress = { _, tappedNode ->
                                    scope.launch {
                                        handler.onLongPress(tappedNode)
                                    }
                                }
                            ),
                            onSessionUpdated = { session, updatedFrame ->
                                scope.launch {
                                    state.context = context
                                    handler.handleSessionUpdate(session, updatedFrame, state)
                                }
                            },
                            onViewCreated = {
                                handler.onViewCreated(this)
                            },
                            onViewUpdated = {
                                handler.onViewUpdated(this)
                            }
                        )

                        if (showAlertDialog) {
                            AlertDialog(
                                onDismissRequest = { showAlertDialog = false },
                                title = { Text("Введите название точки") },
                                text = {
                                    TextField(
                                        value = inputTitle,
                                        onValueChange = { inputTitle = it },
                                        label = { Text("Название") }
                                    )
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            showAlertDialog = false
                                            scope.launch {
                                                motionEvent?.let { event ->
                                                    arPageViewModel.handleIntent(ArPageIntent.SetReadyToPlace(true))
                                                    arPageViewModel.handleIntent(ArPageIntent.SetLinking(false))
                                                    arPageViewModel.handleIntent(ArPageIntent.SetModel("view"))

                                                    handler.onSingleTapConfirmedWithTitle(event, null, state, inputTitle)
                                                }
                                            }
                                        }
                                    ) {
                                        Text("Сохранить")
                                    }
                                },
                                dismissButton = {
                                    Button(
                                        onClick = { showAlertDialog = false }
                                    ) {
                                        Text("Отмена")
                                    }
                                }
                            )
                        }

                        if (state.showBottomSheet) {
                            ModalBottomSheet(
                                onDismissRequest = { arPageViewModel.handleIntent(ArPageIntent.SetShowBottomSheet(false)) },
                                sheetState = rememberModalBottomSheetState(),
                                //windowInsets = WindowInsets(0)
                            ) {
                                NodesListContent(
                                    nodes = state.nodeList,
                                    onPointsSelected = { point1, point2 ->
                                        scope.launch {
                                            arPageViewModel.handleIntent(ArPageIntent.SelectPoint(point1, point2))
                                            arPageViewModel.handleIntent(ArPageIntent.SetButtonText(point1, point2))
                                            arPageViewModel.handleIntent(ArPageIntent.SetShowBottomSheet(false))
                                        }
                                    },
                                    //modifier = Modifier.navigationBarsPadding()
                                )
                            }
                        }

                        BottomSheetScaffold(
                            snackbarHost = { SnackbarHost(snackbarHostState) },
                            sheetContent = {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    if (!isAnonymous || isBuildingOwner) {
                                        Text(
                                            text = "Допустим управление",
                                            style = MaterialTheme.typography.bodyLarge,
                                            modifier = Modifier.padding(bottom = 6.dp)
                                        )
                                        Row {
                                            Button(onClick = {
                                                scope.launch {
                                                    arPageViewModel.handleIntent(ArPageIntent.SetReadyToPlace(true))
                                                    arPageViewModel.handleIntent(ArPageIntent.SetLinking(false))
                                                    arPageViewModel.handleIntent(ArPageIntent.SetModel("models/cylinder.glb"))
                                                }
                                            }) {
                                                Text("Точка связи")
                                            }

                                            Spacer(modifier = Modifier.padding(6.dp))

                                            Button(onClick = {
                                                scope.launch {
                                                    arPageViewModel.handleIntent(ArPageIntent.SetReadyToPlace(true))
                                                    arPageViewModel.handleIntent(ArPageIntent.SetLinking(false))
                                                    arPageViewModel.handleIntent(ArPageIntent.SetModel("view"))
                                                }
                                            }) {
                                                Text("Точка входа")
                                            }
                                        }

                                        Spacer(modifier = Modifier.padding(6.dp))
                                        Row {
                                            Button(onClick = {
                                                scope.launch {
                                                    try {
                                                        handler.selectedNode?.name?.let { nodeName ->
                                                            arPageViewModel.handleIntent(ArPageIntent.DeleteNode(nodeName.toInt()))
                                                            scope.launch {
                                                                arPageViewModel.handleIntent(ArPageIntent.SetNodeList(nodeViewModel.getAllNodes().first()))
                                                            }
                                                            arPageViewModel.handleIntent(ArPageIntent.BuildPath)
                                                        }
                                                    } catch (_: Exception) {
                                                        scope.launch {
                                                            snackbarHostState.showSnackbar("Ошибка при удалении точки")
                                                        }
                                                    }
                                                }
                                            }) {
                                                Text("Удалить")
                                            }

                                            Spacer(modifier = Modifier.padding(6.dp))

                                            Button(onClick = {
                                                state.childNodes.forEach {
                                                    Log.d("tag", it.name + "_|" + it)
                                                }
                                                Log.d("tag1", state.nodeList.size.toString())
                                            }) {
                                                Text("QR")
                                            }
                                            Spacer(modifier = Modifier.padding(6.dp))

                                            Button(onClick = {
                                                scope.launch {
                                                    arPageViewModel.handleIntent(ArPageIntent.SetReadyToPlace(false))
                                                    arPageViewModel.handleIntent(ArPageIntent.SetLinking(false))

                                                    if (handler.selectedNode?.childNodes?.isNotEmpty() == true) {
                                                        handler.selectedNode?.childNodes!!.forEach { childNode ->
                                                            if (childNode.name!! == "cone")
                                                                childNode.childNodes.forEach {
                                                                    it.destroy()
                                                                }
                                                        }
                                                    }
                                                }
                                            }) {
                                                Text("Убрать фокус")
                                            }
                                        }
                                        Spacer(modifier = Modifier.padding(6.dp))
                                        Button(
                                            onClick = {
                                                scope.launch {
                                                    handler.selectedNode?.name?.let { nodeName ->
                                                        arPageViewModel.handleIntent(ArPageIntent.SetLinking(true))
                                                        arPageViewModel.handleIntent(ArPageIntent.SetReadyToPlace(false))
                                                    }
                                                }
                                            },
                                        ) {
                                            Text("Связать")
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.padding(6.dp))
                                Row {
                                    Button(onClick = {
                                        arPageViewModel.handleIntent(ArPageIntent.BuildPath)
                                        arPageViewModel.handleIntent(ArPageIntent.SetShowBottomSheet(true))
                                        arPageViewModel.handleIntent(ArPageIntent.SetShowBottomSheet(false))
                                    }) {
                                        Text("Построить")
                                    }
                                    Text("Выбор кабинетов:")
                                    Row {
                                        Button(onClick = {
                                            scope.launch {
                                                arPageViewModel.handleIntent(ArPageIntent.SelectPoint(null, null))
                                                arPageViewModel.handleIntent(ArPageIntent.SetShowBottomSheet(true))
                                            }
                                        }) {
                                            Text(if (state.buttonText1.isNotEmpty() && state.buttonText2.isNotEmpty()) {
                                                "${state.buttonText1} -> ${state.buttonText2}"
                                            } else {
                                                "Выбрать маршрут"
                                            })
                                        }
                                    }
                                }
                            },
                            content = {
                                // Content
                            }
                        )
                    }
                }
            }
        }

        if (isDrawerOpen) {
            ModalDrawerSheet {
                DrawerContent(
                    isAnonymous = isAnonymous,
                    isBuildingOwner = isBuildingOwner,
                    onNewBuilding = {
                        showNewBuildingDialog = true
                    },
                    onEditBuilding = {
                        arPageViewModel.handleIntent(ArPageIntent.ToggleEditMode(true))
                    },
                    onSaveBuilding = {
                        arPageViewModel.saveBuilding()
                    },
                    onCloseDrawer = { isDrawerOpen = false },
                    onLoginClick = { isLoginDialogOpen = true },
                    onLogoutClick = {
                        scope.launch {
                            tokenManager.logout()
                            isAnonymous = true
                            isBuildingOwner = false
                            // Перезагружаем данные с новыми правами
                            val nodes = nodeViewModel.getNodesForBuilding(buildingId.toLong())
                            arPageViewModel.handleIntent(ArPageIntent.SetNodeList(nodes))
                        }
                    },
                    state = state,
                    hasUnsavedChanges = hasUnsavedChanges
                )
            }
        }

        if (showNewBuildingDialog) {
            AlertDialog(
                onDismissRequest = { showNewBuildingDialog = false },
                title = { Text("Введите название нового здания") },
                text = {
                    TextField(
                        value = newBuildingName,
                        onValueChange = { newBuildingName = it },
                        label = { Text("Название здания") }
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            arPageViewModel.createBuilding(newBuildingName)
                            showNewBuildingDialog = false
                        }
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

        if (showNewNodeDialog) {
            AlertDialog(
                onDismissRequest = { showNewNodeDialog = false },
                title = { Text("Введите данные новой точки") },
                text = {
                    Column {
                        TextField(
                            value = newNodeTitle,
                            onValueChange = { newNodeTitle = it },
                            label = { Text("Название точки") }
                        )
                        TextField(
                            value = newNodeType,
                            onValueChange = { newNodeType = it },
                            label = { Text("Тип точки") }
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val newNode = NodeDb(
                                translation = floatArrayOf(0f, 0f, 0f),
                                quaternion = floatArrayOf(0f, 0f, 0f, 1f),
                                type = newNodeType,
                                title = newNodeTitle,
                                neighbors = "",
                                buildingId = buildingId.toLong(),
                                nameNode = "node"
                            )
                            arPageViewModel.createNode(newNode)
                            showNewNodeDialog = false
                        }
                    ) {
                        Text("Создать")
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { showNewNodeDialog = false }
                    ) {
                        Text("Отмена")
                    }
                }
            )
        }

        if (isLoginDialogOpen) {
            LoginPage(
                onDismiss = { isLoginDialogOpen = false },
                onLogin = { username, password ->
                    scope.launch {
                        try {
                            val loginRequest = LoginRequest(username, password)
                            val response = ApiFactory.apiService.login(loginRequest)
                            
                            // Сохраняем токен через TokenManager
                            tokenManager.login(username, response.token)
                            
                            // Обновляем статус пользователя
                            isAnonymous = false
                            isBuildingOwner = ApiFactory.apiService.isBuildingOwner(buildingId.toLong())
                            
                            // Показываем уведомление об успешном входе
                            snackbarHostState.showSnackbar("Вход выполнен успешно")
                            isLoginDialogOpen = false

                            // Перезагружаем данные с новыми правами
                            arPageViewModel.handleIntent(ArPageIntent.SetLoading(true))
                            val nodes = nodeViewModel.getNodesForBuilding(buildingId.toLong())
                            arPageViewModel.handleIntent(ArPageIntent.SetNodeList(nodes))
                            arPageViewModel.handleIntent(ArPageIntent.SetLoading(false))
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("Ошибка входа: ${e.message}")
                        }
                    }
                },
                onRegister = { username, password ->
                    scope.launch {
                        try {
                            val signUpRequest = SignUpRequest(username, password)
                            val response = ApiFactory.apiService.register(signUpRequest)
                            snackbarHostState.showSnackbar(response)
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("Ошибка регистрации: ${e.message}")
                        }
                    }
                },
                snackbarHostState = snackbarHostState,
                scope = scope,
                context = context
            )
        }

        if (isChangeBuildingDialogOpen) {
            ChangeBuildingDialog(
                onDismiss = { isChangeBuildingDialogOpen = false },
                onChangeBuilding = { newBuildingId, newBuildingName ->
                    arPageViewModel.resetState()
                    navController.navigate("arPage/$newBuildingId/$newBuildingName")
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
                    val distanceToTurn = PathHelper().distance(currentPosition, nextNode?.position ?: currentNode.position)
                    val totalDistance = PathHelper().sumOfDistances(state.path)
                    val turnDirection = calculateTurnDirection(currentPosition, nextNode?.position ?: currentNode.position)

                    Log.d("tag", currentPosition.toString())

                    arPageViewModel.handleIntent(ArPageIntent.SetDistanceToTurn(distanceToTurn))
                    arPageViewModel.handleIntent(ArPageIntent.SetTurnDirection(turnDirection))
                }
            }
        }

        if (state.path.isNotEmpty()) {
            val currentNode = state.path.first()
            val nextNode = state.path.getOrNull(1)
            val currentPosition = handler.getCurrentCameraPosition(state.frame!!)
            val distanceToTurn = PathHelper().distance(currentPosition, nextNode?.position ?: currentNode.position)
            val totalDistance = PathHelper().sumOfDistances(state.path)
            val turnDirection = calculateTurnDirection(currentPosition, nextNode?.position ?: currentNode.position)

            RouteInfoBar(
                distanceToTurn = distanceToTurn,
                turnDirection = turnDirection,
                modifier = Modifier.statusBarsPadding()
            )
        }

        if (showQrDialog && qrBitmap != null) {
            QrCodeDialog(
                onDismiss = { showQrDialog = false },
                qrBitmap = qrBitmap!!,
                qrInfo = QrBuildingInfo(
                    buildingName = "Building Name",
                    buildingAddress = "Building Address",
                    nodeTitle = "Node Title",
                    nodeName = "",
                    buildingId = 1,
                    nodeId = 1
                ),
                onSave = {
                    // TODO: Добавить сохранение QR-кода
                    arPageViewModel.handleIntent(ArPageIntent.ShowSnackbar("QR-код сохранен"))
                }
            )
        }
    }

    // Диалог подтверждения выхода из режима редактирования
    if (showExitEditModeDialog) {
        AlertDialog(
            onDismissRequest = { showExitEditModeDialog = false },
            title = { Text("Несохраненные изменения") },
            text = { Text("У вас есть несохраненные изменения. Сохранить перед выходом из режима редактирования?") },
            confirmButton = {
                Button(
                    onClick = {
                        arPageViewModel.saveBuilding()
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
                        arPageViewModel.handleIntent(ArPageIntent.ToggleEditMode(false))
                        showExitEditModeDialog = false
                    }
                ) {
                    Text("Выйти без сохранения")
                }
            }
        )
    }

    // Обновляем состояние несохраненных изменений при любых изменениях в режиме редактирования
    LaunchedEffect(state.isEditMode) {
        if (!state.isEditMode) {
            hasUnsavedChanges = false
        }
    }

    // Обработка выхода из приложения с несохраненными изменениями
    BackHandler(enabled = state.isEditMode && hasUnsavedChanges) {
        tryExitEditMode()
    }
}

@Composable
fun RouteInfoBar(distanceToTurn: Float, turnDirection: String, modifier: Modifier = Modifier) {
    val turnIcon = when (turnDirection) {
        "Вправо" -> R.drawable.turn_right
        "Влево" -> R.drawable.turn_left
        "Вперед" -> R.drawable.turn_forward
        "Назад" -> R.drawable.turn_back
        else -> R.drawable.turn_forward
    }

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
            color = MaterialTheme.colorScheme.surfaceVariant,
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
                        .padding(8.dp)
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                ) {
                    Text(
                        text = "Поворот: $turnDirection",
                        style = MaterialTheme.typography.bodyLarge,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "До поворота: ${"%.2f".format(distanceToTurn)} м",
                        style = MaterialTheme.typography.bodyLarge,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
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
fun DrawerContent(
    isAnonymous: Boolean,
    isBuildingOwner: Boolean,
    onNewBuilding: () -> Unit,
    onEditBuilding: () -> Unit,
    onSaveBuilding: () -> Unit,
    onCloseDrawer: () -> Unit,
    onLoginClick: () -> Unit,
    onLogoutClick: () -> Unit,
    state: ArPageState,
    hasUnsavedChanges: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Меню",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (isAnonymous) {
            Button(
                onClick = onLoginClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Войти / Зарегистрироваться")
            }
        } else {
            Button(
                onClick = onLogoutClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Выйти из аккаунта")
            }

            if (isBuildingOwner) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Управление зданием",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // Кнопка редактирования теперь показывает текущий режим
                Button(
                    onClick = onEditBuilding,
                    colors = if (state.isEditMode) {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    } else {
                        ButtonDefaults.buttonColors()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (state.isEditMode) "Завершить редактирование" else "Редактировать здание")
                }
                
                // Кнопка сохранения активна только в режиме редактирования
                if (state.isEditMode && hasUnsavedChanges) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onSaveBuilding,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text("Сохранить изменения")
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onCloseDrawer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Закрыть")
        }
    }
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
                // QR код
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

                // Информация о здании
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
                        onClick = { /* Поделиться */ },
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