/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Interfaces;

import Modelo.Proceso;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Ventana flotante que muestra el PCB (Process Control Block) en tiempo real.
 */
public class VentanaInfoProceso extends JFrame {

    private Proceso proceso;
    private Timer timerActualizacion;

    // Labels que cambiarán dinámicamente
    private JLabel lblEstado;
    private JLabel lblPC;
    private JLabel lblMAR;
    private JLabel lblInstrucciones;
    private JLabel lblBloqueo;
    private JProgressBar barraProgreso;

    public VentanaInfoProceso(Proceso p) {
        this.proceso = p;
        
        setTitle("PCB: " + p.getNombre());
        setSize(350, 400);
        setLayout(new BorderLayout());
        setResizable(false);
        setLocationRelativeTo(null); // Centrar
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // --- Panel de Encabezado ---
        JPanel panelHeader = new JPanel();
        panelHeader.setBackground(new Color(50, 50, 50));
        panelHeader.setLayout(new FlowLayout(FlowLayout.CENTER));
        JLabel titulo = new JLabel("Proceso ID: " + p.getId());
        titulo.setForeground(Color.WHITE);
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        panelHeader.add(titulo);
        add(panelHeader, BorderLayout.NORTH);

        // --- Panel Central (Datos) ---
        JPanel panelDatos = new JPanel(new GridLayout(6, 2, 10, 10));
        panelDatos.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        // Inicializamos los labels
        lblEstado = new JLabel("...");
        lblPC = new JLabel("...");
        lblMAR = new JLabel("...");
        lblInstrucciones = new JLabel("...");
        lblBloqueo = new JLabel("0");
        
        // Estilo negrita para etiquetas fijas
        Font negrita = new Font("Segoe UI", Font.BOLD, 12);
        
        agregarDato(panelDatos, "Estado Actual:", lblEstado, negrita);
        agregarDato(panelDatos, "Program Counter (PC):", lblPC, negrita);
        agregarDato(panelDatos, "Memory Address (MAR):", lblMAR, negrita);
        agregarDato(panelDatos, "Instrucciones:", lblInstrucciones, negrita);
        agregarDato(panelDatos, "Ciclos Bloqueado:", lblBloqueo, negrita);
        
        // Barra de progreso del proceso individual
        barraProgreso = new JProgressBar(0, p.getTotalInstrucciones());
        barraProgreso.setStringPainted(true);
        panelDatos.add(new JLabel("Progreso:"));
        panelDatos.add(barraProgreso);

        add(panelDatos, BorderLayout.CENTER);

        // --- Timer de Actualización (El corazón del tiempo real) ---
        // Se ejecuta cada 100ms para refrescar la vista
        timerActualizacion = new Timer(10, e -> actualizarDatos());
        timerActualizacion.start();

        // Detener el timer cuando se cierre la ventana para ahorrar recursos
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                timerActualizacion.stop();
            }
        });
    }

    private void agregarDato(JPanel p, String titulo, JLabel valor, Font fuente) {
        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(fuente);
        p.add(lblTitulo);
        p.add(valor);
    }

    /**
     * Lee los datos frescos del objeto Proceso y actualiza la GUI
     */
    private void actualizarDatos() {
        if (proceso != null) {
            lblEstado.setText(proceso.getEstado().toString());
            lblPC.setText(String.valueOf(proceso.getPC()));   // Asegúrate de tener getPC() en Proceso
            lblMAR.setText(String.valueOf(proceso.getMAR())); // Asegúrate de tener getMAR() en Proceso
            lblInstrucciones.setText(proceso.getInstruccionesRestantes() + " / " + proceso.getTotalInstrucciones());
            
            // Si tienes un getter para ciclos de bloqueo, úsalo aquí
            // lblBloqueo.setText(String.valueOf(proceso.getCiclosBloqueoRestantes()));
            
            barraProgreso.setValue(proceso.getTotalInstrucciones() - proceso.getInstruccionesRestantes());
            
            // Cambiar color según estado
            switch (proceso.getEstado()) {
                case EJECUCION: lblEstado.setForeground(new Color(0, 150, 0)); break; // Verde
                case BLOQUEADO: lblEstado.setForeground(Color.RED); break;
                case TERMINADO: lblEstado.setForeground(Color.BLUE); break;
                default: lblEstado.setForeground(Color.BLACK);
            }
        }
    }
}
