-- =====================================================================================
-- V8: la bandeja acepta los avisos de rechazo y de propuesta/rechazo de reprogramacion.
--
-- La columna tipo es un enum de MySQL: un valor nuevo de TipoEvento que no este en la lista
-- haria fallar el insert. V1 ya pudo aplicarse en alguna base, asi que se amplia aca.
-- =====================================================================================

alter table notificaciones modify tipo enum (
    'ACEPTACION',
    'CANCELACION',
    'PAGO_CONFIRMADO',
    'REPROGRAMACION',
    'SESION_COMPLETADA',
    'SOLICITUD_RECIBIDA',
    'RECHAZO',
    'REPROGRAMACION_PROPUESTA',
    'REPROGRAMACION_RECHAZADA'
) not null;
