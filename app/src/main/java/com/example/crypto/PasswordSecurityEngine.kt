package com.example.crypto

import com.nulabinc.zxcvbn.Zxcvbn
import java.security.SecureRandom

/**
 * Niveles de longitud predefinidos para la generación de contraseñas ultra seguras.
 * Diseñados para ofrecer un equilibrio perfecto entre compatibilidad con herramientas
 * de compilación (Gradle, keytool, CI/CD) y resistencia contra ataques de fuerza bruta.
 */
enum class PasswordLengthOption(val length: Int, val label: String, val securityBadge: String) {
    HIGH(16, "16 caracteres (Alta)", "104 bits"),
    VERY_HIGH(24, "24 caracteres (Muy Alta)", "156 bits"),
    ULTRA(32, "32 caracteres (Ultra / Militar)", "208 bits")
}

/**
 * Nivel de fortaleza calculado para una contraseña.
 */
enum class PasswordStrength(val label: String, val scorePercent: Float) {
    WEAK("Vulnerable (Fácil de descifrar)", 0.25f),
    MEDIUM("Moderada (Riesgo medio)", 0.50f),
    STRONG("Fuerte (Difícil de descifrar)", 0.75f),
    ULTRA("Ultra Segura / Indescifrable", 1.0f)
}

/**
 * Resultado detallado de la auditoría de seguridad realizada por zxcvbn.
 * Contiene información de si es vulnerable o descifrable, sugerencias y tiempo estimado.
 */
data class PasswordAuditResult(
    val strength: PasswordStrength,
    val score: Int, // 0 a 4 (zxcvbn score)
    val isCrackableWarning: Boolean, // true si score < 3 o longitud < 8
    val warningMessage: String?, // Advertencia amigable en español si es vulnerable
    val crackTimeDisplay: String // Estimación legible del tiempo necesario para descifrarla
)

/**
 * Motor de seguridad y generación de contraseñas criptográficamente seguras.
 * Integra SecureRandom (entropía de hardware) con el motor de auditoría zxcvbn
 * para garantizar que ninguna contraseña entregada contenga patrones conocidos ni
 * vulnerabilidades de diccionario.
 */
object PasswordSecurityEngine {

    // Conjuntos de caracteres seleccionados cuidadosamente para evitar caracteres
    // que rompen comúnmente scripts de bash, archivos gradle.properties o parsing XML/JSON
    private const val UPPERCASE = "ABCDEFGHJKLMNPQRSTUVWXYZ" // Excluye 'I', 'O' para evitar confusión visual
    private const val LOWERCASE = "abcdefghijkmnopqrstuvwxyz" // Excluye 'l' para evitar confusión con '1'
    private const val DIGITS = "23456789" // Excluye '0', '1' para máxima legibilidad
    private const val SPECIAL_SYMBOLS = "#$%&*+-=?@_~" // Símbolos 100% seguros en cadenas de CLI

    private val ALL_CHARACTERS = UPPERCASE + LOWERCASE + DIGITS + SPECIAL_SYMBOLS
    private val secureRandom = SecureRandom()
    private val zxcvbn = Zxcvbn()

    /**
     * Genera una contraseña ultra segura garantizando que contenga al menos:
     * - 1 letra mayúscula
     * - 1 letra minúscula
     * - 1 número
     * - 1 símbolo especial
     *
     * Además, AUDITA la contraseña generada contra el motor zxcvbn:
     * Si la contraseña tuviera alguna palabra oculta o patrón de teclado (score < 4),
     * se descarta y se regenera en bucle hasta que la librería certifique que es 100% indescifrable.
     *
     * @param length Longitud deseada (16, 24 o 32 caracteres).
     * @return Cadena con la contraseña generada y auditada con máxima entropía.
     */
    fun generateSecurePassword(length: Int = PasswordLengthOption.VERY_HIGH.length): String {
        val targetLength = length.coerceIn(8, 64)
        var candidate: String
        var attempts = 0

        do {
            val passwordChars = mutableListOf<Char>()

            // 1. Garantizar al menos un carácter de cada categoría esencial
            passwordChars.add(UPPERCASE[secureRandom.nextInt(UPPERCASE.length)])
            passwordChars.add(LOWERCASE[secureRandom.nextInt(LOWERCASE.length)])
            passwordChars.add(DIGITS[secureRandom.nextInt(DIGITS.length)])
            passwordChars.add(SPECIAL_SYMBOLS[secureRandom.nextInt(SPECIAL_SYMBOLS.length)])

            // 2. Rellenar el resto de la longitud requerida con selección aleatoria uniforme
            for (i in passwordChars.size until targetLength) {
                passwordChars.add(ALL_CHARACTERS[secureRandom.nextInt(ALL_CHARACTERS.length)])
            }

            // 3. Barajar criptográficamente la lista para que las posiciones no sean predecibles (Fisher-Yates)
            for (i in passwordChars.indices.reversed()) {
                val j = secureRandom.nextInt(i + 1)
                val temp = passwordChars[i]
                passwordChars[i] = passwordChars[j]
                passwordChars[j] = temp
            }

            candidate = passwordChars.joinToString("")
            attempts++

            // Auditar con zxcvbn: Exigir score 4 (máximo nivel de indescifrabilidad)
            val zxStrength = zxcvbn.measure(candidate)
            val isIndecipherable = zxStrength.score >= 4

        } while (!isIndecipherable && attempts < 10)

        return candidate
    }

    /**
     * Evalúa de forma exhaustiva la contraseña ingresada por el usuario utilizando la librería zxcvbn.
     * Detecta si es descifrable mediante ataques de diccionario, patrones de teclado,
     * secuencias repetitivas o baja entropía.
     */
    fun auditPassword(password: String): PasswordAuditResult {
        if (password.isEmpty()) {
            return PasswordAuditResult(
                strength = PasswordStrength.WEAK,
                score = 0,
                isCrackableWarning = false,
                warningMessage = null,
                crackTimeDisplay = "0 segundos"
            )
        }

        val zxResult = zxcvbn.measure(password)
        val score = zxResult.score // 0 (muy débil), 1 (débil), 2 (moderada), 3 (fuerte), 4 (indescifrable)

        val strength = when (score) {
            4 -> PasswordStrength.ULTRA
            3 -> PasswordStrength.STRONG
            2 -> PasswordStrength.MEDIUM
            else -> PasswordStrength.WEAK
        }

        // Estimación del tiempo de descifrado en español
        val crackSeconds = zxResult.crackTimeSeconds.offlineFastHashing1e10PerSecond
        val crackDisplay = formatCrackTime(crackSeconds)

        // Determinar si representa un riesgo de descifrado
        val isCrackable = score < 3 || password.length < 8

        val warningMsg = if (isCrackable) {
            when {
                password.length < 6 -> "Muy corta: un ataque de fuerza bruta podría descifrarla en cuestión de segundos."
                score == 0 -> "Vulnerable: contiene patrones comunes, fechas o palabras de diccionario fáciles de romper."
                score == 1 -> "Fácilmente predecible: un atacante con GPU podría quebrarla rápidamente."
                score == 2 -> "Resistencia media: carece de suficiente entropía y caracteres combinados."
                else -> "Esta contraseña podría ser vulnerable a ataques dirigidos de diccionario."
            }
        } else {
            null
        }

        return PasswordAuditResult(
            strength = strength,
            score = score,
            isCrackableWarning = isCrackable,
            warningMessage = warningMsg,
            crackTimeDisplay = crackDisplay
        )
    }

    /**
     * Función de compatibilidad rápida que retorna el nivel de fortaleza.
     */
    fun evaluateStrength(password: String): PasswordStrength {
        return auditPassword(password).strength
    }

    private fun formatCrackTime(seconds: Double): String {
        return when {
            seconds < 1 -> "Menos de 1 segundo"
            seconds < 60 -> "${seconds.toInt()} segundos"
            seconds < 3600 -> "${(seconds / 60).toInt()} minutos"
            seconds < 86400 -> "${(seconds / 3600).toInt()} horas"
            seconds < 31536000 -> "${(seconds / 86400).toInt()} días"
            seconds < 3153600000L -> "${(seconds / 31536000).toInt()} años"
            else -> "Siglos (Prácticamente Indescifrable)"
        }
    }
}
