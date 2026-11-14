package com.skillswap.infrastructure;

import com.skillswap.domain.CanalNotificacion;
import com.skillswap.domain.EstadoPublicacion;
import com.skillswap.domain.Perfil;
import com.skillswap.domain.Publicacion;
import com.skillswap.domain.TipoPublicacion;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * Migraciones Flyway (consigna, seccion 8). Cada test usa su propia base H2 vacia en modo MySQL.
 *
 * La prueba clave: despues de migrar, Hibernate arranca con hbm2ddl.auto=validate. Si una tabla,
 * columna o tipo de V1 no coincide con orm.xml, la EntityManagerFactory no se puede crear.
 */
class MigracionesTest {

    private static final UUID ANA = UUID.fromString("03000000-0000-0000-0000-000000000001");
    private static final UUID DESAFIO_ACME = UUID.fromString("05000000-0000-0000-0000-000000000006");
    private static final UUID ACME = UUID.fromString("04000000-0000-0000-0000-000000000001");

    private static String baseNueva() {
        return "jdbc:h2:mem:migracion_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
    }

    @Test
    void lasMigracionesCreanUnEsquemaQueCoincideConElMapeoYCarganLaDemo() {
        String url = baseNueva();

        MigrateResult resultado = MigracionesBaseDeDatos.migrar(url, "sa", "");
        assertEquals(2, resultado.migrationsExecuted);

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

            Publicacion desafio = em.find(Publicacion.class, DESAFIO_ACME);
            assertEquals(TipoPublicacion.DESAFIO, desafio.getTipo());
            assertEquals(ACME, desafio.getAutorId());
            assertEquals("PUBLICADA", desafio.getHistorial().get(0).getDestino());
        } finally {
            em.close();
            emf.close();
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
