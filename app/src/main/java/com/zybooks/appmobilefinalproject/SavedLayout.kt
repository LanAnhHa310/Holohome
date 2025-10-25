package com.zybooks.appmobilefinalproject

data class SavedLayout(
    val id: String,
    val name: String,
    val roomType: String,
    val dateCreated: Long,
    val thumbnailResId: Int? = null
)