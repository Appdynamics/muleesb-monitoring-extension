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
import static com.appdynamics.extensions.muleesb.collector.MetricsCollector.getMetricAfterCharacterReplacement;
import com.appdynamics.extensions.muleesb.utils.Constant;
import static com.appdynamics.extensions.muleesb.utils.Constant.ENCRYPTION_KEY;
import static com.appdynamics.extensions.muleesb.utils.Constant.HEARTBEAT;
import static com.appdynamics.extensions.muleesb.utils.Constant.METRICS_SEPARATOR;
import com.appdynamics.extensions.muleesb.utils.JMXUtil;
import com.appdynamics.extensions.util.CryptoUtils;
import com.google.common.collect.Lists;
import org.slf4j.Logger;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class MuleESBMonitorTask implements AMonitorTaskRunnable {
    private static final Logger logger = ExtensionsLoggerFactory.getLogger(MuleESBMonitorTask.class);
    private Map<String, ?> server;
    private MonitorContextConfiguration configuration;
    private MetricWriteHelper metricWriteHelper;
    private String metricPrefix;
    private BigInteger heartBeatValue = BigInteger.ZERO;

    public MuleESBMonitorTask(MonitorContextConfiguration monitorContextConfiguration, MetricWriteHelper metricWriteHelper, Map<String, ?> server) {
        this.configuration = monitorContextConfiguration;
        this.server = server;
        this.metricWriteHelper = metricWriteHelper;
        this.metricPrefix = configuration.getMetricPrefix() + METRICS_SEPARATOR + this.server.get("displayName") + METRICS_SEPARATOR;
    }

    public void run() {
        try {
            populateStats();
            heartBeatValue = BigInteger.ONE;
            logger.info("Completed the Mule esb Monitoring task");
        } catch (Exception e) {
            logger.error("Error while running the task " + server.get("displayName") + e);
       }
    }

    private void populateStats() {

        JMXConnector jmxConnector = null;
        MBeanServerConnection mBeanServerConnection = null;
        List<Metric> metrics = Lists.newArrayList();
        try {
            try {
                Map<String, String> requestMap = buildRequestMap();
                jmxConnector = JMXUtil.getJmxConnector(requestMap.get(Constant.HOST), Integer.parseInt(requestMap.get(Constant.PORT)), requestMap.get(Constant.USERNAME), requestMap.get(Constant.PASSWORD));
                mBeanServerConnection = jmxConnector.getMBeanServerConnection();
            } catch (IOException e) {
                logger.error("Error JMX-ing into Mule ESB Server ", e);
                metrics.add(new Metric(Constant.METRICS_COLLECTED, Constant.ERROR_VALUE, metricPrefix + Constant.METRICS_COLLECTED));
            }

            Map<String, ?> mbeanDetails = (Map<String, String>) server.get(Constant.MBEAN_DETAILS);

            String domainMatcher = (String) mbeanDetails.get(Constant.DOMAIN_MATCHER);
            List<String> types = (List<String>) mbeanDetails.get(Constant.TYPES);
            List<String> excludeDomains = (List<String>) mbeanDetails.get(Constant.EXCLUDE_DOMAINS);
            //Adding support for Flows filtering
            List<String> flows = (List<String>) mbeanDetails.get(Constant.FLOWS);

            for (String type : types) {
                String mbeanMatcher = buildMbeanMatcher(domainMatcher, type);
                try {
                    logger.debug("started processing the type : " + type + "with mbeanMatcher : " + mbeanMatcher);
                    Set<ObjectInstance> objectInstances = JMXUtil.queryMbeans(mBeanServerConnection, mbeanMatcher);
                    extractMetrics(mBeanServerConnection, objectInstances, excludeDomains, flows, metrics);
                    logger.debug("Successfully processed the type : " + type + "with mbeanMatcher : " + mbeanMatcher);
                } catch (IOException e) {
                    logger.error("Error getting bean with type :" + type, e);
                } catch (MalformedObjectNameException e) {
                    logger.error("Error getting bean with type :" + type, e);
                } catch (Exception e) {
                    logger.error("Error in Exception getting bean with type :" + type, e);
                }
            }
        } catch (Exception e) {
            logger.error("Error while collecting the metrics for: " + server.get(Constant.DISPLAY_NAME));
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



    private void extractMetrics(MBeanServerConnection mBeanServerConnection, Set<ObjectInstance> objectInstances, List<String> excludeDomains, List<String> flows, List<Metric> metrics) {
        List<Map<String, String>> metricReplacer = (List<Map<String, String>>) configuration.getConfigYml().get(Constant.METRIC_PATH_REPLACEMENTS);
        for (ObjectInstance objectInstance : objectInstances) {
            ObjectName objectName = objectInstance.getObjectName();
            String domain = objectName.getDomain();
            List<Metric> memoryMetrics = getMemoryMetrics(mBeanServerConnection, domain);
            metrics.addAll(memoryMetrics);
            logger.debug(objectName + " and processing for domain => " + domain);
            if (!isDomainExcluded(objectName, excludeDomains)) {
                try {
                    String objectNameSuffix = objectName.toString().split(",")[1];
                    Boolean isFlow = false;
                    String flow = "";
                    if (objectNameSuffix.contains("Flow")) {
                        isFlow = true;
                        flow = objectNameSuffix.split("=")[1];
                    }
                    if ((isFlow && isFlowIncluded(flows, flow)) || (!objectNameSuffix.equals("") && !isFlow)) {
                        try {
                            MBeanAttributeInfo[] attributes = mBeanServerConnection.getMBeanInfo(objectName).getAttributes();
                            for (MBeanAttributeInfo mBeanAttributeInfo : attributes) {
                                logger.debug("collecting metrics for the attribute : " + mBeanAttributeInfo);
                                Object attribute = mBeanServerConnection.getAttribute(objectName, mBeanAttributeInfo.getName());
                                String metricKey = getMetricAfterCharacterReplacement(getMetricsKey(objectName, objectNameSuffix, mBeanAttributeInfo), metricReplacer);
                                logger.debug("collecting metrics for domain: " + domain + " with " + attribute + " and  : metrickey : " + metricKey);
                                if (attribute != null && attribute instanceof Number) {
                                    metrics.add(new Metric(metricKey, attribute.toString(), metricPrefix + metricKey));
                                } else {
                                    logger.info("Excluded " + metricKey + " as its value can not be converted to number");
                                }
                            }
                        } catch (Exception e) {
                            logger.error("Failed to collect attributed for the flow : " + flow, e);
                        }
                    } else {
                        logger.info("Flow not found: " + flow + "metric path String " + objectNameSuffix);
                    }
                } catch (Exception e) {
                    logger.error("Unable to get info for object " + objectInstance.getObjectName(), e);
                }
            } else {
                logger.info("Excluding domain: " + domain + " as configured");
            }
        }
    }

    private boolean isFlowIncluded(List<String> flows, String flow) {
        for (String currFlow : flows) {
            Pattern p = Pattern.compile(currFlow);
            if (p.matcher(flow).matches())
                return true;
        }
        return false;
    }

    private boolean isDomainExcluded(ObjectName objectName, List<String> excludeDomains) {
        String domain = objectName.getDomain();
        return excludeDomains.contains(domain);
    }

    private String getMetricsKey(ObjectName objectName, String flowPath, MBeanAttributeInfo attr) {
        StringBuilder metricsKey = new StringBuilder();
        metricsKey.append(objectName.getDomain()).append(METRICS_SEPARATOR).append(flowPath).append(METRICS_SEPARATOR).append(attr.getName());
        return metricsKey.toString();
    }

    private String buildMbeanMatcher(String domainMatcher, String type) {
        StringBuilder sb = new StringBuilder(domainMatcher);
        sb.append(":").append("type=").append(type).append(",*");
        return sb.toString();
    }

    private List<Metric> getMemoryMetrics(MBeanServerConnection mBeanServerConnection, String muledomain) {
        List<Metric> memoryMetrics = new ArrayList<Metric>();
        muledomain += Constant.MULE_CONTEXT;
        try {
            Iterator<ObjectInstance> instanceIterator = mBeanServerConnection.queryMBeans(new ObjectName(muledomain), null).iterator();
            while (instanceIterator.hasNext()) {
                ObjectInstance objectInstance = instanceIterator.next();
                ObjectName objectName = objectInstance.getObjectName();
                Object freeMemory = mBeanServerConnection.getAttribute(objectName, "FreeMemory");
                memoryMetrics.add(new Metric("FreeMemory", freeMemory.toString(), metricPrefix + "FreeMemory"));
                Object maxMemory = mBeanServerConnection.getAttribute(objectName, "MaxMemory");
                memoryMetrics.add(new Metric("MaxMemory", maxMemory.toString(), metricPrefix + "MaxMemory"));
                Object totalMemory = mBeanServerConnection.getAttribute(objectName, "TotalMemory");
                memoryMetrics.add(new Metric("TotalMemory", totalMemory.toString(), metricPrefix + "TotalMemory"));
                logger.debug("successfully collected memory metrics for : " + mBeanServerConnection.toString());
            }
        } catch (Exception e) {
            logger.error("Unable to get memory stats", e);
        }
        return memoryMetrics;
    }


    private Map<String, String> buildRequestMap() {
        Map<String, String> requestMap = new HashMap();
        requestMap.put(Constant.HOST, (String) server.get(Constant.HOST));
        requestMap.put(Constant.PORT, String.valueOf(server.get(Constant.PORT)));
        requestMap.put(Constant.USERNAME, (String) server.get(Constant.USERNAME));
        requestMap.put(Constant.PASSWORD, getPassword((Map<String, String>) server));
        return requestMap;
    }


    private String getPassword(Map<String, String> server) {
        if (configuration.getConfigYml().get(ENCRYPTION_KEY) != null) {
            String encryptionKey = (String) configuration.getConfigYml().get(ENCRYPTION_KEY);
            server.put(ENCRYPTION_KEY, encryptionKey);
        }
        return CryptoUtils.getPassword(server);
    }

    public void onTaskComplete() {
        logger.debug("Task Complete");
        metricWriteHelper.printMetric(metricPrefix + METRICS_SEPARATOR + HEARTBEAT, String.valueOf(heartBeatValue), "AVERAGE", "AVERAGE", "INDIVIDUAL");

    }
}
