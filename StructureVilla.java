package net.minecraft.world.gen.structure;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.structure.nbt.StructNBT;

public class StructureVilla extends StructurePandoraBase
{
	private static final String STRUCTTYPE = "villa.nbt";
	private static List<BlockPos> DOORS = null;
	
    public StructureVilla()
    {
    	super();
    }
    
    public StructureVilla(int type, int belongness, EnumFacing streetFacing, int x, int z)
    {
        super(type, belongness, streetFacing, x, z, STRUCTTYPE);
        
        if (DOORS == null)
        {
        	DOORS = new ArrayList<>();
        	initDoors(DOORS);
        }
        transferDoors(DOORS);
    }
    
    public static int preStreetWidth()
    {
    	return StructNBT.compounds.get(STRUCTTYPE).getTagList("size", 3).getIntAt(0);
    }
    
    public static int preStreetLength()
    {
    	return StructNBT.compounds.get(STRUCTTYPE).getTagList("size", 3).getIntAt(2);
    }
    
    protected EnumFacing calculateDoorFacing(EnumFacing streetFacing)
    {
    	switch(streetFacing)
    	{
    	case NORTH:
    		return EnumFacing.SOUTH;
    	case WEST:
    		return EnumFacing.EAST;
    	case SOUTH:
    		return EnumFacing.NORTH;
    	case EAST:
    		return EnumFacing.WEST;
    	default:
    		break;
    	}
    	return streetFacing;
    }
}
