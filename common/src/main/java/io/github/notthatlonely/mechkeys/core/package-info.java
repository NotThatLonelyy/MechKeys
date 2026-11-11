/**
 * MechKeys core: Mechvibes pack format (V1 + V2 single/multi), importer, key-code tables, PCM bank, mixer and sound engine.
 *
 * <p><b>PORTING NOTES (next update)</b> - this package is shared by every loader module (fabric/, forge-1.8.9/ ...):
 * <ul>
 *  <li>It must stay Minecraft-free: no {@code net.minecraft.*}, Forge or Fabric imports. Loader code lives in the loader modules.</li>
 *  <li>It must stay Java 8 source level (no {@code var}, records, {@code List.of}, text blocks ...) because the Forge 1.8.9
 *      toolchain compiles it with JDK 8. Newer modules compile it fine too.</li>
 *  <li>The only third-party classes are JOrbis ({@code com.jcraft.*}, used by {@code OggDecoder}), supplied by the game on 1.8.9.
 *      If a newer game version no longer ships JOrbis, add it as a dependency of that module (and shade / jar-in-jar it there).</li>
 *  <li>Changing a core class changes it for ALL versions - run {@code gradle build} in each module (CI does this) before releasing.</li>
 *  <li>Pack format changes (new Mechvibes config version): extend {@code PackLoader} and add a case to {@code SelfTest}.</li>
 * </ul>
 */
package io.github.notthatlonely.mechkeys.core;
