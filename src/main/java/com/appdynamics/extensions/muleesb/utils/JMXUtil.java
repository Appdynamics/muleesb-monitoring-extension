/*
 *   Copyright 2020 . AppDynamics LLC and its affiliates.
 *   All Rights Reserved.
 *   This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *   The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.muleesb.utils;

import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.muleesb.config.Stat;
import com.appdynamics.extensions.muleesb.config.Stats;
import static com.appdynamics.extensions.muleesb.utils.Constants.METRICS_SEPARATOR;

import javax.management.ObjectName;

/**
 * Created by Prashant Mehta on 2/26/18.
 */
public class JMXUtil {

    public static boolean isCompositeObject(String objectName) {
        return (objectName.indexOf('.') != -1);
    }

    public static String getMetricNameFromCompositeObject(String objectName) {
        return objectName.split("\\.")[0];
    }

    public static Stat getAptMetricConfigAttr(MonitorContextConfiguration configuration, String metricType) {
        Stats stats = (Stats) configuration.getMetricsXml();
        for (Stat stat : stats.getStat()) {
            if (stat.getName().equals(metricType))
                return stat;
        }
        return null;
    }

    public static String getMetricsSuffixKey(ObjectName objectName, String flowPath) {
        StringBuilder metricsKey = new StringBuilder();
        metricsKey.append(objectName.getDomain()).append(METRICS_SEPARATOR).append(flowPath).append(METRICS_SEPARATOR);
        return metricsKey.toString();
    }
}
