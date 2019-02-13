package net.minecraft.entity.player.ai.logic;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityNPC;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.ai.pathfinding.NPCPathNavigate;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;

public class EntityAICombat extends EntityAIJob
{
	private static enum States
	{
		ATTACK(0),
		ABDUCT(1),
		FINISH(2);
		
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
	
	private States state;
	
    /**
     * An amount of decrementing ticks that allows the entity to attack once the tick reaches 0.
     */
    protected int attackTick;

    private int delayCounter;
    private double targetX;
    private double targetY;
    private double targetZ;
    private ItemStack switchedItemStack;
    protected final int attackInterval = 10;

	public EntityAICombat(EntityNPC npc)
	{
		super(npc);
		state = States.ATTACK;
        this.delayCounter = 0;
        this.focus = 10;
        switchedItemStack = null;
	}

	@Override
	public int execute()
	{
		int finishFlag = 0;
		switch (this.state)
		{
		case ATTACK:
			if (!this.checkAttackTarget(this.npc))
			{
				state = States.FINISH;
				break;
			}
	        EntityLivingBase entitylivingbase = this.npc.getAttackTarget();
	        if (entitylivingbase instanceof EntityPlayer && entitylivingbase.isSneaking() && this.npc.hasItem(Items.LEAD))
	        {
	        	if (!this.npc.getHeldItemMainhand().func_190926_b())
	        	{
	        		switchedItemStack = this.npc.getHeldItemMainhand().copy();
	        	}
	        	else
	        	{
	        		switchedItemStack = new ItemStack(Items.field_190931_a, 1);
	        	}
	        	state = States.ABDUCT;
	        	break;
	        }
	        this.npc.getLookHelper().setLookPositionWithEntity(entitylivingbase, 30.0F, 30.0F);
	        double d0 = this.npc.getDistanceSq(entitylivingbase.posX, entitylivingbase.getEntityBoundingBox().minY, entitylivingbase.posZ);
	        --this.delayCounter;

	        if ((this.npc.getEntitySenses().canSee(entitylivingbase)) && this.delayCounter <= 0 && (this.targetX == 0.0D && this.targetY == 0.0D && this.targetZ == 0.0D || entitylivingbase.getDistanceSq(this.targetX, this.targetY, this.targetZ) >= 1.0D || this.npc.getRNG().nextFloat() < 0.05F))
	        {
	            this.targetX = entitylivingbase.posX;
	            this.targetY = entitylivingbase.getEntityBoundingBox().minY;
	            this.targetZ = entitylivingbase.posZ;
	            this.delayCounter = 4 + this.npc.getRNG().nextInt(7);

	            if (d0 > 1024.0D)
	            {
	                this.delayCounter += 10;
	            }
	            else if (d0 > 256.0D)
	            {
	                this.delayCounter += 5;
	            }

	            boolean flag;
            	if (this.npc.shieldUp == 0)
            	{
            		flag = this.npc.getNavigator().tryMoveToEntityLiving(entitylivingbase, this.npc.SPEED);
            	}
            	else
            	{
            		flag = this.npc.getNavigator().tryMoveToEntityLiving(entitylivingbase, this.npc.SPEED / 4);
            	}
	            if (!flag)
	            {
	                this.delayCounter += 15;
	            }
	        }

	        this.attackTick = Math.max(this.attackTick - 1, 0);
	        this.checkAndPerformAttack(entitylivingbase, d0);
	        this.checkAndPerformDefend(entitylivingbase, d0);
			break;
		case ABDUCT:
        	this.npc.stopActiveHand();
        	this.npc.shieldUp = 0;
        	this.npc.getNavigator().setSpeed(this.npc.SPEED);
	        entitylivingbase = this.npc.getAttackTarget();
	        if (!entitylivingbase.isSneaking() || !(entitylivingbase instanceof EntityPlayer) || !this.npc.switchMainHandToItem(Items.LEAD))
	        {
	        	if (switchedItemStack != null)
	        	{
		    		this.npc.switchMainHandToItem(switchedItemStack.getItem());
	        	}
	    		switchedItemStack = null;
	        	state = States.ATTACK;
	        	break;
	        }
	        d0 = this.npc.getDistanceSq(entitylivingbase.posX, entitylivingbase.getEntityBoundingBox().minY, entitylivingbase.posZ);
	        this.npc.getNavigator().tryMoveToEntityLiving(entitylivingbase, this.npc.SPEED);
	        this.checkAndPerformAbduct(entitylivingbase, d0);
			break;
		case FINISH:
			if (switchedItemStack != null)
			{
	    		this.npc.switchMainHandToItem(switchedItemStack.getItem());
			}
        	this.npc.stopActiveHand();
        	this.npc.shieldUp = 0;
        	this.npc.getNavigator().setSpeed(this.npc.SPEED);
        	this.npc.setAttackTarget(null);
        	finishFlag = 1;
        	break;
		default:
			break;
		}
		return finishFlag;
	}
	

    protected void checkAndPerformAbduct(EntityLivingBase p_190102_1_, double p_190102_2_)
    {
        double d0 = this.getAttackReachSqr(this.npc, p_190102_1_);

        if (((EntityPlayer)p_190102_1_).getLeashed())
        {
        	state = States.FINISH;
        	return;
        }
        if (p_190102_2_ <= d0)
        {
        	if (((EntityPlayer)p_190102_1_).processInitialInteract(this.npc, EnumHand.MAIN_HAND))
        	{
            	state = States.FINISH;
				this.npc.mind.speakTo((EntityPlayer)p_190102_1_, 0, "You are under arrest.");
        	}
        	else
        	{
	        	if (switchedItemStack != null)
	        	{
	        		this.npc.switchMainHandToItem(switchedItemStack.getItem());
	        	}
	    		switchedItemStack = null;
        		state = States.ATTACK;
        	}
        	return;
        }
    }
	
    protected void checkAndPerformAttack(EntityLivingBase p_190102_1_, double p_190102_2_)
    {
        double d0 = this.getAttackReachSqr(this.npc, p_190102_1_);

        if (p_190102_2_ <= d0 && this.attackTick <= 0)
        {
        	this.npc.stopActiveHand();
        	this.npc.shieldUp = 0;
            this.attackTick = attackInterval;
            this.npc.swingArm(EnumHand.MAIN_HAND);
            this.npc.attackEntityAsMob(p_190102_1_);
        }
    }
    
    protected void checkAndPerformDefend(EntityLivingBase p_190102_1_, double p_190102_2_)
    {
    	if (this.npc.shieldUp == 0)
    	{
	        double d0 = this.getAttackReachSqr(this.npc, p_190102_1_);
	        
	        ItemStack itemStack = this.npc.getHeldItemOffhand();
	        if (itemStack != null && !itemStack.func_190926_b() && itemStack.getItem() == Items.SHIELD && p_190102_2_ <= 1.5 * d0)
	        {
	        	itemStack.useItemRightClick(this.npc.world, this.npc, EnumHand.OFF_HAND);
	        	this.npc.shieldUp = 1;
	        	this.npc.getNavigator().setSpeed(this.npc.SPEED / 4);
	        }
    	}
    	else
    	{
	        double d0 = this.getAttackReachSqr(this.npc, p_190102_1_);
	        
	        ItemStack itemStack = this.npc.getHeldItemOffhand();
	        if (itemStack != null && !itemStack.func_190926_b() && itemStack.getItem() == Items.SHIELD && p_190102_2_ > 1.5 * d0)
	        {
	        	this.npc.stopActiveHand();
	        	this.npc.shieldUp = 0;
	        	this.npc.getNavigator().setSpeed(this.npc.SPEED);
	        }
    	}
    }

    protected static double getAttackReachSqr(EntityNPC entity, EntityLivingBase attackTarget)
    {
        return (double)(entity.width * 5.0F * entity.width * 5.0F + attackTarget.width);
    }
    
    public static boolean checkAttackTarget(EntityNPC entity)
    {
        EntityLivingBase entitylivingbase = entity.getAttackTarget();

        if (entitylivingbase == null)
        {
        	return false;
        }
        
        if (!entitylivingbase.isEntityAlive() || (entitylivingbase instanceof EntityPlayer && (((EntityPlayer) entitylivingbase).isCreative() || ((EntityPlayer) entitylivingbase).getLeashed())))
        {
            return false;
        }
        else
        {
        	NPCPathNavigate navigator = entity.getNavigator();
        	
        	if (navigator.noPath())
        	{
                if (getAttackReachSqr(entity, entitylivingbase) >= entity.getDistanceSq(entitylivingbase.posX, entitylivingbase.getEntityBoundingBox().minY, entitylivingbase.posZ))
                {
                	return true;
                }
                else
                {
                	if (entity.shieldUp == 0)
                	{
                		navigator.tryMoveToEntityLiving(entitylivingbase, entity.SPEED);
                	}
                	else
                	{
                		navigator.tryMoveToEntityLiving(entitylivingbase, entity.SPEED / 4);
                	}
            		return navigator.getPath() != null;
                }
        	}
        	else
        	{
        		return true;
        	}
        }
    }

	@Override
	public void readFromNBT(NBTTagCompound compound)
	{
		state = States.stateMap.get(compound.getInteger("state"));
		if (compound.hasKey("switched"))
		{
			switchedItemStack = new ItemStack(compound.getCompoundTag("switched"));
		}
	}

	@Override
	public void writeEntityToNBT(NBTTagCompound compound)
	{
		compound.setString("type", "combat");
		compound.setInteger("state", state.value);
		if (switchedItemStack != null)
		{
	        NBTTagCompound nbttagcompound = new NBTTagCompound();
			switchedItemStack.writeToNBT(nbttagcompound);
			compound.setTag("switched", nbttagcompound);
		}
	}
	
}
