package com.example.wantuch.data.local.database

import androidx.room.*
import com.google.gson.Gson
import com.example.wantuch.domain.model.*
import com.example.wantuch.data.local.entities.InstitutionEntity
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromMap(value: Map<String, Any?>?): String? = gson.toJson(value)

    @TypeConverter
    fun toMap(value: String?): Map<String, Any?>? {
        val type = object : TypeToken<Map<String, Any?>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun fromModuleList(value: List<ModuleItem>?): String? = gson.toJson(value)

    @TypeConverter
    fun toModuleList(value: String?): List<ModuleItem>? {
        val type = object : TypeToken<List<ModuleItem>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun fromInstitutionList(value: List<InstitutionEntity>?): String? = gson.toJson(value)

    @TypeConverter
    fun toInstitutionList(value: String?): List<InstitutionEntity>? {
        val type = object : TypeToken<List<InstitutionEntity>>() {}.type
        return gson.fromJson(value, type)
    }
}
