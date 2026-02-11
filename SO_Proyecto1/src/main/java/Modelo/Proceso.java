package Modelo;
import Modelo.PCB;
/**
 *
 * @author tomas
 */

/**
 * El Proceso actúa como su propio PCB.
 * Contiene identificadores, estado y contadores de ejecución.
 */
/**
 * El Proceso actúa como su propio PCB.
 * Contiene identificadores, estado y contadores de ejecución.
 */
public class Proceso {
    
    // Datos Estáticos
    private String id;               
    private String nombre;
    private int totalInstrucciones;
    private int prioridadInicial;
    private int deadline;  
    private int tamano_proceso;

    private boolean esDeSistema;     // true = SO, false = Usuario
    private int periodo; 
    private PCB PCB;
  // 0 = Aperiodico, >0 = Periodico
             

    
    // Datos Dinámicos (El contexto de ejecución)
    private int pc;                      // Program Counter
    private int mar;                     // Memory Address Register
    private Estado estado;               
    private int ciclosBloqueadoRestantes;
    private long tiempoLlegada;          // <--- Variable necesaria para el error

    public Proceso(String id, String nombre, int totalInstrucciones, int deadline, int prioridad, boolean esSistema, int periodo) {
        this.id = id;
        this.nombre = nombre;
        this.totalInstrucciones = totalInstrucciones;
        this.deadline = deadline;
        this.prioridadInicial = prioridad;
        this.esDeSistema = esSistema;
        this.periodo = periodo;
        
        
        // Inicialización de registros
        this.pc = 0;
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

    // --- Lógica de CPU ---

    public boolean ejecutarInstruccion() {
        if (pc < totalInstrucciones) {
            pc++;
            mar++;
        }
        return estaTerminado();
    }
    
    public boolean estaTerminado() {
        return pc >= totalInstrucciones;
    }
    
    // --- Lógica de Bloqueo (E/S) ---
    
    public void establecerBloqueo(int ciclos) {
        this.ciclosBloqueadoRestantes = ciclos;
        this.estado = Estado.BLOQUEADO;
    }
    
    public boolean reducirTiempoBloqueo() {
        if (ciclosBloqueadoRestantes > 0) {
            ciclosBloqueadoRestantes--;
        }
        return ciclosBloqueadoRestantes == 0; 
    }

    // --- Getters y Setters ---
    
    public Integer getTotalInstrucciones(){return this.totalInstrucciones;}
    public Integer getMAR(){return this.mar;}
    
    public void setTiempoLlegada(long t) { this.tiempoLlegada = t; }
    public long getTiempoLlegada() { return tiempoLlegada; }       
    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public int getPrioridad() { return prioridadInicial; }
    public int getDeadline() { return deadline; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
    public int getPC() { return pc; }
    public int getInstruccionesRestantes() { return totalInstrucciones - pc; }
    public boolean esDeSistema() { return esDeSistema; }
    
    @Override
    public String toString() {
        return id + " | " + nombre + " (" + estado + ")";
    }
}