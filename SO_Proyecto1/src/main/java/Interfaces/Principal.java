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
    private JPanel panelInfoCPU;
    private Proceso procesoEnCPU = null;
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
       panelInfoCPU = new JPanel(new GridLayout(4, 1, 5, 5)); 
       panelInfoCPU.setBorder(new TitledBorder("CPU - Ejecución"));

        // 2. Inicializamos los componentes (igual que antes)
        lblCpuId = new JLabel("ID: [Vacío]");
        lblCpuNombre = new JLabel("Proceso: [Ninguno]");
        barraProgresoCPU = new JProgressBar(0, 100);
        barraProgresoCPU.setStringPainted(true);
        barraProgresoCPU.setString("CPU Libre");

        // 3. Agregamos al panel
        panelInfoCPU.add(new JLabel("ID Proceso:"));
        panelInfoCPU.add(lblCpuId);
        panelInfoCPU.add(lblCpuNombre);
        panelInfoCPU.add(barraProgresoCPU);

        // 4. --- LÓGICA DEL CLICK (NUEVO) ---
        // Creamos el oyente (Listener) para detectar el click
        java.awt.event.MouseAdapter eventoClickCPU = new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                // Si hay un proceso en la variable, abrimos la ventana
                if (procesoEnCPU != null) {
                    Interfaces.VentanaInfoProceso ventana = new Interfaces.VentanaInfoProceso(procesoEnCPU);
                    ventana.setVisible(true);
                } else {
                    // Si no hay nada, mostramos un mensaje simple
                    JOptionPane.showMessageDialog(null, "La CPU está libre (IDLE).");
                }
            }
        };

        // Asignamos el evento a TODOS los elementos para asegurar que el click funcione
        // sin importar dónde toque el usuario (en el borde, en el texto o en la barra)
        panelInfoCPU.addMouseListener(eventoClickCPU);
        lblCpuId.addMouseListener(eventoClickCPU);
        lblCpuNombre.addMouseListener(eventoClickCPU);
        barraProgresoCPU.addMouseListener(eventoClickCPU);
        
        
        
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

        panelCentral.add(panelInfoCPU);
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
        JButton btnCrearProceso = new JButton("Nuevo Proceso");
        btnCrearProceso.addActionListener(e -> mostrarVentanaCreacion());
        
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
        panelInferior.add(btnCrearProceso);
        add(panelInferior, BorderLayout.SOUTH);
    }
    
    private JPanel crearPanelColumna(String titulo) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY), titulo, TitledBorder.CENTER, TitledBorder.TOP, new Font("Arial", Font.BOLD, 14)
        ));
        return p;
    }

    private void mostrarVentanaCreacion() {
        JDialog dialogo = new JDialog(this, "Crear Nuevo Proceso", true);
        dialogo.setSize(350, 400);
        dialogo.setLayout(new GridLayout(7, 2, 10, 10));
        dialogo.setLocationRelativeTo(this);

        // --- Componentes ---
        JTextField txtNombre = new JTextField();
        JSpinner spinInstrucciones = new JSpinner(new SpinnerNumberModel(10, 1, 500, 1));
        JSpinner spinPrioridad = new JSpinner(new SpinnerNumberModel(1, 1, 99, 1)); // Min 1 (Mayor prio), Max 99
        JCheckBox chkSistema = new JCheckBox("Es de Sistema");
        JSpinner spinPeriodo = new JSpinner(new SpinnerNumberModel(0, 0, 1000, 1)); // 0 = Aperiodico
        
        // Labels
        dialogo.add(new JLabel("  Nombre:"));
        dialogo.add(txtNombre);
        
        dialogo.add(new JLabel("  Instrucciones:"));
        dialogo.add(spinInstrucciones);
        
        dialogo.add(new JLabel("  Prioridad (1=Max):"));
        dialogo.add(spinPrioridad);
        
        dialogo.add(new JLabel("  Tipo:"));
        dialogo.add(chkSistema);
        
        dialogo.add(new JLabel("  Periodo (0=No):"));
        dialogo.add(spinPeriodo);
        
        // Botón Guardar
        JButton btnGuardar = new JButton("Crear");
        btnGuardar.addActionListener(e -> {
            // --- VALIDACIONES ---
            String nombre = txtNombre.getText().trim();
            if(nombre.isEmpty()) {
                JOptionPane.showMessageDialog(dialogo, "El nombre no puede estar vacío.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Los JSpinner ya limitan numéricamente, pero obtenemos los valores
            int instrucciones = (int) spinInstrucciones.getValue();
            int prioridad = (int) spinPrioridad.getValue();
            boolean esSistema = chkSistema.isSelected();
            int periodo = (int) spinPeriodo.getValue();
            
            // Validación extra de prioridad (por si acaso)
            if(prioridad <= 0) {
                 JOptionPane.showMessageDialog(dialogo, "La prioridad debe ser mayor a 0.", "Error", JOptionPane.ERROR_MESSAGE);
                 return;
            }

            // --- LLAMADA AL NUCLEO ---
            Administrador.getInstancia().agregarProcesoManual(nombre, instrucciones, prioridad, esSistema, periodo);
            
            dialogo.dispose(); // Cerrar ventana
        });

        // Botón Cancelar
        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.addActionListener(e -> dialogo.dispose());

        dialogo.add(btnGuardar);
        dialogo.add(btnCancelar);
        dialogo.setVisible(true);
    }
    // --- MÉTODOS DE ACTUALIZACIÓN (Llamados por el Kernel/Administrador) ---

    public void actualizarMemoria(int usada, int total) {
        SwingUtilities.invokeLater(() -> {
            barraMemoria.setValue(usada);
            barraMemoria.setString(usada + " MB / " + total + " MB");
            
            if (usada > total * 0.9) {
                barraMemoria.setForeground(Color.RED); // Alerta visual
            } else {
                barraMemoria.setForeground(new Color(100, 149, 237));
            }
        });
    }
    
    public void actualizarColaSuspendidos(Cola cola) {
        SwingUtilities.invokeLater(() -> {
            panelColaSuspendidos.removeAll();
            
            if (!cola.estaVacia()) {
                Nodo<Proceso> actual = cola.getFrente();
                while (actual != null) {
                    Proceso p = actual.dato;
                    
                    // Usamos color gris para indicar que está en disco/suspendido
                    LabelRedondo lbl = new LabelRedondo(p, Color.GRAY); 
                    lbl.setText(p.getId() + " (Swap)");
                    lbl.setToolTipText("Esperando memoria RAM...");
                    lbl.setPreferredSize(new Dimension(70, 60));
                    
                    panelColaSuspendidos.add(lbl);
                    actual = actual.siguiente;
                }
            }
            
            panelColaSuspendidos.revalidate();
            panelColaSuspendidos.repaint();
        });
    }
    
    public void actualizarReloj(int ciclo) {
        // SwingUtilities asegura que esto corra en el hilo de la interfaz para no congelarla
        SwingUtilities.invokeLater(() -> {
            lblReloj.setText("MISSION CLOCK: Cycle " + ciclo);
        });
    }

    public void actualizarCPU(Proceso p) {
    this.procesoEnCPU = p; // Guardamos referencia

    SwingUtilities.invokeLater(() -> {
        if (p != null) {
            lblCpuId.setText(p.getId());
            lblCpuNombre.setText(p.getNombre());
            
            barraProgresoCPU.setValue(p.getTotalInstrucciones() - p.getInstruccionesRestantes());
            barraProgresoCPU.setMaximum(p.getTotalInstrucciones());
            barraProgresoCPU.setString(p.getInstruccionesRestantes() + " instr. restantes");

            // AHORA SÍ FUNCIONA ESTA LÍNEA:
            if (panelInfoCPU != null) {
                panelInfoCPU.setCursor(new Cursor(Cursor.HAND_CURSOR));
                panelInfoCPU.setBackground(new Color(230, 240, 255)); // Un ligero azul para indicar actividad
            }

        } else {
            lblCpuId.setText("IDLE");
            lblCpuNombre.setText("Esperando...");
            barraProgresoCPU.setValue(0);
            barraProgresoCPU.setString("CPU Libre");

            if (panelInfoCPU != null) {
                panelInfoCPU.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                panelInfoCPU.setBackground(null); // Volver al color normal
            }
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
                    LabelRedondo lbl = new LabelRedondo(p, Color.BLUE);
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
                    LabelRedondo lbl = new LabelRedondo(p, Color.BLUE);
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