/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */
package com.appdynamics.monitors.muleesb.config;

import java.util.HashSet;
import java.util.Set;

public class MBeanData {

    private String domainMatcher;
    private Set<String> types = new HashSet<String>();
    private Set<String> excludeDomains = new HashSet<String>();

    public String getDomainMatcher() {
        return domainMatcher;
    }

    public void setDomainMatcher(String domainMatcher) {
        this.domainMatcher = domainMatcher;
    }

    public Set<String> getTypes() {
        return types;
    }

    public void setTypes(Set<String> types) {
        this.types = types;
    }

    public Set<String> getExcludeDomains() {
        return excludeDomains;
    }

    public void setExcludeDomains(Set<String> excludeDomains) {
        this.excludeDomains = excludeDomains;
    }
}
