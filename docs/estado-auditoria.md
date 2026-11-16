# Estado de la auditoría de `bauty3` (16/11/2026)

177 tests pasan sobre H2. La app se probó contra **MySQL 8.4 real**: Flyway aplicó las migraciones desde cero y la ventana funciona con los datos demo.

## Hecho

### Demo y ejecución

- `SkillSwapApplication` (raíz): tiene el `main` y es el composition root, el único lugar que arma repositorios, servicios y controllers.
- `presentation/ui/SkillSwapFrame`: ventana Swing con las pestañas Explorar, Mis contrataciones, Billetera, Notificaciones y Mi perfil. Tiene login simulado y un botón de demo guiada.
- Gestión de publicaciones (4.2) desde **Mi perfil**: nueva publicación (oferta o desafío, con primera franja y publicar opcionales), agregar franja, publicar, pausar, cerrar y cancelar.
- `DatosPublicacionDTO` recibe el precio como `int` (la interfaz no usa `SkillCoins`). En un desafío sin `organizacionId`, el servicio usa la organización que representa el usuario.
- `presentation/consola/ConsolaSkillSwap`: la misma funcionalidad por consola (`--consola`).
- Paquetes reorganizados por capa y por funcionalidad (ver `docs/arquitectura.md`).
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
- El representante de una organización (sin perfil) ve en "Mis contrataciones" las solicitudes de sus desafíos, las acepta y cobra. Hay un test de integración (`DesafioIntegracionTest`).
- Datos demo: la organización es UADE y la representa `uade-admin` (migraciones V6 y V7).
- `Perfil.recalcularReputacion` solo cuenta las calificaciones dirigidas a ese perfil.

### Persistencia y concurrencia (`V5`)

- `version` en `Contratacion` y `Publicacion`. Hay un test de aceptar y rechazar en simultáneo.
- Claves foráneas que faltaban: `contrataciones.cliente_id`, `solicitudes.cliente_id`, `solicitudes.franja_id` y `propuestas_reprogramacion.nueva_franja_id`.
- CHECKs de `saldo >= 0` y de `0 <= ocupados <= cupo`.
- Índices de las consultas frecuentes.

## Pendiente

### Entregables del grupo

- [x] **Probar en MySQL real** (16/11, MySQL 8.4).
- [ ] **UML en StarUML (`.mdj`).**
- [ ] **Informe PDF de decisiones de diseño.**
- [ ] **Cobertura de aplicación:** faltan tests de `WalletService`, de `PerfilService` y de los controllers de Perfil, Publicacion y Wallet.

### Código

- [ ] `PublicacionService` no usa `UnidadTransaccional`.
- [ ] Faltan tests de doble pago y doble calificación concurrentes.
- [ ] Paginación en memoria en `ContratacionService.listar` y en `WalletService`.
- [ ] Casi todo el mapeo es `EAGER`, y `buscarCandidatas` trae todas las publicaciones.
- [ ] Un desafío no se puede calificar: la organización no tiene `Perfil` ni reputación.
- [ ] Menores: `SkillCoins` sin `equals`/`hashCode`; `Movimiento.tipo` es `String`; algunos getters devuelven las listas internas; `PagoService` recibe un `CalculadorPrecio` que no usa.

### Decisiones para el grupo

- **Publicación cancelada con contrataciones ya pagadas:** hoy siguen su curso (se pueden iniciar, completar o cancelar con reintegro). Solo se bloquea aceptar y pagar.
- **Cancelar con un proveedor que ya gastó lo cobrado:** falla, para no crear SkillCoins.
- **Alcance de las promociones:** hoy son globales.
- **Patrones:** hay 5 implementados (Strategy, State, Observer, Adapter, Decorator). Se puede presentar `EstadoContratacionBase.desdeNombre` como Simple Factory.
