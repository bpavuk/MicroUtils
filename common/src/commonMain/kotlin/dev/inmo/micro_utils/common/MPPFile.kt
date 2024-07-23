package dev.inmo.micro_utils.common

import kotlinx.io.Buffer
import kotlinx.io.Source
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class FileName(val string: String) {
    val name: String
        get() = withoutSlashAtTheEnd.takeLastWhile { it != '/' }
    val extension: String
        get() = name.takeLastWhile { it != '.' }
    val nameWithoutExtension: String
        get() {
            val filename = name
            return filename.indexOfLast { it == '.' }.takeIf { it > -1 } ?.let {
                filename.substring(0, it)
            } ?: filename
        }
    val withoutSlashAtTheEnd: String
        get() = string.dropLastWhile { it == '/' }
    override fun toString(): String = string
}


typealias MPPFile = Path

val MPPFile.filename: FileName
    get() = FileName(name)
val MPPFile.filesize: Long
    get() = SystemFileSystem.metadataOrNull(this) ?.size ?.takeIf { it > -1 } ?: error("Path $filename does not exists or is folder")
val MPPFile.bytesAllocatorSync: ByteArrayAllocator
    get() = {
        source().readByteArray()
    }
val MPPFile.bytesAllocator: SuspendByteArrayAllocator
    get() = {
        bytesAllocatorSync()
    }
fun MPPFile.bytesSync() = bytesAllocatorSync()
suspend fun MPPFile.bytes() = bytesAllocator()

fun MPPFile.source(): Source = SystemFileSystem.source(this).buffered()


//expect class MPPFile
//
//expect val MPPFile.filename: FileName
//expect val MPPFile.filesize: Long
//expect val MPPFile.bytesAllocatorSync: ByteArrayAllocator
//expect val MPPFile.bytesAllocator: SuspendByteArrayAllocator
//fun MPPFile.bytesSync() = bytesAllocatorSync()
//suspend fun MPPFile.bytes() = bytesAllocator()
