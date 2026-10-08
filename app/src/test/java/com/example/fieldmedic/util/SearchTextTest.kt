package com.example.fieldmedic.util

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchTextTest {

    @Test
    fun normalizeSearch_nguyenVanDuc_returnsNormalizedString() {
        val result = normalizeSearch("Nguyễn Văn Đức")
        assertEquals("nguyen van duc", result)
    }

    @Test
    fun normalizeSearch_dangThiAnh_returnsNormalizedString() {
        val result = normalizeSearch("Đặng Thị Ánh")
        assertEquals("dang thi anh", result)
    }

    @Test
    fun normalizeSearch_multipleSpaces_collapsesAndTrimsSpaces() {
        val result = normalizeSearch("  Lý   Thị Mai ")
        assertEquals("ly thi mai", result)
    }

    @Test
    fun normalizeSearch_khuoiMa_returnsNormalizedString() {
        val result = normalizeSearch("Khuổi Mạ")
        assertEquals("khuoi ma", result)
    }

    @Test
    fun normalizeSearch_naPan_returnsNormalizedString() {
        val result = normalizeSearch("Nà Pàn")
        assertEquals("na pan", result)
    }

    @Test
    fun normalizeSearch_emptyString_returnsEmptyString() {
        val result = normalizeSearch("")
        assertEquals("", result)
    }
}
