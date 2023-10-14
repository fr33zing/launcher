package dev.fr33zing.launcher.data.persistent.payloads

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat.startActivity
import androidx.room.Entity
import androidx.room.Ignore
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.ui.util.UserEditable
import java.net.URLDecoder
import java.net.URLEncoder

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

    fun refresh(useUri: String = geoUri, except: String? = null) {
        val uri =
            Uri.parse(
                if (useUri.startsWith("geo:")) {
                    if (useUri.startsWith("geo://")) useUri else "geo://${useUri.substring(4)}"
                } else "geo://$useUri"
            )
        val latLon = uri.host?.split(',')
        if (except != "latitude")
            latitude.value = latLon?.getOrNull(0)?.toFloatOrNull()?.toString() ?: "0.0"
        if (except != "longitude")
            longitude.value = latLon?.getOrNull(1)?.toFloatOrNull()?.toString() ?: "0.0"
        if (except != "zoom")
            zoom.value =
                (uri.getQueryParameter("z") ?: uri.getQueryParameter("zoom"))
                    ?.toIntOrNull()
                    ?.toString() ?: ""
        if (except != "query")
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
                append("geo://")
                append(latitude.value.toFloatOrNull() ?: "0.0")
                append(",")
                append(longitude.value.toFloatOrNull() ?: "0.0")
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

    data class Status(val validScheme: Boolean)

    val status: Status
        get() {
            val uri = Uri.parse(geoUri)
            return Status(validScheme = uri.scheme == "geo")
        }

    override fun activate(db: AppDatabase, context: Context) {
        val mapIntent = Intent(Intent.ACTION_VIEW, toUri())
        startActivity(context, mapIntent, null)
    }
}
