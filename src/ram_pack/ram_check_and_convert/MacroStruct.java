package ram_pack.ram_check_and_convert;

import java.util.ArrayList;

public class MacroStruct
{
	public String macroName;
	public ArrayList<PreCompiledLine> preCompiledLines;
	public MacroStruct(String macroName,ArrayList<PreCompiledLine> preCompiledLines)
	{
		this.macroName=macroName;
		this.preCompiledLines=preCompiledLines;
	}
	public String getTranslatedCode(ArrayList<String> usedLabels)
	{
		ArrayList<String> labelsToAdd=new ArrayList<String>();
		String result="";
		for(PreCompiledLine item: preCompiledLines)
		{
			result+=item.createStr(usedLabels,labelsToAdd);
		}
		usedLabels.addAll(labelsToAdd);
		return result;
	}
	
}
