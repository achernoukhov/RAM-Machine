package ram_pack.ram_check_and_convert;

public class PreCompiledString
{
	public String str;	//������ � �������
	public int startIndex;	//������ ������(��� ��������� � ��������� ���� ����� ����������� ��������)
	public int endIndex;	//����� ������(��� ��������� � ��������� ���� ����� ����������� ��������)
	public CodeSubstringType type;	//��� ������

	public PreCompiledString(String iStr,int iStartIndex,int iEndIndex,CodeSubstringType iType)
	{
		str=iStr;
		startIndex=iStartIndex;
		endIndex=iEndIndex;
		type=iType;
	}
//	public PreCompiledString(int iStartIndex,int iEndIndex,CodeSubstringType iType)
////		this("", iStartIndex, iEndIndex, iType);
	//}
}
