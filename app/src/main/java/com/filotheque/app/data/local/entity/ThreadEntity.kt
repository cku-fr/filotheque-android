package com.filotheque.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "threads",
    indices = [
        Index(value = ["brandId", "code"], unique = true),
        Index(value = ["colorFamily"])
    ]
)
data class ThreadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val brandId: String,
    val brandName: String,
    val code: String,
    val name: String,
    val hexColor: String,
    val colorFamily: String,
    val quantity: Int = 0,
    val isOnShoppingList: Boolean = false,
    val note: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
