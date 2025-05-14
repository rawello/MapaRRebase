package com.rawello.mapar.presentation.scenePage

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.rawello.mapar.data.models.NodeDb
import com.rawello.mapar.domain.pathfinder.Dijkstra
import com.rawello.mapar.domain.pathfinder.Graph
import com.google.android.filament.Engine
import com.google.ar.sceneform.rendering.ViewAttachmentManager
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.node.Node
import androidx.compose.runtime.snapshots.SnapshotStateList

data class ArPageState(
    val isQrMode: Boolean = false,
    val nodeList: List<NodeDb> = emptyList(),
    var nodeGraph: Graph = Graph(),
    var selectedPoint1: String? = null,
    var selectedPoint2: String? = null,
    var path: List<Dijkstra.Node> = emptyList(),
    val snackbarMessage: String? = null,
    val readyToPlace: Boolean = false,
    val linking: Boolean = false,
    val firstLoad: Boolean = true,
    var arSession: Session? = null,
    var frame: Frame? = null,
    val model: String = "",
    val showBottomSheet: Boolean = false,
    var buttonText1: String = "Откуда",
    var buttonText2: String = "Куда",
    var engine: Engine? = null,
    var modelLoader: ModelLoader? = null,
    var viewAttachmentManager: ViewAttachmentManager? = null,
    var context: Context? = null,
    var childNodes: SnapshotStateList<Node> = SnapshotStateList(),
    var isLoading: Boolean = false,
    val distanceToTurn: Float = 0f,
    val totalDistance: Float = 0f,
    val turnDirection: String = "Прямо",
    var isAnonymous: Boolean = true,
    var isBuildingOwner: Boolean = false,
    var isEditMode: Boolean = false,
    var buildingId: Int = 0,
    var buildingName: String = "",
    var showQrDialog: Boolean = false,
    var qrBitmap: Bitmap? = null,
    var destinationNodeId: Int? = null
)