package com.example.data

import kotlinx.coroutines.flow.Flow

class ClockRepository(private val db: ClockDatabase) {
    val allAlarms: Flow<List<AlarmEntity>> = db.alarmDao().getAllAlarms()
    val allWorldClocks: Flow<List<WorldClockEntity>> = db.worldClockDao().getAllWorldClocks()

    suspend fun insertAlarm(alarm: AlarmEntity): Long {
        return db.alarmDao().insertAlarm(alarm)
    }

    suspend fun updateAlarm(alarm: AlarmEntity) {
        db.alarmDao().updateAlarm(alarm)
    }

    suspend fun deleteAlarm(alarm: AlarmEntity) {
        db.alarmDao().deleteAlarm(alarm)
    }

    suspend fun insertWorldClock(worldClock: WorldClockEntity) {
        db.worldClockDao().insertWorldClock(worldClock)
    }

    suspend fun deleteWorldClock(worldClock: WorldClockEntity) {
        db.worldClockDao().deleteWorldClock(worldClock)
    }

    suspend fun deleteAllAlarms() {
        db.alarmDao().deleteAllAlarms()
    }

    suspend fun deleteAllWorldClocks() {
        db.worldClockDao().deleteAllWorldClocks()
    }
}
