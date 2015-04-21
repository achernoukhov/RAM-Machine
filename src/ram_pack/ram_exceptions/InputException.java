package ram_pack.ram_exceptions;

public class InputException extends RAMException
{
	public InputException()
	{
		super();
	}
	public InputException(String message)
	{
		super(message);
	}
	public InputException(String message,int line)
	{
		super(message, line);
	}
	public InputException(Throwable cause)
	{
		super(cause);
	}
}
