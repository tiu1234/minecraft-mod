package net.minecraft.entity.player.ai.logic;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.entity.player.EntityNPC;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

public class EntityNPCMind
{
	public float receivedDamage;
	
    private final EntityNPC npc;
    
	private final List<EntityAIJob> taskQueue;
	
	private EntityAIMemory memory;
	
	public EntityNPCMind(EntityNPC npc, World worldIn)
	{
		this.npc = npc;
		memory = new EntityAIMemory();
		taskQueue = new ArrayList();
		
		receivedDamage = 0.0f;
	}
	
	public void think()
	{
		//TODO check unusual blocks
		//TODO check escaped cows
		
		if (npc.isInWater() || npc.isInLava())
		{
			npc.getJumpHelper().setJumping();
		}

		if (this.npc.getAITarget() != null)
		{
			if (this.receivedDamage != 0.0f)
			{
				this.processBeingAssaulted();
				this.receivedDamage = 0.0f;
			}
		}
		
		int checkSurround = this.npc.getRNG().nextInt(10);
		if (checkSurround >= taskQueue.get(taskQueue.size() - 1).getFocus())
		{
			int checkFlag = this.npc.getRNG().nextInt(5);
			switch(checkFlag)
			{
			case 0:
			case 1:
				this.checkSurroundPlayers(checkFlag + 2);
				break;
			case 2:
				//TODO check surround items
				break;
			default:
				break;
			}
		}
		
		if (this.npc.getAITarget() != null)
		{
			if (this.taskQueue.get(this.taskQueue.size() - 1).getFocus() < 10)
			{
				this.npc.faceEntity(this.npc.getAITarget(), 30.0F, 30.0F);
				this.npc.getNavigator().clearPathEntity();
				return;
			}
		}
		
		if (this.npc.onGround == false)
		{
			return;
		}
		
		if (this.npc.getLeashed())
		{
			//TODO break the leash
			if (this.taskQueue.size() > 0)
			{
				EntityAIJob lastJob = this.taskQueue.get(this.taskQueue.size() - 1);
				if (lastJob.shouldSuspendCritical() && lastJob.runningStatus != 0)
				{
					lastJob.suspend();
				}
			}
			return;
		}
		
		if (taskQueue.size() > 0 && taskQueue.get(taskQueue.size() - 1).execute() == 1)
		{
			taskQueue.remove(taskQueue.size() - 1);
			taskQueue.get(taskQueue.size() - 1).revoke();
		}
	}

	public void readFromNBT(NBTTagCompound compound)
	{
		memory.readFromNBT(compound);
		NBTTagList jobs = compound.getTagList("jobs", 10);
		
		for (int i = 0; i < jobs.tagCount(); i++)
		{
			NBTTagCompound jobCompund = jobs.getCompoundTagAt(i);
			EntityAIJob job = analyzeJob(jobCompund.getString("type"));
			job.readFromNBT(jobCompund);
		}
	}
	
	public void writeEntityToNBT(NBTTagCompound compound)
	{
		memory.writeEntityToNBT(compound);
		NBTTagList jobs = new NBTTagList();
		for (EntityAIJob job : taskQueue)
		{
			NBTTagCompound jobCompund = new NBTTagCompound();
			job.writeEntityToNBT(jobCompund);
			jobs.appendTag(jobCompund);
		}
		compound.setTag("jobs", jobs);
	}
	
	private void processBeingAssaulted()
	{
		
		String name = this.npc.getAITarget().getName();
		
		if (PlayerNameMap.existPlayer(name))
		{
			if (this.receivedDamage <= 2.0f)
			{
				this.memory.modifyRelation(name, -5);
			}
			else
			{
				this.memory.modifyRelation(name, -10);
			}
			
			if (this.memory.getRelationships().get(name) > -10)
			{
				this.speakTo((EntityPlayer) this.npc.getAITarget(), 0, "Stop the nonsense!");
			}
		}
	}
	
	private void checkPlayer(EntityPlayer entity)
	{
		if (PlayerNameMap.existPlayer(entity.getName()))
		{
			if (this.memory.getRelationships().containsKey(entity.getName()))
			{
				int relationship = this.memory.getRelationships().get(entity.getName());
				if (relationship <= -10 && this.npc.isFacing(entity.getPositionVector()))
				{
					this.npc.setAttackTarget(entity);
					if (EntityAICombat.checkAttackTarget(this.npc))
					{
						this.addCombatJob();
						this.speakTo(entity, 1, "Yield or die!");
					}
					else
					{
						this.npc.setAttackTarget(null);
					}
					return;
				}
			}
		}
	}
	
	private void checkSurroundPlayers(int offset)
	{
        AxisAlignedBB axisalignedbb = this.npc.getEntityBoundingBox().expand(10.0D * offset, 0.5D + 6.0D * (offset - 1), 10.0D * offset);
        
        List<EntityPlayer> list = this.npc.world.getEntitiesWithinAABB(EntityPlayer.class, axisalignedbb);

        for (int i = 0; i < list.size(); ++i)
        {
        	EntityPlayer entity = list.get(i);
        	if (entity != this.npc && entity.isEntityAlive() && this.npc.canEntityBeSeen(entity))
        	{
            	checkPlayer(entity);
        	}
        }
	}
	
	private EntityAIJob analyzeJob(String jobType)
	{
		EntityAIJob job;
		switch(jobType)
		{
		case "farm":
			job = addFarmJob();
			break;
		case "guard":
			job = addGuardJob();
			break;
		case "combat":
			job = addCombatJob();
			break;
		case "city":
			job = addCityJob();
			break;
		default:
			job = null;
			break;
		}
		
		return job;
	}
	
	private void suspendLastTask()
	{
		if (taskQueue.size() > 0)
		{
			taskQueue.get(taskQueue.size() - 1).suspend();
		}
	}
	
	public EntityAIFarm addFarmJob()
	{
		suspendLastTask();
		EntityAIFarm farmJob = new EntityAIFarm(npc);
		taskQueue.add(farmJob);
		return (EntityAIFarm) taskQueue.get(taskQueue.size() - 1);
	}
	
	public EntityAIGuard addGuardJob()
	{
		suspendLastTask();
		EntityAIGuard farmJob = new EntityAIGuard(npc);
		taskQueue.add(farmJob);
		return (EntityAIGuard) taskQueue.get(taskQueue.size() - 1);
	}
	
	public EntityAICombat addCombatJob()
	{
		suspendLastTask();
		EntityAICombat combatJob = new EntityAICombat(npc);
		taskQueue.add(combatJob);
		return (EntityAICombat) taskQueue.get(taskQueue.size() - 1);
	}
	
	public EntityAIWalkInCity addCityJob()
	{
		suspendLastTask();
		EntityAIWalkInCity cityJob = new EntityAIWalkInCity(npc);
		taskQueue.add(cityJob);
		return (EntityAIWalkInCity) taskQueue.get(taskQueue.size() - 1);
	}

	public EntityAIMemory getMemory()
	{
		return memory;
	}

	public void setMemory(EntityAIMemory memory)
	{
		this.memory = memory;
	}

	public void speakTo(EntityPlayer entity, int type, String message)
	{
		if (!(entity instanceof EntityNPC))
		{
			TextFormatting format;
			switch(type)
			{
			case 0:
				format = TextFormatting.WHITE;
				break;
			case 1:
				format = TextFormatting.RED;
				break;
			default:
				format = TextFormatting.WHITE;
			break;
			}
			this.speakTo(entity, format, message);
		}
		else
		{
			//TODO send msg to npc
		}
	}
	
	private void speakTo(EntityPlayer entityplayer, TextFormatting format, String message)
	{
		String[] arg = {entityplayer.getName(), message};
        ITextComponent itextcomponent = null;
		try
		{
			itextcomponent = CommandBase.getChatComponentFromNthArg(this.npc, arg, 1, !(this.npc instanceof EntityPlayer));
		}
		catch (CommandException e)
		{
			e.printStackTrace();
			return;
		}
        TextComponentTranslation textcomponenttranslation = new TextComponentTranslation("commands.message.display.incoming", new Object[] {this.npc.getDisplayName(), itextcomponent.createCopy()});
        textcomponenttranslation.getStyle().setColor(format).setItalic(Boolean.valueOf(true)).setBold(Boolean.valueOf(true));
        entityplayer.addChatMessage(textcomponenttranslation);
	}
}
