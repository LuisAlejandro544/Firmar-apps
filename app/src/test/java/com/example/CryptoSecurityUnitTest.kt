package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.crypto.CertificateExportHelper
import com.example.crypto.CertificateFormat
import com.example.crypto.KeystoreGenerator
import com.example.crypto.KeystoreParams
import com.example.crypto.PasswordLengthOption
import com.example.crypto.PasswordSecurityEngine
import com.example.crypto.PasswordStrength
import com.example.crypto.SecureCredentialsCipher
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

/**
 * Pruebas unitarias para el motor de generación de contraseñas ultra seguras
 * y el cifrado/descifrado de credenciales en reposo.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CryptoSecurityUnitTest {

    @Test
    fun `password generator respects all three length options`() {
        val pass16 = PasswordSecurityEngine.generateSecurePassword(PasswordLengthOption.HIGH.length)
        val pass24 = PasswordSecurityEngine.generateSecurePassword(PasswordLengthOption.VERY_HIGH.length)
        val pass32 = PasswordSecurityEngine.generateSecurePassword(PasswordLengthOption.ULTRA.length)

        assertEquals(16, pass16.length)
        assertEquals(24, pass24.length)
        assertEquals(32, pass32.length)
    }

    @Test
    fun `generated passwords contain required character classes`() {
        // Verificar 20 contraseñas aleatorias consecutivas
        for (i in 1..20) {
            val pass = PasswordSecurityEngine.generateSecurePassword(24)
            assertTrue("Debe contener al menos una mayúscula", pass.any { it.isUpperCase() })
            assertTrue("Debe contener al menos una minúscula", pass.any { it.isLowerCase() })
            assertTrue("Debe contener al menos un dígito", pass.any { it.isDigit() })
            assertTrue("Debe contener al menos un carácter especial", pass.any { !it.isLetterOrDigit() })
        }
    }

    @Test
    fun `password strength evaluation works accurately`() {
        val weak = PasswordSecurityEngine.evaluateStrength("12345")
        assertEquals(PasswordStrength.WEAK, weak)

        val ultra = PasswordSecurityEngine.generateSecurePassword(24)
        val strength = PasswordSecurityEngine.evaluateStrength(ultra)
        assertTrue(strength == PasswordStrength.ULTRA || strength == PasswordStrength.STRONG)
    }

    @Test
    fun `secure credentials cipher encrypts and decrypts correctly`() {
        val originalPassword = "MyUltraSecurePassword#2026!"
        val encrypted = SecureCredentialsCipher.encrypt(originalPassword)

        assertTrue(encrypted.startsWith("enc:v1:"))
        assertNotEquals(originalPassword, encrypted)

        val decrypted = SecureCredentialsCipher.decrypt(encrypted)
        assertEquals(originalPassword, decrypted)
    }

    @Test
    fun `secure credentials cipher maintains backwards compatibility with legacy plaintext`() {
        val legacyPassword = "legacy_unencrypted_password"
        // Si no tiene prefijo enc:v1:, decrypt lo retorna intacto
        val decrypted = SecureCredentialsCipher.decrypt(legacyPassword)
        assertEquals(legacyPassword, decrypted)
    }

    @Test
    fun `zxcvbn audit detects crackable passwords and warns accurately`() {
        // Contraseñas comunes que un hacker rompería en segundos
        val vulnerableAudit = PasswordSecurityEngine.auditPassword("password123")
        assertTrue("password123 debe ser clasificada como crackable", vulnerableAudit.isCrackableWarning)
        assertTrue(vulnerableAudit.score <= 1)
        assertTrue(vulnerableAudit.warningMessage != null)

        val qwertyAudit = PasswordSecurityEngine.auditPassword("qwerty12345")
        assertTrue("qwerty12345 debe ser clasificada como crackable", qwertyAudit.isCrackableWarning)

        // Contraseña ultra segura generada y certificada por zxcvbn
        val ultraPass = PasswordSecurityEngine.generateSecurePassword(24)
        val safeAudit = PasswordSecurityEngine.auditPassword(ultraPass)
        assertEquals("Contraseña generada debe obtener score 4 (máximo)", 4, safeAudit.score)
        assertTrue("No debe tener advertencia de crackeo", !safeAudit.isCrackableWarning)
    }

    @Test
    fun `certificate export extracts valid X509 certificate and formats correctly`() = runBlocking {
        val context: Context = ApplicationProvider.getApplicationContext()
        val params = KeystoreParams(
            title = "Test Cert Keystore",
            fileName = "test_cert_export.jks",
            alias = "test_alias",
            storePassword = "StorePassword#2026",
            keyPassword = "KeyPassword#2026",
            keySize = 2048,
            validityYears = 25,
            commonName = "Test Developer",
            organization = "Test Org",
            countryCode = "ES"
        )

        val keystoreEntity = KeystoreGenerator.generateKeystore(context, params).getOrThrow()

        // 1. Extraer certificado X.509
        val certResult = CertificateExportHelper.extractCertificate(keystoreEntity)
        assertTrue("La extracción del certificado debe ser exitosa", certResult.isSuccess)
        val cert = certResult.getOrThrow()
        assertNotNull("El certificado no debe ser nulo", cert)
        assertEquals("X.509", cert.type)

        // 2. Formato PEM
        val pemText = CertificateExportHelper.formatAsPem(cert)
        assertTrue("PEM debe iniciar con el delimitador estándar", pemText.startsWith("-----BEGIN CERTIFICATE-----"))
        assertTrue("PEM debe terminar con el delimitador estándar", pemText.endsWith("-----END CERTIFICATE-----"))

        // Verificar que el PEM sea decodificable por CertificateFactory estándar
        val certFactory = CertificateFactory.getInstance("X.509")
        val parsedCertFromPem = certFactory.generateCertificate(ByteArrayInputStream(pemText.toByteArray(Charsets.UTF_8))) as X509Certificate
        assertEquals(cert.serialNumber, parsedCertFromPem.serialNumber)

        // 3. Formato DER y CRT
        val derBytes = CertificateExportHelper.formatAsDer(cert)
        assertTrue("Los bytes DER deben tener longitud mayor a cero", derBytes.isNotEmpty())
        val parsedCertFromDer = certFactory.generateCertificate(ByteArrayInputStream(derBytes)) as X509Certificate
        assertEquals(cert.serialNumber, parsedCertFromDer.serialNumber)

        // 4. Nombre de archivo sugerido
        assertEquals("test_alias_cert.pem", CertificateExportHelper.getSuggestedFileName(keystoreEntity, CertificateFormat.PEM))
        assertEquals("test_alias_cert.crt", CertificateExportHelper.getSuggestedFileName(keystoreEntity, CertificateFormat.CRT))
        assertEquals("test_alias_cert.der", CertificateExportHelper.getSuggestedFileName(keystoreEntity, CertificateFormat.DER))
    }

    @Test
    fun `zip import helper validates keystore and extracts metadata correctly`() = runBlocking {
        val context: Context = ApplicationProvider.getApplicationContext()
        val params = KeystoreParams(
            title = "Import Test Keystore",
            fileName = "import_test.jks",
            alias = "my_import_alias",
            storePassword = "StorePassword#2026",
            keyPassword = "KeyPassword#2026",
            keySize = 2048,
            validityYears = 20,
            commonName = "Imported Developer",
            organization = "Imported Org",
            countryCode = "ES"
        )

        val keystoreEntity = KeystoreGenerator.generateKeystore(context, params).getOrThrow()
        val jksFile = java.io.File(keystoreEntity.filePath)
        assertTrue(jksFile.exists())

        // 1. Validación exitosa con credenciales correctas
        val validationResult = com.example.crypto.ZipImportHelper.validateAndInspectKeystore(
            jksFile = jksFile,
            storePassword = "StorePassword#2026",
            alias = "my_import_alias",
            keyPassword = "KeyPassword#2026"
        )
        assertTrue("La validación de la keystore debe ser exitosa", validationResult.isSuccess)
        val validation = validationResult.getOrThrow()
        assertEquals("my_import_alias", validation.validatedAlias)
        assertEquals("Imported Developer", validation.commonName)
        assertEquals("Imported Org", validation.organization)
        assertEquals("ES", validation.countryCode)
        assertTrue("SHA-256 debe estar calculado", validation.sha256Fingerprint.isNotEmpty())
        assertTrue("SHA-1 debe estar calculado", validation.sha1Fingerprint.isNotEmpty())
        assertEquals(keystoreEntity.sha256Fingerprint, validation.sha256Fingerprint)

        // 2. Rechazo con contraseña errónea del almacén
        val wrongStorePassResult = com.example.crypto.ZipImportHelper.validateAndInspectKeystore(
            jksFile = jksFile,
            storePassword = "WrongPassword#999",
            alias = "my_import_alias",
            keyPassword = "KeyPassword#2026"
        )
        assertTrue("Debe fallar con contraseña de almacén incorrecta", wrongStorePassResult.isFailure)

        // 3. Rechazo con contraseña errónea de la clave privada
        val wrongKeyPassResult = com.example.crypto.ZipImportHelper.validateAndInspectKeystore(
            jksFile = jksFile,
            storePassword = "StorePassword#2026",
            alias = "my_import_alias",
            keyPassword = "WrongKeyPass#999"
        )
        assertTrue("Debe fallar con contraseña de clave incorrecta", wrongKeyPassResult.isFailure)
    }
}
