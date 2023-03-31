package com.unboundTech.mpc.client;

import com.unboundTech.mpc.Context;
import com.unboundTech.mpc.Share;
import com.unboundTech.mpc.helper.MPC22Sink;
import com.unboundTech.mpc.model.MPC22Interaction;
import lombok.SneakyThrows;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.junit.jupiter.api.Test;

import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.util.Base64;

public class MemoryStepTest {

    @SneakyThrows
    @Test
    void generateShare() {
        MemoryStep.TestContext testContext = new MemoryStep.TestContext();
        testContext.client = Context.initGenerateGenericSecret(1, 256);
        testContext.server = Context.initGenerateGenericSecret(2, 256);
        MemoryStep.testClientServer(new MemoryStep.TestShare(), testContext);

        System.out.println("ok");
    }

    @Test
    void deriveWithSecp256Share() throws Exception {
        String userId = "Alice";

        MemoryStep.TestShare testShare = new MemoryStep.TestShare();
        byte[] clientShare = MPC22Sink.loadFirstShare(1, userId, MPC22Interaction.ShareType.KEY_TYPE_ECDSA);
        byte[] serverShare = MPC22Sink.loadFirstShare(2, userId, MPC22Interaction.ShareType.KEY_TYPE_ECDSA);
        testShare.client = Share.fromBuf(clientShare);
        testShare.server = Share.fromBuf(serverShare);
        System.out.println("Alice publicKey=" + testShare.client.getEcdsaPublic().toString());
        System.out.println("Alice share bip32Info=" + ToStringBuilder.reflectionToString(testShare.client.getBIP32Info(), ToStringStyle.JSON_STYLE));

        MemoryStep.TestContext deriveContext = new MemoryStep.TestContext();
        deriveContext.client = testShare.client.initDeriveBIP32(1, true, 0);
        deriveContext.server = testShare.server.initDeriveBIP32(2, true, 0);
        MemoryStep.testClientServer(testShare, deriveContext);

        Share sonShare = deriveContext.client.getResultDeriveBIP32();
        System.out.println("sonShare type=" + sonShare.getInfo().type);
        System.out.println("sonShare publicKey=" + sonShare.getEcdsaPublic().toString());
        System.out.println("son share bip32Info=" + ToStringBuilder.reflectionToString(sonShare.getBIP32Info(), ToStringStyle.JSON_STYLE));
    }

    @Test
    void deriveFromSeedShare() throws Exception {
        String userId = "unbound";
        MemoryStep.TestShare seedShare = new MemoryStep.TestShare();
        byte[] clientShare = MPC22Sink.loadFirstShare(1, userId, MPC22Interaction.ShareType.KEY_TYPE_GENERIC_SECRET);
        byte[] serverShare = MPC22Sink.loadFirstShare(2, userId, MPC22Interaction.ShareType.KEY_TYPE_GENERIC_SECRET);
        seedShare.client = Share.fromBuf(clientShare);
        seedShare.server = Share.fromBuf(serverShare);
        System.out.println("seed share type=" + seedShare.client.getInfo().type);

        System.out.println("derive root start=============");
        MemoryStep.TestContext seedContext = new MemoryStep.TestContext();
        seedContext.client = seedShare.client.initDeriveBIP32(1, false, 0);
        seedContext.server = seedShare.server.initDeriveBIP32(2, false, 0);
        MemoryStep.testClientServer(seedShare, seedContext);

        MemoryStep.TestShare rootShare = new MemoryStep.TestShare();
        rootShare.client = seedContext.client.getResultDeriveBIP32();
        System.out.println("rootShare type=" + rootShare.client.getInfo().type);
        System.out.println("rootShare publicKey=" + rootShare.client.getEcdsaPublic().toString());
        rootShare.server = seedContext.server.getResultDeriveBIP32();
        System.out.println("derive root end=============");


        MemoryStep.TestContext rootContext = new MemoryStep.TestContext();
        rootContext.client = rootShare.client.initDeriveBIP32(1, true, 5);
        rootContext.server = rootShare.server.initDeriveBIP32(2, true, 5);
        MemoryStep.testClientServer(rootShare, rootContext);

        MemoryStep.TestShare sonShare = new MemoryStep.TestShare();
        sonShare.client = rootContext.client.getResultDeriveBIP32();
        System.out.println("sonShare type=" + sonShare.client.getInfo().type);
        System.out.println("sonShare publicKey=" + sonShare.client.getEcdsaPublic().toString());
    }

    @Test
    void signWithRootShare() throws Exception {

        String userId = "unbound";
        MemoryStep.TestShare seedShare = new MemoryStep.TestShare();
        byte[] clientShare = MPC22Sink.loadFirstShare(1, userId, MPC22Interaction.ShareType.KEY_TYPE_GENERIC_SECRET);
        byte[] serverShare = MPC22Sink.loadFirstShare(2, userId, MPC22Interaction.ShareType.KEY_TYPE_GENERIC_SECRET);
        seedShare.client = Share.fromBuf(clientShare);
        seedShare.server = Share.fromBuf(serverShare);
        System.out.println("seed share type=" + seedShare.client.getInfo().type);

        System.out.println("derive root start=============");
        MemoryStep.TestContext seedContext = new MemoryStep.TestContext();
        seedContext.client = seedShare.client.initDeriveBIP32(1, false, 0);
        seedContext.server = seedShare.server.initDeriveBIP32(2, false, 0);
        MemoryStep.testClientServer(seedShare, seedContext);

        MemoryStep.TestShare rootShare = new MemoryStep.TestShare();
        rootShare.client = seedContext.client.getResultDeriveBIP32();
        System.out.println("rootShare type=" + rootShare.client.getInfo().type);
        System.out.println("rootShare publicKey=" + rootShare.client.getEcdsaPublic().toString());
        rootShare.server = seedContext.server.getResultDeriveBIP32();
        System.out.println("derive root end=============");


        MemoryStep.TestShare testKey = rootShare;

        System.out.print("testEcdsaSign...");
        byte[] test = "123456".getBytes();
        try (MemoryStep.TestContext testContext = new MemoryStep.TestContext()) {
            testContext.client = testKey.client.initEcdsaSign(1, test, false);
            testContext.server = testKey.server.initEcdsaSign(2, test, false);
            MemoryStep.testClientServer(testKey, testContext);

            byte[] signature = testContext.client.getResultEcdsaSign();
            ECPublicKey pubKey = testKey.client.getEcdsaPublic();
            Signature sig = Signature.getInstance("NoneWithECDSA");
            sig.initVerify(pubKey);
            sig.update(test);
            if (!sig.verify(signature)) {
                throw new Exception("verifyEcdsa failed");
            }
        }

        System.out.println(" ok");
    }

    /**
     * <a href="https://github.com/unboundsecurity/blockchain-crypto-mpc/blob/master/docs/Unbound_Cryptocurrency_Wallet_Library_White_Paper.md">...</a>
     * BIP derivation: Derive keys in MPC, using the initial secret, according to the BIP32 standard.
     * The result of this step is a key that can be used in ECDSA.
     * <p>
     * We do not provide a method for backing up the BIP master seed itself directly,
     * since a more efficient zero-knowledge proof exists for the root node private key.
     */
    @SneakyThrows
    @Test
    void deriveWithEd25519Share() {
        String userId = "Bob";

        MemoryStep.TestShare bobShare = new MemoryStep.TestShare();
        byte[] clientShare = MPC22Sink.loadFirstShare(1, userId, MPC22Interaction.ShareType.KEY_TYPE_EDDSA);
        byte[] serverShare = MPC22Sink.loadFirstShare(2, userId, MPC22Interaction.ShareType.KEY_TYPE_EDDSA);
        bobShare.client = Share.fromBuf(clientShare);
        bobShare.server = Share.fromBuf(serverShare);
        String bobPublicKey = Base64.getEncoder().encodeToString(bobShare.client.getEddsaPublic());
        System.out.println("bob publicKey=" + bobPublicKey);

        MemoryStep.TestContext deriveContext = new MemoryStep.TestContext();
        deriveContext.client = bobShare.client.initDeriveBIP32(1, false, 0);
        deriveContext.server = bobShare.server.initDeriveBIP32(2, false, 0);
        MemoryStep.testClientServer(bobShare, deriveContext);
    }
}
