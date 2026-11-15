package com.example.js_to_isoservice;

import com.example.js_to_isoservice.entities.Currency;
import com.example.js_to_isoservice.services.CurrencyService;
import org.jpos.iso.ISOUtil;
import org.jpos.iso.MUX;
import org.jpos.q2.Q2;
import org.jpos.q2.iso.QMUX;
import org.jpos.util.NameRegistrar;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class JsToIsoServiceApplication {

    private static final long Q2_START_TIMEOUT_MS = 30_000;

    public static void main(String[] args) {
        SpringApplication.run(JsToIsoServiceApplication.class, args);
    }

    @Bean(destroyMethod = "shutdown")
    public Q2 q2() {
        Q2 q2 = new Q2("deploy");
        q2.start();
        return q2;
    }

    @Bean
    public MUX mux(Q2 q2) throws NameRegistrar.NotFoundException {
        long deadline = System.currentTimeMillis() + Q2_START_TIMEOUT_MS;
        while (!q2.ready()) {
            if (System.currentTimeMillis() > deadline) {
                throw new IllegalStateException("Q2 n'a pas démarré en " + Q2_START_TIMEOUT_MS + " ms");
            }
            ISOUtil.sleep(10);
        }
        return QMUX.getMUX("clientsimulator-mux");
    }

    //@Bean
    CommandLineRunner commandLineRunner(CurrencyService currencyService) {
        return args -> {
            currencyService.saveCurrency(new Currency(null, "US Dollar", "USD", "840"));
            currencyService.saveCurrency(new Currency(null, "European Currency Unit", "EUR", "978"));
            currencyService.saveCurrency(new Currency(null, "Moroccan Dirham", "MAD", "504"));
        };
    }
}
