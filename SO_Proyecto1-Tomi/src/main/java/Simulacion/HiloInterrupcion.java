/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Simulacion;

/**
 *
 * @author tomas
 */
import Modelo.Proceso;
import java.util.Random;

/**
 * Hilo asíncrono que simula el tiempo que tarda un dispositivo de E/S.
 */
public class HiloInterrupcion extends Thread {
    
    private Proceso proceso;

    public HiloInterrupcion(Proceso proceso) {
        this.proceso = proceso;
    }

    @Override
    public void run() {
        try {
            // Se calcula cuánto tiempo real debe dormir en base a tu configuración global
            int tiempoDormirMs = proceso.getCiclosResolver() * Config.VELOCIDAD_RELOJ;
            
            System.out.println("[THREAD-I/O] Proceso " + proceso.getId() + " usando dispositivo por " + tiempoDormirMs + "ms...");
            
            // Simular el retraso del hardware
            Thread.sleep(tiempoDormirMs);
            
            // Registrar estadísticas usando tus propios métodos
            proceso.sumarTiempoBloqueado(proceso.getCiclosResolver());
            
            System.out.println("[THREAD-I/O] Interrupción resuelta para Proceso " + proceso.getId() + ". Retornando al procesador.");
            
            // CORRECCIÓN: Volvemos a usar tu método seguro del Administrador para no romper nada
            Administrador.getInstancia().moverDeBloqueadoAListoSeguro(proceso);
            
        } catch (InterruptedException e) {
            System.out.println("[THREAD-I/O] Operación cancelada para el proceso " + proceso.getId());
        }
    }
}