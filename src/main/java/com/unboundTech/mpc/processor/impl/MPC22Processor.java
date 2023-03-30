package com.unboundTech.mpc.processor.impl;

import com.unboundTech.mpc.Context;
import com.unboundTech.mpc.MPCException;
import com.unboundTech.mpc.Share;
import com.unboundTech.mpc.model.MPC22Interaction;
import com.unboundTech.mpc.processor.ReqMsgProcessor;
import com.unboundTech.mpc.server.ConnectionHolder;
import com.unboundTech.mpc.socketmsg.ReqKey;
import com.unboundTech.mpc.step.OracleStep;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MPC22Processor extends ReqMsgProcessor<MPC22Interaction> {
    @Override
    public String reqKey() {
        return ReqKey.mpc22;
    }

    @Override
    protected void processMsg(MPC22Interaction msg) {
        log.info("get mpc22 req, seq={}, +++++++++++++++++++++", getMsgWrapper().seq);
        Channel channel = super.getChannel();
        OracleStep oracleStep = ConnectionHolder.getOracleStep(channel);

        if (oracleStep != null) {
            oracleStep.step(this, msg);
            return;
        }

        Context context = null;
        Share share = null;

        if (MPC22Interaction.Command.generate.equals(msg.command)) {
            if (MPC22Interaction.Type.ecdsa.equals(msg.type)) {
                try {
                    context = Context.initGenerateEcdsaKey(2);
                } catch (MPCException e) {
                    log.error("initGenerateEcdsaKey err:", e);
                    return;
                }
            } else if (MPC22Interaction.Type.eddsa.equals(msg.type)) {
                try {
                    context = Context.initGenerateEddsaKey(2);
                } catch (MPCException e) {
                    log.error("initGenerateEcdsaKey err:", e);
                    return;
                }
            }
        }

        oracleStep = new OracleStep(share, context);
        ConnectionHolder.addConnection(channel, oracleStep);
        oracleStep.step(this, msg);

    }
}
