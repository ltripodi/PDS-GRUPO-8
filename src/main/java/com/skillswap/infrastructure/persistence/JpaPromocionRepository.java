package com.skillswap.infrastructure.persistence;

import com.skillswap.application.precio.PromocionRepository;
import com.skillswap.domain.precio.Promocion;

import java.util.List;
import java.util.UUID;

public class JpaPromocionRepository extends SimpleJpaRepository<Promocion, UUID> implements PromocionRepository {
    public JpaPromocionRepository() {
        super(Promocion.class);
    }

    /* Todas las filas de "promociones" estan vigentes: para retirar una se la borra. */
    @Override
    public List<Promocion> listarVigentes() {
        return JPAUtil.consultar(em -> em
                .createQuery("FROM Promocion p ORDER BY p.id", Promocion.class)
                .getResultList());
    }

    @Override
    public void guardar(Promocion promocion) {
        guardarEntidad(promocion);
    }
}
