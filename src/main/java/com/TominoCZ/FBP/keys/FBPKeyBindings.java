package com.TominoCZ.FBP.keys;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.input.Keyboard;

public class FBPKeyBindings {

	public static KeyBinding FBPMenu, FBPFreeze, FBPToggle, FBPSweep, FBPFastAdd;

	public static void init() {
		FBPMenu = new KeyBinding("Open Menu", Keyboard.KEY_P, "Fancy Block Particles");
		FBPFreeze = new KeyBinding("Toggle Freeze Effect", Keyboard.KEY_NONE, "Fancy Block Particles");
		FBPToggle = new KeyBinding("Enable/Disable", Keyboard.KEY_NONE, "Fancy Block Particles");
		FBPSweep = new KeyBinding("Kill Particles", Keyboard.KEY_NONE, "Fancy Block Particles");
		FBPFastAdd = new KeyBinding("Blacklist Block", Keyboard.KEY_NONE, "Fancy Block Particles");

		ClientRegistry.registerKeyBinding(FBPMenu);
		ClientRegistry.registerKeyBinding(FBPFreeze);
		ClientRegistry.registerKeyBinding(FBPToggle);
		ClientRegistry.registerKeyBinding(FBPSweep);
		ClientRegistry.registerKeyBinding(FBPFastAdd);
	}
}