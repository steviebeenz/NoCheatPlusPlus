package xyz.msws.anticheat.checks.movement;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffectType;

import xyz.msws.anticheat.NOPE;
import xyz.msws.anticheat.checks.Check;
import xyz.msws.anticheat.checks.CheckType;
import xyz.msws.anticheat.checks.Global.Stat;
import xyz.msws.anticheat.data.CPlayer;

/**
 * Checks if a player moves vertically straight up
 * 
 * @author imodm
 *
 */
public class Flight3 implements Check, Listener {

	private NOPE plugin;

	@Override
	public CheckType getType() {
		return CheckType.MOVEMENT;
	}

	@Override
	public void register(NOPE plugin) {
		this.plugin = plugin;
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	@EventHandler
	public void onMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		CPlayer cp = plugin.getCPlayer(player);

		if (player.isFlying() || cp.isInWeirdBlock() || player.isInsideVehicle() || cp.isInClimbingBlock())
			return;

		if (cp.timeSince(Stat.DAMAGE_TAKEN) < 2000)
			return;

		if (cp.timeSince(Stat.FLYING) < 2000)
			return;

		if (cp.timeSince(Stat.BLOCK_PLACE) < 1000)
			return;

		if (cp.timeSince(Stat.IN_LIQUID) < 1000)
			return;

		if (cp.timeSince(Stat.TELEPORT) < 1000)
			return;

		if (cp.timeSince(Stat.FLIGHT_GROUNDED) < 500)
			return;

		if (player.hasPotionEffect(PotionEffectType.LEVITATION))
			return;

		if (cp.isBlockNearby(Material.SCAFFOLDING))
			return;
//		if (cp.timeSince(Stat.CLIMBING) < 1000) {
//			cp.removeTempData("lastGroundLocation");
//			return;
//		}

		Location safe = cp.getLastSafeLocation();

		if (event.getTo().getY() - 3 < safe.getY())
			return;

		if (event.getTo().getY() <= event.getFrom().getY())
			return;

		cp.flagHack(this, 10);
	}

	@Override
	public String getCategory() {
		return "Flight";
	}

	@Override
	public String getDebugName() {
		return "Flight#3";
	}

	@Override
	public boolean lagBack() {
		return true;
	}
}
