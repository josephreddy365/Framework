package com.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

public class ReadProperties {

    private static Logger log = LogManager.getLogger(ReadProperties.class);
    public static String ENVIRONMENT = System.getProperty("env", "dev2").toLowerCase();

    public static String returnConfig(String property) {

        String sysProperty = System.getProperty(property);

        if (sysProperty != null) {
            log.info("Asked to retrieve property =>" + property + "<= from config file but returning =>" + sysProperty + "<= instead");
            return sysProperty;
        }

        if (property.endsWith("_env")) {
            property = property.replaceFirst("_env", "_"+ENVIRONMENT);
        }

        try {
            Properties prop = new Properties();

            File file = new File(System.getProperty("user.dir") + "/Configsfiles/Config.properties");
            FileInputStream fileInput = new FileInputStream(file);
            prop.load(fileInput);

            return prop.getProperty(property);
        } catch (Exception e) {
            log.warn("Received exception =>"+e+"<= when not expected whilst returning property =>"+property+"<= Returning null without error");
            e.printStackTrace();
            return null;
        }
    }

}
