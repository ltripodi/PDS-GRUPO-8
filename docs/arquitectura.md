# Arquitectura y organización de paquetes

Esta versión corrige la propuesta `SkillSwap_Propuesta_Arquitectura.docx` para que coincida con el código actual de `bauty3`.

## Criterio

El proyecto está organizado por capas y, dentro de cada capa, por funcionalidad. Las dependencias van siempre hacia adentro:

```
presentation → application → domain
infrastructure → application → domain
```

- **`domain`** no conoce JPA ni MySQL. No tiene ninguna anotación `jakarta.persistence`: todo el mapeo objeto-relacional está en `src/main/resources/META-INF/orm.xml`. Esto ya está resuelto, no es una decisión pendiente.
- **`application`** coordina los casos de uso y declara los puertos que necesita (interfaces `*Repository`, `UnidadTransaccional`, `PublicadorEventosPort`, `CanalEnvio`). Recibe y devuelve DTOs.
- **`infrastructure`** implementa esos puertos con JPA, Flyway y los adaptadores de email y push.
- **`presentation`** tiene los controllers (clases Java normales, sin REST) y la interfaz. La interfaz solo recibe `Controladores` y DTOs: nunca entidades ni repositorios.
- **`SkillSwapApplication`** (en la raíz) es el punto de entrada y el composition root. Es la única clase que conoce las implementaciones concretas: arma repositorios, servicios y controllers, y arranca la consola.

## Árbol

```
com.skillswap
├── SkillSwapApplication                main + composition root
├── domain
│   ├── busqueda       EstrategiaRecomendacion, RecomendacionPorHabilidades, RecomendacionPorPopularidad,
│   │                  OrdenPublicacion, TipoRecomendacion                                   [Strategy]
│   ├── calificacion   Calificacion
│   ├── compartido     SkillCoins, RegistroTransicion
│   ├── contratacion   Contratacion, Estado* (9 clases), Solicitud, Reserva, PropuestaReprogramacion  [State]
│   │                  PoliticaCancelacion, PoliticaPorAnticipacion, PoliticaSinReintegro      [Strategy]
│   ├── evento         EventoDominio, ObservadorEvento, TipoEvento                            [Observer]
│   ├── notificacion   Notificacion, CanalNotificacion, PreferenciaNotificacion
│   ├── pagos          Billetera, Movimiento, Pago
│   ├── perfil         Perfil, Habilidad, DisponibilidadPerfil
│   ├── precio         CalculadorPrecio, PrecioBase, PrecioConPromocion, Promocion            [Decorator]
│   └── publicacion    Publicacion, FranjaHoraria, Categoria, Organizacion, EstadoPublicacion,
│                      Modalidad, TipoPublicacion
├── application
│   ├── busqueda       BusquedaService, RecomendacionService, FiltrosBusqueda, CategoriaDTO
│   ├── calificacion   CalificacionService, CalificacionRepository
│   ├── compartido     Pagina, UnidadTransaccional, ErrorAplicacion, ConflictoConcurrenciaException
│   ├── contratacion   ContratacionService, ContratacionRepository, SolicitudRepository,
│   │                  ContratacionDTO, DatosSolicitudDTO, EstadoContratacionDTO, RegistroHistorialDTO
│   ├── evento         PublicadorEventosPort
│   ├── notificacion   NotificacionService, NotificacionesObserver, CanalEnvio,
│   │                  NotificacionRepository, NotificacionDTO
│   ├── pagos          PagoService, WalletService, BilleteraRepository, PagoRepository,
│   │                  ComprobantePagoDTO, MovimientoDTO
│   ├── perfil         PerfilService, PerfilRepository, HabilidadRepository, DatosPerfilDTO, PerfilPublicoDTO
│   ├── precio         PromocionRepository
│   └── publicacion    PublicacionService, PublicacionRepository, OrganizacionRepository, PublicacionMapper,
│                      PublicacionDTO, FranjaDTO, DatosPublicacionDTO
├── infrastructure
│   ├── notificacion            CanalInterno, EmailAdapter, PushAdapter,
│   │                           EmailExternoMock, PushExternoMock                             [Adapter]
│   ├── evento                  PublicadorEventosEnMemoria
│   └── persistence             JPAUtil, SimpleJpaRepository, Jpa*Repository (11), JpaUnidadTransaccional,
│                               MigracionesBaseDeDatos
└── presentation
    ├── controller     Busqueda, Recomendacion, Contratacion, Calificacion, Perfil, Publicacion,
    │                  Wallet y Notificacion (*Controller) + ControllerBase
    ├── contrato       Controladores (lo que la interfaz recibe de la aplicación)
    ├── ui             SkillSwapFrame (ventana Swing, la interfaz principal)
    └── consola        ConsolaSkillSwap, DemoGuiada (versión de consola, con --consola)
```

## Correcciones a la propuesta original

| Propuesta | Qué se hizo | Por qué |
|---|---|---|
| Estados en `domain/contratacion/estado/` | Los estados quedan en `domain/contratacion`. | `Contratacion.cambiarEstado()` es package-private: solo los estados pueden cambiar el estado. En otro paquete habría que hacerlo público, y cualquier clase podría saltear las reglas del State. |
| Sección 5: "independencia del dominio y JPA, pendiente" | Se eliminó. | El dominio ya no tiene anotaciones JPA: el mapeo está en `orm.xml`. |
| `RegistroEstadoContratacion` y `RegistroEstadoPublicacion` | No existen. | El historial es `RegistroTransicion` (embebible). La tabla vieja la borró la migración V3. |
| Faltaban clases | Se agregaron: `ErrorAplicacion`, `ConflictoConcurrenciaException`, `PromocionRepository`, `JpaPromocionRepository`, `MigracionesBaseDeDatos`, `FranjaDTO`, `MovimientoDTO`, `RecomendacionController`, `Controladores`. | — |
| 4 patrones | Son 5: Strategy, State, Observer, Adapter y Decorator. | Decorator ya estaba conectado en `ContratacionService` para calcular el precio con promociones. |
| `UiControllers`, `Models`, `SkillSwapView`, `*UiAdapter` (kit de UI) | La interfaz es `presentation.ui.SkillSwapFrame` (Swing), armada con el criterio del ejemplo MagicCafe de la cátedra. | Recibe solo `Controladores` y DTOs. Cada llamada corre en un `SwingWorker` para que la ventana no se congele, y los errores se muestran según el tipo de `ErrorAplicacion`. |

`orm.xml` y `persistence.xml` usan los nombres completos de las clases (por ejemplo, `com.skillswap.domain.contratacion.Contratacion`).

## Para el oral

> "Organizamos SkillSwap por capas y, dentro de cada capa, por funcionalidad. El dominio tiene las entidades y las reglas, y no conoce JPA: el mapeo está en `orm.xml`. Aplicación coordina los casos de uso y declara los puertos que necesita, como los repositorios. Infraestructura los implementa con JPA y Flyway, y adapta los servicios externos de email y push. Presentación tiene los controllers, que solo hablan en DTOs, y la consola. `SkillSwapApplication` es el único lugar que conoce las clases concretas y las conecta. Los estados de la contratación están en el mismo paquete que `Contratacion` a propósito: solo ellos pueden cambiarle el estado."
