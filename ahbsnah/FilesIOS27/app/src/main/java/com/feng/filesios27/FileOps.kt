package com.feng.filesios27

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object FileOps {
    val IMG = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic")
    val VID = setOf("mp4", "mkv", "webm", "3gp", "mov", "avi")
    val AUD = setOf("mp3", "m4a", "wav", "ogg", "aac", "flac", "opus")
    val TXT = setOf(
        "txt", "md", "json", "xml", "log", "csv", "html", "htm", "css", "js", "kt", "java", "py",
        "c", "cpp", "h", "sh", "ini", "conf", "yml", "yaml", "gradle", "properties", "sql", "srt", "kts"
    )

    fun ext(f: File) = f.extension.lowercase()

    fun kind(f: File): String {
        if (f.isDirectory) return "Folder"
        val e = ext(f)
        return when {
            e in IMG -> "Image"
            e in VID -> "Video"
            e in AUD -> "Audio"
            e in TXT -> "Text"
            e == "zip" -> "ZIP archive"
            e == "apk" -> "Android app"
            e.isEmpty() -> "Document"
            else -> e.uppercase() + " document"
        }
    }

    fun size(b: Long): String {
        if (b < 1000) return "$b bytes"
        val u = arrayOf("KB", "MB", "GB", "TB")
        var v = b / 1000.0
        var i = 0
        while (v >= 1000 && i < 3) { v /= 1000; i++ }
        return String.format(Locale.US, "%.1f %s", v, u[i])
    }

    fun dirSize(f: File): Long =
        if (f.isDirectory) f.walkTopDown().filter { it.isFile }.sumOf { it.length() } else f.length()

    fun list(dir: File): List<File> =
        (dir.listFiles() ?: emptyArray()).filter { !it.name.startsWith(".") }

    fun unique(dir: File, name: String, isDir: Boolean = false): File {
        var f = File(dir, name)
        if (!f.exists()) return f
        val i = name.lastIndexOf('.')
        val b = if (i > 0 && !isDir) name.substring(0, i) else name
        val e = if (i > 0 && !isDir) name.substring(i) else ""
        var n = 2
        while (true) {
            f = File(dir, "$b $n$e")
            if (!f.exists()) return f
            n++
        }
    }

    fun copy(src: File, dir: File): File {
        val dst = unique(dir, src.name, src.isDirectory)
        if (src.isDirectory) {
            if (dst.canonicalPath.startsWith(src.canonicalPath + File.separator)) throw Exception("Cannot copy a folder into itself")
            src.copyRecursively(dst)
        } else src.copyTo(dst)
        return dst
    }

    fun move(src: File, dir: File): File {
        val dst = unique(dir, src.name, src.isDirectory)
        if (src.isDirectory && dst.canonicalPath.startsWith(src.canonicalPath + File.separator)) throw Exception("Cannot move a folder into itself")
        if (!src.renameTo(dst)) {
            if (src.isDirectory) src.copyRecursively(dst) else src.copyTo(dst)
            src.deleteRecursively()
        }
        return dst
    }

    fun zip(files: List<File>, dir: File): File {
        val first = files[0]
        val name = if (files.size == 1) (if (first.isDirectory) first.name else first.nameWithoutExtension) + ".zip" else "Archive.zip"
        val out = unique(dir, name)
        ZipOutputStream(BufferedOutputStream(FileOutputStream(out))).use { z ->
            fun add(f: File, path: String) {
                if (f.isDirectory) {
                    val ch = f.listFiles() ?: return
                    if (ch.isEmpty()) { z.putNextEntry(ZipEntry("$path/")); z.closeEntry() }
                    for (c in ch) add(c, "$path/${c.name}")
                } else {
                    z.putNextEntry(ZipEntry(path))
                    f.inputStream().use { it.copyTo(z) }
                    z.closeEntry()
                }
            }
            for (f in files) add(f, f.name)
        }
        return out
    }

    fun unzip(zip: File, dir: File): File {
        val dest = unique(dir, zip.nameWithoutExtension, true)
        dest.mkdirs()
        val base = dest.canonicalPath
        ZipInputStream(BufferedInputStream(FileInputStream(zip))).use { zi ->
            var e = zi.nextEntry
            while (e != null) {
                val t = File(dest, e.name)
                if (!t.canonicalPath.startsWith(base + File.separator)) throw SecurityException("Bad zip entry")
                if (e.isDirectory) t.mkdirs() else {
                    t.parentFile?.mkdirs()
                    FileOutputStream(t).use { zi.copyTo(it) }
                }
                zi.closeEntry()
                e = zi.nextEntry
            }
        }
        return dest
    }

    fun recent(root: File, n: Int): List<File> {
        val out = ArrayList<File>()
        var visited = 0
        fun walk(d: File, depth: Int) {
            if (depth > 6 || visited > 30000) return
            val ch = d.listFiles() ?: return
            for (c in ch) {
                if (c.name.startsWith(".")) continue
                visited++
                if (c.isDirectory) { if (c.name != "Android") walk(c, depth + 1) } else out.add(c)
            }
        }
        walk(root, 0)
        return out.sortedByDescending { it.lastModified() }.take(n)
    }

    fun search(dir: File, q: String): List<File> {
        val out = ArrayList<File>()
        fun walk(d: File, depth: Int) {
            if (depth > 5 || out.size >= 500) return
            val ch = d.listFiles() ?: return
            for (c in ch) {
                if (c.name.startsWith(".")) continue
                if (c.name.contains(q, true)) out.add(c)
                if (c.isDirectory && c.name != "Android") walk(c, depth + 1)
            }
        }
        walk(dir, 0)
        return out
    }
}
