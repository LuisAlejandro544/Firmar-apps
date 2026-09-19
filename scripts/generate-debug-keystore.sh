#!/usr/bin/env bash
# ==============================================================================
# Script de Generación Forzada de Firma Keystore de Depuración (debug.keystore)
# ==============================================================================
# Propósito:
# Este script obliga al entorno de compilación (local o GitHub Actions CI/CD)
# a generar una nueva firma criptográfica desde cero en formato PKCS12 / JKS.
# Elimina cualquier firma previa o temporal para evitar la reutilización de
# claves heredadas o estados en caché, garantizando una compilación 100% limpia.
# ==============================================================================

set -euo pipefail

echo "======================================================================"
echo "🔐 INICIANDO GENERACIÓN FORZADA DE FIRMA DEBUG DESDE CERO"
echo "======================================================================"

# Parámetros estándar para la firma de depuración de Android (debugConfig)
KEYSTORE_FILE="debug.keystore"
KEY_ALIAS="androiddebugkey"
STORE_PASS="android"
KEY_PASS="android"
KEY_SIZE=2048
VALIDITY_DAYS=10000
KEY_ALG="RSA"
STORE_TYPE="PKCS12"
DNAME="CN=Android Debug, OU=Debug Build, O=Crypto Lab Mobile, C=US"

# Paso 1: Eliminar cualquier firma previa si existiera en la raíz o en el módulo app
echo "🧹 Limpiando firmas previas o temporales..."
if [ -f "$KEYSTORE_FILE" ]; then
    echo "  -> Eliminando $KEYSTORE_FILE existente..."
    rm -f "$KEYSTORE_FILE"
fi

if [ -f "app/$KEYSTORE_FILE" ]; then
    echo "  -> Eliminando app/$KEYSTORE_FILE existente..."
    rm -f "app/$KEYSTORE_FILE"
fi

# Paso 2: Verificar la disponibilidad de keytool (JDK de Java)
if ! command -v keytool &> /dev/null; then
    echo "❌ ERROR: 'keytool' no se encuentra en el PATH del sistema."
    echo "   Asegúrate de tener instalado Java JDK (ej. OpenJDK 17 o Temurin)."
    exit 1
fi

echo "⚙️  Generando nuevo par de claves asimétricas RSA de $KEY_SIZE bits..."
echo "    Alias: $KEY_ALIAS"
echo "    Validez: $VALIDITY_DAYS días"
echo "    Formato: $STORE_TYPE"

# Paso 3: Generar el archivo debug.keystore desde cero
keytool -genkeypair \
    -v \
    -keystore "$KEYSTORE_FILE" \
    -storepass "$STORE_PASS" \
    -alias "$KEY_ALIAS" \
    -keypass "$KEY_PASS" \
    -keyalg "$KEY_ALG" \
    -keysize "$KEY_SIZE" \
    -validity "$VALIDITY_DAYS" \
    -storetype "$STORE_TYPE" \
    -dname "$DNAME"

# Paso 4: Validar la integridad del archivo generado
if [ ! -f "$KEYSTORE_FILE" ] || [ ! -s "$KEYSTORE_FILE" ]; then
    echo "❌ ERROR CRÍTICO: El archivo $KEYSTORE_FILE no se generó o está vacío."
    exit 1
fi

FILE_SIZE=$(wc -c < "$KEYSTORE_FILE")
echo "✅ Firma generada exitosamente: $KEYSTORE_FILE ($FILE_SIZE bytes)."

# Paso 5: Extraer y mostrar huellas digitales forenses en el log
echo "----------------------------------------------------------------------"
echo "📜 HUELLAS FORENSES DEL CERTIFICADO DEBUG GENERADO:"
echo "----------------------------------------------------------------------"
keytool -list -v -keystore "$KEYSTORE_FILE" -storepass "$STORE_PASS" -alias "$KEY_ALIAS" | grep -E "SHA256:|SHA1:|MD5:" || true
echo "----------------------------------------------------------------------"
echo "🎉 LISTO: El pipeline puede compilar el APK Debug con esta firma única."
echo "======================================================================"
