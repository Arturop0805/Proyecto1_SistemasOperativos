/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Interfaces;

/**
 *
 * @author tomas
 */
import ComponentesVisuales.LabelRedondo;
import Interfaces.PanelGraficaCPU;
import EstructurasDeDatos.Nodo;
import EstructurasDeDatos.Cola;
import Modelo.Estado;
import Modelo.Proceso;
import Simulacion.Administrador;
import Simulacion.Reloj;
import Simulacion.Config;
import Utilidades.GeneradorProcesos;



import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.OutputStream;
import java.io.PrintStream;
import EstructurasDeDatos.ListaSimple;

/**
 * Ventana Principal del Simulador RTOS UNIMET-Sat.
 * INTERFAZ DEFINITIVA: Log de Eventos conectado, Memoria Dinámica,
 * Visor de PCB, Panel de Terminados y Gestor Inteligente de Swap.
 */
public class Principal extends JFrame {

    private static Principal instancia;

    // --- Componentes Visuales Dinámicos ---
    private JLabel lblReloj;
    private JLabel lblCpuId;
    private JLabel lblCpuNombre;
    private JLabel lblModoEjecucion; 
    private PanelGraficaCPU panelGrafica;
    private JProgressBar barraMemoriaRAM;
    private JProgressBar barraMemoriaSwap; 
    private JTextArea areaLogEventos; 
    
    // --- Controles de la Simulación ---
    private JComboBox<String> comboPolitica;
    private JSpinner spinnerQuantum; 
    private JSlider sliderVelocidad; 
    private JButton btnIniciar;
    private JButton btnPausar;
    private JButton btnReporte; 
    private JButton btnCargarCSV; 
    private JButton btnGenerar20; 
    private JButton btnCrearAleatorio; 
    
    // --- Controles para Crear Proceso Manual ---
    private JTextField txtNombreProc;
    private JTextField txtInstruccionesProc;
    private JTextField txtPrioridadProc;
    private JTextField txtDeadlineProc;
    private JTextField txtPeriodoProc; 
    private JCheckBox chkEsSistema;
    private JButton btnCrearManual;

    // --- Contenedores de las Colas ---
    private JPanel panelColaListos;
    private JPanel panelColaBloqueados;
    private JPanel panelColaListosSuspendidos;
    private JPanel panelColaBloqueadosSuspendidos;
    private JPanel panelColaTerminados; // NUEVO PANEL
    
    private boolean simulacionIniciada = false;

    // Registro global de procesos para la cola de terminados y estadísticas
    private ListaSimple<Proceso> historialProcesos = new ListaSimple<>();

    public Principal() {
        setTitle("UNIMET-Sat RTOS Simulator - Mission Control");
        setSize(1450, 900); 
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(15, 15)); 

        Administrador.getInstancia().setActualizadorVisual(() -> actualizarInterfaz());

        inicializarComponentes();
        redirigirTerminalAlLog(); 
    }
    
    public static Principal getInstancia() {
        if (instancia == null) {
            instancia = new Principal();
        }
        return instancia;
    }

    private void inicializarComponentes() {
        // =========================================================
        // PANEL IZQUIERDO: CONTROLES
        // =========================================================
        JPanel panelIzquierdo = new JPanel();
        panelIzquierdo.setLayout(new BoxLayout(panelIzquierdo, BoxLayout.Y_AXIS));
        panelIzquierdo.setBorder(new EmptyBorder(10, 10, 10, 10));

        // --- Reloj ---
        JPanel panelReloj = new JPanel(new GridLayout(2, 1, 5, 5));
        panelReloj.setBackground(new Color(30, 30, 30));
        panelReloj.setBorder(new EmptyBorder(10, 10, 10, 10));
        panelReloj.setMaximumSize(new Dimension(400, 80));
        
        JLabel lblTituloReloj = new JLabel("MISSION CLOCK", SwingConstants.CENTER);
        lblTituloReloj.setForeground(Color.GRAY);
        lblReloj = new JLabel("Ciclo: 0", SwingConstants.CENTER);
        lblReloj.setForeground(Color.GREEN);
        lblReloj.setFont(new Font("Consolas", Font.BOLD, 26));
        panelReloj.add(lblTituloReloj);
        panelReloj.add(lblReloj);

        // --- Controles y Políticas ---
        JPanel panelControles = new JPanel(new GridLayout(10, 1, 5, 10)); 
        panelControles.setBorder(BorderFactory.createTitledBorder("Control de Misión"));
        panelControles.setMaximumSize(new Dimension(400, 390));
        
        btnCargarCSV = new JButton("📁 Cargar Procesos (CSV)");
        
        JPanel panelPolitica = new JPanel(new BorderLayout(5, 0));
        comboPolitica = new JComboBox<>(new String[]{"FCFS", "Round Robin", "SRT", "Prioridad Estática Preemptiva", "EDF"}); 
        spinnerQuantum = new JSpinner(new SpinnerNumberModel(Config.QUANTUM_DEFAULT, 1, 50, 1));
        spinnerQuantum.setToolTipText("Quantum (Solo para RR)");
        panelPolitica.add(comboPolitica, BorderLayout.CENTER);
        panelPolitica.add(spinnerQuantum, BorderLayout.EAST);
        
        btnIniciar = new JButton("▶ Iniciar Simulación");
        estilarBoton(btnIniciar, new Color(34, 139, 34), Color.WHITE);
        
        btnPausar = new JButton("⏸ Pausar");
        btnPausar.setEnabled(false);
        
        btnReporte = new JButton("📊 Ver Reporte"); 
        
        btnGenerar20 = new JButton("🚀 Generar 20 Procesos");
        estilarBoton(btnGenerar20, new Color(70, 130, 180), Color.WHITE);

        btnCrearAleatorio = new JButton("🎲 Generar 1 Proceso Aleatorio");
        estilarBoton(btnCrearAleatorio, new Color(128, 0, 128), Color.WHITE);

        // --- Slider de Velocidad ---
        sliderVelocidad = new JSlider(10, 2000, 1000); 
        sliderVelocidad.setMajorTickSpacing(500);
        sliderVelocidad.setMinorTickSpacing(100);
        sliderVelocidad.setPaintTicks(true);
        sliderVelocidad.setPaintLabels(true);
        sliderVelocidad.setBorder(BorderFactory.createTitledBorder("Duración del Ciclo (ms)"));
        
        panelControles.add(btnCargarCSV);
        panelControles.add(new JLabel("Política de Planificación | Quantum:"));
        panelControles.add(panelPolitica);
        panelControles.add(sliderVelocidad); 
        panelControles.add(btnIniciar);
        panelControles.add(btnPausar);
        panelControles.add(btnReporte);
        panelControles.add(btnGenerar20); 
        panelControles.add(btnCrearAleatorio);

        // --- Creación Manual de Procesos ---
        JPanel panelCreacion = new JPanel(new GridLayout(8, 2, 8, 10));
        panelCreacion.setBorder(BorderFactory.createTitledBorder("Crear Proceso Manual"));
        panelCreacion.setMaximumSize(new Dimension(400, 320));
        
        panelCreacion.add(new JLabel("Nombre:"));
        txtNombreProc = new JTextField("Proc_Test");
        panelCreacion.add(txtNombreProc);
        
        panelCreacion.add(new JLabel("Instrucciones:"));
        txtInstruccionesProc = new JTextField("100");
        panelCreacion.add(txtInstruccionesProc);
        
        panelCreacion.add(new JLabel("Prioridad (1-30 Sis, 31-99 Us):"));
        txtPrioridadProc = new JTextField("50");
        panelCreacion.add(txtPrioridadProc);
        
        panelCreacion.add(new JLabel("Deadline:"));
        txtDeadlineProc = new JTextField("500");
        panelCreacion.add(txtDeadlineProc);
        
        panelCreacion.add(new JLabel("Periodo (0=Aperiódico):"));
        txtPeriodoProc = new JTextField("0");
        panelCreacion.add(txtPeriodoProc);
        
        panelCreacion.add(new JLabel("Tipo:"));
        chkEsSistema = new JCheckBox("Es de Sistema");
        panelCreacion.add(chkEsSistema);
        
        btnCrearManual = new JButton("Crear y Encolar");
        panelCreacion.add(new JLabel("")); 
        panelCreacion.add(btnCrearManual);

        // Ensamblar Panel Izquierdo
        panelIzquierdo.add(panelReloj);
        panelIzquierdo.add(Box.createVerticalStrut(15));
        panelIzquierdo.add(panelControles);
        panelIzquierdo.add(Box.createVerticalStrut(15));
        panelIzquierdo.add(panelCreacion);
        panelIzquierdo.add(Box.createVerticalGlue());

        JScrollPane scrollIzquierdo = new JScrollPane(panelIzquierdo);
        scrollIzquierdo.setPreferredSize(new Dimension(380, getHeight()));
        scrollIzquierdo.setBorder(null);
        scrollIzquierdo.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        add(scrollIzquierdo, BorderLayout.WEST);

        // =========================================================
        // PANEL CENTRAL: CPU, MEMORIAS Y COLAS
        // =========================================================
        JPanel panelCentro = new JPanel(new BorderLayout(15, 15));
        panelCentro.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel panelSuperior = new JPanel(new GridLayout(1, 2, 15, 15));
        
        // 1. CPU
        JPanel panelCPU = new JPanel(new BorderLayout(10, 10));
        panelCPU.setBorder(BorderFactory.createTitledBorder("Unidad Central de Procesamiento (CPU)"));
        JPanel infoCPU = new JPanel(new GridLayout(3, 1)); 
        infoCPU.setPreferredSize(new Dimension(170, 100));
        



        
        lblCpuId = new JLabel("LIBRE", SwingConstants.CENTER);
        lblCpuId.setFont(new Font("Arial", Font.BOLD, 22));
        lblCpuNombre = new JLabel("Ninguno", SwingConstants.CENTER);
        
        lblModoEjecucion = new JLabel("MODO: ESPERA / IDLE", SwingConstants.CENTER);
        lblModoEjecucion.setOpaque(true);
        lblModoEjecucion.setBackground(Color.LIGHT_GRAY);
        lblModoEjecucion.setForeground(Color.BLACK);
        lblModoEjecucion.setFont(new Font("Arial", Font.BOLD, 12));
        lblModoEjecucion.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));

        infoCPU.add(lblCpuId);
        infoCPU.add(lblCpuNombre);
        infoCPU.add(lblModoEjecucion);
        
        panelGrafica = new PanelGraficaCPU();
        panelGrafica.setPreferredSize(new Dimension(300, 100));
        panelCPU.add(infoCPU, BorderLayout.WEST);
        panelCPU.add(panelGrafica, BorderLayout.CENTER);
        
        // 2. Memorias (RAM y Swap)
        JPanel panelMemorias = new JPanel(new GridLayout(2, 1, 5, 10));
        panelMemorias.setBorder(BorderFactory.createTitledBorder("Gestión de Memoria (RAM & Swap)"));
        barraMemoriaRAM = new JProgressBar(0, Config.MEMORIA_TOTAL);
        barraMemoriaRAM.setStringPainted(true);
        barraMemoriaRAM.setFont(new Font("Arial", Font.BOLD, 14));
        
        barraMemoriaSwap = new JProgressBar(0, Config.MEMORIA_TOTAL * 2); 
        barraMemoriaSwap.setStringPainted(true);
        barraMemoriaSwap.setFont(new Font("Arial", Font.BOLD, 14));
        barraMemoriaSwap.setForeground(new Color(148, 0, 211)); 
        
        panelMemorias.add(crearPanelConTituloPequeño("Memoria Principal (RAM)", barraMemoriaRAM));
        panelMemorias.add(crearPanelConTituloPequeño("Espacio de Intercambio (Swap)", barraMemoriaSwap));

        panelSuperior.add(panelCPU);
        panelSuperior.add(panelMemorias);
        panelCentro.add(panelSuperior, BorderLayout.NORTH);

        // 3. Colas (Dividido en Activas y Terminados)
        JPanel panelPrincipalColas = new JPanel(new BorderLayout(10, 10));
        
        JPanel colasActivas = new JPanel(new GridLayout(2, 2, 10, 10));
        panelColaListos = crearPanelCola("Cola Listos (RAM)");
        panelColaListosSuspendidos = crearPanelCola("Listos-Suspendidos (Swap Out)");
        panelColaBloqueados = crearPanelCola("Cola Bloqueados (RAM)");
        panelColaBloqueadosSuspendidos = crearPanelCola("Bloqueados-Suspendidos (Swap Out)");
        
        colasActivas.add(new JScrollPane(panelColaListos, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));
        colasActivas.add(new JScrollPane(panelColaListosSuspendidos, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));
        colasActivas.add(new JScrollPane(panelColaBloqueados, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));
        colasActivas.add(new JScrollPane(panelColaBloqueadosSuspendidos, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));

        panelColaTerminados = crearPanelCola("🏁 Procesos Terminados (Histórico)");
        JScrollPane scrollTerminados = new JScrollPane(panelColaTerminados, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollTerminados.setPreferredSize(new Dimension(0, 120));

        panelPrincipalColas.add(colasActivas, BorderLayout.CENTER);
        panelPrincipalColas.add(scrollTerminados, BorderLayout.SOUTH);

        panelCentro.add(panelPrincipalColas, BorderLayout.CENTER);
        
        // 4. Log de Eventos (Terminal)
        areaLogEventos = new JTextArea();
        areaLogEventos.setEditable(false);
        areaLogEventos.setFont(new Font("Consolas", Font.PLAIN, 13));
        areaLogEventos.setBackground(new Color(25, 25, 25));
        areaLogEventos.setForeground(new Color(0, 255, 0)); 
        
        JScrollPane scrollLog = new JScrollPane(areaLogEventos);
        scrollLog.setBorder(BorderFactory.createTitledBorder("Terminal / Log de Eventos del SO"));
        scrollLog.setPreferredSize(new Dimension(panelCentro.getWidth(), 180));
        panelCentro.add(scrollLog, BorderLayout.SOUTH);

        add(panelCentro, BorderLayout.CENTER);

        // =========================================================
        // EVENTOS
        // =========================================================

        sliderVelocidad.addChangeListener(e -> {
            int ms = sliderVelocidad.getValue();
            if (Reloj.getInstancia() != null) {
                Reloj.getInstancia().setDuracionCiclo(ms); 
            }
        });

        btnGenerar20.addActionListener(e -> {
            for (int i = 0; i < 20; i++) {
                Proceso p = GeneradorProcesos.crearProcesoAleatorio();
                aplicarLimiteSeguridadMemoria(p);
                historialProcesos.agregarFinal(p);
                despacharProceso(p);
            }
            actualizarInterfaz();
        });

        btnCrearAleatorio.addActionListener(e -> {
            Proceso p = GeneradorProcesos.crearProcesoAleatorio();
            aplicarLimiteSeguridadMemoria(p);
            historialProcesos.agregarFinal(p);
            despacharProceso(p);
            actualizarInterfaz();
        });

        spinnerQuantum.addChangeListener(e -> {
            int q = (int) spinnerQuantum.getValue();
            System.out.println("[KERNEL] Quantum ajustado a: " + q);
        });

        btnIniciar.addActionListener(e -> {
            if (!simulacionIniciada) {
                Administrador.getInstancia().iniciarSimulacion(0); 
                Reloj.getInstancia().start();
                simulacionIniciada = true;
                btnIniciar.setEnabled(false);
                btnPausar.setEnabled(true);
                System.out.println(">>> SISTEMA OPERATIVO INICIADO <<<");
            }
        });

        btnPausar.addActionListener(e -> {
            if (Reloj.getInstancia().isPausado()) {
                Reloj.getInstancia().reanudar();
                btnPausar.setText("⏸ Pausar");
                btnPausar.setForeground(Color.BLACK);
                System.out.println("[USER] Simulación Reanudada.");
            } else {
                Reloj.getInstancia().pausar();
                btnPausar.setText("▶ Reanudar");
                btnPausar.setForeground(Color.RED);
                System.out.println("[USER] Simulación Pausada.");
            }
        });

        btnReporte.addActionListener(e -> {
            String reporte = Administrador.getInstancia().obtenerReporteEstadisticas();
            JOptionPane.showMessageDialog(this, reporte, "Reporte de Rendimiento", JOptionPane.INFORMATION_MESSAGE);
        });

        btnCrearManual.addActionListener(e -> {
            try {
                String nombre = txtNombreProc.getText();
                int inst = Integer.parseInt(txtInstruccionesProc.getText());
                int prio = Integer.parseInt(txtPrioridadProc.getText());
                int deadline = Integer.parseInt(txtDeadlineProc.getText());
                int periodo = Integer.parseInt(txtPeriodoProc.getText());
                boolean esSis = chkEsSistema.isSelected();
                
                String id = (esSis ? "S" : "U") + (int)(Math.random() * 1000);
                int cicloExcepcion = 1 + (int)(Math.random() * inst);   
                int ciclosResolver = 3 + (int)(Math.random() * 8);      
                int memoriaRequerida = 16 + (int)(Math.random() * 241); 

                Proceso p = new Proceso(id, nombre, inst, deadline, prio, esSis, periodo, cicloExcepcion, ciclosResolver, memoriaRequerida);
                p.setEstado(Estado.NUEVO);
                
                aplicarLimiteSeguridadMemoria(p);
                historialProcesos.agregarFinal(p);
                despacharProceso(p);
                actualizarInterfaz();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Solo números en Instrucciones, Prioridad, Deadline y Periodo.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        comboPolitica.addActionListener(e -> {
            String politica = (String) comboPolitica.getSelectedItem();
            Administrador.getInstancia().setPolitica(politica); 
            System.out.println("[KERNEL] Política cambiada a: " + politica);
        });
    }

    /**
     * Aplica límite de seguridad para que ningún proceso pueda causar un interbloqueo irresoluble
     * exigiendo más RAM de la que físicamente existe.
     */
    private void aplicarLimiteSeguridadMemoria(Proceso p) {
        int maxMemoriaUsuario = Config.MEMORIA_TOTAL - Config.MEMORIA_RESERVADA_SO;
        if (p.getMemoriaRequerida() > maxMemoriaUsuario) {
            p.setMemoriaRequerida(maxMemoriaUsuario);
            System.out.println("[SISTEMA] Alerta: Memoria de " + p.getId() + " reducida a " + maxMemoriaUsuario + "MB por límite físico de seguridad.");
        }
    }

    /**
     * MÉTODO CLAVE: Verifica la RAM disponible antes de ingresar un proceso nuevo.
     */
    private void despacharProceso(Proceso p) {
        Administrador admin = Administrador.getInstancia();
        int ramOcupada = calcularMemoriaRAMActual();

        if (ramOcupada + p.getMemoriaRequerida() <= Config.MEMORIA_TOTAL) {
            p.setEstado(Estado.LISTO);
            admin.getColaListos().encolar(p); 
            System.out.println("[KERNEL] Proceso " + p.getId() + " admitido en RAM (Cola Listos). Req: " + p.getMemoriaRequerida() + "MB");
        } else {
            p.setEstado(Estado.LISTO_SUSPENDIDO);
            admin.getColaListosSuspendidos().encolar(p);
            System.out.println("[KERNEL-SWAP] RAM LLENA. Proceso " + p.getId() + " enviado directo a Swap (Listos Suspendidos). Req: " + p.getMemoriaRequerida() + "MB");
        }
    }

    private int calcularMemoriaRAMActual() {
        Administrador admin = Administrador.getInstancia();
        int ram = Config.MEMORIA_RESERVADA_SO;
        if (admin.getProcesoEnEjecucion() != null) ram += admin.getProcesoEnEjecucion().getMemoriaRequerida();
        
        ram += sumarMemoriaCola(admin.getColaListos());
        ram += sumarMemoriaCola(admin.getColaListosPrioridad());
        ram += sumarMemoriaCola(admin.getColaBloqueados());
        return ram;
    }

    private int sumarMemoriaCola(Cola cola) {
        if (cola == null) return 0;
        int suma = 0;
        Nodo<Proceso> aux = cola.getFrente();
        while (aux != null) { 
            suma += aux.getDato().getMemoriaRequerida(); 
            aux = aux.getSiguiente(); 
        }
        return suma;
    }

    /**
     * GESTOR INTELIGENTE DE SWAP:
     * Si la RAM está llena y hay procesos atascados en Swap, expulsa proactivamente
     * a los procesos bloqueados en RAM hacia el disco para hacerles espacio.
     */
    private void gestionarSwapInteligente() {
        Administrador admin = Administrador.getInstancia();
        Cola listosSuspendidos = admin.getColaListosSuspendidos();
        
        if (listosSuspendidos == null || listosSuspendidos.estaVacia()) return;

        int ramLibre = Config.MEMORIA_TOTAL - calcularMemoriaRAMActual();
        Proceso candidatoSwapIn = (Proceso) listosSuspendidos.getFrente().getDato();

        // Si cabe naturalmente, lo traemos
        if (candidatoSwapIn.getMemoriaRequerida() <= ramLibre) {
            listosSuspendidos.desencolar();
            candidatoSwapIn.setEstado(Estado.LISTO);
            admin.getColaListos().encolar(candidatoSwapIn);
            System.out.println("[SWAP] " + candidatoSwapIn.getId() + " vuelve a RAM desde Swap. Estado: Listo.");
            return;
        }

        // Si no cabe, buscamos una VÍCTIMA en los Bloqueados
        Cola bloqueados = admin.getColaBloqueados();
        if (bloqueados != null && !bloqueados.estaVacia()) {
            boolean victimaEncontrada = false;
            int tamanoOriginal = obtenerTamanoCola(bloqueados);
            
            // Rotamos la cola para buscar y sacar a la víctima sin dañar el resto de la estructura
            for (int i = 0; i < tamanoOriginal; i++) {
                Proceso victima = (Proceso) bloqueados.desencolar();
                
                if (!victimaEncontrada && (ramLibre + victima.getMemoriaRequerida() >= candidatoSwapIn.getMemoriaRequerida())) {
                    // ¡Encontramos la víctima ideal! Lo mandamos a Bloqueado-Suspendido
                    victima.setEstado(Estado.BLOQUEADO_SUSPENDIDO);
                    if(admin.getColaBloqueadosSuspendidos() != null) {
                        admin.getColaBloqueadosSuspendidos().encolar(victima);
                    }
                    ramLibre += victima.getMemoriaRequerida();
                    victimaEncontrada = true;
                    System.out.println("[SWAP MANAGER] Víctima elegida: " + victima.getId() + " movido a Bloqueado-Suspendido para hacer espacio.");
                } else {
                    // Lo regresamos a su cola normal si no fue elegido
                    bloqueados.encolar(victima);
                }
            }

            // Si hicimos espacio exitosamente, metemos al candidato
            if (victimaEncontrada) {
                listosSuspendidos.desencolar();
                candidatoSwapIn.setEstado(Estado.LISTO);
                admin.getColaListos().encolar(candidatoSwapIn);
                System.out.println("[SWAP MANAGER] " + candidatoSwapIn.getId() + " ingresa a RAM tras el desalojo de memoria.");
            }
        }
    }

    private int obtenerTamanoCola(Cola cola) {
        int count = 0;
        Nodo<Proceso> aux = cola.getFrente();
        while (aux != null) {
            count++;
            aux = aux.getSiguiente();
        }
        return count;
    }

    /**
     * Muestra el modal del PCB del proceso con sus atributos detallados.
     */
    private void mostrarPCB(Proceso p) {
        JDialog modal = new JDialog(this, "PCB - Detalle del Proceso", true);
        modal.setSize(380, 400);
        modal.setLocationRelativeTo(this);
        modal.setLayout(new BorderLayout(10, 10));
        
        JPanel grid = new JPanel(new GridLayout(10, 2, 5, 10));
        grid.setBorder(new EmptyBorder(15, 15, 15, 15));
        
        grid.add(new JLabel("ID del Proceso:")); grid.add(new JLabel("<html><b>" + p.getId() + "</b></html>"));
        grid.add(new JLabel("Nombre:")); grid.add(new JLabel(p.getNombre()));
        
        JLabel lblEstado = new JLabel("<html><b><font color='blue'>" + p.getEstado() + "</font></b></html>");
        grid.add(new JLabel("Estado Actual:")); grid.add(lblEstado);
        
        grid.add(new JLabel("Tipo:")); grid.add(new JLabel(p.isEsSistema() ? "Proceso de Sistema" : "Proceso de Usuario"));
        grid.add(new JLabel("Prioridad Base:")); grid.add(new JLabel(String.valueOf(p.getPrioridad())));
        
        // Uso try-catch simples por si algún getter no se llama exactamente así en tu clase Proceso
        grid.add(new JLabel("Instrucciones:")); 
grid.add(new JLabel(p.getPC() + " / " + p.getTotalInstrucciones()));
        grid.add(new JLabel("Memoria:")); grid.add(new JLabel(p.getMemoriaRequerida() + " MB"));
        grid.add(new JLabel("Deadline:")); grid.add(new JLabel(String.valueOf(p.getDeadline())));
        grid.add(new JLabel("Periodo:")); grid.add(new JLabel(p.getPeriodo() > 0 ? String.valueOf(p.getPeriodo()) : "Aperiódico"));
        
        modal.add(grid, BorderLayout.CENTER);
        
        JButton btnCerrar = new JButton("Cerrar Visor");
        btnCerrar.addActionListener(e -> modal.dispose());
        JPanel pnlSur = new JPanel();
        pnlSur.add(btnCerrar);
        modal.add(pnlSur, BorderLayout.SOUTH);
        
        modal.setVisible(true);
    }

    private void redirigirTerminalAlLog() {
        PrintStream printStream = new PrintStream(new OutputStream() {
            @Override
            public void write(int b) {
                SwingUtilities.invokeLater(() -> {
                    areaLogEventos.append(String.valueOf((char) b));
                    areaLogEventos.setCaretPosition(areaLogEventos.getDocument().getLength());
                });
            }
        });
        System.setOut(printStream);
        System.setErr(printStream); 
    }

    private void estilarBoton(JButton btn, Color bg, Color fg) {
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFont(new Font("Arial", Font.BOLD, 13));
        btn.setFocusPainted(false);
        btn.setOpaque(true);
        btn.setBorderPainted(false); 
    }

    private JPanel crearPanelConTituloPequeño(String titulo, JComponent comp) {
        JPanel p = new JPanel(new BorderLayout());
        JLabel lbl = new JLabel(titulo);
        lbl.setFont(new Font("Arial", Font.PLAIN, 11));
        p.add(lbl, BorderLayout.NORTH);
        p.add(comp, BorderLayout.CENTER);
        return p;
    }

    private JPanel crearPanelCola(String titulo) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY), titulo));
        panel.setBackground(Color.WHITE);
        return panel;
    }

    public void setEstadoEjecucion(boolean esSO, String texto) {
        SwingUtilities.invokeLater(() -> {
            if (esSO) {
                lblModoEjecucion.setText(texto.toUpperCase());
                lblModoEjecucion.setBackground(new Color(220, 50, 50)); 
                lblModoEjecucion.setForeground(Color.WHITE);
            } else {
                lblModoEjecucion.setText(texto.toUpperCase());
                lblModoEjecucion.setBackground(new Color(50, 200, 50)); 
                lblModoEjecucion.setForeground(Color.BLACK);
            }
        });
    }

    public void actualizarInterfaz() {
        // GESTOR DE SWAP: Se ejecuta antes de pintar la UI para descongestionar si es posible
        gestionarSwapInteligente();

        SwingUtilities.invokeLater(() -> {
            Administrador admin = Administrador.getInstancia();

            if (lblReloj != null) lblReloj.setText("Ciclo: " + admin.getRelojSistema());

            Proceso pCpu = admin.getProcesoEnEjecucion();
            
            int memoriaUsoRAM = Config.MEMORIA_RESERVADA_SO;
            int memoriaUsoSwap = 0;

            if (pCpu != null) {
                lblCpuId.setText(pCpu.getId());
                lblCpuNombre.setText(pCpu.getNombre());
                lblCpuId.setForeground(new Color(0, 150, 0)); 
                actualizarGraficaCPU(true);
                
                memoriaUsoRAM += pCpu.getMemoriaRequerida();
                
                if (pCpu.isEsSistema()) {
                    lblModoEjecucion.setText("EJECUCIÓN: SISTEMA OPERATIVO");
                    lblModoEjecucion.setBackground(new Color(220, 50, 50));
                    lblModoEjecucion.setForeground(Color.WHITE);
                } else {
                    lblModoEjecucion.setText("EJECUCIÓN: PROGRAMA USUARIO");
                    lblModoEjecucion.setBackground(new Color(50, 200, 50));
                    lblModoEjecucion.setForeground(Color.BLACK);
                }
            } else {
                lblCpuId.setText("LIBRE");
                lblCpuNombre.setText("Ninguno");
                lblCpuId.setForeground(Color.BLACK);
                actualizarGraficaCPU(false);
                
                lblModoEjecucion.setText("MODO: ESPERA / IDLE");
                lblModoEjecucion.setBackground(Color.LIGHT_GRAY);
                lblModoEjecucion.setForeground(Color.BLACK);
            }

            panelColaListos.removeAll();
            panelColaBloqueados.removeAll();
            panelColaListosSuspendidos.removeAll();
            panelColaBloqueadosSuspendidos.removeAll();
            panelColaTerminados.removeAll();

            // RECORRER COLAS RAM
            Nodo<Proceso> actualPrio = admin.getColaListosPrioridad().getFrente();
            while (actualPrio != null) {
                Proceso p = (Proceso) actualPrio.getDato();
                agregarLabel(panelColaListos, p, Color.ORANGE);
                memoriaUsoRAM += p.getMemoriaRequerida();
                actualPrio = actualPrio.getSiguiente();
            }
            
            Nodo<Proceso> actualListo = admin.getColaListos().getFrente();
            while (actualListo != null) {
                Proceso p = (Proceso) actualListo.getDato();
                agregarLabel(panelColaListos, p, Color.GREEN);
                memoriaUsoRAM += p.getMemoriaRequerida();
                actualListo = actualListo.getSiguiente();
            }

            Nodo<Proceso> actualBloq = admin.getColaBloqueados().getFrente();
            while (actualBloq != null) {
                Proceso p = (Proceso) actualBloq.getDato();
                agregarLabel(panelColaBloqueados, p, Color.RED);
                memoriaUsoRAM += p.getMemoriaRequerida();
                actualBloq = actualBloq.getSiguiente();
            }

            // RECORRER COLAS SWAP
            Nodo<Proceso> actualListSus = admin.getColaListosSuspendidos().getFrente();
            while (actualListSus != null) {
                Proceso p = (Proceso) actualListSus.getDato();
                agregarLabel(panelColaListosSuspendidos, p, Color.GRAY);
                memoriaUsoSwap += p.getMemoriaRequerida();
                actualListSus = actualListSus.getSiguiente();
            }

            Cola colaBloqSus = admin.getColaBloqueadosSuspendidos();
            if (colaBloqSus != null) {
                Nodo<Proceso> actualBloqSus = colaBloqSus.getFrente();
                while (actualBloqSus != null) {
                    Proceso p = (Proceso) actualBloqSus.getDato();
                    agregarLabel(panelColaBloqueadosSuspendidos, p, new Color(139, 0, 0));
                    memoriaUsoSwap += p.getMemoriaRequerida();
                    actualBloqSus = actualBloqSus.getSiguiente();
                }
            }

            // RECORRER HISTORIAL PARA TERMINADOS
            Nodo<Proceso> actualHistorial = historialProcesos.cabeza; // O usar .getCabeza() si es privado
            while (actualHistorial != null) {
            Proceso p = actualHistorial.dato; // O usar .getDato() si es privado
            if (p.getEstado() != null && p.getEstado() == Estado.TERMINADO) {
                agregarLabel(panelColaTerminados, p, Color.BLUE);
            }
            actualHistorial = actualHistorial.siguiente; // O usar .getSiguiente()
}

            panelColaListos.revalidate(); panelColaListos.repaint();
            panelColaBloqueados.revalidate(); panelColaBloqueados.repaint();
            panelColaListosSuspendidos.revalidate(); panelColaListosSuspendidos.repaint();
            panelColaBloqueadosSuspendidos.revalidate(); panelColaBloqueadosSuspendidos.repaint();
            panelColaTerminados.revalidate(); panelColaTerminados.repaint();

            // ACTUALIZAR BARRAS DE PROGRESO
            if (barraMemoriaRAM != null) {
                if (memoriaUsoRAM > Config.MEMORIA_TOTAL) memoriaUsoRAM = Config.MEMORIA_TOTAL;
                barraMemoriaRAM.setValue(memoriaUsoRAM);
                barraMemoriaRAM.setString("Uso RAM: " + memoriaUsoRAM + " MB / " + Config.MEMORIA_TOTAL + " MB");
                barraMemoriaRAM.setForeground(memoriaUsoRAM > (Config.MEMORIA_TOTAL * 0.85) ? Color.RED : new Color(50, 150, 250));
            }
            
            if (barraMemoriaSwap != null) {
                barraMemoriaSwap.setValue(memoriaUsoSwap);
                barraMemoriaSwap.setString("Uso Swap: " + memoriaUsoSwap + " MB");
            }
        });
    }
    
    

    private void agregarLabel(JPanel panel, Proceso p, Color color) {
        if (p == null) return;
        LabelRedondo lbl = new LabelRedondo(p, color);
        lbl.setPreferredSize(new Dimension(70, 70));
        lbl.setToolTipText("Haz Clic para abrir el PCB");
        lbl.setCursor(new Cursor(Cursor.HAND_CURSOR)); // Cursor de mano interactivo
        
        // EVENTO: Mostrar modal al hacer clic en cualquier proceso
        lbl.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                mostrarPCB(p);
            }
        });

        panel.add(lbl);
    }

    public void actualizarGraficaCPU(boolean ocupado) {
        if (panelGrafica != null) {
            panelGrafica.agregarDato(ocupado ? 100 : 0);
            panelGrafica.repaint();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } 
            catch (Exception e) { e.printStackTrace(); }
            new Principal().setVisible(true);
        });
    }
}