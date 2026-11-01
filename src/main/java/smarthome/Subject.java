package smarthome;

public interface Subject<E extends HomeEvent> {
    void subscribe(EventType type, Observer<E> observer);
    void unsubscribe(EventType type, Observer<E> observer);
    void publish(E event);
}
