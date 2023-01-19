package com.example.js_to_isoservice.services;


import com.example.js_to_isoservice.repositories.CurrencyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.js_to_isoservice.entities.Currency;
import com.example.js_to_isoservice.repositories.CurrencyRepository;

import java.util.List;
@Service
public class CurrencyServiceImpl implements CurrencyService {
    @Autowired
    private CurrencyRepository currencyRepository;

    @Override
    public List<Currency> listCurrency() {
      return  currencyRepository.findAll();
    }

    @Override
    public Currency saveCurrency(Currency currency) {
        return currencyRepository.save(currency);
    }
}
