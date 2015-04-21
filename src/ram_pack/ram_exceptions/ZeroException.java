package ram_pack.ram_exceptions;

public class ZeroException extends RAMException
{
	public ZeroException()
	{
		super();
	}
	public ZeroException(String message)
	{
		super(message);
	}
	public ZeroException(String message,int line)
	{
		super(message, line);
	}
	public ZeroException(Throwable cause)
	{
		super(cause);
	}
}
