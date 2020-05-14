package com.appdynamics.extensions.muleesb.collector;
/*
 * Copyright 2020. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.muleesb.JMXConnectionAdapter;
import com.appdynamics.extensions.muleesb.collectors.InstanceProcessor;
import com.appdynamics.extensions.muleesb.config.MetricConfig;
import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import javax.management.Attribute;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class InstanceProcessorTest {
    private JMXConnectionAdapter jmxConnectionAdapter;
    JMXConnector jmxConnector = mock(JMXConnector.class);

    private ObjectInstance objectInstance;
    private InstanceProcessor instanceProcessor;
    private String[] metricNames = {"TotalMemory", "FreeMemory", "MaxMemory"};
    private List<Attribute> attributes;
    private List<Metric> metrics = Lists.newArrayList();
    @Before
    public void inititalize() throws MalformedObjectNameException, IOException {
        objectInstance = new ObjectInstance("Mule.default:name=MuleContext", "org.mule.module.management.mbean.MuleService");
            jmxConnectionAdapter =  mock(JMXConnectionAdapter.class);
        MetricConfig [] metricConfigs = new MetricConfig[3];
        MetricConfig metricConfig = new MetricConfig();
        metricConfig.setAttr("FreeMemory");
        metricConfig.setAlias("FreeMemory");
        metricConfigs[0] = metricConfig;
        MetricConfig metricConfig1 = new MetricConfig();
        metricConfig1.setAttr("TotalMemory");
        metricConfig1.setAlias("TotalMemory");
        MetricConfig metricConfig2 = new MetricConfig();
        metricConfig2.setAttr("MaxMemory");
        metricConfig2.setAlias("MaxMemory");
        metricConfigs[1] = metricConfig1;
        metricConfigs[2] = metricConfig2;
        jmxConnector = jmxConnectionAdapter.open();
        instanceProcessor = mock(InstanceProcessor.class);

        attributes = Lists.newArrayList();
        Attribute attribute = new Attribute("TotalMemory", 1020067840);
        Attribute attribute1 = new Attribute("FreeMemory", 846575192);
        Attribute attribute2 = new Attribute("MaxMemory", 1020067840);
        attributes.add(attribute);
        attributes.add(attribute1);
        attributes.add(attribute2);
        metrics.add(new Metric("TotalMemory", "1020067840", "Server|Component:<COMPONENT_ID>|Custom Metrics|Mule ESB|Mule Server|TotalMemory"));
        metrics.add(new Metric("FreeMemory", "846575192", "Server|Component:<COMPONENT_ID>|Custom Metrics|Mule ESB|Mule Server|FreeMemory"));
        metrics.add(new Metric("MaxMemory", "1020067840", "Server|Component:<COMPONENT_ID>|Custom Metrics|Mule ESB|Mule Server|MaxMemory"));
    }

    @Test
    public void processInstanceTest() throws IOException, IntrospectionException, InstanceNotFoundException, ReflectionException, MalformedObjectNameException {
        String[] collectedReadableAttributes = {"Copyright", "Stopped", "Initialised", "BuildNumber", "BuildDate", "ServerId", "JdkVersion", "Vendor", "FreeMemory", "MaxMemory", "TotalMemory", "Hostname", "HostIp", "License", "InstanceId", "ConfigBuilderClassName", "OsVersion", "StartTime", "Version"};

        List<String> jmxReadableAttributes = Arrays.asList(collectedReadableAttributes);
        when(jmxConnectionAdapter.getReadableAttributeNames(jmxConnector, objectInstance)).thenReturn(jmxReadableAttributes);
        when(jmxConnectionAdapter.getAttributes(jmxConnector, objectInstance.getObjectName(), metricNames)).thenReturn(attributes);
        when(instanceProcessor.processInstance(objectInstance)).thenReturn(metrics);
        List<Metric> collectedMetricsForInstance = instanceProcessor.processInstance(objectInstance);
        Assert.assertEquals(collectedMetricsForInstance.size(), 3);
        Assert.assertEquals(collectedMetricsForInstance.get(0).getMetricValue(), "1020067840");
    }

}
