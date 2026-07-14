package com.tchalanet.server.app.config.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.support.SimpleValueWrapper;

@DisplayName("CombinedCache")
class CombinedCacheTest {

    private Cache local;
    private Cache remote;

    @BeforeEach
    void setUp() {
        local = mock(Cache.class);
        remote = mock(Cache.class);
    }

    @Nested
    @DisplayName("get")
    class Get {

        @Test
        @DisplayName("should return L1 hit without checking L2")
        void shouldReturnL1Hit() {
            var vw = new SimpleValueWrapper("val");
            when(local.get("k")).thenReturn(vw);

            var cache = new CombinedCache("test", local, remote);
            assertThat(cache.get("k")).isSameAs(vw);
            verify(remote, never()).get("k");
        }

        @Test
        @DisplayName("should promote L2 hit to L1")
        void shouldPromoteL2HitToL1() {
            when(local.get("k")).thenReturn(null);
            var vw = new SimpleValueWrapper("val");
            when(remote.get("k")).thenReturn(vw);

            var cache = new CombinedCache("test", local, remote);
            assertThat(cache.get("k")).isSameAs(vw);
            verify(local).put("k", "val");
        }

        @Test
        @DisplayName("should return null on complete miss")
        void shouldReturnNullOnMiss() {
            when(local.get("k")).thenReturn(null);
            when(remote.get("k")).thenReturn(null);

            var cache = new CombinedCache("test", local, remote);
            assertThat(cache.get("k")).isNull();
        }

        @Test
        @DisplayName("should swallow remote failure and return null")
        void shouldSwallowRemoteFailure() {
            when(local.get("k")).thenReturn(null);
            when(remote.get("k")).thenThrow(new RuntimeException("redis down"));

            var cache = new CombinedCache("test", local, remote);
            assertThat(cache.get("k")).isNull();
            verify(remote).evict("k");
        }
    }

    @Nested
    @DisplayName("get (no remote)")
    class GetNoRemote {

        @Test
        @DisplayName("should degrade gracefully when remote is null")
        void shouldWorkWithoutRemote() {
            when(local.get("k")).thenReturn(null);

            var cache = new CombinedCache("test", local, null);
            assertThat(cache.get("k")).isNull();
        }
    }

    @Nested
    @DisplayName("put")
    class Put {

        @Test
        @DisplayName("should write to both tiers")
        void shouldWriteToBothTiers() {
            var cache = new CombinedCache("test", local, remote);
            cache.put("k", "v");

            verify(remote).put("k", "v");
            verify(local).put("k", "v");
        }

        @Test
        @DisplayName("should evict on null value")
        void shouldEvictOnNull() {
            var cache = new CombinedCache("test", local, remote);
            cache.put("k", null);

            verify(remote).evict("k");
            verify(local).evict("k");
            verify(local, never()).put(any(), any());
        }

        @Test
        @DisplayName("should swallow remote put failure")
        void shouldSwallowRemotePutFailure() {
            org.mockito.Mockito.doThrow(new RuntimeException("redis down")).when(remote).put("k", "v");

            var cache = new CombinedCache("test", local, remote);
            cache.put("k", "v");

            verify(local).put("k", "v");
        }
    }

    @Nested
    @DisplayName("evict")
    class Evict {

        @Test
        @DisplayName("should evict from both tiers")
        void shouldEvictBothTiers() {
            var cache = new CombinedCache("test", local, remote);
            cache.evict("k");

            verify(remote).evict("k");
            verify(local).evict("k");
        }
    }

    @Nested
    @DisplayName("clear")
    class Clear {

        @Test
        @DisplayName("should clear both tiers")
        void shouldClearBothTiers() {
            var cache = new CombinedCache("test", local, remote);
            cache.clear();

            verify(remote).clear();
            verify(local).clear();
        }
    }
}
