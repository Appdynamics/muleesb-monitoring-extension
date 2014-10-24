# AppDynamics Mule ESB Monitoring Extension

This extension works only with the standalone machine agent.

##Use Case

Mule ESB is a lightweight Java-based enterprise service bus (ESB) and integration platform that allows developers to connect applications together quickly and easily, enabling them to exchange data. 
This extension monitors Mule ESB instance and collects useful statistics exposed through MBeans and reports to AppDynamics Controller.

##Prerequisites

To use this extension please configure JMX in Mule ESB instance. To configure JMX please add following properties to the wrapper.conf of the Mule instance.
 
 ```
     ###JMX Connection Properties###
     wrapper.java.additional.5=-Dcom.sun.management.jmxremote
     wrapper.java.additional.6=-Dcom.sun.management.jmxremote.port=9000
     wrapper.java.additional.7=-Dcom.sun.management.jmxremote.authenticate=false
     wrapper.java.additional.8=-Dcom.sun.management.jmxremote.ssl=false
     wrapper.java.additional.9=-Djava.rmi.server.hostname=localhost
 ```

##Installation

1. Run 'mvn clean install' from the muleesb-monitoring-extension directory and find the MuleESBMonitor.zip in the 'target' directory.
2. Unzip MuleESBMonitor.zip and copy the 'MuleESBMonitor' directory to `<MACHINE_AGENT_HOME>/monitors/`
3. Configure the extension by referring to the below section.
5. Restart the Machine Agent.

In the AppDynamics Metric Browser, look for: Application Infrastructure Performance  | \<Tier\> | Custom Metrics | Mule ESB in case of default metric path

## Configuration

Note : Please make sure not to use tab (\t) while editing yaml files. You can validate the yaml file using a [yaml validator](http://yamllint.com/)

1. Configure the Mule ESB Extension by editing the config.yml file in `<MACHINE_AGENT_HOME>/monitors/MuleESBMonitor/`.
2. Specify the Mule ESB instance host, JMX port, username and password in the config.yml. By default the extension will fetch metrics from org.mule.Statistics type for all the domains under Mule. It also fetches memory metrics of the Mule instance.
3. We can exclude domains by specifying them in the `excludeDomains` configuration. 
   For eg.
   ```
        # Mule ESB instance particulars
        server:
            host: "localhost"
            port: 9000
            username: ""
            password: ""
        
        # Mule ESB MBeans
        mbeans:
            domainMatcher: "Mule.*"
            types: [org.mule.Statistics]
            excludeDomains: [Mule..agent,Mule.default]
        
        #prefix used to show up metrics in AppDynamics
        metricPrefix:  "Custom Metrics|Mule ESB|"

   ```

3. Configure the path to the config.yml file by editing the <task-arguments> in the monitor.xml file in the `<MACHINE_AGENT_HOME>/monitors/MuleESBMonitor/` directory. Below is the sample

     ```
     <task-arguments>
         <!-- config file-->
         <argument name="config-file" is-required="true" default-value="monitors/MuleESBMonitor/config.yml" />
          ....
     </task-arguments>
    ```



##Metrics

###Domain metrics
* {Domain_Name}/ExecutionErrors
* {Domain_Name}/FatalErrors
* {Domain_Name}/ProcessedEvents
* {Domain_Name}/MaxProcessingTime
* {Domain_Name}/TotalProcessingTime
* {Domain_Name}/SyncEventsReceived
* {Domain_Name}/TotalEventsReceived
* {Domain_Name}/MinProcessingTime
* {Domain_Name}/AsyncEventsReceived
* {Domain_Name}/AverageProcessingTime

###Memory Metrics
* MaxMemory
* TotalMemory
* FreeMemory

## Custom Dashboard
![](https://raw.githubusercontent.com/Appdynamics/muleesb-monitoring-extension/master/mule_custom_dashboard.png)

##Contributing

Always feel free to fork and contribute any changes directly here on GitHub.

##Community

Find out more in the [AppSphere]() community.

##Support

For any questions or feature request, please contact [AppDynamics Center of Excellence](mailto:help@appdynamics.com).

