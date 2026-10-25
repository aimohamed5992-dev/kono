package sa.kono.app.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.toArgb
import org.osmdroid.config.Configuration
import sa.kono.app.ui.theme.KonoColor
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/** A small non-interactive OSM map with one marker. */
@SuppressLint("ClickableViewAccessibility")
@Composable
fun MiniMap(lat: Double, lng: Double, modifier: Modifier = Modifier) {
    Box(modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { c ->
                Configuration.getInstance().userAgentValue = c.packageName
                MapView(c).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                    controller.setZoom(16.0); controller.setCenter(GeoPoint(lat, lng))
                    overlays.add(Marker(this).apply {
                        position = GeoPoint(lat, lng); setInfoWindow(null)
                        icon = sa.kono.app.ui.customer.mapPin(c, KonoColor.Brown.toArgb(), sa.kono.app.R.drawable.ic_pin_home, 34)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    })
                    setOnTouchListener { _, _ -> true }
                }
            },
            onRelease = { it.onDetach() },
        )
    }
}
