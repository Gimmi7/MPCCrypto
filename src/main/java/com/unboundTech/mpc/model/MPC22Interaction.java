package com.unboundTech.mpc.model;

import com.unboundTech.mpc.processor.ProcessedMsg;

public class MPC22Interaction extends ProcessedMsg {
    /**
     * when initContext=true, server will init a new context
     * with old context discarded
     */
    public boolean initContext;
    public String command;
    public String type;
    /**
     * message used by mpc_protocol
     */
    public byte[] messageBuf;


    /**
     * content to be signed
     */
    public byte[] rawBytes;
    public boolean refreshWhenSign;


    /**
     * bip32 seed bits length, [ 128,512 ]
     * recommend 256
     */
    public int seedBits;


    public static class Command {
        public static String generate = "generate";
        public static String refresh = "refresh";
        public static String import_ = "import";
        public static String sign = "sign";
        public static String derive = "derive";
    }

    public static class Type {
        public static String eddsa = "eddsa";
        public static String ecdsa = "ecdsa";
        public static String bip32 = "bip32";
        public static String generic = "generic";
    }


}
