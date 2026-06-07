package com.fiap.gs2.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fiap.gs2.dao.IntersecaoDao;

@Service
public class IntersecaoService {

    private static final Logger log = LoggerFactory.getLogger(IntersecaoService.class);

    private final IntersecaoDao intersecaoDao;
    private final boolean materializarOnStartup;

    public IntersecaoService(
            IntersecaoDao intersecaoDao,
            @Value("${intersecao.materializar-on-startup:true}") boolean materializarOnStartup) {
        this.intersecaoDao = intersecaoDao;
        this.materializarOnStartup = materializarOnStartup;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void materializarNaInicializacao() {
        if (!materializarOnStartup) {
            log.info("Materializacao de intersecoes no startup desativada.");
            return;
        }
        long inicio = System.currentTimeMillis();
        int novas = intersecaoDao.materializar();
        log.info("Intersecoes materializadas: {} novas em {} ms.", novas, System.currentTimeMillis() - inicio);
    }
}
