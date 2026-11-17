# Integrante 1 — perfiles, publicaciones, calificaciones y reputación

## Trazabilidad

- **Requisito explícito:** perfiles administran datos profesionales, baja lógica y reputación derivada exclusivamente de calificaciones verificadas (consigna §§4.1 y 5).
- **Requisito explícito:** una publicación se crea como borrador; solo puede publicarse si tiene título, categoría, precio no negativo y una franja válida; las transiciones se auditan y una cancelación requiere motivo (consigna §§4.2 y 5).
- **Requisito explícito:** cada participante de una contratación `COMPLETADA` califica una sola vez a la otra parte (consigna §§4.4, 4.5 y 5).
- **Decisión del equipo:** el puntaje es entero de 1 a 5 y la reputación es el promedio aritmético de las calificaciones verificadas. La consigna no fija escala ni fórmula; se debe confirmar o ajustar esta decisión si la cátedra entrega un contrato de UI diferente.

## Responsabilidades y dependencias

- `Perfil`, `Publicacion`, `FranjaHoraria`, `DisponibilidadPerfil` y `Calificacion` conservan sus invariantes en dominio.
- `PerfilService`, `PublicacionService` y `CalificacionService` coordinan autorización por `userId`, puertos y el límite transaccional de calificar + recalcular reputación.
- Los controllers únicamente delegan. Los servicios reciben puertos por constructor; no dependen de JPA ni de la UI.
- Se agregó el puerto `HabilidadRepository` y su adaptador JPA para resolver IDs de habilidades sin que aplicación conozca persistencia.

Esto aplica **Information Expert** y **GRASP Controller**: las entidades conocen y protegen su estado; los servicios coordinan casos de uso. La transacción en `CalificacionService` evita que se persista una reseña sin que se actualice la reputación. La restricción única de base de datos `(contratacion_id, calificador_id)` permanece como respaldo ante concurrencia.

## Correcciones de cierre

### Calificaciones en desafíos de organizaciones

- **Requisito explícito:** al completar una contratación, cada parte puede calificar a la otra una única vez (§§4.4, 4.5 y 5). Esto también incluye un desafío cuyo proveedor es una organización.
- **Decisión del equipo:** `CalificacionService` resuelve al participante en el contexto de la contratación: puede ser un `Perfil` o la `Organizacion` representada por el `userId`. `Contratacion.validarCalificacion(...)` conserva en el dominio las reglas de estado, participación y contraparte.
- **Consecuencia:** el representante puede calificar al perfil participante y el perfil puede emitir una calificación verificada hacia la organización. Sólo si el destinatario es un `Perfil` se recalcula reputación, porque la consigna obliga reputación pública para perfiles, no para organizaciones.
- **Alternativa descartada:** agregar reputación pública a `Organizacion`. Habría ampliado el modelo, los DTOs y la interfaz sin que el TP lo exija.
- **Evidencia:** `CalificacionServiceTest.lasDosPartesDeUnDesafioPuedenCalificarseYSoloElPerfilRecalculaReputacion` y `DesafioIntegracionTest.lasDosPartesDeUnDesafioCompletadoPuedenCalificarse`.

### Encapsulación de colecciones de agregados

- **Problema:** las colecciones de `Perfil`, `Publicacion`, `Billetera` y `Contratacion` se exponían mutables. Un cliente podía alterar habilidades, franjas, movimientos o propuestas sin validar el estado del agregado.
- **Decisión:** sus getters ahora devuelven copias inmutables; las modificaciones se hacen por métodos de dominio (`agregarFranja`, `reemplazarHabilidades`, `acreditar`, `proponerReprogramacion`, etc.).
- **Principios:** protege invariantes por *Information Expert* y encapsulación; los tests preparan los escenarios usando las mismas operaciones válidas que producción.
- **Evidencia:** `ColeccionesDelDominioTest.losGettersDeColeccionesDelDominioNoPermitenModificarElEstadoInterno`.

## Actualización requerida en StarUML

Antes de entregar, actualizar `SkillSwap_2.0.mdj` con:

1. `HabilidadRepository` en Application y `JpaHabilidadRepository` en Infrastructure, con dependencia hacia el puerto.
2. Dependencias constructoras de `PerfilService` hacia `HabilidadRepository`, `PerfilRepository`, `CalificacionRepository`, `PublicacionRepository` y `ContratacionRepository`.
3. `PerfilController.verPerfil` retornando `PerfilPublicoDTO` (la adaptación posterior al modelo concreto del Mock UI Kit queda en el controller).
4. La nota de decisión de escala 1–5/promedio, si el equipo la ratifica.
5. Dependencia de `CalificacionService` hacia `OrganizacionRepository` y operación `Contratacion.validarCalificacion(calificadorId, calificadoId)`.
6. Notas de inmutabilidad en las colecciones expuestas por `Perfil`, `Publicacion`, `Billetera` y `Contratacion`; agregar `Publicacion.reemplazarHabilidades(...)`.
