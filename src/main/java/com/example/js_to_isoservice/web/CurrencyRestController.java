package com.example.js_to_isoservice.web;

import com.example.js_to_isoservice.entities.Currency;
import com.example.js_to_isoservice.services.CurrencyService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@AllArgsConstructor
@Slf4j
@CrossOrigin("*")
public class CurrencyRestController {
    private CurrencyService currencyService;

    @GetMapping("/currency")
    public List<Currency> currency(){
        return currencyService.listCurrency();
    }

}
