package com.forrest.titanlauncher

import android.Manifest
import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.LauncherApps
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.Icon
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.forrest.titanlauncher.ai.GeminiApiKeyStore
import com.forrest.titanlauncher.ai.GeminiRepository
import com.forrest.titanlauncher.calendar.CalendarCommandParser
import com.forrest.titanlauncher.calendar.CalendarEventDraft
import com.forrest.titanlauncher.calendar.CalendarRepository
import com.forrest.titanlauncher.calendar.CalendarAssistCalendar
import com.forrest.titanlauncher.calendar.CalendarAssistRepository
import com.forrest.titanlauncher.calendar.CalendarVisibilityStore
import com.forrest.titanlauncher.calendar.CalendarSelectionStore
import com.forrest.titanlauncher.calendar.DeviceCalendarRepository
import com.forrest.titanlauncher.calendar.CalendarTarget
import com.forrest.titanlauncher.calendar.GoogleCalendarAuthManager
import com.forrest.titanlauncher.calendar.GoogleCalendarOption
import com.forrest.titanlauncher.calls.CallLogEntry
import com.forrest.titanlauncher.calls.CallLogRepository
import com.forrest.titanlauncher.calls.CallType
import com.forrest.titanlauncher.commands.Command
import com.forrest.titanlauncher.commands.CommandParser
import com.forrest.titanlauncher.contacts.Contact
import com.forrest.titanlauncher.contacts.ContactRepository
import com.forrest.titanlauncher.contacts.EmailContact
import com.forrest.titanlauncher.messages.SmsDatabase
import com.forrest.titanlauncher.messages.threadKeyFor
import com.forrest.titanlauncher.mail.composeInMailApp
import com.forrest.titanlauncher.mail.openMailApp
import com.forrest.titanlauncher.messages.SmsMessage
import com.forrest.titanlauncher.messages.SmsHistoryImporter
import com.forrest.titanlauncher.messages.SmsRoleManager
import com.forrest.titanlauncher.messages.SmsSender
import com.forrest.titanlauncher.mail.GmailRepository
import com.forrest.titanlauncher.mail.GoogleMailAuthManager
import com.forrest.titanlauncher.mail.MailMessage
import com.forrest.titanlauncher.mail.MailReplySupportRepository
import com.forrest.titanlauncher.mail.MailActionRepository
import com.forrest.titanlauncher.mail.MailSnoozeStore
import com.forrest.titanlauncher.notes.NoteCategory
import com.forrest.titanlauncher.notes.NoteItem
import com.forrest.titanlauncher.notes.NoteStyleRange
import com.forrest.titanlauncher.notes.NoteTextStyle
import com.forrest.titanlauncher.notes.NoteStore
import com.forrest.titanlauncher.notifications.HubNotification
import com.forrest.titanlauncher.notifications.NotificationCenter
import com.forrest.titanlauncher.settings.CommandSurfaceTone
import com.forrest.titanlauncher.settings.HomeCornerStyle
import com.forrest.titanlauncher.settings.HomeElementSize
import com.forrest.titanlauncher.settings.HomeSurfaceTone
import com.forrest.titanlauncher.settings.LauncherAccent
import com.forrest.titanlauncher.settings.LauncherInterfaceFont
import com.forrest.titanlauncher.settings.LauncherSecondaryTextColor
import com.forrest.titanlauncher.settings.LauncherSettings
import com.forrest.titanlauncher.settings.LauncherTextColor
import com.forrest.titanlauncher.settings.LauncherSettingsStore
import com.forrest.titanlauncher.settings.ReadabilityContrast
import com.forrest.titanlauncher.settings.ReadabilityMotion
import com.forrest.titanlauncher.settings.ReadabilityTextSize
import com.forrest.titanlauncher.settings.ReadabilityTextWeight
import com.forrest.titanlauncher.settings.LauncherThemeMode
import com.forrest.titanlauncher.todoist.TodoistRepository
import com.forrest.titanlauncher.todoist.TodoistTokenStore
import com.forrest.titanlauncher.usage.CurrentHourUsageDiagnostics
import com.forrest.titanlauncher.usage.HourProductivityStatus
import com.forrest.titanlauncher.usage.UsageStatsRepository
import com.forrest.titanlauncher.weather.WeatherRepository
import com.forrest.titanlauncher.weather.WeatherSnapshot
import com.forrest.titanlauncher.ui.theme.TitanLauncherTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.roundToInt

internal var BackgroundBlack by
mutableStateOf(
    Color(0xFF000000)
)

internal var SurfaceBlack by
mutableStateOf(
    Color(0xFF0B0B0B)
)

internal var InputSurface by
mutableStateOf(
    Color(0xFF171717)
)

internal var BorderGray by
mutableStateOf(
    Color(0xFF242424)
)

internal var PrimaryText by
mutableStateOf(
    Color(0xFFF1F1F1)
)

internal var SecondaryText by
mutableStateOf(
    Color(0xFF999999)
)

internal var TertiaryText by
mutableStateOf(
    Color(0xFF666666)
)

internal var AccentOrange by
mutableStateOf(
    Color(0xFFFF6438)
)

/*
 * Poppins, bundled from app/src/main/res/font.
 *
 * Both weights are declared so FontWeight.Medium on headers and
 * button labels resolves to the real medium cut instead of Android
 * synthesising a heavier Regular. Font() only holds the resource id,
 * so nothing is read until something is drawn with the family.
 */
/*
 * Bricolage Grotesque, Daylight's typeface, bundled from
 * app/src/main/res/font (SIL Open Font License, see licenses/).
 */
internal val BricolageFamily =
    FontFamily(
        Font(
            R.font.bricolage_regular,
            FontWeight.Normal
        ),
        Font(
            R.font.bricolage_medium,
            FontWeight.Medium
        ),
        Font(
            R.font.bricolage_semibold,
            FontWeight.SemiBold
        ),
        Font(
            R.font.bricolage_bold,
            FontWeight.Bold
        )
    )

internal val PoppinsFamily =
    FontFamily(
        Font(
            R.font.poppins_regular,
            FontWeight.Normal
        ),
        Font(
            R.font.poppins_medium,
            FontWeight.Medium
        )
    )

/*
 * Seeded with Poppins so the first frame, drawn before the stored
 * settings are read, already matches the default rather than flashing
 * monospace and then swapping.
 */
internal var InterfaceFont by
mutableStateOf<FontFamily>(
    BricolageFamily
)

internal var HomeButtonSurface by
mutableStateOf(
    Color(0xFF0B0B0B)
)

internal var HomeInfoCardSurface by
mutableStateOf(
    Color(0xFF0B0B0B)
)

internal var HomeCommandSurface by
mutableStateOf(
    Color(0xFF171717)
)

internal var HomeButtonIconSizeDp by
mutableFloatStateOf(
    22f
)

internal var HomeButtonLabelSizeSp by
mutableFloatStateOf(
    9.5f
)

internal var HomeButtonCornerRadiusDp by
mutableFloatStateOf(
    10f
)

/*
 * Bumped every time the launcher comes back to the front, so the
 * home screen can re-check things that may have changed in other apps
 * (for example texts read in the messaging app).
 */
internal var LauncherResumeCount by
mutableIntStateOf(
    0
)

internal var ShowHomeWeather by
mutableStateOf(
    true
)

/*
 * Weather unit: true shows °C, false shows °F.
 */
internal var WeatherInCelsius by
mutableStateOf(
    false
)

internal var ShowHomeProductivityDots by
mutableStateOf(
    true
)

internal var ShowHomeCalendarCard by
mutableStateOf(
    true
)

internal var ShowHomeAttentionCard by
mutableStateOf(
    true
)

/*
 * Search picker settings, read by the home prompt.
 */
internal var ShowSearchTargetPicker by
mutableStateOf(
    true
)

/*
 * The command legend above the prompt (Settings > Commands box).
 */
internal var ShowCommandHints by
mutableStateOf(
    true
)

internal var SearchChipChrome by
mutableStateOf(
    true
)

internal var SearchChipClaude by
mutableStateOf(
    true
)

internal var SearchChipApps by
mutableStateOf(
    true
)

/*
 * Mail app new emails are written in. Blank means Prompt Launcher's
 * own Gmail compose.
 */
/*
 * Task picker settings, read by the home prompt.
 */
internal var ShowTaskPicker by
mutableStateOf(
    true
)

internal var TaskChipGoogle by
mutableStateOf(
    true
)

internal var TaskChipTodoist by
mutableStateOf(
    true
)

/*
 * A one-time choice from the "write in" chips: "" writes in Daylight,
 * a package name writes in that mail app, null follows the setting.
 */
internal var MailWriteInOnce by
mutableStateOf<String?>(
    null
)

internal var MailComposeAppPackage by
mutableStateOf(
    ""
)

/*
 * Email apps chosen by the user, in order. Empty means all of them.
 */
internal var EnabledMailApps by
mutableStateOf(
    emptyList<String>()
)

/*
 * Hours that have not happened yet. Supplied by the active palette so
 * pending dots stay distinct from PrimaryText in every theme.
 */
internal var ProductivityGray by
mutableStateOf(
    Color(0xFF6E6E6E)
)

/*
 * Daylight orange: an hour lost to distracting apps.
 */
internal val ProductivityRed =
    Color(0xFFE8662B)

internal val LauncherCommandRed =
    Color(0xFFFF3B30)

internal val CameraSafeStartPadding =
    42.dp

internal var ReadabilityFontScale by
mutableFloatStateOf(
    0.90f
)

internal var ReadabilityFontWeightValue by
mutableStateOf(
    FontWeight.Normal
)

internal var ReduceLauncherMotion by
mutableStateOf(
    false
)

internal enum class LauncherSettingsPage {
    MAIN,
    APPEARANCE,
    HOME_SCREEN,
    CALENDARS,
    PRODUCTIVITY_BAR,
    NOTIFICATIONS,
    READABILITY,
    BACKUP_RESTORE
}

internal fun accentColorFor(
    accent: LauncherAccent
): Color {
    return when (accent) {
        LauncherAccent.COBALT -> Color(0xFF2E4BF0)
        LauncherAccent.ORANGE -> Color(0xFFFF6438)
        LauncherAccent.RED -> Color(0xFFFF4D4D)
        LauncherAccent.YELLOW -> Color(0xFFFFC83D)
        LauncherAccent.GREEN -> Color(0xFF52D273)
        LauncherAccent.BLUE -> Color(0xFF59A8FF)
        LauncherAccent.PURPLE -> Color(0xFFB47CFF)
        LauncherAccent.PINK -> Color(0xFFFF78B7)
    }
}

/*
 * THEME PALETTES
 *
 * Each mode supplies the full set of surfaces and text colours, so a
 * new theme is one entry here rather than a new branch everywhere the
 * old dark/light check appeared.
 */
internal data class LauncherPalette(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val input: Color,
    val border: Color,
    val raised: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val tertiaryText: Color,
    val pendingDot: Color
)

internal fun paletteFor(
    mode: LauncherThemeMode
): LauncherPalette {

    return when (
        mode
    ) {

        /*
         * DAYLIGHT
         *
         * Dark and light are Daylight's own pair: a cool near-black
         * with dark grey cards, and a cool light grey with white
         * cards. The other themes are kept as extras.
         */
        LauncherThemeMode.DARK ->
            LauncherPalette(
                isDark = true,
                background = Color(0xFF0F1216),
                surface = Color(0xFF1B1F26),
                input = Color(0xFF1B1F26),
                border = Color(0xFF2A3038),
                raised = Color(0xFF3D444E),
                primaryText = Color(0xFFEEF1F4),
                secondaryText = Color(0xFFA0A8B3),
                tertiaryText = Color(0xFF7D8691),
                pendingDot = Color(0xFF3D444E)
            )

        LauncherThemeMode.LIGHT ->
            LauncherPalette(
                isDark = false,
                background = Color(0xFFEDF0F3),
                surface = Color(0xFFFFFFFF),
                input = Color(0xFFFFFFFF),
                border = Color(0xFFD5DAE0),
                raised = Color(0xFFC3CAD3),
                primaryText = Color(0xFF11141A),
                secondaryText = Color(0xFF4D5562),
                tertiaryText = Color(0xFF6B7480),
                pendingDot = Color(0xFFC3CAD3)
            )

        LauncherThemeMode.NAVY ->
            LauncherPalette(
                isDark = true,
                background = Color(0xFF060B16),
                surface = Color(0xFF0C1425),
                input = Color(0xFF141F34),
                border = Color(0xFF1E2C47),
                raised = Color(0xFF2A3A59),
                primaryText = Color(0xFFE8EDF7),
                secondaryText = Color(0xFF8F9BB3),
                tertiaryText = Color(0xFF5C6880),
                pendingDot = Color(0xFF5C6880)
            )

        LauncherThemeMode.SLATE ->
            LauncherPalette(
                isDark = true,
                background = Color(0xFF14161A),
                surface = Color(0xFF1C1F24),
                input = Color(0xFF262A31),
                border = Color(0xFF343941),
                raised = Color(0xFF434952),
                primaryText = Color(0xFFECEEF1),
                secondaryText = Color(0xFF9CA3AE),
                tertiaryText = Color(0xFF6B7280),
                pendingDot = Color(0xFF6B7280)
            )

        LauncherThemeMode.TAN ->
            LauncherPalette(
                isDark = false,
                background = Color(0xFFF3EADA),
                surface = Color(0xFFEADFCB),
                input = Color(0xFFE0D2B9),
                border = Color(0xFFCBBA9C),
                raised = Color(0xFFBFAC8A),
                primaryText = Color(0xFF2A2318),
                secondaryText = Color(0xFF6E6250),
                tertiaryText = Color(0xFF938572),
                pendingDot = Color(0xFFC0B29A)
            )

        LauncherThemeMode.FOREST ->
            LauncherPalette(
                isDark = true,
                background = Color(0xFF07120D),
                surface = Color(0xFF0D1C15),
                input = Color(0xFF14291F),
                border = Color(0xFF1F3A2C),
                raised = Color(0xFF2B4D3B),
                primaryText = Color(0xFFE6F0E9),
                secondaryText = Color(0xFF8CA396),
                tertiaryText = Color(0xFF5C7367),
                pendingDot = Color(0xFF5C7367)
            )
    }
}

/*
 * Null means "derive the muted tones from whatever the primary text
 * ended up being", which is the previous behaviour.
 */
internal fun secondaryTextColorOverrideFor(
    choice: LauncherSecondaryTextColor,
    accent: Color
): Color? {

    return when (
        choice
    ) {
        LauncherSecondaryTextColor.DEFAULT -> null
        LauncherSecondaryTextColor.LIGHT -> Color(0xFFC9C9C9)
        LauncherSecondaryTextColor.MEDIUM -> Color(0xFF9A9A9A)
        LauncherSecondaryTextColor.DARK -> Color(0xFF6B6B6B)
        LauncherSecondaryTextColor.WARM -> Color(0xFFC7B49A)
        LauncherSecondaryTextColor.COOL -> Color(0xFF9FB3C8)
        LauncherSecondaryTextColor.ACCENT -> accent
    }
}

/*
 * Null means "leave the palette's own text colours alone".
 */
internal fun textColorOverrideFor(
    choice: LauncherTextColor,
    accent: Color
): Color? {

    return when (
        choice
    ) {
        LauncherTextColor.DEFAULT -> null
        LauncherTextColor.WHITE -> Color(0xFFFFFFFF)
        LauncherTextColor.BLACK -> Color(0xFF111111)
        LauncherTextColor.WARM -> Color(0xFFF2E3CC)
        LauncherTextColor.COOL -> Color(0xFFD8E4F0)
        LauncherTextColor.ACCENT -> accent
    }
}

internal fun homeSurfaceColorFor(
    tone: HomeSurfaceTone,
    themeMode: LauncherThemeMode
): Color {
    val palette =
        paletteFor(
            themeMode
        )

    return when (tone) {
        HomeSurfaceTone.BLACK -> palette.surface
        HomeSurfaceTone.CHARCOAL -> palette.input
        HomeSurfaceTone.GRAY -> palette.border
    }
}

internal fun commandSurfaceColorFor(
    tone: CommandSurfaceTone,
    themeMode: LauncherThemeMode
): Color {
    val palette =
        paletteFor(
            themeMode
        )

    return when (tone) {
        CommandSurfaceTone.DARK -> palette.input
        CommandSurfaceTone.MEDIUM -> palette.border
        CommandSurfaceTone.LIGHT -> palette.raised
    }
}

/*
 * The theme actually drawn: with "auto" on, Daylight light or dark
 * following the phone's dark mode, otherwise the chosen theme.
 */
internal fun effectiveThemeMode(
    settings: LauncherSettings,
    systemDark: Boolean
): LauncherThemeMode =
    if (
        settings.followSystemTheme
    ) {
        if (systemDark) LauncherThemeMode.DARK else LauncherThemeMode.LIGHT
    } else {
        settings.themeMode
    }

internal fun applyLauncherAppearance(
    settings: LauncherSettings,
    systemDark: Boolean = false
) {
    val themeMode =
        effectiveThemeMode(
            settings,
            systemDark
        )

    val isDaylightPair =
        themeMode == LauncherThemeMode.LIGHT ||
                themeMode == LauncherThemeMode.DARK

    AccentOrange =
        if (
            settings.accent == LauncherAccent.COBALT &&
            paletteFor(themeMode).isDark
        ) {
            /*
             * Cobalt is too dark to read on a dark background, so
             * dark mode uses a lighter periwinkle of the same hue.
             */
            Color(0xFF8C9BFF)
        } else {
            accentColorFor(
                settings.accent
            )
        }

    InterfaceFont =
        when (settings.interfaceFont) {
            LauncherInterfaceFont.BRICOLAGE -> BricolageFamily
            LauncherInterfaceFont.MONO -> FontFamily.Monospace
            LauncherInterfaceFont.SANS -> FontFamily.SansSerif
            LauncherInterfaceFont.SERIF -> FontFamily.Serif
            LauncherInterfaceFont.POPPINS -> PoppinsFamily
        }

    val palette =
        paletteFor(
            themeMode
        )

    BackgroundBlack = palette.background
    SurfaceBlack = palette.surface
    InputSurface = palette.input
    BorderGray = palette.border
    ProductivityGray = palette.pendingDot

    val textOverride =
        textColorOverrideFor(
            settings.textColor,
            AccentOrange
        )

    if (
        textOverride == null
    ) {

        PrimaryText = palette.primaryText
        SecondaryText = palette.secondaryText
        TertiaryText = palette.tertiaryText

    } else {

        /*
         * Secondary and tertiary are derived from the chosen colour so
         * the three-step hierarchy survives whatever the user picks.
         */
        PrimaryText = textOverride
        SecondaryText = textOverride.copy(alpha = 0.62f)
        TertiaryText = textOverride.copy(alpha = 0.42f)
    }

    HomeButtonSurface =
        homeSurfaceColorFor(
            settings.appButtonSurface,
            themeMode
        )

    HomeInfoCardSurface =
        homeSurfaceColorFor(
            settings.infoCardSurface,
            themeMode
        )

    HomeCommandSurface =
        commandSurfaceColorFor(
            settings.commandSurface,
            themeMode
        )

    HomeButtonIconSizeDp =
        when (settings.buttonIconSize) {
            /* 2.1.2: the old Small is the new Normal. */
            HomeElementSize.SMALL -> 15f
            HomeElementSize.NORMAL -> 18f
            HomeElementSize.LARGE -> 22f
        }

    HomeButtonLabelSizeSp =
        when (settings.buttonLabelSize) {
            HomeElementSize.SMALL -> 8.5f
            HomeElementSize.NORMAL -> 9.5f
            HomeElementSize.LARGE -> 10.5f
        }

    HomeButtonCornerRadiusDp =
        when (settings.buttonCorners) {
            HomeCornerStyle.SQUARE -> 2f
            HomeCornerStyle.SOFT -> 10f
            HomeCornerStyle.ROUND -> 18f
        }

    ShowHomeWeather = settings.showWeather
    WeatherInCelsius = settings.weatherCelsius
    ShowHomeProductivityDots = settings.showProductivityDots
    ShowHomeCalendarCard = settings.showCalendarCard
    ShowHomeAttentionCard = settings.showAttentionCard
    ShowSearchTargetPicker = settings.showSearchTargetPicker
    ShowCommandHints = settings.showCommandHints
    SearchChipChrome = settings.searchChipChrome
    SearchChipClaude = settings.searchChipClaude
    SearchChipApps = settings.searchChipApps
    MailComposeAppPackage = settings.mailComposeApp
    ShowTaskPicker = settings.showTaskPicker
    TaskChipGoogle = settings.taskChipGoogle
    TaskChipTodoist = settings.taskChipTodoist
    EnabledMailApps = settings.mailApps

    ReadabilityFontScale =
        when (settings.readabilityTextSize) {
            /*
             * 2.1.2: the old Small is the new Normal, and the
             * other sizes moved down the same 10%.
             */
            ReadabilityTextSize.SMALL -> 0.81f
            ReadabilityTextSize.NORMAL -> 0.90f
            ReadabilityTextSize.LARGE -> 0.97f
            ReadabilityTextSize.EXTRA_LARGE -> 1.04f
        }

    ReadabilityFontWeightValue =
        when (settings.readabilityTextWeight) {
            ReadabilityTextWeight.NORMAL -> FontWeight.Normal
            ReadabilityTextWeight.MEDIUM -> FontWeight.Medium
            ReadabilityTextWeight.BOLD -> FontWeight.Bold
        }

    ReduceLauncherMotion =
        settings.readabilityMotion ==
                ReadabilityMotion.REDUCED

    /*
     * Was "themeMode == DARK", which sent navy, slate and forest down
     * the light-mode branch and gave them dark grey muted text on a
     * dark background. The palette knows whether a theme is dark.
     */
    /*
     * Daylight's light and dark palettes already carry tuned cool
     * greys, so standard contrast keeps them as they are.
     */
    if (
        isDaylightPair &&
        settings.readabilityContrast == ReadabilityContrast.STANDARD &&
        !settings.dimSecondaryText
    ) {
        // Keep the palette's own secondary and tertiary text.
    } else if (palette.isDark) {
        when (settings.readabilityContrast) {
            ReadabilityContrast.STANDARD -> {
                SecondaryText =
                    if (settings.dimSecondaryText) Color(0xFF999999) else Color(0xFFBDBDBD)
                TertiaryText =
                    if (settings.dimSecondaryText) Color(0xFF666666) else Color(0xFF929292)
            }
            ReadabilityContrast.HIGH -> {
                SecondaryText = Color(0xFFC8C8C8)
                TertiaryText = Color(0xFFA8A8A8)
                BorderGray = Color(0xFF3A3A3A)
            }
            ReadabilityContrast.MAXIMUM -> {
                SecondaryText = PrimaryText
                TertiaryText = Color(0xFFD6D6D6)
                BorderGray = Color(0xFF5A5A5A)
            }
        }
    } else {
        when (settings.readabilityContrast) {
            ReadabilityContrast.STANDARD -> {
                SecondaryText =
                    if (settings.dimSecondaryText) Color(0xFF66625D) else Color(0xFF4F4B46)
                TertiaryText =
                    if (settings.dimSecondaryText) Color(0xFF8B8780) else Color(0xFF66615B)
            }
            ReadabilityContrast.HIGH -> {
                SecondaryText = Color(0xFF45413D)
                TertiaryText = Color(0xFF5C5752)
                BorderGray = Color(0xFFB8B2AA)
            }
            ReadabilityContrast.MAXIMUM -> {
                SecondaryText = PrimaryText
                TertiaryText = Color(0xFF393633)
                BorderGray = Color(0xFF938D84)
            }
        }
    }

    /*
     * Last word on the muted tones.
     *
     * The readability contrast pass above rewrites SecondaryText and
     * TertiaryText unconditionally, so an explicit choice has to be
     * applied after it or it gets overwritten a few lines later.
     */
    val secondaryOverride =
        secondaryTextColorOverrideFor(
            settings.secondaryTextColor,
            AccentOrange
        )

    if (
        secondaryOverride != null
    ) {

        SecondaryText =
            secondaryOverride

        TertiaryText =
            secondaryOverride.copy(
                alpha = 0.68f
            )
    }
}

data class UpcomingCalendarEvent(
    val title: String,
    val startTime: Long,
    val allDay: Boolean,
    val location: String = ""
)

data class AssistantTurn(
    val question: String,
    val answer: String
)

data class ConversationSummary(
    val contact: Contact,
    val latestMessage: SmsMessage,
    val unreadCount: Int,
    val photoUri: String? = null,

    /*
     * Identifies the conversation. For a group there is no single
     * "other person", so the thread key is what the screens key on.
     */
    val threadKey: String = "",
    val participants: List<String> = emptyList(),
    val isGroup: Boolean = false
)

/*
 * What the conversation screen needs to open a thread. A one-to-one
 * chat could be described by its Contact alone; a group cannot.
 */
data class ConversationThread(
    val threadKey: String,
    val title: String,
    val participants: List<String>,
    val isGroup: Boolean,

    /*
     * Text typed in the command bar before the thread opened, so a
     * group started with "@@" does not lose the message.
     */
    val draft: String = ""
)

internal enum class HubCategory {
    ALL,
    MESSAGES,
    CALLS,
    EMAIL,
    NOTIFICATIONS
}

internal enum class HubItemType {
    MESSAGE,
    CALL,
    EMAIL,
    NOTIFICATION
}

internal data class HubDisplayItem(
    val id: String,
    val type: HubItemType,
    val title: String,
    val preview: String,
    val timestamp: Long,
    val unread: Boolean = false,
    val missed: Boolean = false,
    val phoneNumber: String? = null,
    val mailMessage: MailMessage? = null,
    val notificationKey: String? = null,
    val photoUri: String? = null,

    /*
     * Set on message rows so a group can be opened as a thread
     * rather than as a chat with whoever sent last.
     */
    val threadKey: String = "",
    val isGroup: Boolean = false
)

data class MailComposeDraft(
    val toAddress: String,
    val toLabel: String,
    val subject: String = "",
    val body: String = "",
    val threadId: String? = null,
    val inReplyToMessageId: String? = null,
    val isReply: Boolean = false,
    val isReplyAll: Boolean = false,
    val isForward: Boolean = false,
    val recipientEditable: Boolean = false,
    val forwardedBody: String = ""
)

internal enum class GoogleCalendarAction {
    CREATE_EVENT,
    LOAD_SETUP,
    LOAD_VISIBILITY
}

internal enum class CalendarSetupStep {
    PERSONAL,
    FAMILY
}

@OptIn(ExperimentalComposeUiApi::class)
class MainActivity :
    ComponentActivity() {

    private var homeRequestVersion by
    mutableIntStateOf(
        0
    )

    private var redirectingToOnboarding =
        false

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )


        /*
         * Android can launch MainActivity immediately after
         * Prompt Launcher is selected as the default Home app.
         *
         * If setup is still in progress, return the user to the
         * onboarding flow instead of showing Home early.
         */
        if (
            redirectToOnboardingIfNeeded()
        ) {
            return
        }

        WindowCompat.setDecorFitsSystemWindows(
            window,
            false
        )

        hideSystemBars()

        @Suppress("DEPRECATION")
        window.decorView.setOnSystemUiVisibilityChangeListener {
            window.decorView.post {
                hideSystemBars()
            }
        }

        setContent {

            TitanLauncherTheme {

                TitanUiScale {

                    val baseDensity =
                        LocalDensity.current

                    val readabilityDensity =
                        Density(
                            density =
                                baseDensity.density,
                            fontScale =
                                baseDensity.fontScale *
                                        ReadabilityFontScale *
                                        1.16f
                        )

                    CompositionLocalProvider(
                        LocalDensity provides readabilityDensity
                    ) {
                        ProvideTextStyle(
                            value =
                                TextStyle(
                                    fontWeight =
                                        ReadabilityFontWeightValue
                                )
                        ) {
                            /*
                             * Keyboard app allowed or not: see
                             * PromptKeyboard.kt.
                             */
                            PromptTextInput {
                                TitanApp(
                                    homeRequestVersion = homeRequestVersion
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(
        intent: Intent
    ) {

        super.onNewIntent(
            intent
        )

        setIntent(
            intent
        )

        /*
         * If another Home intent reaches this activity while
         * onboarding is unfinished, keep setup in front.
         */
        if (
            redirectToOnboardingIfNeeded()
        ) {
            return
        }

        homeRequestVersion +=
            1

        hideSystemBars()
    }

    override fun onResume() {

        super.onResume()

        /*
         * Some devices resume an existing launcher activity after
         * the default Home selection instead of creating a new one.
         */
        if (
            redirectToOnboardingIfNeeded()
        ) {
            return
        }

        hideSystemBars()

        /*
         * Coming back from the messaging or email app: drop anything
         * that was read there, so quick reply and the hub only show
         * what is still unread.
         */
        NotificationCenter
            .requestRefresh()

        LauncherResumeCount++
    }

    override fun onWindowFocusChanged(
        hasFocus: Boolean
    ) {

        super.onWindowFocusChanged(
            hasFocus
        )

        if (
            hasFocus
        ) {

            hideSystemBars()
        }
    }

    private fun redirectToOnboardingIfNeeded(): Boolean {

        val onboardingComplete =
            getSharedPreferences(
                ONBOARDING_PREFS,
                Context.MODE_PRIVATE
            )
                .getBoolean(
                    KEY_ONBOARDING_COMPLETE,
                    false
                )

        if (
            onboardingComplete
        ) {
            redirectingToOnboarding =
                false

            return false
        }

        if (
            !redirectingToOnboarding
        ) {
            redirectingToOnboarding =
                true

            startActivity(
                Intent(
                    this,
                    OnboardingActivity::class.java
                ).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP
                    )
                }
            )
        }

        finish()

        return true
    }

    private fun hideSystemBars() {

        WindowCompat.setDecorFitsSystemWindows(
            window,
            false
        )

        val controller =
            WindowCompat.getInsetsController(
                window,
                window.decorView
            )

        controller.hide(
            WindowInsetsCompat.Type.statusBars() or
                    WindowInsetsCompat.Type.navigationBars()
        )

        controller.systemBarsBehavior =
            WindowInsetsControllerCompat
                .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
            android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                    android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    companion object {

        private const val ONBOARDING_PREFS =
            "prompt_launcher_onboarding"

        private const val KEY_ONBOARDING_COMPLETE =
            "onboarding_complete"
    }
}

@Composable
fun TitanApp(
    homeRequestVersion: Int,
    /*
     * True when hosted by PromptAnywhereActivity: only the command
     * layer is drawn, over whatever app the user was in.
     */
    assistOverlay: Boolean = false
) {

    val context =
        LocalContext.current

    val activity =
        context as Activity

    val scope =
        rememberCoroutineScope()

    val contactRepository =
        remember {
            ContactRepository(
                context
            )
        }

    val smsDatabase =
        remember {
            SmsDatabase.getInstance(
                context
            )
        }

    val smsSender =
        remember {
            SmsSender(
                context
            )
        }

    val smsRoleManager =
        remember {
            SmsRoleManager(
                context
            )
        }

    val todoistTokenStore =
        remember {
            TodoistTokenStore(
                context
            )
        }

    val todoistRepository =
        remember {
            TodoistRepository(
                todoistTokenStore
            )
        }

    val calendarRepository =
        remember {
            CalendarRepository()
        }

    val calendarAssistRepository =
        remember {
            CalendarAssistRepository()
        }

    val calendarSelectionStore =
        remember {
            CalendarSelectionStore(
                context
            )
        }

    val calendarVisibilityStore =
        remember {
            CalendarVisibilityStore(
                context
            )
        }

    val googleCalendarAuthManager =
        remember {
            GoogleCalendarAuthManager(
                activity
            )
        }

    val noteStore =
        remember {
            NoteStore(
                context
            )
        }

    val callLogRepository =
        remember {
            CallLogRepository(
                context
            )
        }

    val gmailRepository =
        remember {
            GmailRepository()
        }

    val mailReplySupportRepository =
        remember {
            MailReplySupportRepository()
        }

    val mailActionRepository =
        remember {
            MailActionRepository()
        }

    val mailSnoozeStore =
        remember {
            MailSnoozeStore(
                context
            )
        }

    val googleMailAuthManager =
        remember {
            GoogleMailAuthManager(
                activity
            )
        }

    var currentConversation by remember {
        mutableStateOf<Contact?>(
            null
        )
    }

    /*
     * Set only when the thread is a group. Null means the screen
     * behaves exactly as it did before, keyed on the contact.
     */
    var currentConversationThread by remember {
        mutableStateOf<ConversationThread?>(
            null
        )
    }

    var showHub by remember {
        mutableStateOf(
            false
        )
    }

    var hubSelectedCategory by remember {
        mutableStateOf(
            HubCategory.ALL
        )
    }

    var hubSelectedIndex by remember {
        mutableIntStateOf(
            0
        )
    }

    var showSettings by remember {
        mutableStateOf(
            false
        )
    }

    var launcherSettingsPage by remember {
        mutableStateOf(
            LauncherSettingsPage.MAIN
        )
    }

    val launcherSettingsStore =
        remember {
            LauncherSettingsStore(
                context
            )
        }

    var launcherSettings by remember {
        mutableStateOf(
            launcherSettingsStore.load()
        )
    }

    var backupRestoreStatus by remember {
        mutableStateOf(
            ""
        )
    }

    val backupSettingsLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.CreateDocument(
                    "application/json"
                )
        ) { uri ->
            if (
                uri != null
            ) {
                val result =
                    runCatching {
                        context
                            .contentResolver
                            .openOutputStream(
                                uri
                            )
                            ?.bufferedWriter()
                            ?.use { writer ->
                                writer.write(
                                    launcherSettingsStore.exportBackup(
                                        launcherSettings
                                    )
                                )
                            }
                            ?: error(
                                "Could not open backup file"
                            )
                    }

                backupRestoreStatus =
                    if (
                        result.isSuccess
                    ) {
                        "backup saved"
                    } else {
                        "backup failed"
                    }
            }
        }

    val restoreSettingsLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (
                uri != null
            ) {
                val restored =
                    runCatching {
                        val backupJson =
                            context
                                .contentResolver
                                .openInputStream(
                                    uri
                                )
                                ?.bufferedReader()
                                ?.use { reader ->
                                    reader.readText()
                                }
                                ?: error(
                                    "Could not read backup file"
                                )

                        launcherSettingsStore.importBackup(
                            backupJson
                        )
                    }

                restored
                    .onSuccess { settings ->
                        launcherSettingsStore.save(
                            settings
                        )
                        launcherSettings =
                            settings
                        backupRestoreStatus =
                            "settings restored"
                    }
                    .onFailure {
                        backupRestoreStatus =
                            "restore failed · invalid backup"
                    }
            }
        }

    /*
     * Read here so "auto" switches the moment the phone's dark mode
     * changes.
     */
    val systemDarkTheme =
        isSystemInDarkTheme()

    LaunchedEffect(launcherSettings, systemDarkTheme) {
        applyLauncherAppearance(
            launcherSettings,
            systemDarkTheme
        )
    }

    var returnToHubAfterConversation by remember {
        mutableStateOf(
            false
        )
    }

    var returnToHubAfterMail by remember {
        mutableStateOf(
            false
        )
    }

    var callLogEntries by remember {
        mutableStateOf(
            emptyList<CallLogEntry>()
        )
    }

    var showMessagesInbox by remember {
        mutableStateOf(
            false
        )
    }

    var showCalendarSetup by remember {
        mutableStateOf(
            false
        )
    }

    var showMailInbox by remember {
        mutableStateOf(
            false
        )
    }

    var currentMailMessage by remember {
        mutableStateOf<MailMessage?>(
            null
        )
    }

    var mailComposeDraft by remember {
        mutableStateOf<MailComposeDraft?>(
            null
        )
    }

    var mailMessages by remember {
        mutableStateOf(
            emptyList<MailMessage>()
        )
    }

    var mailLoading by remember {
        mutableStateOf(
            false
        )
    }

    /*
     * True when Google wants a consent screen but the current entry
     * point was not allowed to show one.
     */
    var mailNeedsAuthorization by remember {
        mutableStateOf(false)
    }

    var mailAccessToken by remember {
        mutableStateOf<String?>(
            null
        )
    }

    var showNotes by remember {
        mutableStateOf(
            false
        )
    }

    var currentNote by remember {
        mutableStateOf<NoteItem?>(
            null
        )
    }

    var calendarSetupOptions by remember {
        mutableStateOf(
            emptyList<GoogleCalendarOption>()
        )
    }

    /*
     * Which source the calendar setup card is showing: the phone's
     * own calendars (no Google needed) or a Google account.
     */
    var calendarSetupUsesGoogle by remember {
        mutableStateOf(false)
    }

    var calendarSetupLoading by remember {
        mutableStateOf(false)
    }

    /*
     * Waiting on a calendar permission: open the phone calendar list
     * once read access is granted, or add this event once write
     * access is.
     */
    var pendingPhoneCalendarSetup by remember {
        mutableStateOf(false)
    }

    /*
     * True from the moment calendar setup is asked for until it's
     * closed or saved. A Google answer that arrives after Close must
     * not bring the card back.
     */
    var calendarSetupWanted by remember {
        mutableStateOf(false)
    }

    var pendingPhoneCalendarDraft by remember {
        mutableStateOf<CalendarEventDraft?>(null)
    }

    var calendarVisibilityOptions by remember {
        mutableStateOf(
            emptyList<CalendarAssistCalendar>()
        )
    }

    var calendarVisibilityLoading by remember {
        mutableStateOf(
            false
        )
    }

    var hiddenCalendarIds by remember {
        mutableStateOf(
            calendarVisibilityStore
                .getHiddenCalendarIds()
        )
    }

    var statusText by remember {
        mutableStateOf("")
    }

    LaunchedEffect(
        homeRequestVersion
    ) {

        if (
            homeRequestVersion > 0
        ) {

            showHub =
                false

            showSettings =
                false

            launcherSettingsPage =
                LauncherSettingsPage.MAIN

            currentConversation =
                null

            showMessagesInbox =
                false

            showCalendarSetup =
                false

            showMailInbox =
                false

            currentMailMessage =
                null

            mailComposeDraft =
                null

            showNotes =
                false

            currentNote =
                null

            returnToHubAfterConversation =
                false

            returnToHubAfterMail =
                false

            statusText =
                ""
        }
    }

    var upcomingEvent by remember {
        mutableStateOf<UpcomingCalendarEvent?>(
            null
        )
    }

    var calendarAgendaEvents by remember {
        mutableStateOf(
            emptyList<UpcomingCalendarEvent>()
        )
    }

    var pendingSmsContact by remember {
        mutableStateOf<Contact?>(
            null
        )
    }

    var pendingSmsMessage by remember {
        mutableStateOf<String?>(
            null
        )
    }

    /*
     * Set when a text typed in the prompt ("@name message") should
     * also open the conversation in the phone's messaging app once
     * it has been sent.
     */
    var pendingSmsOpenMessagesAfter by remember {
        mutableStateOf(false)
    }

    var pendingCalendarEvent by remember {
        mutableStateOf<CalendarEventDraft?>(
            null
        )
    }

    var pendingGoogleAction by remember {
        mutableStateOf<GoogleCalendarAction?>(
            null
        )
    }

    var hasContactsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.SEND_SMS
            ) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    var hasCalendarPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CALENDAR
            ) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    var hasCallLogPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CALL_LOG
            ) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    LaunchedEffect(
        statusText
    ) {

        if (
            statusText.isNotBlank()
        ) {

            /*
             * Long enough to read a short confirmation, short enough
             * not to linger over the next thing being typed.
             */
            delay(
                1500
            )

            statusText =
                ""
        }
    }

    BackHandler(
        enabled =
            currentConversation != null ||
                    showHub ||
                    showMessagesInbox ||
                    showCalendarSetup ||
                    showMailInbox ||
                    currentMailMessage != null ||
                    mailComposeDraft != null ||
                    showNotes ||
                    currentNote != null
    ) {

        when {

            currentConversation != null -> {

                currentConversation =
                    null

                if (
                    returnToHubAfterConversation
                ) {

                    returnToHubAfterConversation =
                        false

                    showHub =
                        true

                } else {

                    showMessagesInbox =
                        true
                }
            }

            currentNote != null -> {

                val noteToClose = currentNote!!

                if (
                    noteToClose.body.isBlank() &&
                    noteToClose.title.trim().let { it.isBlank() || it == "New note" }
                ) {
                    noteStore.deleteNote(noteToClose.id)
                }

                currentNote =
                    null

                showNotes =
                    true
            }

            mailComposeDraft != null -> {

                mailComposeDraft =
                    null
            }

            currentMailMessage != null -> {

                currentMailMessage =
                    null

                if (
                    returnToHubAfterMail
                ) {

                    returnToHubAfterMail =
                        false

                    showHub =
                        true

                } else {

                    showMailInbox =
                        true
                }
            }

            showHub -> {

                showHub =
                    false
            }

            showMessagesInbox -> {

                showMessagesInbox =
                    false
            }

            showCalendarSetup -> {

                showCalendarSetup =
                    false
            }

            showMailInbox -> {

                showMailInbox =
                    false
            }

            showNotes -> {

                showNotes =
                    false
            }
        }

        statusText =
            ""
    }

    val contactsPermissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.RequestPermission()
        ) { granted ->

            hasContactsPermission =
                granted
        }

    val calendarPermissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.RequestPermission()
        ) { granted ->

            hasCalendarPermission =
                granted

            if (
                pendingPhoneCalendarSetup
            ) {
                pendingPhoneCalendarSetup =
                    false

                if (granted) {
                    scope.launch {
                        calendarSetupOptions =
                            DeviceCalendarRepository
                                .loadWritableCalendars(context)
                        calendarSetupLoading =
                            false
                    }
                } else {
                    calendarSetupLoading =
                        false
                }
            }
        }

    /*
     * Adding events to a phone calendar needs write access, asked the
     * first time it's needed.
     */
    val calendarWritePermissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.RequestPermission()
        ) { granted ->

            val draft =
                pendingPhoneCalendarDraft

            pendingPhoneCalendarDraft =
                null

            if (
                !granted
            ) {
                statusText =
                    "CALENDAR ACCESS DENIED"
                return@rememberLauncherForActivityResult
            }

            if (
                draft != null
            ) {
                val calendarId =
                    when (draft.target) {
                        CalendarTarget.PERSONAL -> calendarSelectionStore.getPersonalCalendarId()
                        CalendarTarget.FAMILY -> calendarSelectionStore.getFamilyCalendarId()
                    }

                if (calendarId != null) {
                    scope.launch {
                        val result =
                            DeviceCalendarRepository.createEvent(
                                context,
                                calendarId,
                                draft
                            )
                        statusText =
                            result.message
                    }
                }
            }
        }

    /*
     * Opens calendar setup on the phone's own calendars.
     */
    fun openPhoneCalendarSetup() {

        calendarSetupWanted =
            true

        calendarSetupUsesGoogle =
            false

        calendarSetupOptions =
            emptyList()

        showMessagesInbox =
            false

        currentConversation =
            null

        showNotes =
            false

        currentNote =
            null

        showCalendarSetup =
            true

        if (
            !DeviceCalendarRepository.canRead(context)
        ) {
            calendarSetupLoading =
                true

            pendingPhoneCalendarSetup =
                true

            calendarPermissionLauncher.launch(
                Manifest.permission.READ_CALENDAR
            )

            return
        }

        calendarSetupLoading =
            true

        scope.launch {
            calendarSetupOptions =
                DeviceCalendarRepository
                    .loadWritableCalendars(context)
            calendarSetupLoading =
                false
        }
    }

    /*
     * From Android 13 the launcher must hold POST_NOTIFICATIONS to
     * raise its own shade entry for an incoming text. Asked for once
     * on first launch; denial simply means silent messages.
     */
    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.RequestPermission()
        ) { _ ->
        }

    LaunchedEffect(Unit) {

        MessageNotifications.ensureChannel(
            context
        )

        /*
         * First time Prompt Launcher holds the SMS role, copy the
         * history Google Messages left in the system store so threads
         * do not start empty. Runs once; the importer keeps its own
         * marker.
         */
        if (
            runCatching {
                smsRoleManager.isDefaultSmsApp()
            }
                .getOrDefault(
                    false
                ) &&
            !SmsHistoryImporter.hasImported(
                context
            )
        ) {

            val imported =
                withContext(
                    Dispatchers.IO
                ) {

                    SmsHistoryImporter.importIfNeeded(
                        context =
                            context,
                        dao =
                            smsDatabase.smsDao()
                    )
                }

            if (
                imported != null &&
                imported > 0
            ) {

                statusText =
                    "✓ IMPORTED $imported MESSAGES"
            }
        }

        if (
            !assistOverlay &&
            android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {

            notificationPermissionLauncher.launch(
                android.Manifest.permission.POST_NOTIFICATIONS
            )
        }
    }

    val callLogPermissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.RequestPermission()
        ) { granted ->

            hasCallLogPermission =
                granted

            if (
                !granted
            ) {

                statusText =
                    "CALL HISTORY PERMISSION DENIED"
            }
        }

    LaunchedEffect(
        showHub,
        hasCallLogPermission
    ) {

        if (
            showHub &&
            hasCallLogPermission
        ) {

            callLogEntries =
                callLogRepository
                    .loadRecentCalls()
        }
    }

    fun refreshPersonalUpcomingEvent() {

        /*
         * Phone calendars: read straight from the phone, no Google.
         */
        if (
            !calendarSelectionStore.usesGoogle()
        ) {
            scope.launch {

                val snapshot =
                    DeviceCalendarRepository.loadSnapshot(
                        context = context,
                        agendaDays = com.forrest.titanlauncher.ai.AssistantPayloadLimits.AGENDA_DAYS.toLong(),
                        nextEventDays = 30
                    )

                val homeCalendarId =
                    calendarSelectionStore
                        .getPersonalCalendarId()
                        ?.takeIf { DeviceCalendarRepository.isDeviceId(it) }

                val nextTimedEvent =
                    if (homeCalendarId != null) {
                        DeviceCalendarRepository.loadSnapshot(
                            context = context,
                            agendaDays = 1,
                            nextEventDays = 30,
                            onlyCalendarId = homeCalendarId
                        ).nextEvent
                    } else {
                        snapshot.nextEvent
                    }

                upcomingEvent =
                    nextTimedEvent?.let { event ->
                        UpcomingCalendarEvent(
                            title = event.title,
                            startTime = event.startTime,
                            allDay = event.allDay,
                            location = event.location
                        )
                    }

                calendarAgendaEvents =
                    snapshot.agendaEvents.map { event ->
                        UpcomingCalendarEvent(
                            title = event.title,
                            startTime = event.startTime,
                            allDay = event.allDay,
                            location = event.location
                        )
                    }
            }

            return
        }

        /*
         * Despite the historical function name, this is now the
         * read-side refresh for ALL readable Google calendars.
         *
         * CalendarSelectionStore is intentionally not consulted:
         * calendarsetup only controls the destination for ":"
         * event creation.
         */
        googleCalendarAuthManager
            .authorize(
                onAuthorized = {
                        accessToken ->

                    scope.launch {

                        val snapshot =
                            calendarAssistRepository
                                .loadAllCalendarsSnapshot(
                                    accessToken =
                                        accessToken,
                                    hiddenCalendarIds =
                                        calendarVisibilityStore
                                            .getHiddenCalendarIds(),
                                    agendaDays =
                                        8,
                                    nextEventDays =
                                        30
                                )

                        /*
                         * HOME CALENDAR CARD
                         *
                         * Shows the next timed event on the calendar
                         * picked in calendarsetup, never an all-day
                         * event. Before a calendar is picked, it uses
                         * every visible calendar, still skipping
                         * all-day events.
                         */
                        val now =
                            System.currentTimeMillis()

                        val homeCalendarId =
                            calendarSelectionStore
                                .getPersonalCalendarId()

                        val nextTimedEvent =
                            if (
                                homeCalendarId != null
                            ) {
                                runCatching {
                                    calendarAssistRepository
                                        .loadNextTimedEvent(
                                            accessToken =
                                                accessToken,
                                            calendarId =
                                                homeCalendarId
                                        )
                                }
                                    .getOrNull()
                            } else {
                                snapshot.agendaEvents
                                    .filter {
                                        !it.allDay &&
                                                it.startTime >= now
                                    }
                                    .minByOrNull {
                                        it.startTime
                                    }
                            }

                        upcomingEvent =
                            nextTimedEvent
                                ?.let {
                                        event ->

                                    UpcomingCalendarEvent(
                                        title =
                                            event.title,
                                        startTime =
                                            event.startTime,
                                        allDay =
                                            event.allDay,
                                        location =
                                            event.location
                                    )
                                }

                        calendarAgendaEvents =
                            snapshot.agendaEvents
                                .map {
                                        event ->

                                    UpcomingCalendarEvent(
                                        title =
                                            event.title,
                                        startTime =
                                            event.startTime,
                                        allDay =
                                            event.allDay,
                                        location =
                                            event.location
                                    )
                                }
                    }
                },
                onNeedsUserConsent = {

                    upcomingEvent =
                        null

                    calendarAgendaEvents =
                        emptyList()
                },
                onError = {

                    upcomingEvent =
                        null

                    calendarAgendaEvents =
                        emptyList()
                }
            )
    }

    fun createEventWithToken(
        accessToken: String,
        draft: CalendarEventDraft
    ) {

        val calendarId =
            when (
                draft.target
            ) {

                CalendarTarget.PERSONAL ->
                    calendarSelectionStore
                        .getPersonalCalendarId()

                CalendarTarget.FAMILY ->
                    calendarSelectionStore
                        .getFamilyCalendarId()
            }

        if (
            calendarId.isNullOrBlank()
        ) {

            statusText =
                "RUN CALENDARSETUP"

            return
        }

        val label =
            when (
                draft.target
            ) {

                CalendarTarget.PERSONAL ->
                    "PERSONAL"

                CalendarTarget.FAMILY ->
                    "FAMILY"
            }

        scope.launch {

            statusText =
                "ADDING EVENT"

            val result =
                calendarRepository
                    .createEvent(
                        accessToken =
                            accessToken,
                        calendarId =
                            calendarId,
                        calendarLabel =
                            label,
                        draft =
                            draft
                    )

            statusText =
                result.message

            if (
                result.success
            ) {

                refreshPersonalUpcomingEvent()
            }
        }
    }

    fun loadCalendarSetupWithToken(
        accessToken: String
    ) {

        scope.launch {

            statusText =
                "LOADING CALENDARS"

            val result =
                calendarRepository
                    .loadWritableCalendars(
                        accessToken
                    )

            /*
             * Closed while Google was answering: stay closed.
             */
            if (
                !calendarSetupWanted
            ) {
                calendarSetupLoading =
                    false

                statusText =
                    ""

                return@launch
            }

            if (
                result.success
            ) {

                calendarSetupOptions =
                    result.calendars

                calendarSetupUsesGoogle =
                    true

                calendarSetupLoading =
                    false

                showMessagesInbox =
                    false

                currentConversation =
                    null

                showNotes =
                    false

                currentNote =
                    null

                showCalendarSetup =
                    true

                statusText =
                    ""

            } else {

                calendarSetupLoading =
                    false

                statusText =
                    result.message
            }
        }
    }

    fun loadCalendarVisibilityWithToken(
        accessToken: String
    ) {

        scope.launch {

            calendarVisibilityLoading =
                true

            val calendars =
                calendarAssistRepository
                    .loadReadableCalendars(
                        accessToken
                    )

            calendarVisibilityOptions =
                calendars

            hiddenCalendarIds =
                calendarVisibilityStore
                    .getHiddenCalendarIds()

            calendarVisibilityLoading =
                false

            statusText =
                if (
                    calendars.isEmpty()
                ) {
                    "NO CALENDARS AVAILABLE"
                } else {
                    ""
                }
        }
    }


    val googleAuthorizationLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->

            /*
             * Google Identity can return valid authorization data
             * even when the activity result code is not RESULT_OK
             * on some account-selection / consent flows.
             *
             * Always try to read the authorization result first.
             * Only call the flow cancelled when no token was
             * actually returned.
             */
            val accessToken =
                googleCalendarAuthManager
                    .getAccessTokenFromResult(
                        result.data
                    )

            if (
                !accessToken.isNullOrBlank()
            ) {

                when (
                    pendingGoogleAction
                ) {

                    GoogleCalendarAction.CREATE_EVENT -> {

                        val draft =
                            pendingCalendarEvent

                        if (
                            draft != null
                        ) {

                            createEventWithToken(
                                accessToken =
                                    accessToken,
                                draft =
                                    draft
                            )
                        }
                    }

                    GoogleCalendarAction.LOAD_SETUP -> {

                        loadCalendarSetupWithToken(
                            accessToken
                        )
                    }

                    GoogleCalendarAction.LOAD_VISIBILITY -> {

                        loadCalendarVisibilityWithToken(
                            accessToken
                        )
                    }

                    null -> {
                    }
                }

                pendingCalendarEvent =
                    null

                pendingGoogleAction =
                    null

                return@rememberLauncherForActivityResult
            }

            pendingCalendarEvent =
                null

            pendingGoogleAction =
                null

            calendarVisibilityLoading =
                false

            calendarSetupLoading =
                false

            statusText =
                if (
                    result.resultCode ==
                    Activity.RESULT_CANCELED
                ) {
                    "GOOGLE AUTH CANCELLED"
                } else {
                    "GOOGLE AUTH FAILED"
                }
        }

    fun requestGoogleAuthorization(
        action: GoogleCalendarAction,
        draft: CalendarEventDraft? =
            null
    ) {

        pendingGoogleAction =
            action

        pendingCalendarEvent =
            draft

        statusText =
            "AUTHORIZING CALENDAR"

        googleCalendarAuthManager
            .authorize(
                onAuthorized = {
                        accessToken ->

                    when (
                        action
                    ) {

                        GoogleCalendarAction.CREATE_EVENT -> {

                            if (
                                draft != null
                            ) {

                                createEventWithToken(
                                    accessToken =
                                        accessToken,
                                    draft =
                                        draft
                                )
                            }
                        }

                        GoogleCalendarAction.LOAD_SETUP -> {

                            loadCalendarSetupWithToken(
                                accessToken
                            )
                        }

                        GoogleCalendarAction.LOAD_VISIBILITY -> {

                            loadCalendarVisibilityWithToken(
                                accessToken
                            )
                        }
                    }

                    pendingCalendarEvent =
                        null

                    pendingGoogleAction =
                        null
                },
                onNeedsUserConsent = {
                        pendingIntent ->

                    try {

                        val request =
                            IntentSenderRequest
                                .Builder(
                                    pendingIntent.intentSender
                                )
                                .build()

                        googleAuthorizationLauncher
                            .launch(
                                request
                            )

                    } catch (
                        _: Exception
                    ) {

                        pendingCalendarEvent =
                            null

                        pendingGoogleAction =
                            null

                        calendarVisibilityLoading =
                            false

                        calendarSetupLoading =
                            false

                        statusText =
                            "GOOGLE AUTH FAILED"
                    }
                },
                onError = {
                        message ->

                    pendingCalendarEvent =
                        null

                    pendingGoogleAction =
                        null

                    calendarVisibilityLoading =
                        false

                    calendarSetupLoading =
                        false

                    statusText =
                        message
                }
            )
    }

    LaunchedEffect(Unit) {

        while (
            true
        ) {

            refreshPersonalUpcomingEvent()

            delay(
                60_000
            )
        }
    }

    fun loadMailWithToken(
        accessToken: String,
        openInbox: Boolean
    ) {

        mailAccessToken =
            accessToken

        scope.launch {

            mailLoading =
                true

            mailSnoozeStore
                .dueItems()
                .forEach {
                        snoozed ->

                    val restored =
                        mailActionRepository
                            .restoreToInbox(
                                accessToken =
                                    accessToken,
                                messageId =
                                    snoozed.messageId
                            )

                    if (
                        restored
                    ) {

                        mailSnoozeStore
                            .remove(
                                snoozed.messageId
                            )
                    }
                }

            if (
                openInbox
            ) {

                showMessagesInbox =
                    false

                showCalendarSetup =
                    false

                showNotes =
                    false

                currentNote =
                    null

                currentConversation =
                    null

                currentMailMessage =
                    null

                showMailInbox =
                    true
            }

            val result =
                gmailRepository
                    .loadInbox(
                        accessToken
                    )

            if (
                result.success
            ) {

                mailMessages =
                    result.messages

            } else if (
                openInbox
            ) {

                statusText =
                    result.message
            }

            mailLoading =
                false
        }
    }

    val mailAuthorizationLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->

            val accessToken =
                googleMailAuthManager
                    .getAccessTokenFromResult(
                        result.data
                    )

            if (
                !accessToken.isNullOrBlank()
            ) {

                loadMailWithToken(
                    accessToken =
                        accessToken,
                    openInbox =
                        true
                )

                return@rememberLauncherForActivityResult
            }

            statusText =
                if (
                    result.resultCode ==
                    Activity.RESULT_CANCELED
                ) {
                    "GOOGLE MAIL AUTH CANCELLED"
                } else {
                    "GOOGLE MAIL AUTH FAILED"
                }
        }

    fun requestMailAuthorization(
        openInbox: Boolean,
        allowConsent: Boolean
    ) {

        googleMailAuthManager
            .authorize(
                onAuthorized = {
                        accessToken ->

                    mailNeedsAuthorization =
                        false

                    loadMailWithToken(
                        accessToken =
                            accessToken,
                        openInbox =
                            openInbox
                    )
                },
                onNeedsUserConsent = {
                        pendingIntent ->

                    if (
                        !allowConsent
                    ) {

                        /*
                         * Nothing may prompt here, so record that mail
                         * is reachable but unauthorized. Screens show
                         * a connect affordance instead of an empty
                         * list that looks broken.
                         */
                        mailNeedsAuthorization =
                            true

                        return@authorize
                    }

                    try {

                        val request =
                            IntentSenderRequest
                                .Builder(
                                    pendingIntent.intentSender
                                )
                                .build()

                        mailAuthorizationLauncher
                            .launch(
                                request
                            )

                    } catch (
                        _: Exception
                    ) {

                        statusText =
                            "GOOGLE MAIL AUTH FAILED"
                    }
                },
                onError = {
                        message ->

                    if (
                        openInbox
                    ) {

                        statusText =
                            message
                    }
                }
            )
    }

    LaunchedEffect(Unit) {

        requestMailAuthorization(
            openInbox =
                false,
            allowConsent =
                false
        )
    }

    LaunchedEffect(
        mailAccessToken
    ) {

        while (
            true
        ) {

            val token =
                mailAccessToken

            if (
                !token.isNullOrBlank()
            ) {

                mailSnoozeStore
                    .dueItems()
                    .forEach {
                            snoozed ->

                        val restored =
                            mailActionRepository
                                .restoreToInbox(
                                    accessToken =
                                        token,
                                    messageId =
                                        snoozed.messageId
                                )

                        if (
                            restored
                        ) {

                            mailSnoozeStore
                                .remove(
                                    snoozed.messageId
                                )
                        }
                    }
            }

            delay(
                60_000
            )
        }
    }

    /*
     * Opens the conversation with this number in whichever app is the
     * default for SMS (Google Messages, Samsung Messages, ...).
     */
    fun openThreadInDefaultMessagesApp(
        phoneNumber: String
    ) {

        runCatching {
            context.startActivity(
                Intent(
                    Intent.ACTION_SENDTO,
                    Uri.parse(
                        "smsto:" + phoneNumber
                    )
                )
            )
        }
            .onFailure {
                statusText =
                    "SENT, BUT COULD NOT OPEN MESSAGES"
            }
    }

    fun actuallySendSms(
        contact: Contact,
        message: String,
        openMessagesAfter: Boolean = false
    ) {

        smsSender.sendSms(
            phoneNumber =
                contact.phoneNumber,
            message =
                message
        ) { success, result ->

            if (
                !success
            ) {

                statusText =
                    "SMS FAILED: $result"
            }
        }

        /*
         * The send is already queued, and Android records a text sent
         * this way in the phone's message history, so the messaging
         * app shows it in the thread.
         */
        if (
            openMessagesAfter
        ) {
            openThreadInDefaultMessagesApp(
                contact.phoneNumber
            )
        }
    }

    val smsPermissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.RequestPermission()
        ) { granted ->

            hasSmsPermission =
                granted

            if (
                granted
            ) {

                val contact =
                    pendingSmsContact

                val message =
                    pendingSmsMessage

                if (
                    contact != null &&
                    message != null
                ) {

                    actuallySendSms(
                        contact,
                        message,
                        openMessagesAfter =
                            pendingSmsOpenMessagesAfter
                    )
                }

            } else {

                statusText =
                    "SMS PERMISSION DENIED"
            }

            pendingSmsContact =
                null

            pendingSmsMessage =
                null

            pendingSmsOpenMessagesAfter =
                false
        }

    val smsRoleLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.StartActivityForResult()
        ) {

            statusText =
                if (
                    smsRoleManager
                        .isDefaultSmsApp()
                ) {

                    "✓ SMS READY"

                } else {

                    "SMS APP NOT CHANGED"
                }
        }

    LaunchedEffect(Unit) {

        /*
         * First-run permission prompts belong to the launcher, not to
         * the prompt drawn over another app.
         */
        if (
            assistOverlay
        ) {
            return@LaunchedEffect
        }

        if (
            !hasContactsPermission
        ) {

            contactsPermissionLauncher.launch(
                Manifest.permission.READ_CONTACTS
            )

        } else if (
            !hasCalendarPermission
        ) {

            calendarPermissionLauncher.launch(
                Manifest.permission.READ_CALENDAR
            )
        }
    }

    LaunchedEffect(
        hasContactsPermission
    ) {

        if (
            !assistOverlay &&
            hasContactsPermission &&
            !hasCalendarPermission
        ) {

            calendarPermissionLauncher.launch(
                Manifest.permission.READ_CALENDAR
            )
        }
    }

    fun sendMessage(
        contact: Contact,
        message: String,
        openMessagesAfter: Boolean = false
    ) {

        if (
            !hasSmsPermission
        ) {

            pendingSmsContact =
                contact

            pendingSmsMessage =
                message

            pendingSmsOpenMessagesAfter =
                openMessagesAfter

            smsPermissionLauncher.launch(
                Manifest.permission.SEND_SMS
            )

            return
        }

        actuallySendSms(
            contact,
            message,
            openMessagesAfter =
                openMessagesAfter
        )
    }

    fun sendMailDraft(
        draft: MailComposeDraft,
        toAddress: String,
        subject: String,
        body: String
    ) {

        val token =
            mailAccessToken

        if (
            token.isNullOrBlank()
        ) {

            statusText =
                "OPEN MAIL TO AUTHORIZE"

            return
        }

        if (
            toAddress.isBlank()
        ) {

            statusText =
                "RECIPIENT REQUIRED"

            return
        }

        if (
            subject.isBlank()
        ) {

            statusText =
                "SUBJECT REQUIRED"

            return
        }

        val outgoingBody =
            if (
                draft.isForward &&
                draft.forwardedBody.isNotBlank()
            ) {

                buildString {

                    if (
                        body.isNotBlank()
                    ) {

                        append(
                            body.trimEnd()
                        )

                        append(
                            "\n\n"
                        )
                    }

                    append(
                        draft.forwardedBody
                    )
                }

            } else {

                body.trimEnd()
            }

        if (
            outgoingBody.isBlank()
        ) {

            statusText =
                "MESSAGE IS EMPTY"

            return
        }

        scope.launch {

            statusText =
                "SENDING EMAIL"

            val result =
                gmailRepository
                    .sendEmail(
                        accessToken = token,
                        toAddress = toAddress.trim(),
                        subject = subject.trim(),
                        body = outgoingBody,
                        threadId = draft.threadId,
                        inReplyToMessageId = draft.inReplyToMessageId
                    )

            statusText =
                result.message

            if (
                result.success
            ) {

                mailComposeDraft =
                    null

                if (
                    draft.isReply ||
                    draft.isForward
                ) {

                    currentMailMessage =
                        null

                    returnToHubAfterMail =
                        false

                    showHub =
                        false

                    loadMailWithToken(
                        accessToken =
                            token,
                        openInbox =
                            true
                    )

                } else {

                    requestMailAuthorization(
                        openInbox =
                            false,
                        allowConsent =
                            false
                    )
                }
            }
        }
    }

    fun openConversation(
        contact: Contact
    ) {

        scope.launch {

            /*
             * A group is marked read by thread; matching on a single
             * number would only clear that member's messages.
             */
            val thread =
                currentConversationThread

            if (
                thread != null
            ) {

                smsDatabase
                    .smsDao()
                    .markThreadRead(
                        thread.threadKey
                    )

            } else {

                smsDatabase
                    .smsDao()
                    .markConversationRead(
                        contact.phoneNumber
                    )
            }
        }

        showHub =
            false

        showMessagesInbox =
            false

        showCalendarSetup =
            false

        showMailInbox =
            false

        currentMailMessage =
            null

        showNotes =
            false

        currentNote =
            null

        currentConversation =
            contact
    }

    @Composable
    fun RenderHomeScreen(
        commandOverlayOnly: Boolean = false,
        autoFocusCommandInput: Boolean = true,
        onCommandOverlayActiveChange: (Boolean) -> Unit = {},
        promptAlwaysOpen: Boolean = false
    ) {

        TitanHomeScreen(
            homeRequestVersion =
                homeRequestVersion,
            contactRepository =
                contactRepository,
            smsDatabase =
                smsDatabase,
            smsRoleManager =
                smsRoleManager,
            todoistTokenStore =
                todoistTokenStore,
            todoistRepository =
                todoistRepository,
            smsRoleLauncher = {
                    intent ->

                smsRoleLauncher.launch(
                    intent
                )
            },
            hasContactsPermission =
                hasContactsPermission,
            hasCalendarPermission =
                hasCalendarPermission,
            onRequestContactsPermission = {

                contactsPermissionLauncher.launch(
                    Manifest.permission.READ_CONTACTS
                )
            },
            onRequestCalendarPermission = {

                calendarPermissionLauncher.launch(
                    Manifest.permission.READ_CALENDAR
                )
            },
            onCreateCalendarEvent = {
                    draft ->

                val phoneCalendarId =
                    when (draft.target) {
                        CalendarTarget.PERSONAL -> calendarSelectionStore.getPersonalCalendarId()
                        CalendarTarget.FAMILY -> calendarSelectionStore.getFamilyCalendarId()
                    }

                if (
                    !calendarSelectionStore.usesGoogle()
                ) {

                    /*
                     * Phone calendars: add it directly, asking for
                     * write access or a calendar first if needed.
                     */
                    if (
                        phoneCalendarId == null ||
                        !DeviceCalendarRepository.isDeviceId(phoneCalendarId)
                    ) {
                        statusText =
                            "PICK A CALENDAR FIRST"

                        openPhoneCalendarSetup()

                    } else if (
                        !DeviceCalendarRepository.canWrite(context)
                    ) {
                        pendingPhoneCalendarDraft =
                            draft

                        calendarWritePermissionLauncher.launch(
                            Manifest.permission.WRITE_CALENDAR
                        )

                    } else {
                        scope.launch {
                            val result =
                                DeviceCalendarRepository.createEvent(
                                    context,
                                    phoneCalendarId,
                                    draft
                                )

                            statusText =
                                result.message

                            if (result.success) {
                                refreshPersonalUpcomingEvent()
                            }
                        }
                    }

                } else if (
                    !calendarSelectionStore
                        .isConfigured()
                ) {

                    statusText =
                        "RUN CALENDARSETUP"

                } else {

                    requestGoogleAuthorization(
                        action =
                            GoogleCalendarAction.CREATE_EVENT,
                        draft =
                            draft
                    )
                }
            },
            onCalendarSetup = {

                calendarSetupWanted =
                    true

                if (
                    calendarSelectionStore.usesGoogle()
                ) {
                    calendarSetupUsesGoogle =
                        true

                    requestGoogleAuthorization(
                        action =
                            GoogleCalendarAction.LOAD_SETUP
                    )
                } else {
                    openPhoneCalendarSetup()
                }
            },
            calendarSetupUsesGoogle =
                calendarSetupUsesGoogle,
            calendarSetupLoading =
                calendarSetupLoading,
            onCalendarSetupSource = { google ->

                calendarSetupWanted =
                    true

                if (google) {
                    calendarSetupUsesGoogle =
                        true

                    calendarSetupOptions =
                        emptyList()

                    calendarSetupLoading =
                        true

                    requestGoogleAuthorization(
                        action =
                            GoogleCalendarAction.LOAD_SETUP
                    )
                } else {
                    openPhoneCalendarSetup()
                }
            },
            /*
             * Draw the calendar card once, in the layer the user can
             * see. While Settings, Hub, Notes, Mail or Messages is
             * open, home is off screen and the command layer on top
             * draws it; a second copy on the hidden home would hold
             * focus and keys (Enter, Esc) away from the visible one.
             */
            showCalendarSetup =
                showCalendarSetup &&
                        (
                                commandOverlayOnly ||
                                        !(
                                                currentConversation != null ||
                                                        currentNote != null ||
                                                        mailComposeDraft != null ||
                                                        currentMailMessage != null ||
                                                        showMailInbox ||
                                                        showMessagesInbox ||
                                                        showNotes ||
                                                        showHub ||
                                                        showSettings
                                                )
                                ),
            calendarSetupOptions =
                calendarSetupOptions,
            currentCalendarId =
                calendarSelectionStore
                    .getPersonalCalendarId(),
            onSaveCalendarSelection = {
                    selected ->

                /*
                 * The selected calendar is the default
                 * destination for normal ":" event commands.
                 *
                 * Keep an actual Google calendar named Family
                 * mapped to the FAMILY route so explicit
                 * family-event commands remain separate.
                 */
                val familyCalendar =
                    calendarSetupOptions
                        .firstOrNull {
                                option ->

                            option.summary
                                .trim()
                                .equals(
                                    "Family",
                                    ignoreCase =
                                        true
                                )
                        }
                        ?: calendarSetupOptions
                            .firstOrNull {
                                    option ->

                                option.id ==
                                        calendarSelectionStore
                                            .getFamilyCalendarId()
                            }
                        ?: selected

                val pickedPhone =
                    DeviceCalendarRepository.isDeviceId(selected.id)

                calendarSelectionStore
                    .setUsesGoogle(
                        !pickedPhone
                    )

                if (
                    pickedPhone &&
                    !DeviceCalendarRepository.canWrite(context)
                ) {
                    calendarWritePermissionLauncher.launch(
                        Manifest.permission.WRITE_CALENDAR
                    )
                }

                calendarSelectionStore
                    .saveSelections(
                        personalCalendarId =
                            selected.id,
                        personalCalendarName =
                            selected.summary,
                        familyCalendarId =
                            familyCalendar.id,
                        familyCalendarName =
                            familyCalendar.summary
                    )

                calendarSetupWanted =
                    false

                showCalendarSetup =
                    false

                refreshPersonalUpcomingEvent()

                statusText =
                    "✓ EVENT CALENDAR SAVED"
            },
            onDismissCalendarSetup = {

                calendarSetupWanted =
                    false

                pendingPhoneCalendarSetup =
                    false

                if (
                    pendingGoogleAction == GoogleCalendarAction.LOAD_SETUP
                ) {
                    pendingGoogleAction =
                        null
                }

                showCalendarSetup =
                    false

                calendarSetupLoading =
                    false

                statusText =
                    ""
            },
            onCalendarStatus = {

                statusText =
                    if (
                        calendarSelectionStore
                            .isConfigured()
                    ) {

                        "✓ CALENDARS CONFIGURED"

                    } else {

                        "CALENDARS NOT CONFIGURED"
                    }
            },
            onCreateNote = {
                    body,
                    category ->

                if (
                    body.isBlank()
                ) {

                    statusText =
                        "NOTE IS EMPTY"

                } else {

                    val title =
                        body
                            .lineSequence()
                            .firstOrNull()
                            ?.trim()
                            ?.take(80)
                            ?.ifBlank { "New note" }
                            ?: "New note"

                    /*
                     * The typed text is the title. Leaving the body
                     * empty means opening the note starts on a blank
                     * page rather than with the title repeated back.
                     */
                    noteStore.addNote(
                        title = title,
                        body = "",
                        category = category
                    )

                    statusText =
                        "✓ ${category.name} NOTE SAVED"
                }
            },
            onOpenNotes = {

                showNotes =
                    true
            },
            mailMessages =
                mailMessages,
            onOpenMail = {

                /*
                 * Email works like texting: it opens in the user's
                 * email app, not inside Prompt Launcher.
                 */
                if (
                    !openMailApp(
                        context,
                        EnabledMailApps
                    )
                ) {
                    statusText =
                        "NO EMAIL APP FOUND"
                }
            },
            onComposeMail = {
                    emailContact ->

                /*
                 * A mail app chosen in settings writes the email
                 * there instead of in Prompt Launcher.
                 */
                /*
                 * The "write in" chips on the address list can
                 * override the setting for this one email.
                 */
                val composeTarget =
                    MailWriteInOnce
                        ?: MailComposeAppPackage

                MailWriteInOnce =
                    null

                if (
                    composeTarget.isNotBlank()
                ) {
                    if (
                        !composeInMailApp(
                            context,
                            composeTarget,
                            emailContact.emailAddress
                        )
                    ) {
                        statusText =
                            "NO MAIL APP FOUND"
                    }

                    return@TitanHomeScreen
                }

                currentConversation =
                    null

                showMessagesInbox =
                    false

                showCalendarSetup =
                    false

                showMailInbox =
                    false

                currentMailMessage =
                    null

                showNotes =
                    false

                currentNote =
                    null

                mailComposeDraft =
                    MailComposeDraft(
                        toAddress = emailContact.emailAddress,
                        toLabel = emailContact.name
                    )
            },
            onOpenMessagesInbox = {

                showMessagesInbox =
                    true
            },
            onOpenConversation = {
                    contact ->

                openConversation(
                    contact
                )
            },
            onSendMessage = {
                    contact,
                    message ->

                sendMessage(
                    contact,
                    message
                )
            },
            onSendMessageAndOpenMessages = {
                    contact,
                    message ->

                sendMessage(
                    contact,
                    message,
                    openMessagesAfter = true
                )
            },
            onOpenGroupThread = {
                    participants,
                    names,
                    draft ->

                val key =
                    threadKeyFor(
                        participants
                    )

                currentConversationThread =
                    ConversationThread(
                        threadKey =
                            key,
                        title =
                            names.joinToString(
                                separator = ", "
                            ) {
                                it.substringBefore(" ")
                            },
                        participants =
                            key
                                .split(",")
                                .filter {
                                    it.isNotBlank()
                                },
                        isGroup =
                            true,
                        draft =
                            draft
                    )

                openConversation(
                    Contact(
                        name =
                            names.joinToString(
                                separator = ", "
                            ),
                        phoneNumber =
                            participants.firstOrNull()
                                ?: ""
                    )
                )
            },
            onOpenHub = {

                showHub =
                    true

                requestMailAuthorization(
                    openInbox =
                        false,
                    allowConsent =
                        false
                )

                if (
                    !hasCallLogPermission
                ) {

                    callLogPermissionLauncher.launch(
                        Manifest.permission.READ_CALL_LOG
                    )
                }
            },
            upcomingEvent =
                upcomingEvent,
            calendarAgendaEvents =
                calendarAgendaEvents,
            statusText =
                statusText,
            setStatusText = {

                statusText =
                    it
            },
            commandOverlayOnly =
                commandOverlayOnly,
            autoFocusCommandInput =
                autoFocusCommandInput,
            onCommandOverlayActiveChange =
                onCommandOverlayActiveChange,
            promptAlwaysOpen =
                promptAlwaysOpen
        )
    }

    var globalCommandOverlayActive by remember {
        mutableStateOf(
            false
        )
    }

    /*
     * Home already owns its normal prompt bar. Everywhere else inside
     * Prompt Launcher gets a transparent command-only layer on top of
     * the currently visible page.
     *
     * This means a quick command can be entered from Mail, Messages,
     * Notes lists, Hub, Settings, detail screens, etc. Quick actions
     * such as adding a note/task/event do not change the underlying
     * page, so after Enter the user is immediately back where they were.
     */
    val useGlobalCommandLayer =
        currentConversation != null ||
                currentNote != null ||
                mailComposeDraft != null ||
                currentMailMessage != null ||
                showMailInbox ||
                showMessagesInbox ||
                showNotes ||
                showHub ||
                showSettings

    /*
     * Screens with an active writing field keep that field's normal
     * typing behavior. Everywhere else can hand the physical keyboard
     * directly to the launcher command field.
     *
     * This prevents global commands from breaking SMS replies, note
     * editing, or an email that is actively being composed.
     */
    val preserveEditorTypingFocus =
        currentConversation != null ||
                currentNote != null ||
                mailComposeDraft != null

    /*
     * Prompt over other apps (PromptAnywhereActivity, the assist
     * entry point). Only the command layer is drawn, on a light scrim
     * over the app the user came from. A quick command finishes and
     * drops the user back in that app; anything that needs a full
     * Daylight screen hands off to MainActivity instead.
     */
    if (
        assistOverlay
    ) {

        var assistPromptActive by remember {
            mutableStateOf(
                false
            )
        }

        var assistPromptWasActive by remember {
            mutableStateOf(
                false
            )
        }

        var assistStatusAtActivation by remember {
            mutableStateOf(
                ""
            )
        }

        /*
         * A command asked for a launcher screen (hub, notes, settings,
         * messages, a conversation, mail...). Those only exist in the
         * full launcher, so open Daylight and close the overlay.
         */
        LaunchedEffect(
            useGlobalCommandLayer
        ) {

            if (
                useGlobalCommandLayer
            ) {

                runCatching {
                    activity.startActivity(
                        Intent(
                            context,
                            MainActivity::class.java
                        ).apply {
                            addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK
                            )
                        }
                    )
                }

                activity.finish()
            }
        }

        /*
         * The prompt went from active to idle with a fresh "✓ ..."
         * confirmation: the command ran. Leave the confirmation up
         * briefly, then return to the other app. Clearing the prompt
         * by hand (no new confirmation) or an error message keeps the
         * overlay open so the user can try again.
         */
        LaunchedEffect(
            assistPromptActive,
            statusText
        ) {

            if (
                !assistPromptActive &&
                assistPromptWasActive &&
                statusText.startsWith("✓") &&
                statusText != assistStatusAtActivation
            ) {

                delay(
                    900
                )

                activity.finish()
            }
        }

        /*
         * Registered before the command layer, so its own back
         * handling (locked contact, assistant answer, pickers) still
         * wins while one of those is open.
         */
        BackHandler {
            activity.finish()
        }

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .onPreviewKeyEvent {
                            event ->

                        /*
                         * Esc with nothing open goes straight back to
                         * the other app. While the prompt or an
                         * overlay is open, the key passes through to
                         * it (and falls back to Back if unhandled).
                         */
                        if (
                            event.key == Key.Escape &&
                            !assistPromptActive
                        ) {

                            if (
                                event.type == KeyEventType.KeyUp
                            ) {
                                activity.finish()
                            }

                            true
                        } else if (
                            /*
                             * Backspace on an empty prompt closes it
                             * too. Checked on key down, before the
                             * field sees it, so the press that deletes
                             * the last letter doesn't also close.
                             */
                            event.key == Key.Backspace &&
                            event.type == KeyEventType.KeyDown &&
                            !assistPromptActive
                        ) {

                            activity.finish()

                            true
                        } else {
                            false
                        }
                    }
        ) {

            /*
             * No scrim of its own: the window blurs the app behind
             * (Theme.Daylight.Overlay) and the prompt frosts it, the
             * same look as typing on the home screen.
             */
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .pointerInput(
                            assistPromptActive
                        ) {

                            detectTapGestures {

                                if (
                                    !assistPromptActive
                                ) {
                                    activity.finish()
                                }
                            }
                        }
            )

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
            ) {

                RenderHomeScreen(
                    commandOverlayOnly =
                        true,
                    autoFocusCommandInput =
                        true,
                    promptAlwaysOpen =
                        true,
                    onCommandOverlayActiveChange = {
                            active ->

                        if (
                            active &&
                            !assistPromptActive
                        ) {

                            assistStatusAtActivation =
                                statusText

                            assistPromptWasActive =
                                true
                        }

                        assistPromptActive =
                            active
                    }
                )


            }
        }

        return
    }

    Box(
        modifier =
            Modifier.fillMaxSize()
    ) {

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .blur(
                        if (
                            useGlobalCommandLayer &&
                            globalCommandOverlayActive
                        ) {
                            3.5.dp
                        } else {
                            0.dp
                        }
                    )
        ) {

            when {

                currentConversation != null -> {

                    val closeConversation = {

                        currentConversation =
                            null

                        currentConversationThread =
                            null

                        if (
                            returnToHubAfterConversation
                        ) {

                            returnToHubAfterConversation =
                                false

                            showMessagesInbox =
                                false

                            showHub =
                                true

                        } else {

                            showMessagesInbox =
                                true

                            showHub =
                                false
                        }

                        statusText =
                            ""
                    }

                    FloatingDetailPage(
                        onDismiss =
                            closeConversation,
                        backgroundContent = {

                            if (
                                returnToHubAfterConversation
                            ) {

                                HubScreen(
                                    smsDatabase =
                                        smsDatabase,
                                    contactRepository =
                                        contactRepository,
                                    hasContactsPermission =
                                        hasContactsPermission,
                                    mailMessages =
                                        mailMessages,
                                    callLogEntries =
                                        callLogEntries,
                                    mutedNotificationApps =
                                        launcherSettings.mutedNotificationApps,
                                    hasCallLogPermission =
                                        hasCallLogPermission,
                                    selectedCategory =
                                        hubSelectedCategory,
                                    onSelectedCategoryChange = {
                                            category ->

                                        hubSelectedCategory =
                                            category
                                    },
                                    selectedIndex =
                                        hubSelectedIndex,
                                    onSelectedIndexChange = {
                                            index ->

                                        hubSelectedIndex =
                                            index
                                    },
                                    onRequestCallLogPermission = {
                                    },
                                    onOpenConversation = {
                                    },
                                    onOpenMail = {
                                    },
                                    onDial = {
                                    },
                                    onBack = {
                                    }
                                )

                            } else {

                                MessagesInboxScreen(
                                    smsDatabase =
                                        smsDatabase,
                                    contactRepository =
                                        contactRepository,
                                    hasContactsPermission =
                                        hasContactsPermission,
                                    onOpenConversation = {
                                    },
                                    onOpenThread = {
                                            _,
                                            _ ->
                                    },
                                    onBack = {
                                    }
                                )
                            }
                        },
                        content = {

                            ConversationScreen(
                                contact =
                                    currentConversation!!,
                                thread =
                                    currentConversationThread,
                                smsDatabase =
                                    smsDatabase,
                                statusText =
                                    statusText,
                                onSend = {
                                        message ->

                                    sendMessage(
                                        currentConversation!!,
                                        message
                                    )
                                },
                                onBack =
                                    closeConversation,
                                floatingMode =
                                    true
                            )
                        }
                    )
                }

                currentNote != null -> {

                    SwipeRightToHomeContainer(
                        edgeOnly =
                            false,
                        edgeTopPaddingDp =
                            36,
                        onReturnHome = {

                            val noteToClose = currentNote!!

                            if (
                                noteToClose.body.isBlank() &&
                                noteToClose.title.trim().let { it.isBlank() || it == "New note" }
                            ) {
                                noteStore.deleteNote(noteToClose.id)
                            }

                            showNotes =
                                true

                            currentNote =
                                null

                            showHub =
                                false

                            statusText =
                                ""
                        },
                        homeContent = {

                            NotesScreen(
                                noteStore =
                                    noteStore,
                                onOpenNote = {
                                },
                                onCreateNote = {
                                },
                                onBack = {
                                }
                            )
                        },
                        screenContent = {

                            NoteDetailScreen(
                                note =
                                    currentNote!!,
                                noteStore =
                                    noteStore,
                                onNoteUpdated = { updated ->
                                    currentNote = updated
                                },
                                onBack = {

                                    val noteToClose = currentNote!!

                                    if (
                                        noteToClose.body.isBlank() &&
                                        noteToClose.title.trim().let { it.isBlank() || it == "New note" }
                                    ) {
                                        noteStore.deleteNote(noteToClose.id)
                                    }

                                    showNotes =
                                        true

                                    currentNote =
                                        null

                                    statusText =
                                        ""
                                }
                            )
                        }
                    )
                }

                mailComposeDraft != null -> {

                    SwipeRightToHomeContainer(
                        edgeOnly =
                            false,
                        onReturnHome = {

                            mailComposeDraft =
                                null

                            statusText =
                                ""
                        },
                        homeContent = {

                            if (
                                currentMailMessage != null
                            ) {

                                MailDetailScreen(
                                    message =
                                        currentMailMessage!!,
                                    onReply = {
                                    },
                                    onReplyAll = {
                                    },
                                    onForward = {
                                    },
                                    onArchive = {
                                    },
                                    onSnooze24Hours = {
                                    },
                                    onTrash = {
                                    },
                                    onBack = {
                                    }
                                )

                            } else {

                                RenderHomeScreen(
                                    autoFocusCommandInput =
                                        false
                                )
                            }
                        },
                        screenContent = {

                            MailComposeScreen(
                                draft =
                                    mailComposeDraft!!,
                                statusText =
                                    statusText,
                                onSend = {
                                        toAddress,
                                        subject,
                                        body ->

                                    sendMailDraft(
                                        draft = mailComposeDraft!!,
                                        toAddress = toAddress,
                                        subject = subject,
                                        body = body
                                    )
                                },
                                onBack = {

                                    mailComposeDraft =
                                        null

                                    statusText =
                                        ""
                                }
                            )
                        }
                    )
                }

                currentMailMessage != null -> {

                    val closeMailDetail = {

                        currentMailMessage =
                            null

                        if (
                            returnToHubAfterMail
                        ) {

                            returnToHubAfterMail =
                                false

                            showMailInbox =
                                false

                            showHub =
                                true

                        } else {

                            showMailInbox =
                                true

                            showHub =
                                false
                        }

                        statusText =
                            ""
                    }

                    FloatingDetailPage(
                        onDismiss =
                            closeMailDetail,
                        backgroundContent = {

                            if (
                                returnToHubAfterMail
                            ) {

                                HubScreen(
                                    smsDatabase =
                                        smsDatabase,
                                    contactRepository =
                                        contactRepository,
                                    hasContactsPermission =
                                        hasContactsPermission,
                                    mailMessages =
                                        mailMessages,
                                    callLogEntries =
                                        callLogEntries,
                                    mutedNotificationApps =
                                        launcherSettings.mutedNotificationApps,
                                    hasCallLogPermission =
                                        hasCallLogPermission,
                                    selectedCategory =
                                        hubSelectedCategory,
                                    onSelectedCategoryChange = {
                                            category ->

                                        hubSelectedCategory =
                                            category
                                    },
                                    selectedIndex =
                                        hubSelectedIndex,
                                    onSelectedIndexChange = {
                                            index ->

                                        hubSelectedIndex =
                                            index
                                    },
                                    onRequestCallLogPermission = {
                                    },
                                    onOpenConversation = {
                                    },
                                    onOpenMail = {
                                    },
                                    onDial = {
                                    },
                                    onBack = {
                                    }
                                )

                            } else {

                                MailInboxScreen(
                                    messages =
                                        mailMessages,
                                    loading =
                                        mailLoading,
                                    onOpenMessage = {
                                    },
                                    onTrashMessage = {
                                    },
                                    onMarkUnread = {
                                    },
                                    onRefresh = {
                                    },
                                    onBack = {
                                    }
                                )
                            }
                        },
                        content = {

                            MailDetailScreen(
                                message =
                                    currentMailMessage!!,
                                onReply = {
                                        message ->

                                    if (
                                        message.senderEmail.isBlank()
                                    ) {

                                        statusText =
                                            "REPLY ADDRESS NOT FOUND"

                                    } else {

                                        val replySubject =
                                            if (
                                                message.subject.startsWith(
                                                    "Re:",
                                                    ignoreCase =
                                                        true
                                                )
                                            ) {

                                                message.subject

                                            } else {

                                                "Re: ${message.subject}"
                                            }

                                        mailComposeDraft =
                                            MailComposeDraft(
                                                toAddress =
                                                    message.senderEmail,
                                                toLabel =
                                                    message.sender,
                                                subject =
                                                    replySubject,
                                                threadId =
                                                    message.threadId,
                                                inReplyToMessageId =
                                                    message.messageIdHeader,
                                                isReply =
                                                    true
                                            )
                                    }
                                },
                                onReplyAll = {
                                        message ->

                                    val token =
                                        mailAccessToken

                                    if (
                                        token.isNullOrBlank()
                                    ) {

                                        statusText =
                                            "MAIL AUTH REQUIRED"

                                    } else {

                                        scope.launch {

                                            val recipients =
                                                mailReplySupportRepository
                                                    .loadReplyAllRecipients(
                                                        accessToken =
                                                            token,
                                                        messageId =
                                                            message.id,
                                                        senderEmail =
                                                            message.senderEmail
                                                    )
                                                    .recipients

                                            if (
                                                recipients.isEmpty()
                                            ) {

                                                statusText =
                                                    "REPLY ADDRESS NOT FOUND"

                                            } else {

                                                val replySubject =
                                                    if (
                                                        message.subject.startsWith(
                                                            "Re:",
                                                            ignoreCase =
                                                                true
                                                        )
                                                    ) {

                                                        message.subject

                                                    } else {

                                                        "Re: ${message.subject}"
                                                    }

                                                mailComposeDraft =
                                                    MailComposeDraft(
                                                        toAddress =
                                                            recipients.joinToString(
                                                                ", "
                                                            ),
                                                        toLabel =
                                                            if (
                                                                recipients.size ==
                                                                1
                                                            ) {
                                                                recipients.first()
                                                            } else {
                                                                "${recipients.size} recipients"
                                                            },
                                                        subject =
                                                            replySubject,
                                                        threadId =
                                                            message.threadId,
                                                        inReplyToMessageId =
                                                            message.messageIdHeader,
                                                        isReply =
                                                            true,
                                                        isReplyAll =
                                                            true
                                                    )
                                            }
                                        }
                                    }
                                },
                                onForward = {
                                        message ->

                                    val forwardSubject =
                                        if (
                                            message.subject.startsWith(
                                                "Fwd:",
                                                ignoreCase =
                                                    true
                                            )
                                        ) {

                                            message.subject

                                        } else {

                                            "Fwd: ${message.subject}"
                                        }

                                    val forwardedBlock =
                                        buildString {

                                            append(
                                                "---------- forwarded message ----------\n"
                                            )

                                            append(
                                                "from: ${message.sender}"
                                            )

                                            if (
                                                message.senderEmail.isNotBlank()
                                            ) {

                                                append(
                                                    " <${message.senderEmail}>"
                                                )
                                            }

                                            append(
                                                "\n"
                                            )

                                            append(
                                                "date: ${formatMailDate(message.timestamp)}\n"
                                            )

                                            append(
                                                "subject: ${message.subject}\n\n"
                                            )

                                            append(
                                                message.body
                                            )
                                        }

                                    mailComposeDraft =
                                        MailComposeDraft(
                                            toAddress =
                                                "",
                                            toLabel =
                                                "",
                                            subject =
                                                forwardSubject,
                                            isForward =
                                                true,
                                            recipientEditable =
                                                true,
                                            forwardedBody =
                                                forwardedBlock
                                        )
                                },
                                onArchive = {
                                        message ->

                                    val token =
                                        mailAccessToken

                                    if (
                                        token.isNullOrBlank()
                                    ) {

                                        statusText =
                                            "MAIL AUTH REQUIRED"

                                    } else {

                                        scope.launch {

                                            val success =
                                                mailActionRepository
                                                    .archiveMessage(
                                                        accessToken =
                                                            token,
                                                        messageId =
                                                            message.id
                                                    )

                                            if (
                                                success
                                            ) {

                                                mailMessages =
                                                    mailMessages
                                                        .filterNot {
                                                            it.id ==
                                                                    message.id
                                                        }

                                                currentMailMessage =
                                                    null

                                                returnToHubAfterMail =
                                                    false

                                                showHub =
                                                    false

                                                loadMailWithToken(
                                                    accessToken =
                                                        token,
                                                    openInbox =
                                                        true
                                                )

                                            } else {

                                                statusText =
                                                    "MAIL ARCHIVE FAILED"
                                            }
                                        }
                                    }
                                },
                                onSnooze24Hours = {
                                        message ->

                                    val token =
                                        mailAccessToken

                                    if (
                                        token.isNullOrBlank()
                                    ) {

                                        statusText =
                                            "MAIL AUTH REQUIRED"

                                    } else {

                                        scope.launch {

                                            val archived =
                                                mailActionRepository
                                                    .archiveMessage(
                                                        accessToken =
                                                            token,
                                                        messageId =
                                                            message.id
                                                    )

                                            if (
                                                archived
                                            ) {

                                                mailSnoozeStore
                                                    .snoozeFor24Hours(
                                                        message.id
                                                    )

                                                mailMessages =
                                                    mailMessages
                                                        .filterNot {
                                                            it.id ==
                                                                    message.id
                                                        }

                                                currentMailMessage =
                                                    null

                                                returnToHubAfterMail =
                                                    false

                                                showHub =
                                                    false

                                                loadMailWithToken(
                                                    accessToken =
                                                        token,
                                                    openInbox =
                                                        true
                                                )

                                            } else {

                                                statusText =
                                                    "MAIL SNOOZE FAILED"
                                            }
                                        }
                                    }
                                },
                                onTrash = {
                                        message ->

                                    val token =
                                        mailAccessToken

                                    if (
                                        token.isNullOrBlank()
                                    ) {

                                        statusText =
                                            "MAIL AUTH REQUIRED"

                                    } else {

                                        scope.launch {

                                            val success =
                                                gmailRepository
                                                    .trashMessage(
                                                        accessToken =
                                                            token,
                                                        messageId =
                                                            message.id
                                                    )

                                            if (
                                                success
                                            ) {

                                                mailMessages =
                                                    mailMessages
                                                        .filterNot {
                                                            it.id ==
                                                                    message.id
                                                        }

                                                currentMailMessage =
                                                    null

                                                returnToHubAfterMail =
                                                    false

                                                showHub =
                                                    false

                                                loadMailWithToken(
                                                    accessToken =
                                                        token,
                                                    openInbox =
                                                        true
                                                )

                                            } else {

                                                statusText =
                                                    "MAIL TRASH FAILED"
                                            }
                                        }
                                    }
                                },
                                onBack =
                                    closeMailDetail,
                                floatingMode =
                                    true
                            )
                        }
                    )
                }

                showMailInbox -> {

                    SwipeRightToHomeContainer(
                        edgeOnly =
                            true,
                        edgeTopPaddingDp =
                            36,
                        onReturnHome = {

                            showMailInbox =
                                false

                            showHub =
                                false
                        },
                        homeContent = {

                            RenderHomeScreen(
                                autoFocusCommandInput = false
                            )
                        },
                        screenContent = {

                            MailInboxScreen(
                                messages =
                                    mailMessages,
                                loading =
                                    mailLoading,
                                onOpenMessage = {
                                        message ->

                                    returnToHubAfterMail =
                                        false

                                    val openedMessage =
                                        if (
                                            message.unread
                                        ) {

                                            message.copy(
                                                unread =
                                                    false
                                            )

                                        } else {

                                            message
                                        }

                                    currentMailMessage =
                                        openedMessage

                                    if (
                                        message.unread
                                    ) {

                                        mailMessages =
                                            mailMessages.map {
                                                    existing ->

                                                if (
                                                    existing.id ==
                                                    message.id
                                                ) {

                                                    existing.copy(
                                                        unread =
                                                            false
                                                    )

                                                } else {

                                                    existing
                                                }
                                            }

                                        val token =
                                            mailAccessToken

                                        if (
                                            !token.isNullOrBlank()
                                        ) {

                                            scope.launch {

                                                val success =
                                                    gmailRepository
                                                        .markAsRead(
                                                            accessToken =
                                                                token,
                                                            messageId =
                                                                message.id
                                                        )

                                                if (
                                                    !success
                                                ) {

                                                    mailMessages =
                                                        mailMessages.map {
                                                                existing ->

                                                            if (
                                                                existing.id ==
                                                                message.id
                                                            ) {

                                                                existing.copy(
                                                                    unread =
                                                                        true
                                                                )

                                                            } else {

                                                                existing
                                                            }
                                                        }

                                                    currentMailMessage =
                                                        currentMailMessage
                                                            ?.takeIf {
                                                                it.id ==
                                                                        message.id
                                                            }
                                                            ?.copy(
                                                                unread =
                                                                    true
                                                            )

                                                    statusText =
                                                        "MAIL READ UPDATE FAILED"
                                                }
                                            }
                                        }
                                    }

                                    showMailInbox =
                                        false
                                },
                                onTrashMessage = {
                                        message ->

                                    val token =
                                        mailAccessToken

                                    if (
                                        token.isNullOrBlank()
                                    ) {

                                        statusText =
                                            "MAIL AUTH REQUIRED"

                                    } else {

                                        val previousMessages =
                                            mailMessages

                                        mailMessages =
                                            mailMessages.filterNot {
                                                    existing ->

                                                existing.id ==
                                                        message.id
                                            }

                                        scope.launch {

                                            val success =
                                                gmailRepository
                                                    .trashMessage(
                                                        accessToken =
                                                            token,
                                                        messageId =
                                                            message.id
                                                    )

                                            if (
                                                success
                                            ) {

                                                statusText =
                                                    "✓ MOVED TO TRASH"

                                            } else {

                                                mailMessages =
                                                    previousMessages

                                                statusText =
                                                    "MAIL TRASH FAILED"
                                            }
                                        }
                                    }
                                },
                                onMarkUnread = {
                                        message ->

                                    val token =
                                        mailAccessToken

                                    if (
                                        token.isNullOrBlank()
                                    ) {

                                        statusText =
                                            "MAIL AUTH REQUIRED"

                                    } else if (
                                        message.unread
                                    ) {

                                        statusText =
                                            "✓ ALREADY UNREAD"

                                    } else {

                                        val previousMessages =
                                            mailMessages

                                        mailMessages =
                                            mailMessages.map {
                                                    existing ->

                                                if (
                                                    existing.id ==
                                                    message.id
                                                ) {

                                                    existing.copy(
                                                        unread =
                                                            true
                                                    )

                                                } else {

                                                    existing
                                                }
                                            }

                                        scope.launch {

                                            val success =
                                                gmailRepository
                                                    .markAsUnread(
                                                        accessToken =
                                                            token,
                                                        messageId =
                                                            message.id
                                                    )

                                            if (
                                                success
                                            ) {

                                                statusText =
                                                    "✓ MARKED UNREAD"

                                            } else {

                                                mailMessages =
                                                    previousMessages

                                                statusText =
                                                    "MAIL UNREAD UPDATE FAILED"
                                            }
                                        }
                                    }
                                },
                                onRefresh = {

                                    requestMailAuthorization(
                                        openInbox =
                                            true,
                                        allowConsent =
                                            true
                                    )
                                },
                                onBack = {

                                    showMailInbox =
                                        false

                                    showHub =
                                        false

                                    statusText =
                                        ""
                                }
                            )
                        }
                    )
                }

                showMessagesInbox -> {

                    SwipeRightToHomeContainer(
                        edgeOnly =
                            false,
                        onReturnHome = {

                            showMessagesInbox =
                                false

                            showHub =
                                false
                        },
                        homeContent = {

                            RenderHomeScreen(
                                autoFocusCommandInput = false
                            )
                        },
                        screenContent = {

                            MessagesInboxScreen(
                                smsDatabase =
                                    smsDatabase,
                                contactRepository =
                                    contactRepository,
                                hasContactsPermission =
                                    hasContactsPermission,
                                onOpenConversation = {
                                        contact ->

                                    returnToHubAfterConversation =
                                        false

                                    currentConversationThread =
                                        null

                                    openConversation(
                                        contact
                                    )
                                },
                                onOpenThread = {
                                        thread,
                                        contact ->

                                    returnToHubAfterConversation =
                                        false

                                    currentConversationThread =
                                        thread

                                    openConversation(
                                        contact
                                    )
                                },
                                onBack = {

                                    showMessagesInbox =
                                        false

                                    showHub =
                                        false

                                    statusText =
                                        ""
                                }
                            )
                        }
                    )
                }
                showNotes -> {

                    SwipeRightToHomeContainer(
                        edgeOnly =
                            false,
                        edgeTopPaddingDp =
                            72,
                        onReturnHome = {

                            showNotes =
                                false

                            showHub =
                                false
                        },
                        homeContent = {

                            RenderHomeScreen(
                                autoFocusCommandInput = false
                            )
                        },
                        screenContent = {

                            NotesScreen(
                                noteStore =
                                    noteStore,
                                onOpenNote = { note ->
                                    showNotes = true
                                    currentNote = note
                                },
                                onCreateNote = { category ->
                                    showNotes = true
                                    currentNote =
                                        noteStore.createBlankNote(category)
                                },
                                onBack = {
                                    showNotes = false
                                    showHub = false
                                    statusText = ""
                                }
                            )
                        }
                    )
                }

                else -> {

                    HomeHubSettingsSwipeContainer(
                        showHub =
                            showHub,
                        onShowHub = {

                            if (
                                !showHub
                            ) {

                                showHub =
                                    true

                                requestMailAuthorization(
                                    openInbox =
                                        false,
                                    allowConsent =
                                        false
                                )

                                if (
                                    !hasCallLogPermission
                                ) {

                                    callLogPermissionLauncher.launch(
                                        Manifest.permission.READ_CALL_LOG
                                    )
                                }
                            }
                        },
                        onShowHome = {

                            showHub =
                                false

                            showSettings =
                                false

                            launcherSettingsPage =
                                LauncherSettingsPage.MAIN
                        },
                        showSettings =
                            showSettings,
                        settingsDetailOpen =
                            launcherSettingsPage != LauncherSettingsPage.MAIN,
                        onShowSettings = {

                            showHub =
                                false

                            showSettings =
                                true
                        },
                        hubContent = {
                            HubScreen(
                                smsDatabase =
                                    smsDatabase,
                                contactRepository =
                                    contactRepository,
                                hasContactsPermission =
                                    hasContactsPermission,
                                mailMessages =
                                    mailMessages,
                                callLogEntries =
                                    callLogEntries,
                                mutedNotificationApps =
                                    launcherSettings.mutedNotificationApps,
                                hasCallLogPermission =
                                    hasCallLogPermission,
                                selectedCategory =
                                    hubSelectedCategory,
                                onSelectedCategoryChange = {
                                        category ->

                                    hubSelectedCategory =
                                        category
                                },
                                selectedIndex =
                                    hubSelectedIndex,
                                onSelectedIndexChange = {
                                        index ->

                                    hubSelectedIndex =
                                        index
                                },
                                onRequestCallLogPermission = {

                                    callLogPermissionLauncher.launch(
                                        Manifest.permission.READ_CALL_LOG
                                    )
                                },
                                onOpenConversation = {
                                        contact ->

                                    returnToHubAfterConversation =
                                        true

                                    openConversation(
                                        contact
                                    )
                                },
                                onOpenMail = {
                                        message ->

                                    /*
                                     * Hand off to the email app, like
                                     * texts; the in-app mail viewer is
                                     * no longer used.
                                     */
                                    if (
                                        openMailApp(
                                            context,
                                            EnabledMailApps
                                        )
                                    ) {
                                        return@HubScreen
                                    }

                                    returnToHubAfterMail =
                                        true

                                    currentMailMessage =
                                        if (
                                            message.unread
                                        ) {
                                            message.copy(
                                                unread = false
                                            )
                                        } else {
                                            message
                                        }

                                    if (
                                        message.unread
                                    ) {

                                        mailMessages =
                                            mailMessages.map { existing ->

                                                if (
                                                    existing.id ==
                                                    message.id
                                                ) {

                                                    existing.copy(
                                                        unread = false
                                                    )

                                                } else {

                                                    existing
                                                }
                                            }

                                        val token =
                                            mailAccessToken

                                        if (
                                            !token.isNullOrBlank()
                                        ) {

                                            scope.launch {

                                                gmailRepository
                                                    .markAsRead(
                                                        accessToken = token,
                                                        messageId = message.id
                                                    )
                                            }
                                        }
                                    }

                                    showHub =
                                        false
                                },
                                onOpenThread = {
                                        thread,
                                        contact ->

                                    returnToHubAfterConversation =
                                        true

                                    currentConversationThread =
                                        thread

                                    openConversation(
                                        contact
                                    )
                                },
                                mailNeedsAuthorization =
                                    mailNeedsAuthorization,
                                onConnectMail = {

                                    requestMailAuthorization(
                                        openInbox =
                                            false,
                                        allowConsent =
                                            true
                                    )
                                },
                                onDial = {
                                        phoneNumber ->

                                    try {

                                        context.startActivity(
                                            Intent(
                                                Intent.ACTION_DIAL,
                                                Uri.parse(
                                                    "tel:$phoneNumber"
                                                )
                                            )
                                        )

                                    } catch (
                                        _: Exception
                                    ) {

                                        statusText =
                                            "PHONE NOT AVAILABLE"
                                    }
                                },
                                onBack = {

                                    showHub =
                                        false
                                }
                            )

                        },
                        homeContent = {

                            RenderHomeScreen(
                                autoFocusCommandInput =
                                    !showHub &&
                                            !showSettings
                            )

                        },
                        settingsContent = {
                            if (launcherSettingsPage != LauncherSettingsPage.MAIN) {
                                SwipeRightToHomeContainer(
                                    edgeOnly = false,
                                    onReturnHome = {
                                        launcherSettingsPage = LauncherSettingsPage.MAIN
                                    },
                                    homeContent = {
                                        SettingsMainScreen(
                                            onAppearance = {
                                                launcherSettingsPage = LauncherSettingsPage.APPEARANCE
                                            },
                                            onHomeScreen = {
                                                launcherSettingsPage = LauncherSettingsPage.HOME_SCREEN
                                            },
                                            onCalendars = {
                                                /*
                                                 * Phone calendars have nothing to sign in to: open the
                                                 * calendar card (with its Phone / Google switch) instead.
                                                 */
                                                if (
                                                    !calendarSelectionStore.usesGoogle()
                                                ) {
                                                    showSettings =
                                                        false

                                                    openPhoneCalendarSetup()
                                                } else {


                                                launcherSettingsPage =
                                                    LauncherSettingsPage.CALENDARS

                                                calendarVisibilityLoading =
                                                    true

                                                requestGoogleAuthorization(
                                                    action =
                                                        GoogleCalendarAction.LOAD_VISIBILITY
                                                )
                                                }
                                            },
                                            onProductivityBar = {
                                                launcherSettingsPage = LauncherSettingsPage.PRODUCTIVITY_BAR
                                            },
                                            onNotifications = {
                                                launcherSettingsPage = LauncherSettingsPage.NOTIFICATIONS
                                            },
                                            onBackupRestore = {
                                                backupRestoreStatus = ""
                                                launcherSettingsPage = LauncherSettingsPage.BACKUP_RESTORE
                                            },
                                            onReadability = {
                                                launcherSettingsPage = LauncherSettingsPage.READABILITY
                                            },
                                            onBack = {
                                                showSettings = false
                                            }
                                        )
                                    },
                                    screenContent = {
                                        when (launcherSettingsPage) {
                                            LauncherSettingsPage.APPEARANCE ->
                                                AppearanceSettingsScreen(
                                                    settings = launcherSettings,
                                                    onSettingsChange = { updated ->
                                                        launcherSettings = updated
                                                        launcherSettingsStore.save(updated)
                                                    },
                                                    onBack = {
                                                        launcherSettingsPage = LauncherSettingsPage.MAIN
                                                    }
                                                )

                                            LauncherSettingsPage.HOME_SCREEN ->
                                                HomeScreenSettingsScreen(
                                                    settings = launcherSettings,
                                                    onSettingsChange = { updated ->
                                                        launcherSettings = updated
                                                        launcherSettingsStore.save(updated)
                                                    },
                                                    onBack = {
                                                        launcherSettingsPage = LauncherSettingsPage.MAIN
                                                    }
                                                )

                                            LauncherSettingsPage.CALENDARS ->
                                                CalendarVisibilitySettingsScreen(
                                                    calendars =
                                                        calendarVisibilityOptions,
                                                    hiddenCalendarIds =
                                                        hiddenCalendarIds,
                                                    currentEventCalendarId =
                                                        calendarSelectionStore
                                                            .getPersonalCalendarId(),
                                                    loading =
                                                        calendarVisibilityLoading,
                                                    onToggleCalendar = {
                                                            calendarId,
                                                            visible ->

                                                        calendarVisibilityStore
                                                            .setVisible(
                                                                calendarId =
                                                                    calendarId,
                                                                visible =
                                                                    visible
                                                            )

                                                        hiddenCalendarIds =
                                                            calendarVisibilityStore
                                                                .getHiddenCalendarIds()

                                                        refreshPersonalUpcomingEvent()
                                                    },
                                                    onRefresh = {

                                                        calendarVisibilityLoading =
                                                            true

                                                        requestGoogleAuthorization(
                                                            action =
                                                                GoogleCalendarAction.LOAD_VISIBILITY
                                                        )
                                                    },
                                                    onBack = {
                                                        launcherSettingsPage =
                                                            LauncherSettingsPage.MAIN
                                                    }
                                                )

                                            LauncherSettingsPage.PRODUCTIVITY_BAR ->
                                                ProductivityBarSettingsScreen(
                                                    settings = launcherSettings,
                                                    onSettingsChange = { updated ->
                                                        launcherSettings = updated
                                                        launcherSettingsStore.save(updated)
                                                    },
                                                    onBack = {
                                                        launcherSettingsPage = LauncherSettingsPage.MAIN
                                                    }
                                                )

                                            LauncherSettingsPage.NOTIFICATIONS ->
                                                NotificationSettingsScreen(
                                                    settings = launcherSettings,
                                                    onSettingsChange = { updated ->
                                                        launcherSettings = updated
                                                        launcherSettingsStore.save(updated)
                                                    },
                                                    onBack = {
                                                        launcherSettingsPage = LauncherSettingsPage.MAIN
                                                    }
                                                )

                                            LauncherSettingsPage.READABILITY ->
                                                ReadabilitySettingsScreen(
                                                    settings = launcherSettings,
                                                    onSettingsChange = { updated ->
                                                        launcherSettings = updated
                                                        launcherSettingsStore.save(updated)
                                                    },
                                                    onBack = {
                                                        launcherSettingsPage = LauncherSettingsPage.MAIN
                                                    }
                                                )

                                            LauncherSettingsPage.BACKUP_RESTORE ->
                                                BackupRestoreSettingsScreen(
                                                    statusText = backupRestoreStatus,
                                                    onBackup = {
                                                        backupRestoreStatus = ""
                                                        backupSettingsLauncher.launch(
                                                            "prompt-launcher-settings.json"
                                                        )
                                                    },
                                                    onRestore = {
                                                        backupRestoreStatus = ""
                                                        restoreSettingsLauncher.launch(
                                                            arrayOf(
                                                                "application/json",
                                                                "text/plain"
                                                            )
                                                        )
                                                    },
                                                    onBack = {
                                                        launcherSettingsPage = LauncherSettingsPage.MAIN
                                                    }
                                                )

                                            LauncherSettingsPage.MAIN -> Unit
                                        }
                                    }
                                )
                            } else {
                                SettingsMainScreen(
                                    onAppearance = {
                                        launcherSettingsPage = LauncherSettingsPage.APPEARANCE
                                    },
                                    onHomeScreen = {
                                        launcherSettingsPage = LauncherSettingsPage.HOME_SCREEN
                                    },
                                    onCalendars = {
                                        /*
                                         * Phone calendars have nothing to sign in to: open the
                                         * calendar card (with its Phone / Google switch) instead.
                                         */
                                        if (
                                            !calendarSelectionStore.usesGoogle()
                                        ) {
                                            showSettings =
                                                false

                                            openPhoneCalendarSetup()
                                        } else {


                                        launcherSettingsPage =
                                            LauncherSettingsPage.CALENDARS

                                        calendarVisibilityLoading =
                                            true

                                        requestGoogleAuthorization(
                                            action =
                                                GoogleCalendarAction.LOAD_VISIBILITY
                                        )
                                        }
                                    },
                                    onProductivityBar = {
                                        launcherSettingsPage = LauncherSettingsPage.PRODUCTIVITY_BAR
                                    },
                                    onNotifications = {
                                        launcherSettingsPage = LauncherSettingsPage.NOTIFICATIONS
                                    },
                                    onBackupRestore = {
                                        backupRestoreStatus = ""
                                        launcherSettingsPage = LauncherSettingsPage.BACKUP_RESTORE
                                    },
                                    onReadability = {
                                        launcherSettingsPage = LauncherSettingsPage.READABILITY
                                    },
                                    onBack = {
                                        showSettings = false
                                    }
                                )
                            }
                        }
                    )
                }
            }

        }

        if (
            useGlobalCommandLayer
        ) {

            RenderHomeScreen(
                commandOverlayOnly =
                    true,
                autoFocusCommandInput =
                    !preserveEditorTypingFocus,
                onCommandOverlayActiveChange = {
                        active ->

                    globalCommandOverlayActive =
                        active
                }
            )
        }
    }
}

@Composable
internal fun FloatingDetailPage(
    onDismiss: () -> Unit,
    backgroundContent: @Composable () -> Unit,
    content: @Composable () -> Unit
) {

    BackHandler {
        onDismiss()
    }

    Box(
        modifier =
            Modifier.fillMaxSize()
    ) {

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .blur(
                        7.dp
                    )
        ) {
            backgroundContent()
        }

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Color.Black.copy(
                            alpha =
                                0.26f
                        )
                    )
                    .pointerInput(Unit) {
                        detectTapGestures {
                            onDismiss()
                        }
                    }
        )

        Box(
            modifier =
                Modifier
                    .align(
                        Alignment.Center
                    )
                    .fillMaxWidth(
                        0.94f
                    )
                    .fillMaxHeight(
                        0.91f
                    )
                    .clip(
                        RoundedCornerShape(
                            16.dp
                        )
                    )
                    .background(
                        BackgroundBlack
                    )
                    .border(
                        0.9.dp,
                        BorderGray,
                        RoundedCornerShape(
                            16.dp
                        )
                    )
                    .pointerInput(Unit) {
                        detectTapGestures {
                            // Consume blank-area taps inside the floating page.
                        }
                    }
        ) {
            content()
        }
    }
}