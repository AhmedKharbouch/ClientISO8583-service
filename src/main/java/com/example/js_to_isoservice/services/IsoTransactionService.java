package com.example.js_to_isoservice.services;

import com.example.js_to_isoservice.entities.Isomsg;
import com.example.js_to_isoservice.entities.Transaction_Hist;
import com.example.js_to_isoservice.iso.ExchangeLog;
import com.example.js_to_isoservice.iso.IsoExchange;
import com.example.js_to_isoservice.iso.StanGenerator;
import lombok.extern.slf4j.Slf4j;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.jpos.iso.MUX;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Transforme une transaction JSON en message ISO 8583 0200, l'envoie au switch
 * via le QMUX, enregistre le résultat dans l'historique et dans le journal des échanges.
 */
@Service
@Slf4j
public class IsoTransactionService {

    private static final Pattern PAN = Pattern.compile("\\d{12,19}");
    private static final Pattern EXPIRY_YYYY_MM = Pattern.compile("^\\d{2}(\\d{2})-(\\d{2})");
    private static final Pattern EXPIRY_YYMM = Pattern.compile("^\\d{4}$");
    /** Format envoyé par le front : HH/mm/ss/MM/dd -> champ 12 (hhmmss) et 13 (MMDD). */
    private static final Pattern TX_DATE = Pattern.compile("^(\\d{2})/(\\d{2})/(\\d{2})/(\\d{2})/(\\d{2})$");

    private static final String PROCESSING_CODE_PURCHASE = "000000";
    private static final String ROUTE_SERVER_1 = "8001";

    private final MUX mux;
    private final StanGenerator stanGenerator;
    private final Transaction_Hist_Service historyService;
    private final ExchangeLog exchangeLog;
    private final long timeoutMs;

    public IsoTransactionService(MUX mux,
                                 StanGenerator stanGenerator,
                                 Transaction_Hist_Service historyService,
                                 ExchangeLog exchangeLog,
                                 @Value("${iso.mux.timeout-ms:30000}") long timeoutMs) {
        this.mux = mux;
        this.stanGenerator = stanGenerator;
        this.historyService = historyService;
        this.exchangeLog = exchangeLog;
        this.timeoutMs = timeoutMs;
    }

    public Transaction_Hist authorize(Isomsg tx) {
        long start = System.nanoTime();
        IsoExchange exchange = new IsoExchange();
        exchange.setTimestamp(Instant.now().toString());
        exchange.setAmount(tx.getAmount());
        exchange.setCurrency(tx.getCode_Currency());
        exchange.setPointDeService(tx.getPoint_de_service());

        try {
            return doAuthorize(tx, exchange);
        } catch (ResponseStatusException e) {
            if (exchange.getStatus() == null) {
                exchange.setStatus("REJETE");
            }
            exchange.setError(e.getReason());
            throw e;
        } finally {
            exchange.setDurationMs((System.nanoTime() - start) / 1_000_000);
            exchangeLog.add(exchange);
        }
    }

    private Transaction_Hist doAuthorize(Isomsg tx, IsoExchange exchange) {
        String pan = tx.getCardNumber() == null ? "" : tx.getCardNumber().replaceAll("\\s", "");
        exchange.setCardMasked(PAN.matcher(pan).matches() ? ISOUtil.protect(pan) : "invalide");
        if (!PAN.matcher(pan).matches()) {
            throw badRequest("cardNumber doit contenir 12 à 19 chiffres");
        }
        if (tx.getCode_Currency() == null || !tx.getCode_Currency().matches("\\d{3}")) {
            throw badRequest("code_Currency doit être un code numérique ISO 4217 (ex. 504, 840, 978)");
        }

        String amountField = toMinorUnits(tx.getAmount());
        String expiry = toYYMM(tx.getExpireDate());
        String[] localDateTime = toLocalDateTime(tx.getDate_transaction());

        ISOMsg request = new ISOMsg();
        try {
            request.setMTI("0200");
            request.set(2, pan);
            request.set(3, PROCESSING_CODE_PURCHASE);
            request.set(4, amountField);
            request.set(7, LocalDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("MMddHHmmss")));
            request.set(11, stanGenerator.next());
            request.set(12, localDateTime[0]);
            request.set(13, localDateTime[1]);
            request.set(14, expiry);
            request.set(49, tx.getCode_Currency());
            request.set(120, ROUTE_SERVER_1);
        } catch (ISOException e) {
            throw badRequest("Message ISO invalide : " + e.getMessage());
        }
        exchange.setStan(request.getString(11));
        exchange.setRequest(ExchangeLog.fields(request));
        exchange.setRequestWire(exchangeLog.requestWire(request));

        Transaction_Hist hist = new Transaction_Hist();
        hist.setCardNumber(ISOUtil.protect(pan));
        hist.setAmount(tx.getAmount());
        hist.setExpireDate(expiry);
        hist.setCode_Currency(tx.getCode_Currency());
        hist.setPoint_de_service(tx.getPoint_de_service());
        hist.setDate_transaction(localDateTime[0] + localDateTime[1]);

        ISOMsg response;
        try {
            log.info("-> 0200 STAN={} PAN={} montant={}", request.getString(11), ISOUtil.protect(pan), amountField);
            response = mux.request(request, timeoutMs);
        } catch (ISOException e) {
            log.error("Erreur d'envoi ISO", e);
            exchange.setStatus("ERREUR");
            hist.setState("erreur_envoi");
            historyService.saveIsomsg(hist);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Erreur d'envoi au switch", e);
        }

        if (response == null) {
            log.warn("Timeout ({} ms) STAN={}", timeoutMs, request.getString(11));
            exchange.setStatus("TIMEOUT");
            hist.setState("timeout");
            historyService.saveIsomsg(hist);
            throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "Pas de réponse du switch");
        }

        String rc = response.getString(39);
        String state = response.hasField(125) ? response.getString(125) : ("00".equals(rc) ? "success" : "refuse_" + rc);
        log.info("<- {} STAN={} 39={} 125={}", safeMti(response), response.getString(11), rc, state);

        exchange.setResponse(ExchangeLog.fields(response));
        exchange.setResponseWire(exchangeLog.responseWire(response));
        exchange.setResponseCode(rc);
        exchange.setState(state);
        exchange.setStatus("00".equals(rc) ? "APPROUVE" : "REFUSE");

        hist.setState(state);
        return historyService.saveIsomsg(hist);
    }

    /** Champ 4 : montant en unités mineures (centimes), 12 chiffres. */
    private static String toMinorUnits(double amount) {
        if (!(amount > 0)) {
            throw badRequest("amount doit être strictement positif");
        }
        long minor = BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
        if (minor > 999_999_999_999L) {
            throw badRequest("amount trop grand");
        }
        return String.format("%012d", minor);
    }

    /** Champ 14 : date d'expiration AAMM, à partir de "yyyy-MM" (ou déjà "YYMM"). */
    private static String toYYMM(String expireDate) {
        if (expireDate != null) {
            Matcher m = EXPIRY_YYYY_MM.matcher(expireDate.trim());
            if (m.find()) {
                return m.group(1) + m.group(2);
            }
            if (EXPIRY_YYMM.matcher(expireDate.trim()).matches()) {
                return expireDate.trim();
            }
        }
        throw badRequest("expireDate doit être au format yyyy-MM");
    }

    /** Champs 12 (hhmmss) et 13 (MMDD). Format attendu "HH/mm/ss/MM/dd", sinon l'heure courante. */
    private static String[] toLocalDateTime(String dateTransaction) {
        if (dateTransaction != null) {
            Matcher m = TX_DATE.matcher(dateTransaction.trim());
            if (m.matches()) {
                return new String[]{m.group(1) + m.group(2) + m.group(3), m.group(4) + m.group(5)};
            }
        }
        LocalDateTime now = LocalDateTime.now();
        return new String[]{
                now.format(DateTimeFormatter.ofPattern("HHmmss")),
                now.format(DateTimeFormatter.ofPattern("MMdd"))
        };
    }

    private static String safeMti(ISOMsg msg) {
        try {
            return msg.getMTI();
        } catch (ISOException e) {
            return "????";
        }
    }

    private static ResponseStatusException badRequest(String reason) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
    }
}
