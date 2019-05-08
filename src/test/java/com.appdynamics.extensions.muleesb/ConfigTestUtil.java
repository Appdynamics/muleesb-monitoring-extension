/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.muleesb;

import com.appdynamics.extensions.AMonitorJob;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import org.mockito.Mockito;

import java.io.File;

public class ConfigTestUtil {

    public static MonitorContextConfiguration getContextConfiguration(String configPath) {
        MonitorContextConfiguration configuration = new MonitorContextConfiguration("MuleESBBMonitor", "Custom Metrics|Mule ESB|", Mockito.mock(File.class), Mockito.mock(AMonitorJob.class));
        if (configPath != null)
            configuration.setConfigYml(configPath);
        return configuration;
    }

}
