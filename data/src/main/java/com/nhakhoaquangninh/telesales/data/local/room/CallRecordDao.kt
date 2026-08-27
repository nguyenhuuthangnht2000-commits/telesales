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

    @Query("SELECT * FROM call_records WHERE ownerUserId = :ownerUserId")
    fun getByOwner(ownerUserId: Int): List<CallRecordEntity>

    @Query("SELECT * FROM call_records WHERE ownerUserId = :ownerUserId AND status IN ('PENDING', 'RETRYABLE')")
    fun getPendingByOwner(ownerUserId: Int): List<CallRecordEntity>

    @Query("SELECT * FROM call_records WHERE callId = :callId")
    fun getByCallId(callId: String): CallRecordEntity?

    @Query(
        """
        DELETE FROM call_records
        WHERE startedAtMillis < :startOfTodayMillis
          AND status NOT IN ('PENDING', 'UPLOADING', 'RETRYABLE')
    """
    )
    fun deleteTerminalRecordsBefore(startOfTodayMillis: Long)
}
