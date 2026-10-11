package com.forrest.titanlauncher

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import android.telephony.PhoneNumberUtils
import androidx.core.content.ContextCompat
import com.forrest.titanlauncher.contacts.ContactRepository
import com.forrest.titanlauncher.notifications.NotificationMessage
import kotlin.math.abs

/*
 * THREAD HISTORY FOR QUICK REPLY
 *
 * A messaging app's notification usually carries only the texts you
 * haven't read yet, so quick reply showed the other person's side and
 * none of your own replies. Texts (SMS) are also kept in Android's
 * shared message store, so for a one-to-one chat Daylight reads the
 * recent exchange from there, both directions, and merges it with
 * what the notification carried.
 *
 * Needs the SMS permission. Chat (RCS) messages are kept privately by
 * the messaging app, so they can't be read this way and only what the
 * notification carries is shown for them.
 */

private val TextingAppPackages =
    setOf(
        "com.google.android.apps.messaging",
        "com.android.messaging",
        "com.samsung.android.messaging",
        "com.textra",
        "com.moez.QKSMS",
        "xyz.klinker.messenger",
        "org.fossify.messages",
        "com.simplemobiletools.smsmessenger"
    )

private const val HistoryLimit = 40

private const val ScanLimit = 400

internal fun loadSmsThreadHistory(
    context: Context,
    entry: QuickReplyEntry
): List<NotificationMessage> {

    /*
     * Texts from Daylight's own message list already know the number;
     * notifications only count when they come from a texting app.
     */
    if (
        !entry.isMessage
    ) {

        val source =
            entry.sourcePackage
                ?: return emptyList()

        val defaultSms =
            runCatching {
                Telephony.Sms.getDefaultSmsPackage(
                    context
                )
            }
                .getOrNull()

        if (
            source !in TextingAppPackages &&
            source != defaultSms
        ) {
            return emptyList()
        }
    }

    if (
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        return emptyList()
    }

    val nameFor: (String) -> String? = { number ->
        runCatching {
            ContactRepository(context)
                .findContactNameByPhoneNumber(number)
        }
            .getOrNull()
    }

    val numbers =
        if (entry.isGroup) {
            emptyList()
        } else {
            entry.phoneNumber
                ?.takeIf { it.isNotBlank() }
                ?.let { listOf(it) }
                ?: numbersForTitle(
                    context,
                    entry.title
                )
        }

    /*
     * The real conversation: Android's thread for these people, read
     * whole (texts AND MMS, both sides). Google Messages also keeps
     * chat (RCS) messages there as MMS, so this is the up-to-date
     * thread rather than only what the notification carried.
     */
    val threadId =
        runCatching {
            if (entry.isGroup) {
                findThreadId(context, entry.participants, nameFor)
                    ?: matchGroupForNotification(
                        loadGroupConversations(context, nameFor),
                        entry.title,
                        entry.messages
                            .filter { !it.fromUser }
                            .map { it.sender }
                            .distinct()
                    )
                        ?.target
                        ?.threadId
            } else {
                numbers.firstNotNullOfOrNull { number ->
                    findThreadId(context, listOf(number), nameFor)
                }
            }
        }
            .getOrNull()

    if (
        threadId != null
    ) {
        val thread =
            runCatching {
                loadThreadById(
                    context,
                    threadId,
                    entry.title,
                    nameFor
                )
            }
                .getOrDefault(emptyList())

        if (
            thread.isNotEmpty()
        ) {
            /*
             * One person: their messages carry the conversation's
             * name, so quick reply doesn't label every bubble.
             */
            return thread
                .map {
                    if (!entry.isGroup && !it.fromUser) {
                        it.copy(sender = entry.title)
                    } else {
                        it
                    }
                }
                .takeLast(HistoryLimit)
        }
    }

    if (
        numbers.isEmpty()
    ) {
        return emptyList()
    }

    val found =
        mutableListOf<NotificationMessage>()

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

                val addressIndex =
                    cursor.getColumnIndex(Telephony.Sms.ADDRESS)

                val bodyIndex =
                    cursor.getColumnIndex(Telephony.Sms.BODY)

                val dateIndex =
                    cursor.getColumnIndex(Telephony.Sms.DATE)

                val typeIndex =
                    cursor.getColumnIndex(Telephony.Sms.TYPE)

                if (
                    addressIndex < 0 ||
                    bodyIndex < 0 ||
                    dateIndex < 0 ||
                    typeIndex < 0
                ) {
                    return@use
                }

                var scanned = 0

                while (
                    cursor.moveToNext() &&
                    scanned < ScanLimit &&
                    found.size < HistoryLimit
                ) {

                    scanned++

                    val address =
                        cursor.getString(addressIndex).orEmpty()

                    if (
                        numbers.none { number ->
                            PhoneNumberUtils.compare(
                                address,
                                number
                            )
                        }
                    ) {
                        continue
                    }

                    val body =
                        cursor.getString(bodyIndex).orEmpty()

                    if (
                        body.isBlank()
                    ) {
                        continue
                    }

                    /*
                     * Inbox is theirs; sent, outbox, queued and
                     * failed are yours.
                     */
                    val fromUser =
                        cursor.getInt(typeIndex) !=
                                Telephony.Sms.MESSAGE_TYPE_INBOX

                    found +=
                        NotificationMessage(
                            sender =
                                if (fromUser) "" else entry.title,
                            text =
                                body,
                            timestamp =
                                cursor.getLong(dateIndex),
                            fromUser =
                                fromUser
                        )
                }
            }
    }

    return found
        .sortedBy {
            it.timestamp
        }
}

/*
 * The phone numbers behind a notification title: the contact with
 * exactly that name, or the title itself when it is a number (an
 * unsaved contact).
 */
internal fun numbersForTitle(
    context: Context,
    title: String
): List<String> {

    val trimmed =
        title.trim()

    if (
        trimmed.isBlank()
    ) {
        return emptyList()
    }

    val digits =
        trimmed.count {
            it.isDigit()
        }

    if (
        digits >= 7 &&
        trimmed.all {
            it.isDigit() || it in "+-() ."
        }
    ) {
        return listOf(trimmed)
    }

    return runCatching {
        ContactRepository(
            context
        )
            .findContacts(
                searchName = trimmed,
                limit = 8
            )
            .filter {
                it.name.trim().equals(
                    trimmed,
                    ignoreCase = true
                )
            }
            .map {
                it.phoneNumber
            }
            .filter {
                it.isNotBlank()
            }
            .distinct()
    }
        .getOrDefault(
            emptyList()
        )
}

/*
 * A picture with no words arrives as "📷 Photo" from the text store
 * but "Image", "Photo" or "Sent a photo" (and a different timestamp)
 * from the messaging app's notification. Those count as the same
 * message, so a picture shows once, not twice.
 */
private fun isPictureOnly(
    message: NotificationMessage
): Boolean {

    val text =
        message.text.trim().lowercase()

    return message.imageUris.isNotEmpty() && text.isBlank() ||
            text.startsWith("📷") ||
            text in setOf("image", "photo", "picture", "sent an image", "sent a photo", "sent a picture")
}

private fun sameMessage(
    a: NotificationMessage,
    b: NotificationMessage
): Boolean {

    if (a.fromUser != b.fromUser) return false

    if (abs(a.timestamp - b.timestamp) >= 3 * 60 * 1000L) return false

    return a.text.trim() == b.text.trim() ||
            (isPictureOnly(a) && isPictureOnly(b))
}

/*
 * The notification's messages plus the stored history, without the
 * same text showing twice. When both have a message, the
 * notification's copy is kept, since it carries the sender's name.
 */
internal fun mergeThreadHistory(
    fromNotification: List<NotificationMessage>,
    history: List<NotificationMessage>
): List<NotificationMessage> {

    if (
        history.isEmpty()
    ) {
        return fromNotification
    }

    val extra =
        history.filter { stored ->
            fromNotification.none { shown ->
                sameMessage(shown, stored)
            }
        }

    /*
     * The same message in both lists: keep the notification's copy,
     * but take the stored copy's pictures when it has them (an MMS
     * image Prompt can open beats a notification image it may not be
     * allowed to read).
     */
    val shownWithPictures =
        fromNotification.map { shown ->

            val stored =
                history.firstOrNull { stored ->
                    stored.imageUris.isNotEmpty() &&
                            sameMessage(shown, stored)
                }

            if (stored != null) {
                shown.copy(
                    imageUris =
                        (stored.imageUris + shown.imageUris).distinct()
                )
            } else {
                shown
            }
        }

    return (shownWithPictures + extra)
        .sortedBy {
            it.timestamp
        }
}

/*
 * DAYLIGHT'S OWN THREAD LOG
 *
 * Chat (RCS) messages never reach Android's shared text store, so for
 * those Daylight keeps its own short record per conversation: the
 * messages each notification carried, and every reply you send from
 * quick reply. Kept on the phone only, about the last 40 per person.
 */
internal object DaylightThreadLog {

    private const val PrefsName =
        "daylight_thread_log"

    private const val KeepPerThread =
        40

    private fun threadId(
        entry: QuickReplyEntry
    ): String =
        (entry.sourcePackage ?: "texts") + "|" +
                (entry.phoneNumber ?: entry.title).trim().lowercase()

    fun load(
        context: Context,
        entry: QuickReplyEntry
    ): List<NotificationMessage> {

        val raw =
            context
                .getSharedPreferences(PrefsName, Context.MODE_PRIVATE)
                .getString(threadId(entry), null)
                ?: return emptyList()

        return runCatching {

            val array =
                org.json.JSONArray(raw)

            (0 until array.length()).map { index ->

                val item =
                    array.getJSONObject(index)

                NotificationMessage(
                    sender = item.optString("s"),
                    text = item.optString("t"),
                    timestamp = item.optLong("d"),
                    fromUser = item.optBoolean("u")
                )
            }
        }
            .getOrDefault(
                emptyList()
            )
    }

    @Synchronized
    fun record(
        context: Context,
        entry: QuickReplyEntry,
        messages: List<NotificationMessage>
    ) {

        if (
            entry.isGroup ||
            messages.isEmpty()
        ) {
            return
        }

        val kept =
            mergeThreadHistory(
                load(context, entry),
                messages.filter { it.text.isNotBlank() }
            )
                .takeLast(
                    KeepPerThread
                )

        val array =
            org.json.JSONArray()

        kept.forEach { message ->
            array.put(
                org.json.JSONObject()
                    .put("s", message.sender)
                    .put("t", message.text)
                    .put("d", message.timestamp)
                    .put("u", message.fromUser)
            )
        }

        context
            .getSharedPreferences(PrefsName, Context.MODE_PRIVATE)
            .edit()
            .putString(threadId(entry), array.toString())
            .apply()
    }

    fun recordReply(
        context: Context,
        entry: QuickReplyEntry,
        text: String
    ) {
        record(
            context,
            entry,
            listOf(
                NotificationMessage(
                    sender = "",
                    text = text,
                    timestamp = System.currentTimeMillis(),
                    fromUser = true
                )
            )
        )
    }
}

/*
 * Whether a thread is a one-to-one text conversation, where the text
 * store can fill in your replies (and so asking for the SMS
 * permission makes sense).
 */
internal fun isTextThread(
    context: Context,
    entry: QuickReplyEntry
): Boolean {

    if (entry.isGroup) return false
    if (entry.isMessage) return true

    val source =
        entry.sourcePackage
            ?: return false

    val defaultSms =
        runCatching {
            Telephony.Sms.getDefaultSmsPackage(context)
        }
            .getOrNull()

    return source in TextingAppPackages || source == defaultSms
}
