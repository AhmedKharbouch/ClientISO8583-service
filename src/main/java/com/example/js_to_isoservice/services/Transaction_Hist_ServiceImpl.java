package com.example.js_to_isoservice.services;

import com.example.js_to_isoservice.entities.Transaction_Hist;
import com.example.js_to_isoservice.repositories.Transaction_Hist_Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class Transaction_Hist_ServiceImpl implements Transaction_Hist_Service {
    @Autowired
    private Transaction_Hist_Repository transaction_hist_repository;

    @Override
    public List<Transaction_Hist> listHist() {
        return transaction_hist_repository.findAll();
    }

    @Override
    public Transaction_Hist saveIsomsg(Transaction_Hist transactionHist) {
        return transaction_hist_repository.save(transactionHist);
    }

    @Override
    public Transaction_Hist getTransactions(String c) {
        List<Transaction_Hist> list = transaction_hist_repository.findByCardNumber(c);
        return list.isEmpty() ? null : list.get(list.size() - 1);
    }

    /** Renvoie la transaction la plus récente pour cette carte et cette date (null si aucune). */
    @Override
    public Transaction_Hist getHist(String c, String date) {
        List<Transaction_Hist> list = transaction_hist_repository.findByCardNumberAndDateTransaction(c, date);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public void deleteStr(String state) {
        transaction_hist_repository.deleteByState(state);
    }

    @Override
    public void deleteHist(Long state) {
        transaction_hist_repository.deleteById(state);
    }
}
