package com.zybooks.appmobilefinalproject

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.node.ModelNode
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale

class ArViewActivity : AppCompatActivity() {

    private lateinit var arSceneView: ARSceneView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ar_view)

        arSceneView = findViewById(R.id.arSceneView)

        // Tap to place a cube
        arSceneView.onTouchAr = { hitResults, _ ->
            val hit = hitResults.firstOrNull() ?: return@onTouchAr
            val anchor = hit.createAnchor()

            val node = ModelNode(
                modelInstance = null,
                scaleToUnits = 0.1f,
                centerOrigin = Position(0.0f, 0.05f, 0.0f)
            )

            node.position = Position(0f, 0f, -0.3f)
            node.rotation = Rotation(0f, 0f, 0f)
            node.scale = Scale(0.1f)

            arSceneView.addChild(node)
            node.anchor = anchor
        }
    }

    override fun onResume() {
        super.onResume()
        arSceneView.resume()
    }

    override fun onPause() {
        super.onPause()
        arSceneView.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        arSceneView.destroy()
    }
}
