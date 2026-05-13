package com.watchlock.app

import android.content.Context
import androidx.wear.tiles.*
import androidx.wear.tiles.material.*
import androidx.wear.tiles.material.layouts.PrimaryLayout
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.future

/**
 * Wear OS Tile — appears in the tile carousel (swipe right from watch face).
 *
 * To add it: Watch Settings → Tiles → Add tile → "Lock Now"
 *
 * One tap on the tile locks the watch immediately.
 */
class LockTileService : TileService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val LOCK_ACTION = "lock_action"
    }

    override fun onTileRequest(
        requestParams: RequestBuilders.TileRequest
    ): ListenableFuture<TileBuilders.Tile> = serviceScope.future {

        // If the tile was tapped, lock now
        if (requestParams.currentState.lastClickableId == LOCK_ACTION) {
            LockHelper.lockNow(this@LockTileService)
        }

        buildTile(this@LockTileService)
    }

    override fun onResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest
    ): ListenableFuture<ResourceBuilders.Resources> = serviceScope.future {
        ResourceBuilders.Resources.Builder()
            .setVersion("1")
            .build()
    }

    private fun buildTile(context: Context): TileBuilders.Tile {
        val deviceParams = DeviceParametersBuilders.DeviceParameters.Builder()
            .setScreenWidthDp(192)
            .setScreenHeightDp(192)
            .setScreenShape(DeviceParametersBuilders.SCREEN_SHAPE_ROUND)
            .build()

        val lockButton = Button.Builder(
            context,
            ModifiersBuilders.Clickable.Builder()
                .setId(LOCK_ACTION)
                .setOnClick(
                    ActionBuilders.LoadAction.Builder()
                        .setRequestState(
                            StateBuilders.State.Builder().build()
                        ).build()
                )
                .build()
        )
            .setTextContent("🔒")
            .setButtonColors(
                ButtonColors(
                    /* backgroundColor= */ argb(0xFF1A6EFF.toInt()),
                    /* contentColor= */ argb(0xFFFFFFFF.toInt())
                )
            )
            .setSize(DimensionBuilders.dp(72f))
            .build()

        val primaryLayout = PrimaryLayout.Builder(deviceParams)
            .setContent(lockButton)
            .setPrimaryLabelTextContent(
                Text.Builder(context, "Lock Watch")
                    .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                    .setColor(ColorBuilders.argb(0xFFFFFFFF.toInt()))
                    .build()
            )
            .build()

        val timeline = TimelineBuilders.Timeline.Builder()
            .addTimelineEntry(
                TimelineBuilders.TimelineEntry.Builder()
                    .setLayout(
                        LayoutElementBuilders.Layout.Builder()
                            .setRoot(primaryLayout)
                            .build()
                    )
                    .build()
            )
            .build()

        return TileBuilders.Tile.Builder()
            .setResourcesVersion("1")
            .setTimeline(timeline)
            // Refresh tile every 60 minutes (not critical for this tile)
            .setFreshnessIntervalMillis(60 * 60 * 1000L)
            .build()
    }

    private fun argb(color: Int) = ColorBuilders.argb(color)
}
