package com.example.ui.debug

import android.app.Application
import android.os.Build
import android.os.Process
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crypto.KeystoreGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import java.security.Signature
import java.security.cert.X509Certificate
import java.util.Date

/**
 * Métricas obtenidas durante el benchmark de un tamaño de clave específico.
 */
data class KeyBenchmarkMetric(
    val keySizeBits: Int,
    val entropyTimeMs: Long,
    val keyGenTimeMs: Long,
    val certGenTimeMs: Long,
    val pkcs12StoreTimeMs: Long,
    val signVerifyTimeMs: Long,
    val totalTimeMs: Long
)

/**
 * Información de telemetría de hardware del dispositivo móvil.
 */
data class DeviceHardwareTelemetry(
    val deviceName: String,
    val androidVersion: String,
    val primaryAbi: String,
    val supportedAbis: List<String>,
    val is64BitProcess: Boolean,
    val cpuCores: Int,
    val maxMemoryMb: Long,
    val usedMemoryMb: Long
)

/**
 * Resultado integral del Benchmark Criptográfico.
 */
data class BenchmarkResult(
    val telemetry: DeviceHardwareTelemetry,
    val rsa2048: KeyBenchmarkMetric,
    val rsa4096: KeyBenchmarkMetric,
    val ratio4096Vs2048: Float,
    val performanceRating: String,
    val technicalSummary: String
)

sealed interface BenchmarkUiState {
    data object Idle : BenchmarkUiState
    data class Running(val currentPhase: String) : BenchmarkUiState
    data class Completed(val result: BenchmarkResult) : BenchmarkUiState
    data class Error(val message: String) : BenchmarkUiState
}

/**
 * ViewModel encargado de evaluar el rendimiento del procesador móvil (32-bit vs 64-bit)
 * ejecutando operaciones criptográficas reales (RSA, X.509, PKCS12 y Firma Digital).
 */
class CryptoBenchmarkViewModel(application: Application) : AndroidViewModel(application) {

    private val _telemetry = MutableStateFlow(gatherDeviceTelemetry())
    val telemetry: StateFlow<DeviceHardwareTelemetry> = _telemetry.asStateFlow()

    private val _benchmarkState = MutableStateFlow<BenchmarkUiState>(BenchmarkUiState.Idle)
    val benchmarkState: StateFlow<BenchmarkUiState> = _benchmarkState.asStateFlow()

    /**
     * Ejecuta el benchmark de rendimiento criptográfico completo midiendo RSA 2048 vs RSA 4096 bits.
     */
    fun startBenchmark() {
        viewModelScope.launch {
            _benchmarkState.value = BenchmarkUiState.Running("Iniciando benchmark criptográfico...")
            try {
                val currentTelemetry = gatherDeviceTelemetry()
                _telemetry.value = currentTelemetry

                val result = withContext(Dispatchers.Default) {
                    // Benchmark RSA 2048 bits
                    _benchmarkState.value = BenchmarkUiState.Running("Evaluando RSA 2048 bits (Entropía & Claves)...")
                    val metric2048 = runKeyBenchmark(2048) { phase ->
                        _benchmarkState.value = BenchmarkUiState.Running("RSA 2048: $phase")
                    }

                    // Benchmark RSA 4096 bits
                    _benchmarkState.value = BenchmarkUiState.Running("Evaluando RSA 4096 bits (Cálculo intensivo)...")
                    val metric4096 = runKeyBenchmark(4096) { phase ->
                        _benchmarkState.value = BenchmarkUiState.Running("RSA 4096: $phase")
                    }

                    val ratio = if (metric2048.totalTimeMs > 0) {
                        metric4096.totalTimeMs.toFloat() / metric2048.totalTimeMs.toFloat()
                    } else 1.0f

                    val rating = when {
                        metric2048.totalTimeMs < 300 -> "Excelente (Rendimiento de Gama Alta)"
                        metric2048.totalTimeMs < 800 -> "Óptimo (Rendimiento Fluido)"
                        metric2048.totalTimeMs < 2000 -> "Adecuado (Dispositivo Estándar)"
                        else -> "Bajo (Limitación de Hardware o CPU ocupada)"
                    }

                    val archNote = if (currentTelemetry.is64BitProcess) {
                        "El proceso se ejecuta en 64 bits nativos (${currentTelemetry.primaryAbi}), aprovechando registros ampliados e instrucciones vectoriales para aritmética de números grandes."
                    } else {
                        "El proceso se ejecuta en 32 bits (${currentTelemetry.primaryAbi}). La app opera con compatibilidad total gracias a la biblioteca Bouncy Castle optimizada."
                    }

                    BenchmarkResult(
                        telemetry = currentTelemetry,
                        rsa2048 = metric2048,
                        rsa4096 = metric4096,
                        ratio4096Vs2048 = ratio,
                        performanceRating = rating,
                        technicalSummary = archNote
                    )
                }

                _benchmarkState.value = BenchmarkUiState.Completed(result)
            } catch (e: Exception) {
                _benchmarkState.value = BenchmarkUiState.Error("Error en benchmark: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    private fun runKeyBenchmark(keySizeBits: Int, onPhase: (String) -> Unit): KeyBenchmarkMetric {
        // 1. Entropía SecureRandom
        onPhase("Generación de entropía")
        val t0 = System.currentTimeMillis()
        val random = SecureRandom()
        val seedBytes = ByteArray(32)
        random.nextBytes(seedBytes)
        val entropyTime = System.currentTimeMillis() - t0

        // 2. Generación del par de claves RSA
        onPhase("Generación de par de claves RSA $keySizeBits bits")
        val t1 = System.currentTimeMillis()
        val keyPairGen = KeyPairGenerator.getInstance("RSA")
        keyPairGen.initialize(keySizeBits, random)
        val keyPair = keyPairGen.generateKeyPair()
        val keyGenTime = System.currentTimeMillis() - t1

        // 3. Generación y auto-firma del certificado X.509
        onPhase("Emisión de certificado X.509 v3")
        val t2 = System.currentTimeMillis()
        val notBefore = Date(System.currentTimeMillis() - 1000L * 60)
        val notAfter = Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 365)
        val subject = X500Name("CN=Benchmark Test, O=Crypto Lab, C=MX")
        val serial = BigInteger(64, random)

        val certBuilder = JcaX509v3CertificateBuilder(
            subject,
            serial,
            notBefore,
            notAfter,
            subject,
            keyPair.public
        )
        val signer = JcaContentSignerBuilder("SHA256withRSA").build(keyPair.private)
        val certHolder = certBuilder.build(signer)
        val cert = org.bouncycastle.cert.jcajce.JcaX509CertificateConverter().getCertificate(certHolder)
        val certGenTime = System.currentTimeMillis() - t2

        // 4. Serialización a PKCS12 en memoria
        onPhase("Serialización y codificación PKCS12")
        val t3 = System.currentTimeMillis()
        val keyStore = KeyStore.getInstance("PKCS12")
        keyStore.load(null, null)
        keyStore.setKeyEntry(
            "bench_alias",
            keyPair.private,
            "benchPass123!".toCharArray(),
            arrayOf<X509Certificate>(cert)
        )
        val baos = ByteArrayOutputStream()
        keyStore.store(baos, "benchStorePass123!".toCharArray())
        val pkcs12StoreTime = System.currentTimeMillis() - t3

        // 5. Verificación de firma real
        onPhase("Firma asimétrica y comprobación")
        val t4 = System.currentTimeMillis()
        val testMessage = "BenchmarkIntegrityPayload".toByteArray(Charsets.UTF_8)
        val sig = Signature.getInstance("SHA256withRSA")
        sig.initSign(keyPair.private)
        sig.update(testMessage)
        val signatureBytes = sig.sign()

        val verifier = Signature.getInstance("SHA256withRSA")
        verifier.initVerify(keyPair.public)
        verifier.update(testMessage)
        val isValid = verifier.verify(signatureBytes)
        if (!isValid) throw IllegalStateException("Fallo en verificación de firma matemática")
        val signVerifyTime = System.currentTimeMillis() - t4

        val totalTime = entropyTime + keyGenTime + certGenTime + pkcs12StoreTime + signVerifyTime

        return KeyBenchmarkMetric(
            keySizeBits = keySizeBits,
            entropyTimeMs = entropyTime,
            keyGenTimeMs = keyGenTime,
            certGenTimeMs = certGenTime,
            pkcs12StoreTimeMs = pkcs12StoreTime,
            signVerifyTimeMs = signVerifyTime,
            totalTimeMs = totalTime
        )
    }

    private fun gatherDeviceTelemetry(): DeviceHardwareTelemetry {
        val runtime = Runtime.getRuntime()
        val is64Bit = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Process.is64Bit()
        } else {
            Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()
        }

        val primaryAbi = if (Build.SUPPORTED_ABIS.isNotEmpty()) Build.SUPPORTED_ABIS[0] else "Desconocido"
        val supportedList = Build.SUPPORTED_ABIS.toList()

        val maxMem = runtime.maxMemory() / (1024 * 1024)
        val usedMem = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)

        return DeviceHardwareTelemetry(
            deviceName = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            primaryAbi = primaryAbi,
            supportedAbis = supportedList,
            is64BitProcess = is64Bit,
            cpuCores = runtime.availableProcessors(),
            maxMemoryMb = maxMem,
            usedMemoryMb = usedMem
        )
    }
}
