package com.acurast.attested.executor.utils

import com.acurast.attested.executor.Constants
import it.airgap.tezos.rpc.http.HttpClientProvider
import it.airgap.tezos.rpc.http.HttpHeader
import it.airgap.tezos.rpc.http.HttpParameter
import kotlinx.coroutines.suspendCancellableCoroutine
import org.apache.http.HttpException
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.cert.Certificate
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.net.ssl.HttpsURLConnection
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class Networking {
    companion object {
        val executor: ExecutorService = Executors.newFixedThreadPool(10)
        fun httpsRequest(
            url: URL,
            method: String,
            body: ByteArray = ByteArray(0),
            headers: Map<String, String>,
            successCallback: (String, ByteArray) -> Unit,
            errorCallback: (Exception) -> Unit,
            connectTimeout: Int = Constants.HTTP_CONNECT_TIMEOUT,
            readTimeout: Int = Constants.HTTP_CONNECT_TIMEOUT
        ) {
            executor.submit {
                val urlConnection: HttpsURLConnection = url.openConnection() as HttpsURLConnection
                try {
                    urlConnection.requestMethod = method
                    urlConnection.connectTimeout = connectTimeout
                    urlConnection.readTimeout = readTimeout

                    for (header in headers.entries) {
                        urlConnection.setRequestProperty(header.key, header.value)
                    }

                    if (body.isNotEmpty()) {
                        urlConnection.doOutput = true
                        urlConnection.outputStream.write(
                            body,
                            0,
                            body.size
                        )
                        urlConnection.outputStream.close()
                    }

                    if (urlConnection.responseCode !in 200 until 300) {
                        val errMsg = urlConnection.errorStream.bufferedReader().readText()
                        throw HttpException("HTTP $method failed with $errMsg (${urlConnection.responseCode})")
                    }

                    val payload = urlConnection.inputStream.bufferedReader().readText()
                    val certificateSha256 =
                        getCertificatePin(urlConnection.serverCertificates.first())

                    urlConnection.inputStream.close()
                    successCallback(payload, certificateSha256)
                } catch (exception: Exception) {
                    errorCallback(exception)
                } finally {
                    urlConnection.disconnect()
                }
            }
        }

        fun httpsPostString(
            url: URL,
            string: String,
            headers: Map<String, String>,
            success: (String, ByteArray) -> Unit,
            error: (Exception) -> Unit,
            connectTimeout: Int = Constants.HTTP_CONNECT_TIMEOUT,
            readTimeout: Int = Constants.HTTP_CONNECT_TIMEOUT
        ) {
            httpsRequest(url, "POST", string.toByteArray(), headers, success, error, connectTimeout, readTimeout)
        }

        fun httpsGetString(
            url: URL,
            headers: Map<String, String>,
            success: (String, ByteArray) -> Unit,
            error: (Exception) -> Unit,
            connectTimeout: Int = Constants.HTTP_CONNECT_TIMEOUT,
            readTimeout: Int = Constants.HTTP_CONNECT_TIMEOUT
        ) {
            httpsRequest(url, "GET", ByteArray(0), headers, success, error, connectTimeout, readTimeout)
        }

        fun getCertificatePin(certificate: Certificate): ByteArray {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(certificate.publicKey.encoded)
            return digest
        }
    }

    class HttpClient : HttpClientProvider {
        override suspend fun delete(
            baseUrl: String,
            endpoint: String,
            headers: List<HttpHeader>,
            parameters: List<HttpParameter>,
            requestTimeout: Long?,
            connectionTimeout: Long?,
        ): String = call(HttpMethod.Delete, baseUrl, endpoint, headers, parameters, requestTimeout = requestTimeout, connectionTimeout = connectionTimeout)

        override suspend fun get(
            baseUrl: String,
            endpoint: String,
            headers: List<HttpHeader>,
            parameters: List<HttpParameter>,
            requestTimeout: Long?,
            connectionTimeout: Long?,
        ): String = call(HttpMethod.Get, baseUrl, endpoint, headers, parameters, requestTimeout = requestTimeout, connectionTimeout = connectionTimeout)

        override suspend fun patch(
            baseUrl: String,
            endpoint: String,
            headers: List<HttpHeader>,
            parameters: List<HttpParameter>,
            body: String?,
            requestTimeout: Long?,
            connectionTimeout: Long?,
        ): String = call(HttpMethod.Patch, baseUrl, endpoint, headers, parameters, body, requestTimeout, connectionTimeout)

        override suspend fun post(
            baseUrl: String,
            endpoint: String,
            headers: List<HttpHeader>,
            parameters: List<HttpParameter>,
            body: String?,
            requestTimeout: Long?,
            connectionTimeout: Long?,
        ): String = call(HttpMethod.Post, baseUrl, endpoint, headers, parameters, body, requestTimeout, connectionTimeout)

        override suspend fun put(
            baseUrl: String,
            endpoint: String,
            headers: List<HttpHeader>,
            parameters: List<HttpParameter>,
            body: String?,
            requestTimeout: Long?,
            connectionTimeout: Long?,
        ): String = call(HttpMethod.Put, baseUrl, endpoint, headers, parameters, body, requestTimeout, connectionTimeout)

        private suspend fun call(
            method: HttpMethod,
            baseUrl: String,
            endpoint: String,
            headers: List<HttpHeader>,
            parameters: List<HttpParameter>,
            body: String? = null,
            requestTimeout: Long? = null,
            connectionTimeout: Long? = null,
        ): String = suspendCancellableCoroutine { continuation ->
            Networking.httpsRequest(
                url(baseUrl, endpoint, parameters),
                method.value,
                body = body?.toByteArray() ?: byteArrayOf(),
                headers = headers.associate { Pair(it.first, it.second ?: "") },
                successCallback = { response, _ -> continuation.resume(response) },
                errorCallback = { exception -> continuation.resumeWithException(exception) },
                connectTimeout = connectionTimeout?.toInt() ?: Constants.HTTP_CONNECT_TIMEOUT,
                readTimeout = requestTimeout?.toInt() ?: Constants.HTTP_CONNECT_TIMEOUT,
            )
        }

        private fun url(baseUrl: String, endpoint: String, parameters: List<HttpParameter>): URL {
            val query = parameters.takeIf { it.isNotEmpty() }?.joinToString("&", prefix = "?") {
                if (it.second != null) "${it.first}=${URLEncoder.encode(it.second, "utf-8")}"
                else it.first
            } ?: ""

            val path = "${baseUrl}/${endpoint.trimStart('/')}".trim('/')

            return URL("${path}${query}")
        }

        private enum class HttpMethod(val value: String) {
            Delete("DELETE"),
            Get("GET"),
            Patch("PATCH"),
            Post("POST"),
            Put("PUT"),
        }
    }
}