package com.rawello.mapar.presentation.scenePage

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.rawello.mapar.data.database.NodeViewModel
import com.rawello.mapar.data.models.NodeDb
import com.rawello.mapar.domain.graphHelpers.createGraph
import com.rawello.mapar.domain.pathfinder.Dijkstra
import com.rawello.mapar.presentation.helpers.NodeActions
import com.rawello.mapar.presentation.helpers.NodeDrawers
import com.rawello.mapar.presentation.helpers.PathHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.google.ar.core.Pose
import com.rawello.mapar.data.models.Building
import com.rawello.mapar.data.network.ApiFactory
import com.rawello.mapar.data.network.ApiFactory.apiService
import com.rawello.mapar.data.network.NodeResponse
import androidx.navigation.NavController
import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.rawello.mapar.data.models.QrBuildingInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ArPageViewModel(private val nodeViewModel: NodeViewModel) : ViewModel() {
    private val _state = MutableStateFlow(ArPageState())
    val state: StateFlow<ArPageState>
        get() = _state

    private val drawer = NodeDrawers()
    private val helper = PathHelper()
    private val nodeActions = NodeActions()

    init {
        viewModelScope.launch {
            val nodes = nodeViewModel.getAllNodes().first()
            _state.value = _state.value.copy(
                nodeGraph = createGraph(nodes),
                nodeList = nodes
            )
        }
    }

    fun handleIntent(intent: ArPageIntent) {
        viewModelScope.launch {
            when (intent) {
                is ArPageIntent.ToggleQrMode -> toggleQrMode(intent.isQrMode)
                is ArPageIntent.SelectPoint -> selectPoint(intent.point1, intent.point2)
                is ArPageIntent.PlaceNode -> placeNode(intent.position, intent.quaternion, intent.type, intent.title)
                is ArPageIntent.DeleteNode -> {
                    deleteNode(intent.nodeId)
                    redrawLinks()
                }
                is ArPageIntent.LinkNodes -> {
                    linkNodes(intent.nodeId1, intent.nodeId2)
                    redrawLinks()
                }
                is ArPageIntent.BuildPath -> buildPath()
                is ArPageIntent.ShowSnackbar -> showSnackbar(intent.message)
                is ArPageIntent.SetReadyToPlace -> setReadyToPlace(intent.readyToPlace)
                is ArPageIntent.SetLinking -> setLinking(intent.linking)
                is ArPageIntent.SetFirstLoad -> setFirstLoad(intent.firstLoad)
                is ArPageIntent.SetArSession -> setArSession(intent.arSession)
                is ArPageIntent.SetFrame -> setFrame(intent.frame)
                is ArPageIntent.SetModel -> setModel(intent.model)
                is ArPageIntent.SetShowBottomSheet -> setShowBottomSheet(intent.showBottomSheet)
                is ArPageIntent.SetButtonText -> setButtonText(intent.buttonText1, intent.buttonText2)
                is ArPageIntent.SetNodeList -> setNodeList(intent.nodeList)
                is ArPageIntent.SetLoading -> setLoading(intent.isLoading)
                is ArPageIntent.SetDistanceToTurn -> setDistanceToTurn(intent.distanceToTurn)
                is ArPageIntent.SetTotalDistance -> setTotalDistance(intent.totalDistance)
                is ArPageIntent.SetTurnDirection -> setTurnDirection(intent.turnDirection)
                is ArPageIntent.ToggleEditMode -> toggleEditMode(intent.isEditMode)
                is ArPageIntent.SetBuildingId -> setBuildingId(intent.buildingId)
                is ArPageIntent.SetBuildingName -> setBuildingName(intent.buildingName)
                is ArPageIntent.SetUserLocation -> setUserLocation(intent.nodeId, intent.navController)
                is ArPageIntent.ShowQrDialog -> showQrDialog(intent.show)
                is ArPageIntent.SetQrBitmap -> setQrBitmap(intent.bitmap)
                else -> {} // Handle other intents
            }
        }
    }

    private fun setBuildingId(buildingId: Int) {
        _state.value = _state.value.copy(buildingId = buildingId)
    }

    private fun setBuildingName(buildingName: String) {
        _state.value = _state.value.copy(buildingName = buildingName)
    }

    fun resetState() {
        _state.value = ArPageState()
    }

    private fun toggleQrMode(isQrMode: Boolean) {
        _state.value = _state.value.copy(isQrMode = isQrMode)
    }

    private fun selectPoint(point1: String?, point2: String?) {
        _state.value = _state.value.copy(selectedPoint1 = point1, selectedPoint2 = point2)
    }

    private suspend fun placeNode(position: FloatArray, quaternion: FloatArray, type: String, title: String) {
        val newNode = NodeDb(
            translation = position,
            quaternion = quaternion,
            type = type,
            title = title,
            neighbors = "",
            buildingId = 1,
            nameNode = "node"
        )
        nodeViewModel.insertNode(newNode)
        _state.value = _state.value.copy(nodeList = nodeViewModel.getAllNodes().first())
    }

    private suspend fun deleteNode(nodeId: Int) {
        val nodeToDelete = state.value.childNodes.find { it.name == nodeId.toString() }
        if (nodeToDelete != null) {
            nodeActions.deleteNode(state.value.nodeList, nodeToDelete, nodeViewModel)
            _state.value = _state.value.copy(nodeList = nodeViewModel.getAllNodes().first())
        }
    }

    private suspend fun linkNodes(nodeId1: Int, nodeId2: Int) {
        val node1 = _state.value.nodeList.find { it.id == nodeId1 }
        val node2 = _state.value.nodeList.find { it.id == nodeId2 }
        if (node1 != null && node2 != null) {
            nodeViewModel.updateNode(node1.copy(neighbors = node1.neighbors + "," + node2.id))
            nodeViewModel.updateNode(node2.copy(neighbors = node2.neighbors + "," + node1.id))
            _state.value = _state.value.copy(nodeList = nodeViewModel.getAllNodes().first())
        }
    }

    private fun buildPath() {
        helper.clearOldArrows(_state.value.childNodes)

        if (_state.value.selectedPoint1 != null && _state.value.selectedPoint2 != null && !_state.value.selectedPoint1.equals(_state.value.selectedPoint2)) {
            val startNode = _state.value.nodeGraph.getGraph().keys.find { it.id == _state.value.selectedPoint1!! }
            val endNode = _state.value.nodeGraph.getGraph().keys.find { it.id == _state.value.selectedPoint2!! }

            if (startNode != null && endNode != null) {
                val path = Dijkstra().dijkstra(_state.value.nodeGraph.getGraph(), startNode, endNode)

                if (path.size >= 2) {
                    Log.d("size", state.value.childNodes.size.toString())
                    Log.d("size1", _state.value.childNodes.size.toString())
                    state.value.childNodes = drawer.drawPathSegment(
                        _state.value.engine!!,
                        _state.value.modelLoader!!,
                        _state.value.arSession!!,
                        _state.value.viewAttachmentManager!!,
                        _state.value.context,
                        path,
                        _state.value.childNodes
                    )
                    Log.d("size", state.value.childNodes.size.toString())
                    Log.d("size1", _state.value.childNodes.size.toString())

                    _state.value = _state.value.copy(path = path)
                    Log.d("path", "Path updated: ${path.size} nodes")
                } else if (path.isNotEmpty()) {
                    Log.d("size", state.value.childNodes.size.toString())
                    Log.d("size1", _state.value.childNodes.size.toString())

                    state.value.childNodes = drawer.drawPathSegment(
                        _state.value.engine!!,
                        _state.value.modelLoader!!,
                        _state.value.arSession!!,
                        _state.value.viewAttachmentManager!!,
                        _state.value.context,
                        listOf(startNode, endNode),
                        _state.value.childNodes
                    )
                    Log.d("size", state.value.childNodes.size.toString())
                    Log.d("size1",_state.value.childNodes.size.toString())

                    _state.value = _state.value.copy(path = path)
                    Log.d("path", "Path updated: ${path.size} nodes")
                } else {
                    handleIntent(ArPageIntent.ShowSnackbar("Не удалось построить маршрут"))
                }
            } else {
                handleIntent(ArPageIntent.ShowSnackbar("Не удалось найти начальную или конечную точку"))
            }
        } else {
            handleIntent(ArPageIntent.ShowSnackbar("Выберите обе точки для построения маршрута"))
        }
    }

    private fun showSnackbar(message: String) {
        _state.value = _state.value.copy(snackbarMessage = message)
    }

    private fun setReadyToPlace(readyToPlace: Boolean) {
        _state.value = _state.value.copy(readyToPlace = readyToPlace)
    }

    private fun setLinking(linking: Boolean) {
        _state.value = _state.value.copy(linking = linking)
    }

    private fun setFirstLoad(firstLoad: Boolean) {
        _state.value = _state.value.copy(firstLoad = firstLoad)
    }

    private fun setArSession(arSession: Session?) {
        _state.value.arSession = arSession
    }

    private fun setFrame(frame: Frame?) {
        _state.value.frame = frame
    }

    private fun setModel(model: String) {
        _state.value = _state.value.copy(model = model)
    }

    private fun setShowBottomSheet(showBottomSheet: Boolean) {
        _state.value = _state.value.copy(showBottomSheet = showBottomSheet)
    }

    private fun setButtonText(buttonText1: String, buttonText2: String) {
        _state.value = _state.value.copy(buttonText1 = buttonText1, buttonText2 = buttonText2)
    }

    private fun setNodeList(nodeList: List<NodeDb>) {
        _state.value = _state.value.copy(nodeList = nodeList)
    }

    private fun setLoading(isLoading: Boolean) {
        _state.value = _state.value.copy(isLoading = isLoading)
    }

    private fun setDistanceToTurn(distanceToTurn: Float) {
        _state.value = _state.value.copy(distanceToTurn = distanceToTurn)
    }

    private fun setTotalDistance(totalDistance: Float) {
        _state.value = _state.value.copy(totalDistance = totalDistance)
    }

    private fun setTurnDirection(turnDirection: String) {
        _state.value = _state.value.copy(turnDirection = turnDirection)
    }

    private fun redrawLinks() {
        viewModelScope.launch {
            val currentNodeList = _state.value.nodeList
            val currentLinks = _state.value.childNodes.filter { it.name!!.contains("_") }

            currentLinks.forEach { link ->
                _state.value.childNodes.remove(link)
            }

            currentNodeList.forEach { node ->
                node.neighbors.split(",").forEach { neighborId ->
                    val neighborNode = currentNodeList.find { it.id == neighborId.toInt() }
                    if (neighborNode != null) {
                        val linkName = "${node.id}_${neighborNode.id}"
                        val position = node.translation
                        val orientation = helper.calculateOrientation(node.translation, neighborNode.translation)

                        val newLink = drawer.drawStrip(
                            _state.value.engine!!,
                            _state.value.modelLoader!!,
                            _state.value.arSession!!.createAnchor(Pose(position, orientation)),
                            _state.value.viewAttachmentManager!!,
                            _state.value.context!!,
                            position,
                            orientation,
                            linkName
                        ) {}

                        _state.value.childNodes.add(newLink)
                    }
                }
            }
        }
    }

    private fun toggleEditMode(isEditMode: Boolean) {
        _state.value = _state.value.copy(isEditMode = isEditMode)
    }

    private fun setUserLocation(nodeId: Int, navController: NavController) {
        viewModelScope.launch {
            try {
                val handler = ArPageHandler(
                    viewModel = this@ArPageViewModel,
                    nodeViewModel = nodeViewModel,
                    context = state.value.context!!,
                    engine = state.value.engine!!,
                    modelLoader = state.value.modelLoader!!,
                    childNodes = state.value.childNodes
                )
                handler.setUserLocation(nodeId, state.value, navController)
            } catch (e: Exception) {
                handleIntent(ArPageIntent.ShowSnackbar("Ошибка при установке местоположения: ${e.message}"))
            }
        }
    }

    fun saveBuilding() {
        viewModelScope.launch {
            try {
                val building = Building(
                    id = state.value.buildingId,
                    name = state.value.buildingName,
                    address = ""
                )

                updateBuilding(building)

                state.value.nodeList.forEach { node ->
                    updateNode(node)
                }

                handleIntent(ArPageIntent.ShowSnackbar("Здание успешно сохранено"))
            } catch (e: Exception) {
                handleIntent(ArPageIntent.ShowSnackbar("Ошибка при сохранении здания: ${e.message}"))
            }
        }
    }

    fun createBuilding(buildingName: String) {
        viewModelScope.launch {
            try {
                val building = Building(name = buildingName, address = "")
                val createdBuilding = ApiFactory.apiService.createBuilding(building)
                _state.value = _state.value.copy(buildingId = createdBuilding.id, buildingName = createdBuilding.name)
            } catch (e: Exception) {
                // Обработка ошибок
                handleIntent(ArPageIntent.ShowSnackbar("Ошибка при создании здания: ${e.message}"))
            }
        }
    }

    fun updateBuilding(building: Building) {
        viewModelScope.launch {
            try {
                val updatedBuilding = ApiFactory.apiService.updateBuilding(building.id.toLong(), building)
                _state.value = _state.value.copy(buildingId = updatedBuilding.id, buildingName = updatedBuilding.name)
            } catch (e: Exception) {
                handleIntent(ArPageIntent.ShowSnackbar("Ошибка при обновлении здания: ${e.message}"))
            }
        }
    }

    fun createNode(node: NodeDb) {
        viewModelScope.launch {
            try {
                val nodeResponse = convertNodeDbToNodeResponse(node)
                val createdNode = ApiFactory.apiService.createNode(nodeResponse)
                val updatedNodeList = _state.value.nodeList.toMutableList()
                updatedNodeList.add(convertNodeResponseToNodeDb(createdNode))
                _state.value = _state.value.copy(nodeList = updatedNodeList)
            } catch (e: Exception) {
                handleIntent(ArPageIntent.ShowSnackbar("Ошибка при создании точки: ${e.message}"))
            }
        }
    }

    fun updateNode(node: NodeDb) {
        viewModelScope.launch {
            try {
                val nodeResponse = convertNodeDbToNodeResponse(node)
                val updatedNode = ApiFactory.apiService.updateNode(node.id.toLong(), nodeResponse)
                val updatedNodeList = _state.value.nodeList.toMutableList()
                val index = updatedNodeList.indexOfFirst { it.id == updatedNode.id }
                if (index != -1) {
                    updatedNodeList[index] = convertNodeResponseToNodeDb(updatedNode)
                    _state.value = _state.value.copy(nodeList = updatedNodeList)
                }
            } catch (e: Exception) {
                handleIntent(ArPageIntent.ShowSnackbar("Ошибка при обновлении точки: ${e.message}"))
            }
        }
    }

    fun convertNodeDbToNodeResponse(nodeDb: NodeDb): NodeResponse {
        return NodeResponse(
            id = nodeDb.id,
            name = nodeDb.nameNode.toInt(),
            translation = nodeDb.translation.joinToString(","),
            quaternion = nodeDb.quaternion.joinToString(","),
            modelPath = nodeDb.type,
            title = nodeDb.title,
            neighbors = nodeDb.neighbors
        )
    }

    fun convertNodeResponseToNodeDb(nodeResponse: NodeResponse): NodeDb {
        return NodeDb(
            id = nodeResponse.id,
            buildingId = state.value.buildingId.toLong(),
            nameNode = nodeResponse.name.toString(),
            translation = nodeResponse.translation.split(",").map { it.toFloat() }.toFloatArray(),
            quaternion = nodeResponse.quaternion.split(",").map { it.toFloat() }.toFloatArray(),
            type = nodeResponse.modelPath,
            title = nodeResponse.title,
            neighbors = nodeResponse.neighbors
        )
    }

    private fun showQrDialog(show: Boolean) {
        _state.value = _state.value.copy(showQrDialog = show)
    }

    private fun setQrBitmap(bitmap: android.graphics.Bitmap?) {
        _state.value = _state.value.copy(qrBitmap = bitmap)
    }

    fun generateQrCodeForNode(node: NodeDb, buildingName: String, buildingAddress: String): Bitmap {
        val qrInfo = QrBuildingInfo(
            buildingId = node.buildingId.toInt(),
            buildingName = buildingName,
            buildingAddress = buildingAddress,
            nodeId = node.id.toInt(),
            nodeName = node.nameNode,
            nodeTitle = node.title
        )
        
        val jsonString = Json.encodeToString(qrInfo)
        val hints = mutableMapOf<EncodeHintType, Any>().apply {
            put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H)
            put(EncodeHintType.MARGIN, 2)
        }
        
        val qrCodeWriter = QRCodeWriter()
        val bitMatrix = qrCodeWriter.encode(jsonString, BarcodeFormat.QR_CODE, 512, 512, hints)
        
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        
        return bitmap
    }

    fun showQrCodeForNode(node: NodeDb) {
        viewModelScope.launch {
            try {
                val building = getBuildingById(node.buildingId)
                val qrBitmap = generateQrCodeForNode(node, building.name, building.address)
                _state.update { it.copy(
                    qrBitmap = qrBitmap,
                    showQrDialog = true
                ) }
            } catch (e: Exception) {
                _state.update { it.copy(
                    snackbarMessage = "Ошибка при генерации QR-кода: ${e.message}"
                ) }
            }
        }
    }

    suspend fun getBuildingById(buildingId: Long): Building {
        return withContext(Dispatchers.IO) {
            // Здесь должен быть ваш код для получения информации о здании
            // Временно возвращаем моковые данные
            Building(buildingId.toInt(), "Building 1", "Address 1")
        }
    }
}