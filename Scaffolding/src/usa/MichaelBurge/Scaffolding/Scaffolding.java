// $Id$
/*
 * WorldEdit
 * Copyright (C) 2010 sk89q <http://www.sk89q.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

package usa.MichaelBurge.Scaffolding;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.event.Event;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.worldedit.*;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.patterns.Pattern;
import com.sk89q.worldedit.tools.BlockTool;
import com.sk89q.worldedit.tools.brushes.Brush;

/**
 * A scaffolding tool that lets you teleport around creating a dirt block underneath you.
 * 
 * @author MichaelBurge
 */
public class Scaffolding extends JavaPlugin {
	private ScaffoldingPlayerListener listener = new ScaffoldingPlayerListener(this);
    private BaseBlock platform;
    private int range;

	@Override
	public void onDisable() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onEnable() {
		PluginManager pm = getServer().getPluginManager();
		//Create PlayerCommand listener
	    pm.registerEvent(Event.Type.PLAYER_COMMAND_PREPROCESS, this.listener, Event.Priority.Normal, this);
	}

}