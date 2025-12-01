package com.zybooks.appmobilefinalproject

// For storing furniture locally using Room dependencies
import androidx.room.Entity
import androidx.room.PrimaryKey

// Room entity representing one furniture item saved in the local DB.
// This is the "database model" (not necessarily what your UI directly uses).
@Entity(tableName = "furniture")
data class FurnitureEntity(

    // Use the API's id (converted to String) as the primary key.
    @PrimaryKey val id: String,

    // Product title from the API.
    val name: String,

    // Category is stored as a String (e.g., "TABLES", "CHAIRS", "DESKS").
    // In MainActivity map this to the Category enum.
    val category: String,      // "TABLES", "CHAIRS", "DESKS"

    // URL to an image / thumbnail from the API.
    val imageUrl: String?,

    // Price in whole units (your code converts double to Int).
    val price: Int,

    // Currently set to "Unknown", but you can later map color from API or user input.
    val color: String,

    // Same idea as color – placeholder for now.
    val material: String,

    // Optional tags stored as a single comma-separated String.
    // convert this to List<String> in MainActivity via split(",").
    val tags: String?          // "dining,family" etc.
)
