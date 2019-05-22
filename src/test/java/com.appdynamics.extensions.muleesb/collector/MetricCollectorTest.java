package com.appdynamics.extensions.muleesb.collector;/*
 * Copyright 2019. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.muleesb.ConfigTestUtil;
import com.appdynamics.extensions.muleesb.JMXConnectionAdapter;
import com.appdynamics.extensions.muleesb.collectors.MetricsCollector;
import com.appdynamics.extensions.util.AssertUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public class MetricCollectorTest {

    private MetricsCollector metricsCollector = mock(MetricsCollector.class);
    private JMXConnectionAdapter jmxConnectionAdapter = mock(JMXConnectionAdapter.class);
    private JMXConnector jmxConnector;
    private Set<ObjectInstance> objectInstances = Sets.newHashSet();
    private List<String> excludeDomains = Lists.newArrayList();
    private List<String> flows = Lists.newArrayList();
    private List<Metric> metrics;

    @Before
    public void inititalize() throws MalformedObjectNameException, IOException {
        objectInstances.add(new ObjectInstance("Mule.default:type=org.mule.Statistics,Application=\"application totals\"", "org.mule.module.management.mbean.FlowConstructStats"));
        flows.add(".*");
        jmxConnector = jmxConnectionAdapter.open();
        metrics = ConfigTestUtil.readAllMetrics("src/test/resources/conf/mbeanMetrics.txt");
    }

    @Test
    public void initMetricsCollectionValidityTest() throws IOException {
        ObjectName objectName = mock(ObjectName.class);
        when(jmxConnectionAdapter.queryMBeans(jmxConnector, objectName)).thenReturn(null);
        when(metricsCollector.initMetricsCollection(objectInstances, excludeDomains, flows)).thenReturn(metrics);
        List<Metric> metrics = metricsCollector.initMetricsCollection(objectInstances, excludeDomains, flows);
        AssertUtils.assertNotNull(metrics, "collected metrics");
    }

    @Test
    public void initMetricsCollectionTest() throws IOException {
        ObjectName objectName = mock(ObjectName.class);
        when(jmxConnectionAdapter.queryMBeans(jmxConnector, objectName)).thenReturn(null);
        when(metricsCollector.initMetricsCollection(objectInstances, excludeDomains, flows)).thenReturn(metrics);
        List<Metric> metrics = metricsCollector.initMetricsCollection(objectInstances, excludeDomains, flows);
        Assert.assertEquals(metrics.size(), 10);
    }

}
