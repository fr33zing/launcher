package dev.fr33zing.launcher.data.persistent.payloads

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.Keep
import androidx.core.content.ContextCompat.startActivity
import androidx.room.Entity
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.ui.components.NoticeKind
import dev.fr33zing.launcher.ui.components.sendNotice
import dev.fr33zing.launcher.ui.util.UserEditable

val UrlRegex =
    (buildString {
            append("^((http|https)://)(www.)?")
            append("[a-zA-Z0-9@:%._\\+~#?&//=]")
            append("{2,256}\\.[a-z]")
            append("{2,6}\\b([-a-zA-Z0-9@:%")
            append("._\\+~#?&//=]*)$")
        })
        .toRegex()

@Keep
@Entity
class WebLink(payloadId: Int, nodeId: Int, @UserEditable("URL") var url: String = "") :
    Payload(payloadId, nodeId) {

    val validUrl: Boolean
        get() = UrlRegex.matches(url)

    override fun activate(db: AppDatabase, context: Context) = launch(context)

    fun launch(context: Context) {
        if (validUrl) {
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(context, browserIntent, null)
            } catch (e: Exception) {
                sendNotice(
                    "weblink-launch-failed:${nodeId}",
                    "Failed to launch web browser. Error: ${e.message}.",
                    NoticeKind.Error
                )
            }
        } else {
            sendNotice(
                "weblink-launch-invalid:${nodeId}",
                "Cannot launch web browser because the URL is invalid."
            )
        }
    }
}
