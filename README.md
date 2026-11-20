# PDS-GRUPO-8
SkillSwap - TP Proceso de Desarrollo de Software - UADE

## Estructura del proyecto

El proyecto usa Maven y organiza `com.skillswap` por capas. Dentro de cada capa, las clases se agrupan por funcionalidad (`busqueda`, `contratacion`, `pagos`, `perfil`, `publicacion`…). El árbol completo y su justificación están en [docs/arquitectura.md](docs/arquitectura.md).

- `SkillSwapApplication` (raíz): el `main` y el composition root, que es el único lugar que arma repositorios JPA, servicios y controllers.
- `domain`: entidades, reglas, estados (State), políticas (Strategy), eventos y precio (Decorator). No conoce JPA: el mapeo está en `orm.xml`.
- `application`: casos de uso, interfaces de repositorio (puertos) y DTOs.
- `infrastructure`: repositorios JPA (`persistence`), publicador de eventos (`evento`) y adaptadores de email y push (`adapters`).
- `presentation`:
  - `controller`: reciben y devuelven solo DTOs, y todo error sale como `ErrorAplicacion` (con tipo `DATOS_INVALIDOS`, `SIN_PERMISO`, `ESTADO_INVALIDO`, `CONFLICTO` o `ERROR_INTERNO`).
  - `contrato`: lo que la interfaz recibe.
  - `ui`: la ventana Swing (`SkillSwapFrame`).
  - `consola`: la versión de consola y la demo guiada.

## Cómo ejecutar la aplicación

### Requisitos

- JDK 17 o superior.
- Maven 3.9 o superior, o un IDE con soporte Maven (IntelliJ, Eclipse).
- MySQL 8 corriendo en `localhost:3306`. La base no hace falta crearla: la crea la aplicación.

### Pasos

1. Clonar el repositorio y pararse en la carpeta del proyecto.
2. Si tu MySQL no usa `root` / `root`, pasá tus credenciales con `-D` (no hace falta editar ningún archivo).
3. Arrancar la aplicación. Se abre la ventana de SkillSwap (Swing):

   ```bash
   mvn compile exec:java
   ```

   Para usar la versión de consola en vez de la ventana:

   ```bash
   mvn compile exec:java -Dexec.args=--consola
   ```

   Con otras credenciales:

   ```bash
   mvn compile exec:java -Djakarta.persistence.jdbc.user=miusuario -Djakarta.persistence.jdbc.password=miclave
   ```

   Desde el IDE: ejecutar `com.skillswap.SkillSwapApplication` con la opción de VM `-Dnet.bytebuddy.experimental=true`.
4. Al arrancar, Flyway crea las tablas y carga los datos demo (V1 a V9). La primera vez tarda unos segundos.
5. Elegir un usuario demo en el selector de arriba a la derecha (es un login simulado, sin clave):

   | userId | Rol en la demo |
   |---|---|
   | `ana` | Mentora Java. Proveedora de 3 publicaciones (una en borrador). |
   | `carla` | Fotógrafa y diseñadora UX. Proveedora de 2 publicaciones. |
   | `bruno` | Cliente con 1000 SkillCoins. |
   | `diego` | Cliente con 100 SkillCoins: no le alcanza para la mentoría de 150 (135 con las promociones). |
   | `uade-admin` | Representante de la organización UADE: publica desafíos y no tiene perfil. |

### Demo guiada

El botón **Demo guiada** de la ventana (o la opción **D** de la consola) recorre los 10 casos de aceptación obligatorios de la consigna (sección 15) y muestra cada paso:

1. **Caso 1:** Ana publica una mentoría nueva y Bruno la encuentra con una búsqueda filtrada (texto, categoría y precio máximo).
2. **Caso 2:** publicar el borrador de Ana que no tiene franjas se rechaza.
3. **Caso 9:** las dos promociones de V9 (-10 y -5) se acumulan: la mentoría de 150 sale 135, y una consulta de 10 sale 0, no -5 (Decorator).
4. **Caso 3:** Ana acepta y Bruno paga con saldo suficiente (State).
5. **Caso 8:** esa misma aceptación sale por email a Ana y por push a Bruno, además de la bandeja interna (Observer y Adapter).
6. **Caso 4:** Bruno y Diego piden la misma franja de cupo 1. La segunda aceptación se rechaza.
7. **Caso 5:** Diego intenta pagar sin saldo y no cambia ningún saldo ni el estado.
8. **Caso 6:** cancelaciones a 30 h del inicio. Si cancela Bruno (cliente), recupera el 50 %. Si cancela Ana (proveedora), Bruno recupera el 100 % (Strategy).
9. **Caso 7:** las recomendaciones para Carla cambian según la estrategia (por habilidades o por popularidad).
10. **Caso 10:** Ana completa la sesión y Bruno califica una sola vez; la reputación se recalcula. Al final se muestra el historial de transiciones.

Cada corrida crea su propia publicación con franjas nuevas, así que se puede repetir sin borrar la base. Si a Bruno ya no le alcanza el saldo, la demo le carga SkillCoins de demo. Para volver a los datos iniciales, borrá la base y volvé a arrancar:

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
  - `V2__datos_demo.sql`: datos de demostración (usuarios `ana`, `carla`, `bruno`, `diego` y el representante de la organización, que V6 renombra a `uade-admin`; el detalle está en el encabezado del script).
  - `V3__eliminar_registro_estado_publicacion.sql`: borra una tabla que no se usaba (ejemplo de cómo se cambia el esquema sin tocar V1).
  - `V4__version_billeteras.sql`: agrega la columna `version` a `billeteras` (optimistic locking: dos pagos simultáneos no se pisan el saldo).
  - `V5__integridad_y_concurrencia.sql`:
    - `version` en `contrataciones` y `publicaciones`: aceptar y cancelar a la vez ya no se pisan.
    - Claves foráneas que faltaban: cliente de contrataciones y solicitudes, franja de solicitudes y propuestas.
    - CHECKs de saldo no negativo y de `ocupados <= cupo`.
    - Índices de las consultas frecuentes.
  - `V6__usuario_organizacion_uade.sql`: el representante de la organización pasa de `acme-admin` a `uade-admin`. Se hace con una migración nueva porque V2 ya está aplicada en las bases del grupo.
  - `V7__organizacion_uade.sql`: la organización pasa de Acme Software a UADE.
  - `V8__nuevos_tipos_de_notificacion.sql`: tipos de aviso para el rechazo y la reprogramación propuesta o descartada.
  - `V9__promociones_demo.sql`: dos promociones globales y acumulables (-10 y -5 SkillCoins). Se aplican al solicitar, así que las contrataciones anteriores conservan su precio.
- Flyway anota en la tabla `flyway_schema_history` qué versiones aplicó, así que cada script corre una sola vez. Para cambiar el esquema se agrega `V10__descripcion.sql`; nunca se edita un script ya aplicado.
- **Si ya tenías la base creada por Hibernate** (antes de Flyway), borrala una vez para que Flyway la arme de cero: `DROP DATABASE skillswap;`
- Para volver a los datos demo originales: borrá la base y volvé a arrancar.
- Usuario/clave por defecto: `root` / `root`. Si tu MySQL local usa otros, pasalos con `-Djakarta.persistence.jdbc.user=...` y `-Djakarta.persistence.jdbc.password=...` (ver "Cómo ejecutar la aplicación").
- `mvn test` corre los tests de persistencia contra H2 en memoria (no necesita MySQL).
