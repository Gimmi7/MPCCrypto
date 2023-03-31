package com.unboundTech.mpc.client;


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


    @BeforeAll
    @SneakyThrows
    void connect() {
        syncClient.userId = userId;
        String url = "ws://localhost:2021/live";
        syncClient.connect(url);
        System.out.println("client: connect socket success");
    }

    @Test
    void generateBipMasterSeed(){

    }
}
