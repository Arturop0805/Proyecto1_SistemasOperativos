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
    private Cola colaListosSuspendidos;
    private ListaSimple<Proceso> listaTodos;
    private String politicaActual = "FCFS";
    private int contadorQuantum = 0;
    private int relojDelSistema = 0;
    
    // --- VARIABLES PARA ESTADÍSTICAS ---
    private int totalProcesosTerminados = 0;
    private int totalProcesosCumplenDeadline = 0;
    private int sumaTiempoRespuesta = 0; // Suma de (t_primera_ejec - t_llegada)
    private int sumaTiempoBloqueado = 0; // Suma de tiempo bloqueado de procesos terminados
    
    private Proceso procesoEnEjecucion;
    private int memoriaUsada;
    private int procesosTerminados;
    private int contadorProcesosManuales = 1;
    
    private Administrador() {
        this.colaListos = new Cola();
        this.colaListosPrioridad = new Cola(); 
        this.colaBloqueados = new Cola();
        this.colaListosSuspendidos = new Cola();
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
        // Verificar si hay alguien en CPU para mandar 100 o 0
        boolean cpuOcupado = (procesoEnEjecucion != null);
        
        // Llamar a Principal (usando tu Singleton o referencia estática)
        Principal.getInstancia().actualizarGraficaCPU(cpuOcupado);
        this.relojDelSistema++; // El Kernel avanza su propio reloj
        
        // Actualizamos la Vista con NUESTRO reloj interno, no el externo
        Principal.getInstancia().actualizarReloj(this.relojDelSistema);
        
        // 1. Gestión del Reloj y Procesos (Igual que antes)
        // ... (Tu código de revisar bloqueados y nuevos) ...
        revisarColaBloqueados();
        checkNuevosProcesos();
            
        // 2. Despachador (Si CPU libre)
        if (procesoEnEjecucion == null) {
            despacharProceso();
        }

        // 3. Ejecución y Control de Quantum
        if (procesoEnEjecucion != null) {
            
            if (Math.random() < Config.PROB_BLOQUEO) { 
                bloquearProcesoActual();
                return; // Importante: Si se bloquea, no ejecuta instrucción ni gasta Quantum en este ciclo
            }
            
            
            // --- IMPLEMENTACIÓN ROUND ROBIN ---
            if (politicaActual.equals("Round Robin")) {
                contadorQuantum++; // Incrementamos el uso de CPU
                
                // Verificamos contra la configuración global
                if (contadorQuantum >= Config.QUANTUM_DEFAULT) {
                    System.out.println("[RR QUANTUM] " + procesoEnEjecucion.getId() + " agotó su tiempo.");
                    
                    // Context Switch: Expulsión
                    procesoEnEjecucion.setEstado(Estado.LISTO);
                    colaListos.encolar(procesoEnEjecucion); // Va al final de la cola
                    
                    // Actualizar interfaz
                    Principal.getInstancia().actualizarCPU(null);
                    Principal.getInstancia().actualizarColaListos(colaListos);
                    
                    procesoEnEjecucion = null;
                    contadorQuantum = 0; // Reseteamos contador
                    
                    // Intentamos meter al siguiente inmediatamente
                    despacharProceso();
                }
            }
            // ----------------------------------

            // Ejecución normal de la instrucción (si aún sigue en CPU tras el chequeo RR)
            if (procesoEnEjecucion != null) {
                // ... (Tu código de bloqueo aleatorio y ejecución) ...
                
                boolean termino = procesoEnEjecucion.ejecutarInstruccion();
                Principal.getInstancia().actualizarCPU(procesoEnEjecucion);

                if (termino) {
                    terminarProceso(procesoEnEjecucion);
                    contadorQuantum = 0; // Importante: Resetear al terminar
                }
            }
        }
    }

   public void agregarProcesoManual(String nombre, int instrucciones, int prioridad, boolean esSistema, int periodo) {
        
        // Generación de ID (Ej: SYS-1 o USR-1)
        String idPrefix = "PM" + this.contadorProcesosManuales;
        String nuevoId = idPrefix + "-" + String.format("%03d", contadorProcesosManuales++);
        
        // Calculo de Deadline (Si es periódico, deadline = periodo, si no, un valor por defecto)
        // Como el usuario no lo edita, asumimos esta regla de negocio básica.
        int deadlineCalculado = (periodo > 0) ? periodo : instrucciones * 2; 

        // Creación del objeto Proceso
        Proceso nuevoProceso = new Proceso(
            nuevoId, 
            nombre, 
            instrucciones, 
            deadlineCalculado, 
            prioridad, 
            esSistema, 
            periodo
        );
        
        nuevoProceso.setTiempoLlegada(this.relojDelSistema);
        // --- LÓGICA DE ADMISIÓN (RAM vs DISCO) ---
        
        // Verificamos si cabe en la memoria (Usando la constante de Config)
        if (memoriaUsada + Config.TAMANO_PROCESO <= Config.MEMORIA_TOTAL) {
            
            // Si hay espacio -> A la cola de LISTOS (RAM)
            nuevoProceso.setEstado(Estado.LISTO);
            nuevoProceso.setTiempoLlegada(this.relojDelSistema);
            
            colaListos.encolar(nuevoProceso);
            memoriaUsada += Config.TAMANO_PROCESO;
            
            System.out.println("[CREACION MANUAL] " + nuevoId + " agregado a RAM (Listo).");
            
            // Actualizar GUI
            Principal.getInstancia().actualizarColaListos(colaListos);
            Principal.getInstancia().actualizarMemoria(memoriaUsada, Config.MEMORIA_TOTAL);
            
        } else {
            
            // No hay espacio -> A la cola de SUSPENDIDOS (Disco)
            nuevoProceso.setEstado(Estado.LISTO_SUSPENDIDO);
            nuevoProceso.setTiempoLlegada(this.relojDelSistema);
            
            colaListosSuspendidos.encolar(nuevoProceso);
            
            System.out.println("[CREACION MANUAL] " + nuevoId + " enviado a SUSPENDIDOS (Memoria llena).");
            
            // Actualizar GUI
            Principal.getInstancia().actualizarColaSuspendidos(colaListosSuspendidos);
        }
    }

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
            p.agregarTiempoBloqueado();
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
        boolean huboCambiosListos = false;
        boolean huboCambiosSuspendidos = false;
        
        Nodo<Proceso> actual = listaTodos.cabeza;
        while (actual != null) {
            Proceso p = actual.dato;
            
            // Solo procesamos si es NUEVO (aún no ha entrado al sistema)
            if (p.getEstado() == Estado.NUEVO) {
                p.setTiempoLlegada(this.relojDelSistema);
                // Opción A: Hay espacio en RAM
                if (memoriaUsada + Config.TAMANO_PROCESO <= Config.MEMORIA_TOTAL) {
                    p.setEstado(Estado.LISTO);
                    p.setTiempoLlegada(this.relojDelSistema);
                    colaListos.encolar(p);
                    memoriaUsada += Config.TAMANO_PROCESO;
                    huboCambiosListos = true;
                    System.out.println("[ADMISION RAM] " + p.getId() + " -> Ready.");
                } 
                // Opción B: Memoria llena -> A Suspendidos (Disco)
                else {
                    p.setEstado(Estado.LISTO_SUSPENDIDO);
                    p.setTiempoLlegada(this.relojDelSistema);
                    colaListosSuspendidos.encolar(p);
                    huboCambiosSuspendidos = true;
                    System.out.println("[ADMISION SWAP] " + p.getId() + " -> Suspendido (Memoria llena).");
                }
            }
            actual = actual.siguiente;
        }

        if (huboCambiosListos) Principal.getInstancia().actualizarColaListos(colaListos);
        if (huboCambiosSuspendidos) Principal.getInstancia().actualizarColaSuspendidos(colaListosSuspendidos); // [NUEVO]
        
        // Actualizar barra de memoria
        Principal.getInstancia().actualizarMemoria(memoriaUsada, Config.MEMORIA_TOTAL);
    }
    
    private void intentarSwapIn() {
        if (!colaListosSuspendidos.estaVacia() && memoriaUsada + Config.TAMANO_PROCESO <= Config.MEMORIA_TOTAL) {
            Proceso p = colaListosSuspendidos.desencolar();
            p.setEstado(Estado.LISTO);
            colaListos.encolar(p);
            memoriaUsada += Config.TAMANO_PROCESO;
            
            System.out.println("[SWAP IN] " + p.getId() + " movido de Disco a RAM.");
            
            // Actualizar ambas colas en la GUI
            Principal.getInstancia().actualizarColaSuspendidos(colaListosSuspendidos);
            Principal.getInstancia().actualizarColaListos(colaListos);
            Principal.getInstancia().actualizarMemoria(memoriaUsada, Config.MEMORIA_TOTAL);
            
            // Si liberamos suficiente espacio, intenta traer a otro recursivamente (opcional)
            // intentarSwapIn(); 
        }
    }
    
    private void despacharProceso() {
        if (!colaListos.estaVacia()) {
            Proceso p = colaListos.desencolar();
            p.setEstado(Estado.EJECUCION);
            
            // --- CORRECCIÓN MÉTRICA TIEMPO RESPUESTA ---
            if (p.getTiempoPrimeraEjecucion() == -1) {
                // 1. Marcar el ciclo actual como primera ejecución
                p.setTiempoPrimeraEjecucion(this.relojDelSistema);
                
                // 2. Obtener llegada (Asegurándonos que sea el ciclo, ej: 0, 5, 10)
                int llegada = p.getTiempoLlegadaInt(); 
                
                // 3. Calcular respuesta: Ciclo Actual (ej: 50) - Llegada (ej: 0) = 50
                int respuesta = this.relojDelSistema - llegada;
                
                // 4. Protección contra números negativos o absurdos
                //if (respuesta < 0) respuesta = 0; 
                
                this.sumaTiempoRespuesta += respuesta;
                
                // Debug (Opcional): Si sale un número raro, esto te lo dirá en consola
                // System.out.println("Proceso " + p.getId() + " Respuesta: " + respuesta + " (Reloj: " + this.relojDelSistema + " - Llegada: " + llegada + ")");
            }
            this.procesoEnEjecucion = p;
            
            // Actualizar GUI: Quitamos de la cola y ponemos en CPU
            Principal.getInstancia().actualizarColaListos(colaListos);
            Principal.getInstancia().actualizarCPU(p);
            
            System.out.println("[DISPATCH] " + p.getId() + " entra a CPU.");
        }
        
       

    }
    
    private void terminarProceso(Proceso p) {
        p.setEstado(Estado.TERMINADO);
        p.setTiempoFinalizacion(this.relojDelSistema); // Marcar hora fin

        this.procesoEnEjecucion = null;
        this.memoriaUsada -= Config.TAMANO_PROCESO;
        
        // --- NUEVO: Lógica de Estadísticas ---
        this.procesosTerminados++;
        this.totalProcesosTerminados++;
        
        // 1. Verificar Deadline (Tiempo Retorno <= Deadline)
        // Tiempo Retorno = (Fin - Llegada)
        int tiempoRetorno = p.getTiempoFinalizacion() - (int)p.getTiempoLlegada();
        if (tiempoRetorno <= p.getDeadline()) {
            this.totalProcesosCumplenDeadline++;
        }
        
        // 2. Acumular tiempo bloqueado global
        this.sumaTiempoBloqueado += p.getTiempoTotalBloqueado();
        // -------------------------------------

        // Actualizar GUI CPU (ponerla libre)
        Principal.getInstancia().actualizarCPU(null);
        System.out.println("[TERMINADO] " + p.getId());

        intentarSwapIn();
    }
    
   public void cambiarPolitica(String nuevaPolitica) {
        this.politicaActual = nuevaPolitica;
        System.out.println("[KERNEL] Cambio de política a: " + nuevaPolitica);

        // 1. Reordenar la cola de listos inmediatamente
        reordenarColaListos();
        
        // 2. Verificar si el proceso en CPU debe ser expulsado (Preemption)
        verificarPreempcion();

        // 3. Refrescar la interfaz
        Principal.getInstancia().actualizarColaListos(colaListos);
        Principal.getInstancia().actualizarCPU(procesoEnEjecucion);
    }
    
    private void reordenarColaListos() {
    if (colaListos.estaVacia()) return;

    // 1. Extraer todos los procesos a una lista temporal para ordenar
    ListaSimple<Proceso> temporal = new ListaSimple<>();
    while (!colaListos.estaVacia()) {
        temporal.agregarFinal(colaListos.desencolar());
    }

    // 2. Aplicar algoritmo de ordenamiento según política
    // Aquí implementas la lógica de comparación
    ordenarListaSegunPolitica(temporal);

    // 3. Devolver a la cola ya ordenados
    Nodo<Proceso> aux = temporal.cabeza;
    while (aux != null) {
        colaListos.encolar(aux.dato);
        aux = aux.siguiente;
    }
}
    
   private void ordenarListaSegunPolitica(ListaSimple<Proceso> lista) {
        if (lista.cabeza == null || lista.cabeza.siguiente == null) return;

        // IMPORTANTE: Round Robin es FIFO, no se debe reordenar la cola.
        if (politicaActual.equals("Round Robin")) return;

        boolean huboIntercambio;
        do {
            huboIntercambio = false;
            Nodo<Proceso> actual = lista.cabeza;
            Nodo<Proceso> siguiente = lista.cabeza.siguiente;

            while (siguiente != null) {
                boolean debeCambiar = false;
                Proceso p1 = actual.dato;
                Proceso p2 = siguiente.dato;

                switch (politicaActual) {
                    case "FCFS":
                        if (p1.getTiempoLlegada() > p2.getTiempoLlegada()) debeCambiar = true;
                        break;

                    case "SRT":
                        if (p1.getInstruccionesRestantes() > p2.getInstruccionesRestantes()) debeCambiar = true;
                        break;

                    case "EDF":
                        if (p1.getDeadline() > p2.getDeadline()) debeCambiar = true;
                        break;

                    // --- NUEVA LÓGICA DE PRIORIDAD ---
                    case "Prioridad":
                        // Criterio: Menor valor numérico = Mayor prioridad
                        if (p1.getPrioridad() > p2.getPrioridad()) {
                            debeCambiar = true;
                        } 
                        // Desempate por llegada (FIFO) si tienen misma prioridad
                        else if (p1.getPrioridad() == p2.getPrioridad()) {
                            if (p1.getTiempoLlegada() > p2.getTiempoLlegada()) {
                                debeCambiar = true;
                            }
                        }
                        break;
                    // ---------------------------------
                }

                if (debeCambiar) {
                    Proceso temp = actual.dato;
                    actual.dato = siguiente.dato;
                    siguiente.dato = temp;
                    huboIntercambio = true;
                }
                actual = siguiente;
                siguiente = siguiente.siguiente;
            }
        } while (huboIntercambio);
    }
    
    private void verificarPreempcion() {
        if (procesoEnEjecucion == null || colaListos.estaVacia()) return;

        Proceso mejorCandidato = colaListos.getFrente().dato;
        boolean debeExpulsar = false;

        switch (politicaActual) {
            case "SRT":
                if (mejorCandidato.getInstruccionesRestantes() < procesoEnEjecucion.getInstruccionesRestantes()) 
                    debeExpulsar = true;
                break;
                
            case "EDF":
                if (mejorCandidato.getDeadline() < procesoEnEjecucion.getDeadline()) 
                    debeExpulsar = true;
                break;

            // --- EXPROPIACIÓN POR PRIORIDAD ---
            case "Prioridad":
                // Si el proceso en cola tiene un número MENOR (mejor prioridad)
                // que el proceso actual -> Expulsar.
                if (mejorCandidato.getPrioridad() < procesoEnEjecucion.getPrioridad()) {
                    debeExpulsar = true;
                    System.out.println("[PRIO PREEMPTION] " + procesoEnEjecucion.getId() + 
                                       " (Prio " + procesoEnEjecucion.getPrioridad() + ")" +
                                       " expulsado por " + mejorCandidato.getId() + 
                                       " (Prio " + mejorCandidato.getPrioridad() + ")");
                }
                break;
            // ----------------------------------
        }

        if (debeExpulsar) {
            procesoEnEjecucion.setEstado(Estado.LISTO);
            colaListos.encolar(procesoEnEjecucion);
            procesoEnEjecucion = null;
            contadorQuantum = 0; // Resetear siempre al salir de CPU
            
            reordenarColaListos(); // Reordenar para que el expulsado se ubique bien
            despacharProceso();    // Meter al nuevo
        }
    }
    
    public int getRelojSistema() {return this.relojDelSistema;}
    
    // --- MÉTODO DE REPORTE ---
    public String obtenerReporteEstadisticas() {
        

        double porcentajeDeadline = ((double) totalProcesosCumplenDeadline / totalProcesosTerminados) * 100;
        double promedioRespuesta = (double) sumaTiempoRespuesta / totalProcesosTerminados;
        // Nota: El promedio de bloqueo se suele calcular sobre el total de procesos terminados
        // o solo sobre los que se bloquearon. Aquí usaremos el total terminados para ver el impacto global.
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
        sb.append("   (Tiempo promedio en estado I/O wait)\n");
        
        return sb.toString();
    }
}