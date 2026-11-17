# Integrante 3 — solicitudes, reservas, reprogramaciones y estados de contratación

## Trazabilidad

- **Requisito explícito (§4.4):** el cliente solicita indicando mensaje, franja y objetivo; el proveedor acepta o rechaza; aceptar crea la reserva y bloquea el cupo en una transacción; una reserva se reprograma antes de comenzar solo si ambas partes aceptan.
- **Requisito explícito (§4.5):** las acciones válidas dependen del estado (`PENDIENTE`, `ACEPTADA`, `CONFIRMADA`, `EN_CURSO`, `COMPLETADA`, `RECHAZADA`, `CANCELADA`). En `RECHAZADA`/`CANCELADA` solo se consulta el historial.
- **Requisito explícito (§5):** el autor no puede solicitar su propia publicación; una publicación no publicada no admite solicitudes; no se puede superar el cupo; toda transición registra fecha, actor y motivo en un historial inmutable.
- **Caso de aceptación (§15):** dos aceptaciones no pueden consumir el último cupo.

## Decisiones de diseño

| Problema | Alternativa elegida | Consecuencias | Evidencia |
|---|---|---|---|
| Las acciones válidas cambian según el estado | **State**: `EstadoContratacion` + `EstadoContratacionBase` (todo inválido por defecto) y una subclase por estado que solo sobrescribe lo que permite la tabla 4.5 | Agregar un estado no toca los existentes; una transición inválida lanza `IllegalStateException` sin cambiar el estado | `ContratacionTest.transicionInvalidaEsRechazadaPorElDominioSinCambiarElEstado` |
| Persistir un State | En la DB se guarda el nombre (`estado`); `@PostLoad` reconstruye el objeto con `EstadoContratacionBase.desdeNombre` | El dominio no conoce tablas; el estado sobrevive al reload | `elEstadoSeReconstruyeDesdeElNombrePersistido`, `PersistenciaTest.contratacionPersisteEstadoReservaPropuestasEHistorial` |
| Quién puede hacer cada acción | `Contratacion` (Information Expert) valida el actor: el proveedor acepta/rechaza/inicia/completa, el cliente paga, cualquiera de los dos cancela o reprograma | El service solo resuelve `userId → id de Perfil/Organización`; el permiso vive en el dominio | `cadaAccionLaPuedeHacerSoloLaParteQueCorresponde`, `servicioRechazaUnUsuarioQueNoParticipa` |
| Reglas al crear la solicitud | Factory method `Solicitud.crear(...)` concentra las reglas de §5 | No se puede construir una solicitud inválida desde el caso de uso | `solicitudRespetaLasReglasDeNegocio` |
| Sobreventa de cupos | `Reserva` ocupa/libera el cupo de `FranjaHoraria`, que tiene `@Version` (optimistic locking). Aceptar = estado + reserva + cupo en una sola transacción | Si dos aceptaciones leen el mismo cupo, la segunda falla al commit y hace rollback completo (queda `PENDIENTE`) | `noSePuedeAceptarSiLaFranjaNoTieneCupo`, `PersistenciaTest.dosAceptacionesSimultaneasNoConsumenElUltimoCupo` |
| Reprogramar con acuerdo de ambas partes | `PropuestaReprogramacion` guarda el doble consentimiento. Quien propone ya acepta; la otra parte la aplica (la reserva se mueve de franja) o la rechaza. Quien propuso también puede retirarla | Una propuesta no aceptada no bloquea para siempre: al rechazarla se puede proponer otra. Solo hay una propuesta pendiente a la vez | `laReprogramacionSoloSeAplicaCuandoAceptanAmbasPartes`, `unaPropuestaRechazadaNoMueveLaReservaYPermiteProponerOtra`, `servicioRechazaUnaPropuestaDeReprogramacion` |
| Reprogramar "antes de comenzar" | Se valida que la franja reservada no haya empezado, además de que el estado lo permita (`ACEPTADA` o `CONFIRMADA`) | **Decisión del equipo:** también se permite reprogramar en `ACEPTADA` (antes de pagar) | `noSePuedeReprogramarUnaSesionEnCurso` |
| Historial inmutable | `RegistroTransicion` (embeddable, sin setters) en una `@ElementCollection`; `getHistorial()` devuelve una vista no modificable y solo `registrarTransicion` agrega | Nadie puede borrar ni falsear entradas desde afuera de la entidad | `elHistorialNoSePuedeModificarDesdeAfuera` |
| Consultar historial (única acción en estados finales) | `ContratacionService.consultarHistorial` / `ContratacionController.consultarHistorial` devuelven `RegistroHistorialDTO` (fecha, nombre del actor, origen, destino, motivo), solo a participantes | La UI nunca recibe la entidad | `enUnaContratacionRechazadaSoloSeConsultaElHistorial` |
| Monto del reintegro al cancelar | El estado solo decide si se puede cancelar; el monto lo calcula `PoliticaCancelacion` (**Strategy**, integrante 4) y solo si estaba pagada | Cambiar la política no toca el State | `cancelarUnaContratacionPagadaReintegraSegunLaPoliticaConfigurada` |
| Notificar sin acoplarse a los canales | El service publica `EventoDominio` por `PublicadorEventosPort` después del commit (**Observer**, integrante 2) | Si la transacción hace rollback no se notifica | `servicioSolicitaAceptaYPublicaLosEventos` |

**Decisiones del equipo** (la consigna no las fija): inicia y completa la sesión el proveedor; el rechazo de una solicitud no genera notificación porque §4.6 no lo lista.

## Cambios respecto de la versión anterior

- Se eliminó `RegistroEstadoContratacion`: era una entidad sin uso que duplicaba a `RegistroTransicion` y creaba una tabla vacía.
- Se agregaron `rechazarReprogramacion` (dominio, service y controller) y `consultarHistorial` (service y controller).

## Actualización requerida en StarUML (diagramas 4.4.1 y 4.5)

1. Quitar `RegistroEstadoContratacion` del diagrama.
2. `Contratacion`: agregar `rechazarReprogramacion(actorId)`; nota de que `getHistorial()` devuelve una vista inmutable.
3. `PropuestaReprogramacion`: atributo `rechazada` y operaciones `rechazar()` / `estaPendiente()`.
4. `ContratacionService` y `ContratacionController`: `rechazarReprogramacion(...)` y `consultarHistorial(...) : List<RegistroHistorialDTO>`; agregar `RegistroHistorialDTO` en Application.
5. Diagrama de estados 4.5: la reprogramación (proponer / aceptar / rechazar) es una autotransición en `ACEPTADA` y `CONFIRMADA`.
