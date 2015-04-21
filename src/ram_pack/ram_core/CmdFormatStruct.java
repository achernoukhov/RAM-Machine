package ram_pack.ram_core;

import ram_pack.ram_exceptions.RAMException;

import java.util.ArrayList;

public class CmdFormatStruct // ������ ��������
{
	private RAM.Runner runner; /* �����, ����������� ��������: ����� ������� ��������� �����
									���� Runner � ���������������� ������� run() */
	private ArrayList<ArgFormatStruct> argFormats; // ������ �������� ���������� ArgFormatStruct

	public CmdFormatStruct(RAM.Runner newRunner, ArrayList<ArgFormatStruct> formats)
	{
		runner=newRunner;
		argFormats=new ArrayList<ArgFormatStruct>(formats);
	}

	public ArrayList<ArgFormatStruct> getArgFormats() // ���������� ������� ���������� ��� ������ ��������
	{
		return (ArrayList<ArgFormatStruct>)argFormats.clone();
	}
	
	public void runCmd() throws RAMException // ��������� ���������� ������� (runner)
	{
		runner.run();
	}
}
