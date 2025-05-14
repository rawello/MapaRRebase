package com.rawello.mapar.presentation.buildSelection

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.with
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.rawello.mapar.data.database.NodeViewModel
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
fun SelectBuildPage(onIntent: (SelectBuildIntent) -> Unit, state: SelectBuildState) {
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLoading = true
        try {
            val buildings = ApiFactory.apiService.getAllBuildings()
            try {
                val favorites = ApiFactory.apiService.getFavorites()
                onIntent(SelectBuildIntent.UpdateFavorites(favorites))
            }
            catch(_:Exception){

            }
            onIntent(SelectBuildIntent.UpdateBuildings(buildings.map { Building(it.id, it.name, it.address) }))
        } catch (e: Exception) {
            onIntent(SelectBuildIntent.UpdateErrorMessage("Error: ${e.message}"))
        } finally {
            isLoading = false
        }
    }

    Surface {
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding() // Добавляем отступ от строки состояния
                    .navigationBarsPadding(), // Добавляем отступ от нижней навигации
                verticalArrangement = Arrangement.Top, // Меняем выравнивание на Top
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = { query -> onIntent(SelectBuildIntent.UpdateSearchQuery(query)) },
                        label = { Text("Поиск здания") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            //todo
                        })
                    )
                    IconButton(
                        onClick = { onIntent(SelectBuildIntent.NavigateQr) },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = "Сканировать QR")
                    }
                }

                AnimatedVisibility(
                    visible = state.favorites.isNotEmpty(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column {
                        Text(
                            text = "Избранные здания",
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp)
                        )
                        AnimatedFavoritesList(
                            favorites = state.favorites.map { id -> state.buildings.find { it.id.toLong() == id }!! },
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
                    modifier = Modifier.weight(1f)
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

    fun onIntent(intent: SelectBuildIntent) {
        when (intent) {
            is SelectBuildIntent.NavigateToNext -> {
                state.selectedBuilding?.let { building ->
                    Log.d("NavigationDebug", "Building ID: ${building.id}, Building Name: ${building.name}")
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
                loadNodesForBuilding(intent.building.id, nodeViewModel, snackbarHostState, state, isLoading, navController)
            }
            is SelectBuildIntent.UpdateSearchQuery -> {
                state = state.copy(searchQuery = intent.query)
            }
            is SelectBuildIntent.ToggleFavorite -> {
                val favorites = state.favorites.toMutableList()
                if (favorites.contains(intent.buildingId)) {
                    favorites.remove(intent.buildingId)
                } else {
                    favorites.add(intent.buildingId)
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
            SelectBuildPage(onIntent = { intent ->
                onIntent(intent)
            }, state = state)
        }
    }

    SnackbarHost(hostState = snackbarHostState)
}

fun loadNodesForBuilding(buildingId: Int, nodeViewModel: NodeViewModel, snackbarHostState: SnackbarHostState,
                         state: SelectBuildState, isLoading: MutableState<Boolean>, navController: NavController) {
    CoroutineScope(Dispatchers.IO).launch {
        isLoading.value = true
        try {
            Log.d("SelectBuildPage", "Starting to load nodes for building $buildingId")
            val nodesResponse = ApiFactory.apiService.getNodesForBuilding(buildingId.toLong())
            Log.d("SelectBuildPage", "Received ${nodesResponse.size} nodes from server for building $buildingId")
            
            // Очищаем существующие точки
            Log.d("SelectBuildPage", "Truncating existing nodes")
            nodeViewModel.truncate()
            delay(500) // Даем время на очистку базы
            
            withContext(Dispatchers.Main) {
                var allNodesInserted = true
                
                nodesResponse.forEach { nodeResponse ->
                    try {
                        Log.d("SelectBuildPage", "Converting node: id=${nodeResponse.id}, type=${nodeResponse.modelPath}")
                        val nodeDb = convertNodeResponseToNodeDb(nodeResponse, buildingId.toLong())
                        Log.d("SelectBuildPage", "Converted node: ${nodeDb.id}, type=${nodeDb.type}, title=${nodeDb.title}")
                        nodeViewModel.insertNode(nodeDb)
                    } catch (e: Exception) {
                        Log.e("SelectBuildPage", "Error inserting node: ${e.message}")
                        allNodesInserted = false
                    }
                }

                if (allNodesInserted) {
                    delay(1000) // Даем больше времени на завершение вставки
                    Log.d("SelectBuildPage", "All nodes inserted successfully, navigating to AR screen")
                    val encodedName = state.selectedBuilding?.name?.replace(" ", "_") ?: ""
                    val route = Routes.ArScene.string
                        .replace("{buildingId}", buildingId.toString())
                        .replace("{buildingName}", encodedName)
                    navController.navigate(route) {
                        // Опционально: добавляем настройки навигации
                        launchSingleTop = true
                    }
                } else {
                    Log.e("SelectBuildPage", "Some nodes failed to insert")
                    snackbarHostState.showSnackbar("Ошибка при загрузке некоторых точек")
                }
            }
        } catch (e: Exception) {
            Log.e("SelectBuildPage", "Error loading nodes: ${e.message}", e)
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
            Log.e("updateFavorites", "Error updating favorites: ${e.message}", e)
        }
    }
}