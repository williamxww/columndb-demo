package com.bow.lab.columnar;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

/**
 * @author vv
 * @since 2017/10/8.
 */
public class BigDataGen {

    public static void main(String[] args) throws Exception {
        File file = new File("./input/vv1000");
        FileOutputStream fos = new FileOutputStream(file);
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
        StringBuilder sb = new StringBuilder();
        for(int i=0; i<1000; i++){
            sb.append(i+","+i+"\n");
            bw.write(sb.toString());
        }
        bw.close();
    }
}
