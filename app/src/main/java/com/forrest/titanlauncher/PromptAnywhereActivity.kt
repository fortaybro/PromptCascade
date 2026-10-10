package com.forrest.titanlauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowInsetsCompat
import com.forrest.titanlauncher.ui.theme.TitanLauncherTheme
import kotlinx.coroutines.awaitCancellation

/*
 * "Prompt over other apps".
 *
 * Registered for android.intent.action.ASSIST, so once Daylight is
 * picked under Settings > Apps > Default apps > Digital assistant app,
 * the assist gesture (hold Home, or the assist key) opens the Daylight
 * prompt on top of whatever app is in front. The window is translucent:
 * the other app stays visible behind a light scrim.
 *
 * Enter runs the command and, once the "✓ ..." confirmation has shown,
 * this activity finishes and the user is back in their app. Back/Esc,
 * tapping the scrim, or switching away also close it. Commands that
 * need a full launcher screen hand off to MainActivity (see the
 * assistOverlay branch of TitanApp).
 *
 * Also opened by Daylight's app entry (DaylightEntryActivity),
 * which the Titan's PTT key launches from any app.
 *
 * Deliberately no onboarding redirect: this never shows Home, and the
 * user can only reach it after choosing Daylight as the assistant.
 */
@OptIn(ExperimentalComposeUiApi::class)
class PromptAnywhereActivity :
    ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        /*
         * Draw under the (transparent) system bars so the scrim covers
         * the whole screen; TitanApp pads the prompt itself with
         * systemBarsPadding.
         */
        WindowCompat.setDecorFitsSystemWindows(
            window,
            false
        )

        /*
         * Fade in over the app (instead of sliding up), and hide the
         * status and navigation bars like home does, so the blurred
         * app fills the screen with no black strip at the top.
         */
        fadeTransitions()

        WindowCompat.getInsetsController(
            window,
            window.decorView
        ).apply {
            hide(
                WindowInsetsCompat.Type.statusBars() or
                        WindowInsetsCompat.Type.navigationBars()
            )
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        /*
         * Mirrors MainActivity.onCreate exactly, so the prompt has the
         * same scale, readability density and text handling.
         */
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
                                    homeRequestVersion = 0,
                                    assistOverlay = true
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    /*
     * Pressing the shortcut key again while the prompt is open closes
     * it, so the same key toggles the prompt on and off. (Android
     * pauses an activity before handing it a new intent, so "open" is
     * STARTED rather than RESUMED.)
     */
    override fun onNewIntent(
        intent: android.content.Intent
    ) {

        super.onNewIntent(
            intent
        )

        if (
            lifecycle.currentState.isAtLeast(
                androidx.lifecycle.Lifecycle.State.STARTED
            )
        ) {
            finish()
        }
    }

    @Suppress("DEPRECATION")
    private fun fadeTransitions() {

        if (
            android.os.Build.VERSION.SDK_INT >= 34
        ) {
            overrideActivityTransition(
                OVERRIDE_TRANSITION_OPEN,
                R.anim.daylight_fade_in,
                R.anim.daylight_stay
            )
            overrideActivityTransition(
                OVERRIDE_TRANSITION_CLOSE,
                R.anim.daylight_stay,
                R.anim.daylight_fade_out
            )
        } else {
            overridePendingTransition(
                R.anim.daylight_fade_in,
                R.anim.daylight_stay
            )
        }
    }

    override fun finish() {

        super.finish()

        if (
            android.os.Build.VERSION.SDK_INT < 34
        ) {
            @Suppress("DEPRECATION")
            overridePendingTransition(
                R.anim.daylight_stay,
                R.anim.daylight_fade_out
            )
        }
    }

    override fun onStop() {

        super.onStop()

        /*
         * The user switched away (or a command opened another app):
         * the overlay is single-use, so don't leave it waiting in the
         * background. onStop rather than onPause, so translucent
         * permission dialogs raised by a command don't kill it.
         */
        if (
            !isChangingConfigurations &&
            !isFinishing
        ) {
            finish()
        }
    }
}