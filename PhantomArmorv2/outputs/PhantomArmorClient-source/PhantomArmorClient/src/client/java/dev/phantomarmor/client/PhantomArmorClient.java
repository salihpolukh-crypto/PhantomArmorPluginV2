package dev.phantomarmor.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.netty.buffer.ByteBuf;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class PhantomArmorClient implements ClientModInitializer {
    private static final int PURPLE = 0xFFD76CFF;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GRAY = 0xFFAAAAAA;
    private static final int GREEN = 0xFF55FF55;
    private static final int RED = 0xFFFF5555;

    private static volatile HudState hud = HudState.empty();
    private static boolean leftAltWasDown;

    @Override
    public void onInitializeClient() {
        PayloadTypeRegistry.playS2C().register(HudPayload.TYPE, HudPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SelectPayload.TYPE, SelectPayload.CODEC);

        ClientPlayNetworking.registerGlobalReceiver(HudPayload.TYPE, (payload, context) -> hud = payload.state());
        ClientTickEvents.END_CLIENT_TICK.register(this::handleLeftAlt);
        HudRenderCallback.EVENT.register((graphics, tickCounter) -> renderHud(graphics));
    }

    private void handleLeftAlt(Minecraft client) {
        boolean leftAltDown = client.screen == null
                && client.player != null
                && InputConstants.isKeyDown(client.getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_ALT);

        if (leftAltDown && !leftAltWasDown && !hud.abilities().isEmpty()) {
            int next = Math.floorMod(hud.selectedIndex() + 1, hud.abilities().size());
            hud = hud.withSelectedIndex(next);
            ClientPlayNetworking.send(new SelectPayload(next));
        }
        leftAltWasDown = leftAltDown;
    }

    private void renderHud(GuiGraphics graphics) {
        HudState state = hud;
        if (state.abilities().isEmpty()) return;

        Minecraft client = Minecraft.getInstance();
        int listX = 8;
        int listY = graphics.guiHeight() - 72 - state.abilities().size() * 12;
        graphics.drawString(client.font, "Phantom Abilities", listX, listY, PURPLE, true);

        for (int i = 0; i < state.abilities().size(); i++) {
            Ability ability = state.abilities().get(i);
            boolean selected = i == state.selectedIndex();
            String prefix = selected ? "› " : "  ";
            int color = selected ? WHITE : GRAY;
            graphics.drawString(client.font, prefix + displayName(ability.name()), listX, listY + 12 + i * 12, color, true);
        }

        Ability selected = state.selectedAbility();
        if (selected == null) return;
        String cooldown = selected.secondsRemaining() == 0 ? "READY" : selected.secondsRemaining() + "s";
        int color = selected.secondsRemaining() == 0 ? GREEN : RED;
        String text = "✦ " + displayName(selected.name()) + "  |  " + cooldown;
        int x = (graphics.guiWidth() - client.font.width(text)) / 2;
        graphics.drawString(client.font, text, x, graphics.guiHeight() - 68, color, true);
    }

    private static String displayName(String name) {
        return name.isEmpty() ? name : Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    public record Ability(String name, int secondsRemaining) { }

    public record HudState(int selectedIndex, List<Ability> abilities) {
        static HudState empty() { return new HudState(0, List.of()); }
        HudState withSelectedIndex(int index) { return new HudState(index, abilities); }
        Ability selectedAbility() {
            return abilities.isEmpty() ? null : abilities.get(Math.floorMod(selectedIndex, abilities.size()));
        }
    }

    public record HudPayload(HudState state) implements CustomPacketPayload {
        public static final Type<HudPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("phantomarmor", "hud"));
        public static final StreamCodec<RegistryFriendlyByteBuf, HudPayload> CODEC = StreamCodec.of(HudPayload::write, HudPayload::new);

        public HudPayload(RegistryFriendlyByteBuf buffer) {
            this(readState(buffer));
        }

        private static HudState readState(ByteBuf buffer) {
            int selected = buffer.readInt();
            int count = Math.max(0, Math.min(buffer.readInt(), 32));
            List<Ability> abilities = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                int length = buffer.readUnsignedShort();
                byte[] nameBytes = new byte[length];
                buffer.readBytes(nameBytes);
                abilities.add(new Ability(new String(nameBytes, StandardCharsets.UTF_8), Math.max(0, buffer.readInt())));
            }
            return new HudState(selected, List.copyOf(abilities));
        }

        private void write(RegistryFriendlyByteBuf buffer) {
            buffer.writeInt(state.selectedIndex());
            buffer.writeInt(state.abilities().size());
            for (Ability ability : state.abilities()) {
                byte[] name = ability.name().getBytes(StandardCharsets.UTF_8);
                buffer.writeShort(name.length);
                buffer.writeBytes(name);
                buffer.writeInt(ability.secondsRemaining());
            }
        }

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record SelectPayload(int selectedIndex) implements CustomPacketPayload {
        public static final Type<SelectPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("phantomarmor", "select"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SelectPayload> CODEC = StreamCodec.of(SelectPayload::write, SelectPayload::new);

        public SelectPayload(RegistryFriendlyByteBuf buffer) { this(buffer.readInt()); }
        private void write(RegistryFriendlyByteBuf buffer) { buffer.writeInt(selectedIndex); }
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
}
