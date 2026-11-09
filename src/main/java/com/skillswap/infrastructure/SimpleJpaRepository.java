package com.skillswap.infrastructure;

import java.util.Optional;

/*
 * CRUD comun para los repositorios JPA (equivalente al SimpleJpaRepository del ejemplo del profe).
 * Los repositorios concretos solo implementan sus consultas especificas.
 */
public abstract class SimpleJpaRepository<T, ID> {

    private final Class<T> entityClass;

    protected SimpleJpaRepository(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    protected T guardarEntidad(T entidad) {
        return JPAUtil.transaccion(em -> em.merge(entidad));
    }

    protected Optional<T> buscarEntidadPorId(ID id) {
        return JPAUtil.consultar(em -> Optional.ofNullable(em.find(entityClass, id)));
    }
}
