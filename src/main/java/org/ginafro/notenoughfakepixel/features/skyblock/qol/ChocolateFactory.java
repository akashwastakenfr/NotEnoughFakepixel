package org.ginafro.notenoughfakepixel.features.skyblock.qol;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemSkull;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S29PacketSoundEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.ginafro.notenoughfakepixel.Configuration;
import org.ginafro.notenoughfakepixel.events.PacketReadEvent;
import org.ginafro.notenoughfakepixel.utils.*;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.ginafro.notenoughfakepixel.Configuration.*;

public class ChocolateFactory {

    private Pattern upgradeCostPattern = Pattern.compile("(\u00A7.)(?<cost>[0-9,]+) Chocolate");
    private Pattern pattern = Pattern.compile("Id:\"([^\"]+)\"");
    private ArrayList<String> eggIDS = new ArrayList<>(); //"e3da4593-afbb-38df-bf1e-b57e27a2e0e1";
    private String eggLime = "e3da4593-afbb-38df-bf1e-b57e27a2e0e1";
    private String eggBlue = "15785089-b2b0-38ac-b379-8af3d6253c62";
    private String eggCake = "9e39f2f4-8038-3aac-97fd-d7420cdf4601";
    private ArrayList <Waypoint> waypoints = new ArrayList<>();



    public ChocolateFactory() {
        eggIDS.add(eggLime);
        eggIDS.add(eggBlue);
        eggIDS.add(eggCake);
    }

    @SubscribeEvent
    public void onRenderLast(RenderWorldLastEvent event) {
        if (!ScoreboardUtils.currentGamemode.isSkyblock()) return;
        if (!Configuration.chocolateEggWaypoints) return;
        checkForEggs();
        drawWaypoints(event.partialTicks);
    }

    @SubscribeEvent()
    public void onGuiOpen(GuiScreenEvent.BackgroundDrawnEvent event) {
        if (!Configuration.showBestUpgrade || !(event.gui instanceof GuiChest)) return;

        TreeMap<Float, Slot> upgradeCosts = new TreeMap<>();
        GuiChest chest = (GuiChest) event.gui;
        Container container = chest.inventorySlots;

        String chestName = TablistParser.currentOpenChestName;
        if (chestName == null || !chestName.startsWith("Chocolate Factory")) return;

        int index = 0;
        ContainerChest containerChest = (ContainerChest) container;
        for (Slot slot : containerChest.inventorySlots) {
            if (slot.getSlotIndex() < 28 || slot.getSlotIndex() > 34) continue;
            index++;
            ItemStack item = slot.getStack();
            if (item != null && item.getItem() instanceof ItemSkull) {
                String upgradeCost = ItemUtils.getLoreLine(item, upgradeCostPattern);
                if (upgradeCost == null) continue;
                upgradeCost = StringUtils.cleanColor(upgradeCost).replaceAll(",", "").replaceAll(" Chocolate", "");

                float costRatio = Float.parseFloat(upgradeCost) / index;
                upgradeCosts.put(costRatio, slot);
            }
        }
        if (upgradeCosts.isEmpty()) return;
        float lowestValue = upgradeCosts.firstKey();
        Slot associatedSlot = upgradeCosts.get(lowestValue);

        RenderUtils.drawOnSlot(containerChest.inventorySlots.size(), associatedSlot.xDisplayPosition, associatedSlot.yDisplayPosition, new Color(0, 255, 0, 100).getRGB());
    }

    @SubscribeEvent
    public void onChat(@NotNull ClientChatReceivedEvent e){
        if (!Configuration.chocolateEggWaypoints) return;
        if (!ScoreboardUtils.currentGamemode.isSkyblock()) return;
        if (ChatUtils.middleBar.matcher(e.message.getFormattedText()).matches()) return;
        Matcher matcher = Pattern.compile("HOPPITY'S HUNT You found").matcher(e.message.getUnformattedText());
        Matcher matcher2 = Pattern.compile("HOPPITY'S HUNT A Chocolate .* Egg has appeared").matcher(e.message.getUnformattedText());
        Matcher matcher3 = Pattern.compile("You have already collected this Chocolate .* Egg! Try again when it respawns!").matcher(e.message.getUnformattedText());
        int[] playerCoords = new int[] {Minecraft.getMinecraft().thePlayer.getPosition().getX(), Minecraft.getMinecraft().thePlayer.getPosition().getY(), Minecraft.getMinecraft().thePlayer.getPosition().getZ()};
        if (matcher.find()) {
            Waypoint w = getClosestWaypoint(playerCoords);
            if (w == null) return;
            w.setHidden(true);
        }
        if (matcher2.find()) {
            ArrayList<Waypoint> waypointsToRemove = new ArrayList<>();
            for (Waypoint w : waypoints) {
                if (w.isHidden() && distance(playerCoords,w.getCoordinates()) > 64) waypointsToRemove.add(w);
            }
            waypoints.removeAll(waypointsToRemove);
        }
        if (matcher3.find()) {
            Waypoint w = getClosestWaypoint(playerCoords);
            if (w != null && distance(playerCoords,w.getCoordinates()) < 6) w.setHidden(true);
        }
    }

    @SubscribeEvent()
    public void onWorldUnload(WorldEvent.Unload event) {
        if (Configuration.chocolateEggWaypoints) waypoints.clear();
    }

    private Waypoint getClosestWaypoint(int[] coords) {
        if (waypoints.isEmpty()) return null;
        // Variables to keep track of the closest waypoint and shortest distance
        Waypoint closestWaypoint = null;
        double shortestDistance = Double.MAX_VALUE;

        // Iterate through all waypoints
        for (Waypoint waypoint : waypoints) {
            int[] waypointCoordinates = waypoint.getCoordinates();
            // Calculate the Euclidean distance
            double distance = distance(coords, waypointCoordinates);
            // Check if this waypoint is closer than the current closest
            if (distance < shortestDistance) {
                shortestDistance = distance;
                closestWaypoint = waypoint;
            }
        }
        return closestWaypoint;
    }

    private double distance(int[] coords1, int[] coords2) {
        return Math.sqrt(
                Math.pow(coords1[0] - coords2[0], 2) +
                        Math.pow(coords1[1] - coords2[1], 2) +
                        Math.pow(coords1[2] - coords2[2], 2)
        );
    }

    private void checkForEggs() {
        WorldClient world = Minecraft.getMinecraft().theWorld;
        for (int i = 0; i < world.loadedEntityList.size(); i++) {
            Entity entity = world.loadedEntityList.get(i);
            if (entity == null) continue;
            if (entity.getName() == null) continue;
            if (entity instanceof EntityArmorStand) {
                ItemStack it = ((EntityArmorStand) entity).getEquipmentInSlot(4);
                if (it != null && it.getUnlocalizedName().contains("item.skull.char")) {
                    if (it.getTagCompound().getTag("SkullOwner") != null) {
                        String nbtString = it.getTagCompound().getTag("SkullOwner").toString();
                        // Regex to extract the Id
                        Matcher matcher = pattern.matcher(nbtString);
                        if (matcher.find() && isEgg(matcher.group(1))) {
                            int[] entityCoords = new int[]{entity.getPosition().getX(), entity.getPosition().getY(), entity.getPosition().getZ()};
                            Waypoint waypoint = new Waypoint("EGG", entityCoords);
                            if (checkIfAdded(waypoint)) continue;
                            waypoints.add(waypoint);
                            SoundUtils.playSound(entityCoords,"random.pop", 4.0f, 2.5f);
                            // Create waypoint
                        }
                    }
                }
            }
        }
    }

    private void drawWaypoints(float partialTicks) {
        Entity viewer = Minecraft.getMinecraft().getRenderViewEntity();
        double viewerX = viewer.lastTickPosX + (viewer.posX - viewer.lastTickPosX) * partialTicks;
        double viewerY = viewer.lastTickPosY + (viewer.posY - viewer.lastTickPosY) * partialTicks;
        double viewerZ = viewer.lastTickPosZ + (viewer.posZ - viewer.lastTickPosZ) * partialTicks;
        ArrayList<Waypoint> toRemove = new ArrayList<>();
        for (Waypoint waypoint : waypoints) {
            if (waypoint == null || waypoint.isHidden()) continue;
            Color colorDrawWaypoint = chocolateEggWaypointsColor.toJavaColor();
            colorDrawWaypoint = new Color(colorDrawWaypoint.getRed(), colorDrawWaypoint.getGreen(), colorDrawWaypoint.getBlue(), 150);
            AxisAlignedBB bb = new AxisAlignedBB(
                    waypoint.getCoordinates()[0] - viewerX,
                    waypoint.getCoordinates()[1] - viewerY,
                    waypoint.getCoordinates()[2] - viewerZ,
                    waypoint.getCoordinates()[0] + 1 - viewerX,
                    waypoint.getCoordinates()[1] + 1 - viewerY + 150,
                    waypoint.getCoordinates()[2] + 1 - viewerZ
            ).expand(0.01f, 0.01f, 0.01f);
            GlStateManager.disableCull();
            RenderUtils.drawFilledBoundingBox(bb, 1f, colorDrawWaypoint);
            GlStateManager.enableCull();
            GlStateManager.enableTexture2D();
        }
    }

    private boolean checkIfAdded(Waypoint waypoint) {
        for (Waypoint w : waypoints) {
            if (w.getCoordinates()[0] == waypoint.getCoordinates()[0] && w.getCoordinates()[1] == waypoint.getCoordinates()[1] && w.getCoordinates()[2] == waypoint.getCoordinates()[2]) return true;
        }
        return false;
    }

    private boolean isEgg(String id) {
        for (String egg : eggIDS) {
            if (egg.equals(id)) {
                return true;
            }
        }
        return false;
    }
}
