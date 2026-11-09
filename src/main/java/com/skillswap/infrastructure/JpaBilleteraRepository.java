package com.skillswap.infrastructure;

import com.skillswap.application.BilleteraRepository;
import com.skillswap.domain.Billetera;
import com.skillswap.domain.Movimiento;
import jakarta.persistence.Id;

import java.util.List;

public class JpaBilleteraRepository extends SimpleJpaRepository<Billetera, String> implements BilleteraRepository {
    public JpaBilleteraRepository() {
        super(Billetera.class);
    }

    @Override
    public Billetera buscarPorTitular(String userId) {
        return buscarEntidadPorId(userId).orElse(null);
    }

    @Override
    public void guardar(Billetera billetera) {
        guardarEntidad(billetera);
    }

    @Override
    public List<Movimiento> listarMovimientos(String userId) {
        return JPAUtil.consultar(em -> em
                .createQuery("FROM Movimiento m WHERE m.titularId = :userId ORDER BY m.fecha DESC, m.id", Movimiento.class)
                .setParameter("userId", userId)
                .getResultList());
    }
}
