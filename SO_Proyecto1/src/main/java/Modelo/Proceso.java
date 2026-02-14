package Modelo;
import Modelo.PCB;
/**
 *
 * @author tomas
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
    
    // --- NUEVAS VARIABLES PARA ESTADÍSTICAS ---
    private int tiempoPrimeraEjecucion = -1; // -1 indica que nunca se ha ejecutado
    private int tiempoTotalBloqueado = 0;
    private int tiempoFinalizacion = 0;
    

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
    public void setTiempoLlegada(long t) { 
        this.tiempoLlegada = t; 
    }
    public long getTiempoLlegada() { return this.tiempoLlegada;}  
    public int getTiempoLlegadaInt() {
        return (int) this.tiempoLlegada;
    }
    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public int getPrioridad() { return prioridadInicial; }
    public int getDeadline() { return deadline; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
    public int getPC() { return pc; }
    public int getInstruccionesRestantes() { return totalInstrucciones - pc; }
    public boolean esDeSistema() { return esDeSistema; }
    // --- MÉTODOS PARA MÉTRICAS ---
    public void setTiempoPrimeraEjecucion(int tiempo) {
        if (this.tiempoPrimeraEjecucion == -1) { // Solo guardamos la primera vez
            this.tiempoPrimeraEjecucion = tiempo;
        }
    }
    
    public int getTiempoPrimeraEjecucion() { return tiempoPrimeraEjecucion; }
    
    public void agregarTiempoBloqueado() {
        this.tiempoTotalBloqueado++;
    }
    
    public int getTiempoTotalBloqueado() { return tiempoTotalBloqueado; }
    
    public void setTiempoFinalizacion(int tiempo) { this.tiempoFinalizacion = tiempo; }
    public int getTiempoFinalizacion() { return tiempoFinalizacion; }

    @Override
    public String toString() {
        return id + " | " + nombre + " (" + estado + ")";
    }
}