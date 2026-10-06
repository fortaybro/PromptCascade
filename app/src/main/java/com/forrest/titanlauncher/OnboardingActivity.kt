package com.forrest.titanlauncher

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.forrest.titanlauncher.ai.GeminiApiKeyStore
import com.forrest.titanlauncher.settings.LauncherAccent
import com.forrest.titanlauncher.settings.LauncherInterfaceFont
import com.forrest.titanlauncher.settings.LauncherSettings
import com.forrest.titanlauncher.settings.LauncherSettingsStore
import com.forrest.titanlauncher.settings.LauncherTextColor
import com.forrest.titanlauncher.settings.LauncherThemeMode
import com.forrest.titanlauncher.messages.SmsRoleManager
import com.forrest.titanlauncher.todoist.GoogleTasksAuthManager
import com.forrest.titanlauncher.todoist.TaskProvider
import com.forrest.titanlauncher.usage.UsageStatsRepository
import com.forrest.titanlauncher.mail.findEmailProviders
import com.forrest.titanlauncher.todoist.TodoistTokenStore
import com.forrest.titanlauncher.ui.theme.TitanLauncherTheme
import com.forrest.titanlauncher.weather.WeatherRepository
import com.forrest.titanlauncher.weather.WeatherSnapshot

class OnboardingActivity : ComponentActivity() {

    /*
     * Every time we return from an Android system screen
     * (Home chooser, SMS chooser, permissions/settings, etc.)
     * this value changes.
     *
     * Compose observes it and re-checks the real Android state.
     */
    private val resumeRefreshState =
        mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        if (onboardingComplete()) {
            openLauncher()
            return
        }

        hideSystemBars()

        /*
         * Onboarding is its own Activity, so the Daylight theme globals
         * (page, card, text, accent, font) would otherwise still hold
         * their dark defaults. Applied here so the very first frame is
         * already Daylight, and again from the composition whenever the
         * appearance step or the phone's dark mode changes.
         */
        val systemDark =
            (
                    resources.configuration.uiMode and
                            Configuration.UI_MODE_NIGHT_MASK
                    ) ==
                    Configuration.UI_MODE_NIGHT_YES

        runCatching {
            applyLauncherAppearance(
                LauncherSettingsStore(
                    this
                ).load(),
                systemDark
            )
        }

        setContent {
            TitanLauncherTheme {

                /*
                 * The same 360 dp canvas and 1.16x text the launcher
                 * uses, so Daylight sizes match the home screen.
                 */
                TitanUiScale {

                    val baseDensity =
                        LocalDensity.current

                    val onboardingDensity =
                        Density(
                            density =
                                baseDensity.density,
                            fontScale =
                                baseDensity.fontScale *
                                        ReadabilityFontScale *
                                        1.16f
                        )

                    CompositionLocalProvider(
                        LocalDensity provides onboardingDensity
                    ) {
                        PromptLauncherOnboarding(
                            resumeRefresh =
                                resumeRefreshState.intValue,
                            initialStep =
                                getSavedOnboardingStep(),
                            onStepChanged = {
                                    step ->

                                saveOnboardingStep(
                                    step
                                )
                            },
                            onFinish = {
                                markOnboardingComplete()
                                openLauncher()
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        resumeRefreshState.intValue +=
            1

        hideSystemBars()
    }

    private fun onboardingComplete(): Boolean {
        return getSharedPreferences(
            ONBOARDING_PREFS,
            Context.MODE_PRIVATE
        ).getBoolean(
            KEY_ONBOARDING_COMPLETE,
            false
        )
    }

    private fun getSavedOnboardingStep(): Int {
        return getSharedPreferences(
            ONBOARDING_PREFS,
            Context.MODE_PRIVATE
        )
            .getInt(
                KEY_ONBOARDING_STEP,
                0
            )
            .coerceIn(
                0,
                7
            )
    }

    private fun saveOnboardingStep(
        step: Int
    ) {
        getSharedPreferences(
            ONBOARDING_PREFS,
            Context.MODE_PRIVATE
        )
            .edit()
            .putInt(
                KEY_ONBOARDING_STEP,
                step.coerceIn(
                    0,
                    7
                )
            )
            .apply()
    }

    private fun markOnboardingComplete() {
        getSharedPreferences(
            ONBOARDING_PREFS,
            Context.MODE_PRIVATE
        )
            .edit()
            .putBoolean(
                KEY_ONBOARDING_COMPLETE,
                true
            )
            .remove(
                KEY_ONBOARDING_STEP
            )
            .apply()
    }

    private fun openLauncher() {
        startActivity(
            Intent(
                this,
                MainActivity::class.java
            ).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            }
        )

        finish()
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
            WindowInsetsCompat.Type.systemBars()
        )

        controller.systemBarsBehavior =
            WindowInsetsControllerCompat
                .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    companion object {
        private const val ONBOARDING_PREFS =
            "prompt_launcher_onboarding"

        private const val KEY_ONBOARDING_COMPLETE =
            "onboarding_complete"

        private const val KEY_ONBOARDING_STEP =
            "onboarding_step"
    }
}

@Composable
private fun PromptLauncherOnboarding(
    resumeRefresh: Int,
    initialStep: Int,
    onStepChanged: (Int) -> Unit,
    onFinish: () -> Unit
) {
    val context =
        LocalContext.current

    val activity =
        context as Activity

    val displayedVersion =
        remember(context) {
            getPromptLauncherVersionName(
                context
            )
        }

    val taskStore =
        remember {
            TodoistTokenStore(
                context
            )
        }

    val googleTasksAuth =
        remember {
            GoogleTasksAuthManager(
                activity
            )
        }

    val geminiStore =
        remember {
            GeminiApiKeyStore(
                context
            )
        }

    val launcherSettingsStore =
        remember {
            LauncherSettingsStore(
                context
            )
        }

    /*
     * Applied as it is edited, so the choices on the appearance step
     * are what the launcher opens with.
     */
    var launcherSettings by remember {
        mutableStateOf(
            launcherSettingsStore.load()
        )
    }

    /*
     * Keeps the theme globals in step with the stored settings, so the
     * appearance step re-themes onboarding as choices are made, and
     * "auto" follows the phone's dark mode.
     */
    val systemDarkTheme =
        isSystemInDarkTheme()

    LaunchedEffect(
        launcherSettings,
        systemDarkTheme
    ) {
        applyLauncherAppearance(
            launcherSettings,
            systemDarkTheme
        )
    }

    val smsRoleManager =
        remember {
            SmsRoleManager(
                context
            )
        }

    val weatherRepository =
        remember {
            WeatherRepository(
                context
            )
        }

    var step by remember(
        initialStep
    ) {
        mutableIntStateOf(
            initialStep.coerceIn(
                0,
                7
            )
        )
    }

    LaunchedEffect(
        step
    ) {
        onStepChanged(
            step
        )
    }

    var permissionRefresh by remember {
        mutableIntStateOf(0)
    }

    var homeRefresh by remember {
        mutableIntStateOf(0)
    }

    var smsRefresh by remember {
        mutableIntStateOf(0)
    }

    var weatherRefresh by remember {
        mutableIntStateOf(0)
    }

    var permissionAttempted by remember {
        mutableStateOf(false)
    }

    var weatherChecking by remember {
        mutableStateOf(false)
    }

    /*
     * Usage Access can't use a system runtime dialog, so onboarding
     * shows its own prompt once, right after the Android prompts.
     */
    var showUsageAccessPrompt by remember {
        mutableStateOf(false)
    }

    var usageAccessPromptShown by remember {
        mutableStateOf(false)
    }

    var weatherCheckAttempted by remember {
        mutableStateOf(false)
    }

    var weatherSnapshot by remember {
        mutableStateOf<WeatherSnapshot?>(
            null
        )
    }

    var todoistToken by remember {
        mutableStateOf("")
    }

    var geminiApiKey by remember {
        mutableStateOf("")
    }

    var taskProvider by remember {
        mutableStateOf(
            taskStore.getTaskProvider()
        )
    }

    var googleTasksConnected by remember {
        mutableStateOf(
            taskStore.isGoogleTasksConnected()
        )
    }

    var taskSetupMessage by remember {
        mutableStateOf<String?>(null)
    }

    var todoistSaved by remember {
        mutableStateOf(
            runCatching {
                taskStore
                    .getToken()
                    .isNotBlank()
            }
                .getOrDefault(
                    false
                )
        )
    }

    var googleTasksAuthorizing by remember {
        mutableStateOf(false)
    }

    var geminiSaved by remember {
        mutableStateOf(
            geminiStore.hasKey()
        )
    }

    /*
     * resumeRefresh is included in these remember keys.
     *
     * That forces a fresh Android permission/role check whenever
     * onboarding returns from a system screen.
     */
    val contactsReady =
        remember(
            permissionRefresh,
            resumeRefresh
        ) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        }

    val calendarReady =
        remember(
            permissionRefresh,
            resumeRefresh
        ) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CALENDAR
            ) == PackageManager.PERMISSION_GRANTED
        }

    val weatherPermissionReady =
        remember(
            permissionRefresh,
            resumeRefresh
        ) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }

    /*
     * Usage Access powers the productivity dots. A fresh install
     * resets it, and without it every dot stays gray.
     */
    val usageAccessReady =
        remember(
            permissionRefresh,
            resumeRefresh
        ) {
            runCatching {
                UsageStatsRepository(
                    context
                ).hasUsageAccess()
            }
                .getOrDefault(
                    false
                )
        }

    val essentialPermissionsReady =
        contactsReady &&
                calendarReady

    val shouldRequestPermissions =
        !essentialPermissionsReady ||
                (
                        !weatherPermissionReady &&
                                !permissionAttempted
                        )

    val homeReady =
        remember(
            homeRefresh,
            resumeRefresh
        ) {
            promptLauncherIsHome(
                context
            )
        }

    /*
     * Retained but no longer part of onboarding. Prompt Launcher
     * works alongside whichever app owns SMS now, so asking for the
     * role up front was a step that changed how messaging behaved
     * before the user had seen any of it.
     *
     * The toggles card still offers the switch for anyone who wants
     * it, and these keep that path working.
     */
    val smsRoleAvailable =
        remember(
            smsRefresh,
            resumeRefresh
        ) {
            runCatching {
                smsRoleManager
                    .isSmsRoleAvailable()
            }
                .getOrDefault(
                    false
                )
        }

    val smsReady =
        remember(
            smsRefresh,
            resumeRefresh
        ) {
            runCatching {
                smsRoleManager
                    .isDefaultSmsApp()
            }
                .getOrDefault(
                    false
                )
        }

    /*
     * Weather gets another check whenever we return from
     * Android settings as well.
     */
    LaunchedEffect(
        weatherPermissionReady,
        weatherRefresh,
        resumeRefresh
    ) {
        if (
            weatherPermissionReady
        ) {
            weatherChecking =
                true

            weatherCheckAttempted =
                false

            weatherSnapshot =
                weatherRepository
                    .loadCurrentWeather()

            weatherChecking =
                false

            weatherCheckAttempted =
                true
        } else {
            weatherChecking =
                false

            weatherCheckAttempted =
                false

            weatherSnapshot =
                null
        }
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.RequestMultiplePermissions()
        ) {
            permissionAttempted =
                true

            permissionRefresh +=
                1

            val usageGranted =
                runCatching {
                    UsageStatsRepository(
                        context
                    ).hasUsageAccess()
                }
                    .getOrDefault(
                        false
                    )

            if (
                !usageGranted &&
                !usageAccessPromptShown
            ) {
                usageAccessPromptShown =
                    true

                showUsageAccessPrompt =
                    true
            }
        }

    val appSettingsLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.StartActivityForResult()
        ) {
            permissionRefresh +=
                1
        }

    val homeLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.StartActivityForResult()
        ) {
            homeRefresh +=
                1
        }

    val smsRoleLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.StartActivityForResult()
        ) {
            smsRefresh +=
                1
        }

    val googleTasksConsentLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->

            googleTasksAuthorizing =
                false

            val token =
                googleTasksAuth
                    .getAccessTokenFromResult(
                        result.data
                    )

            if (
                !token.isNullOrBlank()
            ) {
                taskStore
                    .selectGoogleTasks()

                taskProvider =
                    TaskProvider.GOOGLE_TASKS

                googleTasksConnected =
                    true

                taskSetupMessage =
                    null
            } else {
                taskSetupMessage =
                    "Google Tasks connection cancelled."
            }
        }

    fun requestSetupPermissions() {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.READ_CALENDAR,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    fun openApplicationSettings() {
        try {
            appSettingsLauncher.launch(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse(
                        "package:${context.packageName}"
                    )
                )
            )
        } catch (
            _: Exception
        ) {
            context.startActivity(
                Intent(
                    Settings.ACTION_SETTINGS
                )
            )
        }
    }

    /*
     * Usage Access is a special app-op, not a runtime permission, so
     * it can only be granted from its own system screen. Falls back to
     * this app's details page if the Usage Access screen is missing.
     */
    fun openUsageAccessSettings() {
        try {
            appSettingsLauncher.launch(
                Intent(
                    Settings.ACTION_USAGE_ACCESS_SETTINGS
                )
            )
        } catch (
            _: Exception
        ) {
            openApplicationSettings()
        }
    }

    fun requestHomeRole() {
        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.Q
        ) {
            val roleManager =
                context.getSystemService(
                    RoleManager::class.java
                )

            if (
                roleManager != null &&
                roleManager.isRoleAvailable(
                    RoleManager.ROLE_HOME
                )
            ) {
                if (
                    roleManager.isRoleHeld(
                        RoleManager.ROLE_HOME
                    )
                ) {
                    homeRefresh +=
                        1
                } else {
                    try {
                        homeLauncher.launch(
                            roleManager
                                .createRequestRoleIntent(
                                    RoleManager.ROLE_HOME
                                )
                        )
                    } catch (
                        _: Exception
                    ) {
                        openHomeSettings(
                            context =
                                context,
                            launcher = { intent ->
                                homeLauncher.launch(
                                    intent
                                )
                            }
                        )
                    }
                }

                return
            }
        }

        openHomeSettings(
            context =
                context,
            launcher = { intent ->
                homeLauncher.launch(
                    intent
                )
            }
        )
    }

    fun requestSmsRole() {
        if (
            smsRoleManager
                .isDefaultSmsApp()
        ) {
            smsRefresh +=
                1

            return
        }

        if (
            !smsRoleManager
                .isSmsRoleAvailable()
        ) {
            smsRefresh +=
                1

            return
        }

        try {
            smsRoleLauncher.launch(
                smsRoleManager
                    .createSmsRoleRequestIntent()
            )
        } catch (
            _: Exception
        ) {
            smsRefresh +=
                1
        }
    }

    fun connectGoogleTasks() {
        googleTasksAuthorizing =
            true

        taskSetupMessage =
            null

        googleTasksAuth.authorize(
            onAuthorized = {
                taskStore
                    .selectGoogleTasks()

                taskProvider =
                    TaskProvider.GOOGLE_TASKS

                googleTasksConnected =
                    true

                googleTasksAuthorizing =
                    false

                taskSetupMessage =
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

                    googleTasksConsentLauncher
                        .launch(
                            request
                        )
                } catch (
                    _: Exception
                ) {
                    googleTasksAuthorizing =
                        false

                    taskSetupMessage =
                        "Couldn't open Google sign-in."
                }
            },
            onError = {
                    message ->

                googleTasksAuthorizing =
                    false

                taskSetupMessage =
                    message
            }
        )
    }

    /*
     * Saves a pasted Todoist token right away (the card's Save button),
     * or on Continue if one is still in the field.
     */
    fun saveTodoistToken() {
        if (
            todoistToken.isBlank()
        ) {
            return
        }

        taskStore
            .saveToken(
                todoistToken.trim()
            )

        taskProvider =
            TaskProvider.TODOIST

        todoistToken =
            ""

        todoistSaved =
            true

        taskSetupMessage =
            null
    }

    /*
     * DAYLIGHT LAYOUT
     *
     * Page grey, "Step N of 8" beside the camera with Skip on the
     * right, the step's title and cards in the middle (scrolling on
     * the tallest steps), and the actions pinned to the bottom.
     */
    val headerTrailing: String? =
        when (
            step
        ) {
            0 -> "Skip setup"
            1 -> "Everything stays on your phone"
            ONBOARDING_LAST_STEP -> null
            else -> "Skip"
        }

    /*
     * "Skip setup" on the welcome page goes straight to the last page,
     * which still finishes through Start prompting. "Skip" on the
     * others moves on without saving anything typed on that step.
     */
    val headerTrailingAction: (() -> Unit)? =
        when (
            step
        ) {
            0 -> {
                {
                    step =
                        ONBOARDING_LAST_STEP
                }
            }

            1, ONBOARDING_LAST_STEP -> null

            else -> {
                {
                    step =
                        (step + 1).coerceAtMost(
                            ONBOARDING_LAST_STEP
                        )
                }
            }
        }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    BackgroundBlack
                )
                .padding(
                    start = 20.dp,
                    end = 20.dp,
                    top = 16.dp,
                    bottom = 16.dp
                )
    ) {
        Column(
            modifier =
                Modifier.fillMaxSize()
        ) {

            DaylightOnboardingHeader(
                stepLabel =
                    "Step ${step + 1} of $ONBOARDING_STEP_COUNT",
                trailing =
                    headerTrailing,
                onTrailingClick =
                    headerTrailingAction
            )

            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(
                            1f
                        )
                        .verticalScroll(
                            rememberScrollState()
                        )
            ) {
                when (
                    step
                ) {
                    0 -> {
                        WelcomeStep(
                            versionName =
                                displayedVersion
                        )
                    }

                    1 -> {
                        PermissionsStep(
                            contactsReady =
                                contactsReady,
                            calendarReady =
                                calendarReady,
                            weatherPermissionReady =
                                weatherPermissionReady,
                            weatherChecking =
                                weatherChecking,
                            weatherCheckAttempted =
                                weatherCheckAttempted,
                            weatherSnapshot =
                                weatherSnapshot,
                            permissionAttempted =
                                permissionAttempted,
                            usageAccessReady =
                                usageAccessReady,
                            onAllow = {
                                requestSetupPermissions()
                            },
                            onOpenSettings = {
                                openApplicationSettings()
                            },
                            onOpenUsageAccess = {
                                openUsageAccessSettings()
                            },
                            onRetryWeather = {
                                weatherRefresh +=
                                    1
                            }
                        )
                    }

                    2 -> {
                        HomeStep(
                            homeReady =
                                homeReady,
                            onChooseHome = {
                                requestHomeRole()
                            }
                        )
                    }

                    3 -> {
                        EmailSetupStep(
                            settings =
                                launcherSettings,
                            onSettingsChange = {
                                    updated ->
                                launcherSettings =
                                    updated
                                launcherSettingsStore.save(
                                    updated
                                )
                            }
                        )
                    }

                    4 -> {
                        TasksSetupStep(
                            provider =
                                taskProvider,
                            googleTasksConnected =
                                googleTasksConnected,
                            googleTasksAuthorizing =
                                googleTasksAuthorizing,
                            taskSetupMessage =
                                taskSetupMessage,
                            todoistToken =
                                todoistToken,
                            todoistSaved =
                                todoistSaved,
                            onTodoistTokenChange = {
                                todoistToken =
                                    it
                            },
                            onConnectGoogleTasks = {
                                connectGoogleTasks()
                            },
                            onSaveTodoist = {
                                saveTodoistToken()
                            }
                        )
                    }

                    5 -> {
                        GeminiSetupStep(
                            apiKey =
                                geminiApiKey,
                            onApiKeyChange = {
                                geminiApiKey =
                                    it
                            },
                            saved =
                                geminiSaved
                        )
                    }

                    6 -> {
                        AppearanceSetupStep(
                            settings =
                                launcherSettings,
                            onSettingsChange = {
                                    updated ->

                                launcherSettings =
                                    updated

                                launcherSettingsStore.save(
                                    updated
                                )
                            }
                        )
                    }

                    else -> {
                        ReadyStep(
                            geminiReady =
                                geminiSaved,
                            weatherPermissionReady =
                                weatherPermissionReady,
                            weatherSnapshot =
                                weatherSnapshot
                        )
                    }
                }
            }

            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )

            if (
                showUsageAccessPrompt
            ) {
                UsageAccessPromptDialog(
                    onAllow = {
                        showUsageAccessPrompt =
                            false

                        openUsageAccessSettings()
                    },
                    onDismiss = {
                        showUsageAccessPrompt =
                            false
                    }
                )
            }

            when (
                step
            ) {
                0 -> {
                    DaylightOnboardingActions(
                        primaryLabel =
                            "Get started",
                        onPrimary = {
                            step =
                                1
                        }
                    )
                }

                1 -> {
                    DaylightOnboardingActions(
                        primaryLabel =
                            if (
                                shouldRequestPermissions
                            ) {
                                "Allow access"
                            } else {
                                "Continue"
                            },
                        onPrimary = {
                            if (
                                shouldRequestPermissions
                            ) {
                                requestSetupPermissions()
                            } else if (
                                !usageAccessReady &&
                                !usageAccessPromptShown
                            ) {
                                /*
                                 * Runtime permissions were already
                                 * granted, so ask about usage here.
                                 */
                                usageAccessPromptShown =
                                    true

                                showUsageAccessPrompt =
                                    true
                            } else {
                                step =
                                    2
                            }
                        },
                        onBack = {
                            step =
                                0
                        }
                    )
                }

                2 -> {
                    DaylightOnboardingActions(
                        primaryLabel =
                            if (
                                homeReady
                            ) {
                                "Continue"
                            } else {
                                "Choose Prompt Launcher"
                            },
                        onPrimary = {
                            if (
                                homeReady
                            ) {
                                step =
                                    3
                            } else {
                                requestHomeRole()
                            }
                        },
                        onBack = {
                            step =
                                1
                        }
                    )
                }

                3 -> {
                    DaylightOnboardingActions(
                        primaryLabel =
                            "Continue",
                        onPrimary = {
                            step =
                                4
                        },
                        onBack = {
                            step =
                                2
                        }
                    )
                }

                4 -> {
                    val taskButtonText =
                        when {
                            todoistToken.isNotBlank() ->
                                "Save & continue"

                            taskProvider ==
                                    TaskProvider.GOOGLE_TASKS ->
                                "Continue"

                            taskProvider ==
                                    TaskProvider.TODOIST ->
                                "Continue"

                            else ->
                                "Skip for now"
                        }

                    DaylightOnboardingActions(
                        primaryLabel =
                            taskButtonText,
                        onPrimary = {
                            saveTodoistToken()

                            step =
                                5
                        },
                        onBack = {
                            step =
                                3
                        }
                    )
                }

                5 -> {
                    DaylightOnboardingActions(
                        primaryLabel =
                            when {
                                geminiApiKey.isNotBlank() ->
                                    "Save & continue"

                                geminiSaved ->
                                    "Continue"

                                else ->
                                    "Skip for now"
                            },
                        onPrimary = {

                            if (
                                geminiApiKey.isNotBlank()
                            ) {
                                geminiStore
                                    .saveKey(
                                        geminiApiKey.trim()
                                    )

                                geminiSaved =
                                    true

                                geminiApiKey =
                                    ""
                            }

                            step =
                                6
                        },
                        onBack = {
                            step =
                                4
                        }
                    )
                }

                6 -> {
                    DaylightOnboardingActions(
                        primaryLabel =
                            "Continue",
                        onPrimary = {
                            step =
                                7
                        },
                        onBack = {
                            step =
                                5
                        }
                    )
                }

                else -> {
                    DaylightOnboardingActions(
                        primaryLabel =
                            "Start prompting",
                        onPrimary =
                            onFinish
                    )
                }
            }
        }
    }
}

private const val ONBOARDING_STEP_COUNT =
    8

private const val ONBOARDING_LAST_STEP =
    ONBOARDING_STEP_COUNT - 1

/*
 * WELCOME (D-Welcome)
 *
 * The hour ring around the accent key, the name, the one-line pitch.
 */
@Composable
private fun WelcomeStep(
    versionName: String
) {
    Column(
        modifier =
            Modifier.fillMaxWidth(),
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Spacer(
            modifier =
                Modifier.height(
                    8.dp
                )
        )

        DaylightWelcomeRing()

        Spacer(
            modifier =
                Modifier.height(
                    12.dp
                )
        )

        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.spacedBy(
                    6.5.dp
                )
        ) {

            Text(
                text =
                    "Prompt Launcher",
                color =
                    PrimaryText,
                fontSize =
                    26.5.sp,
                lineHeight =
                    27.sp,
                letterSpacing =
                    (-0.8).sp,
                fontFamily =
                    InterfaceFont,
                fontWeight =
                    FontWeight.Bold,
                textAlign =
                    TextAlign.Center
            )

            Text(
                text =
                    "Type what you want. Press Enter.",
                color =
                    PrimaryText,
                fontSize =
                    12.sp,
                lineHeight =
                    15.sp,
                fontFamily =
                    InterfaceFont,
                fontWeight =
                    FontWeight.Medium,
                textAlign =
                    TextAlign.Center
            )

            Text(
                text =
                    "Text, call, add events and to-dos, or search, all from one line on your home screen.",
                color =
                    SecondaryText,
                fontSize =
                    10.sp,
                lineHeight =
                    13.5.sp,
                fontFamily =
                    InterfaceFont,
                textAlign =
                    TextAlign.Center,
                modifier =
                    Modifier.widthIn(
                        max = 246.dp
                    )
            )
        }

        Spacer(
            modifier =
                Modifier.height(
                    10.dp
                )
        )

        Text(
            text =
                "Version $versionName",
            color =
                TertiaryText,
            fontSize =
                8.sp,
            lineHeight =
                10.sp,
            fontFamily =
                InterfaceFont
        )
    }
}

/*
 * DEVICE ACCESS (D-Access)
 *
 * One card, one row per permission: what it is, what it unlocks, and
 * a pill on the right — accent "Allow" when needed, a green tick once
 * granted, grey for the optional ones.
 */
@Composable
private fun PermissionsStep(
    contactsReady: Boolean,
    calendarReady: Boolean,
    weatherPermissionReady: Boolean,
    weatherChecking: Boolean,
    weatherCheckAttempted: Boolean,
    weatherSnapshot: WeatherSnapshot?,
    permissionAttempted: Boolean,
    usageAccessReady: Boolean,
    onAllow: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenUsageAccess: () -> Unit,
    onRetryWeather: () -> Unit
) {
    Column(
        modifier =
            Modifier.fillMaxWidth(),
        verticalArrangement =
            Arrangement.spacedBy(
                10.dp
            )
    ) {

        DaylightOnboardingTitle(
            title =
                "What Prompt Launcher can see",
            subtitle =
                "Each one turns on a feature. Skip any you don't want."
        )

        DaylightCard {

            DaylightListRow(
                title =
                    "Contacts",
                detail =
                    "Text and call people by name"
            ) {
                RequiredPermissionPill(
                    ready =
                        contactsReady,
                    onAllow =
                        onAllow
                )
            }

            DaylightRowDivider()

            DaylightListRow(
                title =
                    "Calendar",
                detail =
                    "Your next event, and adding events with :"
            ) {
                RequiredPermissionPill(
                    ready =
                        calendarReady,
                    onAllow =
                        onAllow
                )
            }

            DaylightRowDivider()

            DaylightListRow(
                title =
                    "Location",
                detail =
                    "Approximate, for the weather"
            ) {
                when {
                    !weatherPermissionReady ->
                        DaylightStatusPill(
                            label =
                                "Allow",
                            kind =
                                DaylightPillKind.NEUTRAL,
                            onClick =
                                onAllow
                        )

                    weatherChecking ->
                        DaylightStatusPill(
                            label =
                                "Checking",
                            kind =
                                DaylightPillKind.NEUTRAL
                        )

                    weatherSnapshot != null ->
                        DaylightStatusPill(
                            label =
                                "Ready",
                            kind =
                                DaylightPillKind.READY
                        )

                    weatherCheckAttempted ->
                        DaylightStatusPill(
                            label =
                                "Retry",
                            kind =
                                DaylightPillKind.NEUTRAL,
                            onClick =
                                onRetryWeather
                        )

                    else ->
                        DaylightStatusPill(
                            label =
                                "On",
                            kind =
                                DaylightPillKind.READY
                        )
                }
            }

            DaylightRowDivider()

            /*
             * Usage Access has no runtime dialog: the pill opens its
             * system screen, and the status refreshes on return
             * through resumeRefresh.
             */
            DaylightListRow(
                title =
                    "Usage access",
                detail =
                    "Fills in the 24-hour ring"
            ) {
                if (
                    usageAccessReady
                ) {
                    DaylightStatusPill(
                        label =
                            "Ready",
                        kind =
                            DaylightPillKind.READY
                    )
                } else {
                    DaylightStatusPill(
                        label =
                            "Optional",
                        kind =
                            DaylightPillKind.NEUTRAL,
                        onClick =
                            onOpenUsageAccess
                    )
                }
            }
        }

        if (
            !usageAccessReady
        ) {
            DaylightOnboardingNote(
                text =
                    "Usage access lets the hourly dots fill in. If its switch is greyed out, open App info → ⋮ → Allow restricted settings first."
            )
        }

        if (
            permissionAttempted &&
            (
                    !contactsReady ||
                            !calendarReady
                    )
        ) {
            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween,
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                DaylightOnboardingNote(
                    text =
                        "Access is still off."
                )

                DaylightOnboardingLink(
                    text =
                        "App settings →",
                    onClick =
                        onOpenSettings
                )
            }
        } else if (
            weatherPermissionReady &&
            weatherCheckAttempted &&
            weatherSnapshot == null
        ) {
            DaylightOnboardingNote(
                text =
                    "Location is on, but the weather isn't available yet."
            )
        } else if (
            permissionAttempted &&
            contactsReady &&
            calendarReady &&
            !weatherPermissionReady
        ) {
            DaylightOnboardingNote(
                text =
                    "Weather is optional. Prompt Launcher works without it."
            )
        }
    }
}

@Composable
private fun RequiredPermissionPill(
    ready: Boolean,
    onAllow: () -> Unit
) {
    if (
        ready
    ) {
        DaylightStatusPill(
            label =
                "Ready",
            kind =
                DaylightPillKind.READY
        )
    } else {
        DaylightStatusPill(
            label =
                "Allow",
            kind =
                DaylightPillKind.ACCENT,
            onClick =
                onAllow
        )
    }
}

/*
 * Shown once after the Android prompts. Allow hands off to the Usage
 * Access screen, since the toggle itself only lives there.
 */
@Composable
private fun UsageAccessPromptDialog(
    onAllow: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest =
            onDismiss
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        SurfaceBlack,
                        RoundedCornerShape(
                            16.dp
                        )
                    )
                    .padding(
                        16.dp
                    ),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {
            Text(
                text =
                    "Allow Prompt Launcher to see app usage?",
                color =
                    PrimaryText,
                fontSize =
                    13.sp,
                lineHeight =
                    16.sp,
                fontFamily =
                    InterfaceFont,
                fontWeight =
                    FontWeight.Bold,
                textAlign =
                    TextAlign.Center
            )

            Spacer(
                modifier =
                    Modifier.height(
                        6.dp
                    )
            )

            Text(
                text =
                    "This fills in the 24-hour ring. On the next screen, tap Prompt Launcher and turn on usage access.",
                color =
                    SecondaryText,
                fontSize =
                    10.sp,
                lineHeight =
                    13.5.sp,
                fontFamily =
                    InterfaceFont,
                textAlign =
                    TextAlign.Center
            )

            Spacer(
                modifier =
                    Modifier.height(
                        14.dp
                    )
            )

            DaylightPrimaryButton(
                label =
                    "Allow",
                onClick =
                    onAllow
            )

            Spacer(
                modifier =
                    Modifier.height(
                        6.dp
                    )
            )

            DaylightSecondaryButton(
                label =
                    "Not now",
                modifier =
                    Modifier.fillMaxWidth(),
                onClick =
                    onDismiss
            )
        }
    }
}

/*
 * EMAIL (D-Email)
 *
 * Email works like texting: Daylight shows new mail on the home screen
 * and in the hub, and opening it goes to the email app. This step
 * picks which email apps count. The first one picked is the main app
 * that opens for "mail". Changeable later in settings.
 */
@Composable
private fun EmailSetupStep(
    settings: LauncherSettings,
    onSettingsChange: (LauncherSettings) -> Unit
) {

    val context =
        LocalContext.current

    val providers =
        remember(
            context
        ) {
            findEmailProviders(
                context
            )
        }

    Column(
        modifier =
            Modifier.fillMaxWidth(),
        verticalArrangement =
            Arrangement.spacedBy(
                10.dp
            )
    ) {

        DaylightOnboardingTitle(
            title =
                "Where's your email?",
            subtitle =
                "New mail shows on home. Tapping it opens the email in its own app."
        )

        if (
            providers.isEmpty()
        ) {
            DaylightCard(
                verticalPadding =
                    11.dp
            ) {
                DaylightOnboardingNote(
                    text =
                        "No email apps found. You can pick one later in Settings."
                )
            }
        } else {
            DaylightCard {
                providers.forEachIndexed { index, provider ->

                    val picked =
                        provider.packageName in
                                settings.mailApps

                    val isMain =
                        settings.mailApps.firstOrNull() ==
                                provider.packageName

                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(
                                    min = 36.dp
                                )
                                .clickable {
                                    onSettingsChange(
                                        settings.copy(
                                            mailApps =
                                                if (
                                                    picked
                                                ) {
                                                    settings.mailApps -
                                                            provider.packageName
                                                } else {
                                                    settings.mailApps +
                                                            provider.packageName
                                                }
                                        )
                                    )
                                }
                                .padding(
                                    vertical =
                                        9.dp
                                ),
                        verticalAlignment =
                            Alignment.CenterVertically,
                        horizontalArrangement =
                            Arrangement.spacedBy(
                                10.dp
                            )
                    ) {
                        DaylightCheckbox(
                            checked =
                                picked
                        )

                        Text(
                            text =
                                provider.label,
                            color =
                                PrimaryText,
                            fontSize =
                                10.5.sp,
                            lineHeight =
                                13.sp,
                            fontFamily =
                                InterfaceFont,
                            fontWeight =
                                FontWeight.SemiBold,
                            maxLines =
                                1,
                            overflow =
                                TextOverflow.Ellipsis,
                            modifier =
                                Modifier.weight(
                                    1f
                                )
                        )

                        if (
                            isMain
                        ) {
                            DaylightAccentTag(
                                text =
                                    "Main"
                            )
                        }
                    }

                    if (
                        index < providers.lastIndex
                    ) {
                        DaylightRowDivider()
                    }
                }
            }
        }

        DaylightOnboardingNote(
            text =
                "The first one you pick is your main app. Picking none uses every email app. Change it anytime in Settings."
        )
    }
}

/*
 * DEFAULT HOME
 *
 * No mockup of its own; drawn like D-Tasks: one service card with the
 * state on the right.
 */
@Composable
private fun HomeStep(
    homeReady: Boolean,
    onChooseHome: () -> Unit
) {
    Column(
        modifier =
            Modifier.fillMaxWidth(),
        verticalArrangement =
            Arrangement.spacedBy(
                10.dp
            )
    ) {

        DaylightOnboardingTitle(
            title =
                "Make Prompt Launcher home",
            subtitle =
                "Set Prompt Launcher as your Home app so the Home button always comes back here."
        )

        DaylightCard(
            horizontalPadding =
                11.5.dp,
            verticalPadding =
                11.5.dp
        ) {
            DaylightServiceHeader(
                icon =
                    DaylightTileIcon.HOME,
                tint =
                    AccentOrange,
                title =
                    "Default home app",
                detail =
                    if (
                        homeReady
                    ) {
                        "Prompt Launcher is your Home app"
                    } else {
                        "Android will ask which launcher to use"
                    }
            ) {
                if (
                    homeReady
                ) {
                    DaylightStatusPill(
                        label =
                            "Ready",
                        kind =
                            DaylightPillKind.READY
                    )
                } else {
                    DaylightStatusPill(
                        label =
                            "Set",
                        kind =
                            DaylightPillKind.ACCENT,
                        onClick =
                            onChooseHome
                    )
                }
            }
        }

        DaylightOnboardingNote(
            text =
                "You can switch back in Android's Settings → Apps → Default apps."
        )
    }
}

/*
 * Retained but no longer part of onboarding (see smsRoleAvailable in
 * PromptLauncherOnboarding). Drawn in the Daylight style so it is ready
 * if the step returns.
 */
@Composable
private fun MessagesSetupStep(
    smsReady: Boolean,
    smsRoleAvailable: Boolean,
    onOpenAppSettings: () -> Unit
) {

    Column(
        modifier =
            Modifier.fillMaxWidth(),
        verticalArrangement =
            Arrangement.spacedBy(
                10.dp
            )
    ) {

        DaylightOnboardingTitle(
            title =
                "Messages",
            subtitle =
                "Choose Prompt Launcher as your default SMS app."
        )

        DaylightCard {
            DaylightListRow(
                title =
                    "Default SMS app",
                detail =
                    when {
                        smsReady ->
                            "Messages are ready inside Prompt Launcher"

                        !smsRoleAvailable ->
                            "Unavailable on this device"

                        else ->
                            "Android will ask which app handles SMS"
                    }
            ) {
                when {
                    smsReady ->
                        DaylightStatusPill(
                            label =
                                "Ready",
                            kind =
                                DaylightPillKind.READY
                        )

                    !smsRoleAvailable ->
                        DaylightStatusPill(
                            label =
                                "Unavailable",
                            kind =
                                DaylightPillKind.NEUTRAL
                        )

                    else ->
                        DaylightStatusPill(
                            label =
                                "Not set",
                            kind =
                                DaylightPillKind.NEUTRAL
                        )
                }
            }
        }

        if (
            !smsReady &&
            smsRoleAvailable
        ) {
            DaylightOnboardingNote(
                text =
                    "Sideloaded: if SMS access is denied, open app settings, tap ⋮ → Allow restricted settings, then come back and choose Prompt Launcher again."
            )

            DaylightOnboardingLink(
                text =
                    "Open app settings →",
                onClick =
                    onOpenAppSettings
            )
        }
    }
}

/*
 * TASKS (D-Tasks)
 *
 * Two cards: Google Tasks with a connect pill, and Todoist with its
 * token field and Save.
 */
@Composable
private fun TasksSetupStep(
    provider: TaskProvider,
    googleTasksConnected: Boolean,
    googleTasksAuthorizing: Boolean,
    taskSetupMessage: String?,
    todoistToken: String,
    todoistSaved: Boolean,
    onTodoistTokenChange: (String) -> Unit,
    onConnectGoogleTasks: () -> Unit,
    onSaveTodoist: () -> Unit
) {
    Column(
        modifier =
            Modifier.fillMaxWidth(),
        verticalArrangement =
            Arrangement.spacedBy(
                10.dp
            )
    ) {

        DaylightOnboardingTitle(
            title =
                "Where should + go?",
            subtitle =
                "Type +buy milk and it lands in your to-do app. Connect one or both.",
            highlight =
                "+buy milk"
        )

        DaylightCard(
            horizontalPadding =
                11.5.dp,
            verticalPadding =
                11.5.dp
        ) {
            DaylightServiceHeader(
                icon =
                    DaylightTileIcon.CHECK_SQUARE,
                tint =
                    AccentOrange,
                title =
                    "Google Tasks",
                detail =
                    "Sign in with Google"
            ) {
                when {
                    googleTasksAuthorizing ->
                        DaylightStatusPill(
                            label =
                                "Connecting",
                            kind =
                                DaylightPillKind.NEUTRAL
                        )

                    googleTasksConnected &&
                            provider ==
                            TaskProvider.GOOGLE_TASKS ->
                        DaylightStatusPill(
                            label =
                                "Connected",
                            kind =
                                DaylightPillKind.READY
                        )

                    /*
                     * Connected earlier but Todoist is the current
                     * default: one tap makes Google Tasks the default
                     * again.
                     */
                    googleTasksConnected ->
                        DaylightStatusPill(
                            label =
                                "Use",
                            kind =
                                DaylightPillKind.NEUTRAL,
                            onClick =
                                onConnectGoogleTasks
                        )

                    else ->
                        DaylightStatusPill(
                            label =
                                "Connect",
                            kind =
                                DaylightPillKind.ACCENT,
                            onClick =
                                onConnectGoogleTasks
                        )
                }
            }
        }

        DaylightCard(
            horizontalPadding =
                11.5.dp,
            verticalPadding =
                11.5.dp
        ) {
            DaylightServiceHeader(
                icon =
                    DaylightTileIcon.LINES,
                tint =
                    ProductivityRed,
                title =
                    "Todoist",
                detail =
                    "Settings → Integrations → Developer"
            ) {
                if (
                    provider ==
                    TaskProvider.TODOIST ||
                    todoistSaved
                ) {
                    DaylightStatusPill(
                        label =
                            "Connected",
                        kind =
                            DaylightPillKind.READY
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(
                        8.dp
                    )
            )

            DaylightSecretField(
                value =
                    todoistToken,
                onValueChange =
                    onTodoistTokenChange,
                placeholder =
                    "Paste your API token",
                actionLabel =
                    "Save",
                onAction =
                    onSaveTodoist
            )
        }

        if (
            !taskSetupMessage.isNullOrBlank()
        ) {
            DaylightOnboardingNote(
                text =
                    taskSetupMessage,
                color =
                    ProductivityRed
            )
        }

        DaylightOnboardingNote(
            text =
                "Connected both? Chips above the prompt let you switch while you type."
        )
    }
}

/*
 * ASSISTANT
 *
 * No mockup of its own; a service card with the key field inside, like
 * the Todoist card.
 */
@Composable
private fun GeminiSetupStep(
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    saved: Boolean
) {
    Column(
        modifier =
            Modifier.fillMaxWidth(),
        verticalArrangement =
            Arrangement.spacedBy(
                10.dp
            )
    ) {

        DaylightOnboardingTitle(
            title =
                "Add an assistant",
            subtitle =
                "Optional. A Gemini API key answers anything you start with ?",
            highlight =
                "?"
        )

        DaylightCard(
            horizontalPadding =
                11.5.dp,
            verticalPadding =
                11.5.dp
        ) {
            DaylightServiceHeader(
                icon =
                    DaylightTileIcon.QUESTION,
                tint =
                    AccentOrange,
                title =
                    "Gemini",
                detail =
                    "Create a key in Google AI Studio"
            ) {
                if (
                    saved
                ) {
                    DaylightStatusPill(
                        label =
                            "Key saved",
                        kind =
                            DaylightPillKind.READY
                    )
                } else {
                    DaylightStatusPill(
                        label =
                            "Optional",
                        kind =
                            DaylightPillKind.NEUTRAL
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(
                        8.dp
                    )
            )

            DaylightSecretField(
                value =
                    apiKey,
                onValueChange =
                    onApiKeyChange,
                placeholder =
                    "Paste your API key"
            )
        }

        DaylightOnboardingNote(
            text =
                "Your question goes straight from this phone to Google Gemini with your key. " +
                        "Prompt Launcher has no server. Calendar, texts and mail are not sent unless you " +
                        "turn each one on later with the geminisetup command, which lists exactly what each sends."
        )
    }
}

/*
 * READY (D-Ready)
 *
 * A two-column grid of the command characters, then Start prompting.
 */
@Composable
private fun ReadyStep(
    geminiReady: Boolean,
    weatherPermissionReady: Boolean,
    weatherSnapshot: WeatherSnapshot?
) {

    val commands =
        listOf(
            Triple("@", "Text someone", false),
            Triple("/", "Call", false),
            Triple(":", "Add an event", false),
            Triple("+", "Add a to-do", false),
            Triple("\"", "Write a note", false),
            Triple("!", "Timer, !! alarm", false),
            Triple("?", "Ask the assistant", false),
            Triple("abc", "Search or open an app", true)
        )

    Column(
        modifier =
            Modifier.fillMaxWidth(),
        verticalArrangement =
            Arrangement.spacedBy(
                10.dp
            )
    ) {

        DaylightOnboardingTitle(
            title =
                "You're set.",
            subtitle =
                "Start typing anywhere on home. The first character picks the action."
        )

        Column(
            modifier =
                Modifier.fillMaxWidth(),
            verticalArrangement =
                Arrangement.spacedBy(
                    6.5.dp
                )
        ) {
            commands
                .chunked(
                    2
                )
                .forEach { pair ->

                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(
                                    IntrinsicSize.Min
                                ),
                        horizontalArrangement =
                            Arrangement.spacedBy(
                                6.5.dp
                            )
                    ) {
                        pair.forEach { (symbol, label, neutral) ->
                            DaylightCommandCard(
                                symbol =
                                    symbol,
                                label =
                                    label,
                                neutral =
                                    neutral,
                                modifier =
                                    Modifier
                                        .weight(
                                            1f
                                        )
                                        .fillMaxHeight()
                            )
                        }
                    }
                }
        }

        if (
            weatherSnapshot != null
        ) {
            DaylightOnboardingNote(
                text =
                    "Weather is ready."
            )
        } else if (
            weatherPermissionReady
        ) {
            DaylightOnboardingNote(
                text =
                    "Weather location is on. Prompt Launcher will keep trying."
            )
        }

        if (
            !geminiReady
        ) {
            DaylightOnboardingNote(
                text =
                    "Add a Gemini key anytime with the geminisetup command."
            )
        }

        DaylightOnboardingNote(
            text =
                "More commands are in the help card inside Prompt Launcher."
        )
    }
}

private fun promptLauncherIsHome(
    context: Context
): Boolean {
    if (
        Build.VERSION.SDK_INT >=
        Build.VERSION_CODES.Q
    ) {
        val roleManager =
            context.getSystemService(
                RoleManager::class.java
            )

        if (
            roleManager != null &&
            roleManager.isRoleAvailable(
                RoleManager.ROLE_HOME
            )
        ) {
            return roleManager.isRoleHeld(
                RoleManager.ROLE_HOME
            )
        }
    }

    val homeIntent =
        Intent(
            Intent.ACTION_MAIN
        ).apply {
            addCategory(
                Intent.CATEGORY_HOME
            )
        }

    val resolved =
        context.packageManager
            .resolveActivity(
                homeIntent,
                PackageManager.MATCH_DEFAULT_ONLY
            )

    return resolved
        ?.activityInfo
        ?.packageName ==
            context.packageName
}

private fun openHomeSettings(
    context: Context,
    launcher: (Intent) -> Unit
) {
    try {
        launcher(
            Intent(
                Settings.ACTION_HOME_SETTINGS
            )
        )
    } catch (
        _: Exception
    ) {
        context.startActivity(
            Intent(
                Settings.ACTION_SETTINGS
            )
        )
    }
}

private fun getPromptLauncherVersionName(
    context: Context
): String {
    return try {
        val packageInfo =
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU
            ) {
                context.packageManager
                    .getPackageInfo(
                        context.packageName,
                        PackageManager.PackageInfoFlags.of(
                            0
                        )
                    )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager
                    .getPackageInfo(
                        context.packageName,
                        0
                    )
            }

        packageInfo
            .versionName
            ?.takeIf {
                it.isNotBlank()
            }
            ?: "1.0"

    } catch (
        _: Exception
    ) {
        "1.0"
    }
}

/*
 * APPEARANCE
 *
 * The last choice before the launcher opens, so the first screen the
 * user sees is already theirs. Writes straight through to the
 * settings store — the same values the settings screens edit later —
 * and onboarding itself re-themes as each choice is made.
 */
@Composable
private fun AppearanceSetupStep(
    settings: LauncherSettings,
    onSettingsChange: (LauncherSettings) -> Unit
) {

    /*
     * Derived through the same mappers the launcher uses, so what is
     * shown here is literally what the home screen will render.
     */
    val previewAccent =
        accentColorFor(
            settings.accent
        )

    val previewPalette =
        paletteFor(
            effectiveThemeMode(
                settings,
                isSystemInDarkTheme()
            )
        )

    val previewFont =
        when (
            settings.interfaceFont
        ) {
            LauncherInterfaceFont.BRICOLAGE -> BricolageFamily
            LauncherInterfaceFont.MONO -> FontFamily.Monospace
            LauncherInterfaceFont.SANS -> FontFamily.SansSerif
            LauncherInterfaceFont.SERIF -> FontFamily.Serif
            LauncherInterfaceFont.POPPINS -> PoppinsFamily
        }

    val previewTextOverride =
        textColorOverrideFor(
            settings.textColor,
            previewAccent
        )

    val previewPrimary =
        previewTextOverride
            ?: previewPalette.primaryText

    val previewSecondary =
        previewTextOverride
            ?.copy(
                alpha = 0.62f
            )
            ?: previewPalette.secondaryText

    Column(
        modifier =
            Modifier.fillMaxWidth(),
        verticalArrangement =
            Arrangement.spacedBy(
                10.dp
            )
    ) {

        DaylightOnboardingTitle(
            title =
                "Make it yours",
            subtitle =
                "Pick a look. You can change any of this later in Settings."
        )

        /*
         * A miniature of the home screen header, so the choices are
         * judged against something recognisable rather than in the
         * abstract.
         */
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        previewPalette.surface,
                        RoundedCornerShape(14.dp)
                    )
                    .border(
                        0.75.dp,
                        previewPalette.border,
                        RoundedCornerShape(14.dp)
                    )
                    .padding(
                        horizontal = 12.dp,
                        vertical = 10.dp
                    )
        ) {

            Text(
                text = "Monday, Sep 28",
                color = previewPrimary,
                fontSize = 12.sp,
                lineHeight = 14.sp,
                fontFamily = previewFont,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "Cloudy, 84°",
                color = previewSecondary,
                fontSize = 9.5.sp,
                lineHeight = 12.sp,
                fontFamily = previewFont
            )

            Spacer(
                modifier = Modifier.height(7.dp)
            )

            Text(
                text = "@shan running late",
                color = previewAccent,
                fontSize = 10.5.sp,
                lineHeight = 13.sp,
                fontFamily = previewFont,
                fontWeight = FontWeight.Medium
            )
        }

        DaylightCard {

            OnboardingChoiceSection(
                label = "Accent",
                options =
                    LauncherAccent.entries.map {
                        onboardingOptionLabel(it.name)
                    },
                selectedIndex =
                    LauncherAccent.entries.indexOf(
                        settings.accent
                    ),
                onSelected = { index ->
                    onSettingsChange(
                        settings.copy(
                            accent =
                                LauncherAccent.entries[index]
                        )
                    )
                }
            )

            DaylightRowDivider()

            OnboardingChoiceSection(
                label = "Theme",
                options =
                    listOf("Auto") +
                            LauncherThemeMode.entries.map {
                                onboardingOptionLabel(it.name)
                            },
                selectedIndex =
                    if (
                        settings.followSystemTheme
                    ) {
                        0
                    } else {
                        LauncherThemeMode.entries.indexOf(
                            settings.themeMode
                        ) + 1
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
                                    LauncherThemeMode.entries[index - 1]
                            )
                        }
                    )
                }
            )

            DaylightRowDivider()

            OnboardingChoiceSection(
                label = "Font",
                options =
                    LauncherInterfaceFont.entries.map {
                        onboardingOptionLabel(it.name)
                    },
                selectedIndex =
                    LauncherInterfaceFont.entries.indexOf(
                        settings.interfaceFont
                    ),
                onSelected = { index ->
                    onSettingsChange(
                        settings.copy(
                            interfaceFont =
                                LauncherInterfaceFont.entries[index]
                        )
                    )
                }
            )

            DaylightRowDivider()

            OnboardingChoiceSection(
                label = "Text",
                options =
                    LauncherTextColor.entries.map {
                        onboardingOptionLabel(it.name)
                    },
                selectedIndex =
                    LauncherTextColor.entries.indexOf(
                        settings.textColor
                    ),
                onSelected = { index ->
                    onSettingsChange(
                        settings.copy(
                            textColor =
                                LauncherTextColor.entries[index]
                        )
                    )
                }
            )
        }
    }
}

/*
 * "COBALT" -> "Cobalt".
 */
private fun onboardingOptionLabel(
    name: String
): String =
    name
        .lowercase()
        .replaceFirstChar {
            it.uppercase()
        }

/*
 * A small spaced-capitals label over wrapping Daylight chips, because
 * eight accents will not fit on one line.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OnboardingChoiceSection(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit
) {

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 9.dp
                )
    ) {

        DaylightSectionLabel(
            text = label
        )

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        FlowRow(
            horizontalArrangement =
                Arrangement.spacedBy(6.dp),
            verticalArrangement =
                Arrangement.spacedBy(6.dp)
        ) {

            options.forEachIndexed { index, option ->

                DaylightChoiceChip(
                    label = option,
                    selected = index == selectedIndex,
                    onClick = {
                        onSelected(index)
                    }
                )
            }
        }
    }
}
