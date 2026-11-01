# Smart home explained in simple words

## What this program does

Imagine a small house with a living room, an entrance, and a bedroom. The house has lamps, a heater controlled by a thermostat, a camera, a front-door lock, and a smoke detector. The program simulates these devices. It runs in the terminal; it is not a physical home controller or a graphical mobile application.

When a device changes, several services need to know. The mobile display counts updates, the energy tracker recalculates power, the alarm checks security, automation suggests a cooler thermostat target, and the log saves what happened. Those services are listeners. The device source sends one event and does not need to know how each listener works.

The same house also needs several operations: calculate power, find security problems, select devices, export JSON, and count device types. Visitors implement those operations separately from the device data.

## How the folders fit together

- `src/main/java/smarthome` contains the 33 production Java files.
- `src/test/java/smarthome` contains six JUnit test classes and a shared Fixtures helper.
- `pom.xml` tells Maven to use Java 17 and JUnit 5 and to produce an executable JAR.
- `dist/smart-home.jar` is a ready-to-run copy of the compiled application.
- `validation/VerifyMain.java` runs 11 checks without needing JUnit.
- `docs` contains the report, UML, explanations, and clearly labeled verification evidence.

All production classes share one package so the project is easy to navigate. You can later split the package by responsibility, but that is unnecessary for this small assignment.

## Observer in everyday language

Think about subscribing to notifications from a phone app. You choose which kind of notification you want. You can unsubscribe whenever you want. The app sends notifications without knowing what you will do with them.

`Observer<E>` means “I can receive an event of type E.” Its one method is `onEvent(event)`. `Subject<E>` means “I can keep subscribers and publish events.” It exposes subscribe, unsubscribe, and publish.

`ThreadSafeSubject` implements the subscription system once. `DeviceSource` and `EnergyTracker` inherit that implementation. A DeviceSource is a publisher of room updates. EnergyTracker receives those updates but also publishes energy summaries. That makes a chain: source → energy tracker → mobile app and log.

The event type acts like a notification category. An observer subscribed only to DEVICE_STATE does not receive SECURITY_REPORT events from the same subject. EventType is an enum, a fixed set of named categories.

## Why an event contains a room snapshot

A snapshot is a picture of the data at one moment. In code it is a collection of immutable Java records. The event contains the room as it looked when the update happened. The listeners do not need to ask the source for data again.

This is push delivery. Pull delivery would send a small notification and make listeners fetch current state afterward. With several threads, fetching later could return a different update. The snapshot avoids that mismatch.

Records are short Java classes for holding values. Their fields cannot be reassigned after construction. But a record containing a mutable List would still be unsafe. Therefore Home, Room, SecurityReport, and EnergySummary copy their collections using List.copyOf or Map.copyOf. Every object inside those copied device collections is also immutable.

## Every Observer class

**EventType** defines DEVICE_STATE, ENERGY_SUMMARY, SECURITY_REPORT, and AUTOMATION_COMMAND.

**HomeEvent** is the common event interface. It requires a type() method.

**DeviceStateChanged** is the input event. It stores the source ID, a source-local version number, the full room snapshot, and the changed device's ID. The version starts at 1 and increases with every accepted source update.

**EnergySummary** stores an immutable map of power readings by source and their total. It reports watts, not kWh.

**SecurityReport** stores the affected room ID, event version, and an immutable list of security findings. An empty list means that audit found no problems.

**AutomationCommand** stores a target device ID and a recommended action. The demo records recommendations instead of controlling hardware.

**Observer** defines the receive operation. **Subject** defines subscribe, unsubscribe, and publish. **ThreadSafeSubject** stores subscribers separately for each event type.

**DeviceSource** owns one room. change(replacement) checks that the device belongs to the room, creates a new list replacing the old device, and atomically installs a new versioned room. It then publishes the immutable event once.

**MobileApp** counts events by type with AtomicLong counters. It is the model behind the console display, not an Android or iOS app.

**EnergyTracker** uses PowerUseVisitor on the updated room, stores the newest reading from each source, and publishes a new aggregate summary. It also checks source versions so an older event cannot replace newer data.

**SecurityAlarm** connects the patterns. It receives a device event, creates a fresh security visitor using its injected factory, inspects the event's room, and publishes a SecurityReport.

**AutomationRules** examines the changed device. If it is a heating thermostat with a target above 26 C, it publishes a recommendation to use 22 C.

**AuditLog** puts every event it receives into a concurrent queue. It exposes an immutable snapshot for display and tests.

## Composite in everyday language

Composite means that a structure has groups and individual items behind one common interface. Here Home groups Rooms, and Room groups Devices. Every object is an Element, so every object can accept a visitor.

The three levels are Home → Room → Device. Home and Room are the two container types. Lamps, thermostats, cameras, locks, and smoke detectors are leaves because they have no children. A Room is allowed to be empty, which is useful for construction and tests.

**Element** provides id(), name(), and accept(visitor). **Device** extends Element and adds watts().

**Home** stores its ID, name, and rooms. **Room** stores its ID, name, and devices. Both copy child lists and reject duplicate child IDs within their own scope.

**Lamp** stores whether it is on and its rated power. **Thermostat** stores whether heating is active, its controlled heater demand, and target temperature. **Camera** stores its power, whether it is on, and whether it is recording. **SmartLock** stores whether the door is locked and its standby power. **SmokeDetector** stores whether smoke was detected and its standby power.

**DeviceChecks** shares validation: IDs and names cannot be blank; watts must be finite and nonnegative. Thermostat also rejects non-finite temperature values. This keeps invalid data out of JSON and calculations.

## Visitor in everyday language

Imagine an inspector who walks through the house and does one job. One inspector adds power, another checks security, and another creates a device list. The house does not need to learn a new method every time an inspector gets a new job.

Visitor<R> lists one visit method for each kind of element. R is the result type. In Visitor<Double>, R is Double; in Visitor<String>, R is String. The angle brackets are Java generics. They let the compiler check the result type instead of making you cast Object values.

Each concrete element has this kind of method:

```java
@Override
public <R> R accept(Visitor<R> visitor) {
    return visitor.visit(this);
}
```

Suppose an Element variable actually refers to a Lamp. Calling accept first selects Lamp.accept using the real element type. Inside Lamp.accept, Java knows that this is a Lamp and selects the visit(Lamp) overload. The real visitor object then supplies the power calculation, audit rule, or JSON rule. Those two selections are double dispatch.

A direct visitor.visit(element) call does not work when element is declared as Element: the Visitor interface has methods for Home, Room, and each device, but no visit(Element). Accept provides the concrete type information at the correct place.

## Every Visitor class

**Visitor** defines visit overloads and their generic result type.

**PowerUseVisitor** adds current power. Off lamps, inactive heaters, and off cameras contribute zero. Locks and smoke detectors always contribute their standby watts. Home adds room totals; Room adds device totals. There is no mutable visitor state.

**SecurityAuditVisitor** keeps a list of findings while visiting the whole requested subtree. It flags cameras that are off or not recording, unlocked doors, and smoke alarms. A new visitor starts a new audit. Reusing the same instance would accumulate old findings, so create a fresh one.

**DeviceFilterVisitor** receives a Predicate<Device>. A predicate is a yes/no rule, such as `d -> d.watts() > 100`. It returns the matching leaves as List<Element>. Containers combine the results from their children.

**JsonExportVisitor** builds JSON text manually. Home contains a rooms array; each Room contains a devices array. Every device has a type and its own fields. The quote method escapes characters that would otherwise break the JSON. The export uses no third-party JSON library.

**InventoryVisitor** counts devices by type in a map while traversing the subtree. It was added in its own commit without changing any existing element. It is another stateful visitor and should be fresh for each independent inventory.

Visitors own traversal. The elements do not automatically walk their children. This choice keeps records simple and lets the JSON visitor place brackets and nested arrays correctly.

## How the two patterns work together

A source publishes a room with an unlocked front door. SecurityAlarm receives that event. It does not scan the entire Home. It calls accept on the Room already carried in the event. The security visitor inspects only that room's devices and returns a list containing the unlocked-door finding. SecurityAlarm wraps the list in a new SecurityReport. MobileApp and AuditLog receive the result through Observer again.

Another room may also have a security problem, but that other room must not appear in this event's report. The integration test checks this explicitly.

## The difficult concurrency concepts

A thread is a worker that can run at the same time as another worker. The program starts three producer tasks using an ExecutorService. The executor owns the worker threads. Main can configure its size, but requires at least three threads for this assignment.

A plain integer update such as count++ can lose increments when two threads perform it together. AtomicLong.incrementAndGet performs that update atomically, so each event is counted.

AtomicReference holds a reference that can be replaced safely. DeviceSource stores one immutable versioned room inside it. updateAndGet repeatedly tries to replace the old reference with a new one. If another thread won first, it recalculates from that thread's latest snapshot. This is compare-and-set, or CAS. Its update function has no publication side effects because it may be retried.

CopyOnWriteArrayList copies the subscriber array when a subscription changes. A publishing iterator keeps its original array. That is why a listener can remove itself while the subject is notifying people. Removal does not erase callbacks already captured by another thread, and a new subscriber starts with a later snapshot.

ConcurrentLinkedQueue lets several threads add log entries without corrupting the queue. List.copyOf returns an immutable snapshot to a caller.

Callback order is not globally guaranteed across producer threads. EnergyTracker keeps version numbers to stop older readings from replacing newer ones, but observers may still receive completed summaries in a different order. Final results are calculated after all producers finish, rather than assuming the last delivered summary is the newest global state.

## How the program stops safely

ProducerRunner receives an executor and timeout through its constructor. Its run method submits jobs, stops accepting new jobs with shutdown, and waits for accepted jobs to finish. Because callbacks run synchronously, a producer finishing means its notifications and nested observer chain have also finished. Future.get reports any producer exception.

The normal finite demo drains all events. Timeout is an explicit error: it cancels unfinished work instead of pretending everything completed. Main uses 30 seconds. Java cannot guarantee termination of arbitrary user code that ignores interruption; the supplied jobs are finite.

## Dependency injection and the composition root

Dependency injection means giving an object what it needs from outside. For example, SecurityAlarm receives the report publisher and the visitor factory in its constructor. It does not search a global registry or create a hidden singleton.

Main is the composition root. It creates sources, listeners, visitors, the error queue, and the executor, then connects subscriptions. Business objects eagerly create their internal counters and collections in constructors. The injected audit factory creates a fresh operation for each event because that operation accumulates state; it is not a lazily initialized shared service.

**ProducerRunner** manages the executor lifetime. **Main** assembles and runs the application. These complete the 33 production types described above.

## How to understand the numbers

The unit-test house contains eight devices. The living room uses `10 + 1000 + 8 + 2 = 1020 W`. The bedroom has an off lamp, inactive heating, an off camera, and a 1 W lock, so it uses `0 + 0 + 0 + 1 = 1 W`. The whole house uses 1021 W. It has three security findings: living camera, living lock, and bedroom camera.

The demo house is a different fixture with six devices, including a smoke detector. After the default 100 updates per producer, its final demand is 1017 W. Do not confuse the demo's 1017 W with the test fixture's 1021 W.

Three producers × 100 updates gives 300 input events. Each input produces an energy summary and a security report. Half the thermostat updates produce recommendations: 50. The log receives 300 + 300 + 300 + 50 = 950 events.

## What the two extension commits prove

The InventoryVisitor commit adds a new operation without changing any element. This is where Visitor is convenient.

The SmokeDetector commit adds one new kind of object. It must add a Visitor overload and update all five existing operations. This is where Visitor costs more work. The compiler helps by reporting missing methods. The README lists all three files in the first proof commit and all ten files in the second.

## What you should do before submission

Run `mvn clean test` with real JUnit on your own machine. The 39 test bodies were checked with a local assertion harness during preparation, but the official JUnit engine could not be downloaded there. The separate JDK-only verification program is also available.

Restore the Git bundle to preserve the proof commits, review the code, and create your own GitHub repository. Make genuine progress commits during your assignment period. The included history was created in one preparation session and does not fulfill a two-week development-history requirement. Submit your repository link and report. Practice the questions in DEFENSE_GUIDE.md because the live defense is half the grade.
