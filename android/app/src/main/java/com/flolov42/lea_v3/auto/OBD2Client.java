package com.flolov42.lea_v3.auto;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OBD2Client {

    private static final UUID SPP_UUID =
        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public interface Callback {
        void onConnected(String deviceName);
        void onDisconnected(String reason);
        void onData(OBD2Data data);
        void onDtcResult(List<String> codes);
        void onDtcCleared();
        void onError(String message);
    }

    public static class OBD2Data {
        public int rpm         = 0;
        public int speed       = 0;
        public int coolantTemp = 0;
        public int throttlePct = 0;
    }

    // ─── état ──────────────────────────────────────────────────────
    private BluetoothSocket   socket;
    private InputStream       in;
    private OutputStream      out;
    private volatile boolean  running      = false;
    private volatile boolean  polling      = false;
    private volatile boolean  pollingActive = false; // vrai tant que la boucle tourne réellement
    private Callback          callback;

    private final Handler ui = new Handler(Looper.getMainLooper());

    // Deux executors distincts : un pour le polling, un pour les commandes DTC
    private final ExecutorService connExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService dtcExecutor  = Executors.newSingleThreadExecutor();

    // ─── connexion ─────────────────────────────────────────────────
    public void connect(BluetoothDevice device, Callback cb) {
        this.callback = cb;
        connExecutor.execute(() -> {
            try {
                BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                socket = device.createRfcommSocketToServiceRecord(SPP_UUID);
                socket.connect();
                in  = socket.getInputStream();
                out = socket.getOutputStream();
                running = true;

                initELM327();
                ui.post(() -> cb.onConnected(device.getName()));
                startPolling();

            } catch (Exception e) {
                ui.post(() -> cb.onError("Connexion échouée : " + e.getMessage()));
            }
        });
    }

    public void disconnect() {
        running = false;
        polling = false;
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    // ─── init ELM327 ───────────────────────────────────────────────
    private void initELM327() throws IOException, InterruptedException {
        sendRaw("ATZ",  2000); // reset complet
        sendRaw("ATE0",  300); // echo off
        sendRaw("ATL0",  300); // linefeeds off
        sendRaw("ATH0",  300); // headers off
        sendRaw("ATS0",  300); // spaces off dans headers (pas dans données)
        sendRaw("ATAT2", 300); // adaptive timing niveau 2 — attend plus longtemps l'ECU
        sendRaw("ATST FF", 300); // timeout max pour les réponses véhicule
        sendRaw("ATSP0", 800); // auto-detect protocole
    }

    // ─── polling données ───────────────────────────────────────────
    private void startPolling() {
        polling = true;
        connExecutor.execute(() -> {
            pollingActive = true;
            while (running && polling) {
                try {
                    OBD2Data d = new OBD2Data();
                    d.rpm         = readRPM();
                    d.speed       = readSpeed();
                    d.coolantTemp = readCoolant();
                    d.throttlePct = readThrottle();
                    final OBD2Data copy = d;
                    ui.post(() -> callback.onData(copy));
                    Thread.sleep(600);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    if (running) ui.post(() -> callback.onError("Perte de connexion"));
                    running = false;
                }
            }
            pollingActive = false;
        });
    }

    // ─── PIDs ──────────────────────────────────────────────────────
    private int readRPM() throws IOException, InterruptedException {
        String[] p = sendRaw("010C", 2000).split("\\s+");
        if (p.length >= 4 && "41".equalsIgnoreCase(p[0]) && "0C".equalsIgnoreCase(p[1]))
            return ((parse(p[2]) * 256) + parse(p[3])) / 4;
        return 0;
    }

    private int readSpeed() throws IOException, InterruptedException {
        String[] p = sendRaw("010D", 2000).split("\\s+");
        if (p.length >= 3 && "41".equalsIgnoreCase(p[0]) && "0D".equalsIgnoreCase(p[1]))
            return parse(p[2]);
        return 0;
    }

    private int readCoolant() throws IOException, InterruptedException {
        String[] p = sendRaw("0105", 2000).split("\\s+");
        if (p.length >= 3 && "41".equalsIgnoreCase(p[0]) && "05".equalsIgnoreCase(p[1]))
            return parse(p[2]) - 40;
        return 0;
    }

    private int readThrottle() throws IOException, InterruptedException {
        String[] p = sendRaw("0111", 2000).split("\\s+");
        if (p.length >= 3 && "41".equalsIgnoreCase(p[0]) && "11".equalsIgnoreCase(p[1]))
            return (parse(p[2]) * 100) / 255;
        return 0;
    }

    // ─── DTC ───────────────────────────────────────────────────────
    public void readDTCs() {
        polling = false; // demande à la boucle de s'arrêter

        dtcExecutor.execute(() -> {
            try {
                // Attendre que la boucle de polling s'arrête réellement (max 4 s)
                long waited = 0;
                while (pollingActive && waited < 4000) {
                    Thread.sleep(50);
                    waited += 50;
                }

                // Mode 03 = codes défaut stockés (timeout long : 10 s)
                String r03 = sendRaw("03", 10000);
                List<String> codes = parseDTCs(r03);

                // Mode 0A = codes défaut permanents (non effaçables par scan tool)
                // Certains véhicules stockent uniquement dans 0A
                if (codes.isEmpty()) {
                    String r0A = sendRaw("0A", 10000);
                    codes = parseDTCs(r0A);
                }

                final List<String> result = codes;
                ui.post(() -> callback.onDtcResult(result));

            } catch (Exception e) {
                ui.post(() -> callback.onError("Erreur DTC : " + e.getMessage()));
            } finally {
                if (running) startPolling();
            }
        });
    }

    public void clearDTCs() {
        polling = false;
        dtcExecutor.execute(() -> {
            try {
                long waited = 0;
                while (pollingActive && waited < 4000) {
                    Thread.sleep(50);
                    waited += 50;
                }
                sendRaw("04", 5000);
                ui.post(() -> callback.onDtcCleared());
            } catch (Exception e) {
                ui.post(() -> callback.onError("Erreur effacement : " + e.getMessage()));
            } finally {
                if (running) startPolling();
            }
        });
    }

    // ─── parseDTCs ─────────────────────────────────────────────────
    // Gère : SEARCHING..., multi-frame (plusieurs lignes "43 XX XX"),
    // NO DATA, et CR/LF remplacés par des espaces dans sendRaw.
    private List<String> parseDTCs(String response) {
        List<String> codes = new ArrayList<>();
        if (response == null || response.isEmpty()) return codes;

        // NO DATA ou NODATA = aucun code
        String flat = response.toUpperCase().replaceAll("\\s+", "");
        if (flat.contains("NODATA") || flat.isEmpty()) return codes;

        // Nettoyer : ne garder que les tokens hexadécimaux (max 2 chars chacun)
        // CR/LF ont déjà été remplacés par des espaces dans sendRaw
        String clean = response.replaceAll("[^0-9A-Fa-f\\s]", " ")
                               .replaceAll("\\s+", " ").trim();
        String[] tokens = clean.split("\\s+");

        // Parcourir les tokens et trouver chaque bloc "43 [byte pairs...]"
        // Gère plusieurs blocs "43" (réponse multi-frame CAN)
        int i = 0;
        while (i < tokens.length) {
            if (!"43".equalsIgnoreCase(tokens[i]) && !"4A".equalsIgnoreCase(tokens[i])) {
                i++; // ignore les tokens avant/entre les headers (ex: "EAC" de SEARCHING)
                continue;
            }
            i++; // saute le header "43" ou "4A"

            // Lit les paires de bytes jusqu'au prochain header ou fin
            while (i + 1 < tokens.length
                   && !"43".equalsIgnoreCase(tokens[i])
                   && !"4A".equalsIgnoreCase(tokens[i])) {

                String s1 = tokens[i];
                String s2 = tokens[i + 1];

                // Ignorer les tokens qui ne sont pas exactement 2 chars hex
                if (s1.length() == 2 && s2.length() == 2) {
                    try {
                        int b1 = Integer.parseInt(s1, 16);
                        int b2 = Integer.parseInt(s2, 16);
                        if (!(b1 == 0 && b2 == 0)) { // ignore le padding 00 00
                            char type = "PCBU".charAt((b1 >> 6) & 0x03);
                            String code = String.format("%c%d%X%02X",
                                type, (b1 >> 4) & 0x03, b1 & 0x0F, b2);
                            if (!codes.contains(code)) codes.add(code);
                        }
                    } catch (Exception ignored) {}
                }
                i += 2;
            }
        }
        return codes;
    }

    // ─── sendRaw ───────────────────────────────────────────────────
    // Remplace CR/LF par un espace pour éviter la concaténation de bytes.
    // timeoutMs : délai max depuis le dernier octet reçu ou depuis l'envoi.
    private synchronized String sendRaw(String cmd, long timeoutMs)
            throws IOException {
        out.write((cmd + "\r").getBytes("UTF-8"));
        out.flush();

        StringBuilder sb = new StringBuilder();
        long deadline = System.currentTimeMillis() + timeoutMs;

        while (System.currentTimeMillis() < deadline) {
            if (in.available() == 0) {
                // Pas d'octet disponible, on fait une micro-pause plutôt que
                // de bloquer in.read() indéfiniment
                try { Thread.sleep(5); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                continue;
            }
            int c = in.read();
            if (c == -1 || c == '>') break;

            if (c == '\r' || c == '\n') {
                // Remplace le saut de ligne par un espace pour ne pas coller les bytes
                if (sb.length() > 0 && sb.charAt(sb.length() - 1) != ' ')
                    sb.append(' ');
            } else {
                sb.append((char) c);
            }
        }

        // Retire l'écho de la commande si présent
        String result = sb.toString().trim();
        // Retire l'écho insensible à la casse
        result = result.replaceAll("(?i)^" + cmd.replace(" ", "\\s*") + "\\s*", "").trim();
        return result;
    }

    // ─── utilitaires ───────────────────────────────────────────────
    private static int parse(String hex) {
        return Integer.parseInt(hex.trim(), 16);
    }

    public static List<BluetoothDevice> getPairedDevices() {
        BluetoothAdapter a = BluetoothAdapter.getDefaultAdapter();
        if (a == null) return new ArrayList<>();
        Set<BluetoothDevice> paired = a.getBondedDevices();
        return new ArrayList<>(paired != null ? paired : new ArrayList<>());
    }

    public boolean isConnected() { return running; }
}
