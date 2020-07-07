package xyz.msws.nope;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import com.rabbitcompany.adminbans.AdminBans;

import io.sentry.Sentry;
import io.sentry.SentryClient;
import io.sentry.SentryClientFactory;


import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import xyz.msws.nope.commands.NOPECommand;
import xyz.msws.nope.listeners.LogImplementation;
import xyz.msws.nope.listeners.LoginAndQuit;
import xyz.msws.nope.listeners.MessageListener;
import xyz.msws.nope.listeners.UpdateCheckerListener;
import xyz.msws.nope.modules.AbstractModule;
import xyz.msws.nope.modules.actions.ActionManager;
import xyz.msws.nope.modules.animations.AnimationManager;
import xyz.msws.nope.modules.bans.AdvancedBanHook;
import xyz.msws.nope.modules.bans.BanHook;
import xyz.msws.nope.modules.bans.BanManagementHook;
import xyz.msws.nope.modules.bans.Banwave;
import xyz.msws.nope.modules.bans.LiteBansHook;
import xyz.msws.nope.modules.bans.MaxBansHook;
import xyz.msws.nope.modules.bans.NativeBanHook;
import xyz.msws.nope.modules.checks.Check;
import xyz.msws.nope.modules.checks.Checks;
import xyz.msws.nope.modules.checks.Global;
import xyz.msws.nope.modules.checks.TPSManager;
import xyz.msws.nope.modules.checks.VLDecrementer;
import xyz.msws.nope.modules.compatability.AbstractCompatability;
import xyz.msws.nope.modules.compatability.CrazyEnchantsHook;
import xyz.msws.nope.modules.compatability.McMMOHook;
import xyz.msws.nope.modules.compatability.TraincartsHook;
import xyz.msws.nope.modules.data.CPlayer;
import xyz.msws.nope.modules.data.ConfigOption;
import xyz.msws.nope.modules.data.Option;
import xyz.msws.nope.modules.data.PlayerManager;
import xyz.msws.nope.modules.data.Stats;
import xyz.msws.nope.modules.extensions.ExtensionModule;
import xyz.msws.nope.modules.npc.NPCModule;
import xyz.msws.nope.modules.reports.ReportTracker;
import xyz.msws.nope.modules.scoreboard.ScoreboardAssigner;
import xyz.msws.nope.modules.scoreboard.ScoreboardModule;
import xyz.msws.nope.modules.trust.TrustFactor;
import xyz.msws.nope.utils.MSG;
import xyz.msws.nope.utils.Metrics;
import xyz.msws.nope.utils.Metrics.CustomChart;
/**
 * NOPE is a 1.15.2 Minecraft Anti-Cheat NOPE relies purely on Dependency
 * Injection and Modularity, get modules with {@link NOPE#getModule(Class)}.
 * 
 * {@link https://github.com/MSWS/NOPE/wiki}
 * 
 * @author imodm
 *
 */
public class NOPE extends JavaPlugin {
	private static SentryClient sentry;

	public static void main(String... args) {
		/*
		 * It is recommended that you use the DSN detection system, which will check the
		 * environment variable "SENTRY_DSN", the Java System Property "sentry.dsn", or
		 * the "sentry.properties" file in your classpath. This makes it easier to
		 * provide and adjust your DSN without needing to change your code. See the
		 * configuration page for more information.
		 * 
		 * For example, using an environment variable
		 * 
		 * export SENTRY_DSN=
		 * "https://327807c1697c48d0b5e5c26d6c11491d@o406895.ingest.sentry.io/5275597"
		 */

		// You can also manually provide the DSN to the ``init`` method.
		Sentry.init("https://a06b4d46e8624d0692f5ee3e1b56e9dd@o406895.ingest.sentry.io/5313682");

		/*
		 * It is possible to go around the static ``Sentry`` API, which means you are
		 * responsible for making the SentryClient instance available to your code.
		 */
		sentry = SentryClientFactory.sentryClient(); // Sentry.capture(e);

		NOPE myClass = new NOPE();
		// myClass.logWithInstanceAPI();
	}

	private FileConfiguration config, data, lang;
	private File configYml = new File(getDataFolder(), "config.yml"), dataYml = new File(getDataFolder(), "data.yml"),
			langYml = new File(getDataFolder(), "lang.yml");

	private String serverName = "Unknown Server", nmsVersion = "v1_UNKNOWN";

	private PluginInfo pluginInfo;

	private String newVersion = null;

	private Collection<AbstractCompatability> compatabilities = new ArrayList<>();

	private Map<String, Option> options;

	private HashSet<AbstractModule> modules = new HashSet<>();

	private PlayerManager pManager;

	public void onEnable() {
		

		compatabilities = loadCompatabilities();
		Sentry.init("https://a06b4d46e8624d0692f5ee3e1b56e9dd@o406895.ingest.sentry.io/5313682");

		/*
		 * It is possible to go around the static ``Sentry`` API, which means you are
		 * responsible for making the SentryClient instance available to your code.
		 */
		sentry = SentryClientFactory.sentryClient(); // Sentry.capture(e);

		NOPE myClass = new NOPE();

		try {
			setupFiles();
			MSG.plugin=this;
			registerOptions();
			MSG.log(checkConfigVersion());
		loadModules();
		uploadCustomCharts();
		runUpdateCheck();
		} catch (Exception e) {
			e.printStackTrace();
			Sentry.capture(e);
		}
	}

	private String checkConfigVersion() {
		if (config.getString("ConfigVersion", "").equals(getDescription().getVersion()))
			return "You are using an up-to-date version of the config.";
		switch (config.getString("ConfigVersion", "")) {
			case "1.7.1":
			case "1.7":
			case "1.6.3":
			case "1.6.2":
			case "1.6.1":
				return "Nothing new has been changed in the config.";
			case "1.6.0.1":
			case "1.6":
				return "Your lang file, you should reset your language file for message to work.";
			default:
				return "Your config version is unknown, it is strongly recommended you reset your config.";
		}
	}

	public void reload() {
		onDisable();
		onEnable();
	}

	private void setupFiles() {
		if (!configYml.exists())
			saveResource("config.yml", true);
		if (!langYml.exists())
			saveResource("lang.yml", true);
		config = YamlConfiguration.loadConfiguration(configYml);
		data = YamlConfiguration.loadConfiguration(dataYml);
		lang = YamlConfiguration.loadConfiguration(langYml);
	}

	/**
	 * Initialize all modules to resolve any dependency issues
	 */
	private void loadModules() {
		try {
		modules = new HashSet<>();
		modules.add(new ActionManager(this, configYml));
		modules.add(new PlayerManager(this));
		modules.add(new TPSManager(this));
		modules.add(new Banwave(this));
		modules.add(new Checks(this));
		modules.add(new Stats(this));
		modules.add(new Global(this));
		modules.add(hookBans());
		modules.add(new LogImplementation(this));
		modules.add(new LoginAndQuit(this));
		modules.add(new MessageListener(this));
		modules.add(new NOPECommand(this));
		if (options.get("gscoreboard").asBoolean()) {
			modules.add(new ScoreboardModule(this));
			modules.add(new ScoreboardAssigner(this));
		}
		modules.add(new TrustFactor(this));
		modules.add(new ReportTracker(this));
		modules.add(new VLDecrementer(this));
		modules.add(new ExtensionModule(this));

		modules.add(new AnimationManager(this));
		if (Bukkit.getPluginManager().isPluginEnabled("ProtocolLib"))
			modules.add(new NPCModule(this));
		enableModules();
	} catch (Exception e) {
		e.printStackTrace();
		Sentry.capture(e);
	}
	}

	private void registerOptions() {
		options = new HashMap<>();
		options.put("global", new ConfigOption(config, "Global", true, false));
		options.put("gscoreboard", new ConfigOption(config, "Scoreboard", true, false));
		options.put("log", new ConfigOption(config, "Log", "file", "NONE", "hastebin"));
		options.put("updatechecker", new ConfigOption(config, "UpdateChecker.Enabled", true, false));
		options.put("bungeename", new ConfigOption(config, "BungeeNameOverride", (Object[]) null));
		options.put("dev", new ConfigOption(config, "DevMode", false, true));
		options.put("debug", new ConfigOption(config, "DebugMode", Arrays.asList(false, true)));
	}

	private void enableModules() {
		for (AbstractModule mod : modules)
			try {
				mod.enable();
			} catch (Exception e) {
				e.printStackTrace();
				Sentry.capture(e);
			}
	}

	/**
	 * Returns the appropriate module from the specified class
	 * 
	 * @param <T>
	 * @param cast
	 * @return
	 */
	@Nullable
	public <T extends AbstractModule> T getModule(Class<T> cast) {
		for (AbstractModule module : modules) {
			if (cast.isAssignableFrom(module.getClass()))
				return cast.cast(module);
		}
		return null;
	}

	private Collection<AbstractCompatability> loadCompatabilities() {
		Set<AbstractCompatability> cs = new HashSet<>();
		if (Bukkit.getPluginManager().isPluginEnabled("mcMMO"))
			cs.add(new McMMOHook(this));
		if (Bukkit.getPluginManager().isPluginEnabled("CrazyEnchantments"))
			cs.add(new CrazyEnchantsHook(this));
		if (Bukkit.getPluginManager().isPluginEnabled("Train_Carts")
				|| Bukkit.getPluginManager().isPluginEnabled("TrainCarts"))
			cs.add(new TraincartsHook(this));

		for (AbstractCompatability comp : compatabilities)
			MSG.log("&7Registered compatability for " + MSG.FORMAT_INFO + comp.getName() + "&7.");

		return cs;
	}

	private BanHook hookBans() {
		try {
		if (Bukkit.getPluginManager().isPluginEnabled("AdvancedBan")) {
			MSG.log("Successfully hooked into AdvancedBans.");
			return new AdvancedBanHook(this);
		} else if (Bukkit.getPluginManager().isPluginEnabled("MaxBans")) {
			MSG.log("Successfully hooked into MaxBans.");
			return new MaxBansHook(this);
		} else if (Bukkit.getPluginManager().isPluginEnabled("BanManagement")) {
			MSG.log("Successfully hooked into BanManagement.");
			return new BanManagementHook(this);
		} else if (Bukkit.getPluginManager().isPluginEnabled("LiteBans")) {
			MSG.log("Successfully hooked into LiteBans.");
			return new LiteBansHook(this);
		} else {
			MSG.log("Unable to find a ban management plugin, using native support.");
			return new NativeBanHook(this);
		}
		} catch (Exception e) {
			e.printStackTrace();
			Sentry.capture(e);
			return new NativeBanHook(this);
		}
	}

	private void uploadCustomCharts() {
		try {
		Metrics metrics = new Metrics(this, 7422);
		CustomChart chart = new Metrics.SingleLineChart("bans", new Callable<Integer>() {
			@Override
			public Integer call() throws Exception {
				return getModule(Stats.class).getAllBans();
			}
		});
		metrics.addCustomChart(chart);
		chart = new Metrics.AdvancedBarChart("checkweights", new Callable<Map<String, int[]>>() {
			@Override
			public Map<String, int[]> call() throws Exception {
				Map<String, int[]> result = new HashMap<>();
				Checks checks = getModule(Checks.class);
				Stats stats = getModule(Stats.class);
				for (Check check : checks.getAllChecks()) {
					int[] arr = new int[2];
					arr[0] = stats.getTotalVl(check);
					arr[1] = stats.getTotalTriggers(check);
					result.put(check.getDebugName(), arr);
				}
				return result;
			}
		});
		metrics.addCustomChart(chart);
		chart = new Metrics.SimplePie("defaultactions", new Callable<String>() {
			@Override
			public String call() throws Exception {
				return NOPE.this.getConfig().getString("Actions.Default", "Unset");
			}
		});
		metrics.addCustomChart(chart);
		chart = new Metrics.SimplePie("configversion", new Callable<String>() {
			@Override
			public String call() throws Exception {
				return NOPE.this.getConfig().getString("ConfigVersion", "Unknown");
			}
		});
		metrics.addCustomChart(chart);
		chart = new Metrics.SimplePie("bungeeoverride", new Callable<String>() {
			@Override
			public String call() throws Exception {
				if (NOPE.this.getConfig().getString("BungeeNameOverride", "").equals(""))
					return "Unset";
				return NOPE.this.getConfig().getString("BungeeNameOverride", "Unset");
			}
		});
		metrics.addCustomChart(chart);
		chart = new Metrics.SimplePie("updatechecker", new Callable<String>() {
			@Override
			public String call() throws Exception {
				return NOPE.this.getConfig().getString("UpdateChecker.Enabled", "Unset");
			}
		});
		metrics.addCustomChart(chart);
		chart = new Metrics.SimplePie("customactions", new Callable<String>() {
			@Override
			public String call() throws Exception {
				if (!NOPE.this.getConfig().isConfigurationSection("Commands"))
					return "None";
				return NOPE.this.getConfig().getConfigurationSection("Commands").getKeys(false).size() + "";
			}
		});
		metrics.addCustomChart(chart);
	} catch (Exception e) {
		e.printStackTrace();
		Sentry.capture(e);
	}
	}

	/**
	 * Re-updates the {@link PluginInfo}
	 */
	private void runUpdateCheck() {
		try {
		if (config.getBoolean("UpdateChecker.Enabled", true)) {
			if (config.getBoolean("UpdateChecker.InGame", true))
				new UpdateCheckerListener(this);
			pluginInfo = new PluginInfo(this, 64671);
			pluginInfo.fetch(pi -> {
				newVersion = pi.getVersion();
				String info = "";
				switch (pi.outdated()) {
					case DEVELOPER_VERSION:
						info = "You are using a developer version.";
						break;
					case OUTDATED_VERSION:
						info = "You are using an outdated version. Version &e" + newVersion
								+ " &7is now available (&bhttps://www.spigotmc.org/resources/64671&7)";
						break;
					case SAME_VERSION:
						info = "You are using an up-to-date version.";
						break;
				}
				MSG.log(info);
			});
		}
	} catch (Exception e) {
		e.printStackTrace();
		Sentry.capture(e);
	}
	}

	/**
	 * Returns the plugin version that is available online.
	 * 
	 * @return
	 */
	@Nullable
	public String getNewVersion() {
		return newVersion;
	}

	/**
	 * Returns the {@link PluginInfo} that was obtained when NOPE was last enabled.
	 * 
	 * @return
	 */
	public PluginInfo getPluginInfo() {
		return pluginInfo;
	}

	/**
	 * Gets the server's bungee name, if BungeeNameOverride is set it returns that
	 * instead
	 * 
	 * @return
	 */
	public String getServerName() {
		if (!config.getString("BungeeNameOverride", "").isEmpty())
			return config.getString("BungeeNameOverride");
		return serverName;
	}

	/**
	 * Sets the server name from the bungee network, does not override the config
	 * value
	 * 
	 * @param name
	 */
	public void setServerName(String name) {
		this.serverName = name;
	}

	/**
	 * Disables all modules, unregisters listeners, and cancels all tasks
	 */
	public void onDisable() {
		for (AbstractModule mod : modules)
			mod.disable();

		HandlerList.unregisterAll(this);
		Bukkit.getScheduler().cancelTasks(this);
	}

	public void saveData() {
		try {
			data.save(dataYml);
		} catch (Exception e) {
			MSG.log("&cError saving data file");
			MSG.log("&a----------Start of Stack Trace----------");
			e.printStackTrace();
			Sentry.capture(e);
			MSG.log("&a----------End of Stack Trace----------");
		}
	}

	public void saveConfig() {
		try {
			config.save(configYml);
		} catch (Exception e) {
			MSG.log("&cError saving data file");
			MSG.log("&a----------Start of Stack Trace----------");
			e.printStackTrace();
			Sentry.capture(e);
			MSG.log("&a----------End of Stack Trace----------");
		}
	}

	public FileConfiguration getConfig() {
		return config;
	}

	public File getConfigFile() {
		return configYml;
	}

	public void setConfig(FileConfiguration config) {
		this.config = config;
	}

	public FileConfiguration getLang() {
		return lang;
	}

	public void setLang(FileConfiguration lang) {
		this.lang = lang;
	}

	/**
	 * Returns a CPlayer, will never return null.
	 * 
	 * @param off
	 * @return
	 */
	public CPlayer getCPlayer(OfflinePlayer off) {
		if (pManager == null)
			pManager = getModule(PlayerManager.class);
		return pManager.getPlayer(off.getUniqueId());
	}

	/**
	 * @return the compatabilities
	 */
	public Collection<AbstractCompatability> getCompatabilities() {
		return compatabilities;
	}

	/**
	 * Registers and enables the specified hook
	 * 
	 * @param hook
	 */
	public void registerCompatability(AbstractCompatability hook) {
		try {
		hook.enable();
		compatabilities.add(hook);
		} catch (Exception e) {
			e.printStackTrace();
			Sentry.capture(e);
		}
	}

	/**
	 * Returns a specific option specified for NOPE For now these are all options in
	 * the config.
	 * 
	 * @param key
	 * @return
	 */
	@Nullable
	public Option getOption(String key) {
		return options.get(key);
	}

	/**
	 * Returns the global settings of NOPE.
	 * 
	 * @return
	 */
	public Map<String, Option> getOptionMappings() {
		return options;
	}

	public String nms() {
		return nmsVersion;
	}
}

