package Modelo;
import Modelo.PCB;
/**
 * Clase Proceso (PCB) Actualizada.
 * Compatible con la ColaPrioridad existente y con los requisitos del RTOS.
 */
public class Proceso {
    
    // --- Configuración Estática ---
    private String id;               
    private String nombre;
    private int totalInstrucciones;
    private int prioridadInicial;
    private int deadline;            
    private boolean esDeSistema;     // true = SO, false = Usuario
    private int periodo; 
    private PCB PCB;
  // 0 = Aperiodico, >0 = Periodico
    
    // --- Estado Dinámico ---
    private int instruccionesEjecutadas; // Program Counter (PC)
    private int mar;                     // Memory Address Register
    private Estado estado;               
    private int ciclosBloqueadoRestantes;// Contador para simular I/O
    private long tiempoLlegada;          

    /**
     * Constructor Completo
     */
    public Proceso(String id, String nombre, int totalInstrucciones, int deadline, int prioridad, boolean esSistema, int periodo) {
        this.id = id;
        this.nombre = nombre;
        this.totalInstrucciones = totalInstrucciones;
        this.deadline = deadline;
        this.prioridadInicial = prioridad;
        this.esDeSistema = esSistema;
        this.periodo = periodo;
        
        // Inicialización
        this.instruccionesEjecutadas = 0;
        this.mar = 0;
        this.estado = Estado.NUEVO;
        this.ciclosBloqueadoRestantes = 0;
    }
    
    public Proceso(String nombre, int numInstrucciones, int deadline, int prioridad ) {
        this.nombre = nombre;
        this.totalInstrucciones = numInstrucciones;
        this.deadline = deadline;
        this.prioridadInicial = prioridad;
    }
            
    
    // --- Simulación de CPU ---

    /**
     * Ejecuta UNA sola instrucción (1 ciclo).
     * @return true si el proceso termina en este ciclo.
     */
    public boolean ejecutarInstruccion() {
        if (instruccionesEjecutadas < totalInstrucciones) {
            instruccionesEjecutadas++;
            mar++;
        }
        return estaTerminado();
    }
    
    /**
     * Mantiene compatibilidad con código legacy si lo usas.
     */
    public void ejecutar(int cantidad) {
        for(int i=0; i<cantidad; i++) {
            if(!estaTerminado()) ejecutarInstruccion();
        }
    }

    public boolean estaTerminado() {
        return instruccionesEjecutadas >= totalInstrucciones;
    }
    
    // --- Manejo de E/S (Bloqueos) ---
    
    public void establecerBloqueo(int ciclos) {
        this.ciclosBloqueadoRestantes = ciclos;
        this.estado = Estado.BLOQUEADO;
    }
    
    public boolean reducirTiempoBloqueo() {
        if (ciclosBloqueadoRestantes > 0) {
            ciclosBloqueadoRestantes--;
        }
        // Retorna true si ya terminó de esperar (listo para volver a Ready)
        return ciclosBloqueadoRestantes == 0; 
    }

    // --- Getters (Legacy & Nuevos) ---
    
    public int getPrioridad() { return prioridadInicial; }
    public int getDeadline() { return deadline; }
    public String getNombre() { return nombre; }
    
    public String getId() { return id; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
    public int getInstruccionesRestantes() { return totalInstrucciones - instruccionesEjecutadas; }
    public int getPC() { return instruccionesEjecutadas; }
    public boolean esDeSistema() { return esDeSistema; }
    public void setTiempoLlegada(long t) { this.tiempoLlegada = t; }
    
    @Override
    public String toString() {
        return id + " | " + nombre + " (" + estado + ")";
    }
}
