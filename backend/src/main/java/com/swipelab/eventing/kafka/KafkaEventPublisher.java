package com.swipelab.eventing.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(String topic, Object event) {
        log.info("Publishing event to topic {}: {}", topic, event);
        kafkaTemplate.send(topic, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.debug("Sent message=[" + event + "] with offset=[" +
                                result.getRecordMetadata().offset() + "]");
                    } else {
                        log.error("Unable to send message=[" + event + "] due to : " + ex.getMessage());
                    }
                });
    }
}
