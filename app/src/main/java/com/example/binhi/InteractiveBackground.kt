package com.example.binhi

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

data class Ripple(val center: Offset, val radius: Animatable<Float, *>, val alpha: Animatable<Float, *>)

@Composable
fun InteractiveBackground(modifier: Modifier = Modifier) {
    val ripples = remember { mutableStateListOf<Ripple>() }
    val backgroundColor = Color(0xFFE8F5E9) // A light green color
    val scope = rememberCoroutineScope()

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val ripple = Ripple(
                        center = offset,
                        radius = Animatable(0f),
                        alpha = Animatable(1f)
                    )
                    ripples.add(ripple)

                    scope.launch {
                        coroutineScope {
                            launch {
                                ripple.radius.animateTo(
                                    targetValue = size.width.toFloat(),
                                    animationSpec = tween(durationMillis = 1000)
                                )
                            }
                            launch {
                                ripple.alpha.animateTo(
                                    targetValue = 0f,
                                    animationSpec = tween(durationMillis = 1000)
                                )
                            }
                        }
                        ripples.remove(ripple)
                    }
                }
            }
    ) {
        drawRect(color = backgroundColor)
        ripples.forEach { ripple ->
            drawCircle(
                color = Color.White.copy(alpha = ripple.alpha.value),
                radius = ripple.radius.value,
                center = ripple.center,
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}
