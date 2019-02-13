package net.minecraft.entity.player.ai.logic;

import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;

public class NPCInventoryCrafting extends InventoryCrafting
{

	public NPCInventoryCrafting(Container eventHandlerIn, int width, int height)
	{
		super(eventHandlerIn, width, height);
	}

	@Override
    public void setInventorySlotContents(int index, ItemStack stack)
    {
        this.stackList.set(index, stack);
    }
}
