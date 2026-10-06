package com.forrest.titanlauncher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log

/*
 * THE TITAN'S PTT KEY
 *
 * With Settings > Shortcut keys > Func1 key set to "PTT key" and the
 * PTT app set to Daylight, the Titan doesn't open the app: it sends
 * Daylight a broadcast when the key goes down
 * (android.intent.action.PTT.down) and another when it comes up
 * (android.intent.action.PTT.up).
 *
 * Key down opens the Daylight prompt over whatever app is in front
 * (PromptAnywhereActivity); pressing it again while the prompt is open
 * closes it (PromptAnywhereActivity.onNewIntent). Key up is ignored.
 *
 * Starting a screen from a broadcast is normally blocked while another
 * app is in front, but Android always lets the default Home app do it,
 * which Daylight is.
 */
class PttKeyReceiver :
    BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        if (
            intent.action != ACTION_PTT_DOWN
        ) {
            return
        }

        /*
         * Some keys repeat the down broadcast while held.
         */
        val now =
            SystemClock.elapsedRealtime()

        if (
            now - lastDown < 400
        ) {
            return
        }

        lastDown = now

        runCatching {
            context.startActivity(
                Intent(
                    context,
                    PromptAnywhereActivity::class.java
                )
                    .addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                    )
            )
        }
            .onFailure {
                Log.w(
                    "DaylightPtt",
                    "Could not open the prompt: ${it.message}"
                )
            }
    }

    companion object {

        const val ACTION_PTT_DOWN =
            "android.intent.action.PTT.down"

        const val ACTION_PTT_UP =
            "android.intent.action.PTT.up"

        @Volatile
        private var lastDown = 0L
    }
}