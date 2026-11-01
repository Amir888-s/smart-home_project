# Smart home defense practice

## A short project explanation

“My project simulates a smart home with rooms and five device types. Three device sources notify five listeners using my own generic Observer and Subject interfaces. Events carry immutable room snapshots. EnergyTracker is both a listener and a subject, so it republishes aggregated power. The home is a Composite tree. Five visitors calculate power, accumulate security findings, filter devices, export nested JSON, and count device types. A device event causes SecurityAlarm to audit only that event's room, then publish a SecurityReport. Producer threads use an ExecutorService, and shared data is protected by immutable records, atomic references, atomic counters, copy-on-write subscriber lists, and concurrent queues.”

Use this as a starting point, then explain it in your own words and show the corresponding files.

## Likely questions

**Why Observer?** The source should publish one typed update without depending on the mobile display, security logic, power calculation, or logging implementation. Listeners can join and leave at runtime.

**Why push instead of pull?** The event already carries the immutable state that caused it. Pulling later could read a different state under concurrency.

**Where is the observer chain?** DeviceSource publishes DeviceStateChanged; EnergyTracker receives it and publishes EnergySummary; MobileApp and AuditLog receive that summary.

**Why cannot you call visitor.visit(element) directly?** A variable declared as Element has no matching visit(Element) overload. accept runs on the actual element class, where this has the concrete type, and then selects the correct visitor overload.

**Walk through double dispatch.** Element variable → Lamp.accept at runtime → visit(Lamp) overload selected inside that method → PowerUseVisitor.visit(Lamp) runs on the actual visitor.

**Who traverses the tree?** Each visitor. Home and Room only provide data and accept. This supports different return types and correctly nested JSON.

**Which visitor accumulates state?** SecurityAuditVisitor collects findings across the requested subtree. InventoryVisitor collects counts. Each independent run receives a fresh instance.

**What happens when a listener unsubscribes during publication?** Current iterators keep their captured arrays. Future publication snapshots exclude it; already captured callbacks may still run. The test with latches demonstrates this without timing guesses.

**Why not synchronize publish?** That would serialize every callback through the entire subject and make reentrant subscription changes harder to reason about. Copy-on-write arrays let publication iterate safely without an application-wide lock.

**Can events arrive out of order?** Across threads, yes. EnergyTracker compares source-local versions to prevent stale state replacement. Aggregate summaries may still arrive out of order, so final power uses snapshots after worker completion.

**What protects each shared field?** Source room and aggregate reading map: AtomicReference to immutable snapshots. Counts: AtomicLong. Subscribers: CopyOnWriteArrayList. Logs and failures: ConcurrentLinkedQueue. Stateful visitors: confined to one operation.

**What is the exact stress expectation?** Four producers each publish 1,000 events to one subscriber. The count must be exactly 4,000 after the executor terminates.

**What is the power result?** Demo with 100 updates: 1017 W. Test fixture: 1021 W. Explain which fixture you are discussing. Watts measure power, while kWh measures energy over time.

**What changed when adding an operation?** InventoryVisitor, its test file, and README only. No element or Visitor interface changed.

**What changed when adding an element?** SmokeDetector, Visitor, all five visitor classes, Main, the new tests, and README: ten files. Visitor favors stable object types with changing operations.

**What if a callback throws?** Its RuntimeException is recorded by the nonthrowing injected error sink; other listeners still receive the event. Main reports failure after processing. This is not a durable or retrying message system.

**Does shutdown lose events?** The normal finite run shuts down submissions and waits for all jobs and synchronous callbacks. Timeout explicitly cancels unfinished work and reports failure; it does not claim successful lossless processing.

## Live coding exercises

1. Change the automation threshold from 26 to 27 C and update/add boundary tests.
2. Add a MaintenanceVisitor without changing any element. For example, return IDs of cameras that are not recording.
3. Add a low-power filter and prove it returns the expected devices.
4. Subscribe a one-shot observer that removes itself after its first notification, then test it.
5. Add a new device leaf and identify every visitor that must change. Compile before fixing the missing methods to see how the compiler guides you.
6. Add a regression test showing that a version-1 reading cannot overwrite version 2.

Run `mvn clean test` after each actual change. Commit meaningful changes with their real dates. Do not memorize answers without locating the implementation.

## Submission checks

- Verify all 39 official JUnit tests on your machine; the preparation harness was not the JUnit engine.
- Preserve proof commits by cloning the supplied Git bundle.
- Review `git show --stat new-visitor` and `git show --stat new-element`.
- Add your own real progress over the required assignment period; included history is from one session.
- Push your reviewed project and tags to your own GitHub repository.
- Submit its link and the report to LMS.
- Be ready to run the demo and edit the code yourself during the defense.
