package org.mineserver.dynmapcolorsync;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.MarkerSet;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class DynmapColorSyncPlugin extends JavaPlugin {

    // Dynmap-Towny default update period is 15 seconds (300 ticks).
    // We start at 320 ticks (16 sec) so we run 1 second AFTER Dynmap-Towny,
    // then repeat every 300 ticks to stay in sync.
    private static final long INITIAL_DELAY = 360L;
    private static final long PERIOD = 40L;  // 2 seconds — quickly overrides Dynmap-Towny's 15s reset

    // Cached named colors (Towny accepts these as color names)
    private static final Map<String, Integer> NAMED_COLORS = new HashMap<>();
    static {
        NAMED_COLORS.put("red",    0xFF0000);
        NAMED_COLORS.put("blue",   0x0000FF);
        NAMED_COLORS.put("green",  0x008000);
        NAMED_COLORS.put("lime",   0x00FF00);
        NAMED_COLORS.put("yellow", 0xFFFF00);
        NAMED_COLORS.put("orange", 0xFF7F00);
        NAMED_COLORS.put("purple", 0x800080);
        NAMED_COLORS.put("white",  0xFFFFFF);
        NAMED_COLORS.put("black",  0x000000);
        NAMED_COLORS.put("aqua",   0x00FFFF);
        NAMED_COLORS.put("cyan",   0x00FFFF);
        NAMED_COLORS.put("navy",   0x000080);
        NAMED_COLORS.put("pink",   0xFFC0CB);
        NAMED_COLORS.put("brown",  0x8B4513);
        NAMED_COLORS.put("gray",   0x808080);
        NAMED_COLORS.put("grey",   0x808080);
        NAMED_COLORS.put("silver", 0xC0C0C0);
        NAMED_COLORS.put("teal",   0x008080);
        NAMED_COLORS.put("maroon", 0x800000);
    }

    @Override
    public void onEnable() {
        getServer().getScheduler().scheduleSyncRepeatingTask(this, this::syncColors, INITIAL_DELAY, PERIOD);
        getLogger().info("DynmapColorSync enabled — will sync town colors 1s after each Dynmap-Towny update");
    }

    private boolean isDynamicTownColorsEnabled() {
        File cfg = new File(getDataFolder().getParentFile(), "Dynmap-Towny/config.yml");
        if (!cfg.exists()) return false;
        return YamlConfiguration.loadConfiguration(cfg).getBoolean("dynamic-town-colors", false);
    }

    private void syncColors() {
        if (!isDynamicTownColorsEnabled()) return;

        Plugin dynmapPlugin = getServer().getPluginManager().getPlugin("dynmap");
        if (dynmapPlugin == null || !dynmapPlugin.isEnabled()) return;
        DynmapAPI dynmap = (DynmapAPI) dynmapPlugin;

        var markerAPI = dynmap.getMarkerAPI();
        if (markerAPI == null) return;

        MarkerSet set = markerAPI.getMarkerSet("towny.markerset");
        if (set == null) return;

        TownyAPI townyAPI = TownyAPI.getInstance();
        if (townyAPI == null) return;

        Collection<AreaMarker> markers = set.getAreaMarkers();
        for (AreaMarker marker : markers) {
            String markerId = marker.getMarkerID();
            // Marker IDs from Dynmap-Towny: "TownName__0", "TownName__0_COMMERCIAL", etc.
            int sep = markerId.indexOf("__");
            if (sep < 0) continue;
            String townName = markerId.substring(0, sep);

            Town town;
            try {
                town = townyAPI.getTown(townName);
            } catch (Exception e) {
                continue;
            }
            if (town == null) continue;

            String hexCode = town.getMapColorHexCode();
            if (hexCode == null || hexCode.isEmpty()) continue;

            int color = parseColor(hexCode);
            if (color < 0) continue;

            marker.setFillStyle(marker.getFillOpacity(), color);
            marker.setLineStyle(marker.getLineWeight(), marker.getLineOpacity(), color);
        }
    }

    private int parseColor(String hex) {
        if (hex.startsWith("#")) hex = hex.substring(1);
        // Try hex first
        try {
            return Integer.parseInt(hex, 16);
        } catch (NumberFormatException ignored) {}
        // Try named color
        Integer named = NAMED_COLORS.get(hex.toLowerCase());
        return named != null ? named : -1;
    }
}
