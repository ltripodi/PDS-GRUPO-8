package com.skillswap.infrastructure.persistence;

import com.skillswap.application.pagos.PagoRepository;
import com.skillswap.domain.pagos.Pago;

import jakarta.persistence.Id;

import java.util.Optional;
import java.util.UUID;

public class JpaPagoRepository extends SimpleJpaRepository<Pago, UUID> implements PagoRepository {
    public JpaPagoRepository() {
        super(Pago.class);
    }

    @Override
    public Optional<Pago> buscarPorContratacion(UUID contratacionId) {
        return JPAUtil.consultar(em -> em
                .createQuery("FROM Pago p WHERE p.contratacionId = :contratacionId", Pago.class)
                .setParameter("contratacionId", contratacionId)
                .getResultList()
                .stream()
                .findFirst());
    }

    @Override
    public void guardar(Pago pago) {
        guardarEntidad(pago);
    }
}
