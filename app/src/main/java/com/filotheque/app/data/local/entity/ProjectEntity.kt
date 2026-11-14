package com.filotheque.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val patternSource: String = "",
    val status: String = "EN_COURS", // A_FAIRE, EN_COURS, TERMINE
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
