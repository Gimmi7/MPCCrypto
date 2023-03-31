package com.unboundTech.mpc.client;


import com.unboundTech.mpc.Context;
import com.unboundTech.mpc.model.MPC22Interaction;
import com.unboundTech.mpc.step.OracleStep;
import lombok.SneakyThrows;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest
public class DeriveTest {

    @Autowired
    private SyncClient syncClient;

    private final String userId = System.getProperty("test.userid", "Bip");
    private static final int clientPeer = 1;


    @BeforeAll
    @SneakyThrows
    void connect() {
        syncClient.userId = userId;
        String url = "ws://localhost:2021/live";
        syncClient.connect(url);
        System.out.println("client: connect socket success");
    }

    @Order(2)
    @SneakyThrows
    @Test
    void generateBipMasterSeed() {
        int seedBits = 256;

        MPC22Interaction interaction = new MPC22Interaction();
        interaction.initContext = true;
        interaction.command = MPC22Interaction.Command.generate;
        interaction.type = MPC22Interaction.Type.generic;
        interaction.seedBits = seedBits;

        Context context = Context.initGenerateGenericSecret(clientPeer, seedBits);
        OracleStep oracleStep = new OracleStep(null, context);
        boolean flag = oracleStep.clientStep(syncClient, interaction);
        System.out.println("generateBipMasterSeed flag=" + flag);
        Assertions.assertTrue(flag);
    }
}
