package com.ui.dataproviders;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class ConfigFileReader {

    private static Properties properties;
    private String CONFIG_VALUE;
    private static String Browser;
    private final String configFilesRoot = System.getProperty("user.dir") + "/Configsfiles/";
    private final String propertyFilePath = configFilesRoot + "Config.properties";


    public String ConfigFileReader(String element) {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(propertyFilePath));
            properties = new Properties();
            try {
                properties.load(reader);
                CONFIG_VALUE = properties.get(element).toString();
                reader.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException("Configuration.properties not found at " + propertyFilePath);
        }
        return CONFIG_VALUE;
    }

    public String getReportConfigPath() {
        String reportConfigPath = properties.getProperty("reportConfigPath");
        if (reportConfigPath != null) return reportConfigPath;
        else throw new RuntimeException("Report Config Path not specified in the Configuration.properties file for the Key:reportConfigPath");
    }

    public void setBrowserValue(String value) {
        Browser = properties.get("browser").toString();
        properties.setProperty(Browser, value);
    }

    public String getFileAsString(String fileName) {
        BufferedReader reader;
        StringBuilder fileBuilder = new StringBuilder();
        try {
            reader = new BufferedReader(new FileReader(configFilesRoot + fileName));
            reader.lines().forEach(line -> fileBuilder.append("\n").append(line));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException("Configuration.properties not found at " + propertyFilePath);
        }
        return fileBuilder.toString();
    }
}

