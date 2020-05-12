package org.mswsplex.anticheat.msws.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Consumer;
import org.mswsplex.anticheat.utils.MSG;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class PluginInfo {
	private final int id;

	private String title, tag, version;

	private Stats stats;

	private Plugin plugin;

	public PluginInfo(Plugin plugin, int id) {
		this.id = id;
		this.plugin = plugin;
	}

	public void fetch(final Consumer<PluginInfo> result) {
		new BukkitRunnable() {
			@Override
			public void run() {
				try (InputStream inputStream = new URL(
						"https://api.spigotmc.org/simple/0.1/index.php?action=getResource&id=" + PluginInfo.this.id)
								.openStream();
						Scanner scanner = new Scanner(inputStream)) {
					StringBuilder builder = new StringBuilder();
					while (scanner.hasNext())
						builder.append(scanner.next());
					
					String text = builder.toString();
					JsonObject parse = new JsonParser().parse(text).getAsJsonObject();
					
					PluginInfo.this.title = parse.getAsJsonPrimitive("title").getAsString();
					PluginInfo.this.tag = parse.getAsJsonPrimitive("tag").getAsString();
					PluginInfo.this.version = parse.getAsJsonPrimitive("current_version").getAsString();
					PluginInfo.this.stats = new Stats(parse.getAsJsonObject("stats"));
					result.accept(PluginInfo.this);
				} catch (IOException exception) {
					MSG.log("Cannot look for updates: " + exception.getMessage());
				}
			}
		}.runTaskAsynchronously(plugin);

	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public Stats getStats() {
		return stats;
	}

	public void setStats(Stats stats) {
		this.stats = stats;
	}

	public int getId() {
		return id;
	}

	public class Stats {

		private int downloads, updates, reviews;
		private float rating;

		public Stats(JsonElement value) {
			JsonObject stats = value.getAsJsonObject();
			this.downloads = stats.getAsJsonPrimitive("downloads").getAsInt();
			this.updates = stats.getAsJsonPrimitive("updates").getAsInt();
			this.reviews = stats.getAsJsonPrimitive("reviews").getAsInt();
			this.rating = stats.getAsJsonPrimitive("rating").getAsFloat();
		}

		public int getDownloads() {
			return downloads;
		}

		public void setDownloads(int downloads) {
			this.downloads = downloads;
		}

		public int getUpdates() {
			return updates;
		}

		public void setUpdates(int updates) {
			this.updates = updates;
		}

		public int getReviews() {
			return reviews;
		}

		public void setReviews(int reviews) {
			this.reviews = reviews;
		}

		public float getRating() {
			return rating;
		}

		public void setRating(float rating) {
			this.rating = rating;
		}
	}
}
