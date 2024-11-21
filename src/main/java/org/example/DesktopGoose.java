package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;
import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DesktopGoose extends JFrame implements NativeKeyListener {
    private static final int GOOSE_WIDTH = 50;
    private static final int GOOSE_HEIGHT = 50;
    private Point targetPosition;
    private Point currentPosition;
    private final Random random = new Random();
    private boolean isRunning = true;
    private final int SPEED = 5;
    private double angle = 0;
    private Robot robot;
    private boolean ctrlPressed = false;
    private boolean shiftPressed = false;

    public DesktopGoose() {
        try {
            robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }

        // Configurar JNativeHook
        try {
            // Desactivar el logging de JNativeHook
            Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
            logger.setLevel(Level.OFF);
            logger.setUseParentHandlers(false);

            GlobalScreen.registerNativeHook();
            GlobalScreen.addNativeKeyListener(this);
        } catch (NativeHookException ex) {
            System.err.println("Error al registrar JNativeHook");
            ex.printStackTrace();
            System.exit(1);
        }

        // Inicializar posiciones
        currentPosition = new Point(0, 0);
        targetPosition = MouseInfo.getPointerInfo().getLocation();

        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        setAlwaysOnTop(true);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(GOOSE_WIDTH, GOOSE_HEIGHT);

        // Panel con el pato
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                // Dibujar el pato
                g2d.setColor(Color.WHITE);

                // Cuerpo
                g2d.fillOval(10, 15, 30, 20);

                // Cabeza
                g2d.fillOval(30, 10, 15, 15);

                // Pico
                g2d.setColor(Color.ORANGE);
                int[] xPoints = {45, 50, 45};
                int[] yPoints = {15, 17, 19};
                g2d.fillPolygon(xPoints, yPoints, 3);
            }
        };
        panel.setOpaque(false);
        add(panel);

        // Thread para actualizar la posición del cursor
        new Thread(() -> {
            while (isRunning) {
                try {
                    Point mousePos = MouseInfo.getPointerInfo().getLocation();
                    targetPosition = mousePos;
                    Thread.sleep(16);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        // Thread para mover el ganso
        new Thread(() -> {
            while (isRunning) {
                moveTowardsTarget();
                try {
                    Thread.sleep(16);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void moveTowardsTarget() {
        if (targetPosition == null) return;

        double dx = targetPosition.x - currentPosition.x;
        double dy = targetPosition.y - currentPosition.y;
        double distance = Math.sqrt(dx*dx + dy*dy);

        angle += 0.1;

        if (distance > SPEED) {
            double ratio = SPEED / distance;
            double baseX = dx * ratio;
            double baseY = dy * ratio;

            double perpX = -baseY / distance * Math.sin(angle) * 2;
            double perpY = baseX / distance * Math.sin(angle) * 2;

            currentPosition.x += baseX + perpX;
            currentPosition.y += baseY + perpY;

            if (random.nextInt(10) == 0) {
                currentPosition.x += random.nextInt(3) - 1;
                currentPosition.y += random.nextInt(3) - 1;
            }

            SwingUtilities.invokeLater(() -> {
                setLocation(currentPosition.x, currentPosition.y);
            });
        }
    }

    // Implementación de NativeKeyListener
    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        if (e.getKeyCode() == NativeKeyEvent.VC_CONTROL) {
            ctrlPressed = true;
        }
        if (e.getKeyCode() == NativeKeyEvent.VC_SHIFT) {
            shiftPressed = true;
        }

        if (ctrlPressed && shiftPressed) {
            cleanupAndExit();
        }
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent e) {
        if (e.getKeyCode() == NativeKeyEvent.VC_CONTROL) {
            ctrlPressed = false;
        }
        if (e.getKeyCode() == NativeKeyEvent.VC_SHIFT) {
            shiftPressed = false;
        }
    }

    @Override
    public void nativeKeyTyped(NativeKeyEvent e) {
        // No necesitamos implementar esto
    }

    private void cleanupAndExit() {
        try {
            GlobalScreen.unregisterNativeHook();
        } catch (NativeHookException ex) {
            ex.printStackTrace();
        }
        isRunning = false;
        dispose();
        System.exit(0);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            DesktopGoose goose = new DesktopGoose();
            goose.setVisible(true);
        });
    }
}