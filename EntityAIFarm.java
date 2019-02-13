package net.minecraft.entity.player.ai.logic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.google.common.collect.Sets;

import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.player.EntityNPC;
import net.minecraft.entity.player.ai.pathfinding.NPCPathNavigate;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.ILockableContainer;

public class EntityAIFarm extends EntityAIJob
{
	private static enum States
	{
		NONE(0),
		
		PREHARVEST(1),
		PREPLANT(2),
		PREFEED(3),
		PREBUTCHER(4),
		PRECOOKBREAD(5),
		
		HARVEST(6),
		COLLECTHARVEST(7),
		STOREHARVEST(8),
		PUTHARVEST(9),
		
		FETCHPLANT(10),
		GOTOPLANT(11),
		PLANT(12),
		
		FETCHFEEDWHEAT(13),
		GOTOFEED(14),
		FEED(15),
		
		BUTCHER(16),
		COLLECTBEEF(17),
		STOREBEEF(18),
		PUTBEEF(19),
		
		FETCHCOOKWHEAT(20),
		COOKBREAD(21),
		STOREBREAD(22),
		PUTBREAD(23);
		
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
	
	private Vec3d[][] fieldLocations;
	private Vec3d[][] craftLocations;
	private Vec3d[][] cowLocations;
	private Vec3d[][] storageLocations;
	
	private List<Vec3d[]> fieldDestinations;
	private Vec3d[] fieldLocation;
	private Vec3d fieldDestination;
	private List<Vec3d> cropDestinations;
	private Vec3d cropDestination;
	private List<Vec3d> collectDestinations;
	private Vec3d collectDestination;
	private List<Vec3d> storageDestinations;
	private Vec3d storageDestination;
	private List<Vec3d[]> cowDestinations;
	private Vec3d cowDestination;
	private Vec3d[] cowLocation;
	private List<Vec3d> craftDestinations;
	private Vec3d craftDestination;
	
	private List<EntityCow> cows;
	private EntityCow cow;
	
	private int tick;
	
	private States state;
	
	public EntityAIFarm(EntityNPC npc)
	{
		super(npc);
		state = States.NONE;
		fieldDestinations = null;
		fieldLocation = null;
		fieldDestination = null;
		collectDestinations = null;
		collectDestination = null;
		cropDestinations = null;
		cropDestination = null;
		storageDestinations = null;
		storageDestination = null;
		cowDestinations = null;
		cowDestination = null;
		cowLocations = null;
		cowLocation = null;
		
		cows = null;
		cow = null;
		
		tick = 0;
	}

	@Override
	public int execute()
	{
		switch (state)
		{
		case NONE:
			Random random = npc.getRNG();
			int i = random.nextInt(5) + 1;
			state = States.getState(i);
			break;
			
		case PREHARVEST:
			preHarvest();
			break;
			
		case HARVEST:
			harvest();
			break;

		case COLLECTHARVEST:
			collectHarvest();
			break;
			
		case STOREHARVEST:
			storeHarvest();
			break;
			
		case PUTHARVEST:
			putHarvest();
			break;
			
		case PREPLANT:
			prePlant();
			break;
			
		case FETCHPLANT:
			fetchPlant();
			break;
			
		case GOTOPLANT:
			goToPlant();
			break;
			
		case PLANT:
			plant();
			break;
		
		case PREFEED:
			preFeed();
			break;
		
		case FETCHFEEDWHEAT:
			fetchFeedWheat();
			break;
		
		case GOTOFEED:
			goToFeed();
			break;
		
		case FEED:
			feed();
			break;
		
		case PREBUTCHER:
			preButcher();
			break;
		
		case BUTCHER:
			butcher();
			break;
		
		case COLLECTBEEF:
			collectBeef();
			break;
			
		case STOREBEEF:
			storeBeef();
			break;
		
		case PUTBEEF:
			putBeef();
			break;

		case PRECOOKBREAD:
			preCookBread();
			break;
		
		case FETCHCOOKWHEAT:
			fetchCookWheat();
			break;
		
		case COOKBREAD:
			cookBread();
			break;
		
		case STOREBREAD:
			storeBread();
			break;
		
		case PUTBREAD:
			putBread();
			break;
		
		default:
			state = States.NONE;
			break;
		}
		
		return 0;
	}
	
	private int goToField()
	{
		if (fieldDestinations == null)
		{
			fieldDestinations = initDestionations(fieldLocations);
		}
		
		if (fieldDestinations.size() == 0 && fieldDestination == null)
		{
			fieldDestinations = null;
			fieldDestination = null;
			return -1;
		}
		
		NPCPathNavigate navigator = npc.getNavigator();
		if (fieldDestination == null)
		{
			DestinationBundle result = findRandomDestinationBundle(fieldDestinations);
			fieldDestination = result.destination;
			if (fieldDestination == null)
			{
				return 0;
			}
			fieldLocation = result.location.clone();
			navigator.clearPathEntity();
			this.checkMoveTo(fieldDestination.xCoord, fieldDestination.yCoord, fieldDestination.zCoord);
			//navigator.tryMoveToXYZ(fieldDestination.xCoord, fieldDestination.yCoord, fieldDestination.zCoord, this.npc.SPEED);
		}
		else
		{
			if (!checkBlockValid(fieldDestination, Blocks.WHEAT, BlockCrops.AGE, 7))
			{
				fieldDestination = null;
				return 0;
			}
			
			if (navigator.getPath() == null)
			{
				this.checkMoveTo(fieldDestination.xCoord, fieldDestination.yCoord, fieldDestination.zCoord);
				//navigator.tryMoveToXYZ(fieldDestination.xCoord, fieldDestination.yCoord, fieldDestination.zCoord, this.npc.SPEED);
				if (navigator.getPath() == null)
				{
					fieldDestination = null;
				}
			}
			else if (navigator.noPath())
			{
				fieldDestination = null;
				return 1;
			}
		}
		
		return 0;
	}
	
	private int goToStorage()
	{
		if (storageDestinations == null)
		{
			storageDestinations = findBlocksInsideLocations(storageLocations, Blocks.CHEST, null, null);
			if (storageDestinations == null)
			{
				storageDestination = null;
				return -1;
			}
		}
		
		if (storageDestinations.size() == 0 && storageDestination == null)
		{
			storageDestinations = null;
			storageDestination = null;
			return -1;
		}

		NPCPathNavigate navigator = npc.getNavigator();
		if (storageDestination == null)
		{
			storageDestination = findRandomDestination(storageDestinations);
			navigator.clearPathEntity();
			if (storageDestination == null)
			{
				return 0;
			}
			BlockPos pos = findStorePosition();
			if (pos == null)
			{
				storageDestination = null;
				return 0;
			}
			this.checkMoveTo(pos.getX(), pos.getY(), pos.getZ());
			//navigator.tryMoveToXYZ(pos.getX(), pos.getY(), pos.getZ(), this.npc.SPEED);
		}
		else
		{
			if (navigator.getPath() == null)
			{
				BlockPos pos = findStorePosition();
				if (pos == null)
				{
					storageDestination = null;
					return 0;
				}
				this.checkMoveTo(pos.getX(), pos.getY(), pos.getZ());
				//navigator.tryMoveToXYZ(pos.getX(), pos.getY(), pos.getZ(), this.npc.SPEED);
				if (navigator.getPath() == null)
				{
					storageDestination = null;
					return 0;
				}
			}
			else if (navigator.noPath())
			{
				if (!checkBlockValid(storageDestination, Blocks.CHEST, null, null))
				{
					storageDestination = null;
					return 0;
				}
	            return 1;
			}
		}
		
		return 0;
	}
	
	private int goToCraftTable()
	{
		if (craftDestinations == null)
		{
			craftDestinations = findBlocksInsideLocations(craftLocations, Blocks.CRAFTING_TABLE, null, null);
			if (craftDestinations == null)
			{
				craftDestination = null;
				return -1;
			}
		}
		
		if (craftDestinations.size() == 0 && craftDestination == null)
		{
			craftDestinations = null;
			craftDestination = null;
			return -1;
		}

		NPCPathNavigate navigator = npc.getNavigator();
		if (craftDestination == null)
		{
			craftDestination = findRandomDestination(craftDestinations);
			if (craftDestination == null)
			{
				return 0;
			}
			navigator.clearPathEntity();
			this.checkMoveTo(craftDestination.xCoord, craftDestination.yCoord, craftDestination.zCoord);
			//navigator.tryMoveToXYZ(craftDestination.xCoord, craftDestination.yCoord, craftDestination.zCoord, this.npc.SPEED);
		}
		else
		{
			if (navigator.getPath() == null)
			{
				this.checkMoveTo(craftDestination.xCoord, craftDestination.yCoord, craftDestination.zCoord);
				//navigator.tryMoveToXYZ(craftDestination.xCoord, craftDestination.yCoord, craftDestination.zCoord, this.npc.SPEED);
				if (navigator.getPath() == null)
				{
					craftDestination = null;
					return 0;
				}
			}
			else if (navigator.noPath())
			{
				if (!checkBlockValid(craftDestination, Blocks.CRAFTING_TABLE, null, null))
				{
					craftDestination = null;
					return 0;
				}
	            return 1;
			}
		}
		
		return 0;
	}
	
	private int goToCow()
	{
		if (cowDestinations == null)
		{
			cowDestinations = initDestionations(cowLocations);
		}
		
		if (cowDestinations.size() == 0 && cowDestination == null)
		{
			cowDestinations = null;
			cowDestination = null;
			return -1;
		}
		
		NPCPathNavigate navigator = npc.getNavigator();
		if (cowDestination == null)
		{
			DestinationBundle result = findRandomDestinationBundle(cowDestinations);
			cowDestination = result.destination;
			if (cowDestination == null)
			{
				return 0;
			}
			cowLocation = result.location.clone();
			navigator.clearPathEntity();
			this.checkMoveTo(cowDestination.xCoord, cowDestination.yCoord, cowDestination.zCoord);
			//navigator.tryMoveToXYZ(cowDestination.xCoord, cowDestination.yCoord, cowDestination.zCoord, this.npc.SPEED);
		}
		else
		{
			if (navigator.getPath() == null)
			{
				this.checkMoveTo(cowDestination.xCoord, cowDestination.yCoord, cowDestination.zCoord);
				//navigator.tryMoveToXYZ(cowDestination.xCoord, cowDestination.yCoord, cowDestination.zCoord, this.npc.SPEED);
				if (navigator.getPath() == null)
				{
					cowDestination = null;
				}
			}
			else if (navigator.noPath())
			{
				cowDestination = null;
				return 1;
			}
		}
		
		return 0;
	}
	
	private void preHarvest()
	{
		switch (goToField())
		{
		case -1:
			state = States.NONE;
			break;
		case 1:
			state = States.HARVEST;
			break;
		default:
			break;
		}
	}
	
	private void harvest()
	{
		if (fieldLocation == null)
		{
			cropDestinations = null;
			cropDestination = null;
			state = States.PREHARVEST;
			return;
		}
		if (cropDestinations == null)
		{
			cropDestinations = findBlocksInsideLocation(fieldLocation, Blocks.WHEAT, BlockCrops.AGE, 7);
			if (cropDestinations == null)
			{
				cropDestination = null;
				state = States.PREHARVEST;
				return;
			}
		}
		
		if (cropDestinations.size() == 0 && cropDestination == null)
		{
			cropDestination = null;
			cropDestinations = null;
			state = States.STOREHARVEST;
			return;
		}
		
		NPCPathNavigate navigator = npc.getNavigator();
		if (cropDestination == null)
		{
			cropDestination = findRandomDestination(cropDestinations);
			navigator.clearPathEntity();
			if (cropDestination == null)
			{
				return;
			}
			this.checkMoveTo(cropDestination.xCoord, cropDestination.yCoord, cropDestination.zCoord);
			//navigator.tryMoveToXYZ(cropDestination.xCoord, cropDestination.yCoord, cropDestination.zCoord, this.npc.SPEED);
		}
		else
		{
			if (navigator.getPath() == null)
			{
				this.checkMoveTo(cropDestination.xCoord, cropDestination.yCoord, cropDestination.zCoord);
				//navigator.tryMoveToXYZ(cropDestination.xCoord, cropDestination.yCoord, cropDestination.zCoord, this.npc.SPEED);
				if (navigator.getPath() == null)
				{
					cropDestination = null;
					return;
				}
			}
			else if (navigator.noPath())
			{
				if (!checkBlockValid(cropDestination, Blocks.WHEAT, BlockCrops.AGE, 7))
				{
					cropDestination = null;
					return;
				}
				BlockPos pos = new BlockPos(cropDestination.xCoord, cropDestination.yCoord, cropDestination.zCoord);
				npc.hitBlock(pos);
				cropDestination = null;
				navigator.clearPathEntity();
				state = States.COLLECTHARVEST;
			}
		}
	}
	
	private boolean checkCollectItems(Set<Item> items)
	{
		boolean found = false;
        
        AxisAlignedBB axisalignedbb = this.npc.getEntityBoundingBox().expand(2.0D, 0.5D, 2.0D);
        
        List<Entity> list = this.npc.world.getEntitiesWithinAABB(EntityItem.class, axisalignedbb);

        for (int i = 0; i < list.size(); ++i)
        {
            Entity entity = list.get(i);
            if (entity.onGround && items.contains(((EntityItem)entity).getEntityItem().getItem()))
            {
            	collectDestinations.add(new Vec3d(entity.posX, entity.posY, entity.posZ));
            	found = true;
            }
        }
        
		return found;
	}
	
	private int collectItems(Set<Item> items)
	{
		if (this.npc.getName().startsWith("Isag"))
		{
			int test = 0;
		}
		if (collectDestinations == null)
		{
			collectDestinations = new ArrayList<>();
			if (!checkCollectItems(items))
			{
				collectDestinations = null;
				return -1;
			}
		}
		
		if (collectDestinations.size() == 0 && collectDestination == null)
		{
			collectDestinations = null;
			collectDestination = null;
			return 1;
		}
		
		if (collectDestination == null)
		{
			collectDestination = findRandomDestination(collectDestinations);
			if (collectDestination == null)
			{
				return 0;
			}
			BlockPos pos = new BlockPos(collectDestination.xCoord, Math.floor(collectDestination.yCoord), collectDestination.zCoord);
			if (!this.npc.world.getBlockState(pos).getBlock().isPassable(this.npc.world, pos))
			{
				collectDestination = null;
				return 0;
			}
            this.npc.getMoveHelper().setMoveTo(collectDestination.xCoord, Math.floor(collectDestination.yCoord), collectDestination.zCoord, this.npc.SPEED);
		}
		else
		{
			if (Math.abs(this.npc.posX - collectDestination.xCoord) <= 0.5 && Math.abs(this.npc.posZ - collectDestination.zCoord) <= 0.5)
			{
				collectDestination = null;
			}
			else
			{
				this.npc.getMoveHelper().setMoveTo(collectDestination.xCoord, Math.floor(collectDestination.yCoord), collectDestination.zCoord, this.npc.SPEED);
			}
		}
		
		return 0;
	}
	
	private void collectHarvest()
	{
		Set<Item> items = Sets.newHashSet();
		items.add(Items.WHEAT);
		items.add(Items.WHEAT_SEEDS);
		switch (collectItems(items))
		{
		case -1:
		case 1:
			state = States.HARVEST;
			break;
		default:
			break;
		}
		if (this.npc.getHeldItemMainhand().getItem() == Items.WHEAT)
		{
			this.npc.switchMainHandToItem(Items.field_190931_a);
		}
	}
	
	private BlockPos findStorePosition()
	{
		BlockPos result = null;
		BlockPos pos = new BlockPos(storageDestination.xCoord, storageDestination.yCoord, storageDestination.zCoord);
		Block block = this.npc.world.getBlockState(pos).getBlock();
		if (!(block instanceof BlockChest))
		{
			return result;
		}
		IBlockState blockState = npc.getEntityWorld().getBlockState(pos);
		switch ((EnumFacing) blockState.getProperties().get(BlockChest.FACING))
		{
		case NORTH:
			result = new BlockPos(storageDestination.xCoord, storageDestination.yCoord, storageDestination.zCoord - 1);
			break;
		case SOUTH:
			result = new BlockPos(storageDestination.xCoord, storageDestination.yCoord, storageDestination.zCoord + 1);
			break;
		case WEST:
			result = new BlockPos(storageDestination.xCoord - 1, storageDestination.yCoord, storageDestination.zCoord);
			break;
		case EAST:
			result = new BlockPos(storageDestination.xCoord + 1, storageDestination.yCoord, storageDestination.zCoord);
			break;
		default:
			break;
		}
		
		if (!this.npc.world.getBlockState(result).getBlock().isPassable(this.npc.world, result))
		{
			result = null;
		}
		
		if (result == null)
		{
			result = new BlockPos(storageDestination.xCoord + 1, storageDestination.yCoord, storageDestination.zCoord);
			if (this.npc.world.getBlockState(result).getBlock() == Blocks.AIR)
			{
				return result;
			}
			
			result = new BlockPos(storageDestination.xCoord - 1, storageDestination.yCoord, storageDestination.zCoord);
			if (this.npc.world.getBlockState(result).getBlock() == Blocks.AIR)
			{
				return result;
			}
			
			result = new BlockPos(storageDestination.xCoord, storageDestination.yCoord, storageDestination.zCoord + 1);
			if (this.npc.world.getBlockState(result).getBlock() == Blocks.AIR)
			{
				return result;
			}
			
			result = new BlockPos(storageDestination.xCoord, storageDestination.yCoord, storageDestination.zCoord - 1);
			if (this.npc.world.getBlockState(result).getBlock() == Blocks.AIR)
			{
				return result;
			}
			
			return null;
		}
		
		return result;
	}
	
	private void fetchItems(States successState, States failedState, Item item, int num)
	{
		if (storageDestination == null)
		{
			tick = 0;
            storageDestinations = null;
            state = States.NONE;
            return;
		}
        if (this.npc.hasItemNum(item) >= num)
        {
			BlockPos pos = new BlockPos(storageDestination.xCoord, storageDestination.yCoord, storageDestination.zCoord);
			Block block = this.npc.world.getBlockState(pos).getBlock();
			if (!(block instanceof BlockChest))
			{
	            tick = 0;
	            storageDestination = null;
            	state = failedState;
				return;
			}
            ILockableContainer ilockablecontainer = ((BlockChest) block).getLockableContainer(this.npc.world, pos);
            
            ilockablecontainer.closeInventory(this.npc);
            
            tick = 0;
            storageDestination = null;
            storageDestinations = null;
            state = successState;
            return;
        }
		if (tick == 10)
		{
			BlockPos pos = new BlockPos(storageDestination.xCoord, storageDestination.yCoord, storageDestination.zCoord);
			Block block = this.npc.world.getBlockState(pos).getBlock();
			if (!(block instanceof BlockChest))
			{
	            tick = 0;
	            storageDestination = null;
            	state = failedState;
				return;
			}
            ILockableContainer ilockablecontainer = ((BlockChest) block).getLockableContainer(this.npc.world, pos);
            
            Map<Item, Integer> items = new HashMap<>();
            items.put(item, num - this.npc.hasItemNum(item));
            this.npc.getFromChest(ilockablecontainer, items);
            ilockablecontainer.closeInventory(this.npc);
            
            tick = 0;
            storageDestination = null;

            if (this.npc.hasItemNum(item) >= num)
            {
                storageDestinations = null;
                state = successState;
            }
            else
            {
            	state = failedState;
            }
		}
		else
		{
			tick++;
		}
	}
	
	private void storeHarvest()
	{
		if (npc.hasItem(Items.WHEAT) || npc.hasItem(Items.WHEAT_SEEDS))
		{
			switch (goToStorage())
			{
			case -1:
				state = States.NONE;
				break;
			case 1:
				BlockPos pos = new BlockPos(storageDestination.xCoord, storageDestination.yCoord, storageDestination.zCoord);
				Block block = this.npc.world.getBlockState(pos).getBlock();
				if (!(block instanceof BlockChest))
				{
					tick = 0;
		            storageDestination = null;
					return;
				}
	            ILockableContainer ilockablecontainer = ((BlockChest) block).getLockableContainer(this.npc.world, pos);
	            ilockablecontainer.openInventory(this.npc);
				state = States.PUTHARVEST;
				tick = 0;
				break;
			default:
				break;
			}
		}
		else
		{
			storageDestinations = null;
			storageDestination = null;
			state = States.NONE;
		}
	}
	
	private int putItems(Map<Item, Integer> items)
	{
		if (storageDestination == null)
		{
			tick = 0;
            storageDestinations = null;
            return -1;
		}
		if (tick == 10)
		{
			BlockPos pos = new BlockPos(storageDestination.xCoord, storageDestination.yCoord, storageDestination.zCoord);
			Block block = this.npc.world.getBlockState(pos).getBlock();
			if (!(block instanceof BlockChest))
			{
				tick = 0;
	            storageDestination = null;
				return -1;
			}
            ILockableContainer ilockablecontainer = ((BlockChest) block).getLockableContainer(this.npc.world, pos);
            
            this.npc.putIntoChest(ilockablecontainer, items);
            ilockablecontainer.closeInventory(this.npc);
            
            tick = 0;
			storageDestination = null;
			
			for (Item item : items.keySet())
			{
	    		if (npc.hasItem(item))
	            {
	            	return 2;
	            }
			}
            
            storageDestinations = null;
            return 1;
		}
		else
		{
			tick++;
		}
		return 0;
	}

	private void putHarvest()
	{
		Map<Item, Integer> items = new HashMap<>();
        items.put(Items.WHEAT, -1);
        items.put(Items.WHEAT_SEEDS, -1);
        switch (putItems(items))
        {
        case -1:
        case 1:
            state = States.NONE;
        	break;
        case 2:
            state = States.STOREHARVEST;
            break;
        default:
        	break;
        }
	}
	
	private void prePlant()
	{
		switch (goToStorage())
		{
		case -1:
			state = States.STOREHARVEST;
			break;
		case 1:
			BlockPos pos = new BlockPos(storageDestination.xCoord, storageDestination.yCoord, storageDestination.zCoord);
			Block block = this.npc.world.getBlockState(pos).getBlock();
			if (!(block instanceof BlockChest))
			{
				tick = 0;
	            storageDestination = null;
				return;
			}
            ILockableContainer ilockablecontainer = ((BlockChest) block).getLockableContainer(this.npc.world, pos);
            ilockablecontainer.openInventory(this.npc);
			state = States.FETCHPLANT;
			tick = 0;
			break;
		default:
			break;
		}
	}
	
	private void fetchPlant()
	{
		this.fetchItems(States.GOTOPLANT, States.PREPLANT, Items.WHEAT_SEEDS, 5);
	}
	
	private void goToPlant()
	{
		switch (goToField())
		{
		case -1:
			state = States.STOREHARVEST;
			break;
		case 1:
			state = States.PLANT;
			break;
		default:
			break;
		}
	}
	
	private void plant()
	{
		if (fieldLocation == null)
		{
			cropDestinations = null;
			cropDestination = null;
			state = States.GOTOPLANT;
			return;
		}
		if (cropDestinations == null)
		{
			List<Vec3d> airDestinations = findBlocksInsideLocation(fieldLocation, Blocks.AIR, null, null);
			if (airDestinations == null)
			{
				cropDestinations = null;
				cropDestination = null;
				state = States.GOTOPLANT;
				return;
			}
			Vec3d[] dirtLocation = new Vec3d[2];
			dirtLocation[0] = new Vec3d(fieldLocation[0].xCoord, fieldLocation[0].yCoord - 1, fieldLocation[0].zCoord);
			dirtLocation[1] = new Vec3d(fieldLocation[1].xCoord, fieldLocation[1].yCoord - 1, fieldLocation[1].zCoord);
			List<Vec3d> farmLandDestinations = findBlocksInsideLocation(dirtLocation, Blocks.FARMLAND, null, null);
			if (farmLandDestinations == null)
			{
				cropDestinations = null;
				cropDestination = null;
				state = States.GOTOPLANT;
				return;
			}
			
			cropDestinations = new ArrayList();
			for (Vec3d air : airDestinations)
			{
				for (Vec3d farmLand : farmLandDestinations)
				{
					if (air.xCoord == farmLand.xCoord && air.zCoord == farmLand.zCoord)
					{
						cropDestinations.add(air);
					}
				}
			}
		}
		
		if (cropDestinations.size() == 0 && cropDestination == null)
		{
			cropDestination = null;
			cropDestinations = null;
			state = States.GOTOPLANT;
			return;
		}
		
		NPCPathNavigate navigator = npc.getNavigator();
		if (cropDestination == null)
		{
			cropDestination = findRandomDestination(cropDestinations);
			navigator.clearPathEntity();
			if (cropDestination == null)
			{
				return;
			}
			this.checkMoveTo(cropDestination.xCoord, cropDestination.yCoord, cropDestination.zCoord);
			//navigator.tryMoveToXYZ(cropDestination.xCoord, cropDestination.yCoord, cropDestination.zCoord, this.npc.SPEED);
		}
		else
		{
			if (!checkBlockValid(cropDestination, Blocks.AIR, null, null))
			{
				cropDestination = null;
				return;
			}
			
			if (navigator.getPath() == null)
			{
				this.checkMoveTo(cropDestination.xCoord, cropDestination.yCoord, cropDestination.zCoord);
				//navigator.tryMoveToXYZ(cropDestination.xCoord, cropDestination.yCoord, cropDestination.zCoord, this.npc.SPEED);
				if (navigator.getPath() == null)
				{
					cropDestination = null;
					return;
				}
			}
			else if (navigator.noPath())
			{
				if (!this.npc.switchMainHandToItem(Items.WHEAT_SEEDS))
				{
					cropDestination = null;
					cropDestinations = null;
					state = States.GOTOPLANT;
					return;
				}
				
				this.npc.getHeldItemMainhand().onItemUse(this.npc, this.npc.world, new BlockPos(cropDestination.xCoord, cropDestination.yCoord - 1, cropDestination.zCoord),
					EnumHand.MAIN_HAND, EnumFacing.UP, 0, 0, 0);
				
				cropDestination = null;
				navigator.clearPathEntity();
			}
		}
	}
	
	private void preFeed()
	{
		switch (goToStorage())
		{
		case -1:
			state = States.STOREHARVEST;
			break;
		case 1:
			BlockPos pos = new BlockPos(storageDestination.xCoord, storageDestination.yCoord, storageDestination.zCoord);
			Block block = this.npc.world.getBlockState(pos).getBlock();
			if (!(block instanceof BlockChest))
			{
				tick = 0;
	            storageDestination = null;
				return;
			}
            ILockableContainer ilockablecontainer = ((BlockChest) block).getLockableContainer(this.npc.world, pos);
            ilockablecontainer.openInventory(this.npc);
			state = States.FETCHFEEDWHEAT;
			tick = 0;
			break;
		default:
			break;
		}
	}
	
	private void fetchFeedWheat()
	{
		this.fetchItems(States.GOTOFEED, States.PREFEED, Items.WHEAT, 8);
		if (!this.npc.switchMainHandToItem(Items.field_190931_a))
		{
			state = States.STOREHARVEST;
			return;
		}
	}
	
	private void goToFeed()
	{
		switch (goToCow())
		{
		case -1:
			state = States.NONE;
			break;
		case 1:
			state = States.FEED;
			break;
		default:
			break;
		}
	}
	
	private void feed()
	{
		if (!this.npc.hasItem(Items.WHEAT))
		{
			cow = null;
			cows = null;
			state = States.NONE;
			return;
		}
		if (cows == null || cows.size() == 0)
		{
			cows = this.npc.world.getEntitiesWithinAABB(EntityCow.class, this.npc.getEntityBoundingBox().expand(10.0D, 2.0D, 10.0D));
			if (cows == null || cows.size() == 0)
			{
				state = States.STOREHARVEST;
				return;
			}
		}
		
		NPCPathNavigate navigator = this.npc.getNavigator();
		if (cow == null)
		{
			cow = findRandomCow();
			if (cow == null)
			{
				cows = null;
				state = States.NONE;
				return;
			}
			navigator.clearPathEntity();
			navigator.tryMoveToEntityLiving(cow, this.npc.SPEED);
			if (!this.npc.switchMainHandToItem(Items.WHEAT))
			{
				state = States.NONE;
				return;
			}
		}
		else
		{
			if (navigator.getPath() == null)
			{
				navigator.tryMoveToEntityLiving(cow, this.npc.SPEED);
				if (navigator.getPath() == null)
				{
					cow = null;
					return;
				}
			}
			else if (navigator.noPath())
			{
				this.npc.func_190775_a(cow, EnumHand.MAIN_HAND);
				cow = null;
				navigator.clearPathEntity();
			}
		}
	}
	
	private EntityCow findRandomCow()
	{
		if (cows == null || cows.size() == 0)
		{
			return null;
		}

		Random random = npc.getRNG();
		int index = random.nextInt(cows.size());
		EntityCow result = cows.get(index);
		cows.remove(index);
		return result;
	}
	
	private void preButcher()
	{
		switch (goToCow())
		{
		case -1:
			state = States.NONE;
			break;
		case 1:
			state = States.BUTCHER;
			break;
		default:
			break;
		}
	}
	
	private void butcher()
	{
		if (cow == null)
		{
			Vec3d lower = this.cowLocation[0];
			Vec3d upper = this.cowLocation[1];
			List<EntityCow> foundCows = this.npc.world.getEntitiesWithinAABB(EntityCow.class, new AxisAlignedBB(lower.xCoord, lower.yCoord, lower.zCoord, upper.xCoord, upper.yCoord + 1, upper.zCoord));
			List<EntityCow> matureCows = new ArrayList<>();
			for (EntityCow element : foundCows)
			{
				if (!element.isChild())
				{
					matureCows.add(element);
				}
			}
			if (matureCows.size() <= 20)
			{
				this.state = States.NONE;
				return;
			}
			cows = matureCows;
			cow = this.findRandomCow();
			cows = null;
			this.npc.setAttackTarget(cow);
			if (EntityAICombat.checkAttackTarget(this.npc))
			{
				this.npc.mind.addCombatJob();
			}
		}
		else if (!cow.isEntityAlive())
		{
			cow = null;
			this.state = States.COLLECTBEEF;
		}
		else if (this.npc.getNavigator().getPath() == null)
		{
			this.npc.getNavigator().tryMoveToEntityLiving(cow, this.npc.SPEED);
			if (this.npc.getNavigator().getPath() == null)
			{
				cow = null;
			}
		}
	}
	
	private void collectBeef()
	{
		Set<Item> items = Sets.newHashSet();
		items.add(Items.BEEF);
		switch (collectItems(items))
		{
		case -1:
			state = States.BUTCHER;
			break;
		case 1:
			state = States.STOREBEEF;
			break;
		default:
			break;
		}
	}
	
	private void storeBeef()
	{
		switch (goToStorage())
		{
		case -1:
			state = States.NONE;
			break;
		case 1:
			BlockPos pos = new BlockPos(storageDestination.xCoord, storageDestination.yCoord, storageDestination.zCoord);
			Block block = this.npc.world.getBlockState(pos).getBlock();
			if (!(block instanceof BlockChest))
			{
				tick = 0;
	            storageDestination = null;
				return;
			}
            ILockableContainer ilockablecontainer = ((BlockChest) block).getLockableContainer(this.npc.world, pos);
            ilockablecontainer.openInventory(this.npc);
			state = States.PUTBEEF;
			tick = 0;
			break;
		default:
			break;
		}
	}
	
	private void putBeef()
	{
		Map<Item, Integer> items = new HashMap<>();
        items.put(Items.BEEF, -1);
        items.put(Items.LEATHER, -1);
        items.put(Items.COOKED_BEEF, -1);
        switch (putItems(items))
        {
        case -1:
        case 1:
            state = States.NONE;
        	break;
        case 2:
            state = States.STOREBEEF;
            break;
        default:
        	break;
        }
	}
	
	private void preCookBread()
	{
		switch (goToStorage())
		{
		case -1:
			state = States.STOREBREAD;
			break;
		case 1:
			BlockPos pos = new BlockPos(storageDestination.xCoord, storageDestination.yCoord, storageDestination.zCoord);
			Block block = this.npc.world.getBlockState(pos).getBlock();
			if (!(block instanceof BlockChest))
			{
				tick = 0;
	            storageDestination = null;
				return;
			}
            ILockableContainer ilockablecontainer = ((BlockChest) block).getLockableContainer(this.npc.world, pos);
            ilockablecontainer.openInventory(this.npc);
			state = States.FETCHCOOKWHEAT;
			tick = 0;
			break;
		default:
			break;
		}
	}
	
	private void fetchCookWheat()
	{
		this.fetchItems(States.COOKBREAD, States.PRECOOKBREAD, Items.WHEAT, 6);
	}
	
	private void cookBread()
	{
		switch (goToCraftTable())
		{
		case -1:
			craftDestination = null;
			craftDestinations = null;
			state = States.NONE;
			break;
		case 1:
			craftDestination = null;
			craftDestinations = null;
			while (this.npc.hasItemNum(Items.WHEAT) >= 3)
			{
				NPCInventoryCrafting craft = new NPCInventoryCrafting(null, 3, 3);
				craft.setInventorySlotContents(0, this.npc.popItem(Items.WHEAT, 1));
				craft.setInventorySlotContents(1, this.npc.popItem(Items.WHEAT, 1));
				craft.setInventorySlotContents(2, this.npc.popItem(Items.WHEAT, 1));
				
				ItemStack bread = this.npc.craft(craft);
				if (!this.npc.inventory.addItemStackToInventory(bread))
				{
					this.npc.dropItem(bread, false);
				}
			}
			state = States.STOREBREAD;
			break;
		default:
			break;
		}
	}
	
	private void storeBread()
	{
		switch (goToStorage())
		{
		case -1:
			state = States.NONE;
			break;
		case 1:
			BlockPos pos = new BlockPos(storageDestination.xCoord, storageDestination.yCoord, storageDestination.zCoord);
			Block block = this.npc.world.getBlockState(pos).getBlock();
			if (!(block instanceof BlockChest))
			{
				tick = 0;
	            storageDestination = null;
				return;
			}
            ILockableContainer ilockablecontainer = ((BlockChest) block).getLockableContainer(this.npc.world, pos);
            ilockablecontainer.openInventory(this.npc);
			state = States.PUTBREAD;
			tick = 0;
			break;
		default:
			break;
		}
	}
	
	private void putBread()
	{
		Map<Item, Integer> items = new HashMap<>();
        items.put(Items.WHEAT, -1);
        items.put(Items.BREAD, -1);
        switch (putItems(items))
        {
        case -1:
        case 1:
            state = States.NONE;
        	break;
        case 2:
            state = States.STOREBREAD;
            break;
        default:
        	break;
        }
	}

	@Override
	public void readFromNBT(NBTTagCompound compound)
	{
		state = States.stateMap.get(compound.getInteger("state"));
		fieldLocations = read2dFromNBT("fieldLocations", compound);
		craftLocations = read2dFromNBT("craftLocations", compound);
		cowLocations = read2dFromNBT("cowLocations", compound);
		storageLocations = read2dFromNBT("storageLocations", compound);
		
		fieldDestinations = read2dArrayFromNBT("fieldDestinations", compound);
		fieldLocation = read1dFromNBT("fieldLocation", compound);
		fieldDestination = readVec3dFromNBT("fieldDestination", compound);
		cropDestinations = read1dArrayFromNBT("cropDestinations", compound);
		cropDestination = readVec3dFromNBT("cropDestination", compound);
		storageDestinations = read1dArrayFromNBT("storageDestinations", compound);
		storageDestination = readVec3dFromNBT("storageDestination", compound);
		cowDestinations = read2dArrayFromNBT("cowDestinations", compound);
		cowDestination = readVec3dFromNBT("cowDestination", compound);
		cowLocation = read1dFromNBT("cowLocation", compound);
		craftDestinations = read1dArrayFromNBT("craftDestinations", compound);
		craftDestination = readVec3dFromNBT("craftDestination", compound);
	}

	@Override
	public void writeEntityToNBT(NBTTagCompound compound)
	{
		compound.setString("type", "farm");
		compound.setInteger("state", state.value);;
		add2dToNBT("fieldLocations", fieldLocations, compound);
		add2dToNBT("craftLocations", craftLocations, compound);
		add2dToNBT("cowLocations", cowLocations, compound);
		add2dToNBT("storageLocations", storageLocations, compound);

		add2dArrayToNBT("fieldDestinations", fieldDestinations, compound);
		add1dToNBT("fieldLocation", fieldLocation, compound);
		addVec3dToNBT("fieldDestination", fieldDestination, compound);
		add1dArrayToNBT("cropDestinations", cropDestinations, compound);
		addVec3dToNBT("cropDestination", cropDestination, compound);
		add1dArrayToNBT("storageDestinations", storageDestinations, compound);
		addVec3dToNBT("storageDestination", storageDestination, compound);
		add2dArrayToNBT("cowDestinations", cowDestinations, compound);
		addVec3dToNBT("cowDestination", cowDestination, compound);
		add1dToNBT("cowLocation", cowLocation, compound);
		add1dArrayToNBT("craftDestinations", craftDestinations, compound);
		addVec3dToNBT("craftDestination", craftDestination, compound);
	}

	public Vec3d[][] getFieldLocations()
	{
		return fieldLocations;
	}

	public void setFieldLocations(Vec3d[][] fieldLocations)
	{
		this.fieldLocations = fieldLocations;
	}

	public Vec3d[][] getStorageLocations()
	{
		return storageLocations;
	}

	public void setStorageLocations(Vec3d[][] storageLocations)
	{
		this.storageLocations = storageLocations;
	}

	public Vec3d[][] getCraftLocations()
	{
		return craftLocations;
	}

	public void setCraftLocations(Vec3d[][] craftLocations)
	{
		this.craftLocations = craftLocations;
	}

	public Vec3d[][] getCowLocations()
	{
		return cowLocations;
	}

	public void setCowLocations(Vec3d[][] cowLocations)
	{
		this.cowLocations = cowLocations;
	}
}
