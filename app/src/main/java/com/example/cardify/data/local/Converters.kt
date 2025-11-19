package com.example.cardify.data.local

import androidx.room.TypeConverter
import org.json.JSONArray
import org.json.JSONObject

class Converters {
    @TypeConverter
    fun fromStringList(list: List<String>?): String? {
        if (list == null) return null
        val array = JSONArray()
        list.forEach { array.put(it) }
        return array.toString()
    }

    @TypeConverter
    fun toStringList(json: String?): List<String> {
        if (json.isNullOrEmpty()) return emptyList()
        val array = JSONArray(json)
        val values = ArrayList<String>(array.length())
        for (index in 0 until array.length()) {
            values.add(array.optString(index))
        }
        return values
    }

    @TypeConverter
    fun fromStringMap(map: Map<String, String>?): String? {
        if (map == null) return null
        val obj = JSONObject()
        map.forEach { (key, value) -> obj.put(key, value) }
        return obj.toString()
    }

    @TypeConverter
    fun toStringMap(json: String?): Map<String, String> {
        if (json.isNullOrEmpty()) return emptyMap()
        val obj = JSONObject(json)
        val iterator = obj.keys()
        val values = mutableMapOf<String, String>()
        while (iterator.hasNext()) {
            val key = iterator.next()
            values[key] = obj.optString(key)
        }
        return values
    }
}
