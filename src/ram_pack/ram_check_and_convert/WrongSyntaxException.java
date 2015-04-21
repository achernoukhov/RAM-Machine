package ram_pack.ram_check_and_convert;

import ram_pack.ram_exceptions.RAMException;

public class WrongSyntaxException extends RAMException
{
	public WrongSyntaxException()
	{
		super();
	}
	public WrongSyntaxException(String message)
	{
		super(message);
	}
	public WrongSyntaxException(String message, int line)
	{
		super(message,line);
	}
	public WrongSyntaxException(Throwable cause)
	{
		super(cause);
	}
}
