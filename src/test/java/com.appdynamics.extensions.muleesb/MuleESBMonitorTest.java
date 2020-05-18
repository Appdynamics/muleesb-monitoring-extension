package com.appdynamics.extensions.muleesb;

import com.google.common.collect.Maps;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.junit.Test;

import java.util.Map;

public class MuleESBMonitorTest {
    @Test
    public void test() throws TaskExecutionException {
        MuleESBMonitor monitor = new MuleESBMonitor();
        Map<String, String> taskArgs = Maps.newHashMap();
        taskArgs.put("config-file", "src/test/resources/conf/config.yml");
        taskArgs.put("metric-file", "src/test/resources/conf/metrics.xml");
        monitor.execute(taskArgs, null);
    }
}