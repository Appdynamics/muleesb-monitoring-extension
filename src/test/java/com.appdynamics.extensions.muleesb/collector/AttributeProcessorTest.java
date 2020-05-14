package com.appdynamics.extensions.muleesb.collector;

/*
 * Copyright 2020. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

import com.appdynamics.extensions.muleesb.collectors.AttributeProcessor;
import com.appdynamics.extensions.muleesb.config.MetricConfig;
import org.junit.Assert;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;

import javax.management.Attribute;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AttributeProcessorTest {
    Attribute compositeAttribute;

    @Test
    public void processAttributeToMetricTest() throws IOException{
        Attribute attribute = new Attribute("usage", 60);
        MetricConfig config = new MetricConfig();
        config.setAttr("usage");
        Map<String, MetricConfig> attrMap = new HashMap<>();
        attrMap.put("usage", config);
        AttributeProcessor attributeProcessor = PowerMockito.spy(new AttributeProcessor());
        Assert.assertEquals((attributeProcessor.processAttributeToMetric(attribute, attrMap, null)).getMetricValue(), "60");

    }

    @Test
    public void processCompositeAttriubteToMetricTest() throws IOException{
        compositeAttribute = new Attribute("usage", 60);
        MetricConfig config = new MetricConfig();
        config.setAttr("usage");
        Map<String, MetricConfig> attrMap = new HashMap<>();
        attrMap.put("usage", config);
        AttributeProcessor attributeProcessor = PowerMockito.spy(new AttributeProcessor());
        Assert.assertEquals((attributeProcessor.processAttributeToMetric(compositeAttribute, attrMap, null)).getMetricValue(), "60");

    }

}
