package com.rawello.mapar.presentation.scenePage

import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.rawello.mapar.data.models.NodeDb
import androidx.navigation.NavController

sealed class ArPageIntent {
    data class ToggleQrMode(val isQrMode: Boolean) : ArPageIntent()
    data class SelectPoint(val point1: String?, val point2: String?) : ArPageIntent()
    data class PlaceNode(val position: FloatArray, val quaternion: FloatArray, val type: String, val title: String) : ArPageIntent() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as PlaceNode

            if (!position.contentEquals(other.position)) return false
            if (!quaternion.contentEquals(other.quaternion)) return false
            if (type != other.type) return false
            if (title != other.title) return false

            return true
        }

        override fun hashCode(): Int {
            var result = position.contentHashCode()
            result = 31 * result + quaternion.contentHashCode()
            result = 31 * result + type.hashCode()
            result = 31 * result + title.hashCode()
            return result
        }
    }

    data class DeleteNode(val nodeId: Int) : ArPageIntent()
    data class LinkNodes(val nodeId1: Int, val nodeId2: Int) : ArPageIntent()
    data object BuildPath : ArPageIntent()
    data class ShowSnackbar(val message: String) : ArPageIntent()
    data class SetReadyToPlace(val readyToPlace: Boolean) : ArPageIntent()
    data class SetLinking(val linking: Boolean) : ArPageIntent()
    data class SetFirstLoad(val firstLoad: Boolean) : ArPageIntent()
    data class SetArSession(val arSession: Session?) : ArPageIntent()
    data class SetFrame(val frame: Frame?) : ArPageIntent()
    data class SetModel(val model: String) : ArPageIntent()
    data class SetShowBottomSheet(val showBottomSheet: Boolean) : ArPageIntent()
    data class SetButtonText(val buttonText1: String, val buttonText2: String) : ArPageIntent()
    data class SetNodeList(val nodeList: List<NodeDb>) : ArPageIntent()
    data class SetLoading(val isLoading: Boolean) : ArPageIntent()
    data class SetDistanceToTurn(val distanceToTurn: Float) : ArPageIntent()
    data class SetTotalDistance(val totalDistance: Float) : ArPageIntent()
    data class SetTurnDirection(val turnDirection: String) : ArPageIntent()
    data class ToggleEditMode(val isEditMode: Boolean) : ArPageIntent()
    data class SetBuildingId(val buildingId: Int) : ArPageIntent()
    data class SetBuildingName(val buildingName: String) : ArPageIntent()
    data class SetUserLocation(val nodeId: Int, val navController: NavController) : ArPageIntent()
    data class ShowQrDialog(val show: Boolean) : ArPageIntent()
    data class SetQrBitmap(val bitmap: android.graphics.Bitmap?) : ArPageIntent()
    data class SetDestinationNodeId(val nodeId: Int?) : ArPageIntent()
}