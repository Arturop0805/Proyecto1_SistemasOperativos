/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ComponentesVisuales;

import Interfaces.VentanaInfoProceso; // Importamos la nueva ventana
import Modelo.Proceso;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class LabelRedondo extends JLabel {

    private int radio = 15;
    private Color colorBorde;
    private Proceso procesoAsociado; // Referencia al objeto real

    // --- Constructor Modificado ---
    // Ahora recibe el OBJETO Proceso, no solo el String
    public LabelRedondo(Proceso proceso, Color colorBorde) {
        super(proceso.getId()); // Texto inicial
        this.procesoAsociado = proceso;
        this.colorBorde = colorBorde;
        
        setOpaque(false);
        setHorizontalAlignment(CENTER);
        
        // Cursor de mano para indicar que es clickeable
        setCursor(new Cursor(Cursor.HAND_CURSOR));

        // --- Evento de Click ---
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Al hacer click, abrimos la ventana de detalles
                if (procesoAsociado != null) {
                    VentanaInfoProceso detalles = new VentanaInfoProceso(procesoAsociado);
                    detalles.setVisible(true);
                }
            }
        });
    }

    // Constructor vacío (si lo requiere Netbeans)
    public LabelRedondo() {
        super("?");
        this.colorBorde = Color.GRAY;
    }

    @Override
    protected void paintComponent(Graphics g) {
        // ... (Tu código de dibujo existente se queda igual) ...
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, radio, radio);
        g2.setColor(colorBorde);
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, radio, radio);
        super.paintComponent(g);
    }
}
