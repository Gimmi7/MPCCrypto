package com.unboundTech.mpc.model;

import com.unboundTech.mpc.processor.ProcessedMsg;

public class MPC22Interaction extends ProcessedMsg {
    public String command; // only for trigger
    public String type;  // only for trigger
    public byte[] messageBuf;

    public static class Command {
        public static String generate = "generate";
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
