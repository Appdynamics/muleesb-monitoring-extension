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
package com.appdynamics.monitors.muleesb;

import com.google.common.base.Strings;
import org.apache.commons.lang.text.StrSubstitutor;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class JMXUtil {

    private static final String JMX_URL = "service:jmx:rmi:///jndi/rmi://${HOST}:${PORT}/jmxrmi";

    public static JMXConnector getJmxConnector(String host, int port, String username, String password) throws IOException {
        return connect(host, port, username, password);
    }

    private static JMXConnector connect(String host, int port, String username, String password) throws IOException {
        String jmxUrl = buildUrl(host, port);
        JMXServiceURL url = new JMXServiceURL(jmxUrl);
        final Map<String, Object> env = new HashMap<String, Object>();
        JMXConnector connector = null;
        if (!Strings.isNullOrEmpty(username)) {
            env.put(JMXConnector.CREDENTIALS, new String[]{username, password});
            connector = JMXConnectorFactory.connect(url, env);
        } else {
            connector = JMXConnectorFactory.connect(url);
        }
        return connector;
    }

    private static String buildUrl(String host, int port) {
        Map<String, String> valueMap = new HashMap<String, String>();
        valueMap.put("HOST", host);
        valueMap.put("PORT", String.valueOf(port));
        StrSubstitutor strSubstitutor = new StrSubstitutor(valueMap);
        return strSubstitutor.replace(JMX_URL);
    }

    public static Set<ObjectInstance> queryMbeans(MBeanServerConnection connection, String mbeanMatcher) throws IOException, MalformedObjectNameException {
        Set<ObjectInstance> mBeans = connection.queryMBeans(new ObjectName(mbeanMatcher), null);
        return mBeans;
    }
}