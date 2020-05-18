/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.muleesb;

import com.appdynamics.extensions.AMonitorTaskRunnable;
import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.muleesb.collectors.InstanceProcessor;
import com.appdynamics.extensions.muleesb.collectors.MetricsCollector;
import com.appdynamics.extensions.muleesb.utils.Constants;
import static com.appdynamics.extensions.muleesb.utils.Constants.DISPLAY_NAME;
import static com.appdynamics.extensions.muleesb.utils.Constants.ENCRYPTION_KEY;
import static com.appdynamics.extensions.muleesb.utils.Constants.HEARTBEAT;
import static com.appdynamics.extensions.muleesb.utils.Constants.LANG_MEMORY_METRICS;
import static com.appdynamics.extensions.muleesb.utils.Constants.METRICS_SEPARATOR;
import static com.appdynamics.extensions.muleesb.utils.Constants.USERNAME;
import com.appdynamics.extensions.muleesb.utils.JMXUtil;
import com.appdynamics.extensions.util.CryptoUtils;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.slf4j.Logger;

import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;
import java.io.IOException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MuleESBMonitorTask implements AMonitorTaskRunnable {
    private static final Logger logger = ExtensionsLoggerFactory.getLogger(MuleESBMonitorTask.class);

    private Map<String, ?> server;
    private MonitorContextConfiguration configuration;
    private MetricWriteHelper metricWriteHelper;
    private JMXConnectionAdapter jmxConnectionAdapter;
    private String metricPrefix;
    private BigInteger heartBeatValue = BigInteger.ZERO;

    public MuleESBMonitorTask(MonitorContextConfiguration monitorContextConfiguration, MetricWriteHelper metricWriteHelper, Map<String, ?> server) {
        this.configuration = monitorContextConfiguration;
        this.server = server;
        this.metricWriteHelper = metricWriteHelper;
        this.metricPrefix = configuration.getMetricPrefix() + METRICS_SEPARATOR + this.server.get(DISPLAY_NAME) + METRICS_SEPARATOR;
    }

    public void run() {
        try {
            getJMXConnectionAdapter();
            if (jmxConnectionAdapter != null) {
                populateStats();
                heartBeatValue = BigInteger.ONE;
                logger.info("Completed the Mule esb Monitoring task for {}", server.get(DISPLAY_NAME));
            }
        } catch (Exception e) {
            logger.error("Error while running the task " + server.get("displayName") + e);
        }
    }

    private void populateStats() {
        JMXConnector jmxConnector = null;
        List<Metric> metrics = Lists.newArrayList();
        try {
            try {
                if (jmxConnectionAdapter != null)
                    jmxConnector = jmxConnectionAdapter.open();
            } catch (IOException e) {
                logger.error("Error JMX-ing into Mule ESB Server ", e);
                metrics.add(new Metric(Constants.METRICS_COLLECTED, Constants.ERROR_VALUE, metricPrefix + Constants.METRICS_COLLECTED));
            }

            Map<String, ?> mbeanDetails = (Map<String, String>) server.get(Constants.MBEAN_DETAILS);
            String domainMatcher = (String) mbeanDetails.get(Constants.DOMAIN_MATCHER);
            List<String> types = (List<String>) mbeanDetails.get(Constants.TYPES);
            List<String> excludeDomains = (List<String>) mbeanDetails.get(Constants.EXCLUDE_DOMAINS);
            //Adding support for Flows filtering
            List<String> flows = (List<String>) mbeanDetails.get(Constants.FLOWS);

            for (String type : types) {
                String mbeanMatcher = buildMbeanMatcher(domainMatcher, type);
                try {
                    logger.debug("started metricsCollector the type : " + type + "with mbeanMatcher : " + mbeanMatcher);
                    Set<ObjectInstance> objectInstances = jmxConnectionAdapter.queryMBeans(jmxConnector, ObjectName.getInstance(mbeanMatcher));
                    MetricsCollector metricsCollector = new MetricsCollector(configuration, jmxConnectionAdapter, jmxConnector, metricPrefix);
                    metrics.addAll(metricsCollector.initMetricsCollection(objectInstances, excludeDomains, flows));
                    logger.debug("Successfully processed metricsCollector for the type : " + type + "with mbeanMatcher : " + mbeanMatcher);
                } catch (IOException e) {
                    logger.error("Error getting bean with type :" + type, e);
                } catch (MalformedObjectNameException e) {
                    logger.error("Error getting bean with type :" + type, e);
                } catch (Exception e) {
                    logger.error("Error in Exception getting bean with type :" + type, e);
                }
            }
//          Collect the composite memory metrics as configures in the metrics.xml
            metrics.addAll(collectCompositeMemoryInstanceMetrics(jmxConnector));
        } catch (Exception e) {
            logger.error("Error while collecting the metrics for: " + server.get(Constants.DISPLAY_NAME));
        } finally {
            if (jmxConnector != null) {
                try {
                    jmxConnector.close();
                } catch (IOException e) {
                    logger.error("Unable to close the connection", e);
                }
            }
            metricWriteHelper.transformAndPrintMetrics(metrics);
        }
    }

    private List<Metric> collectCompositeMemoryInstanceMetrics(JMXConnector jmxConnector) throws IntrospectionException, ReflectionException, InstanceNotFoundException, IOException, MalformedObjectNameException {
        String langMemoryMbean = Constants.OBJECT_NAME;
        Set<ObjectInstance> langMemoryInstances = jmxConnectionAdapter.queryMBeans(jmxConnector, ObjectName.getInstance(langMemoryMbean));
        InstanceProcessor instanceProcessor = new InstanceProcessor(jmxConnectionAdapter, jmxConnector, JMXUtil.getAptMetricConfigAttr(configuration, LANG_MEMORY_METRICS).getMetricConfig(), metricPrefix);
        return instanceProcessor.processInstance(langMemoryInstances.iterator().next());
    }

    private String buildMbeanMatcher(String domainMatcher, String type) {
        StringBuilder sb = new StringBuilder(domainMatcher);
        sb.append(":").append("type=").append(type).append(",*");
        return sb.toString();
    }

    private void getJMXConnectionAdapter() {
        String serviceUrl = (String) server.get(Constants.SERVICE_URL);
        String username = (String) server.get(USERNAME);
        String password = getPassword(server);
        String host = (String) server.get(com.appdynamics.extensions.Constants.HOST);
        try {
            if (!Strings.isNullOrEmpty(serviceUrl)) {
                jmxConnectionAdapter = new JMXConnectionAdapter(serviceUrl, username, password);
            } else if (!Strings.isNullOrEmpty(host)) {

                String portStr = server.get(com.appdynamics.extensions.Constants.PORT).toString();
                int port = checkPortValidity(portStr);
                jmxConnectionAdapter = new JMXConnectionAdapter(host, port, username, password);
            } else
                logger.info("JMX details not provided, not creating connection");

        } catch (MalformedURLException me) {
            logger.error("Malformed Error while creating Jmx connection Adapter:", me);
        } catch (IllegalArgumentException ie) {
            logger.error("Illegal port argument sent:", ie);
        } catch (Exception e) {
            logger.error("Error while creating Jmx connection Adapter:", e);
        }
        if(jmxConnectionAdapter == null)
            logger.info("JMX connection could not be established");
    }

    private int checkPortValidity(String portStr) throws IllegalArgumentException {
        int port = portStr != null ? Integer.parseInt(portStr) : -1;
        if (port < 0 || port > 0xFFFF) // Valid ports: 1 ==> 65535
            throw new IllegalArgumentException("port out of range:" + port);
        return port;
    }

    private String getPassword(Map server) {
        String encryptedPassword =  (String)server.get(Constants.ENCRYPTED_PASSWORD);
        if (!Strings.isNullOrEmpty(encryptedPassword)) {
            String encryptionKey = (String) configuration.getConfigYml().get(ENCRYPTION_KEY);
            server.put(ENCRYPTION_KEY, encryptionKey);
        }
        return CryptoUtils.getPassword(server);
    }

    public void onTaskComplete() {
        logger.debug("Completed the task for server {}", server.get(DISPLAY_NAME));
        metricWriteHelper.printMetric(metricPrefix + METRICS_SEPARATOR + HEARTBEAT, String.valueOf(heartBeatValue), "AVERAGE", "AVERAGE", "INDIVIDUAL");

    }
}
