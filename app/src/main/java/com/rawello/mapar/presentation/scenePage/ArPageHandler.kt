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
                state.childNodes += drawer.drawView(engine, modelLoader, vam, context, anchor, nodeViewModel, false, title)
            }
        } else {
            val nodeList = state.nodeList
            val newId = if (nodeList.isEmpty()) 1 else 1 + nodeList.last().id
            state.childNodes += drawer.drawModelNode(
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
        }
    }

    private suspend fun handleLinking(node: Node, state: ArPageState) {
        node.name?.let { nodeName ->
            if (nodeName != "arrow" && !nodeName.contains("_")) {
                val foundNode = state.nodeList.find { it.id.toString() == selectedNode?.name }
                foundNode?.let {
                    actions.updateNodeNeighbors(true, foundNode, nodeName.toInt(), nodeViewModel, state.nodeList)
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
                viewModel.handleIntent(ArPageIntent.SetArSession(session))
            }
            if (state.frame != frame) {
                viewModel.handleIntent(ArPageIntent.SetFrame(frame))
            }
            viewModel.state.value.childNodes = childNodes

            frame?.let { fr ->
                if (fr.isTrackingPlane()) {
                    if (state.firstLoad || state.childNodes.isEmpty()) {
                        try {
                            viewModel.handleIntent(ArPageIntent.SetLoading(true))
                            if (state.nodeList.isNotEmpty()) {
                                Log.d("ArPageHandler", "Initializing ${state.nodeList.size} nodes")
                                initializeNodes(state)
                            } else {
                                Log.d("ArPageHandler", "No nodes to initialize")
                            }
                        } catch (e: Exception) {
                            Log.e("ArPageHandler", "Error initializing nodes", e)
                        } finally {
                            viewModel.handleIntent(ArPageIntent.SetLoading(false))
                            viewModel.handleIntent(ArPageIntent.SetFirstLoad(false))
                        }
                    }
                }

                val currentPosition = getCurrentCameraPosition(fr)
                updateRoute(currentPosition, state)

                if (state.path.isNotEmpty()) {
                    updateTurnInfo(state.path)
                }
            }
        }
    }

    fun initializeNodes(state: ArPageState) {
        Log.d("ArPageHandler", "Starting node initialization with ${state.nodeList.size} nodes")
        
        if (state.nodeList.isNotEmpty()) {
            try {
                // Сначала инициализируем все точки доступа (view)
                state.nodeList.filter { it.type == "view" }.forEach { node ->
                    Log.d("ArPageHandler", "Initializing view node ${node.id}")
                    try {
                        state.arSession?.let { arSess ->
                            state.childNodes += drawer.drawView(
                                engine,
                                modelLoader,
                                state.viewAttachmentManager!!,
                                state.context!!,
                                arSess.createAnchor(Pose(node.translation, node.quaternion)),
                                nodeViewModel,
                                false,
                                node.title
                            )
                            Log.d("ArPageHandler", "Successfully initialized view node ${node.id}")
                        }
                    } catch (e: Exception) {
                        Log.e("ArPageHandler", "Error initializing view node ${node.id}: ${e.message}")
                    }
                }
                
                // Затем инициализируем все остальные точки
                state.nodeList.filter { it.type != "view" }.forEach { node ->
                    Log.d("ArPageHandler", "Initializing model node ${node.id}")
                    try {
                        state.arSession?.let { arSess ->
                            state.childNodes += drawer.drawModelNode(
                                engine,
                                modelLoader,
                                node.type,
                                modelInstances,
                                arSess.createAnchor(Pose(node.translation, node.quaternion)),
                                nodeViewModel,
                                false,
                                position = node.translation,
                                quaternion = node.quaternion,
                                id = node.id,
                                isVisible = false
                            )
                            Log.d("ArPageHandler", "Successfully initialized model node ${node.id}")
                        }
                    } catch (e: Exception) {
                        Log.e("ArPageHandler", "Error initializing model node ${node.id}: ${e.message}")
                    }
                }

                // Наконец, создаем связи между точками
                Log.d("ArPageHandler", "Creating node graph")
                state.nodeGraph = createGraph(state.nodeList)
                if (!state.isAnonymous || state.isBuildingOwner) {
                    Log.d("ArPageHandler", "Drawing node connections")
                    state.nodeList.forEach { node ->
                        if (node.neighbors.isNotEmpty()) {
                            node.neighbors.split(",").map { it.toInt() }.forEach { neighborId ->
                                try {
                                    val neighborNode = state.nodeList.find { it.id == neighborId }
                                    if (neighborNode != null) {
                                        Log.d("ArPageHandler", "Drawing connection between nodes ${node.id} and $neighborId")
                                        val fromDijkstraNode = Dijkstra.Node(
                                            id = node.id.toString(),
                                            position = node.translation
                                        )
                                        val toDijkstraNode = Dijkstra.Node(
                                            id = neighborNode.id.toString(),
                                            position = neighborNode.translation
                                        )
                                        
                                        state.childNodes = drawer.drawLineBetweenNodes(
                                            engine,
                                            modelLoader,
                                            state.arSession,
                                            state.viewAttachmentManager,
                                            state.context,
                                            listOf(fromDijkstraNode, toDijkstraNode),
                                            state.childNodes
                                        )
                                    }
                                } catch (e: Exception) {
                                    Log.e("ArPageHandler", "Error drawing connection: ${e.message}")
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ArPageHandler", "Error in node initialization: ${e.message}")
            } finally {
                viewModel.handleIntent(ArPageIntent.SetFirstLoad(false))
            }
        } else {
            Log.e("ArPageHandler", "No nodes to initialize")
        }
        viewModel.handleIntent(ArPageIntent.SetLoading(false))
    }

    fun onViewCreated(view: View) {
        viewAttachmentManager = ViewAttachmentManager(view.context, view)
        viewModel.state.value.engine = engine
        viewModel.state.value.modelLoader = modelLoader
        viewModel.state.value.viewAttachmentManager = viewAttachmentManager
        viewAttachmentManager?.onResume()
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
        while (path.isNotEmpty() && isPointPassed(currentPosition, path.first().position)) {
            path.removeAt(0)
        }
        state.path = path

        if (state.path.isEmpty()) {
            viewModel.handleIntent(ArPageIntent.ShowSnackbar("Вы достигли конца маршрута"))
        }
    }

    private fun isPointPassed(currentPosition: FloatArray, pointPosition: FloatArray): Boolean {
        val distance = PathHelper().distance(currentPosition, pointPosition)
        //Log.d("distance__", distance.toString())
        return distance < 1.0f
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
                
                viewModel.handleIntent(ArPageIntent.ShowSnackbar("Местоположение установлено"))
            } catch (e: Exception) {
                viewModel.handleIntent(ArPageIntent.ShowSnackbar("Ошибка при установке местоположения: ${e.message}"))
            }
        }
    }
}