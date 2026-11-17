package com.skillswap.application.contratacion;

import com.skillswap.application.compartido.Pagina;
import com.skillswap.domain.contratacion.Contratacion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContratacionRepository {
    Optional<Contratacion> buscarPorId(UUID id);

    List<Contratacion> listarPorParticipante(String userId);
    default Pagina<Contratacion> listarPorParticipantePaginado(
            String userId, int pagina, int tamanio) {

        return Pagina.de(
                listarPorParticipante(userId),
                pagina,
                tamanio
        );
    }

    void guardar(Contratacion contratacion);
}
