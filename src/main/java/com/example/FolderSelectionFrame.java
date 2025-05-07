package com.example;

import com.example.rx.RxProjectDependencyAnalyzer;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import static guru.nidi.graphviz.model.Factory.*;
import java.nio.file.Path;

class FolderSelectionFrame extends JFrame {

    private final ImageDisplayFrame imageDisplayFrame = new ImageDisplayFrame();
    private final MutableGraph g = mutGraph().setDirected(true);

    public FolderSelectionFrame() {
        setTitle("Select Folder");
        setSize(400, 150);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JButton selectButton = new JButton("Select Folder");
        selectButton.addActionListener(e -> selectFolder());

        JPanel panel = new JPanel();
        panel.add(selectButton);

        add(panel);
        setVisible(true);
    }

    private void selectFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int option = chooser.showOpenDialog(this);

        if (option == JFileChooser.APPROVE_OPTION) {
            Path directoryPath = Path.of(chooser.getSelectedFile().getAbsolutePath());
            final var pda = new RxProjectDependencyAnalyzer(directoryPath);
            try {
                pda.getPackageDependencies(directoryPath)
                        .map(this::regenerateGraph)
                        .subscribe(imageDisplayFrame::refreshImage);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage());
            }
        }
    }

    private File findFirstImage(File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File f : files) {
                String name = f.getName().toLowerCase();
                if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")) {
                    return f;
                }
            }
        }
        return null;
    }

    private BufferedImage regenerateGraph(DepsReport depsReport) {
        for (final var dependency: depsReport.dependencies()) {
            g.add(mutNode(depsReport.name())).addLink(mutNode(dependency));
        }
        return Graphviz.fromGraph(g).render(Format.PNG).toImage();
    }
}
