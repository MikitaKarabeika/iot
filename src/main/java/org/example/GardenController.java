package org.example;

import com.fazecast.jSerialComm.SerialPort;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Scanner;

@RestController
@RequestMapping("/api")
@CrossOrigin("*")
public class GardenController {

    private final PlantRepository repository;
    private SerialPort comPort;
    private String lastData = "Czekam na dane z Arduino...";

    public GardenController(PlantRepository repository) {
        this.repository = repository;
        initSerial();
    }

    // Внутри GardenController.java измени метод initSerial:

    private void initSerial() {
        comPort = SerialPort.getCommPort("COM5"); // Sprawdź czy to nadal COM5
        comPort.setBaudRate(9600);

        // Ustawiamy timeouty, żeby Java nie czekała w nieskończoność na jedną linię
        comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 100, 0);

        if (comPort.openPort()) {
            System.out.println("Port otwarty. Czekam na dane...");

            new Thread(() -> {
                // Używamy BufferedReader zamiast Scannera dla lepszej stabilności
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(comPort.getInputStream()))) {
                    while (true) {
                        if (reader.ready()) {
                            String line = reader.readLine();
                            if (line != null && line.startsWith("DATA|")) {
                                lastData = line.trim();
                                // Opcjonalnie: System.out.println("Odebrano: " + lastData);
                            }
                        }
                        Thread.sleep(50); // Mała pauza, żeby nie obciążać procesora
                    }
                } catch (Exception e) {
                    System.err.println("Błąd odczytu: " + e.getMessage());
                }
            }).start();
        } else {
            lastData = "BŁĄD: Nie można otworzyć portu COM";
        }
    }

    @GetMapping("/data")
    public String getArduinoData() { return lastData; }

    @PostMapping("/command")
    public String sendCommand(@RequestParam String cmd) {
        String fullCmd = cmd + "\n";
        comPort.writeBytes(fullCmd.getBytes(), fullCmd.length());
        return "Wysłano: " + cmd;
    }

    @GetMapping("/plants")
    public List<Plant> getAllPlants() { return repository.findAll(); }

    @PostMapping("/plants")
    public Plant addPlant(@RequestBody Plant plant) { return repository.save(plant); }

    @DeleteMapping("/plants/{id}")
    public void deletePlant(@PathVariable Long id) {
        repository.deleteById(id);
    }
}
