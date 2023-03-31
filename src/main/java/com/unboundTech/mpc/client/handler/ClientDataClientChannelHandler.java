package com.unboundTech.mpc.client.handler;

import com.alibaba.fastjson2.JSON;
import com.unboundTech.mpc.client.PromiseHolder;
import com.unboundTech.mpc.client.SyncClient;
import com.unboundTech.mpc.socketmsg.MsgWrapper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@ChannelHandler.Sharable
@Slf4j
public class ClientDataClientChannelHandler extends ClientChannelHandlerBase {

    @Autowired
    private SyncClient syncClient;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, BinaryWebSocketFrame msg) throws Exception {
        ByteBuf byteBuf = msg.content();
        byte[] bytes = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(bytes);

        MsgWrapper wrapper = JSON.parseObject(bytes, MsgWrapper.class);
        // dispatch msg to syncClient
        if (MsgWrapper.MsgAction.RSP == wrapper.action) {
            log.debug("get {} rsp, seq={}, ++++++++++++++", wrapper.reqKey, wrapper.seq);
            PromiseHolder.resolve(wrapper.seq, wrapper);
        }
    }
}
