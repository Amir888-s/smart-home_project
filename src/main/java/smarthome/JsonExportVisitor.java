package smarthome;

import java.util.stream.Collectors;

/** Hand-written JSON: every container owns its nested child array in the output. */
public final class JsonExportVisitor implements Visitor<String> {
    private String fields(String type, Element element) {
        return "{\"type\":" + quote(type) + ",\"id\":" + quote(element.id()) + ",\"name\":" + quote(element.name());
    }
    private String deviceFields(String type, Device device) { return fields(type, device) + ",\"watts\":" + device.watts(); }
    @Override public String visit(Home home) {
        return fields("Home", home) + ",\"rooms\":[" + home.rooms().stream().map(r -> r.accept(this)).collect(Collectors.joining(",")) + "]}";
    }
    @Override public String visit(Room room) {
        return fields("Room", room) + ",\"devices\":[" + room.devices().stream().map(d -> d.accept(this)).collect(Collectors.joining(",")) + "]}";
    }
    @Override public String visit(Lamp lamp) { return deviceFields("Lamp", lamp) + ",\"on\":" + lamp.on() + "}"; }
    @Override public String visit(Thermostat thermostat) {
        return deviceFields("Thermostat", thermostat) + ",\"heating\":" + thermostat.heating() + ",\"targetCelsius\":" + thermostat.targetCelsius() + "}";
    }
    @Override public String visit(Camera camera) { return deviceFields("Camera", camera) + ",\"on\":" + camera.on() + ",\"recording\":" + camera.recording() + "}"; }
    @Override public String visit(SmartLock lock) { return deviceFields("SmartLock", lock) + ",\"locked\":" + lock.locked() + "}"; }
    public static String quote(String text) {
        var out = new StringBuilder("\"");
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20 || Character.isSurrogate(c)) out.append(String.format("\\u%04x", (int)c));
                    else out.append(c);
                }
            }
        }
        return out.append('"').toString();
    }
    @Override public String visit(SmokeDetector detector) { return deviceFields("SmokeDetector", detector) + ",\"alarm\":" + detector.alarm() + "}"; }
}
