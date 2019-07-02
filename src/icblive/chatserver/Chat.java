package icblive.chatserver;

import redis.clients.jedis.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Chat {
    public static JedisPool pool;

    static {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxActive(500);
        config.setMaxIdle(5);
        config.setMaxWait(1000 * 10);
        config.maxActive = 500;
        config.testWhileIdle = true;
        config.timeBetweenEvictionRunsMillis = 3000;
        pool = new JedisPool(config, "127.0.0.1", 6379);
    }

    public static void main(String[] args) throws java.lang.Exception {
        Jedis jedis = pool.getResource();
        jedis.connect();
        Pipeline pipeline = jedis.pipelined();
        pipeline.hset("key3", "f3", "v3");
        pipeline.hset("key4", "f4", "v4");
        pipeline.sync();
        Map<String, Response<String>> resMap = new HashMap<>();
        for(int i = 3; i < 5; i++) {
            resMap.put("key" + i, pipeline.hget("key" + i, "f" + i));
        }
        pipeline.sync();
        for (Map.Entry<String, Response<String>> entry : resMap.entrySet()) {
            System.out.println(entry.getValue().get());
        }

        List<Response> list = new ArrayList<>();
        for(int i = 3; i < 5; i++) {
            list.add(pipeline.hget("key" + i, "f" + i));
        }
        pipeline.sync();
        for(Response res : list) {
            System.out.println(res.get().toString());
        }
        jedis.disconnect();
    }
}
