package com.appdynamics.extensions.muleesb.config;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class Stat {
    @XmlAttribute
    private String name;
    @XmlElement(name = "metric")
    private MetricConfig[] metricConfigs;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public MetricConfig[] getMetricConfig() {
        return metricConfigs;
    }

    public void setMetricConfig(MetricConfig[] metricConfigs) {
        this.metricConfigs = metricConfigs;
    }
}
