package com.unboundTech.mpc.handler;

import com.alibaba.fastjson2.JSON;
import com.unboundTech.mpc.processor.ReqMsgDispatcher;
import com.unboundTech.mpc.socketmsg.MsgWrapper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@ChannelHandler.Sharable
@Slf4j
public class DataChannelHandler extends ChannelHandlerBase {
    @Autowired
    private ReqMsgDispatcher reqMsgDispatcher;


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, BinaryWebSocketFrame binaryWebSocketFrame) throws Exception {
        ByteBuf byteBuf = binaryWebSocketFrame.content();
        byte[] bytes=new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(bytes);

        // dispatch msg
        MsgWrapper msgWrapper = JSON.parseObject(bytes, MsgWrapper.class);
        if (msgWrapper.action == MsgWrapper.MsgAction.REQ) {
            reqMsgDispatcher.dispatchReqMsg(ctx.channel(), msgWrapper);
        }
    }


}
