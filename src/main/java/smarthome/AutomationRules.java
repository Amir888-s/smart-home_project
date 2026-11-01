package smarthome;

import java.util.Objects;

/** Commands are recommendations recorded by the app/log; there is no recursive feedback loop. */
public final class AutomationRules implements Observer<HomeEvent> {
    private final Subject<HomeEvent> commands;
    public AutomationRules(Subject<HomeEvent> commands) { this.commands = Objects.requireNonNull(commands); }
    @Override public void onEvent(HomeEvent event) {
        if (!(event instanceof DeviceStateChanged changed)) return;
        for (Device device : changed.room().devices()) {
            if (device.id().equals(changed.deviceId()) && device instanceof Thermostat thermostat
                    && thermostat.heating() && thermostat.targetCelsius() > 26) {
                commands.publish(new AutomationCommand(device.id(), "Recommend target temperature of 22 C"));
            }
        }
    }
}
