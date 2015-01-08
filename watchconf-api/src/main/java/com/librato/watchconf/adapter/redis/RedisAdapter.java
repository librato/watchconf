package com.librato.watchconf.adapter.redis;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.librato.watchconf.adapter.AbstractConfigAdapter;
import com.librato.watchconf.converter.Converter;
import org.apache.log4j.Logger;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class RedisAdapter<T> extends AbstractConfigAdapter<T> {

    private final Logger log = Logger.getLogger(RedisAdapter.class);
    private final JedisPool jedisPool;
    private final String path;
    private final Executor redisExecutor = Executors.newSingleThreadExecutor();

    public RedisAdapter(String path, JedisPool jedisPool, Converter<T> converter) throws Exception {
        this(path, jedisPool, converter, null);
    }

    public RedisAdapter(final String path, final JedisPool jedisPool, Converter<T> converter, ChangeListener<T> changeListener) throws Exception {
        super(converter, Optional.fromNullable(changeListener));
        Preconditions.checkArgument(path != null && !path.isEmpty(), "path cannot be null or blank");
        this.path = path;
        this.jedisPool = jedisPool;
        getAndSet();

        jedisPool.getResource().configSet("notify-keyspace-events", "AKE");
        redisExecutor.execute(new Runnable() {

            @Override
            public void run() {
                jedisPool.getResource().psubscribe(new JedisPubSub() {

                    @Override
                    public void onMessage(String s, String s1) {

                    }

                    @Override
                    public void onPMessage(String s, String s1, String s2) {
                        getAndSet();
                        notifyListeners();
                    }

                    @Override
                    public void onSubscribe(String s, int i) {
                    }

                    @Override
                    public void onUnsubscribe(String s, int i) {

                    }

                    @Override
                    public void onPUnsubscribe(String s, int i) {

                    }

                    @Override
                    public void onPSubscribe(String s, int i) {

                    }
                }, "__key*__:" + path);
            }
        });


    }

    private void getAndSet() {
        try {
            config.set(Optional.of(converter.toDomain(jedisPool.getResource().get(path).getBytes(), clazz)));
        } catch (Exception ex) {
            log.error("unable to parse config", ex);
        }
    }
}
