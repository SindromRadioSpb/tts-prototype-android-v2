package com.sindromradiospb.ttsprototypev2.data.settings

import com.sindromradiospb.ttsprototypev2.core.settings.ProviderCredentialId
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

sealed interface ProviderCredentialMaterial {
    data class ApiKey(val value: String) : ProviderCredentialMaterial
    data class GoogleServiceAccount(
        val projectId: String,
        val privateKey: String,
        val clientEmail: String,
        val tokenUri: String = DefaultTokenUri,
    ) : ProviderCredentialMaterial

    companion object {
        const val DefaultTokenUri = "https://oauth2.googleapis.com/token"
    }
}

object ProviderCredentialParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parseStored(id: ProviderCredentialId, value: String): ProviderCredentialMaterial {
        val trimmed = value.trim()
        return if (trimmed.startsWith("{")) {
            parseJsonCredential(id, trimmed)
        } else {
            ProviderCredentialMaterial.ApiKey(trimmed)
        }
    }

    fun validateJsonForProvider(id: ProviderCredentialId, rawJson: String): ProviderCredentialMaterial {
        require(rawJson.isNotBlank()) { "Credential JSON cannot be blank." }
        require(rawJson.length <= MaxJsonCredentialChars) { "Credential JSON is too long for on-device storage." }
        return parseJsonCredential(id, rawJson.trim())
    }

    private fun parseJsonCredential(id: ProviderCredentialId, rawJson: String): ProviderCredentialMaterial {
        val obj = try {
            json.parseToJsonElement(rawJson).jsonObject
        } catch (error: SerializationException) {
            throw IllegalArgumentException("Credential file is not valid JSON.", error)
        } catch (error: IllegalArgumentException) {
            throw IllegalArgumentException("Credential file is not a JSON object.", error)
        }

        return when (id) {
            ProviderCredentialId.GcpTranslate,
            ProviderCredentialId.GoogleOnlineTts,
            -> obj.toGoogleServiceAccount()

            ProviderCredentialId.GeminiLegacy -> obj.toGeminiApiKey()
        }
    }

    private fun JsonObject.toGoogleServiceAccount(): ProviderCredentialMaterial.GoogleServiceAccount {
        val type = requiredString("type")
        require(type == "service_account") { """Field "type" must be "service_account".""" }
        return ProviderCredentialMaterial.GoogleServiceAccount(
            projectId = requiredString("project_id"),
            privateKey = requiredString("private_key"),
            clientEmail = requiredString("client_email"),
            tokenUri = optionalString("token_uri") ?: ProviderCredentialMaterial.DefaultTokenUri,
        )
    }

    private fun JsonObject.toGeminiApiKey(): ProviderCredentialMaterial.ApiKey {
        val provider = optionalString("provider")
        if (provider != null) {
            require(provider == ProviderCredentialId.GeminiLegacy.wireId) {
                """Field "provider" must be "${ProviderCredentialId.GeminiLegacy.wireId}"."""
            }
        }
        return ProviderCredentialMaterial.ApiKey(requiredString("api_key"))
    }

    private fun JsonObject.requiredString(name: String): String =
        optionalString(name)?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("""Missing or empty credential field: "$name".""")

    private fun JsonObject.optionalString(name: String): String? =
        get(name)?.jsonPrimitive?.contentOrNull?.trim()

    private const val MaxJsonCredentialChars = 64 * 1024
}

