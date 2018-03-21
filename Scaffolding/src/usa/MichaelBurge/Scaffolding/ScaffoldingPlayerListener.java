package usa.MichaelBurge.Scaffolding;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerListener;

public class ScaffoldingPlayerListener extends PlayerListener{
	 public Scaffolding plugin;
	 public ScaffoldingPlayerListener(Scaffolding instance) {
	       plugin = instance;
	 }
	//This method is called whenever a player uses a command.
	  public void onPlayerCommand(PlayerChatEvent event) {
		  //Make the message a string.
			String[] split = event.getMessage().split(" ");
			//Get the player that talked.
			Player player = event.getPlayer();
			//If the first part of the string is /basic or /b then do this.
			if (split[0].equalsIgnoreCase("/scaffold")) {
				//Run the method toggleVision for player
				//plugin.toggleVision(player);
				event.setCancelled(true);
			}

		}
}
