package com.unboundTech.mpc.client;

import com.unboundTech.mpc.Context;
import com.unboundTech.mpc.Message;
import com.unboundTech.mpc.Share;
import com.unboundTech.mpc.helper.MPC22Sink;

/**
 * mpc_protocol transport protocol message with memory swap
 */
public class MemoryStep {

    public static class TestShare implements AutoCloseable {
        Share client = null;
        Share server = null;

        @Override
        public void close() throws Exception {
            if (client != null) client.close();
            if (server != null) server.close();
            client = null;
            server = null;
        }
    }

    public static class TestContext implements AutoCloseable {
        Context client = null;
        Context server = null;

        @Override
        public void close() throws Exception {
            if (client != null) client.close();
            if (server != null) server.close();
            client = null;
            server = null;
        }
    }

    static class TestStep implements AutoCloseable {
        byte[] messageBuf = null;
        public Share share = null;
        public Context context = null;

        TestStep(Share share, Context context) {
            this.share = share;
            this.context = context;
        }

        boolean step() throws Exception {
            boolean finished = false;

            try (
                    Message inMessage = (messageBuf == null) ? null : Message.fromBuf(messageBuf);
                    Context.MessageAndFlags messageAndFlags = context.step(inMessage)) {
                byte[] contextBuf = context.toBuf();
                context.close();
                context = Context.fromBuf(contextBuf);

                finished = messageAndFlags.protocolFinished;

                if (messageAndFlags.shareChanged) {
                    if (share != null) share.close();
                    share = context.getShare();

                    byte[] shareBuf = share.toBuf();
                    int peer = context.getInfo().peer;
                    System.out.printf("share change: %s, ++++++++++++++ \n", peer);
                    MPC22Sink.sinkShare(shareBuf, peer, "unbound");
                    share.close();
                    share = Share.fromBuf(shareBuf);
                }

                if (messageAndFlags.message != null) {
                    messageBuf = messageAndFlags.message.toBuf();
                }
            }

            return finished;
        }

        @Override
        public void close() throws Exception {
            if (share != null) share.close();
            if (context != null) context.close();
            share = null;
            context = null;
        }
    }

    public static void testClientServer(TestShare testShare, TestContext testContext) throws Exception {
        boolean clientFinished = false;
        boolean serverFinished = false;

        try (
                TestStep clientStep = new TestStep(testShare.client, testContext.client);
                TestStep serverStep = new TestStep(testShare.server, testContext.server)) {
            testShare.client = testShare.server = null;
            testContext.client = testContext.server = null;

            while (!clientFinished || !serverFinished) {
                if (!clientFinished) {
                    clientFinished = clientStep.step();
                }

                if (clientStep.messageBuf == null) break;
                serverStep.messageBuf = clientStep.messageBuf;
                clientStep.messageBuf = null;

                if (!serverFinished) {
                    serverFinished = serverStep.step();
                }

                clientStep.messageBuf = serverStep.messageBuf;
                serverStep.messageBuf = null;
            }

            testShare.client = clientStep.share;
            testShare.server = serverStep.share;
            testContext.client = clientStep.context;
            testContext.server = serverStep.context;

            clientStep.share = serverStep.share = null;
            clientStep.context = serverStep.context = null;
        }
    }
}
