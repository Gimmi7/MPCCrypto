package com.unboundTech.mpc.server;

import com.unboundTech.mpc.handler.ChildChannelHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
@Slf4j
public class NettyServerLaunch {
    @Autowired
    private ChildChannelHandler childChannelHandler;

//    @PostConstruct
    public void launch() {
        NettyServer nettyServer = new NettyServer(2021);
        Thread t = new Thread(() -> {
            nettyServer.runServer(childChannelHandler);
        });
        t.setName("nettyStart-" + "ws");
        t.start();
        log.info("start netty server at port 2021 +++++++++++++++++++++++++++++++++++");
    }
}
