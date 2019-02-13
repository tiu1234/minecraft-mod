package net.minecraft.world.gen.structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.template.TemplateManager;

public class StructureStreet extends StructureComponent
{
    protected int averageGroundLvl = -1;
    private IBlockState material;
    private List<StructureStreet> neighbors;
    
    public StructureStreet()
    {
    	this.neighbors = new ArrayList<>();
    }

	public StructureStreet(int type, StructureBoundingBox boundingBoxIn, IBlockState material)
	{
		super(type);
		
		this.boundingBox = boundingBoxIn;
		this.material = material;
		this.neighbors = new ArrayList<>();
	}

	@Override
	protected void writeStructureToNBT(NBTTagCompound tagCompound)
	{
		
	}

	@Override
	protected void readStructureFromNBT(NBTTagCompound tagCompound, TemplateManager p_143011_2_)
	{
		
	}

    public static StructureStreet findIntersectingStreet(List<StructureStreet> listIn, StructureBoundingBox boundingboxIn)
    {
        for (StructureStreet structurecomponent : listIn)
        {
            if (structurecomponent.getBoundingBox() != null && structurecomponent.getBoundingBox().intersectsWith(boundingboxIn))
            {
                return structurecomponent;
            }
        }

        return null;
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

            this.boundingBox.offset(0, this.averageGroundLvl - this.boundingBox.maxY - 1, 0);
        }

        if (this.material != null)
        {
        	this.fillWithBlocks(worldIn, structureBoundingBoxIn, this.boundingBox.minX, this.boundingBox.minY, this.boundingBox.minZ, this.boundingBox.maxX, this.boundingBox.maxY, this.boundingBox.maxZ, this.material, this.material, false);
        }
		
		return true;
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
    
    public void addNeighbor(StructureStreet street)
    {
    	if (street != null)
    	{
        	this.neighbors.add(street);
    	}
    }

	public List<StructureStreet> getNeighbors()
	{
		return neighbors;
	}
}
