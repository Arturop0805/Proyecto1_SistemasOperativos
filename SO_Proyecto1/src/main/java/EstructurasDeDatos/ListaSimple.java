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
        
        
        if (this.EsVacia()){
            System.out.println("La lista esta vacia");
            return;
        }
        
        if (indice > this.tamaño){
            System.out.println("El indice proporcionado es mayor al tamaño de lista");
            return;
        }
        
        if (indice == 0){
            this.eliminarInicio();
        }
        
        int contador = 0;
        
        Nodo auxiliar = this.cabeza;
        
        while (contador <= this.tamaño){
            if (contador+1 == indice){
                
             auxiliar.siguiente = auxiliar.siguiente.siguiente;
            }
        }
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
    
    public void print() {
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
