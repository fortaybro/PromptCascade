package com.forrest.titanlauncher

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.forrest.titanlauncher.ai.AssistantPayloadLimits
import com.forrest.titanlauncher.contacts.ContactRepository
import com.forrest.titanlauncher.notifications.NotificationCenter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/*
 * TEXTS FOR ASSIST
 *
 * What Assist reads when texts are shared: your recent conversations,
 * both sides, so it can answer "what did Sarah say", "did I reply to
 * Mom" or "when is dinner with Jake".
 *
 *  - Texts (SMS) come from Android's shared text store (needs the SMS
 *    permission quick reply asks for).
 *  - Chat (RCS) and other messaging apps can't be read that way, so
 *    their current notifications are added: whatever each one still
 *    shows.
 *
 * Capped at a handful of people and messages so the question stays
 * quick and only recent conversations leave the phone.
 */
internal fun buildRecentTextsContext(
    context: Context,
    contactRepository: ContactRepository,
    hasContactsPermission: Boolean
): String {

    val stamp =
        SimpleDateFormat(
            "EEE MMM d h:mm a",
            Locale.getDefault()
        )

    fun nameFor(
        number: String
    ): String =
        if (hasContactsPermission) {
            runCatching {
                contactRepository.findContactNameByPhoneNumber(number)
            }
                .getOrNull()
                ?: number
        } else {
            number
        }

    val lines =
        mutableListOf<String>()

    /*
     * SMS: newest first, grouped by person.
     */
    if (
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    ) {

        data class Row(
            val address: String,
            val body: String,
            val date: Long,
            val fromUser: Boolean
        )

        val rows =
            mutableListOf<Row>()

        runCatching {
            context.contentResolver
                .query(
                    Telephony.Sms.CONTENT_URI,
                    arrayOf(
                        Telephony.Sms.ADDRESS,
                        Telephony.Sms.BODY,
                        Telephony.Sms.DATE,
                        Telephony.Sms.TYPE
                    ),
                    null,
                    null,
                    Telephony.Sms.DATE + " DESC"
                )
                ?.use { cursor ->

                    val addressIndex = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
                    val bodyIndex = cursor.getColumnIndex(Telephony.Sms.BODY)
                    val dateIndex = cursor.getColumnIndex(Telephony.Sms.DATE)
                    val typeIndex = cursor.getColumnIndex(Telephony.Sms.TYPE)

                    if (
                        addressIndex < 0 ||
                        bodyIndex < 0 ||
                        dateIndex < 0 ||
                        typeIndex < 0
                    ) {
                        return@use
                    }

                    while (
                        cursor.moveToNext() &&
                        rows.size < AssistantPayloadLimits.TEXT_SCAN_ROWS
                    ) {
                        val address = cursor.getString(addressIndex).orEmpty()
                        val body = cursor.getString(bodyIndex).orEmpty()

                        if (address.isBlank() || body.isBlank()) continue

                        rows +=
                            Row(
                                address = address,
                                body = body,
                                date = cursor.getLong(dateIndex),
                                fromUser =
                                    cursor.getInt(typeIndex) !=
                                            Telephony.Sms.MESSAGE_TYPE_INBOX
                            )
                    }
                }
        }

        rows
            .groupBy {
                normalizePhoneNumber(it.address)
            }
            .values
            .sortedByDescending { thread ->
                thread.maxOf { it.date }
            }
            .take(
                AssistantPayloadLimits.TEXT_THREADS
            )
            .forEach { thread ->

                val name =
                    nameFor(thread.first().address)

                val unanswered =
                    thread.maxByOrNull { it.date }?.fromUser == false

                lines +=
                    "Conversation with $name" +
                            if (unanswered) " (their message is the latest; you haven't replied):" else ":"

                thread
                    .sortedBy { it.date }
                    .takeLast(
                        AssistantPayloadLimits.TEXT_MESSAGES_PER_THREAD
                    )
                    .forEach { message ->
                        lines +=
                            "  [${stamp.format(Date(message.date))}] " +
                                    (if (message.fromUser) "You" else name) +
                                    ": " +
                                    message.body
                                        .replace("\\s+".toRegex(), " ")
                                        .take(AssistantPayloadLimits.LATEST_TEXT_CHARS)
                    }
            }
    }

    /*
     * Chats still showing as notifications (RCS, WhatsApp, Signal…).
     */
    NotificationCenter
        .notifications
        .value
        .filter {
            it.messages.isNotEmpty()
        }
        .sortedByDescending {
            it.timestamp
        }
        .take(
            AssistantPayloadLimits.TEXT_THREADS
        )
        .forEach { notification ->

            val name =
                notification.title.ifBlank { notification.appName }

            lines +=
                "Unread chat in ${notification.appName} with $name:"

            notification.messages
                .takeLast(
                    AssistantPayloadLimits.TEXT_MESSAGES_PER_THREAD
                )
                .forEach { message ->
                    lines +=
                        "  [${stamp.format(Date(message.timestamp))}] " +
                                (if (message.fromUser) "You" else message.sender.ifBlank { name }) +
                                ": " +
                                message.text
                                    .replace("\\s+".toRegex(), " ")
                                    .take(AssistantPayloadLimits.LATEST_TEXT_CHARS)
                }
        }

    return if (lines.isEmpty()) {
        "No recent texts could be read (the SMS permission may be off)."
    } else {
        lines.joinToString("\n")
    }
}

/*
 * Email notifications still showing from mail apps: sender, app and
 * the subject/preview line.
 */
internal fun buildMailNotificationsContext(
    context: Context
): String {

    val mailPackages =
        runCatching {
            com.forrest.titanlauncher.mail.allMailPackages(context)
        }
            .getOrDefault(
                emptySet()
            )

    val stamp =
        SimpleDateFormat(
            "EEE MMM d h:mm a",
            Locale.getDefault()
        )

    return NotificationCenter
        .notifications
        .value
        .filter {
            it.packageName in mailPackages &&
                    it.messages.isEmpty()
        }
        .sortedByDescending {
            it.timestamp
        }
        .take(
            AssistantPayloadLimits.CONTEXT_EMAILS
        )
        .joinToString("\n") { notification ->
            "- [${stamp.format(Date(notification.timestamp))}] ${notification.appName}: from ${notification.title}: " +
                    notification.text
                        .replace("\\s+".toRegex(), " ")
                        .take(AssistantPayloadLimits.CONTEXT_EMAIL_PREVIEW_CHARS)
        }
}
