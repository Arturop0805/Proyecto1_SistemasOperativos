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
import EstructurasDeDatos.Nodo;
import Modelo.Estado;
import Modelo.Proceso;
import Simulacion.Administrador;
import Simulacion.Reloj;
import Simulacion.Config;
import Utilidades.GeneradorProcesos;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Ventana Principal del Simulador RTOS UNIMET-Sat.
 * INTERFAZ FINALIZADA: Incluye Carga de CSV, Quantum y Periodo.
 */
public class Principal extends JFrame {

    private static Principal instancia;

    // --- Componentes Visuales Dinámicos ---
    private JLabel lblReloj;
    private JLabel lblCpuId;
    private JLabel lblCpuNombre;
    private PanelGraficaCPU panelGrafica;
    private JProgressBar barraMemoriaRAM;
    private JProgressBar barraMemoriaSwap; 
    
    // --- Controles de la Simulación ---
    private JComboBox<String> comboPolitica;
    private JSpinner spinnerQuantum; // NUEVO: Para Round Robin
    private JButton btnIniciar;
    private JButton btnPausar;
    private JButton btnReporte; 
    private JButton btnCargarCSV; // NUEVO: Para leer procesos.csv
    
    // --- Controles para Crear Proceso Manual ---
    private JTextField txtNombreProc;
    private JTextField txtInstruccionesProc;
    private JTextField txtPrioridadProc;
    private JTextField txtDeadlineProc;
    private JTextField txtPeriodoProc; // NUEVO: Para procesos periódicos (RTOS)
    private JCheckBox chkEsSistema;
    private JButton btnCrearManual;
    private JButton btnCrearAleatorio;

    // --- Contenedores de las Colas ---
    private JPanel panelColaListos;
    private JPanel panelColaBloqueados;
    private JPanel panelColaListosSuspendidos;
    private JPanel panelColaBloqueadosSuspendidos;
    
    private boolean simulacionIniciada = false;

    public Principal() {
        setTitle("UNIMET-Sat RTOS Simulator - Mission Control");
        setSize(1350, 800); 
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        Administrador.getInstancia().setActualizadorVisual(() -> actualizarInterfaz());

        inicializarComponentes();
    }

    private void inicializarComponentes() {
        // =========================================================
        // PANEL IZQUIERDO: CONTROLES
        // =========================================================
        JPanel panelIzquierdo = new JPanel(new BorderLayout(10, 10));
        panelIzquierdo.setPreferredSize(new Dimension(340, getHeight()));
        panelIzquierdo.setBorder(new EmptyBorder(10, 10, 10, 10));

        // --- Reloj ---
        JPanel panelReloj = new JPanel(new GridLayout(2, 1, 5, 5));
        panelReloj.setBackground(new Color(30, 30, 30));
        panelReloj.setBorder(new EmptyBorder(10, 10, 10, 10));
        JLabel lblTituloReloj = new JLabel("MISSION CLOCK", SwingConstants.CENTER);
        lblTituloReloj.setForeground(Color.GRAY);
        lblReloj = new JLabel("Ciclo: 0", SwingConstants.CENTER);
        lblReloj.setForeground(Color.GREEN);
        lblReloj.setFont(new Font("Consolas", Font.BOLD, 24));
        panelReloj.add(lblTituloReloj);
        panelReloj.add(lblReloj);

        // --- Controles y Políticas ---
        JPanel panelControles = new JPanel(new GridLayout(7, 1, 5, 8)); 
        panelControles.setBorder(BorderFactory.createTitledBorder("Control de Misión"));
        
        btnCargarCSV = new JButton("📁 Cargar Procesos (CSV)");
        
        JPanel panelPolitica = new JPanel(new BorderLayout(5, 0));
        comboPolitica = new JComboBox<>(new String[]{"FCFS", "Round Robin", "SJF", "SRTF", "HRRN"}); // Nota: ¿El PDF pide EDF o RMS?
        spinnerQuantum = new JSpinner(new SpinnerNumberModel(Config.QUANTUM_DEFAULT, 1, 50, 1));
        spinnerQuantum.setToolTipText("Quantum (Solo para RR)");
        panelPolitica.add(comboPolitica, BorderLayout.CENTER);
        panelPolitica.add(spinnerQuantum, BorderLayout.EAST);
        
        btnIniciar = new JButton("▶ Iniciar Simulación");
        btnIniciar.setBackground(new Color(0, 150, 0));
        btnIniciar.setForeground(Color.WHITE);
        btnPausar = new JButton("⏸ Pausar");
        btnPausar.setEnabled(false);
        btnReporte = new JButton("📊 Ver Reporte"); 
        
        panelControles.add(btnCargarCSV);
        panelControles.add(new JLabel("Política de Planificación | Quantum:"));
        panelControles.add(panelPolitica);
        panelControles.add(btnIniciar);
        panelControles.add(btnPausar);
        panelControles.add(btnReporte);

        // --- Creación Manual de Procesos ---
        JPanel panelCreacion = new JPanel(new GridLayout(8, 2, 5, 5));
        panelCreacion.setBorder(BorderFactory.createTitledBorder("Crear Proceso Manual"));
        
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
        
        panelCreacion.add(new JLabel("Periodo (0=Aperiódico):")); // NUEVO
        txtPeriodoProc = new JTextField("0");
        panelCreacion.add(txtPeriodoProc);
        
        panelCreacion.add(new JLabel("Tipo:"));
        chkEsSistema = new JCheckBox("Es de Sistema");
        panelCreacion.add(chkEsSistema);
        
        btnCrearManual = new JButton("Crear y Encolar");
        panelCreacion.add(new JLabel("")); 
        panelCreacion.add(btnCrearManual);

        btnCrearAleatorio = new JButton("Generar Proceso Aleatorio");

        JPanel contenedorControles = new JPanel(new BorderLayout());
        contenedorControles.add(panelControles, BorderLayout.NORTH);
        contenedorControles.add(panelCreacion, BorderLayout.CENTER);
        contenedorControles.add(btnCrearAleatorio, BorderLayout.SOUTH);

        panelIzquierdo.add(panelReloj, BorderLayout.NORTH);
        panelIzquierdo.add(contenedorControles, BorderLayout.CENTER);

        add(panelIzquierdo, BorderLayout.WEST);

        // =========================================================
        // PANEL CENTRAL: CPU, MEMORIAS Y COLAS (Igual que antes)
        // =========================================================
        JPanel panelCentro = new JPanel(new BorderLayout(10, 10));
        panelCentro.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel panelSuperior = new JPanel(new GridLayout(1, 2, 10, 10));
        
        // 1. CPU
        JPanel panelCPU = new JPanel(new BorderLayout(10, 10));
        panelCPU.setBorder(BorderFactory.createTitledBorder("Unidad Central de Procesamiento (CPU)"));
        JPanel infoCPU = new JPanel(new GridLayout(2, 1));
        infoCPU.setPreferredSize(new Dimension(150, 100));
        lblCpuId = new JLabel("LIBRE", SwingConstants.CENTER);
        lblCpuId.setFont(new Font("Arial", Font.BOLD, 24));
        lblCpuNombre = new JLabel("Ninguno", SwingConstants.CENTER);
        infoCPU.add(lblCpuId);
        infoCPU.add(lblCpuNombre);
        panelGrafica = new PanelGraficaCPU();
        panelGrafica.setPreferredSize(new Dimension(300, 100));
        panelCPU.add(infoCPU, BorderLayout.WEST);
        panelCPU.add(panelGrafica, BorderLayout.CENTER);
        
        // 2. Memorias (RAM y Swap)
        JPanel panelMemorias = new JPanel(new GridLayout(2, 1, 5, 5));
        panelMemorias.setBorder(BorderFactory.createTitledBorder("Memory Management & Swap"));
        barraMemoriaRAM = new JProgressBar(0, Config.MEMORIA_TOTAL);
        barraMemoriaRAM.setStringPainted(true);
        barraMemoriaRAM.setFont(new Font("Arial", Font.BOLD, 14));
        barraMemoriaSwap = new JProgressBar(0, Config.MEMORIA_TOTAL * 2); 
        barraMemoriaSwap.setStringPainted(true);
        barraMemoriaSwap.setFont(new Font("Arial", Font.BOLD, 14));
        barraMemoriaSwap.setForeground(new Color(128, 0, 128)); 
        panelMemorias.add(crearPanelConTituloPequeño("Main Memory (RAM)", barraMemoriaRAM));
        panelMemorias.add(crearPanelConTituloPequeño("Swap Space (Disk)", barraMemoriaSwap));

        panelSuperior.add(panelCPU);
        panelSuperior.add(panelMemorias);
        panelCentro.add(panelSuperior, BorderLayout.NORTH);

        // 3. Colas
        JPanel panelColas = new JPanel(new GridLayout(2, 2, 10, 10));
        panelColaListos = crearPanelCola("Ready Queue (RAM)");
        panelColaListosSuspendidos = crearPanelCola("Ready-Suspended (Swap Out)");
        panelColaBloqueados = crearPanelCola("Blocked Queue (RAM)");
        panelColaBloqueadosSuspendidos = crearPanelCola("Blocked-Suspended (Swap Out)");
        panelColas.add(new JScrollPane(panelColaListos, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));
        panelColas.add(new JScrollPane(panelColaListosSuspendidos, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));
        panelColas.add(new JScrollPane(panelColaBloqueados, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));
        panelColas.add(new JScrollPane(panelColaBloqueadosSuspendidos, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));

        panelCentro.add(panelColas, BorderLayout.CENTER);
        add(panelCentro, BorderLayout.CENTER);

        // =========================================================
        // EVENTOS
        // =========================================================

        btnCargarCSV.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Próximamente: Lógica para leer " + Config.RUTA_ARCHIVO_PROCESOS);
            // Aquí llamaremos al Utilidades.LectorCSV cuando lo hagamos
        });

        spinnerQuantum.addChangeListener(e -> {
            int q = (int) spinnerQuantum.getValue();
            // Administrador.getInstancia().setQuantum(q); // Descomentar cuando exista
            System.out.println("Quantum actualizado a: " + q);
        });

        btnIniciar.addActionListener(e -> {
            if (!simulacionIniciada) {
                Administrador.getInstancia().iniciarSimulacion(0); 
                Reloj.getInstancia().start();
                simulacionIniciada = true;
                btnIniciar.setEnabled(false);
                btnPausar.setEnabled(true);
            }
        });

        btnPausar.addActionListener(e -> {
            if (Reloj.getInstancia().isPausado()) {
                Reloj.getInstancia().reanudar();
                btnPausar.setText("⏸ Pausar");
                btnPausar.setForeground(Color.BLACK);
            } else {
                Reloj.getInstancia().pausar();
                btnPausar.setText("▶ Reanudar");
                btnPausar.setForeground(Color.RED);
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
                Proceso p = new Proceso(id, nombre, inst, deadline, prio, esSis, periodo);
                p.setEstado(Estado.NUEVO);
                
                Administrador.getInstancia().getColaListos().encolar(p);
                actualizarInterfaz();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Solo números en Instrucciones, Prioridad, Deadline y Periodo.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnCrearAleatorio.addActionListener(e -> {
            Proceso p = GeneradorProcesos.crearProcesoAleatorio();
            Administrador.getInstancia().getColaListos().encolar(p);
            actualizarInterfaz();
        });
        
        comboPolitica.addActionListener(e -> {
            String politica = (String) comboPolitica.getSelectedItem();
            // Administrador.getInstancia().setPolitica(politica); 
            System.out.println("Política: " + politica);
        });
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

    public void actualizarInterfaz() {
        SwingUtilities.invokeLater(() -> {
            Administrador admin = Administrador.getInstancia();

            if (lblReloj != null) lblReloj.setText("Ciclo: " + admin.getRelojSistema());

            Proceso pCpu = admin.getProcesoEnEjecucion();
            if (pCpu != null) {
                lblCpuId.setText(pCpu.getId());
                lblCpuNombre.setText(pCpu.getNombre());
                lblCpuId.setForeground(new Color(0, 150, 0)); 
                actualizarGraficaCPU(true);
            } else {
                lblCpuId.setText("LIBRE");
                lblCpuNombre.setText("Ninguno");
                lblCpuId.setForeground(Color.BLACK);
                actualizarGraficaCPU(false);
            }

            panelColaListos.removeAll();
            panelColaBloqueados.removeAll();
            panelColaListosSuspendidos.removeAll();
            panelColaBloqueadosSuspendidos.removeAll();
            
            int contadorProcesosRAM = (pCpu != null) ? 1 : 0;
            int contadorProcesosSwap = 0;

            Nodo<Proceso> actualPrio = admin.getColaListosPrioridad().getFrente();
            while (actualPrio != null) {
                agregarLabel(panelColaListos, actualPrio.getDato(), Color.ORANGE);
                contadorProcesosRAM++;
                actualPrio = actualPrio.getSiguiente();
            }
            
            Nodo<Proceso> actualListo = admin.getColaListos().getFrente();
            while (actualListo != null) {
                agregarLabel(panelColaListos, actualListo.getDato(), Color.GREEN);
                contadorProcesosRAM++;
                actualListo = actualListo.getSiguiente();
            }

            Nodo<Proceso> actualBloq = admin.getColaBloqueados().getFrente();
            while (actualBloq != null) {
                agregarLabel(panelColaBloqueados, actualBloq.getDato(), Color.RED);
                contadorProcesosRAM++;
                actualBloq = actualBloq.getSiguiente();
            }

            /* Descomentar cuando la lógica del Administrador tenga Swap
            Nodo<Proceso> actualListSus = admin.getColaListosSuspendidos().getFrente();
            while (actualListSus != null) {
                agregarLabel(panelColaListosSuspendidos, actualListSus.getDato(), Color.GRAY);
                contadorProcesosSwap++;
                actualListSus = actualListSus.getSiguiente();
            }

            Nodo<Proceso> actualBloqSus = admin.getColaBloqueadosSuspendidos().getFrente();
            while (actualBloqSus != null) {
                agregarLabel(panelColaBloqueadosSuspendidos, actualBloqSus.getDato(), new Color(139, 0, 0));
                contadorProcesosSwap++;
                actualBloqSus = actualBloqSus.getSiguiente();
            }
            */

            panelColaListos.revalidate(); panelColaListos.repaint();
            panelColaBloqueados.revalidate(); panelColaBloqueados.repaint();
            panelColaListosSuspendidos.revalidate(); panelColaListosSuspendidos.repaint();
            panelColaBloqueadosSuspendidos.revalidate(); panelColaBloqueadosSuspendidos.repaint();

            if (barraMemoriaRAM != null) {
                int usoRAM = Config.MEMORIA_RESERVADA_SO + (contadorProcesosRAM * Config.TAMANO_PROCESO);
                if (usoRAM > Config.MEMORIA_TOTAL) usoRAM = Config.MEMORIA_TOTAL;
                barraMemoriaRAM.setValue(usoRAM);
                barraMemoriaRAM.setString("Uso RAM: " + usoRAM + " MB / " + Config.MEMORIA_TOTAL + " MB");
                barraMemoriaRAM.setForeground(usoRAM > (Config.MEMORIA_TOTAL * 0.85) ? Color.RED : new Color(50, 150, 250));
            }
            
            if (barraMemoriaSwap != null) {
                int usoSwap = contadorProcesosSwap * Config.TAMANO_PROCESO;
                barraMemoriaSwap.setValue(usoSwap);
                barraMemoriaSwap.setString("Uso Swap: " + usoSwap + " MB");
            }
        });
    }

    private void agregarLabel(JPanel panel, Proceso p, Color color) {
        LabelRedondo lbl = new LabelRedondo(p, color);
        lbl.setPreferredSize(new Dimension(70, 70));
        panel.add(lbl);
    }

    public void actualizarGraficaCPU(boolean ocupado) {
        if (panelGrafica != null) {
            panelGrafica.agregarDato(ocupado ? 100 : 0);
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