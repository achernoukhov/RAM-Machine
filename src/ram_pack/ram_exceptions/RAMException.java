package ram_pack.ram_exceptions;

public class RAMException extends Exception
{
	private int errorLine=-1;
	public RAMException()
	{
		super();
	}
	public RAMException(String message)
	{
		super(message);
	}
	public RAMException(String message,int line)
	{
		super(message);
		errorLine=line;
	}
	public RAMException(Throwable cause)
	{
		super(cause);
	}

	public int getErrorLine()
	{
		return errorLine;
	}
}
