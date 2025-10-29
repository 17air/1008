package com.example.cardify.data.local

import androidx.room.TypeConverter
import org.json.JSONArray

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
}
