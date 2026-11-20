-- =====================================================================================
-- V9: promociones vigentes de la demo (consigna, regla 5 y caso de aceptacion 9).
--
-- Hasta ahora la tabla "promociones" estaba vacia y los descuentos solo se veian en los tests.
-- Son dos promociones globales y acumulables (Decorator: PrecioBase -> PrecioConPromocion -> ...):
--   10 SkillCoins + 5 SkillCoins = 15 de descuento sobre el precio base de cada solicitud nueva.
-- Ejemplos con los datos de V2:
--   Mentoria de entrevistas Java: 150 -> 135 (a diego, con 100, sigue sin alcanzarle).
--   Revision de portfolio UX (gratis): 0 -> 0, el total nunca queda negativo.
-- El precio se fija al solicitar: las contrataciones anteriores conservan el suyo.
-- Para retirar una promocion se borra su fila (ver JpaPromocionRepository.listarVigentes).
--
-- ID: el primer byte 08 identifica a las promociones (sigue la convencion de V2).
-- =====================================================================================

insert into promociones (id, descuento) values
    (X'08000000000000000000000000000001', 10),
    (X'08000000000000000000000000000002', 5);
