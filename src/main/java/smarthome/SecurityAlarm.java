package smarthome;

import java.util.Objects;
import java.util.function.Supplier;

/** Integration: audit ONLY the event's room, then publish the result back into the system. */
public final class SecurityAlarm implements Observer<HomeEvent> {
    private final Subject<HomeEvent> reports;
    private final Supplier<SecurityAuditVisitor> auditFactory;
    public SecurityAlarm(Subject<HomeEvent> reports, Supplier<SecurityAuditVisitor> auditFactory) {
        this.reports = Objects.requireNonNull(reports);
        this.auditFactory = Objects.requireNonNull(auditFactory);
    }
    @Override public void onEvent(HomeEvent event) {
        if (event instanceof DeviceStateChanged changed) {
            var findings = changed.room().accept(auditFactory.get());
            reports.publish(new SecurityReport(changed.room().id(), changed.version(), findings));
        }
    }
}
