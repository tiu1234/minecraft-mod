package net.minecraft.world.gen.structure;

public class StructureTownComponent
{
	
    public static void registerTownComponents()
    {
        MapGenStructureIO.registerStructureComponent(StructureVilla.class, "PANVILLA");
        MapGenStructureIO.registerStructureComponent(StructureFarm.class, "PANFARM");
        MapGenStructureIO.registerStructureComponent(StructureHouse.class, "PANHOUSE");
        MapGenStructureIO.registerStructureComponent(StructureBarrack.class, "PANBARRACK");
        MapGenStructureIO.registerStructureComponent(StructureStreet.class, "PANSTREET");
    }
}
