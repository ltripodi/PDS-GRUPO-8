-- =====================================================================================
-- V1: esquema inicial de SkillSwap (MySQL 8).
--
-- Corresponde 1 a 1 con el mapeo de META-INF/orm.xml (MigracionesTest lo valida con
-- hibernate.hbm2ddl.auto=validate). Un script aplicado NO se edita: los cambios de
-- esquema van en una migracion nueva (V3__..., V4__...).
--
-- Los UUID se guardan como binary(16) (asi los mapea Hibernate 6 en MySQL).
-- =====================================================================================

-- ---------- Perfiles y reputacion (4.1) ----------

create table perfiles (
    id binary(16) not null,
    activo bit not null,
    biografia varchar(1000),
    nombre_visible varchar(255) not null,
    reputacion float(53) not null,
    user_id varchar(255) not null,
    zona_horaria varchar(255),
    primary key (id),
    constraint uk_perfiles_user_id unique (user_id)
) engine=InnoDB;

create table habilidades (
    id binary(16) not null,
    nombre varchar(255) not null,
    primary key (id),
    constraint uk_habilidades_nombre unique (nombre)
) engine=InnoDB;

create table perfil_habilidades (
    perfil_id binary(16) not null,
    habilidad_id binary(16) not null,
    primary key (perfil_id, habilidad_id),
    constraint fk_perfil_habilidades_perfil foreign key (perfil_id) references perfiles (id),
    constraint fk_perfil_habilidades_habilidad foreign key (habilidad_id) references habilidades (id)
) engine=InnoDB;

create table perfil_intereses (
    perfil_id binary(16) not null,
    interes varchar(255),
    constraint fk_perfil_intereses_perfil foreign key (perfil_id) references perfiles (id)
) engine=InnoDB;

create table perfil_disponibilidad (
    perfil_id binary(16) not null,
    fin datetime(6),
    inicio datetime(6),
    constraint fk_perfil_disponibilidad_perfil foreign key (perfil_id) references perfiles (id)
) engine=InnoDB;

create table perfil_canales_notificacion (
    perfil_id binary(16) not null,
    canal enum ('EMAIL','INTERNA','PUSH'),
    constraint fk_perfil_canales_perfil foreign key (perfil_id) references perfiles (id)
) engine=InnoDB;

create table organizaciones (
    id binary(16) not null,
    nombre varchar(255) not null,
    user_id_autorizado varchar(255) not null,
    primary key (id)
) engine=InnoDB;

-- ---------- Publicaciones (4.2) ----------

create table categorias (
    id binary(16) not null,
    nombre varchar(255) not null,
    primary key (id),
    constraint uk_categorias_nombre unique (nombre)
) engine=InnoDB;

-- autor_id: perfil autor de una oferta; en un desafio es null y el autor es organizacion_id.
create table publicaciones (
    id binary(16) not null,
    autor_id binary(16),
    cupo integer not null,
    descripcion varchar(2000),
    duracion_minutos integer,
    estado enum ('BORRADOR','CANCELADA','CERRADA','PAUSADA','PUBLICADA') not null,
    fecha_creacion datetime(6),
    modalidad enum ('HIBRIDA','PRESENCIAL','REMOTA') not null,
    precio_base integer,
    tipo enum ('DESAFIO','MENTORIA','SERVICIO') not null,
    titulo varchar(255) not null,
    categoria_id binary(16) not null,
    organizacion_id binary(16),
    primary key (id),
    constraint fk_publicaciones_categoria foreign key (categoria_id) references categorias (id),
    constraint fk_publicaciones_organizacion foreign key (organizacion_id) references organizaciones (id),
    constraint fk_publicaciones_autor foreign key (autor_id) references perfiles (id)
) engine=InnoDB;

-- version: optimistic locking para no sobrevender cupos.
create table franjas_horarias (
    id binary(16) not null,
    cupo integer not null,
    fin datetime(6),
    inicio datetime(6),
    ocupados integer not null,
    version bigint not null,
    publicacion_id binary(16) not null,
    primary key (id),
    constraint fk_franjas_publicacion foreign key (publicacion_id) references publicaciones (id)
) engine=InnoDB;

create table publicacion_etiquetas (
    publicacion_id binary(16) not null,
    etiqueta varchar(255),
    constraint fk_publicacion_etiquetas_publicacion foreign key (publicacion_id) references publicaciones (id)
) engine=InnoDB;

create table publicacion_habilidades (
    publicacion_id binary(16) not null,
    habilidad_id binary(16) not null,
    primary key (publicacion_id, habilidad_id),
    constraint fk_publicacion_habilidades_publicacion foreign key (publicacion_id) references publicaciones (id),
    constraint fk_publicacion_habilidades_habilidad foreign key (habilidad_id) references habilidades (id)
) engine=InnoDB;

-- Historial inmutable de transiciones (fecha, actor, motivo).
create table publicacion_historial (
    publicacion_id binary(16) not null,
    actorId varchar(255),
    destino varchar(255),
    fecha datetime(6),
    motivo varchar(255),
    origen varchar(255),
    constraint fk_publicacion_historial_publicacion foreign key (publicacion_id) references publicaciones (id)
) engine=InnoDB;

create table registro_estado_publicacion (
    id binary(16) not null,
    actor_id binary(16),
    destino enum ('BORRADOR','CANCELADA','CERRADA','PAUSADA','PUBLICADA'),
    fecha datetime(6),
    motivo varchar(255),
    origen enum ('BORRADOR','CANCELADA','CERRADA','PAUSADA','PUBLICADA'),
    publicacion_id binary(16) not null,
    primary key (id),
    constraint fk_registro_estado_publicacion foreign key (publicacion_id) references publicaciones (id)
) engine=InnoDB;

-- ---------- Solicitud, reserva y contratacion (4.4 y 4.5) ----------

create table solicitudes (
    id binary(16) not null,
    cliente_id binary(16) not null,
    fecha_creacion datetime(6) not null,
    franja_id binary(16),
    mensaje varchar(1000),
    objetivo varchar(255),
    publicacion_id binary(16) not null,
    primary key (id),
    constraint fk_solicitudes_publicacion foreign key (publicacion_id) references publicaciones (id)
) engine=InnoDB;

create table reservas (
    id binary(16) not null,
    cantidad integer not null,
    franja_id binary(16) not null,
    vigente bit not null,
    primary key (id),
    constraint fk_reservas_franja foreign key (franja_id) references franjas_horarias (id)
) engine=InnoDB;

create table contrataciones (
    id binary(16) not null,
    cliente_id binary(16) not null,
    estado varchar(255) not null,
    precio integer,
    proveedor_id binary(16) not null,
    reserva_id binary(16),
    solicitud_id binary(16) not null,
    primary key (id),
    constraint uk_contrataciones_reserva unique (reserva_id),
    constraint uk_contrataciones_solicitud unique (solicitud_id),
    constraint fk_contrataciones_reserva foreign key (reserva_id) references reservas (id),
    constraint fk_contrataciones_solicitud foreign key (solicitud_id) references solicitudes (id)
) engine=InnoDB;

create table contratacion_historial (
    contratacion_id binary(16) not null,
    actorId varchar(255),
    destino varchar(255),
    fecha datetime(6),
    motivo varchar(255),
    origen varchar(255),
    constraint fk_contratacion_historial_contratacion foreign key (contratacion_id) references contrataciones (id)
) engine=InnoDB;

create table propuestas_reprogramacion (
    id binary(16) not null,
    aceptacion_cliente bit,
    aceptacion_proveedor bit,
    contratacion_id binary(16) not null,
    nueva_franja_id binary(16) not null,
    rechazada bit,
    primary key (id),
    constraint fk_propuestas_contratacion foreign key (contratacion_id) references contrataciones (id)
) engine=InnoDB;

-- Una persona califica una sola vez cada contratacion.
create table calificaciones (
    id binary(16) not null,
    calificado_id varchar(255) not null,
    calificador_id varchar(255) not null,
    contratacion_id binary(16) not null,
    puntaje integer not null,
    primary key (id),
    constraint uk_calificaciones_contratacion_calificador unique (contratacion_id, calificador_id),
    constraint fk_calificaciones_contratacion foreign key (contratacion_id) references contrataciones (id)
) engine=InnoDB;

-- ---------- SkillCoins, pagos y promociones ----------

create table billeteras (
    titular_id varchar(255) not null,
    saldo integer,
    primary key (titular_id)
) engine=InnoDB;

create table movimientos (
    id binary(16) not null,
    fecha datetime(6),
    monto integer,
    tipo varchar(255),
    titular_id varchar(255) not null,
    primary key (id),
    constraint fk_movimientos_billetera foreign key (titular_id) references billeteras (titular_id)
) engine=InnoDB;

-- Un solo pago por contratacion: evita el doble cobro.
create table pagos (
    id binary(16) not null,
    contratacion_id binary(16) not null,
    fecha datetime(6),
    total integer,
    total_reintegrado integer,
    primary key (id),
    constraint uk_pagos_contratacion unique (contratacion_id),
    constraint fk_pagos_contratacion foreign key (contratacion_id) references contrataciones (id)
) engine=InnoDB;

create table promociones (
    id binary(16) not null,
    descuento integer,
    primary key (id)
) engine=InnoDB;

-- ---------- Notificaciones (4.6) ----------

create table notificaciones (
    id binary(16) not null,
    destinatario_id varchar(255) not null,
    detalle varchar(1000),
    fecha datetime(6) not null,
    leida bit not null,
    tipo enum ('ACEPTACION','CANCELACION','PAGO_CONFIRMADO','REPROGRAMACION','SESION_COMPLETADA','SOLICITUD_RECIBIDA') not null,
    titulo varchar(255) not null,
    primary key (id)
) engine=InnoDB;

-- La bandeja se consulta por destinatario, de la mas reciente a la mas vieja.
create index idx_notificaciones_destinatario_fecha on notificaciones (destinatario_id, fecha);
