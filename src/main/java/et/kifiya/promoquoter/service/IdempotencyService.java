package et.kifiya.promoquoter.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
@Service
public class IdempotencyService {
    private final Map<String, String> idempotencyStore = new ConcurrentHashMap<>();

    public void storeIdempotencyKey(String idempotencyKey, String orderId) {
        idempotencyStore.put(idempotencyKey, orderId);
    }

    public Optional<String> getProcessedOrderId(String idempotencyKey) {
        return Optional.ofNullable(idempotencyStore.get(idempotencyKey));
    }

    public boolean isKeyProcessed(String idempotencyKey) {
        return idempotencyStore.containsKey(idempotencyKey);
    }
}
