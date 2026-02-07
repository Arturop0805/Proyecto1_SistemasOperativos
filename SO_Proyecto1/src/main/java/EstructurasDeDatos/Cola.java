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
    
    // Recorrer la cola hasta encontrar la posición correcta (orden ascendente por prioridad)
    Nodo<Proceso> auxiliar = this.frente;
    while (auxiliar.siguiente != null && auxiliar.siguiente.dato.getPrioridad() <= nuevoNodo.dato.getPrioridad()) {
        auxiliar = auxiliar.siguiente;
    }
    
    // En Cola.java
    
    
    // Insertar el nuevo nodo después del auxiliar
    nuevoNodo.siguiente = auxiliar.siguiente;
    auxiliar.siguiente = nuevoNodo;
    
    // Si se insertó al final, actualizar el final de la cola
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

    // Método para ver el frente sin remover (opcional)
    public Proceso verFrente() {
        if (estaVacia()) {
            System.out.println("la cola esta vacia");
        }
        return frente.dato;
    }

    // Método para imprimir la cola (útil para pruebas, asume que T tiene toString())
    public void imprimir() {
        Nodo<Proceso> actual = frente;
        while (actual != null) {
            System.out.print(actual.dato + " ");
            actual = actual.siguiente;
        }
        System.out.println();
    }
}
