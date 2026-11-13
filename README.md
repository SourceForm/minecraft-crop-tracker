# CropTracker

Client-only Fabric mod scaffold for Minecraft Java 26.2. It tracks the crop item entities your client receives while a farming session is running; it does not rely on your inventory, so it suits hopper-based collection.

## Included crop drops

Nether wart, carrots, potatoes, wheat, beetroot, cocoa beans, and sweet berries.

## Controls

| Default key | Action |
| --- | --- |
| Home | Start a new session |
| End | Pause or resume |
| Delete | End the session |
| Insert | Copy the current or last session report |

All four bindings are configurable in Minecraft's Controls screen. The HUD shows the live total, per-crop totals, and drops per hour. On first launch, config/croptracker.properties is created; use it to change the HUD visibility, position, and tracking radius.

## Important accuracy note

The tracker counts `ItemEntity` spawn packets seen within 12 blocks of the local player during an active session. This catches normal crop drops even when a server-side hopper removes them immediately afterward. It cannot count a server that silently transfers drops without sending item entities to the client; no client-only mod can see those drops. Test it on the intended server and adjust the correlation strategy if that server behaves differently.

## Build and run

Use JDK 25 and Gradle 9.5.1, then run:

```powershell
gradle build
gradle runClient
```

The built jar will be in `build/libs`.
