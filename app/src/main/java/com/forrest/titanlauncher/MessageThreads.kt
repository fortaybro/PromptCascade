package com.forrest.titanlauncher

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import com.forrest.titanlauncher.notifications.NotificationMessage

/*
 * ANDROID'S MESSAGE THREADS (texts, MMS and groups)
 *
 * Used by quick reply to read a whole conversation, both sides
 * (DaylightThreadHistory.kt).
 *
 * Group texts and picture messages are MMS, stored by Android next to
 * SMS but in a different shape:
 *
 *  - threads: content://mms-sms/conversations?simple=true lists every
 *    conversation with its recipient ids, newest first
 *  - recipients: content://mms-sms/canonical-addresses maps those ids
 *    to phone numbers
 *  - a message: content://mms (msg_box 1 = received, 2 = sent; date in
 *    seconds), its sender in content://mms/<id>/addr (type 137 = from),
 *    and its text in content://mms/part (ct text/plain)
 *
 * Needs READ_SMS, the same permission as texts. Group chats (RCS) are
 * kept privately by Messages and only show what its notifications
 * carry.
 */

/*
 * Who a conversation is with. phoneNumber for one person, participants
 * for a group; notificationKey when Messages is showing it as a
 * notification right now.
 */
internal data class DaylightThreadTarget(
    val title: String,
    val phoneNumber: String? = null,
    val participants: List<String> = emptyList(),
    val isGroup: Boolean = false,
    val notificationKey: String? = null,
    /*
     * Android's own thread id, when known: reads the whole thread
     * (texts and MMS) rather than matching by number.
     */
    val threadId: Long? = null
)

internal data class DaylightConversationSummary(
    val target: DaylightThreadTarget,
    val preview: String,
    val timestamp: Long,
    val lastFromUser: Boolean,
    val unread: Boolean
)

private val ThreadsUri: Uri =
    Uri.parse("content://mms-sms/conversations?simple=true")

private val CanonicalAddressesUri: Uri =
    Uri.parse("content://mms-sms/canonical-addresses")

private val MmsUri: Uri =
    Uri.parse("content://mms")

private val MmsPartUri: Uri =
    Uri.parse("content://mms/part")

private const val GroupConversationLimit = 25
private const val ThreadMessageLimit = 80

private const val MmsBoxInbox = 1
private const val MmsAddressFrom = 137

/*
 * Every recipient id → phone number.
 */
private fun canonicalAddresses(
    context: Context
): Map<Long, String> {

    val addresses =
        mutableMapOf<Long, String>()

    runCatching {
        context.contentResolver
            .query(
                CanonicalAddressesUri,
                arrayOf("_id", "address"),
                null,
                null,
                null
            )
            ?.use { cursor ->
                val idIndex = cursor.getColumnIndex("_id")
                val addressIndex = cursor.getColumnIndex("address")

                if (idIndex < 0 || addressIndex < 0) return@use

                while (cursor.moveToNext()) {
                    val address =
                        cursor.getString(addressIndex).orEmpty()

                    if (address.isNotBlank()) {
                        addresses[cursor.getLong(idIndex)] = address
                    }
                }
            }
    }

    return addresses
}

/*
 * "Sarah, Mike, Jen" from the people on a thread.
 */
internal fun groupNameFor(
    participants: List<String>,
    contactNameFor: (String) -> String?
): String =
    participants
        .map { number ->
            (contactNameFor(number) ?: number)
                .substringBefore(" ")
        }
        .joinToString(", ")

/*
 * A message's text and sender never change, so each MMS is read once
 * and remembered: the window refreshes every few seconds and would
 * otherwise re-read every part each time.
 */
private val mmsTextCache =
    java.util.concurrent.ConcurrentHashMap<Long, String>()

private val mmsSenderCache =
    java.util.concurrent.ConcurrentHashMap<Long, String>()

private fun cachedMmsText(
    context: Context,
    messageId: Long
): String =
    mmsTextCache[messageId]
        ?: mmsText(context, messageId).also {
            /*
             * An MMS still downloading has no parts yet: not cached,
             * so it fills in once it arrives.
             */
            if (it.isNotEmpty()) {
                mmsTextCache[messageId] = it
            }
        }

private fun cachedMmsSender(
    context: Context,
    messageId: Long
): String? {

    mmsSenderCache[messageId]
        ?.let { return it.ifEmpty { null } }

    val sender =
        mmsSender(context, messageId)

    mmsSenderCache[messageId] = sender.orEmpty()

    return sender
}

/*
 * The group list is rebuilt at most this often.
 */
private const val GroupListCacheMillis = 15_000L

@Volatile
private var groupListCache: Pair<Long, List<DaylightConversationSummary>>? = null

/*
 * The text of one MMS: its text parts joined, "Photo" / "Video" /
 * "Attachment" standing in for media.
 */
private fun mmsText(
    context: Context,
    messageId: Long
): String {

    val pieces =
        mutableListOf<String>()

    runCatching {
        context.contentResolver
            .query(
                MmsPartUri,
                arrayOf("_id", "ct", "text", "_data"),
                "mid = ?",
                arrayOf(messageId.toString()),
                null
            )
            ?.use { cursor ->
                val idIndex = cursor.getColumnIndex("_id")
                val typeIndex = cursor.getColumnIndex("ct")
                val textIndex = cursor.getColumnIndex("text")
                val dataIndex = cursor.getColumnIndex("_data")

                while (cursor.moveToNext()) {

                    val type =
                        if (typeIndex >= 0) cursor.getString(typeIndex).orEmpty() else ""

                    when {
                        type == "text/plain" -> {

                            val inline =
                                if (textIndex >= 0) cursor.getString(textIndex) else null

                            val text =
                                if (
                                    inline.isNullOrEmpty() &&
                                    dataIndex >= 0 &&
                                    !cursor.getString(dataIndex).isNullOrEmpty() &&
                                    idIndex >= 0
                                ) {
                                    runCatching {
                                        context.contentResolver
                                            .openInputStream(
                                                Uri.parse("content://mms/part/" + cursor.getLong(idIndex))
                                            )
                                            ?.bufferedReader()
                                            ?.use { it.readText() }
                                    }
                                        .getOrNull()
                                } else {
                                    inline
                                }

                            if (!text.isNullOrBlank()) {
                                pieces += text.trim()
                            }
                        }

                        type.startsWith("image/") -> pieces += "📷 Photo"
                        type.startsWith("video/") -> pieces += "🎬 Video"
                        type.startsWith("audio/") -> pieces += "🎤 Audio"
                        type == "application/smil" -> Unit
                        type.isNotBlank() -> pieces += "📎 Attachment"
                    }
                }
            }
    }

    return pieces.joinToString("\n")
}

/*
 * Who sent a received MMS.
 */
private fun mmsSender(
    context: Context,
    messageId: Long
): String? =
    runCatching {
        context.contentResolver
            .query(
                Uri.parse("content://mms/$messageId/addr"),
                arrayOf("address", "type"),
                "type = $MmsAddressFrom",
                null,
                null
            )
            ?.use { cursor ->
                val addressIndex = cursor.getColumnIndex("address")
                if (addressIndex >= 0 && cursor.moveToFirst()) {
                    cursor.getString(addressIndex)
                } else {
                    null
                }
            }
    }
        .getOrNull()
        ?.takeIf {
            it.isNotBlank() && it != "insert-address-token"
        }

/*
 * Group conversations (two or more other people), newest first.
 */
internal fun loadGroupConversations(
    context: Context,
    contactNameFor: (String) -> String?
): List<DaylightConversationSummary> {

    groupListCache
        ?.takeIf { System.currentTimeMillis() - it.first < GroupListCacheMillis }
        ?.let { return it.second }

    return readGroupConversations(context, contactNameFor)
        .also {
            groupListCache = System.currentTimeMillis() to it
        }
}

private fun readGroupConversations(
    context: Context,
    contactNameFor: (String) -> String?
): List<DaylightConversationSummary> {

    val addresses =
        canonicalAddresses(context)

    val summaries =
        mutableListOf<DaylightConversationSummary>()

    runCatching {
        context.contentResolver
            .query(
                ThreadsUri,
                arrayOf("_id", "date", "recipient_ids", "read", "snippet"),
                null,
                null,
                "date DESC"
            )
            ?.use { cursor ->
                val idIndex = cursor.getColumnIndex("_id")
                val dateIndex = cursor.getColumnIndex("date")
                val recipientsIndex = cursor.getColumnIndex("recipient_ids")
                val readIndex = cursor.getColumnIndex("read")
                val snippetIndex = cursor.getColumnIndex("snippet")

                if (idIndex < 0 || recipientsIndex < 0) return@use

                while (
                    cursor.moveToNext() &&
                    summaries.size < GroupConversationLimit
                ) {
                    val participants =
                        cursor.getString(recipientsIndex)
                            .orEmpty()
                            .split(" ")
                            .mapNotNull { it.trim().toLongOrNull() }
                            .mapNotNull { addresses[it] }

                    if (participants.size < 2) continue

                    val threadId =
                        cursor.getLong(idIndex)

                    val latest =
                        latestMmsIn(context, threadId)

                    summaries +=
                        DaylightConversationSummary(
                            target =
                                DaylightThreadTarget(
                                    title = groupNameFor(participants, contactNameFor),
                                    participants = participants,
                                    isGroup = true,
                                    threadId = threadId
                                ),
                            preview =
                                latest?.text
                                    ?: (if (snippetIndex >= 0) cursor.getString(snippetIndex) else null)
                                    ?: "",
                            timestamp =
                                if (dateIndex >= 0) cursor.getLong(dateIndex) else 0L,
                            lastFromUser =
                                latest?.fromUser ?: false,
                            unread =
                                readIndex >= 0 && cursor.getInt(readIndex) == 0
                        )
                }
            }
    }

    return summaries
}

/*
 * The newest MMS in a thread, for the list preview.
 */
private fun latestMmsIn(
    context: Context,
    threadId: Long
): NotificationMessage? =
    runCatching {
        context.contentResolver
            .query(
                MmsUri,
                arrayOf("_id", "date", "msg_box"),
                "thread_id = ?",
                arrayOf(threadId.toString()),
                "date DESC"
            )
            ?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null

                val id = cursor.getLong(cursor.getColumnIndexOrThrow("_id"))

                NotificationMessage(
                    sender = "",
                    text = cachedMmsText(context, id),
                    timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("date")) * 1000L,
                    fromUser = cursor.getInt(cursor.getColumnIndexOrThrow("msg_box")) != MmsBoxInbox
                )
            }
    }
        .getOrNull()

/*
 * Android's thread for a set of people (group or one person), without
 * creating one that doesn't exist yet.
 */
internal fun findThreadId(
    context: Context,
    participants: List<String>,
    contactNameFor: (String) -> String?
): Long? {

    if (participants.isEmpty()) return null

    val addresses =
        canonicalAddresses(context)

    return runCatching {
        context.contentResolver
            .query(
                ThreadsUri,
                arrayOf("_id", "recipient_ids"),
                null,
                null,
                "date DESC"
            )
            ?.use { cursor ->
                val idIndex = cursor.getColumnIndex("_id")
                val recipientsIndex = cursor.getColumnIndex("recipient_ids")

                if (idIndex < 0 || recipientsIndex < 0) return@use null

                while (cursor.moveToNext()) {
                    val numbers =
                        cursor.getString(recipientsIndex)
                            .orEmpty()
                            .split(" ")
                            .mapNotNull { it.trim().toLongOrNull() }
                            .mapNotNull { addresses[it] }

                    if (
                        numbers.size == participants.size &&
                        participants.all { wanted ->
                            numbers.any { android.telephony.PhoneNumberUtils.compare(it, wanted) }
                        }
                    ) {
                        return@use cursor.getLong(idIndex)
                    }
                }

                null
            }
    }
        .getOrNull()
}

/*
 * A group thread from a Messages notification: the group whose
 * name matches, or whose people include everyone who wrote in it.
 */
internal fun matchGroupForNotification(
    groups: List<DaylightConversationSummary>,
    title: String,
    senders: List<String>
): DaylightConversationSummary? {

    groups.firstOrNull {
        it.target.title.equals(title.trim(), ignoreCase = true)
    }
        ?.let { return it }

    val firstNames =
        senders
            .map { it.trim().substringBefore(" ").lowercase() }
            .filter { it.isNotBlank() }
            .distinct()

    if (firstNames.size < 2) return null

    return groups.firstOrNull { group ->
        val names =
            group.target.title
                .split(",")
                .map { it.trim().lowercase() }

        firstNames.all { it in names }
    }
}

/*
 * Every message in one Android thread, oldest first: its texts (SMS)
 * and its MMS, with who sent each in a group.
 */
internal fun loadThreadById(
    context: Context,
    threadId: Long,
    fallbackSender: String,
    contactNameFor: (String) -> String?
): List<NotificationMessage> {

    val messages =
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
                Telephony.Sms.THREAD_ID + " = ?",
                arrayOf(threadId.toString()),
                Telephony.Sms.DATE + " DESC"
            )
            ?.use { cursor ->
                val addressIndex = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIndex = cursor.getColumnIndex(Telephony.Sms.BODY)
                val dateIndex = cursor.getColumnIndex(Telephony.Sms.DATE)
                val typeIndex = cursor.getColumnIndex(Telephony.Sms.TYPE)

                if (bodyIndex < 0 || dateIndex < 0 || typeIndex < 0) return@use

                while (
                    cursor.moveToNext() &&
                    messages.size < ThreadMessageLimit
                ) {
                    val body = cursor.getString(bodyIndex).orEmpty()
                    if (body.isBlank()) continue

                    val fromUser =
                        cursor.getInt(typeIndex) != Telephony.Sms.MESSAGE_TYPE_INBOX

                    val address =
                        if (addressIndex >= 0) cursor.getString(addressIndex).orEmpty() else ""

                    messages +=
                        NotificationMessage(
                            sender =
                                if (fromUser) ""
                                else contactNameFor(address) ?: address.ifBlank { fallbackSender },
                            text = body,
                            timestamp = cursor.getLong(dateIndex),
                            fromUser = fromUser
                        )
                }
            }
    }

    val senderNames =
        mutableMapOf<String, String>()

    var mmsCount = 0

    runCatching {
        context.contentResolver
            .query(
                MmsUri,
                arrayOf("_id", "date", "msg_box"),
                "thread_id = ?",
                arrayOf(threadId.toString()),
                "date DESC"
            )
            ?.use { cursor ->
                val idIndex = cursor.getColumnIndex("_id")
                val dateIndex = cursor.getColumnIndex("date")
                val boxIndex = cursor.getColumnIndex("msg_box")

                if (idIndex < 0 || dateIndex < 0 || boxIndex < 0) return@use

                while (
                    cursor.moveToNext() &&
                    mmsCount < ThreadMessageLimit
                ) {
                    mmsCount++

                    val id = cursor.getLong(idIndex)
                    val text = cachedMmsText(context, id)
                    if (text.isBlank()) continue

                    val fromUser =
                        cursor.getInt(boxIndex) != MmsBoxInbox

                    val sender =
                        if (fromUser) {
                            ""
                        } else {
                            val address =
                                cachedMmsSender(context, id)

                            if (address == null) {
                                fallbackSender
                            } else {
                                senderNames.getOrPut(address) {
                                    contactNameFor(address) ?: address
                                }
                            }
                        }

                    messages +=
                        NotificationMessage(
                            sender = sender,
                            text = text,
                            timestamp = cursor.getLong(dateIndex) * 1000L,
                            fromUser = fromUser
                        )
                }
            }
    }

    return messages
        .sortedBy { it.timestamp }
        .takeLast(ThreadMessageLimit)
}