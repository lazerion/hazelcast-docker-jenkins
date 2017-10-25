package com.hazelcast.docker;


import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MemberAcceptanceTest {

    private HazelcastInstance client;

    @Before
    public void before() {
        ClientConfig config = new ClientConfig();
        config.getNetworkConfig().addAddress("localhost");
        client = HazelcastClient.newHazelcastClient(config);
    }


    @Test
    public void shouldDiscoverMember() {
        final String key = RandomStringUtils.randomAlphanumeric(42);
        final String value = RandomStringUtils.randomAlphanumeric(42);
        IMap<String, String> map = client.getMap("data");
        map.put(key, value);
        assertEquals(map.get(key), value);
    }
}
