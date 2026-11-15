package com.example.js_to_isoservice.iso;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Trame ISO 8583 telle qu'elle passe sur le NACChannel :
 * [longueur 2 octets][en-tête TPDU][MTI][bitmap][champs...]. PAN masqué.
 */
@Data
public class WireDump {
    /** Trame complète en hexadécimal (longueur + en-tête + message). */
    private String hex;
    /** Nombre total d'octets de la trame. */
    private int totalBytes;
    /** Découpage de la trame, dans l'ordre. */
    private List<Segment> segments = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Segment {
        /** "LEN", "HEADER", "MTI", "BITMAP" ou le numéro de champ. */
        private String key;
        private String label;
        private int offset;
        private String hex;
        /** Représentation ASCII (caractères non imprimables remplacés par "."). */
        private String ascii;
    }
}
