package com.skillswap.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;

import java.util.Map;

/*
 * Migraciones versionadas con Flyway (consigna, seccion 8: no depender de ddl-auto).
 *
 * Los scripts estan en src/main/resources/db/migration:
 *   V1__esquema_inicial.sql  -> todas las tablas, claves, indices y restricciones.
 *   V2__datos_demo.sql       -> datos de demostracion reproducibles para el entorno local.
 *   V3__...                  -> cambios posteriores del esquema.
 *
 * Flyway guarda en la tabla flyway_schema_history que versiones ya aplico, asi que cada
 * script corre una sola vez por base. Para cambiar el esquema se agrega un V3__..., nunca
 * se edita un script ya aplicado.
 */
public final class MigracionesBaseDeDatos {

    static final String UBICACION = "classpath:db/migration";

    private MigracionesBaseDeDatos() {
    }

    /*
     * Migra solo si Hibernate no administra el esquema (hibernate.hbm2ddl.auto = none, el valor
     * de persistence.xml). Los tests lo pisan con create-drop sobre H2 y ahi Flyway no corre.
     */
    static void aplicarSiCorresponde(Map<String, Object> propiedades) {
        Object ddlAuto = propiedades.get("hibernate.hbm2ddl.auto");
        if (ddlAuto != null && !"none".equalsIgnoreCase(ddlAuto.toString().trim())) {
            return;
        }
        migrar(texto(propiedades, "jakarta.persistence.jdbc.url", "hibernate.connection.url"),
                texto(propiedades, "jakarta.persistence.jdbc.user", "hibernate.connection.username"),
                texto(propiedades, "jakarta.persistence.jdbc.password", "hibernate.connection.password"));
    }

    public static MigrateResult migrar(String url, String usuario, String clave) {
        return Flyway.configure()
                .dataSource(url, usuario, clave)
                .locations(UBICACION)
                .load()
                .migrate();
    }

    /* La propiedad JPA estandar o, si no esta, su equivalente de Hibernate. */
    private static String texto(Map<String, Object> propiedades, String clave, String alternativa) {
        Object valor = propiedades.get(clave);
        if (valor == null) {
            valor = propiedades.get(alternativa);
        }
        return valor == null ? null : valor.toString();
    }
}
