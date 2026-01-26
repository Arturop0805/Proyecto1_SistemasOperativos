/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.mycompany.so_proyecto1;
import EstructurasDeDatos.ListaSimple;
import EstructurasDeDatos.Cola;
/**
 *
 * @author Arturo
 */
public class SO_Proyecto1 {

    public static void main(String[] args) {
        
         ListaSimple<Integer> lista = new ListaSimple();
         Cola<Integer> cola = new Cola();
        
        
        
         
        cola.encolar(5);
        cola.encolar(3);
        cola.encolar(2);
        cola.encolar(1);
         
        
         
       cola.imprimir();
         
       cola.desencolar();
       
       cola.imprimir();
               
         
    }
}
