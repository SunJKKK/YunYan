package com.sunjk.sunjktool.data.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.util.concurrent.TimeUnit

class KtorWebDavClient(
    private val baseUrl: String,
    private val username: String,
    private val password: String,
    private val client: OkHttpClient
) : WebDavClient {

    private fun buildUrl(path: String): String {
        val cleanPath = path.trimStart('/')
        val cleanBase = baseUrl.trimEnd('/')
        return if (cleanPath.isEmpty()) cleanBase else "$cleanBase/$cleanPath"
    }

    private val authHeader: String
        get() = Credentials.basic(username, password)

    override suspend fun listDirectory(path: String): List<DavResource> =
        withContext(Dispatchers.IO) {
            val url = buildUrl(path)
            try {
                val body = """
                    <?xml version="1.0" encoding="utf-8"?>
                    <D:propfind xmlns:D="DAV:">
                      <D:allprop/>
                    </D:propfind>
                """.trimIndent().toRequestBody("application/xml".toMediaType())

                val request = Request.Builder()
                    .url(url)
                    .method("PROPFIND", body)
                    .header("Authorization", authHeader)
                    .header("Depth", "1")
                    .build()

                val response = client.newCall(request).execute()
                response.use { resp ->
                    when {
                        resp.code == 404 -> emptyList()
                        resp.code == 401 -> throw SyncException.AuthFailure("认证失败")
                        !resp.isSuccessful -> throw SyncException.NetworkError("PROPFIND failed: ${resp.code}")
                        else -> {
                            val xml = resp.body?.string() ?: return@withContext emptyList()
                            parsePropfindResponse(xml)
                        }
                    }
                }
            } catch (e: SyncException) { throw e }
            catch (e: Exception) {
                val msg = e.message ?: ""
                if (msg.contains("401")) throw SyncException.AuthFailure("认证失败", e)
                throw SyncException.NetworkError("Failed to list directory: $msg", e)
            }
        }

    override suspend fun downloadFile(remotePath: String): ByteArray =
        withContext(Dispatchers.IO) {
            val url = buildUrl(remotePath)
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("Authorization", authHeader)
                    .build()

                val response = client.newCall(request).execute()
                response.use { resp ->
                    when {
                        resp.code == 401 -> throw SyncException.AuthFailure("认证失败")
                        resp.code == 404 -> throw SyncException.NotFound("File not found: $remotePath")
                        !resp.isSuccessful -> throw SyncException.NetworkError("Download failed: ${resp.code}")
                        else -> resp.body?.bytes() ?: ByteArray(0)
                    }
                }
            } catch (e: SyncException) { throw e }
            catch (e: Exception) {
                throw SyncException.NetworkError("Failed to download: ${e.message}", e)
            }
        }

    override suspend fun uploadFile(remotePath: String, data: ByteArray, contentType: String) =
        withContext(Dispatchers.IO) {
            val url = buildUrl(remotePath)
            try {
                val body = data.toRequestBody(contentType.toMediaType())
                val request = Request.Builder()
                    .url(url)
                    .put(body)
                    .header("Authorization", authHeader)
                    .build()

                val response = client.newCall(request).execute()
                response.use { resp ->
                    when {
                        resp.code == 401 -> throw SyncException.AuthFailure("认证失败")
                        resp.code == 507 -> throw SyncException.QuotaExceeded("坚果云存储空间不足")
                        !resp.isSuccessful -> throw SyncException.NetworkError("Upload failed: ${resp.code}")
                    }
                }
            } catch (e: SyncException) { throw e }
            catch (e: Exception) {
                throw SyncException.NetworkError("Failed to upload: ${e.message}", e)
            }
        }

    override suspend fun deleteFile(remotePath: String) =
        withContext(Dispatchers.IO) {
            val url = buildUrl(remotePath)
            try {
                val request = Request.Builder()
                    .url(url)
                    .delete()
                    .header("Authorization", authHeader)
                    .build()

                client.newCall(request).execute().use { /* ignore response; 404 = already deleted */ }
            } catch (_: Exception) { /* ignore delete errors */ }
        }

    override suspend fun exists(remotePath: String): Boolean =
        withContext(Dispatchers.IO) {
            val url = buildUrl(remotePath)
            try {
                val request = Request.Builder()
                    .url(url)
                    .head()
                    .header("Authorization", authHeader)
                    .build()

                client.newCall(request).execute().use { it.isSuccessful }
            } catch (_: Exception) {
                false
            }
        }

    override suspend fun createDirectory(remotePath: String) {
        withContext(Dispatchers.IO) {
            val url = buildUrl(remotePath)
            try {
                val request = Request.Builder()
                    .url(url)
                    .method("MKCOL", null)
                    .header("Authorization", authHeader)
                    .build()

                val resp = client.newCall(request).execute()
                resp.use {
                    if (!it.isSuccessful && it.code != 405 && it.code != 409) {
                        throw SyncException.NetworkError("Failed to create directory: ${it.code}")
                    }
                }
            } catch (e: SyncException) { throw e }
            catch (e: Exception) {
                val msg = e.message ?: ""
                if (!msg.contains("405") && !msg.contains("409")) {
                    throw SyncException.NetworkError("Failed to create directory: $msg", e)
                }
            }
        }
    }

    // ─── PROPFIND XML Parser ─────────────────────────────────────────

    private fun parsePropfindResponse(xml: String): List<DavResource> {
        val resources = mutableListOf<DavResource>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))

            var currentResource: DavResource? = null
            var currentTag: String? = null
            val davNs = "DAV:"

            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> {
                        currentTag = parser.name
                        val ns = parser.namespace ?: davNs
                        if (currentTag == "response" && ns == davNs) {
                            currentResource = DavResource("", "", false, 0, 0, "")
                        }
                    }
                    XmlPullParser.TEXT -> {
                        val r = currentResource ?: run {
                            event = parser.next(); continue
                        }
                        when (currentTag) {
                            "href" -> currentResource = r.copy(href = parser.text.trim())
                            "displayname" -> currentResource = r.copy(name = parser.text.trim())
                            "getcontentlength" -> {
                                val len = parser.text.trim().toLongOrNull() ?: 0L
                                currentResource = r.copy(contentLength = len)
                            }
                            "getcontenttype" -> currentResource =
                                r.copy(contentType = parser.text.trim())
                            "getlastmodified" -> {
                                val ts = parseRfc2822(parser.text.trim())
                                currentResource = r.copy(modified = ts)
                            }
                            "resourcetype" -> { /* handled by presence of collection child */ }
                            "collection" -> currentResource = r.copy(isDirectory = true)
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "response") {
                            val r = currentResource
                            if (r != null && r.href.isNotEmpty()) {
                                resources.add(r)
                            }
                            currentResource = null
                        }
                    }
                }
                event = parser.next()
            }
        } catch (_: Exception) { /* malformed XML — return what we have */ }
        return resources
    }

    private fun parseRfc2822(dateStr: String): Long {
        return try {
            java.time.ZonedDateTime.parse(
                dateStr,
                java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME
            ).toInstant().toEpochMilli()
        } catch (_: Exception) {
            0L
        }
    }
}
