package net.minecraft.world.gen.structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class PandoraTownManager
{
	public static List<PandoraTown> towns;
	
	public static void init(long seed)
	{
		towns = new ArrayList<>();
		towns.add(new PandoraTown(0, 0, seed));
		towns.get(0).generateStructures();
	}
	
	public static void populate(World world)
	{
		for (PandoraTown town: towns)
		{
			town.populate(world);
		}
	}
	
	public static PandoraTown findNearestTown(Vec3d pos)
	{
		double minDistance = -1.0;
		PandoraTown targetTown = null;
		for (PandoraTown town: towns)
		{
			double distance = pos.distanceTo(new Vec3d(town.startX, town.STREETY, town.startZ));
			if (targetTown == null || minDistance > distance)
			{
				distance = minDistance;
				targetTown = town;
			}
		}
		
		return targetTown;
	}
	
	public static void clear()
	{
		for (PandoraTown town: towns)
		{
			town.clear();
		}
		towns.clear();
		towns = null;
	}
}
