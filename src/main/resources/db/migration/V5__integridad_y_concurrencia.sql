-- =====================================================================================
-- V5: optimistic locking en publicaciones y contrataciones, claves foraneas, CHECKs e indices
-- que faltaban en V1. No se edita V1: un cambio de esquema va siempre en una migracion nueva.
-- =====================================================================================

-- ---------- Optimistic locking ----------
-- Hibernate hace "UPDATE ... WHERE id = ? AND version = ?": si dos transacciones modifican la misma
-- fila (por ejemplo, aceptar y cancelar la misma contratacion), la segunda falla en vez de pisar.
alter table contrataciones add column version bigint not null default 0;
alter table publicaciones add column version bigint not null default 0;

-- ---------- Indices de las consultas frecuentes ----------
-- Se crean antes de las FK para que MySQL las use y no cree indices duplicados.
create index idx_contrataciones_cliente on contrataciones (cliente_id);
create index idx_contrataciones_proveedor on contrataciones (proveedor_id);
create index idx_calificaciones_calificado on calificaciones (calificado_id);
create index idx_publicaciones_estado on publicaciones (estado);
create index idx_solicitudes_cliente on solicitudes (cliente_id);
create index idx_solicitudes_franja on solicitudes (franja_id);
create index idx_propuestas_nueva_franja on propuestas_reprogramacion (nueva_franja_id);

-- ---------- Claves foraneas ----------
-- contrataciones.proveedor_id no lleva FK: en un desafio el proveedor es una organizacion, no un perfil.
alter table contrataciones
    add constraint fk_contrataciones_cliente foreign key (cliente_id) references perfiles (id);
alter table solicitudes
    add constraint fk_solicitudes_cliente foreign key (cliente_id) references perfiles (id);
alter table solicitudes
    add constraint fk_solicitudes_franja foreign key (franja_id) references franjas_horarias (id);
alter table propuestas_reprogramacion
    add constraint fk_propuestas_nueva_franja foreign key (nueva_franja_id) references franjas_horarias (id);

-- ---------- Reglas que la base tambien garantiza (MySQL 8.0.16+ aplica los CHECK) ----------
alter table billeteras
    add constraint ck_billeteras_saldo_no_negativo check (saldo >= 0);
alter table franjas_horarias
    add constraint ck_franjas_ocupados_validos check (ocupados >= 0 and ocupados <= cupo);
