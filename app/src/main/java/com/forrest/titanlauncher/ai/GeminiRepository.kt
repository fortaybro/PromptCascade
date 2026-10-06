package com.forrest.titanlauncher.ai

import android.util.Log
import com.forrest.titanlauncher.mail.MailMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class GeminiRepository {

    suspend fun prioritizeEmails(
        apiKey: String,
        question: String,
        messages: List<MailMessage>
    ): String {

        return withContext(
            Dispatchers.IO
        ) {

            if (
                apiKey.isBlank()
            ) {

                return@withContext "GEMINI NOT SET UP\n\nrun geminisetup"
            }

            if (
                messages.isEmpty()
            ) {

                return@withContext "MAIL\n\nno inbox messages available"
            }

            val candidates =
                messages
                    .sortedWith(
                        compareByDescending<MailMessage> {
                            it.unread &&
                                    it.important
                        }
                            .thenByDescending {
                                it.important
                            }
                            .thenByDescending {
                                it.unread
                            }
                            .thenByDescending {
                                it.timestamp
                            }
                    )
                    .take(
                        AssistantPayloadLimits.PRIORITIZE_EMAILS
                    )

            val dateFormat =
                SimpleDateFormat(
                    "MMM d, h:mm a",
                    Locale.getDefault()
                )

            val mailContext =
                candidates
                    .mapIndexed {
                            index,
                            message ->

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
                                    AssistantPayloadLimits.PRIORITIZE_EMAIL_PREVIEW_CHARS
                                )

                        buildString {

                            append(
                                "EMAIL ${index + 1}\n"
                            )

                            append(
                                "From: ${message.sender}\n"
                            )

                            append(
                                "Subject: ${message.subject}\n"
                            )

                            append(
                                "Received: ${dateFormat.format(Date(message.timestamp))}\n"
                            )

                            append(
                                "Unread: ${message.unread}\n"
                            )

                            append(
                                "Important: ${message.important}\n"
                            )

                            append(
                                "Preview: $preview"
                            )
                        }
                    }
                    .joinToString(
                        "\n\n"
                    )

            val prompt =
                """
                You are the intelligence layer inside a minimalist Android launcher.
                The user asked: "$question"

                Review ONLY the inbox email context below. Rank the emails that most likely deserve a personal reply, decision, or follow-up from the user.

                Rules:
                - Do not invent facts that are not in the email context.
                - Be practical rather than overly strict: an email can deserve a reply even if it does not contain an explicit question mark.
                - Look for personal messages, direct requests, decisions, deadlines, scheduling, approvals, unresolved logistics, follow-ups, and messages where silence would likely leave someone waiting.
                - Use unread/important status as helpful signals, but do not treat them as proof by themselves.
                - Deprioritize newsletters, receipts, automated notices, marketing, mass announcements, and purely informational messages.
                - Unless every candidate is clearly automated or informational, return the 3 to 5 strongest reply candidates.
                - If evidence is uncertain, say "likely needs reply" or "worth reviewing" instead of excluding the message entirely.
                - Return at most 5 items.
                - Keep the answer extremely concise and useful on a small phone screen.
                - Plain text only. No markdown symbols, no emojis.
                - Format exactly like this:

                NEED REPLIES

                1. SENDER — SUBJECT
                   one short reason

                2. SENDER — SUBJECT
                   one short reason

                Only if every candidate is clearly automated, promotional, or purely informational, say:
                NEED REPLIES

                nothing clearly requires a reply

                EMAIL CONTEXT:
                $mailContext
                """.trimIndent()

            val requestBody =
                createRequestBody(
                    prompt =
                        prompt,
                    /*
                     * Raised from 450. The old ceiling truncated
                     * answers mid-sentence, which read as the view
                     * failing to scroll rather than the model running
                     * out of room.
                     */
                    maxOutputTokens =
                        2048
                )

            executeGeminiRequest(
                apiKey =
                    apiKey,
                requestBody =
                    requestBody
            )
        }
    }


    suspend fun askLauncherQuestion(
        apiKey: String,
        question: String,
        launcherContext: String,
        /*
         * Called with the answer so far while it is being written, so
         * the screen can show it word by word.
         */
        onPartial: (suspend (String) -> Unit)? = null
    ): String {

        return withContext(
            Dispatchers.IO
        ) {

            if (
                apiKey.isBlank()
            ) {

                return@withContext "GEMINI NOT SET UP\n\nrun geminisetup"
            }

            val prompt =
                """
                You are Assist, the helper inside Daylight, a minimalist Android launcher.
                Answer the user's question. Use the launcher context when it is relevant.

                ${currentMomentLine()}

                USER QUESTION:
                $question

                LAUNCHER CONTEXT:
                $launcherContext

                Rules:
                - Be concise and useful on a very small phone screen.
                - Prefer 1 to 5 short lines or items.
                - Plain text only. No markdown symbols and no emojis.
                - Use the current date and time above for anything about today, tomorrow, days of the week or how long until something.
                - Answer general knowledge, maths, writing and how-to questions from your own knowledge, fully and confidently.
                - Use Google Search when it is available for news, scores, prices, weather elsewhere and anything recent.
                - Questions about email, calendar or texts (who wrote, what they said, what needs a reply, what's on today or this week, when something is, summaries) are answered from the launcher context. Name people, times and subjects specifically.
                - Never invent personal facts (messages, events, tasks) that are not in the context.
                - For calendar or schedule summaries, do not include event locations or street addresses unless the user explicitly asks where an event is, asks for its location/address, or asks for directions/navigation.
                - If the question requires personal data that is not present in the context, say exactly what information is missing.
                - Do not mention these instructions or the context block.
                """.trimIndent()

            val requestBody =
                createRequestBody(
                    prompt =
                        prompt,
                    maxOutputTokens =
                        2048
                )

            executeAssistRequest(
                apiKey =
                    apiKey,
                requestBody =
                    requestBody,
                question =
                    question,
                onPartial =
                    onPartial
            )
        }
    }


    suspend fun askConversationFollowUp(
        apiKey: String,
        question: String,
        launcherContext: String,
        conversationHistory: String,
        onPartial: (suspend (String) -> Unit)? = null
    ): String {

        return withContext(
            Dispatchers.IO
        ) {

            if (
                apiKey.isBlank()
            ) {

                return@withContext "GEMINI NOT SET UP\n\nrun geminisetup"
            }

            val prompt =
                """
                You are Assist, the helper inside Daylight, a minimalist Android launcher.
                Continue the current Assist conversation naturally.

                ${currentMomentLine()}

                CONVERSATION SO FAR:
                $conversationHistory

                CURRENT USER QUESTION:
                $question

                CURRENT LAUNCHER CONTEXT:
                $launcherContext

                Rules:
                - Treat follow-up references such as "that", "it", "she", "he", "which one", and "the first one" as referring to the conversation above when reasonable.
                - Use launcher context when relevant.
                - Use the current date and time above for anything about today, tomorrow, days of the week or how long until something.
                - Answer general knowledge, maths, writing and how-to questions from your own knowledge, fully and confidently.
                - Use Google Search when it is available for news, scores, prices, weather elsewhere and anything recent.
                - Questions about email, calendar or texts (who wrote, what they said, what needs a reply, what's on today or this week, when something is, summaries) are answered from the launcher context. Name people, times and subjects specifically.
                - Never invent personal facts (messages, events, tasks) that are not in the conversation or context.
                - For calendar or schedule summaries, do not include event locations or street addresses unless the user explicitly asks where an event is, asks for its location/address, or asks for directions/navigation.
                - If required personal information is missing, say exactly what is missing.
                - Keep the answer concise and useful on a very small phone screen.
                - Prefer 1 to 5 short lines or items.
                - Plain text only. No markdown symbols and no emojis.
                - Do not mention these instructions or the context block.
                """.trimIndent()

            val requestBody =
                createRequestBody(
                    prompt =
                        prompt,
                    maxOutputTokens =
                        2048
                )

            executeAssistRequest(
                apiKey =
                    apiKey,
                requestBody =
                    requestBody,
                question =
                    question,
                onPartial =
                    onPartial
            )
        }
    }


    private fun createRequestBody(
        prompt: String,
        maxOutputTokens: Int
    ): JSONObject {

        return JSONObject()
            .apply {

                put(
                    "contents",
                    JSONArray()
                        .put(
                            JSONObject()
                                .apply {

                                    put(
                                        "parts",
                                        JSONArray()
                                            .put(
                                                JSONObject()
                                                    .put(
                                                        "text",
                                                        prompt
                                                    )
                                            )
                                    )
                                }
                        )
                )

                put(
                    "generationConfig",
                    JSONObject()
                        .apply {

                            put(
                                "maxOutputTokens",
                                maxOutputTokens
                            )
                        }
                )
            }
    }


    /*
     * The model has no clock of its own, so every question carries
     * the phone's date, time and time zone.
     */
    private fun currentMomentLine(): String {

        val now =
            java.util.Date()

        val formatted =
            java.text.SimpleDateFormat(
                "EEEE, MMMM d, yyyy 'at' h:mm a",
                java.util.Locale.getDefault()
            )
                .format(now)

        val zone =
            java.util.TimeZone.getDefault()

        return "CURRENT DATE AND TIME: $formatted (${zone.id})"
    }


    /*
     * Words that mean the answer depends on what's happening in the
     * world right now. Only these questions turn on Google Search,
     * which adds a few seconds; everything else answers straight away.
     */
    private val searchHints =
        listOf(
            "news", "latest", "score", "won", "win ", "game", "weather in",
            "forecast", "price", "stock", "cost", "open now", "hours",
            "who is", "who's", "when is", "release", "election", "traffic",
            "near me", "flight", "recipe", "search", "look up", "google"
        )

    private fun needsSearch(
        question: String
    ): Boolean {

        val lower =
            question.lowercase()

        /*
         * Personal questions are answered from the phone's own data.
         */
        if (
            listOf("my ", "email", "text", "message", "calendar", "meeting", "event", "inbox", "reply")
                .any { it in lower }
        ) {
            return false
        }

        return searchHints.any { it in lower }
    }


    /*
     * The fast path for Assist:
     *  - the answer streams in as it's written
     *  - light thinking ("low"), which is plenty for launcher questions
     *  - Google Search only when the question needs it
     * If the model refuses any of that, it asks again plainly, and the
     * phone remembers not to try the extras again this session.
     */
    private suspend fun executeAssistRequest(
        apiKey: String,
        requestBody: JSONObject,
        question: String,
        onPartial: (suspend (String) -> Unit)?
    ): String {

        val model =
            MODEL_PLAN.first().model

        if (
            leanSupported
        ) {

            val lean =
                JSONObject(
                    requestBody.toString()
                )

            lean.optJSONObject("generationConfig")
                ?.put(
                    "thinkingConfig",
                    JSONObject()
                        .put("thinkingLevel", "low")
                )

            if (
                needsSearch(question)
            ) {
                lean.put(
                    "tools",
                    JSONArray()
                        .put(
                            JSONObject()
                                .put(
                                    "google_search",
                                    JSONObject()
                                )
                        )
                )
            }

            val result =
                performStreamingRequest(
                    apiKey = apiKey,
                    model = model,
                    requestBody = lean,
                    onPartial = onPartial
                )

            if (
                result is GeminiCallResult.Success
            ) {
                return result.text
            }

            if (
                result is GeminiCallResult.Failure &&
                result.responseCode == 400
            ) {
                Log.w(
                    TAG,
                    "Lean request refused, using plain requests: ${result.responseText.take(200)}"
                )
                leanSupported = false
            }
        }

        val plain =
            performStreamingRequest(
                apiKey = apiKey,
                model = model,
                requestBody = requestBody,
                onPartial = onPartial
            )

        if (
            plain is GeminiCallResult.Success
        ) {
            return plain.text
        }

        /*
         * Last resort: the original path with retries and the
         * fallback model.
         */
        return executeGeminiRequest(
            apiKey =
                apiKey,
            requestBody =
                requestBody
        )
    }


    /*
     * streamGenerateContent with server-sent events: each "data:" line
     * carries the next piece of the answer.
     */
    private suspend fun performStreamingRequest(
        apiKey: String,
        model: String,
        requestBody: JSONObject,
        onPartial: (suspend (String) -> Unit)?
    ): GeminiCallResult {

        val connection =
            URL(
                "https://generativelanguage.googleapis.com/v1beta/models/$model:streamGenerateContent?alt=sse"
            )
                .openConnection() as HttpURLConnection

        return try {

            connection.requestMethod = "POST"
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.doOutput = true
            connection.setRequestProperty("x-goog-api-key", apiKey)
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            connection.setRequestProperty("Accept", "text/event-stream")

            connection.outputStream.use { stream ->
                stream.write(
                    requestBody.toString().toByteArray(Charsets.UTF_8)
                )
            }

            val responseCode =
                connection.responseCode

            if (
                responseCode !in 200..299
            ) {
                val errorText =
                    connection.errorStream
                        ?.bufferedReader()
                        ?.use { it.readText() }
                        .orEmpty()

                return GeminiCallResult.Failure(
                    responseCode = responseCode,
                    responseText = errorText
                )
            }

            val answer =
                StringBuilder()

            connection.inputStream
                .bufferedReader(Charsets.UTF_8)
                .use { reader ->
                    while (true) {

                        val line =
                            reader.readLine()
                                ?: break

                        if (
                            !line.startsWith("data:")
                        ) {
                            continue
                        }

                        val chunk =
                            runCatching {
                                JSONObject(
                                    line.removePrefix("data:").trim()
                                )
                            }
                                .getOrNull()
                                ?: continue

                        val parts =
                            chunk
                                .optJSONArray("candidates")
                                ?.optJSONObject(0)
                                ?.optJSONObject("content")
                                ?.optJSONArray("parts")
                                ?: continue

                        var added = false

                        for (index in 0 until parts.length()) {
                            val part =
                                parts.optJSONObject(index)
                                    ?: continue

                            if (
                                part.optBoolean("thought", false)
                            ) {
                                continue
                            }

                            val text =
                                part.optString("text")

                            if (
                                text.isNotEmpty()
                            ) {
                                answer.append(text)
                                added = true
                            }
                        }

                        if (
                            added &&
                            onPartial != null
                        ) {
                            onPartial(
                                answer.toString().trim()
                            )
                        }
                    }
                }

            val text =
                answer.toString().trim()

            if (
                text.isBlank()
            ) {
                GeminiCallResult.Failure(
                    responseCode = 200,
                    responseText = "empty Gemini response"
                )
            } else {
                GeminiCallResult.Success(
                    text = text
                )
            }

        } catch (
            exception: Exception
        ) {

            if (
                exception is kotlinx.coroutines.CancellationException
            ) {
                throw exception
            }

            GeminiCallResult.ConnectionFailure(
                message =
                    "${exception.javaClass.simpleName}: ${exception.message.orEmpty()}"
            )

        } finally {

            connection.disconnect()
        }
    }


    private suspend fun executeGeminiRequest(
        apiKey: String,
        requestBody: JSONObject
    ): String {

        var lastFailure =
            GeminiFailure(
                responseCode =
                    null,
                responseText =
                    "",
                connectionFailure =
                    false
            )

        MODEL_PLAN
            .forEachIndexed {
                    modelIndex,
                    modelPlan ->

                repeat(
                    modelPlan.attempts
                ) {
                        attemptIndex ->

                    val result =
                        performSingleRequest(
                            apiKey =
                                apiKey,
                            model =
                                modelPlan.model,
                            requestBody =
                                requestBody
                        )

                    when (
                        result
                    ) {

                        is GeminiCallResult.Success -> {

                            if (
                                modelIndex > 0
                            ) {

                                Log.i(
                                    TAG,
                                    "Gemini fallback succeeded with ${modelPlan.model}"
                                )
                            }

                            return result.text
                        }

                        is GeminiCallResult.Failure -> {

                            lastFailure =
                                GeminiFailure(
                                    responseCode =
                                        result.responseCode,
                                    responseText =
                                        result.responseText,
                                    connectionFailure =
                                        false
                                )

                            logFailure(
                                model =
                                    modelPlan.model,
                                attempt =
                                    attemptIndex + 1,
                                responseCode =
                                    result.responseCode,
                                responseText =
                                    result.responseText
                            )

                            if (
                                !shouldRetry(
                                    result.responseCode
                                )
                            ) {

                                if (
                                    shouldTryFallback(
                                        result.responseCode
                                    ) &&
                                    modelIndex <
                                    MODEL_PLAN.lastIndex
                                ) {

                                    return@repeat
                                }

                                return userFacingError(
                                    result.responseCode,
                                    result.responseText,
                                    connectionFailure =
                                        false
                                )
                            }
                        }

                        is GeminiCallResult.ConnectionFailure -> {

                            lastFailure =
                                GeminiFailure(
                                    responseCode =
                                        null,
                                    responseText =
                                        result.message,
                                    connectionFailure =
                                        true
                                )

                            Log.w(
                                TAG,
                                "Gemini connection failure on ${modelPlan.model}, attempt ${attemptIndex + 1}: ${result.message}"
                            )
                        }
                    }

                    val hasAnotherAttempt =
                        attemptIndex <
                                modelPlan.attempts - 1

                    if (
                        hasAnotherAttempt
                    ) {

                        val backoff =
                            retryDelayMillis(
                                attemptIndex
                            )

                        delay(
                            backoff
                        )
                    }
                }
            }

        return userFacingError(
            responseCode =
                lastFailure.responseCode,
            responseText =
                lastFailure.responseText,
            connectionFailure =
                lastFailure.connectionFailure
        )
    }


    private fun performSingleRequest(
        apiKey: String,
        model: String,
        requestBody: JSONObject
    ): GeminiCallResult {

        val connection =
            URL(
                "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"
            )
                .openConnection() as HttpURLConnection

        return try {

            connection.requestMethod =
                "POST"

            connection.connectTimeout =
                15_000

            connection.readTimeout =
                30_000

            connection.doOutput =
                true

            connection.setRequestProperty(
                "x-goog-api-key",
                apiKey
            )

            connection.setRequestProperty(
                "Content-Type",
                "application/json; charset=UTF-8"
            )

            connection.setRequestProperty(
                "Accept",
                "application/json"
            )

            connection
                .outputStream
                .use {
                        stream ->

                    stream.write(
                        requestBody
                            .toString()
                            .toByteArray(
                                Charsets.UTF_8
                            )
                    )
                }

            val responseCode =
                connection.responseCode

            val responseText =
                (
                        if (
                            responseCode in 200..299
                        ) {

                            connection.inputStream

                        } else {

                            connection.errorStream
                        }
                        )
                    ?.bufferedReader()
                    ?.use {
                        it.readText()
                    }
                    .orEmpty()

            if (
                responseCode !in 200..299
            ) {

                GeminiCallResult.Failure(
                    responseCode =
                        responseCode,
                    responseText =
                        responseText
                )

            } else {

                val root =
                    JSONObject(
                        responseText
                    )

                val text =
                    root
                        .optJSONArray(
                            "candidates"
                        )
                        ?.optJSONObject(
                            0
                        )
                        ?.optJSONObject(
                            "content"
                        )
                        ?.optJSONArray(
                            "parts"
                        )
                        ?.let { parts ->

                            /*
                             * Answers can come in several parts
                             * (search-grounded ones especially), so
                             * all text parts are joined; thought
                             * summaries are skipped.
                             */
                            (0 until parts.length())
                                .mapNotNull { index ->
                                    parts.optJSONObject(index)
                                }
                                .filterNot {
                                    it.optBoolean("thought", false)
                                }
                                .joinToString("") {
                                    it.optString("text")
                                }
                        }
                        .orEmpty()
                        .trim()

                if (
                    text.isBlank()
                ) {

                    GeminiCallResult.Failure(
                        responseCode =
                            200,
                        responseText =
                            "empty Gemini response"
                    )

                } else {

                    GeminiCallResult.Success(
                        text =
                            text
                    )
                }
            }

        } catch (
            exception: Exception
        ) {

            GeminiCallResult.ConnectionFailure(
                message =
                    "${exception.javaClass.simpleName}: ${exception.message.orEmpty()}"
            )

        } finally {

            connection.disconnect()
        }
    }


    private fun shouldRetry(
        responseCode: Int
    ): Boolean {

        return responseCode in
                setOf(
                    408,
                    429,
                    500,
                    502,
                    503,
                    504
                )
    }


    private fun shouldTryFallback(
        responseCode: Int
    ): Boolean {

        return responseCode ==
                404
    }


    private fun retryDelayMillis(
        attemptIndex: Int
    ): Long {

        val baseDelay =
            when (
                attemptIndex
            ) {

                0 ->
                    900L

                1 ->
                    1_800L

                else ->
                    3_600L
            }

        val jitter =
            Random.nextLong(
                from =
                    100L,
                until =
                    401L
            )

        return baseDelay +
                jitter
    }


    private fun userFacingError(
        responseCode: Int?,
        responseText: String,
        connectionFailure: Boolean
    ): String {

        if (
            connectionFailure
        ) {

            return "GEMINI CONNECTION FAILED\n\ntry again shortly"
        }

        return when (
            responseCode
        ) {

            400 ->
                "GEMINI REQUEST FAILED"

            401,
            403 ->
                "GEMINI KEY NOT AUTHORIZED"

            404 ->
                "GEMINI MODEL UNAVAILABLE\n\ntry again shortly"

            408,
            504 ->
                "GEMINI TIMED OUT\n\ntry again shortly"

            429 ->
                if (
                    responseText.contains(
                        "quota",
                        ignoreCase =
                            true
                    )
                ) {
                    "GEMINI QUOTA REACHED"
                } else {
                    "GEMINI BUSY\n\ntry again shortly"
                }

            500,
            502,
            503 ->
                "GEMINI TEMPORARILY UNAVAILABLE\n\ntry again shortly"

            200 ->
                "GEMINI RETURNED NO ANSWER"

            null ->
                "GEMINI CONNECTION FAILED\n\ntry again shortly"

            else ->
                "GEMINI ERROR $responseCode"
        }
    }


    private fun logFailure(
        model: String,
        attempt: Int,
        responseCode: Int,
        responseText: String
    ) {

        val safeMessage =
            try {

                JSONObject(
                    responseText
                )
                    .optJSONObject(
                        "error"
                    )
                    ?.optString(
                        "message"
                    )
                    .orEmpty()
                    .take(
                        500
                    )

            } catch (
                _: Exception
            ) {

                responseText
                    .take(
                        500
                    )
            }

        Log.w(
            TAG,
            "Gemini request failed: model=$model attempt=$attempt code=$responseCode message=$safeMessage"
        )
    }


    private sealed class GeminiCallResult {

        data class Success(
            val text: String
        ) : GeminiCallResult()


        data class Failure(
            val responseCode: Int,
            val responseText: String
        ) : GeminiCallResult()


        data class ConnectionFailure(
            val message: String
        ) : GeminiCallResult()
    }


    private data class GeminiFailure(
        val responseCode: Int?,
        val responseText: String,
        val connectionFailure: Boolean
    )


    private data class GeminiModelPlan(
        val model: String,
        val attempts: Int
    )


    companion object {

        /*
         * Whether the model accepts the fast-path extras (light
         * thinking, search). Turned off for the session on a refusal.
         */
        @Volatile
        private var leanSupported = true

        private const val TAG =
            "PromptGemini"

        private val MODEL_PLAN =
            listOf(
                GeminiModelPlan(
                    model =
                        "gemini-3.7-flash",
                    attempts =
                        3
                ),
                GeminiModelPlan(
                    model =
                        "gemini-3.5-flash",
                    attempts =
                        2
                )
            )
    }
}