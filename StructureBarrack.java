package net.minecraft.world.gen.structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityNPC;
import net.minecraft.entity.player.Identities;
import net.minecraft.entity.player.ai.logic.EntityAIGuard;
import net.minecraft.entity.player.ai.logic.PlayerNameMap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.nbt.StructNBT;

public class StructureBarrack extends StructurePandoraBase
{
	private static final String STRUCTTYPE = "barrack.nbt";
	private static List<BlockPos> DOORS = null;
	
    public StructureBarrack()
    {
    	super();
    }
    
    public StructureBarrack(int type, int belongness, EnumFacing streetFacing, int x, int z)
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
    
    @Override
	public void generateEntities(World worldIn, Random randomIn, NBTTagList blocks, IBlockState[] states)
	{
    	super.generateEntities(worldIn, randomIn, blocks, states);
    	if (!this.isAllChunkLoaded())
    	{
    		return;
    	}

    	for (int i = 0; i < 1; i++)
    	{
    		initGuard(worldIn, randomIn);
    	}
	}

	protected EnumFacing calculateDoorFacing(EnumFacing streetFacing)
    {
    	return streetFacing;
    }

    private boolean initGuard(World worldIn, Random randomIn)
    {
		EntityNPC citizen = new EntityNPC(worldIn, PlayerNameMap.getRandomName());
		citizen.setOutfit(EntityNPC.getOutfit("iron"));
		citizen.inventory.addItemStackToInventory(new ItemStack(Items.IRON_SWORD, 1));
		citizen.inventory.addItemStackToInventory(new ItemStack(Items.LEAD, 1));
		citizen.switchMainHandToItem(Items.IRON_SWORD);
		citizen.setHeldItem(EnumHand.OFF_HAND, EntityNPC.getShield());
		citizen.mind.getMemory().setIdentity(Identities.SOlDIER);
		EntityAIGuard guardJob = citizen.mind.addGuardJob();
		guardJob.setGuardLocations(this.getGuardLocations());
		return spawnPlayerInLocations(worldIn, randomIn, citizen, this.getGuardSpawnLocations());
    }
    
    private Vec3d[][] getGuardSpawnLocations()
    {
    	return getGuardLocations();
    }
    
    private Vec3d[][] getGuardLocations()
    {
    	Vec3d[][] locations = new Vec3d[4][2];
        
        locations[0][0] = new Vec3d(this.getBoundingBox().minX - 1, this.getBoundingBox().minY + 1, this.getBoundingBox().minZ - 1);
        locations[0][1] = new Vec3d(this.getBoundingBox().minX - 1, this.getBoundingBox().minY + 1, this.getBoundingBox().minZ - 1);
        locations[1][0] = new Vec3d(this.getBoundingBox().maxX + 1, this.getBoundingBox().minY + 1, this.getBoundingBox().minZ - 1);
        locations[1][1] = new Vec3d(this.getBoundingBox().maxX + 1, this.getBoundingBox().minY + 1, this.getBoundingBox().minZ - 1);
        locations[2][0] = new Vec3d(this.getBoundingBox().minX - 1, this.getBoundingBox().minY + 1, this.getBoundingBox().maxZ + 1);
        locations[2][1] = new Vec3d(this.getBoundingBox().minX - 1, this.getBoundingBox().minY + 1, this.getBoundingBox().maxZ + 1);
        locations[3][0] = new Vec3d(this.getBoundingBox().maxX + 1, this.getBoundingBox().minY + 1, this.getBoundingBox().maxZ + 1);
        locations[3][1] = new Vec3d(this.getBoundingBox().maxX + 1, this.getBoundingBox().minY + 1, this.getBoundingBox().maxZ + 1);
    	
    	return locations;
    }
}
