package com.skillswap.infrastructure;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * Punto unico de acceso a JPA (igual que el JPAUtil del ejemplo del profe).
 *
 * - Cada operacion de un repositorio abre y cierra su propio EntityManager.
 * - Si hay una unidad transaccional en curso (ver JpaUnidadTransaccional), todos los
 *   repositorios del mismo hilo comparten el EntityManager de esa transaccion.
 */
public class JPAUtil {

    private static final String PERSISTENCE_UNIT = "skillSwapPU";

    /* Propiedades que se pueden pisar con -D (los tests las usan para apuntar a H2). */
    private static final String[] OVERRIDES = {
            "jakarta.persistence.jdbc.driver",
            "jakarta.persistence.jdbc.url",
            "jakarta.persistence.jdbc.user",
            "jakarta.persistence.jdbc.password",
            "hibernate.dialect",
            "hibernate.hbm2ddl.auto"
    };

    private static final EntityManagerFactory ENTITY_MANAGER_FACTORY = crearFactory();

    private static final ThreadLocal<EntityManager> ACTUAL = new ThreadLocal<>();
    /* Acciones que esperan el commit de la transaccion en curso (por ejemplo, avisar un evento). */
    private static final ThreadLocal<List<Runnable>> PENDIENTES = new ThreadLocal<>();
    private static final Logger LOG = Logger.getLogger(JPAUtil.class.getName());

    private JPAUtil() {
    }

    private static EntityManagerFactory crearFactory() {
        Map<String, Object> overrides = new HashMap<>();

        for (String propiedad : OVERRIDES) {
            String valor = System.getProperty(propiedad);

            if (valor != null) {
                overrides.put(propiedad, valor);
            }
        }

        return Persistence.createEntityManagerFactory(PERSISTENCE_UNIT, overrides);
    }

    public static EntityManager getEntityManager() {
        return ENTITY_MANAGER_FACTORY.createEntityManager();
    }

    /*
     * Consulta de solo lectura. Usa el EntityManager de la transaccion en curso si existe.
     */
    public static <R> R consultar(Function<EntityManager, R> operacion) {
        EntityManager actual = ACTUAL.get();

        if (actual != null) {
            return operacion.apply(actual);
        }

        EntityManager em = getEntityManager();

        try {
            return operacion.apply(em);

        } finally {
            em.close();
        }
    }

    /*
     * Ejecuta la operacion dentro de una transaccion. Si ya hay una en curso se suma a ella
     * (el commit lo hace quien la abrio).
     */
    public static <R> R transaccion(Function<EntityManager, R> operacion) {
        EntityManager actual = ACTUAL.get();

        if (actual != null) {
            return operacion.apply(actual);
        }

        EntityManager em = getEntityManager();
        ACTUAL.set(em);
        PENDIENTES.set(new ArrayList<>());
        boolean confirmada = false;

        try {
            em.getTransaction().begin();

            R resultado = operacion.apply(em);

            em.getTransaction().commit();
            confirmada = true;

            return resultado;

        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }

            throw ex;

        } finally {
            List<Runnable> pendientes = PENDIENTES.get();
            PENDIENTES.remove();
            ACTUAL.remove();
            try {
                em.close();
            } finally {
                // Recien aca: sin transaccion en curso, cada accion puede abrir la suya para guardar.
                if (confirmada) {
                    ejecutarSinPropagar(pendientes);
                }
            }
        }
    }

    /*
     * Ejecuta la accion cuando se confirma la transaccion en curso; si hay rollback, la descarta.
     * En una transaccion anidada espera al commit de la de afuera. Sin transaccion, la ejecuta ya.
     */
    public static void despuesDelCommit(Runnable accion) {
        if (accion == null) {
            throw new IllegalArgumentException("La accion es obligatoria");
        }
        List<Runnable> pendientes = PENDIENTES.get();
        if (pendientes == null) {
            accion.run();
            return;
        }
        pendientes.add(accion);
    }

    /* La operacion ya se confirmo: un error aca se registra, pero no se informa como falla a quien la pidio. */
    private static void ejecutarSinPropagar(List<Runnable> acciones) {
        for (Runnable accion : acciones) {
            try {
                accion.run();
            } catch (RuntimeException e) {
                LOG.log(Level.WARNING, "Fallo una accion posterior al commit", e);
            }
        }
    }

    public static void close() {
        ENTITY_MANAGER_FACTORY.close();
    }
}
