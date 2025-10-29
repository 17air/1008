package com.example.cardify.ai

import android.content.Context
import android.util.Log
import org.json.JSONObject
import kotlin.math.sqrt

class LocalTagRecommender(context: Context) {
    private val tagEmbeddings: Map<String, List<Float>>

    init {
        tagEmbeddings = try {
            val json = context.assets.open(TAG_EMBEDDINGS_FILE)
                .bufferedReader()
                .use { it.readText() }
            val obj = JSONObject(json)
            obj.keys().asSequence().associateWith { key ->
                val arr = obj.getJSONArray(key)
                List(arr.length()) { index -> arr.getDouble(index).toFloat() }
            }
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Failed to load tag embeddings", e)
            emptyMap()
        }
    }

    fun recommendTags(input: String): List<String> {
        if (input.isBlank() || tagEmbeddings.isEmpty()) {
            return emptyList()
        }
        val inputVec = embed(input)
        val similarities = buildList {
            for ((tag, embedding) in tagEmbeddings) {
                val similarity = cosineSimilarity(embedding, inputVec)
                if (!similarity.isNaN()) {
                    add(tag to similarity)
                }
            }
        }
        return similarities
            .sortedByDescending { it.second }
            .take(3)
            .map { it.first }
    }

    private fun cosineSimilarity(v1: List<Float>, v2: List<Float>): Float {
        if (v1.isEmpty() || v2.isEmpty() || v1.size != v2.size) {
            return Float.NaN
        }
        var dot = 0f
        var norm1 = 0f
        var norm2 = 0f
        for (i in v1.indices) {
            val a = v1[i]
            val b = v2[i]
            dot += a * b
            norm1 += a * a
            norm2 += b * b
        }
        if (norm1 == 0f || norm2 == 0f) {
            return Float.NaN
        }
        return dot / (sqrt(norm1) * sqrt(norm2))
    }

    // Lightweight text embedding (no deep learning, just character-level encoding)
    private fun embed(text: String): List<Float> {
        val vec = FloatArray(VECTOR_SIZE)
        text.forEachIndexed { index, c ->
            if (index < VECTOR_SIZE) {
                vec[index] = c.code.toFloat() / NORMALIZATION_FACTOR
            }
        }
        return vec.toList()
    }

    companion object {
        private const val TAG_EMBEDDINGS_FILE = "tag_embeddings.json"
        private const val VECTOR_SIZE = 384
        private const val NORMALIZATION_FACTOR = 1000f
        private const val LOG_TAG = "LocalTagRecommender"
    }
}
