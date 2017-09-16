package edu.caltech.nanodb.util;

import org.junit.Test;


/**
 * @author vv
 * @since 2017/9/16.
 */
public class PropertiesUtilTest {
    @Test
    public void getProperty() throws Exception {
        String baseDir = PropertiesUtil.getProperty("nanodb.basedir");
        System.out.println(baseDir);
    }

}