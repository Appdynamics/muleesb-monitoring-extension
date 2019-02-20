/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.monitors.muleesb;

import com.appdynamics.extensions.PathResolver;
import com.appdynamics.monitors.muleesb.config.ConfigUtil;
import com.appdynamics.monitors.muleesb.config.Configuration;
import com.appdynamics.monitors.muleesb.config.MBeanData;
import com.appdynamics.monitors.muleesb.config.MuleESBMonitorConstants;
import com.appdynamics.monitors.muleesb.config.Server;
import com.google.common.base.Strings;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.apache.log4j.Logger;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class MuleESBMonitor extends AManagedMonitor {
    private static final Logger logger = Logger.getLogger(MuleESBMonitor.class);

    public static final String METRICS_SEPARATOR = "|";
    private static final String CONFIG_ARG = "config-file";
    private static final String FILE_NAME = "monitors/MuleESBMonitor/config.yml";

    private static final ConfigUtil<Configuration> configUtil = new ConfigUtil<Configuration>();

    public MuleESBMonitor() {
        String details = MuleESBMonitor.class.getPackage().getImplementationTitle();
        String msg = "Using Monitor Version [" + details + "]";
        logger.info(msg);
        System.out.println(msg);
    }

    public TaskOutput execute(Map<String, String> taskArgs, TaskExecutionContext taskExecutionContext) throws TaskExecutionException {
        if (taskArgs != null) {
            logger.info("Starting the Mule ESB Monitoring task.");
            String configFilename = getConfigFilename(taskArgs.get(CONFIG_ARG));
            try {
                Configuration config = configUtil.readConfig(configFilename, Configuration.class);
                Map<String, Number> metrics = populateStats(config);
                metrics.put(MuleESBMonitorConstants.METRICS_COLLECTED, MuleESBMonitorConstants.SUCCESS_VALUE);
                printStats(config, metrics);
                logger.info("Completed the Mule ESB Monitoring Task successfully");
                return new TaskOutput("Mule ESB Monitor executed successfully");
            } catch (FileNotFoundException e) {
                logger.error("Config File not found: " + configFilename, e);
            } catch (Exception e) {
                logger.error("Metrics Collection Failed: ", e);
            }
        }
        throw new TaskExecutionException("Mule ESB Monitor completed with failures");
    }

    private Map<String, Number> populateStats(Configuration config) {

        Map<String, Number> metrics = new HashMap<String, Number>();
        Server server = config.getServer();
        JMXConnector jmxConnector = null;
        MBeanServerConnection mBeanServerConnection = null;
        try {
            try {
                jmxConnector = JMXUtil.getJmxConnector(server.getHost(), server.getPort(), server.getUsername(), server.getPassword());
                mBeanServerConnection = jmxConnector.getMBeanServerConnection();
            } catch (IOException e) {
                logger.error("Error JMX-ing into Mule ESB Server ", e);
                metrics.put(MuleESBMonitorConstants.METRICS_COLLECTED, MuleESBMonitorConstants.ERROR_VALUE);
                return metrics;
            }

            MBeanData mbeans = config.getMbeans();

            String domainMatcher = mbeans.getDomainMatcher();
            Set<String> types = mbeans.getTypes();
            Set<String> excludeDomains = mbeans.getExcludeDomains();
            //Adding support for Flows filtering
            Set<String> flows = mbeans.getFlows();

            for (String type : types) {
                String mbeanMatcher = buildMbeanMatcher(domainMatcher, type);
                try {
                    logger.debug("started processing the type : " + type + "with mbeanMatcher : " + mbeanMatcher);
                    Set<ObjectInstance> objectInstances = JMXUtil.queryMbeans(mBeanServerConnection, mbeanMatcher);
                    Map<String, Number> curMetrics = extractMetrics(mBeanServerConnection, objectInstances, excludeDomains, flows);
                    metrics.putAll(curMetrics);
                    logger.debug("Successfully processed the type : " + type + "with mbeanMatcher : " + mbeanMatcher);
                } catch (IOException e) {
                    logger.error("Error getting bean with type :" + type, e);
                } catch (MalformedObjectNameException e) {
                    logger.error("Error getting bean with type :" + type, e);
                } catch (Exception e) {
                    logger.error("Error in Exception getting bean with type :" + type, e);
                }
            }
        } finally {
            if (jmxConnector != null) {
                try {
                    jmxConnector.close();
                } catch (IOException e) {
                    logger.error("Unable to close the connection", e);
                }
            }
        }
        return metrics;
    }

    private Map<String, Number> getMemoryMetrics(MBeanServerConnection mBeanServerConnection) {
        Map<String, Number> memoryMetrics = new HashMap<String, Number>();
        try {
            Set<ObjectInstance> mBeans = mBeanServerConnection.queryMBeans(new ObjectName("Mule.default:name=MuleContext,*"), null);
            Iterator<ObjectInstance> iterator = mBeans.iterator();
            if (iterator.hasNext()) {
                ObjectInstance objectInstance = iterator.next();
                ObjectName objectName = objectInstance.getObjectName();
                Object freeMemory = mBeanServerConnection.getAttribute(objectName, "FreeMemory");
                memoryMetrics.put("FreeMemory", (Number) freeMemory);
                Object maxMemory = mBeanServerConnection.getAttribute(objectName, "MaxMemory");
                memoryMetrics.put("MaxMemory", (Number) maxMemory);
                Object totalMemory = mBeanServerConnection.getAttribute(objectName, "TotalMemory");
                memoryMetrics.put("TotalMemory", (Number) totalMemory);
                logger.debug("successfully collected memory metrics for : " + mBeanServerConnection.toString());
            }
        } catch (Exception e) {
            logger.error("Unable to get memory stats", e);
        }
        return memoryMetrics;
    }

    private String buildMbeanMatcher(String domainMatcher, String type) {
        StringBuilder sb = new StringBuilder(domainMatcher);
        sb.append(":").append("type=").append(type).append(",*");
        return sb.toString();
    }

    private Map<String, Number> extractMetrics(MBeanServerConnection mBeanServerConnection, Set<ObjectInstance> objectInstances, Set<String> excludeDomains, Set<String> flows) {
        Map<String, Number> metrics = new HashMap<String, Number>();
        Map<String, Number> memoryMetrics = getMemoryMetrics(mBeanServerConnection);
        metrics.putAll(memoryMetrics);
        for (ObjectInstance objectInstance : objectInstances) {
            ObjectName objectName = objectInstance.getObjectName();
            String domain = objectName.getDomain();
            logger.debug(objectName + " and processing for domain => " + domain);
            if (!isDomainExcluded(objectName, excludeDomains)) {
                try {
                    String objectNameSuffix = objectName.toString().split(",")[1];
                    Boolean isFlow = false;
                    String metricPathStr;
                    String flow = "";
                    if (objectNameSuffix.contains("Flow")) {
                        isFlow = true;
                        flow = objectNameSuffix.split("=")[1];
                        metricPathStr = objectNameSuffix.split("=")[1].replaceAll("[:/\"]", "");
                    } else {
                        metricPathStr = objectNameSuffix.split("=")[1].replaceAll("[:/\"]", "");
                    }
                    if ((isFlow && isFlowIncluded(flows, flow)) || (!metricPathStr.equals("") && !isFlow)) {
                        try {
                            MBeanAttributeInfo[] attributes = mBeanServerConnection.getMBeanInfo(objectName).getAttributes();
                            for (MBeanAttributeInfo mBeanAttributeInfo : attributes) {
                                logger.debug("collecting metrics for the attribute : " + mBeanAttributeInfo);
                                Object attribute = mBeanServerConnection.getAttribute(objectName, mBeanAttributeInfo.getName());
                                String metricKey = getMetricsKey(objectName, metricPathStr, mBeanAttributeInfo);
                                logger.debug("collecting metrics for domain: " + domain + " with " + attribute + " and  : metrickey : " + metricKey);
                                if (attribute != null && attribute instanceof Number) {
                                    metrics.put(metricKey, (Number) attribute);
                                } else {
                                    logger.info("Excluded " + metricKey + " as its value can not be converted to number");
                                }
                            }
                        } catch (Exception e) {
                            logger.error("Failed to collect attribubted for the flow : " + flow, e);
                        }
                    } else {
                        logger.info("Flow not found: " + flow + "metric path String " + metricPathStr);
                    }
                } catch (Exception e) {
                    logger.error("Unable to get info for object " + objectInstance.getObjectName(), e);
                }
            } else {
                logger.info("Excluding domain: " + domain + " as configured");
            }
        }
        return metrics;
    }

    private boolean isFlowIncluded(Set<String> flows, String flow) {
        for (String currFlow : flows) {
            Pattern p = Pattern.compile(currFlow);
            if (p.matcher(flow).matches())
                return true;
        }
        return false;
    }

    private boolean isDomainExcluded(ObjectName objectName, Set<String> excludeDomains) {
        String domain = objectName.getDomain();
        return excludeDomains.contains(domain);
    }

    private String getMetricsKey(ObjectName objectName, String flowPath, MBeanAttributeInfo attr) {
        StringBuilder metricsKey = new StringBuilder();
        metricsKey.append(objectName.getDomain()).append(METRICS_SEPARATOR).append(flowPath).append(METRICS_SEPARATOR).append(attr.getName());
        return metricsKey.toString();
    }

    private String getConfigFilename(String filename) {
        if (filename == null) {
            return "";
        }

        if ("".equals(filename)) {
            filename = FILE_NAME;
        }
        // for absolute paths
        if (new File(filename).exists()) {
            return filename;
        }
        // for relative paths
        File jarPath = PathResolver.resolveDirectory(AManagedMonitor.class);
        String configFileName = "";
        if (!Strings.isNullOrEmpty(filename)) {
            configFileName = jarPath + File.separator + filename;
        }
        return configFileName;
    }

    private void printStats(Configuration config, Map<String, Number> metrics) {
        String metricPath = config.getMetricPrefix();
        for (Map.Entry<String, Number> entry : metrics.entrySet()) {
            printMetric(metricPath + entry.getKey(), entry.getValue());
        }
    }

    private void printMetric(String metricPath, Number metricValue) {
        printMetric(metricPath, metricValue, MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE, MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
                MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);
    }

    private void printMetric(String metricPath, Number metricValue, String aggregation, String timeRollup, String cluster) {
        MetricWriter metricWriter = super.getMetricWriter(metricPath, aggregation, timeRollup, cluster);
        if (metricValue != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Metric [" + metricPath + " = " + metricValue + "]");
            }
            if (metricValue instanceof Double) {
                metricWriter.printMetric(String.valueOf(Math.round((Double) metricValue)));
            } else if (metricValue instanceof Float) {
                metricWriter.printMetric(String.valueOf(Math.round((Float) metricValue)));
            } else {
                metricWriter.printMetric(String.valueOf(metricValue));
            }
        }
    }

    public static void main(String[] args) throws TaskExecutionException {

        Map<String, String> taskArgs = new HashMap<String, String>();
        taskArgs.put(CONFIG_ARG, "src/main/resources/config/config.yml");

        com.appdynamics.monitors.muleesb.MuleESBMonitor muleesbMonitor = new com.appdynamics.monitors.muleesb.MuleESBMonitor();
        muleesbMonitor.execute(taskArgs, null);
    }
}