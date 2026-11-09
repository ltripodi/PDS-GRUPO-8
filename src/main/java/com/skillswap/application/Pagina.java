package com.skillswap.application;

import java.util.List;

/** DTO. */
public class Pagina<T> {
    private List<T> elementos;
    private int numero;
    private int tamanio;
    private long totalElementos;

    public Pagina(List<T> elementos, int numero, int tamanio, long totalElementos) {
        this.elementos = elementos;
        this.numero = numero;
        this.tamanio = tamanio;
        this.totalElementos = totalElementos;
    }

    public List<T> getElementos() {
        return elementos;
    }

    public int getNumero() {
        return numero;
    }

    public int getTamanio() {
        return tamanio;
    }

    public long getTotalElementos() {
        return totalElementos;
    }
}
