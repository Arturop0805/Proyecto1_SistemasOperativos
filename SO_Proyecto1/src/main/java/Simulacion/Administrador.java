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
import EstructurasDeDatos.Nodo;
import Interfaces.Principal; // Importante
import Modelo.Estado;
import Modelo.Proceso;
import Utilidades.GeneradorProcesos;

public class Administrador {
    
    private static Administrador instancia;
    private Cola colaListos;
    private Cola colaListosPrioridad;
    private Cola colaBloqueados;
    private ListaSimple<Proceso> listaTodos;
    
    private Proceso procesoEnEjecucion;
    private int memoriaUsada;
    private int procesosTerminados;
    
    private Administrador() {
        this.colaListos = new Cola();
        this.colaListosPrioridad = new Cola(); 
        this.colaBloqueados = new Cola();
        this.listaTodos = new ListaSimple<>();
        this.procesoEnEjecucion = null;
        this.memoriaUsada = 0;
        this.procesosTerminados = 0;
    }
    
    public static Administrador getInstancia() {
        if (instancia == null) {
            instancia = new Administrador();
        }
        return instancia;
    }
    
    public void iniciarSimulacion(int cantidadProcesos) {
        System.out.println("[KERNEL] Inicializando simulación...");
        this.listaTodos = GeneradorProcesos.generarAleatorios(cantidadProcesos);
        this.memoriaUsada = 0;
        this.procesoEnEjecucion = null;
        this.procesosTerminados = 0;
        
        // Limpiar interfaz
        Principal.getInstancia().actualizarColaListos(colaListos);
        Principal.getInstancia().actualizarCPU(null);
    }
    
    public void ejecutarCiclo(int cicloActual) {
        
        // 0. Actualizar Reloj Visual
        Principal.getInstancia().actualizarReloj(cicloActual);

        // 1. Revisar Procesos Bloqueados (NUEVO)
        revisarColaBloqueados();

        // 2. Revisar Nuevos (Admisión)
        checkNuevosProcesos();

        // 3. Dispatcher
        if (procesoEnEjecucion == null) {
            despacharProceso();
        }

        // 4. Ejecución CPU
        if (procesoEnEjecucion != null) {
            
            // --- SIMULACIÓN DE INTERRUPCIÓN (I/O) ---
            // 1% de probabilidad de bloquearse en cada ciclo (ajustable)
            if (Math.random() < 0.05) { // 5% de probabilidad
                bloquearProcesoActual();
                return; // Salimos del ciclo porque perdió la CPU
            }
            
            boolean termino = procesoEnEjecucion.ejecutarInstruccion();
            
            // Actualizar GUI CPU
            Principal.getInstancia().actualizarCPU(procesoEnEjecucion);
            
            if (termino) {
                terminarProceso(procesoEnEjecucion);
            }
        }
    }

    // --- LÓGICA DE BLOQUEOS (NUEVA) ---

    private void bloquearProcesoActual() {
        if (procesoEnEjecucion != null) {
            // Tiempo de bloqueo aleatorio (ej. 3 a 7 ciclos)
            int tiempoBloqueo = 3 + (int)(Math.random() * 5);
            procesoEnEjecucion.establecerBloqueo(tiempoBloqueo);
            
            System.out.println("[I/O INTERRUPT] " + procesoEnEjecucion.getId() + " bloqueado por " + tiempoBloqueo + " ciclos.");
            
            // Mover a cola de bloqueados
            colaBloqueados.encolar(procesoEnEjecucion);
            procesoEnEjecucion = null; // CPU Libre
            
            // Actualizar GUIs
            Principal.getInstancia().actualizarCPU(null);
            Principal.getInstancia().actualizarColaBloqueados(colaBloqueados);
        }
    }

    private void revisarColaBloqueados() {
        if (colaBloqueados.estaVacia()) return;
        
        boolean huboCambios = false;
        
        // Necesitamos recorrer y modificar. Como tu Cola es simple, 
        // vamos a desencolar todo, actualizar y volver a encolar lo que siga bloqueado.
        // (Estrategia segura para colas simples sin iterador)
        
        int tamañoOriginal = colaBloqueados.tamano();
        
        for (int i = 0; i < tamañoOriginal; i++) {
            Proceso p = colaBloqueados.desencolar();
            
            boolean terminoEspera = p.reducirTiempoBloqueo();
            
            if (terminoEspera) {
                // Vuelve a la vida -> Cola de Listos
                p.setEstado(Estado.LISTO);
                colaListos.encolar(p);
                System.out.println("[I/O DONE] " + p.getId() + " regresa a Listos.");
                // Nota: También deberíamos actualizar la GUI de Listos
                Principal.getInstancia().actualizarColaListos(colaListos);
            } else {
                // Sigue bloqueado -> De vuelta a la cola de bloqueados
                colaBloqueados.encolar(p);
            }
            huboCambios = true;
        }
        
        if (huboCambios) {
            Principal.getInstancia().actualizarColaBloqueados(colaBloqueados);
        }
    }
    
    private void checkNuevosProcesos() {
        boolean huboCambios = false;
        Nodo<Proceso> actual = listaTodos.cabeza;
        
        while (actual != null) {
            Proceso p = actual.dato;
            if (p.getEstado() == Estado.NUEVO) {
                if (memoriaUsada + 64 <= Config.MEMORIA_TOTAL) {
                    p.setEstado(Estado.LISTO);
                    p.setTiempoLlegada(System.currentTimeMillis());
                    colaListos.encolar(p);
                    memoriaUsada += 64;
                    huboCambios = true; // Marcamos que la cola cambió
                    System.out.println("[ADMISION] " + p.getId() + " -> Ready.");
                }
            }
            actual = actual.siguiente;
        }
        
        // Si hubo cambios, refrescamos la GUI
        if (huboCambios) {
            Principal.getInstancia().actualizarColaListos(colaListos);
        }
    }
    
    private void despacharProceso() {
        if (!colaListos.estaVacia()) {
            Proceso p = colaListos.desencolar();
            p.setEstado(Estado.EJECUCION);
            this.procesoEnEjecucion = p;
            
            // Actualizar GUI: Quitamos de la cola y ponemos en CPU
            Principal.getInstancia().actualizarColaListos(colaListos);
            Principal.getInstancia().actualizarCPU(p);
            
            System.out.println("[DISPATCH] " + p.getId() + " entra a CPU.");
        }
    }
    
    private void terminarProceso(Proceso p) {
        p.setEstado(Estado.TERMINADO);
        this.procesoEnEjecucion = null;
        this.memoriaUsada -= 64;
        this.procesosTerminados++;
        
        // Actualizar GUI CPU (ponerla libre)
        Principal.getInstancia().actualizarCPU(null);
        System.out.println("[TERMINADO] " + p.getId());
    }
}