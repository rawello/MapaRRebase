package com.rawello.mapar.presentation.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.core.content.ContextCompat
import com.google.android.filament.Engine
import com.google.ar.core.Anchor
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.sceneform.rendering.ViewAttachmentManager
import com.rawello.mapar.R
import com.rawello.mapar.data.database.NodeViewModel
import com.rawello.mapar.data.models.NodeDb
import com.rawello.mapar.domain.pathfinder.Dijkstra
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.math.Scale
import io.github.sceneview.math.toFloat4
import io.github.sceneview.math.toPosition
import io.github.sceneview.model.ModelInstance
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import io.github.sceneview.node.ViewNode
import kotlin.math.sqrt

class NodeDrawers {
    val helper = PathHelper()
    val actioner = NodeActions()

    fun drawModelNode(
        engine: Engine,
        modelLoader: ModelLoader,
        model: String,
        modelInstances: MutableList<ModelInstance>,
        anchor: Anchor,
        viewModel: NodeViewModel,
        needSave: Boolean,
        position: FloatArray?,
        quaternion: FloatArray?,
        id: Int,
        isVisible: Boolean
    ): AnchorNode {
        val anchorNode = AnchorNode(engine = engine, anchor = anchor).apply {
            if (position != null && quaternion != null) {
                this.pose = Pose(position, quaternion)
            }
        }

        val modelNode = ModelNode(
            modelInstance = modelInstances.apply {
                if (isEmpty()) {
                    this += modelLoader.createInstancedModel(model, 1)
                }
            }.removeAt(modelInstances.apply {
                if (isEmpty()) {
                    this += modelLoader.createInstancedModel(model, 1)
                }
            }.lastIndex),
            scaleToUnits = 0.2f
        ).apply {
            isEditable = true
            editableScaleRange = 0.2f..0.75f
            isPositionEditable = false
            this.isVisible = isVisible

            if (needSave) {
                actioner.saveNode(viewModel, anchorNode, model, "models/box.glb")
            }

            this.name = id.toString()
            anchorNode.name = id.toString()

            anchorNode.addChildNode(this)
            return anchorNode
        }
    }

    @SuppressLint("SetTextI18n")
    fun drawView(
        engine: Engine,
        modelLoader: ModelLoader,
        viewAttachmentManager: ViewAttachmentManager,
        context: Context,
        anchor: Anchor,
        viewModel: NodeViewModel,
        needSave: Boolean,
        title: String
    ): AnchorNode {
        val anchorNode = AnchorNode(engine = engine, anchor = anchor)
        val viewNode = ViewNode(engine, modelLoader, viewAttachmentManager).apply {
            isEditable = true
            editableScaleRange = 0.2f..0.75f
        }

        viewNode.loadView(context, R.layout.cardtext, onLoaded = { _, view ->
            view.findViewById<TextView>(R.id.signTextView).text = title

            if (needSave) {
                actioner.saveNode(viewModel, anchorNode, "view", title)
            }

            anchorNode.addChildNode(viewNode)
        },
            onError = {
                Log.d("error draw", it.toString())
                if (it.message != null)
                    Log.d("error draw", it.message!!)
            })

        viewNode.scale = Scale(-1f, 1f, 1f)
        return anchorNode
    }


    fun drawPathSegment(
        engine: Engine,
        modelLoader: ModelLoader,
        arSession: Session?,
        viewAttachmentManager: ViewAttachmentManager?,
        context: Context?,
        path: List<Dijkstra.Node>,
        childNodes: SnapshotStateList<Node>
    ): SnapshotStateList<Node> {
        if (path.size < 2) return childNodes

        val bezierPoints = helper.generateBezierPath(path)

        for (i in 0 until bezierPoints.size - 1) {
            val modelPosition = bezierPoints[i]
            val nextPosition = bezierPoints[i + 1]
            val orientation = helper.calculateOrientation(modelPosition, nextPosition)

            viewAttachmentManager?.toString()?.let { Log.d("tag", it) }
            viewAttachmentManager?.let { vam ->
                context?.let { ctx ->
                    arSession?.let { session ->
                        childNodes += drawViewNodePaths(
                            engine,
                            modelLoader,
                            session.createAnchor(Pose(modelPosition, orientation)),
                            vam,
                            ctx,
                            modelPosition,
                            orientation,
                        ) { viewNode ->
                            Log.d("tag", viewNode.toString())
                        }
                    }
                }
            }
        }
        Log.d("updateChildNodes", childNodes.size.toString())
        return childNodes
    }

    private fun <T> List<T>.toSnapshotStateList(): SnapshotStateList<T> {
        val snapshotStateList = SnapshotStateList<T>()
        snapshotStateList.addAll(this)
        return snapshotStateList
    }

    fun drawViewNodePaths(
        engine: Engine,
        modelLoader: ModelLoader,
        anchor: Anchor,
        viewAttachmentManager: ViewAttachmentManager,
        context: Context,
        position: FloatArray?,
        quaternion: FloatArray?,
        onViewNodeLoaded: (AnchorNode?) -> Unit
    ): AnchorNode {
        Log.d("tag", "path path path")
        val anchorNode = AnchorNode(engine = engine, anchor = anchor).apply {
            if (position != null && quaternion != null) {
                pose = Pose(position, quaternion)
                Log.d("AnchorNodePose", "Position: ${position.joinToString(", ")}, Quaternion: ${quaternion.joinToString(", ")}")
            }
        }


        val viewNode = ViewNode(engine, modelLoader, viewAttachmentManager).apply {
            isEditable = false

            loadView(context, R.layout.route_node, onLoaded = { _, view ->
                val icon = view.findViewById<ImageView>(R.id.imageView)
                icon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_up_arrow))

                this.name = "arrow"
                anchorNode.name = "arrow"
            },
                onError = {
                    Log.d("error draw", it.toString())
                    if (it.message != null)
                        Log.d("error draw", it.message!!)
                    onViewNodeLoaded(null)
                })
            this.scale = Scale(1.5f, 1.5f, 1.5f)
            this.isVisible = true

            anchorNode.addChildNode(this)
        }
        return anchorNode
    }

    fun drawLineBetweenNodes(
        engine: Engine,
        modelLoader: ModelLoader,
        arSession: Session?,
        viewAttachmentManager: ViewAttachmentManager?,
        context: Context?,
        path: List<Dijkstra.Node>,
        childNodes: SnapshotStateList<Node>
    ): SnapshotStateList<Node> {
        if (path.size < 2) return childNodes

        val bezierPoints = helper.generateBezierPath(path)

        for (i in 0 until bezierPoints.size - 1) {
            val modelPosition = bezierPoints[i]
            val nextPosition = bezierPoints[i + 1]
            val orientation = helper.calculateOrientation(modelPosition, nextPosition)

            viewAttachmentManager?.let { vam ->
                context?.let { ctx ->
                    arSession?.let { session ->
                        try {
                            val linkName = "${path[0].id}_${path[1].id}"
                            childNodes += drawStrip(
                                engine,
                                modelLoader,
                                session.createAnchor(Pose(modelPosition, orientation)),
                                vam,
                                ctx,
                                modelPosition,
                                orientation,
                                linkName
                            ) {}
                        }catch (e: Exception){
                            e.message?.let { Log.e("ex", it) }
                        }
                    }
                }
            }
        }
        return childNodes
    }

    fun drawStrip(
        engine: Engine,
        modelLoader: ModelLoader,
        anchor: Anchor,
        viewAttachmentManager: ViewAttachmentManager,
        context: Context,
        position: FloatArray?,
        quaternion: FloatArray?,
        linkName: String,
        onViewNodeLoaded: (AnchorNode?) -> Unit
    ): AnchorNode {
        val anchorNode = AnchorNode(engine = engine, anchor = anchor).apply {
            if (position != null && quaternion != null) {
                pose = Pose(position, quaternion)
                Log.d("AnchorNodePose", "Position: ${position.joinToString(", ")}, Quaternion: ${quaternion.joinToString(", ")}")
            }
        }

        val viewNode = ViewNode(engine, modelLoader, viewAttachmentManager).apply {
            isEditable = true

            loadView(context, R.layout.route_link, onLoaded = { instance, view ->
                val icon = view.findViewById<ImageView>(R.id.imageView)
                icon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_up_link))

                this.name = linkName
                anchorNode.name = linkName
                this.scale = Scale(1.5f, 1.5f, 1.5f)

                Log.e("tag", "linked")
                anchorNode.addChildNode(this)
            },
                onError = {
                    if (it.message != null)
                        Log.e("error draw", it.message!!)
                    onViewNodeLoaded(null)
                })

            Log.e("tag", "linked")
            return anchorNode
        }
    }
}