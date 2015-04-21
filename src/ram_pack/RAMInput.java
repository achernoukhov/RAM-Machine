package ram_pack;

import java.util.LinkedList;
import java.util.Collection;

public class RAMInput extends LinkedList<Integer> implements ram_pack.ram_core.Readable
{
	public RAMInput()
	{
		super();
	}

	public RAMInput(Collection<Integer> c)
	{
		super(c);
	}

	public Integer readNext()
	{
		return poll();
	}
}
