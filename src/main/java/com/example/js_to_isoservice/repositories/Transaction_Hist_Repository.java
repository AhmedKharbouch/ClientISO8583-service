package com.example.js_to_isoservice.repositories;

import com.example.js_to_isoservice.entities.Transaction_Hist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface Transaction_Hist_Repository extends JpaRepository<Transaction_Hist, Long> {

    List<Transaction_Hist> findByCardNumber(String card);

    // "=" et non "like" : sinon "%" ou "_" dans l'URL correspondent à n'importe quoi
    @Query("select c from Transaction_Hist c where c.cardNumber = :cd and c.date_transaction = :dt order by c.id desc")
    List<Transaction_Hist> findByCardNumberAndDateTransaction(@Param("cd") String card, @Param("dt") String date);

    @Modifying
    @Query("delete from Transaction_Hist c where c.state = :st")
    int deleteByState(@Param("st") String state);
}
