# PDS-GRUPO-8
SkillSwap - TP Proceso de Desarrollo de Software - UADE

## Estructura del proyecto

El proyecto usa Maven y separa el código por capas dentro de `com.skillswap`:

- `domain`: entidades y reglas de negocio.
- `application`: casos de uso y servicios de aplicación.
- `infrastructure`: persistencia e integraciones externas.
- `controllers`: adaptadores de entrada. Reciben y devuelven solo DTOs, y todo error sale como `ErrorAplicacion` (con tipo `DATOS_INVALIDOS`, `SIN_PERMISO`, `ESTADO_INVALIDO`, `CONFLICTO` o `ERROR_INTERNO`).
- `presentation`: la consola de la demo (`ConsolaSkillSwap`, con el `main`) y el composition root (`ContenedorAplicacion`), que es el único lugar que arma repositorios JPA, servicios y controllers.

## Cómo ejecutar la aplicación

### Requisitos

- JDK 17 o superior.
- Maven 3.9 o superior, o un IDE con soporte Maven (IntelliJ, Eclipse).
- MySQL 8 corriendo en `localhost:3306`. La base no hace falta crearla: la crea la aplicación.

### Pasos

1. Clonar el repositorio y pararse en la carpeta del proyecto.
2. Si tu MySQL no usa `root` / `root`, pasá tus credenciales con `-D` (no hace falta editar ningún archivo).
3. Arrancar la consola:

   ```bash
   mvn compile exec:java
   ```

   Con otras credenciales:

   ```bash
   mvn compile exec:java -Djakarta.persistence.jdbc.user=miusuario -Djakarta.persistence.jdbc.password=miclave
   ```

   Desde el IDE: ejecutar `com.skillswap.presentation.ConsolaSkillSwap` con la opción de VM `-Dnet.bytebuddy.experimental=true`.
4. Al arrancar, Flyway crea las tablas y carga los datos demo (V1 a V5). La primera vez tarda unos segundos.
5. Iniciar sesión con un usuario demo (es un login simulado, sin clave):

   | userId | Rol en la demo |
   |---|---|
   | `ana` | Mentora Java. Proveedora de 3 publicaciones (una en borrador). |
   | `carla` | Fotógrafa y diseñadora UX. Proveedora de 2 publicaciones. |
   | `bruno` | Cliente con 1000 SkillCoins. |
   | `diego` | Cliente con 100 SkillCoins: no le alcanza para la mentoría de 150. |
   | `acme-admin` | Representante de la organización Acme Software (autora de un desafío). |

### Demo guiada

La opción **D** del menú recorre el flujo completo y muestra cada paso:

1. Bruno busca y solicita la mentoría de Ana.
2. Ana acepta (State y Observer).
3. Diego intenta pagar sin saldo y se rechaza.
4. Bruno paga con SkillCoins.
5. Ana inicia y completa la sesión.
6. Bruno califica a Ana (la reputación pasa a 5) y no puede calificarla dos veces.
7. Se muestran el historial de transiciones y las notificaciones (bandeja interna y mocks de email y push).

Cada corrida ocupa un lugar de la franja. Para volver a los datos iniciales, borrá la base y volvé a arrancar:

```sql
DROP DATABASE skillswap;
```

### Opciones útiles

- Ver el SQL que genera Hibernate: agregar `-Dhibernate.show_sql=true`.
- Correr los tests: `mvn test`. Usan H2 en memoria y no necesitan MySQL.

## Base de datos

- Persistencia con Hibernate/JPA sobre MySQL (`src/main/resources/META-INF/persistence.xml`, unidad `skillSwapPU`).
- El mapeo objeto-relacional está en `src/main/resources/META-INF/orm.xml`, no en anotaciones: las clases de `domain` no conocen JPA (Clean Architecture). Si se agrega un atributo con columna especial, una relación o una entidad nueva, se mapea ahí.
- La base `skillswap` se crea sola (`createDatabaseIfNotExist=true`). Las tablas **no** las crea Hibernate (`hbm2ddl.auto=none`): las crea **Flyway** al arrancar, con las migraciones de `src/main/resources/db/migration`:
  - `V1__esquema_inicial.sql`: todas las tablas, claves e índices.
  - `V2__datos_demo.sql`: datos de demostración (usuarios `ana`, `carla`, `bruno`, `diego` y `acme-admin`; el detalle está en el encabezado del script).
  - `V3__eliminar_registro_estado_publicacion.sql`: borra una tabla que no se usaba (ejemplo de cómo se cambia el esquema sin tocar V1).
  - `V4__version_billeteras.sql`: agrega la columna `version` a `billeteras` (optimistic locking: dos pagos simultáneos no se pisan el saldo).
  - `V5__integridad_y_concurrencia.sql`:
    - `version` en `contrataciones` y `publicaciones`: aceptar y cancelar a la vez ya no se pisan.
    - Claves foráneas que faltaban: cliente de contrataciones y solicitudes, franja de solicitudes y propuestas.
    - CHECKs de saldo no negativo y de `ocupados <= cupo`.
    - Índices de las consultas frecuentes.
- Flyway anota en la tabla `flyway_schema_history` qué versiones aplicó, así que cada script corre una sola vez. Para cambiar el esquema se agrega `V6__descripcion.sql`; nunca se edita un script ya aplicado.
- **Si ya tenías la base creada por Hibernate** (antes de Flyway), borrala una vez para que Flyway la arme de cero: `DROP DATABASE skillswap;`
- Para volver a los datos demo originales: borrá la base y volvé a arrancar.
- Usuario/clave por defecto: `root` / `root`. Si tu MySQL local usa otros, pasalos con `-Djakarta.persistence.jdbc.user=...` y `-Djakarta.persistence.jdbc.password=...` (ver "Cómo ejecutar la aplicación").
- `mvn test` corre los tests de persistencia contra H2 en memoria (no necesita MySQL).
