# Informe de cambios — Rama `Danna` (Integrante 2: puntos 4.3 y 4.6)

## Contexto
Proyecto SkillSwap (Java 17, JPA/Hibernate 6.5, MySQL; los tests usan H2). Arquitectura en capas:
`domain` → `application` (casos de uso y puertos) → `infrastructure` (JPA y adaptadores) → `controllers`.
La rama `Danna` sale de `main` (commit a026081) y tiene 2 commits:

| Commit | Contenido | Estado verificado |
|---|---|---|
| `bac0fb6` | 4.3 Búsqueda y recomendación + cambios compartidos que necesita | Compila solo; 52 tests en verde |
| `c384a00` | 4.6 Notificaciones | Compila; 118 tests en verde (suite completa) |

Los dos commits se verificaron por separado: cada uno compila y pasa los tests sin depender del otro.

---

## 1. Cambios que ROMPEN compatibilidad (requieren acción de otros integrantes)

| Archivo | Cambio | Qué hay que hacer |
|---|---|---|
| `application/PerfilService.java` | El constructor agrega un parámetro: `UnidadTransaccional transacciones` (no puede ser null) | Todo lugar que construya `PerfilService` (main, tests) debe pasarlo |
| `application/FiltrosBusqueda.java` | Precios y reputación pasan de `int/double` a `Integer/Double`. **`null` = sin filtro**; `0` ahora filtra de verdad (por ejemplo, `precioMaximo = 0` → solo gratis) | Reemplazar los `0` usados como "sin filtro" por `null` |
| `domain/Publicacion.java` | La columna `autor_id` deja de ser `NOT NULL` (en los desafíos queda en null) | Las bases MySQL locales ya creadas siguen teniendo `NOT NULL`, porque `hbm2ddl=update` no quita esa restricción. Hay que recrear la base o ejecutar `ALTER TABLE publicaciones MODIFY autor_id BINARY(16) NULL;` (ajustar el tipo al que tenga la columna) |
| `application/NotificacionRepository.java` | Puerto del 4.6: `listar(userId)` pasa a ser `listar(userId, pagina, tamanio)` y se agrega `marcarTodasLeidas(userId)` | Solo afecta a quien implemente ese puerto (hoy: JPA y el doble de prueba del 4.6) |
| `domain/Notificacion.java` | `tipo` pasa de `String` a `TipoEvento` (enum, persistido como texto); el constructor ahora recibe `TipoEvento` | Solo lo usa el 4.6 y `PersistenciaTest` (ya adaptado) |

---

## 2. Cambios en archivos compartidos o de otros integrantes (aditivos o correcciones)

### 2.1 Desafíos de organizaciones — `domain/Publicacion.java` (4.2)
- **Problema:** `crearDesafio` guardaba el id de la organización en `autor_id`, que tiene clave foránea hacia `perfiles`. Por eso los desafíos no se podían persistir y no aparecían en búsqueda ni recomendación.
- **Cambio:**
  - En un desafío `autorId` queda en null y el autor es `organizacion` (columna `organizacion_id`, con su propia clave foránea).
  - Se agregó un constructor privado con los datos comunes. El constructor público (ofertas) exige `autorId` y rechaza `TipoPublicacion.DESAFIO`; los desafíos se crean solo con `crearDesafio`.
  - `getAutorId()` devuelve `organizacion.getId()` en un desafío y `autorId` en una oferta, así que el código existente que lo usa no cambia.
- **Tests:** `PersistenciaTest` (guardar y recuperar un desafío, la columna `autor_id` en null, una oferta con autor inexistente falla) y `PerfilesYPublicacionesTest`.

### 2.2 Baja de perfil — `application/PerfilService.java` (4.1)
- **Problema:** al dar de baja un perfil, sus publicaciones seguían activas y aparecían en búsqueda y recomendación.
- **Cambio:** `darDeBaja` pausa todas las publicaciones activas del perfil (`publicacion.pausar`) y da de baja el perfil **en una sola transacción** (`UnidadTransaccional`). De ahí el parámetro nuevo del constructor.

### 2.3 Preferencias de notificación — `domain/Perfil.java` (4.1)
- Se agregó `cambiarCanalesNotificacion(Set<CanalNotificacion>)`. Exige el perfil activo y reemplaza el value object `PreferenciaNotificacion` (no lo modifica). Es aditivo.

### 2.4 Carga de autores en lote — `application/PerfilRepository.java` + `infrastructure/JpaPerfilRepository.java` (4.1)
- **Problema:** al convertir una página de publicaciones a DTO se buscaba cada autor por separado (N+1).
- **Cambio:** nuevo `buscarPorIds(Set<UUID>) → Map<UUID, Perfil>`.
  - En el puerto es un método `default` que busca de a uno, así que los dobles de prueba existentes compilan sin cambios.
  - `JpaPerfilRepository` lo sobrescribe con una sola consulta `IN` + `LEFT JOIN FETCH` de `habilidades`, `intereses` y `preferenciaNotificacion.canales` (las tres EAGER y de tipo `Set`).
  - Para `null` o un conjunto vacío, las dos versiones devuelven un mapa vacío.
- **Costo aceptado:** las tres colecciones en un mismo JOIN generan un producto cartesiano. Se probó dividirlo en 3 consultas y vuelve el N+1 (medido: de 13 a 29 consultas al pasar de 2 a 10 autores), porque las colecciones son EAGER. La solución de fondo es cambiar el mapeo de `Perfil` (`@Fetch(SUBSELECT)` o `@BatchSize`); no se hizo por ser un archivo de otro integrante.

### 2.5 Eventos después del commit — `infrastructure/JPAUtil.java` (compartido)
- **Problema:** el puerto `PublicadorEventosPort.publicarDespuesDelCommit` (definido en el commit base) no podía cumplirse, porque nadie sabía cuándo se confirmaba la transacción. `ContratacionService` publica los eventos dentro de la transacción.
- **Cambio:**
  - `ThreadLocal<List<Runnable>> PENDIENTES` y el método público `despuesDelCommit(Runnable)`. Con una transacción abierta encola la acción; sin transacción la ejecuta en el momento.
  - `transaccion(...)` ejecuta las acciones pendientes **solo si hubo commit**, después de cerrar el EntityManager y limpiar `ACTUAL`, para que cada acción pueda abrir su propia transacción. Si hay rollback, se descartan.
  - En una transacción anidada, las acciones esperan al commit de la transacción externa.
  - Si una acción falla después del commit, se registra en el log y no se propaga.
- Es aditivo: quien no usa `despuesDelCommit` no nota diferencia.

### 2.6 Paginación — `application/Pagina.java` (compartido)
- Inmutable (`final` y `List.copyOf`). Valida `numero >= 0`, `tamanio > 0`, `total >= 0` y que no haya más elementos que el tamaño.
- Utilidades estáticas nuevas: `de(lista, numero, tamanio)` (pagina en memoria), `validarPedido`, `desplazamiento` (offset sin desbordar `int`) y `convertir(Function<List<T>, List<R>>)`.
- `ContratacionService` sigue funcionando igual.

### 2.7 Búsqueda en la base — `infrastructure/JpaPublicacionRepository.java` (métodos del commit base, usados solo por el 4.3)
- **`buscar(filtros, pagina, tamanio)`:**
  - Solo publicaciones `PUBLICADA`.
  - `LEFT JOIN p.perfil autor`: antes, el join implícito dejaba afuera los desafíos. Un desafío no tiene reputación: no cumple una reputación mínima y va al final en los órdenes por reputación (`NULLS LAST`).
  - Filtros: texto (título o descripción, sin distinguir mayúsculas, con `LIKE ... ESCAPE '!'` para que `%` y `_` se busquen literalmente), categoría, modalidad, rango de precio, habilidad (`EXISTS`) y reputación mínima.
  - Órdenes:
    - `REPUTACION`;
    - `MENOR_PRECIO`;
    - `MAS_RECIENTE`;
    - `RELEVANCIA`: con texto, primero las que lo tienen en el título; después reputación y fecha.
    - Todos desempatan por `p.id`, así la paginación es estable.
  - **Fetch plan en dos pasos (sin N+1):**
    1. Consulta paginada que devuelve solo los ids, más una consulta `COUNT`.
    2. Una consulta con `JOIN FETCH` de categoría, organización, habilidades y etiquetas para esos ids, reordenada según la lista de ids.
- **`buscarCandidatas()`:** `JOIN FETCH` de categoría, organización, habilidades y etiquetas (sin paginar), para evitar el N+1 en la recomendación.

### 2.8 Tests compartidos — `test/.../infrastructure/PersistenciaTest.java`
- En los tests existentes solo se ajustaron los `FiltrosBusqueda` (de `0` a `null`) y el constructor de `Notificacion` (`TipoEvento`).
- Tests nuevos:
  - 4.6: notificación guardada recién después del commit, descartada si hay rollback, transacción anidada, error al notificar que no deshace la operación, `marcarLeida` persistido;
  - desafíos (4 tests).
- Conocido y preexistente: su `@AfterAll` llama a `JPAUtil.close()` y cierra la fábrica estática. Los tests JPA que corran después en la misma JVM fallarían; hoy el orden de ejecución lo evita.

---

## 3. Punto 4.3 — Búsqueda y recomendación (archivos propios)

| Archivo | Responsabilidad |
|---|---|
| `application/BusquedaService.java` | Valida la entrada; filtra, ordena y pagina en el repositorio; convierte a DTO cargando los autores en lote (`buscarPorIds`); `listarCategorias()` → `List<CategoriaDTO>` |
| `application/RecomendacionService.java` | **Context del Strategy.** Indexa las estrategias por `TipoRecomendacion` en un `EnumMap` (rechaza duplicados, nulos y estrategias sin tipo); excluye las publicaciones propias (como autor o como representante de la organización); **consulta las solicitudes recientes solo si la estrategia lo necesita** (`requiereSolicitudesRecientes()`); pagina en memoria con `Pagina.de` |
| `domain/EstrategiaRecomendacion.java` | Interfaz Strategy: `tipo()`, `recomendar(...)`, `default requiereSolicitudesRecientes()` (false), `static ordenarPorPuntaje(...)` (calcula el puntaje **una vez** por candidata, descarta las de puntaje 0, ordena de mayor a menor) y el comparador `DESEMPATE` (más reciente y después id) |
| `domain/RecomendacionPorHabilidades.java` | Puntaje = habilidades en común entre el perfil y la publicación |
| `domain/RecomendacionPorPopularidad.java` | Puntaje = solicitudes dentro de la ventana de tiempo; `requiereSolicitudesRecientes() = true` |
| `application/FiltrosBusqueda.java` | Inmutable; filtros opcionales con `null`; valida precios ≥ 0, mínimo ≤ máximo y reputación finita y ≥ 0; texto en blanco → `null`; orden por defecto `RELEVANCIA` |
| `application/PublicacionDTO.java` | DTO inmutable de salida (la interfaz nunca recibe la entidad). Se eliminó el campo `destacada` del esqueleto, porque nunca se calculaba |
| `application/PublicacionMapper.java` (nuevo) | Conversión `Publicacion` → `PublicacionDTO`. Proveedor: nombre del perfil, nombre de la organización (desafío) o "Desconocido" |
| `application/CategoriaDTO.java` (nuevo) | `record (UUID id, String nombre)`: opción del filtro de categoría |
| `controllers/BusquedaController.java` | Controller delgado: `buscar(filtros)` / `buscar(filtros, pagina, tamanio)`, `recomendar(userId)` / `recomendar(userId, tipo, pagina, tamanio)` y `listarCategorias()`. Sin página explícita: página 0, tamaño 20; tipo por defecto `HABILIDADES`. **Devuelve `Pagina<PublicacionDTO>`** |

**Patrón Strategy:** caso de aceptación 7 ("cambiar la estrategia sin modificar el caso de uso") cubierto por tests. Una estrategia nueva se inyecta en el constructor sin modificar `RecomendacionService`.

---

## 4. Punto 4.6 — Notificaciones (archivos propios)

**Flujo:** el caso de uso (por ejemplo `ContratacionService.aceptar`) llama a `publicarDespuesDelCommit(EventoDominio)` → **commit** → `PublicadorEventosEnMemoria` avisa a sus observadores → `NotificacionesObserver.alOcurrir` → `NotificacionService.notificar` → por cada destinatario: texto según el tipo y según si es quien hizo la acción → `CanalInterno` guarda en la bandeja → email y push según la preferencia del usuario.

| Archivo | Responsabilidad |
|---|---|
| `infrastructure/PublicadorEventosEnMemoria.java` | **Subject del Observer.** `CopyOnWriteArrayList` + `addIfAbsent` (seguro con varios hilos, sin observadores duplicados); publica mediante `JPAUtil.despuesDelCommit`; un observador que falla no frena a los demás |
| `application/NotificacionesObserver.java` | **Concrete Observer:** delega el evento en `NotificacionService` |
| `application/NotificacionService.java` | Caso de uso. **Exige el canal INTERNA** en el constructor. `notificar`: guarda **primero** en la bandeja y recién después envía por email y push según la preferencia; si no pudo guardar, no envía nada afuera; un canal externo que falla no frena a los demás. Textos en un `switch` sin `default` (un `TipoEvento` nuevo sin texto no compila). Además de los seis eventos de la consigna, notifica `RECHAZO` (solicitud rechazada), `REPROGRAMACION_PROPUESTA` (la otra parte tiene que aceptarla) y `REPROGRAMACION_RECHAZADA`. La fecha de la notificación es `evento.getOcurridoEn()`. Bandeja: `listar` paginado en la base, `marcarLeida` (verifica que la notificación sea del usuario; si no, `SecurityException`), `marcarTodasLeidas` (un solo `UPDATE`), `configurarCanales`. Valida el `userId` en todos los métodos |
| `infrastructure/notificacion/CanalInterno.java` | Canal `INTERNA`: entregar = guardar en la bandeja (el único canal que persiste) |
| `infrastructure/notificacion/EmailAdapter.java` + `EmailExternoMock.java` | **Adapter**: traduce `CanalEnvio.enviar(Notificacion)` a `enviarCorreo(userId, contenido)` del proveedor simulado (el mock guarda lo enviado en una `CopyOnWriteArrayList`, segura entre hilos) |
| `infrastructure/notificacion/PushAdapter.java` + `PushExternoMock.java` | **Adapter**: traduce a `emitirPush(userId, titulo)` |
| `domain/Notificacion.java` | Entidad. Índice `idx_notificaciones_destinatario_fecha (destinatario_id, fecha)`; valida destinatario, tipo, título y fecha; `marcarLeida` idempotente; `esDe(userId)` |
| `domain/PreferenciaNotificacion.java` | Value object inmutable; al menos un canal, sin nulos; **`equals`/`hashCode` por valor** |
| `application/NotificacionRepository.java` + `infrastructure/JpaNotificacionRepository.java` | `guardar`, `buscarPorId`, `listar` paginado (`COUNT` + `LIMIT/OFFSET`), `marcarTodasLeidas` (`UPDATE` masivo en una transacción) |
| `application/NotificacionDTO.java` (nuevo) | DTO de salida. `fecha` es `LocalDateTime` (zona del sistema), como `Models.Notificacion` del UI kit |
| `controllers/NotificacionController.java` | `listar(userId)` / `listar(userId, pagina, tamanio)` → `Pagina<NotificacionDTO>`, `marcarLeida(userId, idTexto)`, `marcarTodasLeidas`, `configurarCanales` |

**Decisión de diseño:** toda notificación se guarda en la bandeja interna, aunque el usuario haya elegido solo email. La preferencia decide únicamente los canales externos.

**Caso de aceptación 8** ("dos tipos de notificación ante un mismo evento mediante adaptadores mock") cubierto de punta a punta: `ContratacionService.aceptar` real → notificación interna guardada en H2 + email mock.

---

## 5. Tests
- **Ubicación:** por módulo, en los paquetes espejo de `src/main` (los tests ya no se agrupan por integrante).
  - Unitarios: `application/busqueda/BusquedaYRecomendacionTest`, `application/notificacion/NotificacionesTest`, `application/perfil/BajaDePerfilTest` y `domain/publicacion/PublicacionTest` (autor de una publicación); doble de prueba compartido: `application/DoblesEnMemoria`.
  - Integración con H2 (`infrastructure/persistence`): `BusquedaYRecomendacionIntegracionTest`, que incluye un test que cuenta consultas con las estadísticas de Hibernate para detectar N+1 en búsqueda y recomendación; y `NotificacionesIntegracionTest`.
- **Suite completa:** 118 tests, 0 fallas (`mvn clean test`).
- **No probado contra MySQL real:** los tests usan H2 en modo MySQL. Verificar `NULLS LAST`, el `CASE WHEN ... LIKE ... ESCAPE` y el índice nuevo con `hbm2ddl=update`.

---

## 6. Pendientes que dependen de otros integrantes
1. **Pagos (integrante 4):** `PagoService.pagar` tiene que publicar, dentro de su transacción:
   `eventos.publicarDespuesDelCommit(new EventoDominio(TipoEvento.PAGO_CONFIRMADO, contratacionId, userIdCliente, Set.of(userIdCliente, userIdProveedor)))`.
   El 4.6 ya lo procesa (está testeado publicándolo directamente).
2. **Reintegro:** mientras `PagoService.realizarReintegro` no exista, cancelar una contratación pagada falla y no se notifica la cancelación.
3. **Armado de la aplicación (`main`):** registrar `publicador.suscribir(new NotificacionesObserver(notificacionService))`. Si no, no se envía ninguna notificación.
4. **Integración con el Mock UI Kit:** conectar `BusquedaController` y `NotificacionController`.
5. **Grupo:** migraciones versionadas (Flyway o Liquibase, exigidas por el TP; hoy se usa `hbm2ddl=update`); índices de búsqueda en `publicaciones`; `@Fetch(SUBSELECT)` en las colecciones de `Perfil` para eliminar el producto cartesiano de `buscarPorIds`.
