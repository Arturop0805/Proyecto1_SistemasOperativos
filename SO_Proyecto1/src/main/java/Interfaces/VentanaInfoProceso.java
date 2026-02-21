/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Interfaces;


import Modelo.Proceso;
import Simulacion.Reloj;
import javax.swing.*;
import java.awt.*;

/**
 * Ventana flotante que muestra el PCB en tiempo real con Alerta de Deadline.
 */
public class VentanaInfoProceso extends JFrame {

    private Proceso proceso;
    private Timer timerActualizacion;

    // Labels dinámicos
    private JLabel lblEstado;
    private JLabel lblPrioridad;
    private JLabel lblPC;
    private JLabel lblInstrucciones;
    private JLabel lblBloqueo;
    private JLabel lblDeadline;
    private JLabel lblTiempoRestante;
    private JLabel lblAlertaDeadline;
    private JProgressBar barraProgreso;

    public VentanaInfoProceso(Proceso p) {
        this.proceso = p;
        
        setTitle("PCB: " + p.getNombre());
        setSize(380, 500);
        setLayout(new BorderLayout());
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // --- Panel de Encabezado ---
        JPanel panelHeader = new JPanel();
        panelHeader.setBackground(new Color(50, 50, 50));
        panelHeader.setLayout(new BoxLayout(panelHeader, BoxLayout.Y_AXIS));
        
        JLabel titulo = new JLabel("Proceso ID: " + p.getId());
        titulo.setForeground(Color.WHITE);
        titulo.setFont(new Font("Arial", Font.BOLD, 16));
        titulo.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        lblAlertaDeadline = new JLabel("EVALUANDO DEADLINE...");
        lblAlertaDeadline.setFont(new Font("Arial", Font.BOLD, 14));
        lblAlertaDeadline.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        panelHeader.add(Box.createVerticalStrut(10));
        panelHeader.add(titulo);
        panelHeader.add(Box.createVerticalStrut(5));
        panelHeader.add(lblAlertaDeadline);
        panelHeader.add(Box.createVerticalStrut(10));

        add(panelHeader, BorderLayout.NORTH);

        // --- Panel de Información ---
        JPanel panelInfo = new JPanel(new GridLayout(7, 2, 10, 10));
        panelInfo.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        panelInfo.add(new JLabel("Estado Actual:"));
        lblEstado = new JLabel();
        lblEstado.setFont(new Font("Arial", Font.BOLD, 12));
        panelInfo.add(lblEstado);

        panelInfo.add(new JLabel("Prioridad:"));
        lblPrioridad = new JLabel();
        panelInfo.add(lblPrioridad);

        panelInfo.add(new JLabel("Program Counter (PC):"));
        lblPC = new JLabel();
        panelInfo.add(lblPC);

        panelInfo.add(new JLabel("Instrucciones Ejecutadas:"));
        lblInstrucciones = new JLabel();
        panelInfo.add(lblInstrucciones);

        panelInfo.add(new JLabel("Ciclos Bloqueado:"));
        lblBloqueo = new JLabel();
        panelInfo.add(lblBloqueo);

        panelInfo.add(new JLabel("Deadline (Límite Máx):"));
        lblDeadline = new JLabel();
        panelInfo.add(lblDeadline);

        panelInfo.add(new JLabel("Tiempo para Vencer:"));
        lblTiempoRestante = new JLabel();
        lblTiempoRestante.setFont(new Font("Arial", Font.BOLD, 12));
        panelInfo.add(lblTiempoRestante);

        add(panelInfo, BorderLayout.CENTER);

        // --- Panel Inferior (Barra de progreso) ---
        JPanel panelInferior = new JPanel(new BorderLayout());
        panelInferior.setBorder(BorderFactory.createEmptyBorder(10, 15, 15, 15));
        panelInferior.add(new JLabel("Progreso de Ejecución:"), BorderLayout.NORTH);
        
        barraProgreso = new JProgressBar();
        barraProgreso.setStringPainted(true);
        panelInferior.add(barraProgreso, BorderLayout.CENTER);

        add(panelInferior, BorderLayout.SOUTH);

        // Timer de actualización (cada 200ms)
        timerActualizacion = new Timer(200, e -> actualizarDatos());
        timerActualizacion.start();
        
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent windowEvent) {
                timerActualizacion.stop();
            }
        });

        actualizarDatos();
    }

    private void actualizarDatos() {
        if (proceso != null) {
            // Mostrar estado simple y luego con color
            lblEstado.setText("ESTADO: " + proceso.getEstado().name());
            lblPrioridad.setText(String.valueOf(proceso.getPrioridad()));
            lblPC.setText(String.valueOf(proceso.getPC()));   
            
            int ejecutadas = proceso.getInstruccionesEjecutadas();
            int totalInst = proceso.getTotalInstrucciones();
            int restantes = proceso.getInstruccionesRestantes();
            
            lblInstrucciones.setText(ejecutadas + " / " + totalInst + " (restantes: " + restantes + ")");
            lblBloqueo.setText(String.valueOf(proceso.getTiempoTotalBloqueado()));
            
            barraProgreso.setMaximum(Math.max(1, totalInst));
            barraProgreso.setValue(Math.min(ejecutadas, totalInst));

            // --- LÓGICA DE DEADLINE ---
            int relojActual = Reloj.getInstancia().getCicloActual();
            int deadlineAbsoluto = proceso.getDeadlineAbsoluto(); 
            lblDeadline.setText(String.valueOf(deadlineAbsoluto));

            // Si fue terminado y además fue abortado por deadline, forzamos mensaje de fallo
            if (proceso.getEstado() == Modelo.Estado.TERMINADO) {

                if (proceso.isAbortadoPorDeadline()) {
                    lblAlertaDeadline.setText("FALLÓ: ABORTADO POR DEADLINE");
                    lblAlertaDeadline.setForeground(Color.RED);
                    lblTiempoRestante.setText("Vencido por " + (proceso.getTiempoFinalizacion() - deadlineAbsoluto));
                    lblTiempoRestante.setForeground(Color.RED);
                } else {
                    if (proceso.getTiempoFinalizacion() <= deadlineAbsoluto) {
                        lblAlertaDeadline.setText("ÉXITO: CUMPLIÓ DEADLINE");
                        lblAlertaDeadline.setForeground(Color.GREEN);
                        lblTiempoRestante.setText("Sobró tiempo");
                        lblTiempoRestante.setForeground(Color.GREEN);
                    } else {
                        lblAlertaDeadline.setText("FALLÓ: SUPERÓ DEADLINE");
                        lblAlertaDeadline.setForeground(Color.RED);
                        lblTiempoRestante.setText("Vencido por " + (proceso.getTiempoFinalizacion() - deadlineAbsoluto));
                        lblTiempoRestante.setForeground(Color.RED);
                    }
                }
            } else {
                // Proceso todavía vivo
                int tiempoParaVencer = deadlineAbsoluto - relojActual;
                lblTiempoRestante.setText(String.valueOf(tiempoParaVencer));
                
                if (relojActual > deadlineAbsoluto) {
                    lblAlertaDeadline.setText("¡ALERTA! DEADLINE VENCIDO");
                    lblAlertaDeadline.setForeground(Color.RED);
                    lblTiempoRestante.setForeground(Color.RED);
                } else {
                    lblAlertaDeadline.setText("ESTADO: EN TIEMPO");
                    lblAlertaDeadline.setForeground(new Color(150, 255, 150));
                    lblTiempoRestante.setForeground(Color.BLACK);
                }
            }
            
            // Color de estado
            switch (proceso.getEstado()) {
                case EJECUCION: lblEstado.setForeground(new Color(0, 150, 0)); break;
                case LISTO: lblEstado.setForeground(new Color(200, 150, 0)); break;
                case BLOQUEADO: lblEstado.setForeground(Color.RED); break;
                case BLOQUEADO_SUSPENDIDO: lblEstado.setForeground(new Color(150, 0, 0)); break;
                case LISTO_SUSPENDIDO: lblEstado.setForeground(new Color(200, 100, 0)); break;
                case TERMINADO: lblEstado.setForeground(Color.BLUE); break;
                default: lblEstado.setForeground(Color.BLACK);
            }
        }
    }
}
