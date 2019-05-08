package com.appdynamics.extensions.muleesb.config;/*
 * Copyright 2019. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

import com.google.common.collect.Maps;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class MetricsDetails {
    private String name;
    private String metricType;
    private Map<String, ?> metricsProperties;
    private Map<String, List<MetricsDetails>> metricTypesMap = Maps.newHashMap();

    MetricsDetails(String name, String metricType){
        this.name = name;
        this.metricType = metricType;
        addToTypesMap();
    }

    MetricsDetails(String name, String metricType, Map<String, ?> metricsProperties){
        this.name = name;
        this.metricType = metricType;
        this.metricsProperties = metricsProperties;
        addToTypesMap();
    }

    private void addToTypesMap(){
        if (metricTypesMap.containsKey(this.metricType) == false) {
            metricTypesMap.put(this.metricType, Arrays.asList(this));
        } else {
            metricTypesMap.get(this.metricType).add(this);
        }
    }

}
