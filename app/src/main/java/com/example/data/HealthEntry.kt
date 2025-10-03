package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "entries")
data class HealthEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val metric: String,
    val value: Int,
    val notes: String? = null
)