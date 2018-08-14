package com.librato.watconf.adapter.file;

import com.google.common.base.Optional;
import com.librato.ExampleConfig;
import com.librato.watchconf.DynamicConfig;
import com.librato.watchconf.adapter.file.DynamicConfigFileAdapter;
import com.librato.watchconf.converter.Converter;
import com.librato.watchconf.converter.YAMLConverter;
import org.junit.Test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class DynamicConfigFileAdapterTest {

    public static class ExampleConfigAdapter extends DynamicConfigFileAdapter<ExampleConfig> {
        public ExampleConfigAdapter(String path, Converter<ExampleConfig, byte[]> converter) throws IOException, InterruptedException {
            super(ExampleConfig.class, path, converter);
        }

        public ExampleConfigAdapter(String path, Converter<ExampleConfig, byte[]> converter, ChangeListener<ExampleConfig> changeListener) throws IOException, InterruptedException {
            super(ExampleConfig.class, path, converter, changeListener);
        }
    }

    @Test
    public void testReadConfig() throws Exception {
        URL url = this.getClass().getResource("/example_config.yml");
        ExampleConfigAdapter exampleConfigAdapter = new ExampleConfigAdapter(url.getFile(), new YAMLConverter<ExampleConfig>());
        exampleConfigAdapter.start();
        Optional<ExampleConfig> exampleConfig = exampleConfigAdapter.get();
        assertTrue(exampleConfig.isPresent());
        assertEquals(1, exampleConfig.get().id);
        assertEquals("foo", exampleConfig.get().name);
        assertEquals(1, exampleConfig.get().things.size());
        assertEquals("thing1", exampleConfig.get().things.get(0).name);
    }

    @Test
    public void testWatchConfig() throws Exception {
        URL url = this.getClass().getResource("/example_config.yml");
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        ExampleConfigAdapter exampleConfigAdapter = new ExampleConfigAdapter(url.getFile(), new YAMLConverter<ExampleConfig>(), new DynamicConfig.ChangeListener<ExampleConfig>() {
            public void onChange(Optional<ExampleConfig> t) {
                countDownLatch.countDown();
            }

            public void onError(Exception ex) {

            }
        });

        exampleConfigAdapter.start();
        ExampleConfig exampleConfig = exampleConfigAdapter.get().get();
        exampleConfig.id = 100;
        exampleConfig.name = "ray";
        exampleConfig.things.clear();

        YAMLConverter<ExampleConfig> exampleConfigYAMLConverter = new YAMLConverter();
        FileOutputStream fileOutputStream = new FileOutputStream(url.getFile());
        fileOutputStream.write(exampleConfigYAMLConverter.fromDomain(exampleConfig));
        fileOutputStream.close();
        countDownLatch.await(20, TimeUnit.SECONDS);
        assertEquals(0, countDownLatch.getCount());
        exampleConfig = exampleConfigAdapter.get().get();
        assertEquals(100, exampleConfig.id);
        assertEquals("ray", exampleConfig.name);
        assertEquals(0, exampleConfig.things.size());
    }
}
