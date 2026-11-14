-- =====================================================================================
-- V3: se elimina registro_estado_publicacion.
--
-- La entidad RegistroEstadoPublicacion no se usaba: el historial de una publicacion se
-- guarda en publicacion_historial (RegistroTransicion), asi que esta tabla quedaba vacia.
-- No se edita V1 porque ya estaba publicado: un cambio de esquema siempre va en una
-- migracion nueva.
-- =====================================================================================

drop table registro_estado_publicacion;
