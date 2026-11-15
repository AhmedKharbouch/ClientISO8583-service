package com.example.js_to_isoservice.iso;

import lombok.Data;

import java.util.Map;

/** Un échange ISO 8583 (appel 0200 + réception 0210) tel qu'affiché par le front. PAN toujours masqué. */
@Data
public class IsoExchange {
    private long id;
    private String timestamp;
    private long durationMs;

    /** APPROUVE, REFUSE, TIMEOUT, ERREUR ou REJETE (refusé localement avant envoi). */
    private String status;
    private String stan;
    private String responseCode;
    private String state;
    private String error;

    private String cardMasked;
    private double amount;
    private String currency;
    private String pointDeService;

    /** Champs du message envoyé (clé "MTI" puis numéros de champ). */
    private Map<String, String> request;
    /** Champs du message reçu, null si aucune réponse. */
    private Map<String, String> response;

    /** Trame ISO construite et envoyée (hex + découpage). */
    private WireDump requestWire;
    /** Trame ISO reçue en retour, null si aucune réponse. */
    private WireDump responseWire;
}
