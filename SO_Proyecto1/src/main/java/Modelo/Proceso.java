/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Modelo;

/**
 *
 * @author Arturo
 */
public class Proceso {
    
    private String nombre;
    private int totalInstrucciones;
    private int instruccionesEjecutadas;
    private int Deadline;   // Deadline
    private int prioridad;
    

    // Constructor
    public Proceso(String nombre, int totalInstrucciones, int deadline, int prioridad) {
        this.nombre = nombre;
        this.totalInstrucciones = totalInstrucciones;
        this.instruccionesEjecutadas = 0;
        
        this.Deadline = deadline;
        this.prioridad = prioridad;
        
    }

    // Getters necesarios para la lógica de planificación
    public int getPrioridad() {
        return prioridad;
    }

    public int getDeadline() {
        return Deadline;
    }

    public int getInstruccionesRestantes() {
        return totalInstrucciones - instruccionesEjecutadas;
    }

    public String getNombre() {
        return nombre;
    }

    // Método para simular la ejecución
    public void ejecutar(int cantidad) {
        this.instruccionesEjecutadas += cantidad;
        if (this.instruccionesEjecutadas > totalInstrucciones) {
            this.instruccionesEjecutadas = totalInstrucciones;
        }
    }

    public boolean estaTerminado() {
        return instruccionesEjecutadas >= totalInstrucciones;
    }

    
}
