package dev.dettmer.simplenotes.sync.drive

import com.google.gson.JsonParser
import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/** Narrow Drive REST client: only the application's hidden appDataFolder is accessible. */
internal class DriveApi(private val accessToken: String) {
    data class FileInfo(val id: String, val name: String)

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    fun accountEmail(): String? {
        val url = "https://www.googleapis.com/drive/v3/about".toHttpUrl().newBuilder()
            .addQueryParameter("fields", "user(emailAddress)")
            .build()
        val root = JsonParser.parseString(execute(Request.Builder().url(url).get().build())).asJsonObject
        return root.getAsJsonObject("user")?.get("emailAddress")?.asString
    }

    fun listSnapshots(): List<FileInfo> {
        val files = mutableListOf<FileInfo>()
        var pageToken: String? = null
        do {
            val url = "https://www.googleapis.com/drive/v3/files".toHttpUrl().newBuilder()
                .addQueryParameter("spaces", "appDataFolder")
                .addQueryParameter("q", "name contains 'simple-notes-plus-snapshot-' and trashed = false")
                .addQueryParameter("fields", "nextPageToken,files(id,name)")
                .addQueryParameter("pageSize", "1000")
                .apply { pageToken?.let { addQueryParameter("pageToken", it) } }
                .build()
            val body = execute(Request.Builder().url(url).get().build())
            val root = JsonParser.parseString(body).asJsonObject
            root.getAsJsonArray("files")?.forEach { item ->
                val file = item.asJsonObject
                val id = file.get("id")?.asString
                val name = file.get("name")?.asString
                if (!id.isNullOrBlank() && name?.startsWith(SNAPSHOT_PREFIX) == true) {
                    files += FileInfo(id, name)
                }
            }
            pageToken = root.get("nextPageToken")?.asString
        } while (!pageToken.isNullOrEmpty())
        return files
    }

    fun download(id: String): String {
        val url = "https://www.googleapis.com/drive/v3/files".toHttpUrl().newBuilder()
            .addPathSegment(id)
            .addQueryParameter("alt", "media")
            .build()
        return execute(Request.Builder().url(url).get().build())
    }

    fun upload(name: String, existingId: String?, content: String): String {
        val bytes = content.toByteArray(Charsets.UTF_8)
        val uploadUrl = "https://www.googleapis.com/upload/drive/v3/files".toHttpUrl().newBuilder()
            .apply { existingId?.let { addPathSegment(it) } }
            .addQueryParameter("uploadType", "resumable")
            .addQueryParameter("fields", "id")
            .build()
        val metadata = if (existingId == null) {
            """{"name":"$name","parents":["appDataFolder"],"mimeType":"application/json"}"""
        } else {
            "{}"
        }
        val initRequest = Request.Builder().url(uploadUrl)
            .method(if (existingId == null) "POST" else "PATCH", metadata.toRequestBody(jsonType))
            .header("Authorization", "Bearer $accessToken")
            .header("X-Upload-Content-Type", "application/json")
            .header("X-Upload-Content-Length", bytes.size.toString())
            .build()
        val location = initializeUpload(initRequest)
        val uploadRequest = Request.Builder().url(location)
            .put(bytes.toRequestBody(jsonType))
            .header("Authorization", "Bearer $accessToken")
            .build()
        val response = execute(uploadRequest)
        return JsonParser.parseString(response).asJsonObject.get("id")?.asString
            ?: existingId ?: throw IOException("Drive upload returned no file ID")
    }

    private fun initializeUpload(request: Request): String = client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) throw IOException("Drive upload initialization failed: HTTP ${response.code}")
        response.header("Location") ?: throw IOException("Drive did not return an upload URL")
    }

    private fun execute(request: Request): String {
        val authenticated = request.newBuilder().header("Authorization", "Bearer $accessToken").build()
        return client.newCall(authenticated).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Drive request failed: HTTP ${response.code}")
            response.body?.string() ?: throw IOException("Drive returned an empty response")
        }
    }

    companion object {
        const val SNAPSHOT_PREFIX = "simple-notes-plus-snapshot-"
    }
}
