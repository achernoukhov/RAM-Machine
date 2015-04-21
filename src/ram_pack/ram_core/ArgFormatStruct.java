package ram_pack.ram_core;

/**
 * Created by IntelliJ IDEA.
 * User: Саня
 * Date: 23.03.2008
 * Time: 17:57:18
 * To change this template use File | Settings | File Templates.
 */


public class ArgFormatStruct // Формат аргументов комманды
{
	public boolean labelPermit; // может ли быть меткой
	public boolean primitivePermit; // может ли быть примитивным числом (содержать символ "=")
	public boolean referencePermit; // может ли быть косвенной ссылкой (содержать символы "*")
	public int numFormat; // формат числа (int numFormatInt, long numFormatLong, double numFormatDouble)
	public static int NumFormatInt=0;
	public static int NumFormatLong=1;
	public static int NumFormatDouble=2;
	
	public ArgFormatStruct (boolean label,boolean primitive,boolean reference,int num)
	{
		labelPermit=label;
		primitivePermit=primitive;
		referencePermit=reference;
		numFormat=num;
	}
	
	public static ArgFormatStruct argLabelInt()	// формат для аргументов-меток (тип значения int)
	{
		return new ArgFormatStruct(true,false,false,NumFormatInt);
	}
	
	public static ArgFormatStruct argPrimitiveReferenceInt() // формат для примитивных аргументов или ссылок (тип значения int)
	{
		return new ArgFormatStruct(false,true,true,NumFormatInt);
	}

	public static ArgFormatStruct argPrimitiveInt() // формат для примитивных аргументов (тип значения int)
	{
		return new ArgFormatStruct(false,true,false,NumFormatInt);
	}

	public static ArgFormatStruct argReferenceInt() // формат для ссылок (тип значения int)
	{
		return new ArgFormatStruct(false,false,true,NumFormatInt);
	}

	public static ArgFormatStruct argNumInt() // формат для обычных чисел (тип значения int)
	{
		return new ArgFormatStruct(false,false,false,NumFormatInt);
	}
}
