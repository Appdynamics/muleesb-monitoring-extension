# AppDynamics Mule ESB Monitoring Extension

## Use Case

Mule ESB is a lightweight Java-based enterprise service bus (ESB) and integration platform that allows developers to connect applications together quickly and easily, enabling them to exchange data. This extension monitors Mule ESB instance and collects useful statistics exposed through MBeans.

## Prerequisites

Before the extension is installed, the prerequisites mentioned [here](https://community.appdynamics.com/t5/Knowledge-Base/Extensions-Prerequisites-Guide/ta-p/35213) need to be met. Please do not proceed with the extension installation if the specified prerequisites are not met.
To use this extension please configure JMX in Mule ESB instance. To configure JMX please add following properties to the wrapper.conf of the Mule instance.

 ```
     ###JMX Connection Properties###
     wrapper.java.additional.19=-Dcom.sun.management.jmxremote
     wrapper.java.additional.20=-Dcom.sun.management.jmxremote.port=9000
     wrapper.java.additional.21=-Dcom.sun.management.jmxremote.authenticate=false
     wrapper.java.additional.22=-Dcom.sun.management.jmxremote.ssl=false
     wrapper.java.additional.23=-Djava.rmi.server.hostname=localhost
 ```
 Please make sure that you put proper value of `n` in `wrapper.java.additional.<n>` as per your conf file.
 In order to use this extension, you do need a [Standalone JAVA Machine Agent](https://docs.appdynamics.com/display/PRO44/Standalone+Machine+Agents) or [SIM Agent](https://docs.appdynamics.com/display/PRO44/Server+Visibility).  For more details on downloading these products, please  visit [here](https://download.appdynamics.com/).
 The extension needs to be able to connect to the Mule ESB in order to collect and send metrics. To do this, you will have to either establish a remote connection in between the extension and the product, or have an agent on the same machine running the product in order for the extension to collect and send the metrics.

## Installation

1. Download and unzip the MuleESBMonitor-2.0.0.zip to the "<MachineAgent_Dir>/monitors" directory.
2. Edit the file config.yml located at <MachineAgent_Dir>/monitors/MuleESBMonitor The metricPrefix of the extension has to be configured as specified [here](https://community.appdynamics.com/t5/Knowledge-Base/How-do-I-troubleshoot-missing-custom-metrics-or-extensions/ta-p/28695#Configuring%20an%20Extension). Please make sure that the right metricPrefix is chosen based on your machine agent deployment, otherwise this could lead to metrics not being visible in the controller.
3. All metrics to be reported are configured in metrics.xml. Users can remove entries from metrics.xml to stop the metric from reporting, or add new entries as well.
4. Restart the Machine Agent.

Please place the extension in the **"monitors"** directory of your **Machine Agent** installation directory. Do not place the extension in the **"extensions"** directory of your **Machine Agent** installation directory.
In the AppDynamics Metric Browser, look for **Application Infrastructure Performance|\<Tier\>|Custom Metrics|Mule ESB** and you should be able to see all the metrics.


## Configuration
### Config.yml

Configure the Mule ESB Extension by editing the config.yml file in `<MACHINE_AGENT_HOME>/monitors/MuleESBMonitor/`.
  1. Configure the "COMPONENT_ID" under which the metrics need to be reported. This can be done by changing the value of `<COMPONENT_ID>` in   **metricPrefix: Server|Component:<COMPONENT_ID>|Custom Metrics|Mule ESB|**.
       For example,
       ```
       metricPrefix: "Server|Component:100|Custom Metrics|Mule ESB|"
       ```
  2. Specify the Mule ESB instance host, JMX port, username and password in the config.yml. By default the extension will fetch metrics from org.mule.Statistics type for all the domains under Mule. You can also configure any custom domainMatcher(regex supported) in the config.yml. It also fetches memory metrics of the Mule instance.

  3. We can exclude domains by specifying them in the `excludeDomains` configuration. Similary, you can add flows to be monitored in the `flows`.
   For eg.
   ```
     servers:
       - displayName: "Mule Server" # mandatory
          # You can either use just a host and port to connect or use your full serviceURL to make the connection
          # Do not choose both, comment one out and only use the other.
          # Below is a sample serviceUrl for a server.
          #  serviceUrl: ""         #e.g: "service:jmx:rmi:///jndi/rmi://localhost:9000/jmxrmi"
          #  serviceUrl: ""
         host: localhost
         port: 9000
         username: ""
         password: ""
         # Mule ESB MBeans
         mbeanDetails:
            domainMatcher: "Mule.*"
            types: [org.mule.Statistics]
            excludeDomains: []
            flows: [.*]

   ```



  3. Configure the path to the config.yml file by editing the <task-arguments> in the monitor.xml file in the `<MACHINE_AGENT_HOME>/monitors/MuleESBMonitor/` directory. Below is the sample

     ```
     <task-arguments>
         <!-- config file-->
         <argument name="config-file" is-required="true" default-value="monitors/MuleESBMonitor/config.yml" />
     </task-arguments>
     ```
  4. Configure the numberOfThreads.
    Each server instance needs 1 threads to call the mule ESB.
    For example,
    By default we want to support 3 servers, so it is 3 threads.
     ```
     numberOfThreads: 3
     ```


### Metrics.xml
You can add/remove metrics of your choice by modifying the provided metrics.xml file. This file consists of all the metrics that will be monitored and sent to the controller. Please look how the metrics have been defined and follow the same convention, when adding new metrics. You do have the ability to choose your Rollup types as well as set an alias that you would like to be displayed on the metric browser.

 #### Metric Configuration
    Add the `metric` to be monitored with the metric tag as shown below. Also please note that the `metrics` are grouped under appropriate stats in the metrics.xml.
```
<metric attr="AverageProcessingTime" alias="AverageProcessingTime" aggregationType = "OBSERVATION" timeRollUpType = "AVERAGE" clusterRollUpType = "INDIVIDUAL" />
 ```
For configuring the metrics, the following properties can be used:

 |     Property      |   Default value |         Possible values         |                                               Description                                                      |
 | ----------------- | --------------- | ------------------------------- | -------------------------------------------------------------------------------------------------------------- |
 | alias             | metric name     | Any string                      | The substitute name to be used in the metric browser instead of metric name.                                   |
 | aggregationType   | "AVERAGE"       | "AVERAGE", "SUM", "OBSERVATION" | [Aggregation qualifier](https://docs.appdynamics.com/display/PRO44/Build+a+Monitoring+Extension+Using+Java)    |
 | timeRollUpType    | "AVERAGE"       | "AVERAGE", "SUM", "CURRENT"     | [Time roll-up qualifier](https://docs.appdynamics.com/display/PRO44/Build+a+Monitoring+Extension+Using+Java)   |
 | clusterRollUpType | "INDIVIDUAL"    | "INDIVIDUAL", "COLLECTIVE"      | [Cluster roll-up qualifier](https://docs.appdynamics.com/display/PRO44/Build+a+Monitoring+Extension+Using+Java)|
 | multiplier        | 1               | Any number                      | Value with which the metric needs to be multiplied.                                                            |
 | convert           | null            | Any key value map               | Set of key value pairs that indicates the value to which the metrics need to be transformed. eg: UP:1, OPEN:1  |
 | delta             | false           | true, false                     | If enabled, gives the delta values of metrics instead of actual values.                                        |


 **All these metric properties are optional, and the default value shown in the table is applied to the metric (if a property has not been specified) by default.**

#### Metrics
Mule ESB metrics are exported by the JMX interface and the extension collects the metrics as available in the Jconsole.

### Domain metrics
* `<Domain_Name>`/ExecutionErrors
* `<Domain_Name>`/FatalErrors
* `<Domain_Name>`/ProcessedEvents
* `<Domain_Name>`/MaxProcessingTime
* `<Domain_Name>`/TotalProcessingTime
* `<Domain_Name>`/SyncEventsReceived
* `<Domain_Name>`/TotalEventsReceived
* `<Domain_Name>`/MinProcessingTime
* `<Domain_Name>`/AsyncEventsReceived
* `<Domain_Name>`/AverageProcessingTime

### Memory Metrics
* MaxMemory
* TotalMemory
* FreeMemory

## Credentials Encryption

Please visit [this page](https://community.appdynamics.com/t5/Knowledge-Base/How-to-use-Password-Encryption-with-Extensions/ta-p/29397) to get detailed instructions on password encryption. The steps in this document will guide you through the whole process.

## Extensions Workbench
Workbench is an inbuilt feature provided with each extension in order to assist you to fine tune the extension setup before you actually deploy it on the controller. Please review the following document on [How to use the Extensions WorkBench](https://community.appdynamics.com/t5/Knowledge-Base/How-to-use-the-Extensions-WorkBench/ta-p/30130).

## Troubleshooting
Before configuring the extension, please make sure to run the below steps to check if the set up is correct.
1. Please follow the steps listed in this [troubleshooting-document](https://community.appdynamics.com/t5/Knowledge-Base/How-to-troubleshoot-missing-custom-metrics-or-extensions-metrics/ta-p/28695) in order to troubleshoot your issue. These are a set of common issues that customers might have faced during the installation of the extension.
2. Start 'jconsole'. Jconsole comes as a utility with installed jdk. After giving the correct host and port , check if Mule ESB mbeans shows up.
3. It is a good idea to match the mbeanDetails mentioned in the config.yml against the jconsole. JMX is case sensitive so make sure the config matches exactly.

## Contributing
Always feel free to fork and contribute any changes directly here on [GitHub](https://github.com/Appdynamics/muleesb-monitoring-extension).

## Version
|          Name            |  Version   |
|--------------------------|------------|
|Extension Version         |2.2.1       |
|Controller Compatibility  |4.5 or Later|
|Agent Compatibility  |4.5.13 or Later|
|Product Tested On         |3.9.2       |
|Last Update               |04/02/2021  |
|Changes list              |[ChangeLog](https://github.com/Appdynamics/muleesb-monitoring-extension/blob/master/CHANGELOG.md)|
