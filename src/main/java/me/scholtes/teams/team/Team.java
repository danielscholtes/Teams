package me.scholtes.teams.team;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;

import me.scholtes.teams.Utils;

public class Team {
	
	private List<UUID> members;
	private UUID owner;
	private String teamName;
	private UUID teamUUID;
	private double teamBalance;
	
	public Team(UUID owner, String teamName) {
		this.owner = owner;
		this.teamName = teamName;
		this.teamUUID = UUID.randomUUID();
		this.teamBalance = 0;
		this.members = new ArrayList<UUID>();
	}
	
	public UUID getOwner() {
		return this.owner;
	}
	
	public void setOwner(UUID newOwner) {
		this.owner = newOwner;
	}
	
	public List<UUID> getMembers() {
		return members;
	}
	
	public void addMember(UUID member) {
		getMembers().add(member);
	}
	
	public void removeMember(UUID member) {
		getMembers().remove(member);
	}
	
	public void messageMembers(String message) {
		for (UUID uuid : this.members) {
			if (Bukkit.getPlayer(uuid) != null) {
				Utils.message(Bukkit.getPlayer(uuid), message);
			}
		}
		if (Bukkit.getPlayer(owner) != null) {
			Utils.message(Bukkit.getPlayer(owner), message);
		}
	}
	
	public String getName() {
		return teamName;
	}
	
	public void setName(String teamName) {
		this.teamName = teamName;
	}
	
	public UUID getUUID() {
		return teamUUID;
	}
	
	public void setUUID(UUID teamUUID) {
		this.teamUUID = teamUUID;
	}
	
	public double getBalance() {
		return teamBalance;
	}
	
	public void setBalance(double teamBalance) {
		this.teamBalance = teamBalance;
	}

}
