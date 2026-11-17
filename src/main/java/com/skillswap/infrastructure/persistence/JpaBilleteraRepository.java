package com.skillswap.infrastructure.persistence;

import com.skillswap.application.compartido.Pagina;
import com.skillswap.application.pagos.BilleteraRepository;
import com.skillswap.domain.pagos.Billetera;
import com.skillswap.domain.pagos.Movimiento;

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
    @Override
    public Pagina<Movimiento> listarMovimientosPaginado(
            String userId, int pagina, int tamanio) {
        Pagina.validarPedido(pagina, tamanio); // antes de consultar: el tope tiene que cortar antes de la base

        return JPAUtil.consultar(em -> {

            long total = em.createQuery(
                            "SELECT COUNT(m) FROM Movimiento m "
                                    + "WHERE m.titularId = :userId",
                            Long.class)
                    .setParameter("userId", userId)
                    .getSingleResult();

            int offset = Pagina.desplazamiento(pagina, tamanio);

            if (offset >= total) {
                return new Pagina<>(List.of(), pagina, tamanio, total);
            }

            List<Movimiento> elementos = em.createQuery(
                            "FROM Movimiento m WHERE m.titularId = :userId "
                                    + "ORDER BY m.fecha DESC, m.id",
                            Movimiento.class)
                    .setParameter("userId", userId)
                    .setFirstResult(offset)
                    .setMaxResults(tamanio)
                    .getResultList();

            return new Pagina<>(
                    elementos, pagina, tamanio, total
            );
        });
    }
}
