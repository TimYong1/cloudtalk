package com.zhangwuji.im.imcore.manager;

import android.content.Intent;
import android.text.TextUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.zhangwuji.im.DB.entity.Group;
import com.zhangwuji.im.DB.entity.Message;
import com.zhangwuji.im.DB.entity.PeerEntity;
import com.zhangwuji.im.DB.entity.Session;
import com.zhangwuji.im.DB.entity.User;
import com.zhangwuji.im.config.DBConstant;
import com.zhangwuji.im.DB.DBInterface;
import com.zhangwuji.im.config.MessageConstant;
import com.zhangwuji.im.config.SysConstant;
import com.zhangwuji.im.imcore.callback.Packetlistener;
import com.zhangwuji.im.imcore.entity.AudioMessage;
import com.zhangwuji.im.imcore.entity.ImageMessage;
import com.zhangwuji.im.imcore.entity.TextMessage;
import com.zhangwuji.im.imcore.entity.UnreadEntity;
import com.zhangwuji.im.imcore.event.MessageEvent;
import com.zhangwuji.im.imcore.event.PriorityEvent;
import com.zhangwuji.im.imcore.event.RefreshHistoryMsgEvent;
import com.zhangwuji.im.imcore.event.UnreadEvent;
import com.zhangwuji.im.imcore.service.LoadImageService;
import com.zhangwuji.im.protobuf.IMSwitchService;
import com.zhangwuji.im.protobuf.helper.EntityChangeEngine;
import com.zhangwuji.im.protobuf.helper.Java2ProtoBuf;
import com.zhangwuji.im.protobuf.helper.ProtoBuf2JavaBean;
import com.zhangwuji.im.protobuf.IMBaseDefine;
import com.zhangwuji.im.protobuf.IMMessage;
import com.zhangwuji.im.imcore.support.SequenceNumberMaker;
import com.zhangwuji.im.utils.Logger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.zhangwuji.im.protobuf.IMBaseDefine;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import static com.zhangwuji.im.protobuf.IMBaseDefine.MsgType.MSG_TYPE_NOTICE_FRIEND;
import static com.zhangwuji.im.protobuf.IMBaseDefine.MsgType.MSG_TYPE_NOTICE_FRIEND_VALUE;
import static com.zhangwuji.im.protobuf.IMBaseDefine.MsgType.MSG_TYPE_NOTICE_SYSTEM;
import static com.zhangwuji.im.protobuf.IMBaseDefine.MsgType.MSG_TYPE_SYSTEM;

/**
 * ???????????????
 */
public class IMMessageManager extends IMManager{
    private Logger logger = Logger.getLogger(IMMessageManager.class);
    private static IMMessageManager inst = new IMMessageManager();
    public static IMMessageManager instance() {
        return inst;
    }

    private IMSocketManager imSocketManager = IMSocketManager.instance();
    private IMSessionManager sessionManager = IMSessionManager.instance();
    private DBInterface dbInterface = DBInterface.instance();

    // ????????????????????????????????????
    // todo eric, after testing ok, make it a longer value
    private final long TIMEOUT_MILLISECONDS = 6 * 1000;
    private final long IMAGE_TIMEOUT_MILLISECONDS = 4 * 60 * 1000;


    private long getTimeoutTolerance(Message msg) {
        switch (msg.getDisplayType()){
            case DBConstant.SHOW_IMAGE_TYPE:
                return IMAGE_TIMEOUT_MILLISECONDS;
            default:break;
        }
        return TIMEOUT_MILLISECONDS;
    }

    public void recVideoMsg(IMSwitchService.IMP2PCmdMsg p2pmsg)
    {

        //???????????????????????????????????????????????????????????????????????????
        String msgdata="{\"cmd_id\":66668,\"content\":\"\",\"service_id\":6}";
        IMSwitchService.IMP2PCmdMsg ackp2pmsg= null;
        try {
            ackp2pmsg = IMSwitchService.IMP2PCmdMsg.newBuilder().setToUserId(p2pmsg.getFromUserId()).setFromUserId(p2pmsg.getToUserId()).setCmdMsgData(msgdata).setCmdMsgDataBytes(ByteString.copyFrom(msgdata.getBytes("utf-8"))).build();
        } catch (UnsupportedEncodingException e) {
        }

        int sid = IMBaseDefine.ServiceID.SID_SWITCH_SERVICE_VALUE;
        int cid = IMBaseDefine.SwitchServiceCmdID.CID_SWITCH_P2P_CMD_VALUE;
        imSocketManager.sendRequest(ackp2pmsg,sid,cid);



        PriorityEvent notifyEvent = new PriorityEvent();
        notifyEvent.event = PriorityEvent.Event.MSG_RECEIVED_VIDEO;
        notifyEvent.object = p2pmsg;
        triggerEvent(notifyEvent);
    }

    //????????????????????????
    public void sendVideoactOk(IMSwitchService.IMP2PCmdMsg p2pmsg)
    {
        PriorityEvent notifyEvent = new PriorityEvent();
        notifyEvent.event = PriorityEvent.Event.MSG_SEND_VIDEO_SUCC_ACT;
        notifyEvent.object = p2pmsg;
        triggerEvent(notifyEvent);
    }

    //????????????????????????
    public void endofVideo(IMSwitchService.IMP2PCmdMsg p2pmsg)
    {
        PriorityEvent notifyEvent = new PriorityEvent();
        notifyEvent.event = PriorityEvent.Event.MSG_END_VIDEO;
        notifyEvent.object = p2pmsg;
        triggerEvent(notifyEvent);
    }

    //??????????????????
    public void rejectofVideo(IMSwitchService.IMP2PCmdMsg p2pmsg)
    {
        PriorityEvent notifyEvent = new PriorityEvent();
        notifyEvent.event = PriorityEvent.Event.MSG_REJECT_VIDEO;
        notifyEvent.object = p2pmsg;
        triggerEvent(notifyEvent);
    }

    //??????????????????
    public void agreeofVideo(IMSwitchService.IMP2PCmdMsg p2pmsg)
    {
        PriorityEvent notifyEvent = new PriorityEvent();
        notifyEvent.event = PriorityEvent.Event.MSG_AGREE_VIDEO;
        notifyEvent.object = p2pmsg;
        triggerEvent(notifyEvent);
    }

    /**----------------------???????????????-------------------------------------*/
    /**
     * ????????????????????????????????????
     * MessageManager???????????????
     * ????????????????????????????????????msgId???0
     * ???????????????DB id????????????
     */
    public void sendVideoMessage(int ToUserId,int myuserId,int cmdid,String roomid) {

        String content=roomid;
        String msgdata="{\"cmd_id\":"+cmdid+",\"content\":\""+content+"\",\"service_id\":6}";
        IMSwitchService.IMP2PCmdMsg ackp2pmsg= null;
        try {
            ackp2pmsg = IMSwitchService.IMP2PCmdMsg.newBuilder().setToUserId(ToUserId).setFromUserId(myuserId).setCmdMsgData(msgdata).setCmdMsgDataBytes(ByteString.copyFrom(msgdata.getBytes("utf-8"))).build();
        } catch (UnsupportedEncodingException e) {
        }

        int sid = IMBaseDefine.ServiceID.SID_SWITCH_SERVICE_VALUE;
        int cid = IMBaseDefine.SwitchServiceCmdID.CID_SWITCH_P2P_CMD_VALUE;

        imSocketManager.sendRequest(ackp2pmsg,sid,cid,new Packetlistener(TIMEOUT_MILLISECONDS) {
            @Override
            public void onSuccess(Object response) {
//                try {
//                    PriorityEvent notifyEvent = new PriorityEvent();
//                    notifyEvent.event = PriorityEvent.Event.MSG_SEND_VIDEO_SUCC;
//                    notifyEvent.object = null;
//                    triggerEvent(notifyEvent);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
            }
            @Override
            public void onFaild() {
//                PriorityEvent notifyEvent = new PriorityEvent();
//                notifyEvent.event = PriorityEvent.Event.MSG_SEND_VIDEO_FAIL;
//                notifyEvent.object = null;
//                triggerEvent(notifyEvent);
            }
            @Override
            public void onTimeout() {
//                PriorityEvent notifyEvent = new PriorityEvent();
//                notifyEvent.event = PriorityEvent.Event.MSG_SEND_VIDEO_TIMEOUT;
//                notifyEvent.object = null;
//                triggerEvent(notifyEvent);
            }
        });
    }

    /**
     * ????????????????????????????????????????????????
     * @param msg
     */
    public void ackReceiveMsg(Message msg){
        logger.d("chat#ackReceiveMsg -> msg:%s", msg);
        IMBaseDefine.SessionType sessionType = Java2ProtoBuf.getProtoSessionType(msg.getSessionType());
        IMMessage.IMMsgDataAck imMsgDataAck = IMMessage.IMMsgDataAck.newBuilder()
                .setMsgId(msg.getMsgId())
                .setSessionId(msg.getToId())
                .setUserId(msg.getFromId())
                .setSessionType(sessionType)
                .build();
        int sid = IMBaseDefine.ServiceID.SID_MSG_VALUE;
        int cid = IMBaseDefine.MessageCmdID.CID_MSG_DATA_ACK_VALUE;
        imSocketManager.sendRequest(imMsgDataAck,sid,cid);
    }

    @Override
    public void doOnStart() {
    }

    public void onLoginSuccess(){
        if(!EventBus.getDefault().isRegistered(inst)){
            EventBus.getDefault().register(inst);
        }
    }

    @Override
    public void reset() {
        EventBus.getDefault().unregister(inst);
    }

    /**
     * ?????????????????????
     * @param event
     */
    public void triggerEvent(Object event) {
        EventBus.getDefault().post(event);
    }


    /**???????????????????????????????????????????????????????????????????????????messageActivity???????????????*/
    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onUserEvent(MessageEvent event){
        MessageEvent.Event  type = event.getEvent();
        switch (type){
            case IMAGE_UPLOAD_FAILD:{
                logger.d("pic#onUploadImageFaild");
                ImageMessage imageMessage = (ImageMessage)event.getMessageEntity();
                imageMessage.setLoadStatus(MessageConstant.IMAGE_LOADED_FAILURE);
                imageMessage.setStatus(MessageConstant.MSG_FAILURE);
                dbInterface.insertOrUpdateMessage(imageMessage);

                /**??????Activity??? ??????*/
                event.setEvent(MessageEvent.Event.HANDLER_IMAGE_UPLOAD_FAILD);
                event.setMessageEntity(imageMessage);
                triggerEvent(event);
            }break;

            case IMAGE_UPLOAD_SUCCESS:{
                onImageLoadSuccess(event);
            }break;
        }
    }

    /**
     * ????????????????????????????????????????????????????????????????????????onEventBackgroundThread???
     * ???????????????BackgroundThread????????????????????????????????????
     * ??????????????????????????????????????????
     * ???????????????????????????????????????????????????????????????????????????
     * ?????????????????????UI???????????????????????????????????????????????????????????????????????????????????????
     * ?????????????????????????????????????????????????????????????????????????????????
     * */
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onEventBackgroundThread(RefreshHistoryMsgEvent historyMsgEvent){
        doRefreshLocalMsg(historyMsgEvent);
    }


    /**----------------------???????????????-------------------------------------*/
    /**
     * ??????????????????????????????????????????
     * MessageManager???????????????
     */
    public void sendSystemNoticeMessage(Message msgEntity) {
        logger.d("chat#sendMessage, msg:%s", msgEntity);

        // ??????????????? msg_id ??????0
        // ???????????????1???????????????
        if(!SequenceNumberMaker.getInstance().isFailure(msgEntity.getMsgId())){
            throw new RuntimeException("#sendMessage# msgId is wrong,cause by 0!");
        }

        IMBaseDefine.MsgType msgType = Java2ProtoBuf.getProtoMsgType(msgEntity.getMsgType());
        byte[] sendContent = msgEntity.getSendContent();


        IMMessage.IMMsgData msgData = IMMessage.IMMsgData.newBuilder()
                .setFromUserId(msgEntity.getFromId())
                .setToSessionId(msgEntity.getToId())
                .setSessionType(IMBaseDefine.SessionType.SESSION_TYPE_SINGLE)
                .setMsgId(0)
                .setCreateTime(msgEntity.getCreated())
                .setMsgType(msgType)
                .setMsgData(ByteString.copyFrom(sendContent))  // ???????????????????????? todo ByteString.copyFrom
                .build();
        int sid = IMBaseDefine.ServiceID.SID_MSG_VALUE;
        int cid = IMBaseDefine.MessageCmdID.CID_MSG_DATA_VALUE;


        final Message messageEntity  = msgEntity;
        imSocketManager.sendRequest(msgData,sid,cid,new Packetlistener(getTimeoutTolerance(messageEntity)) {
            @Override
            public void onSuccess(Object response) {
            }
            @Override
            public void onFaild() {
            }
            @Override
            public void onTimeout() {
            }
        });
    }

    /**----------------------???????????????-------------------------------------*/
    /**
     * ????????????????????????????????????
     * MessageManager???????????????
     * ????????????????????????????????????msgId???0
     * ???????????????DB id????????????
     */
    public void sendMessage(Message msgEntity) {
        logger.d("chat#sendMessage, msg:%s", msgEntity);
        // ??????????????? msg_id ??????0
        // ???????????????1???????????????
        if(!SequenceNumberMaker.getInstance().isFailure(msgEntity.getMsgId())){
            throw new RuntimeException("#sendMessage# msgId is wrong,cause by 0!");
        }

        IMBaseDefine.MsgType msgType = Java2ProtoBuf.getProtoMsgType(msgEntity.getMsgType());
        byte[] sendContent = msgEntity.getSendContent();


        IMMessage.IMMsgData msgData = IMMessage.IMMsgData.newBuilder()
                .setFromUserId(msgEntity.getFromId())
                .setToSessionId(msgEntity.getToId())
                .setMsgId(0)
                .setCreateTime(msgEntity.getCreated())
                .setMsgType(msgType)
                .setSessionType(IMBaseDefine.SessionType.valueOf(msgEntity.getSessionType()))
                .setMsgData(ByteString.copyFrom(sendContent))  // ???????????????????????? todo ByteString.copyFrom
                .build();
        int sid = IMBaseDefine.ServiceID.SID_MSG_VALUE;
        int cid = IMBaseDefine.MessageCmdID.CID_MSG_DATA_VALUE;


        final Message messageEntity  = msgEntity;
        imSocketManager.sendRequest(msgData,sid,cid,new Packetlistener(getTimeoutTolerance(messageEntity)) {
            @Override
            public void onSuccess(Object response) {
                try {
                    IMMessage.IMMsgDataAck imMsgDataAck = IMMessage.IMMsgDataAck.parseFrom((CodedInputStream)response);
                    logger.i("chat#onAckSendedMsg");
                    if(imMsgDataAck.getMsgId() <=0){
                        throw  new RuntimeException("Msg ack error,cause by msgId <=0");
                    }
                    messageEntity.setStatus(MessageConstant.MSG_SUCCESS);
                    messageEntity.setMsgId(imMsgDataAck.getMsgId());
                    /**??????ID???????????????????????????*/
                    dbInterface.insertOrUpdateMessage(messageEntity);
                    /**??????sessionEntity lastMsgId??????*/
                    sessionManager.updateSession(messageEntity);
                    triggerEvent(new MessageEvent(MessageEvent.Event.ACK_SEND_MESSAGE_OK,messageEntity));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void onFaild() {
                messageEntity.setStatus(MessageConstant.MSG_FAILURE);
                dbInterface.insertOrUpdateMessage(messageEntity);
                triggerEvent(new MessageEvent(MessageEvent.Event.ACK_SEND_MESSAGE_FAILURE,messageEntity));
            }
            @Override
            public void onTimeout() {
                messageEntity.setStatus(MessageConstant.MSG_FAILURE);
                dbInterface.insertOrUpdateMessage(messageEntity);
                triggerEvent(new MessageEvent(MessageEvent.Event.ACK_SEND_MESSAGE_TIME_OUT,messageEntity));
            }
        });
    }

    /**
     * ???????????????????????????
     * 1. ?????????????????????
     * 2. ?????????????????????,????????????????????????
     * 3. ????????????DB[insertOrreplace]??????session????????????Unread?????????
     * 4????????????
     * @param imMsgData
     */
    public void onRecvMessage(IMMessage.IMMsgData imMsgData) {
        logger.i("chat#onRecvMessage");
        if (imMsgData == null) {
            logger.e("chat#decodeMessageInfo failed,cause by is null");
            return;
        }

        Message recvMessage = ProtoBuf2JavaBean.getMessageEntity(imMsgData);
        IMBaseDefine.SessionType sessionType=imMsgData.getSessionType();

//        //?????????????????????????????????
//        if(sessionType==IMBaseDefine.SessionType.SESSION_TYPE_SYSTEM || sessionType==IMBaseDefine.SessionType.SESSION_TYPE_NOTICE) {
//            //?????????????????????????????????
//            if(sessionType==IMBaseDefine.SessionType.SESSION_TYPE_NOTICE)
//            {
        if(recvMessage.getFromId()==1 || imMsgData.getMsgType()==MSG_TYPE_NOTICE_FRIEND || imMsgData.getMsgType()== MSG_TYPE_NOTICE_SYSTEM  ||  imMsgData.getMsgType()== MSG_TYPE_SYSTEM)
        {
            switch (imMsgData.getMsgType())
            {
                case MSG_TYPE_NOTICE_FRIEND:
                {
                    if(recvMessage.getContent().contains("FRIEND_INVITE")) {//????????????????????????
                        //???????????????????????????
                        IMUnreadMsgManager.instance().ackReadMsg(recvMessage,IMBaseDefine.SessionType.SESSION_TYPE_SYSTEM);

                        PriorityEvent notifyEvent = new PriorityEvent();
                        notifyEvent.event = PriorityEvent.Event.MSG_FRIEND_INVITE;
                        notifyEvent.object = recvMessage;
                        triggerEvent(notifyEvent);
                    }
                    else  if(recvMessage.getContent().contains("FRIEND_AGEREE")) {//????????????????????????
                        //???????????????????????????
                        IMUnreadMsgManager.instance().ackReadMsg(recvMessage,IMBaseDefine.SessionType.SESSION_TYPE_SYSTEM);

                        PriorityEvent notifyEvent = new PriorityEvent();
                        notifyEvent.event = PriorityEvent.Event.MSG_FRIEND_AGEREE;
                        notifyEvent.object = recvMessage;
                        triggerEvent(notifyEvent);
                    }
                }
                break;
                case  MSG_TYPE_NOTICE_SYSTEM:
                {

                    JSONObject msgobj= JSON.parseObject(recvMessage.getContent());
                    if(msgobj.getString("type").equals("atme")) {
                        //??????????????????
                        //???????????????????????????
                        IMUnreadMsgManager.instance().ackReadMsg(recvMessage);
                        PriorityEvent notifyEvent = new PriorityEvent();
                        notifyEvent.event = PriorityEvent.Event.MSG_NOTICE_SYSTEM;
                        notifyEvent.object = recvMessage;
                        triggerEvent(notifyEvent);

                        JSONObject content = JSONObject.parseObject(recvMessage.getContent());

                        UnreadEntity unreadEntity;
                        unreadEntity = new UnreadEntity();
                        unreadEntity.setUnReadCnt(1);
                        unreadEntity.setPeerId(recvMessage.getPeerId(false));
                        unreadEntity.setSessionType(recvMessage.getSessionType());
                        //unreadEntity.buildSessionKey();
                        unreadEntity.setSessionKey(content.getString("sessionKey"));
                        unreadEntity.setLatestMsgData(content.getString("fromName") + "@??? " + content.getString("message"));
                        unreadEntity.setLaststMsgId(recvMessage.getMsgId());
                        unreadEntity.setFromSessionName(content.getString("groupName"));

                        UnreadEvent unreadEvent = new UnreadEvent();
                        unreadEvent.event = UnreadEvent.Event.SYSTEM_MESSAGES_NOTICE;
                        unreadEvent.entity = unreadEntity;
                        triggerEvent(unreadEvent);
                    }
                    else if(msgobj.getString("type").equals("group_dis_msg"))
                    {

                        IMUnreadMsgManager.instance().ackReadMsg(recvMessage);

                        PriorityEvent notifyEvent = new PriorityEvent();
                        notifyEvent.event = PriorityEvent.Event.GROUP_DIS_MESSAGE;
                        notifyEvent.object = recvMessage;
                        triggerEvent(notifyEvent);
                    }

                }
                break;
            }
//            }
//        }
//        else if(recvMessage.getFromId()==1)
//        {
//            JSONObject msgobj= JSON.parseObject(recvMessage.getContent());
//            if(msgobj.getString("type").equals("atme")) {
//                //??????????????????
//                //???????????????????????????
//                IMUnreadMsgManager.instance().ackReadMsg(recvMessage);
//                PriorityEvent notifyEvent = new PriorityEvent();
//                notifyEvent.event = PriorityEvent.Event.MSG_NOTICE_SYSTEM;
//                notifyEvent.object = recvMessage;
//                triggerEvent(notifyEvent);
//
//                JSONObject content = JSONObject.parseObject(recvMessage.getContent());
//
//                UnreadEntity unreadEntity;
//                unreadEntity = new UnreadEntity();
//                unreadEntity.setUnReadCnt(1);
//                unreadEntity.setPeerId(recvMessage.getPeerId(false));
//                unreadEntity.setSessionType(recvMessage.getSessionType());
//                //unreadEntity.buildSessionKey();
//                unreadEntity.setSessionKey(content.getString("sessionKey"));
//                unreadEntity.setLatestMsgData(content.getString("fromName") + "@??? " + content.getString("message"));
//                unreadEntity.setLaststMsgId(recvMessage.getMsgId());
//                unreadEntity.setFromSessionName(content.getString("groupName"));
//
//                UnreadEvent unreadEvent = new UnreadEvent();
//                unreadEvent.event = UnreadEvent.Event.SYSTEM_MESSAGES_NOTICE;
//                unreadEvent.entity = unreadEntity;
//                triggerEvent(unreadEvent);
//            }
//            else if(msgobj.getString("type").equals("group_dis_msg"))
//            {
//
//                IMUnreadMsgManager.instance().ackReadMsg(recvMessage);
//
//                PriorityEvent notifyEvent = new PriorityEvent();
//                notifyEvent.event = PriorityEvent.Event.GROUP_DIS_MESSAGE;
//                notifyEvent.object = recvMessage;
//                triggerEvent(notifyEvent);
//            }
        } else {
            int loginId = IMLoginManager.instance().getLoginId();
            boolean isSend = recvMessage.isSend(loginId);
            recvMessage.buildSessionKey(isSend);
            recvMessage.setStatus(MessageConstant.MSG_SUCCESS);
            /**?????????????????????????????????????????????1,session????????????*/
            IMContactManager.instance().findContact(recvMessage.getFromId(),2);//????????????????????????????????????

            boolean issave=true;
            if(sessionType==IMBaseDefine.SessionType.SESSION_TYPE_GROUP)
            {
                Group group=IMGroupManager.instance().findGroup(recvMessage.getPeerId(false));
                if(group!=null && group.getGroupType()==3) //????????????3 ?????????????????? ???????????????
                {
                    issave=false;
                    PriorityEvent notifyEvent = new PriorityEvent();
                    notifyEvent.event = PriorityEvent.Event.MSG_RECEIVED_CHATROOMMESSAGE;
                    notifyEvent.object = recvMessage;
                    triggerEvent(notifyEvent);
                }
            }

            if(issave)
            {
                dbInterface.insertOrUpdateMessage(recvMessage);
                sessionManager.updateSession(recvMessage);
                /**
                 *  ??????????????????????????????activity?????? ????????????
                 *  1. ??????????????? ?????????session??????
                 *  2. ????????????
                 * */
                PriorityEvent notifyEvent = new PriorityEvent();
                notifyEvent.event = PriorityEvent.Event.MSG_RECEIVED_MESSAGE;
                notifyEvent.object = recvMessage;
                triggerEvent(notifyEvent);
            }
        }


    }


    /**-------------------??????????????????????????????---------??????????????????-------------------------------*/
    /**
     * 1. ?????????DB
     * 2. push???adapter???
     * 3. ??????ack,????????????
     * */
    public void sendText(TextMessage textMessage) {
        logger.i("chat#text#textMessage");
        textMessage.setStatus(MessageConstant.MSG_SENDING);
        long pkId =  DBInterface.instance().insertOrUpdateMessage(textMessage);
        sessionManager.updateSession(textMessage);
        sendMessage(textMessage);
    }

    public void sendVoice(AudioMessage audioMessage) {
        logger.i("chat#audio#sendVoice");
        audioMessage.setStatus(MessageConstant.MSG_SENDING);
        long pkId =  DBInterface.instance().insertOrUpdateMessage(audioMessage);
        sessionManager.updateSession(audioMessage);
        sendMessage(audioMessage);
    }


    public void sendSingleImage(ImageMessage msg){
        logger.d("ImMessageManager#sendImage ");
        ArrayList<ImageMessage> msgList = new ArrayList<>();
        msgList.add(msg);
        sendImages(msgList);
    }

    public void delMessageByMsgId(Long msgId)
    {
        DBInterface.instance().deleteMessageById(msgId);
    }

    /**
     * ??????????????????
     * @param msgList
     */
    public void sendImages(List<ImageMessage> msgList) {
        logger.i("chat#image#sendImages size:%d",msgList.size());
        if(null == msgList || msgList.size() <=0){
            return ;
        }

        int len = msgList.size();
        ArrayList<Message> needDbList = new ArrayList<>();
        for (ImageMessage msg : msgList) {
            needDbList.add(msg);
        }
        DBInterface.instance().batchInsertOrUpdateMessage(needDbList);

        for (ImageMessage msg : msgList) {
            logger.d("chat#pic#sendImage  msg:%s",msg);
            // image message would wrapped as a text message after uploading
            int loadStatus = msg.getLoadStatus();

            switch (loadStatus){
                case MessageConstant.IMAGE_LOADED_FAILURE:
                case MessageConstant.IMAGE_UNLOAD:
                case MessageConstant.IMAGE_LOADING:
                    msg.setLoadStatus(MessageConstant.IMAGE_LOADING);
                    Intent loadImageIntent = new Intent(ctx, LoadImageService.class);
                    loadImageIntent.putExtra(SysConstant.UPLOAD_IMAGE_INTENT_PARAMS,msg);
                    ctx.startService(loadImageIntent);
                    break;
                case MessageConstant.IMAGE_LOADED_SUCCESS:
                    sendMessage(msg);
                    break;
                default:
                    throw new RuntimeException("sendImages#status????????????????????????");
            }
        }
        /**????????????????????????Session??????*/
        sessionManager.updateSession(msgList.get(len-1));
    }

    /**
     * ???????????? message?????????
     * 1.??????DB??????
     * 2.??????DB?????? [????????????]
     * 3.?????????????????????
     * ??????????????????????????????????????????????????????
     * */
    public void resendMessage(Message msgInfo) {
        if (msgInfo == null) {
            logger.d("chat#resendMessage msgInfo is null or already send success!");
            return;
        }
        /**check ??????????????????*/
        if(!SequenceNumberMaker.getInstance().isFailure(msgInfo.getMsgId())){
            // ??????????????????????????????
            msgInfo.setStatus(MessageConstant.MSG_SUCCESS);
            dbInterface.insertOrUpdateMessage(msgInfo);
            triggerEvent(new MessageEvent(MessageEvent.Event.ACK_SEND_MESSAGE_OK,msgInfo));
            return;
        }

        logger.d("chat#resendMessage msgInfo %s",msgInfo);
        /**????????????message ?????????,?????????DB?????????*/
        int nowTime = (int) (System.currentTimeMillis()/1000);
        msgInfo.setUpdated(nowTime);
        msgInfo.setCreated(nowTime);

        /**?????????????????????*/
        int msgType = msgInfo.getDisplayType();
        switch (msgType){
            case DBConstant.SHOW_ORIGIN_TEXT_TYPE:
                sendText((TextMessage)msgInfo);
                break;
            case DBConstant.SHOW_IMAGE_TYPE:
                sendSingleImage((ImageMessage) msgInfo);
                break;
            case DBConstant.SHOW_AUDIO_TYPE:
                sendVoice((AudioMessage)msgInfo); break;
            default:
                throw new IllegalArgumentException("#resendMessage#enum type is wrong!!,cause by displayType"+msgType);
        }
    }



    // ?????????????????? {from MessageActivity}
    public List<Message> loadHistoryMsg(int pullTimes, String sessionKey, PeerEntity peerEntity){
        int lastMsgId = 99999999;
        int lastCreateTime = Integer.MAX_VALUE;
        int count = SysConstant.MSG_CNT_PER_PAGE;
        Session sessionEntity = IMSessionManager.instance().findSession(sessionKey);
        if (sessionEntity != null) {
            // ???????????????????????????????????????sessionEntity?????????
            logger.i("#loadHistoryMsg# sessionEntity is null");
            lastMsgId = sessionEntity.getLatestMsgId();
            // ??????????????????????????????????????????????????????,session???update??????????????????
            //lastCreateTime = sessionEntity.getUpdated();
        }

        if(lastMsgId <1 || TextUtils.isEmpty(sessionKey)){
            return Collections.emptyList();
        }
        if(count > lastMsgId){
            count = lastMsgId;
        }
        List<Message> msgList = doLoadHistoryMsg(
                pullTimes, peerEntity.getPeerId(),
                peerEntity.getType(),
                sessionKey, lastMsgId, lastCreateTime, count);

        return msgList;
    }

    // ????????????????????????
    public List<Message> loadHistoryMsg(Message entity, int pullTimes){
        logger.d("IMMessageActivity#LoadHistoryMsg");
        // ??????????????????????????????msgId??????????????????
        int reqLastMsgId = entity.getMsgId() - 1;
        int loginId = IMLoginManager.instance().getLoginId();
        int reqLastCreateTime = entity.getCreated();
        String chatKey = entity.getSessionKey();
        int cnt = SysConstant.MSG_CNT_PER_PAGE;
        List<Message> msgList = doLoadHistoryMsg(pullTimes,
                entity.getPeerId(entity.isSend(loginId)),
                entity.getSessionType(),
                chatKey, reqLastMsgId, reqLastCreateTime, cnt);
        return msgList;
    }

    /**
     * ???DB???????????????
     * 1. ??????????????????????????????????????????
     * 2. ???????????????????????????
     * @param pullTimes
     * @param peerId
     * @param peerType
     * @param sessionKey
     * @param lastMsgId
     * @param lastCreateTime
     * @param count
     * @return
     */
    private List<Message> doLoadHistoryMsg(int pullTimes, final int peerId, final int peerType, final String sessionKey, int lastMsgId, int lastCreateTime, int count){
        if(lastMsgId <1 || TextUtils.isEmpty(sessionKey)){
            return Collections.emptyList();
        }
        if(count > lastMsgId){
            count = lastMsgId;
        }
        // ??????????????????desc
        List<Message> listMsg = dbInterface.getHistoryMsg(sessionKey,lastMsgId,lastCreateTime,count);
        // asyn task refresh
        int resSize = listMsg.size();
        logger.d("LoadHistoryMsg return size is %d",resSize);
        if(resSize==0 || pullTimes == 1 || pullTimes %3==0){
            RefreshHistoryMsgEvent historyMsgEvent = new RefreshHistoryMsgEvent();
            historyMsgEvent.pullTimes = pullTimes;
            historyMsgEvent.count = count;
            historyMsgEvent.lastMsgId = lastMsgId;
            historyMsgEvent.listMsg = listMsg;
            historyMsgEvent.peerId = peerId;
            historyMsgEvent.peerType = peerType;
            historyMsgEvent.sessionKey = sessionKey;
            triggerEvent(historyMsgEvent);
        }
        return listMsg;
    }

    /**
     * asyn task
     * ????????????????????????????????????????????????????????????????????????????????????
     * */
    private void doRefreshLocalMsg(RefreshHistoryMsgEvent hisEvent){
        /**check DB??????????????????*/
        int lastSuccessMsgId = hisEvent.lastMsgId;
        List<Message> listMsg = hisEvent.listMsg;

        int resSize = listMsg.size();
        if(hisEvent.pullTimes > 1) {
            for (int index = resSize - 1; index >= 0; index--) {
                Message entity = listMsg.get(index);
                if (!SequenceNumberMaker.getInstance().isFailure(entity.getMsgId())) {
                    lastSuccessMsgId = entity.getMsgId();
                    break;
                }
            }
        }else{
            /**??????????????????*/
            if(SequenceNumberMaker.getInstance().isFailure(lastSuccessMsgId))
            /**???????????????*/
                for(Message entity:listMsg){
                    if (!SequenceNumberMaker.getInstance().isFailure(entity.getMsgId())) {
                        lastSuccessMsgId = entity.getMsgId();
                        break;
                    }
                }
        }

        final int refreshCnt = hisEvent.count * 3;
        int peerId = hisEvent.peerId;
        int peerType = hisEvent.peerType;
        String sessionKey = hisEvent.sessionKey;
        boolean localFailure =  SequenceNumberMaker.getInstance().isFailure(lastSuccessMsgId);
        if(localFailure){
            logger.e("LoadHistoryMsg# all msg is failure!");
            if(hisEvent.pullTimes ==1){
                reqHistoryMsgNet(peerId,peerType,lastSuccessMsgId,refreshCnt);
            }
        }else {
            /**??????*/
            refreshDBMsg(peerId, peerType, sessionKey, lastSuccessMsgId, refreshCnt);
        }
    }

    /**
     * ?????????????????????DB????????????
     * ???????????????DB??????????????????
     */
    public void refreshDBMsg(int peerId,int peedType,String chatKey,int lastMsgId,int refreshCnt){
        if(lastMsgId <1){return;}
        int beginMsgId = lastMsgId - refreshCnt;
        if(beginMsgId<1){beginMsgId=1;}

        // ????????????????????????
        List<Integer> msgIdList =  dbInterface.refreshHistoryMsgId(chatKey, beginMsgId, lastMsgId);
        if(msgIdList.size() == (lastMsgId-beginMsgId+1)){
            logger.d("refreshDBMsg#do need refresh Message!,cause sizeOfList is right");
            return;
        }
        // ???????????????msgid
        List<Integer> needReqList = new ArrayList<>();
        for(int startIndex=beginMsgId,endIndex=lastMsgId;startIndex<=endIndex;startIndex++){
            if(!msgIdList.contains(startIndex)){
                needReqList.add(startIndex);
            }
        }
        // ?????????????????????
        if(needReqList.size()>0){
            reqMsgById(peerId,peedType,needReqList);
        }
    }


    private void reqMsgById(int peerId,int sessionType,List<Integer> msgIds){
        int userId = IMLoginManager.instance().getLoginId();
        IMBaseDefine.SessionType  sType = Java2ProtoBuf.getProtoSessionType(sessionType);
        IMMessage.IMGetMsgByIdReq  imGetMsgByIdReq = IMMessage.IMGetMsgByIdReq.newBuilder()
                .setSessionId(peerId)
                .setUserId(userId)
                .setSessionType(sType)
                .addAllMsgIdList(msgIds)
                .build();
        int sid = IMBaseDefine.ServiceID.SID_MSG_VALUE;
        int cid = IMBaseDefine.MessageCmdID.CID_MSG_GET_BY_MSG_ID_REQ_VALUE;
        imSocketManager.sendRequest(imGetMsgByIdReq,sid,cid);
    }

    public void onReqMsgById(IMMessage.IMGetMsgByIdRsp rsp){
        int userId = rsp.getUserId();
        int peerId = rsp.getSessionId();
        int sessionType = ProtoBuf2JavaBean.getJavaSessionType(rsp.getSessionType());
        String sessionKey = EntityChangeEngine.getSessionKey(peerId,sessionType);

        List<IMBaseDefine.MsgInfo>  msgList = rsp.getMsgListList();
        if(msgList.size() <=0){
            logger.i("onReqMsgById# have no msgList");
            return;
        }
        List<Message> dbEntity = new ArrayList<>();
        for(IMBaseDefine.MsgInfo msg:msgList){
            Message entity = ProtoBuf2JavaBean.getMessageEntity(msg);
            if(entity == null){
                logger.d("#IMMessageManager# onReqHistoryMsg#analyzeMsg is null,%s",entity);
                continue;
            }

            entity.setSessionKey(sessionKey);
            switch (sessionType){
                case DBConstant.SESSION_TYPE_GROUP:{
                    entity.setToId(peerId);
                }break;
                case DBConstant.SESSION_TYPE_SINGLE:{
                    if(entity.getFromId() == userId){
                        entity.setToId(peerId);
                    }else{
                        entity.setToId(userId);
                    }
                }break;
            }

            dbEntity.add(entity);
        }
        dbInterface.batchInsertOrUpdateMessage(dbEntity);
        /**??????????????????*/
        MessageEvent event = new MessageEvent();
        event.setEvent(MessageEvent.Event.HISTORY_MSG_OBTAIN);
        triggerEvent(event);
    }


    /**
     * network ??????????????????
     */
    public  void reqHistoryMsgNet(int peerId,int peerType, int lastMsgId, int cnt){
        int loginId = IMLoginManager.instance().getLoginId();

        IMMessage.IMGetMsgListReq req = IMMessage.IMGetMsgListReq.newBuilder()
                .setUserId(loginId)
                .setSessionType(Java2ProtoBuf.getProtoSessionType(peerType))
                .setSessionId(peerId)
                .setMsgIdBegin(lastMsgId)
                .setMsgCnt(cnt)
                .build();

        int sid = IMBaseDefine.ServiceID.SID_MSG_VALUE;
        int cid = IMBaseDefine.MessageCmdID.CID_MSG_LIST_REQUEST_VALUE;
        imSocketManager.sendRequest(req,sid,cid);
    }

    /**
     * ???????????????????????????
     * ?????????DB???
     * ?????????????????????????????????
     *
     *??????????????????????????????????????????????????????????????????cnt,?????????????????????????????????????????????????????????????????????????????????
     *??????msg_cnt ??? msg_id_begin???????????????????????????id??????????????????????????????id???????????????????????????????????????????????????
     *????????????????????????????????????????????????????????????
     * */
    public void onReqHistoryMsg(IMMessage.IMGetMsgListRsp rsp){
        // ??????loginId ??????sessionId
        int userId = rsp.getUserId();
        int sessionType = ProtoBuf2JavaBean.getJavaSessionType(rsp.getSessionType());
        int peerId = rsp.getSessionId();
        String sessionKey = EntityChangeEngine.getSessionKey(peerId,sessionType);
        int msgBegin = rsp.getMsgIdBegin();

        List<IMBaseDefine.MsgInfo> msgList = rsp.getMsgListList();

        ArrayList<Message> result = new ArrayList<>();
        for(IMBaseDefine.MsgInfo msgInfo:msgList){
            Message messageEntity = ProtoBuf2JavaBean.getMessageEntity(msgInfo);
            if(messageEntity == null){
                logger.d("#IMMessageManager# onReqHistoryMsg#analyzeMsg is null,%s",messageEntity);
                continue;
            }
            messageEntity.setSessionKey(sessionKey);
            switch (sessionType){
                case DBConstant.SESSION_TYPE_GROUP:{
                    messageEntity.setToId(peerId);
                }break;
                case DBConstant.SESSION_TYPE_SINGLE:{
                    if(messageEntity.getFromId() == userId){
                        messageEntity.setToId(peerId);
                    }else{
                        messageEntity.setToId(userId);
                    }
                }break;
            }
            result.add(messageEntity);
        }
        /**??????????????? check */
        if(result.size()>0) {
            dbInterface.batchInsertOrUpdateMessage(result);
            MessageEvent event = new MessageEvent();
            event.setEvent(MessageEvent.Event.HISTORY_MSG_OBTAIN);
            triggerEvent(event);
        }
    }

    /**?????????????????????????????????*/
    private void onImageLoadSuccess(MessageEvent imageEvent){

        ImageMessage imageMessage = (ImageMessage)imageEvent.getMessageEntity();
        logger.d("pic#onImageUploadFinish");
        String imageUrl = imageMessage.getUrl();
        logger.d("pic#imageUrl:%s", imageUrl);
        String realImageURL = "";
        try {
            realImageURL = URLDecoder.decode(imageUrl, "utf-8");
            logger.d("pic#realImageUrl:%s", realImageURL);
        } catch (UnsupportedEncodingException e) {
            logger.e(e.toString());
        }

        imageMessage.setUrl(realImageURL);
        imageMessage.setStatus(MessageConstant.MSG_SUCCESS);
        imageMessage.setLoadStatus(MessageConstant.IMAGE_LOADED_SUCCESS);
        dbInterface.insertOrUpdateMessage(imageMessage);

        /**??????Activity??? ?????? ??? ????????????*/
        imageEvent.setEvent(MessageEvent.Event.HANDLER_IMAGE_UPLOAD_SUCCESS);
        imageEvent.setMessageEntity(imageMessage);
        triggerEvent(imageEvent);

        imageMessage.setContent(MessageConstant.IMAGE_MSG_START
                + realImageURL + MessageConstant.IMAGE_MSG_END);
        sendMessage(imageMessage);
    }

//    /**??????session????????????????????????*/
//    private void reqSessionLastMsgId(int sessionId,int sessionType,Packetlistener packetlistener){
//        int userId = IMLoginManager.instance().getLoginId();
//        IMMessage.IMGetLatestMsgIdReq latestMsgIdReq = IMMessage.IMGetLatestMsgIdReq.newBuilder()
//                .setUserId(userId)
//                .setSessionId(sessionId)
//                .setSessionType(Java2ProtoBuf.getProtoSessionType(sessionType))
//                .build();
//        int sid = IMBaseDefine.ServiceID.SID_MSG_VALUE;
//        int cid = IMBaseDefine.MessageCmdID.CID_MSG_GET_LATEST_MSG_ID_REQ_VALUE;
//        imSocketManager.sendRequest(latestMsgIdReq,sid,cid,packetlistener);
//    }
//
//    public void onReqSessionLastMsgId(IMMessage.IMGetLatestMsgIdRsp latestMsgIdRsp){
//        int lastMsgId = latestMsgIdRsp.getLatestMsgId();
//    }
}
