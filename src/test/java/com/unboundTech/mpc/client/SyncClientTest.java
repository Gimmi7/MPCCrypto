package com.unboundTech.mpc.client;

import com.unboundTech.mpc.Context;
import com.unboundTech.mpc.Share;
import com.unboundTech.mpc.helper.MPC22Sink;
import com.unboundTech.mpc.model.MPC22Interaction;
import com.unboundTech.mpc.step.MPC22ShareType;
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

    private final String userId = System.getProperty("test.userid", "Alice");
    private int shareType = MPC22ShareType.KEY_TYPE_ECDSA;
    private static final int clientPeer = 1;

    @BeforeAll
    @SneakyThrows
    void connect() {
        syncClient.userId = userId;
        String url = "ws://localhost:2021/live";
        syncClient.connect(url);
        System.out.println("client: connect socket success");
    }

    /**
     * used for concurrent test
     */
//    @AfterEach
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


    @Order(2)
    @SneakyThrows
    @Test
    void generateDSA() {
        MPC22Interaction interaction = new MPC22Interaction();
        interaction.initContext = true;
        interaction.command = MPC22Interaction.Command.generate;

        Context context = null;

        if (MPC22ShareType.KEY_TYPE_ECDSA == shareType) {
            context = Context.initGenerateEcdsaKey(clientPeer);
            interaction.type = MPC22Interaction.Type.ecdsa;
        } else if (MPC22ShareType.KEY_TYPE_EDDSA == shareType) {
            context = Context.initGenerateEddsaKey(clientPeer);
            interaction.type = MPC22Interaction.Type.eddsa;
        }

        OracleStep oracleStep = new OracleStep(null, context);
        boolean flag = oracleStep.clientStep(syncClient, interaction);
        System.out.println("generateDSA flag=" + flag);
        Assertions.assertTrue(flag);
    }


    @SneakyThrows
//    @Test
    void compareShare() {
        byte[] clientShareBuf = MPC22Sink.loadShare(1, syncClient.userId, shareType);
        Share clientShare = Share.fromBuf(clientShareBuf);
        System.out.println("client share:" + ToStringBuilder.reflectionToString(clientShare.getInfo(), ToStringStyle.JSON_STYLE) + ToStringBuilder.reflectionToString(clientShare, ToStringStyle.JSON_STYLE));

        byte[] ServerShareBuf = MPC22Sink.loadShare(2, syncClient.userId, shareType);
        Share serverShare = Share.fromBuf(ServerShareBuf);
        System.out.println("server share:" + ToStringBuilder.reflectionToString(serverShare.getInfo(), ToStringStyle.JSON_STYLE) + ToStringBuilder.reflectionToString(serverShare, ToStringStyle.JSON_STYLE));
    }

    @Order(3)
    @SneakyThrows
    @Test
    void refreshShare() {
        MPC22Interaction interaction = new MPC22Interaction();
        interaction.initContext = true;
        interaction.command = MPC22Interaction.Command.refresh;

        if (MPC22ShareType.KEY_TYPE_ECDSA == shareType) {
            interaction.type = MPC22Interaction.Type.ecdsa;
        } else if (MPC22ShareType.KEY_TYPE_EDDSA == shareType) {
            interaction.type = MPC22Interaction.Type.eddsa;
        }

        byte[] shareBuf = MPC22Sink.loadShare(clientPeer, userId, shareType);
        Share share = Share.fromBuf(shareBuf);
        System.out.println("client share:" + ToStringBuilder.reflectionToString(share.getInfo(), ToStringStyle.JSON_STYLE));

        OracleStep oracleStep = new OracleStep(share, share.initRefreshKey(clientPeer));
        boolean flag = oracleStep.clientStep(syncClient, interaction);
        System.out.println("refreshShare flag=" + flag);

        shareBuf = MPC22Sink.loadShare(clientPeer, userId, shareType);
        share = Share.fromBuf(shareBuf);
        System.out.println("client share:" + ToStringBuilder.reflectionToString(share.getInfo(), ToStringStyle.JSON_STYLE));
        Assertions.assertTrue(flag);
    }


    @Order(4)
    @SneakyThrows
    @Test
    void sign() {
        byte[] rawBytes = "hello".getBytes();
        boolean refreshWhenSign = true;

        MPC22Interaction interaction = new MPC22Interaction();
        interaction.initContext = true;
        interaction.command = MPC22Interaction.Command.sign;
        interaction.rawBytes = rawBytes;
        interaction.refreshWhenSign = refreshWhenSign;


        byte[] shareBuf = MPC22Sink.loadShare(clientPeer, userId, shareType);
        Share share = Share.fromBuf(shareBuf);
        System.out.println("client share:" + ToStringBuilder.reflectionToString(share.getInfo(), ToStringStyle.JSON_STYLE));

        Context context = null;

        if (MPC22ShareType.KEY_TYPE_ECDSA == shareType) {
            context = share.initEcdsaSign(clientPeer, rawBytes, refreshWhenSign);
            interaction.type = MPC22Interaction.Type.ecdsa;
        } else if (MPC22ShareType.KEY_TYPE_EDDSA == shareType) {
            context = share.initEddsaSign(clientPeer, rawBytes, refreshWhenSign);
            interaction.type = MPC22Interaction.Type.eddsa;
        }

        OracleStep oracleStep = new OracleStep(share, context);
        boolean flag = oracleStep.clientStep(syncClient, interaction);
        System.out.println("sign flag=" + flag);

        Assertions.assertTrue(flag);

        /**
         *  update local variable share
         */
        if (refreshWhenSign) {
            share.close();
            shareBuf = MPC22Sink.loadShare(clientPeer, userId, shareType);
            share = Share.fromBuf(shareBuf);
        }

        if (MPC22ShareType.KEY_TYPE_ECDSA == shareType) {
            byte[] signature = context.getResultEcdsaSign();
            ECPublicKey pubKey = share.getEcdsaPublic();
            flag = Share.verifyEcdsa(pubKey, rawBytes, signature);
            System.out.println("signature verify flag=" + flag);
        } else if (MPC22ShareType.KEY_TYPE_EDDSA == shareType) {
            byte[] signature = context.getResultEddsaSign();
            byte[] pubKey = share.getEddsaPublic();
            flag = Share.verifyEddsa(pubKey, rawBytes, signature);
            System.out.println("signature verify flag=" + flag);
        }

        Assertions.assertTrue(flag);
    }

}

