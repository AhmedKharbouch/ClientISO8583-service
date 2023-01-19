package com.example.js_to_isoservice;

import com.example.js_to_isoservice.entities.Currency;
import com.example.js_to_isoservice.entities.Isomsg;
import com.example.js_to_isoservice.services.CurrencyService;
import lombok.extern.slf4j.Slf4j;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.jpos.iso.MUX;
import org.jpos.q2.Q2;
import org.jpos.q2.iso.QMUX;
import org.jpos.util.NameRegistrar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import java.util.stream.Stream;

//@EnableDiscoveryClient
@SpringBootApplication
public class JsToIsoServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(JsToIsoServiceApplication.class, args);
    }

    @Bean
    public Q2 q2(){
        Q2 q2= new Q2("deploy");
        q2.start();

        return q2;
    }

    //get MUX
    @Bean
    public MUX mux(Q2 q2) throws NameRegistrar.NotFoundException {
        while(!q2.ready()){
            ISOUtil.sleep(10);
        }
        return QMUX.getMUX("clientsimulator-mux");
    }




   //@Bean
    CommandLineRunner commandLineRunner(CurrencyService currencyService){
        return args -> {
            Currency c1 = new Currency();
            c1.setCurrency("US Dollar");
            c1.setCode_numeric("840");
            c1.setCode_alpha("USD");
            Currency c2 = new Currency();
            c2.setCurrency("European Currency Unit");
            c2.setCode_numeric("978");
            c2.setCode_alpha("EUR");
            Currency c3 = new Currency();
            c3.setCurrency("Moroccan Dirham");
            c3.setCode_numeric("504");
            c3.setCode_alpha("MAD");

            currencyService.saveCurrency(c1);
            currencyService.saveCurrency(c2);
            currencyService.saveCurrency(c3);

        };
    }


}
