package com.rawello.mapar.presentation.scenePage

import android.content.Context
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.google.android.filament.Engine
import com.google.ar.core.Anchor
import com.google.ar.core.Frame
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.sceneform.rendering.ViewAttachmentManager
import com.rawello.mapar.data.database.NodeViewModel
import com.rawello.mapar.data.models.NodeDb
import com.rawello.mapar.domain.graphHelpers.createGraph
import com.rawello.mapar.domain.pathfinder.Dijkstra
import com.rawello.mapar.presentation.Routes
import com.rawello.mapar.presentation.helpers.NodeActions
import com.rawello.mapar.presentation.helpers.NodeDrawers
import com.rawello.mapar.presentation.helpers.PathHelper
import io.github.sceneview.ar.ARCore
import io.github.sceneview.ar.arcore.createAnchorOrNull
import io.github.sceneview.ar.arcore.isTrackingPlane
import io.github.sceneview.ar.arcore.isValid
import io.github.sceneview.ar.arcore.quaternion
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.model.ModelInstance
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ArPageHandler(
    private val viewModel: ArPageViewModel,
    private val nodeViewModel: NodeViewModel,
    private val context: Context,
    private val engine: Engine,
    private val modelLoader: ModelLoader,
    private val childNodes: SnapshotStateList<Node>
) {
    private val actions = NodeActions()
    private val drawer = NodeDrawers()
    private var modelInstances = mutableListOf<ModelInstance>()
    var selectedNode: Node? = null
    private var viewAttachmentManager: ViewAttachmentManager? = null

    fun onSingleTapConfirmedWithTitle(motionEvent: MotionEvent, node: Node?, state: ArPageState, title: String) {
        viewModel.viewModelScope.launch {
            if (state.readyToPlace && !state.linking) {
                handlePlacementWithTitle(motionEvent, state, title)
            } else if (!state.readyToPlace && !state.linking) {
                Log.d("pipes", "zdes")
            } else if (state.linking && node != null) {
                handleLinking(node, state)
            }
        }
    }

    private fun handlePlacementWithTitle(motionEvent: MotionEvent, state: ArPageState, title: String) {
        val hitResults = state.frame?.hitTest(motionEvent.x, motionEvent.y)
        hitResults?.firstOrNull { it.isValid(depthPoint = false, point = false) }
            ?.createAnchorOrNull()
            ?.let { anchor ->
                addModelWithTitle(anchor, state, title)
                viewModel.handleIntent(ArPageIntent.SetReadyToPlace(false))
                viewModel.handleIntent(ArPageIntent.BuildPath)
            }
    }

    private fun addModelWithTitle(anchor: Anchor, state: ArPageState, title: String) {
        if (state.model == "view") {
            viewAttachmentManager?.let { vam ->
                val anchorNode = drawer.drawView(engine, modelLoader, vam, context, anchor, nodeViewModel, false, title)
                state.childNodes += anchorNode
                // Инкрементально добавляем NodeDb
                val newId = if (state.nodeList.isEmpty()) 1 else 1 + state.nodeList.maxOf { it.id }
                val nodeDb = com.rawello.mapar.data.models.NodeDb(
                    id = newId,
                    buildingId = state.buildingId.toLong(),
                    nameNode = "node_$newId",
                    translation = anchorNode.pose.translation,
                    quaternion = anchorNode.pose.quaternion.toFloatArray(),
                    type = "view",
                    title = title,
                    neighbors = ""
                )
                val updatedList = state.nodeList.toMutableList().apply { add(nodeDb) }
                viewModel.handleIntent(ArPageIntent.SetNodeList(updatedList))
            }
        } else {
            val nodeList = state.nodeList
            val newId = if (nodeList.isEmpty()) 1 else 1 + nodeList.maxOf { it.id }
            val anchorNode = drawer.drawModelNode(
                engine = engine,
                modelLoader = modelLoader,
                model = state.model,
                modelInstances = modelInstances,
                anchor = anchor,
                viewModel = nodeViewModel,
                needSave = true,
                position = null,
                quaternion = null,
                id = newId,
                true
            )
            state.childNodes += anchorNode
            // Инкрементально добавляем NodeDb
            val nodeDb = com.rawello.mapar.data.models.NodeDb(
                id = newId,
                buildingId = state.buildingId.toLong(),
                nameNode = "node_$newId",
                translation = anchorNode.pose.translation,
                quaternion = anchorNode.pose.quaternion.toFloatArray(),
                type = state.model,
                title = title,
                neighbors = ""
            )
            val updatedList = state.nodeList.toMutableList().apply { add(nodeDb) }
            viewModel.handleIntent(ArPageIntent.SetNodeList(updatedList))
        }
    }

    private suspend fun handleLinking(node: Node, state: ArPageState) {
        node.name?.let { nodeName ->
            if (nodeName != "arrow" && !nodeName.contains("_")) {
                val foundNode = state.nodeList.find { it.id.toString() == selectedNode?.name }
                foundNode?.let {
                    actions.updateNodeNeighbors(true, foundNode, nodeName.toInt(), nodeViewModel, state.nodeList)
                    // После добавления связи сразу рисуем линию между этими точками
                    val neighbor = state.nodeList.find { it.id == nodeName.toInt() }
                    if (neighbor != null) {
                        Log.d("ArPageHandler", "handleLinking: drawing connection from node ${foundNode.id} to ${neighbor.id}")
                        val anchorNode = drawer.drawConnectionLineBetweenNodes(
                            engine = engine,
                            modelLoader = modelLoader,
                            arSession = state.arSession,
                            viewAttachmentManager = state.viewAttachmentManager,
                            context = state.context,
                            node1 = foundNode,
                            node2 = neighbor
                        )
                        if (anchorNode != null) state.childNodes += anchorNode
                    }
                }
                viewModel.handleIntent(ArPageIntent.SetNodeList(nodeViewModel.getAllNodes().first()))
                viewModel.handleIntent(ArPageIntent.BuildPath)
                viewModel.handleIntent(ArPageIntent.SetLinking(false))
            }
        }
    }

    fun onLongPress(node: Node?) {
        viewModel.viewModelScope.launch {
            node?.let { nodeNn ->
                if (selectedNode != nodeNn) {
                    selectedNode?.childNodes?.lastOrNull()?.destroy()
                    selectedNode = nodeNn

                    node.addChildNode(
                        ModelNode(
                            modelInstance = modelInstances.apply {
                                if (isEmpty()) {
                                    this += modelLoader.createInstancedModel("models/cone.glb", 1)
                                }
                            }.removeAt(modelInstances.apply {
                                if (isEmpty()) {
                                    this += modelLoader.createInstancedModel("models/cone.glb", 1)
                                }
                            }.lastIndex),
                            scaleToUnits = 1f
                        ).apply {
                            isHittable = false
                            isEditable = true
                            position.y += 0.5f
                            name = "cone"
                        }
                    )
                }
            }
        }
    }

    fun handleSessionUpdate(session: Session, frame: Frame?, state: ArPageState) {
        viewModel.viewModelScope.launch {
            if (state.arSession != session) {
                //Log.d("ArPageHandler", "handleSessionUpdate: updating arSession in state")
                viewModel.handleIntent(ArPageIntent.SetArSession(session))
            }
            if (state.frame != frame) {
                viewModel.handleIntent(ArPageIntent.SetFrame(frame))
            }
            viewModel.state.value.childNodes = childNodes

            frame?.let { fr ->
                if (fr.isTrackingPlane()) {
                    if ((state.firstLoad || state.childNodes.isEmpty()) && state.arSession != null) {
                        //Log.d("ArPageHandler", "handleSessionUpdate: ready to initialize nodes, nodeList.size=${state.nodeList.size}")
                        try {
                            viewModel.handleIntent(ArPageIntent.SetLoading(true))
                            if (state.nodeList.isNotEmpty()) {
                                Log.d("ArPageHandler", "Initializing ${state.nodeList.size} nodes, isEditMode=${state.isEditMode}, isQrMode=${state.isQrMode}")
                                initializeNodes(state, isEditMode = state.isEditMode, isQrMode = state.isQrMode)
                            } else {
                                Log.d("ArPageHandler", "No nodes to initialize")
                            }
                        } catch (e: Exception) {
                            Log.e("ArPageHandler", "Error initializing nodes", e)
                        } finally {
                            viewModel.handleIntent(ArPageIntent.SetLoading(false))
                            viewModel.handleIntent(ArPageIntent.SetFirstLoad(false))
                        }
                    } else {
                        //Log.d("ArPageHandler", "handleSessionUpdate: not firstLoad and childNodes not empty or arSession is null")
                    }
                } else {
                    //Log.d("ArPageHandler", "handleSessionUpdate: frame is not tracking plane")
                }
                val currentPosition = getCurrentCameraPosition(fr)
                updateRoute(currentPosition, state)

                if (state.path.isNotEmpty()) {
                    updateTurnInfo(state.path)
                }
            } ?: Log.d("ArPageHandler", "handleSessionUpdate: frame is null")
        }
    }

    fun initializeNodes(state: ArPageState, isEditMode: Boolean, isQrMode: Boolean) {
        Log.d("ArPageHandler", "Starting node initialization with ${state.nodeList.size} nodes, isEditMode=$isEditMode, isQrMode=$isQrMode, arSession=${state.arSession}, viewAttachmentManager=${state.viewAttachmentManager}, context=${state.context}")
        // Полный перезапуск: очищаем все childNodes
        state.childNodes.clear()
        // UI движение: временно меняем selectedPoint1 для форсирования recomposition
        state.selectedPoint1 = null
        if (state.nodeList.isNotEmpty()) {
            try {
                val nodesToShow = if (isEditMode && !isQrMode) {
                    Log.d("ArPageHandler", "initializeNodes: edit mode, rendering all nodes")
                    state.nodeList // ВСЕ точки, включая связные, в режиме редактирования
                } else {
                    Log.d("ArPageHandler", "initializeNodes: view mode, rendering only view nodes")
                    state.nodeList.filter { it.type == "view" } // Только view в режиме просмотра
                }
                // Рендерим все точки заново
                nodesToShow.forEach { node ->
                    try {
                        Log.d("ArPageHandler", "initializeNodes: rendering node id=${node.id}, type=${node.type}, translation=${node.translation.contentToString()}, quaternion=${node.quaternion.contentToString()}")
                        state.arSession?.let { arSess ->
                            val anchor = arSess.createAnchor(Pose(node.translation, node.quaternion))
                            val anchorNode = if (node.type == "view") {
                                Log.d("ArPageHandler", "initializeNodes: drawing view node id=${node.id}")
                                drawer.drawView(
                                    engine,
                                    modelLoader,
                                    state.viewAttachmentManager!!,
                                    state.context!!,
                                    anchor,
                                    nodeViewModel,
                                    false,
                                    node.title
                                )
                            } else {
                                Log.d("ArPageHandler", "initializeNodes: drawing model node id=${node.id}")
                                drawer.drawModelNode(
                                    engine = engine,
                                    modelLoader = modelLoader,
                                    model = node.type.ifBlank { "models/box.glb" },
                                    modelInstances = modelInstances,
                                    anchor = anchor,
                                    viewModel = nodeViewModel,
                                    needSave = false,
                                    position = node.translation,
                                    quaternion = node.quaternion,
                                    id = node.id,
                                    isVisible = true
                                )
                            }
                            state.childNodes += anchorNode
                        } ?: Log.e("ArPageHandler", "initializeNodes: arSession is null for node id=${node.id}")
                    } catch (e: Exception) {
                        Log.e("ArPageHandler", "Error initializing node ${node.id}: ${e.message}")
                    }
                }
                // Даем UI время обновиться
                kotlinx.coroutines.runBlocking { delay(120) }
                // В режиме редактирования рендерим все связи
                if (isEditMode && !isQrMode) {
                    Log.d("ArPageHandler", "initializeNodes: rendering connections between nodes (TEST: drawLinkSegment)")
                    state.nodeList.forEach { node ->
                        node.neighbors.split(",").mapNotNull { it.toIntOrNull() }
                            .forEach { neighborId ->
                                if (neighborId > node.id) {
                                    val neighbor = state.nodeList.find { it.id == neighborId }
                                    if (neighbor != null) {
                                        try {
                                            Log.d("ArPageHandler", "initializeNodes: drawing link from node ${node.id} to $neighborId (TEST)")
                                            val path = listOf(
                                                com.rawello.mapar.domain.pathfinder.Dijkstra.Node(
                                                    id = node.id.toString(),
                                                    position = node.translation
                                                ),
                                                com.rawello.mapar.domain.pathfinder.Dijkstra.Node(
                                                    id = neighbor.id.toString(),
                                                    position = neighbor.translation
                                                )
                                            )
                                            state.arSession?.let { arSess ->
                                                drawer.drawLinkSegment(
                                                    engine = engine,
                                                    modelLoader = modelLoader,
                                                    arSession = arSess,
                                                    viewAttachmentManager = state.viewAttachmentManager,
                                                    context = state.context,
                                                    path = path,
                                                    childNodes = state.childNodes
                                                )
                                            }
                                        } catch (e: Exception) {
                                            Log.e("ArPageHandler", "Ошибка создания связи (drawLinkSegment): ${e.message}")
                                        }
                                    }
                                }
                            }
                    }
                }
                // UI движение: временно включаем и выключаем showBottomSheet для форсирования обновления UI
                state.showBottomSheet = true
                kotlinx.coroutines.runBlocking { delay(60) }
                state.showBottomSheet = false
                // UI движение: меняем selectedPoint1 обратно (или любое другое поле, чтобы форсировать recomposition)
                state.selectedPoint1 = null
            } catch (e: Exception) {
                Log.e("ArPageHandler", "Error in node initialization: ${e.message}")
            } finally {
                Log.d("ArPageHandler", "initializeNodes: finished node initialization")
            }
        } else {
            Log.d("ArPageHandler", "initializeNodes: nodeList is empty, nothing to render")
        }
    }

    fun onViewCreated(view: View, state: ArPageState) {
        Log.d("ArPageHandler", "onViewCreated: called, view=$view, state.isEditMode=${state.isEditMode}, state.isQrMode=${state.isQrMode}, arSession=${state.arSession}")
        viewAttachmentManager = ViewAttachmentManager(view.context, view)
        viewModel.state.value.engine = engine
        viewModel.state.value.modelLoader = modelLoader
        viewModel.state.value.viewAttachmentManager = viewAttachmentManager
        viewAttachmentManager?.onResume()
        Log.d("ArPageHandler", "onViewCreated: viewAttachmentManager initialized, engine/modelLoader set")
    }

    fun onViewUpdated(view: View) {
        viewAttachmentManager = ViewAttachmentManager(view.context, view)
        viewModel.state.value.engine = engine
        viewModel.state.value.modelLoader = modelLoader
        viewModel.state.value.viewAttachmentManager = viewAttachmentManager
        viewAttachmentManager?.onResume()
    }

    fun getCurrentCameraPosition(frame: Frame): FloatArray {
        val camera = frame.camera
        val pose = camera.displayOrientedPose
        return floatArrayOf(pose.tx(), pose.ty(), pose.tz())
    }

    private fun calculateRelativeTransform(
        originPosition: FloatArray,
        targetPosition: FloatArray,
        frame: Frame
    ): Pair<FloatArray, FloatArray> {
        val camera = frame.camera
        val cameraPose = camera.displayOrientedPose

        // Вычисляем смещение относительно точки отсчета
        val offsetPosition = FloatArray(3)
        offsetPosition[0] = targetPosition[0] - originPosition[0]
        offsetPosition[1] = targetPosition[1] - originPosition[1]
        offsetPosition[2] = targetPosition[2] - originPosition[2]

        // Получаем кватернион поворота камеры для сохранения ориентации
        val cameraRotation = FloatArray(4)
        cameraPose.getRotationQuaternion(cameraRotation, 0)

        return Pair(offsetPosition, cameraRotation)
    }

    private fun updateRoute(currentPosition: FloatArray, state: ArPageState) {
        val path = state.path.toMutableList()
        val wasNotEmpty = path.isNotEmpty()
        while (path.isNotEmpty() && isPointPassed(currentPosition, path.first().position)) {
            path.removeAt(0)
        }
        val becameEmpty = path.isEmpty() && wasNotEmpty
        state.path = path

        // Показываем окно только один раз, когда маршрут только что закончился
        if (becameEmpty) {
            viewModel.handleIntent(ArPageIntent.ShowSnackbar("Вы достигли конца маршрута"))
            viewModel.showDestinationReachedBar()
            PathHelper().clearOldArrows(state.childNodes)
        }
    }

    private fun isPointPassed(currentPosition: FloatArray, pointPosition: FloatArray): Boolean {
        val distance = PathHelper().distance(currentPosition, pointPosition)
        //Log.d("distance__", distance.toString())
        return distance < 2.0f
    }

    private fun updateTurnInfo(path: List<Dijkstra.Node>): Pair<Float, String> {
        if (path.size < 2) return Pair(0f, "Прямо")

        val currentNode = path.first()
        val nextNode = path[1]
        val distanceToTurn = PathHelper().distance(currentNode.position, nextNode.position)
        val turnDirection = calculateTurnDirection(currentNode.position, nextNode.position)

        return Pair(distanceToTurn, turnDirection)
    }

    fun setUserLocation(nodeId: Int, state: ArPageState, navController: NavController) {
        viewModel.viewModelScope.launch {
            try {
                val locationNode = state.nodeList.find { it.id == nodeId }
                
                if (locationNode == null) {
                    viewModel.handleIntent(ArPageIntent.ShowSnackbar("Точка не найдена"))
                    return@launch
                }
                
                // Обновляем точки в базе данных
                val adjustedNodes = state.nodeList.map { node ->
                    val translationOffset = floatArrayOf(
                        node.translation[0] - locationNode.translation[0],
                        node.translation[1] - locationNode.translation[1],
                        node.translation[2] - locationNode.translation[2]
                    )
                    node.copy(translation = translationOffset)
                }
                
                // Сохраняем в базе данных
                adjustedNodes.forEach { node ->
                    nodeViewModel.updateNode(node)
                }

                // Синхронизируем с сервером
                adjustedNodes.forEach { node ->
                    viewModel.updateNode(node)
                }

                // Навигация назад
                navController.navigateUp()
                // Ждем 1 секунду перед повторной навигацией
                delay(1000)
                // Повторная навигация на тот же экран
                navController.navigate(
                    Routes.ArScene.string
                        .replace("{buildingId}", state.buildingId.toString())
                        .replace("{buildingName}", state.buildingName)
                )
                // --- ДОБАВЛЕНО: сбросить выбранные точки и перестроить маршрут ---
                viewModel.handleIntent(ArPageIntent.SelectPoint(null, null))
                viewModel.handleIntent(ArPageIntent.BuildPath)
                
                viewModel.handleIntent(ArPageIntent.ShowSnackbar("Местоположение установлено"))
            } catch (e: Exception) {
                viewModel.handleIntent(ArPageIntent.ShowSnackbar("Ошибка при установке местоположения: ${e.message}"))
            }
        }
    }

    fun resetSceneState(state: ArPageState) {
        Log.d("ArPageHandler", "resetSceneState: clearing all AR/scene state")
        // Очищаем все childNodes (узлы и связи)
        state.childNodes.clear()
        // Сброс viewAttachmentManager (если требуется)
        state.viewAttachmentManager = null
        // Сброс AR-сессии (если требуется)
        state.arSession = null
        // Сброс списка точек и связей
        state.nodeList = emptyList()
        // Сброс маршрута и выделений
        state.path = emptyList()
        state.selectedPoint1 = null
        state.selectedPoint2 = null
        state.showBottomSheet = false
        state.showRouteInfoBar = false
        // Можно добавить сброс других временных/визуальных стейтов по необходимости
        Log.d("ArPageHandler", "resetSceneState: done")
    }

    fun syncNodeListWithChildNodes(state: ArPageState) {
        val nodes = state.childNodes.mapNotNull { node ->
            // Только те, что реально добавлены через drawView/drawModelNode
            val id = node.name?.toIntOrNull()
            val title = node.name ?: "Точка"
            val type = when (node) {
                is ModelNode -> "linked"
                else -> "view"
            }
            val buildingId = state.buildingId.toLong() ?: 0L
            val nameNode = node.name ?: "node_${id ?: 0}"
            if (id != null) {
                NodeDb(
                    id = id,
                    buildingId = buildingId,
                    nameNode = nameNode,
                    translation = node.position.toFloatArray(),
                    quaternion = node.quaternion.toFloatArray(),
                    type = type,
                    title = title,
                    neighbors = ""
                )
            } else null
        }
        viewModel.handleIntent(ArPageIntent.SetNodeList(nodes))
        Log.d("ArPageHandler", "syncNodeListWithChildNodes: синхронизировано ${nodes.size} точек из AR-сцены (только реально добавленные)")
    }
}