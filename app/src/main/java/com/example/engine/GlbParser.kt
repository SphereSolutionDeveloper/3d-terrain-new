package com.example.engine

import com.example.model.GlbHeaderInfo
import org.json.JSONObject
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object GlbParser {

    fun parse(inputStream: InputStream): Pair<GlbHeaderInfo, JSONObject?> {
        val bytes = inputStream.readBytes()
        if (bytes.size < 12) {
            return GlbHeaderInfo("INVALID", 0, bytes.size.toLong(), 0, 0, false, 0, 0) to null
        }

        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val magicBytes = ByteArray(4)
        buffer.get(magicBytes)
        val magic = String(magicBytes)
        val version = buffer.int
        val length = buffer.int.toLong() and 0xFFFFFFFFL

        if (magic != "glTF" || version != 2) {
            return GlbHeaderInfo(magic, version, length, 0, 0, false, 0, 0) to null
        }

        var jsonLength = 0L
        var binLength = 0L
        var gltfJson: JSONObject? = null
        var nodeCount = 0
        var meshCount = 0

        var offset = 12
        while (offset + 8 <= bytes.size) {
            buffer.position(offset)
            val chunkLength = buffer.int.toLong() and 0xFFFFFFFFL
            val chunkType = buffer.int

            offset += 8
            if (offset + chunkLength > bytes.size) break

            if (chunkType == 0x4E4F534A) { // "JSON"
                jsonLength = chunkLength
                val jsonBytes = ByteArray(chunkLength.toInt())
                buffer.position(offset)
                buffer.get(jsonBytes)
                val jsonString = String(jsonBytes, Charsets.UTF_8).trim()
                try {
                    val parsed = JSONObject(jsonString)
                    gltfJson = parsed
                    nodeCount = parsed.optJSONArray("nodes")?.length() ?: 0
                    meshCount = parsed.optJSONArray("meshes")?.length() ?: 0
                } catch (e: Exception) {
                    // Ignore json parse error
                }
            } else if (chunkType == 0x004E4942) { // "BIN\0"
                binLength = chunkLength
            }

            offset += chunkLength.toInt()
        }

        val headerInfo = GlbHeaderInfo(
            magic = magic,
            version = version,
            length = length,
            jsonLength = jsonLength,
            binLength = binLength,
            isValid = true,
            nodeCount = nodeCount,
            meshCount = meshCount
        )

        return headerInfo to gltfJson
    }
}
