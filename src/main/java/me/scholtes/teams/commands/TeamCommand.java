package me.scholtes.teams.commands;

import java.util.Iterator;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.scholtes.teams.Teams;
import me.scholtes.teams.Utils;
import me.scholtes.teams.team.Team;
import me.scholtes.teams.team.TeamData;

public class TeamCommand implements CommandExecutor {

	private Teams plugin;
	private TeamData teamData;

	public TeamCommand(Teams plugin, TeamData teamData) {
		this.plugin = plugin;
		this.teamData = teamData;
	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

		
		if (args.length < 1) {
			for (String s : plugin.getConfig().getStringList("messages.help.1")) {
				Utils.message(sender, s);
			}
			return true;
		}

		if (!(sender instanceof Player)) {
			Utils.message(sender, "&cYou need to be a player!");
			return true;
		}

		Player player = (Player) sender;

		switch (args[0]) {

		case "reload": {
			if (!player.hasPermission("team.admin")) {
				Utils.message(player, plugin.getConfig().getString("messages.reload.no_permission"));
				return true;
			}
			plugin.reloadConfig();
			Utils.message(player, plugin.getConfig().getString("messages.reload.reloaded"));
			return true;
		}

		case "create": {
			if (args.length < 2) {
				Utils.message(player, plugin.getConfig().getString("messages.create.incorrect"));
				return true;
			}

			if (teamData.getPlayerTeam().containsKey(player.getUniqueId())) {
				Utils.message(player, plugin.getConfig().getString("messages.create.already_in"));
				return true;
			}
			
			if (!args[1].matches("^[a-zA-Z0-9]*$")) {
				Utils.message(player, plugin.getConfig().getString("messages.create.alphanumeric"));
				return true;
			}
			
			if (args[1].length() < plugin.getConfig().getInt("min_characters")) {
				Utils.message(player, plugin.getConfig().getString("messages.create.too_short"));
				return true;
			}
			
			if (args[1].length() > plugin.getConfig().getInt("max_characters")) {
				Utils.message(player, plugin.getConfig().getString("messages.create.too_long"));
				return true;
			}
			
			if (teamData.getTeamFromName(args[1]) != null) {
				Utils.message(player, plugin.getConfig().getString("messages.create.already_exists"));
				return true;
			}
			
			Team team = new Team(player.getUniqueId(), args[1]);
			teamData.getTeams().add(team);
			teamData.getPlayerTeam().put(player.getUniqueId(), team);
			Utils.message(player, plugin.getConfig().getString("messages.create.success").replaceAll("\\{name\\}", args[1]));
			
			if (teamData.getInvitations().containsKey(player.getUniqueId())) {
				teamData.getInvitations().remove(player.getUniqueId());
			}
			
			if (teamData.getInvitationTasks().containsKey(player.getUniqueId())) {
				for (int task : teamData.getInvitationTasks().get(player.getUniqueId())) {
					Bukkit.getScheduler().cancelTask(task);
				}
				teamData.getInvitationTasks().remove(player.getUniqueId());
			}
			
			return true;
		}

		case "invite": {
			if (args.length < 2) {
				Utils.message(player, plugin.getConfig().getString("messages.invite.incorrect"));
				return true;
			}
			
			if (!teamData.getPlayerTeam().containsKey(player.getUniqueId())) {
				Utils.message(player, plugin.getConfig().getString("messages.invite.not_in"));
				return true;
			}
			
			Team team = teamData.getPlayerTeam().get(player.getUniqueId());

			if (team.getMembers().size() + 1 >= plugin.getConfig().getInt("max_faction_size")) {
				Utils.message(player, plugin.getConfig().getString("messages.invite.max_team"));
				return true;
			}

			if (!team.getOwner().equals(player.getUniqueId())) {
				Utils.message(player, plugin.getConfig().getString("messages.invite.not_leader"));
				return true;
			}

			Player toInvite = Bukkit.getPlayerExact(args[1]);

			if (toInvite == null) {
				Utils.message(player, plugin.getConfig().getString("messages.invite.player_offline").replaceAll("\\{player\\}", args[1]));
				return true;
			}

			if (teamData.getPlayerTeam().containsKey(toInvite.getUniqueId())) {
				Utils.message(player, plugin.getConfig().getString("messages.invite.already_in").replaceAll("\\{player\\}", toInvite.getName()));
				return true;
			}

			if (teamData.getInvitations().containsKey(toInvite.getUniqueId()) && teamData.getPlayerInvitations(toInvite.getUniqueId()).contains(team)) {
				Utils.message(player, plugin.getConfig().getString("messages.invite.already_invited").replaceAll("\\{player\\}", toInvite.getName()));
				return true;
			}

			Utils.message(toInvite, plugin.getConfig().getString("messages.invite.invited").replaceAll("\\{faction_name\\}", team.getName()));
			team.messageMembers(plugin.getConfig().getString("messages.invite.success").replaceAll("\\{player\\}", toInvite.getName()));
			teamData.sendInvitation(team, toInvite.getUniqueId());
			return true;
		}

		case "join": {
			if (args.length < 2) {
				Utils.message(player, plugin.getConfig().getString("messages.join.incorrect"));
				return true;
			}
			
			if (!teamData.getInvitations().containsKey(player.getUniqueId())) {
				Utils.message(player, plugin.getConfig().getString("messages.join.not_invited").replaceAll("\\{faction_name\\}", args[1]));
				return true;
			}
			
			Iterator<Team> iterator = teamData.getInvitations().get(player.getUniqueId()).iterator();
			
			while (iterator.hasNext()) {
				Team team = iterator.next();
				if (team.getName().equalsIgnoreCase(args[1])) {
					if (!teamData.getTeams().contains(team)) {
						iterator.remove();
						if (teamData.getInvitationTasks().containsKey(player.getUniqueId())) {
							for (int task : teamData.getInvitationTasks().get(player.getUniqueId())) {
								Bukkit.getScheduler().cancelTask(task);
							}
							teamData.getInvitationTasks().remove(player.getUniqueId());
						}

						if (teamData.getInvitations().get(player.getUniqueId()).isEmpty()) {
							teamData.getInvitations().remove(player.getUniqueId());
						}
						
						continue;
					}
					if (team.getMembers().size() + 1 >= plugin.getConfig().getInt("max_faction_size")) {
						Utils.message(player, plugin.getConfig().getString("messages.join.max_team").replaceAll("\\{player\\}", args[1]));
						return true;
					}

					team.messageMembers(plugin.getConfig().getString("messages.join.joined").replaceAll("\\{player\\}", player.getName()));
					Utils.message(player, plugin.getConfig().getString("messages.join.success").replaceAll("\\{faction_name\\}", team.getName()));
					team.getMembers().add(player.getUniqueId());
					teamData.getPlayerTeam().put(player.getUniqueId(), team);
					
					if (teamData.getInvitations().containsKey(player.getUniqueId())) {
						teamData.getInvitations().remove(player.getUniqueId());
					}
					
					if (teamData.getInvitationTasks().containsKey(player.getUniqueId())) {
						for (int task : teamData.getInvitationTasks().get(player.getUniqueId())) {
							Bukkit.getScheduler().cancelTask(task);
						}
						teamData.getInvitationTasks().remove(player.getUniqueId());
					}
					
					return true;
				}
			}

			Utils.message(player, plugin.getConfig().getString("messages.join.not_invited").replaceAll("\\{faction_name\\}", args[1]));
			return true;
		}

		case "leave": {
			if (!teamData.getPlayerTeam().containsKey(player.getUniqueId())) {
				Utils.message(player, plugin.getConfig().getString("messages.leave.not_in"));
				return true;
			}

			Team team = teamData.getPlayerTeam().get(player.getUniqueId());
			
			if (team.getOwner().equals(player.getUniqueId())) {
				Utils.message(player, plugin.getConfig().getString("messages.leave.leader"));
				return true;
			}

			team.getMembers().remove(player.getUniqueId());
			teamData.getPlayerTeam().remove(player.getUniqueId());
			
			Utils.message(player, plugin.getConfig().getString("messages.leave.success").replaceAll("\\{faction_name\\}", team.getName()));
			team.messageMembers(plugin.getConfig().getString("messages.leave.left").replaceAll("\\{player\\}", player.getName()));
			return true;
		}

		case "kick": {
			if (args.length < 2) {
				Utils.message(player, plugin.getConfig().getString("messages.kick.incorrect"));
				return true;
			}
			
			if (!teamData.getPlayerTeam().containsKey(player.getUniqueId())) {
				Utils.message(player, plugin.getConfig().getString("messages.kick.not_in"));
				return true;
			}
			
			Team team = teamData.getPlayerTeam().get(player.getUniqueId());

			if (!team.getOwner().equals(player.getUniqueId())) {
				Utils.message(player, plugin.getConfig().getString("messages.kick.not_leader"));
				return true;
			}
			
			OfflinePlayer toKick = Bukkit.getOfflinePlayer(args[1]);

			if (team.getOwner().equals(toKick.getUniqueId())) {
				Utils.message(player, plugin.getConfig().getString("messages.kick.cant_kick_yourself"));
				return true;
			}
			
			if (!team.getMembers().contains(toKick.getUniqueId())) {
				Utils.message(player, plugin.getConfig().getString("messages.kick.not_member").replaceAll("\\{player\\}", args[1]));
				return true;
			}

			team.getMembers().remove(toKick.getUniqueId());
			
			if (Bukkit.getPlayerExact(args[1]) != null) {
				Utils.message(Bukkit.getPlayerExact(args[1]), plugin.getConfig().getString("messages.kick.kicked").replaceAll("\\{faction_name\\}", team.getName()));
				teamData.getPlayerTeam().remove(toKick.getUniqueId());
				team.messageMembers(plugin.getConfig().getString("messages.kick.success").replaceAll("\\{player\\}", Bukkit.getPlayerExact(args[1]).getName()));
				return true;
			}
			
			team.messageMembers(plugin.getConfig().getString("messages.kick.success").replaceAll("\\{player\\}", args[1]));
			
			return true;
		}

		case "disband": {
			if (!teamData.getPlayerTeam().containsKey(player.getUniqueId())) {
				Utils.message(player, plugin.getConfig().getString("messages.disband.not_in"));
				return true;
			}
			
			Team team = teamData.getPlayerTeam().get(player.getUniqueId());

			if (!team.getOwner().equals(player.getUniqueId())) {
				Utils.message(player, plugin.getConfig().getString("messages.disband.not_leader"));
				return true;
			}

			team.messageMembers(plugin.getConfig().getString("messages.disband.success").replaceAll("\\{faction_name\\}", team.getName()));
			teamData.getPlayerTeam().remove(team.getOwner());
			for (UUID uuid : team.getMembers()) {
				if (Bukkit.getPlayer(uuid) != null) {
					teamData.getPlayerTeam().remove(uuid);
				}
			}
			
			teamData.getTeams().remove(team);
			
			return true;
		}

		case "rename": {
			if (args.length < 2) {
				Utils.message(player, plugin.getConfig().getString("messages.rename.incorrect"));
				return true;
			}
			
			if (!teamData.getPlayerTeam().containsKey(player.getUniqueId())) {
				Utils.message(player, plugin.getConfig().getString("messages.rename.not_in"));
				return true;
			}
			
			Team team = teamData.getPlayerTeam().get(player.getUniqueId());

			if (!team.getOwner().equals(player.getUniqueId())) {
				Utils.message(player, plugin.getConfig().getString("messages.rename.not_leader"));
				return true;
			}
			
			if (!args[1].matches("^[a-zA-Z0-9]*$")) {
				Utils.message(player, plugin.getConfig().getString("messages.rename.alphanumeric"));
				return true;
			}
			
			if (args[1].length() < plugin.getConfig().getInt("min_characters")) {
				Utils.message(player, plugin.getConfig().getString("messages.rename.too_short"));
				return true;
			}
			
			if (args[1].length() > plugin.getConfig().getInt("max_characters")) {
				Utils.message(player, plugin.getConfig().getString("messages.rename.too_long"));
				return true;
			}
			
			if (teamData.getTeamFromName(args[1]) != null) {
				Utils.message(player, plugin.getConfig().getString("messages.rename.already_exists"));
				return true;
			}

			team.messageMembers(plugin.getConfig().getString("messages.rename.success").replaceAll("\\{old_faction_name\\}", team.getName()).replaceAll("\\{new_faction_name\\}", args[1]));
			team.setName(args[1]);
			
			return true;
		}

		case "balance": {
			
			if (!teamData.getPlayerTeam().containsKey(player.getUniqueId())) {
				Utils.message(player, plugin.getConfig().getString("messages.balance.not_in"));
				return true;
			}
			
			Team team = teamData.getPlayerTeam().get(player.getUniqueId());

			Utils.message(player, plugin.getConfig().getString("messages.balance.success").replaceAll("\\{balance\\}", String.format("%,d", (int) team.getBalance())));
			
			return true;
		}

		case "promote": {
			if (args.length < 2) {
				Utils.message(player, plugin.getConfig().getString("messages.promote.incorrect"));
				return true;
			}
			
			if (!teamData.getPlayerTeam().containsKey(player.getUniqueId())) {
				Utils.message(player, plugin.getConfig().getString("messages.promote.not_in"));
				return true;
			}
			
			Team team = teamData.getPlayerTeam().get(player.getUniqueId());

			if (!team.getOwner().equals(player.getUniqueId())) {
				Utils.message(player, plugin.getConfig().getString("messages.promote.not_leader"));
				return true;
			}

			OfflinePlayer toKick = Bukkit.getOfflinePlayer(args[1]);
			
			if (team.getOwner().equals(toKick.getUniqueId())) {
				Utils.message(player, plugin.getConfig().getString("messages.promote.cant_promote_yourself"));
				return true;
			}

			if (!team.getMembers().contains(toKick.getUniqueId())) {
				Utils.message(player, plugin.getConfig().getString("messages.promote.not_member").replaceAll("\\{player\\}", args[1]));
				return true;
			}

			team.messageMembers(plugin.getConfig().getString("messages.promote.success").replaceAll("\\{player\\}", args[1]));
			team.addMember(player.getUniqueId());
			team.removeMember(toKick.getUniqueId());
			team.setOwner(toKick.getUniqueId());
			
			return true;
		}

		case "deposit": {
			if (args.length < 2) {
				Utils.message(player, plugin.getConfig().getString("messages.deposit.incorrect"));
				return true;
			}
			
			if (!teamData.getPlayerTeam().containsKey(player.getUniqueId())) {
				Utils.message(player, plugin.getConfig().getString("messages.deposit.not_in"));
				return true;
			}
			
			if (!isDouble(args[1])) {
				Utils.message(player, plugin.getConfig().getString("messages.deposit.not_a_number"));
				return true;
			}
			
			double money = Double.parseDouble(args[1]);
			if (plugin.getEconomy().getBalance(player) < money) {
				Utils.message(player, plugin.getConfig().getString("messages.deposit.not_enough"));
				return true;
			}
			
			Team team = teamData.getPlayerTeam().get(player.getUniqueId());

			team.messageMembers(plugin.getConfig().getString("messages.deposit.success").replaceAll("\\{money\\}",  String.format("%,d", (int) money)).replaceAll("\\{player\\}", player.getName()));
			plugin.getEconomy().withdrawPlayer(player, money);
			team.setBalance(team.getBalance() + money);
			
			return true;
		}

		case "withdraw": {
			if (args.length < 2) {
				Utils.message(player, plugin.getConfig().getString("messages.withdraw.incorrect"));
				return true;
			}
			
			if (!teamData.getPlayerTeam().containsKey(player.getUniqueId())) {
				Utils.message(player, plugin.getConfig().getString("messages.withdraw.not_in"));
				return true;
			}

			Team team = teamData.getPlayerTeam().get(player.getUniqueId());

			if (!team.getOwner().equals(player.getUniqueId())) {
				Utils.message(player, plugin.getConfig().getString("messages.withdraw.not_leader"));
				return true;
			}
			
			if (!isDouble(args[1])) {
				Utils.message(player, plugin.getConfig().getString("messages.withdraw.not_a_number"));
				return true;
			}
			
			double money = Double.parseDouble(args[1]);
			if (team.getBalance() < money) {
				Utils.message(player, plugin.getConfig().getString("messages.withdraw.not_enough"));
				return true;
			}

			Utils.message(player, plugin.getConfig().getString("messages.withdraw.success").replaceAll("\\{money\\}",  String.format("%,d", (int) money)));
			team.setBalance(team.getBalance() - money);
			plugin.getEconomy().depositPlayer(player, money);
			
			return true;
		}

		case "help": {
			if (args.length < 2 || !isInteger(args[1]) || !plugin.getConfig().isSet("messages.help." + args[1])) {
				for (String s : plugin.getConfig().getStringList("messages.help.1")) {
					Utils.message(sender, s);
				}
				return true;
			}
			
			for (String s : plugin.getConfig().getStringList("messages.help." + args[1])) {
				Utils.message(sender, s);
			}
			
			return true;
		}

		case "who": {
			if (args.length < 2) {
				if (teamData.getPlayerTeam().containsKey(player.getUniqueId())) {
					sendWhoMessage(player, teamData.getPlayerTeam().get(player.getUniqueId()));
					return true;
				}

				Utils.message(player, plugin.getConfig().getString("messages.who.incorrect"));
				return true;
			}
			
			Team team = teamData.getTeamFromMember(Bukkit.getOfflinePlayer(args[1]).getUniqueId());
			
			if (team == null) {
				Utils.message(player, plugin.getConfig().getString("messages.who.not_in").replaceAll("\\{player\\}", args[1]));
				return true;
			}
			
			sendWhoMessage(player, team);
			
			return true;
		}

		default: {
			for (String s : plugin.getConfig().getStringList("messages.help.1")) {
				Utils.message(sender, s);
			}
			return true;
		}

		}
	}
	
	private void sendWhoMessage(Player player, Team team) {
		
		String leader = "";
		leader = "&7" + Bukkit.getOfflinePlayer(team.getOwner()).getName() + "&7";
		if (Bukkit.getPlayer(team.getOwner()) != null) {
			leader = "&a" + Bukkit.getPlayer(team.getOwner()).getName() + "&7";
		}
		
		String members = leader + "&7, ";
		for (int i = 0; i < team.getMembers().size(); i++) {
			if (Bukkit.getPlayer(team.getMembers().get(i)) != null) {
				members += "&a" + Bukkit.getPlayer(team.getMembers().get(i)).getName() + "&7, ";
			} else {
				members += "&7" + Bukkit.getOfflinePlayer(team.getMembers().get(i)).getName() + "&7, ";
			}
			
		}
		members = members.substring(0, members.length() - 4);
		
		for (String s : plugin.getConfig().getStringList("messages.who.message")) {
			s = s.replaceAll("\\{balance\\}",  String.format("%,d", (int) team.getBalance())).replaceAll("\\{faction_name\\}",  team.getName());
			s = s.replaceAll("\\{leader_name\\}",  leader).replaceAll("\\{members\\}", members);
			Utils.message(player, s);
		}
		
	}
	
	public boolean isDouble(String number) {
		try {
			Double.parseDouble(number);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}
	
	public boolean isInteger(String number) {
		try {
			Integer.parseInt(number);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

}
