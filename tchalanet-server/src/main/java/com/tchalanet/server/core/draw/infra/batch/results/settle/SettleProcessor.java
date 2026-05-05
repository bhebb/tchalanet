    package com.tchalanet.server.core.draw.infra.batch.results.settle;

    import com.tchalanet.server.common.types.id.DrawId;
    import org.springframework.batch.infrastructure.item.ItemProcessor;
    import org.springframework.stereotype.Component;

    @Component
    public class SettleProcessor implements ItemProcessor<DrawId, DrawId> {

        @Override
        public DrawId process(DrawId item) {
            // pass-through (tu peux transformer en command plus tard)
            return item;
        }
    }
