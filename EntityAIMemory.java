package net.minecraft.entity.player.ai.logic;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.math.Vec3d;

public class EntityAIMemory
{
	private int identity;
	
	private Map<String, Integer> relationships;
	
	private Set<String> superiors;
	
	private Set<String> subordinates;
	
	private Map<String, Vec3d[][]> locations;
	
	public EntityAIMemory()
	{
		identity = 0;
		relationships = Maps.<String, Integer>newHashMap();
		superiors = Sets.<String>newLinkedHashSet();
		subordinates = Sets.<String>newLinkedHashSet();
		locations = Maps.<String, Vec3d[][]>newHashMap();
	}
	
	public void addLocation(String key, Vec3d[][] location)
	{
		locations.put(key, location);
	}
	
	public Vec3d[][] getLocation(String key)
	{
		if (locations.containsKey(key))
		{
			return locations.get(key);
		}
		
		return null;
	}
	
	public void removeLocation(String key)
	{
		if (locations.containsKey(key))
		{
			locations.remove(key);
		}
	}

	public void readFromNBT(NBTTagCompound compound)
	{
		String[] temp = new String[1];
		
		if (compound.hasKey("identity"))
		{
			identity = compound.getInteger("identity");
		}

		if (compound.hasKey("superiors"))
		{
			NBTTagList superiorData = compound.getTagList("superiors", 8);
			for (int i = 0; i < superiorData.tagCount(); i++)
			{
				String data = superiorData.getStringTagAt(i);
				superiors.add(data);
			}
		}

		if (compound.hasKey("subordinates"))
		{
			NBTTagList subordinateData = compound.getTagList("superiors", 8);
			for (int i = 0; i < subordinateData.tagCount(); i++)
			{
				String data = subordinateData.getStringTagAt(i);
				subordinates.add(data);
			}
		}

		if (compound.hasKey("relationships"))
		{
			NBTTagList relationshipData = compound.getTagList("relationships", 10);
			for (int i = 0; i < relationshipData.tagCount(); i++)
			{
				NBTTagCompound data = relationshipData.getCompoundTagAt(i);
				Set<String> keys = data.getKeySet();
				String key = keys.toArray(temp)[0];
				relationships.put(key, data.getInteger(key));
			}
		}
		
		if (compound.hasKey("locations"))
		{
			NBTTagCompound locationCompund = compound.getCompoundTag("locations");
			for (String key : locationCompund.getKeySet())
			{
				locations.put(key, EntityAIJob.read2dFromNBT(key, locationCompund));
			}
		}
	}
	
	public void writeEntityToNBT(NBTTagCompound compound)
	{
		String[] temp = new String[1];
		
		compound.setInteger("identity", identity);
		
		if (superiors.size() > 0)
		{
			NBTTagList superiorData = new NBTTagList();
			for (String superior: superiors.toArray(temp))
			{
				NBTTagString data = new NBTTagString(superior);
				superiorData.appendTag(data);
			}
			compound.setTag("superiors", superiorData);
		}

		if (subordinates.size() > 0)
		{
			NBTTagList subordinateData = new NBTTagList();
			for (String subordinate: subordinates.toArray(temp))
			{
				NBTTagString data = new NBTTagString(subordinate);
				subordinateData.appendTag(data);
			}
			compound.setTag("subordinates", subordinateData);
		}
		
		if (relationships.size() > 0)
		{
			NBTTagList relationshipData = new NBTTagList();
			for (Entry<String, Integer> relationship : relationships.entrySet())
			{
	            NBTTagCompound data = new NBTTagCompound();
	            data.setInteger(relationship.getKey(), relationship.getValue());
				relationshipData.appendTag(data);
			}
			compound.setTag("relationships", relationshipData);
		}
		
		if (locations.size() > 0)
		{
			NBTTagCompound locationsCompund = new NBTTagCompound();
			
			for (Entry<String, Vec3d[][]> element : locations.entrySet())
			{
				EntityAIJob.add2dToNBT(element.getKey(), element.getValue(), locationsCompund);
			}
			
			compound.setTag("locations", locationsCompund);
		}
	}

	public int getIdentity()
	{
		return identity;
	}

	public void setIdentity(int identity)
	{
		this.identity = identity;
	}

	public Map<String, Integer> getRelationships()
	{
		return relationships;
	}

	public void setRelationships(Map<String, Integer> relationships)
	{
		this.relationships = relationships;
	}

	public Set<String> getSuperiors()
	{
		return superiors;
	}

	public void setSuperiors(Set<String> superiors)
	{
		this.superiors = superiors;
	}

	public Set<String> getSubordinates()
	{
		return subordinates;
	}

	public void setSubordinates(Set<String> subordinates)
	{
		this.subordinates = subordinates;
	}
	
	public void modifyRelation(String name, int offset)
	{
		if (this.relationships.containsKey(name))
		{
			int value = this.relationships.get(name) + offset;
			this.relationships.put(name, value);
		}
		else
		{
			this.relationships.put(name, offset);
		}
	}
	
	public void setRelation(String name, int value)
	{
		this.relationships.put(name, value);
	}
}
