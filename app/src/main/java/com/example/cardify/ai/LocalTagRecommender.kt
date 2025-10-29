package com.example.cardify.ai

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.sqrt
import kotlin.random.Random

class LocalTagRecommender(context: Context) {
    private val tagEmbeddings: Map<String, List<Float>> = loadEmbeddings(context)
    private val recommendedTags: List<String> = loadRecommendedTags(context)

    fun recommendTags(input: String): List<String> {
        val query = input.trim()
        if (query.isEmpty()) {
            return recommendedTags.take(MAX_RECOMMENDATIONS)
        }

        val suggestions = linkedSetOf<String>()

        if (tagEmbeddings.isNotEmpty()) {
            val normalized = query.lowercase()
            tagEmbeddings.keys
                .filter { normalized.contains(it.lowercase()) }
                .forEach { tag -> suggestions.add(tag) }

            val inputVec = embed(query)
            tagEmbeddings.mapNotNull { (tag, embedding) ->
                val similarity = cosineSimilarity(embedding, inputVec)
                if (similarity.isNaN()) null else tag to similarity
            }
                .filter { it.second >= SIMILARITY_THRESHOLD }
                .sortedByDescending { it.second }
                .forEach { (tag, _) ->
                    if (suggestions.size < MAX_RECOMMENDATIONS) {
                        suggestions.add(tag)
                    }
                }
        }

        if (suggestions.isEmpty()) {
            fallbackTags(query)
                .forEach { tag ->
                    if (suggestions.size < MAX_RECOMMENDATIONS) {
                        suggestions.add(tag)
                    }
                }
        }

        return suggestions.take(MAX_RECOMMENDATIONS).toList()
    }

    fun embeddingForTag(tag: String): List<Float>? = tagEmbeddings[tag]

    fun averageEmbedding(tags: List<String>): List<Float>? {
        val valid = tags.mapNotNull { tagEmbeddings[it] }
        if (valid.isEmpty()) return null
        val accumulator = FloatArray(VECTOR_SIZE)
        valid.forEach { vector ->
            for (index in vector.indices) {
                accumulator[index] = accumulator[index] + vector[index]
            }
        }
        val count = valid.size.coerceAtLeast(1).toFloat()
        for (i in accumulator.indices) {
            accumulator[i] = accumulator[i] / count
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
        private const val RECOMMENDED_TAGS_FILE = "recommended_tags.json"
        private const val VECTOR_SIZE = 768
        private const val NORMALIZATION_FACTOR = 1000f
        private const val MAX_RECOMMENDATIONS = 3
        private const val SIMILARITY_THRESHOLD = 0.25f
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

    private fun loadRecommendedTags(context: Context): List<String> {
        val json = runCatching {
            context.assets.open(RECOMMENDED_TAGS_FILE)
                .bufferedReader()
                .use { it.readText() }
        }.getOrNull()
        if (json.isNullOrEmpty()) {
            return FALLBACK_TAGS
        }
        return runCatching {
            val array = JSONArray(json)
            buildList {
                for (index in 0 until array.length()) {
                    val value = array.optString(index)
                    if (value.isNotBlank()) add(value)
                }
            }
        }.getOrElse { FALLBACK_TAGS }
    }

    private fun fallbackTags(input: String): List<String> {
        val query = input.trim()
        if (query.isEmpty()) {
            return recommendedTags.take(MAX_RECOMMENDATIONS)
        }
        val prefixMatches = recommendedTags.filter { it.startsWith(query, ignoreCase = true) }
        if (prefixMatches.isNotEmpty()) {
            return prefixMatches.take(MAX_RECOMMENDATIONS)
        }
        return recommendedTags.take(MAX_RECOMMENDATIONS)
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
