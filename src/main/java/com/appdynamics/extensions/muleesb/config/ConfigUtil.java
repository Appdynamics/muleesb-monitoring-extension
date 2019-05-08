///*
// * Copyright 2018. AppDynamics LLC and its affiliates.
// * All Rights Reserved.
// * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
// * The copyright notice above does not evidence any actual or intended publication of such source code.
// */
//package com.appdynamics.extensions.muleesb.conf;
//
//import org.apache.log4j.Logger;
//import org.yaml.snakeyaml.Yaml;
//import org.yaml.snakeyaml.constructor.Constructor;
//
//import java.io.FileInputStream;
//import java.io.FileNotFoundException;
//
//public class ConfigUtil<T> {
//
//	private static final Logger logger = Logger.getLogger(ConfigUtil.class);
//
//	public T readConfig(String fileName, Class<T> clazz) throws FileNotFoundException {
//		logger.info("Reading conf file: " + fileName);
//		Yaml yaml = new Yaml(new Constructor(clazz));
//		T conf = (T) yaml.load(new FileInputStream(fileName));
//		return conf;
//	}
//
//}
