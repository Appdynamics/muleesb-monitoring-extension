package com.appdynamics.extensions.muleesb.collectors;/*
 * Copyright 2019. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.muleesb.JMXConnectionAdapter;
import com.appdynamics.extensions.muleesb.config.MetricConfig;
import com.appdynamics.extensions.muleesb.filters.IncludeFilter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.slf4j.Logger;

import javax.management.Attribute;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.ObjectInstance;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeData;
import javax.management.remote.JMXConnector;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InstanceProcessor {

    private static final Logger logger = ExtensionsLoggerFactory.getLogger(InstanceProcessor.class);

    private JMXConnectionAdapter jmxAdapter;
    private JMXConnector jmxConnector;
    private MetricConfig[] metricConfigs;
    Map<String, MetricConfig> mbeanMetricsWithConfig;
    private String metricPrefix;

    public InstanceProcessor(JMXConnectionAdapter jmxAdapter, JMXConnector jmxConnector, MetricConfig[] metricConfigs, String metricPrefix) {
        this.jmxAdapter = jmxAdapter;
        this.jmxConnector = jmxConnector;
        this.metricConfigs = metricConfigs;
        this.metricPrefix = metricPrefix;
    }

    public List<Metric> processInstance(ObjectInstance instance) throws IntrospectionException, ReflectionException, InstanceNotFoundException, IOException {
        List<String> jmxReadableAttributes = jmxAdapter.getReadableAttributeNames(jmxConnector, instance);
        List<Attribute> attributes = fetchAttributes(instance, jmxReadableAttributes);
        String metricPath = metricPrefix;
        List<Metric> attributeMetrics = Lists.newArrayList();
        for (Attribute attr : attributes) {
            AttributeProcessor attributeProcessor = new AttributeProcessor();
            try {
                if (attr.getValue() instanceof CompositeData) {
                    attributeMetrics.addAll(attributeProcessor.processCompositeAttriubteToMetric(attr, mbeanMetricsWithConfig, metricPath));
                } else {
                    Metric metric = attributeProcessor.processAttributeToMetric(attr, mbeanMetricsWithConfig, metricPath);
                    if (metric != null)
                        attributeMetrics.add(metric);
                }
            } catch (Exception e) {
                logger.error("Error collecting value for {} {}", instance.getObjectName(), attr.getName(), e);
            }
        }
        logger.debug("collecting metrics for {} with attributes {}", instance, attributes);
        return attributeMetrics;
    }

    private List<Attribute> fetchAttributes(ObjectInstance instance, List<String> jmxReadableAttributes) throws InstanceNotFoundException, IOException, ReflectionException {
        mbeanMetricsWithConfig = buildMetricToCollectWithConfig(metricConfigs);
        Set<String> metricNamesToBeExtracted = applyFilters(mbeanMetricsWithConfig.keySet(), jmxReadableAttributes);
        return jmxAdapter.getAttributes(jmxConnector, instance.getObjectName(), metricNamesToBeExtracted.toArray(new String[metricNamesToBeExtracted.size()]));
    }

    private Map<String, MetricConfig> buildMetricToCollectWithConfig(MetricConfig[] metricConfigs) {

        Map<String, MetricConfig> metricsWithConfig = Maps.newHashMap();
        for (MetricConfig metricConfig : metricConfigs) {
            String metricName = metricConfig.getAttr();
            metricsWithConfig.put(metricName, metricConfig);
        }
        return metricsWithConfig;
    }

    private Set<String> applyFilters(Set<String> metricKeys, List<String> jmxReadableAttributes) {
        return new IncludeFilter(metricKeys).apply(jmxReadableAttributes);
    }
}
