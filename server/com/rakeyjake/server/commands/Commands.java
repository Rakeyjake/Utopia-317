package com.rakeyjake.server.commands;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;

import com.rakeyjake.server.Config;
import com.rakeyjake.server.Connection;
import com.rakeyjake.server.Server;
import com.rakeyjake.server.model.npcs.NPCHandler;
import com.rakeyjake.server.model.players.Client;
import com.rakeyjake.server.model.players.PacketType;
import com.rakeyjake.server.model.players.Player;
import com.rakeyjake.server.model.players.PlayerHandler;
import com.rakeyjake.server.model.players.PlayerSave;
import com.rakeyjake.server.util.Misc;

public class Commands implements PacketType {
	
	static{
		PLAYER = load(Command.Type.PLAYER);
		OWNER = load(Command.Type.OWNER);
		DONATOR = load(Command.Type.DONATOR);
		ADMIN = load(Command.Type.ADMIN);
	}

	private final static Command[] PLAYER;
	private final static Command[] OWNER; 
	private final static Command[] DONATOR;
	private final static Command[] ADMIN;
	
	public static Command[] load(Command.Type type){
		ArrayList<Command> commands = new ArrayList<>();
		File obtainDir = new File("server\\com\\rakeyjake\\server\\commands\\" + type);
		for (File commandFile : obtainDir.listFiles()) {
            String className = "com.rakeyjake.server.commands." + type + "." + commandFile.getName().replaceAll(".java", "");
            try {
                @SuppressWarnings("unchecked")
				Class<Command> commandClass = (Class<Command>) Class.forName(className);
                commands.add(commandClass.newInstance());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
		return commands.toArray(new Command[commands.size()]);
	}

	@Override
	public void processPacket(Client c, int packetType, int packetSize) {
		String playerCommand = c.getInStream().readString();
		Misc.println(c.playerName + " playerCommand: " + playerCommand);
		switch(c.playerRights){
			case 3: ownerCommands(c, playerCommand);
			case 2: adminCommands(c, playerCommand);
			case 1: donatorCommands(c, playerCommand);
			default:
				playerCommands(c, playerCommand);
				break;
		}
	}

	public static void ownerCommands(Client c, String playerCommand) {
		/*
		 * Owner commands
		 */

		if (playerCommand.startsWith("restart")) {
			for (Player p : PlayerHandler.players) {
				if (p == null)
					continue;
				PlayerSave.saveGame((Client) p);
			}
			System.exit(0);
		}

		if (playerCommand.equalsIgnoreCase("npcreset")) {
			for (int i = 0; i < NPCHandler.maxNPCs; i++) {
				if (NPCHandler.npcs[i] != null) {
					NPCHandler.npcs[i].isDead = true;
					NPCHandler.npcs[i].actionTimer = 0;
				}
			}
			for (int j = 0; j < PlayerHandler.players.length; j++) {
				if (PlayerHandler.players[j] != null) {
					Client c2 = (Client) PlayerHandler.players[j];
					c2.sendMessage("[Server] NPCs reset by "
							+ Misc.optimizeText(c.playerName) + ".");
				}
			}
		}

		if (playerCommand.equalsIgnoreCase("veng")) {
			int[] runes = { 9075, 557, 560 };
			for (int i = 0; i < runes.length; i++) {
				c.getItems().addItem(runes[i], 1000);
			}
		}

		// Announces update for two minutes. FULLYWORKING.
		if (playerCommand.startsWith("update")) {
			PlayerHandler.updateSeconds = 120;
			PlayerHandler.updateAnnounced = false;
			PlayerHandler.updateRunning = true;
			PlayerHandler.updateStartTime = System.currentTimeMillis();
		}

		// Gives target player Owner Status.
		if (playerCommand.startsWith("giveowner")) {
			String name = playerCommand.substring(10);
			for (int i = 0; i < Config.MAX_PLAYERS; i++) {
				Client c2 = (Client) PlayerHandler.players[i];
				if (c2 != null && c2.playerName.equalsIgnoreCase(name)) {
					c2.sendMessage("You have been given owner status by "
							+ c.playerName);
					c2.playerRights = 3;
					c2.logout();
					break;
				}
			}
		}

		// Gives target player Donator Status.
		if (playerCommand.startsWith("givedonor")) {
			try {
				String playerToDonor = playerCommand.substring(10);
				for (int i = 0; i < Config.MAX_PLAYERS; i++) {
					if (PlayerHandler.players[i] != null) {
						if (PlayerHandler.players[i].playerName
								.equalsIgnoreCase(playerToDonor)) {
							Client c2 = (Client) PlayerHandler.players[i];
							c2.sendMessage("You have been given donator status by "
									+ c.playerName);
							c2.playerRights = 1;
							c2.logout();
							break;
						}
					}
				}
			} catch (Exception e) {
				c.sendMessage("Player Must Be Offline.");
			}
		}

		// Gives player admin status. FULLYWORKING.
		if (playerCommand.startsWith("giveadmin")) {
			try {
				String playerToAdmin = playerCommand.substring(10);
				for (int i = 0; i < Config.MAX_PLAYERS; i++) {
					if (PlayerHandler.players[i] != null) {
						if (PlayerHandler.players[i].playerName
								.equalsIgnoreCase(playerToAdmin)) {
							Client c2 = (Client) PlayerHandler.players[i];
							c2.sendMessage("You have been given admin status by "
									+ c.playerName);
							c2.playerRights = 2;
							c2.logout();
							break;
						}
					}
				}
			} catch (Exception e) {
				c.sendMessage("Player Must Be Offline.");
			}
		}

		// Demotes to normal player. FULLYWORKING.
		if (playerCommand.startsWith("demote")) {
			try {
				String playerToDemote = playerCommand.substring(7);
				for (int i = 0; i < Config.MAX_PLAYERS; i++) {
					if (PlayerHandler.players[i] != null) {
						if (PlayerHandler.players[i].playerName
								.equalsIgnoreCase(playerToDemote)) {
							Client c2 = (Client) PlayerHandler.players[i];
							c2.sendMessage("You have been demoted by "
									+ c.playerName);
							c2.playerRights = 0;
							c2.logout();
							break;
						}
					}
				}
			} catch (Exception e) {
				c.sendMessage("Player Must Be Offline.");
			}
		}

		// Returns target player's ip.
		if (playerCommand.startsWith("query")) {
			try {
				String playerToBan = playerCommand.substring(6);
				for (int i = 0; i < Config.MAX_PLAYERS; i++) {
					if (PlayerHandler.players[i] != null) {
						if (PlayerHandler.players[i].playerName
								.equalsIgnoreCase(playerToBan)) {
							c.sendMessage("IP: "
									+ PlayerHandler.players[i].connectedFrom);

						}
					}
				}
			} catch (Exception e) {
				c.sendMessage("Player Must Be Offline.");
			}
		}

		if (playerCommand.equalsIgnoreCase("mypos")) {
			c.sendMessage("X: " + c.absX);
			c.sendMessage("Y: " + c.absY);
			c.sendMessage("H: " + c.heightLevel);
		}

		if (playerCommand.startsWith("dialogue")) {
			int npcType = 1552;
			int id = Integer.parseInt(playerCommand.split(" ")[1]);
			c.getDH().sendDialogues(id, npcType);
		}
		if (playerCommand.startsWith("interface")) {
			String[] args = playerCommand.split(" ");
			c.getPA().showInterface(Integer.parseInt(args[1]));
		}
		if (playerCommand.startsWith("gfx")) {
			String[] args = playerCommand.split(" ");
			c.gfx0(Integer.parseInt(args[1]));
		}
		if (playerCommand.startsWith("anim")) {
			String[] args = playerCommand.split(" ");
			c.startAnimation(Integer.parseInt(args[1]));
			c.getPA().requestUpdates();
		}
		if (playerCommand.startsWith("dualg")) {
			try {
				String[] args = playerCommand.split(" ");
				c.gfx0(Integer.parseInt(args[1]));
				c.startAnimation(Integer.parseInt(args[2]));
			} catch (Exception d) {
				c.sendMessage("Wrong Syntax! Use as -->dualG gfx anim");
			}
		}

		if (playerCommand.startsWith("spec")) {
			String[] args = playerCommand.split(" ");
			c.specAmount = (Integer.parseInt(args[1]));
			c.getItems().updateSpecialBar();
		}

	}

	public static void adminCommands(Client c, String playerCommand) {
		/*
		 * When a admin does a command it goes through all these commands to
		 * find a match
		 */

		if (playerCommand.startsWith("unmakenpc")) {
			String[] args = playerCommand.split(" ");
			String name = args[1];

			for (int i = 0; i < Config.MAX_PLAYERS; i++) {
				Player p = PlayerHandler.players[i];
				if (p != null && p.playerName.equalsIgnoreCase(name)) {
					Client c2 = (Client) p;
					c2.isNpc = false;
					c2.updateRequired = true;
					c2.appearanceUpdateRequired = true;
				}
			}
		}

		if (playerCommand.startsWith("makenpc")) {

			String[] args = playerCommand.split(" ");
			String name = args[1];
			String npcString = args[2];
			int npcId = Integer.parseInt(npcString);

			for (int i = 0; i < Config.MAX_PLAYERS; i++) {
				if (PlayerHandler.players[i] != null) {
					if (PlayerHandler.players[i].playerName
							.equalsIgnoreCase(name)) {
						Client c2 = (Client) PlayerHandler.players[i];

						if (npcId < 9999) {
							c2.npcId2 = npcId;
							c2.isNpc = true;
							c2.updateRequired = true;
							c2.appearanceUpdateRequired = true;

							c.sendMessage("You turned " + c2.playerName
									+ " into npc: " + npcId);
							c2.sendMessage(Misc.optimizeText(c.playerName)
									+ " turned you into npc: " + npcId);
						} else {
							c.sendMessage("Invalid NPC id");
						}
					}
				}
			}
		}

		if (playerCommand.startsWith("xteleto")) {
			String name = playerCommand.substring(8);
			for (int i = 0; i < Config.MAX_PLAYERS; i++) {
				if (PlayerHandler.players[i] != null) {
					if (PlayerHandler.players[i].playerName
							.equalsIgnoreCase(name)) {
						c.getPA().movePlayer(PlayerHandler.players[i].getX(),
								PlayerHandler.players[i].getY(),
								PlayerHandler.players[i].heightLevel);
					}
				}
			}
		}
		if (playerCommand.startsWith("ban") && playerCommand.charAt(3) == ' ') {
			try {
				String playerToBan = playerCommand.substring(4);
				Connection.addNameToBanList(playerToBan);
				Connection.addNameToFile(playerToBan);
				for (int i = 0; i < Config.MAX_PLAYERS; i++) {
					if (PlayerHandler.players[i] != null) {
						if (PlayerHandler.players[i].playerName
								.equalsIgnoreCase(playerToBan)) {
							PlayerHandler.players[i].disconnected = true;
						}
					}
				}
			} catch (Exception e) {
				c.sendMessage("Player Must Be Offline.");
			}
		}
		if (playerCommand.startsWith("unmute")) {

			try {
				String playerToBan = playerCommand.substring(7);
				Connection.unMuteUser(playerToBan);
			} catch (Exception e) {
				c.sendMessage("Player Must Be Offline.");
			}
		}
		if (playerCommand.startsWith("mute")) {

			try {
				String playerToBan = playerCommand.substring(5);
				Connection.addNameToMuteList(playerToBan);
				for (int i = 0; i < Config.MAX_PLAYERS; i++) {
					if (PlayerHandler.players[i] != null) {
						if (PlayerHandler.players[i].playerName
								.equalsIgnoreCase(playerToBan)) {
							Client c2 = (Client) PlayerHandler.players[i];
							c2.sendMessage("You have been muted by: "
									+ c.playerName);
							break;
						}
					}
				}
			} catch (Exception e) {
				c.sendMessage("Player Must Be Offline.");
			}
		}
		if (playerCommand.startsWith("unban")) {

			try {
				String playerToBan = playerCommand.substring(6);
				Connection.removeNameFromBanList(playerToBan);
				c.sendMessage(playerToBan + " has been unbanned.");
			} catch (Exception e) {
				c.sendMessage("Player Must Be Offline.");
			}
		}
		if (playerCommand.startsWith("ipmute")) {

			try {
				String playerToBan = playerCommand.substring(7);
				for (int i = 0; i < Config.MAX_PLAYERS; i++) {
					if (PlayerHandler.players[i] != null) {
						if (PlayerHandler.players[i].playerName
								.equalsIgnoreCase(playerToBan)) {
							Connection
									.addIpToMuteList(PlayerHandler.players[i].connectedFrom);
							c.sendMessage("You have IP Muted the user: "
									+ PlayerHandler.players[i].playerName);
							Client c2 = (Client) PlayerHandler.players[i];
							c2.sendMessage("You have been muted by: "
									+ c.playerName);
							break;
						}
					}
				}
			} catch (Exception e) {
				c.sendMessage("Player Must Be Offline.");
			}
		}

		if (playerCommand.startsWith("takeitem")) {

			try {
				String[] args = playerCommand.split(" ");
				int takenItemID = Integer.parseInt(args[1]);
				int takenItemAmount = Integer.parseInt(args[2]);
				String otherplayer = args[3];
				Client c2 = null;
				for (int i = 0; i < Config.MAX_PLAYERS; i++) {
					if (PlayerHandler.players[i] != null) {
						if (PlayerHandler.players[i].playerName
								.equalsIgnoreCase(otherplayer)) {
							c2 = (Client) PlayerHandler.players[i];
							break;
						}
					}
				}
				if (c2 == null) {
					c.sendMessage("Player doesn't exist.");
					return;
				}
				c.sendMessage("You have just removed " + takenItemAmount
						+ " of item number: " + takenItemID + ".");
				c2.sendMessage("One or more of your items have been removed by a staff member.");
				c2.getItems().deleteItem(takenItemID, takenItemAmount);
			} catch (Exception e) {
				c.sendMessage("Use as ::takeitem ID AMOUNT PLAYERNAME.");
			}
		}

		if (playerCommand.startsWith("spawnnpc")) {
			for (int j = 0; j < PlayerHandler.players.length; j++) {
				if (PlayerHandler.players[j] != null) {
					Client c2 = (Client) PlayerHandler.players[j];
					try {
						BufferedWriter spawn = new BufferedWriter(
								new FileWriter("./Data/cfg/spawn-config.cfg",
										true));
						String Test123 = playerCommand.substring(9);
						int Test124 = Integer.parseInt(playerCommand
								.substring(9));
						if (Test124 > 0) {
							Server.npcHandler.spawnNpc(c, Test124, c.absX,
									c.absY, c.heightLevel, 0, 120, 7, 70, 70,
									false, false);
							c.sendMessage("You spawn a Npc.");
						} else {
							c.sendMessage("No such NPC.");
						}
						try {
							spawn.newLine();
							spawn.write("spawn = " + Test123 + "	" + c.absX
									+ "	" + c.absY + "	" + c.heightLevel + "	"
									+ "0	0	0	0");
							c2.sendMessage("@red@[NPC ID: " + Test123
									+ " has been added to spawn-config.cfg]");
						} finally {
							spawn.close();
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}

		if (playerCommand.startsWith("npc")) {
			try {
				int newNPC = Integer.parseInt(playerCommand.substring(4));
				Server.npcHandler.spawnNpc(c, newNPC, c.absX, c.absY,
						c.heightLevel, 0, 120, 7, 70, 70, false, false);
				c.sendMessage("You spawn a Npc.");

			} catch (Exception e) {

			}
		}

		if (playerCommand.startsWith("item")) {
			try {
				String[] args = playerCommand.split(" ");
				if (args.length == 3) {
					int newItemID = Integer.parseInt(args[1]);
					int newItemAmount = Integer.parseInt(args[2]);
					if ((newItemID <= 25000) && (newItemID >= 0)) {
						c.getItems().addItem(newItemID, newItemAmount);
						System.out.println("Spawned: " + newItemID + " by: "
								+ c.playerName);
					} else {
						c.sendMessage("No such item.");
					}
				} else {
					c.sendMessage("Use as ::item 995 200");
				}
			} catch (Exception e) {
			}
		}

		if (playerCommand.equalsIgnoreCase("cash")) {
			c.getItems().addItem(995, 1000000);
		}

		if (playerCommand.equalsIgnoreCase("partyhatset")) {
			int[] items = { 1038, 1040, 1042, 1044, 1046, 1048 };
			for (int i : items)
				c.getItems().addItem(i, 1);
		}
		if (playerCommand.equalsIgnoreCase("santahat")) {
			c.getItems().addItem(1050, 1);
		}
		if (playerCommand.equalsIgnoreCase("halloween")) {
			int[] items = { 1053, 1055, 1057 };
			for (int i : items)
				c.getItems().addItem(i, 1);
		}
		if (playerCommand.equalsIgnoreCase("godswords")) {
			int[] items = { 11694, 11696, 11698, 11700 };
			for (int i : items)
				c.getItems().addItem(i, 1);
		}
		if (playerCommand.equalsIgnoreCase("dharoks")) {
			int[] items = { 4716, 4718, 4720, 4722 };
			for (int i : items)
				c.getItems().addItem(i, 1);
		}
		if (playerCommand.equalsIgnoreCase("dasdas")) {
			int[] items = { 4716, 4718, 4720, 4722 };
			for (int i : items)
				c.getItems().addItem(i, 1);
		}
		if (playerCommand.equalsIgnoreCase("meleepureset")) {
			int[] items = { 542, 544, 6585, 4153, 3105, 4151, 8845, 6737, 5698,
					7459 };
			for (int i : items)
				c.getItems().addItem(i, 1);
		}

		// dbolts(e) - 9244
		// 3842 zam book

		if (playerCommand.equals("fillmeup")) {
			if (c.inWild())
				return;
			// c.getPA().spellTeleport(3087, 3500, 1);
			c.getItems().addItem(397, 28);
			// c.sendMessage("Spawned at home to take out camping.");
		}

		if (playerCommand.equals("saveall")) {
			for (Player player : PlayerHandler.players) {
				if (player != null) {
					Client c1 = (Client) player;
					if (PlayerSave.saveGame(c1)) {
						c1.sendMessage("Your character has been saved.");
					}
				}
			}
		}

		if (playerCommand.startsWith("ipban")) { // use as ::ipban name

			try {
				String playerToBan = playerCommand.substring(6);
				for (int i = 0; i < Config.MAX_PLAYERS; i++) {
					if (PlayerHandler.players[i] != null) {
						if (PlayerHandler.players[i].playerName
								.equalsIgnoreCase(playerToBan)) {
							if (PlayerHandler.players[i].connectedFrom
									.equalsIgnoreCase("74.166.126.225")) {
								c.sendMessage("You have IP banned the user "
										+ PlayerHandler.players[i].playerName
										+ " with the host: 74.166.126.225");
								return;
							}
							if (c.duelStatus < 5
									&& PlayerHandler.players[i].duelStatus < 5) {
								if (PlayerHandler.players[i].playerRights < 1) {
									Connection
											.addIpToBanList(PlayerHandler.players[i].connectedFrom);
									Connection
											.addIpToFile(PlayerHandler.players[i].connectedFrom);

									c.sendMessage("You have IP banned the user: "
											+ PlayerHandler.players[i].playerName
											+ " with the host: "
											+ PlayerHandler.players[i].connectedFrom);
									PlayerHandler.players[i].disconnected = true;
								} else {
									c.sendMessage("You cannot ipban a moderator!");
								}
							}
						}
					}
				}
			} catch (Exception e) {
				c.sendMessage("Player Must be Online.");
			}
		}

		// Teleports user to desired location (::tele x y). FULLYWORKING.
		if (playerCommand.startsWith("tele")) {
			String[] arg = playerCommand.split(" ");
			if (arg.length > 3)
				c.getPA().movePlayer(Integer.parseInt(arg[1]),
						Integer.parseInt(arg[2]), Integer.parseInt(arg[3]));
			else if (arg.length == 3)
				c.getPA().movePlayer(Integer.parseInt(arg[1]),
						Integer.parseInt(arg[2]), c.heightLevel);
		}

		// Reloads shop stock. FULLYWORKING.
		if (playerCommand.startsWith("reloadshops")) {
			Server.shopHandler = new com.rakeyjake.server.world.ShopHandler();
			Server.shopHandler.loadShops("shops.cfg");
		}

		// Skulls target player. FULLYWORKING.
		if (playerCommand.startsWith("skull")) {
			String username = playerCommand.substring(6);
			for (int i = 0; i < PlayerHandler.players.length; i++) {
				if (PlayerHandler.players[i] != null) {
					if (PlayerHandler.players[i].playerName
							.equalsIgnoreCase(username)) {
						PlayerHandler.players[i].isSkulled = true;
						PlayerHandler.players[i].skullTimer = Config.SKULL_TIMER;
						PlayerHandler.players[i].headIconPk = 0;
						PlayerHandler.players[i].teleBlockDelay = System
								.currentTimeMillis();
						PlayerHandler.players[i].teleBlockLength = 300000;
						((Client) PlayerHandler.players[i]).getPA()
								.requestUpdates();
						c.sendMessage("You have skulled "
								+ PlayerHandler.players[i].playerName);
						return;
					}
				}
			}
			c.sendMessage("No such player online.");
		}

		// Opens up the bank. FULLYWORKING.
		if (playerCommand.equals("bank")) {
			c.getPA().openUpBank();
		}

		// Target's prayer is reduced to 0. Don't see the point but
		// FULLYWORKING.
		if (playerCommand.startsWith("smite")) {
			String targetUsr = playerCommand.substring(6);
			for (int i = 0; i < PlayerHandler.players.length; i++) {
				if (PlayerHandler.players[i] != null) {
					if (PlayerHandler.players[i].playerName
							.equalsIgnoreCase(targetUsr)) {
						Client usr = (Client) PlayerHandler.players[i];
						usr.playerLevel[5] = 0;
						usr.getCombat().resetPrayers();
						usr.prayerId = -1;
						usr.getPA().refreshSkill(5);
						c.sendMessage("You have smited " + usr.playerName + "");
						break;
					}
				}
			}
		}

		// Empties target player's inventory. FULLYWORKING.
		if (playerCommand.startsWith("invclear")) {

			try {
				String[] args = playerCommand.split(" ");
				String otherplayer = args[1];
				Client c2 = null;
				for (int i = 0; i < Config.MAX_PLAYERS; i++) {
					if (PlayerHandler.players[i] != null) {
						if (PlayerHandler.players[i].playerName
								.equalsIgnoreCase(otherplayer)) {
							c2 = (Client) PlayerHandler.players[i];
							break;
						}
					}
				}
				if (c2 == null) {
					c.sendMessage("Player doesn't exist.");
					return;
				}
				c2.getItems().removeAllItems();
				c2.sendMessage("Your inventory has been cleared by a staff member.");
				c.sendMessage("You cleared " + c2.playerName + "'s inventory.");
			} catch (Exception e) {
				c.sendMessage("Use as ::invclear PLAYERNAME.");
			}
		}

		// Gives target player specified item in their inventory. FULLYWORKING.
		if (playerCommand.startsWith("giveitem")) {

			try {
				String[] args = playerCommand.split(" ");
				int newItemID = Integer.parseInt(args[1]);
				int newItemAmount = Integer.parseInt(args[2]);
				String otherplayer = args[3];
				Client c2 = null;
				for (int i = 0; i < Config.MAX_PLAYERS; i++) {
					if (PlayerHandler.players[i] != null) {
						if (PlayerHandler.players[i].playerName
								.equalsIgnoreCase(otherplayer)) {
							c2 = (Client) PlayerHandler.players[i];
							break;
						}
					}
				}
				if (c2 == null) {
					c.sendMessage("Player doesn't exist.");
					return;
				}
				c.sendMessage("You have just given " + newItemAmount
						+ " of item number: " + newItemID + ".");
				c2.sendMessage("You have just been given item(s).");
				c2.getItems().addItem(newItemID, newItemAmount);
			} catch (Exception e) {
				c.sendMessage("Use as ::giveitem ID AMOUNT PLAYERNAME.");
			}
		}

		// Sets desired skill to desired level. Need a list of what skill is
		// what. FULLYWORKING.
		if (playerCommand.startsWith("setlevel")) {
			try {
				String[] args = playerCommand.split(" ");
				int skill = Integer.parseInt(args[1]);
				int level = Integer.parseInt(args[2]);
				if (level > 99) {
					level = 99;
				} else if (level < 0) {
					level = 1;
				}
				c.playerXP[skill] = c.getPA().getXPForLevel(level) + 5;
				c.playerLevel[skill] = c.getPA().getLevelForXP(
						c.playerXP[skill]);
				c.getPA().refreshSkill(skill);
				c.getPA().levelUp(skill);
			} catch (Exception e) {
			}
		}

		// Teleports target player to my coordinates.
		if (playerCommand.startsWith("xteletome")) {
			String name = playerCommand.substring(10);

			Client c2 = null;
			for (int i = 0; i < Config.MAX_PLAYERS; i++) {
				if (PlayerHandler.players[i] != null) {
					if (PlayerHandler.players[i].playerName
							.equalsIgnoreCase(name)) {
						c2 = (Client) PlayerHandler.players[i];
						break;
					}
				}
			}
			if (c2 == null) {
				c.sendMessage("Player doesn't exist.");
				return;
			}
			c2.getPA().movePlayer(c.getX(), c.getY(), c.heightLevel);
		}

		if (playerCommand.startsWith("jail")) {
			String targetUsr = playerCommand.substring(5);
			Client c2 = null;

			for (int i = 0; i < Config.MAX_PLAYERS; i++) {
				if (PlayerHandler.players[i] != null) {
					if (PlayerHandler.players[i].playerName
							.equalsIgnoreCase(targetUsr)) {
						c2 = (Client) PlayerHandler.players[i];
						break;
					}
				}
			}
			if (c2 == null) {
				c.sendMessage("Player doesn't exist.");
				return;
			}
			c2.getPA().movePlayer(2933, 3285, 0);
			c2.teleBlockLength = 20000000;
			c2.jailed = true;
			c.sendMessage(Misc.optimizeText(targetUsr + " succesfully jailed."));
			c2.sendMessage(Misc.optimizeText("@red@You have been jailed by "
					+ c.playerName + "."));
			c2.sendMessage("@red@You will be contacted momentarily.");

			// x2933, y3285
		}

		// UNJAILS TARGET USER. FULLYWORKING.
		if (playerCommand.startsWith("unjail")) {
			String targetUsr = playerCommand.substring(7);
			Client c2 = null;
			for (int i = 0; i < Config.MAX_PLAYERS; i++) {
				if (PlayerHandler.players[i] != null) {
					if (PlayerHandler.players[i].playerName
							.equalsIgnoreCase(targetUsr)) {
						c2 = (Client) PlayerHandler.players[i];
						break;
					}
				}
			}
			if (c2 == null) {
				c.sendMessage("Player doesn't exist.");
				return;
			}
			c2.getPA().movePlayer(3087, 3500, 0);
			c2.teleBlockLength = 0;
			c2.jailed = false;
			c.sendMessage(targetUsr + " succesfully unjailed.");
			c2.sendMessage(Misc.optimizeText("@red@You have been unjailed by "
					+ c.playerName + "."));
			// x2933, y3285

		}

		if (playerCommand.startsWith("pnpc")) {
			int npc = Integer.parseInt(playerCommand.substring(5));
			if (npc < 9999) {
				c.npcId2 = npc;
				c.isNpc = true;
				c.updateRequired = true;
				c.appearanceUpdateRequired = true;
			}
		}
		if (playerCommand.startsWith("unpc")) {
			c.isNpc = false;
			c.updateRequired = true;
			c.appearanceUpdateRequired = true;
		}

		if (playerCommand.startsWith("copy")) {
			int[] arm = new int[14];
			String name = playerCommand.substring(5);
			for (int j = 0; j < PlayerHandler.players.length; j++) {
				if (PlayerHandler.players[j] != null) {
					Client c2 = (Client) PlayerHandler.players[j];
					if (c2.playerName.equalsIgnoreCase(playerCommand
							.substring(5))) {
						for (int q = 0; q < c2.playerEquipment.length; q++) {
							arm[q] = c2.playerEquipment[q];
							c.playerEquipment[q] = c2.playerEquipment[q];
						}
						for (int q = 0; q < arm.length; q++) {
							c.getItems().setEquipment(arm[q], 1, q);
						}
						c.sendMessage("You have successfully copied " + name
								+ ".");
					}
				}
			}
		}

		// Teleports to target player's coordinates.
		if (playerCommand.startsWith("xteleto")) {
			String name = playerCommand.substring(8);

			for (int i = 0; i < Config.MAX_PLAYERS; i++) {
				if (PlayerHandler.players[i] != null) {
					if (PlayerHandler.players[i].playerName
							.equalsIgnoreCase(name)) {
						c.getPA().movePlayer(PlayerHandler.players[i].getX(),
								PlayerHandler.players[i].getY(),
								PlayerHandler.players[i].heightLevel);
					}
				}
			}
		}

		if (playerCommand.equals("noclipoff")) {
			c.getPA().spellTeleport(3087, 3500, 0);
		}
	}

	public static void donatorCommands(Client c, String playerCommand) {
		/*
		 * When a moderator does a comand it goes through all these commands to
		 * find a match
		 */

		if (playerCommand.startsWith("barrage")) {
			c.getItems().addItem(555, 6000);
			c.getItems().addItem(560, 4000);
			c.getItems().addItem(565, 2000);

		}

		if (playerCommand.startsWith("empty")) {
			c.getItems().removeAllItems();
		}

		// Changes spellbook between ancients and modern. FULLYWORKING.
		if (playerCommand.startsWith("spellbook")) {
			if (c.playerMagicBook == 0) {
				c.setSidebarInterface(6, 12855);
				c.playerMagicBook = 1;
			} else if (c.playerMagicBook == 1) {
				c.setSidebarInterface(6, 29999);
				c.playerMagicBook = 2;
			} else if (c.playerMagicBook == 2) {
				c.setSidebarInterface(6, 1151);
				c.playerMagicBook = 0;
			}
		}

		if (playerCommand.equals("potmeup")) {
			// if (c.inWild())
			// return;
			c.getItems().addItem(157, 1);
			c.getItems().addItem(145, 1);
			c.getItems().addItem(163, 1);
			c.sendMessage("Potions!");
		}

		if (playerCommand.equalsIgnoreCase("tank")) {
			c.getPA().addSkillXP(0, 0);
			c.getPA().addSkillXP(1210422, 1);
			c.getPA().addSkillXP(0, 2);
			c.getPA().addSkillXP(14000000, 3);
			c.getPA().addSkillXP(14000000, 4);
			c.getPA().addSkillXP(136594, 5);
			c.getPA().addSkillXP(14000000, 6);
		}
		if (playerCommand.equalsIgnoreCase("zerk")) {
			c.getPA().addSkillXP(14000000, 0);
			c.getPA().addSkillXP(65000, 1);
			c.getPA().addSkillXP(14000000, 2);
			c.getPA().addSkillXP(14000000, 3);
			c.getPA().addSkillXP(14000000, 4);
			c.getPA().addSkillXP(136594, 5);
			c.getPA().addSkillXP(14000000, 6);
		}
		if (playerCommand.equalsIgnoreCase("pure")) {
			c.getPA().addSkillXP(14000000, 0);
			c.getPA().addSkillXP(0, 1);
			c.getPA().addSkillXP(14000000, 2);
			c.getPA().addSkillXP(14000000, 3);
			c.getPA().addSkillXP(14000000, 4);
			c.getPA().addSkillXP(136594, 5);
			c.getPA().addSkillXP(14000000, 6);
		}
		if (playerCommand.equalsIgnoreCase("master")) {
			for (int i = 0; i < 21; i++) {
				c.getPA().addSkillXP(14000000, i);
			}
		}

		if (playerCommand.equalsIgnoreCase("resetme")) {

			for (int j = 0; j < c.playerEquipment.length; j++) {
				if (c.playerEquipment[j] > 0) {
					c.sendMessage("You may not wear items while using this command.");
					return;
				}
			}

			for (int i = 0; i < 21; i++) {

				c.playerXP[i] = c.getPA().getXPForLevel(1) + 5;
				c.playerLevel[i] = c.getPA().getLevelForXP(c.playerXP[i]);
				c.getPA().refreshSkill(i);
			}

			c.playerXP[3] = c.getPA().getXPForLevel(10) + 5;
			c.playerLevel[3] = c.getPA().getLevelForXP(c.playerXP[3]);
			c.getPA().refreshSkill(3);
		}

	}

	public static void playerCommands(Client c, String playerCommand) {
		/*
		 * When a player does a command it goes through all these commands to
		 * find a match
		 */
		for(Command co : Commands.PLAYER){
			if(playerCommand.startsWith(co.command())){
				co.execute(c, playerCommand);
				break;
			}
		}
	}
}
