package tech.kayys.wayang.workflow.kernel;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Simple in-memory message broker for cluster communication
 */
@ApplicationScoped
public class MessageBroker {

    private static final Logger LOG = LoggerFactory.getLogger(MessageBroker.class);

    private final Map<String, List<Subscription>> subscriptions = new ConcurrentHashMap<>();
    private final Map<String, List<Message>> deadLetterQueue = new ConcurrentHashMap<>();
    private final Map<String, Instant> messageTimestamps = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor;
    private volatile boolean isRunning = false;

    public MessageBroker() {
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "message-broker-cleanup");
            t.setDaemon(true);
            return t;
        });
    }

    public Uni<Boolean> send(String topic, String targetNodeId, Object message) {
        return Uni.createFrom().deferred(() -> {
            if (topic == null || targetNodeId == null || message == null) {
                return Uni.createFrom().item(false);
            }

            String messageId = UUID.randomUUID().toString();
            Instant timestamp = Instant.now();

            Message wrappedMessage = new Message(
                    messageId,
                    topic,
                    targetNodeId,
                    message,
                    timestamp,
                    0 // initial delivery attempt
            );

            // Store timestamp for cleanup
            messageTimestamps.put(messageId, timestamp);

            // Deliver to subscribers
            return deliverMessage(topic, wrappedMessage)
                    .onItem().invoke(success -> {
                        if (success) {
                            LOG.debug("Message {} delivered to topic {}", messageId, topic);
                        } else {
                            // Move to dead letter queue
                            moveToDeadLetterQueue(topic, wrappedMessage);
                            LOG.warn("Message {} failed delivery to topic {}", messageId, topic);
                        }
                    })
                    .onFailure().recoverWithItem(false);
        });
    }

    public Uni<Boolean> broadcast(String topic, Object message) {
        return Uni.createFrom().deferred(() -> {
            if (topic == null || message == null) {
                return Uni.createFrom().item(false);
            }

            List<Subscription> topicSubscriptions = subscriptions.get(topic);
            if (topicSubscriptions == null || topicSubscriptions.isEmpty()) {
                return Uni.createFrom().item(false);
            }

            String messageId = UUID.randomUUID().toString();
            Instant timestamp = Instant.now();

            List<Uni<Boolean>> deliveryUnis = new ArrayList<>();

            for (Subscription subscription : topicSubscriptions) {
                Message wrappedMessage = new Message(
                        messageId,
                        topic,
                        subscription.getNodeId(),
                        message,
                        timestamp,
                        0);

                deliveryUnis.add(deliverToSubscription(subscription, wrappedMessage));
            }

            return Uni.combine().all().unis(deliveryUnis).asList()
                    .map(results -> results.stream().allMatch(Boolean::booleanValue))
                    .onItem().invoke(allDelivered -> {
                        if (allDelivered) {
                            LOG.debug("Broadcast message {} delivered to {} subscribers on topic {}",
                                    messageId, topicSubscriptions.size(), topic);
                        } else {
                            LOG.warn("Broadcast message {} partially delivered on topic {}", messageId, topic);
                        }
                    });
        });
    }

    public Uni<String> subscribe(String topic, String nodeId, Consumer<Object> handler) {
        return Uni.createFrom().deferred(() -> {
            if (topic == null || nodeId == null || handler == null) {
                return Uni.createFrom().failure(
                        new IllegalArgumentException("Topic, nodeId, and handler cannot be null"));
            }

            Subscription subscription = new Subscription(nodeId, handler);
            subscriptions.computeIfAbsent(topic, k -> new CopyOnWriteArrayList<>())
                    .add(subscription);

            String subscriptionId = UUID.randomUUID().toString();
            LOG.debug("Node {} subscribed to topic {} with ID {}", nodeId, topic, subscriptionId);

            // Start cleanup if not already running
            if (!isRunning) {
                startCleanup();
            }

            return Uni.createFrom().item(subscriptionId);
        });
    }

    public Uni<Boolean> unsubscribe(String topic, String subscriptionId) {
        return Uni.createFrom().deferred(() -> {
            List<Subscription> topicSubscriptions = subscriptions.get(topic);
            if (topicSubscriptions == null) {
                return Uni.createFrom().item(false);
            }

            boolean removed = topicSubscriptions.removeIf(
                    sub -> sub.getSubscriptionId().equals(subscriptionId));

            if (removed) {
                LOG.debug("Subscription {} removed from topic {}", subscriptionId, topic);
            }

            return Uni.createFrom().item(removed);
        });
    }

    public Uni<List<Message>> getDeadLetterQueue(String topic) {
        return Uni.createFrom().deferred(() -> {
            List<Message> dlq = deadLetterQueue.get(topic);
            if (dlq == null) {
                return Uni.createFrom().item(List.of());
            }
            return Uni.createFrom().item(List.copyOf(dlq));
        });
    }

    public Uni<Boolean> retryDeadLetter(String topic, String messageId) {
        return Uni.createFrom().deferred(() -> {
            List<Message> dlq = deadLetterQueue.get(topic);
            if (dlq == null) {
                return Uni.createFrom().item(false);
            }

            Optional<Message> messageOpt = dlq.stream()
                    .filter(msg -> msg.getMessageId().equals(messageId))
                    .findFirst();

            if (messageOpt.isPresent()) {
                Message message = messageOpt.get();
                dlq.remove(message);

                // Increment retry count
                Message retryMessage = new Message(
                        message.getMessageId(),
                        message.getTopic(),
                        message.getTargetNodeId(),
                        message.getPayload(),
                        Instant.now(),
                        message.getRetryCount() + 1);

                return deliverMessage(topic, retryMessage)
                        .onItem().invoke(success -> {
                            if (success) {
                                LOG.info("Retried dead letter message {} on topic {}", messageId, topic);
                            } else {
                                // Move back to DLQ if retry failed
                                moveToDeadLetterQueue(topic, retryMessage);
                            }
                        });
            }

            return Uni.createFrom().item(false);
        });
    }

    public Uni<Map<String, Object>> getBrokerStats() {
        return Uni.createFrom().deferred(() -> {
            Map<String, Object> stats = new HashMap<>();

            int totalSubscriptions = subscriptions.values().stream()
                    .mapToInt(List::size)
                    .sum();

            int totalDLQ = deadLetterQueue.values().stream()
                    .mapToInt(List::size)
                    .sum();

            stats.put("topics", subscriptions.size());
            stats.put("totalSubscriptions", totalSubscriptions);
            stats.put("totalDeadLetters", totalDLQ);
            stats.put("activeMessages", messageTimestamps.size());
            stats.put("timestamp", Instant.now().toString());

            return Uni.createFrom().item(stats);
        });
    }

    public Uni<Void> shutdown() {
        return Uni.createFrom().deferred(() -> {
            isRunning = false;
            cleanupExecutor.shutdown();
            subscriptions.clear();
            deadLetterQueue.clear();
            messageTimestamps.clear();

            LOG.info("Message broker shutdown");
            return Uni.createFrom().voidItem();
        });
    }

    private Uni<Boolean> deliverMessage(String topic, Message message) {
        List<Subscription> topicSubscriptions = subscriptions.get(topic);
        if (topicSubscriptions == null || topicSubscriptions.isEmpty()) {
            return Uni.createFrom().item(false);
        }

        // Find subscription for target node
        Optional<Subscription> targetSubscription = topicSubscriptions.stream()
                .filter(sub -> sub.getNodeId().equals(message.getTargetNodeId()))
                .findFirst();

        if (targetSubscription.isPresent()) {
            return deliverToSubscription(targetSubscription.get(), message);
        }

        // If no specific target, deliver to all (for broadcast)
        if (message.getTargetNodeId() == null) {
            List<Uni<Boolean>> deliveryUnis = topicSubscriptions.stream()
                    .map(sub -> deliverToSubscription(sub, message))
                    .toList();

            return Uni.combine().all().unis(deliveryUnis).asList()
                    .map(results -> !results.isEmpty() && results.stream().anyMatch(Boolean::booleanValue));
        }

        return Uni.createFrom().item(false);
    }

    private Uni<Boolean> deliverToSubscription(Subscription subscription, Message message) {
        return Uni.createFrom().deferred(() -> {
            try {
                subscription.getHandler().accept(message.getPayload());

                // Clean up timestamp on successful delivery
                messageTimestamps.remove(message.getMessageId());

                return Uni.createFrom().item(true);
            } catch (Exception e) {
                LOG.error("Error delivering message to subscription {}",
                        subscription.getSubscriptionId(), e);

                // Check retry limit
                if (message.getRetryCount() < 3) {
                    // Schedule retry
                    scheduleRetry(message);
                } else {
                    // Move to dead letter queue
                    moveToDeadLetterQueue(message.getTopic(), message);
                }

                return Uni.createFrom().item(false);
            }
        });
    }

    private void scheduleRetry(Message message) {
        // Schedule retry with exponential backoff
        long delayMs = (long) Math.pow(2, message.getRetryCount()) * 1000; // 2^retryCount seconds

        cleanupExecutor.schedule(() -> {
            Message retryMessage = new Message(
                    message.getMessageId(),
                    message.getTopic(),
                    message.getTargetNodeId(),
                    message.getPayload(),
                    Instant.now(),
                    message.getRetryCount() + 1);

            deliverMessage(message.getTopic(), retryMessage).subscribe().with(
                    success -> LOG.debug("Retry succeeded for message {}", message.getMessageId()),
                    failure -> LOG.warn("Retry failed for message {}", message.getMessageId(), failure));
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    private void moveToDeadLetterQueue(String topic, Message message) {
        deadLetterQueue.computeIfAbsent(topic, k -> new CopyOnWriteArrayList<>())
                .add(message);

        LOG.warn("Message {} moved to dead letter queue for topic {}",
                message.getMessageId(), topic);
    }

    private void startCleanup() {
        if (isRunning) {
            return;
        }

        isRunning = true;
        cleanupExecutor.scheduleAtFixedRate(() -> {
            try {
                cleanupOldMessages();
                cleanupExpiredDLQ();
            } catch (Exception e) {
                LOG.error("Error in message broker cleanup", e);
            }
        }, 1, 1, TimeUnit.MINUTES);

        LOG.debug("Message broker cleanup started");
    }

    private void cleanupOldMessages() {
        Instant now = Instant.now();
        Duration messageTTL = Duration.ofMinutes(10);

        List<String> toRemove = new ArrayList<>();

        for (Map.Entry<String, Instant> entry : messageTimestamps.entrySet()) {
            if (entry.getValue().plus(messageTTL).isBefore(now)) {
                toRemove.add(entry.getKey());
            }
        }

        toRemove.forEach(messageTimestamps::remove);

        if (!toRemove.isEmpty()) {
            LOG.debug("Cleaned up {} old messages", toRemove.size());
        }
    }

    private void cleanupExpiredDLQ() {
        Instant now = Instant.now();
        Duration dlqTTL = Duration.ofHours(24);

        for (List<Message> dlq : deadLetterQueue.values()) {
            dlq.removeIf(message -> message.getTimestamp().plus(dlqTTL).isBefore(now));
        }
    }

    public static class Message {
        private final String messageId;
        private final String topic;
        private final String targetNodeId;
        private final Object payload;
        private final Instant timestamp;
        private final int retryCount;

        public Message(String messageId, String topic, String targetNodeId,
                Object payload, Instant timestamp, int retryCount) {
            this.messageId = messageId;
            this.topic = topic;
            this.targetNodeId = targetNodeId;
            this.payload = payload;
            this.timestamp = timestamp;
            this.retryCount = retryCount;
        }

        // Getters...
        public String getMessageId() {
            return messageId;
        }

        public String getTopic() {
            return topic;
        }

        public String getTargetNodeId() {
            return targetNodeId;
        }

        public Object getPayload() {
            return payload;
        }

        public Instant getTimestamp() {
            return timestamp;
        }

        public int getRetryCount() {
            return retryCount;
        }
    }

    private static class Subscription {
        private final String subscriptionId;
        private final String nodeId;
        private final Consumer<Object> handler;

        public Subscription(String nodeId, Consumer<Object> handler) {
            this.subscriptionId = UUID.randomUUID().toString();
            this.nodeId = nodeId;
            this.handler = handler;
        }

        public String getSubscriptionId() {
            return subscriptionId;
        }

        public String getNodeId() {
            return nodeId;
        }

        public Consumer<Object> getHandler() {
            return handler;
        }
    }
}