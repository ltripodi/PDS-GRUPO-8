# Estado de la auditoría de `bauty3` (16/11/2026)

174 tests pasan sobre H2. La demo guiada corre de punta a punta sobre H2 en modo MySQL, con Flyway aplicando V1 a V5. **Todavía no se probó contra un MySQL real.**

## Hecho

### Demo y ejecución

- `presentation/ConsolaSkillSwap`: tiene el `main`, login simulado y un menú con todos los casos de uso. La opción **D** es la demo guiada.
- `presentation/ContenedorAplicacion`: es el composition root, el único lugar que arma repositorios, servicios y controllers.
- `README`: instrucciones para ejecutar (`mvn compile exec:java`) y credenciales por `-D`.
- `show_sql` está apagado. Se prende con `-Dhibernate.show_sql=true`.

### Controllers

- `PublicacionController` devuelve `PublicacionDTO`, que ahora incluye sus franjas (`FranjaDTO`).
- `WalletController` devuelve `MovimientoDTO`.
- Nuevo `RecomendacionController`, separado de `BusquedaController`.
- `CalificacionController` valida la entrada.
- `ContratacionDTO` incluye el `userId` de la contraparte, para poder calificarla.
- `WalletController.acreditarDemo` quedó documentado como función solo para la demo.

### Reglas de negocio

- `Contratacion.aceptar` valida todo antes de cambiar de estado: si no hay cupo, la contratación sigue PENDIENTE y sin reserva. También revalida que la publicación siga PUBLICADA y que la franja no haya comenzado.
- `Solicitud.crear` exige una franja futura. El representante de una organización no puede postularse a su propio desafío.
- No se puede pagar una contratación de una publicación cancelada.
- `rechazarReprogramacion` solo funciona en ACEPTADA o CONFIRMADA.
- Dar de baja un perfil con contrataciones abiertas falla.
- `Perfil.recalcularReputacion` solo cuenta las calificaciones dirigidas a ese perfil.

### Persistencia y concurrencia (`V5`)

- `version` en `Contratacion` y `Publicacion`. Hay un test de aceptar y rechazar en simultáneo.
- Claves foráneas que faltaban: `contrataciones.cliente_id`, `solicitudes.cliente_id`, `solicitudes.franja_id` y `propuestas_reprogramacion.nueva_franja_id`.
- CHECKs de `saldo >= 0` y de `0 <= ocupados <= cupo`.
- Índices de las consultas frecuentes.

## Pendiente

### Entregables del grupo

- [ ] **Probar en MySQL real:** `DROP DATABASE skillswap;`, después `mvn compile exec:java` y la opción D.
- [ ] **UML en StarUML (`.mdj`).**
- [ ] **Informe PDF de decisiones de diseño.**
- [ ] **Cobertura de aplicación:** faltan tests de `WalletService`, de `PerfilService` y de los controllers de Perfil, Publicacion y Wallet.

### Código

- [ ] `PublicacionService` no usa `UnidadTransaccional`.
- [ ] Faltan tests de doble pago y doble calificación concurrentes.
- [ ] Paginación en memoria en `ContratacionService.listar` y en `WalletService`.
- [ ] Casi todo el mapeo es `EAGER`, y `buscarCandidatas` trae todas las publicaciones.
- [ ] Un desafío no se puede calificar: la organización no tiene `Perfil` ni reputación.
- [ ] `listarPorParticipante` no lista las contrataciones de un desafío para el representante de la organización.
- [ ] Menores: `SkillCoins` sin `equals`/`hashCode`; `Movimiento.tipo` es `String`; algunos getters devuelven las listas internas; `PagoService` recibe un `CalculadorPrecio` que no usa.

### Decisiones para el grupo

- **Publicación cancelada con contrataciones ya pagadas:** hoy siguen su curso (se pueden iniciar, completar o cancelar con reintegro). Solo se bloquea aceptar y pagar.
- **Cancelar con un proveedor que ya gastó lo cobrado:** falla, para no crear SkillCoins.
- **Alcance de las promociones:** hoy son globales.
- **Patrones:** hay 5 implementados (Strategy, State, Observer, Adapter, Decorator). Se puede presentar `EstadoContratacionBase.desdeNombre` como Simple Factory.
