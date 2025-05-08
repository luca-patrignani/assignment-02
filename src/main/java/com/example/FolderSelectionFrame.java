package com.example;

import com.example.rx.RxDependencyAnalyzer;
import guru.nidi.graphviz.attribute.Rank;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.engine.Renderer;
import guru.nidi.graphviz.model.Factory;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.Node;
import io.reactivex.rxjava3.core.Flowable;

import javax.swing.*;
import java.awt.*;

import static guru.nidi.graphviz.attribute.Attributes.attr;
import static guru.nidi.graphviz.attribute.Attributes.attrs;
import static guru.nidi.graphviz.attribute.Rank.RankDir.TOP_TO_BOTTOM;
import static guru.nidi.graphviz.model.Factory.*;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

class FolderSelectionFrame extends JFrame {
    private final JLabel graphLabel = new JLabel();
    private final List<DepsReport> graphData = new ArrayList<>();
    private final Map<String, MutableGraph> packageCache = new HashMap<>();
    private final Map<String, Node> nodeCache = new HashMap<>();
    private final MutableGraph rootGraph = mutGraph()
            .setDirected(true)
            .graphAttrs().add(Rank.dir(TOP_TO_BOTTOM))
            .graphAttrs().add("ranksep", "0.5");

    public FolderSelectionFrame() {
        super("Dependency Analyzer");
        setLayout(new BorderLayout());
        setSize(400, 150);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JButton selectButton = new JButton("Select Folder");
        selectButton.addActionListener(e -> selectFolder());

        add(selectButton, BorderLayout.NORTH);
        add(new JScrollPane(graphLabel), BorderLayout.CENTER);
        setVisible(true);
    }

    private void selectFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int option = chooser.showOpenDialog(this);

        if (option == JFileChooser.APPROVE_OPTION) {
            Path directoryPath = Path.of(chooser.getSelectedFile().getAbsolutePath());
            final var pda = new RxDependencyAnalyzer(directoryPath);

            subscribeToReports(pda.getProjectDependencies());

        }
    }

    private void subscribeToReports(Flowable<DepsReport> reports) {
        graphData.clear();
        nodeCache.clear();

        reports.subscribe(
                depsReport -> {
                    Thread.sleep(2000);
                    addNodeToGraph(depsReport);
                    updateGraphImage();
                    },
               error -> SwingUtilities.invokeLater(() ->
                       JOptionPane.showMessageDialog(this, "Errore: " + error.getMessage()))
        );
    }


    private synchronized void addNodeToGraph(DepsReport report) {
        graphData.add(report);
        var name = report.name();
        var pkg = getPackage(name);
        var dep = report.dependencies();

        var cluster = packageCache.computeIfAbsent(pkg, p -> {
            MutableGraph cl = mutGraph("cluster_" + p)
                    .setCluster(true)
                    .setDirected(true)
                    .graphAttrs().add(attrs(
                            attr("style", "filled"),
                            attr("fillcolor", "lightgrey"),
                            attr("color", "black"),
                            attr("label", p)
                    ));
            rootGraph.add(cl);
            return cl;
        });

        Node from = nodeCache.computeIfAbsent(name, Factory::node);

        cluster.add(from);

        for (String d : dep) {

            Node to = nodeCache.computeIfAbsent(d, Factory::node);
            MutableGraph targetCluster = packageCache.computeIfAbsent(getPackage(d), p -> {
                MutableGraph cl = mutGraph("cluster_" + p)
                        .setCluster(true)
                        .setDirected(true)
                        .graphAttrs().add(attrs(
                                attr("style", "filled"),
                                attr("fillcolor", "lightgrey"),
                                attr("color", "black"),
                                attr("label", p)
                        ));
                rootGraph.add(cl);
                return cl;
            });
            targetCluster.add(to);

            rootGraph.add(from.link(to));

        }
    }

    private void updateGraphImage() {
        SwingUtilities.invokeLater(() -> {
            try {
                Renderer gv = Graphviz.fromGraph(rootGraph.graphAttrs().add(attr("dpi",100))).render(Format.PNG);
                ImageIcon icon = new ImageIcon(gv.toImage());
                graphLabel.setIcon(icon);
                graphLabel.revalidate();
                setSize(Math.max(icon.getIconWidth(),800),Math.max(icon.getIconHeight(), 600));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    private String getPackage(String fqn) {

        // Split the FQN by dots
        String[] segments = fqn.split("\\.");
        var result = "";
        // Iterate over the segments in reverse order
        for (int i = 0; i <=segments.length - 1; i++) {
            // Return the first segment that doesn't start with an uppercase letter
            if (!Character.isUpperCase(segments[i].charAt(0))) {
                result = result.concat(segments[i]+".");
            } else {
                return result.substring(0,result.length()-1);
            }
        }

        // If all segments start with an uppercase letter, return default package
        return fqn;
    }
}
