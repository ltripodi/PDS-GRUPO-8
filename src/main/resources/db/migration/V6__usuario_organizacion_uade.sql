-- =====================================================================================
-- V6: el representante de la organizacion de los datos demo pasa de 'acme-admin' a 'uade-admin'.
--
-- No se edita V2: ya esta aplicada en las bases de los integrantes y Flyway rechazaria el cambio
-- (compara el checksum de cada script aplicado). Se renombra el userId en todas las tablas que lo
-- guardan, por si ya se uso en la demo (historial, notificaciones, calificaciones, billetera).
-- =====================================================================================

update organizaciones set user_id_autorizado = 'uade-admin' where user_id_autorizado = 'acme-admin';

update publicacion_historial set actorId = 'uade-admin' where actorId = 'acme-admin';

update notificaciones set destinatario_id = 'uade-admin' where destinatario_id = 'acme-admin';

update calificaciones set calificador_id = 'uade-admin' where calificador_id = 'acme-admin';
update calificaciones set calificado_id = 'uade-admin' where calificado_id = 'acme-admin';

-- La billetera se identifica por el userId y sus movimientos la referencian: se crea la nueva,
-- se le pasan los movimientos y se borra la vieja (si nunca se cargo saldo, no hace nada).
insert into billeteras (titular_id, saldo, version)
    select 'uade-admin', saldo, version from billeteras where titular_id = 'acme-admin';
update movimientos set titular_id = 'uade-admin' where titular_id = 'acme-admin';
delete from billeteras where titular_id = 'acme-admin';
