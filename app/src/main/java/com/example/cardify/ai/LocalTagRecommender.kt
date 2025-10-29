package com.example.cardify.ai

import android.content.Context
import android.util.Log
import org.json.JSONObject
import kotlin.math.sqrt
import kotlin.random.Random

class LocalTagRecommender(context: Context) {
    private val tagEmbeddings: Map<String, List<Float>> = loadEmbeddings(context)

    fun recommendTags(input: String): List<String> {
        if (input.isBlank() || tagEmbeddings.isEmpty()) {
            return emptyList()
        }
        val inputVec = embed(input)
        val scoredTags = buildList {
            for ((tag, embedding) in tagEmbeddings) {
                val similarity = cosineSimilarity(embedding, inputVec)
                if (!similarity.isNaN()) {
                    add(tag to similarity)
                }
            }
        }.sortedByDescending { it.second }

        if (scoredTags.isEmpty()) {
            return emptyList()
        }

        val filtered = scoredTags
            .filter { it.second >= MIN_SIMILARITY_THRESHOLD }
            .take(MAX_RECOMMENDATIONS)

        if (filtered.isNotEmpty()) {
            return filtered.map { it.first }
        }

        return scoredTags
            .take(1)
            .map { it.first }
    }

    fun embeddingForTag(tag: String): List<Float>? = tagEmbeddings[tag]

    fun averageEmbedding(tags: List<String>): List<Float>? {
        val valid = tags.mapNotNull { tagEmbeddings[it] }
        if (valid.isEmpty()) return null
        val accumulator = FloatArray(VECTOR_SIZE)
        valid.forEach { vector ->
            for (index in vector.indices) {
                accumulator[index] += vector[index]
            }
        }
        val count = valid.size.coerceAtLeast(1)
        for (i in accumulator.indices) {
            accumulator[i] /= count
        }
        return accumulator.toList()
    }

    fun similarityBetween(tagSetA: List<String>, tagSetB: List<String>): Float? {
        val first = averageEmbedding(tagSetA) ?: return null
        val second = averageEmbedding(tagSetB) ?: return null
        val similarity = cosineSimilarity(first, second)
        return if (similarity.isNaN()) null else similarity
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
        private const val VECTOR_SIZE = 768
        private const val NORMALIZATION_FACTOR = 1000f
        private const val MIN_SIMILARITY_THRESHOLD = 0.2f
        private const val MAX_RECOMMENDATIONS = 3
        private const val LOG_TAG = "LocalTagRecommender"
        private const val FALLBACK_SEED = 2024
        private val FALLBACK_TAGS = listOf("운동", "러닝", "여행", "코딩", "요리")
    }

    private fun loadEmbeddings(context: Context): Map<String, List<Float>> {
        val fromAssets = runCatching { loadEmbeddingsFromAssets(context) }
            .onFailure { error ->
                Log.w(
                    LOG_TAG,
                    "Failed to load $TAG_EMBEDDINGS_FILE from assets, falling back to dummy data",
                    error
                )
            }
            .getOrNull()

        if (!fromAssets.isNullOrEmpty()) {
            Log.d(
                LOG_TAG,
                "Loaded ${fromAssets.size} tag embeddings from assets (using real data)"
            )
            return fromAssets
        }

        return createFallbackEmbeddings().also { fallback ->
            Log.d(LOG_TAG, "Loaded ${fallback.size} dummy tag embeddings for offline usage")
        }
    }

    private fun loadEmbeddingsFromAssets(context: Context): Map<String, List<Float>> {
        val json = context.assets.open(TAG_EMBEDDINGS_FILE)
            .bufferedReader()
            .use { it.readText() }
        val obj = JSONObject(json)
        val embeddings = mutableMapOf<String, List<Float>>()
        for (key in obj.keys()) {
            val arr = obj.getJSONArray(key)
            if (arr.length() != VECTOR_SIZE) {
                Log.w(
                    LOG_TAG,
                    "Skipping tag '$key' because embedding length ${arr.length()} != $VECTOR_SIZE"
                )
                continue
            }
            val values = ArrayList<Float>(VECTOR_SIZE)
            for (index in 0 until arr.length()) {
                values.add(arr.getDouble(index).toFloat())
            }
            embeddings[key] = values
        }
        if (embeddings.isEmpty()) {
            throw IllegalStateException("No valid tag embeddings found in assets")
        }
        return embeddings
    }

    private fun createFallbackEmbeddings(): Map<String, List<Float>> {
        val random = Random(FALLBACK_SEED)
        return FALLBACK_TAGS.associateWith {
            List(VECTOR_SIZE) { random.nextFloat() * 4f - 2f }
        }
    }
}
