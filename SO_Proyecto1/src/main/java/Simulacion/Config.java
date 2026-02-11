/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Simulacion;

/**
 *
 * @author tomas
 */

/**
 * Parámetros globales de la simulación.
 * Centraliza la configuración para facilitar ajustes.
 */

public class Config {
    
    // Memoria
    public static final int MEMORIA_TOTAL = 170; // MB o Bloques
    public static final int MEMORIA_RESERVADA_SO = 128; 
    
    // Planificación
    public static final int QUANTUM_DEFAULT = 5; // Ciclos para Round Robin
    
    // Tiempos
    public static int VELOCIDAD_RELOJ =200; // ms por ciclo (Default 1s)
    
    // Límites
    public static final int MAX_PROCESOS_SISTEMA = 5;
    
    // Paths (Para lectura de archivos)
    public static final String RUTA_ARCHIVO_PROCESOS = "procesos.csv";
    
    public static final double PROB_BLOQUEO = 0;
    
    public static int TAMANO_PROCESO = 32;
}