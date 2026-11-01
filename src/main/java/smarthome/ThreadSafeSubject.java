package smarthome;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** Delivery uses a subscriber snapshot; callbacks run on the publishing thread. */
public class ThreadSafeSubject<E extends HomeEvent> implements Subject<E> {
    private final Map<EventType, CopyOnWriteArrayList<Observer<E>>> listeners;
    private final Consumer<RuntimeException> errorSink;

    public ThreadSafeSubject(Consumer<RuntimeException> errorSink) {
        this.errorSink = Objects.requireNonNull(errorSink);
        var registry = new EnumMap<EventType, CopyOnWriteArrayList<Observer<E>>>(EventType.class);
        for (EventType type : EventType.values()) registry.put(type, new CopyOnWriteArrayList<>());
        listeners = Map.copyOf(registry);
    }
    @Override public void subscribe(EventType type, Observer<E> observer) {
        listeners.get(Objects.requireNonNull(type)).addIfAbsent(Objects.requireNonNull(observer));
    }
    @Override public void unsubscribe(EventType type, Observer<E> observer) {
        listeners.get(Objects.requireNonNull(type)).remove(Objects.requireNonNull(observer));
    }
    @Override public void publish(E event) {
        Objects.requireNonNull(event);
        // Iterator captures an immutable array: handlers may change subscriptions safely.
        for (Observer<E> listener : listeners.get(event.type())) {
            try { listener.onEvent(event); }
            catch (RuntimeException failure) { errorSink.accept(failure); }
        }
    }
}
