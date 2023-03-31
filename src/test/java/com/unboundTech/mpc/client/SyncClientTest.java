package com.unboundTech.mpc.client;

import com.unboundTech.mpc.Context;
import com.unboundTech.mpc.Share;
import com.unboundTech.mpc.helper.MPC22Sink;
import com.unboundTech.mpc.model.MPC22Interaction;
import com.unboundTech.mpc.step.MPC22KeyType;
import com.unboundTech.mpc.step.OracleStep;
import lombok.SneakyThrows;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.security.interfaces.ECPublicKey;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest
class SyncClientTest {
    @Autowired
    private SyncClient syncClient;

    private String userId = System.getProperty("test.userid", "Alice");

    // used for concurrent test
    @AfterEach
    void delay() {
        int min = 3000;
        int max = 5000;
        int gap = max - min;
        double seed = Math.random();
        double random = seed * gap;
        int duration = min + (int) random;
        System.out.printf("sleep %d ms ................ \n", duration);
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Order(1)
//    @BeforeAll
    @SneakyThrows
    @Test
    void connect() {
        syncClient.userId = userId;
        String url = "ws://localhost:2021/live";
        syncClient.connect(url);
        System.out.println("client: connect socket success");
    }

    @Order(2)
    @SneakyThrows
    @Test
    void generateEcdsa() {
        OracleStep oracleStep = new OracleStep(null, Context.initGenerateEcdsaKey(1));

        MPC22Interaction interaction = new MPC22Interaction();
        interaction.initContext = true;
        interaction.command = MPC22Interaction.Command.generate;
        interaction.type = MPC22Interaction.Type.ecdsa;

        boolean flag = oracleStep.clientStep(syncClient, interaction);
        System.out.println("generate flag=" + flag);
        Assertions.assertTrue(flag);
    }

    @SneakyThrows
//    @Test
    void generateEddsa() {
        OracleStep oracleStep = new OracleStep(null, Context.initGenerateEddsaKey(1));

        MPC22Interaction interaction = new MPC22Interaction();
        interaction.initContext = true;
        interaction.command = MPC22Interaction.Command.generate;
        interaction.type = MPC22Interaction.Type.eddsa;

        boolean flag = oracleStep.clientStep(syncClient, interaction);
        System.out.println("generate flag=" + flag);
        Assertions.assertTrue(flag);
    }

    @SneakyThrows
//    @Test
    void compareShare() {
        byte[] clientShareBuf = MPC22Sink.loadShare(1, syncClient.userId);
        Share clientShare = Share.fromBuf(clientShareBuf);
        System.out.println("client share:" + ToStringBuilder.reflectionToString(clientShare.getInfo(), ToStringStyle.JSON_STYLE) + ToStringBuilder.reflectionToString(clientShare, ToStringStyle.JSON_STYLE));

        byte[] ServerShareBuf = MPC22Sink.loadShare(2, syncClient.userId);
        Share serverShare = Share.fromBuf(ServerShareBuf);
        System.out.println("server share:" + ToStringBuilder.reflectionToString(serverShare.getInfo(), ToStringStyle.JSON_STYLE) + ToStringBuilder.reflectionToString(serverShare, ToStringStyle.JSON_STYLE));
    }

    @Order(3)
    @SneakyThrows
    @Test
    void refreshShare() {
        byte[] clientShareBuf = MPC22Sink.loadShare(1, syncClient.userId);
        Share clientShare = Share.fromBuf(clientShareBuf);
        System.out.println("client share:" + ToStringBuilder.reflectionToString(clientShare.getInfo(), ToStringStyle.JSON_STYLE));

        OracleStep oracleStep = new OracleStep(clientShare, clientShare.initRefreshKey(1));

        MPC22Interaction interaction = new MPC22Interaction();
        interaction.initContext = true;
        interaction.command = MPC22Interaction.Command.refresh;


        boolean flag = oracleStep.clientStep(syncClient, interaction);
        System.out.println("flag=" + flag);

        clientShareBuf = MPC22Sink.loadShare(1, syncClient.userId);
        clientShare = Share.fromBuf(clientShareBuf);
        System.out.println("client share:" + ToStringBuilder.reflectionToString(clientShare.getInfo(), ToStringStyle.JSON_STYLE));
        Assertions.assertTrue(flag);
    }


    @Order(4)
    @SneakyThrows
    @Test
    void sign() {
        byte[] rawBytes = "hello".getBytes();
        boolean refreshWhenSign = false;

        byte[] clientShareBuf = MPC22Sink.loadShare(1, syncClient.userId);
        Share clientShare = Share.fromBuf(clientShareBuf);
        System.out.println("client share:" + ToStringBuilder.reflectionToString(clientShare.getInfo(), ToStringStyle.JSON_STYLE));

        int shareType = clientShare.getInfo().type;
        Context context = null;

        MPC22Interaction interaction = new MPC22Interaction();
        interaction.initContext = true;
        interaction.command = MPC22Interaction.Command.sign;
        interaction.rawBytes = rawBytes;
        interaction.refreshWhenSign = refreshWhenSign;

        if (MPC22KeyType.KEY_TYPE_ECDSA == shareType) {
            context = clientShare.initEcdsaSign(1, rawBytes, refreshWhenSign);
            interaction.type = MPC22Interaction.Type.ecdsa;
        } else if (MPC22KeyType.KEY_TYPE_EDDSA == shareType) {
            context = clientShare.initEddsaSign(1, rawBytes, refreshWhenSign);
            interaction.type = MPC22Interaction.Type.eddsa;
        }

        OracleStep oracleStep = new OracleStep(clientShare, context);

        boolean flag = oracleStep.clientStep(syncClient, interaction);
        System.out.println("flag=" + flag);

        Assertions.assertTrue(flag);

        if (refreshWhenSign) {
            clientShare.close();
            clientShare = Share.fromBuf(clientShareBuf);
        }
        if (MPC22KeyType.KEY_TYPE_ECDSA == shareType) {
            byte[] signature = context.getResultEcdsaSign();
            ECPublicKey pubKey = clientShare.getEcdsaPublic();
            flag = Share.verifyEcdsa(pubKey, rawBytes, signature);
            System.out.println("signature verify flag=" + flag);
        } else if (MPC22KeyType.KEY_TYPE_EDDSA == shareType) {
            byte[] signature = context.getResultEddsaSign();
            byte[] pubKey = clientShare.getEddsaPublic();
            flag = Share.verifyEddsa(pubKey, rawBytes, signature);
            System.out.println("signature verify flag=" + flag);
        }

        Assertions.assertTrue(flag);
    }

}

