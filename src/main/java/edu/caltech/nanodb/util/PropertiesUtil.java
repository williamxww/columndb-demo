package edu.caltech.nanodb.util;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author vv
 * @since 2017/9/16.
 */
public class PropertiesUtil {

    private static final Logger LOGGER = Logger.getLogger(PropertiesUtil.class);

    /**
     * 默认加载的properties文件
     */
    private static final String[] DEFAULT_PROPERTIES_URL = {"config.conf"};

    private static Properties properties = new Properties();

    static {
        for (String url : DEFAULT_PROPERTIES_URL) {
            loadFile(url);
        }
    }

    private static void loadFile(String fileUrl) {
        InputStream is = PropertiesUtil.class.getClassLoader().getResourceAsStream(fileUrl);

        try {
            properties.load(is);
        } catch (IOException e) {
            LOGGER.warn("can not load "+ fileUrl);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                LOGGER.error("IOException when close inputStream " + fileUrl, e);
            }
        }
    }

    public static String getProperty(String key) {
        return StringUtils.trim(properties.getProperty(key));
    }

    public static String getProperty(String key, String defaultValue) {
        return StringUtils.trim(properties.getProperty(key, defaultValue));
    }

    public static int getInt(String key, int def) {
        try {
            return Integer.parseInt(getProperty(key));
        } catch (NumberFormatException e) {
            LOGGER.warn("fail to parseInt for key: " + key);
            return def;
        }
    }

    public static long getLong(String key, long def) {
        try {
            return Long.parseLong(getProperty(key));
        } catch (NumberFormatException e) {
            LOGGER.warn("fail to parseLong for key: " + key);
            return def;
        }
    }

    public static boolean getBoolean(String key, boolean def) {
        try {
            return Boolean.parseBoolean(getProperty(key));
        } catch (NumberFormatException e) {
            LOGGER.warn("fail to parseInt for key: " + key);
            return def;
        }
    }
}
