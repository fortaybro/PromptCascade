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
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
import com.forrest.titanlauncher.calendar.CalendarAssistCalendar
import com.forrest.titanlauncher.calendar.CalendarCommandParser
import com.forrest.titanlauncher.calendar.CalendarEventDraft
import com.forrest.titanlauncher.calendar.CalendarRepository
import com.forrest.titanlauncher.calendar.CalendarSelectionStore
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
import com.forrest.titanlauncher.messages.SmsMessage
import com.forrest.titanlauncher.messages.SmsRoleManager
import com.forrest.titanlauncher.mail.findMailApps
import com.forrest.titanlauncher.mail.findEmailProviders
import com.forrest.titanlauncher.messages.SmsSender
import com.forrest.titanlauncher.mail.GmailRepository
import com.forrest.titanlauncher.mail.GoogleMailAuthManager
import com.forrest.titanlauncher.mail.MailMessage
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
import com.forrest.titanlauncher.settings.LauncherSettings
import com.forrest.titanlauncher.settings.LauncherSettingsStore
import com.forrest.titanlauncher.settings.ReadabilityContrast
import com.forrest.titanlauncher.settings.ReadabilityMotion
import com.forrest.titanlauncher.settings.ReadabilityTextSize
import com.forrest.titanlauncher.settings.ReadabilityTextWeight
import com.forrest.titanlauncher.settings.LauncherSecondaryTextColor
import com.forrest.titanlauncher.settings.LauncherTextColor
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

@Composable
internal fun SwipeRightToHomeContainer(
    edgeOnly: Boolean,
    edgeTopPaddingDp: Int = 0,
    onReturnHome: () -> Unit,
    homeContent: @Composable () -> Unit,
    screenContent: @Composable () -> Unit
) {

    var dragProgress by remember {
        mutableFloatStateOf(
            0f
        )
    }

    BoxWithConstraints(
        modifier =
            Modifier.fillMaxSize()
    ) {

        val widthPx =
            constraints
                .maxWidth
                .toFloat()
                .coerceAtLeast(
                    1f
                )

        val dragState =
            rememberDraggableState {
                    delta ->

                dragProgress =
                    (
                            dragProgress +
                                    delta /
                                    widthPx
                            )
                        .coerceIn(
                            0f,
                            1f
                        )
            }

        val swipeModifier =
            Modifier.draggable(
                state =
                    dragState,
                orientation =
                    Orientation.Horizontal,
                onDragStopped = {
                        velocity ->

                    val target =
                        if (
                            velocity >
                            900f ||
                            dragProgress >=
                            0.38f
                        ) {
                            1f
                        } else {
                            0f
                        }

                    animate(
                        initialValue =
                            dragProgress,
                        targetValue =
                            target,
                        animationSpec =
                            tween(
                                durationMillis =
                                    170
                            )
                    ) {
                            value,
                            _ ->

                        dragProgress =
                            value
                    }

                    if (
                        target ==
                        1f
                    ) {

                        onReturnHome()
                    }
                }
            )

        Box(
            modifier =
                Modifier.fillMaxSize()
        ) {

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .offset {

                            IntOffset(
                                x =
                                    (
                                            -widthPx +
                                                    dragProgress *
                                                    widthPx
                                            )
                                        .roundToInt(),
                                y =
                                    0
                            )
                        }
            ) {

                homeContent()
            }

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .offset {

                            IntOffset(
                                x =
                                    (
                                            dragProgress *
                                                    widthPx
                                            )
                                        .roundToInt(),
                                y =
                                    0
                            )
                        }
                        .then(
                            if (
                                edgeOnly
                            ) {
                                Modifier
                            } else {
                                swipeModifier
                            }
                        )
            ) {

                screenContent()
            }

            if (
                edgeOnly
            ) {

                Box(
                    modifier =
                        Modifier
                            .align(
                                Alignment.CenterStart
                            )
                            .fillMaxHeight()
                            .width(
                                30.dp
                            )
                            .padding(
                                top =
                                    edgeTopPaddingDp.dp
                            )
                            .then(
                                swipeModifier
                            )
                )
            }
        }
    }
}

@Composable
internal fun HomeHubSettingsSwipeContainer(
    showHub: Boolean,
    showSettings: Boolean,
    settingsDetailOpen: Boolean,
    onShowHub: () -> Unit,
    onShowHome: () -> Unit,
    onShowSettings: () -> Unit,
    homeContent: @Composable () -> Unit,
    hubContent: @Composable () -> Unit,
    settingsContent: @Composable () -> Unit
) {
    val selectedPage =
        when {
            showHub -> 1f
            showSettings -> -1f
            else -> 0f
        }

    var dragProgress by remember {
        mutableFloatStateOf(
            selectedPage
        )
    }

    var isDragging by remember {
        mutableStateOf(
            false
        )
    }

    BoxWithConstraints(
        modifier =
            Modifier.fillMaxSize()
    ) {
        val widthPx =
            constraints
                .maxWidth
                .toFloat()
                .coerceAtLeast(
                    1f
                )

        val dragState =
            rememberDraggableState {
                    delta ->

                dragProgress =
                    (
                            dragProgress +
                                    delta /
                                    widthPx
                            )
                        .coerceIn(
                            -1f,
                            1f
                        )
            }

        LaunchedEffect(
            showHub,
            showSettings,
            widthPx
        ) {
            if (
                !isDragging
            ) {
                val target =
                    when {
                        showHub -> 1f
                        showSettings -> -1f
                        else -> 0f
                    }

                if (
                    abs(
                        dragProgress -
                                target
                    ) >
                    0.001f
                ) {
                    animate(
                        initialValue =
                            dragProgress,
                        targetValue =
                            target,
                        animationSpec =
                            tween(
                                durationMillis =
                                    190
                            )
                    ) {
                            value,
                            _ ->

                        dragProgress =
                            value
                    }
                }
            }
        }

        val gestureModifier =
            if (
                settingsDetailOpen
            ) {
                Modifier
            } else {
                Modifier.draggable(
                    state =
                        dragState,
                    orientation =
                        Orientation.Horizontal,
                    onDragStarted = {
                        isDragging =
                            true
                    },
                    onDragStopped = {
                            velocity ->

                        val flingThreshold =
                            900f

                        val target =
                            when {
                                velocity > flingThreshold ->
                                    (dragProgress + 1f)
                                        .coerceAtMost(1f)
                                        .let {
                                            when {
                                                it > 0.5f -> 1f
                                                it < -0.5f -> -1f
                                                else -> 0f
                                            }
                                        }

                                velocity < -flingThreshold ->
                                    (dragProgress - 1f)
                                        .coerceAtLeast(-1f)
                                        .let {
                                            when {
                                                it > 0.5f -> 1f
                                                it < -0.5f -> -1f
                                                else -> 0f
                                            }
                                        }

                                dragProgress >= 0.5f -> 1f
                                dragProgress <= -0.5f -> -1f
                                else -> 0f
                            }

                        when (target) {
                            1f -> onShowHub()
                            -1f -> onShowSettings()
                            else -> onShowHome()
                        }

                        animate(
                            initialValue =
                                dragProgress,
                            targetValue =
                                target,
                            animationSpec =
                                tween(
                                    durationMillis =
                                        170
                                )
                        ) {
                                value,
                                _ ->

                            dragProgress =
                                value
                        }

                        isDragging =
                            false
                    }
                )
            }

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .then(
                        gestureModifier
                    )
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .offset {
                            IntOffset(
                                x =
                                    (
                                            -widthPx +
                                                    dragProgress *
                                                    widthPx
                                            )
                                        .roundToInt(),
                                y = 0
                            )
                        }
            ) {
                hubContent()
            }

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .offset {
                            IntOffset(
                                x =
                                    (
                                            dragProgress *
                                                    widthPx
                                            )
                                        .roundToInt(),
                                y = 0
                            )
                        }
            ) {
                homeContent()
            }

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .offset {
                            IntOffset(
                                x =
                                    (
                                            widthPx +
                                                    dragProgress *
                                                    widthPx
                                            )
                                        .roundToInt(),
                                y = 0
                            )
                        }
            ) {
                settingsContent()
            }
        }
    }
}

@Composable
internal fun SettingsMainScreen(
    onAppearance: () -> Unit,
    onHomeScreen: () -> Unit,
    onCalendars: () -> Unit,
    onProductivityBar: () -> Unit,
    onNotifications: () -> Unit,
    onBackupRestore: () -> Unit,
    onReadability: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    BackgroundBlack
                )
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    horizontal = DaylightPageHorizontal,
                    vertical = DaylightPageVertical
                ),
        verticalArrangement =
            Arrangement.spacedBy(SettingsBlockGap)
    ) {
        /*
         * Daylight list: a big title right of the camera, then white
         * cards of rows with chevrons.
         */
        DaylightScreenTitle(
            title = "Settings"
        )

        /*
         * Grouped by what people look for: the prompt and home screen
         * first, then how it looks, then the apps and accounts it
         * connects to. Things set up by typing a command are listed
         * with that command, so nothing is hidden.
         */
        SettingsSectionLabel(
            text = "PROMPT & HOME"
        )

        SettingsCardGroup {
            SettingsNavigationRow(
                symbol = "⌂",
                title = "Prompt & home screen",
                subtitle = "Keyboard, commands box, pickers, weather & cards",
                onClick = onHomeScreen
            )

            DaylightRowDivider()

            SettingsNavigationRow(
                symbol = "◔",
                title = "24-hour ring",
                subtitle = "Screen-time ring & distracting apps",
                onClick = onProductivityBar
            )
        }

        SettingsSectionLabel(
            text = "LOOK"
        )

        SettingsCardGroup {
            SettingsNavigationRow(
                symbol = "◉",
                title = "Appearance",
                subtitle = "Theme, accent, font, buttons & surfaces",
                onClick = onAppearance
            )

            DaylightRowDivider()

            SettingsNavigationRow(
                symbol = "Aa",
                title = "Readability",
                subtitle = "Text size, contrast & motion",
                onClick = onReadability
            )
        }

        SettingsSectionLabel(
            text = "CALENDAR, MAIL & HUB"
        )

        SettingsCardGroup {
            SettingsNavigationRow(
                symbol = "•",
                title = "Calendar",
                subtitle = "Where new events go & what home shows",
                onClick = onCalendars
            )

            DaylightRowDivider()

            SettingsNavigationRow(
                symbol = "@",
                title = "Mail & hub",
                subtitle = "Email apps, compose app & hub apps",
                onClick = onNotifications
            )
        }

        SettingsSectionLabel(
            text = "SET UP BY TYPING IN THE PROMPT"
        )

        SettingsCardGroup {
            SettingsCommandRow(
                title = "Calendar",
                detail = "Phone or Google calendar",
                command = "calendarsetup"
            )

            DaylightRowDivider()

            SettingsCommandRow(
                title = "Weather",
                detail = "Location for the forecast",
                command = "weathersetup"
            )

            DaylightRowDivider()

            SettingsCommandRow(
                title = "Assist",
                detail = "Gemini key & what it may read",
                command = "geminisetup"
            )

            DaylightRowDivider()

            SettingsCommandRow(
                title = "To-dos",
                detail = "Google Tasks or Todoist",
                command = "taskssetup"
            )

            DaylightRowDivider()

            SettingsCommandRow(
                title = "Screen time",
                detail = "Usage access for the ring",
                command = "usagesetup"
            )
        }

        SettingsSectionLabel(
            text = "DATA"
        )

        SettingsCardGroup {
            SettingsNavigationRow(
                symbol = "•",
                title = "Backup & restore",
                subtitle = "Save or restore launcher settings",
                onClick = onBackupRestore
            )
        }

        Spacer(
            modifier = Modifier.height(4.dp)
        )
    }
}

@Composable
internal fun BackupRestoreSettingsScreen(
    statusText: String,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    BackgroundBlack
                )
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    horizontal = DaylightPageHorizontal,
                    vertical = DaylightPageVertical
                ),
        verticalArrangement =
            Arrangement.spacedBy(SettingsBlockGap)
    ) {
        SettingsHeader(
            title = "Backup & restore",
            onBack = onBack
        )

        SettingsSectionLabel(
            text = "SETTINGS"
        )

        SettingsCardGroup {
            SettingsNavigationRow(
                symbol = "•",
                title = "Back up settings",
                subtitle = "Save a portable settings file",
                onClick = onBackup
            )

            DaylightRowDivider()

            SettingsNavigationRow(
                symbol = "•",
                title = "Restore settings",
                subtitle = "Load settings from a backup file",
                onClick = onRestore
            )
        }

        if (
            statusText.isNotBlank()
        ) {
            Text(
                text = statusText,
                color =
                    if (
                        statusText.contains(
                            "failed"
                        )
                    ) {
                        SecondaryText
                    } else {
                        AccentOrange
                    },
                fontSize = 8.5.sp,
                lineHeight = 10.5.sp,
                fontFamily = InterfaceFont,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
internal fun AppearanceSettingsScreen(
    settings: LauncherSettings,
    onSettingsChange: (LauncherSettings) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    BackgroundBlack
                )
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    horizontal = DaylightPageHorizontal,
                    vertical = DaylightPageVertical
                ),
        verticalArrangement =
            Arrangement.spacedBy(SettingsBlockGap)
    ) {
        SettingsHeader(
            title = "Appearance",
            onBack = onBack
        )

        SettingsControlCard {
            SettingsAppearanceRow(
                settings = settings,
                onSettingsChange = onSettingsChange
            )
        }

        SettingsControlCard {
            SettingsAccentRow(
                settings = settings,
                onSettingsChange = onSettingsChange
            )
        }

        SettingsSectionLabel(
            text = "THEME & TEXT"
        )

        SettingsCardGroup {
            SettingsChoiceWrapRow(
                label = "Theme",
                /*
                 * "Auto" follows the phone's dark mode, switching
                 * between Daylight light and Daylight dark. The other
                 * themes are fixed.
                 */
                detail = "Auto follows your phone's dark mode",
                choices =
                    listOf(
                        "Auto",
                        "Light",
                        "Dark",
                        "Navy",
                        "Slate",
                        "Tan",
                        "Forest"
                    ),
                selectedIndex =
                    if (
                        settings.followSystemTheme
                    ) {
                        0
                    } else {
                        when (settings.themeMode) {
                            LauncherThemeMode.LIGHT -> 1
                            LauncherThemeMode.DARK -> 2
                            LauncherThemeMode.NAVY -> 3
                            LauncherThemeMode.SLATE -> 4
                            LauncherThemeMode.TAN -> 5
                            LauncherThemeMode.FOREST -> 6
                        }
                    },
                onSelected = { index ->
                    onSettingsChange(
                        if (
                            index == 0
                        ) {
                            settings.copy(
                                followSystemTheme = true
                            )
                        } else {
                            settings.copy(
                                followSystemTheme = false,
                                themeMode =
                                    when (index) {
                                        1 -> LauncherThemeMode.LIGHT
                                        3 -> LauncherThemeMode.NAVY
                                        4 -> LauncherThemeMode.SLATE
                                        5 -> LauncherThemeMode.TAN
                                        6 -> LauncherThemeMode.FOREST
                                        else -> LauncherThemeMode.DARK
                                    }
                            )
                        }
                    )
                }
            )

            DaylightRowDivider()

            /*
             * "Default" leaves each theme to supply its own text
             * colours. Anything else overrides all three tiers.
             */
            SettingsChoiceWrapRow(
                label = "Text color",
                choices =
                    listOf(
                        "Default",
                        "White",
                        "Black",
                        "Warm",
                        "Cool",
                        "Accent"
                    ),
                selectedIndex =
                    when (settings.textColor) {
                        LauncherTextColor.DEFAULT -> 0
                        LauncherTextColor.WHITE -> 1
                        LauncherTextColor.BLACK -> 2
                        LauncherTextColor.WARM -> 3
                        LauncherTextColor.COOL -> 4
                        LauncherTextColor.ACCENT -> 5
                    },
                onSelected = { index ->
                    onSettingsChange(
                        settings.copy(
                            textColor =
                                when (index) {
                                    1 -> LauncherTextColor.WHITE
                                    2 -> LauncherTextColor.BLACK
                                    3 -> LauncherTextColor.WARM
                                    4 -> LauncherTextColor.COOL
                                    5 -> LauncherTextColor.ACCENT
                                    else -> LauncherTextColor.DEFAULT
                                }
                        )
                    )
                }
            )

            DaylightRowDivider()

            /*
             * Controls the muted text: labels, previews, timestamps
             * and hints. "Default" leaves it derived from the theme
             * and the primary text colour.
             */
            SettingsChoiceWrapRow(
                label = "Secondary text",
                choices =
                    listOf(
                        "Default",
                        "Light",
                        "Medium",
                        "Dark",
                        "Warm",
                        "Cool",
                        "Accent"
                    ),
                selectedIndex =
                    when (settings.secondaryTextColor) {
                        LauncherSecondaryTextColor.DEFAULT -> 0
                        LauncherSecondaryTextColor.LIGHT -> 1
                        LauncherSecondaryTextColor.MEDIUM -> 2
                        LauncherSecondaryTextColor.DARK -> 3
                        LauncherSecondaryTextColor.WARM -> 4
                        LauncherSecondaryTextColor.COOL -> 5
                        LauncherSecondaryTextColor.ACCENT -> 6
                    },
                onSelected = { index ->
                    onSettingsChange(
                        settings.copy(
                            secondaryTextColor =
                                when (index) {
                                    1 -> LauncherSecondaryTextColor.LIGHT
                                    2 -> LauncherSecondaryTextColor.MEDIUM
                                    3 -> LauncherSecondaryTextColor.DARK
                                    4 -> LauncherSecondaryTextColor.WARM
                                    5 -> LauncherSecondaryTextColor.COOL
                                    6 -> LauncherSecondaryTextColor.ACCENT
                                    else -> LauncherSecondaryTextColor.DEFAULT
                                }
                        )
                    )
                }
            )

            DaylightRowDivider()

            SettingsChoiceWrapRow(
                label = "Interface font",
                choices =
                    listOf(
                        "Bricolage",
                        "Mono",
                        "Sans",
                        "Serif",
                        "Poppins"
                    ),
                selectedIndex =
                    when (settings.interfaceFont) {
                        LauncherInterfaceFont.BRICOLAGE -> 0
                        LauncherInterfaceFont.MONO -> 1
                        LauncherInterfaceFont.SANS -> 2
                        LauncherInterfaceFont.SERIF -> 3
                        LauncherInterfaceFont.POPPINS -> 4
                    },
                onSelected = { index ->
                    onSettingsChange(
                        settings.copy(
                            interfaceFont =
                                when (index) {
                                    1 -> LauncherInterfaceFont.MONO
                                    2 -> LauncherInterfaceFont.SANS
                                    3 -> LauncherInterfaceFont.SERIF
                                    4 -> LauncherInterfaceFont.POPPINS
                                    else -> LauncherInterfaceFont.BRICOLAGE
                                }
                        )
                    )
                }
            )
        }

        SettingsSectionLabel(
            text = "SURFACES"
        )

        SettingsCardGroup {
            SettingsChoiceRow(
                label = "App buttons",
                choices = listOf("Black", "Charcoal", "Gray"),
                selectedIndex = settings.appButtonSurface.ordinal,
                onSelected = { index ->
                    onSettingsChange(
                        settings.copy(
                            appButtonSurface = HomeSurfaceTone.entries[index]
                        )
                    )
                }
            )

            DaylightRowDivider()

            SettingsChoiceRow(
                label = "Info cards",
                choices = listOf("Black", "Charcoal", "Gray"),
                selectedIndex = settings.infoCardSurface.ordinal,
                onSelected = { index ->
                    onSettingsChange(
                        settings.copy(
                            infoCardSurface = HomeSurfaceTone.entries[index]
                        )
                    )
                }
            )

            DaylightRowDivider()

            SettingsChoiceRow(
                label = "Prompt bar",
                choices = listOf("Dark", "Medium", "Light"),
                selectedIndex = settings.commandSurface.ordinal,
                onSelected = { index ->
                    onSettingsChange(
                        settings.copy(
                            commandSurface = CommandSurfaceTone.entries[index]
                        )
                    )
                }
            )
        }

        SettingsSectionLabel(
            text = "BUTTONS"
        )

        SettingsCardGroup {
            SettingsChoiceRow(
                label = "Icon size",
                choices = listOf("Small", "Normal", "Large"),
                selectedIndex = settings.buttonIconSize.ordinal,
                onSelected = { index ->
                    onSettingsChange(
                        settings.copy(
                            buttonIconSize = HomeElementSize.entries[index]
                        )
                    )
                }
            )

            DaylightRowDivider()

            SettingsChoiceRow(
                label = "Label size",
                choices = listOf("Small", "Normal", "Large"),
                selectedIndex = settings.buttonLabelSize.ordinal,
                onSelected = { index ->
                    onSettingsChange(
                        settings.copy(
                            buttonLabelSize = HomeElementSize.entries[index]
                        )
                    )
                }
            )

            DaylightRowDivider()

            SettingsChoiceRow(
                label = "Corners",
                choices = listOf("Square", "Soft", "Round"),
                selectedIndex = settings.buttonCorners.ordinal,
                onSelected = { index ->
                    onSettingsChange(
                        settings.copy(
                            buttonCorners = HomeCornerStyle.entries[index]
                        )
                    )
                }
            )
        }

        /*
         * A weighted spacer would demand infinite height inside a
         * scrolling column. A fixed tail keeps the last card clear of
         * the bottom edge instead.
         */
        Spacer(
            modifier = Modifier.height(6.dp)
        )
    }
}

internal data class LauncherAppEntry(
    val label: String,
    val packageName: String
)

internal fun loadLaunchableApps(
    context: Context
): List<LauncherAppEntry> {
    val packageManager =
        context.packageManager

    @Suppress("DEPRECATION")
    val installedApplications =
        packageManager
            .getInstalledApplications(
                0
            )

    return installedApplications
        .mapNotNull { applicationInfo ->
            val packageName =
                applicationInfo.packageName

            val launchIntent =
                packageManager
                    .getLaunchIntentForPackage(
                        packageName
                    )

            if (
                packageName.isBlank() ||
                packageName ==
                context.packageName ||
                launchIntent == null ||
                !applicationInfo.enabled
            ) {
                null
            } else {
                LauncherAppEntry(
                    label =
                        packageManager
                            .getApplicationLabel(
                                applicationInfo
                            )
                            .toString()
                            .ifBlank {
                                packageName
                            },
                    packageName =
                        packageName
                )
            }
        }
        .distinctBy {
            it.packageName
        }
        .sortedBy {
            it.label.lowercase()
        }
}

internal data class InstalledProductivityApp(
    val label: String,
    val packageName: String
)

internal fun loadInstalledProductivityApps(
    context: Context
): List<InstalledProductivityApp> {
    val packageManager =
        context.packageManager

    @Suppress("DEPRECATION")
    val installedApplications =
        packageManager
            .getInstalledApplications(
                0
            )

    val excludedPackages =
        setOf(
            context.packageName,
            "com.android.settings",
            "com.android.stk"
        )

    return installedApplications
        .mapNotNull { applicationInfo ->
            val packageName =
                applicationInfo.packageName

            val launchIntent =
                packageManager
                    .getLaunchIntentForPackage(
                        packageName
                    )

            if (
                packageName.isBlank() ||
                packageName in excludedPackages ||
                launchIntent == null ||
                !applicationInfo.enabled
            ) {
                null
            } else {
                InstalledProductivityApp(
                    label =
                        packageManager
                            .getApplicationLabel(
                                applicationInfo
                            )
                            .toString()
                            .ifBlank {
                                packageName
                            },
                    packageName =
                        packageName
                )
            }
        }
        .distinctBy {
            it.packageName
        }
        .sortedBy {
            it.label.lowercase()
        }
}

@Composable
internal fun NotificationSettingsScreen(
    settings: LauncherSettings,
    onSettingsChange: (LauncherSettings) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    /*
     * Mail and the dialers are hidden because Prompt Launcher always
     * shows those itself, so a toggle would do nothing. The messaging
     * apps are only hidden while Prompt owns SMS: when another app
     * holds the role its notifications do appear, so they need a
     * toggle like anything else.
     */
    val apps = remember(context) {

        val promptOwnsSms =
            runCatching {
                android.provider.Telephony.Sms
                    .getDefaultSmsPackage(
                        context
                    ) ==
                        context.packageName
            }
                .getOrDefault(
                    true
                )

        val hidden =
            buildSet {

                add("com.google.android.dialer")
                add("com.android.dialer")

                if (
                    promptOwnsSms
                ) {
                    add("com.google.android.apps.messaging")
                    add("com.android.messaging")
                }
            }

        loadLaunchableApps(context)
            .filterNot { app ->
                app.packageName in hidden
            }
    }

    /*
     * Where "email <name>" writes the new message: Prompt Launcher's
     * own Gmail compose, or one of the installed mail apps.
     */
    val mailApps = remember(context) {
        findMailApps(context)
    }

    val emailProviders = remember(context) {
        findEmailProviders(context)
    }

    val composeChoices =
        listOf(
            "" to "Prompt Launcher (Gmail)"
        ) +
                mailApps.map {
                    it.packageName to it.label
                }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBlack)
            .padding(
                horizontal = DaylightPageHorizontal,
                vertical = DaylightPageVertical
            )
    ) {
        SettingsHeader(
            title = "Mail & hub",
            onBack = onBack
        )

        Spacer(Modifier.height(SettingsBlockGap))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            item {
                SettingsSectionHeader(
                    text = "EMAIL APPS",
                    first = true
                )
            }

            /*
             * Their notifications fill the hub's email tab, and the
             * first one picked opens for "mail". None picked means
             * every installed email app.
             */
            if (
                emailProviders.isEmpty()
            ) {
                item {
                    SettingsControlCard {
                        SettingsRow(
                            title = "No email apps found",
                            detail = "Install an email app to fill the hub's email tab"
                        )
                    }
                }
            } else {
                itemsIndexed(
                    items = emailProviders,
                    key = { _, provider -> "provider:" + provider.packageName }
                ) { index, provider ->
                    val picked =
                        provider.packageName in settings.mailApps

                    val toggle: (Boolean) -> Unit = { pick ->
                        onSettingsChange(
                            settings.copy(
                                mailApps =
                                    if (pick) {
                                        if (provider.packageName in settings.mailApps) {
                                            settings.mailApps
                                        } else {
                                            settings.mailApps + provider.packageName
                                        }
                                    } else {
                                        settings.mailApps - provider.packageName
                                    }
                            )
                        )
                    }

                    SettingsGroupItem(
                        isFirst = index == 0,
                        isLast = index == emailProviders.lastIndex
                    ) {
                        SettingsRow(
                            title = provider.label,
                            detail =
                                when {
                                    settings.mailApps.isEmpty() && index == 0 ->
                                        "None picked: every email app shows"
                                    picked &&
                                            settings.mailApps.firstOrNull() ==
                                            provider.packageName ->
                                        "Main app: opens for \"mail\""
                                    else -> null
                                },
                            onClick = {
                                toggle(!picked)
                            }
                        ) {
                            DaylightSwitch(
                                checked = picked,
                                onCheckedChange = toggle
                            )
                        }
                    }
                }
            }

            item {
                SettingsSectionHeader(
                    text = "COMPOSE EMAIL IN"
                )
            }

            itemsIndexed(
                items = composeChoices,
                key = { _, choice -> "compose:" + choice.first }
            ) { index, (packageName, label) ->
                val selected =
                    settings.mailComposeApp == packageName

                SettingsGroupItem(
                    isFirst = index == 0,
                    isLast = index == composeChoices.lastIndex
                ) {
                    SettingsRow(
                        title = label,
                        onClick = {
                            onSettingsChange(
                                settings.copy(
                                    mailComposeApp = packageName
                                )
                            )
                        }
                    ) {
                        SettingsRadioMark(
                            selected = selected
                        )
                    }
                }
            }

            item {
                SettingsSectionHeader(
                    text = "HUB APPS"
                )
            }

            itemsIndexed(
                items = apps,
                key = { _, app -> app.packageName }
            ) { index, app ->
                val enabled =
                    app.packageName !in settings.mutedNotificationApps

                val toggle: (Boolean) -> Unit = { show ->
                    val updatedMuted =
                        settings.mutedNotificationApps.toMutableSet()

                    if (show) {
                        updatedMuted.remove(app.packageName)
                    } else {
                        updatedMuted.add(app.packageName)
                    }

                    onSettingsChange(
                        settings.copy(
                            mutedNotificationApps = updatedMuted
                        )
                    )
                }

                SettingsGroupItem(
                    isFirst = index == 0,
                    isLast = index == apps.lastIndex
                ) {
                    SettingsRow(
                        title = app.label,
                        onClick = {
                            toggle(!enabled)
                        }
                    ) {
                        DaylightSwitch(
                            checked = enabled,
                            onCheckedChange = toggle
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}


@Composable
internal fun CalendarVisibilitySettingsScreen(
    calendars: List<CalendarAssistCalendar>,
    hiddenCalendarIds: Set<String>,
    currentEventCalendarId: String?,
    loading: Boolean,
    onToggleCalendar: (
        calendarId: String,
        visible: Boolean
    ) -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit
) {

    val currentEventCalendarName =
        calendars
            .firstOrNull {
                it.id ==
                        currentEventCalendarId
            }
            ?.summary
            ?: if (
                currentEventCalendarId.isNullOrBlank()
            ) {
                "Not set"
            } else {
                "Selected calendar"
            }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    BackgroundBlack
                )
                .padding(
                    horizontal =
                        DaylightPageHorizontal,
                    vertical =
                        DaylightPageVertical
                )
    ) {

        SettingsHeader(
            title =
                "Calendar",
            onBack =
                onBack
        )

        Spacer(
            modifier =
                Modifier.height(
                    SettingsBlockGap
                )
        )

        LazyColumn(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(
                        1f
                    )
        ) {

            item {

                SettingsSectionHeader(
                    text =
                        "EVENT CREATION",
                    first =
                        true
                )
            }

            item {

                SettingsControlCard {

                    SettingsRow(
                        title =
                            "Event calendar",
                        detail =
                            currentEventCalendarName
                    )

                    Text(
                        text =
                            "calendarsetup controls where \":\" adds events",
                        color =
                            TertiaryText,
                        fontSize =
                            8.sp,
                        lineHeight =
                            10.sp,
                        fontFamily =
                            InterfaceFont,
                        modifier =
                            Modifier.padding(
                                bottom =
                                    9.dp
                            )
                    )
                }
            }

            item {

                SettingsSectionHeader(
                    text =
                        "VISIBLE CALENDARS"
                )
            }

            item {

                SettingsFootnote(
                    text =
                        "Controls the home calendar card + AI assist",
                    modifier =
                        Modifier.padding(
                            bottom =
                                6.dp
                        )
                )
            }

            if (
                loading &&
                calendars.isEmpty()
            ) {

                item {

                    SettingsControlCard {

                        SettingsRow(
                            title =
                                "Loading calendars...",
                            titleColor =
                                SecondaryText
                        )
                    }
                }

            } else if (
                calendars.isEmpty()
            ) {

                item {

                    SettingsControlCard {

                        SettingsRow(
                            title =
                                "No calendars loaded",
                            titleColor =
                                SecondaryText,
                            onClick = {
                                onRefresh()
                            }
                        ) {

                            Text(
                                text =
                                    "Refresh",
                                color =
                                    AccentOrange,
                                fontSize =
                                    9.sp,
                                fontFamily =
                                    InterfaceFont,
                                fontWeight =
                                    FontWeight.SemiBold
                            )
                        }
                    }
                }

            } else {

                itemsIndexed(
                    items =
                        calendars,
                    key = {
                            _,
                            calendar ->

                        calendar.id
                    }
                ) {
                        index,
                        calendar ->

                    val visible =
                        calendar.id !in
                                hiddenCalendarIds

                    SettingsGroupItem(
                        isFirst =
                            index == 0,
                        isLast =
                            index == calendars.lastIndex
                    ) {

                        SettingsRow(
                            title =
                                calendar.summary,
                            onClick = {

                                onToggleCalendar(
                                    calendar.id,
                                    !visible
                                )
                            }
                        ) {

                            DaylightSwitch(
                                checked =
                                    visible,
                                onCheckedChange = {
                                        show ->

                                    onToggleCalendar(
                                        calendar.id,
                                        show
                                    )
                                }
                            )
                        }
                    }
                }

                item {

                    SettingsFootnote(
                        text =
                            if (
                                loading
                            ) {
                                "Refreshing..."
                            } else {
                                "Tap a calendar to show or hide it. Tap here to refresh."
                            },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onRefresh()
                                }
                                .padding(
                                    top =
                                        8.dp,
                                    bottom =
                                        12.dp
                                )
                    )
                }
            }
        }
    }
}


@Composable
internal fun HomeScreenSettingsScreen(
    settings: LauncherSettings,
    onSettingsChange: (LauncherSettings) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    BackgroundBlack
                )
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    horizontal = DaylightPageHorizontal,
                    vertical = DaylightPageVertical
                ),
        verticalArrangement =
            Arrangement.spacedBy(SettingsBlockGap)
    ) {
        SettingsHeader(
            title = "Prompt & home screen",
            onBack = onBack
        )

        SettingsSectionLabel(
            text = "PROMPT"
        )

        SettingsCardGroup {

            /*
             * Lets Android's keyboard app in (on-screen keyboard,
             * voice, other languages). Restarts the screen so every
             * text field picks it up. See PromptKeyboard.kt.
             */
            val keyboardContext =
                LocalContext.current

            var useKeyboardApp by remember {
                mutableStateOf(
                    KeyboardPrefs.useInputMethod(keyboardContext)
                )
            }

            SettingsToggleRow(
                label = "On-screen keyboard",
                detail = "Use your keyboard app: on-screen keys, voice, other languages · off types with the physical keyboard only",
                enabled = useKeyboardApp,
                onToggle = { enabled ->
                    KeyboardPrefs.setUseInputMethod(
                        keyboardContext,
                        enabled
                    )

                    useKeyboardApp =
                        enabled

                    keyboardContext
                        .findHostActivity()
                        ?.recreate()
                }
            )

            DaylightRowDivider()

            /*
             * The command legend above the prompt. When on, it can
             * also be collapsed to its header by tapping it.
             */
            SettingsToggleRow(
                label = "Commands box",
                detail = "Command list above the prompt · tap its header to collapse",
                enabled = settings.showCommandHints,
                onToggle = { enabled ->
                    onSettingsChange(
                        settings.copy(
                            showCommandHints = enabled
                        )
                    )
                }
            )

            DaylightRowDivider()

            /*
             * Chrome / Claude / Apps chips above the prompt when free
             * text is typed. The chips themselves are picked below the
             * row while the picker is on.
             */
            val searchChips =
                listOf(
                    "Chrome" to settings.searchChipChrome,
                    "Claude" to settings.searchChipClaude,
                    "Apps" to settings.searchChipApps
                )

            SettingsToggleRow(
                label = "Search picker",
                detail =
                    searchChips
                        .filter { it.second }
                        .joinToString(", ") { it.first }
                        .ifBlank { "No chips picked" },
                enabled = settings.showSearchTargetPicker,
                onToggle = { enabled ->
                    onSettingsChange(
                        settings.copy(
                            showSearchTargetPicker = enabled
                        )
                    )
                }
            )

            if (
                settings.showSearchTargetPicker
            ) {
                SettingsChipToggles(
                    options = searchChips,
                    onToggle = { index, enabled ->
                        onSettingsChange(
                            when (index) {
                                0 -> settings.copy(searchChipChrome = enabled)
                                1 -> settings.copy(searchChipClaude = enabled)
                                else -> settings.copy(searchChipApps = enabled)
                            }
                        )
                    }
                )
            }

            DaylightRowDivider()

            /*
             * Chips above the prompt while typing "+": add the task
             * to Google Tasks or Todoist.
             */
            val taskChips =
                listOf(
                    "Google Tasks" to settings.taskChipGoogle,
                    "Todoist" to settings.taskChipTodoist
                )

            SettingsToggleRow(
                label = "Task picker",
                detail =
                    taskChips
                        .filter { it.second }
                        .joinToString(", ") { it.first }
                        .ifBlank { "No chips picked" },
                enabled = settings.showTaskPicker,
                onToggle = { enabled ->
                    onSettingsChange(
                        settings.copy(
                            showTaskPicker = enabled
                        )
                    )
                }
            )

            if (
                settings.showTaskPicker
            ) {
                SettingsChipToggles(
                    options = taskChips,
                    onToggle = { index, enabled ->
                        onSettingsChange(
                            if (index == 0) {
                                settings.copy(taskChipGoogle = enabled)
                            } else {
                                settings.copy(taskChipTodoist = enabled)
                            }
                        )
                    }
                )
            }

        }

        SettingsSectionLabel(
            text = "HOME SCREEN"
        )

        SettingsCardGroup {

            /*
             * Off hides the weather; °F / °C show it in that unit.
             * Turning it off keeps the unit for next time.
             */
            SettingsChoiceRow(
                label = "Weather",
                choices = listOf("Off", "°F", "°C"),
                selectedIndex =
                    when {
                        !settings.showWeather -> 0
                        settings.weatherCelsius -> 2
                        else -> 1
                    },
                onSelected = { index ->
                    onSettingsChange(
                        when (index) {
                            0 -> settings.copy(showWeather = false)
                            1 -> settings.copy(showWeather = true, weatherCelsius = false)
                            else -> settings.copy(showWeather = true, weatherCelsius = true)
                        }
                    )
                }
            )


            DaylightRowDivider()

            SettingsToggleRow(
                label = "Next event",
                detail = "Your next calendar event on the home card",
                enabled = settings.showCalendarCard,
                onToggle = { enabled ->
                    onSettingsChange(
                        settings.copy(
                            showCalendarCard = enabled
                        )
                    )
                }
            )

            DaylightRowDivider()

            SettingsToggleRow(
                label = "Texts & mail",
                detail = "New texts and mail on the home card",
                enabled = settings.showAttentionCard,
                onToggle = { enabled ->
                    onSettingsChange(
                        settings.copy(
                            showAttentionCard = enabled
                        )
                    )
                }
            )
        }

        Spacer(
            modifier = Modifier.height(6.dp)
        )
    }
}

@Composable
internal fun ProductivityBarSettingsScreen(
    settings: LauncherSettings,
    onSettingsChange: (LauncherSettings) -> Unit,
    onBack: () -> Unit
) {
    val context =
        LocalContext.current

    val installedApps =
        remember(
            context
        ) {
            loadInstalledProductivityApps(
                context
            )
        }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    BackgroundBlack
                )
                .padding(
                    horizontal = DaylightPageHorizontal,
                    vertical = DaylightPageVertical
                )
    ) {
        SettingsHeader(
            title = "24-hour ring",
            onBack = onBack
        )

        Spacer(
            modifier = Modifier.height(SettingsBlockGap)
        )

        LazyColumn(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
        ) {
            item {
                SettingsSectionHeader(
                    text = "RING",
                    first = true
                )
            }

            item {
                SettingsControlCard {
                    SettingsToggleRow(
                        label = "24-hour ring",
                        detail = "Flags hours with 20+ min on distracting apps",
                        enabled = settings.showProductivityDots,
                        onToggle = { enabled ->
                            onSettingsChange(
                                settings.copy(
                                    showProductivityDots =
                                        enabled
                                )
                            )
                        }
                    )
                }
            }

            item {
                SettingsSectionHeader(
                    text = "DISTRACTING APPS"
                )
            }

            if (
                installedApps.isEmpty()
            ) {
                item {
                    SettingsControlCard {
                        SettingsRow(
                            title = "No installed apps found",
                            titleColor = SecondaryText
                        )
                    }
                }
            } else {
                itemsIndexed(
                    installedApps,
                    key = { _, app ->
                        app.packageName
                    }
                ) { index, app ->
                    SettingsGroupItem(
                        isFirst = index == 0,
                        isLast = index == installedApps.lastIndex
                    ) {
                        SettingsAppToggleRow(
                            appName =
                                app.label,
                            packageName =
                                app.packageName,
                            enabled =
                                app.packageName in
                                        settings.distractingApps,
                            onToggle = { enabled ->
                                val updatedPackages =
                                    settings
                                        .distractingApps
                                        .toMutableSet()
                                        .apply {
                                            if (
                                                enabled
                                            ) {
                                                add(
                                                    app.packageName
                                                )
                                            } else {
                                                remove(
                                                    app.packageName
                                                )
                                            }
                                        }
                                        .toSet()

                                onSettingsChange(
                                    settings.copy(
                                        distractingApps =
                                            updatedPackages
                                    )
                                )
                            }
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
internal fun ReadabilitySettingsScreen(
    settings: LauncherSettings,
    onSettingsChange: (LauncherSettings) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    BackgroundBlack
                )
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    horizontal = DaylightPageHorizontal,
                    vertical = DaylightPageVertical
                ),
        verticalArrangement =
            Arrangement.spacedBy(SettingsBlockGap)
    ) {
        SettingsHeader(
            title = "Readability",
            onBack = onBack
        )

        SettingsSectionLabel(
            text = "TEXT"
        )

        SettingsCardGroup {
            SettingsChoiceRow(
                label = "Text size",
                choices = listOf(
                    "Small",
                    "Normal",
                    "Large",
                    "X-large"
                ),
                selectedIndex =
                    settings.readabilityTextSize.ordinal,
                onSelected = { index ->
                    onSettingsChange(
                        settings.copy(
                            readabilityTextSize =
                                ReadabilityTextSize.entries[index]
                        )
                    )
                }
            )

            DaylightRowDivider()

            SettingsChoiceRow(
                label = "Text weight",
                choices = listOf(
                    "Normal",
                    "Medium",
                    "Bold"
                ),
                selectedIndex =
                    settings.readabilityTextWeight.ordinal,
                onSelected = { index ->
                    onSettingsChange(
                        settings.copy(
                            readabilityTextWeight =
                                ReadabilityTextWeight.entries[index]
                        )
                    )
                }
            )
        }

        SettingsSectionLabel(
            text = "VISIBILITY"
        )

        SettingsCardGroup {
            SettingsChoiceRow(
                label = "Contrast",
                choices = listOf(
                    "Standard",
                    "High",
                    "Maximum"
                ),
                selectedIndex =
                    settings.readabilityContrast.ordinal,
                onSelected = { index ->
                    onSettingsChange(
                        settings.copy(
                            readabilityContrast =
                                ReadabilityContrast.entries[index]
                        )
                    )
                }
            )

            DaylightRowDivider()

            SettingsToggleRow(
                label = "Dim secondary text",
                enabled = settings.dimSecondaryText,
                onToggle = { enabled ->
                    onSettingsChange(
                        settings.copy(
                            dimSecondaryText = enabled
                        )
                    )
                }
            )
        }

        SettingsSectionLabel(
            text = "MOTION"
        )

        SettingsControlCard {
            SettingsChoiceRow(
                label = "Motion",
                choices = listOf(
                    "Normal",
                    "Reduced"
                ),
                selectedIndex =
                    settings.readabilityMotion.ordinal,
                onSelected = { index ->
                    onSettingsChange(
                        settings.copy(
                            readabilityMotion =
                                ReadabilityMotion.entries[index]
                        )
                    )
                }
            )
        }

        Spacer(
            modifier = Modifier.height(6.dp)
        )
    }
}

@Composable
internal fun LauncherBackButton(
    onBack: () -> Unit
) {
    Text(
        text = "<",
        color = AccentOrange,
        fontSize = 24.sp,
        lineHeight = 24.sp,
        fontFamily = InterfaceFont,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.clickable {
            onBack()
        }
    )
}

/*
 * DAYLIGHT SETTINGS KIT
 *
 * Every settings page is built from the blocks below, which render
 * the Daylight style from C-Settings: a BackgroundBlack page, a small
 * "Settings" caption over the big page title (right of the camera),
 * SurfaceBlack cards with hairlines between related rows, bold row
 * titles with an optional muted detail line, pill switches, and
 * segmented controls or wrapping chips for multi-choice settings.
 *
 * Sizes follow the mockup: px x 0.82 = dp, px x 0.70 = sp.
 */

/*
 * Gap between cards and labels on a page (12 px).
 */
internal val SettingsBlockGap = 10.dp

/*
 * Inner horizontal padding of a card (14 px) and the vertical padding
 * of one row (11 px).
 */
internal val SettingsCardHorizontal = 11.5.dp
internal val SettingsRowVertical = 9.dp
internal val SettingsCardRadius = 14.dp

/*
 * The page header: "‹ Settings" caption over the page title. Tapping
 * it goes back, as do the Back and Escape keys handled by the host.
 */
@Composable
internal fun SettingsHeader(
    title: String,
    onBack: () -> Unit
) {
    DaylightScreenTitle(
        title = title,
        caption = "‹ Settings",
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {
                    onBack()
                }
    )
}

@Composable
internal fun SettingsSectionLabel(
    text: String
) {
    DaylightSectionLabel(
        text = text,
        modifier =
            Modifier.padding(
                start = 4.dp,
                top = 2.dp
            )
    )
}

/*
 * A section label inside a LazyColumn, carrying its own gaps because
 * lazy settings pages do not use spacedBy (so grouped rows can touch).
 */
@Composable
internal fun SettingsSectionHeader(
    text: String,
    first: Boolean = false
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        if (!first) {
            Spacer(
                modifier = Modifier.height(SettingsBlockGap)
            )
        }

        SettingsSectionLabel(
            text = text
        )

        Spacer(
            modifier = Modifier.height(6.dp)
        )
    }
}

/*
 * Small muted helper text under or between cards.
 */
@Composable
internal fun SettingsFootnote(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        color = SecondaryText,
        fontSize = 8.sp,
        lineHeight = 10.sp,
        fontFamily = InterfaceFont,
        modifier = modifier.padding(horizontal = 4.dp)
    )
}

/*
 * A white (dark grey in dark mode) card holding several rows; put a
 * DaylightRowDivider between them.
 */
@Composable
internal fun SettingsCardGroup(
    content: @Composable ColumnScope.() -> Unit
) {
    DaylightCard(
        horizontalPadding = SettingsCardHorizontal,
        content = content
    )
}

@Composable
internal fun SettingsControlCard(
    content: @Composable () -> Unit
) {
    DaylightCard(
        horizontalPadding = SettingsCardHorizontal
    ) {
        content()
    }
}

/*
 * One row of a card that is split across LazyColumn items (long app
 * lists). The first and last rows round the card's corners; every
 * row after the first starts with a hairline.
 */
@Composable
internal fun SettingsGroupItem(
    isFirst: Boolean,
    isLast: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    val top =
        if (isFirst) SettingsCardRadius else 0.dp

    val bottom =
        if (isLast) SettingsCardRadius else 0.dp

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    SurfaceBlack,
                    RoundedCornerShape(
                        topStart = top,
                        topEnd = top,
                        bottomEnd = bottom,
                        bottomStart = bottom
                    )
                )
                .padding(
                    horizontal = SettingsCardHorizontal
                )
    ) {
        if (!isFirst) {
            DaylightRowDivider()
        }

        content()
    }
}

@Composable
private fun SettingsRowTitle(
    text: String,
    color: Color = PrimaryText
) {
    Text(
        text = text,
        color = color,
        fontSize = 10.5.sp,
        lineHeight = 12.5.sp,
        fontFamily = InterfaceFont,
        fontWeight = FontWeight.SemiBold,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun SettingsRowDetail(
    text: String
) {
    Text(
        text = text,
        color = SecondaryText,
        fontSize = 8.5.sp,
        lineHeight = 10.5.sp,
        fontFamily = InterfaceFont,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

/*
 * The Daylight settings row: bold title, optional muted detail, and
 * whatever control sits on the right. Clickable rows are focusable,
 * so the hardware keyboard can move through them.
 */
@Composable
internal fun SettingsRow(
    title: String,
    detail: String? = null,
    onClick: (() -> Unit)? = null,
    titleColor: Color = PrimaryText,
    trailing: @Composable () -> Unit = {}
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 40.dp)
                .then(
                    if (onClick != null) {
                        Modifier.clickable { onClick() }
                    } else {
                        Modifier
                    }
                )
                .padding(vertical = SettingsRowVertical),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            SettingsRowTitle(
                text = title,
                color = titleColor
            )

            if (!detail.isNullOrBlank()) {
                SettingsRowDetail(
                    text = detail
                )
            }
        }

        Spacer(
            modifier = Modifier.width(10.dp)
        )

        trailing()
    }
}

/*
 * The thin ">" at the end of a row that opens another page.
 */
@Composable
internal fun SettingsChevron() {
    val color = SecondaryText

    Canvas(
        modifier = Modifier.size(13.dp)
    ) {
        val strokeWidth = 1.6.dp.toPx()

        drawLine(
            color = color,
            start = Offset(size.width * 0.38f, size.height * 0.22f),
            end = Offset(size.width * 0.66f, size.height * 0.5f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        drawLine(
            color = color,
            start = Offset(size.width * 0.66f, size.height * 0.5f),
            end = Offset(size.width * 0.38f, size.height * 0.78f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

/*
 * A round selection mark for pick-one lists ("Compose email in").
 */
@Composable
internal fun SettingsRadioMark(
    selected: Boolean
) {
    Box(
        modifier =
            Modifier
                .size(18.dp)
                .border(
                    1.5.dp,
                    if (selected) AccentOrange else BorderGray,
                    CircleShape
                )
                .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            AccentOrange,
                            CircleShape
                        )
            )
        }
    }
}

/*
 * A setting that lives behind a prompt command: what it is, and the
 * command to type, shown in the accent colour.
 */
@Composable
internal fun SettingsCommandRow(
    title: String,
    detail: String,
    command: String
) {
    SettingsRow(
        title = title,
        detail = detail
    ) {
        Text(
            text = command,
            color = AccentOrange,
            fontSize = 9.5.sp,
            fontFamily = InterfaceFont,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
internal fun SettingsNavigationRow(
    symbol: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    SettingsRow(
        title = title,
        detail = subtitle,
        onClick = onClick
    ) {
        SettingsChevron()
    }
}

/*
 * A pick-one setting. Up to three short options sit in a segmented
 * control beside the title (like "Weather  °F | °C"); longer lists
 * wrap as chips under the title.
 */
@Composable
internal fun SettingsChoiceRow(
    label: String,
    choices: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    detail: String? = null
) {
    val fitsSegmented =
        choices.size <= 3 &&
                choices.all { it.length <= 9 } &&
                choices.sumOf { it.length } <= 22

    if (fitsSegmented) {
        SettingsRow(
            title = label,
            detail = detail
        ) {
            DaylightSegmented(
                options = choices,
                selectedIndex = selectedIndex,
                onSelect = onSelected
            )
        }
    } else {
        SettingsChoiceWrapRow(
            label = label,
            choices = choices,
            selectedIndex = selectedIndex,
            onSelected = onSelected,
            detail = detail
        )
    }
}

/*
 * A pick-one setting with many options: the title, then the options
 * as wrapping Daylight chips. perRow is kept for older callers; the
 * chips now flow to fit the width.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SettingsChoiceWrapRow(
    label: String,
    choices: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    perRow: Int = 3,
    detail: String? = null
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = SettingsRowVertical)
    ) {
        SettingsRowTitle(
            text = label
        )

        if (!detail.isNullOrBlank()) {
            SettingsRowDetail(
                text = detail
            )
        }

        Spacer(
            modifier = Modifier.height(7.dp)
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(5.dp),
            verticalArrangement =
                Arrangement.spacedBy(5.dp)
        ) {
            choices.forEachIndexed { index, choice ->
                DaylightChoiceChip(
                    label = choice,
                    selected = index == selectedIndex,
                    onClick = {
                        onSelected(index)
                    }
                )
            }
        }
    }
}

/*
 * Several independent on/off options shown as chips under a row
 * (the search picker's Chrome / Claude / Apps).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SettingsChipToggles(
    options: List<Pair<String, Boolean>>,
    onToggle: (index: Int, enabled: Boolean) -> Unit
) {
    FlowRow(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(bottom = SettingsRowVertical),
        horizontalArrangement =
            Arrangement.spacedBy(5.dp),
        verticalArrangement =
            Arrangement.spacedBy(5.dp)
    ) {
        options.forEachIndexed { index, (label, enabled) ->
            DaylightChoiceChip(
                label = label,
                selected = enabled,
                onClick = {
                    onToggle(index, !enabled)
                }
            )
        }
    }
}

@Composable
internal fun SettingsToggleRow(
    label: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    detail: String? = null
) {
    SettingsRow(
        title = label,
        detail = detail,
        onClick = {
            onToggle(!enabled)
        }
    ) {
        DaylightSwitch(
            checked = enabled,
            onCheckedChange = onToggle
        )
    }
}

@Composable
internal fun SettingsAppToggleRow(
    appName: String,
    packageName: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    SettingsRow(
        title = appName,
        detail = packageName,
        onClick = {
            onToggle(!enabled)
        }
    ) {
        DaylightSwitch(
            checked = enabled,
            onCheckedChange = onToggle
        )
    }
}

/*
 * "Appearance  Light | Dark | Auto". Auto follows the phone's dark
 * mode; Light and Dark pin Daylight light or dark. While one of the
 * other themes (navy, slate...) is in use none of the three is lit;
 * the full list lives on the Appearance page.
 */
@Composable
internal fun SettingsAppearanceRow(
    settings: LauncherSettings,
    onSettingsChange: (LauncherSettings) -> Unit
) {
    SettingsRow(
        title = "Appearance",
        detail = "Auto follows your phone's dark mode"
    ) {
        DaylightSegmented(
            options = listOf("Light", "Dark", "Auto"),
            selectedIndex =
                when {
                    settings.followSystemTheme -> 2
                    settings.themeMode == LauncherThemeMode.LIGHT -> 0
                    settings.themeMode == LauncherThemeMode.DARK -> 1
                    else -> -1
                },
            onSelect = { index ->
                onSettingsChange(
                    when (index) {
                        0 ->
                            settings.copy(
                                followSystemTheme = false,
                                themeMode = LauncherThemeMode.LIGHT
                            )

                        1 ->
                            settings.copy(
                                followSystemTheme = false,
                                themeMode = LauncherThemeMode.DARK
                            )

                        else ->
                            settings.copy(
                                followSystemTheme = true
                            )
                    }
                )
            }
        )
    }
}

/*
 * "Accent" with a row of colour swatches; the picked one wears a ring.
 */
@Composable
internal fun SettingsAccentRow(
    settings: LauncherSettings,
    onSettingsChange: (LauncherSettings) -> Unit
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = SettingsRowVertical)
    ) {
        SettingsRowTitle(
            text = "Accent"
        )

        Spacer(
            modifier = Modifier.height(7.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LauncherAccent.entries.forEach { accent ->
                val selected =
                    settings.accent == accent

                val swatch =
                    accentColorFor(accent)

                Box(
                    modifier =
                        Modifier
                            .size(24.dp)
                            .border(
                                width = 1.5.dp,
                                color = if (selected) swatch else Color.Transparent,
                                shape = CircleShape
                            )
                            .clip(CircleShape)
                            .clickable {
                                onSettingsChange(
                                    settings.copy(
                                        accent = accent
                                    )
                                )
                            }
                            .padding(3.5.dp)
                            .background(
                                swatch,
                                CircleShape
                            )
                )
            }
        }
    }
}

internal fun settingsChoiceTextNeedsDarkAccent(): Boolean {
    return AccentOrange == Color(0xFFFFC83D)
}

internal fun hasNotificationListenerAccess(
    context: Context
): Boolean {
    val enabledListeners =
        Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
            ?: return false

    return enabledListeners
        .split(":")
        .mapNotNull {
            ComponentName.unflattenFromString(
                it
            )
        }
        .any {
            it.packageName ==
                    context.packageName
        }
}