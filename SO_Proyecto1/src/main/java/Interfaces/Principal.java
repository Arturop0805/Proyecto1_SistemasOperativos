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
import EstructurasDeDatos.Cola;
import EstructurasDeDatos.Nodo;
import Modelo.Proceso;
import Simulacion.Administrador;
import Simulacion.Reloj;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Ventana Principal del Simulador RTOS UNIMET-Sat.
 * Construida manualmente para soportar actualizaciones dinámicas.
 */
public class Principal extends JFrame {

    // Singleton para acceso global
    private static Principal instancia;

    // Componentes Dinámicos (Los que cambian con el tiempo)
    private JLabel lblReloj;
    private JLabel lblCpuId;
    private JLabel lblCpuNombre;
    private JProgressBar barraProgresoCPU;
    private JProgressBar barraMemoria;
    private JComboBox<String> comboPolitica;
    // Contenedores para las colas (Aquí agregaremos los LabelRedondo)
    private JPanel panelColaListos;
    private JPanel panelColaBloqueados;
    private JPanel panelColaSuspendidos; // Swap

    public Principal() {
        configurarVentana();
        inicializarComponentes();
        instancia = this; // Registrar instancia
    }

    public static Principal getInstancia() {
        return instancia;
    }

    private void configurarVentana() {
        setTitle("UNIMET-Sat RTOS Simulator - Mission Control");
        setSize(1200, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        
        // Estilo oscuro "Espacial" (Opcional, usa colores estándar si prefieres)
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void inicializarComponentes() {
        // --- 1. ENCABEZADO (Top) ---
        JPanel panelSuperior = new JPanel(new BorderLayout());
        panelSuperior.setBackground(new Color(30, 30, 30));
        panelSuperior.setBorder(new EmptyBorder(10, 20, 10, 20));

        JLabel titulo = new JLabel("UNIMET-Sat RTOS");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titulo.setForeground(Color.WHITE);
        titulo.setIcon(UIManager.getIcon("FileView.computerIcon")); // Icono genérico

        lblReloj = new JLabel("MISSION CLOCK: Cycle 0");
        lblReloj.setFont(new Font("Monospaced", Font.BOLD, 18));
        lblReloj.setForeground(Color.CYAN);

        panelSuperior.add(titulo, BorderLayout.WEST);
        panelSuperior.add(lblReloj, BorderLayout.EAST);
        add(panelSuperior, BorderLayout.NORTH);
        
        String[] politicas = {"FCFS", "Prioridad", "Round Robin", "EDF", "SRT"};
        comboPolitica = new JComboBox<>(politicas);
        comboPolitica.setFont(new Font("Segoe UI", Font.BOLD, 12));
        
        comboPolitica.addActionListener((ActionEvent e) -> {
        String seleccion = (String) comboPolitica.getSelectedItem();
        Administrador.getInstancia().cambiarPolitica(seleccion);
        });
                
        
            
        // --- 2. CONTENIDO CENTRAL (Dashboard) ---
        JPanel panelCentral = new JPanel(new GridLayout(1, 3, 10, 10)); // 3 Columnas
        panelCentral.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Columna 1: CPU y Estado
        JPanel col1 = crearPanelColumna("Unidad Central de Procesamiento (CPU)");
        
        JPanel panelCPU = new JPanel(new GridLayout(4, 1, 5, 5));
        panelCPU.setBorder(BorderFactory.createTitledBorder("Ejecutando Ahora"));
        
        lblCpuId = new JLabel("ID: [IDLE]", SwingConstants.CENTER);
        lblCpuId.setFont(new Font("Arial", Font.BOLD, 20));
        lblCpuId.setForeground(new Color(0, 128, 0));
        
        lblCpuNombre = new JLabel("Esperando procesos...", SwingConstants.CENTER);
        
        barraProgresoCPU = new JProgressBar(0, 100);
        barraProgresoCPU.setStringPainted(true);
        barraProgresoCPU.setString("CPU Libre");

        panelCPU.add(lblCpuId);
        panelCPU.add(lblCpuNombre);
        panelCPU.add(new JLabel("Progreso de Instrucción:"));
        panelCPU.add(barraProgresoCPU);
        
        col1.add(panelCPU, BorderLayout.NORTH);
        
        // Columna 2: Colas RAM (Listos y Bloqueados)
        JPanel col2 = crearPanelColumna("Gestión de Colas (RAM)");
        col2.setLayout(new GridLayout(2, 1, 0, 10)); // Dividido en 2 verticalmente

        // Panel Listos
        panelColaListos = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelColaListos.setBackground(Color.WHITE);
        JScrollPane scrollListos = new JScrollPane(panelColaListos);
        scrollListos.setBorder(BorderFactory.createTitledBorder("Cola de Listos (Ready Queue)"));
        
        // Panel Bloqueados
        panelColaBloqueados = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelColaBloqueados.setBackground(new Color(255, 240, 240)); // Rojizo suave
        JScrollPane scrollBloqueados = new JScrollPane(panelColaBloqueados);
        scrollBloqueados.setBorder(BorderFactory.createTitledBorder("Cola de Bloqueados (I/O Wait)"));

        col2.add(scrollListos);
        col2.add(scrollBloqueados);

        // Columna 3: Memoria y Swap
        JPanel col3 = crearPanelColumna("Gestión de Memoria & Swap");
        
        barraMemoria = new JProgressBar(0, 1024); // Asumiendo 1024MB total
        barraMemoria.setValue(0);
        barraMemoria.setStringPainted(true);
        barraMemoria.setForeground(new Color(100, 149, 237));
        
        JPanel panelMemInfo = new JPanel(new BorderLayout());
        panelMemInfo.setBorder(BorderFactory.createTitledBorder("Uso de Memoria RAM"));
        panelMemInfo.add(barraMemoria, BorderLayout.CENTER);
        
        // Panel Swap (Suspendidos)
        panelColaSuspendidos = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelColaSuspendidos.setBackground(new Color(240, 240, 240));
        JScrollPane scrollSwap = new JScrollPane(panelColaSuspendidos);
        scrollSwap.setBorder(BorderFactory.createTitledBorder("Disco / Swap (Suspendidos)"));

        col3.add(panelMemInfo, BorderLayout.NORTH);
        col3.add(scrollSwap, BorderLayout.CENTER);

        panelCentral.add(col1);
        panelCentral.add(col2);
        panelCentral.add(col3);
        add(panelCentral, BorderLayout.CENTER);

        // --- 3. CONTROLES (Bottom) ---
        JPanel panelInferior = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        panelInferior.setBackground(new Color(230, 230, 230));

        JButton btnIniciar = new JButton("INICIAR SIMULACIÓN");
        btnIniciar.setBackground(new Color(0, 100, 0));
        btnIniciar.setForeground(Color.WHITE);
        btnIniciar.setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        JButton btnPausar = new JButton("PAUSAR");
        
        JButton btnCargar = new JButton("Cargar JSON/CSV");

        // Evento Botón Iniciar
        btnIniciar.addActionListener((ActionEvent e) -> {
            System.out.println(">>> Iniciando desde GUI...");
            // Lógica para conectar con el backend
            Administrador.getInstancia().iniciarSimulacion(5); // Cargar 5 iniciales
            Reloj.getInstancia().start();
            btnIniciar.setEnabled(false);
        });

        // Evento Botón Pausar (NUEVO)
        btnPausar.addActionListener((ActionEvent e) -> {
            Reloj reloj = Reloj.getInstancia();
            if (reloj.isPausado()) {
                reloj.reanudar();
                btnPausar.setText("PAUSAR");
                btnPausar.setBackground(null);
            } else {
                reloj.pausar();
                btnPausar.setText("REANUDAR");
                btnPausar.setBackground(Color.ORANGE);
            }
        });

        panelInferior.add(btnCargar);
        panelInferior.add(btnIniciar);
        panelInferior.add(btnPausar);
        panelInferior.add(new JLabel("Planificador:"));
        panelInferior.add(comboPolitica);
        add(panelInferior, BorderLayout.SOUTH);
    }
    
    private JPanel crearPanelColumna(String titulo) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY), titulo, TitledBorder.CENTER, TitledBorder.TOP, new Font("Arial", Font.BOLD, 14)
        ));
        return p;
    }

    // --- MÉTODOS DE ACTUALIZACIÓN (Llamados por el Kernel) ---

    public void actualizarReloj(int ciclo) {
        // SwingUtilities asegura que esto corra en el hilo de la interfaz para no congelarla
        SwingUtilities.invokeLater(() -> {
            lblReloj.setText("MISSION CLOCK: Cycle " + ciclo);
        });
    }

    public void actualizarCPU(Proceso p) {
        SwingUtilities.invokeLater(() -> {
            if (p != null) {
                lblCpuId.setText(p.getId());
                lblCpuNombre.setText(p.getNombre());
                lblCpuId.setForeground(Color.RED); // Ocupado
                
                // Calculamos porcentaje completado
                int total = p.getInstruccionesRestantes() + p.getPC();
                int progreso = (int) ((p.getPC() / (double) total) * 100);
                barraProgresoCPU.setValue(progreso);
                barraProgresoCPU.setString("Ejecutando: " + p.getPC() + " / " + total);
            } else {
                lblCpuId.setText("IDLE");
                lblCpuNombre.setText("Esperando procesos...");
                lblCpuId.setForeground(new Color(0, 128, 0)); // Verde
                barraProgresoCPU.setValue(0);
                barraProgresoCPU.setString("CPU Libre");
            }
        });
    }
    
    /**
     * Redibuja completamente la cola de listos en la GUI.
     * Recorre tu estructura de datos 'Cola' manual.
     */
    public void actualizarColaListos(Cola cola) {
        SwingUtilities.invokeLater(() -> {
            panelColaListos.removeAll(); // Limpiar panel
            
            // Recorrer la cola manual (sin Iterator de Java)
            if (!cola.estaVacia()) {
                Nodo<Proceso> actual = cola.getFrente(); // Necesitas hacer público getFrente() en Cola.java
                while (actual != null) {
                    Proceso p = actual.dato;
                    
                    // Crear burbuja visual
                    Color colorBorde = p.esDeSistema() ? Color.RED : Color.BLUE;
                    LabelRedondo lbl = new LabelRedondo(p.getId(), colorBorde);
                    lbl.setToolTipText(p.getNombre() + " (Prio: " + p.getPrioridad() + ")");
                    lbl.setPreferredSize(new Dimension(60, 60));
                    
                    panelColaListos.add(lbl);
                    actual = actual.siguiente;
                }
            }
            
            panelColaListos.revalidate();
            panelColaListos.repaint();
        });
    }
    
    /**
     * Redibuja la cola de bloqueados en la GUI.
     */
    public void actualizarColaBloqueados(Cola cola) {
        SwingUtilities.invokeLater(() -> {
            panelColaBloqueados.removeAll(); // Limpiar panel
            
            if (!cola.estaVacia()) {
                Nodo<Proceso> actual = cola.getFrente();
                while (actual != null) {
                    Proceso p = actual.dato;
                    
                    // Burbuja Gris/Roja para indicar bloqueo
                    LabelRedondo lbl = new LabelRedondo(p.getId(), Color.MAGENTA);
                    lbl.setText(p.getId() + " (I/O)"); // Mostrar que espera I/O
                    lbl.setToolTipText("Esperando E/S...");
                    lbl.setPreferredSize(new Dimension(70, 60));
                    
                    panelColaBloqueados.add(lbl);
                    actual = actual.siguiente;
                }
            }
            
            panelColaBloqueados.revalidate();
            panelColaBloqueados.repaint();
        });
    }
    
    // Método Main para probar la interfaz SOLA (Test de Fuego Visual)
    public static void main(String[] args) {
        try {
            Principal ventana = new Principal();
            ventana.setVisible(true);
            
            // TEST VISUAL: Agregar unos procesos falsos para ver cómo se ven
            // (Esto simula lo que hará el Kernel luego)
            System.out.println("Probando componentes visuales...");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}