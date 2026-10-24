package com.example.memorialdays.domain

import java.time.LocalDate

data class MemorialEvent(
    val id: Long,
    val title: String,
    val date: LocalDate,
    val note: String?,
    val isYearly: Boolean = false,
    val eventType: EventType = EventType.GENERAL,
    val isLunar: Boolean = false,
    val sortOrder: Int = 0,
    val isPinned: Boolean = false,
    val color: Int = 0
)
