package com.example.minecraftmod.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BookViewScreen.class)
public abstract class BookViewScreenMixin {
	@Shadow
	private BookViewScreen.BookAccess bookAccess;

	@Shadow
	private PageButton forwardButton;

	@Shadow
	private PageButton backButton;

	@Shadow
	protected abstract boolean forcePage(int page);

	@Inject(method = "init", at = @At("TAIL"))
	private void minecraftmod$openManagedBookAtSavedPage(CallbackInfo ci) {
		int page = minecraftmod$managedBookOpenPage();
		if (page > 1) {
			forcePage(page - 1);
		}
	}

	@Inject(method = "createMenuControls", at = @At("HEAD"), cancellable = true)
	private void minecraftmod$hideManagedBookDoneButton(CallbackInfo ci) {
		if (minecraftmod$isManagedBook()) {
			ci.cancel();
		}
	}

	@Inject(method = "updateButtonVisibility", at = @At("TAIL"))
	private void minecraftmod$hideManagedBookPageButtons(CallbackInfo ci) {
		if (!minecraftmod$isManagedBook()) {
			return;
		}

		forwardButton.visible = false;
		backButton.visible = false;
	}

	@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
	private void minecraftmod$blockManagedBookEscape(KeyEvent keyEvent, CallbackInfoReturnable<Boolean> cir) {
		if (minecraftmod$isManagedBook() && (keyEvent.key() == 256 || keyEvent.key() == 266 || keyEvent.key() == 267)) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "handleClickEvent", at = @At("HEAD"), cancellable = true)
	private void minecraftmod$sendManagedBookCommandWithoutClosing(ClickEvent clickEvent, CallbackInfoReturnable<Boolean> cir) {
		if (!(clickEvent instanceof ClickEvent.RunCommand runCommand)) {
			return;
		}

		String command = runCommand.command();
		if (!command.startsWith("/minecraftmod_end_terms ") && !command.startsWith("/minecraftmod_test_book ")) {
			return;
		}

		if (Minecraft.getInstance().player != null) {
			Minecraft.getInstance().player.connection.sendCommand(command.substring(1));
		}

		if (command.startsWith("/minecraftmod_end_terms ")) {
			int page = minecraftmod$endTermsPageFromCommand(command);
			if (page >= 1 && page < 18) {
				forcePage(page);
			} else if (page == 18) {
				Minecraft.getInstance().setScreen(null);
			}
		} else {
			int page = minecraftmod$testBookPageFromCommand(command);
			int pageCount = bookAccess == null ? 0 : bookAccess.getPageCount();
			if (page >= 1 && page < pageCount) {
				forcePage(page);
			} else if (page == pageCount) {
				Minecraft.getInstance().setScreen(null);
			}
		}
		cir.setReturnValue(true);
	}

	private boolean minecraftmod$isManagedBook() {
		return minecraftmod$isMarkedBook("minecraftmod_end_terms_book") || minecraftmod$isMarkedBook("minecraftmod_test_book");
	}

	private boolean minecraftmod$isMarkedBook(String marker) {
		if (bookAccess == null || bookAccess.getPageCount() <= 0 || Minecraft.getInstance().player == null) {
			return false;
		}

		ItemStack stack = Minecraft.getInstance().player.getMainHandItem();
		CompoundTag probe = new CompoundTag();
		probe.putBoolean(marker, true);
		return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).matchedBy(probe);
	}

	private int minecraftmod$managedBookOpenPage() {
		if (bookAccess == null || bookAccess.getPageCount() <= 0 || Minecraft.getInstance().player == null) {
			return 1;
		}

		ItemStack stack = Minecraft.getInstance().player.getMainHandItem();
		CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		if (tag.getBooleanOr("minecraftmod_test_book", false)) {
			return Math.max(1, Math.min(bookAccess.getPageCount(), tag.getIntOr("minecraftmod_test_book_open_page", 1)));
		}
		if (tag.getBooleanOr("minecraftmod_end_terms_book", false)) {
			return Math.max(1, Math.min(bookAccess.getPageCount(), tag.getIntOr("minecraftmod_end_terms_open_page", 1)));
		}
		return 1;
	}

	private int minecraftmod$endTermsPageFromCommand(String command) {
		String[] parts = command.split(" ", 3);
		if (parts.length < 2) {
			return -1;
		}

		try {
			return Integer.parseInt(parts[1]);
		} catch (NumberFormatException exception) {
			return -1;
		}
	}

	private int minecraftmod$testBookPageFromCommand(String command) {
		String[] parts = command.split(" ", 4);
		if (parts.length < 3) {
			return -1;
		}

		try {
			return Integer.parseInt(parts[2]);
		} catch (NumberFormatException exception) {
			return -1;
		}
	}
}
