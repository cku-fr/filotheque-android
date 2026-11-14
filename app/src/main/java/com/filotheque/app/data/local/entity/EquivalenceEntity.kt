package com.filotheque.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "equivalences",
    indices = [
        Index(value = ["dmcCode"]),
        Index(value = ["anchorCode"])
    ]
)
data class EquivalenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dmcCode: String,
    val anchorCode: String,
    val matchQuality: Int = 1 // 1 = Exact, 2 = Proche
)
