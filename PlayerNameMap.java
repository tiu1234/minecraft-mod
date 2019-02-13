package net.minecraft.entity.player.ai.logic;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;

public final class PlayerNameMap
{
	private static ArrayList<String> names = new ArrayList<>();
	
	private static Map<String, EntityPlayer> playerMap = new HashMap<String, EntityPlayer>();
	
	public static void readNames(long seed)
	{
		try
		{
			FileReader fileReader = new FileReader(System.getProperty("user.dir") + "\\names\\male_names.txt");
			BufferedReader bufferReader = new BufferedReader(fileReader);
			
			String line;
			while ((line = bufferReader.readLine()) != null)
			{
				String[] namesInLine = line.split("\\s+");
				if (namesInLine.length > 0)
				{
					for (String name : namesInLine)
					{
						names.add(name);
					}
				}
			}
			
			bufferReader.close();
			fileReader.close();
		}
		catch(Exception e)
		{
			System.err.print(e.toString());
		}
		
		Collections.shuffle(names, new Random(seed));
	}
	
	public static String getRandomName()
	{
		return names.get(playerMap.size());
	}
	
	public static void addPlayer(EntityPlayer player)
	{
		playerMap.put(player.getName(), player);
	}
	
	public static EntityPlayer getPlayerByName(String playerName)
	{
		if (playerName.contains(playerName))
		{
			return playerMap.get(playerName);
		}
		return null;
	}
	
	public static boolean existPlayer(String playerName)
	{
		return playerMap.containsKey(playerName);
	}
	
	public static void clear()
	{
		playerMap.clear();
	}
	
	public static void clearNames()
	{
		names.clear();
	}
}
