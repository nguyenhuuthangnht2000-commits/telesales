package com.nhakhoaquangninh.telesales.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CallRecordDao {
    @Query("SELECT * FROM call_records WHERE id = :id")
    fun getById(id: String): CallRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(record: CallRecordEntity)

    @Query("DELETE FROM call_records WHERE id = :id")
    fun delete(id: String)
    
    @Query("DELETE FROM call_records")
    fun clearAll()
}
