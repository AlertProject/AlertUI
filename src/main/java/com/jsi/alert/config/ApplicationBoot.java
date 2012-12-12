package com.jsi.alert.config;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationBoot implements ServletContextListener {
	
	private static final Logger log = LoggerFactory.getLogger(ApplicationBoot.class);

	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		if (log.isDebugEnabled())
			log.debug("Shutting down...");
	}

	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		if (log.isDebugEnabled())
			log.debug("Starting application...");
		
		// init configuration
		@SuppressWarnings("unused")
		String dummyVar = Configuration.ACTIVEMQ_URL;
	}
}
