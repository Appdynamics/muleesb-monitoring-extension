/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */
package com.appdynamics.extensions.muleesb.config;

import java.util.List;

public class MBeanDetails {

    private String domainMatcher;
    private List<String> types;
    private List<String> excludeDomains;
    private List<String> flows;

    MBeanDetails(String domainMatcher, List<String> types, List<String> excludeDomains, List<String> flows) {
        this.domainMatcher = domainMatcher;
        this.types = types;
        this.excludeDomains = excludeDomains;
        this.flows = flows;
    }

    public String getDomainMatcher() {
        return domainMatcher;
    }

    public List<String> getTypes() {
        return types;
    }

    public List<String> getExcludeDomains() {
        return excludeDomains;
    }

    public List<String> getFlows() {
        return flows;
    }

}
