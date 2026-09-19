package com.example

import com.example.crypto.PasswordLengthOption
import com.example.crypto.PasswordSecurityEngine
import com.example.crypto.PasswordStrength
import com.example.crypto.SecureCredentialsCipher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

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
}
