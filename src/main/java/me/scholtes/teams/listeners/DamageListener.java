package me.scholtes.teams.listeners;

import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import me.scholtes.teams.team.TeamData;


public class DamageListener implements Listener {
	
	private TeamData teamData;
	
	public DamageListener(TeamData teamData) {
		this.teamData = teamData;
	}
	
	@EventHandler
	public void onDamage(EntityDamageByEntityEvent event) {
		if (!(event.getEntity() instanceof Player)) {
			return;
		}
		
		Player victim = (Player) event.getEntity();
		Player attacker = null;
		if ((event.getDamager() instanceof Player)) {
			attacker = (Player) event.getDamager();
		}
		
		if ((event.getDamager() instanceof Projectile)) {
			Projectile projectile = (Projectile) event.getDamager();
			
			if (projectile.getShooter() == null || !(projectile.getShooter() instanceof Player)) {
				return;
			}
			
			attacker = (Player) projectile.getShooter();
		}
		

		if (!teamData.getPlayerTeam().containsKey(victim.getUniqueId()) || !teamData.getPlayerTeam().containsKey(attacker.getUniqueId())) {
			return;
		}
		
		if (teamData.getPlayerTeam().get(victim.getUniqueId()) == teamData.getPlayerTeam().get(attacker.getUniqueId())) {
			event.setCancelled(true);
		}
	}

}
