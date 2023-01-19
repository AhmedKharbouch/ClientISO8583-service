package com.example.js_to_isoservice.services;


import com.example.js_to_isoservice.entities.Currency;

import java.util.List;

public interface CurrencyService {
    List<Currency> listCurrency();
    Currency saveCurrency(Currency currency);
}
