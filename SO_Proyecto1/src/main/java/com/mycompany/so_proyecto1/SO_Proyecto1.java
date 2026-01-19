/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.mycompany.so_proyecto1;
import EstructurasDeDatos.ListaSimple;
/**
 *
 * @author Arturo
 */
public class SO_Proyecto1 {

    public static void main(String[] args) {
        
         ListaSimple<Integer> lista = new ListaSimple();
        
         lista.agregarInicio(10);
         lista.agregarInicio(8);
         lista.agregarInicio(6);
         lista.agregarInicio(4);
         lista.agregarInicio(2);
        
         lista.agregarFinal(12);
        lista.eliminarFinal();
    
         
         lista.eliminarInicio();
         lista.print();
         
         
         
    }
}
