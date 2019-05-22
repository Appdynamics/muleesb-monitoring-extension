package com.appdynamics.extensions.muleesb.collectors;/*
 * Copyright 2019. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.muleesb.config.MetricConfig;
import static com.appdynamics.extensions.muleesb.utils.Constants.PERIOD;
import com.google.common.collect.Lists;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;

import javax.management.Attribute;
import javax.management.openmbean.CompositeData;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AttributeProcessor {

    private static final Logger logger = ExtensionsLoggerFactory.getLogger(AttributeProcessor.class);
    private ObjectMapper objectMapper = new ObjectMapper();

    public Metric processAttributeToMetric(Attribute attr, Map<String, MetricConfig> mbeanMetricsWithConfig, String metricPath) {
        Metric metric = null;
        try {
            String attrName = attr.getName();
            Object value = attr.getValue();
            metric = collectAttributeMetric(attrName, value, mbeanMetricsWithConfig, metricPath);
        } catch (Exception e) {
            logger.error("Error collecting value for {} ", attr.getName(), e);
        } finally {
            return metric;
        }
    }

    private Metric collectAttributeMetric(String attrName, Object value, Map<String, MetricConfig> mbeanMetricsWithConfig, String metricPath) {
        if (value != null) {
            MetricConfig config = mbeanMetricsWithConfig.get(attrName);
            return new Metric(attrName, String.valueOf(value), metricPath + config.getAlias(), objectMapper.convertValue(config, Map.class));
        } else {
            logger.warn("Ignoring metric {} with path {} as the value is null", attrName, metricPath);
        }
        return null;
    }

    public List<Metric> processCompositeAttriubteToMetric(Attribute attr, Map<String, MetricConfig> mbeanMetricsConfig, String metricPath) {
        List<Metric> metrics = Lists.newArrayList();
        String attributeName = attr.getName();
        CompositeData metricValue = (CompositeData) attr.getValue();
        Set<String> attributesFound = metricValue.getCompositeType().keySet();
        for (String str : attributesFound) {
            String key = attributeName + PERIOD + str;
            if (mbeanMetricsConfig.containsKey(key)) {
                Object attributeValue = metricValue.get(str);
                metrics.add(collectAttributeMetric(key, attributeValue, mbeanMetricsConfig, metricPath));
            }
        }
        return metrics;
    }
}
