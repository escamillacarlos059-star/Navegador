package com.example.nativecore

import android.util.Log

enum class SecurityClassification(val code: Int) {
    INTERNAL(0),
    SECURE_HTTPS(1),
    INSECURE_HTTP(2),
    SUSPICIOUS_PUNYCODE(3),
    IP_LITERAL(4);

    companion object {
        fun fromCode(code: Int): SecurityClassification =
            entries.firstOrNull { it.code == code } ?: INSECURE_HTTP
    }
}

data class NormalizedInputResult(
    val finalUrl: String,
    val displayHost: String,
    val isSearchQuery: Boolean,
    val strippedParamsCount: Int
)

data class NativeEngineDiagnostics(
    val nativeLibraryLoaded: Boolean,
    val cppStandard: String,
    val compilerInfo: String,
    val targetAbi: String,
    val filterRulesCount: Int,
    val customRulesCount: Int,
    val totalRequestsInspected: Long,
    val totalTrackersBlocked: Long,
    val totalParamsStripped: Long,
    val lastLookupNanos: Long
)

data class PeBinaryInspection(
    val isValidPe: Boolean,
    val architecture: String,
    val peFormat: String,
    val subsystem: String,
    val sectionsCount: Int,
    val statusDetail: String
)

object VectraNativeEngine {
    private const val TAG = "VectraNativeEngine"

    val isNativeLoaded: Boolean = try {
        System.loadLibrary("vectra_core")
        true
    } catch (e: UnsatisfiedLinkError) {
        Log.w(TAG, "libvectra_core.so not available in current runtime: ${e.message}")
        false
    }

    private external fun nativeNormalizeInput(
        rawInput: String,
        searchEngineId: Int,
        stripTrackingParams: Boolean
    ): String

    private external fun nativeShouldBlockRequest(
        requestUrl: String,
        pageHost: String
    ): Boolean

    private external fun nativeAnalyzeSecurity(url: String): Int
    private external fun nativeExtractHost(url: String): String
    private external fun nativeAddCustomBlockedDomain(domain: String): Boolean
    private external fun nativeRemoveCustomBlockedDomain(domain: String): Boolean
    private external fun nativeGetCustomBlockedDomains(): String
    private external fun nativeGetDiagnostics(): String
    private external fun nativeInspectPeBytes(bytes: ByteArray): String
    private external fun nativeResetStats()

    fun normalizeInput(
        rawInput: String,
        searchEngineId: Int,
        stripTrackingParams: Boolean
    ): NormalizedInputResult {
        if (!isNativeLoaded) {
            return fallbackNormalizeForHostTests(rawInput, searchEngineId)
        }
        val packed = nativeNormalizeInput(rawInput, searchEngineId, stripTrackingParams)
        val parts = packed.split("\n")
        return NormalizedInputResult(
            finalUrl = parts.getOrNull(0)?.ifBlank { "vectra://home" } ?: "vectra://home",
            displayHost = parts.getOrNull(1).orEmpty(),
            isSearchQuery = parts.getOrNull(2) == "1",
            strippedParamsCount = parts.getOrNull(3)?.toIntOrNull() ?: 0
        )
    }

    fun shouldBlockRequest(requestUrl: String, pageHost: String): Boolean {
        if (!isNativeLoaded) return false
        return nativeShouldBlockRequest(requestUrl, pageHost)
    }

    fun analyzeSecurity(url: String): SecurityClassification {
        if (!isNativeLoaded) {
            return when {
                url.isBlank() || url.startsWith("vectra://") -> SecurityClassification.INTERNAL
                url.startsWith("https://", ignoreCase = true) -> SecurityClassification.SECURE_HTTPS
                else -> SecurityClassification.INSECURE_HTTP
            }
        }
        return SecurityClassification.fromCode(nativeAnalyzeSecurity(url))
    }

    fun extractHost(url: String): String {
        if (!isNativeLoaded) {
            return url.substringAfter("://").substringBefore("/").substringBefore(":").lowercase()
        }
        return nativeExtractHost(url)
    }

    fun addCustomBlockedDomain(domain: String): Boolean {
        if (!isNativeLoaded) return false
        return nativeAddCustomBlockedDomain(domain)
    }

    fun removeCustomBlockedDomain(domain: String): Boolean {
        if (!isNativeLoaded) return false
        return nativeRemoveCustomBlockedDomain(domain)
    }

    fun getCustomBlockedDomains(): List<String> {
        if (!isNativeLoaded) return emptyList()
        val raw = nativeGetCustomBlockedDomains()
        return if (raw.isBlank()) emptyList() else raw.split("\n").filter { it.isNotBlank() }
    }

    fun getDiagnostics(): NativeEngineDiagnostics {
        if (!isNativeLoaded) {
            return NativeEngineDiagnostics(
                nativeLibraryLoaded = false,
                cppStandard = "C++20 (Host JVM Test Fallback)",
                compilerInfo = "N/A",
                targetAbi = "JVM",
                filterRulesCount = 0,
                customRulesCount = 0,
                totalRequestsInspected = 0,
                totalTrackersBlocked = 0,
                totalParamsStripped = 0,
                lastLookupNanos = 0
            )
        }
        val parts = nativeGetDiagnostics().split("\n")
        return NativeEngineDiagnostics(
            nativeLibraryLoaded = true,
            cppStandard = parts.getOrNull(0) ?: "C++20",
            compilerInfo = parts.getOrNull(1) ?: "Clang",
            targetAbi = parts.getOrNull(2) ?: "arm64-v8a",
            filterRulesCount = parts.getOrNull(3)?.toIntOrNull() ?: 0,
            customRulesCount = parts.getOrNull(4)?.toIntOrNull() ?: 0,
            totalRequestsInspected = parts.getOrNull(5)?.toLongOrNull() ?: 0L,
            totalTrackersBlocked = parts.getOrNull(6)?.toLongOrNull() ?: 0L,
            totalParamsStripped = parts.getOrNull(7)?.toLongOrNull() ?: 0L,
            lastLookupNanos = parts.getOrNull(8)?.toLongOrNull() ?: 0L
        )
    }

    fun inspectPeBytes(bytes: ByteArray): PeBinaryInspection {
        if (!isNativeLoaded) {
            return PeBinaryInspection(
                isValidPe = false,
                architecture = "JVM",
                peFormat = "N/A",
                subsystem = "N/A",
                sectionsCount = 0,
                statusDetail = "Librería C++20 no cargada en prueba JVM."
            )
        }
        val parts = nativeInspectPeBytes(bytes).split("\n")
        return PeBinaryInspection(
            isValidPe = parts.getOrNull(0) == "1",
            architecture = parts.getOrNull(1) ?: "N/A",
            peFormat = parts.getOrNull(2) ?: "N/A",
            subsystem = parts.getOrNull(3) ?: "N/A",
            sectionsCount = parts.getOrNull(4)?.toIntOrNull() ?: 0,
            statusDetail = parts.getOrNull(5) ?: ""
        )
    }

    fun resetStats() {
        if (isNativeLoaded) nativeResetStats()
    }

    private fun fallbackNormalizeForHostTests(rawInput: String, searchEngineId: Int): NormalizedInputResult {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty() || trimmed.equals("vectra://home", ignoreCase = true)) {
            return NormalizedInputResult("vectra://home", "vectra://home", false, 0)
        }
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return NormalizedInputResult(trimmed, extractHost(trimmed), false, 0)
        }
        val looksLikeDomain = !trimmed.contains(" ") && trimmed.contains(".")
        return if (looksLikeDomain) {
            val url = "https://$trimmed"
            NormalizedInputResult(url, extractHost(url), false, 0)
        } else {
            val encoded = java.net.URLEncoder.encode(trimmed, "UTF-8")
            val searchUrl = when (searchEngineId) {
                1 -> "https://duckduckgo.com/?q=$encoded"
                2 -> "https://search.brave.com/search?q=$encoded"
                3 -> "https://es.wikipedia.org/w/index.php?search=$encoded"
                else -> "https://www.google.com/search?q=$encoded"
            }
            NormalizedInputResult(searchUrl, extractHost(searchUrl), true, 0)
        }
    }
}
