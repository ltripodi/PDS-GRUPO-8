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
- El mapeo objeto-relacional está en `src/main/resources/META-INF/orm.xml`, no en anotaciones: las clases de `domain` no conocen JPA (Clean Architecture). Si se agrega un atributo con columna especial, una relación o una entidad nueva, se mapea ahí.
- La base `skillswap` se crea sola (`createDatabaseIfNotExist=true`). Las tablas **no** las crea Hibernate (`hbm2ddl.auto=none`): las crea **Flyway** al arrancar, con las migraciones de `src/main/resources/db/migration`:
  - `V1__esquema_inicial.sql`: todas las tablas, claves e índices.
  - `V2__datos_demo.sql`: datos de demostración (usuarios `ana`, `carla`, `bruno`, `diego` y `acme-admin`; el detalle está en el encabezado del script).
  - `V3__eliminar_registro_estado_publicacion.sql`: borra una tabla que no se usaba (ejemplo de cómo se cambia el esquema sin tocar V1).
  - `V4__version_billeteras.sql`: agrega la columna `version` a `billeteras` (optimistic locking: dos pagos simultáneos no se pisan el saldo).
- Flyway anota en la tabla `flyway_schema_history` qué versiones aplicó, así que cada script corre una sola vez. Para cambiar el esquema se agrega `V5__descripcion.sql`; nunca se edita un script ya aplicado.
- **Si ya tenías la base creada por Hibernate** (antes de Flyway), borrala una vez para que Flyway la arme de cero: `DROP DATABASE skillswap;`
- Para volver a los datos demo originales: borrá la base y volvé a arrancar.
- Usuario/clave por defecto: `root` / `root`. Ajustarlos en `persistence.xml` si tu MySQL local usa otros.
- `mvn test` corre los tests de persistencia contra H2 en memoria (no necesita MySQL).
