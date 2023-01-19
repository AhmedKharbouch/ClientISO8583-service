package com.example.js_to_isoservice.repositories;

import com.example.js_to_isoservice.entities.Currency;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CurrencyRepository extends JpaRepository<Currency,Long > {

}
