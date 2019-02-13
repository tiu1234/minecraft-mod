package net.minecraft.world.gen.structure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockQuartz;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.item.EntityPainting;
import net.minecraft.entity.player.EntityNPC;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBanner;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.nbt.StructNBT;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraft.world.gen.structure.template.TemplateManager;

public class StructurePandoraBase extends StructureComponent
{
	public static final int YPOSITION = 17;
	
	protected int width = 0;
	protected int length = 0;
	protected int height = 0;
	protected int yPosition = 17;
    protected String structType = "";
    protected int averageGroundLvl = -1;
    protected int structureType;
    protected int structureBelongness;
    public List<BlockPos> doors;
    
    protected volatile int init;
    
    public StructurePandoraBase()
    {
        init = 0;
    }
    
    public StructurePandoraBase(int type, int belongness, EnumFacing streetFacing, int x, int z, String structType)
    {
        super(type);
        init = 0;
        this.structureType = type;
        this.structureBelongness = belongness;
        this.setCoordBaseMode(calculateDoorFacing(streetFacing));
        this.structType = structType;
        
        NBTTagList size = StructNBT.compounds.get(structType).getTagList("size", 3);
        this.width = size.getIntAt(0);
        this.height = size.getIntAt(1);
        this.length = size.getIntAt(2);
        this.yPosition = YPOSITION;
        
        updateBoundingBox(x, z);
    }
    
    public int facingStreetWidth()
    {
    	return this.width;
    }

    public static StructurePandoraBase findIntersectingPandora(List<StructurePandoraBase> listIn, StructureBoundingBox boundingboxIn)
    {
        for (StructurePandoraBase structurecomponent : listIn)
        {
            if (structurecomponent.getBoundingBox() != null && structurecomponent.getBoundingBox().intersectsWith(boundingboxIn))
            {
                return structurecomponent;
            }
        }

        return null;
    }
    
    protected EnumFacing calculateDoorFacing(EnumFacing streetFacing)
    {
    	return streetFacing;
    }
    
    protected void updateBoundingBox(int x, int z)
    {
        if (this.getCoordBaseMode().getAxis() == EnumFacing.Axis.Z)
        {
            this.boundingBox = new StructureBoundingBox(x, yPosition, z, x + width - 1, yPosition + height, z + length - 1);
        }
        else
        {
            this.boundingBox = new StructureBoundingBox(x, yPosition, z, x + length - 1, yPosition + height, z + width - 1);
        }
    }

	@Override
	protected void writeStructureToNBT(NBTTagCompound tagCompound)
	{
        tagCompound.setInteger("HPos", this.averageGroundLvl);
        tagCompound.setInteger("Type", this.structureType);
        tagCompound.setInteger("Belong", this.structureBelongness);
        tagCompound.setString("StructType", structType);
	}

	@Override
	protected void readStructureFromNBT(NBTTagCompound tagCompound, TemplateManager p_143011_2_)
	{
        this.averageGroundLvl = tagCompound.getInteger("HPos");
        this.structureType = tagCompound.getInteger("Type");
        this.structureBelongness = tagCompound.getInteger("Belong");
        this.structType = tagCompound.getString("StructType");
	}
	
	protected void initDoors(List<BlockPos> DOORS)
	{
        if (StructNBT.compounds.containsKey(structType))
        {
        	HashMap<BlockPos, Integer> doorDistance = new HashMap();
        	int minDistance = -1;
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
        		
        		if ((states[state].getBlock() instanceof BlockTrapDoor || states[state].getBlock() instanceof BlockDoor || states[state].getBlock() instanceof BlockFenceGate) && states[state].getMaterial() == Material.WOOD || (states[state].getBlock() instanceof BlockQuartz && states[state].getProperties().get(BlockQuartz.VARIANT) == BlockQuartz.EnumType.LINES_Y))
        		{
        			int x = pos.getIntAt(0);
        			int y = pos.getIntAt(1);
        			int z = length - 1 - pos.getIntAt(2);
        			BlockPos doorPos = new BlockPos(x, y, z);
        			int distance = x * x <= (this.width - x) * (this.width - x) ? x * x : (this.width - x) * (this.width - x) + y * y <= (this.height - y) * (this.height - y) ? y * y : (this.height - y) * (this.height - y) + z * z <= (this.length - z) * (this.length - z) ? z * z : (this.length - z) * (this.length - z);
        			doorDistance.put(doorPos, distance);
        			if (minDistance == -1 || distance < minDistance)
        			{
        				minDistance = distance;
        			}
        		}
        	}
        	
        	for (Map.Entry<BlockPos, Integer> set : doorDistance.entrySet())
        	{
        		if (set.getValue() == minDistance)
        		{
        			DOORS.add(set.getKey());
        		}
        	}
        }
	}
	
	public BlockPos findNearestDoor(BlockPos pos)
	{
		BlockPos doorPos = null;
		
		double distance = -1.0;
		
		for (BlockPos door : this.doors)
		{
			double curDistance = door.distanceSq(pos);
			if (doorPos == null || distance > curDistance)
			{
				doorPos = door;
				distance = curDistance;
			}
		}
		
		return doorPos;
	}
	
	protected void transferDoors(List<BlockPos> DOORS)
	{
		this.doors = new ArrayList();
        for (BlockPos pos : DOORS)
        {
			int x = pos.getX();
			int y = pos.getY();
			int z = pos.getZ();
			
	        int X = this.getXWithOffset(x, z);
	        int Y = this.getYWithOffset(y);
	        int Z = this.getZWithOffset(x, z);
			this.doors.add(new BlockPos(X, Y, Z));
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
        		
        		if (structureBoundingBoxIn.isVecInside(new Vec3i(x, y, z)) && states[state].getBlock() == Blocks.STANDING_BANNER || states[state].getBlock() == Blocks.WALL_BANNER)
        		{
        			TileEntity tileentity = worldIn.getTileEntity(blockpos);
                    if (tileentity instanceof TileEntityBanner)
                    {
                        ((TileEntityBanner)tileentity).setItemValues(EntityNPC.getBanner(), true);
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
	
	public void generateEntities(World worldIn, Random randomIn, NBTTagList blocks, IBlockState[] states)
	{
    	if (!this.isAllChunkLoaded())
    	{
    		return;
    	}
    	
    	Mirror mirror = Mirror.NONE;
    	Rotation rotation = Rotation.NONE;
    	Rotation rotation2 = Rotation.NONE;
        switch (this.getCoordBaseMode())
        {
            case SOUTH:
                mirror = Mirror.LEFT_RIGHT;
                rotation = Rotation.NONE;
            	rotation2 = Rotation.CLOCKWISE_180;
                break;

            case WEST:
                mirror = Mirror.LEFT_RIGHT;
                rotation = Rotation.CLOCKWISE_90;
                rotation2 = Rotation.CLOCKWISE_90;
                break;

            case EAST:
                mirror = Mirror.NONE;
                rotation = Rotation.CLOCKWISE_90;
                rotation2 = Rotation.COUNTERCLOCKWISE_90;
                break;

            default:
                mirror = Mirror.NONE;
                rotation = Rotation.NONE;
                break;
        }
    	
		NBTTagCompound compound = StructNBT.compounds.get(this.structType);
        NBTTagList nbttaglist4 = compound.getTagList("entities", 10);
        List<Template.EntityInfo> entities = new ArrayList();

        for (int k = 0; k < nbttaglist4.tagCount(); ++k)
        {
            NBTTagCompound nbttagcompound3 = nbttaglist4.getCompoundTagAt(k);
            NBTTagList nbttaglist5 = nbttagcompound3.getTagList("pos", 6);
            Vec3d vec3d = new Vec3d(nbttaglist5.getDoubleAt(0), nbttaglist5.getDoubleAt(1), nbttaglist5.getDoubleAt(2));
            NBTTagList nbttaglist6 = nbttagcompound3.getTagList("blockPos", 3);
            BlockPos blockpos1 = new BlockPos(nbttaglist6.getIntAt(0), nbttaglist6.getIntAt(1), nbttaglist6.getIntAt(2));

            if (nbttagcompound3.hasKey("nbt"))
            {
                NBTTagCompound nbttagcompound2 = nbttagcompound3.getCompoundTag("nbt");
                entities.add(new Template.EntityInfo(vec3d, blockpos1, nbttagcompound2));
            }
        }

        for (Template.EntityInfo template$entityinfo : entities)
        {
            int x = this.getXWithOffset(template$entityinfo.blockPos.getX(), length - 1 - template$entityinfo.blockPos.getZ());
            int y = this.getYWithOffset(template$entityinfo.blockPos.getY());
            int z = this.getZWithOffset(template$entityinfo.blockPos.getX(), length - 1 - template$entityinfo.blockPos.getZ());

            NBTTagCompound nbttagcompound = template$entityinfo.entityData;
            Vec3d vec3d1 = new Vec3d(x, y ,z);
            NBTTagList nbttaglist = new NBTTagList();
            nbttaglist.appendTag(new NBTTagDouble(vec3d1.xCoord));
            nbttaglist.appendTag(new NBTTagDouble(vec3d1.yCoord));
            nbttaglist.appendTag(new NBTTagDouble(vec3d1.zCoord));
            nbttagcompound.setTag("Pos", nbttaglist);
            nbttagcompound.setUniqueId("UUID", UUID.randomUUID());
            Entity entity;

            try
            {
                entity = EntityList.createEntityFromNBT(nbttagcompound, worldIn);
            }
            catch (Exception var15)
            {
                entity = null;
            }

            if (entity != null)
            {
                if (!(entity instanceof EntityPainting) && !(entity instanceof EntityItemFrame))
                {
	                float f = entity.getMirroredYaw(mirror);
	                f = f + (entity.rotationYaw - entity.getRotatedYaw(rotation2));
	                entity.setLocationAndAngles(vec3d1.xCoord, vec3d1.yCoord, vec3d1.zCoord, f, entity.rotationPitch);
	                worldIn.spawnEntityInWorld(entity);
                }
                else
                {
	                float f = entity.getMirroredYaw(mirror);
	                f = f + (entity.rotationYaw - entity.getRotatedYaw(rotation));
	                entity.setLocationAndAngles(vec3d1.xCoord, vec3d1.yCoord, vec3d1.zCoord, f, entity.rotationPitch);
	                worldIn.spawnEntityInWorld(entity);
                }
            }
        }
	}

    protected Vec3d[][] initLocations(Block blockType, Block[][][] blockTypes)
    {
    	int sizeX = this.boundingBox.maxX - this.boundingBox.minX;
    	int sizeY = this.boundingBox.maxY - this.boundingBox.minY;
    	int sizeZ = this.boundingBox.maxZ - this.boundingBox.minZ;
    	int minX =  this.boundingBox.minX;
    	int minY =  this.boundingBox.minY;
    	int minZ =  this.boundingBox.minZ;
    	DisjointSet set = new DisjointSet(sizeY, sizeZ);
		
    	for (int x = 0; x <= sizeX; x++)
    	{
    		for (int y = 0; y <= sizeY; y++)
    		{
    			for (int z = 0; z <= sizeZ; z++)
    			{
    				if (blockTypes[x][y][z] != blockType)
    				{
    					continue;
    				}
    				
    				int pointIndex = set.calIndex(new Vec3i(x, y, z));
    				set.find(pointIndex);
    				
    				int checkIndex = set.calIndex(new Vec3i(x + 1, y, z));
    				if (x + 1 <= sizeX && blockTypes[x + 1][y][z] == blockType)
    				{
    					set.union(pointIndex, checkIndex);
    				}
    	
    				checkIndex = set.calIndex(new Vec3i(x - 1, y, z));
    				if (x - 1 >= 0 && blockTypes[x - 1][y][z] == blockType)
    				{
    					set.union(pointIndex, checkIndex);
    				}
    	
    				checkIndex = set.calIndex(new Vec3i(x, y + 1, z));
    				if (y + 1 <= sizeY && blockTypes[x][y + 1][z] == blockType)
    				{
    					set.union(pointIndex, checkIndex);
    				}
    	
    				checkIndex = set.calIndex(new Vec3i(x, y - 1, z));
    				if (y - 1 >= 0 && blockTypes[x][y - 1][z] == blockType)
    				{
    					set.union(pointIndex, checkIndex);
    				}
    	
    				checkIndex = set.calIndex(new Vec3i(x, y, z + 1));
    				if (z + 1 <= sizeZ && blockTypes[x][y][z + 1] == blockType)
    				{
    					set.union(pointIndex, checkIndex);
    				}
    	
    				checkIndex = set.calIndex(new Vec3i(x, y, z - 1));
    				if (z - 1 >= 0 && blockTypes[x][y][z - 1] == blockType)
    				{
    					set.union(pointIndex, checkIndex);
    				}
    			}
    		}
    	}
    	
    	HashMap<Integer, Integer> min = new HashMap<>();
    	HashMap<Integer, Integer> max = new HashMap<>();
    	
    	for (Entry<Integer, Integer> node : set.parent.entrySet())
    	{
    		int key = node.getKey();
    		int value = node.getValue();
    		
    		int root = set.find(key);
    		if (min.containsKey(root))
    		{
	    		Vec3i minPoint = set.calPoint(min.get(root));
	    		Vec3i maxPoint = set.calPoint(max.get(root));
	    		Vec3i nodePoint = set.calPoint(key);
	            int mX = Math.min(minPoint.getX(), nodePoint.getX());
	            int mY = Math.min(minPoint.getY(), nodePoint.getY());
	            int mZ = Math.min(minPoint.getZ(), nodePoint.getZ());
	            int maxX = Math.max(maxPoint.getX(), nodePoint.getX());
	            int maxY = Math.max(maxPoint.getY(), nodePoint.getY());
	            int maxZ = Math.max(maxPoint.getZ(), nodePoint.getZ());
	            min.put(root, set.calIndex(new Vec3i(mX, mY, mZ)));
	            max.put(root, set.calIndex(new Vec3i(maxX, maxY, maxZ)));
    		}
    		else
    		{
	    		Vec3i rootPoint = set.calPoint(root);
	    		Vec3i nodePoint = set.calPoint(key);
	            int mX = Math.min(rootPoint.getX(), nodePoint.getX());
	            int mY = Math.min(rootPoint.getY(), nodePoint.getY());
	            int mZ = Math.min(rootPoint.getZ(), nodePoint.getZ());
	            int maxX = Math.max(rootPoint.getX(), nodePoint.getX());
	            int maxY = Math.max(rootPoint.getY(), nodePoint.getY());
	            int maxZ = Math.max(rootPoint.getZ(), nodePoint.getZ());
	            min.put(root, set.calIndex(new Vec3i(mX, mY, mZ)));
	            max.put(root, set.calIndex(new Vec3i(maxX, maxY, maxZ)));
    		}
    	}
    	ArrayList<Vec3d[]> locationList = new ArrayList<>();
    	for (Entry<Integer, Integer> node : min.entrySet())
    	{
			Vec3i minPoint = set.calPoint(min.get(node.getKey()));
			Vec3i maxPoint= set.calPoint(max.get(node.getKey()));
			Vec3d[] location = new Vec3d[2];
			location[0] = new Vec3d(minPoint.getX() + minX, minPoint.getY() + minY, minPoint.getZ() + minZ);
			location[1] = new Vec3d(maxPoint.getX() + minX, maxPoint.getY() + minY, maxPoint.getZ() + minZ);
			locationList.add(location);
    	}
    	
    	Vec3d[][] locations = new Vec3d[locationList.size()][2];
    	for (int i = 0; i < locationList.size(); i++)
    	{
    		locations[i][0] = locationList.get(i)[0];
    		locations[i][1] = locationList.get(i)[1];
    	}
    	
    	return locations;
    }

    protected int getAverageGroundLevel(World worldIn, StructureBoundingBox structurebb)
    {
        int i = 0;
        int j = 0;
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

        for (int k = this.boundingBox.minZ; k <= this.boundingBox.maxZ; ++k)
        {
            for (int l = this.boundingBox.minX; l <= this.boundingBox.maxX; ++l)
            {
                blockpos$mutableblockpos.setPos(l, 17, k);

                if (structurebb.isVecInside(blockpos$mutableblockpos))
                {
                    i += Math.max(worldIn.getTopSolidOrLiquidBlock(blockpos$mutableblockpos).getY(), worldIn.provider.getAverageGroundLevel() - 1);
                    ++j;
                }
            }
        }

        if (j == 0)
        {
            return -1;
        }
        else
        {
            return i / j;
        }
    }

    protected boolean spawnPlayerInLocations(World worldIn, Random randomIn, Entity entity, Vec3d[][] locations)
    {
    	int locationIndex = randomIn.nextInt(locations.length);
    	Vec3d lower = locations[locationIndex][0];
    	Vec3d upper = locations[locationIndex][1];
    	entity.setLocationAndAngles(lower.xCoord + randomIn.nextInt((int)(upper.xCoord - lower.xCoord) + 1),
    			lower.yCoord + randomIn.nextInt((int)(upper.yCoord - lower.yCoord) + 1),
    			lower.zCoord + randomIn.nextInt((int)(upper.zCoord - lower.zCoord) + 1),
    			MathHelper.wrapDegrees(worldIn.rand.nextFloat() * 360.0F), 0.0f);
		return worldIn.spawnEntityInWorld(entity);
    }
    
    protected boolean spawnAnimalInLocations(World worldIn, Random randomIn, EntityLiving entity, Vec3d[][] locations)
    {
    	int locationIndex = randomIn.nextInt(locations.length);
    	Vec3d lower = locations[locationIndex][0];
    	Vec3d upper = locations[locationIndex][1];
        EntityLiving entityliving = (EntityLiving)entity;
        entity.setLocationAndAngles(lower.xCoord + randomIn.nextInt((int)(upper.xCoord - lower.xCoord) + 1) + 0.5,
    			lower.yCoord + randomIn.nextInt((int)(upper.yCoord - lower.yCoord) + 1),
    			lower.zCoord + randomIn.nextInt((int)(upper.zCoord - lower.zCoord) + 1) + 0.5, MathHelper.wrapDegrees(worldIn.rand.nextFloat() * 360.0F), 0.0F);
        entityliving.rotationYawHead = entityliving.rotationYaw;
        entityliving.renderYawOffset = entityliving.rotationYaw;
        entityliving.onInitialSpawn(worldIn.getDifficultyForLocation(new BlockPos(entityliving)), (IEntityLivingData)null);
        return worldIn.spawnEntityInWorld(entityliving);
    }
    
    protected synchronized boolean isAllChunkLoaded()
    {
    	int curX = this.boundingBox.minX - 8 >= 0 ? (this.boundingBox.minX - 8) / 16 * 16 : (this.boundingBox.minX - 8 - 15) / 16 * 16;
    	int curZ = this.boundingBox.minZ - 8 >= 0 ? (this.boundingBox.minZ - 8) / 16 * 16 : (this.boundingBox.minZ - 8 - 15) / 16 * 16;
    	int numX = (this.boundingBox.maxX - 8 >= 0 ? (this.boundingBox.maxX + 15 - 8) / 16 * 16 : (this.boundingBox.maxX - 8) / 16 * 16) - curX;
    	int numZ = (this.boundingBox.maxZ - 8 >= 0 ? (this.boundingBox.maxZ + 15 - 8) / 16 * 16 : (this.boundingBox.maxZ - 8) / 16 * 16) - curZ;
    	numX /= 16;
    	numZ /= 16;
    	if (this.init == numX * numZ)
    	{
    		return true;
    	}
    	
    	return false;
    }

	public String getStructType()
	{
		return structType;
	}

	public void setStructType(String structType)
	{
		this.structType = structType;
	}
}
