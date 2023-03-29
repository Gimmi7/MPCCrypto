package com.unboundTech.mpc.helper;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;

public class MessageSender {

    public static void sendBytes(Channel channel, byte[] bytes) {
        ByteBuf byteBuf = channel.alloc().buffer();
        byteBuf.writeBytes(bytes);
        BinaryWebSocketFrame bwf = new BinaryWebSocketFrame(byteBuf);
        channel.writeAndFlush(bwf);
    }
}
