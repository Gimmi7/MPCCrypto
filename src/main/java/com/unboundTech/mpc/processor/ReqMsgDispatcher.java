package com.unboundTech.mpc.processor;

import com.unboundTech.mpc.socketmsg.MsgWrapper;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class ReqMsgDispatcher {
    @Autowired
    private List<ReqMsgProcessor> reqMsgProcessorList;

    private Map<String, ReqMsgProcessor> reqMsgProcessorMap = new HashMap<>();

    @PostConstruct
    private void initProcessor() {
        if (CollectionUtils.isNotEmpty(reqMsgProcessorList)) {
            for (ReqMsgProcessor processor : reqMsgProcessorList) {
                String reqKey = processor.reqKey();
                if (reqMsgProcessorMap.containsKey(reqKey)) {
                    throw new IllegalStateException("repeat reqMsg processor for:" + reqKey);
                }
                reqMsgProcessorMap.put(reqKey, processor);
            }
            log.info("total reqMsg processor size={} +++++++++++++++++++++++++++++++++++++++", reqMsgProcessorMap.size());
        }
    }

    public void dispatchReqMsg(Channel channel, MsgWrapper msgWrapper) {
        ReqMsgProcessor<?> processor = reqMsgProcessorMap.get(msgWrapper.reqKey);
        if (processor != null) {
            processor.process(channel, msgWrapper);
        } else {
            log.error("not supported reqKey={} for msgWrapper={}", msgWrapper.reqKey, msgWrapper);
        }
    }
}
