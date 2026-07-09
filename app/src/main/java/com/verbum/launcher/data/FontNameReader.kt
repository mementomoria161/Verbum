package com.verbum.launcher.data

import java.io.File
import java.io.RandomAccessFile

/**
 * Reads the human-readable family name out of a .ttf/.otf file by parsing the
 * OpenType `name` table, so the UI can show "Roboto Slab" instead of a copied
 * file name like "font_1736431200000". Returns null if the file cannot be
 * parsed, in which case callers fall back to a generic label.
 */
object FontNameReader {

    fun read(file: File): String? = try {
        RandomAccessFile(file, "r").use { raf ->
            var base = 0L
            val tag = raf.readInt()
            if (tag == 0x74746366) { // 'ttcf' — a font collection; use the first font.
                raf.skipBytes(4) // version
                val numFonts = raf.readInt()
                if (numFonts <= 0) return@use null
                base = raf.readInt().toLong() and 0xFFFFFFFFL
                raf.seek(base)
                raf.readInt() // sfnt version of the first font
            }

            val numTables = raf.readUnsignedShort()
            raf.skipBytes(6) // searchRange, entrySelector, rangeShift

            var nameTableOffset = -1L
            repeat(numTables) {
                val t = ByteArray(4)
                raf.readFully(t)
                raf.skipBytes(4) // checksum
                val offset = raf.readInt().toLong() and 0xFFFFFFFFL
                raf.skipBytes(4) // length
                if (String(t, Charsets.US_ASCII) == "name") nameTableOffset = offset
            }
            if (nameTableOffset < 0) return@use null

            raf.seek(nameTableOffset)
            raf.skipBytes(2) // format
            val count = raf.readUnsignedShort()
            val storageOffset = raf.readUnsignedShort()

            data class Record(
                val platformId: Int,
                val nameId: Int,
                val length: Int,
                val stringOffset: Int,
            )

            val records = ArrayList<Record>(count)
            repeat(count) {
                val platformId = raf.readUnsignedShort()
                raf.skipBytes(2) // encodingId
                raf.skipBytes(2) // languageId
                val nameId = raf.readUnsignedShort()
                val length = raf.readUnsignedShort()
                val stringOffset = raf.readUnsignedShort()
                records.add(Record(platformId, nameId, length, stringOffset))
            }

            fun readString(record: Record): String? {
                raf.seek(nameTableOffset + storageOffset + record.stringOffset)
                val bytes = ByteArray(record.length)
                raf.readFully(bytes)
                val charset = if (record.platformId == 3 || record.platformId == 0) {
                    Charsets.UTF_16BE // Windows / Unicode
                } else {
                    Charsets.ISO_8859_1 // Macintosh (approx. of MacRoman)
                }
                return String(bytes, charset).trim().ifBlank { null }
            }

            // Prefer the full font name (nameID 4), then family (nameID 1),
            // and within each prefer the Windows platform record (id 3).
            fun pick(nameId: Int): String? = records
                .filter { it.nameId == nameId }
                .sortedByDescending { if (it.platformId == 3) 1 else 0 }
                .firstNotNullOfOrNull { readString(it) }

            pick(4) ?: pick(1)
        }
    } catch (_: Exception) {
        null
    }
}
