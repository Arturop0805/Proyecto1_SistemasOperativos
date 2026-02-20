/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package EstructurasDeDatos;
import EstructurasDeDatos.Nodo;
/**
 *
 * @author Arturo
 */
public class ListaSimple<T> {
    
    public  Nodo<T> cabeza;
    private Integer tamaño;
    
    public ListaSimple() {
        this.cabeza = null;
        this.tamaño = 0;
    }
    
    public Integer getTamaño(){
        return this.tamaño;
    }
    
    
    public Boolean EsVacia() {
        if (this.tamaño == 0) {
            return true;
        } else {
            return false;
        }
    }
    
    public void agregarInicio(T dato){
        
       Nodo<T> nuevoNodo = new Nodo(dato);
        
        if (this.EsVacia() ){
            this.cabeza = nuevoNodo;
            this.tamaño++;
            return;
        }  
            
        nuevoNodo.siguiente = this.cabeza;
        this.cabeza = nuevoNodo;
        this.tamaño++;
        
    }
    
    public void agregarFinal(T dato){
    
        Nodo<T> nuevoNodo = new Nodo(dato);
        
        if (this.EsVacia() ){
            this.cabeza = nuevoNodo;
            this.tamaño++;
            return;
        }  
        
        Nodo<T> auxiliar = this.cabeza;
        
        while (auxiliar.siguiente != null) {
            auxiliar = auxiliar.siguiente;
           this.tamaño++;
        }
        
    auxiliar.siguiente = nuevoNodo;
    }
    
    public void eliminarInicio() {
        if (this.EsVacia()){
            System.out.println("La lista esta vacia");
            return;
        }
        
        if (this.cabeza.siguiente == null) {
            this.cabeza = null;
            this.tamaño --;
            return;
        }
        
        this.cabeza = this.cabeza.siguiente;
        this.tamaño--;
       
    }
    
    
    
    //NO USAR TODAVIA, AUN NO FUNCIONA
    public void eliminarPorIndice(Integer indice) {
    // Validar si la lista está vacía
    if (this.EsVacia()) {
        System.out.println("La lista esta vacia");
        return;
    }
    
    // Validar índice: debe ser >= 0 y < tamaño
    if (indice < 0 || indice >= this.tamaño) {
        System.out.println("El indice proporcionado es invalido (fuera de rango)");
        return;
    }
    
    // Caso especial: eliminar la cabeza (índice 0)
    if (indice == 0) {
        this.eliminarInicio();  // Asumiendo que este método ya maneja la actualización de tamaño
        return;
    }
    
    // Recorrer hasta el nodo anterior al índice
    Nodo auxiliar = this.cabeza;
    for (int contador = 0; contador < indice - 1; contador++) {
        auxiliar = auxiliar.siguiente;
    }
    
    // Eliminar el nodo: saltar el siguiente
    auxiliar.siguiente = auxiliar.siguiente.siguiente;
    
    // Decrementar el tamaño
    this.tamaño--;
}
    
    public void eliminarFinal(){
        
        
        if (this.EsVacia()) {
            System.out.println("La lista esta vacia");
            return;
        }
        
        if (this.tamaño == 1) {
            this.eliminarInicio();
            return;
        }
        
        Nodo auxiliar = this.cabeza;
        
                
        while (auxiliar.siguiente.siguiente != null){
           auxiliar = auxiliar.siguiente;
        }
        
        auxiliar.siguiente = null;
    }
    
    public void imprimir() {
        Nodo<T> auxiliar = this.cabeza;
        
        while (auxiliar != null){
            System.out.println(auxiliar.dato);
            if (auxiliar.siguiente == null){
                return;
            } else {
                auxiliar = auxiliar.siguiente;
            }
        }
        
    }
}
