package edu.caltech.test.nanodb.util;

/**
 * @author vv
 * @since 2017/9/16.
 */
public class TestUtil {

    /**
     * 将byte以优雅的方式打印
     * 
     * @param bytes bytes
     */
    public static void printHex(byte[] bytes, boolean shortFor0) {
        StringBuilder result = new StringBuilder();
        StringBuilder line = new StringBuilder("001  ");
        boolean all0 = true;
        int lineNum = 1;
        for (int i = 0; i < bytes.length; i++) {

            String hex = Integer.toHexString(bytes[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            if (all0 && !"00".equals(hex)) {
                all0 = false;
            }
            line.append(hex).append(" ");

            if ((i + 1) % 16 == 0 && i != 0) {
                // line全为0且是要简写，则此行不输出
                if(!shortFor0 || !all0){
                    result.append(line).append("\n");
                }

                // new line and add line num
                line = new StringBuilder();
                String lineNumStr = String.format("%03d  ", ++lineNum);
                line.append(lineNumStr);
                all0 = true;
            }
        }
        System.out.print(result);
    }

    public static void printHex(byte[] bytes) {
        printHex(bytes, false);
    }
}
