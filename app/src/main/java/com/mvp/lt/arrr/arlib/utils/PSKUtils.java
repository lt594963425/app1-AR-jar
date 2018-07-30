package com.mvp.lt.arrr.arlib.utils;

/**
 * $activityName
 *
 * @author LiuTao
 * @date 2018/7/27/027
 */


public class PSKUtils {
    public PSKUtils() {
    }

    public static String matrix16ToString(float[] matrix) {
        return matrix.length == 16?matrix[0] + "\t" + matrix[1] + "\t" + matrix[2] + "\t" + matrix[3] + "\n" + matrix[4] + "\t" + matrix[5] + "\t" + matrix[6] + "\t" + matrix[7] + "\n" + matrix[8] + "\t" + matrix[9] + "\t" + matrix[10] + "\t" + matrix[11] + "\n" + matrix[12] + "\t" + matrix[13] + "\t" + matrix[14] + "\t" + matrix[15]:"Matrix is not 4x4";
    }

    public static String floatArrayToString(float[] array) {
        StringBuilder builder = new StringBuilder();
        float[] arr$ = array;
        int len$ = array.length;

        for(int i$ = 0; i$ < len$; ++i$) {
            float v = arr$[i$];
            builder.append(v + " ");
        }

        builder.append("\n");
        return builder.toString();
    }

    public static String floatArrayToStringWithRadToDeg(float[] array) {
        StringBuilder builder = new StringBuilder();
        float[] arr$ = array;
        int len$ = array.length;

        for(int i$ = 0; i$ < len$; ++i$) {
            float v = arr$[i$];
            builder.append(57.295776F * v + " ");
        }

        builder.append("\n");
        return builder.toString();
    }
}
