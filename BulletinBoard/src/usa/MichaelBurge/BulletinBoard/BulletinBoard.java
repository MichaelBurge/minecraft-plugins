package usa.MichaelBurge.BulletinBoard;

//import info.somethingodd.bukkit.odd.item.OddItem;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

import usa.MichaelBurge.BulletinBoard.Bulletin.BulletinType;

import com.iConomy.iConomy;
import com.iConomy.system.Account;
import com.iConomy.system.Holdings;
import com.iConomy.util.Messaging;

import com.sk89q.worldguard.protection.GlobalRegionManager;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.bukkit.BukkitPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.LocalPlayer;

import com.sk89q.worldedit.DisallowedItemException;
import com.sk89q.worldedit.UnknownItemException;
import com.sk89q.worldedit.blocks.*;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;

@SuppressWarnings("unused")
public class BulletinBoard extends JavaPlugin {
    public List<Bulletin> bulletins;
    public String bulletinTag;
    private int minimumPrice;
    private float postingFeePercent; // Some percentage of the listed job is charged to the poster.
    private float acceptFeePercent; // Some percentage of the listed job is charged to the accepter.
    private int bulletinDuration; // in minutes
    private String governmentAccount; // Account that fees are deposited into.
    
    private Logger logger;
    private WorldGuardPlugin worldGuard = null;
    private WorldEditPlugin worldEdit = null;
    
    private Timer expirationTimer = new Timer();

    public static final int EXPIRATION_TIMER_DELAY = 10000;
    public static final int EXPIRATION_TIMER_PERIOD = 1 * 60 * 1000;
    public static final String[] Colors = new String[] { "&c", "&e", "&f", "&7" };
    
    public boolean completeBulletin(int id, String accepterName) {
        if (id < 0 || id >= bulletins.size())
            return false;
        Player accepter = getServer().getPlayer(accepterName);
        Bulletin bulletin = bulletins.get(id);
        Player owner = getServer().getPlayer(bulletin.getOwner());
        
        Holdings accepterBalance = iConomy.getAccount(accepterName).getHoldings();
        Holdings governmentBalance = iConomy.getAccount(governmentAccount).getHoldings();
        
        if (bulletin.getType() == BulletinType.SERVICE)
        {
        	accepterBalance.add(bulletin.getValue());
        	governmentBalance.subtract(bulletin.getValue());
        	
        	Messaging.broadcast(bulletinTag + Colors[1] + accepterName + Colors[0] + " has completed a job for " + Colors[1] + bulletin.getOwner() + Colors[0]
                        + " worth " + Colors[1] + iConomy.format(bulletin.getValue()) + Colors[0] + "!");
        	Messaging.send(accepter, bulletinTag + Colors[0] + "You have been awarded " + Colors[1] + iConomy.format(bulletin.getValue())
                + Colors[0] + ".");
        } else if (bulletin.getType() == BulletinType.ITEM)
        {
        	// If the poster is buying items, the accepter already lost his items and so give them to the poster.
    		if (bulletin.isBuy())
    		{
    			if (addItemsToPlayer(getServer().getPlayer(bulletin.getOwner()),bulletin.getItemType(),bulletin.getItemDamage(),bulletin.getItemAmount()))
    			{
    				Messaging.send(owner, bulletinTag + Colors[0] + "You have received your items.");
    			} else
    			{
    				Messaging.send(owner, bulletinTag + Colors[0] + "Please clear space in your inventory.");
    				return false;
    			}
    		} else; // Do nothing - owner already lost his items and accepter already gained them.

        } else if (bulletin.getType() == BulletinType.REGION)
        {
        	GlobalRegionManager manager = worldGuard.getGlobalRegionManager();
        	
        	ProtectedRegion region = manager.get(accepter.getWorld()).getRegion(bulletin.getRegionName());
        	if (region == null)
        	{
        		Messaging.send(accepter, bulletinTag + Colors[1] + "Region not found. Are you in the right world?");
        		return false;
        	}
        	region.getOwners().removePlayer(bulletin.getOwner());
        	region.getOwners().addPlayer(accepterName);
        	
			Messaging.broadcast(bulletinTag + Colors[1] + accepterName + Colors[0] + " has purchased land from " + 
					Colors[1] + bulletin.getOwner() + Colors[0] + " worth " + Colors[1] + 
					iConomy.format(bulletin.getValue()) + Colors[0] + "!");
			Messaging.send(accepter, bulletinTag + Colors[0] + "You have lost " + Colors[1] + iConomy.format(bulletin.getValue()));
			accepterBalance.subtract(bulletin.getValue());
			iConomy.getAccount(bulletin.getOwner()).getHoldings().add(bulletin.getValue());
        } else 
        {
        	Messaging.send(accepter, bulletinTag + Colors[0] + "Invalid bulletin type detected: Please contact an admin.");
        	return false;
        }
        
        bulletins.remove(id);
        Collections.sort(bulletins);
        
        saveData();

        return true;
    }

    public boolean checkIConomy() {
        Plugin test = this.getServer().getPluginManager().getPlugin("iConomy");
        boolean useIConomy = false;
        if (test != null) {
            useIConomy = true;
        }
        return useIConomy;

    }
    public boolean checkWorldGuard() {
    	worldGuard = (WorldGuardPlugin)getServer().getPluginManager().getPlugin("WorldGuard");
    	return worldGuard !=  null;
    }
    
    public boolean checkWorldEdit() {
    	worldEdit= (WorldEditPlugin)getServer().getPluginManager().getPlugin("WorldEdit");
    	return worldEdit !=  null;
    }
    
    public boolean checkOddItem() {
    	Plugin test = this.getServer().getPluginManager().getPlugin("OddItem");
        boolean useOddItem = false;
        if (test != null) {
            useOddItem = true;
        }
        return useOddItem;
    }
    
    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
        if (args.length == 0) {
            performShowHelp(sender, args);

            return true;
        }

        String commandName = args[0].toLowerCase();
        String[] trimmedArgs = new String[args.length - 1];

        for (int i = 0; i < args.length - 1; i++)
            trimmedArgs[i] = args[i + 1];

        if (commandName.equals("help"))
            performShowHelp(sender, trimmedArgs);
        else if (commandName.equals("list"))
            performListBulletins(sender, trimmedArgs);
        else if (commandName.equals("accept"))
            performAcceptBulletin(sender, trimmedArgs);
        else if (commandName.equals("cancel"))
            performCancelBulletin(sender, trimmedArgs);
        else if (commandName.equals("service"))
            performServiceBulletin(sender, trimmedArgs);
        else if (commandName.equals("region"))
            performRegionBulletin(sender, trimmedArgs);
        else if (commandName.equals("item"))
            performItemBulletin(sender, trimmedArgs);

        return true;
    }
    
    public boolean performServiceBulletin(CommandSender sender, String[] args) {
        if (args.length == 0) {
            performShowServiceHelp(sender);
            return true;
        }

        String commandName = args[0].toLowerCase();
        String[] trimmedArgs = new String[args.length - 1];

        for (int i = 0; i < args.length - 1; i++)
            trimmedArgs[i] = args[i + 1];

        if (commandName.equals("help"))
        	performShowServiceHelp(sender);
        else if (commandName.equals("buy"))
            performNewServiceBulletin(sender, trimmedArgs);
        else if (commandName.equals("view"))
            performViewBulletins(sender, trimmedArgs);

        return true;
    }
    
    public boolean performRegionBulletin(CommandSender sender, String[] args) {
        if (args.length == 0) {
            performShowRegionHelp(sender);
            return true;
        }

        String commandName = args[0].toLowerCase();
        String[] trimmedArgs = new String[args.length - 1];

        for (int i = 0; i < args.length - 1; i++)
            trimmedArgs[i] = args[i + 1];

        if (commandName.equals("help"))
        	performShowRegionHelp(sender);
        else if (commandName.equals("sell"))
            performNewRegionBulletin(sender, trimmedArgs);

        return true;
    }
    public boolean performItemBulletin(CommandSender sender,String[] args) {
        if (args.length == 0) {
            performShowItemHelp(sender);
            return true;
        }

        String commandName = args[0].toLowerCase();
        String[] trimmedArgs = new String[args.length - 1];

        for (int i = 0; i < args.length - 1; i++)
            trimmedArgs[i] = args[i + 1];

        if (commandName.equals("help"))
        	performShowItemHelp(sender);
        else if (commandName.equals("buy"))
            performNewItemBulletin(sender, trimmedArgs,true);
        else if (commandName.equals("sell"))
            performNewItemBulletin(sender, trimmedArgs,false);

        return true;
    }

	public void performShowItemHelp(CommandSender sender)
    {
        Messaging.send(sender, Colors[0] + "-----[ " + Colors[2] + " Item Bulletin Help " + Colors[0] + " ]-----");
        Messaging.send(sender, Colors[1] + "/bulletin item help - Show this information.");
        Messaging.send(sender, Colors[1] + "/bulletin item sell <amount> <item> <price> <message> - Sell your goods at the given price.");
        Messaging.send(sender, Colors[1] + "/bulletin item buy <amount> <item> <price> <message> - Purchase goods at the given price.");
    }
    public void performShowServiceHelp(CommandSender sender)
    {
        Messaging.send(sender, Colors[0] + "-----[ " + Colors[2] + " Service Bulletin Help " + Colors[0] + " ]-----");
        Messaging.send(sender, Colors[1] + "/bulletin service help - Show this information.");
        Messaging.send(sender, Colors[1] + "/bulletin service buy <value> <message> - Ask for services to be done on the bulletin.");
        Messaging.send(sender, Colors[1] + "/bulletin service view - View your accepted service bulletins.");
    }
    public void performShowRegionHelp(CommandSender sender)
    {
        Messaging.send(sender, Colors[0] + "-----[ " + Colors[2] + " Region Bulletin Help " + Colors[0] + " ]-----");
        Messaging.send(sender, Colors[1] + "/bulletin region help - Show this information.");
        Messaging.send(sender, Colors[1] + "/bulletin region  <value> <message> - Sell regions that you own on the bulletin.");
    }
    @Override
    public void onDisable() {
        expirationTimer.cancel();
        expirationTimer.purge();

        PluginDescriptionFile pdfFile = this.getDescription();
        log(pdfFile.getName() + " version " + pdfFile.getVersion() + " disabled.");
    }
    
    private List<Bulletin> listBulletinsAcceptedByPlayer(String accepter) {
        List<Bulletin> acceptedBulletins = new ArrayList<Bulletin>();

        for (Bulletin b : bulletins)
            if (b.getAccepter().equals(accepter))
                acceptedBulletins.add(b);

        return acceptedBulletins;
    }
    
    @Override
    public void onEnable() {
        PluginManager pm = getServer().getPluginManager();

        logger = Logger.getLogger("Minecraft");

        PluginDescriptionFile pdfFile = this.getDescription();
        log(pdfFile.getName() + " version " + pdfFile.getVersion() + " enabled.");

        Configuration config = getConfiguration();

        minimumPrice = config.getInt("minimum-price", 10);
        postingFeePercent = config.getInt("posting-fee-percent", 10) / 100f;
        acceptFeePercent = config.getInt("accept-fee-percent", 5) / 100f;
        bulletinDuration = config.getInt("bulletin-duration", 3 * 24 * 60);
        bulletinTag = config.getString("bulletin-tag", "&e[BULLETIN] ");
        governmentAccount = config.getString("government-account","Government");
        File file = new File(getDataFolder(), "data.yml");
        bulletins = BulletinFileHandler.load(file);

        // iConomy support
        if (checkIConomy()) {
            TimerTask expirationChecker = new ExpirationChecker(this);
            getServer().getScheduler().scheduleAsyncRepeatingTask(this, expirationChecker, EXPIRATION_TIMER_DELAY, EXPIRATION_TIMER_PERIOD);
        } else {
            log("iConomy not found. Disabling.");
            pm.disablePlugin(this);
        }
        
        // WorldEdit support
        if (!checkWorldEdit()) {
        	log("WorldEdit not found. Disabling."); 
        	pm.disablePlugin(this);
        }
        
        // WorldGuard support
        if (!checkWorldGuard()) {
        	log("WorldGuard not found. Disabling."); 
        	pm.disablePlugin(this);
        }
        

        
    }

    public void log(String log) {
        logger.log(Level.INFO, "[Bulletin Board] " + log);
    }

    public void saveData() {
        File file = new File(getDataFolder(), "data.yml");
        BulletinFileHandler.save(bulletins, file);
    }
    
    private boolean performAbandonBulletin(Player accepter, int id) {
        List<Bulletin> acceptedBulletins = listBulletinsAcceptedByPlayer(accepter.getName());

        if (id == -1) {
            Messaging.send(accepter, bulletinTag + Colors[0] + "Bulletin not found.");
            return false;
        }

        acceptedBulletins.get(id).setAccepter("");
        Messaging.send(accepter, bulletinTag + Colors[0] + "Bulletin abandoned.");

        saveData();

        return false;
    }

    private boolean performAcceptBulletin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player))
            return false;

        if (args.length != 1) {
            Messaging.send(sender, Colors[0] + "Usage: /bulletin accept <id#>");
            return false;
        }

        Player accepter = (Player) sender;
        String accepterName = accepter.getName();

        int id = parseBulletinId(args[0], bulletins);

        if (id < 0) {
            Messaging.send(sender, bulletinTag + Colors[0] + "Bulletin not found.");
            return false;
        }

        Bulletin bulletin = bulletins.get(id);

        // Accepting payment for your own bulletin.
        if (bulletin.getOwner().equalsIgnoreCase(accepterName)) {
        	if (bulletin.getAccepter().equals(""))
        	{
        		if (bulletin.getType() == BulletinType.SERVICE)
        			Messaging.send(sender,bulletinTag + Colors[0] + "Nobody has taken your request; you cannot pay them.");
        		else if (bulletin.getType() == BulletinType.ITEM)
        		{
        			if (bulletin.isBuy())
        				Messaging.send(sender,bulletinTag + Colors[0] + "Nobody has turned in your items yet; you cannot retrieve them.");
        			else 
        				Messaging.send(sender, bulletinTag + Colors[0] + "Cannot sell items to yourself. Please use 'cancel' if you want to cancel your bulletin.");
        		}
        		return false;
        	}
        	if (completeBulletin(id,bulletin.getAccepter()))
        	{
        		if (bulletin.getType() == BulletinType.SERVICE)
        			Messaging.send(sender, bulletinTag + Colors[0] + "Sent payment to player: " + bulletin.getAccepter());
        		else if (bulletin.getType() == BulletinType.ITEM)
        			Messaging.send(sender, bulletinTag + Colors[0] + "Items from bulletin retrieved.");
        	}
        	else 
        	{
        		Messaging.send(sender, bulletinTag + Colors[0] + "Error completing bulletin.");
        		return true;
        	}
        	
            return true;
        }

        if (bulletin.getAccepter().equals(accepterName)) {
            Messaging.send(sender, bulletinTag + Colors[0] + "You have already accepted this bulletin!");
            return false;
        }

        // Someone already took this bulletin.
        if (!bulletin.getAccepter().equals(""))
        {
            Messaging.send(sender, bulletinTag + Colors[0] + "The player " + bulletin.getAccepter() + " has already taken this bulletin!");
            return false;
        }
        
        // Doesn't have enough for the acceptance fee.
        Account accepterAccount = iConomy.getAccount(accepterName);
        Account ownerAccount = iConomy.getAccount(bulletin.getOwner());
        
        Holdings accepterBalance = accepterAccount.getHoldings();
        Holdings governmentBalance = iConomy.getAccount(governmentAccount).getHoldings();
        Holdings ownerBalance = ownerAccount.getHoldings();
        
        if (!accepterBalance.hasEnough(bulletin.getAcceptFee())) {
            Messaging.send(sender, bulletinTag + Colors[0] + "You don't have enough money to accept this bulletin!");
            return false;
        }

        accepterBalance.subtract(bulletin.getAcceptFee());
        governmentBalance.add(bulletin.getAcceptFee());
        
        if (bulletin.getType() == BulletinType.SERVICE)
        {
        	GregorianCalendar expiration = new GregorianCalendar();
        	expiration.add(Calendar.MINUTE, bulletinDuration);
        	bulletin.getExpirations().put(accepterName, expiration.getTime());
        

        	int bulletinRelativeTime = (bulletinDuration < 60) ? bulletinDuration : (bulletinDuration < (60 * 24)) ? bulletinDuration / 60
                : (bulletinDuration < (60 * 24 * 7)) ? bulletinDuration / (60 * 24) : bulletinDuration / (60 * 24 * 7);

            String bulletinRelativeAmount = (bulletinDuration < 60) ? " minutes" : (bulletinDuration < (60 * 24)) ? " hours" : (bulletinDuration < (60 * 24 * 7)) ? " days"
                : " weeks";

            Messaging.send(sender,
                bulletinTag + Colors[0] + "Bulletin accepted. You have been charged a " + Colors[2] + iConomy.format(bulletin.getAcceptFee())
                + Colors[1] + " acceptance fee.");
            Messaging.send(sender,bulletinTag + Colors[2] + bulletinRelativeTime + Colors[1] + bulletinRelativeAmount);
            
            bulletin.setAccepter(accepterName);
        } else if (bulletin.getType() == BulletinType.ITEM)
        {
        	if (bulletin.isBuy())
        	{
        		if (removeItemsFromPlayer(accepter,bulletin.getItemType(),bulletin.getItemDamage(),bulletin.getItemAmount()))
        		{
        			
        			Messaging.broadcast(bulletinTag + Colors[1] + accepterName + Colors[0] + " has completed a job for " + Colors[1] + bulletin.getOwner() + Colors[0]
    			                    + " worth " + Colors[1] + iConomy.format(bulletin.getValue()) + Colors[0] + "!");
        			Messaging.send(sender, bulletinTag + Colors[0] + "You have received " + Colors[1] + iConomy.format(bulletin.getValue())
    		                       + Colors[0] + ".");
        			accepterBalance.add(bulletin.getValue());
        			governmentBalance.subtract(bulletin.getValue());
        			
        			bulletin.setAccepter(accepterName);
        		} else
        		{
        			Messaging.send(sender, bulletinTag + Colors[0] + "You do not have the required items in your inventory.");
        			return false;
        		}
        	}
        	else
        	{
        		// If the bulletin is selling, give the accepter his items, charge his account.
        		if (addItemsToPlayer(accepter,bulletin.getItemType(),bulletin.getItemDamage(),bulletin.getItemAmount()))
        		{
        			Messaging.broadcast(bulletinTag + Colors[1] + accepterName + Colors[0] + " has purchased bulletin items from " + 
						Colors[1] + bulletin.getOwner() + Colors[0] + " worth " + Colors[1] + 
						iConomy.format(bulletin.getValue()) + Colors[0] + "!");
        			Messaging.send(sender, bulletinTag + Colors[0] + "You have lost " + Colors[1] + iConomy.format(bulletin.getValue()));
        			accepterBalance.subtract(bulletin.getValue());
        			ownerBalance.add(bulletin.getValue());
        			
        			completeBulletin(id,accepterName);
        		} else
        		{
        			Messaging.send(sender, bulletinTag + Colors[0] + "You do not have enough space in your inventory for the items.");
        			return false;
        		}
        	}
        } else if (bulletin.getType() == BulletinType.REGION)
        {
        	if (!completeBulletin(id,accepterName))
        		return false;
        } else 
        {
        	Messaging.send(sender,bulletinTag + Colors[0] + "Invalid bulletin type detected for bulletin " + id + ". Please contact an admin.");
        }
        Player owner = getServer().getPlayer(bulletin.getOwner());

        if (owner != null) {
            Messaging.send(owner, Colors[0] + "Your bulletin #" + (id + 1) + " for " + Colors[1] + iConomy.format(bulletin.getValue()) + Colors[0] + " has been accepted by " + Colors[1]
                    + accepterName + Colors[0] + ".");
        }

        saveData();

        return true;
    }

    private boolean performCancelBulletin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player))
            return false;

        if (args.length != 1) {
            Messaging.send(sender, Colors[0] + "Usage: /bulletin cancel <id#>");
            return false;
        }

        Player owner = (Player) sender;
        
        int id = parseBulletinId(args[0], bulletins);

        if (id == -1) {
            Messaging.send(sender, bulletinTag + Colors[0] + "Bulletin not found.");
            return false;
        }

        
        Bulletin bulletin = bulletins.get(id);
        
        // If the poster isn't trying to cancel the bulletin....
    	if (!bulletin.getOwner().equalsIgnoreCase(owner.getName())) 
    	{
    		// Abandon if it's the accepter.
    		if (bulletin.getType() == BulletinType.SERVICE) {
        			performAbandonBulletin(owner,id);
        			return true;
        	} else 
        	{
        		Messaging.send(sender, bulletinTag + Colors[0] + "You can only cancel bulletins you created, or cancel service bulletins you are pursuing.");
            	return false;
        	}
    	} else
    	{
        	if (bulletin.getType() == BulletinType.SERVICE)
        	{
        		// Nothing special with service bulletins for the owner; abandoning is done in the 'if' above.
        	}
        	else if (bulletin.getType() == BulletinType.ITEM)
        	{
        		// If the person is selling items and cancels their bulletin, retrieve the items.
        		if (!bulletin.isBuy())
        		{
        			if (addItemsToPlayer(owner,bulletin.getItemType(),bulletin.getItemDamage(),bulletin.getItemAmount()))
        			{
        				Messaging.send(owner, bulletinTag + Colors[0] + "You have received your items again.");
        			} else
        			{
        				Messaging.send(owner, bulletinTag + Colors[0] + "Please clear inventory space for your items.");
        				return false;
        			}
        		} 
        	} else if (bulletin.getType() == BulletinType.REGION)
        	{
        	// Nothing special to do with regions; ownership is transferred when it completes, not before.
        	}
    	}

        bulletins.remove(bulletin);
        Collections.sort(bulletins);

        Holdings ownerBalance = iConomy.getAccount(owner.getName()).getHoldings();
        if (bulletin.isBuy())
        {
        	ownerBalance.add(bulletin.getValue());
        	Messaging.send(sender, bulletinTag + Colors[0] + "You have been reimbursed " + Colors[1] + iConomy.format(bulletin.getValue()) + Colors[0]
                + " for your bulletin.");
        }
        
        Player accepter = getServer().getPlayer(bulletin.getAccepter());

        Holdings accepterAccount = iConomy.getAccount(bulletin.getAccepter()).getHoldings();
        accepterAccount.add(bulletin.getAcceptFee());
        iConomy.getAccount(governmentAccount).getHoldings().subtract(bulletin.getAcceptFee());
        
        Messaging.send(accepter, bulletinTag + Colors[0] + "The bulletin you were pursuing worth " + Colors[1] + bulletin.getValue() + Colors[0]
                + " has been cancelled.");
        Messaging.send(accepter,
                bulletinTag + Colors[0] + "You have been reimbursed the " + Colors[1] + iConomy.format(bulletin.getAcceptFee()) + Colors[0]
                            + " you paid for the bulletin.");

        saveData();

        return true;
    }

    private boolean performListBulletins(CommandSender sender, String[] args) {
        if (!(sender instanceof Player))
            return false;

        String senderName = ((Player) sender).getName();

        int perPage = 7;
        int currentPage;

        if (args.length == 0)
            currentPage = 0;
        else
            currentPage = (args[0] == null) ? 0 : Integer.valueOf(args[0]);
        currentPage = (currentPage == 0) ? 1 : currentPage;
        
        int amountPages = (int) Math.ceil(bulletins.size() / perPage) + 1;
        int pageStart = (currentPage - 1) * perPage;
        int pageEnd = pageStart + perPage - 1;
        pageEnd = (pageEnd >= bulletins.size()) ? bulletins.size() - 1 : pageEnd;

        if (bulletins.isEmpty()) {
            Messaging.send(sender, bulletinTag + Colors[0] + "No bulletins currently listed.");
        } else if (currentPage > amountPages) {
            Messaging.send(sender, bulletinTag + Colors[0] + "Invalid page number.");
        } else {
            Messaging.send(sender, Colors[0] + "Available Bulletins (Page &f#" + currentPage + Colors[0] + " of &f" + amountPages + Colors[0] + " ):");

            for (int i = pageStart; i <= pageEnd; i++) {
                Bulletin b = bulletins.get(i);

                String msg = Colors[3] + (i + 1) + ". " + Colors[1];

                msg += b.getType().name() + Colors[3] + ":" + Colors[1];
                if (b.getType() == BulletinType.SERVICE)
                {
                	msg += iConomy.format(b.getValue()) + Colors[3] + " - " + Colors[1] + "Fee: "
                        + iConomy.format(b.getAcceptFee());

                	if (senderName.equalsIgnoreCase(b.getOwner()))
                		msg += Colors[3] + " *YOURS*";
                	else if (!b.getAccepter().equals(""))
                		msg += Colors[3] + " *CLAIMED* by " + Colors[1] + b.getAccepter();
                	
                	msg += Colors[3] + " - " + "&f" + b.getMessage();
                } else if (b.getType() == BulletinType.ITEM)
                {
                	if (!b.getAccepter().equals(""))
                		msg += Colors[3] + " *COMPLETED* by " + Colors[1] + b.getAccepter() + " - ";
                	
                	if (b.isBuy())
                		msg += "Buying ";
                	else msg += "Selling ";
                	msg += b.getItemAmount() + " ";
                	msg += blockToName((Player)sender, b.getItemType(),b.getItemDamage());
                	msg += " for " + iConomy.format(b.getValue()) + Colors[3] + " - " + Colors[1] + "Fee: "
                    + iConomy.format(b.getAcceptFee());
                	
                	msg += Colors[3] + " - " + "&f" + b.getMessage();
                } else if (b.getType() == BulletinType.REGION)
                {
                	msg += b.getOwner();
                	msg += ": Selling " + b.getRegionName();
                	msg += " for " + iConomy.format(b.getValue()) + Colors[3] + " - " + Colors[1] + "Fee: "
                    + iConomy.format(b.getAcceptFee());
                	
                	msg += Colors[3] + " - " + "&f" + b.getMessage();
                }
                Messaging.send(sender, msg);
            }
        }

        return true;
    }

    private boolean performNewServiceBulletin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player))
            return false;
        
        if (args.length < 1) {
            Messaging.send(sender, Colors[0] + "Usage: /bulletin service buy <value> <message>");
            return false;
        }

        String message = "";
        for (int i = 1;i < args.length;i++)
        	message += args[i] + " ";
        Player owner = (Player) sender;

        int value;

        try {
            value = Integer.parseInt(args[0]);

            if (value < minimumPrice)
                throw new NumberFormatException();
        } catch (NumberFormatException e) {
            Messaging.send(sender, bulletinTag + Colors[0] + "Value must be a number greater than " + Colors[2] + minimumPrice + Colors[0] + ".");
            return false;
        }

        Holdings ownerBalance = iConomy.getAccount(owner.getName()).getHoldings();
        if (!ownerBalance.hasEnough(value)) {
            Messaging.send(sender, bulletinTag + Colors[0] + "You don't have enough money to do that!");
            return false;
        }

        int postingFee = (int) (postingFeePercent * value);
        int award = value - postingFee;
        int contractFee = (int) (acceptFeePercent * award);

        Bulletin bulletin = Bulletin.CreateServiceBulletin(owner.getName(), owner.getDisplayName(), award, postingFee, contractFee, message);
        
        bulletins.add(bulletin);
        Collections.sort(bulletins);

        ownerBalance.subtract(value);
        iConomy.getAccount(governmentAccount).getHoldings().add(value);
        
        Messaging.send(sender, bulletinTag + Colors[0] + "You have been charged a " + Colors[1] + iConomy.format(postingFee) + Colors[0]
                + " fee for posting a bulletin post.");
        Messaging.broadcast(bulletinTag + Colors[1] + "A new bulletin has been placed for " + Colors[2] + iConomy.format(award) + Colors[1] + ".");

        saveData();
        return true;
    }
    
    private Material stringToBlockId(Player player,String arg) throws UnknownItemException, DisallowedItemException
    {
    	return Material.matchMaterial(arg);
    }
    
    private boolean performNewItemBulletin(CommandSender sender, String[] args, boolean isBuy) {
        if (!(sender instanceof Player))
            return false;
        
        if (args.length < 3) {
            Messaging.send(sender, Colors[0] + "Usage: /bulletin item buy <amount> <item> <price> <message>");
            return false;
        }

        String message = "";
        for (int i = 3;i < args.length;i++)
        	message += args[i] + " ";
        Player owner = (Player) sender;

        int value;
        int itemID;
        int itemAmount;
        byte itemData = 0;
        
        // Parse price.
        try {
            value = Integer.parseInt(args[2]);

            if (value < minimumPrice)
                throw new NumberFormatException();
        } catch (NumberFormatException e) {
            Messaging.send(sender, bulletinTag + Colors[0] + "Value must be a number greater than " + Colors[2] + minimumPrice + Colors[0] + ".");
            return false;
        }
        
        // Parse block id.
        
        String[] gData = args[1].split(":");
        Material material = Material.matchMaterial(gData[0]);
        if (material == null)
        {
        	Messaging.send(sender, bulletinTag + Colors[0] + "Unknown item: " + gData[0]);
        	return false;
        }
        itemID = material.getId();
        if (gData.length == 2) {
            itemData = Byte.valueOf(gData[1]);
        }
        // Parse amount.
        try {
        	itemAmount = Integer.parseInt(args[0]);
        	if (itemAmount < 0)
        		throw new NumberFormatException();
        } catch (NumberFormatException e)
        {
            Messaging.send(sender, bulletinTag + Colors[0] + "Item amount must be a non-negative integer." + Colors[0] + ".");
            return false;
        }
        
        int postingFee = (int) (postingFeePercent * value);
        int award = value - postingFee;
        int contractFee = (int) (acceptFeePercent * award);
        int charge = isBuy ? value : postingFee;
        
        Holdings ownerBalance = iConomy.getAccount(owner.getName()).getHoldings();
        // Check if he has enough money to post a buy or sell listing.
        // TODO Only allow item jobs to be posted from within a physical bulletin board location.
        if (!ownerBalance.hasEnough(charge)) {
            Messaging.send(sender, bulletinTag + Colors[0] + "You don't have enough money to do that!");
            return false;
        }
        if (!isBuy)
        {
        	if (!removeItemsFromPlayer(owner,itemID,itemData,itemAmount))
        	{
                Messaging.send(sender, bulletinTag + Colors[0] + "You don't have enough of that item to post that job!");
                return false;
        	}
        }

        Bulletin bulletin = Bulletin.CreateItemBulletin(owner.getName(), award, postingFee, contractFee, itemID, itemData, itemAmount, message,isBuy);
        
        bulletins.add(bulletin);
        Collections.sort(bulletins);

        ownerBalance.subtract(charge);
        iConomy.getAccount(governmentAccount).getHoldings().add(charge);
        
        Messaging.send(sender, bulletinTag + Colors[0] + "You have been charged a " + Colors[1] + iConomy.format(postingFee) + Colors[0]
                + " fee for posting a bulletin post.");
        Messaging.broadcast(bulletinTag + Colors[1] + "A new item bulletin has been placed " + (isBuy ? "purchasing " : "selling ") + 
        		itemAmount + " " + blockToName(owner,itemID,itemData) + " for " + 
        		Colors[2] + iConomy.format(award) + Colors[1] + ".");
        saveData();
        return true;
    }
    private boolean performNewRegionBulletin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player))
            return false;
        
        if (args.length < 3) {
            Messaging.send(sender, Colors[0] + "Usage: /bulletin region sell <region name> <price> <message>");
            return false;
        }

        String message = "";
        for (int i = 2;i < args.length;i++)
        	message += args[i] + " ";
        Player owner = (Player) sender;

        int value;
        String regionName = args[0];
        try {
            value = Integer.parseInt(args[1]);

            if (value < minimumPrice)
                throw new NumberFormatException();
        } catch (NumberFormatException e) {
            Messaging.send(sender, bulletinTag + Colors[0] + "Value must be a number greater than " + Colors[2] + minimumPrice + Colors[0] + ".");
            return false;
        }
        
        int postingFee = (int) (postingFeePercent * value);
        int award = value - postingFee;
        int contractFee = (int) (acceptFeePercent * award);
        
        Holdings ownerBalance = iConomy.getAccount(owner.getName()).getHoldings();
        // Check if he has enough money to post a buy or sell listing.
        if (!ownerBalance.hasEnough(postingFee)) {
            Messaging.send(sender, bulletinTag + Colors[0] + "You don't have enough money to do that!");
            return false;
        }
        
        // If the region in the poster's world doesn't have the poster as a region owner, then error.
        GlobalRegionManager manager = worldGuard.getGlobalRegionManager();
        
        World w = owner.getWorld();
        
        RegionManager regionManager = manager.get(w);
        ProtectedRegion region = regionManager.getRegion(regionName);
        if (region == null)
        {
        	Messaging.send(sender, bulletinTag + Colors[0] + "This region does not exist.");
        	return false;
        }
        
        if (!region.isOwner(new BukkitPlayer(worldGuard,owner)))
        {
        	Messaging.send(sender, bulletinTag + Colors[0] + "You are not the owner of that region!");
        	return false;
        }
        
        Bulletin bulletin = Bulletin.CreateRegionBulletin(owner.getName(), award, postingFee, contractFee, regionName, message);

        bulletins.add(bulletin);
        Collections.sort(bulletins);

        ownerBalance.subtract(postingFee);
        iConomy.getAccount(governmentAccount).getHoldings().add(postingFee);
        
        Messaging.send(sender, bulletinTag + Colors[0] + "You have been charged a " + Colors[1] + iConomy.format(postingFee) + Colors[0]
                + " fee for posting a bulletin post.");
        Messaging.broadcast(bulletinTag + Colors[1] + "A new region bulletin has been placed selling " +
        		regionName + " for " + Colors[2] + iConomy.format(award) + Colors[1] + ".");
        saveData();
        return true;
    }
    private boolean performShowHelp(CommandSender sender, String[] args) {
        Messaging.send(sender, Colors[0] + "-----[ " + Colors[2] + " Bulletin Board Help " + Colors[0] + " ]-----");
        Messaging.send(sender, Colors[1] + "/bulletin help - Show this information.");
        Messaging.send(sender, Colors[1] + "/bulletin list <page#> - List available entries.");
        Messaging.send(sender, Colors[1] + "/bulletin accept <id#> - Accept bulletin by id.");
        Messaging.send(sender, Colors[1] + "/bulletin cancel <id#> - Cancel bulletin by id.");
        Messaging.send(sender, Colors[1] + "/bulletin service buy <value> <message> - Ask for services to be done on the bulletin.");
        Messaging.send(sender, Colors[1] + "/bulletin service view - View your accepted service bulletins.");
        Messaging.send(sender, Colors[1] + "/bulletin region sell <region name> <price> - Sell ownership of a region on the bulletin board.");
        Messaging.send(sender, Colors[1] + "/bulletin item sell <amount> <item> <price> - Sell items on the bulletin board.");
        Messaging.send(sender, Colors[1] + "/bulletin item buy <amount> <item> <price> - Buy items on the bulletin board.");
        return false;
    }

    private boolean performViewBulletins(CommandSender sender, String[] args) {
        if (!(sender instanceof Player))
            return false;

        Player accepter = (Player) sender;
        String accepterName = accepter.getName();

        List<Bulletin> acceptedBulletins = listBulletinsAcceptedByPlayer(accepterName);

        if (acceptedBulletins.isEmpty()) {
            Messaging.send(sender, bulletinTag + Colors[0] + "You currently have no accepted bulletins.");
        } else {
            Messaging.send(sender, Colors[0] + "Accepted Bulletins:");
            for (int i = 0; i < acceptedBulletins.size(); i++) {
                Bulletin b = acceptedBulletins.get(i);
                int bulletinDuration = b.getMinutesLeft(accepterName);

                int bulletinRelativeTime = (bulletinDuration < 60) ? bulletinDuration : (bulletinDuration < (60 * 24)) ? bulletinDuration / 60
                        : (bulletinDuration < (60 * 24 * 7)) ? bulletinDuration / (60 * 24) : bulletinDuration / (60 * 24 * 7);

                String bulletinRelativeAmount = (bulletinDuration < 60) ? " minutes" : (bulletinDuration < (60 * 24)) ? " hours"
                        : (bulletinDuration < (60 * 24 * 7)) ? " days" : " weeks";

                Messaging.send(sender, Colors[2] + (i + 1) + ". " + Colors[1] + b.getOwner() + " - " + iConomy.format(b.getValue()) + " - "
                        + bulletinRelativeTime + bulletinRelativeAmount);
            }
        }

        return true;
    }

    public static int parseBulletinId(String idStr, List<Bulletin> bulletins) {
        int id;

        try {
            id = Integer.parseInt(idStr) - 1;

            if (id < 0 || id >= bulletins.size())
                throw new IndexOutOfBoundsException();
        } catch (Exception e) {
            return -1;
        }

        return id;
    }
    
    public String blockToName(Player player, int itemType, short s)
    {
    	if (s > 0)
    		return new MaterialData(itemType,(byte) s).toString();
    	else return new MaterialData(itemType).getItemType().name();
    }
    
    /*
     * If the player has enough items of the given type, it removes a certain amount of them. Returns whether
     * the player had enough items.
     */
    public boolean removeItemsFromPlayer(Player player, int itemType, short itemDamage, int itemAmount)
    {
    	ItemStack stack = new ItemStack(itemType,itemAmount,itemDamage);
    	ItemStack[] isPlayerTemp = player.getInventory().getContents().clone();

    	HashMap<Integer,ItemStack> iiItemsLeftover = player.getInventory().removeItem(stack);
    	if(!iiItemsLeftover.isEmpty()){
    	    player.getInventory().setContents(isPlayerTemp);

    	    //didn't have the items

    	    return false;
    	}
    	return true;
    }
    
    /*
     * Attempts to give the player the given number of items. Returns if successful, reverting changes if not.
     */
    public boolean addItemsToPlayer(Player player, int itemType, short itemDamage, int itemAmount)
    {
    	ItemStack stack = new ItemStack(itemType,itemAmount,itemDamage);
    	ItemStack[] isPlayerTemp = player.getInventory().getContents().clone();

    	HashMap<Integer,ItemStack> iiItemsLeftover = player.getInventory().addItem(stack);
    	if(!iiItemsLeftover.isEmpty()){
    	    player.getInventory().setContents(isPlayerTemp);

    	    //didn't have the items

    	    return false;
    	}
    	return true;
    }
}

class ExpirationChecker extends TimerTask {

    private BulletinBoard plugin;

    public ExpirationChecker(BulletinBoard plugin) {
        this.plugin = plugin;
    }

    public BulletinBoard getPlugin() {
        return plugin;
    }

    @Override
    public void run() {
        for (Bulletin bulletin : plugin.bulletins) {
            String[] accepters = bulletin.getExpirations().keySet().toArray(new String[0]);
            for (String name : accepters) {
                if (bulletin.getMillisecondsLeft(name) <= 0) {
                    Player accepter = plugin.getServer().getPlayer(name);
                    
                    bulletin.setAccepter("");
                    bulletin.getExpirations().remove(name);

                    plugin.saveData();

                    if (accepter == null)
                        continue;

                    Messaging.send(accepter,
                            plugin.bulletinTag + BulletinBoard.Colors[0] + "Your bulletin worth " + BulletinBoard.Colors[1] + iConomy.format(bulletin.getValue())
                                    + BulletinBoard.Colors[0] + " has expired!");
                }
            }
        }
    }

    public void setPlugin(BulletinBoard plugin) {
        this.plugin = plugin;
    }

}