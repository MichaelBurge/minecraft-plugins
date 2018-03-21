package usa.MichaelBurge.PuzzleQuest;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import hawox.uquest.*;
import hawox.uquest.questclasses.CurrentQuest;

import org.tal.redstonechips.circuit.CircuitLibrary;

/**
 * PuzzleQuest for Bukkit
 * 
 * @author Michael Burge
 *
 */
public class PuzzleQuest extends CircuitLibrary {
	private static final Logger log = Logger.getLogger("Minecraft");
	public List<QuesterData> completedQuests = new ArrayList<QuesterData>();
	
	@Override
	public void onDisable() {
		PluginDescriptionFile pdfFile = this.getDescription();
		log.log(Level.INFO,pdfFile.getName() + " version " + pdfFile.getVersion() + " disabled.");
	}

	@Override
	public void onEnable() {
		setupUQuest();
		loadData();
        PluginDescriptionFile pdfFile = this.getDescription();
        log.log(Level.INFO,pdfFile.getName() + " version " + pdfFile.getVersion() + " enabled.");
	}

    /**
     * Adds uQuest plugin support
     */
    //create our general quest interaction
    public QuestInteraction questInteraction = null;
    public UQuest uQuest = null;
    
    public void setupUQuest() {
        uQuest = (UQuest)this.getServer().getPluginManager().getPlugin("uQuest");
 
        if (questInteraction == null) {
            if (uQuest != null) {
                questInteraction = new QuestInteraction(uQuest);
            } else {
                System.out.println("[PuzzleQuest] uQuest system not enabled. Disabling plugin.");
                this.getServer().getPluginManager().disablePlugin(this);
            }
        }
    }
 
    public QuestInteraction getQuestInteraction() {
        return questInteraction;
    }
    /**
     * End of uQuest plugin support stuff
     */
    
    public void saveData()
    {
        File file = new File(getDataFolder(), "data.yml");
        QuestFileHandler.save(completedQuests, file);
    }
    public void loadData()
    {
        File file = new File(getDataFolder(), "data.yml");
        completedQuests = QuestFileHandler.load(file);

    }
    
    public Boolean giveQuest(Integer id,Player player)
    {
    	// Check if we've already completed the quest.
    	Boolean completed = false;
    	for (QuesterData q : completedQuests)
    		if (q.player.equalsIgnoreCase(player.getName()))
    		{
    			completed = q.completedQuests.contains(id);
    			break;
    		}
    	completed = completed ? false : questInteraction.giveQuest(id, player);
    	return completed;
    }
    
    /**
     * Is the player's currently active quest the given one?
     * @param Name of the quest to check.
     * @param Name of the player to check.
     * @return
     */
    public Boolean detectQuest(Integer id, Player player)
    {
    	CurrentQuest quest = questInteraction.getCurrentQuest(player, false);
    	if (quest == null)
    		return false;
    	return quest.getName().equalsIgnoreCase(uQuest.getTheQuests().get(id).getName());
    }
    
    /**
     * If the player's quest is equal to the given one, drops it.
     * 
     * @param Name of the quest to drop
     * @param Player to drop the quest for
     * @return
     */
    public Boolean dropQuest(Integer id, Player player)
    {
    	if (detectQuest(id,player))
    	{
    		questInteraction.questDrop(player);
    		return true;
    	} else return false;
    }
    
    
    public Boolean finishQuest(Integer id, Player player)
    {
    	if (detectQuest(id,player))
    	{
    		questInteraction.questTurnInForceDone(player);
    		Boolean exists = false;
    		for (QuesterData q : completedQuests)
    			if (q.player.equals( player.getName()))
    			{
    				exists = true;
    				q.completedQuests.add(id);
    				break;
    			}
 
    		if (!exists)
    		{
    			QuesterData quester = new QuesterData();
    			quester.player = player.getName();
    			quester.completedQuests.add(id);
    			completedQuests.add(quester);
    		}
    		saveData();
    		return true;
    	} else return false;
    }
    
    /**
     * Has the player completed the given quest?
     * @param What is the quest id?
     * @param Which player are we checking?
     * @return
     */
    public Boolean completedQuest(Integer id, Player player)
    {
		for (QuesterData q : completedQuests)
			if (q.player.equals( player.getName()))
			{
				return q.completedQuests.contains(id);
			}
		return false;
    }
    
    /**
     * Clears the quest's completion status. Returns true if the quest was completed.
     * @param id
     * @param player
     * @return
     */
    public Boolean clearQuest(Integer id,Player player)
    {
    	Boolean ret = false;
		for (QuesterData q : completedQuests)
			if (q.player.equals(player.getName()))
			{
				if (q.completedQuests.contains(id))
				{
					ret = q.completedQuests.remove(id);
					saveData();
				}
				break;
			}
		return ret;
    }
    
    if (interfaceBlocks.length!=1) {
        error(sender, "Expecting 1 interface block.");
        return false;
    }

    if (inputs.length!=1) {
        error(sender, "Expecting 1 clock input pin.");
        return false;
    }

    if (outputs.length!=1) {
        error(sender, "Expecting 1 signal output.");
        return false;
    }
    switch (args.length)
    {
    case 0:
    	error(sender,"No quest name given as input for arg 1.");
    	break;
    case 1:
    	error(sender,"No circuit type was given for arg 2('detect','give','drop','completed','finish','clear') a quest).");
    case 3:
        try {
            radius = Integer.decode(args[1]);
            
        } catch (NumberFormatException ne) {
            error(sender, "Incorrect radius: " + args[1]);
            return false;
        }
    case 2:
    	type = args[1];
    	try {
    		questId = Integer.decode(args[0]);
    	} catch (NumberFormatException ne) {
    		error(sender,"Incorrect quest ID: " + args[0]);
    	}
    	// ToDo: Check to see if the quest actually exists(or not).
    	break;
    default:
    	error(sender,"Too many arguments.");
    	break;
    	
    }
    
    Location i = interfaceBlocks[0];
    center = i;
    
    plugin = (PuzzleQuest) Bukkit.getServer().getPluginManager().getPlugin("PuzzleQuest");
    if (plugin == null)
    	error(sender, "PuzzleQuest not a registered plugin.");
    return true;
}
	@SuppressWarnings("rawtypes")
	@Override
	public Class[] getCircuitClasses() {
		return new Class[] { questcircuit.class};
	}
}
