package com.sindromradiospb.ttsprototypev2.data.provider.google

import com.sindromradiospb.ttsprototypev2.core.provider.ProviderErrorCategory
import com.sindromradiospb.ttsprototypev2.core.provider.ProviderException
import com.sindromradiospb.ttsprototypev2.data.settings.ProviderCredentialMaterial
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Instant
import java.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

interface GoogleAccessTokenProvider {
    suspend fun accessToken(
        serviceAccount: ProviderCredentialMaterial.GoogleServiceAccount,
        scope: String,
        providerId: String,
    ): String
}

class JwtGoogleAccessTokenProvider(
    private val clock: () -> Instant = { Instant.now() },
    private val json: Json = Json { ignoreUnknownKeys = true },
) : GoogleAccessTokenProvider {
    override suspend fun accessToken(
        serviceAccount: ProviderCredentialMaterial.GoogleServiceAccount,
        scope: String,
        providerId: String,
    ): String =
        withContext(Dispatchers.IO) {
            val assertion = buildAssertion(serviceAccount, scope)
            val body = "grant_type=${urlEncode(GrantType)}&assertion=${urlEncode(assertion)}"
            val connection = URI(serviceAccount.tokenUri).toURL().openConnection() as HttpURLConnection
            connection.connectTimeout = 20_000
            connection.readTimeout = 20_000
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8")
            try {
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                val responseBody = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                if (status !in 200..299) {
                    throw ProviderException(
                        category = when (status) {
                            400, 401 -> ProviderErrorCategory.InvalidApiKey
                            403 -> ProviderErrorCategory.Unauthorized
                            408 -> ProviderErrorCategory.Timeout
                            429 -> ProviderErrorCategory.QuotaExceeded
                            in 500..599 -> ProviderErrorCategory.ProviderUnavailable
                            else -> ProviderErrorCategory.InvalidResponse
                        },
                        providerId = providerId,
                        userMessage = "Google OAuth token request failed with HTTP $status.",
                    )
                }
                parseAccessToken(responseBody, providerId)
            } finally {
                connection.disconnect()
            }
        }

    private fun buildAssertion(serviceAccount: ProviderCredentialMaterial.GoogleServiceAccount, scope: String): String {
        val now = clock().epochSecond
        val header = json.encodeToString(
            buildJsonObject {
                put("alg", "RS256")
                put("typ", "JWT")
            },
        )
        val claims = json.encodeToString(
            buildJsonObject {
                put("iss", serviceAccount.clientEmail)
                put("scope", scope)
                put("aud", serviceAccount.tokenUri)
                put("iat", now)
                put("exp", now + 3600)
            },
        )
        val signingInput = "${base64Url(header.toByteArray(Charsets.UTF_8))}.${base64Url(claims.toByteArray(Charsets.UTF_8))}"
        val signature = Signature.getInstance("SHA256withRSA")
        signature.initSign(serviceAccount.privateKey.toPrivateKey())
        signature.update(signingInput.toByteArray(Charsets.UTF_8))
        return "$signingInput.${base64Url(signature.sign())}"
    }

    private fun parseAccessToken(body: String, providerId: String): String {
        val token = json.parseToJsonElement(body)
            .jsonObject["access_token"]
            ?.jsonPrimitive
            ?.contentOrNull
            .orEmpty()
        if (token.isBlank()) {
            throw ProviderException(
                category = ProviderErrorCategory.InvalidResponse,
                providerId = providerId,
                userMessage = "Google OAuth token response did not include access_token.",
            )
        }
        return token
    }

    private fun String.toPrivateKey() =
        replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\s".toRegex(), "")
            .let { Base64.getDecoder().decode(it) }
            .let { PKCS8EncodedKeySpec(it) }
            .let { KeyFactory.getInstance("RSA").generatePrivate(it) }

    private fun base64Url(bytes: ByteArray): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    private fun urlEncode(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8.name())

    private companion object {
        const val GrantType = "urn:ietf:params:oauth:grant-type:jwt-bearer"
    }
}

