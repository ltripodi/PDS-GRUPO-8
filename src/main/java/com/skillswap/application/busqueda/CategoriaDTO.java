package com.skillswap.application.busqueda;

import java.util.UUID;

/* Opcion del filtro de categoria: la interfaz muestra el nombre y busca por el id. */
public record CategoriaDTO(UUID id, String nombre) {
}