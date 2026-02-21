/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package EstructurasDeDatos;

/**
 *
 * @author Arturo
 */
import Modelo.Proceso;


public class Cola {
    private Nodo<Proceso> frente; 
    private Nodo<Proceso> finalCola;  
    private int tamano;  


    // Constructor
    public Cola() {
        this.frente = null;
        this.finalCola = null;
        this.tamano = 0;
    }

    // Método para encolar (agregar al final)
    public void encolar(Proceso dato) {
        Nodo<Proceso> nuevoNodo = new Nodo<>(dato);
        if (estaVacia()) {
            frente = nuevoNodo;
            finalCola = nuevoNodo;
        } else {
            finalCola.siguiente = nuevoNodo;
            finalCola = nuevoNodo;
        }
        tamano++;
    }
    
    public void encolarConPrioridad(Proceso dato) {
        Nodo<Proceso> nuevoNodo = new Nodo<Proceso>(dato);
        
        if (this.estaVacia()) {
            this.frente = nuevoNodo;
            this.finalCola = nuevoNodo;
            this.tamano++;
            return;
        }
        
        // Si la prioridad del nuevo nodo es menor que la del frente, insertar al frente
        if (nuevoNodo.dato.getPrioridad() < this.frente.dato.getPrioridad()) {
            nuevoNodo.siguiente = this.frente;
            this.frente = nuevoNodo;
            this.tamano++;
            return;
        }
        
        Nodo<Proceso> actual = this.frente;
        while (actual.siguiente != null && actual.siguiente.dato.getPrioridad() <= nuevoNodo.dato.getPrioridad()) {
            actual = actual.siguiente;
        }
        
        nuevoNodo.siguiente = actual.siguiente;
        actual.siguiente = nuevoNodo;
        
        if (nuevoNodo.siguiente == null) {
            this.finalCola = nuevoNodo;
        }
        this.tamano++;
    }
    
    public Nodo<Proceso> getFrente() {
        return this.frente;
    }
    
    // Método para desencolar (remover del frente)
    public Proceso desencolar() {
        if (estaVacia()) {
            System.out.println("la cola esta vacia");
            return null;
        }
        Proceso datoRemovido = frente.dato;
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

    // Método para ver el frente sin remover
    public Proceso verFrente() {
        if (estaVacia()) {
            System.out.println("la cola esta vacia");
            return null;
        }
        return frente.dato;
    }

    // --- FIX BUG 3: MÉTODO NUEVO NECESARIO PARA ELIMINAR PROCESOS FANTASMAS ---
    // Este método remueve un proceso específico sin importar en qué parte de la cola esté
    public void removerProceso(String idProceso) {
        if (estaVacia()) return;

        // Si el proceso a eliminar está en el frente de la cola
        if (frente.dato.getId().equals(idProceso)) {
            desencolar();
            return;
        }

        Nodo<Proceso> actual = frente;
        while (actual.siguiente != null) {
            if (actual.siguiente.dato.getId().equals(idProceso)) {
                // Se salta el nodo para eliminarlo
                actual.siguiente = actual.siguiente.siguiente;
                tamano--;
                
                // Si eliminamos el último, actualizamos finalCola
                if (actual.siguiente == null) {
                    finalCola = actual;
                }
                return;
            }
            actual = actual.siguiente;
        }
    }
    // --------------------------------------------------------------------------

    public void imprimir() {
        Nodo<Proceso> actual = frente;
        while (actual != null) {
            System.out.print(actual.dato.getId() + " - ");
            actual = actual.siguiente;
        }
        System.out.println();
    }
}
