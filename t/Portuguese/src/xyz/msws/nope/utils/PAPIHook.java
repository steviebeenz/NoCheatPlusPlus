package xyz.msws.nope.utils;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.OfflinePlayer;

public class PAPIHook {
  public String setPlaceholders(OfflinePlayer player, String string) {
    return PlaceholderAPI.setPlaceholders(player, string);
  }
}
