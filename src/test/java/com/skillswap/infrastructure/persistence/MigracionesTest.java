package com.skillswap.infrastructure.persistence;

import com.skillswap.domain.evento.TipoEvento;
import com.skillswap.domain.notificacion.CanalNotificacion;
import com.skillswap.domain.perfil.Perfil;
import com.skillswap.domain.precio.Promocion;
import com.skillswap.domain.publicacion.EstadoPublicacion;
import com.skillswap.domain.publicacion.Publicacion;
import com.skillswap.domain.publicacion.TipoPublicacion;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * Migraciones Flyway (consigna, seccion 8). Cada test usa su propia base H2 vacia en modo MySQL.
 *
 * La prueba clave: despues de migrar, Hibernate arranca con hbm2ddl.auto=validate. Si una tabla,
 * columna o tipo de V1 no coincide con orm.xml, la EntityManagerFactory no se puede crear.
 */
class MigracionesTest {

    private static final UUID ANA = UUID.fromString("03000000-0000-0000-0000-000000000001");
    private static final UUID DESAFIO_UADE = UUID.fromString("05000000-0000-0000-0000-000000000006");
    private static final UUID UADE = UUID.fromString("04000000-0000-0000-0000-000000000001");

    private static String baseNueva() {
        return "jdbc:h2:mem:migracion_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
    }

    @Test
    void lasMigracionesCreanUnEsquemaQueCoincideConElMapeoYCarganLaDemo() throws SQLException {
        String url = baseNueva();

        MigrateResult resultado = MigracionesBaseDeDatos.migrar(url, "sa", "");
        assertEquals(9, resultado.migrationsExecuted, "V1 esquema, V2 datos demo, V3 limpieza, "
                + "V4 version de billeteras, V5 integridad y concurrencia, V6 usuario, V7 nombre de la organizacion, "
                + "V8 tipos de notificacion y V9 promociones");
        assertFalse(existeTabla(url, "registro_estado_publicacion"), "V3 la elimina");

        EntityManagerFactory emf = validarMapeoContra(url);
        EntityManager em = emf.createEntityManager();
        try {
            List<Publicacion> publicadas = em.createQuery(
                            "select p from Publicacion p where p.estado = :estado", Publicacion.class)
                    .setParameter("estado", EstadoPublicacion.PUBLICADA)
                    .getResultList();
            assertEquals(5, publicadas.size(), "5 publicadas + 1 borrador");
            assertTrue(publicadas.stream().allMatch(p -> !p.getFranjas().isEmpty()),
                    "toda publicacion publicada tiene al menos una franja");

            Perfil ana = em.find(Perfil.class, ANA);
            assertEquals("ana", ana.getUserId());
            assertEquals(3, ana.getHabilidades().size());
            assertTrue(ana.getPreferenciaNotificacion().permite(CanalNotificacion.EMAIL));
            assertEquals(0.0, ana.getReputacion(), "la reputacion solo sale de calificaciones");

            Publicacion desafio = em.find(Publicacion.class, DESAFIO_UADE);
            assertEquals(TipoPublicacion.DESAFIO, desafio.getTipo());
            assertEquals(UADE, desafio.getAutorId());
            assertEquals("PUBLICADA", desafio.getHistorial().get(0).getDestino());
            // V6 y V7: la organizacion es UADE y la representa uade-admin, tambien en el historial
            assertEquals("UADE", desafio.getOrganizacion().getNombre());
            assertEquals("uade-admin", desafio.getOrganizacion().getUserIdAutorizado());
            assertEquals("uade-admin", desafio.getHistorial().get(0).getActorId());

            // V9: dos promociones acumulables (10 + 5) sobre el precio base
            List<Promocion> promociones = em.createQuery("select p from Promocion p", Promocion.class)
                    .getResultList();
            assertEquals(15, promociones.stream().mapToInt(p -> p.getDescuento().getValor()).sum());
        } finally {
            em.close();
            emf.close();
        }
    }

    /* La columna tipo es un enum de la base: si le falta un valor de TipoEvento, ese aviso no se podria guardar. */
    @Test
    void laBandejaAceptaTodosLosTiposDeEvento() throws SQLException {
        String url = baseNueva();
        MigracionesBaseDeDatos.migrar(url, "sa", "");

        try (Connection c = DriverManager.getConnection(url, "sa", "");
             PreparedStatement insert = c.prepareStatement("insert into notificaciones "
                     + "(id, destinatario_id, detalle, fecha, leida, tipo, titulo) "
                     + "values (?, 'ana', '', '2027-01-01 10:00:00', false, ?, 'Aviso')")) {
            for (TipoEvento tipo : TipoEvento.values()) {
                ByteBuffer id = ByteBuffer.allocate(16);
                UUID uuid = UUID.randomUUID();
                id.putLong(uuid.getMostSignificantBits()).putLong(uuid.getLeastSignificantBits());
                insert.setBytes(1, id.array());
                insert.setString(2, tipo.name());
                assertEquals(1, insert.executeUpdate(), "la base no acepta " + tipo);
            }
        }
    }

    /* V5: la base tambien rechaza un saldo negativo, una franja sobrevendida y una solicitud sin cliente valido. */
    @Test
    void laBaseRechazaDatosQueRompenLasReglas() throws SQLException {
        String url = baseNueva();
        MigracionesBaseDeDatos.migrar(url, "sa", "");

        try (Connection c = DriverManager.getConnection(url, "sa", "")) {
            assertThrows(SQLException.class, () -> c.createStatement()
                    .executeUpdate("update billeteras set saldo = -1 where titular_id = 'bruno'"));
            assertThrows(SQLException.class, () -> c.createStatement()
                    .executeUpdate("update franjas_horarias set ocupados = cupo + 1"));
            assertThrows(SQLException.class, () -> c.createStatement().executeUpdate(
                    "insert into solicitudes (id, cliente_id, fecha_creacion, franja_id, publicacion_id) values ("
                            + "X'09000000000000000000000000000001', X'03000000000000000000000000000099', "
                            + "'2027-01-01 10:00:00', X'06000000000000000000000000000001', "
                            + "X'05000000000000000000000000000001')"));
        }
    }

    @Test
    void migrarDosVecesNoRepiteScripts() {
        String url = baseNueva();
        MigracionesBaseDeDatos.migrar(url, "sa", "");

        MigrateResult segunda = MigracionesBaseDeDatos.migrar(url, "sa", "");

        assertEquals(0, segunda.migrationsExecuted);
    }

    @Test
    void siHibernateAdministraElEsquemaFlywayNoCorre() throws SQLException {
        String url = baseNueva();
        Map<String, Object> propiedades = new HashMap<>();
        propiedades.put("jakarta.persistence.jdbc.url", url);
        propiedades.put("jakarta.persistence.jdbc.user", "sa");
        propiedades.put("jakarta.persistence.jdbc.password", "");
        propiedades.put("hibernate.hbm2ddl.auto", "create-drop");

        MigracionesBaseDeDatos.aplicarSiCorresponde(propiedades);

        assertFalse(existeTabla(url, "flyway_schema_history"));
    }

    @Test
    void conHbm2ddlNoneFlywayMigra() throws SQLException {
        String url = baseNueva();
        Map<String, Object> propiedades = new HashMap<>();
        propiedades.put("jakarta.persistence.jdbc.url", url);
        propiedades.put("jakarta.persistence.jdbc.user", "sa");
        propiedades.put("jakarta.persistence.jdbc.password", "");
        propiedades.put("hibernate.hbm2ddl.auto", "none");

        MigracionesBaseDeDatos.aplicarSiCorresponde(propiedades);

        assertTrue(existeTabla(url, "flyway_schema_history"));
        assertTrue(existeTabla(url, "contrataciones"));
    }

    @Test
    void tambienTomaLaConexionDeLasClavesDeHibernate() throws SQLException {
        String url = baseNueva();
        Map<String, Object> ajustes = new HashMap<>();
        ajustes.put("hibernate.connection.url", url);
        ajustes.put("hibernate.connection.username", "sa");
        ajustes.put("hibernate.connection.password", "");
        ajustes.put("hibernate.hbm2ddl.auto", "none");

        MigracionesBaseDeDatos.aplicarSiCorresponde(ajustes);

        assertTrue(existeTabla(url, "publicaciones"));
    }

    /* Mismo dialecto que en produccion (MySQL) para que los tipos esperados sean los de MySQL. */
    private static EntityManagerFactory validarMapeoContra(String url) {
        Map<String, Object> p = new HashMap<>();
        p.put("jakarta.persistence.jdbc.driver", "org.h2.Driver");
        p.put("jakarta.persistence.jdbc.url", url);
        p.put("jakarta.persistence.jdbc.user", "sa");
        p.put("jakarta.persistence.jdbc.password", "");
        p.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        p.put("hibernate.hbm2ddl.auto", "validate");
        p.put("hibernate.show_sql", "false");
        return Persistence.createEntityManagerFactory("skillSwapPU", p);
    }

    /* Sin distinguir mayusculas: H2 guarda en mayuscula las tablas de V1 y Flyway crea la suya en minuscula. */
    private static boolean existeTabla(String url, String tabla) throws SQLException {
        try (Connection c = DriverManager.getConnection(url, "sa", "");
             ResultSet rs = c.getMetaData().getTables(null, null, null, new String[]{"TABLE", "BASE TABLE"})) {
            while (rs.next()) {
                if (tabla.equalsIgnoreCase(rs.getString("TABLE_NAME"))) {
                    return true;
                }
            }
            return false;
        }
    }
}
