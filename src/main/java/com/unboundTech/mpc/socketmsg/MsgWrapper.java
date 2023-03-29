package com.unboundTech.mpc.socketmsg;


public class MsgWrapper {
    public int seq;
    public long timestamp;
    public int action;
    public byte[] body;
    public String userId;

    public String reqKey;  // only for action=req,rsp
    public int rspCode; // only for action=rsp
    public String rspErrMsg; // only for action=rsp

    public String msgId; // optional,used for ack a notice
    public String noticeType; //only for action=notice,ack

    public static class MsgAction {
        public static int REQ = 1;
        public static int RSP = 2;
        public static int NOTICE = 3;
        public static int ACK = 4;
    }
}

