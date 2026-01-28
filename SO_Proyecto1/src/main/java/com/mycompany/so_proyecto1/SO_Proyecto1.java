/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.mycompany.so_proyecto1;
import EstructurasDeDatos.ListaSimple;
import EstructurasDeDatos.Cola;
import EstructurasDeDatos.ColaPrioridad;
import Modelo.Proceso;
/**
 *
 * @author Arturo
 */
public class SO_Proyecto1 {

    public static void main(String[] args) {
        
         ListaSimple<Integer> lista = new ListaSimple();
         Cola<Integer> cola = new Cola();
         ColaPrioridad SRT = new ColaPrioridad();
        
        
        Proceso proceso1 = new Proceso("proceso1",20,12,10);
        Proceso proceso2 = new Proceso("proceso2",10,38,6);
        Proceso proceso3 = new Proceso("proceso3",30,53,3);
        Proceso proceso4 = new Proceso("proceso4",60,72,12);
        Proceso proceso5 = new Proceso("proceso5",50,65,8);
               
        
        SRT.encolarConPrioridad(proceso1);
        SRT.encolarConPrioridad(proceso2);
        SRT.encolarConPrioridad(proceso3);
        SRT.encolarConPrioridad(proceso4);
        SRT.encolarConPrioridad(proceso5);
        
        SRT.imprimir();
        System.out.println(SRT.getFrente().dato.getNombre());
        System.out.println(SRT.getFinal().dato.getNombre());
        
        
        
         
    }
}
