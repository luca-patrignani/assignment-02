package com.example;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;

class ImageDisplayFrame extends JFrame {

    private JScrollPane scrollPane;

    public ImageDisplayFrame() {
        setTitle("Image Viewer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public void refreshImage(BufferedImage image) {
        if (scrollPane != null) {
            remove(scrollPane);
        }
        try {
            JLabel label = new JLabel(new ImageIcon(image));
            scrollPane = new JScrollPane(label);
            add(scrollPane);

            setSize(Math.min(image.getWidth(), 800), Math.min(image.getHeight(), 600));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to load image.");
            System.exit(1);
        }

    }
}
