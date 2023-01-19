package com.example.js_to_isoservice.services;

import com.example.js_to_isoservice.entities.Isomsg;

import java.util.List;

public interface IsomsgService {
    List<Isomsg> listmsgs();
    Isomsg saveIsomsg(Isomsg isomsg);
    Isomsg getIsomsg(String c);
}
