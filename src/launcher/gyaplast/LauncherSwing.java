/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package launcher.gyaplast;

import java.awt.BorderLayout;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

/**
 *
 * @author TOULON
 */
public class LauncherSwing extends JFrame {

    private JLabel statusLabel;
    private JProgressBar progressBar;
    private JButton startButton;

    private String destinationDir = "app";
    private String appJar = "app/app-gyaplast-javafx.jar";
    private String javafxPath = "C:\\javafx-sdk-17.0.16\\lib";

    private String installedVersion = null;

    public LauncherSwing() {
        setTitle("Launcher Gyaplast");
        setSize(400, 150);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        statusLabel = new JLabel("Esperando...");
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        startButton = new JButton("Iniciar aplicación");
        startButton.setEnabled(false);

        startButton.addActionListener(e -> {
            try {
                updateStatus("Ejecutando aplicación...");
                ProcessBuilder pb = new ProcessBuilder(
                        "java",
                        "--module-path", javafxPath,
                        "--add-modules", "javafx.controls,javafx.fxml",
                        "-jar", appJar
                );
                pb.inheritIO();
                pb.start();
            } catch (Exception ex) {
                updateStatus("Error: " + ex.getMessage());
                ex.printStackTrace();
            } finally {
                this.dispose();
            }
        });

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        panel.add(statusLabel, BorderLayout.NORTH);
        panel.add(progressBar, BorderLayout.CENTER);
        panel.add(startButton, BorderLayout.SOUTH);

        add(panel);

        // Hilo para no congelar la UI
        new Thread(this::checkUpdates).start();
    }

    private void checkUpdates() {
        try {
            updateStatus("Buscando actualización...");
            boolean jarExists = new java.io.File(appJar).exists();
            
            if (jarExists) {
                installedVersion = getJarTxtVersion(appJar);
            }

            String latestVersion = GitHubUpdater.fetchLatestVersion();

            boolean shouldUpdate = !jarExists || GitHubUpdater.isNewerVersion(installedVersion, latestVersion);

            if (shouldUpdate) {
                updateStatus("Actualización disponible. Descargando nueva versión...");
                downloadAndUnzip(GitHubUpdater.getLatestDownloadUrl(), destinationDir);
                updateStatus("Actualización completada.");
            } else {
                updateStatus("La aplicación está actualizada.");
            }
            enableStartButton();
        } catch (Exception e) {
            updateStatus("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateStatus(String text) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(text));
    }

    private void updateProgress(int value) {
        SwingUtilities.invokeLater(() -> progressBar.setValue(value));
    }

    private void enableStartButton() {
        SwingUtilities.invokeLater(() -> startButton.setEnabled(true));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new LauncherSwing().setVisible(true);
        });
    }

    public String getJarManifestVersion(String jarPath) {
        try (JarFile jarFile = new JarFile(jarPath)) {
            Manifest manifest = jarFile.getManifest();
            if (manifest != null) {
                return manifest.getMainAttributes().getValue("Implementation-Version");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getJarTxtVersion(String jarPath) {
        try (JarFile jarFile = new JarFile(jarPath)) {
            JarEntry entry = jarFile.getJarEntry("Recursos/version.txt");
            if (entry != null) {
                try (InputStream in = jarFile.getInputStream(entry); BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                    return reader.readLine(); // primera línea
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void downloadAndUnzip(String fileURL, String destDir) throws Exception {

        Path tempZipPath = Files.createTempFile("update", ".zip");

        URL url = new URL(fileURL);
        try (InputStream in = url.openStream(); FileOutputStream out = new FileOutputStream(tempZipPath.toFile())) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            int totalRead = 0;
            int fileSize = url.openConnection().getContentLength();

            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                totalRead += bytesRead;
                int progress = (int) ((totalRead / (double) fileSize) * 100);
                updateProgress(progress);
            }
        }

        updateStatus("Descomprimiendo...");
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(tempZipPath.toFile()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File file = new File(destDir, entry.getName());
                if (entry.isDirectory()) {
                    file.mkdirs();
                } else {
                    file.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        byte[] buffer = new byte[4096];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
        }

        Files.delete(tempZipPath);
    }
}
