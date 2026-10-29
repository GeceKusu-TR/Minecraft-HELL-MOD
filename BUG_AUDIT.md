# Bug Audit - 2026-09-15

Scope: main/client Java sources, registered mixins, resources and Gradle build.
The fixes below are applied directly to source. Existing unrelated work was preserved.
Paths in the tables are relative to `src/main/java/com/example/minecraftmod/`, unless stated otherwise.

## Crash, Threading And Item Loss

| File / method | Cause | Applied correction |
| --- | --- | --- |
| `mixin/PhantomThroughBlocksMixin.java`, `src/main/resources/minecraftmod.mixins.json` | Phantom cast Entity to EntityAccessor, but the accessor was not registered. First phantom tick could throw ClassCastException. | Registered EntityAccessor. |
| `mixin/FoodDataMixin.java` | Lunge exhaustion causes magic damage; damage can call exhaustion again while lunge state is active, recursively applying damage. | Per-FoodData reentrancy guard, owner check and try/finally. |
| `mixin/SpearLungeMixin.java` | HEAD/RETURN lunge scope was not balanced on exceptions; non-player calls also ended a scope they had not started. | WrapMethod calls original in try/finally and only ends a scope it opened. |
| `mixin/ServerGamePacketListenerGrassCurseDropMixin.java` | HEAD interceptors touched player inventory before vanilla's server-thread handoff. | Intercept after PacketUtils.ensureRunningOnSameThread. |
| Same file, `syncInventory` | broadcastChanges could send nothing because the server slot had not changed, leaving client-predicted deletion visible. | sendAllDataToRemote forces a complete resync. |
| Same file, container and creative handlers | Cursor-outside drops and creative slot clearing bypassed THROW-only protection. | Cancel these operations before slot mutation and resync. Validate container identity. |
| `mixin/InventoryGrassCurseDeathMixin.java` | Only main inventory was retained; equipment.dropAll could delete cursed offhand grass. | Temporarily retain cursed stacks from all inventory slots, call vanilla dropAll, restore in finally. |
| `GrassCurseRules.java`, `mixin/ServerPlayerGrassCurseDropMixin.java` | Cancelling the final drop after callers removed an item could erase grass. Respawn fallback also overwrote slot 0. | Retain removed grass; drop the displaced ordinary item when a full inventory needs a slot. |
| `ReturnRules.java`, `pullBlockToInventory` | Block entity contents were copied into the pulled item and also spilled by vanilla preRemoveSideEffects. | Use UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS when removing the copied block. Also respect spectator/spawn interaction restrictions. |
| `MinecraftMod.java`, `onInitialize` | Startup cleanup targeted a hard-coded global .minecraft/mods folder, potentially deleting jars belonging to another launcher/profile. | Removed the automatic cleanup call from mod startup. |

## Mechanics And Event Attribution

| File / method | Cause | Applied correction |
| --- | --- | --- |
| `mixin/LivingEntityFireDurationMixin.java` | ON_FIRE damage reset its own remaining duration repeatedly, preventing finite burns from ending. | Do not extend duration for ON_FIRE ticks. Contact fire still ignites; hardcore/peaceful persistence remains. |
| `mixin/ServerPlayerRulesMixin.java`, sprint branch | Immediately resuming sprint after the lock kept the transient slowdown modifier throughout the next sprint period. | Clear the expired modifier in the unlocked sprint branch. |
| `mixin/ServerPlayerOxygenMixin.java` | Dead players continued altitude/underwater logic after death cleanup, reapplying effects and titles. | Skip dead/dying players. |
| `GrassCurseRules.java`, inventory scan | Holding grass on the cursor paused its timer and removed night locking; an earlier normal grass stack could replace an existing curse and reset the timer. | Tick/include carried stack, prefer an already active cursed stack, then delete extras. |
| `GrassCurseRules.java`, `shouldForceNight`; `mixin/ServerLevelTimeMixin.java` | Only Overworld residents were checked, and every dimension wrote shared time back to noon. | Check all server players; only Overworld changes time. |
| `GrassCurseRules.java`, teleport | Teleport preserved accumulated fall damage and could select positions outside the border or synchronously generate new chunks. | Reset fallDistance; require loaded chunk and world-border membership. Existing lava/hazard checks remain. |
| `TaxRules.java` | The countdown bar made grass damageable, so toxic-water item wear could destroy it before the aftermath. | Exclude cursed grass from equipment/inventory durability tax. |
| `mixin/WitherBossMixin.java`, `mixin/WitherSkullBlockMixin.java`, `mixin/BlockItemGrassCurseMixin.java`, `AdvancementRules.java` | Boss ticking awarded the nearest player; the old unregistered skull hook was not tied to successful spawning. | Track the block placer in a try/finally scope and award only after addFreshEntity succeeds. Removed nearest-player ticking path; registered corrected skull hook. |
| `PiglinRules.java`, `mixin/PiglinAiMixin.java` | Non-Poop trades left barterer memory behind; ownerless gold could reuse it. UUID-keyed records for dead piglins never expired physically. | Clear on every completed result and replace/clear memory on gold pickup; weak piglin keys release dead entities. |
| `AdvancementRules.java`, `mixin/ServerAdvancementManagerMixin.java` | Revoking vanilla advancements after they fired could not prevent their toast/chat from appearing. | Replace conflicting vanilla criteria with impossible criteria while preserving tree/display metadata. |
| `AdvancementRules.java`, root criterion | Root was awarded every tick and then revoked by the player's tick handler. | Impossible root criterion prevents the repeated award/revoke cycle; child visibility remains driven by the welcome advancement. |
| `WitherRules.java`, drops | Vanilla already drops a Nether Star in code even with an empty loot table; the mod added a second. | Keep vanilla star; custom drop adds only pearls. |
| `WitherRules.java`, effect duration | Math.max(-1, 400) converted an infinite Wither effect into 20 seconds. | Preserve infinite duration sentinel. |
| `AnimalAttackRules.java` | Peaceful/baby handling stopped all navigation every tick, including ordinary wandering. Health mutation also ran on the client. | Server-only rules; clear navigation only when abandoning a player target. |
| Same file, attack handling | Manual attacks ignored line of sight; reflection triggered even when damage was rejected. Lowering max health could leave current health above the new maximum. | Require sight, reflect only accepted hits, clamp excess health when changing maximum. |
| `mixin/PlayerAnimalDamageMixin.java`, `AnimalAttackRules.java` | Inverse armor calculation was applied before difficulty scaling and evaluated twice, so requested heart damage changed in Hard/Easy. | Adjust once at the superclass damage call after difficulty scaling; vanilla armor/effects still apply. |
| `SkeletonTargetingRules.java`, `SkeletonPlayerTargetGoal.java`, `mixin/SkeletonArrowHomingMixin.java`, `mixin/PhantomThroughBlocksMixin.java`, `GhastRules.java` | Visibility-only checks or unconditional acceptance selected creative/spectator/dead players. | Explicit valid-target checks; phantom also respects Peaceful. |
| `NoMobCommands.java`, `mixin/AnimalAttackMixin.java`, `AnimalAttackRules.java` | /nomob removed existing attackers only when the command ran; later attackers were not removed. | Remove mobs targeting enabled players during server AI, including custom animal targeting. |
| `MinecraftMod.java`, `GrassCurseRules.java`, `AdvancementRules.java`, `NoMobCommands.java` | UUID session flags survived disconnect and could leak across single-player worlds. | Disconnect cleanup for aftermath, ghast-burn attribution and /nomob flags. |

## Performance And Bounds

| File / method | Cause | Applied correction |
| --- | --- | --- |
| `PlayerRules.java`, fall acceleration | A loop over every crossed 5-block threshold could run extremely long or overflow its int counter; exponential growth could produce Infinity/NaN. | Constant-time geometric sum and finite-double fallback. No new gameplay speed cap. |
| `AnimalAttackRules.java`, chase | Every animal recalculated navigation every tick and calculated armor inversion twice per attack. | Throttle ongoing navigation refresh; perform damage adjustment once. |
| `EndermanRules.java`, teleport | Failed searches ran again every tick, generating chunks; safety checked only two air blocks for a nearly three-block entity. | Apply cooldown to failed attempts, require loaded chunks/border and three-block headroom. |
| `SnowRules.java` | First loaded-area scan could decorate 1369 chunks per player synchronously; thick snow could overwrite a non-replaceable block above the surface. | Eight-chunk budget per scan; check replaceability before both snow variants. |
| `NetherWaterRules.java` | A new chunk converted its entire lava volume in one tick. | Budget 512 replacements per chunk tick; only mark fully processed chunks complete. |

## Verification

- Standard Gradle build compiles common and client sources and produces the remapped mod jar.
- `./gradlew.bat -I tools/bug-audit.init.gradle runServer` loads Fabric 1.21.11 in server mode, force-loads server mixin targets and checks eight grass/effect regressions. A result marker is required, since a launcher crash can otherwise still return exit code zero.
- Audit mod lives only in `src/audit` and is not included in the production jar.
- All 15 production JSON files parsed successfully. This checks syntax, not every Minecraft data codec or runtime datapack combination.
- The server audit uses `--initSettings`: no gameplay world is opened, no EULA is accepted and no LAN service is started.
- Multiplayer attribution, interactive inventory/drop/respawn, client rendering, actual combat totals and long-duration gameplay still need an in-game verification pass. Server mixin loading alone does not establish those behaviors.
- Minecraft was open during the build; the existing install task skipped copying the jar into the live mods directory.
