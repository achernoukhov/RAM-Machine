package ram_pack.ram_check_and_convert;

import java.util.LinkedList;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class PreCompiledLine
{
	public boolean isMacroString=false;	//содержитс€ ли в строке объ€вление или окончание объ€вление макроса
	public boolean isMacroStart=false;	//€вл€етс€ ли строка объ€влением макроса
	public boolean isMacroEnd=false;	//€вл€етс€ ли строка окончанием объ€влени€ макроса
	private LinkedList<PreCompiledString> stringData;
	public int startOfLineOffset;
	public int numberOfArguments=0;
	public boolean invalidNumberOfParametres=false;

	private void removeEmptyStrings()
	{
		LinkedList<PreCompiledString> newStringData=new LinkedList<PreCompiledString>();
		for(PreCompiledString curObj:stringData)
		{
			if(curObj.str.length()!=0)
			{
				newStringData.add(curObj);
			}
		}
		stringData=newStringData;
	}
	private void constructorForMacro(String line,int iStartOfLineOffset)
	{
		stringData=new LinkedList<PreCompiledString>();
		startOfLineOffset=iStartOfLineOffset;
		String lineWithoutComment;
        int commentOffset=line.indexOf("//");
		/*Matcher commentFilter= Pattern.compile("(.*)(//.*)").matcher(line);
		if(commentFilter.matches())
		{
			//надо коммент выделить и не учитывать его далее
			lineWithoutComment=commentFilter.group(1);
			stringData.add(new PreCompiledString(commentFilter.group(2),commentFilter.start(2),commentFilter.end(2), CodeSubstringType.COMMENT));	//добавл€ем подсветку комментари€
		}
		else
			lineWithoutComment=line;
			*/
        if(commentOffset==-1)
        {
            lineWithoutComment=line;
        }
        else
        {
            lineWithoutComment=line.substring(0,commentOffset);
            String comment=line.substring(commentOffset);
            stringData.add(new PreCompiledString(comment,commentOffset,line.length(), CodeSubstringType.COMMENT));	//добавл€ем подсветку комментари€
        }

		if(lineWithoutComment.matches("\\s+"))
		{
			stringData.add(new PreCompiledString(lineWithoutComment,0,lineWithoutComment.length(), CodeSubstringType.SPACES_AND_TABULATIONS));
			removeEmptyStrings();
			return;
		}

		Matcher macroFilter= Pattern.compile("(\\s*)"+ SyntaxChecker.macroStartString+"(\\s*)([^\\s](.*)|)").matcher(lineWithoutComment);
		if(macroFilter.matches())
		{
			//разбор макро-строки
			isMacroString=true;
			isMacroStart=true;
			stringData.add(new PreCompiledString(macroFilter.group(1),macroFilter.start(1),macroFilter.end(1), CodeSubstringType.SPACES_AND_TABULATIONS));
			stringData.add(new PreCompiledString(macroFilter.group(2),macroFilter.start(2),macroFilter.end(2), CodeSubstringType.MACRO_DEFINITION_START));
			stringData.add(new PreCompiledString(macroFilter.group(3),macroFilter.start(3),macroFilter.end(3), CodeSubstringType.SPACES_AND_TABULATIONS));
			Matcher macroCommandFilter= Pattern.compile("([^\\s]+)(\\s*)(.*)").matcher(macroFilter.group(4));//SyntaxChecker.commandFormatPattern + "(\\s*)"/*+arguments*/).matcher(macroFilter.group(4));
			if(macroCommandFilter.matches())
			{
				if(macroCommandFilter.group(1).matches(SyntaxChecker.commandFormatPattern))
				{
					stringData.add(new PreCompiledString(macroCommandFilter.group(1),macroFilter.start(4)+macroCommandFilter.start(1),macroFilter.start(4)+macroCommandFilter.end(1), CodeSubstringType.MACRO_NAME_DEFINITION));
				}
				else
				{
					stringData.add(new PreCompiledString(macroCommandFilter.group(1),macroFilter.start(4)+macroCommandFilter.start(1),macroFilter.start(4)+macroCommandFilter.end(1), CodeSubstringType.INVALID_MACRO_NAME));
				}
				stringData.add(new PreCompiledString(macroCommandFilter.group(2),macroFilter.start(4)+macroCommandFilter.start(2),macroFilter.start(4)+macroCommandFilter.end(2), CodeSubstringType.SPACES_AND_TABULATIONS));
				stringData.add(new PreCompiledString(macroCommandFilter.group(3),macroFilter.start(4)+macroCommandFilter.start(3),macroFilter.start(4)+macroCommandFilter.end(3), CodeSubstringType.TRASH));
			}
			removeEmptyStrings();
			return;
		}

		Matcher macroEndFilter= Pattern.compile("(\\s*)"+ SyntaxChecker.macroEndString+"(\\s*)").matcher(lineWithoutComment);
		if(macroEndFilter.matches())
		{
			//разбор макро-строки
			isMacroString=true;
			isMacroEnd=true;
			stringData.add(new PreCompiledString(macroEndFilter.group(1),macroEndFilter.start(1),macroEndFilter.end(1), CodeSubstringType.SPACES_AND_TABULATIONS));
			stringData.add(new PreCompiledString(macroEndFilter.group(2),macroEndFilter.start(2),macroEndFilter.end(2), CodeSubstringType.MACRO_DEFINITION_END));
			stringData.add(new PreCompiledString(macroEndFilter.group(3),macroEndFilter.start(3),macroEndFilter.end(3), CodeSubstringType.SPACES_AND_TABULATIONS));
			removeEmptyStrings();
			return;
		}

		String lineWithoutCommentAndLabel=lineWithoutComment;
		int LabelEndOffset=0;
		Matcher labelFilter= Pattern.compile("(\\s*)"+ SyntaxChecker.labelFormatPattern+"(:)(\\s*)(.*)").matcher(lineWithoutComment);
		if(labelFilter.matches())
		{
			//разбор меток
			stringData.add(new PreCompiledString(labelFilter.group(1),labelFilter.start(1),labelFilter.end(1), CodeSubstringType.SPACES_AND_TABULATIONS));
			stringData.add(new PreCompiledString(labelFilter.group(2),labelFilter.start(2),labelFilter.end(2), CodeSubstringType.LABEL_DEFINITION));
			stringData.add(new PreCompiledString(labelFilter.group(3),labelFilter.start(3),labelFilter.end(3), CodeSubstringType.LABEL_TWO_POINTS));
			stringData.add(new PreCompiledString(labelFilter.group(4),labelFilter.start(4),labelFilter.end(4), CodeSubstringType.SPACES_AND_TABULATIONS));
			LabelEndOffset=labelFilter.start(5);
			lineWithoutCommentAndLabel=labelFilter.group(5);
		}
		String lineWithArguments;
		int ArgumentsOffset;
		if(lineWithoutCommentAndLabel.length()==0)
		{
			removeEmptyStrings();
			return;
		}

		Matcher commandFilter= Pattern.compile("(\\s*)"+SyntaxChecker.commandFormatPattern+"(\\s*)(([^\\s].*)|)").matcher(lineWithoutCommentAndLabel);
		if(commandFilter.matches())
		{
			//разбор команды
			stringData.add(new PreCompiledString(commandFilter.group(1),LabelEndOffset+commandFilter.start(1),LabelEndOffset+commandFilter.end(1), CodeSubstringType.SPACES_AND_TABULATIONS));
			stringData.add(new PreCompiledString(commandFilter.group(2),LabelEndOffset+commandFilter.start(2),LabelEndOffset+commandFilter.end(2), CodeSubstringType.COMMAND));
			stringData.add(new PreCompiledString(commandFilter.group(3),LabelEndOffset+commandFilter.start(3),LabelEndOffset+commandFilter.end(3), CodeSubstringType.SPACES_AND_TABULATIONS));
			if(commandFilter.group(5)!=null)
			{
				ArgumentsOffset=LabelEndOffset+commandFilter.start(5);
				lineWithArguments=commandFilter.group(5);
			}
			else
			{
				removeEmptyStrings();
				return;
			}
		}
		else
		{
			stringData.add(new PreCompiledString(lineWithoutCommentAndLabel,LabelEndOffset,LabelEndOffset+lineWithoutCommentAndLabel.length(), CodeSubstringType.TRASH));
			removeEmptyStrings();
			return;
		}

		String strForOneParameter="("+ SyntaxChecker.argumentFormatPattern+"|"+ SyntaxChecker.labelFormatPattern+")";
		Pattern argPattern=Pattern.compile(strForOneParameter+"(\\s*)(.*)");
		int paramLengthInGroups=Pattern.compile(strForOneParameter).matcher("").groupCount();
		Matcher argumentFilter= argPattern.matcher(lineWithArguments);
		while(argumentFilter.matches())
		{
			stringData.add(new PreCompiledString(argumentFilter.group(1),ArgumentsOffset+argumentFilter.start(1),ArgumentsOffset+argumentFilter.end(1), CodeSubstringType.ARGUMENT));
			stringData.add(new PreCompiledString(argumentFilter.group(paramLengthInGroups+1),ArgumentsOffset+argumentFilter.start(paramLengthInGroups+1),ArgumentsOffset+argumentFilter.end(paramLengthInGroups+1), CodeSubstringType.SPACES_AND_TABULATIONS));
			ArgumentsOffset+=argumentFilter.start(paramLengthInGroups+2);
			lineWithArguments=argumentFilter.group(paramLengthInGroups+2);
			argumentFilter=argPattern.matcher(lineWithArguments);
		}
		stringData.add(new PreCompiledString(lineWithArguments,ArgumentsOffset,ArgumentsOffset+lineWithArguments.length(), CodeSubstringType.TRASH));
		numberOfArguments=this.getNumberOf(CodeSubstringType.ARGUMENT);
		removeEmptyStrings();
	}
	public PreCompiledLine(String line,int iStartOfLineOffset)
	{
		stringData=new LinkedList<PreCompiledString>();
		startOfLineOffset=iStartOfLineOffset;
		String lineWithoutComment;
        int commentOffset=line.indexOf("//");
		/*Matcher commentFilter= Pattern.compile("(.*)(//.*)").matcher(line);
		if(commentFilter.matches())
		{
			//надо коммент выделить и не учитывать его далее
			lineWithoutComment=commentFilter.group(1);
			stringData.add(new PreCompiledString(commentFilter.group(2),commentFilter.start(2),commentFilter.end(2), CodeSubstringType.COMMENT));	//добавл€ем подсветку комментари€
		}
		else
			lineWithoutComment=line;
			*/
        if(commentOffset==-1)
        {
            lineWithoutComment=line;
        }
        else
        {
            lineWithoutComment=line.substring(0,commentOffset);
            String comment=line.substring(commentOffset);
            stringData.add(new PreCompiledString(comment,commentOffset,line.length(), CodeSubstringType.COMMENT));	//добавл€ем подсветку комментари€
        }

		if(lineWithoutComment.matches("\\s+"))
		{
			stringData.add(new PreCompiledString(lineWithoutComment,0,lineWithoutComment.length(), CodeSubstringType.SPACES_AND_TABULATIONS));
			removeEmptyStrings();
			return;
		}

		String lineWithoutCommentAndLabel=lineWithoutComment;
		int LabelEndOffset=0;
		Matcher labelFilter= Pattern.compile("(\\s*)"+ SyntaxChecker.labelFormatPattern+"(:)(\\s*)(.*)").matcher(lineWithoutComment);
		if(labelFilter.matches())
		{
			//разбор меток
			stringData.add(new PreCompiledString(labelFilter.group(1),labelFilter.start(1),labelFilter.end(1), CodeSubstringType.SPACES_AND_TABULATIONS));
			stringData.add(new PreCompiledString(labelFilter.group(2),labelFilter.start(2),labelFilter.end(2), CodeSubstringType.LABEL_DEFINITION));
			stringData.add(new PreCompiledString(labelFilter.group(3),labelFilter.start(3),labelFilter.end(3), CodeSubstringType.LABEL_TWO_POINTS));
			stringData.add(new PreCompiledString(labelFilter.group(4),labelFilter.start(4),labelFilter.end(4), CodeSubstringType.SPACES_AND_TABULATIONS));
			LabelEndOffset=labelFilter.start(5);
			lineWithoutCommentAndLabel=labelFilter.group(5);
		}
		String lineWithArguments;
		int ArgumentsOffset;
		if(lineWithoutCommentAndLabel.length()==0)
		{
			removeEmptyStrings();
			return;
		}

		Matcher commandFilter= Pattern.compile("(\\s*)"+SyntaxChecker.commandFormatPattern+"(|((\\s+)(|([^\\s].*))))").matcher(lineWithoutCommentAndLabel);

		if(commandFilter.matches())
		{
			//разбор команды
			stringData.add(new PreCompiledString(commandFilter.group(1),LabelEndOffset+commandFilter.start(1),LabelEndOffset+commandFilter.end(1), CodeSubstringType.SPACES_AND_TABULATIONS));
			stringData.add(new PreCompiledString(commandFilter.group(2),LabelEndOffset+commandFilter.start(2),LabelEndOffset+commandFilter.end(2), CodeSubstringType.COMMAND));
			if(commandFilter.group(5)!=null)
			{
				stringData.add(new PreCompiledString(commandFilter.group(5),LabelEndOffset+commandFilter.start(5),LabelEndOffset+commandFilter.end(5), CodeSubstringType.SPACES_AND_TABULATIONS));
			}
			if(commandFilter.group(7)!=null)
			{
				ArgumentsOffset=LabelEndOffset+commandFilter.start(7);
				lineWithArguments=commandFilter.group(7);
			}
			else
			{
				removeEmptyStrings();
				return;
			}
		}
		else
		{
			stringData.add(new PreCompiledString(lineWithoutCommentAndLabel,LabelEndOffset,LabelEndOffset+lineWithoutCommentAndLabel.length(), CodeSubstringType.TRASH));
			removeEmptyStrings();
			return;
		}

		String strForOneParameter="("+ SyntaxChecker.argumentFormatPattern+"|"+ SyntaxChecker.labelFormatPattern+")";
		Pattern argPattern=Pattern.compile(strForOneParameter+"(\\s*)(.*)");
		int paramLengthInGroups=Pattern.compile(strForOneParameter).matcher("").groupCount();
		Matcher argumentFilter= argPattern.matcher(lineWithArguments);
		while(argumentFilter.matches())
		{
			stringData.add(new PreCompiledString(argumentFilter.group(1),ArgumentsOffset+argumentFilter.start(1),ArgumentsOffset+argumentFilter.end(1), CodeSubstringType.ARGUMENT));
			stringData.add(new PreCompiledString(argumentFilter.group(paramLengthInGroups+1),ArgumentsOffset+argumentFilter.start(paramLengthInGroups+1),ArgumentsOffset+argumentFilter.end(paramLengthInGroups+1), CodeSubstringType.SPACES_AND_TABULATIONS));
			ArgumentsOffset+=argumentFilter.start(paramLengthInGroups+2);
			lineWithArguments=argumentFilter.group(paramLengthInGroups+2);
			argumentFilter=argPattern.matcher(lineWithArguments);
		}
		stringData.add(new PreCompiledString(lineWithArguments,ArgumentsOffset,ArgumentsOffset+lineWithArguments.length(), CodeSubstringType.TRASH));
		numberOfArguments=this.getNumberOf(CodeSubstringType.ARGUMENT);
		removeEmptyStrings();
	}
	public PreCompiledString getPreString(CodeSubstringType type)
	{
		return getPreString(type,0);
	}
	public PreCompiledString getPreString(CodeSubstringType type,int index)
	{
		if(index<0)
			return null;
		int curIndex=0;
		for(PreCompiledString item:stringData)
		{
			if(item.type==type)
			{
				if(curIndex==index)
					return item;
				curIndex++;
			}
		}
		return null;
	}
	public int getNumberOf(CodeSubstringType type)
	{
		int result=0;
		for(PreCompiledString item:stringData)
		{
			if(item.type==type)
			{
				result++;
			}
		}
		return result;
	}
	public void switchArgument(PreCompiledString argStr, ArrayList<PreCompiledString> list)
	{
		stringData.remove(argStr);
		for(PreCompiledString i:list)
		{
			stringData.add(i);
		}
	}
	public LinkedList<PreCompiledString> getStringData()
	{
		return stringData;
	}
	public String createStr(ArrayList<String> usedLabels,ArrayList<String> labelsToAdd)
	{
		String result="";
		TreeMap<Integer,PreCompiledString> sortedLst=new TreeMap<Integer,PreCompiledString>();
		for(PreCompiledString t:stringData)
		{
			sortedLst.put(t.startIndex,t);
		}
		
		for(int i: sortedLst.keySet())
		{
			PreCompiledString item=sortedLst.get(i);
			if((item.type == CodeSubstringType.LABEL_DEFINITION)||(item.type == CodeSubstringType.LABEL_CALL))
			{
				if(usedLabels.contains(item.str))
				{
					int counter=1;
					while(usedLabels.contains(item.str+counter))
					{
						counter++;
					}
					result+=item.str+counter;
					if(labelsToAdd!=null)
					{
						if(!labelsToAdd.contains(item.str+counter))
							labelsToAdd.add(item.str+counter);
					}
					continue;
				}
				if(labelsToAdd!=null)
				{
					if(!labelsToAdd.contains(item.str))
						labelsToAdd.add(item.str);
				}

			}
			result+=item.str;	
		}
		return result+"\n";
	}

}
