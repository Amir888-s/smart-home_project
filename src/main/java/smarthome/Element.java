package smarthome;

public interface Element {
    String id();
    String name();
    <R> R accept(Visitor<R> visitor);
}
