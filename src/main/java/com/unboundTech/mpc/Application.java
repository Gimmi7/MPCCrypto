package com.unboundTech.mpc;

import com.unboundTech.mpc.server.NettyServerLaunch;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class Application {


    public static void main(String[] args) {

        ConfigurableApplicationContext applicationContext = SpringApplication.run(Application.class, args);
        NettyServerLaunch nettyServerLaunch = applicationContext.getBean(NettyServerLaunch.class);
        nettyServerLaunch.launch();

    }

}
