package tagextractor;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class TagExtractor extends JFrame {
    private JLabel lblFile = new JLabel("No text file selected");
    private JButton btnSelectText = new JButton("Select Text File");
    private JButton btnSelectStop = new JButton("Select Stop Word File");
    private JButton btnExtract = new JButton("Extract Tags");
    private JButton btnSave = new JButton("Save Output");
    private JTextArea txtArea = new JTextArea(25, 60);
    private JFileChooser fileChooser = new JFileChooser();
    private File textFile = null;
    private File stopFile = null;
    private Map<String, Integer> tagMap = new TreeMap<>();

    public TagExtractor() {
        super("Lab 08 — Tag/Keyword Extractor");
        setupUI();
        setupActions();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
    }

    private void setupUI() {
        txtArea.setEditable(false);
        txtArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(txtArea,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(btnSelectText);
        top.add(btnSelectStop);
        top.add(btnExtract);
        top.add(btnSave);

        JPanel filePanel = new JPanel(new BorderLayout());
        filePanel.add(lblFile, BorderLayout.CENTER);

        Container c = getContentPane();
        c.setLayout(new BorderLayout(8, 8));
        c.add(top, BorderLayout.NORTH);
        c.add(filePanel, BorderLayout.CENTER);
        c.add(scroll, BorderLayout.SOUTH);
    }

    private void setupActions() {
        btnSelectText.addActionListener(e -> {
            fileChooser.setDialogTitle("Select text file to extract tags from");
            fileChooser.setFileFilter(new FileNameExtensionFilter("Text files", "txt", "text"));
            int rc = fileChooser.showOpenDialog(this);
            if (rc == JFileChooser.APPROVE_OPTION) {
                textFile = fileChooser.getSelectedFile();
                lblFile.setText("Text file: " + textFile.getName());
            }
        });

        btnSelectStop.addActionListener(e -> {
            fileChooser.setDialogTitle("Select stop words file (one word per line)");
            fileChooser.setFileFilter(new FileNameExtensionFilter("Text files", "txt", "text"));
            int rc = fileChooser.showOpenDialog(this);
            if (rc == JFileChooser.APPROVE_OPTION) {
                stopFile = fileChooser.getSelectedFile();
                JOptionPane.showMessageDialog(this, "Stop words file set: " + stopFile.getName());
            }
        });

        btnExtract.addActionListener(e -> {
            if (textFile == null) {
                JOptionPane.showMessageDialog(this, "Please select a text file first.");
                return;
            }
            if (stopFile == null) {
                // default to bundled stopwords if user hasn't chosen one
                stopFile = new File("stopwords/English Stop Words.txt");
                if (!stopFile.exists()) {
                    JOptionPane.showMessageDialog(this, "Stop words file not selected and bundled file not found.");
                    return;
                }
            }
            try {
                Set<String> stops = loadStopWords(stopFile.toPath());
                tagMap = extractTags(textFile.toPath(), stops);
                displayTags(textFile.getName(), tagMap);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error reading files: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        btnSave.addActionListener(e -> {
            if (tagMap == null || tagMap.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No tags to save. Run extraction first.");
                return;
            }
            fileChooser.setDialogTitle("Save extracted tags to...");
            fileChooser.setSelectedFile(new File("tags_output.txt"));
            int rc = fileChooser.showSaveDialog(this);
            if (rc == JFileChooser.APPROVE_OPTION) {
                File out = fileChooser.getSelectedFile();
                try (PrintWriter pw = new PrintWriter(new FileWriter(out))) {
                    pw.println("Extracting from: " + (textFile != null ? textFile.getName() : "unknown"));
                    pw.println();
                    pw.println("Tag\tFrequency");
                    // renamed variable from 'e' to 'entry' to fix conflict
                    for (Map.Entry<String, Integer> entry : tagMap.entrySet()) {
                        pw.println(entry.getKey() + "\t" + entry.getValue());
                    }
                    JOptionPane.showMessageDialog(this, "Saved to: " + out.getAbsolutePath());
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Error saving file: " + ex.getMessage());
                }
            }
        });
    }

    private Set<String> loadStopWords(Path stopPath) throws IOException {
        Set<String> stops = new TreeSet<>();
        try (BufferedReader br = Files.newBufferedReader(stopPath)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim().toLowerCase();
                if (!line.isEmpty()) stops.add(line);
            }
        }
        return stops;
    }

    private Map<String, Integer> extractTags(Path textPath, Set<String> stops) throws IOException {
        Map<String, Integer> map = new TreeMap<>();
        try (BufferedReader br = Files.newBufferedReader(textPath)) {
            String line;
            while ((line = br.readLine()) != null) {
                // replace non-letter characters with space, keep letters only
                line = line.replaceAll("[^A-Za-z]", " ").toLowerCase();
                String[] parts = line.split("\\s+");
                for (String w : parts) {
                    if (w.isEmpty()) continue;
                    if (stops.contains(w)) continue;
                    map.put(w, map.getOrDefault(w, 0) + 1);
                }
            }
        }
        return map;
    }

    private void displayTags(String fileName, Map<String, Integer> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("Extracting from: ").append(fileName).append("\n\n");
        sb.append(String.format("%-20s %s\n", "Tag", "Frequency"));
        sb.append(String.format("%-20s %s\n", "----", "---------"));
        for (Map.Entry<String, Integer> e : map.entrySet()) {
            sb.append(String.format("%-20s %d\n", e.getKey(), e.getValue()));
        }
        txtArea.setText(sb.toString());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            TagExtractor app = new TagExtractor();
            app.setVisible(true);
        });
    }
}
