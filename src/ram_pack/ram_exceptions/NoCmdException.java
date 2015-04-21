package ram_pack.ram_exceptions;

public class NoCmdException extends RAMException
{
	public NoCmdException()
	{
		super();
	}
	public NoCmdException(String message)
	{
		super(message);
	}
	public NoCmdException(String message,int line)
	{
		super(message, line);
	}
	public NoCmdException(Throwable cause)
	{
		super(cause);
	}
}
