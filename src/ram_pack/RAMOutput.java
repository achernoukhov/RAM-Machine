package ram_pack;

import ram_pack.ram_core.Writeable;

import java.util.LinkedList;
import java.util.Collection;

/**
 * Created by IntelliJ IDEA.
 * User: Саня
 * Date: 27.03.2008
 * Time: 21:43:44
 * To change this template use File | Settings | File Templates.
 */
public class RAMOutput extends LinkedList<String> implements Writeable
{
	boolean isLastAddedChar=false;

	public RAMOutput()
	{
		super();
	}

	public RAMOutput(Collection<String> c)
	{
		super(c);
	}

	public void writeNext(Integer n)
	{
		addLast(n.toString());
		isLastAddedChar=false;
	}

	public void writeNext(char c)
	{
		String last;
		if (isLastAddedChar)
		{
			last=getLast();
			removeLast();
			last=last.concat(Character.toString(c));

		}
		else
		{
			last=Character.toString(c);
		}
		addLast(last);
		isLastAddedChar=true;
	}

	@Override
	public void clear() {
		super.clear();
		isLastAddedChar=false;
	}
}
