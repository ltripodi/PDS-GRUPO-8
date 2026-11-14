-- =====================================================================================
-- V2: datos de demostracion reproducibles para el entorno local (consigna, seccion 8).
--
-- IDs fijos para que la demo sea siempre igual. El primer byte indica el tipo:
--   01 categoria · 02 habilidad · 03 perfil · 04 organizacion · 05 publicacion
--   06 franja horaria · 07 movimiento
-- Ej.: X'03000000000000000000000000000001' = perfil 03000000-0000-0000-0000-000000000001.
--
-- Usuarios del login simulado (userId):
--   ana        Mentora Java/arquitectura. Proveedora de 3 publicaciones (una en BORRADOR).
--   carla      Fotografa y disenadora UX. Proveedora de 2 publicaciones (una gratis).
--   bruno      Cliente con 1000 SkillCoins.
--   diego      Cliente con 100 SkillCoins (no le alcanza para la mentoria de 150: saldo insuficiente).
--   acme-admin Representante de la organizacion Acme Software (autora de un desafio).
--
-- Fechas en UTC. Las franjas son de marzo de 2027 para que sigan siendo futuras en la demo.
-- La reputacion arranca en 0: solo la cambian calificaciones verificadas.
-- =====================================================================================

-- ---------- Catalogo ----------

insert into categorias (id, nombre) values
    (X'01000000000000000000000000000001', 'Tecnologia'),
    (X'01000000000000000000000000000002', 'Diseno'),
    (X'01000000000000000000000000000003', 'Fotografia'),
    (X'01000000000000000000000000000004', 'Desarrollo profesional');

insert into habilidades (id, nombre) values
    (X'02000000000000000000000000000001', 'Java'),
    (X'02000000000000000000000000000002', 'Arquitectura de software'),
    (X'02000000000000000000000000000003', 'Diseno UX'),
    (X'02000000000000000000000000000004', 'Fotografia de producto'),
    (X'02000000000000000000000000000005', 'Entrevistas laborales');

-- ---------- Perfiles ----------

insert into perfiles (id, activo, biografia, nombre_visible, reputacion, user_id, zona_horaria) values
    (X'03000000000000000000000000000001', TRUE, 'Desarrolladora backend Java con 8 anios de experiencia. Mentora de entrevistas tecnicas.', 'Ana Lopez', 0, 'ana', 'America/Argentina/Buenos_Aires'),
    (X'03000000000000000000000000000002', TRUE, 'Estudiante de sistemas buscando su primer trabajo como desarrollador.', 'Bruno Diaz', 0, 'bruno', 'America/Argentina/Buenos_Aires'),
    (X'03000000000000000000000000000003', TRUE, 'Fotografa de producto y disenadora UX freelance.', 'Carla Mendez', 0, 'carla', 'America/Argentina/Cordoba'),
    (X'03000000000000000000000000000004', TRUE, 'Emprendedor armando la tienda online de su marca.', 'Diego Ruiz', 0, 'diego', 'America/Montevideo');

insert into perfil_habilidades (perfil_id, habilidad_id) values
    (X'03000000000000000000000000000001', X'02000000000000000000000000000001'),
    (X'03000000000000000000000000000001', X'02000000000000000000000000000002'),
    (X'03000000000000000000000000000001', X'02000000000000000000000000000005'),
    (X'03000000000000000000000000000003', X'02000000000000000000000000000003'),
    (X'03000000000000000000000000000003', X'02000000000000000000000000000004');

insert into perfil_intereses (perfil_id, interes) values
    (X'03000000000000000000000000000001', 'liderazgo tecnico'),
    (X'03000000000000000000000000000002', 'java'),
    (X'03000000000000000000000000000002', 'entrevistas'),
    (X'03000000000000000000000000000003', 'branding'),
    (X'03000000000000000000000000000004', 'fotografia'),
    (X'03000000000000000000000000000004', 'diseno');

insert into perfil_disponibilidad (perfil_id, inicio, fin) values
    (X'03000000000000000000000000000001', '2027-03-01 12:00:00', '2027-03-01 21:00:00'),
    (X'03000000000000000000000000000003', '2027-03-05 13:00:00', '2027-03-05 22:00:00');

-- Preferencias: cada usuario elige email, push o interna (4.6).
insert into perfil_canales_notificacion (perfil_id, canal) values
    (X'03000000000000000000000000000001', 'INTERNA'),
    (X'03000000000000000000000000000001', 'EMAIL'),
    (X'03000000000000000000000000000002', 'INTERNA'),
    (X'03000000000000000000000000000002', 'PUSH'),
    (X'03000000000000000000000000000003', 'INTERNA'),
    (X'03000000000000000000000000000004', 'EMAIL');

insert into organizaciones (id, nombre, user_id_autorizado) values
    (X'04000000000000000000000000000001', 'Acme Software', 'acme-admin');

-- ---------- Publicaciones (4.2) ----------

insert into publicaciones (id, autor_id, organizacion_id, tipo, titulo, descripcion, categoria_id, modalidad,
                           duracion_minutos, cupo, precio_base, estado, fecha_creacion) values
    (X'05000000000000000000000000000001', X'03000000000000000000000000000001', NULL, 'MENTORIA',
     'Preparacion para entrevistas tecnicas Java',
     'Simulacro de entrevista tecnica con preguntas de Java, POO y SQL, y devolucion personalizada.',
     X'01000000000000000000000000000004', 'REMOTA', 60, 3, 150, 'PUBLICADA', '2026-11-01 10:00:00'),
    (X'05000000000000000000000000000002', X'03000000000000000000000000000001', NULL, 'SERVICIO',
     'Revision de arquitectura de software',
     'Analizo la arquitectura de tu proyecto y te entrego un informe con riesgos y mejoras.',
     X'01000000000000000000000000000001', 'REMOTA', 90, 1, 300, 'PUBLICADA', '2026-11-02 10:00:00'),
    (X'05000000000000000000000000000003', X'03000000000000000000000000000003', NULL, 'SERVICIO',
     'Sesion de fotografia de producto',
     'Sesion en estudio con fondo blanco y 15 fotos editadas para tu tienda online.',
     X'01000000000000000000000000000003', 'PRESENCIAL', 120, 2, 250, 'PUBLICADA', '2026-11-03 10:00:00'),
    (X'05000000000000000000000000000004', X'03000000000000000000000000000003', NULL, 'MENTORIA',
     'Revision de portfolio UX',
     'Repasamos tu portfolio de diseno UX y te doy consejos concretos para mejorarlo.',
     X'01000000000000000000000000000002', 'REMOTA', 45, 4, 0, 'PUBLICADA', '2026-11-04 10:00:00'),
    (X'05000000000000000000000000000005', X'03000000000000000000000000000001', NULL, 'MENTORIA',
     'Taller de testing con JUnit',
     'Borrador: todavia no tiene franjas, asi que no se puede publicar ni aparece en la busqueda.',
     X'01000000000000000000000000000001', 'REMOTA', 60, 5, 100, 'BORRADOR', '2026-11-05 10:00:00'),
    (X'05000000000000000000000000000006', NULL, X'04000000000000000000000000000001', 'DESAFIO',
     'Prototipo de app de turnos medicos',
     'Buscamos quien construya un prototipo navegable de una app de turnos para clinicas.',
     X'01000000000000000000000000000001', 'HIBRIDA', 480, 5, 1000, 'PUBLICADA', '2026-11-06 10:00:00');

-- La franja 06...03 tiene cupo 1: sirve para mostrar que dos aceptaciones no consumen el ultimo cupo.
insert into franjas_horarias (id, publicacion_id, inicio, fin, cupo, ocupados, version) values
    (X'06000000000000000000000000000001', X'05000000000000000000000000000001', '2027-03-02 14:00:00', '2027-03-02 15:00:00', 3, 0, 0),
    (X'06000000000000000000000000000002', X'05000000000000000000000000000001', '2027-03-04 14:00:00', '2027-03-04 15:00:00', 3, 0, 0),
    (X'06000000000000000000000000000003', X'05000000000000000000000000000002', '2027-03-03 18:00:00', '2027-03-03 19:30:00', 1, 0, 0),
    (X'06000000000000000000000000000004', X'05000000000000000000000000000003', '2027-03-06 15:00:00', '2027-03-06 17:00:00', 2, 0, 0),
    (X'06000000000000000000000000000005', X'05000000000000000000000000000004', '2027-03-05 20:00:00', '2027-03-05 20:45:00', 4, 0, 0),
    (X'06000000000000000000000000000006', X'05000000000000000000000000000006', '2027-03-10 13:00:00', '2027-03-10 21:00:00', 5, 0, 0);

insert into publicacion_etiquetas (publicacion_id, etiqueta) values
    (X'05000000000000000000000000000001', 'java'),
    (X'05000000000000000000000000000001', 'entrevistas'),
    (X'05000000000000000000000000000002', 'arquitectura'),
    (X'05000000000000000000000000000002', 'backend'),
    (X'05000000000000000000000000000003', 'fotografia'),
    (X'05000000000000000000000000000003', 'producto'),
    (X'05000000000000000000000000000004', 'ux'),
    (X'05000000000000000000000000000004', 'portfolio'),
    (X'05000000000000000000000000000005', 'testing'),
    (X'05000000000000000000000000000006', 'prototipo'),
    (X'05000000000000000000000000000006', 'mobile');

insert into publicacion_habilidades (publicacion_id, habilidad_id) values
    (X'05000000000000000000000000000001', X'02000000000000000000000000000001'),
    (X'05000000000000000000000000000001', X'02000000000000000000000000000005'),
    (X'05000000000000000000000000000002', X'02000000000000000000000000000002'),
    (X'05000000000000000000000000000003', X'02000000000000000000000000000004'),
    (X'05000000000000000000000000000004', X'02000000000000000000000000000003'),
    (X'05000000000000000000000000000006', X'02000000000000000000000000000001'),
    (X'05000000000000000000000000000006', X'02000000000000000000000000000003');

-- Historial: igual al que deja Publicacion.publicar(...) (BORRADOR -> PUBLICADA, motivo "publicacion").
insert into publicacion_historial (publicacion_id, actorId, origen, destino, fecha, motivo) values
    (X'05000000000000000000000000000001', 'ana', 'BORRADOR', 'PUBLICADA', '2026-11-01 10:05:00', 'publicacion'),
    (X'05000000000000000000000000000002', 'ana', 'BORRADOR', 'PUBLICADA', '2026-11-02 10:05:00', 'publicacion'),
    (X'05000000000000000000000000000003', 'carla', 'BORRADOR', 'PUBLICADA', '2026-11-03 10:05:00', 'publicacion'),
    (X'05000000000000000000000000000004', 'carla', 'BORRADOR', 'PUBLICADA', '2026-11-04 10:05:00', 'publicacion'),
    (X'05000000000000000000000000000006', 'acme-admin', 'BORRADOR', 'PUBLICADA', '2026-11-06 10:05:00', 'publicacion');

-- ---------- Billeteras de SkillCoins ----------

insert into billeteras (titular_id, saldo) values
    ('ana', 0),
    ('bruno', 1000),
    ('carla', 0),
    ('diego', 100);

insert into movimientos (id, titular_id, monto, tipo, fecha) values
    (X'07000000000000000000000000000001', 'bruno', 1000, 'ACREDITACION_INICIAL', '2026-11-01 09:00:00'),
    (X'07000000000000000000000000000002', 'diego', 100, 'ACREDITACION_INICIAL', '2026-11-01 09:00:00');
