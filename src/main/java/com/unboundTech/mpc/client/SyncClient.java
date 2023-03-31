package com.unboundTech.mpc.client;

import com.alibaba.fastjson2.JSON;
import com.unboundTech.mpc.client.handler.ClientChildChannelHandler;
import com.unboundTech.mpc.client.handler.ClientDataClientChannelHandler;
import com.unboundTech.mpc.helper.MessageSender;
import com.unboundTech.mpc.socketmsg.MsgWrapper;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

@Component
@Slf4j
public class SyncClient {
    @Autowired
    private ClientDataClientChannelHandler dataChannelHandler;
    private NettyClient nettyClient;
    private Channel channel;
    private ClientChildChannelHandler clientChildChannelHandler;
    public String userId;
    private int _seq = 0;


    public Channel connect(String url) throws URISyntaxException {
        NettyClient nettyClient = new NettyClient();
        URI uri = new URI(url);
        ClientChildChannelHandler clientChildChannelHandler = new ClientChildChannelHandler(dataChannelHandler);
        Channel channel = nettyClient.runClient(clientChildChannelHandler, uri);

        this.clientChildChannelHandler = clientChildChannelHandler;
        this.nettyClient = nettyClient;
        this.channel = channel;
        return channel;
    }

    public Future<MsgWrapper> sendReq(String reqKey, byte[] body) {
        int seq = getSeq();
        log.info("Send {} req, seq={}, ++++++++++++++++++", reqKey, seq);

        // set client params
        MsgWrapper msgWrapper = new MsgWrapper();
        msgWrapper.seq = seq;
        msgWrapper.timestamp = System.currentTimeMillis();
        msgWrapper.action = MsgWrapper.MsgAction.REQ;
        msgWrapper.reqKey = reqKey;
        msgWrapper.body = body;
        msgWrapper.userId = userId;

        MessageSender.sendBytes(channel, JSON.toJSONBytes(msgWrapper));

        // set lastWriteTime for customIdleHandler
        clientChildChannelHandler.customIdleStateHandler.writeListener();

        CompletableFuture<MsgWrapper> promise = new CompletableFuture<>();
        PromiseHolder.add(seq, promise);
        return promise;
    }


    public void closeClient() {
        System.out.println("invoke closeClient " + LocalDateTime.now());
        if (channel != null) {
            channel.close();
        }
    }

    private synchronized int getSeq() {
        if (Integer.MAX_VALUE == _seq) {
            _seq = 0;
        }
        _seq++;
        return _seq;
    }
}
