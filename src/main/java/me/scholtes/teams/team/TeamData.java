package me.scholtes.teams.team;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import me.scholtes.teams.Teams;

public class TeamData {
	
	private Teams plugin;
	private File teamFolder;
	private Map<UUID, Team> playerTeam = new WeakHashMap<UUID, Team>();
	private List<Team> teams = new ArrayList<Team>();
	private Map<UUID, List<Team>> invitations = new WeakHashMap<UUID, List<Team>>();
	private Map<UUID, List<Integer>> invitationTasks = new WeakHashMap<UUID, List<Integer>>();
	
	public TeamData(Teams plugin) {
		this.plugin = plugin;
		this.teamFolder = new File(plugin.getDataFolder().getAbsolutePath(), "teams/");
	}
	
	public void loadTeams() {
		if (!teamFolder.exists()) {
			teamFolder.mkdir();
		}
		
		for (File file : teamFolder.listFiles()) {
			if (!file.isFile()) {
				continue;
			}
			
			if (!file.getName().endsWith(".yml")) {
                file.delete();
                continue;
            }
			
			UUID teamUUID = UUID.fromString(file.getName().split("\\.")[0]);
			FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

			UUID ownerID = UUID.fromString(cfg.getString("owner"));
			String teamName = cfg.getString("team_name");
			
			Team team = new Team(ownerID, teamName);
			team.setUUID(teamUUID);
			team.setBalance(cfg.getDouble("balance"));
			for (String memberUUID : cfg.getStringList("members")) {
				team.addMember(UUID.fromString(memberUUID));
				playerTeam.put(UUID.fromString(memberUUID), team);
			}
			
			teams.add(team);
			
			if (Bukkit.getPlayer(team.getOwner()) != null) {
				playerTeam.put(team.getOwner(), team);
			}
			
			for (UUID uuid : team.getMembers()) {
				if (Bukkit.getPlayer(uuid) != null) {
					playerTeam.put(uuid, team);
				}
			}
		}
	}
	
	public void saveTeams() {
		for (File file : teamFolder.listFiles()) {
			file.delete();
		}
		for (Team team : teams) {
			File file = new File(teamFolder.getPath() + "/" + team.getUUID() + ".yml");
			FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
			
			cfg.set("owner", team.getOwner().toString());
			cfg.set("balance", team.getBalance());
			List<String> members = new ArrayList<String>();
			for (UUID uuid : team.getMembers()) {
				members.add(uuid.toString());
			}
			cfg.set("members", members);
			cfg.set("team_name", team.getName());
			
			try {
				cfg.save(file);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public Team getTeamFromMember(UUID uuid) {
		if (teams.isEmpty()) {
			return null;
		}
		
		for (Team team : teams) {
			if (team.getOwner().equals(uuid) || team.getMembers().contains(uuid)) {
				return team;
			}
		}
		
		return null;
	}
	
	public Team getTeamFromName(String teamName) {
		if (teams.isEmpty()) {
			return null;
		}
		
		for (Team team : teams) {
			if (team.getName().equalsIgnoreCase(teamName)) {
				return team;
			}
		}
		return null;
	}
	
	public List<Team> getTeams() {
		return teams;
	}
	
	public List<Team> getPlayerInvitations(UUID uuid) {
		return invitations.get(uuid);
	}
	
	public Map<UUID, List<Team>> getInvitations() {
		return invitations;
	}
	
	public Map<UUID, List<Integer>> getInvitationTasks() {
		return invitationTasks;
	}
	
	public Map<UUID, Team> getPlayerTeam() {
		return playerTeam;
	}
	
	public void sendInvitation(Team team, UUID toSend) {
		if (invitations.containsKey(toSend)) {
			invitations.get(toSend).add(team);
		} else {
			List<Team> teams = new ArrayList<Team>();
			teams.add(team);
			invitations.put(toSend, teams);
		}
		
		int taskID = new BukkitRunnable() {
			@Override
			public void run() {
				if (invitations.containsKey(toSend) && invitations.get(toSend).contains(team)) {
					invitations.get(toSend).remove(team);
					if (invitations.get(toSend).isEmpty()) {
						invitations.remove(toSend);
					}
				}
				int id = this.getTaskId();
				if (invitationTasks.containsKey(toSend) && invitationTasks.get(toSend).contains(id)) {
					invitationTasks.get(toSend).remove(id);
					if (invitationTasks.get(toSend).isEmpty()) {
						invitationTasks.remove(toSend);
					}
				}
			}
		}.runTaskLaterAsynchronously(plugin, 20L * 60 * 2).getTaskId();

		if (invitationTasks.containsKey(toSend)) {
			invitationTasks.get(toSend).add(taskID);
		} else {
			List<Integer> tasks = new ArrayList<Integer>();
			tasks.add(taskID);
			invitationTasks.put(toSend, tasks);
		}
	}

}
