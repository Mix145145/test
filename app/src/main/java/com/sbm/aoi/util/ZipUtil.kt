package com.sbm.aoi.util

import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ZipUtil {

    fun extractZip(inputStream: InputStream, destDir: File): List<File> {
        destDir.mkdirs()
        val extractedFiles = mutableListOf<File>()

        ZipInputStream(inputStream).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                val file = File(destDir, entry.name)
                // Защита от zip-slip
                if (!file.canonicalPath.startsWith(destDir.canonicalPath)) {
                    throw SecurityException("Zip entry outside target dir: ${entry.name}")
                }
                if (entry.isDirectory) {
                    file.mkdirs()
                } else {
                    file.parentFile?.mkdirs()
                    FileOutputStream(file).use { fos ->
                        zis.copyTo(fos)
                    }
                    extractedFiles.add(file)
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return extractedFiles
    }

    fun createZip(sourceFiles: List<Pair<String, File>>, outputFile: File) {
        ZipOutputStream(FileOutputStream(outputFile)).use { zos ->
            sourceFiles.forEach { (entryName, file) ->
                if (file.exists()) {
                    val entry = ZipEntry(entryName)
                    zos.putNextEntry(entry)
                    file.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            }
        }
    }
}
