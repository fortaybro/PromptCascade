package com.forrest.titanlauncher

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle

/*
 * DAYLIGHT'S APP ENTRY
 *
 * What runs when something opens "Daylight" as an app rather than as
 * Home: another launcher's icon, Android Studio's Run, or the Titan's
 * PTT key (Settings > Shortcut keys > Func1 > PTT key, PTT app:
 * Daylight). The Titan picks apps by package, so this one entry has to
 * do the useful thing:
 *
 *  - setup not finished: onboarding
 *  - otherwise: the Daylight prompt over whatever app is open
 *    (PromptAnywhereActivity). Pressing the key again closes it.
 *
 * Home itself still comes from the Home button (MainActivity is the
 * HOME activity). Invisible (Theme.NoDisplay) and in its own empty
 * task, so nothing flashes and the Daylight home task isn't brought
 * over the app you're in.
 */
class DaylightEntryActivity :
    Activity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        val onboardingComplete =
            getSharedPreferences(
                "prompt_launcher_onboarding",
                Context.MODE_PRIVATE
            )
                .getBoolean(
                    "onboarding_complete",
                    false
                )

        val next =
            if (onboardingComplete) {
                Intent(
                    this,
                    PromptAnywhereActivity::class.java
                )
            } else {
                Intent(
                    this,
                    OnboardingActivity::class.java
                )
            }

        runCatching {
            startActivity(
                next.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                )
            )
        }

        finish()
    }
}