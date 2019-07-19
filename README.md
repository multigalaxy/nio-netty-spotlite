# spotlite-websocket

* spotlite使用的基于netty框架实现的websocket通信项目，用于直播间礼物消息、聊天消息等的推送

```
├── Nio-netty-spotlite.iml
├── README.md
├── build.xml
├── config.bak.json
├── config.json
├── forbiddenwords.txt
├── hs_err_pid30939.log
├── lang
│   ├── ch.lang.json
│   ├── en.lang.json
│   ├── es.lang.json
│   └── in.lang.json
├── lang.config.json
├── libs
│   ├── commons-codec-1.6.jar
│   ├── commons-logging-1.1.3.jar
│   ├── commons-pool-1.5.4.jar
│   ├── fastjson-1.1.39.jar
│   ├── gson-2.2.4.jar
│   ├── guava-15.0.jar
│   ├── httpasyncclient-4.0.jar
│   ├── httpasyncclient-cache-4.0.jar
│   ├── httpclient-4.3.1.jar
│   ├── httpclient-cache-4.3.1.jar
│   ├── httpcore-4.3.jar
│   ├── httpcore-nio-4.3.jar
│   ├── java-metrics-20151117.jar
│   ├── jedis-2.1.0.jar
│   ├── log4j-api-2.1.jar
│   ├── log4j-core-2.1.jar
│   ├── netty-all-4.0.30.Final.jar
│   └── slf4j-api-1.7.12.jar
├── log4j2.xml
├── src
│   ├── META-INF
│   │   └── MANIFEST.MF
│   └── icblive
│       └── chatserver
│           ├── Chat.java
│           ├── WebSocketMessageHandler.java
│           ├── api
│           │   ├── APICallbackHandler.java
│           │   ├── APIManager.java
│           │   ├── InternalMsgHandler.java
│           │   ├── MessageUtils.java
│           │   ├── RoomMessageQueue.java
│           │   ├── SessionActionHandler.java
│           │   └── SystemMessageManager.java
│           ├── data
│           │   └── LiveCache.java
│           ├── model
│           │   ├── DisableMsgUserData.java
│           │   ├── KickOffUserData.java
│           │   ├── RoomData.java
│           │   ├── UserData.java
│           │   ├── UserSendMsgData.java
│           │   └── json
│           │       ├── ArriveResponseMessage.java
│           │       ├── AudienceListMessage.java
│           │       ├── BaseMessage.java
│           │       ├── BaseRequestMessage.java
│           │       ├── BaseResponseMessage.java
│           │       ├── ChatRequestMessage.java
│           │       ├── ChatResponseMessage.java
│           │       ├── DisableChatRequestMessage.java
│           │       ├── DisableChatResponseMessage.java
│           │       ├── EndLiveResponseMessage.java
│           │       ├── ErrorResponseMessage.java
│           │       ├── ExitRoomMessage.java
│           │       ├── GetAudienceListMessage.java
│           │       ├── GiveGiftMessage.java
│           │       ├── JoinRequestMessage.java
│           │       ├── KickOffUserMessage.java
│           │       ├── NoticeMessage.java
│           │       ├── NoticeMessageList.java
│           │       ├── PauseMessage.java
│           │       ├── ResponseMessage.java
│           │       ├── SingSystemMsg.java
│           │       ├── SystemResponseMessage.java
│           │       ├── SystemWsMessage.java
│           │       ├── UserInfo.java
│           │       └── UserMedalInfo.java
│           ├── service
│           │   ├── ChatInitializer.java
│           │   └── LiveChatServer2.java
│           └── utils
│               ├── AbstractClientMsg.java
│               ├── CBUrlParser.java
│               ├── ChangbaConfig.java
│               ├── ChangbaMetrics.java
│               ├── ChatCount.java
│               ├── ChatHelper.java
│               ├── DesHelper.java
│               ├── GzipUtils.java
│               ├── KTVRedisConfig.java
│               ├── LanguageUtils.java
│               ├── LoggerUtils.java
│               ├── RedisHelper.java
│               ├── SimpleClientMsg.java
│               ├── SupportMultiLangClientMsg.java
│               ├── TokenHelper.java
│               ├── UsersHelper.java
│               └── UtilFunc.java
├── startwebsocket.sh
├── svn_version.sh
├── ws_default_error.log
└── ws_info_all.log
```