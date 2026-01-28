/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package EstructurasDeDatos;
import Modelo.Proceso;
/**
 *
 * @author Arturo
 */
public class Cola<T> {
    private Nodo<T> frente; 
    private Nodo<T> finalCola;  
    private int tamano;  


    // Constructor
    public Cola() {
        this.frente = null;
        this.finalCola = null;
        this.tamano = 0;
    }

    // Método para encolar (agregar al final)
    public void encolar(T dato) {
        Nodo<T> nuevoNodo = new Nodo<>(dato);
        if (estaVacia()) {
            
            frente = nuevoNodo;
            finalCola = nuevoNodo;
        } else {
            
            finalCola.siguiente = nuevoNodo;
            finalCola = nuevoNodo;
        }
        tamano++;
    }

    // Método para desencolar (remover del frente)
    public T desencolar() {
        if (estaVacia()) {
            System.out.println("la cola esta vacia");
        }
        T datoRemovido = frente.dato;
        frente = frente.siguiente;
        if (frente == null) {
            
            finalCola = null;
        }
        tamano--;
        return datoRemovido;
    }
    
   

    // Método para verificar si la cola está vacía
    public boolean estaVacia() {
        return frente == null;
    }

    // Método para obtener el tamaño
    public int tamano() {
        return tamano;
    }

    // Método para ver el frente sin remover (opcional)
    public T verFrente() {
        if (estaVacia()) {
            System.out.println("la cola esta vacia");
        }
        return frente.dato;
    }

    // Método para imprimir la cola (útil para pruebas, asume que T tiene toString())
    public void imprimir() {
        Nodo<T> actual = frente;
        while (actual != null) {
            System.out.print(actual.dato + " ");
            actual = actual.siguiente;
        }
        System.out.println();
    }
}
