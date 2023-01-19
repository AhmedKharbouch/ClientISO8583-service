package com.example.js_to_isoservice.services;

import com.example.js_to_isoservice.entities.Isomsg;
import com.example.js_to_isoservice.entities.Transaction_Hist;

import java.util.Date;
import java.util.List;

public interface Transaction_Hist_Service {
    List<Transaction_Hist> listHist();
    Transaction_Hist saveIsomsg(Transaction_Hist transactionHist);
    Transaction_Hist getTransactions(String c);
    Transaction_Hist getHist(String c, String date);
    void deleteHist(Long state);
    void deleteStr(String state);
}
