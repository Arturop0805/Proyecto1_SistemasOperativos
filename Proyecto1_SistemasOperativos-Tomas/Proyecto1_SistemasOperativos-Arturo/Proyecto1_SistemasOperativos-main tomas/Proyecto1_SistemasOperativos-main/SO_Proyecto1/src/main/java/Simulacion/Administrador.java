/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Simulacion;

/**
 *
 * @author tomas
 */
import EstructurasDeDatos.Cola;
import EstructurasDeDatos.ListaSimple;
import Modelo.Proceso;

/**
 * El Kernel del Sistema Operativo.
 * Administra las colas de procesos y decide quién usa la CPU.
 */
public class Administrador {
    
    private static Administrador instancia;
    
    // --- Estructuras de Datos del Kernel ---
    // CORREGIDO: Usamos solo 'Cola' para todo. Tu clase Cola ya maneja prioridades.
    // Nota: Quitamos <Proceso> porque tu clase Cola parece no ser genérica (según tus archivos).
    private Cola colaListos;           // Para FCFS / Round Robin (FIFO)
    private Cola colaListosPrioridad;  // Para SRT / Prioridad / EDF (Ordenada)
    private Cola colaBloqueados;       // Para procesos en E/S
    
    private ListaSimple<Proceso> listaTodos;    // Registro global (Bitácora)
    
    private Proceso procesoEnEjecucion; // Quien tiene la CPU ahora
    
    private Administrador() {
        // Inicializar estructuras vacías
        this.colaListos = new Cola();
        this.colaListosPrioridad = new Cola(); // Usaremos encolarConPrioridad() aquí
        this.colaBloqueados = new Cola();
        this.listaTodos = new ListaSimple<>();
        
        this.procesoEnEjecucion = null;
    }
    
    public static Administrador getInstancia() {
        if (instancia == null) {
            instancia = new Administrador();
        }
        return instancia;
    }
    
    /**
     * Este método se llama en cada "Tick" del Reloj.
     * Es el corazón de la planificación.
     */
    public void ejecutarCiclo(int numeroCiclo) {
        // Por ahora, solo verificamos conectividad
        System.out.println("[KERNEL] Ciclo " + numeroCiclo + " recibido. CPU Ocupada: " + (procesoEnEjecucion != null));
        
        // AQUÍ vendrá la lógica de planificación en la próxima fase:
        // 1. Revisar bloqueados
        // 2. Revisar si el proceso actual terminó o venció su Quantum
        // 3. Despachar siguiente proceso
    }
    
    // --- Gestión de Procesos ---
    
    public void agregarProcesoNuevo(Proceso p) {
        listaTodos.agregarFinal(p);
        // La lógica para moverlo a LISTO (Ready) irá aquí
        System.out.println("[KERNEL] Proceso creado: " + p.getId());
    }
}