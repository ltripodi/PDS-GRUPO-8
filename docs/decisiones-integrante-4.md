# Decisiones — Integrante 4 (Pagos, SkillCoins, Promociones y Cancelaciones)

## Alcance
Diagrama UML 4.4.2: SkillCoins, Billetera/Movimiento, Pago, Promoción,
CalculadorPrecio (Strategy/Decorator), Políticas de cancelación (Strategy),
PagoService, WalletService, sus puertos y adaptadores JPA, y los métodos
de pago/cancelación/reintegro en los controllers.

## Desviaciones respecto al UML

### 1. PagoService depende de PerfilRepository y OrganizacionRepository
- **Motivo:** `Contratacion` guarda `clienteId`/`proveedorId` como `UUID` (id de Perfil/Organización)
  y `Billetera` está indexada por `userId:String` (login). Hay que traducir en el service.
- **Impacto en StarUML:** agregar dependencias `PerfilRepository` y `OrganizacionRepository` a `PagoService`.

### 2. PagoService.realizarReintegro NO abre transacción
- `ContratacionService.cancelar` ya abre la transacción con `UnidadTransaccional.ejecutar(...)`,
  valida estado/cupo y calcula el reintegro con `PoliticaCancelacion`. `realizarReintegro` participa
  de esa transacción para mantener atomicidad `estado + cupo + reintegro`.
- **Impacto en StarUML:** anotar en `realizarReintegro` “participa de la transacción del llamador”.

### 3. CalculadorPrecio no se usa en pagar()
- El monto a cobrar es `contratacion.getPrecio()` (congelado al solicitar). `CalculadorPrecio` queda
  para el flujo de publicación (precio promocional) y NO para pagos.
- **Impacto en StarUML:** marcar la dependencia como opcional / de uso por publicación.

### 4. PrecioBase retiene SkillCoins en su constructor
- El UML mostraba `total()` sin campos. El patrón Decorator requiere que cada componente conozca su
  monto. `PrecioBase(SkillCoins)` y `PrecioConPromocion(CalculadorPrecio, Promocion)`.
- **Impacto en StarUML:** agregar campos/constructores.

### 5. Billetera.reintegrar() agrega un tipo propio de movimiento
- El UML mostraba solo `debitar/acreditar`. Agregamos `reintegrar` para que `Movimiento.tipo` sea
  `"REINTEGRO"` y quede auditoría distinta de un crédito común.
- **Impacto en StarUML:** agregar `+reintegrar(SkillCoins)` a `Billetera`.

### 6. Movimiento.tipo es String, no enum
- El campo real ya estaba modelado como `String`. Usamos literales `"DEBITO"`, `"CREDITO"`, `"REINTEGRO"`.
- **Impacto en StarUML:** documentar el tipo como `String`.

### 7. WalletService paginación in-memory
- `BilleteraRepository` no expone método paginado. Se pagina in-memory en `WalletService` para no
  ampliar el puerto. Si más adelante el volumen lo justifica, se agrega `listarMovimientosPaginado`
  al repo (desviación menor).

### 8. Defensa doble contra doble cobro
- `Pago.contratacionId` tiene `unique = true` (ya estaba en el `@Table` real).
- `PagoService.pagar` chequea además con `pagos.buscarPorContratacion(...)` antes de crear el Pago.
- `Pago.registrarReintegro` acumula y valida que `totalReintegrado <= total`.

## Archivos modificados
- `domain/SkillCoins`: +`sumar`, +`restar`
- `domain/Billetera`: +`debitar`, +`acreditar`, +`consultarSaldo`, +`reintegrar`
- `domain/Pago`: +`registrarReintegro`
- `domain/Promocion`: +`aplicar`
- `domain/PrecioBase`: constructor + `total()`
- `domain/PrecioConPromocion`: constructor + `total()`
- `domain/PoliticaPorAnticipacion`: constructor + `calcularReintegro`
- `domain/PoliticaSinReintegro`: `calcularReintegro`
- `application/ComprobantePagoDTO`: constructor + getters
- `application/PagoService`: implementado
- `application/WalletService`: implementado
- `controllers/WalletController`: implementado

## Archivos NO modificados (ya estaban completos o no son mi alcance)
- `infrastructure/JpaBilleteraRepository`, `JpaPagoRepository`, `JpaOrganizacionRepository`, `JpaContratacionRepository`, `JpaPerfilRepository`, `JPAUtil`, `SimpleJpaRepository`, `JpaUnidadTransaccional`
- `controllers/ContratacionController` (ya delega en `PagoService`)
- Cualquier `domain/*` no listado arriba

## Cambios a reflejar en StarUML
1. `PagoService`: agregar dependencias a `PerfilRepository` y `OrganizacionRepository`.
2. `PagoService.realizarReintegro`: anotar “participa de la transacción del llamador”.
3. `PagoService`: marcar `CalculadorPrecio` como dependencia opcional / de publicación.
4. `PrecioBase`: agregar campo `monto: SkillCoins` y constructor.
5. `PrecioConPromocion`: agregar constructor `(CalculadorPrecio, Promocion)`.
6. `Billetera`: agregar `+reintegrar(SkillCoins)`.
7. `Movimiento.tipo`: documentar como `String`.
8. `Pago`: agregar `totalReintegrado: SkillCoins` + `+registrarReintegro(SkillCoins)`.