package com.zybooks.appmobilefinalproject

// For storing furniture locally using Room dependencies
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.*

// Room entity representing one furniture item saved in the local DB.
// This is the "database model" (not necessarily what your UI directly uses).
@Entity(tableName = "saved_layouts")
data class SavedLayoutEntity(
    @PrimaryKey val id: String,

    val name: String,

    val roomType: String,

    val dateCreated: Long

)
