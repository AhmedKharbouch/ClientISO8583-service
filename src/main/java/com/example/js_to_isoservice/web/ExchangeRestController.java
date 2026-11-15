package com.example.js_to_isoservice.web;

import com.example.js_to_isoservice.iso.ExchangeLog;
import com.example.js_to_isoservice.iso.IsoExchange;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Journal des appels / réceptions ISO 8583 pour le front de suivi. */
@RestController
@AllArgsConstructor
@CrossOrigin("*")
public class ExchangeRestController {

    private final ExchangeLog exchangeLog;

    @GetMapping("/exchanges")
    public List<IsoExchange> exchanges(@RequestParam(defaultValue = "0") long sinceId) {
        return exchangeLog.list(sinceId);
    }

    @DeleteMapping("/exchanges")
    public void clear() {
        exchangeLog.clear();
    }
}
