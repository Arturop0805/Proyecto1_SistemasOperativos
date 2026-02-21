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
import Interfaces.Principal;
import Modelo.Estado;
import Modelo.Proceso;
import Utilidades.GeneradorProcesos;

import java.util.concurrent.Semaphore;

/**
 * Administrador (Kernel) - Cambio severo: candado global de swap-in por tick.
 *
 * - Solo 1 swap-in GLOBAL por ciclo del reloj (por defecto).
 * - No se permite más swap-in en el mismo tick desde ninguna ruta.
 * - Conserva la estructura original y todos los métodos; añade protecciones.
 */
public class Administrador {

    // SINGLETON
    private static Administrador instancia;

    // Estructuras
    private Cola colaListos;
    private Cola colaListosPrioridad;
    private Cola colaBloqueados;
    private Cola colaListosSuspendidos;
    private Cola colaBloqueadosSuspendidos;
    private ListaSimple<Proceso> listaTodos;

    // Sincronización
    private Semaphore mutexColas = new Semaphore(1);

    // Planificación / Estado
    private String politicaActual = "FCFS";
    private int contadorQuantum = 0;
    private int relojDelSistema = 0;
    private Proceso procesoEnEjecucion;
    private int quantumActual = Config.QUANTUM_DEFAULT;

    // Memoria (MB)
    private int memoriaRAMDisponible;

    // GUI callback
    private Runnable actualizadorVisual;

    // Estadísticas
    private int totalProcesosTerminados = 0;
    private int totalProcesosCumplenDeadline = 0;
    private int sumaTiempoRespuesta = 0;
    private int sumaTiempoBloqueado = 0;
    private int ciclosCPUOcupada = 0;
    private int ciclosCPUInactiva = 0;

    // --- CONFIGURACIONES PARA CONTROLAR ADMISIÓN / SWAP-IN POR CICLO ---
    private static final int MAX_ADMITIR_POR_CICLO = 1;
    // Cambié a un solo swap-in global por ciclo (más severo)
    private static final int MAX_GLOBAL_SWAPIN_POR_CICLO = 1;
    // --------------------------------------------------------------------

    // --- CONTADORES POR CICLO (gestionados con conciencia de ciclo) ---
    private int admitidosThisCycle = 0;
    private int globalSwapInsThisCycle = 0;
    // ------------------------------------------------------------------

    // --- ÚLTIMO CICLO CON EL QUE LOS CONTADORES FUERON SINCRONIZADOS ---
    private int lastCycleCounter = -1;
    // ------------------------------------------------------------------

    private Administrador() {
        this.colaListos = new Cola();
        this.colaListosPrioridad = new Cola();
        this.colaBloqueados = new Cola();
        this.colaListosSuspendidos = new Cola();
        this.colaBloqueadosSuspendidos = new Cola();
        this.listaTodos = new ListaSimple<>();
        this.memoriaRAMDisponible = Config.MEMORIA_TOTAL - Config.MEMORIA_RESERVADA_SO;
        ajustarMemoriaDisponible();
    }

    public static Administrador getInstancia() {
        if (instancia == null) instancia = new Administrador();
        return instancia;
    }

    // ----------------------------
    // UTIL: asegurar memoria en rango válido
    // ----------------------------
    private void ajustarMemoriaDisponible() {
        int max = Config.MEMORIA_TOTAL - Config.MEMORIA_RESERVADA_SO;
        if (memoriaRAMDisponible < 0) memoriaRAMDisponible = 0;
        if (memoriaRAMDisponible > max) memoriaRAMDisponible = max;
    }

    // ----------------------------
    // UTIL: asegurar/resetear contadores por ciclo (con conciencia de relojDelSistema)
    // ----------------------------
    private void asegurarContadoresParaCiclo() {
        if (this.lastCycleCounter != this.relojDelSistema) {
            this.admitidosThisCycle = 0;
            this.globalSwapInsThisCycle = 0;
            this.lastCycleCounter = this.relojDelSistema;
            // Log mínimo opcional:
            // System.out.println("[KERNEL] Contadores reiniciados para ciclo " + this.relojDelSistema);
        }
    }

    // ----------------------------
    // Inicialización / Admisión
    // ----------------------------
    public void iniciarSimulacion(int numProcesos) {
        System.out.println("[KERNEL] Iniciando simulación con " + numProcesos + " procesos.");
        for (int i = 0; i < numProcesos; i++) {
            ListaSimple<Proceso> generados = GeneradorProcesos.generarAleatorios(1);
            if (generados != null && generados.cabeza != null) {
                Proceso p = generados.cabeza.dato;
                // Programamos llegada escalonada (ej.: 0, 2, 4, ...)
                p.setTiempoLlegada(i * 2);
                p.setEnMemoria(false);
                listaTodos.agregarFinal(p);
            }
        }
    }

    /**
     * Admitir proceso manualmente desde la interfaz.
     * Respeta tiempo de llegada si la UI lo estableció.
     */
    public void admitirProceso(Proceso p) {
        if (p == null) return;
        try {
            mutexColas.acquire();

            if (p.getTiempoLlegadaInt() <= 0) {
                p.setTiempoLlegada(relojDelSistema);
            }

            listaTodos.agregarFinal(p);

            if (p.getTiempoLlegadaInt() > relojDelSistema) {
                p.setEstado(Estado.NUEVO);
                System.out.println("[KERNEL] Proceso " + p.getId() + " programado para t=" + p.getTiempoLlegadaInt());
                return;
            }

            if (relojDelSistema >= p.getDeadlineAbsoluto()) {
                terminarProceso(p, relojDelSistema, true);
                return;
            }

            // sincronizar contadores en caso de que esta llamada venga desde otro hilo
            asegurarContadoresParaCiclo();

            int memReq = p.getMemoriaRequerida();
            if (memReq < 0) memReq = 0;

            // Limitamos admisiones por ciclo
            if (this.admitidosThisCycle >= MAX_ADMITIR_POR_CICLO) {
                limpiarProcesodeColas(p);
                p.setEnMemoria(false);
                p.setEstado(Estado.LISTO_SUSPENDIDO);
                colaListosSuspendidos.encolar(p);
                ajustarMemoriaDisponible();
                System.out.println("[KERNEL] Límite de admisiones alcanzado este ciclo. " + p.getId() + " enviado a SWAP.");
                return;
            }

            if (memoriaRAMDisponible >= memReq) {
                limpiarProcesodeColas(p);
                memoriaRAMDisponible -= memReq;
                p.setEnMemoria(true);
                p.setEstado(Estado.LISTO);
                colaListos.encolar(p);
                ajustarMemoriaDisponible();
                this.admitidosThisCycle++;
                System.out.println("[KERNEL] Proceso " + p.getId() + " admitido en RAM (Cola Listos). Req: " + memReq + "MB (admitidos este ciclo: " + this.admitidosThisCycle + ")");
            } else {
                limpiarProcesodeColas(p);
                p.setEnMemoria(false);
                p.setEstado(Estado.LISTO_SUSPENDIDO);
                colaListosSuspendidos.encolar(p);
                ajustarMemoriaDisponible();
                System.out.println("[KERNEL-SWAP] RAM INSUFICIENTE. Proceso " + p.getId() + " enviado a SWAP (Listos Suspendidos). Req: " + memReq + "MB");
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            mutexColas.release();
            actualizarGUI();
        }
    }

    /**
     * Suspender explícitamente un proceso que estaba BLOQUEADO.
     * Libera RAM SI Y SOLO SI estaba marcada como enMemoria.
     */
    public void suspenderProcesoBloqueado(Proceso p) {
        if (p == null) return;
        try {
            mutexColas.acquire();
            if (p.getEstado() == Estado.TERMINADO) return;

            p.setEstado(Estado.BLOQUEADO_SUSPENDIDO);
            colaBloqueados.removerProceso(p.getId());
            colaBloqueadosSuspendidos.encolar(p);

            if (p.isEnMemoria()) {
                memoriaRAMDisponible += p.getMemoriaRequerida();
                p.setEnMemoria(false);
                ajustarMemoriaDisponible();
            }

            System.out.println("[SWAP] Proceso bloqueado " + p.getId() + " fue suspendido. (RAM Libre: " + memoriaRAMDisponible + "MB)");
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            mutexColas.release();
            actualizarGUI();
        }
    }

    // ----------------------------
    // Getters / utilidades
    // ----------------------------
    public void setPolitica(String politica) { this.politicaActual = politica; System.out.println("[KERNEL] Política cambiada a: " + politica); }
    public String getPolitica() { return this.politicaActual; }
    public void setQuantumActual(int q) { this.quantumActual = q; }
    public int getQuantumActual() { return this.quantumActual; }
    public Cola getColaListos() { return this.colaListos; }
    public Cola getColaListosPrioridad() { return this.colaListosPrioridad; }
    public Cola getColaBloqueados() { return this.colaBloqueados; }
    public Cola getColaListosSuspendidos() { return this.colaListosSuspendidos; }
    public Cola getColaBloqueadosSuspendidos() { return this.colaBloqueadosSuspendidos; }
    public ListaSimple<Proceso> getListaTodos() { return this.listaTodos; }
    public Proceso getProcesoEnEjecucion() { return this.procesoEnEjecucion; }
    public int getRelojSistema() { return this.relojDelSistema; }
    public int getMemoriaRAMDisponible() { return this.memoriaRAMDisponible; }

    public int getMemoriaEnSwap() {
        int swapTotal = 0;
        Nodo<Proceso> actual = colaListosSuspendidos.getFrente();
        while (actual != null) {
            swapTotal += Math.max(0, actual.dato.getMemoriaRequerida());
            actual = actual.siguiente;
        }
        actual = colaBloqueadosSuspendidos.getFrente();
        while (actual != null) {
            swapTotal += Math.max(0, actual.dato.getMemoriaRequerida());
            actual = actual.siguiente;
        }
        return swapTotal;
    }

    public void setActualizadorVisual(Runnable actualizador) { this.actualizadorVisual = actualizador; }

    // ----------------------------
    // Limpieza en colas (idempotente)
    // ----------------------------
    private void limpiarProcesodeColas(Proceso p) {
        if (p == null) return;
        try {
            colaListos.removerProceso(p.getId());
            colaBloqueados.removerProceso(p.getId());
            if (colaListosPrioridad != null) colaListosPrioridad.removerProceso(p.getId());
            colaListosSuspendidos.removerProceso(p.getId());
            colaBloqueadosSuspendidos.removerProceso(p.getId());
        } catch (Exception ex) {
            System.err.println("Advertencia al limpiar colas del proceso: " + ex.getMessage());
        }
    }

    // ----------------------------
    // Ciclo del reloj (método llamado por Reloj)
    // ----------------------------
    public void ejecutarCiclo(int cicloActual) {
        this.relojDelSistema = cicloActual;

        try {
            mutexColas.acquire();

            // garantizar que los contadores se correspondan con este ciclo (reinicia si cambió)
            asegurarContadoresParaCiclo();

            // 1) Detectar deadlines vencidos (dos fases para evitar modificar colas mientras se recorren)
            ListaSimple<Proceso> expirados = recolectarExpiradosEnColas();
            Nodo<Proceso> nodoExp = (expirados == null) ? null : expirados.cabeza;
            while (nodoExp != null) {
                terminarProceso(nodoExp.dato, cicloActual, true);
                nodoExp = nodoExp.siguiente;
            }

            // 2) Admitir nuevos procesos (si han llegado) — limitada por MAX_ADMITIR_POR_CICLO
            admitirNuevosProcesos(cicloActual);

            // 3) Revisar swap-in (listos_suspendidos y bloqueados_suspendidos)
            revisarSwapIn();

            // 4) Planificar CPU (preemption incluida)
            planificarCPU();

            // 5) Ejecutar en CPU (un ciclo)
            ejecutarProcesoEnCPU(cicloActual);

        } catch (InterruptedException e) {
            System.err.println("Error de sincronización en las colas: " + e.getMessage());
        } catch (Exception ex) {
            System.err.println("[KERNEL] Excepción inesperada en ejecutarCiclo: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            mutexColas.release();
            actualizarGUI();
        }
    }

    // ----------------------------
    // Recolectar procesos vencidos sin modificar colas
    // ----------------------------
    private ListaSimple<Proceso> recolectarExpiradosEnColas() {
        ListaSimple<Proceso> expirados = new ListaSimple<>();

        int tam;

        // Cola Listos
        tam = colaListos.tamano();
        for (int i = 0; i < tam; i++) {
            Proceso p = colaListos.desencolar();
            if (p == null) continue;
            if (relojDelSistema >= p.getDeadlineAbsoluto()) {
                expirados.agregarFinal(p);
            }
            colaListos.encolar(p);
        }

        // Cola Bloqueados
        tam = colaBloqueados.tamano();
        for (int i = 0; i < tam; i++) {
            Proceso p = colaBloqueados.desencolar();
            if (p == null) continue;
            if (relojDelSistema >= p.getDeadlineAbsoluto()) {
                expirados.agregarFinal(p);
            }
            colaBloqueados.encolar(p);
        }

        // Listos suspendidos
        tam = colaListosSuspendidos.tamano();
        for (int i = 0; i < tam; i++) {
            Proceso p = colaListosSuspendidos.desencolar();
            if (p == null) continue;
            if (relojDelSistema >= p.getDeadlineAbsoluto()) {
                expirados.agregarFinal(p);
            }
            colaListosSuspendidos.encolar(p);
        }

        // Bloqueados suspendidos
        tam = colaBloqueadosSuspendidos.tamano();
        for (int i = 0; i < tam; i++) {
            Proceso p = colaBloqueadosSuspendidos.desencolar();
            if (p == null) continue;
            if (relojDelSistema >= p.getDeadlineAbsoluto()) {
                expirados.agregarFinal(p);
            }
            colaBloqueadosSuspendidos.encolar(p);
        }

        // listaTodos (NUEVOS)
        Nodo<Proceso> n = listaTodos.cabeza;
        while (n != null) {
            Proceso p = n.dato;
            if (p != null && p.getEstado() == Estado.NUEVO && relojDelSistema >= p.getDeadlineAbsoluto()) {
                expirados.agregarFinal(p);
            }
            n = n.siguiente;
        }

        return expirados;
    }

    // ----------------------------
    // Admitir nuevos desde listaTodos (cuando llega su tiempo) - limitado por ciclo
    // ----------------------------
    private void admitirNuevosProcesos(int cicloActual) {
        // Aseguramos contadores por si esta función se llama desde fuera del tick principal
        asegurarContadoresParaCiclo();

        Nodo<Proceso> actual = listaTodos.cabeza;
        while (actual != null && this.admitidosThisCycle < MAX_ADMITIR_POR_CICLO) {
            Proceso p = actual.dato;
            if (p != null && p.getEstado() == Estado.NUEVO && p.getTiempoLlegadaInt() <= cicloActual) {

                if (cicloActual >= p.getDeadlineAbsoluto()) {
                    terminarProceso(p, cicloActual, true);
                    actual = actual.siguiente;
                    continue;
                }

                int memReq = p.getMemoriaRequerida();
                if (memReq < 0) memReq = 0;

                if (memoriaRAMDisponible >= memReq) {
                    limpiarProcesodeColas(p);
                    memoriaRAMDisponible -= memReq;
                    p.setEnMemoria(true);
                    p.setEstado(Estado.LISTO);
                    colaListos.encolar(p);
                    ajustarMemoriaDisponible();
                    this.admitidosThisCycle++;
                    System.out.println("[KERNEL] (Admisión) Proceso " + p.getId() + " admitido en RAM. (RAM Libre: " + memoriaRAMDisponible + "MB). Admitidos este ciclo: " + this.admitidosThisCycle);
                } else {
                    // Si no hay memoria suficiente, lo enviamos a swap de inmediato.
                    limpiarProcesodeColas(p);
                    p.setEnMemoria(false);
                    p.setEstado(Estado.LISTO_SUSPENDIDO);
                    colaListosSuspendidos.encolar(p);
                    ajustarMemoriaDisponible();
                    System.out.println("[SWAP] (Admisión) RAM llena. Proceso " + p.getId() + " suspendido en SWAP.");
                }
            }
            actual = actual.siguiente;
        }

        if (this.admitidosThisCycle > 0) {
            System.out.println("[KERNEL] Admitidos " + this.admitidosThisCycle + " proceso(s) desde listaTodos en el ciclo " + cicloActual);
        }
    }

    // ----------------------------
    // Revisar swap-in: listos_suspendidos y bloqueados_suspendidos
    //   — AHORA: sólo 1 swap-in GLOBAL por ciclo (MAX_GLOBAL_SWAPIN_POR_CICLO)
    // ----------------------------
    private void revisarSwapIn() {
        try {
            // Aseguramos contadores para manejar llamadas desde diferentes hilos
            asegurarContadoresParaCiclo();

            // Si ya alcanzamos el máximo global de swap-ins, no hacemos nada más este ciclo.
            if (this.globalSwapInsThisCycle >= MAX_GLOBAL_SWAPIN_POR_CICLO) {
                // Log para depurar por qué no se trajeron más procesos
                // System.out.println("[KERNEL] Máximo global de swap-ins alcanzado para este ciclo.");
                return;
            }

            // Primero intentamos traer UNO desde listos_suspendidos (si cabe)
            int tamListos = colaListosSuspendidos.tamano();
            for (int i = 0; i < tamListos; i++) {
                if (this.globalSwapInsThisCycle >= MAX_GLOBAL_SWAPIN_POR_CICLO) break;

                Proceso p = colaListosSuspendidos.desencolar();
                if (p == null) continue;

                if (relojDelSistema >= p.getDeadlineAbsoluto()) {
                    terminarProceso(p, relojDelSistema, true);
                    continue;
                }

                int memReq = p.getMemoriaRequerida();
                if (memReq < 0) memReq = 0;

                if (memoriaRAMDisponible >= memReq) {
                    limpiarProcesodeColas(p);
                    memoriaRAMDisponible -= memReq;
                    p.setEnMemoria(true);
                    p.setEstado(Estado.LISTO);
                    colaListos.encolar(p);
                    ajustarMemoriaDisponible();
                    this.globalSwapInsThisCycle++;
                    System.out.println("[KERNEL] SWAP-IN GLOBAL: " + p.getId() + " -> LISTO. RAM Libre: " + memoriaRAMDisponible + "MB (swap-ins globales este ciclo: " + this.globalSwapInsThisCycle + ")");
                } else {
                    // No cabe: lo volvemos a encolar al final.
                    colaListosSuspendidos.encolar(p);
                }
            }

            // Si aún no hicimos ningún swap-in global, probamos traer UNO desde bloqueados_suspendidos
            if (this.globalSwapInsThisCycle < MAX_GLOBAL_SWAPIN_POR_CICLO) {
                int tamBloq = colaBloqueadosSuspendidos.tamano();
                for (int i = 0; i < tamBloq; i++) {
                    if (this.globalSwapInsThisCycle >= MAX_GLOBAL_SWAPIN_POR_CICLO) break;

                    Proceso p = colaBloqueadosSuspendidos.desencolar();
                    if (p == null) continue;

                    if (relojDelSistema >= p.getDeadlineAbsoluto()) {
                        terminarProceso(p, relojDelSistema, true);
                        continue;
                    }

                    int memReq = p.getMemoriaRequerida();
                    if (memReq < 0) memReq = 0;

                    if (memoriaRAMDisponible >= memReq) {
                        limpiarProcesodeColas(p);
                        memoriaRAMDisponible -= memReq;
                        p.setEnMemoria(true);
                        p.setEstado(Estado.LISTO);
                        colaListos.encolar(p);
                        ajustarMemoriaDisponible();
                        this.globalSwapInsThisCycle++;
                        System.out.println("[KERNEL] SWAP-IN GLOBAL: " + p.getId() + " -> LISTO. RAM Libre: " + memoriaRAMDisponible + "MB (swap-ins globales este ciclo: " + this.globalSwapInsThisCycle + ")");
                    } else {
                        colaBloqueadosSuspendidos.encolar(p);
                    }
                }
            }

            if (this.globalSwapInsThisCycle > 0) {
                System.out.println("[KERNEL] Total swap-ins globales este ciclo: " + this.globalSwapInsThisCycle);
            }

        } catch (Exception ex) {
            System.err.println("[KERNEL] Error en revisarSwapIn: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // ----------------------------
    // Planificador: preemption + asignación
    // ----------------------------
    private void planificarCPU() {
        if (procesoEnEjecucion != null &&
            (politicaActual.equals("SRT") || politicaActual.equals("PRIORIDAD") || politicaActual.equals("EDF"))) {

            Proceso candidato = extraerMejorProceso(politicaActual);
            if (candidato != null) {
                if (relojDelSistema >= candidato.getDeadlineAbsoluto()) {
                    terminarProceso(candidato, relojDelSistema, true);
                } else {
                    boolean expropiar = false;
                    if (politicaActual.equals("SRT") && candidato.getInstruccionesRestantes() < procesoEnEjecucion.getInstruccionesRestantes()) expropiar = true;
                    else if (politicaActual.equals("PRIORIDAD") && candidato.getPrioridad() < procesoEnEjecucion.getPrioridad()) expropiar = true;
                    else if (politicaActual.equals("EDF") && candidato.getDeadlineAbsoluto() < procesoEnEjecucion.getDeadlineAbsoluto()) expropiar = true;

                    if (expropiar) {
                        limpiarProcesodeColas(procesoEnEjecucion);
                        procesoEnEjecucion.setEstado(Estado.LISTO);
                        colaListos.encolar(procesoEnEjecucion);

                        procesoEnEjecucion = candidato;
                        prepararProcesoParaEjecucion();
                    } else {
                        limpiarProcesodeColas(candidato);
                        candidato.setEstado(Estado.LISTO);
                        colaListos.encolar(candidato);
                    }
                }
            }
        }

        if (procesoEnEjecucion == null && !colaListos.estaVacia()) {
            int intentos = colaListos.tamano();
            for (int i = 0; i < intentos; i++) {
                Proceso candidato = extratrMejorYDevolver(politicaActual);
                if (candidato == null) break;

                if (relojDelSistema >= candidato.getDeadlineAbsoluto()) {
                    terminarProceso(candidato, relojDelSistema, true);
                    continue;
                }

                limpiarProcesodeColas(candidato);
                procesoEnEjecucion = candidato;
                prepararProcesoParaEjecucion();
                if (politicaActual.equals("RR")) contadorQuantum = 0;
                break;
            }
        }
    }

    // Helper: versión original del extraerMejorProceso (sin alterar)
    private Proceso extraerMejorProceso(String politica) {
        if (colaListos.estaVacia()) return null;
        if (politica.equals("FCFS") || politica.equals("RR")) return colaListos.desencolar();

        Proceso mejor = null;
        int tam = colaListos.tamano();
        double mejorValor = Double.MAX_VALUE;
        for (int i = 0; i < tam; i++) {
            Proceso p = colaListos.desencolar();
            if (p == null) continue;
            double valor = 0;
            if (politica.equals("SRT")) valor = p.getInstruccionesRestantes();
            else if (politica.equals("PRIORIDAD")) valor = p.getPrioridad();
            else if (politica.equals("EDF")) valor = p.getDeadlineAbsoluto();

            if (mejor == null || valor < mejorValor) {
                mejorValor = valor;
                mejor = p;
            }
            colaListos.encolar(p);
        }

        for (int i = 0; i < tam; i++) {
            Proceso p = colaListos.desencolar();
            if (p == null) continue;
            if (p != mejor) colaListos.encolar(p);
        }
        return mejor;
    }

    // Si tu implementación original hizo diferente, mantén su nombre/semántica.
    private Proceso extratrMejorYDevolver(String politica) {
        // Reutilizamos extraerMejorProceso (no cambia semántica)
        return extraerMejorProceso(politica);
    }

    private void prepararProcesoParaEjecucion() {
        if (procesoEnEjecucion == null) return;
        procesoEnEjecucion.setEstado(Estado.EJECUCION);
        System.out.println("[KERNEL] Context Switch: CPU -> " + procesoEnEjecucion.getId());
    }

    // ----------------------------
    // Ejecutar proceso en CPU (un ciclo)
    // ----------------------------
    private void ejecutarProcesoEnCPU(int cicloActual) {
        if (procesoEnEjecucion != null) {

            if (cicloActual >= procesoEnEjecucion.getDeadlineAbsoluto()) {
                terminarProceso(procesoEnEjecucion, cicloActual, true);
                procesoEnEjecucion = null;
                if (Principal.getInstancia() != null) Principal.getInstancia().actualizarGraficaCPU(false);
                return;
            }

            ciclosCPUOcupada++;

            if (procesoEnEjecucion.getPC() == 0) {
                procesoEnEjecucion.setTiempoPrimeraEjecucion(cicloActual);
                sumaTiempoRespuesta += (cicloActual - procesoEnEjecucion.getTiempoLlegadaInt());
            }

            procesoEnEjecucion.ejecutar(1);

            if (!procesoEnEjecucion.estaTerminado() &&
                procesoEnEjecucion.getPC() == procesoEnEjecucion.getCicloExcepcion() &&
                !procesoEnEjecucion.isInterrupcionGenerada()) {

                limpiarProcesodeColas(procesoEnEjecucion);
                procesoEnEjecucion.setEstado(Estado.BLOQUEADO);
                procesoEnEjecucion.setInterrupcionGenerada(true);
                colaBloqueados.encolar(procesoEnEjecucion);

                new HiloInterrupcion(procesoEnEjecucion).start();

                procesoEnEjecucion = null;
                return;
            }

            if (procesoEnEjecucion.estaTerminado()) {
                terminarProceso(procesoEnEjecucion, cicloActual, false);
                procesoEnEjecucion = null;
            } else if (politicaActual.equals("RR")) {
                contadorQuantum++;
                if (contadorQuantum >= quantumActual) {
                    limpiarProcesodeColas(procesoEnEjecucion);
                    procesoEnEjecucion.setEstado(Estado.LISTO);
                    colaListos.encolar(procesoEnEjecucion);
                    procesoEnEjecucion = null;
                }
            }

        } else {
            ciclosCPUInactiva++;
        }
    }

    // ----------------------------
    // Terminación centralizada
    // ----------------------------
    private void terminarProceso(Proceso p, int cicloActual, boolean falloPorDeadline) {
        if (p == null) return;
        if (p.getEstado() == Estado.TERMINADO) return;

        try {
            System.out.println("[KERNEL] Proceso " + p.getId() + (falloPorDeadline ? " ABORTADO (DEADLINE)." : " TERMINADO NORMALMENTE."));

            Estado estadoPrevio = p.getEstado();

            limpiarProcesodeColas(p);

            if (procesoEnEjecucion != null && procesoEnEjecucion.getId().equals(p.getId())) procesoEnEjecucion = null;

            p.setAbortadoPorDeadline(falloPorDeadline);

            p.setEstado(Estado.TERMINADO);
            p.setTiempoFinalizacion(cicloActual); // usar el parámetro explícito

            totalProcesosTerminados++;
            if (!falloPorDeadline && cicloActual <= p.getDeadlineAbsoluto()) {
                totalProcesosCumplenDeadline++;
            }
            sumaTiempoBloqueado += p.getTiempoTotalBloqueado();

            // Liberar RAM solo si estaba en memoria
            if (p.isEnMemoria()) {
                memoriaRAMDisponible += p.getMemoriaRequerida();
                p.setEnMemoria(false);
                ajustarMemoriaDisponible();
                System.out.println("[MEMORIA] RAM liberada. (RAM Libre: " + memoriaRAMDisponible + "MB)");
            } else {
                System.out.println("[MEMORIA] " + p.getId() + " estaba en swap: no liberar RAM.");
            }

            // NOTA: No llamamos a revisarSwapIn() desde aquí para evitar recursividad.
            // El ciclo principal hará revisarSwapIn() en su siguiente tick.

        } catch (Exception ex) {
            System.err.println("[KERNEL] Error en terminarProceso para " + (p == null ? "null" : p.getId()) + " : " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // ----------------------------
    // Hilo E/S: mover de bloqueado a listo (thread-safe)
    // ----------------------------
    public void moverDeBloqueadoAListoSeguro(Proceso p) {
        if (p == null) return;
        try {
            mutexColas.acquire();

            // sincronizar contadores con el ciclo actual (por si se llama desde otro hilo)
            asegurarContadoresParaCiclo();

            if (p.getEstado() == Estado.TERMINADO) {
                colaBloqueados.removerProceso(p.getId());
                return;
            }

            // Remover el proceso de la cola de bloqueados (idempotente)
            try {
                colaBloqueados.removerProceso(p.getId());
            } catch (Exception ignore) {}

            int memReq = p.getMemoriaRequerida();
            if (memReq < 0) memReq = 0;

            // Severidad: respetar el candado global de swap-ins
            if (this.globalSwapInsThisCycle >= MAX_GLOBAL_SWAPIN_POR_CICLO) {
                // Ya hicimos el swap-in global de este ciclo, devolvemos a swap y salimos
                limpiarProcesodeColas(p);
                p.setEnMemoria(false);
                p.setEstado(Estado.BLOQUEADO_SUSPENDIDO);
                colaBloqueadosSuspendidos.encolar(p);
                System.out.println("[KERNEL] Límite global de swap-ins alcanzado este ciclo. " + p.getId() + " -> BLOQUEADO_SUSPENDIDO.");
                return;
            }

            if (memoriaRAMDisponible >= memReq) {
                limpiarProcesodeColas(p);
                memoriaRAMDisponible -= memReq;
                p.setEnMemoria(true);
                p.setEstado(Estado.LISTO);
                colaListos.encolar(p);
                this.globalSwapInsThisCycle++;
                ajustarMemoriaDisponible();
                System.out.println("[KERNEL] E/S completada: Proceso " + p.getId() + " -> LISTO. RAM Libre: " + memoriaRAMDisponible + "MB (swap-ins globales este ciclo: " + this.globalSwapInsThisCycle + ")");
            } else {
                // No hay memoria suficiente: lo dejamos en swap
                limpiarProcesodeColas(p);
                p.setEnMemoria(false);
                p.setEstado(Estado.BLOQUEADO_SUSPENDIDO);
                colaBloqueadosSuspendidos.encolar(p);
                System.out.println("[KERNEL] E/S completada: " + p.getId() + " -> BLOQUEADO_SUSPENDIDO (swap). RAM Libre: " + memoriaRAMDisponible + "MB");
            }

        } catch (InterruptedException e) {
            System.out.println("[KERNEL] Error moverDeBloqueadoAListoSeguro: " + e.getMessage());
        } finally {
            mutexColas.release();
            actualizarGUI();
        }
    }

    // ----------------------------
    // Reset / Reportes
    // ----------------------------
    public void resetearSistema() {
        try {
            mutexColas.acquire();

            this.colaListos = new Cola();
            this.colaListosPrioridad = new Cola();
            this.colaBloqueados = new Cola();
            this.colaListosSuspendidos = new Cola();
            this.colaBloqueadosSuspendidos = new Cola();
            this.listaTodos = new ListaSimple<>();
            this.relojDelSistema = 0;
            this.procesoEnEjecucion = null;
            this.contadorQuantum = 0;
            this.memoriaRAMDisponible = Config.MEMORIA_TOTAL - Config.MEMORIA_RESERVADA_SO;
            ajustarMemoriaDisponible();

            this.totalProcesosTerminados = 0;
            this.totalProcesosCumplenDeadline = 0;
            this.sumaTiempoRespuesta = 0;
            this.sumaTiempoBloqueado = 0;
            this.ciclosCPUOcupada = 0;
            this.ciclosCPUInactiva = 0;

            // Reiniciar contadores por ciclo
            this.admitidosThisCycle = 0;
            this.globalSwapInsThisCycle = 0;
            this.lastCycleCounter = -1;

            System.out.println("[KERNEL] SISTEMA RESETEADO COMPLETAMENTE.");

        } catch (InterruptedException e) {
            System.err.println("[KERNEL] Error al resetear el sistema: " + e.getMessage());
        } finally {
            mutexColas.release();
            actualizarGUI();
        }
    }

    public String obtenerReporteEstadisticas() {
        if (totalProcesosTerminados == 0) return "Aún no hay procesos terminados para generar el reporte.";

        double porcentajeDeadline = ((double) totalProcesosCumplenDeadline / totalProcesosTerminados) * 100;
        double promedioRespuesta = (double) sumaTiempoRespuesta / totalProcesosTerminados;
        double promedioBloqueo = (double) sumaTiempoBloqueado / totalProcesosTerminados;

        int totalCiclosCPU = ciclosCPUOcupada + ciclosCPUInactiva;
        double usoCPU = (totalCiclosCPU == 0) ? 0 : ((double) ciclosCPUOcupada / totalCiclosCPU) * 100;

        StringBuilder sb = new StringBuilder();
        sb.append("=== REPORTE DE RENDIMIENTO DEL SISTEMA OPERATIVO ===\n\n");
        sb.append("Reloj Actual del Sistema: ").append(relojDelSistema).append(" ciclos\n");
        sb.append("Procesos Terminados: ").append(totalProcesosTerminados).append("\n");
        sb.append("----------------------------------------------------\n\n");

        sb.append("--- MÉTRICAS DE PLANIFICACIÓN ---\n");
        sb.append(String.format("1. Cumplimiento de Deadline: %.2f%%\n", porcentajeDeadline));
        sb.append(String.format("2. Tiempo Promedio de Respuesta: %.2f ciclos\n", promedioRespuesta));
        sb.append(String.format("3. Tiempo Promedio Bloqueado: %.2f ciclos\n\n", promedioBloqueo));

        sb.append("--- MÉTRICAS DE HARDWARE ---\n");
        sb.append(String.format("4. Utilización de la CPU: %.2f%%\n", usoCPU));
        sb.append("5. RAM Disponible Restante: ").append(memoriaRAMDisponible).append(" MB\n");
        sb.append("6. Total de Memoria en Swap: ").append(getMemoriaEnSwap()).append(" MB\n");

        return sb.toString();
    }

    private void actualizarGUI() {
        if (actualizadorVisual != null) actualizadorVisual.run();
    }
}