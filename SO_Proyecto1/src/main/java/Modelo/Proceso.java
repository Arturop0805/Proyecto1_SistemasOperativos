package Modelo;

/**
 *
 * @author tomas
 */


/**
 * Representa el Bloque de Control de Proceso (PCB).
 * Almacena toda la información y estado de un proceso simulado.
 */
public class Proceso {
    
    // Identificación
    private String id;
    private String nombre;
    private boolean esSistema;
    
    // Tiempos y Planificación
    private int tiempoLlegada;
    private int tiempoPrimeraEjecucion;
    private int tiempoFinalizacion;
    private int deadline;
    private int prioridad; // 1 a 99 (menor número = mayor prioridad)
    private int periodo;
    
    // Ejecución e Instrucciones
    private int totalInstrucciones;
    private int instruccionesEjecutadas;
    
    // E/S (Bloqueos)
    private int cicloExcepcion;
    private int ciclosResolver;
    private int tiempoTotalBloqueado; // Para estadísticas
    private boolean interrupcionGenerada; // Flag para no repetir I/O
    
    // Estado
    private Estado estado;
    
    // Registros simulados (Para la Interfaz)
    private int pc;
    private int mar;

    // Memoria (NUEVO)
    private int memoriaRequerida;

    // INDICADORES ADICIONALES
    // Indica si el proceso fue abortado explícitamente por vencimiento de deadline
    private boolean abortadoPorDeadline = false;
    // Indica si el proceso actualmente reside en RAM (true) o está en swap (false)
    private boolean enMemoria = false;

    /**
     * Constructor actualizado.
     */
    public Proceso(String id, String nombre, int instrucciones, int deadline, int prioridad, 
                   boolean esSistema, int periodo, int cicloExcepcion, int ciclosResolver, int memoriaRequerida) {
        this.id = id;
        this.nombre = nombre;
        this.totalInstrucciones = instrucciones;
        this.deadline = deadline;
        this.prioridad = prioridad;
        this.esSistema = esSistema;
        this.periodo = periodo;
        this.cicloExcepcion = cicloExcepcion;
        this.ciclosResolver = ciclosResolver;
        this.memoriaRequerida = memoriaRequerida;
        
        this.instruccionesEjecutadas = 0;
        this.interrupcionGenerada = false;
        this.estado = Estado.NUEVO;
        this.tiempoLlegada = 0;
        this.tiempoPrimeraEjecucion = -1; 
        this.tiempoFinalizacion = 0;
        this.tiempoTotalBloqueado = 0;
        
        // PC y MAR inician y avanzan linealmente
        this.pc = 0;
        this.mar = 1000; // Dirección base de memoria simulada

        this.abortadoPorDeadline = false;
        this.enMemoria = false;
    }

    // --- MÉTODOS DE SIMULACIÓN DE EJECUCIÓN ---

    public void ejecutar(int cantidad) {
        if (!estaTerminado()) {
            instruccionesEjecutadas += cantidad;
            // PC y MAR incrementan una unidad por ciclo
            pc += cantidad; 
            mar += cantidad; 
            if (instruccionesEjecutadas > totalInstrucciones) {
                instruccionesEjecutadas = totalInstrucciones;
            }
        }
    }

    public boolean estaTerminado() {
        return instruccionesEjecutadas >= totalInstrucciones;
    }

    // --- GETTERS Y SETTERS ---

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public int getTotalInstrucciones() { return totalInstrucciones; }
    public int getInstruccionesEjecutadas() { return instruccionesEjecutadas; }
    public int getInstruccionesRestantes() { return totalInstrucciones - instruccionesEjecutadas; }
    public int getDeadline() { return deadline; }
    public int getPrioridad() { return prioridad; }
    public boolean isEsSistema() { return esSistema; }
    
    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
    
    public int getTiempoLlegadaInt() { return tiempoLlegada; }
    public void setTiempoLlegada(int tiempoLlegada) { this.tiempoLlegada = tiempoLlegada; }
    
    public int getTiempoPrimeraEjecucion() { return tiempoPrimeraEjecucion; }
    public void setTiempoPrimeraEjecucion(int tiempoPrimeraEjecucion) { this.tiempoPrimeraEjecucion = tiempoPrimeraEjecucion; }
    
    public int getTiempoFinalizacion() { return tiempoFinalizacion; }
    public void setTiempoFinalizacion(int tiempoFinalizacion) { this.tiempoFinalizacion = tiempoFinalizacion; }
    
    public int getCicloExcepcion() { return cicloExcepcion; }
    public int getCiclosResolver() { return ciclosResolver; }
    
    public boolean isInterrupcionGenerada() { return interrupcionGenerada; }
    public void setInterrupcionGenerada(boolean interrupcionGenerada) { this.interrupcionGenerada = interrupcionGenerada; }

    public void sumarTiempoBloqueado(int ciclos) { this.tiempoTotalBloqueado += ciclos; }
    public int getTiempoTotalBloqueado() { return tiempoTotalBloqueado; }
    
    // --- MÉTODOS DE MEMORIA ---
    public void setMemoriaRequerida(int memoria) { 
        this.memoriaRequerida = memoria; 
    }
    public void setMemoriaRequerida(double memoria) { 
        this.memoriaRequerida = (int) memoria; 
    }
    public int getMemoriaRequerida() { 
        return this.memoriaRequerida; 
    }

    // --- DEADLINE (Requerimientos) ---
    public int getDeadlineAbsoluto() {
        // El tiempo real límite es el momento en el que llegó + el plazo (deadline)
        return tiempoLlegada + deadline;
    }

    public int getDeadlineRestante(int cicloActual) {
        // Cuánto le queda = (Límite Absoluto) - (Reloj Actual)
        return getDeadlineAbsoluto() - cicloActual;
    }

    public int getPC() { return pc; }
    public int getMAR() { return mar; }

    // --- FLAG abortadoPorDeadline ---
    public boolean isAbortadoPorDeadline() { return abortadoPorDeadline; }
    public void setAbortadoPorDeadline(boolean abortadoPorDeadline) { this.abortadoPorDeadline = abortadoPorDeadline; }

    // --- FLAG enMemoria ---
    public boolean isEnMemoria() { return enMemoria; }
    public void setEnMemoria(boolean enMemoria) { this.enMemoria = enMemoria; }
}
