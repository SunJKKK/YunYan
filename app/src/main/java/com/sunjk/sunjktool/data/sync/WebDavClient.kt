package com.sunjk.sunjktool.data.sync

/** Mirrors Sardine's DavResource, so we can swap implementations later. */
data class DavResource(
    val name: String,
    val href: String,
    val isDirectory: Boolean,
    val contentLength: Long,
    val modified: Long, // epoch millis
    val contentType: String
)

interface WebDavClient {
    /** PROPFIND – list directory contents. depth=1 only. */
    suspend fun listDirectory(path: String): List<DavResource>

    /** Download file as raw bytes. */
    suspend fun downloadFile(remotePath: String): ByteArray

    /** Upload byte array to remote path. */
    suspend fun uploadFile(remotePath: String, data: ByteArray, contentType: String)

    /** Delete a file or empty directory. */
    suspend fun deleteFile(remotePath: String)

    /** Check whether a file or directory exists (HEAD request). */
    suspend fun exists(remotePath: String): Boolean

    /** MKCOL – create a directory (idempotent – no-op if already exists). */
    suspend fun createDirectory(remotePath: String)
}
