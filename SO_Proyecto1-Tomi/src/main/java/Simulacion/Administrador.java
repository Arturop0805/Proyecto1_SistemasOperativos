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
import Modelo.Estado;
import Modelo.Proceso;
import Utilidades.GeneradorProcesos;
import java.util.concurrent.Semaphore;

/**
 * Kernel del Sistema Operativo.
 * Controla el ciclo de vida de los procesos, las colas y el dispatcher.
 */
public class Administrador {
    
    // Singleton
    private static Administrador instancia;
    
    // Estructuras de Datos
    private Cola colaListos;
    private Cola colaListosPrioridad; // Para SRT y Prioridades a futuro
    private Cola colaBloqueados;
    private Cola colaListosSuspendidos; // Swap de Memoria
    private Cola colaBloqueadosSuspendidos; // NUEVO: Soluciona el error en Principal
    private ListaSimple<Proceso> listaTodos; // Historial y procesos por llegar
    
    // SEMÁFORO NUEVO: Protege las colas para que el Reloj y el Hilo de E/S no choquen
    private Semaphore mutexColas = new Semaphore(1);
    
    // Variables de Estado
    private String politicaActual = "FCFS";
    private int contadorQuantum = 0;
    private int relojDelSistema = 0;
    private Proceso procesoEnEjecucion;
    
    // NUEVO: Variable dinámica para el Quantum
    private int quantumActual = Config.QUANTUM_DEFAULT;
    
    // NUEVO: Variables de Memoria RAM
    private int memoriaRAMDisponible;
    
    // Variable para conectar con la GUI
    private Runnable actualizadorVisual;
    
    // Variables para Estadísticas
    private int totalProcesosTerminados = 0;
    private int totalProcesosCumplenDeadline = 0;
    private int sumaTiempoRespuesta = 0;
    private int sumaTiempoBloqueado = 0;

    // Constructor privado (Singleton)
    private Administrador() {
        this.colaListos = new Cola();
        this.colaListosPrioridad = new Cola();
        this.colaBloqueados = new Cola();
        this.colaListosSuspendidos = new Cola();
        this.colaBloqueadosSuspendidos = new Cola(); // INICIALIZADO
        this.listaTodos = new ListaSimple<>();
        
        // Inicializar Memoria Disponible descontando la del SO
        this.memoriaRAMDisponible = Config.MEMORIA_TOTAL - Config.MEMORIA_RESERVADA_SO;
    }
    
    // Método para obtener la única instancia del Administrador
    public static Administrador getInstancia() {
        if (instancia == null) {
            instancia = new Administrador();
        }
        return instancia;
    }

    /**
     * Método llamado desde Principal para arrancar la simulación
     * con una cantidad específica de procesos iniciales.
     */
    public void iniciarSimulacion(int numProcesos) {
        System.out.println("[KERNEL] Iniciando simulación con " + numProcesos + " procesos.");
        for (int i = 0; i < numProcesos; i++) {
            ListaSimple<Proceso> generados = GeneradorProcesos.generarAleatorios(1);
            if (generados.cabeza != null) {
                Proceso p = generados.cabeza.dato;
                p.setTiempoLlegada(i * 2); 
                listaTodos.agregarFinal(p);
            }
        }
    }
    
    // --- NUEVO MÉTODO: Admitir proceso directo desde la Interfaz (Botones Crear) ---
    public void admitirProceso(Proceso p) {
        try {
            mutexColas.acquire();
            p.setTiempoLlegada(relojDelSistema);
            listaTodos.agregarFinal(p);
            
            // Gestión de Memoria / Swap Inmediata
            int memRequerida = p.getMemoriaRequerida();
            if (memoriaRAMDisponible >= memRequerida) {
                memoriaRAMDisponible -= memRequerida;
                p.setEstado(Estado.LISTO);
                colaListos.encolar(p);
                System.out.println("[MEMORIA] Proceso " + p.getId() + " admitido directo en RAM.");
            } else {
                p.setEstado(Estado.LISTO_SUSPENDIDO);
                colaListosSuspendidos.encolar(p);
                System.out.println("[SWAP] RAM llena. Proceso " + p.getId() + " suspendido en SWAP.");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            mutexColas.release();
        }
        actualizarGUI();
    }

    // --- METODOS DE CONEXIÓN CON LA INTERFAZ (GETTERS Y SETTERS) ---
    
    public void setPolitica(String politica) {
        this.politicaActual = politica;
        System.out.println("[KERNEL] Política cambiada a: " + politica);
    }
    
    public String getPolitica() { return this.politicaActual; }
    
    // Setters y Getters del Quantum Dinámico
    public void setQuantumActual(int q) { this.quantumActual = q; }
    public int getQuantumActual() { return this.quantumActual; }
    
    public Cola getColaListos() { return this.colaListos; }
    
    public Cola getColaListosPrioridad() { return this.colaListosPrioridad; }
    
    public Cola getColaBloqueados() { return this.colaBloqueados; }
    
    public Cola getColaListosSuspendidos() { return this.colaListosSuspendidos; }
    
    // NUEVO: Getter agregado para solucionar el error de Principal
    public Cola getColaBloqueadosSuspendidos() { return this.colaBloqueadosSuspendidos; }
    
    public Proceso getProcesoEnEjecucion() { return this.procesoEnEjecucion; }
    
    public int getRelojSistema() { return this.relojDelSistema; }
    
    public int getMemoriaRAMDisponible() { return this.memoriaRAMDisponible; }
    
    // NUEVO: Para actualizar la barra de SWAP en la interfaz
    public int getMemoriaEnSwap() {
        int swapTotal = 0;
        Nodo<Proceso> actual = colaListosSuspendidos.getFrente();
        while (actual != null) {
            swapTotal += actual.dato.getMemoriaRequerida();
            actual = actual.siguiente;
        }
        return swapTotal;
    }
    
    public void setActualizadorVisual(Runnable actualizador) {
        this.actualizadorVisual = actualizador;
    }

    /**
     * EL CORAZÓN DEL SISTEMA.
     * Este método es llamado por el Reloj en cada Tick.
     */
    public void ejecutarCiclo(int cicloActual) {
        this.relojDelSistema = cicloActual;

        try {
            // Protegemos las colas para que no choquen con el hilo de E/S
            mutexColas.acquire(); 

            // 1. Admitir nuevos procesos que llegaron en este ciclo de reloj a la RAM/Swap
            admitirNuevosProcesos(cicloActual);

            // 2. DISPATCHER: Planificar (Asignar CPU si está libre o aplicar expropiación)
            planificarCPU();

            // 3. CPU: Ejecutar el proceso que tenga asignado el procesador
            ejecutarProcesoEnCPU(cicloActual);

        } catch (InterruptedException e) {
            System.err.println("Error de sincronización en las colas: " + e.getMessage());
        } finally {
            // Siempre liberar el semáforo
            mutexColas.release();
        }

        // 4. Notificar a la interfaz que debe repintarse
        actualizarGUI();
    }

    private void procesarColaBloqueados() {
        int tamanoOriginal = colaBloqueados.tamano();
        // Damos una vuelta completa a la cola de bloqueados
        for (int i = 0; i < tamanoOriginal; i++) {
            Proceso p = colaBloqueados.desencolar();
            if (p != null) {
                // NOTA: Como un hilo maneja el bloqueo, solo lo reencolamos
                colaBloqueados.encolar(p);
            }
        }
    }

    private void admitirNuevosProcesos(int cicloActual) {
        Nodo<Proceso> actual = listaTodos.cabeza;
        while (actual != null) {
            Proceso p = actual.dato;
            // Si el proceso acaba de llegar en este ciclo de reloj.
            if (p.getEstado() == Estado.NUEVO && p.getTiempoLlegadaInt() <= cicloActual) {
                
                // --- GESTIÓN DE MEMORIA (PAGINACIÓN / SWAP) ---
                // CAMBIO APLICADO: Ahora usamos la memoria real del proceso en lugar del fijo
                int memRequerida = p.getMemoriaRequerida(); 
                
                // Verifica si hay RAM suficiente para el proceso
                if (memoriaRAMDisponible >= memRequerida) {
                    // Hay espacio: Entra a RAM (Cola de Listos)
                    memoriaRAMDisponible -= memRequerida;
                    p.setEstado(Estado.LISTO);
                    colaListos.encolar(p);
                    System.out.println("[MEMORIA] Proceso " + p.getId() + " admitido en RAM. (RAM Libre: " + memoriaRAMDisponible + "MB)");
                } else {
                    // No hay espacio: Entra al Disco / SWAP (Suspendido)
                    p.setEstado(Estado.LISTO_SUSPENDIDO);
                    colaListosSuspendidos.encolar(p);
                    System.out.println("[SWAP] RAM llena. Proceso " + p.getId() + " suspendido en SWAP.");
                }
            }
            actual = actual.siguiente;
        }
    }

    private void revisarSwapIn() {
        // Revisa si hay procesos en Disco que ahora quepan en la RAM
        int tamano = colaListosSuspendidos.tamano();
        for (int i = 0; i < tamano; i++) {
            Proceso p = colaListosSuspendidos.desencolar();
            // CAMBIO APLICADO: Usar memoria real del proceso
            int memRequerida = p.getMemoriaRequerida();
            
            if (memoriaRAMDisponible >= memRequerida) {
                // Hay espacio para traerlo del Swap a la RAM
                memoriaRAMDisponible -= memRequerida;
                p.setEstado(Estado.LISTO);
                colaListos.encolar(p);
                System.out.println("[SWAP-IN] Proceso " + p.getId() + " traído de SWAP a RAM. (RAM Libre: " + memoriaRAMDisponible + "MB)");
            } else {
                // Sigue sin caber, vuelve al Swap
                colaListosSuspendidos.encolar(p);
            }
        }
    }

    private void planificarCPU() {
        // --- 1. PREEMPTION PARA SRT, PRIORIDAD Y EDF ---
        // Estas políticas son expropiativas, revisamos si un nuevo proceso desplazará al actual
        if (procesoEnEjecucion != null && 
           (politicaActual.equals("SRT") || politicaActual.equals("PRIORIDAD") || politicaActual.equals("EDF"))) {
            
            Proceso candidato = extraerMejorProceso(politicaActual);
            
            if (candidato != null) {
                boolean debeExpropiar = false;
                
                if (politicaActual.equals("SRT") && candidato.getInstruccionesRestantes() < procesoEnEjecucion.getInstruccionesRestantes()) {
                    debeExpropiar = true;
                } else if (politicaActual.equals("PRIORIDAD") && candidato.getPrioridad() < procesoEnEjecucion.getPrioridad()) {
                    // OJO: Asumimos que número de prioridad MENOR significa MAYOR prioridad (ej. 1 es lo más importante)
                    debeExpropiar = true;
                } else if (politicaActual.equals("EDF") && candidato.getDeadline() < procesoEnEjecucion.getDeadline()) {
                    // El que tenga el deadline más cercano (menor número) gana
                    debeExpropiar = true;
                }

                if (debeExpropiar) {
                    System.out.println("[KERNEL] " + politicaActual + " Expropiación: " + candidato.getId() + " desplaza a " + procesoEnEjecucion.getId());
                    procesoEnEjecucion.setEstado(Estado.LISTO);
                    colaListos.encolar(procesoEnEjecucion);
                    procesoEnEjecucion = candidato;
                    prepararProcesoParaEjecucion();
                } else {
                    colaListos.encolar(candidato); // Falsa alarma, lo devolvemos a la cola
                }
            }
        }

        // --- 2. ASIGNACIÓN DE CPU SI ESTÁ LIBRE ---
        // Si la CPU está libre, metemos el siguiente proceso según la política actual
        if (procesoEnEjecucion == null && !colaListos.estaVacia()) {
            procesoEnEjecucion = extraerMejorProceso(politicaActual);
            prepararProcesoParaEjecucion();
            if (politicaActual.equals("RR") || politicaActual.equals("Round Robin")) {
                contadorQuantum = 0; // Reiniciamos el quantum al asignar un nuevo proceso
            }
        }
    }

    /**
     * Helper que escanea la cola de listos y extrae el proceso que mejor cumpla 
     * con los criterios de la política actual (FCFS, RR, SRT, PRIORIDAD, EDF).
     */
    private Proceso extraerMejorProceso(String politica) {
        if (colaListos.estaVacia()) return null;
        
        // FCFS y RR simplemente sacan el primero de la fila (FIFO Básico)
        if (politica.equals("FCFS") || politica.equals("RR") || politica.equals("Round Robin")) {
            return colaListos.desencolar();
        }

        Proceso mejor = null;
        int tamano = colaListos.tamano();
        double mejorValor = Double.MAX_VALUE;

        // Pasada 1: Evaluar toda la cola para encontrar el "Mejor" candidato
        for (int i = 0; i < tamano; i++) {
            Proceso p = colaListos.desencolar();
            double valorActual = 0;

            if (politica.equals("SRT")) {
                valorActual = p.getInstruccionesRestantes(); // Menor número de instrucciones restantes
            } else if (politica.equals("PRIORIDAD")) {
                valorActual = p.getPrioridad(); // Menor número de prioridad (1 es más prioritario que 99)
            } else if (politica.equals("EDF")) {
                valorActual = p.getDeadline(); // Menor deadline (el tiempo límite más cercano)
            }

            if (valorActual < mejorValor || mejor == null) {
                mejorValor = valorActual;
                mejor = p;
            }
            colaListos.encolar(p); // Devolvemos temporalmente el proceso a la cola
        }

        // Pasada 2: Extraer definitivamente el candidato ganador y dejar los demás
        for (int i = 0; i < tamano; i++) {
            Proceso p = colaListos.desencolar();
            if (p != mejor) {
                colaListos.encolar(p); // Reencolar los perdedores
            }
        }
        return mejor;
    }

    private void prepararProcesoParaEjecucion() {
        procesoEnEjecucion.setEstado(Estado.EJECUCION);
        System.out.println("[KERNEL] Context Switch: CPU asignada a " + procesoEnEjecucion.getId());
    }

    private void ejecutarProcesoEnCPU(int cicloActual) {
        if (procesoEnEjecucion != null) {
            
            // Si es su primera vez en CPU, calculamos tiempo de respuesta
            if (procesoEnEjecucion.getPC() == 0) {
                procesoEnEjecucion.setTiempoPrimeraEjecucion(cicloActual);
                sumaTiempoRespuesta += (cicloActual - procesoEnEjecucion.getTiempoLlegadaInt());
            }

            // Ejecutamos 1 instrucción usando tu método original en Proceso.java
            procesoEnEjecucion.ejecutar(1);
            
            // --- VERIFICAR INTERRUPCIÓN DETERMINÍSTICA (E/S) ---
            if (!procesoEnEjecucion.estaTerminado() && 
                procesoEnEjecucion.getPC() == procesoEnEjecucion.getCicloExcepcion() && 
                !procesoEnEjecucion.isInterrupcionGenerada()) {
                
                System.out.println("[KERNEL] Interrupción: Proceso " + procesoEnEjecucion.getId() + " solicita I/O en PC=" + procesoEnEjecucion.getPC());
                
                procesoEnEjecucion.setEstado(Estado.BLOQUEADO); 
                procesoEnEjecucion.setInterrupcionGenerada(true);
                colaBloqueados.encolar(procesoEnEjecucion);
                
                // DISPARAR HILO INDEPENDIENTE PARA E/S
                new HiloInterrupcion(procesoEnEjecucion).start();
                
                procesoEnEjecucion = null; // CPU libre
                return;
            }
            
            // --- VERIFICAR SI TERMINÓ ---
            if (procesoEnEjecucion.estaTerminado()) {
                System.out.println("[KERNEL] Proceso " + procesoEnEjecucion.getId() + " TERMINADO.");
                procesoEnEjecucion.setEstado(Estado.TERMINADO);
                procesoEnEjecucion.setTiempoFinalizacion(cicloActual);
                
                // Recopilar estadísticas básicas
                totalProcesosTerminados++;
                if (cicloActual <= procesoEnEjecucion.getDeadline()) {
                    totalProcesosCumplenDeadline++;
                }
                
                // LIBERAR MEMORIA RAM 
                // CAMBIO APLICADO: Liberamos la memoria real que usaba el proceso
                memoriaRAMDisponible += procesoEnEjecucion.getMemoriaRequerida();
                System.out.println("[MEMORIA] RAM liberada. (RAM Libre: " + memoriaRAMDisponible + "MB)");
                
                // Liberar CPU
                procesoEnEjecucion = null; 
                
                // INTENTAR SWAP-IN TRAS LIBERAR MEMORIA
                revisarSwapIn();
                
            } else if (politicaActual.equals("RR") || politicaActual.equals("Round Robin")) {
                // --- EXPROPIACIÓN POR QUANTUM (ROUND ROBIN) ---
                contadorQuantum++;
                // CAMBIO APLICADO: Usa la variable `quantumActual` del JSpinner
                if (contadorQuantum >= quantumActual) {
                    System.out.println("[KERNEL] RR: Quantum expirado (" + quantumActual + ") para " + procesoEnEjecucion.getId());
                    procesoEnEjecucion.setEstado(Estado.LISTO);
                    colaListos.encolar(procesoEnEjecucion);
                    procesoEnEjecucion = null; // Liberamos CPU para el siguiente
                }
            }
        }
    }

    /**
     * NUEVO MÉTODO THREAD-SAFE: Invocado por el HiloInterrupcion al finalizar su sleep
     * para reincorporar el proceso al sistema evitando condiciones de carrera.
     * NUEVO COMPORTAMIENTO: Busca tanto en colaBloqueados como en colaBloqueadosSuspendidos.
     */
    public void moverDeBloqueadoAListoSeguro(Proceso p) {
        try {
            mutexColas.acquire(); 
            
            boolean estabaEnRAM = false;
            boolean estabaEnSwap = false;
            
            // Reconstruir la colaBloqueados extrayendo al proceso que ya terminó su E/S
            Cola colaTemp = new Cola();
            while (!colaBloqueados.estaVacia()) {
                Proceso bloqueadoActual = colaBloqueados.desencolar();
                if (bloqueadoActual != null) {
                    if (!bloqueadoActual.getId().equals(p.getId())) {
                        colaTemp.encolar(bloqueadoActual);
                    } else {
                        estabaEnRAM = true;
                    }
                }
            }
            colaBloqueados = colaTemp;
            
            // Reconstruir BloqueadosSuspendidos por si fue mandado al Swap
            Cola colaTempSwap = new Cola();
            while (!colaBloqueadosSuspendidos.estaVacia()) {
                Proceso bloqueadoActual = colaBloqueadosSuspendidos.desencolar();
                if (bloqueadoActual != null) {
                    if (!bloqueadoActual.getId().equals(p.getId())) {
                        colaTempSwap.encolar(bloqueadoActual);
                    } else {
                        estabaEnSwap = true;
                    }
                }
            }
            colaBloqueadosSuspendidos = colaTempSwap;
            
            if (estabaEnRAM) {
                // Insertarlo de vuelta en Listos
                p.setEstado(Estado.LISTO);
                colaListos.encolar(p);
                System.out.println("[KERNEL] HILO ASÍNCRONO: Proceso " + p.getId() + " completó E/S y regresó a Listos.");
            } else if (estabaEnSwap) {
                // Insertarlo de vuelta en Listos Suspendidos (Disco)
                p.setEstado(Estado.LISTO_SUSPENDIDO);
                colaListosSuspendidos.encolar(p);
                System.out.println("[KERNEL] HILO ASÍNCRONO: Proceso " + p.getId() + " completó E/S y regresó a Listos Suspendidos.");
            }
            
        } catch (InterruptedException e) {
            System.out.println("Error reincorporando proceso de E/S: " + e.getMessage());
        } finally {
            mutexColas.release();
        }
        
        actualizarGUI(); // Refrescar vista
    }

    private void actualizarGUI() {
        // En lugar de instanciar o depender directamente de Principal, 
        // ejecutamos el callback que la interfaz nos pasó al inicializar.
        if (actualizadorVisual != null) {
            actualizadorVisual.run();
        }
    }

    // --- MÉTODOS DE REPORTE ---
    public String obtenerReporteEstadisticas() {
        if (totalProcesosTerminados == 0) return "Aún no hay procesos terminados.";

        double porcentajeDeadline = ((double) totalProcesosCumplenDeadline / totalProcesosTerminados) * 100;
        double promedioRespuesta = (double) sumaTiempoRespuesta / totalProcesosTerminados;
        double promedioBloqueo = (double) sumaTiempoBloqueado / totalProcesosTerminados;

        StringBuilder sb = new StringBuilder();
        sb.append("=== REPORTE DE RENDIMIENTO ===\n");
        sb.append("Procesos Terminados: ").append(totalProcesosTerminados).append("\n");
        sb.append("----------------------------\n");
        sb.append(String.format("1. Cumplimiento de Deadline: %.2f%%\n", porcentajeDeadline));
        sb.append("   (Procesos que finalizaron a tiempo)\n\n");
        sb.append(String.format("2. Tiempo Promedio de Respuesta: %.2f ciclos\n", promedioRespuesta));
        sb.append("   (Tiempo desde llegada hasta primera ejecución)\n\n");
        sb.append(String.format("3. Tiempo Promedio Bloqueado: %.2f ciclos\n", promedioBloqueo));
        
        return sb.toString();
    }
}