package com.forrest.titanlauncher

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.awaitCancellation

/*
 * ON-SCREEN KEYBOARD
 *
 * Prompt Launcher was built for the Titan's physical keyboard, so it
 * keeps Android's keyboard app (Gboard, SwiftKey, ...) from opening:
 * the keys go straight to the prompt and nothing covers the screen.
 *
 * That also shuts out what only a keyboard app can do: an on-screen
 * keyboard, voice typing, and languages typed through an input
 * method (Chinese, Japanese, Korean, ...). This switch lets the
 * keyboard app back in. Off by default on phones with a physical
 * keyboard, on for phones without one.
 */
internal object KeyboardPrefs {

    private const val PrefsName = "prompt_keyboard"
    private const val KeyUseInputMethod = "use_input_method"

    fun useInputMethod(
        context: Context
    ): Boolean {

        val prefs =
            context.getSharedPreferences(PrefsName, Context.MODE_PRIVATE)

        if (prefs.contains(KeyUseInputMethod)) {
            return prefs.getBoolean(KeyUseInputMethod, false)
        }

        return context.resources.configuration.keyboard ==
                Configuration.KEYBOARD_NOKEYS
    }

    fun setUseInputMethod(
        context: Context,
        use: Boolean
    ) {
        context
            .getSharedPreferences(PrefsName, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KeyUseInputMethod, use)
            .commit()
    }
}

internal tailrec fun Context.findHostActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findHostActivity()
        else -> null
    }

/*
 * The switch as live state, so flipping it in Settings takes effect
 * without restarting the screen (which used to drop you on home).
 * Null until first read from the saved setting.
 */
internal var KeyboardAppAllowed by
mutableStateOf<Boolean?>(
    null
)

/*
 * Wraps the whole app. The layout never changes with the switch:
 * every time a text field asks Android for the keyboard app, this
 * checks the switch and either lets the request through or quietly
 * holds it, so the physical keys keep going straight to the field.
 * A field that already has the keyboard open picks up the change
 * the next time it's tapped.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun PromptTextInput(
    content: @Composable () -> Unit
) {

    val context =
        LocalContext.current

    InterceptPlatformTextInput(
        interceptor = { request, nextHandler ->

            val allowed =
                KeyboardAppAllowed
                    ?: KeyboardPrefs.useInputMethod(context).also {
                        KeyboardAppAllowed = it
                    }

            if (allowed) {
                nextHandler.startInputMethod(request)
            } else {
                awaitCancellation()
            }
        }
    ) {
        content()
    }
}
