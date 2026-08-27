package com.nhakhoaquangninh.telesales.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FailedCallDao {
    @Query("SELECT * FROM failed_calls ORDER BY callAtMillis DESC")
    fun getAll(): List<FailedCallEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(event: FailedCallEntity)

    @Query("DELETE FROM failed_calls WHERE id = :id")
    fun delete(id: String)

    @Query("SELECT * FROM failed_calls WHERE ownerUserId = :ownerUserId ORDER BY callAtMillis DESC")
    fun getByOwner(ownerUserId: Int): List<FailedCallEntity>

    @Query("SELECT * FROM failed_calls WHERE callId = :callId")
    fun getByCallId(callId: String): FailedCallEntity?
}
