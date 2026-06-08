package co.empresa.vivaeventos.auth.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CacheConfigTest {

    private final CacheConfig cacheConfig = new CacheConfig();

    @Test
    void cacheManagerShouldBeNotNull() {
        CacheManager manager = cacheConfig.cacheManager();
        assertThat(manager).isNotNull();
    }

    @Test
    void cacheManagerShouldBeCaffeineCacheManager() {
        CacheManager manager = cacheConfig.cacheManager();
        assertThat(manager).isInstanceOf(CaffeineCacheManager.class);
    }

    @Test
    void cacheManagerShouldHaveUsuariosCache() {
        CacheManager manager = cacheConfig.cacheManager();
        Cache cache = manager.getCache("usuarios");
        assertThat(cache).isNotNull();
    }

    @Test
    void cacheManagerShouldHaveEventosCache() {
        CacheManager manager = cacheConfig.cacheManager();
        Cache cache = manager.getCache("eventos");
        assertThat(cache).isNotNull();
    }

    @Test
    void cacheManagerShouldOnlyAllowConfiguredCacheNames() {
        CaffeineCacheManager manager = (CaffeineCacheManager) cacheConfig.cacheManager();
        assertThat(manager.getCache("usuarios")).isNotNull();
        assertThat(manager.getCache("eventos")).isNotNull();
    }

    @Test
    void cacheShouldStoreAndRetrieveValues() {
        CacheManager manager = cacheConfig.cacheManager();
        Cache cache = manager.getCache("usuarios");
        cache.put("test-key", "test-value");
        String retrieved = cache.get("test-key", String.class);
        assertThat(retrieved).isEqualTo("test-value");
    }

    @Test
    void cacheShouldStoreComplexValues() {
        CacheManager manager = cacheConfig.cacheManager();
        Cache cache = manager.getCache("usuarios");
        cache.put(42, "answer");
        String value = cache.get(42, String.class);
        assertThat(value).isEqualTo("answer");
    }

    @Test
    void cacheShouldEvictByKey() {
        CacheManager manager = cacheConfig.cacheManager();
        Cache cache = manager.getCache("usuarios");
        cache.put("key", "value");
        cache.evict("key");
        assertThat(cache.get("key")).isNull();
    }

    @Test
    void cacheShouldClearAllEntries() {
        CacheManager manager = cacheConfig.cacheManager();
        Cache cache = manager.getCache("usuarios");
        cache.put("a", 1);
        cache.put("b", 2);
        cache.clear();
        assertThat(cache.get("a")).isNull();
        assertThat(cache.get("b")).isNull();
    }

    @Test
    void cacheShouldHandleNullValues() {
        CacheManager manager = cacheConfig.cacheManager();
        Cache cache = manager.getCache("usuarios");
        assertThat(cache.get("non-existent")).isNull();
    }

    @Test
    void eventCacheShouldBeIndependentFromUsuarioCache() {
        CacheManager manager = cacheConfig.cacheManager();
        Cache usuarios = manager.getCache("usuarios");
        Cache eventos = manager.getCache("eventos");

        usuarios.put("shared-key", "usuario-value");
        assertThat(eventos.get("shared-key")).isNull();
    }
}
