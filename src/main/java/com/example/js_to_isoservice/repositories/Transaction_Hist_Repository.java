package com.example.js_to_isoservice.repositories;

import com.example.js_to_isoservice.entities.Transaction_Hist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;

@EnableJpaRepositories
public interface Transaction_Hist_Repository extends JpaRepository<Transaction_Hist,Long > {

Transaction_Hist findByCardNumber(String card);

@Query("select c from Transaction_Hist c where c.cardNumber like :cd and c.date_transaction like :dt")
Transaction_Hist findByCardNumberAndDate_transaction(@Param(value = "cd")String card,@Param(value = "dt")String date);

@Modifying
@Query("delete from Transaction_Hist c where c.state like :st")
void deleteString(@Param(value = "st")String state);


Page<Transaction_Hist> findAll(Pageable pageable);




}
