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

## Actualización requerida en StarUML

Antes de entregar, actualizar `SkillSwap_2.0.mdj` con:

1. `HabilidadRepository` en Application y `JpaHabilidadRepository` en Infrastructure, con dependencia hacia el puerto.
2. Dependencias constructoras de `PerfilService` hacia `HabilidadRepository`, `PerfilRepository`, `CalificacionRepository`, `PublicacionRepository` y `ContratacionRepository`.
3. `PerfilController.verPerfil` retornando `PerfilPublicoDTO` (la adaptación posterior al modelo concreto del Mock UI Kit queda en el controller).
4. La nota de decisión de escala 1–5/promedio, si el equipo la ratifica.
