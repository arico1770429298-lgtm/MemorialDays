package com.example.memorialdays.domain

enum class EventType(val storage: String) {
    GENERAL("GENERAL"),
    BIRTHDAY("BIRTHDAY"),
    MEMORIAL("MEMORIAL"),
    ANNIVERSARY("ANNIVERSARY");

    companion object {
        fun fromStorage(value: String?): EventType =
            entries.firstOrNull { it.storage == value } ?: GENERAL

        /**
         * Lightweight, deterministic keyword matching. Titles are user text, so
         * only unambiguous words are used; "生活" must not become a birthday.
         */
        fun detect(title: String): EventType {
            val text = title.trim()
            return when {
                text.contains("忌") ||
                    text.contains("逝") ||
                    text.contains("离开") ||
                    text.contains("怀念") ||
                    text.contains("悼") -> MEMORIAL

                text.contains("生日") ||
                    text.contains("出生") ||
                    text.contains("诞生") ||
                    text.contains("生辰") -> BIRTHDAY

                text.contains("相识") ||
                    text.contains("在一起") ||
                    text.contains("结婚") ||
                    text.contains("周年") ||
                    text.contains("纪念日") -> ANNIVERSARY

                else -> GENERAL
            }
        }
    }
}
