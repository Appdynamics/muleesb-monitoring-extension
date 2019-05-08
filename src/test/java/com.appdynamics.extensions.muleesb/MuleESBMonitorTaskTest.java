package com.appdynamics.extensions.muleesb;

import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.TasksExecutionServiceProvider;
import com.appdynamics.extensions.conf.MonitorContext;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.Mockito.when;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RunWith(PowerMockRunner.class)
@PrepareForTest(EntityUtils.class)
@PowerMockIgnore("javax.net.ssl.*")
class MuleESBMonitorTaskTest {

    @Mock
    private TasksExecutionServiceProvider serviceProvider;
    @Mock
    private MetricWriteHelper metricWriter;
    @Mock
    private MonitorContext context;
    @Mock
    private MonitorContextConfiguration configuration;
    @Mock
    private CloseableHttpClient client;
    @Mock
    private CloseableHttpResponse response;
    @Mock
    private HttpEntity entity;
    @Mock
    private Header header;
    Map config;
    private ArgumentCaptor<List> pathCaptor = ArgumentCaptor.forClass(List.class);

    @Before
    public void setup() throws IOException {
        MonitorContextConfiguration contextConfiguration = ConfigTestUtil.getContextConfiguration("src/test/resources/config.yml");
        config = contextConfiguration.getConfigYml();
        PowerMockito.mockStatic(EntityUtils.class);
        Object metricsXml = contextConfiguration.getMetricsXml();
        when(configuration.getContext()).thenReturn(context);
        when(configuration.getConfigYml()).thenReturn(config);
        when(configuration.getMetricsXml()).thenReturn(metricsXml);
        when(context.getHttpClient()).thenReturn(client);
        when(client.execute(Mockito.any(HttpGet.class))).thenReturn(response);
        when(response.getEntity()).thenReturn(entity);
        when(response.getFirstHeader(Mockito.anyString())).thenReturn(header);
        when(header.getValue()).thenReturn("text/plain");
    }

    @Test
    public void checkPopulateStatsCollectedMetrics(){

    }
}