package com.example.js_to_isoservice.iso;

import lombok.extern.slf4j.Slf4j;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.jpos.iso.packager.GenericPackager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Garde en mémoire les derniers échanges ISO pour l'interface de suivi. */
@Component
@Slf4j
public class ExchangeLog {

    private static final int MAX_ENTRIES = 200;

    private final Deque<IsoExchange> entries = new ArrayDeque<>();
    private long lastId = 0;

    private final GenericPackager packager;
    private final byte[] requestHeader;

    public ExchangeLog(@Value("${iso.packager-config:cfg/packager/iso87ascii-binary-bitmap.xml}") String packagerConfig,
                       @Value("${iso.channel-header:6000000000}") String channelHeader) {
        GenericPackager p = null;
        try {
            p = new GenericPackager(packagerConfig);
        } catch (ISOException e) {
            log.warn("Packager {} introuvable : la trame brute ne sera pas affichée", packagerConfig, e);
        }
        this.packager = p;
        this.requestHeader = ISOUtil.hex2byte(channelHeader);
    }

    public synchronized void add(IsoExchange exchange) {
        exchange.setId(++lastId);
        entries.addFirst(exchange);
        while (entries.size() > MAX_ENTRIES) {
            entries.removeLast();
        }
    }

    /** Échanges les plus récents d'abord ; seulement ceux dont l'id est > sinceId. */
    public synchronized List<IsoExchange> list(long sinceId) {
        List<IsoExchange> result = new ArrayList<>();
        for (IsoExchange e : entries) {
            if (e.getId() <= sinceId) {
                break;
            }
            result.add(e);
        }
        return result;
    }

    public synchronized void clear() {
        entries.clear();
    }

    /** Copie les champs d'un ISOMsg dans une map lisible, PAN masqué. */
    public static Map<String, String> fields(ISOMsg msg) {
        Map<String, String> map = new LinkedHashMap<>();
        try {
            map.put("MTI", msg.getMTI());
        } catch (ISOException e) {
            map.put("MTI", "????");
        }
        for (int i = 2; i <= msg.getMaxField(); i++) {
            if (msg.hasField(i)) {
                String value = msg.getString(i);
                map.put(Integer.toString(i), i == 2 ? ISOUtil.protect(value) : value);
            }
        }
        return map;
    }

    /** Trame de l'appel : en-tête configuré sur le channel. */
    public WireDump requestWire(ISOMsg msg) {
        return wire(msg, requestHeader);
    }

    /** Trame de la réception : en-tête réellement reçu (ou celui du channel à défaut). */
    public WireDump responseWire(ISOMsg msg) {
        byte[] h = msg.getHeader();
        return wire(msg, h != null && h.length > 0 ? h : requestHeader);
    }

    /**
     * Reconstitue la trame NACChannel avec le même packager que le channel.
     * Le PAN est remplacé par sa version masquée (*) avant le pack.
     */
    private WireDump wire(ISOMsg original, byte[] header) {
        if (packager == null) {
            return null;
        }
        try {
            ISOMsg msg = (ISOMsg) original.clone();
            msg.setPackager(packager);
            if (msg.hasField(2)) {
                msg.set(2, ISOUtil.protect(msg.getString(2)).replace('_', '*'));
            }
            byte[] body = msg.pack();

            List<byte[]> fieldBytes = new ArrayList<>();
            List<Integer> fieldNos = new ArrayList<>();
            int fieldsLen = 0;
            for (int i = 2; i <= msg.getMaxField(); i++) {
                if (msg.hasField(i)) {
                    byte[] b = packager.getFieldPackager(i).pack(msg.getComponent(i));
                    fieldBytes.add(b);
                    fieldNos.add(i);
                    fieldsLen += b.length;
                }
            }
            byte[] mti = packager.getFieldPackager(0).pack(msg.getComponent(0));
            int bitmapLen = body.length - mti.length - fieldsLen;

            int frameLen = header.length + body.length;
            byte[] len = new byte[]{(byte) (frameLen >> 8), (byte) frameLen};

            ByteArrayOutputStream frame = new ByteArrayOutputStream();
            WireDump dump = new WireDump();
            int offset = 0;
            offset = addSegment(dump, frame, "LEN", "Longueur (2 octets, " + frameLen + ")", len, offset);
            offset = addSegment(dump, frame, "HEADER", "En-tête TPDU", header, offset);
            offset = addSegment(dump, frame, "MTI", "MTI", mti, offset);
            byte[] bitmap = new byte[bitmapLen];
            System.arraycopy(body, mti.length, bitmap, 0, bitmapLen);
            offset = addSegment(dump, frame, "BITMAP", bitmapLen > 8 ? "Bitmap primaire + secondaire" : "Bitmap primaire", bitmap, offset);
            for (int k = 0; k < fieldNos.size(); k++) {
                int no = fieldNos.get(k);
                offset = addSegment(dump, frame, Integer.toString(no), "Champ " + no, fieldBytes.get(k), offset);
            }
            dump.setHex(ISOUtil.hexString(frame.toByteArray()));
            dump.setTotalBytes(frame.size());
            return dump;
        } catch (ISOException | RuntimeException e) {
            log.warn("Impossible de reconstituer la trame ISO", e);
            return null;
        }
    }

    private static int addSegment(WireDump dump, ByteArrayOutputStream frame, String key, String label, byte[] bytes, int offset) {
        frame.write(bytes, 0, bytes.length);
        dump.getSegments().add(new WireDump.Segment(key, label, offset, ISOUtil.hexString(bytes), printable(bytes)));
        return offset + bytes.length;
    }

    private static String printable(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length);
        for (byte b : bytes) {
            int c = b & 0xFF;
            sb.append(c >= 0x20 && c < 0x7F ? (char) c : '.');
        }
        return sb.toString();
    }
}
