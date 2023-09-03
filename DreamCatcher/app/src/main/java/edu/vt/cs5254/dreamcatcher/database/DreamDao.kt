package edu.vt.cs5254.dreamcatcher.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import edu.vt.cs5254.dreamcatcher.Dream
import edu.vt.cs5254.dreamcatcher.DreamEntry
import kotlinx.coroutines.flow.Flow
import java.util.*

@Dao
interface DreamDao {
    @Query("SELECT * FROM dream d JOIN dream_entry e ON e.dreamId = d.id ORDER BY d.lastUpdated DESC")
    fun getDreams(): Flow<Map<Dream, List<DreamEntry>>>

    @Query("SELECT * FROM dream WHERE id=(:id)")
    suspend fun getDream(id: UUID): Dream

    @Query("SELECT * FROM dream_entry where dreamId = (:id)")
    suspend fun getEntriesForDream(id: UUID): List<DreamEntry>

    @Transaction
    suspend fun getDreamAndEntries(id: UUID): Dream {
        return getDream(id).apply { entries = getEntriesForDream(id) }
    }

    @Update
    suspend fun updateDream(dream: Dream)

    @Query("DELETE FROM dream_entry where dreamId = (:id)")
    suspend fun deleteEntriesFromDream(id: UUID)

    @Insert
    suspend fun insertDreamEntry(dreamEntry: DreamEntry)

    @Transaction
    suspend fun updateDreamAndEntries(dream: Dream) {
        deleteEntriesFromDream(dream.id)
        dream.entries.forEach { insertDreamEntry(it) }
        updateDream(dream)
    }

    @Insert
    suspend fun insertDream(dream: Dream)

    @Transaction
    suspend fun addDreamAndEntries(dream: Dream) {
        insertDream(dream)
        dream.entries.forEach { insertDreamEntry(it) }
    }

    @Delete
    suspend fun deleteDream(dream: Dream)

    @Transaction
    suspend fun deleteDreamAndEntries(dream: Dream) {
        deleteEntriesFromDream(dream.id)
        deleteDream(dream)
    }

}