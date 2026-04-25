package com.sindromradiospb.ttsprototypev2.data.settings

import com.sindromradiospb.ttsprototypev2.core.settings.ProviderCredentialId
import com.sindromradiospb.ttsprototypev2.core.settings.ProviderCredentialStatus

class ProviderSettingsRepository(
    private val secureStore: SecureKeyValueStore,
) {
    fun getStatuses(): List<ProviderCredentialStatus> =
        ProviderCredentialId.entries.map { id ->
            val value = runCatching { secureStore.get(id.storeKey()) }.getOrNull()
            ProviderCredentialStatus(
                id = id,
                isConfigured = !value.isNullOrBlank(),
                maskedValue = value?.let { maskStoredCredential(id, it) },
            )
        }

    fun updateCredential(id: ProviderCredentialId, rawValue: String): ProviderCredentialStatus {
        val value = rawValue.trim()
        validateCredential(value)
        secureStore.put(id.storeKey(), value)
        return ProviderCredentialStatus(id = id, isConfigured = true, maskedValue = maskCredential(value))
    }

    fun attachJsonCredential(id: ProviderCredentialId, rawJson: String): ProviderCredentialStatus {
        val value = rawJson.trim()
        val material = ProviderCredentialParser.validateJsonForProvider(id, value)
        secureStore.put(id.storeKey(), value)
        return ProviderCredentialStatus(id = id, isConfigured = true, maskedValue = maskCredential(material))
    }

    fun validateStoredCredential(id: ProviderCredentialId): ProviderCredentialStatus {
        val value = getCredentialForProvider(id)
            ?: throw IllegalArgumentException("${id.displayName} credential is not configured.")
        val material = ProviderCredentialParser.parseStored(id, value)
        return ProviderCredentialStatus(id = id, isConfigured = true, maskedValue = maskCredential(material))
    }

    fun deleteCredential(id: ProviderCredentialId): ProviderCredentialStatus {
        secureStore.remove(id.storeKey())
        return ProviderCredentialStatus(id = id, isConfigured = false, maskedValue = null)
    }

    fun getCredentialForProvider(id: ProviderCredentialId): String? =
        runCatching { secureStore.get(id.storeKey()) }.getOrNull()

    private fun validateCredential(value: String) {
        require(value.isNotBlank()) { "Credential cannot be blank." }
        require(value.length <= MaxCredentialChars) { "Credential is too long for on-device storage." }
        require(!looksLikeServiceAccountJson(value)) {
            "Raw service account JSON is not accepted in Android v2. Use a restricted API key or future brokered auth flow."
        }
        require(!value.contains('\n') && !value.contains('\r')) {
            "Credential must be a single-line value."
        }
    }

    private fun looksLikeServiceAccountJson(value: String): Boolean =
        value.contains(ServicePrivateKeyJsonField, ignoreCase = true) ||
            value.contains(ServicePrivateKeyHeader, ignoreCase = true) ||
            value.contains(ServiceClientEmailJsonField, ignoreCase = true)

    private fun ProviderCredentialId.storeKey(): String = "provider_credential_$wireId"

    private fun maskStoredCredential(id: ProviderCredentialId, value: String): String =
        runCatching { maskCredential(ProviderCredentialParser.parseStored(id, value)) }
            .getOrElse { maskCredential(value) }

    private companion object {
        const val MaxCredentialChars = 4096
        val ServicePrivateKeyJsonField = "\"private_" + "key\""
        val ServiceClientEmailJsonField = "\"client_" + "email\""
        val ServicePrivateKeyHeader = "BEGIN " + "PRIVATE KEY"
    }
}

fun maskCredential(value: String): String {
    if (value.isBlank()) return ""
    val prefix = value.take(4)
    val suffix = value.takeLast(4).takeIf { value.length > 8 }.orEmpty()
    return if (suffix.isBlank()) {
        "$prefix..."
    } else {
        "$prefix...$suffix"
    }
}

fun maskCredential(material: ProviderCredentialMaterial): String =
    when (material) {
        is ProviderCredentialMaterial.ApiKey -> maskCredential(material.value)
        is ProviderCredentialMaterial.GoogleServiceAccount ->
            "sa:${material.clientEmail.take(3)}...${material.projectId.takeLast(6)}"
    }
