package com.skillswap.application;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/* Una pagina de resultados. "numero" es base 0. */
public class Pagina<T> {

    private final List<T> elementos;
    private final int numero;
    private final int tamanio;
    private final long totalElementos;

    public Pagina(List<T> elementos, int numero, int tamanio, long totalElementos) {
        Objects.requireNonNull(elementos, "Los elementos no pueden ser null");
        validarPedido(numero, tamanio);
        if (totalElementos < 0) {
            throw new IllegalArgumentException("El total de elementos no puede ser negativo");
        }
        if (elementos.size() > tamanio) {
            throw new IllegalArgumentException("La cantidad de elementos supera el tamaño de página");
        }
        this.elementos = List.copyOf(elementos);
        this.numero = numero;
        this.tamanio = tamanio;
        this.totalElementos = totalElementos;
    }

    /* Para resultados que ya estan en memoria: recorta la pagina pedida de la lista completa. */
    public static <T> Pagina<T> de(List<T> todos, int numero, int tamanio) {
        validarPedido(numero, tamanio);
        int desde = Math.min(desplazamiento(numero, tamanio), todos.size());
        int hasta = Math.min(desde + tamanio, todos.size());
        return new Pagina<>(todos.subList(desde, hasta), numero, tamanio, todos.size());
    }

    public static void validarPedido(int numero, int tamanio) {
        if (numero < 0) {
            throw new IllegalArgumentException("El número de página no puede ser negativo");
        }
        if (tamanio <= 0) {
            throw new IllegalArgumentException("El tamaño de página debe ser mayor a cero");
        }
    }

    /*
     * Cuantos elementos saltear para llegar a la pagina. Se calcula en long porque con un numero muy alto
     * numero * tamanio no entra en un int; en ese caso se topea y la pagina queda vacia.
     */
    public static int desplazamiento(int numero, int tamanio) {
        return (int) Math.min((long) numero * tamanio, Integer.MAX_VALUE);
    }

    /* Misma pagina con los elementos convertidos. La conversion recibe la lista entera (por ejemplo, para consultar en lote). */
    public <R> Pagina<R> convertir(Function<List<T>, List<R>> conversion) {
        return new Pagina<>(conversion.apply(elementos), numero, tamanio, totalElementos);
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