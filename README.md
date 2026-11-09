# PDS-GRUPO-8
SkillSwap - TP Proceso de Desarrollo de Software - UADE

## Estructura del proyecto

El proyecto usa Maven y separa el código por capas dentro de `com.skillswap`:

- `domain`: entidades y reglas de negocio.
- `application`: casos de uso y servicios de aplicación.
- `infrastructure`: persistencia e integraciones externas.
- `controllers`: adaptadores de entrada.
- `presentation`: vistas o interfaces de usuario.

## Base de datos

- Persistencia con Hibernate/JPA sobre MySQL (`src/main/resources/META-INF/persistence.xml`, unidad `skillSwapPU`).
- La base `skillswap` se crea sola (`createDatabaseIfNotExist=true`) y las tablas se generan con `hbm2ddl.auto=update`.
- Usuario/clave por defecto: `root` / `root`. Ajustarlos en `persistence.xml` si tu MySQL local usa otros.
- `mvn test` corre los tests de persistencia contra H2 en memoria (no necesita MySQL).
