package net.minecraft.world.gen.structure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStoneSlab;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.nbt.StructNBT;

public class PandoraTown
{
	public static final int STREETY = 16;
	private static final int TOWNSIZE = 300;
	
	private static final class NarrowStreet
	{
		public static final int WIDTH = 2;
		
		public static final int GAP = 1;
	}
	
	private static final class MidStreet
	{
		public static final int WIDTH = 4;
		
		public static final int GAP = 3;
	}
	
	private static final class WideStreet
	{
		public static final int WIDTH = 8;
		
		public static final int GAP = 4;
	}
	
	private static enum Urban
	{
		VILLA(0),
		HOUSE(1),
		BARRACK(2);
		private static Map<Integer, Urban> typeMap = new HashMap<>();
		
		private int value;
		
		private Urban(int value)
		{
			this.value = value;
		}
		
		static
		{
			for (Urban state : Urban.values())
			{
				typeMap.put(state.value, state);
			}
		}
		
		public static Urban getType(int value)
		{
			return typeMap.get(value);
		}
		
		public int getValue()
		{
			return value;
		}
	}
	
	private static enum Rural
	{
		FARM(0);
		private static Map<Integer, Rural> typeMap = new HashMap<>();
		
		private int value;
		
		private Rural(int value)
		{
			this.value = value;
		}
		
		static
		{
			for (Rural state : Rural.values())
			{
				typeMap.put(state.value, state);
			}
		}
		
		public static Rural getType(int value)
		{
			return typeMap.get(value);
		}
		
		public int getValue()
		{
			return value;
		}
	}
	
	private class StreetPos
	{
		public Vec3i pos;
		public int direction;
		public StructureStreet prev;
		public int width;
		public int gap;
		
		public StreetPos (Vec3i pos, int direction, int width, int gap, StructureStreet prev)
		{
			this.pos = pos;
			this.direction = direction;
			this.width = width;
			this.gap = gap;
			this.prev = prev;
		}
	}
	
	private class StreetPoint
	{
		public Vec3i pos;
		public int direction;
		public int leftFlag;
		public int width;
		public int counter;
		public StructureStreet cur;
		
		public StreetPoint(Vec3i pos, int direction, int leftFlag, int width, StructureStreet cur)
		{
			super();
			this.pos = pos;
			this.direction = direction;
			this.leftFlag = leftFlag;
			this.width = width;
			this.cur = cur;
			counter = 0;
		}
	}

	private List<StructureStreet> streets;
	private List<StructurePandoraBase> structures;
	
	private List<StreetPoint> streetPoints;
	
	public int startX;
	public int startZ;
	private Random rand;
	private long seed;

	public PandoraTown(int startX, int startZ, long seed)
	{
		this.structures = new ArrayList<>();
		this.streets = new ArrayList<>();
		this.streetPoints = new ArrayList<>();
		this.startX = startX;
		this.startZ = startZ;
		this.rand = new Random(seed);
		this.seed = seed;
	}
	
	public void generateStructures()
	{
		int counter = 0;
		while (!generateTownCenter() && counter < 100)
		{
			structures.clear();
			streets.clear();
			streetPoints.clear();
			counter++;
		}
		
		streetPoints.clear();

//		IBlockState material = Blocks.RED_SANDSTONE.getDefaultState();
//		streets.add(new StructureStreet(1, new StructureBoundingBox(new int[] {startX - TOWNSIZE - 5, STREETY, startZ - TOWNSIZE - 5, startX + TOWNSIZE + 5, STREETY, startZ + TOWNSIZE + 5}), material));
//		
//		this.addNewUrbanStructure(Urban.BARRACK, EnumFacing.NORTH, new Vec3i(0, 17, 0));
//		this.addNewUrbanStructure(Urban.BARRACK, EnumFacing.SOUTH, new Vec3i(50, 17, 0));
//		this.addNewUrbanStructure(Urban.BARRACK, EnumFacing.WEST, new Vec3i(100, 17, 0));
//		this.addNewUrbanStructure(Urban.BARRACK, EnumFacing.EAST, new Vec3i(150, 17, 0));
//		structures.add(new StructureFarm(1, 0, EnumFacing.NORTH, startX + TOWNSIZE + 5, startZ + TOWNSIZE + 5));
	}
	
	public List<BlockPos> findWayPoints(BlockPos start, BlockPos end)
	{
		List<BlockPos> wayPoints = new ArrayList();
		
		if (start.equals(end))
		{
			wayPoints.add(end);
			return wayPoints;
		}
		
		StructurePandoraBase startStruct = null;
		StructurePandoraBase endStruct = null;
		for (StructurePandoraBase struct : this.structures)
		{
			if (struct.boundingBox.isVecInside(start))
			{
				startStruct = struct;
			}
			if (struct.boundingBox.isVecInside(end))
			{
				endStruct = struct;
			}
			if (startStruct != null && endStruct != null)
			{
				break;
			}
		}
		
		if (startStruct != null && startStruct == endStruct)
		{
			wayPoints.add(end);
			return wayPoints;
		}

		StructureStreet startNearestStreet = null;
		if (startStruct == null)
		{
			startNearestStreet = findNearestStreet(start);
		}
		else
		{
			BlockPos nearestDoor = startStruct.findNearestDoor(start);
			if (nearestDoor == null)
			{
				startNearestStreet = findNearestStreet(start);
			}
			else
			{
				startNearestStreet = findNearestStreet(nearestDoor);
			}
		}

		StructureStreet endNearestStreet = null;
		if (endStruct == null)
		{
			endNearestStreet = findNearestStreet(end);
		}
		else
		{
			BlockPos nearestDoor = endStruct.findNearestDoor(end);
			if (nearestDoor == null)
			{
				endNearestStreet = findNearestStreet(end);
			}
			else
			{
				endNearestStreet = findNearestStreet(nearestDoor);
			}
		}
		
		if (startNearestStreet == null || endNearestStreet == null)
		{
			wayPoints.add(end);
			return wayPoints;
		}

		StructureBoundingBox box = new StructureBoundingBox(startNearestStreet.boundingBox);
		box.maxY += 1;
		BlockPos nearestPoint = findNearestPoint(start, box);
		
		if (start.distanceSq(end) <= start.distanceSq(nearestPoint))
		{
			wayPoints.add(end);
			return wayPoints;
		}
		wayPoints.add(nearestPoint);
		
		Set<StructureStreet> visited = new HashSet();
		visited.add(startNearestStreet);
		List<StructureStreet> queue = new ArrayList();
		queue.add(startNearestStreet);
		HashMap<StructureStreet, StructureStreet> parents = new HashMap();
		
		boolean found = false;
		while (queue.size() > 0 && !found)
		{
			List<StructureStreet> newQueue = new ArrayList();
			for (StructureStreet curStreet : queue)
			{
				for (StructureStreet neighbor : curStreet.getNeighbors())
				{
					if (!visited.contains(neighbor))
					{
						visited.add(neighbor);
						newQueue.add(neighbor);
						parents.put(neighbor, curStreet);
					}
					if (neighbor == endNearestStreet)
					{
						found = true;
						break;
					}
				}
				if (found)
				{
					break;
				}
			}
			
			queue = newQueue;
		}
		List<BlockPos> path = new ArrayList();
		
		box = new StructureBoundingBox(endNearestStreet.boundingBox);
		box.maxY += 1;
		BlockPos curPos = findNearestPoint(end, box);
		path.add(curPos);
		
		StructureStreet curStreet = endNearestStreet;
		while (parents.containsKey(curStreet))
		{
			curStreet = parents.get(curStreet);
			box = new StructureBoundingBox(curStreet.boundingBox);
			box.maxY += 1;
			curPos = findNearestPoint(curPos, box);
			path.add(curPos);
		}
		
		for (int i = path.size() - 1; i >= 0; i --)
		{
			wayPoints.add(path.get(i));
		}
		
		wayPoints.add(end);
		
		return wayPoints;
	}
	
	private StructureStreet findNearestStreet(BlockPos pos)
	{
		double distance = -1.0;
		StructureStreet targetStreet = null;
		for (int i = 1; i < this.streets.size(); i++)
		{
			StructureStreet curStreet = this.streets.get(i);
			StructureBoundingBox box = new StructureBoundingBox(curStreet.boundingBox);
			box.maxY += 1;
			BlockPos nearestPoint = findNearestPoint(pos, box);
			double curDistance = nearestPoint.distanceSq(pos);
			if (targetStreet == null || distance > curDistance)
			{
				targetStreet = curStreet;
				distance = curDistance;
			}
		}
		
		return targetStreet;
	}

	private BlockPos findNearestPoint(BlockPos pos, StructureBoundingBox box)
	{
		int resultX = pos.getX();
		int resultY = pos.getY();
		int resultZ = pos.getZ();
		
		if (box.minX > resultX)
		{
			resultX = box.minX;
		}
		if (box.maxX < resultX)
		{
			resultX = box.maxX;
		}
		if (box.minY > resultY)
		{
			resultY = box.minY;
		}
		if (box.maxY < resultY)
		{
			resultY = box.maxY;
		}
		if (box.minZ > resultZ)
		{
			resultZ = box.minZ;
		}
		if (box.maxZ < resultZ)
		{
			resultZ = box.maxZ;
		}
		
		return new BlockPos(resultX, resultY, resultZ);
	}
	
	private boolean generateTownCenter()
	{
		IBlockState material = Blocks.RED_SANDSTONE.getDefaultState();
		streets.add(new StructureStreet(1, new StructureBoundingBox(new int[] {startX - TOWNSIZE - 5, STREETY, startZ - TOWNSIZE - 5, startX + TOWNSIZE + 5, STREETY, startZ + TOWNSIZE + 5}), material));
		
		int[] structureSizes = this.initStructureSize();
		
		List<Urban> structureTypes = new ArrayList<>();
		
		for (int i = 0; i < structureSizes.length; i++)
		{
			Urban type = Urban.getType(i);
			for (int j = 0; j < structureSizes[i]; j ++)
			{
				structureTypes.add(type);
			}
		}
		
		Collections.shuffle(structureTypes, new Random(this.seed));
		
		int curIndex = 0;
		int size = structureTypes.size();
		
		while (curIndex < size)
		{
			int curSize = 4;
			int minSize = 2;
			int width = MidStreet.WIDTH;
			int gap = MidStreet.GAP;
			
			int streetSizeRand = rand.nextInt(5);
			if (streetSizeRand >= 4)
			{
				width = WideStreet.WIDTH;
				gap = WideStreet.GAP;
				curSize = 6;
				minSize = 3;
			}
			else if (streetSizeRand >= 3)
			{
				width = NarrowStreet.WIDTH;
				gap = NarrowStreet.GAP;
				curSize = 2;
				minSize = 1;
			}
			
			if (this.streets.size() != 1 && this.streetPoints.size() == 0)
			{
				width = NarrowStreet.WIDTH;
				gap = NarrowStreet.GAP;
				curSize = 2;
				minSize = 1;
			}
			
			if (size - curIndex < curSize + minSize)
			{
				curSize = size - curIndex;
				minSize = 1;
			}
			
			int streetStructureSize = this.rand.nextInt(curSize) + minSize;
			int[] gapSize = new int[streetStructureSize - 1];
			for (int i = 0; i < gapSize.length; i++)
			{
				gapSize[i] = this.rand.nextInt(4) + 2;
			}
			
			if (this.streets.size() != 1 && this.streetPoints.size() == 0)
			{
				break;
			}
			else
			{
				StreetPos newStreetPos = findNewStreetPos(gapSize, structureTypes, curIndex, streetStructureSize, width, gap);
				if (newStreetPos != null)
				{
					curIndex += buildStreetAndStructs(newStreetPos, gapSize, structureTypes, curIndex, streetStructureSize);
				}
			}
		}
		if (structureTypes.size() != this.structures.size())
		{
			return false;
		}
		else
		{
			return true;
		}
	}
	
	private boolean addNewUrbanStructure(Urban type, EnumFacing streetFacing, Vec3i startPos)
	{
		StructurePandoraBase struct = null;
		switch(type)
		{
		case VILLA:
			struct = new StructureVilla(1, 0, streetFacing, startPos.getX(), startPos.getZ());
			break;
		case HOUSE:
			struct = new StructureHouse(1, 0, streetFacing, startPos.getX(), startPos.getZ());
			break;
		case BARRACK:
			struct = new StructureBarrack(1, 0, streetFacing, startPos.getX(), startPos.getZ());
			break;
		default:
			break;
		}
		
		StructureBoundingBox box = new StructureBoundingBox(struct.boundingBox.minX - 1, struct.boundingBox.minY - 1, struct.boundingBox.minZ - 1, struct.boundingBox.maxX + 1, struct.boundingBox.maxY, struct.boundingBox.maxZ + 1);
		if (struct != null && StructureStreet.findIntersectingStreet(this.streets.subList(1, this.streets.size()), box) == null && StructurePandoraBase.findIntersectingPandora(structures, box) == null)
		{
			structures.add(struct);
			return true;
		}
		
		return false;
	}
	
	private int preCaculateStreetLen(int[] gapSize, List<Urban> structureTypes, int curIndex, int streetStructureSize, int gap)
	{
		int streetLen = gap;
		
		for (int i = curIndex; i < curIndex + streetStructureSize; i++)
		{
			if (i < curIndex + streetStructureSize - 1)
			{
				streetLen += gapSize[i - curIndex];
			}
			else
			{
				streetLen += gap;
			}
			
			streetLen += preStreetWidth(structureTypes.get(i));
		}
		
		return streetLen;
	}
	
	private int preStreetWidth(Urban type)
	{
		switch(type)
		{
		case VILLA:
			return StructureVilla.preStreetWidth();
		case HOUSE:
			return StructureHouse.preStreetWidth();
		case BARRACK:
			return StructureBarrack.preStreetWidth();
		default:
			return 0;
		}
	}
	
	private int preStreetLength(Urban type)
	{
		switch(type)
		{
		case VILLA:
			return StructureVilla.preStreetLength();
		case HOUSE:
			return StructureHouse.preStreetLength();
		case BARRACK:
			return StructureBarrack.preStreetLength();
		default:
			return 0;
		}
	}
	
	private int buildStreetAndStructs(StreetPos newStreetPos, int[] gapSize, List<Urban> structureTypes, int curIndex, int streetStructureSize)
	{
		int streetLen = newStreetPos.gap;
		int i;
		for (i = curIndex; i < curIndex + streetStructureSize; i++)
		{
			Urban type = structureTypes.get(i);
			if (newStreetPos.direction == 1)
			{
				StructureBoundingBox streetBoundingBox = new StructureBoundingBox(new int[] {newStreetPos.pos.getX() - 1, STREETY, newStreetPos.pos.getZ() - 1, newStreetPos.pos.getX() + streetLen - 1 + 1, STREETY + 1, newStreetPos.pos.getZ() + newStreetPos.width - 1 + 1});
				if (StructurePandoraBase.findIntersectingPandora(structures, streetBoundingBox) != null)
				{
					break;
				}
				if (newStreetPos.pos.getX() + streetLen + preStreetWidth(type) > startX + TOWNSIZE)
				{
					break;
				}
				if (newStreetPos.pos.getZ() + newStreetPos.gap + newStreetPos.width + preStreetLength(type) > startZ + TOWNSIZE)
				{
					break;
				}
				Vec3i newStructurePos = new Vec3i(newStreetPos.pos.getX() + streetLen, 0, newStreetPos.pos.getZ() + newStreetPos.gap + newStreetPos.width);
				if (!addNewUrbanStructure(type, EnumFacing.SOUTH, newStructurePos))
				{
					streetLen++;
					i--;
					continue;
				}
			}
			else
			{
				StructureBoundingBox streetBoundingBox = new StructureBoundingBox(new int[] {newStreetPos.pos.getX() - 1, STREETY, newStreetPos.pos.getZ() - 1, newStreetPos.pos.getX() + newStreetPos.width - 1 + 1, STREETY + 1, newStreetPos.pos.getZ() + streetLen - 1 + 1});
				if (StructurePandoraBase.findIntersectingPandora(structures, streetBoundingBox) != null)
				{
					break;
				}
				if (newStreetPos.pos.getZ() + streetLen + preStreetWidth(type) > startZ + TOWNSIZE)
				{
					break;
				}
				if (newStreetPos.pos.getX() + newStreetPos.gap + newStreetPos.width + preStreetLength(type) > startX + TOWNSIZE)
				{
					break;
				}
				Vec3i newStructurePos = new Vec3i(newStreetPos.pos.getX() + newStreetPos.gap + newStreetPos.width, 0, newStreetPos.pos.getZ() + streetLen);
				if (!addNewUrbanStructure(type, EnumFacing.EAST, newStructurePos))
				{
					streetLen++;
					i--;
					continue;
				}
			}
			
			if (i < curIndex + streetStructureSize - 1)
			{
				streetLen += gapSize[i - curIndex];
			}
			else
			{
				streetLen += newStreetPos.gap;
			}
			
			streetLen += structures.get(structures.size() - 1).facingStreetWidth();
		}
		
		if (i != curIndex)
		{
			addStreet(newStreetPos, streetLen);
			if (i < structureTypes.size())
			{
				i = buildStructsOtherSide(newStreetPos, streetLen, structureTypes, i);
			}
		}
		
		
		return i - curIndex;
	}
	
	private int buildStructsOtherSide(StreetPos newStreetPos, int streetLen, List<Urban> structureTypes, int curIndex)
	{
		int newIndex = curIndex;
		int curLen = newStreetPos.gap;
		
		while (curLen < streetLen && newIndex < structureTypes.size())
		{
			int gap = this.rand.nextInt(4) + 2;
			Urban type = structureTypes.get(newIndex);
			if (newStreetPos.direction == 1)
			{
				if (preStreetWidth(type) + curLen + gap > streetLen)
				{
					break;
				}
				if (newStreetPos.pos.getZ() - newStreetPos.gap - preStreetLength(type) < startZ - TOWNSIZE)
				{
					break;
				}
				Vec3i newStructurePos = new Vec3i(newStreetPos.pos.getX() + curLen, 0, newStreetPos.pos.getZ() - newStreetPos.gap - preStreetLength(type));
				if (!addNewUrbanStructure(type, EnumFacing.NORTH, newStructurePos))
				{
					curLen++;
					continue;
				}
				newIndex++;
			}
			else
			{
				if (preStreetWidth(type) + curLen + gap > streetLen)
				{
					break;
				}
				if (newStreetPos.pos.getX() - newStreetPos.gap - preStreetLength(type) < startX - TOWNSIZE)
				{
					break;
				}
				Vec3i newStructurePos = new Vec3i(newStreetPos.pos.getX() - newStreetPos.gap - preStreetLength(type), 0, newStreetPos.pos.getZ() + curLen);
				if (!addNewUrbanStructure(type, EnumFacing.WEST, newStructurePos))
				{
					curLen++;
					continue;
				}
				newIndex++;
			}

			curLen += gap;
			
			curLen += structures.get(structures.size() - 1).facingStreetWidth();
		}
		
		return newIndex;
	}
	
	private int[] initStructureSize()
	{
		int[] structureSizes = new int[Urban.typeMap.size()];
		structureSizes[Urban.VILLA.getValue()] = this.rand.nextInt(20) + 50;
		structureSizes[Urban.HOUSE.getValue()] = this.rand.nextInt(10) + 30;
		structureSizes[Urban.BARRACK.getValue()] = this.rand.nextInt(3) + 15;
		
		return structureSizes;
	}
	
	private StreetPos findNewStreetPos(int[] gapSize, List<Urban> structureTypes, int curIndex, int streetStructureSize, int width, int gap)
	{
		int streetLen = preCaculateStreetLen(gapSize, structureTypes, curIndex, streetStructureSize, gap);
		if (this.streetPoints.size() == 0)
		{
			int direction = this.rand.nextInt(2);
			if (direction == 1)
			{
				return new StreetPos(new Vec3i(startX - streetLen / 2, STREETY, startZ - 2), direction, width, gap, null);
			}
			else
			{
				return new StreetPos(new Vec3i(startX - 2, STREETY, startZ - streetLen / 2), direction, width, gap, null);
			}
		}
		else
		{
			int c = (this.rand.nextInt(this.streetPoints.size() * (this.streetPoints.size() + 1) / 2) + 1) * 2;
			int d = 1 + 4 * c;
			int streetPointIndex = this.streetPoints.size() - (int)((Math.sqrt(d * 1.0) - 1.0)/2.0);
			
			
			StreetPoint streetPoint = this.streetPoints.get(streetPointIndex);
			this.streetPoints.get(streetPointIndex).counter++;
			
			boolean successFlag = true;
			
			StreetPos newStreetPos = null;
			StructureBoundingBox streetBoundingBox = null;
			int x = 0;
			int y = 0;
			int z = 0;
			int newDirection = 0;
			
			if (streetPoint.direction == 1)
			{
				x = streetPoint.pos.getX() - width;
				y = streetPoint.pos.getY();
				z = streetPoint.pos.getZ() - streetLen / 2;
				if (streetPoint.leftFlag != 1)
				{
					x = streetPoint.pos.getX();
				}
				
				newDirection = 0;

				streetBoundingBox = new StructureBoundingBox(new int[] {x - 1, y, z - 1, x + width - 1 + 1, y + 1, z + streetLen - 1 + 1});
			}
			else
			{
				x = streetPoint.pos.getX() - streetLen / 2;
				y = streetPoint.pos.getY();
				z = streetPoint.pos.getZ() - width;
				if (streetPoint.leftFlag != 1)
				{
					z = streetPoint.pos.getZ();
				}
				
				newDirection = 1;

				streetBoundingBox = new StructureBoundingBox(new int[] {x - 1, y, z - 1, x + streetLen - 1 + 1, y + 1, z + width - 1 + 1});
			}
			
			if (newDirection == 1)
			{
				for (int i = curIndex; i < curIndex + streetStructureSize; i++)
				{
					if (z - gap - this.preStreetLength(structureTypes.get(i)) < startZ - TOWNSIZE)
					{
						successFlag = false;
						break;
					}
				}
			}
			else
			{
				for (int i = curIndex; i < curIndex + streetStructureSize; i++)
				{
					if (x - gap - this.preStreetLength(structureTypes.get(i)) < startX - TOWNSIZE)
					{
						successFlag = false;
						break;
					}
				}
			}
			
			if (streetBoundingBox.minX < startX - TOWNSIZE)
			{
				successFlag = false;
			}
			if (streetBoundingBox.minZ < startZ - TOWNSIZE)
			{
				successFlag = false;
			}
			if (streetBoundingBox.maxX > startX + TOWNSIZE)
			{
				successFlag = false;
			}
			if (streetBoundingBox.maxZ > startZ + TOWNSIZE)
			{
				successFlag = false;
			}
			if (StructurePandoraBase.findIntersectingPandora(structures, streetBoundingBox) != null)
			{
				successFlag = false;
			}

			if (successFlag)
			{
				newStreetPos = new StreetPos(new Vec3i(x, y, z), newDirection, width, gap, streetPoint.cur);
			}
			
			if (successFlag || this.streetPoints.get(streetPointIndex).counter >= 50)
			{
				this.streetPoints.remove(streetPointIndex);
			}
			
			return newStreetPos;
		}
	}
	
	private void addStreet(StreetPos streetPos, int streetLen)
	{
		IBlockState material = Blocks.STONE_SLAB.getDefaultState().withProperty(BlockStoneSlab.VARIANT, BlockStoneSlab.EnumType.COBBLESTONE).withProperty(BlockSlab.HALF, BlockSlab.EnumBlockHalf.BOTTOM);
		StructureBoundingBox streetBoundingBox;
		if (streetPos.direction == 1)
		{
			streetBoundingBox = new StructureBoundingBox(new int[] {streetPos.pos.getX(), STREETY, streetPos.pos.getZ(), streetPos.pos.getX() + streetLen - 1, STREETY, streetPos.pos.getZ() + streetPos.width - 1});
			StructureStreet street = new StructureStreet(1, streetBoundingBox, material);
			if (streetPos.prev != null)
			{
				street.addNeighbor(streetPos.prev);
				streetPos.prev.addNeighbor(street);
			}
			streets.add(street);
			this.streetPoints.add(new StreetPoint(new Vec3i(streetBoundingBox.minX, streetBoundingBox.minY, streetBoundingBox.minZ), streetPos.direction, 1, streetPos.width, street));
			this.streetPoints.add(new StreetPoint(new Vec3i(streetBoundingBox.maxX, streetBoundingBox.maxY, streetBoundingBox.minZ), streetPos.direction, 0, streetPos.width, street));
		}
		else
		{
			streetBoundingBox = new StructureBoundingBox(new int[] {streetPos.pos.getX(), STREETY, streetPos.pos.getZ(), streetPos.pos.getX() + streetPos.width - 1, STREETY, streetPos.pos.getZ() + streetLen - 1});
			StructureStreet street = new StructureStreet(1, streetBoundingBox, material);
			if (streetPos.prev != null)
			{
				street.addNeighbor(streetPos.prev);
				streetPos.prev.addNeighbor(street);
			}
			streets.add(street);
			this.streetPoints.add(new StreetPoint(new Vec3i(streetBoundingBox.minX, streetBoundingBox.minY, streetBoundingBox.minZ), streetPos.direction, 1, streetPos.width, street));
			this.streetPoints.add(new StreetPoint(new Vec3i(streetBoundingBox.minX, streetBoundingBox.maxY, streetBoundingBox.maxZ), streetPos.direction, 0, streetPos.width, street));
		}
		
	}
	
	public void populate(World world)
	{
		for (StructurePandoraBase structure : structures)
		{
	        if (StructNBT.compounds.containsKey(structure.getStructType()))
	        {
	        	NBTTagList blocks = StructNBT.compounds.get(structure.getStructType()).getTagList("blocks", 10);
	        	

	        	NBTTagList palette = StructNBT.compounds.get(structure.getStructType()).getTagList("palette", 10);
	        	IBlockState[] states = new IBlockState[palette.tagCount()];
	        	
	        	for (int i = 0; i < palette.tagCount(); i++)
	        	{
	        		states[i] = NBTUtil.readBlockState(palette.getCompoundTagAt(i));
	        	}
	        	
	        	structure.generateEntities(world, rand, blocks, states);
	        }
		}
	}
	
	public void clear()
	{
		this.structures.clear();
		this.streets.clear();
	}
	
	public List<StructurePandoraBase> getStructures()
	{
		return this.structures;
	}

	public List<StructureStreet> getStreets()
	{
		return streets;
	}
}
