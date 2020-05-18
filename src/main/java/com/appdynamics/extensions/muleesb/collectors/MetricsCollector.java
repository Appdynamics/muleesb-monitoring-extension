package com.appdynamics.extensions.muleesb.collectors;

/*
 * Copyright 2020. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.muleesb.JMXConnectionAdapter;
import com.appdynamics.extensions.muleesb.config.Stat;
import com.appdynamics.extensions.muleesb.utils.Constants;
import static com.appdynamics.extensions.muleesb.utils.Constants.EVENT_METRICS;
import static com.appdynamics.extensions.muleesb.utils.Constants.MEMORY_METRICS;
import com.appdynamics.extensions.muleesb.utils.JMXUtil;
import com.google.common.collect.Lists;
import org.slf4j.Logger;

import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class MetricsCollector {
    private static final Logger logger = ExtensionsLoggerFactory.getLogger(MetricsCollector.class);

    private MonitorContextConfiguration configuration;
    private JMXConnectionAdapter jmxConnectionAdapter;
    private JMXConnector jmxConnector;
    private String metricPrefix;

    public MetricsCollector(MonitorContextConfiguration configuration, JMXConnectionAdapter jmxConnectionAdapter, JMXConnector jmxConnector, String metricPrefix) {
        this.configuration = configuration;
        this.jmxConnectionAdapter = jmxConnectionAdapter;
        this.jmxConnector = jmxConnector;
        this.metricPrefix = metricPrefix;
    }


    public List<Metric> initMetricsCollection(Set<ObjectInstance> objectInstances, List<String> excludeDomains, List<String> flows) {
        List<Metric> metrics = Lists.newArrayList();

        for (ObjectInstance instance : objectInstances) {
            ObjectName objectName = instance.getObjectName();
            String domain = objectName.getDomain();
            metrics.addAll(getMemoryMetrics(jmxConnector, domain));
            logger.debug(objectName + " and processing for domain => " + domain);

            if (!isDomainExcluded(objectName, excludeDomains)) {
                try {//  - ObjectInstance: Mule.test-management-api-1:type=org.mule.Statistics,Flow="get:/apistatus:test"
//
                    Boolean isFlow = false;
                    String flow = "";
                    if (objectName.toString().contains("Flow")) {
                        isFlow = true;
                        String[] objectNameSubStrings = objectName.toString().split(",");
                        //Looping over all the substring to find Flow and process.
                        for (String objectNameSubString : objectNameSubStrings)
                            if (objectNameSubString.contains("Flow"))
                                flow = objectNameSubString.split("=")[1];
                    }

                    if ((isFlow && isFlowIncluded(flows, flow)) ||  !isFlow) {
                        try {
                            InstanceProcessor instanceProcessor = new InstanceProcessor(jmxConnectionAdapter, jmxConnector, JMXUtil.getAptMetricConfigAttr(configuration, EVENT_METRICS).getMetricConfig(), metricPrefix + JMXUtil.getMetricsSuffixKey(objectName, flow));
                            logger.debug("collecting metrics for domain: " + domain);
                            metrics.addAll(instanceProcessor.processInstance(instance));
                        } catch (Exception e) {
                            logger.error("Failed to collect attributes for the flow : " + flow, e);
                        }
                    } else {
                        logger.info("Flow not found: " + flow + "metric path String " + flow);
                    }
                } catch (Exception e) {
                    logger.error("Unable to get info for object " + instance.getObjectName(), e);
                }
            } else {
                logger.info("Excluding domain: " + domain + " as configured");
            }

        }
        return metrics;
    }

    private List<Metric> getMemoryMetrics(JMXConnector jmxConnector, String muledomain) {
        List<Metric> memoryMetrics = new ArrayList<Metric>();
        muledomain += Constants.MULE_CONTEXT;
        try {
            Stat memoryStat = JMXUtil.getAptMetricConfigAttr(configuration, MEMORY_METRICS);
            Set<ObjectInstance> instances = jmxConnectionAdapter.queryMBeans(jmxConnector, ObjectName.getInstance(muledomain));
            for (ObjectInstance instance : instances) {
                InstanceProcessor instanceProcessor = new InstanceProcessor(jmxConnectionAdapter, jmxConnector, memoryStat.getMetricConfig(), metricPrefix);
                memoryMetrics.addAll(instanceProcessor.processInstance(instance));
                logger.debug("successfully collected memory metrics for : " + jmxConnector.toString());
            }
        } catch (Exception e) {
            logger.error("Unable to get memory stats", e);
        }
        return memoryMetrics;
    }

    private boolean isDomainExcluded(ObjectName objectName, List<String> excludeDomains) {
        String domain = objectName.getDomain();
        return excludeDomains.contains(domain);
    }

    private boolean isFlowIncluded(List<String> flows, String flow) {
        for (String currFlow : flows) {
            Pattern p = Pattern.compile(currFlow);
            if (p.matcher(flow).matches())
                return true;
        }
        return false;
    }


}
