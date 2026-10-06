package com.forrest.titanlauncher.ai

import android.content.Context
import com.forrest.titanlauncher.mail.MailMessage

/*
 * What the AI assistant is allowed to send to Google Gemini.
 *
 * Every source is off until the user turns it on in geminisetup, and
 * the setup screen lists exactly what each switch sends. With every
 * switch off, Gemini receives only the question the user typed.
 */
data class AssistantSharing(
    val calendar: Boolean = false,
    val texts: Boolean = false,
    val mail: Boolean = false
)

/*
 * Caps on how much each source contributes, kept small on purpose:
 * most questions only need the last few items.
 */
object AssistantPayloadLimits {

    const val CONTEXT_EMAILS = 15
    const val CONTEXT_EMAIL_PREVIEW_CHARS = 200

    const val PRIORITIZE_EMAILS = 15
    const val PRIORITIZE_EMAIL_PREVIEW_CHARS = 250

    const val LATEST_TEXT_CHARS = 160

    const val TEXT_THREADS = 8
    const val TEXT_MESSAGES_PER_THREAD = 6
    const val TEXT_SCAN_ROWS = 300

    const val AGENDA_DAYS = 14
}

/*
 * Plain-language description of each source, shown on the setup
 * screen. Keep this in sync with buildAssistantLauncherContext and
 * GeminiRepository.prioritizeEmails.
 */
object AssistantSharingDisclosure {

    const val ALWAYS =
        "Always sent: the question you type. Nothing is sent until you ask something."

    const val CALENDAR =
        "Your next event (time, title, location) and the next ${AssistantPayloadLimits.AGENDA_DAYS} days of events."

    const val TEXTS =
        "Your unread text count and your ${AssistantPayloadLimits.TEXT_THREADS} most recent conversations, both sides (the last ${AssistantPayloadLimits.TEXT_MESSAGES_PER_THREAD} messages each, first ${AssistantPayloadLimits.LATEST_TEXT_CHARS} characters), plus chats still showing as notifications."

    const val MAIL =
        "Your important-unread count, your ${AssistantPayloadLimits.CONTEXT_EMAILS} newest emails (sender, subject, first ${AssistantPayloadLimits.CONTEXT_EMAIL_PREVIEW_CHARS} characters) and any email notifications on screen. " +
                "Asking which emails need replies sends up to ${AssistantPayloadLimits.PRIORITIZE_EMAILS} emails (first ${AssistantPayloadLimits.PRIORITIZE_EMAIL_PREVIEW_CHARS} characters each)."

    const val DESTINATION =
        "It goes straight from this phone to Google Gemini using your own API key. Prompt Launcher has no server and never sees it."
}

class AssistantSharingStore(
    context: Context
) {

    private val prefs =
        context.applicationContext.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

    fun load(): AssistantSharing {
        return AssistantSharing(
            calendar =
                prefs.getBoolean(
                    KEY_CALENDAR,
                    false
                ),
            texts =
                prefs.getBoolean(
                    KEY_TEXTS,
                    false
                ),
            mail =
                prefs.getBoolean(
                    KEY_MAIL,
                    false
                )
        )
    }

    fun save(
        sharing: AssistantSharing
    ) {
        prefs.edit()
            .putBoolean(
                KEY_CALENDAR,
                sharing.calendar
            )
            .putBoolean(
                KEY_TEXTS,
                sharing.texts
            )
            .putBoolean(
                KEY_MAIL,
                sharing.mail
            )
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "assistant_sharing"
        const val KEY_CALENDAR = "share_calendar"
        const val KEY_TEXTS = "share_texts"
        const val KEY_MAIL = "share_mail"
    }
}

/*
 * Builds the context block sent alongside a question. Only sources the
 * user switched on are included; lambdas keep switched-off sources
 * from even being read.
 */
fun buildAssistantLauncherContext(
    sharing: AssistantSharing,
    unreadTextCount: Int,
    importantUnreadMailCount: Int,
    nextEventLine: () -> String,
    nextEventLocation: () -> String?,
    calendarAgenda: () -> String,
    latestIncomingText: () -> Pair<String, String>?,
    mailMessages: List<MailMessage>,
    /*
     * Recent conversations, both sides. Empty falls back to just the
     * latest incoming text.
     */
    recentTexts: () -> String = { "" },
    /*
     * Email notifications from mail apps (Spark, Outlook, Gmail),
     * for mail that isn't in the Gmail list.
     */
    mailNotifications: () -> String = { "" }
): String {

    val switchedOff =
        listOfNotNull(
            "calendar".takeIf { !sharing.calendar },
            "texts".takeIf { !sharing.texts },
            "mail".takeIf { !sharing.mail }
        )

    /*
     * Tells the model how to answer when a question needs a source
     * that isn't shared, instead of a vague "I don't have access".
     */
    val offNote =
        if (switchedOff.isEmpty()) {
            ""
        } else {
            "Not shared: ${switchedOff.joinToString(", ")}. If the question needs one of these, say it's switched off and that it can be turned on by typing geminisetup in the prompt."
        }

    if (
        !sharing.calendar &&
        !sharing.texts &&
        !sharing.mail
    ) {
        return "No personal context shared. $offNote"
    }

    return buildString {

        if (
            sharing.calendar
        ) {
            appendLine(
                "Next calendar event: ${nextEventLine()}"
            )

            appendLine(
                "Next calendar event location: ${
                    nextEventLocation()
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: "none saved"
                }"
            )

            appendLine(
                "Calendar agenda for the next ${AssistantPayloadLimits.AGENDA_DAYS} days:"
            )

            appendLine(
                calendarAgenda()
            )
        }

        if (
            sharing.texts
        ) {
            appendLine(
                "Unread texts: $unreadTextCount"
            )

            val conversations =
                recentTexts()

            if (
                conversations.isNotBlank()
            ) {
                appendLine(
                    "Recent text conversations (newest first):"
                )

                appendLine(
                    conversations
                )
            } else {
                latestIncomingText()
                    ?.let { (sender, body) ->
                        appendLine(
                            "Latest incoming text from $sender: ${body.take(AssistantPayloadLimits.LATEST_TEXT_CHARS)}"
                        )
                    }
            }
        }

        if (
            sharing.mail
        ) {
            appendLine(
                "Important unread email: $importantUnreadMailCount"
            )

            appendLine(
                "Recent inbox email:"
            )

            mailMessages
                .sortedByDescending {
                    it.timestamp
                }
                .take(
                    AssistantPayloadLimits.CONTEXT_EMAILS
                )
                .forEachIndexed { index, message ->

                    val preview =
                        message.body
                            .ifBlank {
                                message.snippet
                            }
                            .replace(
                                "\\s+".toRegex(),
                                " "
                            )
                            .trim()
                            .take(
                                AssistantPayloadLimits.CONTEXT_EMAIL_PREVIEW_CHARS
                            )

                    val received =
                        java.text.SimpleDateFormat(
                            "EEE MMM d h:mm a",
                            java.util.Locale.getDefault()
                        )
                            .format(
                                java.util.Date(message.timestamp)
                            )

                    appendLine(
                        "${index + 1}. Received $received from ${message.sender} <${message.senderEmail}>; subject ${message.subject}; unread ${message.unread}; important ${message.important}; preview $preview"
                    )
                }
        }

        if (
            sharing.mail
        ) {
            val notified =
                mailNotifications()

            if (
                notified.isNotBlank()
            ) {
                appendLine(
                    "Email notifications on the phone:"
                )

                appendLine(
                    notified
                )
            }
        }

        if (
            offNote.isNotBlank()
        ) {
            appendLine(
                offNote
            )
        }
    }
}
