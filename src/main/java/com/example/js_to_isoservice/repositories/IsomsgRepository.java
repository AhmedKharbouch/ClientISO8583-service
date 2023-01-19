package com.example.js_to_isoservice.repositories;

import com.example.js_to_isoservice.entities.Isomsg;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IsomsgRepository extends JpaRepository<Isomsg,Long > {

Isomsg findByCardNumber(String card);

}
