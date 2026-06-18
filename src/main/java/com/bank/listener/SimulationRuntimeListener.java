package com.bank.listener;

import com.bank.service.impl.SimulationRuntimeManager;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class SimulationRuntimeListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        SimulationRuntimeManager.getInstance().shutdown();
    }
}
