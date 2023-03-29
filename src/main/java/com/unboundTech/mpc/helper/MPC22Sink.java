package com.unboundTech.mpc.helper;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import sun.misc.IOUtils;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

@Slf4j
public class MPC22Sink {

    public static String shareFileFormat = "./%s/%s.share";


    public static boolean sinkShare(byte[] shareBuf, int peerInt, String userId) {

        String shareFilePath = getShareFilePath(peerInt, userId);
        File shareFile = new File(shareFilePath);

        try (OutputStream outputStream = FileUtils.openOutputStream(shareFile)) {
            outputStream.write(shareBuf);
        } catch (Exception e) {
            log.error("sinkShare err:", e);
            return false;
        }
        return true;
    }

    public static byte[] loadShare(int peerInt, String userId) {

        String shareFilePath = getShareFilePath(peerInt, userId);
        File shareFile = new File(shareFilePath);

        try (InputStream inputStream = FileUtils.openInputStream(shareFile)) {
            return IOUtils.readAllBytes(inputStream);
        } catch (Exception e) {
            log.error("loadShare err:", e);
            return null;
        }
    }


    private static String getShareFilePath(int peerInt, String userId) {
        String peer = "server";
        if (peerInt == 1) {
            peer = "client";
        }
        return String.format(shareFileFormat, peer, userId);
    }

}
