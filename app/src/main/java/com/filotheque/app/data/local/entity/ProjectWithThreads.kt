package com.filotheque.app.data.local.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class ProjectWithThreads(
    @Embedded val project: ProjectEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = ProjectThreadCrossRef::class,
            parentColumn = "projectId",
            entityColumn = "threadId"
        )
    )
    val threads: List<ThreadEntity>
)
