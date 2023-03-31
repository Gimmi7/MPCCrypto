package com.unboundTech.mpc.helper;

import com.unboundTech.mpc.MPCException;
import com.unboundTech.mpc.Share;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Slf4j
public class MPC22Sink {

    /**
     * peer_Alice_shareType_shareUid -> shareBuf
     */
    private final static ConcurrentMap<String, byte[]> cachedShareBuf = new ConcurrentHashMap<>();
    /**
     * ./{data_server}/{Alice}_{shareType}_{shareUid}.share
     */
    private final static String shareFileFormat = "./%s/%s_%d_%d.share";

    /**
     * peer_Alice_shareType_shareUid
     */
    private final static String cacheKeyFormat = "%d_%s_%d_%d";

    public static boolean sinkShare(byte[] shareBuf, int peerInt, String userId) {

        int shareType = 0;
        long shareUid = 0;
        try (Share share = Share.fromBuf(shareBuf)) {
            Share.Info shareInfo = share.getInfo();
            shareType = shareInfo.type;
            shareUid = shareInfo.UID;
        } catch (MPCException e) {
            log.error("sinkShare err, fail to call Share.fromBuf:", e);
            return false;
        }
        String shareFilePath = getShareFilePath(peerInt, userId, shareType, shareUid);
        File shareFile = new File(shareFilePath);

        try (OutputStream outputStream = FileUtils.openOutputStream(shareFile)) {
            outputStream.write(shareBuf);
            // remove cached share
            String cacheKey = String.format(cacheKeyFormat, peerInt, userId, shareType, shareUid);
            cachedShareBuf.remove(cacheKey);
        } catch (Exception e) {
            log.error("sinkShare err:", e);
            return false;
        }
        return true;
    }

    public static byte[] loadShare(int peerInt, String userId, int shareType, long shareUid) {

        String cacheKey = String.format(cacheKeyFormat, peerInt, userId, shareType, shareUid);
        byte[] shareBuf = cachedShareBuf.get(cacheKey);
        if (shareBuf != null) {
            return shareBuf;
        }

        String shareFilePath = getShareFilePath(peerInt, userId, shareType, shareUid);
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

    public static byte[] loadFirstShare(int peerInt, String userId, int shareType) {
        String shareFilePath = getShareFilePath(peerInt, userId, shareType, 0);
        String[] pathArr = shareFilePath.split("/");

        String[] folderSegment = Arrays.copyOfRange(pathArr, 0, pathArr.length - 1);
        String folderStr = String.join("/", folderSegment);
        File folder = new File(folderStr);
        String[] list = folder.list();
        if (list == null) {
            return null;
        }
        if (list.length == 0) {
            return null;
        }

        List<String> shareList = Arrays.stream(list).filter(s -> s.contains(userId)).sorted(Comparator.naturalOrder()).collect(Collectors.toList());
        String[] keyArr = shareList.get(0).split("_");
        String shareUidStr = keyArr[keyArr.length - 1].replace(".share", "");
        long shareUid = new Long(shareUidStr);
        return loadShare(peerInt, userId, shareType, shareUid);
    }


    private static String getShareFilePath(int peerInt, String userId, int shareType, long shareUid) {
        String peerStr = "data_server";
        if (peerInt == 1) {
            peerStr = "data_client";
        }
        return String.format(shareFileFormat, peerStr, userId, shareType, shareUid);
    }


}
