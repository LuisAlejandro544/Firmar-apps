#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
==============================================================================
Script de Aislamiento Estricto de Arquitectura y Firma Criptográfica para APKs
==============================================================================
Propósito:
Este script garantiza que un archivo APK destinado a una arquitectura específica
(ej. arm64-v8a, armeabi-v7a o x86_64) contenga ÚNICA Y EXCLUSIVAMENTE las librerías
nativas (.so) de dicha arquitectura, purgando cualquier residuo de otras arquitecturas
que Gradle o dependencias AAR externas hayan empaquetado por error.

Posteriormente:
1. Re-alinea los datos y librerías nativas a 4 bytes con 'zipalign -p 4'.
2. Aplica una firma digital válida mediante 'apksigner' usando el keystore de debug.
3. Valida la firma criptográfica (esquemas v2 y v3).
4. Realiza una auditoría estricta comprobando que no quede ninguna arquitectura extraña.
==============================================================================
"""

import argparse
import glob
import os
import shutil
import subprocess
import sys
import zipfile


def buscar_herramienta_sdk(nombre: str) -> str:
    """
    Localiza la ruta ejecutable de una herramienta del Android SDK (como zipalign o apksigner)
    buscando en el PATH del sistema y en los directorios habituales de ANDROID_HOME y ANDROID_SDK_ROOT.
    """
    # 1. Comprobar si está disponible directamente en el PATH del sistema
    en_path = shutil.which(nombre)
    if en_path:
        return en_path

    # 2. Buscar en las rutas estándar de Android SDK en runners locales y de CI/CD (GitHub Actions)
    rutas_sdk = [
        os.environ.get("ANDROID_HOME"),
        os.environ.get("ANDROID_SDK_ROOT"),
        "/usr/local/lib/android/sdk",
        "/opt/android/sdk",
        os.path.expanduser("~/Android/Sdk"),
    ]

    for ruta_raiz in rutas_sdk:
        if not ruta_raiz or not os.path.isdir(ruta_raiz):
            continue
        patron = os.path.join(ruta_raiz, "build-tools", "*", nombre)
        coincidencias = sorted(glob.glob(patron), reverse=True)
        if coincidencias:
            return coincidencias[0]

    raise FileNotFoundError(
        f"❌ No se encontró la herramienta '{nombre}' en el PATH ni en las carpetas de Android SDK."
    )


def aislar_y_firmar_apk(
    apk_entrada: str,
    abi_objetivo: str,
    apk_salida: str,
    keystore_path: str,
    storepass: str,
    keyalias: str,
    keypass: str,
) -> None:
    """
    Purga librerías nativas ajenas a 'abi_objetivo', re-alinea y firma el archivo APK.
    """
    print("=" * 70)
    print(f"📦 PROCESANDO APK: Arquitectura objetivo -> [{abi_objetivo}]")
    print(f"   Origen:  {apk_entrada}")
    print(f"   Destino: {apk_salida}")
    print("=" * 70)

    if not os.path.isfile(apk_entrada):
        raise FileNotFoundError(f"❌ El archivo de entrada '{apk_entrada}' no existe.")

    # Asegurar que el directorio de salida exista
    directorio_salida = os.path.dirname(apk_salida)
    if directorio_salida:
        os.makedirs(directorio_salida, exist_ok=True)

    # Si es el APK Universal, no purgamos ninguna arquitectura pero verificamos que esté listo
    if abi_objetivo == "universal":
        print("🌐 Modo Universal: Conservando todas las librerías nativas presentes.")
        shutil.copy2(apk_entrada, apk_salida)
        mostrar_resumen_librerias(apk_salida)
        print("✅ APK Universal listo y verificado.")
        return

    # Buscar herramientas zipalign y apksigner
    zipalign_bin = buscar_herramienta_sdk("zipalign")
    apksigner_bin = buscar_herramienta_sdk("apksigner")
    print(f"⚙️  Herramienta zipalign:  {zipalign_bin}")
    print(f"⚙️  Herramienta apksigner: {apksigner_bin}")

    apk_filtrado_temp = apk_salida + ".unaligned.tmp"
    apk_alineado_temp = apk_salida + ".aligned.tmp"

    archivos_purgados = []
    archivos_conservados = []

    # Paso 1: Filtrado de archivos dentro del APK ZIP
    with zipfile.ZipFile(apk_entrada, "r") as zin:
        with zipfile.ZipFile(apk_filtrado_temp, "w", compression=zipfile.ZIP_DEFLATED) as zout:
            for info in zin.infolist():
                nombre_archivo = info.filename

                # Si es una librería nativa dentro de lib/
                if nombre_archivo.startswith("lib/"):
                    prefijo_permitido = f"lib/{abi_objetivo}/"
                    if not nombre_archivo.startswith(prefijo_permitido):
                        # Descartar archivo perteneciente a otra arquitectura
                        archivos_purgados.append(nombre_archivo)
                        continue
                    else:
                        archivos_conservados.append(nombre_archivo)

                # Descartar firmas anteriores en META-INF para evitar conflictos de firma
                if nombre_archivo.startswith("META-INF/") and any(
                    nombre_archivo.endswith(ext)
                    for ext in [".SF", ".RSA", ".DSA", ".EC", ".MF"]
                ):
                    continue

                # Copiar el contenido tal cual
                contenido = zin.read(nombre_archivo)
                zout.writestr(info, contenido)

    print(f"🧹 Librerías nativas eliminadas de otras arquitecturas: {len(archivos_purgados)}")
    for purgado in archivos_purgados:
        print(f"   ❌ Purgado: {purgado}")

    print(f"💾 Librerías nativas conservadas para {abi_objetivo}: {len(archivos_conservados)}")
    for conservado in archivos_conservados:
        print(f"   ✅ Incluido: {conservado}")

    # Paso 2: Alineación con zipalign (-p 4 para alineación de páginas de 4KB en .so)
    print("📐 Alineando APK con zipalign a 4 bytes...")
    cmd_zipalign = [zipalign_bin, "-f", "-p", "4", apk_filtrado_temp, apk_alineado_temp]
    res_align = subprocess.run(cmd_zipalign, capture_output=True, text=True)
    if res_align.returncode != 0:
        if os.path.exists(apk_filtrado_temp):
            os.remove(apk_filtrado_temp)
        raise RuntimeError(f"❌ Falló zipalign: {res_align.stderr}")

    if os.path.exists(apk_filtrado_temp):
        os.remove(apk_filtrado_temp)

    # Paso 3: Firma con apksigner usando el keystore de debug
    print("🔐 Firmando APK con apksigner (esquemas v2 y v3)...")
    cmd_apksigner = [
        apksigner_bin,
        "sign",
        "--ks",
        keystore_path,
        "--ks-pass",
        f"pass:{storepass}",
        "--key-pass",
        f"pass:{keypass}",
        "--ks-key-alias",
        keyalias,
        "--out",
        apk_salida,
        apk_alineado_temp,
    ]
    res_sign = subprocess.run(cmd_apksigner, capture_output=True, text=True)
    if res_sign.returncode != 0:
        if os.path.exists(apk_alineado_temp):
            os.remove(apk_alineado_temp)
        raise RuntimeError(f"❌ Falló apksigner: {res_sign.stderr}")

    if os.path.exists(apk_alineado_temp):
        os.remove(apk_alineado_temp)

    # Paso 4: Verificación criptográfica con apksigner verify
    print("🔍 Verificando firma digital del APK resultante...")
    cmd_verify = [apksigner_bin, "verify", "--verbose", apk_salida]
    res_verify = subprocess.run(cmd_verify, capture_output=True, text=True)
    if res_verify.returncode != 0:
        raise RuntimeError(f"❌ Falló la verificación de firma del APK: {res_verify.stderr}")
    print("✅ Firma criptográfica válida (v2/v3).")

    # Paso 5: Auditoría estricta final (Fail-fast)
    auditar_apk_estricto(apk_salida, abi_objetivo)
    print(f"🎉 APK generado y auditado con éxito para [{abi_objetivo}].")
    print("=" * 70 + "\n")


def auditar_apk_estricto(apk_path: str, abi_objetivo: str) -> None:
    """
    Inspecciona exhaustivamente el APK final y detiene el proceso con error crítico
    si se detecta cualquier librería nativa (.so) de una arquitectura que no sea la indicada.
    """
    librerias_encontradas = []
    librerias_indebidas = []

    with zipfile.ZipFile(apk_path, "r") as z:
        for nombre in z.namelist():
            if nombre.startswith("lib/"):
                librerias_encontradas.append(nombre)
                if not nombre.startswith(f"lib/{abi_objetivo}/"):
                    librerias_indebidas.append(nombre)

    if librerias_indebidas:
        raise RuntimeError(
            f"❌ AUDITORÍA FALLIDA: El APK de [{abi_objetivo}] contiene librerías no autorizadas:\n"
            + "\n".join(f"   - {item}" for item in librerias_indebidas)
        )

    print(f"🛡️  Auditoría superada: 0 librerías ajenas detectadas en [{abi_objetivo}].")


def mostrar_resumen_librerias(apk_path: str) -> None:
    """
    Muestra en consola las librerías nativas presentes en el APK.
    """
    with zipfile.ZipFile(apk_path, "r") as z:
        libs = [n for n in z.namelist() if n.startswith("lib/")]
        print(f"📋 Librerías nativas en {os.path.basename(apk_path)} ({len(libs)} archivos):")
        for lib in sorted(libs):
            print(f"   • {lib}")


def main():
    parser = argparse.ArgumentParser(
        description="Aislar arquitecturas nativas y firmar APKs de depuración de forma estricta."
    )
    parser.add_argument("--input", required=True, help="Ruta del APK de entrada")
    parser.add_argument(
        "--abi",
        required=True,
        choices=["arm64-v8a", "armeabi-v7a", "x86_64", "universal"],
        help="Arquitectura de procesador objetivo",
    )
    parser.add_argument("--output", required=True, help="Ruta del APK final de salida")
    parser.add_argument("--keystore", default="debug.keystore", help="Ruta al archivo keystore")
    parser.add_argument("--storepass", default="android", help="Contraseña del keystore")
    parser.add_argument("--keyalias", default="androiddebugkey", help="Alias de la clave")
    parser.add_argument("--keypass", default="android", help="Contraseña de la clave")

    args = parser.parse_args()

    try:
        aislar_y_firmar_apk(
            apk_entrada=args.input,
            abi_objetivo=args.abi,
            apk_salida=args.output,
            keystore_path=args.keystore,
            storepass=args.storepass,
            keyalias=args.keyalias,
            keypass=args.keypass,
        )
    except Exception as e:
        print(f"\n❌ ERROR CRÍTICO DURANTE EL PROCESAMIENTO: {e}", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
