package com.example.clima_v100.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val username: String,
    val email: String,
    val passwordHash: String,
    val securityQuestion: String = "",
    val securityAnswerHash: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isLoggedIn: Boolean = false
)