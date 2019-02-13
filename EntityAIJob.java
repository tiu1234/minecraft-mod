package net.minecraft.entity.player.ai.logic;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.google.common.collect.Sets;

import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityNPC;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.gen.structure.PandoraTown;
import net.minecraft.world.gen.structure.PandoraTownManager;

public abstract class EntityAIJob
{
    public int runningStatus;
    
    protected final EntityNPC npc;
    
    protected int focus;
	
	public EntityAIJob(EntityNPC npc)
	{
		this.npc = npc;
		focus = 5;
		runningStatus = 1;
	}
    
    public abstract int execute();
    
    public boolean shouldSuspendCritical()
    {
    	return false;
    }
    
    public void suspend()
    {
    	runningStatus = 0;
    }
    
    public void revoke()
    {
    	runningStatus = 1;
    }
    
	public abstract void readFromNBT(NBTTagCompound compound);
	
	public abstract void writeEntityToNBT(NBTTagCompound compound);

	protected List<Vec3d[]> initDestionations(Vec3d[][] locations)
	{
		List<Vec3d[]> destinations = new ArrayList<>();
		for (Vec3d[] location : locations)
		{
			destinations.add(location);
		}
		
		return destinations;
	}
	
	protected DestinationBundle findRandomDestinationBundle(List<Vec3d[]> destinations)
	{
		if (destinations == null || destinations.size() == 0)
		{
			return null;
		}
		
		Random random = npc.getRNG();
		int index = random.nextInt(destinations.size());
		DestinationBundle bundle = new DestinationBundle();
		bundle.destination = findNearestPoint(destinations.get(index));
		bundle.location = destinations.get(index).clone();
		destinations.remove(index);
		return bundle;
	}
	
	protected Vec3d findRandomDestination(List<Vec3d> destinations)
	{
		if (destinations == null || destinations.size() == 0)
		{
			return null;
		}
		
		Random random = npc.getRNG();
		int index = random.nextInt(destinations.size());
		Vec3d destination = destinations.get(index);
		destinations.remove(index);
		return destination;
	}
	
	protected <T> boolean checkBlockValid(Vec3d location, Block block, PropertyInteger key,  T value)
	{
		BlockPos pos = new BlockPos(location.xCoord, location.yCoord, location.zCoord);
		IBlockState blockState = npc.getEntityWorld().getBlockState(pos);
		if (blockState.getBlock() == block && (key == null || (T)blockState.getProperties().get(key) == value))
		{
			return true;
		}
		return false;
	}

	protected <T> List<Vec3d> findBlocksInsideLocations(Vec3d[][] locations, Block block, PropertyInteger key,  T value)
	{
		List<Vec3d> result = new ArrayList<>();
		
		for (Vec3d[] location : locations)
		{
			result.addAll(findBlocksInsideLocation(location, block, key, value));
		}
		
		return result;
	}
	
	protected <T> List<Vec3d> findBlocksInsideLocation(Vec3d[] location, Block block, PropertyInteger key,  T value)
	{
		List<Vec3d> result = new ArrayList<>();
		
		int lowerX = (int) (location[0].xCoord);
		int lowerY = (int) (location[0].yCoord);
		int lowerZ = (int) (location[0].zCoord);
		int upperX = (int) (location[1].xCoord);
		int upperY = (int) (location[1].yCoord);
		int upperZ = (int) (location[1].zCoord);
		for (int i = lowerX; i <= upperX; i++)
		{
			for (int j = lowerY; j <= upperY; j++)
			{
				for (int k = lowerZ; k <= upperZ; k++)
				{
					BlockPos pos = new BlockPos(i, j, k);
					IBlockState blockState = npc.getEntityWorld().getBlockState(pos);
					if (blockState.getBlock() == block && (key == null || (T)blockState.getProperties().get(key) == value))
					{
						result.add(new Vec3d(i, j, k));
					}
				}
			}
		}
		
		return result;
	}
	
	protected void checkMoveTo(double x, double y, double z)
	{
		if (this.npc.getDistance(x, y, z) <= this.npc.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).getAttributeValue())
		{
			this.npc.getNavigator().tryMoveToXYZ(x, y, z, this.npc.SPEED);
		}
		else
		{
			EntityAIWalkInCity job = this.npc.mind.addCityJob();
			job.destination = new Vec3d(x, y ,z);
		}
	}
	
	private int calculateIndex(BlockPos point, Vec3d upper)
	{
    	return point.getX() * MathHelper.floor(upper.yCoord) * MathHelper.floor(upper.zCoord) + point.getY() * MathHelper.floor(upper.zCoord) + point.getZ();
	}
	
	private boolean checkPointPassable(BlockPos next, Set<Integer> visited, Vec3d upper, ArrayList<BlockPos> newQueue)
	{
		if (this.npc.world.getBlockState(next).getBlock().isPassable(this.npc.world, next))
		{
			return true;
		}
		else
		{
			int index = calculateIndex(next, upper);
			if (!visited.contains(index))
			{
				visited.add(index);
				newQueue.add(next);
			}
			return false;
		}
	}
	
	private Vec3d findNearestPoint(Vec3d[] location)
	{
		double resultX = npc.posX;
		double resultY = npc.posY;
		double resultZ = npc.posZ;
		Vec3d lower = location[0];
		Vec3d upper = location[1];
		if (lower.xCoord > resultX)
		{
			resultX = lower.xCoord;
		}
		if (upper.xCoord < resultX)
		{
			resultX = upper.xCoord;
		}
		if (lower.yCoord > resultY)
		{
			resultY = lower.yCoord;
		}
		if (upper.yCoord < resultY)
		{
			resultY = upper.yCoord;
		}
		if (lower.zCoord > resultZ)
		{
			resultZ = lower.zCoord;
		}
		if (upper.zCoord < resultZ)
		{
			resultZ = upper.zCoord;
		}
		
		BlockPos pos = new BlockPos(resultX, resultY, resultZ);
		if (this.npc.world.getBlockState(pos).getBlock().isPassable(this.npc.world, pos))
		{
			Vec3d result = new Vec3d(resultX, resultY, resultZ);
			return result;
		}
		else
		{
			Vec3d result = null;
			
			ArrayList<BlockPos> queue = new ArrayList<>();
			Set<Integer> visited = Sets.<Integer>newHashSet();
			
			queue.add(pos);
			
			while (queue.size() != 0)
			{
				BlockPos current = queue.get(0);
				visited.add(calculateIndex(current, upper));
				
				ArrayList<BlockPos> newQueue = new ArrayList<>();
				
				if (current.getX() - 1 >= lower.xCoord)
				{
					BlockPos next = new BlockPos(current.getX() - 1, current.getY(), current.getZ());
					if (checkPointPassable(next, visited, upper, newQueue))
					{
						result = new Vec3d(next.getX(), next.getY(), next.getZ());
						break;
					}
				}

				if (current.getX() + 1 <= upper.xCoord)
				{
					BlockPos next = new BlockPos(current.getX() + 1, current.getY(), current.getZ());
					if (checkPointPassable(next, visited, upper, newQueue))
					{
						result = new Vec3d(next.getX(), next.getY(), next.getZ());
						break;
					}
				}

				if (current.getY() - 1 >= lower.yCoord)
				{
					BlockPos next = new BlockPos(current.getX(), current.getY() - 1, current.getZ());
					if (checkPointPassable(next, visited, upper, newQueue))
					{
						result = new Vec3d(next.getX(), next.getY(), next.getZ());
						break;
					}
				}

				if (current.getY() + 1 <= upper.yCoord)
				{
					BlockPos next = new BlockPos(current.getX(), current.getY() + 1, current.getZ());
					if (checkPointPassable(next, visited, upper, newQueue))
					{
						result = new Vec3d(next.getX(), next.getY(), next.getZ());
						break;
					}
				}

				if (current.getZ() - 1 >= lower.zCoord)
				{
					BlockPos next = new BlockPos(current.getX(), current.getY(), current.getZ() - 1);
					if (checkPointPassable(next, visited, upper, newQueue))
					{
						result = new Vec3d(next.getX(), next.getY(), next.getZ());
						break;
					}
				}

				if (current.getZ() + 1 <= upper.zCoord)
				{
					BlockPos next = new BlockPos(current.getX(), current.getY(), current.getZ() + 1);
					if (checkPointPassable(next, visited, upper, newQueue))
					{
						result = new Vec3d(next.getX(), next.getY(), next.getZ());
						break;
					}
				}
				
				queue = newQueue;
			}
			
			
			return result;
		}
	}
	
	protected class DestinationBundle
	{
		public Vec3d destination;
		public Vec3d[] location;
	}

	public static void add2dToNBT(String name, Vec3d[][] locations, NBTTagCompound compound)
	{
		if (locations != null && locations.length > 0)
		{
			NBTTagList list = new NBTTagList();
			
			for (Vec3d[] location : locations)
			{
				list.appendTag(new NBTTagDouble(location[0].xCoord));
				list.appendTag(new NBTTagDouble(location[0].yCoord));
				list.appendTag(new NBTTagDouble(location[0].zCoord));
				list.appendTag(new NBTTagDouble(location[1].xCoord));
				list.appendTag(new NBTTagDouble(location[1].yCoord));
				list.appendTag(new NBTTagDouble(location[1].zCoord));
			}
			compound.setTag(name, list);
		}
	}
	
	public static Vec3d[][] read2dFromNBT(String name, NBTTagCompound compound)
	{
		if (!compound.hasKey(name))
		{
			return null;
		}
		
		Vec3d[][] locations;
		
		NBTTagList list = compound.getTagList(name, 6);
		
		locations = new Vec3d[list.tagCount() / 6][2];
		
		for (int i = 0; i < list.tagCount(); i += 6)
		{
			locations[i / 6][0] = new Vec3d(list.getDoubleAt(i), list.getDoubleAt(i + 1), list.getDoubleAt(i + 2));
			locations[i / 6][1] = new Vec3d(list.getDoubleAt(i + 3), list.getDoubleAt(i + 4), list.getDoubleAt(i + 5));
		}
		
		return locations;
	}
	
	public static void add1dToNBT(String name, Vec3d[] locations, NBTTagCompound compound)
	{
		if (locations != null && locations.length > 0)
		{
			NBTTagList list = new NBTTagList();
			
			for (Vec3d location : locations)
			{
				list.appendTag(new NBTTagDouble(location.xCoord));
				list.appendTag(new NBTTagDouble(location.yCoord));
				list.appendTag(new NBTTagDouble(location.zCoord));
			}
			compound.setTag(name, list);
		}
	}
	
	public static Vec3d[] read1dFromNBT(String name, NBTTagCompound compound)
	{
		if (!compound.hasKey(name))
		{
			return null;
		}
		
		Vec3d[] locations;
		
		NBTTagList list = compound.getTagList(name, 6);
		
		locations = new Vec3d[list.tagCount() / 3];
		
		for (int i = 0; i < list.tagCount(); i += 3)
		{
			locations[i / 3] = new Vec3d(list.getDoubleAt(i), list.getDoubleAt(i + 1), list.getDoubleAt(i + 2));
		}
		
		return locations;
	}
	
	public static void add2dArrayToNBT(String name, List<Vec3d[]> locations, NBTTagCompound compound)
	{
		if (locations != null && locations.size() > 0)
		{
			NBTTagList list = new NBTTagList();
			
			for (Vec3d[] location : locations)
			{
				list.appendTag(new NBTTagDouble(location[0].xCoord));
				list.appendTag(new NBTTagDouble(location[0].yCoord));
				list.appendTag(new NBTTagDouble(location[0].zCoord));
				list.appendTag(new NBTTagDouble(location[1].xCoord));
				list.appendTag(new NBTTagDouble(location[1].yCoord));
				list.appendTag(new NBTTagDouble(location[1].zCoord));
			}
			compound.setTag(name, list);
		}
	}
	
	public static List<Vec3d[]> read2dArrayFromNBT(String name, NBTTagCompound compound)
	{
		if (!compound.hasKey(name))
		{
			return null;
		}
		
		List<Vec3d[]> locations;
		
		NBTTagList list = compound.getTagList(name, 6);
		
		locations = new ArrayList<>();
		
		for (int i = 0; i < list.tagCount(); i += 6)
		{
			Vec3d[] tmpLocation = new Vec3d[2];
			tmpLocation[0] = new Vec3d(list.getDoubleAt(i), list.getDoubleAt(i + 1), list.getDoubleAt(i + 2));
			tmpLocation[1] = new Vec3d(list.getDoubleAt(i + 3), list.getDoubleAt(i + 4), list.getDoubleAt(i + 5));
			locations.add(tmpLocation);
		}
		
		return locations;
	}

	public static void add1dArrayToNBT(String name, List<Vec3d> locations, NBTTagCompound compound)
	{
		if (locations != null && locations.size() > 0)
		{
			NBTTagList list = new NBTTagList();
			
			for (Vec3d location : locations)
			{
				list.appendTag(new NBTTagDouble(location.xCoord));
				list.appendTag(new NBTTagDouble(location.yCoord));
				list.appendTag(new NBTTagDouble(location.zCoord));
			}
			compound.setTag(name, list);
		}
	}
	
	public static List<Vec3d> read1dArrayFromNBT(String name, NBTTagCompound compound)
	{
		if (!compound.hasKey(name))
		{
			return null;
		}
		
		List<Vec3d> locations;
		
		NBTTagList list = compound.getTagList(name, 6);
		
		locations = new ArrayList<>();
		
		for (int i = 0; i < list.tagCount(); i += 3)
		{
			Vec3d tmpLocation = new Vec3d(list.getDoubleAt(i), list.getDoubleAt(i + 1), list.getDoubleAt(i + 2));
			locations.add(tmpLocation);
		}
		
		return locations;
	}
	
	public static void addVec3dToNBT(String name, Vec3d location, NBTTagCompound compound)
	{
		if (location != null)
		{
			NBTTagList list = new NBTTagList();
			
			list.appendTag(new NBTTagDouble(location.xCoord));
			list.appendTag(new NBTTagDouble(location.yCoord));
			list.appendTag(new NBTTagDouble(location.zCoord));
			compound.setTag(name, list);
		}
	}
	
	public static Vec3d readVec3dFromNBT(String name, NBTTagCompound compound)
	{
		if (!compound.hasKey(name))
		{
			return null;
		}
		
		NBTTagList list = compound.getTagList(name, 6);
		
		return new Vec3d(list.getDoubleAt(0), list.getDoubleAt(1), list.getDoubleAt(2));
	}
	
//	public static List<BlockPos> readBlockPosListFromNBT(String name, NBTTagCompound compound)
//	{
//		if (!compound.hasKey(name))
//		{
//			return null;
//		}
//		
//		List<BlockPos> locations;
//		
//		NBTTagList list = compound.getTagList(name, 6);
//		
//		locations = new ArrayList<>();
//		
//		for (int i = 0; i < list.tagCount(); i += 3)
//		{
//			BlockPos tmpLocation = new BlockPos(list.getIntAt(i), list.getIntAt(i + 1), list.getIntAt(i + 2));
//			locations.add(tmpLocation);
//		}
//		
//		return locations;
//	}
//
//	public static void addBlockPosListToNBT(String name, List<BlockPos> locations, NBTTagCompound compound)
//	{
//		if (locations != null && locations.size() > 0)
//		{
//			NBTTagList list = new NBTTagList();
//			
//			for (BlockPos location : locations)
//			{
//				list.appendTag(new NBTTagInt(location.getX()));
//				list.appendTag(new NBTTagInt(location.getY()));
//				list.appendTag(new NBTTagInt(location.getZ()));
//			}
//			compound.setTag(name, list);
//		}
//	}
	
	public int getFocus()
	{
		return focus;
	}
}
