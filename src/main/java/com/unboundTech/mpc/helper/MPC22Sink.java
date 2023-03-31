package com.unboundTech.mpc.helper;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
public class MPC22Sink {

    private static ConcurrentMap<String, byte[]> cachedShareBuf = new ConcurrentHashMap<>();
    public static String shareFileFormat = "./%s/%s.share";


    public static boolean sinkShare(byte[] shareBuf, int peerInt, String userId) {

        String shareFilePath = getShareFilePath(peerInt, userId);
        File shareFile = new File(shareFilePath);

        try (OutputStream outputStream = FileUtils.openOutputStream(shareFile)) {
            outputStream.write(shareBuf);
            // remove cached share
            String cacheKey = String.format("%d_%s", peerInt, userId);
            cachedShareBuf.remove(cacheKey);
        } catch (Exception e) {
            log.error("sinkShare err:", e);
            return false;
        }
        return true;
    }

    public static byte[] loadShare(int peerInt, String userId) {

        String cacheKey = String.format("%d_%s", peerInt, userId);
        byte[] shareBuf = cachedShareBuf.get(cacheKey);
        if (shareBuf != null) {
            return shareBuf;
        }

        String shareFilePath = getShareFilePath(peerInt, userId);
        File shareFile = new File(shareFilePath);

        try (InputStream inputStream = FileUtils.openInputStream(shareFile)) {
            shareBuf = IOUtils.readFully(inputStream, inputStream.available());
            cachedShareBuf.put(cacheKey, shareBuf);
            return shareBuf;
        } catch (Exception e) {
            log.error("loadShare err:", e);
            return null;
        }
    }


    private static String getShareFilePath(int peerInt, String userId) {
        String peer = "data_server";
        if (peerInt == 1) {
            peer = "data_client";
        }
        return String.format(shareFileFormat, peer, userId);
    }

}
