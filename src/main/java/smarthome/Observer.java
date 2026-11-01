package smarthome;

@FunctionalInterface
public interface Observer<E extends HomeEvent> { void onEvent(E event); }
