-- =====================================================================================
-- V4: optimistic locking en billeteras.
--
-- Sin version, dos pagos simultaneos del mismo titular leian el mismo saldo y el segundo
-- UPDATE pisaba al primero (saldo incorrecto o credito perdido). Con la columna version,
-- Hibernate hace "UPDATE ... WHERE titular_id = ? AND version = ?" y el commit de la
-- transaccion perdedora falla en vez de sobrescribir.
-- No se edita V1 porque ya estaba publicado: un cambio de esquema va en una migracion nueva.
-- =====================================================================================

alter table billeteras add column version bigint not null default 0;
