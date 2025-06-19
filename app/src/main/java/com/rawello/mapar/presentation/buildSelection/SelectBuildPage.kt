package com.rawello.mapar.presentation.buildSelection

import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.rawello.mapar.R
import com.rawello.mapar.data.TokenManager
import com.rawello.mapar.data.database.FavoriteViewModel
import com.rawello.mapar.data.database.NodeViewModel
import com.rawello.mapar.data.database.BuildingRepository
import com.rawello.mapar.data.database.NetworkUtils
import com.rawello.mapar.data.models.Building
import com.rawello.mapar.data.models.NodeDb
import com.rawello.mapar.data.network.ApiFactory
import com.rawello.mapar.data.network.NodeResponse
import com.rawello.mapar.presentation.Routes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

data class SelectBuildState(
    val buildings: List<Building> = emptyList(),
    val selectedBuilding: Building? = null,
    val searchQuery: String = "",
    val errorMessage: String? = null,
    val favorites: List<Long> = emptyList()
)

sealed class SelectBuildIntent {
    data object NavigateToNext : SelectBuildIntent()
    data object NavigateQr : SelectBuildIntent()
    data object NavigateText : SelectBuildIntent()
    data class SelectBuilding(val building: Building) : SelectBuildIntent()
    data class UpdateSearchQuery(val query: String) : SelectBuildIntent()
    data class ToggleFavorite(val buildingId: Long) : SelectBuildIntent()
    data class UpdateBuildings(val buildings: List<Building>) : SelectBuildIntent()
    data class UpdateErrorMessage(val message: String) : SelectBuildIntent()
    data class UpdateFavorites(val favorites: List<Long>) : SelectBuildIntent()
}

@Composable
fun SelectBuildPage(onIntent: (SelectBuildIntent) -> Unit, state: SelectBuildState, snackbarHostState: SnackbarHostState) {
    var isLoading by remember { mutableStateOf(false) }
    var showLocalDialog by remember { mutableStateOf(false) }
    var showAuthDialog by remember { mutableStateOf(false) }
    var loginErrorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    val favoriteViewModel: FavoriteViewModel = viewModel(
        factory = FavoriteViewModel.Factory(context.applicationContext as android.app.Application)
    )
    val localFavorites by favoriteViewModel.favorites.collectAsState()

    LaunchedEffect(localFavorites) {
        Log.d("FAVORITES_DEBUG", "localFavorites: ${localFavorites.map { it.buildingId to it.buildingName }}")
    }

    LaunchedEffect(Unit) {
        isLoading = true
        val hasNetwork = NetworkUtils.isNetworkAvailable(context)
        val repo = BuildingRepository(context)
        val localBuildings = repo.getAllBuildings()
        val localFavoritesList = localFavorites.map { it.buildingId.toLong() }
        Log.d("FAVORITES_DEBUG", "localFavoritesList: $localFavoritesList, hasNetwork=$hasNetwork, localBuildings.size=${localBuildings.size}")
        if (hasNetwork) {
            try {
                withTimeout(2500) {
                    val buildings = try {
                        ApiFactory.apiService.getAllBuildings()
                    } catch (e: Exception) {
                        emptyList()
                    }
                    val favorites = try {
                        withTimeoutOrNull(1500) {
                            ApiFactory.apiService.getFavorites()
                        }
                    } catch (e: Exception) {
                        null
                    }
                    if (favorites != null && favorites.isNotEmpty()) {
                        onIntent(SelectBuildIntent.UpdateFavorites(favorites))
                    } else if (localFavoritesList.isNotEmpty()) {
                        onIntent(SelectBuildIntent.UpdateFavorites(localFavoritesList))
                    }
                    if (buildings.isNotEmpty()) {
                        onIntent(SelectBuildIntent.UpdateBuildings(buildings.map { Building(it.id, it.name, it.address) }))
                        buildings.forEach { repo.insertBuilding(Building(it.id, it.name, it.address)) }
                    } else if (localBuildings.isNotEmpty()) {
                        if (!hasNetwork) showLocalDialog = true
                        onIntent(SelectBuildIntent.UpdateBuildings(localBuildings))
                        if (localFavoritesList.isNotEmpty()) {
                            onIntent(SelectBuildIntent.UpdateFavorites(localFavoritesList))
                        }
                    } else {
                        onIntent(SelectBuildIntent.UpdateErrorMessage("Нет интернета и нет локальных данных"))
                    }
                }
            } catch (e: Exception) {
                if (localBuildings.isNotEmpty()) {
                    if (!hasNetwork) showLocalDialog = true
                    onIntent(SelectBuildIntent.UpdateBuildings(localBuildings))
                    if (localFavoritesList.isNotEmpty()) {
                        onIntent(SelectBuildIntent.UpdateFavorites(localFavoritesList))
                    }
                } else {
                    onIntent(SelectBuildIntent.UpdateErrorMessage("Нет интернета и нет локальных данных"))
                }
            } finally {
                isLoading = false
            }
        } else {
            if (localBuildings.isNotEmpty()) {
                showLocalDialog = true
                onIntent(SelectBuildIntent.UpdateBuildings(localBuildings))
                if (localFavoritesList.isNotEmpty()) {
                    onIntent(SelectBuildIntent.UpdateFavorites(localFavoritesList))
                }
            } else {
                onIntent(SelectBuildIntent.UpdateErrorMessage("Нет интернета и нет локальных данных"))
            }
            isLoading = false
        }
    }

    if (showLocalDialog) {
        AlertDialog(
            onDismissRequest = { showLocalDialog = false },
            title = { Text("Нет интернета") },
            text = { Text("Данные загружены из памяти. Некоторые функции могут быть недоступны.") },
            confirmButton = {
                Button(onClick = { showLocalDialog = false }) { Text("Ок") }
            }
        )
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        var showSearch by remember { mutableStateOf(false) }
        var showAccountDialog by remember { mutableStateOf(false) }
        var showLogoutDialog by remember { mutableStateOf(false) }
        var searchQuery by remember { mutableStateOf(state.searchQuery) }
        val searchBarHeight = 64.dp
        val searchBarOffset by animateDpAsState(targetValue = if (showSearch) 56.dp else -searchBarHeight - 24.dp)
        val contentTopPadding by animateDpAsState(targetValue = if (showSearch) 104.dp else 32.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = contentTopPadding, bottom = 32.dp)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (state.buildings.isEmpty() && state.errorMessage != null) {
                Log.d("error", state.errorMessage)
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Проверьте подключение к интернету",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                val favoritesToShow = if (state.favorites.isNotEmpty()) {
                    state.favorites
                } else if (localFavorites.isNotEmpty()) {
                    localFavorites.map { it.buildingId.toLong() }
                } else {
                    emptyList()
                }

                AnimatedVisibility(
                    visible = favoritesToShow.isNotEmpty(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column {
                        Text(
                            text = "Избранные здания",
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp)
                        )
                        val context = LocalContext.current
                        val localBuildingsState = produceState<List<Building>>(initialValue = emptyList(), context) {
                            value = BuildingRepository(context).getAllBuildings()
                        }
                        val favoritesBuildings = remember(favoritesToShow, state.buildings, localBuildingsState.value, localFavorites) {
                            val result = favoritesToShow.mapNotNull { id ->
                                state.buildings.find { it.id.toLong() == id }
                                    ?: localBuildingsState.value.find { it.id.toLong() == id }
                                    ?: localFavorites.find { it.buildingId.toLong() == id }?.let { fav ->
                                        com.rawello.mapar.data.models.Building(fav.buildingId, fav.buildingName, "—")
                                    }
                            }
                            Log.d("FAVORITES_DEBUG", "favoritesBuildings: ${result.map { it.id to it.name }}")
                            result
                        }
                        AnimatedFavoritesList(
                            favorites = favoritesBuildings,
                            onSelect = { building -> onIntent(SelectBuildIntent.SelectBuilding(building)) },
                            onToggleFavorite = { buildingId -> onIntent(SelectBuildIntent.ToggleFavorite(buildingId)) }
                        )
                    }
                }

                Text(
                    text = "Все здания",
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp)
                )
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = 32.dp)
                ) {
                    val filteredBuildings = state.buildings.filter { it.name.contains(state.searchQuery, ignoreCase = true) }
                    items(filteredBuildings) { building ->
                        BuildingItem(building = building, onSelect = {
                            onIntent(SelectBuildIntent.SelectBuilding(building))
                        }, onToggleFavorite = {
                            onIntent(SelectBuildIntent.ToggleFavorite(building.id.toLong()))
                        })
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(searchBarHeight)
                .offset(y = searchBarOffset)
                .padding(horizontal = 16.dp)
                .zIndex(2f)
        ) {
            if (showSearch) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        onIntent(SelectBuildIntent.UpdateSearchQuery(it))
                    },
                    label = { Text("Поиск здания") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = searchBarHeight),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            showSearch = false
                            searchQuery = ""
                            onIntent(SelectBuildIntent.UpdateSearchQuery(""))
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Закрыть поиск")
                        }
                    }
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier
                    .widthIn(min = 260.dp, max = 400.dp)
                    .height(64.dp)
                    .offset(y = (-32).dp),
                shape = RoundedCornerShape(32.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f))
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = { showAccountDialog = true }) {
                        Icon(painter = painterResource(id= R.drawable.account), contentDescription = "Аккаунт", modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { showSearch = !showSearch }) {
                        Icon(Icons.Default.Search, contentDescription = "Поиск")
                    }
                    IconButton(onClick = { onIntent(SelectBuildIntent.NavigateQr) }) {
                        Icon(painter = painterResource(id= R.drawable.qrcode), contentDescription = "QR", modifier = Modifier.size(24.dp))
                    }
                }
            }
        }

        if (showAccountDialog) {
            val tokenManager = remember { TokenManager.getInstance(context) }
            var currentUser by remember { mutableStateOf<String?>(null) }
            val scope = rememberCoroutineScope()
            var showNewBuildingDialog by remember { mutableStateOf(false) }
            var newBuildingName by remember { mutableStateOf("") }
            LaunchedEffect(showAccountDialog) {
                currentUser = tokenManager.getCurrentUsername()
            }
            if (currentUser != null) {
                AlertDialog(
                    onDismissRequest = { showAccountDialog = false },
                    title = { Text("Аккаунт") },
                    text = {
                        Column {
                            Text("Вы вошли как $currentUser")
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { showNewBuildingDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = "Добавить здание")
                                Spacer(Modifier.width(8.dp))
                                Text("Создать свое здание")
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            scope.launch {
                                tokenManager.logout()
                                showLogoutDialog = false
                                showAccountDialog = false
                                currentUser = null
                            }
                        }) { Text("Выйти") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAccountDialog = false }) { Text("Назад") }
                    }
                )
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
                                    scope.launch {
                                        try {
                                            val created = ApiFactory.apiService.createBuilding(
                                                Building(0, newBuildingName, newBuildingAddress)
                                            )
                                            val buildings = ApiFactory.apiService.getAllBuildings()
                                            onIntent(SelectBuildIntent.UpdateBuildings(buildings))
                                            showNewBuildingDialog = false
                                            snackbarHostState.showSnackbar("Здание успешно создано")
                                        } catch (e: Exception) {
                                            snackbarHostState.showSnackbar("Ошибка создания здания: ${e.message}")
                                        }
                                    }
                                },
                                enabled = newBuildingName.isNotBlank() && newBuildingAddress.isNotBlank()
                            ) { Text("Создать") }
                        },
                        dismissButton = {
                            Button(onClick = { showNewBuildingDialog = false }) { Text("Отмена") }
                        }
                    )
                }
            } else {
                AnimatedVisibility(
                    visible = currentUser == null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    com.rawello.mapar.presentation.login.LoginPage(
                        onDismiss = { showAccountDialog = false },
                        onLogin = { username, password ->
                            val trimmedUsername = username.trim()
                            val trimmedPassword = password.trim()
                            Log.d("LoginDialog", "Попытка входа: $trimmedUsername")
                            scope.launch {
                                try {
                                    val response = ApiFactory.apiService.login(com.rawello.mapar.data.models.LoginRequest(trimmedUsername, trimmedPassword))
                                    Log.d("LoginDialog", "Успешный вход: $trimmedUsername, token: ${response.token}")
                                    TokenManager.getInstance(context).login(trimmedUsername, response.token)
                                    currentUser = trimmedUsername
                                    showAccountDialog = false
                                    loginErrorMessage = null
                                    isLoading = true
                                    try {
                                        val favorites = withTimeoutOrNull(1500) {
                                            ApiFactory.apiService.getFavorites()
                                        }
                                        if (favorites != null) {
                                            onIntent(SelectBuildIntent.UpdateFavorites(favorites))
                                        }
                                    } catch (e: Exception) {
                                        Log.e("LoginDialog", "Ошибка загрузки избранных после логина: ${e.message}")
                                    }
                                    isLoading = false
                                } catch (e: Exception) {
                                    Log.e("LoginDialog", "Ошибка входа: $trimmedUsername, ${e.message}", e)
                                    val isForbidden = e.message?.contains("403") == true
                                    loginErrorMessage = if (isForbidden) "Неверный логин или пароль" else (e.message ?: "Неизвестная ошибка")
                                }
                            }
                        },
                        onRegister = { username, password ->
                            val trimmedUsername = username.trim()
                            val trimmedPassword = password.trim()
                            Log.d("LoginDialog", "Попытка регистрации: $trimmedUsername")
                            scope.launch {
                                try {
                                    ApiFactory.apiService.register(com.rawello.mapar.data.models.SignUpRequest(trimmedUsername, trimmedPassword))
                                    Log.d("LoginDialog", "Успешная регистрация: $trimmedUsername")
                                    val response = ApiFactory.apiService.login(com.rawello.mapar.data.models.LoginRequest(trimmedUsername, trimmedPassword))
                                    TokenManager.getInstance(context).login(trimmedUsername, response.token)
                                    currentUser = trimmedUsername
                                    showAccountDialog = false
                                    loginErrorMessage = null
                                } catch (e: Exception) {
                                    Log.e("LoginDialog", "Ошибка регистрации: $trimmedUsername, ${e.message}", e)
                                    val isForbidden = e.message?.contains("403") == true
                                    loginErrorMessage = if (isForbidden) "Неверный логин или пароль" else (e.message ?: "Неизвестная ошибка")
                                }
                            }
                        },
                        snackbarHostState = snackbarHostState,
                        scope = scope,
                        context = context,
                        errorMessage = loginErrorMessage
                    )
                }
            }
        }

        if (showLogoutDialog) {
            val scope = rememberCoroutineScope()
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("Выйти из аккаунта?") },
                confirmButton = {
                    Button(onClick = {
                        scope.launch {
                            TokenManager.getInstance(context).logout()
                            showLogoutDialog = false
                            showAccountDialog = false
                        }
                    }) { Text("Выйти") }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) { Text("Отмена") }
                }
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedFavoritesList(favorites: List<Building>, onSelect: (Building) -> Unit, onToggleFavorite: (Long) -> Unit) {
    val maxHeight = 200.dp
    val itemHeight = 80.dp
    val maxItems = (maxHeight / itemHeight).toInt()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight)
            .animateContentSize()
    ) {
        AnimatedContent(
            targetState = favorites,
            transitionSpec = {
                fadeIn(animationSpec = tween(150, 150)) togetherWith
                        fadeOut(animationSpec = tween(150)) using
                        SizeTransform { initialSize, targetSize ->
                            if (targetSize.height > initialSize.height) {
                                keyframes {
                                    IntSize(initialSize.width, initialSize.height) at 150
                                    durationMillis = 300
                                }
                            } else {
                                keyframes {
                                    IntSize(targetSize.width, targetSize.height) at 150
                                    durationMillis = 300
                                }
                            }
                        }
            }, label = "Избранное"
        ) { targetFavorites ->
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(targetFavorites) { building ->
                    AnimatedBuildingItem(building = building, onSelect = { onSelect(building) }, onToggleFavorite = { onToggleFavorite(building.id.toLong()) })
                }
            }
        }
    }
}

@Composable
fun AnimatedBuildingItem(building: Building, onSelect: () -> Unit, onToggleFavorite: () -> Unit) {
    val transition = updateTransition(targetState = building, label = "buildingItemTransition")
    val alpha by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 300) },
        label = "alphaTransition"
    ) {
        if (it == building) 1f else 0f
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .clickable { onSelect() }
            .alpha(alpha),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = building.name,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = building.address,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            IconButton(
                onClick = { onToggleFavorite() }
            ) {
                Icon(Icons.Default.Favorite, contentDescription = "Добавить в избранное")
            }
        }
    }
}

@Composable
fun BuildingItem(building: Building, onSelect: () -> Unit, onToggleFavorite: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .clickable { onSelect() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = building.name,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = building.address,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            IconButton(
                onClick = { onToggleFavorite() }
            ) {
                Icon(Icons.Default.Favorite, contentDescription = "Добавить в избранное")
            }
        }
    }
}

@Composable
fun SelectBuildScreen(navController: NavController, nodeViewModel: NodeViewModel) {
    var state by remember { mutableStateOf(SelectBuildState()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val isLoading: MutableState<Boolean> = remember { mutableStateOf(false) }
    val showSelectBuildPage: MutableState<Boolean> = remember { mutableStateOf(true) }

    val context = LocalContext.current.applicationContext
    val favoriteViewModel: FavoriteViewModel = viewModel(
        factory = FavoriteViewModel.Factory(context as android.app.Application)
    )
    val localFavorites by favoriteViewModel.favorites.collectAsState()

    var showAuthDialog by remember { mutableStateOf(false) }
    var authMessage by remember { mutableStateOf<String?>(null) }

    fun onIntent(intent: SelectBuildIntent) {
        when (intent) {
            is SelectBuildIntent.NavigateToNext -> {
                state.selectedBuilding?.let { building ->
                    Log.d("NavigationDebug", "ID здания: ${building.id}, Название здания: ${building.name}")
                    val encodedName = building.name.replace(" ", "_")
                    val route = Routes.ArScene.string
                        .replace("{buildingId}", building.id.toString())
                        .replace("{buildingName}", encodedName)
                    navController.navigate(route) {
                        launchSingleTop = true
                    }
                }
            }
            is SelectBuildIntent.NavigateQr -> navController.navigate(Routes.QrAlpha.string)
            is SelectBuildIntent.NavigateText -> navController.navigate(Routes.Text.string)
            is SelectBuildIntent.SelectBuilding -> {
                state = state.copy(selectedBuilding = intent.building)
                showSelectBuildPage.value = false
                navController.navigate(
                    Routes.ArScene.string
                        .replace("{buildingId}", intent.building.id.toString())
                        .replace("{buildingName}", intent.building.name.replace(" ", "_"))
                ) {
                    launchSingleTop = true
                }
            }
            is SelectBuildIntent.UpdateSearchQuery -> {
                state = state.copy(searchQuery = intent.query)
            }
            is SelectBuildIntent.ToggleFavorite -> {
                val favorites = state.favorites.toMutableList()
                if (favorites.contains(intent.buildingId)) {
                    favorites.remove(intent.buildingId)
                    favoriteViewModel.removeFavorite(intent.buildingId.toInt())
                } else {
                    favorites.add(intent.buildingId)
                    val building = state.buildings.find { it.id.toLong() == intent.buildingId }
                    if (building != null) {
                        favoriteViewModel.addFavorite(building.id, building.name)
                    }
                }
                state = state.copy(favorites = favorites)
                updateFavorites(favorites)
            }
            is SelectBuildIntent.UpdateBuildings -> {
                state = state.copy(buildings = intent.buildings)
            }
            is SelectBuildIntent.UpdateErrorMessage -> {
                state = state.copy(errorMessage = intent.message)
            }
            is SelectBuildIntent.UpdateFavorites -> {
                state = state.copy(favorites = intent.favorites)
            }
        }
    }

    Surface {
        if (isLoading.value) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (showSelectBuildPage.value) {
            Column {
                SelectBuildPage(onIntent = { intent ->
                    onIntent(intent)
                }, state = state.copy(
                    favorites = if (false) localFavorites.map { it.buildingId.toLong() } else state.favorites
                ), snackbarHostState = snackbarHostState)
            }
        }
    }

    if (showAuthDialog) {
        val scope = rememberCoroutineScope()
        com.rawello.mapar.presentation.login.LoginPage(
            onDismiss = { showAuthDialog = false },
            onLogin = { username, password ->
                val trimmedUsername = username.trim()
                val trimmedPassword = password.trim()
                Log.d("LoginDialog", "Попытка входа: $trimmedUsername")
                scope.launch {
                    try {
                        val response = ApiFactory.apiService.login(com.rawello.mapar.data.models.LoginRequest(trimmedUsername, trimmedPassword))
                        Log.d("LoginDialog", "Успешный вход: $trimmedUsername, token: ${response.token}")
                        TokenManager.getInstance(context).login(trimmedUsername, response.token)
                        authMessage = "Вход выполнен успешно"
                        showAuthDialog = false
                    } catch (e: Exception) {
                        Log.e("LoginDialog", "Ошибка входа: $trimmedUsername, ${e.message}", e)
                        val isForbidden = e.message?.contains("403") == true
                        authMessage = if (isForbidden) "Неверный логин или пароль" else (e.message ?: "Неизвестная ошибка")
                    }
                }
            },
            onRegister = { username, password ->
                val trimmedUsername = username.trim()
                val trimmedPassword = password.trim()
                Log.d("LoginDialog", "Попытка регистрации: $trimmedUsername")
                scope.launch {
                    try {
                        ApiFactory.apiService.register(com.rawello.mapar.data.models.SignUpRequest(trimmedUsername, trimmedPassword))
                        Log.d("LoginDialog", "Успешная регистрация: $trimmedUsername")
                        val response = ApiFactory.apiService.login(com.rawello.mapar.data.models.LoginRequest(trimmedUsername, trimmedPassword))
                        TokenManager.getInstance(context).login(trimmedUsername, response.token)
                        authMessage = "Регистрация успешна. Теперь войдите."
                        showAuthDialog = false
                    } catch (e: Exception) {
                        Log.e("LoginDialog", "Ошибка регистрации: $trimmedUsername, ${e.message}", e)
                        val isForbidden = e.message?.contains("403") == true
                        authMessage = if (isForbidden) "Неверный логин или пароль" else (e.message ?: "Неизвестная ошибка")
                    }
                }
            },
            snackbarHostState = snackbarHostState,
            scope = scope,
            context = context
        )
    }

    LaunchedEffect(authMessage) {
        authMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            authMessage = null
        }
    }

    SnackbarHost(hostState = snackbarHostState)
}

fun loadNodesForBuilding(
    buildingId: Int,
    nodeViewModel: NodeViewModel,
    snackbarHostState: SnackbarHostState,
    state: SelectBuildState,
    isLoading: MutableState<Boolean>,
    navController: NavController
) {
    CoroutineScope(Dispatchers.IO).launch {
        isLoading.value = true
        try {
            Log.d("SelectBuildPage", "Начинаем загрузку точек для здания $buildingId")
            var nodesResponse: List<NodeResponse>? = null
            var networkAvailable = false
            try {
                nodesResponse = ApiFactory.apiService.getNodesForBuilding(buildingId.toLong())
                networkAvailable = true
            } catch (e: Exception) {
                Log.e("SelectBuildPage", "Нет сети или ошибка сервера, пробуем локальную БД: ${e.message}")
            }
            if (networkAvailable && nodesResponse != null) {
                Log.d("SelectBuildPage", "Получено ${nodesResponse.size} точек с сервера для здания $buildingId")
                Log.d("SelectBuildPage", "Очистка существующих точек")
                nodeViewModel.truncate()
                delay(500)
                withContext(Dispatchers.Main) {
                    var allNodesInserted = true
                    nodesResponse.forEach { nodeResponse ->
                        try {
                            val nodeDb = convertNodeResponseToNodeDb(nodeResponse, buildingId.toLong())
                            nodeViewModel.insertNode(nodeDb)
                        } catch (e: Exception) {
                            Log.e("SelectBuildPage", "Ошибка вставки точки: ${e.message}")
                            allNodesInserted = false
                        }
                    }
                    if (allNodesInserted) {
                        delay(1000)
                        val encodedName = state.selectedBuilding?.name?.replace(" ", "_") ?: ""
                        val route = Routes.ArScene.string
                            .replace("{buildingId}", buildingId.toString())
                            .replace("{buildingName}", encodedName)
                        navController.navigate(route) { launchSingleTop = true }
                    } else {
                        snackbarHostState.showSnackbar("Ошибка при загрузке некоторых точек")
                    }
                }
            } else {
                val localNodes = nodeViewModel.getNodesForBuilding(buildingId.toLong())
                withContext(Dispatchers.Main) {
                    if (localNodes.isNotEmpty()) {
                        Log.d("SelectBuildPage", "Загружено ${localNodes.size} точек из локальной БД, переход к экрану AR")
                        val encodedName = state.selectedBuilding?.name?.replace(" ", "_") ?: ""
                        val route = Routes.ArScene.string
                            .replace("{buildingId}", buildingId.toString())
                            .replace("{buildingName}", encodedName)
                        navController.navigate(route) { launchSingleTop = true }
                    } else {
                        snackbarHostState.showSnackbar("Нет точек для здания в локальной базе. Подключитесь к интернету для первой загрузки.")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SelectBuildPage", "Ошибка загрузки точек: ${e.message}", e)
            withContext(Dispatchers.Main) {
                snackbarHostState.showSnackbar("Ошибка загрузки точек: ${e.message}")
            }
        } finally {
            isLoading.value = false
        }
    }
}

fun convertNodeResponseToNodeDb(nodeResponse: NodeResponse, buildingId: Long): NodeDb {
    return NodeDb(
        id = nodeResponse.id,
        buildingId = buildingId,
        nameNode = "node",
        translation = nodeResponse.translation.split(",").map { it.toFloat() }.toFloatArray(),
        quaternion = nodeResponse.quaternion.split(",").map { it.toFloat() }.toFloatArray(),
        type = nodeResponse.modelPath,
        title = nodeResponse.title,
        neighbors = nodeResponse.neighbors
    )
}

fun updateFavorites(favorites: List<Long>) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            ApiFactory.apiService.updateFavorites(favorites)
        } catch (e: Exception) {
            Log.e("updateFavorites", "Ошибка обновления избранного: ${e.message}", e)
        }
    }
}