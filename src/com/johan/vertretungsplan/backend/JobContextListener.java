package com.johan.vertretungsplan.backend;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class JobContextListener implements ServletContextListener {

	private ScheduledExecutorService ses;
	
	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		ses.shutdown();
	}

	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		ses = Executors.newScheduledThreadPool(1);
		ses.scheduleWithFixedDelay(new CronJob(), 10, 60, TimeUnit.SECONDS);
	}

}
