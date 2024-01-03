package dev.fr33zing.launcher.data.utility

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.UserManager
import android.util.Log
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlList
import com.charleskorn.kaml.yamlMap
import com.charleskorn.kaml.yamlScalar
import dev.fr33zing.launcher.TAG
import dev.fr33zing.launcher.data.persistent.payloads.mainPackageManager
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

const val DEFAULT_CATEGORY_NAME = "Uncategorized"

private var intentActivities: Map<String, List<String>>? = null

suspend fun getActivityInfos(context: Context): List<LauncherActivityInfo> {
    return withContext(Dispatchers.IO) {
        val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val result =
            userManager.userProfiles
                .map { launcherApps.getActivityList(null, it) }
                .reduce { acc, activityInfos -> acc + activityInfos }
                .sortedBy { it.label.toString() } // as ArrayList<LauncherActivityInfo>

        intentActivities = null
        result
    }
}

fun getApplicationCategoryName(
    context: Context,
    packageName: String,
    applicationCategoryOverrides: Map<String, String>
): String {
    return applicationCategoryOverrides[packageName]
        ?: getApplicationCategoryByIntentActivitiesQuery(packageName)
        ?: getFirstFDroidApplicationCategory(packageName)
        ?: getApplicationInfoCategoryTitle(context, packageName)
        ?: DEFAULT_CATEGORY_NAME
}

/**
 * Attempts to find the category for a package via PackageManager.queryIntentActivities, but only
 * when there are multiple matches per category.
 */
fun getApplicationCategoryByIntentActivitiesQuery(packageName: String): String? {
    if (intentActivities == null)
        intentActivities =
            mapOf(
                    Intent.CATEGORY_APP_BROWSER to "Web Browsers",
                    Intent.CATEGORY_APP_CALCULATOR to "Calculators",
                    Intent.CATEGORY_APP_CALENDAR to "Calendars",
                    Intent.CATEGORY_APP_CONTACTS to "Contacts",
                    Intent.CATEGORY_APP_EMAIL to "Email Clients",
                    Intent.CATEGORY_APP_FILES to "File Browsers",
                    Intent.CATEGORY_APP_FITNESS to "Fitness",
                    Intent.CATEGORY_APP_GALLERY to "Galleries",
                    Intent.CATEGORY_APP_MAPS to "Navigation",
                    Intent.CATEGORY_APP_MARKET to "Markets",
                    Intent.CATEGORY_APP_MESSAGING to "Messaging",
                    Intent.CATEGORY_APP_MUSIC to "Music Players",
                    Intent.CATEGORY_APP_WEATHER to "Weather",
                )
                .mapNotNull { (intentCategory, categoryName) ->
                    val resolveInfos =
                        mainPackageManager.queryIntentActivities(
                            Intent().setAction(Intent.ACTION_MAIN).addCategory(intentCategory),
                            PackageManager.MATCH_DEFAULT_ONLY
                        )
                    if (resolveInfos.size < 2) null
                    else
                        Pair(
                            categoryName,
                            resolveInfos.map { resolveInfo -> resolveInfo.activityInfo.packageName }
                        )
                }
                .toMap()

    return intentActivities!!.keys.firstOrNull { category ->
        packageName in intentActivities!![category]!!
    }
}

/**
 * Attempt to find the category for a package via F-Droid build metadata.
 *
 * See F-Droid build metadata reference:
 * https://f-droid.org/docs/Build_Metadata_Reference/#Categories
 */
fun getFirstFDroidApplicationCategory(packageName: String): String? =
    try {
        val url =
            URL("https://gitlab.com/fdroid/fdroiddata/-/raw/master/metadata/${packageName}.yml")
        val text = url.readText()
        val yamlNode = Yaml.default.parseToYamlNode(text)
        val categories =
            yamlNode.yamlMap.get<YamlList>("Categories") ?: throw Exception("No categories")
        val firstCategory = categories[0].yamlScalar.content
        Log.v(TAG, "Found F-Droid category for package `$packageName`: `$firstCategory`")
        firstCategory
    } catch (e: Exception) {
        Log.v(TAG, "Failed to find F-Droid category for package `$packageName`: `$e`")
        null
    }

/**
 * Attempt to find the category for a package via R.attr.appCategory.
 *
 * See setApplicationCategoryHint for reference:
 * https://developer.android.com/reference/android/content/pm/PackageManager#setApplicationCategoryHint(java.lang.String,%20int)
 */
fun getApplicationInfoCategoryTitle(context: Context, packageName: String): String? =
    try {
        val applicationInfo = mainPackageManager.getApplicationInfo(packageName, 0)
        when (val category = applicationInfo.category) {
            ApplicationInfo.CATEGORY_GAME -> "Games"
            ApplicationInfo.CATEGORY_AUDIO -> "Multimedia"
            ApplicationInfo.CATEGORY_VIDEO -> "Multimedia"
            ApplicationInfo.CATEGORY_IMAGE -> "Multimedia"
            ApplicationInfo.CATEGORY_SOCIAL -> "Phone & SMS"
            ApplicationInfo.CATEGORY_NEWS -> "Reading"
            ApplicationInfo.CATEGORY_MAPS -> "Navigation"
            else -> ApplicationInfo.getCategoryTitle(context, category).toString()
        }
    } catch (_: Exception) {
        null
    }
