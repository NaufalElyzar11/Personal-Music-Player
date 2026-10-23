package com.noirmusic.data.lyrics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

interface LrclibDataSource {
    /** `/api/get`. `Success(null)` means 404 (no exact match). */
    suspend fun get(trackName: String, artistName: String, durationSec: Int?): ApiResult<LrclibRecordDto?>

    /** `/api/search` with structured fields. */
    suspend fun search(trackName: String, artistName: String?): ApiResult<List<LrclibRecordDto>>
}

/**
 * OkHttp-based LRCLIB client. Does no retrying or sleeping; the repository owns the policy.
 * User-Agent / Accept headers are added by the [client]'s interceptor (see AppContainer).
 */
class LrclibApi(
    private val client: OkHttpClient,
    private val json: Json,
    private val baseUrl: HttpUrl,
    private val clock: () -> Long = System::currentTimeMillis,
) : LrclibDataSource {

    override suspend fun get(trackName: String, artistName: String, durationSec: Int?): ApiResult<LrclibRecordDto?> {
        val url = baseUrl.newBuilder()
            .addPathSegments("api/get")
            .addQueryParameter("track_name", trackName)
            .addQueryParameter("artist_name", artistName)
            .apply { if (durationSec != null && durationSec in 1..3600) addQueryParameter("duration", durationSec.toString()) }
            .build()
        return execute(url, notFoundValue = { null }) { body ->
            json.decodeFromString(LrclibRecordDto.serializer(), body)
        }
    }

    override suspend fun search(trackName: String, artistName: String?): ApiResult<List<LrclibRecordDto>> {
        val url = baseUrl.newBuilder()
            .addPathSegments("api/search")
            .addQueryParameter("track_name", trackName)
            .apply { if (!artistName.isNullOrBlank()) addQueryParameter("artist_name", artistName) }
            .build()
        return execute(url, notFoundValue = { emptyList() }) { body ->
            json.decodeFromString(ListSerializer(LrclibRecordDto.serializer()), body)
        }
    }

    private suspend fun <T> execute(
        url: HttpUrl,
        notFoundValue: () -> T,
        decode: (String) -> T,
    ): ApiResult<T> {
        val request = Request.Builder().url(url).get().build()
        val response = try {
            client.newCall(request).await()
        } catch (e: IOException) {
            return ApiResult.Failure(HttpErrorMapping.mapIoException(e))
        }
        return response.use { r ->
            when {
                r.code == 404 -> ApiResult.Success(notFoundValue())
                r.code == 429 -> ApiResult.Failure(
                    LyricsError.RateLimited(HttpErrorMapping.parseRetryAfterSeconds(r.header("Retry-After"), clock())),
                )
                r.code in 500..599 -> ApiResult.Failure(LyricsError.Server(r.code))
                !r.isSuccessful -> ApiResult.Failure(LyricsError.Http(r.code))
                else -> {
                    val body = try {
                        withContext(Dispatchers.IO) { r.body.string() }
                    } catch (e: IOException) {
                        return@use ApiResult.Failure(HttpErrorMapping.mapIoException(e))
                    }
                    try {
                        ApiResult.Success(withContext(Dispatchers.Default) { decode(body) })
                    } catch (e: SerializationException) {
                        ApiResult.Failure(LyricsError.Malformed)
                    } catch (e: IllegalArgumentException) {
                        ApiResult.Failure(LyricsError.Malformed)
                    }
                }
            }
        }
    }

    private suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
        cont.invokeOnCancellation { cancel() }
        enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (!cont.isCancelled) cont.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                cont.resume(response) { _, value, _ -> value.close() }
            }
        })
    }
}
