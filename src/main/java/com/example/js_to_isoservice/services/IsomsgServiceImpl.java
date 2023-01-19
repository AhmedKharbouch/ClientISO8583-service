package com.example.js_to_isoservice.services;

import com.example.js_to_isoservice.entities.Isomsg;
import com.example.js_to_isoservice.repositories.IsomsgRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IsomsgServiceImpl implements IsomsgService {
    @Autowired
    private IsomsgRepository isomsgRepository;


    @Override
    public List<Isomsg> listmsgs() {
        return isomsgRepository.findAll();
    }

    @Override
    public Isomsg saveIsomsg(Isomsg isomsg) {
        return isomsgRepository.save(isomsg);
    }

    @Override
    public Isomsg getIsomsg(String c) {
        return isomsgRepository.findByCardNumber(c);
    }
}
