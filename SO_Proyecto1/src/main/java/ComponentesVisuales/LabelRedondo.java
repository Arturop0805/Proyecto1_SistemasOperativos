/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ComponentesVisuales;

import javax.swing.*;
import java.awt.*;

public class LabelRedondo extends JLabel {

    private int radio = 15; // Qué tan redondeado lo quieres
    private Color colorBorde = Color.BLACK;
    private int grosorBorde = 2;

   
    public LabelRedondo(String texto, Color colorBorde) {
        super(texto);
        this.colorBorde = colorBorde;
        setOpaque(false);
        setHorizontalAlignment(CENTER);
    }
    
    // Constructor vacío requerido por NetBeans (JavaBeans)
    public LabelRedondo() {
        super("Texto"); // Texto por defecto
        this.colorBorde = Color.BLACK; // Color por defecto
        setOpaque(false);
        setHorizontalAlignment(CENTER);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        
        // Suavizar bordes (Antialiasing) para que no se vea pixelado
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 1. Pintar el Fondo
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, radio, radio);

        // 2. Pintar el Texto (Llama al método original)
        super.paintComponent(g);
        
        // 3. Pintar el Borde
        g2.setColor(colorBorde);
        g2.setStroke(new BasicStroke(grosorBorde));
        g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, radio, radio);
    }
}
