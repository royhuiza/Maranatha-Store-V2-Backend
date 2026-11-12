package com.maranata.store.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Bitacora en memoria de la actividad del chatbot.
 * No persiste datos personales: solo origen, direccion y estado.
 */
@Service
public class ChatLogService {

    private static final int MAX_ENTRIES = 200;

    private final Deque<Entry> entries = new ArrayDeque<>();

    public record Entry(LocalDateTime fecha, String origen, String direccion, String mensaje, String estado) {
        public Entry(String origen, String direccion, String mensaje, String estado) {
            this(LocalDateTime.now(), origen, direccion, mensaje, estado);
        }
    }

    public synchronized void registrar(String origen, String direccion, String mensaje, String estado) {
        entries.addFirst(new Entry(origen, direccion, mensaje, estado));
        while (entries.size() > MAX_ENTRIES) {
            entries.removeLast();
        }
    }

    public synchronized List<Entry> listar(int limite) {
        int max = limite <= 0 ? MAX_ENTRIES : Math.min(limite, MAX_ENTRIES);
        List<Entry> copia = new ArrayList<>(entries);
        return copia.subList(0, Math.min(max, copia.size()));
    }
}
