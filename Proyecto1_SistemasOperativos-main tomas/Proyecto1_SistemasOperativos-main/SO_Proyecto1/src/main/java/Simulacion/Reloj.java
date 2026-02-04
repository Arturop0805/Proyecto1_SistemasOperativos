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
 * Hilo independiente que marca los "Ticks" del sistema.
 * Singleton: Solo existe un reloj en todo el simulador.
 */
public class Reloj extends Thread {
    
    private int cicloActual;
    private boolean ejecutando;
    private int velocidadMs; // Duración real de un ciclo simulado en ms
    
    private static Reloj instancia;

    private Reloj() {
        this.cicloActual = 0;
        this.ejecutando = false;
        this.velocidadMs = 500; // Por defecto 1 seg = 1 ciclo
    }
    
    public static Reloj getInstancia() {
        if (instancia == null) {
            instancia = new Reloj();
        }
        return instancia;
    }

    @Override
    public void run() {
        ejecutando = true;
        System.out.println(">>> RELOJ INICIADO <<<");
        
        while (ejecutando) {
            try {
                // 1. Notificar estado (Más adelante aquí llamaremos al Kernel)
                System.out.println("[RELOJ] Ciclo: " + cicloActual);
                
                // 2. Esperar tiempo real
                Thread.sleep(velocidadMs);
                
                // 3. Avanzar ciclo simulado
                cicloActual++;
                
                
                
            } catch (InterruptedException e) {
                System.out.println(">>> RELOJ INTERRUMPIDO <<<");
                ejecutando = false;
            }
        }
    }
    
    public void detener() {
        this.ejecutando = false;
    }
    
    public int getCicloActual() {
        return cicloActual;
    }
    
    public void setVelocidad(int ms) {
        this.velocidadMs = ms;
    }
}
