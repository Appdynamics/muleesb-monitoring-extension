/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */
package com.appdynamics.monitors.muleesb.config;

public class Configuration {

	private Server server;
	private MBeanData mbeans;
	private String metricPrefix;

	public Server getServer() {
		return server;
	}

	public void setServer(Server server) {
		this.server = server;
	}

	public MBeanData getMbeans() {
		return mbeans;
	}

	public void setMbeans(MBeanData mbeans) {
		this.mbeans = mbeans;
	}

	public String getMetricPrefix() {
		return metricPrefix;
	}

	public void setMetricPrefix(String metricPrefix) {
		this.metricPrefix = metricPrefix;
	}
}
