package me.scholtes.teams;

import java.text.DecimalFormat;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import me.scholtes.teams.commands.TeamCommand;
import me.scholtes.teams.listeners.DamageListener;
import me.scholtes.teams.listeners.JoinLeaveListener;
import me.scholtes.teams.team.TeamData;
import net.milkbowl.vault.economy.Economy;


public class Teams extends JavaPlugin {
	
	private TeamData teamData;
	private DecimalFormat df2 = new DecimalFormat("#.##");
	private Economy eco = null;
	
	public void onEnable() {
		
		saveDefaultConfig();
		
		teamData = new TeamData(this);
		
		getCommand("f").setExecutor(new TeamCommand(this, teamData));
		
		getServer().getPluginManager().registerEvents(new JoinLeaveListener(teamData), this);
		getServer().getPluginManager().registerEvents(new DamageListener(teamData), this);
		
		Bukkit.getScheduler().runTaskAsynchronously(this, new Runnable() {	
			@Override
			public void run() {
				teamData.loadTeams();
			}
		});
		
		Bukkit.getScheduler().runTaskTimerAsynchronously(this, new Runnable() {	
			@Override
			public void run() {
				teamData.saveTeams();
			}
		}, 20 * 60 * 2L, 20 * 60 * 3L);
		

		if (!setupEconomy() ) {
			System.out.println("Vault is not enabled, disabling plugin");
            getServer().getPluginManager().disablePlugin(this);
            return;
		}
		
	}
	
	public void onDisable() {
		teamData.saveTeams();
	}
	
	private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        eco = rsp.getProvider();
        return eco != null;
    }
	
	public Economy getEconomy() {
        return eco;
    }
	
	public DecimalFormat getDecimalFormat() {
		return df2;
	}
	
}
