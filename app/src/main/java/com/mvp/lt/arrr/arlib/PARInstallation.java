package com.mvp.lt.arrr.arlib;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.UUID;

/**
 * $activityName
 *
 * @author LiuTao
 * @date 2018/7/27/027
 */


public class PARInstallation {
    private static String sID = null;

    public PARInstallation() {
    }

    public static synchronized String id(Context context) {
        if(sID == null) {
            File installation = new File(context.getFilesDir(), "PAR-android");

            try {
                if(!installation.exists()) {
                    writeInstallationFile(installation);
                }

                sID = readInstallationFile(installation);
            } catch (Exception var3) {
                throw new RuntimeException(var3);
            }
        }

        return sID;
    }

    private static String readInstallationFile(File installation) throws IOException {
        RandomAccessFile f = new RandomAccessFile(installation, "r");
        byte[] bytes = new byte[(int)f.length()];
        f.readFully(bytes);
        f.close();
        return new String(bytes);
    }

    private static void writeInstallationFile(File installation) throws IOException {
        FileOutputStream out = new FileOutputStream(installation);
        String id = UUID.randomUUID().toString();
        out.write(id.getBytes());
        out.close();
    }
}
