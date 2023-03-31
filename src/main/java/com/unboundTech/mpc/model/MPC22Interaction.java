package com.unboundTech.mpc.model;

import com.unboundTech.mpc.processor.ProcessedMsg;

public class MPC22Interaction extends ProcessedMsg {
    /**
     * when initContext=true, server will init a new context
     * with old context discarded
     */
    public boolean initContext;
    public String command;
    public int shareType;
    /**
     * message used by mpc_protocol
     */
    public byte[] messageBuf;


    public long shareUid;

    /**
     * content to be signed
     */
    public byte[] rawBytes;
    public boolean refreshWhenSign;


    public static class Command {
        public static final String generate = "generate";
        public static final String refresh = "refresh";
        public static final String sign = "sign";
    }

    public static class ShareType {
        public static final int KEY_TYPE_EDDSA = 2;
        public static final int KEY_TYPE_ECDSA = 3;
        public static final int KEY_TYPE_GENERIC_SECRET = 4; // bip32 seed share
    }

}
