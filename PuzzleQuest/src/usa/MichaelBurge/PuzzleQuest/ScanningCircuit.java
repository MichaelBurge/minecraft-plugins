package usa.MichaelBurge.PuzzleQuest;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.tal.redstonechips.circuit.Circuit;
/**
 *
 * @author Michael Burge(modification of Tal Eisenberg's PIR sensor)
 * Scans all the nearby players on input, performing some action.
 */
public class ScanningCircuit extends Circuit {
    public Location center;
    public int radius = 10;
    
    // What should we return once we scan a player?
    protected boolean action(Player p)
    {
    	return false;
    }
    @Override
    public void inputChange(int inIdx, boolean state) {
        if (state) {
            // clock pin triggered
            boolean alarm = false;

            for (Player p : world.getPlayers()) {
                Location l = p.getLocation();

                if (isInRadius(center, l, radius)) {
                	alarm = action(p);
                } 
            }            

            sendOutput(0, alarm);
        } else
        {
        	sendOutput(0,false);
        }
    }

    @Override
    protected boolean init(CommandSender sender, String[] args) {
    	return true;
    }

    private static boolean isInRadius(Location loc1, Location loc2, double radius)  {
        return (loc1.getX() - loc2.getX())*(loc1.getX() - loc2.getX()) + (loc1.getY() - loc2.getY())*(loc1.getY() - loc2.getY()) + (loc1.getZ() - loc2.getZ())*(loc1.getZ() - loc2.getZ()) <= radius*radius;
    }
}