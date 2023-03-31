package com.unboundTech.mpc.processor.impl;

import com.unboundTech.mpc.Context;
import com.unboundTech.mpc.MPCException;
import com.unboundTech.mpc.Share;
import com.unboundTech.mpc.helper.MPC22Sink;
import com.unboundTech.mpc.model.MPC22Interaction;
import com.unboundTech.mpc.processor.ReqMsgProcessor;
import com.unboundTech.mpc.server.ConnectionHolder;
import com.unboundTech.mpc.socketmsg.ReqKey;
import com.unboundTech.mpc.socketmsg.RspCode;
import com.unboundTech.mpc.step.MPC22ShareType;
import com.unboundTech.mpc.step.OracleStep;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MPC22Processor extends ReqMsgProcessor<MPC22Interaction> {
    @Override
    public String reqKey() {
        return ReqKey.mpc22;
    }

    private final static int serverPeer = 2;

    @Override
    protected void processMsg(MPC22Interaction msg) {
        log.info("get mpc22 req, command={}, seq={}, +++++++++++++++++++++", msg.command, getMsgWrapper().seq);
        Channel channel = super.getChannel();
        OracleStep oracleStep = ConnectionHolder.getOracleStep(channel);

        if (oracleStep != null && oracleStep.hasContext() && !msg.initContext) {
            oracleStep.step(this, msg);
            return;
        }

        Context context = null;
        Share share = null;

        if (MPC22Interaction.Command.generate.equals(msg.command)) {
            if (MPC22Interaction.Type.ecdsa.equals(msg.type)) {
                try {
                    context = Context.initGenerateEcdsaKey(serverPeer);
                } catch (MPCException e) {
                    log.error("initGenerateEcdsaKey err:", e);
                    return;
                }
            } else if (MPC22Interaction.Type.eddsa.equals(msg.type)) {
                try {
                    context = Context.initGenerateEddsaKey(serverPeer);
                } catch (MPCException e) {
                    log.error("initGenerateEcdsaKey err:", e);
                    return;
                }
            }
        } else if (MPC22Interaction.Command.refresh.equals(msg.command)) {
            // load share
            share = this.loadShareAutoFailRsp(msg.type);
            if (share == null) {
                return;
            }
            try {
                context = share.initRefreshKey(serverPeer);
            } catch (MPCException e) {
                RspCode rspCode = RspCode.init_refresh_fail;
                failRsp(rspCode.errCode, rspCode.errMsg + ":" + e.errorCode);
                return;
            }
        } else if (MPC22Interaction.Command.sign.equals(msg.command)) {
            share = this.loadShareAutoFailRsp(msg.type);
            if (share == null) {
                return;
            }
            if (MPC22Interaction.Type.ecdsa.equals(msg.type)) {
                try {
                    context = share.initEcdsaSign(serverPeer, msg.rawBytes, msg.refreshWhenSign);
                } catch (MPCException e) {
                    RspCode rspCode = RspCode.init_sign_fail;
                    failRsp(rspCode.errCode, rspCode.errMsg + ":" + e.errorCode);
                    return;
                }
            } else if (MPC22Interaction.Type.eddsa.equals(msg.type)) {
                try {
                    context = share.initEddsaSign(serverPeer, msg.rawBytes, msg.refreshWhenSign);
                } catch (MPCException e) {
                    RspCode rspCode = RspCode.init_sign_fail;
                    failRsp(rspCode.errCode, rspCode.errMsg + ":" + e.errorCode);
                    return;
                }
            }
        }


        oracleStep = new OracleStep(share, context);
        ConnectionHolder.addConnection(channel, oracleStep);
        oracleStep.step(this, msg);
    }


    private Share loadShareAutoFailRsp(String interactionType) {
        RspCode rspCode = RspCode.load_share_fail;

        int shareType = 0;
        if (MPC22Interaction.Type.ecdsa.equals(interactionType)) {
            shareType = MPC22ShareType.KEY_TYPE_ECDSA;
        } else if (MPC22Interaction.Type.eddsa.equals(interactionType)) {
            shareType = MPC22ShareType.KEY_TYPE_EDDSA;
        } else if (MPC22Interaction.Type.generic.equals(interactionType)) {
            shareType = MPC22ShareType.KEY_TYPE_GENERIC_SECRET;
        } else {
            // send fail rsp
            failRsp(rspCode.errCode, rspCode.errMsg + ": no supported share for interactionType=" + interactionType);
            return null;
        }

        byte[] shareBuf = MPC22Sink.loadShare(serverPeer, getUserId(), shareType);

        if (shareBuf == null) {
            // send fail rsp
            failRsp(rspCode.errCode, rspCode.errMsg);
            return null;
        }
        try {
            return Share.fromBuf(shareBuf);
        } catch (MPCException e) {
            log.error("loadShare_err: fail to call Share.fromBuf:", e);
            // send fail rsp
            failRsp(rspCode.errCode, rspCode.errMsg + ":" + e.errorCode);
            return null;
        }
    }
}
