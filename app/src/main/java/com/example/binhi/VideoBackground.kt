package com.example.binhi

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@UnstableApi
@Composable
fun VideoBackground(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val defaultMediaItem = MediaItem.fromUri("android.resource://" + context.packageName + "/raw/background_video")
            setMediaItem(defaultMediaItem)
            repeatMode = Player.REPEAT_MODE_ALL // Loop the video
            playWhenReady = true // Start playing automatically
            videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            prepare()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(factory = {
        PlayerView(context).apply {
            player = exoPlayer
            useController = false // Hide playback controls
            resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL
        }
    }, modifier = modifier.fillMaxSize())
}
