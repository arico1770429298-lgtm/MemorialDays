package com.example.memorialdays.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class EventTypeTest {

    @Test
    fun birthdayKeywordsAreRecognised() {
        assertEquals(EventType.BIRTHDAY, EventType.detect("妈妈生日"))
        assertEquals(EventType.BIRTHDAY, EventType.detect("宝宝出生"))
    }

    @Test
    fun memorialKeywordsAreRecognised() {
        assertEquals(EventType.MEMORIAL, EventType.detect("爷爷忌日"))
        assertEquals(EventType.MEMORIAL, EventType.detect("怀念外婆"))
    }

    @Test
    fun anniversaryKeywordsAreRecognised() {
        assertEquals(EventType.ANNIVERSARY, EventType.detect("相识十周年"))
        assertEquals(EventType.ANNIVERSARY, EventType.detect("在一起的第 1000 天"))
    }

    @Test
    fun ordinaryWordIsNotMisclassifiedAsBirthday() {
        assertEquals(EventType.GENERAL, EventType.detect("生活"))
    }
}
