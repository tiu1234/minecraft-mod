package net.minecraft.entity.player.ai.logic;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityNPC;
import net.minecraft.entity.player.ai.pathfinding.NPCPathNavigate;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.gen.structure.PandoraTown;
import net.minecraft.world.gen.structure.PandoraTownManager;

public class EntityAIWalkInCity extends EntityAIJob
{
	public Vec3d destination;
	public List<BlockPos> wayPoints;
	
	private int curIndex;

	public EntityAIWalkInCity(EntityNPC npc)
	{
		super(npc);
		wayPoints = new ArrayList();
		curIndex = 0;

		NPCPathNavigate navigator = this.npc.getNavigator();
		navigator.clearPathEntity();
	}

	@Override
	public int execute()
	{
		if (this.wayPoints.size() == 0)
		{
			PandoraTown town = PandoraTownManager.findNearestTown(this.destination);
			this.wayPoints = town.findWayPoints(new BlockPos(this.npc.posX, this.npc.posY, this.npc.posZ), new BlockPos(this.destination));
			this.curIndex = 0;
		}
		NPCPathNavigate navigator = this.npc.getNavigator();
		if (curIndex == wayPoints.size())
		{
			navigator.getPathSearchRangeInstance().setBaseValue(60.0);
			return 1;
		}

		BlockPos curWayPoint = wayPoints.get(curIndex);
		BlockPos pos = new BlockPos(this.npc.posX, this.npc.posY, this.npc.posZ);
		if (curIndex == 0 || curIndex == wayPoints.size() - 1)
		{
			if (navigator.getPath() == null)
			{
				double distance = pos.getDistance(curWayPoint.getX(), curWayPoint.getY(), curWayPoint.getZ());
				if (navigator.getPathSearchRange() < distance)
				{
					navigator.getPathSearchRangeInstance().setBaseValue(distance);
				}
				navigator.tryMoveToXYZ(curWayPoint.getX(), curWayPoint.getY(), curWayPoint.getZ(), this.npc.SPEED);
			}
			else if (navigator.noPath())
			{
				navigator.clearPathEntity();
				curIndex++;
			}
		}
		else
		{
			if (Math.abs(this.npc.posX - curWayPoint.getX()) <= 0.5 && Math.abs(this.npc.posZ - curWayPoint.getZ()) <= 0.5)
			{
				curIndex++;
			}
			else
			{
				this.npc.getMoveHelper().setMoveTo(curWayPoint.getX(), curWayPoint.getY(), curWayPoint.getZ(), this.npc.SPEED);
			}
		}
		
		return 0;
	}
	
	@Override
	public boolean shouldSuspendCritical()
	{
		return true;
	}

	@Override
	public void suspend()
	{
		super.suspend();
		NPCPathNavigate navigator = this.npc.getNavigator();
		navigator.getPathSearchRangeInstance().setBaseValue(60.0);
		this.wayPoints.clear();
		this.curIndex = 0;
	}

	@Override
	public void revoke()
	{
		super.revoke();
	}

	@Override
	public void readFromNBT(NBTTagCompound compound)
	{
		curIndex = compound.getInteger("curIndex");
		destination = readVec3dFromNBT("destination", compound);
	}

	@Override
	public void writeEntityToNBT(NBTTagCompound compound)
	{
		compound.setString("type", "city");
		compound.setInteger("curIndex", curIndex);
		addVec3dToNBT("destination", destination, compound);
	}

}
