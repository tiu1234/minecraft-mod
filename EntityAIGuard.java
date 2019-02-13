package net.minecraft.entity.player.ai.logic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.player.EntityNPC;
import net.minecraft.entity.player.ai.logic.EntityAIJob.DestinationBundle;
import net.minecraft.entity.player.ai.pathfinding.NPCPathNavigate;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class EntityAIGuard extends EntityAIJob
{
	private static enum States
	{
		PATROL(0),
		STAND(1);
		
		private static Map<Integer, States> stateMap = new HashMap<>();
		
		private int value;
		
		private States(int value)
		{
			this.value = value;
		}
		
		static
		{
			for (States state : States.values())
			{
				stateMap.put(state.value, state);
			}
		}
		
		public static States getState(int value)
		{
			return stateMap.get(value);
		}
	}

	private Vec3d[][] guardLocations;
	private List<Vec3d[]> guardDestinations;
	private Vec3d guardDestination;
	private Vec3d faceDirection;
	
	private int standTurn;
	private int standTick;
	
	private States state;

	public EntityAIGuard(EntityNPC npc)
	{
		super(npc);
		state = States.PATROL;
		standTurn = -1;
		standTick = -1;
	}

	@Override
	public int execute()
	{
		switch(state)
		{
		case PATROL:
			patrol();
			break;
		
		case STAND:
			stand();
			break;
		
		default:
			break;
		}
		return 0;
	}
	
	private void patrol()
	{
		if (this.npc.getName().startsWith("Charmid"))
		{
			int aaa = 1;
		}
		if (guardDestinations == null)
		{
			guardDestinations = initDestionations(guardLocations);
		}

		if (guardDestinations.size() == 0 && guardDestination == null)
		{
			guardDestinations = null;
			state = States.STAND;
			return;
		}
		

		NPCPathNavigate navigator = npc.getNavigator();
		if (guardDestination == null)
		{
			DestinationBundle result = findRandomDestinationBundle(guardDestinations);
			guardDestination = result.destination;
			if (guardDestination == null)
			{
				return;
			}
			navigator.clearPathEntity();
			this.checkMoveTo(guardDestination.xCoord, guardDestination.yCoord, guardDestination.zCoord);
			//navigator.tryMoveToXYZ(guardDestination.xCoord, guardDestination.yCoord, guardDestination.zCoord, this.npc.SPEED);
		}
		else
		{
			if (navigator.getPath() == null)
			{
				this.checkMoveTo(guardDestination.xCoord, guardDestination.yCoord, guardDestination.zCoord);
				//navigator.tryMoveToXYZ(guardDestination.xCoord, guardDestination.yCoord, guardDestination.zCoord, this.npc.SPEED);
				if (navigator.getPath() == null)
				{
					guardDestination = null;
				}
			}
			else if (navigator.noPath())
			{
				state = States.STAND;
			}
		}
		
		return;
	}
	
	private void switchToPatrol()
	{
		state = States.PATROL;
		guardDestination = null;
		standTurn = -1;
		standTick = -1;
		faceDirection = null;
	}
	
	private void stand()
	{
		if (guardDestination == null)
		{
			switchToPatrol();
			return;
		}
		
		BlockPos pos1= new BlockPos(this.npc.posX, this.npc.posY, this.npc.posZ);
		BlockPos pos2= new BlockPos(guardDestination);
		if (!pos1.equals(pos2))
		{
			switchToPatrol();
			return;
		}
		
		if (standTurn == 0)
		{
			switchToPatrol();
		}
		else
		{
			if (standTurn == -1)
			{
				standTurn = this.npc.getRNG().nextInt(3) + 2;
			}
			
			if (faceDirection == null)
			{
				getNewFaceDirection();
			}
			
			if (this.npc.isFacing(faceDirection))
			{
				if (standTick == -1)
				{
					standTick = this.npc.getRNG().nextInt(100) + 20;
				}
				
				standTick--;
				
				if (standTick == 0)
				{
					getNewFaceDirection();
					standTick = -1;
					standTurn--;
				}
			}
			else
			{
				this.npc.faceVec3d(faceDirection, 30.0f, 30.0f);
			}
		}
	}
	
	private void getNewFaceDirection()
	{
		switch(this.npc.getRNG().nextInt(8))
		{
		case 0:
			faceDirection = new Vec3d(this.npc.posX - 1, this.npc.posY, this.npc.posZ);
			break;
		case 1:
			faceDirection = new Vec3d(this.npc.posX, this.npc.posY, this.npc.posZ - 1);
			break;
		case 2:
			faceDirection = new Vec3d(this.npc.posX - 1, this.npc.posY, this.npc.posZ - 1);
			break;
		case 3:
			faceDirection = new Vec3d(this.npc.posX + 1, this.npc.posY, this.npc.posZ);
			break;
		case 4:
			faceDirection = new Vec3d(this.npc.posX, this.npc.posY, this.npc.posZ + 1);
			break;
		case 5:
			faceDirection = new Vec3d(this.npc.posX + 1, this.npc.posY, this.npc.posZ + 1);
			break;
		case 6:
			faceDirection = new Vec3d(this.npc.posX - 1, this.npc.posY, this.npc.posZ + 1);
			break;
		case 7:
		default:
			faceDirection = new Vec3d(this.npc.posX + 1, this.npc.posY, this.npc.posZ - 1);
			break;
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound compound)
	{
		state = States.stateMap.get(compound.getInteger("state"));
		standTurn = compound.getInteger("standTurn");
		standTick = compound.getInteger("standTick");
		guardLocations = read2dFromNBT("guardLocations", compound);
		guardDestinations = read2dArrayFromNBT("guardDestinations", compound);
		guardDestination = readVec3dFromNBT("guardDestination", compound);
		faceDirection = readVec3dFromNBT("faceDirection", compound);
	}

	@Override
	public void writeEntityToNBT(NBTTagCompound compound)
	{
		compound.setString("type", "guard");
		compound.setInteger("state", state.value);
		compound.setInteger("standTurn", standTurn);
		compound.setInteger("standTick", standTick);
		add2dToNBT("guardLocations", guardLocations, compound);
		add2dArrayToNBT("guardDestinations", guardDestinations, compound);
		addVec3dToNBT("guardDestination", guardDestination, compound);
		addVec3dToNBT("faceDirection", faceDirection, compound);
	}

	public Vec3d[][] getGuardLocations()
	{
		return guardLocations;
	}

	public void setGuardLocations(Vec3d[][] guardLocations)
	{
		this.guardLocations = guardLocations;
	}
}
