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
| `mixin/ServerPlayerRulesMixin.java`, sprint restriction | Sprint restriction was not explicit about the Hardcore world flag and also applied in The End. | Apply the restriction in the Overworld and Hardcore dimensions using `isHardcore()`, while exempting Nether and The End. |
| `DifficultyScalingRules.java` and mod rule classes | Post-game difficulty values were scattered and did not have a consistent 1.5x/2x escalation path. | Centralize the multiplier; scale the mod's combat, mob, hazard, portal, resource-tax and timing systems to 1.5x, then activate 2x after a Hardcore completion. |
| `EndCompletionRules.java`, `mixin/ServerPlayerEndTeleportMixin.java` | End completion had no return-time message, elapsed game-time report or portal shutdown. | Detect a real End-to-Overworld teleport return, send the requested normal/Hardcore chat sequence, report `ServerLevel.getGameTime()` and remove nearby End Portal blocks. |
| `client/MinecraftModClient.java`, client HUD mixins | The visual prototype was hidden when client-side Hardcore metadata was unavailable, and could be hidden by the F3 debug overlay. | Draw the red `[1.5x HARD]` label through the Fabric HUD callback and render it above the F3 text as well; this is visual-only and does not change gameplay mechanics. |
| `mixin/ServerPlayerOxygenMixin.java` | Dead players continued altitude/underwater logic after death cleanup, reapplying effects and titles. | Skip dead/dying players. |
| `mixin/ServerPlayerOxygenMixin.java`, `OxygenRules.java` | Underwater jump input could reset a non-leather-helmet player's air supply to 20, bypassing the intended oxygen shortage. | Removed the jump-input state and the `setAirSupply(20)` refill path. Underwater oxygen now depletes and damages normally. |
| `OxygenRules.java`, `mixin/TurtleShellAirDrainMixin.java` | Turtle shells added +20 maximum health and drained underwater air faster. | Removed the turtle-shell health modifier and the accelerated air-drain mixin. |
| `mixin/UnderwaterBreathMixin.java` | Leather helmets used vanilla drowning damage underwater instead of the mod's shared oxygen path. | Removed the leather-helmet exception; underwater players now use the same oxygen behavior. |
| `mixin/DoorBlockWaterlogMixin.java` | Underwater doors were forced waterlogged or blocked, preventing the vanilla door air-pocket behavior. | Removed the mixin and its registration; underwater door placement is vanilla again. |
| `GrassCurseRules.java`, inventory scan | Holding grass on the cursor paused its timer and removed night locking; an earlier normal grass stack could replace an existing curse and reset the timer. | Tick/include carried stack, prefer an already active cursed stack, then delete extras. |
| `GrassCurseRules.java`, `shouldForceNight`; `mixin/ServerLevelTimeMixin.java` | Only Overworld residents were checked, and every dimension wrote shared time back to noon. | Check all server players; only Overworld changes time. |
| `GrassCurseRules.java`, teleport | Teleport preserved accumulated fall damage and could select positions outside the border or synchronously generate new chunks. | Reset fallDistance; require loaded chunk and world-border membership. Existing lava/hazard checks remain. |
| `TaxRules.java`, `mixin/ServerPlayerRulesMixin.java` | The existing tax loop only recognized Nether water and used a fixed damage interval, so Overworld water did nothing and post-completion 1.5x/2x timing was ignored. | Reuse the same tax/equipment-damage system for water in all dimensions; scale its damage interval with `DifficultyScalingRules.sooner(...)`. Cursed grass remains excluded from the tax. |
| `TaxRules.java`, Elytra handling | Water-tax durability damage could reach the Elytra break value and remove the item. | Cap Elytra damage at `maxDamage - 1` in both equipment and inventory paths, leaving one durability point instead of breaking it. |
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
| `minecraftmod.mixins.json`, Overworld/Nether fluid mixins | Chunk-tick and fluid-update scans converted Overworld water to lava and Nether lava to water, causing avoidable work and first-entry lag. | Deliberate design decision: disable all eight water/lava conversion mixins. Overworld water remains vanilla water, Nether lava remains vanilla lava, and the separate `TaxRules` toxic-Nether-water damage/equipment-tax system is intentionally retained. Existing legacy water blocks in a Nether world are not mass-converted, avoiding another lag spike. |
| `SnowRules.java`, Hardcore snow-golem spawn | The altitude gate was 200 and the spawn check allowed high non-snow biomes. | Lower the gate to Y=130 but require the selected spawn position's biome precipitation to be snow; player-placed snow in a desert or other non-snow biome cannot enable spawning. |
| `SnowRules.java`, Hardcore snow-golem eligibility | Creative players were excluded from the high-altitude spawn trigger. | Creative players can now trigger snow-golem spawning; spectator and `/nomob` exclusions remain, and creative players are still ignored as attack targets. |
| `mixin/SnowIceMeltMixin.java` | The forced daytime setting allowed vanilla light-based snow and ice melting in snowy biomes. | Cancel snow-layer random melting and normal ice melting; packed/blue ice behavior is unchanged. |
| `SnowRules.java`, spawn position search | Only 12 random positions 16–160 blocks away were tested, so a valid snowy surface could be missed repeatedly. | Test the player's own surface first and expand the random search budget while retaining the snow-biome, height, fluid and collision checks. |
| `SnowRules.java`, Hardcore snow-golem support check | Snow-covered terrain and tree-covered snowy terrain could be rejected because the snow layer was treated as the support block and direct sky visibility was required. | Accept a snow layer over a sturdy block as support and allow valid snowy-biome positions beneath tree cover; empty-space, fluid and collision checks remain. |
| `mixin/SnowGolemZombieTargetMixin.java` | Vanilla target goals allowed snow golems and zombies to select each other, creating an unwanted mutual fight. | Cancel both target directions and clear any already-selected pair each mob tick. Snow golems can still target players through the Hardcore rule. |
| `mixin/SnowGolemProtectionMixin.java` | Living attackers, including projectile owners, could still damage Hardcore snow golems. | Cancel damage to snow golems when either the direct or causing damage source is a living entity. Environmental damage remains possible. |
| `mixin/SnowGolemSnowballMixin.java`, `mixin/SnowGolemAccuracyMixin.java` | Snow-golem snowballs used vanilla zero damage and excessive spread, while target handling could allow non-player hits. | Allow snow-golem snowballs to affect players only, give each projectile 1 HP (half a heart) or a 10% chance of 8 HP (4 hearts), and reduce spread from vanilla 12.0 to 3.0 so the single projectile is more accurate without perfect homing. |

## Verification

- Standard Gradle build compiles common and client sources and produces the remapped mod jar.
- `./gradlew.bat -I tools/bug-audit.init.gradle runServer` loads Fabric 1.21.11 in server mode, force-loads server mixin targets and checks eight grass/effect regressions. A result marker is required, since a launcher crash can otherwise still return exit code zero.
- Audit mod lives only in `src/audit` and is not included in the production jar.
- All 15 production JSON files parsed successfully. This checks syntax, not every Minecraft data codec or runtime datapack combination.
- The server audit uses `--initSettings`: no gameplay world is opened, no EULA is accepted and no LAN service is started.
- Multiplayer attribution, interactive inventory/drop/respawn, client rendering, actual combat totals and long-duration gameplay still need an in-game verification pass. Server mixin loading alone does not establish those behaviors.
- Minecraft was open during the build; the existing install task skipped copying the jar into the live mods directory.
