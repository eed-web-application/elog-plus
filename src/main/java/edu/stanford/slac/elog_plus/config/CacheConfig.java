package edu.stanford.slac.elog_plus.config;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.spring.cache.HazelcastCacheManager;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableCaching
@Configuration
public class CacheConfig {
    @Bean
    public HazelcastInstance hazelcastInstance() {
        Config config = Config.loadDefault();
//                .addSerializer(MyObject.class, new MyObjectSerializer());
        return Hazelcast.newHazelcastInstance(config);
    }
}
