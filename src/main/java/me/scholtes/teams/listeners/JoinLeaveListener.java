package me.scholtes.teams.listeners;


import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import me.scholtes.teams.team.Team;
import me.scholtes.teams.team.TeamData;

public class JoinLeaveListener implements Listener {
	
	private TeamData teamData;
	
	public JoinLeaveListener(TeamData teamData) {
		this.teamData = teamData;
	}
	
	@EventHandler
	public void onLeave(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		
		if (teamData.getInvitations().containsKey(player.getUniqueId())) {
			teamData.getInvitations().remove(player.getUniqueId());
		}
		
		if (teamData.getInvitationTasks().containsKey(player.getUniqueId())) {
			for (int task : teamData.getInvitationTasks().get(player.getUniqueId())) {
				Bukkit.getScheduler().cancelTask(task);
			}
			teamData.getInvitationTasks().remove(player.getUniqueId());
		}
		
		if (teamData.getPlayerTeam().containsKey(player.getUniqueId())) {
			teamData.getPlayerTeam().remove(player.getUniqueId());
		}
		
		return;	
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		
		Player player = event.getPlayer();
		
		Team team = teamData.getTeamFromMember(player.getUniqueId());
		
		if (team != null) {
			teamData.getPlayerTeam().put(player.getUniqueId(), team);
		}
		
	}

}
