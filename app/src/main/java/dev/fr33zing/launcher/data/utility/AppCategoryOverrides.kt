package dev.fr33zing.launcher.data.utility

/**
 * Get overrides for applications' categories so that some applications can have their default
 * category set explicitly.
 */
fun getApplicationCategoryOverrides() =
    applicationCategoryOverrideDefinitions()
        .flatMap { (category, apps) -> apps.map { app -> app to category } }
        .toMap()

// TODO remove apps that nobody uses
// TODO add a lot more apps
private fun applicationCategoryOverrideDefinitions() =
    mapOf(
        "App Sources" to
            arrayOf(
                "app.grapheneos.apps",
                "app.skydroid",
                "cf.vojtechh.apkmirror",
                "com.aefyr.sai.fdroid",
                "com.apkmirror.helper.prod",
                "com.apkpure.aegon",
                "com.apkupdater",
                "com.aurora.store",
                "com.looker.droidify",
                "com.machiav3lli.fdroid",
                "com.tomclaw.appsend",
                "de.apkgrabber",
                "dev.imranr.obtainium",
                "eu.bubu1.fdroidclassic",
                "ie.defo.ech_apps",
                "in.sunilpaulmathew.izzyondroid",
                "nya.kitsunyan.foxydroid",
                "org.fdroid.basic",
                "org.fdroid.fdroid",
                "org.fdroid.nearby",
                "subreddit.android.appstore",
                "taco.apkmirror",
            ),
        "Connectivity" to
            arrayOf(
                "com.ap.transmission.btc",
                "com.pyamsoft.tetherfi",
                "com.wireguard.android",
                "de.blinkt.openvpn",
                "org.sdrangel",
            ),
        "Fediverse" to
            arrayOf(
                "app.fedilab.fedilabtube",
                "app.fedilab.tubelab",
                "app.pachli",
                "app.vger.voyager",
                "at.connyduck.pixelcat",
                "com.Sommerlichter.social",
                "com.fediphoto",
                "com.fediphoto.lineage",
                "com.freshfieldreds.muffed",
                "com.gakki",
                "com.github.moko256.twitlatte",
                "com.hjiangsu.thunder",
                "com.indieweb.indigenous",
                "com.jerboa",
                "com.jeroensmeets.mastodon",
                "com.keylessplace.tusky",
                "com.liftoffapp.liftoff",
                "com.thebrokenrail.combustible",
                "de.monocles.social",
                "dev.zwander.lemmyredirect",
                "dev.zwander.mastodonredirect",
                "eu.toldi.infinityforlemmy",
                "fr.gouv.etalab.mastodon",
                "fr.mobdev.peertubelive",
                "fr.xtof54.mousetodon",
                "jp.juggler.subwaytooter.noFcm",
                "net.accelf.yuito",
                "org.joinmastodon.android",
                "org.joinmastodon.android.moshinda",
                "org.joinmastodon.android.sk",
                "org.nuclearfog.twidda",
                "org.pixeldroid.app",
                "su.xash.husky",
            ),
        "Multimedia" to
            arrayOf(
                "app.grapheneos.pdfviewer",
                "com.alexmercerind.audire",
                "com.futo.platformplayer",
                "app.grapheneos.camera",
                "com.mrsep.musicrecognizer",
                "dev.jdtech.jellyfin",
                "nl.moeilijkedingen.jellyfinaudioplayer",
            ),
        "Phone & SMS" to
            arrayOf(
                "im.molly.app",
                "org.thoughtcrime.securesms",
            ),
        "Productivity" to
            arrayOf(
                "com.darkempire78.opencalculator",
                "com.simplemobiletools.calendar.pro",
                "com.vicolo.chrono",
                "eu.faircode.email",
            ),
        "Security" to
            arrayOf(
                "com.aurora.warden",
                "com.x8bit.bitwarden",
                "org.liberty.android.freeotpplus",
            ),
        "Science & Education" to
            arrayOf(
                "com.quaap.audiometer",
                "org.secuso.privacyfriendlytapemeasure",
            ),
        "System" to
            arrayOf(
                "app.attestation.auditor",
                "com.android.documentsui",
                "com.android.settings",
                "com.github.mrrar.gps_lock",
                "org.futo.voiceinput",
            ),
        "Weather" to
            arrayOf(
                "org.breezyweather",
            ),
        "Writing" to
            arrayOf(
                "com.cmgcode.minimoods",
            ),
    )
