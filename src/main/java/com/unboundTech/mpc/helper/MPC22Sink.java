package com.unboundTech.mpc.helper;

import com.unboundTech.mpc.MPCException;
import com.unboundTech.mpc.Share;
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

    /**
     * peer_Alice_shareType -> shareBuf
     */
    private final static ConcurrentMap<String, byte[]> cachedShareBuf = new ConcurrentHashMap<>();
    /**
     * ./{data_server}/{Alice}_{shareType}.share
     */
    private final static String shareFileFormat = "./%s/%s_%d.share";

    /**
     * peer_Alice_shareType
     */
    private final static String cacheKeyFormat = "%d_%s_%d";

    public static boolean sinkShare(byte[] shareBuf, int peerInt, String userId) {

        int shareType = 0;
        try (Share share = Share.fromBuf(shareBuf)) {
            shareType = share.getInfo().type;
        } catch (MPCException e) {
            log.error("sinkShare err, fail to call Share.fromBuf:", e);
            return false;
        }
        String shareFilePath = getShareFilePath(peerInt, userId, shareType);
        File shareFile = new File(shareFilePath);

        try (OutputStream outputStream = FileUtils.openOutputStream(shareFile)) {
            outputStream.write(shareBuf);
            // remove cached share
            String cacheKey = String.format(cacheKeyFormat, peerInt, userId, shareType);
            cachedShareBuf.remove(cacheKey);
        } catch (Exception e) {
            log.error("sinkShare err:", e);
            return false;
        }
        return true;
    }

    public static byte[] loadShare(int peerInt, String userId, int shareType) {

        String cacheKey = String.format(cacheKeyFormat, peerInt, userId, shareType);
        byte[] shareBuf = cachedShareBuf.get(cacheKey);
        if (shareBuf != null) {
            return shareBuf;
        }

        String shareFilePath = getShareFilePath(peerInt, userId, shareType);
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


    private static String getShareFilePath(int peerInt, String userId, int shareType) {
        String peerStr = "data_server";
        if (peerInt == 1) {
            peerStr = "data_client";
        }
        return String.format(shareFileFormat, peerStr, userId, shareType);
    }

}
