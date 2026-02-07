/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package EstructurasDeDatos;

/**
 *
 * @author Arturo
 */
public class Nodo<T> {
    
    public T dato;
    public Nodo<T> siguiente;
    
    public Nodo(Nodo<T> nodo,T dato) {
    
        this.siguiente = nodo;
        this.dato = dato;
    }
            
     public Nodo(T dato) {
    
        this.siguiente = null;
        this.dato = dato;
    }
            
      public Nodo(Nodo<T> nodo) {
    
        this.siguiente = nodo;
        this.dato = null;
    }
            
      
     public T getDato(){
         return this.dato;
     }
     
     public Nodo<T> getSiguiente() {
         return this.siguiente;
     }
    
     
     public void setDato(T dato) {
         this.dato = dato;
     }
     
     public void setSiguiente(Nodo<T> siguiente) {
         this.siguiente = siguiente;
     }
}
