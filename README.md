# MARGEN

**Conoce lo que realmente ganas.**

App Android offline-first para pequeños emprendedores en Colombia: calcula el costo real
de tus productos, el precio de venta, la utilidad, el margen y el recargo. Sin cuentas,
sin Internet, sin permisos, sin publicidad. Moneda: COP.

## Fórmulas
- Costo total = suma de (cantidad × costo unitario) de cada componente
- Precio recomendado = costo / (1 − margen), redondeado hacia arriba al peso
- Utilidad = precio − costo
- Margen = utilidad / precio × 100
- Recargo = utilidad / costo × 100

## Compilar el APK
1. Sube el proyecto a GitHub (rama `main`).
2. Pestaña **Actions** → **Android CI** → entra a la última ejecución.
3. Descarga el artefacto **margen-debug-apk** (zip) y descomprímelo.
4. Instala `app-debug.apk` en tu Android (permite "instalar apps desconocidas").

Local: `gradle testDebugUnitTest assembleDebug` (JDK 17, Android SDK 35).

## Gradle Wrapper
Ejecuta una vez el workflow **Generar Gradle Wrapper** (Actions → Run workflow).
Agrega `gradlew`, `gradlew.bat` y `gradle-wrapper.jar` oficiales al repositorio.

## Release firmado (opcional)
Workflow **Release firmado**. Requiere los secretos `MARGEN_KEYSTORE_BASE64`,
`MARGEN_KEYSTORE_PASSWORD`, `MARGEN_KEY_ALIAS` y `MARGEN_KEY_PASSWORD`.
Nunca subas llaves ni contraseñas al repositorio.

## Arquitectura
`core` (formato y lectura de números) · `domain` (modelos y cálculos puros) ·
`data` (Room, repositorios) · `ui` (Compose + ViewModel) · `navigation`.
Dinero en pesos enteros (`Long`); cálculos con `BigDecimal`.

## Monetización futura (no implementada)
Las pantallas consultan `domain/Entitlements.kt`, nunca "si pagó". Hoy se usa
`MvpEntitlements` (todo abierto). Para activar gratis/PRO (5 productos gratis; PRO de
$19.900 COP, pago único):
1. Añadir Google Play Billing y una clase `BillingEntitlements : Entitlements` que lea
   la compra verificada (`maxProducts = 5` si no es PRO; `null` si es PRO).
2. Cambiar `entitlements` en `MargenApp.kt` (`AppContainer`) por esa clase.
3. Mostrar una pantalla de compra donde hoy aparece "disponible en la versión PRO".
No hay que tocar cálculos, base de datos ni navegación.

## Limitaciones del MVP
Sin copias de seguridad ni exportación (ver hoja de ruta). `allowBackup=false`:
al cambiar de teléfono los datos no se transfieren.
