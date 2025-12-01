package com.zybooks.appmobilefinalproject

// DAO interface
import androidx.room.*


// Data Access Object for the furniture table.
// Contains all SQL operations used by this app.
@Dao
interface FurnitureDao {

    // Get all furniture rows in the table.
    @Query("SELECT * FROM furniture")
    suspend fun getAll(): List<FurnitureEntity>

    // Replace everything with a new list.
    // OnConflict.REPLACE: if same primary key appears, the row is replaced.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<FurnitureEntity>)

    // Delete all rows (used when refreshing from the API).
    @Query("DELETE FROM furniture")
    suspend fun deleteAll()
}