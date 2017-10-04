package com.bow.lab.columnar;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

/**
 * @author vv
 * @since 2017/10/4.
 */
public class Generator {

    public static void main(String[] args) throws Exception {
        File file = new File("./vv.txt");
        FileOutputStream fos = new FileOutputStream(file);
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
        char a = 'A';
        for(int i=0; i<10; i++){
            bw.write(i+","+a+"\n");
            a = (char) (a+1);
        }
        bw.close();
    }
}
