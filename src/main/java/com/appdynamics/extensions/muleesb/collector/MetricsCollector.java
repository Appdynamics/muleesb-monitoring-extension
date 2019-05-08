package com.appdynamics.extensions.muleesb.collector;

/*
 * Copyright 2019. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.muleesb.MuleESBMonitorTask;
import com.appdynamics.extensions.muleesb.utils.Constant;
import static com.appdynamics.extensions.muleesb.utils.Constant.METRICS_SEPARATOR;
import org.slf4j.Logger;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class MetricsCollector {
    private static final Logger logger = ExtensionsLoggerFactory.getLogger(MuleESBMonitorTask.class);

    private static final String REPLACE = "replace";
    private static final String REPLACE_WITH = "replaceWith";
    private MonitorContextConfiguration configuration;
    private String metricPrefix;
    MetricsCollector(MonitorContextConfiguration configuration, String metricPrefix){
        this.configuration = configuration;
        this.metricPrefix = metricPrefix;
    }

    private void extractMetrics(MBeanServerConnection mBeanServerConnection, Set<ObjectInstance> objectInstances, List<String> excludeDomains, List<String> flows, List<Metric> metrics) {
        List<Map<String, String>> metricReplacer = (List<Map<String, String>>) configuration.getConfigYml().get(Constant.METRIC_PATH_REPLACEMENTS);
        for (ObjectInstance objectInstance : objectInstances) {
            ObjectName objectName = objectInstance.getObjectName();
            String domain = objectName.getDomain();
//            List<Metric> memoryMetrics = getMemoryMetrics(mBeanServerConnection, domain);
//            metrics.addAll(memoryMetrics);
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
                            logger.error("Failed to collect attribubted for the flow : " + flow, e);
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

    private boolean isDomainExcluded(ObjectName objectName, List<String> excludeDomains) {
        String domain = objectName.getDomain();
        return excludeDomains.contains(domain);
    }

    private boolean isFlowIncluded(List<String> flows, String flow) {
        for (Object currFlow : flows) {
            Pattern p = Pattern.compile((String) currFlow);
            if (p.matcher(flow).matches())
                return true;
        }
        return false;
    }

    public static String getMetricAfterCharacterReplacement(String replaceTextHere, List<Map<String, String>> metricReplacer) {

        for (Map chars : metricReplacer) {
            String replace = (String) chars.get(REPLACE);
            String replaceWith = (String) chars.get(REPLACE_WITH);

            if (replaceTextHere.contains(replace)) {
                replaceTextHere = replaceTextHere.replaceAll(replace, replaceWith);
            }
        }
        return replaceTextHere;
    }

    private String getMetricsKey(ObjectName objectName, String flowPath, MBeanAttributeInfo attr) {
        StringBuilder metricsKey = new StringBuilder();
        metricsKey.append(objectName.getDomain()).append(METRICS_SEPARATOR).append(flowPath).append(METRICS_SEPARATOR).append(attr.getName());
        return metricsKey.toString();
    }

}
