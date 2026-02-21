/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Utilidades;

/**
 *
 * @author tomas
 */
import EstructurasDeDatos.ListaSimple;
import Modelo.Estado;
import Modelo.Proceso;
import Simulacion.Config;
import java.util.Random;

/**
 * Clase auxiliar para crear procesos de prueba.
 * Cumple el requerimiento de "Generar procesos aleatorios".
 */
public class GeneradorProcesos {
    
    private static int contadorIds = 1;
    private static final Random random = new Random();
    
    // Nombres aleatorios para dar realismo
    private static final String[] NOMBRES_SISTEMA = {
        "Control_Orbita", "Gestion_Bateria", "Termico_Sensor", 
        "Radio_Comms", "Watchdog_Timer"
    };
    
    private static final String[] NOMBRES_USUARIO = {
        "Procesar_Img", "Telemetria_Down", "Cifrado_Datos", 
        "Compresion_Log", "Analisis_Espectro"
    };

    /**
     * Genera una lista de procesos aleatorios listos para la simulación.
     */
    public static ListaSimple<Proceso> generarAleatorios(int cantidad) {
        ListaSimple<Proceso> lista = new ListaSimple<>();
        for (int i = 0; i < cantidad; i++) {
            lista.agregarFinal(crearProcesoAleatorio());
        }
        return lista;
    }
    
    public static Proceso crearProcesoAleatorio() {
        boolean esSistema = random.nextDouble() < 0.3; // 30% prob de ser SO
        
        String id = "P" + String.format("%03d", contadorIds++);
        String nombre;
        int prioridad;
        
        if (esSistema) {
            nombre = NOMBRES_SISTEMA[random.nextInt(NOMBRES_SISTEMA.length)];
            prioridad = 1 + random.nextInt(30); 
        } else {
            nombre = NOMBRES_USUARIO[random.nextInt(NOMBRES_USUARIO.length)];
            prioridad = 31 + random.nextInt(69);
        }
        
        // Instrucciones entre 10 y 300
        int instrucciones = 10 + random.nextInt(291);
        
        // Deadline basado en instrucciones (Holgura aleatoria)
        int deadline = instrucciones + random.nextInt(500);
        
        // Periodo
        int periodo = 0;
        if (random.nextDouble() < 0.2) { 
            periodo = deadline + random.nextInt(200);
        }
        
        // --- REQUERIMIENTO DE E/S ---
        // Generar en qué ciclo de su ejecución pedirá E/S y cuánto tardará
        int cicloExcepcion = 1 + random.nextInt(instrucciones); // Se bloquea en algún momento de su vida
        int ciclosResolver = 3 + random.nextInt(8); // Tarda entre 3 y 10 ciclos en resolverse el E/S
        
        // --- NUEVO REQUERIMIENTO: MEMORIA ---
        // Generar Memoria Requerida (entre 16MB y 256MB)
        int memoriaRequerida = 16 + random.nextInt(241); 
        
        // Creación del proceso (Asegúrate de que el constructor de Proceso reciba "memoriaRequerida")
        Proceso p = new Proceso(id, nombre, instrucciones, deadline, prioridad, esSistema, periodo, cicloExcepcion, ciclosResolver, memoriaRequerida);
        
        p.setEstado(Estado.NUEVO);
        return p;
    }
    
    public static void reiniciarContador() {
        contadorIds = 1;
    }
}