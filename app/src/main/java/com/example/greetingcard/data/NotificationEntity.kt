package com.example.greetingcard.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
        @PrimaryKey(autoGenerate = true) val id: Long = 0,
        val title: String,
        val description: String,
        val packageName: String,
        val timestamp: Long
)
