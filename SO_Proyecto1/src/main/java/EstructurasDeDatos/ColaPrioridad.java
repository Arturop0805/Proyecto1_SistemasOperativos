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

//Esta clase será para las politicas de planificacion que se manejen con prioridades
public class ColaPrioridad {
    
    private Nodo<Proceso> frente; 
    private Nodo<Proceso> finalCola;  
    private int size;  


    // Constructor
    public ColaPrioridad() {
        this.frente = null;
        this.finalCola = null;
        this.size = 0;
    }
    
    public Nodo<Proceso> getFrente() {
        return this.frente;
    }
    
    public Nodo<Proceso> getFinal() {
        return this.finalCola;
    }
    
    public Boolean estaVacia(){
        if (this.size == 0){
            return true;
        } else {
            return false;
        }
    }
            
   public void encolarConPrioridad(Proceso dato) {
    Nodo<Proceso> nuevoNodo = new Nodo<>(dato);
    
    if (this.estaVacia()) {
        this.frente = nuevoNodo;
        this.finalCola = nuevoNodo;
        this.size++;
        return;
    }
    
    // Si la prioridad del nuevo nodo es menor que la del frente, insertar al frente
    if (nuevoNodo.dato.getPrioridad() < this.frente.dato.getPrioridad()) {
        nuevoNodo.siguiente = this.frente;
        this.frente = nuevoNodo;
        this.size++;
        return;
    }
    
    // Recorrer la cola hasta encontrar la posición correcta (orden ascendente por prioridad)
    Nodo<Proceso> auxiliar = this.frente;
    while (auxiliar.siguiente != null && auxiliar.siguiente.dato.getPrioridad() <= nuevoNodo.dato.getPrioridad()) {
        auxiliar = auxiliar.siguiente;
    }
    
    // Insertar el nuevo nodo después del auxiliar
    nuevoNodo.siguiente = auxiliar.siguiente;
    auxiliar.siguiente = nuevoNodo;
    
    // Si se insertó al final, actualizar el final de la cola
    if (nuevoNodo.siguiente == null) {
        this.finalCola = nuevoNodo;
    }
    
    this.size++;
}
    
    public Proceso desencolar() {
        if (estaVacia()) {
            System.out.println("la cola esta vacia");
        }
        Proceso datoRemovido = frente.dato;
        frente = frente.siguiente;
        if (frente == null) {
            
            finalCola = null;
        }
        size--;
        return datoRemovido;
    }

    public void imprimir() {
        Nodo<Proceso> actual = frente;
        while (actual != null) {
            System.out.println(actual.dato.getNombre() + " - Prioridad: " + actual.dato.getPrioridad());
            actual = actual.siguiente;
        }
        
    }
    
}
