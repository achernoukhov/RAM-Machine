package ram_pack.ram_core;

/**
 * Created by IntelliJ IDEA.
 * User: ����
 * Date: 23.03.2008
 * Time: 17:57:18
 * To change this template use File | Settings | File Templates.
 */


public class ArgFormatStruct // ������ ���������� ��������
{
	public boolean labelPermit; // ����� �� ���� ������
	public boolean primitivePermit; // ����� �� ���� ����������� ������ (��������� ������ "=")
	public boolean referencePermit; // ����� �� ���� ��������� ������� (��������� ������� "*")
	public int numFormat; // ������ ����� (int numFormatInt, long numFormatLong, double numFormatDouble)
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
	
	public static ArgFormatStruct argLabelInt()	// ������ ��� ����������-����� (��� �������� int)
	{
		return new ArgFormatStruct(true,false,false,NumFormatInt);
	}
	
	public static ArgFormatStruct argPrimitiveReferenceInt() // ������ ��� ����������� ���������� ��� ������ (��� �������� int)
	{
		return new ArgFormatStruct(false,true,true,NumFormatInt);
	}

	public static ArgFormatStruct argPrimitiveInt() // ������ ��� ����������� ���������� (��� �������� int)
	{
		return new ArgFormatStruct(false,true,false,NumFormatInt);
	}

	public static ArgFormatStruct argReferenceInt() // ������ ��� ������ (��� �������� int)
	{
		return new ArgFormatStruct(false,false,true,NumFormatInt);
	}

	public static ArgFormatStruct argNumInt() // ������ ��� ������� ����� (��� �������� int)
	{
		return new ArgFormatStruct(false,false,false,NumFormatInt);
	}
}
