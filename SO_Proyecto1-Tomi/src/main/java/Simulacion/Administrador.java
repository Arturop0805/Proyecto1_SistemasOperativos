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
import Modelo.Estado;
import Modelo.Proceso;
import java.util.Random;
import java.util.concurrent.Semaphore;

/**
 * El "Cerebro" del Simulador (Kernel / OS).
 * Se encarga de la Memoria, la CPU y la Planificación de procesos.
 */
public class Administrador {
    
    private static Administrador instancia;
    
    // --- Estructuras de Datos (Memoria y Disco) ---
    private Cola colaListos;                  
    private Cola colaListosPrioridad;       
    private Cola colaBloqueados;              
    private Cola colaListosSuspendidos;   
    private Cola colaBloqueadosSuspendidos;   
    private ListaSimple<Proceso> listaTodosProcesos; 
    
    // Protege el acceso a las colas para que los hilos no choquen
    private Semaphore mutexColas = new Semaphore(1);
    
    // --- Estado del Sistema ---
    private Proceso procesoEnEjecucion; 
    private int relojDelSistema = 0;
    private String politicaActual = "FCFS";
    private int quantumActual = Config.QUANTUM_DEFAULT;
    private int contadorCiclosCPU = 0; 
    
    // --- Estadísticas ---
    private int totalProcesosTerminados = 0;
    private int totalProcesosCumplenDeadline = 0;
    private int sumaTiempoRespuesta = 0; 
    private int sumaTiempoBloqueado = 0; 
    
    private Random random = new Random();
    private Runnable actualizadorVisual; 

    private Administrador() {
        colaListos = new Cola();
        colaListosPrioridad = new Cola();
        colaBloqueados = new Cola();
        colaListosSuspendidos = new Cola();
        colaBloqueadosSuspendidos = new Cola();
        listaTodosProcesos = new ListaSimple<>();
    }

    public static Administrador getInstancia() {
        if (instancia == null) {
            instancia = new Administrador();
        }
        return instancia;
    }

    public void setActualizadorVisual(Runnable actualizador) {
        this.actualizadorVisual = actualizador;
    }

    public void iniciarSimulacion(int cicloInicial) {
        this.relojDelSistema = cicloInicial;
        System.out.println("SISTEMA OPERATIVO UNIMET-SAT INICIADO.");
    }
    
    // Método para agregar procesos
    public void agregarProceso(Proceso p) {
        try {
            mutexColas.acquire();
            p.setEstado(Estado.LISTO);
            p.setTiempoLlegada(relojDelSistema);
            // CORRECCIÓN: Tu cola recibe el Proceso directo, sin Nodo
            colaListos.encolar(p);
            listaTodosProcesos.agregarFinal(p);
        } catch (InterruptedException e) {
            System.out.println("Error al agregar proceso: " + e.getMessage());
        } finally {
            mutexColas.release();
        }
    }

    public void ejecutarCiclo(int cicloActual) {
        this.relojDelSistema = cicloActual;
        
        try {
            mutexColas.acquire();
            
            actualizarProcesosBloqueados();
            verificarDeadlines();
            ejecutarProcesoEnCPU();

            if (procesoEnEjecucion == null) {
                planificarSiguienteProceso();
            }
            
        } catch (InterruptedException e) {
            System.out.println("Error de concurrencia: " + e.getMessage());
        } finally {
            mutexColas.release();
        }

        if (actualizadorVisual != null) {
            actualizadorVisual.run();
        }
    }

    private void actualizarProcesosBloqueados() {
        Cola colaTemporal = new Cola();
        
        while (colaBloqueados != null && !colaBloqueados.estaVacia()) {
            // CORRECCIÓN: Tu método desencolar devuelve el Proceso
            Proceso p = colaBloqueados.desencolar();
            if (p != null) {
                sumaTiempoBloqueado++; 
                
                boolean terminoI_O = p.reducirTiempoBloqueo();
                
                if (terminoI_O) {
                    p.setEstado(Estado.LISTO);
                    // CORRECCIÓN: Encolar directo
                    colaListos.encolar(p);
                    System.out.println("Proceso " + p.getId() + " finalizó E/S y volvió a Listos.");
                } else {
                    // CORRECCIÓN: Encolar directo
                    colaTemporal.encolar(p);
                }
            }
        }
        colaBloqueados = colaTemporal;
    }

    private void verificarDeadlines() {
        // Placeholder para validación RTOS
    }

    private void ejecutarProcesoEnCPU() {
        if (procesoEnEjecucion != null) {
            
            boolean termino = procesoEnEjecucion.ejecutarInstruccion();
            
            if (!termino && random.nextDouble() < Config.PROB_BLOQUEO) {
                System.out.println("Excepción de E/S generada por Proceso " + procesoEnEjecucion.getId());
                procesoEnEjecucion.establecerBloqueo(5);
                // CORRECCIÓN: Encolar directo
                colaBloqueados.encolar(procesoEnEjecucion);
                procesoEnEjecucion = null;
                return;
            }
            
            if (termino) {
                System.out.println("Proceso " + procesoEnEjecucion.getId() + " TERMINADO.");
                procesoEnEjecucion.setEstado(Estado.TERMINADO);
                
                totalProcesosTerminados++;
                if (relojDelSistema <= procesoEnEjecucion.getTiempoLlegada() + procesoEnEjecucion.getDeadline()) {
                    totalProcesosCumplenDeadline++;
                }
                sumaTiempoRespuesta += (relojDelSistema - procesoEnEjecucion.getTiempoLlegada());
                
                procesoEnEjecucion = null;
                return;
            }

            if (politicaActual.equals("Round Robin")) {
                contadorCiclosCPU++;
                if (contadorCiclosCPU >= quantumActual) {
                    System.out.println("Fin de Quantum para " + procesoEnEjecucion.getId());
                    procesoEnEjecucion.setEstado(Estado.LISTO);
                    // CORRECCIÓN: Encolar directo
                    colaListos.encolar(procesoEnEjecucion);
                    procesoEnEjecucion = null;
                    contadorCiclosCPU = 0;
                }
            }
        }
    }

    private void planificarSiguienteProceso() {
        if (colaListos == null || colaListos.estaVacia()) return; 

        // CORRECCIÓN: desencolar devuelve el Proceso directo
        Proceso p = colaListos.desencolar();
        if (p != null) {
            procesoEnEjecucion = p;
            procesoEnEjecucion.setEstado(Estado.EJECUCION);
            contadorCiclosCPU = 0; 
        }
    }

    // --- GETTERS Y SETTERS ---
    public Cola getColaListos() { return colaListos; }
    public Cola getColaListosPrioridad() { return colaListosPrioridad; } 
    public Cola getColaBloqueados() { return colaBloqueados; }
    public Cola getColaListosSuspendidos() { return colaListosSuspendidos; }
    public Cola getColaBloqueadosSuspendidos() { return colaBloqueadosSuspendidos; }
    public Proceso getProcesoEnEjecucion() { return procesoEnEjecucion; }
    public int getRelojSistema() { return relojDelSistema; }
    public Semaphore getMutexColas() { return mutexColas; }
    
    // --- ESTADÍSTICAS ---
    public String obtenerReporteEstadisticas() {
        double porcentajeDeadline = totalProcesosTerminados > 0 ? ((double) totalProcesosCumplenDeadline / totalProcesosTerminados) * 100 : 0;
        double promedioRespuesta = totalProcesosTerminados > 0 ? (double) sumaTiempoRespuesta / totalProcesosTerminados : 0;
        double promedioBloqueo = totalProcesosTerminados > 0 ? (double) sumaTiempoBloqueado / totalProcesosTerminados : 0;

        StringBuilder sb = new StringBuilder();
        sb.append("=== REPORTE DE RENDIMIENTO ===\n");
        sb.append("Procesos Terminados: ").append(totalProcesosTerminados).append("\n");
        sb.append("----------------------------\n");
        sb.append(String.format("1. Cumplimiento de Deadline: %.2f%%\n", porcentajeDeadline));
        sb.append("   (Procesos que finalizaron a tiempo)\n\n");
        sb.append(String.format("2. Tiempo Promedio de Respuesta: %.2f ciclos\n", promedioRespuesta));
        sb.append("   (Tiempo desde llegada hasta ejecución)\n\n");
        sb.append(String.format("3. Tiempo Promedio Bloqueado: %.2f ciclos\n", promedioBloqueo));
        return sb.toString();
    }
}