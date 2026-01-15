package org.example;

import com.fazecast.jSerialComm.SerialPort;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Scanner;

public class SmartGardenUI {
    private SerialPort comPort;
    private DatabaseManager db = new DatabaseManager();
    private JComboBox<Plant> plantBox;
    private JLabel statusLabel;
    private JLabel sensorLabel;

    public SmartGardenUI(SerialPort port) {
        this.comPort = port;
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) {}
        createGUI();
        startSerialReader();
    }

    private void createGUI() {
        JFrame frame = new JFrame("Inteligentny Ogród v2.5");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 600);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Wybór rośliny
        plantBox = new JComboBox<>();
        refreshPlantList();

        JButton btnApply = new JButton("Zastosuj próg wilgotności");
        btnApply.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Panel zarządzania bazą
        JPanel managePanel = new JPanel(new GridLayout(1, 2, 10, 0));
        JButton btnAdd = new JButton("Dodaj roślinę");
        JButton btnDelete = new JButton("Usuń wybraną");
        managePanel.add(btnAdd);
        managePanel.add(btnDelete);

        // Panel sterowania ręcznego
        JPanel controlPanel = new JPanel(new GridLayout(1, 3, 5, 0));
        JButton btnOn = new JButton("WŁ POMPĘ");
        JButton btnOff = new JButton("WYŁ POMPĘ");
        JButton btnAuto = new JButton("TRYB AUTO");
        btnOn.setBackground(new Color(144, 238, 144));
        btnOff.setBackground(new Color(255, 182, 193));
        btnAuto.setBackground(new Color(173, 216, 230));
        controlPanel.add(btnOn);
        controlPanel.add(btnOff);
        controlPanel.add(btnAuto);

        // Monitoring
        statusLabel = new JLabel("Status: Oczekiwanie...", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 12));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        sensorLabel = new JLabel("<html><center>Gleba: --%<br>Powietrze: --°C</center></html>", SwingConstants.CENTER);
        sensorLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        sensorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Akcje
        btnApply.addActionListener(e -> {
            Plant selected = (Plant) plantBox.getSelectedItem();
            if (selected != null) sendCommand("SET_HUM:" + selected.getThreshold());
        });

        btnAdd.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(frame, "Nazwa rośliny:");
            String hum = JOptionPane.showInputDialog(frame, "Próg wilgotności (0-100):");
            if (name != null && hum != null) {
                try {
                    db.addPlant(name, Integer.parseInt(hum.trim()));
                    refreshPlantList();
                } catch (Exception ex) { JOptionPane.showMessageDialog(frame, "Błędne dane!"); }
            }
        });

        btnDelete.addActionListener(e -> {
            Plant selected = (Plant) plantBox.getSelectedItem();
            if (selected != null) {
                db.deletePlant(selected.getName());
                refreshPlantList();
            }
        });

        btnOn.addActionListener(e -> sendCommand("MODE_ON"));
        btnOff.addActionListener(e -> sendCommand("MODE_OFF"));
        btnAuto.addActionListener(e -> sendCommand("MODE_AUTO"));

        // Składanie UI
        mainPanel.add(new JLabel("1. Profil rośliny:"));
        mainPanel.add(plantBox);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        mainPanel.add(btnApply);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        mainPanel.add(new JLabel("2. Zarządzanie bazą:"));
        mainPanel.add(managePanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        mainPanel.add(new JLabel("3. Sterowanie ręczne:"));
        mainPanel.add(controlPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 30)));
        mainPanel.add(statusLabel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        mainPanel.add(sensorLabel);

        frame.add(mainPanel);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void sendCommand(String cmd) {
        String fullCmd = cmd + "\n";
        comPort.writeBytes(fullCmd.getBytes(), fullCmd.length());
    }

    private void refreshPlantList() {
        plantBox.removeAllItems();
        for (Plant p : db.getPlantsFromDb()) plantBox.addItem(p);
    }

    private void startSerialReader() {
        new Thread(() -> {
            comPort.flushIOBuffers();
            try (Scanner sc = new Scanner(comPort.getInputStream())) {
                while (sc.hasNextLine()) {
                    String line = sc.nextLine().trim();
                    if (line.startsWith("DATA|")) {
                        String[] p = line.split("\\|");
                        if (p.length >= 7) {
                            SwingUtilities.invokeLater(() -> sensorLabel.setText(
                                    String.format("<html><center>Wilgotność gleby: <b>%s%%</b> (Próg: %s%%)<br>Powietrze: %s°C / %s%%<br>Pompa: <b>%s</b><br>Tryb: %s</center></html>",
                                            p[3], p[4], p[1], p[2], p[5], p[6])
                            ));
                        }
                    } else if (line.contains("CONFIRM|")) {
                        SwingUtilities.invokeLater(() -> statusLabel.setText("Ostatnia akcja: " + line.replace("CONFIRM|", "")));
                    }
                }
            }
        }).start();
    }

    public static void main(String[] args) {
        SerialPort port = SerialPort.getCommPort("COM5"); // Zmień na swój port
        port.setBaudRate(9600);
        port.setComPortTimeouts(SerialPort.TIMEOUT_SCANNER, 0, 0);

        if (port.openPort()) {
            port.setDTR();
            port.setRTS();
            try { Thread.sleep(2000); } catch (Exception e) {}
            new SmartGardenUI(port);
        } else {
            JOptionPane.showMessageDialog(null, "Nie można otworzyć portu COM5");
        }
    }
}