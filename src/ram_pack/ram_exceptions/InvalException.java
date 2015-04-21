package ram_pack.ram_exceptions;

public class InvalException extends RAMException
{
	public InvalException()
	{
		super();
	}
	public InvalException(String message)
	{
		super(message);
	}
	public InvalException(String message,int line)
	{
		super(message, line);
	}
	public InvalException(Throwable cause)
	{
		super(cause);
	}
}
