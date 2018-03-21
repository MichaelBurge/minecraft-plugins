package usa.MichaelBurge.Scaffolding;

import java.util.HashSet;
import java.util.Set;
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
public class Scaffolding implements Brush {
    private BaseBlock platform;
    private int range;
    
    public Scaffolding(BaseBlock block) {
    	platform = block;
    }
	@Override
    public void build(EditSession editSession, Vector pos, Pattern mat, int size)
    throws MaxChangedBlocksException {
		editSession.setBlockIfAir(pos, platform);
	}

}