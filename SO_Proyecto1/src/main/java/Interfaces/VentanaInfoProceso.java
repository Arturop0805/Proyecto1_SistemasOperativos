/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Interfaces;

import Modelo.Proceso;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Ventana flotante que muestra el PCB (Process Control Block) en tiempo real.
 * (TU INTERFAZ ORIGINAL CON LOS ERRORES DE GETTERS SOLUCIONADOS)
 */
public class VentanaInfoProceso extends JFrame {

    private Proceso proceso;
    private Timer timerActualizacion;

    // Labels que cambiarán dinámicamente
    private JLabel lblEstado;
    private JLabel lblPrioridad;
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
        titulo.setFont(new Font("Arial", Font.BOLD, 16));
        panelHeader.add(titulo);
        add(panelHeader, BorderLayout.NORTH);

        // --- Panel Central de Datos ---
        JPanel panelCentro = new JPanel(new GridLayout(6, 2, 10, 10));
        panelCentro.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        lblEstado = new JLabel("-");
        lblPrioridad = new JLabel("-");
        lblPC = new JLabel("-");
        lblMAR = new JLabel("-");
        lblInstrucciones = new JLabel("-");
        lblBloqueo = new JLabel("-");
        barraProgreso = new JProgressBar();
        barraProgreso.setStringPainted(true);

        agregarDato(panelCentro, "Estado:", lblEstado, new Font("Arial", Font.BOLD, 14));
        agregarDato(panelCentro, "Prioridad:", lblPrioridad, new Font("Arial", Font.PLAIN, 14));
        agregarDato(panelCentro, "Program Counter (PC):", lblPC, new Font("Arial", Font.PLAIN, 14));
        agregarDato(panelCentro, "Memory Address (MAR):", lblMAR, new Font("Arial", Font.PLAIN, 14));
        agregarDato(panelCentro, "Instrucciones:", lblInstrucciones, new Font("Arial", Font.PLAIN, 14));
        agregarDato(panelCentro, "Ciclos en Bloqueo:", lblBloqueo, new Font("Arial", Font.PLAIN, 14));

        add(panelCentro, BorderLayout.CENTER);

        // --- Panel Inferior (Barra) ---
        JPanel panelSur = new JPanel(new BorderLayout());
        panelSur.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));
        panelSur.add(new JLabel("Progreso de Ejecución:"), BorderLayout.NORTH);
        panelSur.add(barraProgreso, BorderLayout.CENTER);
        add(panelSur, BorderLayout.SOUTH);

        // Timer para actualizar la ventana cada medio segundo
        timerActualizacion = new Timer(500, e -> actualizarDatos());
        timerActualizacion.start();

        // Detener el timer si el usuario cierra esta ventana
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                timerActualizacion.stop();
            }
        });

        actualizarDatos();
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
            lblPrioridad.setText(String.valueOf(proceso.getPrioridad()));
            lblPC.setText(String.valueOf(proceso.getPC()));   
            
            // CAMBIO 1: Como no hay getMAR(), usamos getPC() ya que avanzan a la par
            lblMAR.setText(String.valueOf(proceso.getPC())); 
            
            // CAMBIO 2: Calcular el total de instrucciones sumando PC + las restantes
            int pcActual = proceso.getPC();
            int instRestantes = proceso.getInstruccionesRestantes();
            int totalInst = pcActual + instRestantes;
            
            lblInstrucciones.setText(instRestantes + " / " + totalInst);
            
            // CAMBIO 3: Usar getTiempoTotalBloqueado en lugar del método viejo
            lblBloqueo.setText(String.valueOf(proceso.getTiempoTotalBloqueado()));
            
            // Configurar barra de progreso
            barraProgreso.setMaximum(totalInst);
            barraProgreso.setValue(pcActual);
            
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