package com.example.engine3d

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

class GameGLSurfaceView(
    context: Context,
    val renderer: GameGLRenderer,
    private val allowTouchOrbit: Boolean = false
) : GLSurfaceView(context) {

    private var lastTouchX = 0f

    init {
        setEGLContextClientVersion(2)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!allowTouchOrbit) return super.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastTouchX
                lastTouchX = event.x
                renderer.dragYawOffsetDeg = (renderer.dragYawOffsetDeg + dx * 0.45f) % 360f
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}

@Composable
fun Game3DViewport(
    renderer: GameGLRenderer,
    modifier: Modifier = Modifier,
    allowTouchOrbit: Boolean = false
) {
    var surfaceRef: GameGLSurfaceView? = null
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            GameGLSurfaceView(ctx, renderer, allowTouchOrbit).also {
                surfaceRef = it
            }
        },
        update = { view ->
            surfaceRef = view
        }
    )
    DisposableEffect(Unit) {
        onDispose {
            surfaceRef?.onPause()
        }
    }
}
