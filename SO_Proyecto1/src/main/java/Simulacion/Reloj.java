



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
    private boolean pausado; // Nuevo flag
    private int velocidadMs;
    
    private static Reloj instancia;

    private Reloj() {
        this.cicloActual = 0;
        this.ejecutando = false;
        this.pausado = false;
        this.velocidadMs = 1000;
        
        if (Config.VELOCIDAD_RELOJ > 0) {
            this.velocidadMs = Config.VELOCIDAD_RELOJ;
        }
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
                if (pausado) {
                    Thread.sleep(100); // Espera pasiva mientras está pausado
                    continue;
                }

                // 1. Notificar al Kernel
                Administrador.getInstancia().ejecutarCiclo(cicloActual);
                
                // 2. Avanzar tiempo
                Thread.sleep(velocidadMs);
                cicloActual++;
                
            } catch (InterruptedException e) {
                System.out.println(">>> RELOJ INTERRUMPIDO <<<");
                ejecutando = false;
            }
        }
    }
    
    // --- Control de Pausa ---
    public void pausar() { this.pausado = true; }
    public void reanudar() { this.pausado = false; }
    public boolean isPausado() { return pausado; }
    
    public void detener() { this.ejecutando = false; }
    public int getCicloActual() { return cicloActual; }
    public void setVelocidad(int ms) { this.velocidadMs = ms; }
}

