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
            RspCode rspCode = RspCode.init_generate_fail;
            if (MPC22Interaction.ShareType.KEY_TYPE_ECDSA == msg.shareType) {
                try {
                    context = Context.initGenerateEcdsaKey(serverPeer);
                } catch (MPCException e) {
                    log.error("initGenerateEcdsaKey err, mpcErrCode={}, userId={}, interaction={}:", e.errorCode, getUserId(), msg, e);
                    failRsp(rspCode.errCode, rspCode.errMsg + ":" + msg.shareType + ", mpcErrCode=" + e.errorCode);
                    return;
                }
            } else if (MPC22Interaction.ShareType.KEY_TYPE_EDDSA == msg.shareType) {
                try {
                    context = Context.initGenerateEddsaKey(serverPeer);
                } catch (MPCException e) {
                    log.error("initGenerateEddsaKey err, mpcErrCode={}, userId={}, interaction={}:", e.errorCode, getUserId(), msg, e);
                    failRsp(rspCode.errCode, rspCode.errMsg + ":" + msg.shareType + ", mpcErrCode=" + e.errorCode);
                    return;
                }
            } else {
                log.error("generate_command err, unsupported interactionType,  userId={}, interaction={}:", getUserId(), msg);
                failRsp(rspCode.errCode, rspCode.errMsg + ":unsupported interactionType=" + msg.shareType);
                return;
            }
        } else if (MPC22Interaction.Command.refresh.equals(msg.command)) {
            // load share
            share = this.loadShareAutoFailRsp(msg.shareType, msg.shareUid);
            if (share == null) {
                log.error("loadShare err: userId={}, interaction={}", getUserId(), msg);
                return;
            }
            try {
                context = share.initRefreshKey(serverPeer);
            } catch (MPCException e) {
                log.error("share.initRefreshKey err, mpcErrCode={}, userId={}, interaction={}:", e.errorCode, getUserId(), msg, e);
                RspCode rspCode = RspCode.init_refresh_fail;
                failRsp(rspCode.errCode, rspCode.errMsg + ":" + e.errorCode);
                return;
            }
        } else if (MPC22Interaction.Command.sign.equals(msg.command)) {
            // load share
            share = this.loadShareAutoFailRsp(msg.shareType, msg.shareUid);
            if (share == null) {
                log.error("loadShare err: userId={}, interaction={}", getUserId(), msg);
                return;
            }

            RspCode rspCode = RspCode.init_sign_fail;
            if (MPC22Interaction.ShareType.KEY_TYPE_ECDSA == msg.shareType) {
                try {
                    context = share.initEcdsaSign(serverPeer, msg.rawBytes, msg.refreshWhenSign);
                } catch (MPCException e) {
                    log.error("initEcdsaSign err, mpcErrCode={}, userId={}, interaction={}:", e.errorCode, getUserId(), msg, e);
                    failRsp(rspCode.errCode, rspCode.errMsg + ":" + e.errorCode);
                    return;
                }
            } else if (MPC22Interaction.ShareType.KEY_TYPE_EDDSA == msg.shareType) {
                try {
                    context = share.initEddsaSign(serverPeer, msg.rawBytes, msg.refreshWhenSign);
                } catch (MPCException e) {
                    log.error("initEddsaSign err, mpcErrCode={}, userId={}, interaction={}:", e.errorCode, getUserId(), msg, e);
                    failRsp(rspCode.errCode, rspCode.errMsg + ":" + e.errorCode);
                    return;
                }
            } else {
                log.error("sign_command err, unsupported interactionType,  userId={}, interaction={}:", getUserId(), msg);
                failRsp(rspCode.errCode, rspCode.errMsg + ":unsupported interactionType=" + msg.shareType);
                return;
            }
        }  else {
            RspCode rspCode = RspCode.parameter_invalid;
            log.error("mpc22 err, unsupported command,  userId={}, interaction={}:", getUserId(), msg);
            failRsp(rspCode.errCode, rspCode.errMsg + ":unsupported mpc22 command=" + msg.command);
            return;
        }


        oracleStep = new OracleStep(share, context);
        ConnectionHolder.addConnection(channel, oracleStep);
        oracleStep.step(this, msg);
    }


    private Share loadShareAutoFailRsp(int shareType, long shareUid) {
        RspCode rspCode = RspCode.load_share_fail;

        byte[] shareBuf = MPC22Sink.loadShare(serverPeer, getUserId(), shareType, shareUid);

        if (shareBuf == null) {
            // send fail rsp
            log.error("loadShare err: can not load share with MPC22Sink");
            failRsp(rspCode.errCode, rspCode.errMsg);
            return null;
        }
        try {
            return Share.fromBuf(shareBuf);
        } catch (MPCException e) {
            log.error("loadShare err: fail to call Share.fromBuf:", e);
            // send fail rsp
            failRsp(rspCode.errCode, rspCode.errMsg + ":" + e.errorCode);
            return null;
        }
    }
}
