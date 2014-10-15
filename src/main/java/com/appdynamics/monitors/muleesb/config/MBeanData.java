/**
 * Copyright 2014 AppDynamics, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
