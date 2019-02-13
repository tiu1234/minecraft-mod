package net.minecraft.world.gen.structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.player.EntityNPC;
import net.minecraft.entity.player.Identities;
import net.minecraft.entity.player.ai.logic.EntityAIFarm;
import net.minecraft.entity.player.ai.logic.EntityAIGuard;
import net.minecraft.entity.player.ai.logic.PlayerNameMap;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBed;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.nbt.StructNBT;

public class StructureFarm extends StructurePandoraBase
{
	private static final String STRUCTTYPE = "farm1.nbt";
	private static List<BlockPos> DOORS = null;
	
	private Vec3d[][] fieldLocations;
	private Vec3d[][] craftLocations;
	private Vec3d[][] cowLocations;
	private Vec3d[][] storageLocations;
	private Vec3d[][] bedLocations;
	
    public StructureFarm()
    {
    	super();
    }
    
    public StructureFarm(int type, int belongness, EnumFacing streetFacing, int x, int z)
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
    	return StructNBT.compounds.get(STRUCTTYPE).getTagList("size", 3).getIntAt(2);
    }
    
    public static int preStreetLength()
    {
    	return StructNBT.compounds.get(STRUCTTYPE).getTagList("size", 3).getIntAt(0);
    }
    
    @Override
    public int facingStreetWidth()
    {
    	return this.length;
    }
    
    @Override
    public void generateEntities(World worldIn, Random randomIn, NBTTagList blocks, IBlockState[] states)
    {
    	super.generateEntities(worldIn, randomIn, blocks, states);
    	if (!this.isAllChunkLoaded())
    	{
    		return;
    	}
    	
    	Block[][][] blockType = initBlockTypes(blocks, states);
    	fieldLocations = initLocations(Blocks.WHEAT, blockType);
    	craftLocations = initLocations(Blocks.CRAFTING_TABLE, blockType);
    	storageLocations = initLocations(Blocks.CHEST, blockType);
    	bedLocations = initLocations(Blocks.BED, blockType);
    	
    	int slaveNum = 3 + randomIn.nextInt(3);
    	for (int i = 0; i < slaveNum; i++)
    	{
	    	initSlaves(worldIn, randomIn);
    	}

    	for (int i = 0; i < 1; i++)
    	{
    		initGuard(worldIn, randomIn);
    	}

    	for (int i = 0; i < 20; i++)
    	{
    		initCows(worldIn, randomIn);
    	}
    }
    
	@Override
	public boolean addComponentParts(World worldIn, Random randomIn, StructureBoundingBox structureBoundingBoxIn)
	{
        if (this.averageGroundLvl < 0)
        {
            this.averageGroundLvl = this.getAverageGroundLevel(worldIn, structureBoundingBoxIn);

            if (this.averageGroundLvl < 0)
            {
                return true;
            }

            this.boundingBox.offset(0, this.averageGroundLvl - this.boundingBox.maxY + height - 1, 0);
        }

        if (StructNBT.compounds.containsKey(structType))
        {
        	NBTTagList blocks = StructNBT.compounds.get(this.getStructType()).getTagList("blocks", 10);
        	

        	NBTTagList palette = StructNBT.compounds.get(this.getStructType()).getTagList("palette", 10);
        	IBlockState[] states = new IBlockState[palette.tagCount()];
        	
        	for (int i = 0; i < palette.tagCount(); i++)
        	{
        		states[i] = NBTUtil.readBlockState(palette.getCompoundTagAt(i));
        	}
        	for (int i = 0; i < blocks.tagCount(); i++)
        	{
        		NBTTagList pos = blocks.getCompoundTagAt(i).getTagList("pos", 3);
        		int state = blocks.getCompoundTagAt(i).getInteger("state");
        		
        		this.fillWithBlocks(worldIn, structureBoundingBoxIn, pos.getIntAt(0), pos.getIntAt(1), length - 1 - pos.getIntAt(2), pos.getIntAt(0), pos.getIntAt(1), length - 1 - pos.getIntAt(2), states[state], states[state], false);


                int x = this.getXWithOffset(pos.getIntAt(0), length - 1 - pos.getIntAt(2));
                int y = this.getYWithOffset(pos.getIntAt(1));
                int z = this.getZWithOffset(pos.getIntAt(0), length - 1 - pos.getIntAt(2));
                BlockPos blockpos = new BlockPos(x, y, z);
        		
        		if (structureBoundingBoxIn.isVecInside(new Vec3i(x, y, z)) && states[state].getBlock() == Blocks.BED)
        		{
        			TileEntity tileentity = worldIn.getTileEntity(blockpos);
                    if (tileentity instanceof TileEntityBed)
                    {
                        ((TileEntityBed)tileentity).func_193052_a(EnumDyeColor.ORANGE);
                    }
        		}
        	}

        	this.init++;
    		generateEntities(worldIn, randomIn, blocks, states);
    		
            return true;
        }
        else
        {
        	return false;
        }
	}
    
    private boolean initSlaves(World worldIn, Random randomIn)
    {
		EntityNPC citizen = new EntityNPC(worldIn, PlayerNameMap.getRandomName());
		citizen.mind.getMemory().setIdentity(Identities.SLAVE);
		EntityAIFarm farmJob = citizen.mind.addFarmJob();
		farmJob.setFieldLocations(this.fieldLocations.clone());
		farmJob.setCowLocations(getCowLocations());
		farmJob.setCraftLocations(this.craftLocations.clone());
		farmJob.setStorageLocations(this.storageLocations.clone());
		citizen.mind.getMemory().addLocation("bed", this.bedLocations.clone());
		return spawnPlayerInLocations(worldIn, randomIn, citizen, this.getSlaveSpawnLocations());
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
    
    private boolean initCows(World worldIn, Random randomIn)
    {
		EntityCow cow = new EntityCow(worldIn);
		return spawnAnimalInLocations(worldIn, randomIn, cow, getCowSpawnLocations());
    }
    
    private Block[][][] initBlockTypes(NBTTagList blocks, IBlockState[] states)
    {

    	int sizeX = this.boundingBox.maxX - this.boundingBox.minX;
    	int sizeY = this.boundingBox.maxY - this.boundingBox.minY;
    	int sizeZ = this.boundingBox.maxZ - this.boundingBox.minZ;
    	Block[][][] blockTypes = new Block[sizeX + 1][sizeY + 1][sizeZ + 1];
    	
    	for (int i = 0; i < blocks.tagCount(); i++)
    	{
    		NBTTagList pos = blocks.getCompoundTagAt(i).getTagList("pos", 3);
    		int state = blocks.getCompoundTagAt(i).getInteger("state");
			
			int x = pos.getIntAt(0);
			int y = pos.getIntAt(1);
			int z = length - 1 - pos.getIntAt(2);
			
	        int X = this.getXWithOffset(x, z) - this.boundingBox.minX;
	        int Y = this.getYWithOffset(y) - this.boundingBox.minY;
	        int Z = this.getZWithOffset(x, z) - this.boundingBox.minZ;
			
			blockTypes[X][Y][Z] = states[state].getBlock();
    	}
    	
    	return blockTypes;
    }

    private Vec3d[][] getCowLocations()
    {
    	Vec3d[][] locations = new Vec3d[1][2];
    	
    	int lowX = 6;
    	int lowY = 1;
    	int lowZ = length - 1 - 59;
        int realLowX = this.getXWithOffset(lowX, lowZ);
        int realLowY = this.getYWithOffset(lowY);
        int realLowZ = this.getZWithOffset(lowX, lowZ);
    	int upX = 6 + 40;
    	int upY = 1;
    	int upZ = length - 1 - 61 - 8;
        int realUpX = this.getXWithOffset(upX, upZ);
        int realUpY = this.getYWithOffset(upY);
        int realUpZ = this.getZWithOffset(upX, upZ);
        locations[0][0] = new Vec3d(Math.min(realLowX, realUpX), Math.min(realLowY, realUpY), Math.min(realLowZ, realUpZ));
        locations[0][1] = new Vec3d(Math.max(realLowX, realUpX), Math.max(realLowY, realUpY), Math.max(realLowZ, realUpZ));
    	
    	return locations;
    }
    
    private Vec3d[][] getCowSpawnLocations()
    {
    	Vec3d[][] locations = new Vec3d[1][2];
    	
    	int lowX = 7;
    	int lowY = 1;
    	int lowZ = length - 1 - 60;
        int realLowX = this.getXWithOffset(lowX, lowZ);
        int realLowY = this.getYWithOffset(lowY);
        int realLowZ = this.getZWithOffset(lowX, lowZ);
    	int upX = 7 + 38;
    	int upY = 1;
    	int upZ = length - 1 - 60 - 8;
        int realUpX = this.getXWithOffset(upX, upZ);
        int realUpY = this.getYWithOffset(upY);
        int realUpZ = this.getZWithOffset(upX, upZ);
        locations[0][0] = new Vec3d(Math.min(realLowX, realUpX), Math.min(realLowY, realUpY), Math.min(realLowZ, realUpZ));
        locations[0][1] = new Vec3d(Math.max(realLowX, realUpX), Math.max(realLowY, realUpY), Math.max(realLowZ, realUpZ));
    	
    	return locations;
    }
    
    private Vec3d[][] getSlaveSpawnLocations()
    {
    	return this.fieldLocations;
    }
    
    private Vec3d[][] getGuardSpawnLocations()
    {
    	Vec3d[][] locations = new Vec3d[1][2];
    	
    	int lowX = 58;
    	int lowY = 1;
    	int lowZ = length - 1 - 28;
        int realLowX = this.getXWithOffset(lowX, lowZ);
        int realLowY = this.getYWithOffset(lowY);
        int realLowZ = this.getZWithOffset(lowX, lowZ);
    	int upX = 58;
    	int upY = 1;
    	int upZ = length - 1 - 28;
        int realUpX = this.getXWithOffset(upX, upZ);
        int realUpY = this.getYWithOffset(upY);
        int realUpZ = this.getZWithOffset(upX, upZ);
        locations[0][0] = new Vec3d(Math.min(realLowX, realUpX), Math.min(realLowY, realUpY), Math.min(realLowZ, realUpZ));
        locations[0][1] = new Vec3d(Math.max(realLowX, realUpX), Math.max(realLowY, realUpY), Math.max(realLowZ, realUpZ));
    	
    	return locations;
    }
    
    private Vec3d[][] getGuardLocations()
    {
    	Vec3d[][] locations = new Vec3d[5][2];
    	
    	int lowX = 58;
    	int lowY = 1;
    	int lowZ = length - 1 - 28;
        int realLowX = this.getXWithOffset(lowX, lowZ);
        int realLowY = this.getYWithOffset(lowY);
        int realLowZ = this.getZWithOffset(lowX, lowZ);
    	int upX = 58;
    	int upY = 1;
    	int upZ = length - 1 - 28;
        int realUpX = this.getXWithOffset(upX, upZ);
        int realUpY = this.getYWithOffset(upY);
        int realUpZ = this.getZWithOffset(upX, upZ);
        locations[0][0] = new Vec3d(Math.min(realLowX, realUpX), Math.min(realLowY, realUpY), Math.min(realLowZ, realUpZ));
        locations[0][1] = new Vec3d(Math.max(realLowX, realUpX), Math.max(realLowY, realUpY), Math.max(realLowZ, realUpZ));
        
        locations[1][0] = new Vec3d(this.getBoundingBox().minX - 1, this.getBoundingBox().minY + 1, this.getBoundingBox().minZ - 1);
        locations[1][1] = new Vec3d(this.getBoundingBox().minX - 1, this.getBoundingBox().minY + 1, this.getBoundingBox().minZ - 1);
        locations[2][0] = new Vec3d(this.getBoundingBox().maxX + 1, this.getBoundingBox().minY + 1, this.getBoundingBox().minZ - 1);
        locations[2][1] = new Vec3d(this.getBoundingBox().maxX + 1, this.getBoundingBox().minY + 1, this.getBoundingBox().minZ - 1);
        locations[3][0] = new Vec3d(this.getBoundingBox().minX - 1, this.getBoundingBox().minY + 1, this.getBoundingBox().maxZ + 1);
        locations[3][1] = new Vec3d(this.getBoundingBox().minX - 1, this.getBoundingBox().minY + 1, this.getBoundingBox().maxZ + 1);
        locations[4][0] = new Vec3d(this.getBoundingBox().maxX + 1, this.getBoundingBox().minY + 1, this.getBoundingBox().maxZ + 1);
        locations[4][1] = new Vec3d(this.getBoundingBox().maxX + 1, this.getBoundingBox().minY + 1, this.getBoundingBox().maxZ + 1);
    	
    	return locations;
    }
}
