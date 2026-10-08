package com.example.fieldmedic.util

import java.text.Normalizer

fun normalizeSearch(s: String): String {
    if (s.isEmpty()) return ""
    val withD = s.replace('đ', 'd').replace('Đ', 'd')
    val normalized = Normalizer.normalize(withD, Normalizer.Form.NFD)
    val withoutDiacritics = Regex("\\p{Mn}+").replace(normalized, "")
    return withoutDiacritics.trim().lowercase().replace(Regex("\\s+"), " ")
}
