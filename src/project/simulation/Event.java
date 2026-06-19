package project.simulation;
enum EventType {
    IN, OUT
}
public record Event(double time, EventType type, Server server) implements Comparable<Event> {
    @Override
    public int compareTo(Event other) {
        return Double.compare(this.time, other.time);
    }
}