package Modelo;

/**
 *
 * @author tomas
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
    private boolean esDeSistema;     
    private int periodo;             
    
    // Datos Dinámicos (El contexto de ejecución)
    private int pc;                      // Program Counter
    private int mar;                     // Memory Address Register
    private Estado estado;               
    private int ciclosBloqueadoRestantes;
    private long tiempoLlegada;          

    // CONSTRUCTOR 1: El que requiere el nuevo diseño (7 argumentos)
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

    // CONSTRUCTOR 2: Sobrecarga de seguridad por si Principal.java u otro archivo sigue mandando 9 parámetros.
    // Simplemente ignoramos los 2 extras para que compile perfecto.
    public Proceso(String id, String nombre, int totalInstrucciones, int pc, int mar, boolean esSistema, int estadoNum, int prioridad, int deadline) {
        this(id, nombre, totalInstrucciones, deadline, prioridad, esSistema, 0);
    }
    
    // --- Lógica de CPU (MÉTODOS FALTANTES AÑADIDOS) ---

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
    
    // --- Lógica de Bloqueo / E/S (MÉTODOS FALTANTES AÑADIDOS) ---
    
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
    
    // MÉTODOS AÑADIDOS PARA ARREGLAR VentanaInfoProceso.java
    public int getTotalInstrucciones() { return totalInstrucciones; }
    public int getMAR() { return mar; }
    
    @Override
    public String toString() {
        return id + " | " + nombre + " (" + estado + ")";
    }
}