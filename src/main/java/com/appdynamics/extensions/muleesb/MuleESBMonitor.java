/*
 * Copyright 2020. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.muleesb;

import com.appdynamics.extensions.ABaseMonitor;
import com.appdynamics.extensions.TasksExecutionServiceProvider;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.muleesb.config.Stats;
import com.appdynamics.extensions.muleesb.utils.Constants;
import com.appdynamics.extensions.util.AssertUtils;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;

public class MuleESBMonitor extends ABaseMonitor {
    private static final Logger logger = ExtensionsLoggerFactory.getLogger(MuleESBMonitor.class);

    protected String getDefaultMetricPrefix() {
        return Constants.METRIC_PREFIX;
    }

    public String getMonitorName() {
        return Constants.MonitorName;
    }

    protected void doRun(TasksExecutionServiceProvider tasksExecutionServiceProvider) {
        try {
            List<Map<String, ?>> muleServers = getServers();
            AssertUtils.assertNotNull(this.getContextConfiguration().getMetricsXml(), "Metrics xml not available");
            for (Map<String, ?> server : muleServers) {
                AssertUtils.assertNotNull(server, "The server arguments are empty");
                AssertUtils.assertNotNull(server.get(Constants.DISPLAY_NAME), "The displayName can not be null");
                logger.info("Starting the Mule ESB Monitoring Task for server : " + server.get(Constants.DISPLAY_NAME));
                MuleESBMonitorTask task = new MuleESBMonitorTask(getContextConfiguration(), tasksExecutionServiceProvider.getMetricWriteHelper(), server);
                tasksExecutionServiceProvider.submit((String) server.get(Constants.DISPLAY_NAME), task);
            }
        } catch (Exception e) {
            logger.error("Mule Esb servers Metrics collection failed", e);
        }

    }

    protected List<Map<String, ?>> getServers() {
        List<Map<String, ?>> servers = (List<Map<String, ?>>) getContextConfiguration().getConfigYml().get(Constants.SERVERS);
        AssertUtils.assertNotNull(servers, "The 'servers' section in conf.yml is not initialised");
        return servers;
    }

    protected void initializeMoreStuff(Map<String, String> args) {
        logger.info("initializing metric.xml file");
        this.getContextConfiguration().setMetricXml(args.get("metric-file"), Stats.class);
    }

//    public static void main(String[] args) throws TaskExecutionException {
//
//        Map<String, String> taskArgs = new HashMap<String, String>();
//        taskArgs.put(CONFIG_ARG, "src/main/resources/conf/config.yml");
//        taskArgs.put(METRIC_ARG, "src/main/resources/conf/metrics.xml");
//
//        MuleESBMonitor muleesbMonitor = new MuleESBMonitor();
//        muleesbMonitor.execute(taskArgs, null);
//    }
}