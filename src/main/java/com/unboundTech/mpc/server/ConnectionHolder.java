package com.unboundTech.mpc.server;

import com.unboundTech.mpc.step.OracleStep;
import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.internal.PlatformDependent;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentMap;

@Slf4j
public class ConnectionHolder {
    //连接到此台机器的所有channel
    public static ChannelGroup group = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    //channelId -> OracleStep
    private static ConcurrentMap<ChannelId, OracleStep> channelId2Oracle = PlatformDependent.newConcurrentHashMap();


    public static void addConnection(Channel channel, OracleStep oracleStep) {
        group.add(channel);
        channelId2Oracle.put(channel.id(), oracleStep);
    }

    public static OracleStep getOracleStep(Channel channel) {
        return channelId2Oracle.getOrDefault(channel.id(), null);
    }

    public static void remove(Channel channel) {
        OracleStep oracleStep = getOracleStep(channel);
        if (oracleStep != null) {

            try {
                oracleStep.close();
            } catch (Exception e) {
                log.error("close oracleStep fail:", e);
            }
            oracleStep = null;
            channelId2Oracle.remove(channel.id());
        }
        group.remove(channel);
    }

}
