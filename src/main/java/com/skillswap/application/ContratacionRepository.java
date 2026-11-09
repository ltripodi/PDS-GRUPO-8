package com.skillswap.application;

import com.skillswap.domain.Contratacion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContratacionRepository {
    Optional<Contratacion> buscarPorId(UUID id);

    List<Contratacion> listarPorParticipante(String userId);

    void guardar(Contratacion contratacion);
}
