package ram_pack.ram_core;

import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: ����
 * Date: 23.03.2008
 * Time: 19:14:08
 * To change this template use File | Settings | File Templates.
 */

public class CmdStruct // ��������
{
	public String cmdName; // ��� ��������
	public ArrayList<Argument> argList; // ������ ���������� Argument
	public CmdStruct(){}
	public CmdStruct(String name, ArrayList<Argument> list)
	{
		cmdName=name;
		argList=new ArrayList<Argument>(list);
	}
	public CmdStruct(String name, Argument arg)
	{
		cmdName=name;
		ArrayList<Argument> list=new ArrayList<Argument>();
		list.add(arg);
		argList=list;
	}
}
