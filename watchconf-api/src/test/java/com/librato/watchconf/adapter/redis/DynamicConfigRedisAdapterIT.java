package com.librato.watchconf.adapter.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.librato.ExampleConfig;
import com.librato.watchconf.DynamicConfig;
import com.librato.watchconf.converter.JsonConverter;
import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class DynamicConfigRedisAdapterIT {

    private static final ObjectMapper objectMapper = new ObjectMapper();


    public static class ExampleConfigAdapter extends DynamicConfigRedisAdapter<ExampleConfig> {

        public ExampleConfigAdapter(JedisPool jedis, ChangeListener<ExampleConfig> changeListener) throws Exception {
            super("config", jedis, new JsonConverter<ExampleConfig>(), changeListener);
        }
    }

    @Before
    public void setUp() throws Exception {
        JedisPool pool = new JedisPool(new JedisPoolConfig(), "localhost");
        pool.getResource().del("config");
    }

    @Test
    public void testReadConfig() throws Exception {
        JedisPool pool = new JedisPool(new JedisPoolConfig(), "localhost");
        ExampleConfig ec = new ExampleConfig();
        ec.name = "ray";
        pool.getResource().set("config", objectMapper.writeValueAsString(ec));
        ExampleConfigAdapter exampleConfigAdapter = new ExampleConfigAdapter(pool, null);
        Optional<ExampleConfig> exampleConfig = exampleConfigAdapter.get();
        assertTrue(exampleConfig.isPresent());
        assertEquals("ray", exampleConfig.get().name);
    }

    @Test
    public void testConfigChange() throws Exception {
        final JedisPool pool = new JedisPool(new JedisPoolConfig(), "localhost");
        ExampleConfig ec = new ExampleConfig();
        ec.id = 1;
        ec.name = "foo";
        ec.things.add(new ExampleConfig.Thing("thing1"));
        pool.getResource().set("config", objectMapper.writeValueAsString(ec));

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        new ExampleConfigAdapter(pool, new DynamicConfig.ChangeListener<ExampleConfig>() {
            @Override
            public void changed(Optional<ExampleConfig> t) {
                assertTrue(t.isPresent());
                assertEquals(2, t.get().id);
                assertEquals("ray", t.get().name);
                assertEquals(1, t.get().things.size());
                assertEquals("thing2", t.get().things.get(0).name);
                countDownLatch.countDown();
            }
        });

        Executors.newSingleThreadScheduledExecutor().schedule(new Runnable() {
            @Override
            public void run() {
                ExampleConfig ec = new ExampleConfig();
                ec.id = 2;
                ec.name = "ray";
                ec.things.add(new ExampleConfig.Thing("thing2"));
                try {
                    pool.getResource().set("config", objectMapper.writeValueAsString(ec));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
        }, 5, TimeUnit.SECONDS);

        countDownLatch.await(20, TimeUnit.SECONDS);
        assertTrue(countDownLatch.getCount() == 0);

    }
}
