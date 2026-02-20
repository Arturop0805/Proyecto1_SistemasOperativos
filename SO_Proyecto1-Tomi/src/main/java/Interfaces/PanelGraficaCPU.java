/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Interfaces;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;

/**
 *
 * @author Artur
 */
class PanelGraficaCPU extends JPanel {
        private java.util.List<Integer> historialUso;
        private int maxPuntos = 100; // Cuántos puntos caben en el ancho de la gráfica

        public PanelGraficaCPU() {
            historialUso = new java.util.ArrayList<>();
            setBackground(Color.BLACK);
            // Pre-llenar con ceros para que empiece limpia
            for (int i = 0; i < maxPuntos; i++) {
                historialUso.add(0);
            }
        }

        public void agregarDato(int porcentaje) {
            historialUso.add(porcentaje);
            if (historialUso.size() > maxPuntos) {
                historialUso.remove(0); // Eliminar el dato más viejo para crear efecto scroll
            }
            repaint(); // Solicitar redibujado
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int pasoX = w / (maxPuntos - 1); // Distancia entre puntos

            // 1. Dibujar Rejilla (Grid)
            g2.setColor(new Color(50, 50, 50));
            for (int i = 0; i < h; i += h / 4) {
                g2.drawLine(0, i, w, i);
            }

            // 2. Dibujar Gráfica de Uso (Verde Matrix)
            g2.setColor(Color.GREEN);
            g2.setStroke(new BasicStroke(2f));

            for (int i = 0; i < historialUso.size() - 1; i++) {
                int valor1 = historialUso.get(i);
                int valor2 = historialUso.get(i + 1);

                // Convertir porcentaje (0-100) a coordenadas Y (invertido porque Y crece hacia abajo)
                int y1 = h - (int) ((valor1 / 100.0) * h);
                int y2 = h - (int) ((valor2 / 100.0) * h);

                int x1 = i * pasoX;
                int x2 = (i + 1) * pasoX;

                g2.drawLine(x1, y1, x2, y2);
            }

            // 3. Mostrar Valor Actual
            g2.setColor(Color.WHITE);
            int valorActual = historialUso.get(historialUso.size() - 1);
            g2.drawString("CPU: " + valorActual + "%", 5, 15);
        }
    }
