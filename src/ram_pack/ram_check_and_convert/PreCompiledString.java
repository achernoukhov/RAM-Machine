package ram_pack.ram_check_and_convert;

public class PreCompiledString
{
	public String str;	//строка с данными
	public int startIndex;	//начало данных(для подсветки и генерации кода после подстановки макросов)
	public int endIndex;	//конец данных(для подсветки и генерации кода после подстановки макросов)
	public CodeSubstringType type;	//тип строки

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
