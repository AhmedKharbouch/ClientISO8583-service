package com.example.js_to_isoservice.web;

import com.example.js_to_isoservice.entities.Isomsg;
import com.example.js_to_isoservice.entities.Transaction_Hist;
import com.example.js_to_isoservice.iso.StanGenerator;
import com.example.js_to_isoservice.services.IsoTransactionService;
import com.example.js_to_isoservice.services.IsomsgService;
import com.example.js_to_isoservice.services.Transaction_Hist_Service;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.jpos.iso.MUX;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@RestController
@AllArgsConstructor
@Slf4j
@CrossOrigin("*")
public class IsomsgRestController {

    private final IsomsgService isomsgService;
    private final Transaction_Hist_Service transactionHistService;
    private final IsoTransactionService isoTransactionService;
    private final StanGenerator stanGenerator;
    private final MUX mux;

    @GetMapping("/isoMsgs")
    public List<Isomsg> isoMsgs() {
        return isomsgService.listmsgs();
    }

    /**
     * Envoie la transaction au switch (0200) et renvoie l'entrée d'historique créée.
     * 400 si les données sont invalides, 504 si le switch ne répond pas, 502 en cas d'erreur d'envoi.
     */
    @PostMapping("/isoMsg")
    public Transaction_Hist saveIsoMsg(@RequestBody Isomsg isomsg) {
        return isoTransactionService.authorize(isomsg);
    }

    /** Test de connectivité : envoie un 0200 factice et renvoie la réponse brute. */
    @GetMapping("/echo")
    public String echo() throws ISOException {
        return sendTestMessage("0000000000000", "000000");
    }

    @GetMapping(value = "/echo-v2", produces = MediaType.TEXT_PLAIN_VALUE)
    public String echoV2(@RequestParam String cardNumber,
                         @RequestParam String processingCode) throws ISOException {
        return sendTestMessage(cardNumber, processingCode);
    }

    private String sendTestMessage(String pan, String processingCode) throws ISOException {
        ISOMsg isoMsg = new ISOMsg();
        isoMsg.setMTI("0200");
        isoMsg.set(2, pan);
        isoMsg.set(3, processingCode);
        isoMsg.set(4, "000000100000");
        isoMsg.set(7, new SimpleDateFormat("MMddHHmmss").format(new Date()));
        isoMsg.set(11, stanGenerator.next());
        isoMsg.set(12, new SimpleDateFormat("HHmmss").format(new Date()));
        ISOMsg respMsg = mux.request(isoMsg, 5000);
        if (respMsg == null) {
            return "No response received from server.";
        }
        log.info("echo PAN={} -> 39={}", ISOUtil.protect(pan), respMsg.getString(39));
        return respMsg.toString();
    }

    @GetMapping("/TransactionHist")
    public List<Transaction_Hist> isoMsgsHist() {
        return transactionHistService.listHist();
    }

    /** Le PAN est stocké masqué : on masque la carte reçue avant la recherche. */
    @GetMapping("/Trs/{card}/{date}")
    public Transaction_Hist transactionByCard(@PathVariable(name = "card") String card,
                                              @PathVariable(name = "date") String date) {
        return transactionHistService.getHist(ISOUtil.protect(card), date);
    }

    @DeleteMapping("/History/{id}")
    public void deleteHist(@PathVariable Long id) {
        transactionHistService.deleteHist(id);
    }

    @DeleteMapping("/deleteHist/{st}")
    public void deleteHist(@PathVariable String st) {
        transactionHistService.deleteStr(st);
    }
}
