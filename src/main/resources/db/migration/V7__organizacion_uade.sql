-- =====================================================================================
-- V7: la organizacion de los datos demo pasa de 'Acme Software' a 'UADE'.
--
-- Va aparte de V6 (que renombro a su representante, uade-admin) porque V6 puede estar ya
-- aplicada en alguna base: un script aplicado no se edita.
-- =====================================================================================

update organizaciones set nombre = 'UADE' where id = X'04000000000000000000000000000001';
