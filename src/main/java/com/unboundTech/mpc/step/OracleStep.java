package com.unboundTech.mpc.step;

import com.alibaba.fastjson2.JSON;
import com.unboundTech.mpc.Context;
import com.unboundTech.mpc.MPCException;
import com.unboundTech.mpc.Message;
import com.unboundTech.mpc.Share;
import com.unboundTech.mpc.client.SyncClient;
import com.unboundTech.mpc.helper.MPC22Sink;
import com.unboundTech.mpc.model.MPC22Interaction;
import com.unboundTech.mpc.processor.impl.MPC22Processor;
import com.unboundTech.mpc.socketmsg.MsgWrapper;
import com.unboundTech.mpc.socketmsg.ReqKey;
import com.unboundTech.mpc.socketmsg.RspCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

@Slf4j
public class OracleStep implements AutoCloseable {
    private Share share = null;
    private Context context = null;


    public OracleStep(Share share, Context context) {
        this.share = share;
        this.context = context;
    }

    public boolean step(MPC22Processor processor, MPC22Interaction interaction) {
        interaction.initContext = false;
        boolean finished = false;

        byte[] msgBuf = interaction.messageBuf;
        try (
                Message inMessage = (msgBuf == null) ? null : Message.fromBuf(msgBuf);
                Context.MessageAndFlags messageAndFlags = context.step(inMessage);
        ) {
            System.out.println("inMessage=" + inMessage);
            System.out.println("mf=" + ToStringBuilder.reflectionToString(messageAndFlags, ToStringStyle.JSON_STYLE));


            finished = messageAndFlags.protocolFinished;

            if (messageAndFlags.shareChanged) {
                if (share != null) share.close();

                share = context.getShare();
                byte[] shareBuf = share.toBuf();
                share.close();
                share = Share.fromBuf(shareBuf);

                boolean sinkShareFlag = MPC22Sink.sinkShare(shareBuf, context.getInfo().peer, processor.getUserId());
                System.out.println("sinkShareFlag=" + sinkShareFlag);
                // todo ensure all peer sink successfully
            }

            System.out.println("mf.message=" + messageAndFlags.message);
            if (messageAndFlags.message != null) {
                interaction.messageBuf = messageAndFlags.message.toBuf();
                processor.successRsp(JSON.toJSONBytes(interaction));
            } else {
                interaction.messageBuf = null;
                processor.successRsp(JSON.toJSONBytes(interaction));
            }
            System.out.println("send mpc22 Rsp=++++++++++++++++++++++++++++++++++");
            // reset the Context
            if (finished) {
                this.close();
            }
        } catch (Exception e) {
            if (e instanceof MPCException) {
                log.error("step MpcException, errorCode={}:", ((MPCException) e).errorCode, e);
            } else {
                log.error("step exception:", e);
            }
            try {
                this.close();
            } catch (Exception ex) {
                log.error("close oracleStep fail:", ex);
            }
            RspCode rspCode = RspCode.unknown_exception;
            processor.failRsp(rspCode.errCode, rspCode.errMsg + ":" + e.getMessage());
        }

        return finished;
    }

    public boolean clientStep(SyncClient syncClient, MPC22Interaction interaction) {
        boolean finished = false;

        byte[] msgBuf = interaction.messageBuf;
        try (
                Message inMessage = (msgBuf == null) ? null : Message.fromBuf(msgBuf);
                Context.MessageAndFlags messageAndFlags = context.step(inMessage);
        ) {
            System.out.println("inMessage=" + inMessage);
            System.out.println("mf=" + ToStringBuilder.reflectionToString(messageAndFlags, ToStringStyle.JSON_STYLE));

            finished = messageAndFlags.protocolFinished;


            if (messageAndFlags.shareChanged) {
                if (share != null) share.close();

                share = context.getShare();
                byte[] shareBuf = share.toBuf();
                share.close();
                share = Share.fromBuf(shareBuf);
                System.out.printf("shareChanged: %s \n", ToStringBuilder.reflectionToString(share.getInfo(), ToStringStyle.JSON_STYLE));

                boolean sinkShareFlag = MPC22Sink.sinkShare(shareBuf, context.getInfo().peer, syncClient.userId);
                System.out.println("sinkShareFlag=" + sinkShareFlag);
                // todo ensure all peer sink successfully
            }


            System.out.println("mf.message=" + messageAndFlags.message);
            if (messageAndFlags.message != null) {
                interaction.messageBuf = messageAndFlags.message.toBuf();

                MsgWrapper rspWrapper = syncClient.sendReq(ReqKey.mpc22, JSON.toJSONBytes(interaction)).get();
                if (rspWrapper.rspCode == 200) {
                    System.out.println("get rsp");
                    MPC22Interaction rspInteraction = JSON.parseObject(rspWrapper.body, MPC22Interaction.class);
                    if (rspInteraction.messageBuf == null) {
                        return finished;
                    }
                    return clientStep(syncClient, rspInteraction);
                } else {
                    log.error("req fail:{}", ToStringBuilder.reflectionToString(rspWrapper, ToStringStyle.JSON_STYLE));
                }
            }
        } catch (Exception e) {
            log.error("step exception:", e);
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

    public boolean hasContext() {
        return this.context != null;
    }

}
