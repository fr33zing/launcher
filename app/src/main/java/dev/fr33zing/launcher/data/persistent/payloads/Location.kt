package dev.fr33zing.launcher.data.persistent.payloads

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.Keep
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat.startActivity
import androidx.room.Entity
import androidx.room.Ignore
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.ui.util.UserEditable
import java.math.RoundingMode
import java.net.URLDecoder
import java.net.URLEncoder
import java.text.DecimalFormat

@Keep
@Entity
class Location(
    payloadId: Int,
    nodeId: Int,
    @UserEditable(label = "Geo URI", locked = true) var geoUri: String = ""
) : Payload(payloadId, nodeId) {

    @Ignore var latitude = mutableStateOf("")
    @Ignore var longitude = mutableStateOf("")
    @Ignore var zoom = mutableStateOf("")
    @Ignore var query = mutableStateOf("")

    init {
        refresh()
    }

    fun refresh(useUri: String? = null, except: List<String>? = null) {
        val uriStr = useUri ?: geoUri
        val uri =
            Uri.parse(
                if (uriStr.startsWith("geo:")) {
                    if (uriStr.startsWith("geo://")) uriStr else "geo://${uriStr.substring(4)}"
                } else "geo://$uriStr"
            )
        val latLon = uri.host?.split(',')
        if (except?.contains("latitude") != true)
            latitude.value = latLon?.getOrNull(0)?.toDoubleOrNull()?.toString() ?: ""
        if (except?.contains("longitude") != true)
            longitude.value = latLon?.getOrNull(1)?.toDoubleOrNull()?.toString() ?: ""
        if (except?.contains("zoom") != true)
            zoom.value =
                (uri.getQueryParameter("z") ?: uri.getQueryParameter("zoom"))
                    ?.toIntOrNull()
                    ?.toString() ?: ""
        if (except?.contains("query") != true)
            query.value =
                URLDecoder.decode(
                    uri.getQueryParameter("q") ?: uri.getQueryParameter("query") ?: "",
                    "utf-8"
                )
        geoUri = toUri().toString()
    }

    fun toUri(): Uri =
        Uri.parse(
            buildString {
                val df = DecimalFormat("#.######")
                df.roundingMode = RoundingMode.FLOOR

                append("geo://")
                append(latitude.value.toDoubleOrNull()?.let { df.format(it) })
                append(",")
                append(longitude.value.toDoubleOrNull()?.let { df.format(it) })
                if (zoom.value.isNotEmpty()) {
                    append("?z=")
                    append(zoom.value)
                    if (query.value.isNotEmpty()) {
                        append("&q=")
                        append(URLEncoder.encode(query.value, "utf-8"))
                    }
                } else if (query.value.isNotEmpty()) {
                    append("?q=")
                    append(URLEncoder.encode(query.value, "utf-8"))
                }
            }
        )

    data class Status(val validScheme: Boolean, val validCoordinates: Boolean) {
        val valid: Boolean
            get() = validScheme && validCoordinates
    }

    val status: Status
        get() {
            fun coordinateValid(coordinate: Double?): Boolean =
                coordinate != null && coordinate >= -180 && coordinate <= 180
            fun stringCoordinateValid(coordinate: String?): Boolean =
                coordinateValid(coordinate?.toDoubleOrNull())

            val uri = Uri.parse(geoUri)
            val latLon = uri.host?.split(',')
            return Status(
                validScheme = uri.scheme == "geo",
                validCoordinates =
                    latLon?.size == 2 &&
                        stringCoordinateValid(latLon.getOrNull(0)) &&
                        stringCoordinateValid(latLon.getOrNull(1))
            )
        }

    override fun activate(db: AppDatabase, context: Context) {
        val mapIntent = Intent(Intent.ACTION_VIEW, toUri())
        startActivity(context, mapIntent, null)
    }
}
