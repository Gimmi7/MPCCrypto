package com.unboundTech.mpc.processor;

import com.alibaba.fastjson2.JSON;
import com.unboundTech.mpc.helper.MessageSender;
import com.unboundTech.mpc.socketmsg.MsgWrapper;
import com.unboundTech.mpc.utils.GenericUtils;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class ReqMsgProcessor<T extends ProcessedMsg> {
    private ThreadLocal<Channel> channelTL = new ThreadLocal<>();
    private ThreadLocal<MsgWrapper> msgWrapperTL = new ThreadLocal<>();

    public abstract String reqKey();


    protected abstract void processMsg(T msg);


    public void process(Channel channel, MsgWrapper msgWrapper) {
        this.channelTL.set(channel);
        this.msgWrapperTL.set(msgWrapper);
        T msg = this.parseReqBody(msgWrapper.body);
        this.processMsg(msg);
    }

    public void successRsp(byte[] body) {
        MsgWrapper rspWrapper = buildCommonRsp();
        rspWrapper.body = body;
        rspWrapper.rspCode = 200;

        MessageSender.sendBytes(getChannel(), JSON.toJSONBytes(rspWrapper));
    }

    public void failRsp(int errCode, String errMsg) {
        MsgWrapper rspWrapper = buildCommonRsp();

        rspWrapper.rspCode = errCode;
        rspWrapper.rspErrMsg = errMsg;

        MessageSender.sendBytes(getChannel(), JSON.toJSONBytes(rspWrapper));
    }

    private MsgWrapper buildCommonRsp() {
        MsgWrapper req = getMsgWrapper();

        MsgWrapper rspWrapper = new MsgWrapper();
        rspWrapper.seq = req.seq;
        rspWrapper.timestamp = System.currentTimeMillis();
        rspWrapper.action = MsgWrapper.MsgAction.RSP;
        rspWrapper.reqKey = req.reqKey;
        rspWrapper.userId = req.userId;
        return rspWrapper;
    }

    protected Channel getChannel() {
        return this.channelTL.get();
    }

    protected MsgWrapper getMsgWrapper() {
        return this.msgWrapperTL.get();
    }

    public String getUserId() {
        return getMsgWrapper().userId;
    }

    @SuppressWarnings("unchecked")
    private T parseReqBody(byte[] body) {
        if (body == null) {
            return null;
        }
        Class<?> parameterizedType = GenericUtils.getSuperClassGenericType(this.getClass());
        Object obj = JSON.parseObject(body, parameterizedType);

        return (T) obj;
    }
}
