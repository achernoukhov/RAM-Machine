package ram_pack.ram_check_and_convert;
import ram_pack.ram_core.ArgFormatStruct;
import ram_pack.ram_core.Argument;
import ram_pack.ram_core.CmdStruct;
import ram_pack.ram_core.RAM;
import ram_pack.RAMInput;
import ram_pack.LanguageTranslator;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

//класс для проверки синтаксиса, для конвертации кода во внутренний формат машины RAM [и подсветки синтаксиса]
public class SyntaxChecker
{
	private RAM ram;//машина для получения данных о допустимых командах и их форматах
	static String argumentFormatPattern="((\\**)(=?)((-?[0-9]+)|('.')))";
	static String labelFormatPattern="([a-zA-Z][_a-zA-Z0-9]*)";
	static String commandFormatPattern="([a-zA-Z][_a-zA-Z0-9]*)";
	static String macroStartString="(#macro)";
	static String macroEndString="(#endmacro)";
	private String compiledCode;
	public SyntaxChecker(RAM ramMachine)
	{
		ram=ramMachine;
	}
	/*конвертирует код в виде текста в массив команд машины RAM, при ошибке конвертации возвращает null[или кидает эксепшн]
	  алгоритм:
	  1)убрать комментарии
	   а)слеш-звезданутые [не реализовано]
	   б)строчные //
	  2)поиск лейблов(в начале строки, начинаются с английской буквы, состоят из английских букв/цифр) и составление таблицы
	  2*)если встретили лейбл дважды - ошибка

	  3) строчка за строчкой:
	  3.1)считать команду
	  3.2)разделить на аргументы
	  3.3)лейблы в аргументах преобразовать в номера строк кода
	  3.4)сформировать cmdStruct
	  3.5)добавить в лист и идти дальше
	  4)вернуть результат
	  */
	//возвращает скомпилированный код(с подставленными макросами)
	public String getCompiledCode()
	{
		return compiledCode;
	}
	//компиляция кода новая, с макросами
	public ArrayList<CmdStruct> compileCode(String code) throws WrongSyntaxException
	{
		compiledCode=code;
		return compileCodeOld(code);
	}
	//компиляция кода новая, с макросами
	public ArrayList<CmdStruct> compileCodeMacro(String code) throws WrongSyntaxException
	{
		//ArrayList<CmdStruct> result=new ArrayList<CmdStruct>(); //нечто, что мы собираемся возвращать
		ArrayList<PreCompiledLine> lineList=this.generateHighlightList(code); 		//лист с размеченными строками
		boolean seekMacroEnd=false;
		for(int i=0;i<lineList.size();i++)
		{
			PreCompiledLine item=lineList.get(i);
			if(item.getPreString(CodeSubstringType.INVALID_COMMAND)!=null)
				throw new WrongSyntaxException(LanguageTranslator.getString("syntax.error.unknown.command") + item.getPreString(CodeSubstringType.INVALID_COMMAND).str+" )", i);
			if(item.getPreString(CodeSubstringType.INVALID_ARGUMENT)!=null)
				throw new WrongSyntaxException(LanguageTranslator.getString("syntax.error.invalid.argument") +item.getPreString(CodeSubstringType.INVALID_ARGUMENT).str+ LanguageTranslator.getString("for.command") +item.getPreString(CodeSubstringType.COMMAND).str+"'", i);
			if(item.getPreString(CodeSubstringType.INVALID_LABEL_DEFINITION)!=null)
				throw new WrongSyntaxException(LanguageTranslator.getString("syntax.error.invalid.label.definition.label") +item.getPreString(CodeSubstringType.INVALID_LABEL_DEFINITION).str+ LanguageTranslator.getString("is.met.twise.line") +i+")",i);
			if(item.getPreString(CodeSubstringType.INVALID_MACRO_NAME)!=null)
				throw new WrongSyntaxException(LanguageTranslator.getString("syntax.error.invalid.macro.name") +item.getPreString(CodeSubstringType.INVALID_MACRO_NAME).str+ LanguageTranslator.getString("line1") +i+")",i);
			if(item.getPreString(CodeSubstringType.INVALID_MACRO_END)!=null)
				throw new WrongSyntaxException(LanguageTranslator.getString("syntax.error.invalid.macro.end.definition.macro.expected.line") +i+")",i);
			if(item.getPreString(CodeSubstringType.INVALID_MACRO_START)!=null)
				throw new WrongSyntaxException(LanguageTranslator.getString("syntax.error.invalid.macro.start.definition.endmacro.expected.line") +i+")",i);
			if(item.invalidNumberOfParametres)
				throw new WrongSyntaxException(LanguageTranslator.getString("syntax.error.the.number.of.arguments.for.command") +item.getPreString(CodeSubstringType.COMMAND).str+ LanguageTranslator.getString("doesn.t.match.required"), i);
			if(item.getPreString(CodeSubstringType.TRASH)!=null)
				throw new WrongSyntaxException(LanguageTranslator.getString("syntax.error.unrecognized.structure"), i);
			if(item.isMacroStart)
				seekMacroEnd=true;
			if(item.isMacroEnd)
				seekMacroEnd=false;
		}
		if(seekMacroEnd)
			throw new WrongSyntaxException(LanguageTranslator.getString("syntax.error.unfinished.macro"), lineList.size()-1);

		HashMap<String,MacroStruct> macroses=new HashMap<String,MacroStruct>();
		ArrayList<PreCompiledLine> codeLines=new ArrayList<PreCompiledLine>();

		fillMacroAndCodeLines(macroses,codeLines,lineList);
		generateAfterCompileCode(codeLines, macroses);
		return compileCodeOld(compiledCode);
	}

	private void fillMacroAndCodeLines(HashMap<String,MacroStruct> macroses,ArrayList<PreCompiledLine> codeLines,final ArrayList<PreCompiledLine> lineList)
	{
		ArrayList<PreCompiledLine> macroLines=new ArrayList<PreCompiledLine>();
		String macroName="";
		boolean seekMacroEnd=false;

		for(PreCompiledLine item:lineList)
		{
			if(!seekMacroEnd)
			{
				if(item.isMacroStart)
				{
					seekMacroEnd=true;
					macroLines=new ArrayList<PreCompiledLine>();
					macroName=item.getPreString(CodeSubstringType.MACRO_NAME_DEFINITION).str;
				}
				else
				{
					codeLines.add(item);
				}
			}
			else
			{
				if(item.isMacroEnd)
				{
					seekMacroEnd=false;
					macroses.put(macroName,new MacroStruct(macroName,macroLines));
				}
				else
				{
					macroLines.add(item);
				}
			}
		}
	}

	//генерит код, как он будет выглядеть после подстановки макросов
	private String generateAfterCompileCode(ArrayList<PreCompiledLine> codeLines,HashMap<String,MacroStruct> macroses)
	{
		compiledCode="";
		ArrayList<String> usedLabels=this.getLabelList(codeLines);
		for(PreCompiledLine item: codeLines)
		{
			PreCompiledString it=item.getPreString(CodeSubstringType.MACRO_CALL);
			if(it!=null)
				if(macroses.containsKey(it.str))
				{
					if(item.getPreString(CodeSubstringType.LABEL_DEFINITION)!=null)
						compiledCode+=item.getPreString(CodeSubstringType.LABEL_DEFINITION).str+":\n";
					compiledCode+=macroses.get(it.str).getTranslatedCode(usedLabels);
					continue;
				}
			compiledCode+=item.createStr(new ArrayList<String>(),null);
		}
		return compiledCode;
	}
	//компиляция кода старая, без макросов
	private ArrayList<CmdStruct> compileCodeOld(String code) throws WrongSyntaxException
	{
		ArrayList<CmdStruct> result=new ArrayList<CmdStruct>(); 		//нечто, что мы собираемся возвращать
		ArrayList<String> lineList=new ArrayList<String>();				//лист с выкинутыми лейблами в начале
		HashMap<String,Integer> labelMap=new HashMap<String,Integer>();	//хеш таблица<String labelName,Integer lineIndex> - сопостовляет лейбл номеру строки
		String codeWithoutLineComments=code.replaceAll("//.*",""); 		// убираем строчные комментарии

		Scanner scanner=new Scanner(codeWithoutLineComments);
		int lineIndex=0;
		while(scanner.hasNextLine())
		{
			//строчки обрабатываем по отдельности
			String line=scanner.nextLine();
			Pattern p=Pattern.compile("(?:[\\s]*)"+labelFormatPattern+":([^:]*)");//проверяем, есть ли в строке лейбл
			Matcher matcher=p.matcher(line);
			boolean m=matcher.matches();
			if(m)//Если есть
			{
				String newLabel=matcher.toMatchResult().group(1);//найденный лейбл
				String newLine=matcher.toMatchResult().group(2);//найденная строка без лейбла
//				System.out.println("найдена метка "+newLabel);
				if(labelMap.containsKey(newLabel))
				{
//					System.out.println("ошибка синтаксиса: лейбл встречен дважды( "+newLabel+" ) - строки "+lineIndex+" и "+labelMap.get(newLabel));
//					return null;
					throw new WrongSyntaxException(LanguageTranslator.getString("syntax.error.label") +newLabel+ LanguageTranslator.getString("met.twise.lines") +lineIndex+ LanguageTranslator.getString("and") +labelMap.get(newLabel));
				}
				labelMap.put(newLabel,lineIndex);
				lineList.add(newLine); //вставляем строку с вырезанным лейблом
			}
			else
			{
				if(line.matches(".*:.*"))
				{
	//				System.out.println("ошибка синтаксиса: неправильная строка с меткой  - строка "+lineIndex+":\t"+line);
	//				return null;
					throw new WrongSyntaxException(LanguageTranslator.getString("syntax.error.invalid.line.with.label"), lineIndex);
				}
				lineList.add(line);//вставляем исходную строку, т.к. лейбла не нашли
			}
			lineIndex++;
		}
		scanner.close();
		//закончили генерить таблицу лейблов
		//начинаем идти по коду и читать команды, преобразуя на ходу лейблы в номера строк
		for(int i=0;i<lineList.size();i++)
		{
			String line=lineList.get(i);
			Pattern p=Pattern.compile("(\\s*("+commandFormatPattern+"\\s+(.*))|(\\s*"+commandFormatPattern+"\\s*)|(\\s*))");//команда с параметрами| команда без параметров| пустая строка
			Matcher m=p.matcher(line);
			String commandName="";
			String argumentStr="";
			if(!m.matches())
			{
	//			System.out.println("ошибка синтаксиса: неправильно оформленная команда - строка "+i+":\t"+line);
	//			return null;
				throw new WrongSyntaxException(LanguageTranslator.getString("syntax.error.invalid.command"), i);
			}
			if(m.group(7)!=null) //если строка пуста
			{
				//пометить строку пустой
				result.add(null);
				continue;
			}
			if(m.group(5)!=null)//если встретили команду без параметров
			{
				commandName=m.group(6).toLowerCase();
			}
			if(m.group(2)!=null)
			{
				commandName=m.group(3).toLowerCase();
				argumentStr=m.group(4);
			}
			ArrayList<ArgFormatStruct> argList=ram.getСmdFormat(commandName);//считываем с машины формат команды
			if(argList==null)//если такой команды нет...
			{
				//встретили незнакомую команду - вылетаем
//				System.out.println("ошибка синтаксиса: найдена незнакомая команда( "+ commandName+" ) - строка "+i+":\t"+line);
//				return null;
				throw new WrongSyntaxException(LanguageTranslator.getString("syntax.error.unknown.command1") + commandName+"'", i);
			}
			ArrayList<Argument> arguments=new ArrayList<Argument>();
			int numberOfArguments=argList.size();
			//создание паттерна для аргументов
			String patternForArguments="\\s*";
			String StrForOneParameter="("+argumentFormatPattern+"|"+labelFormatPattern+")\\s*";
			int paramLengthInGroups=Pattern.compile(StrForOneParameter).matcher("").groupCount();
			for(int j=0;j<numberOfArguments;j++)
			{
				patternForArguments+=StrForOneParameter;
			}
			Pattern pForArgs=Pattern.compile(patternForArguments);
			Matcher matcherForArgs=pForArgs.matcher(argumentStr);
			if(!matcherForArgs.matches())//проверка аргументов на форматы
			{
				//не совпало=> у юзера кривые руки
	//			System.out.println("ошибка синтаксиса: кол-во аргументов команды '"+commandName+"' не совпадает с необходимым ( "+argList.size()+" ) - строка "+i+":\t"+line);
	//			return null;
				throw new WrongSyntaxException(LanguageTranslator.getString("syntax.error.the.number.of.arguments.of.command") +commandName+ LanguageTranslator.getString("doesn.t.match.required1") +argList.size()+" )", i);
			}
			for(int j=0;j<numberOfArguments;j++)
			{
				ArgFormatStruct argFormatStruct=argList.get(j);
				String argStr=matcherForArgs.group(1+j*(paramLengthInGroups));
				Argument argumentToAdd=constructArgument(argFormatStruct,argStr,labelMap);
				if(argumentToAdd==null)
				{
					//не совпало=> у юзера кривые руки
		//			System.out.println("ошибка синтаксиса: неправильный параметр команды '"+commandName+"'  ( "+argStr+" ) - строка "+i+":\t"+line);
		//			return null;
					throw new WrongSyntaxException(LanguageTranslator.getString("syntax.error.invalid.argument") +argStr+ LanguageTranslator.getString("for.command") +commandName+"'", i);
				}
				arguments.add(argumentToAdd);
			}
			result.add(new CmdStruct(commandName,arguments));
		}
		return result;
	}
	//возвращает null в случае некорректного параметра и адрес аргумента в случае успеха
	private static Argument constructArgument(ArgFormatStruct argFormatStruct, String argStr,HashMap<String,Integer> labelMap)
	{
		if(argFormatStruct.labelPermit&&(labelMap.get(argStr)!=null))
			return new Argument(labelMap.get(argStr),0);//нашли метку - вернули аргумент с замененной меткой
		Pattern argPattern=Pattern.compile(argumentFormatPattern);
		Matcher matcher=argPattern.matcher(argStr);
		boolean m=matcher.matches();
		if(!m)
			return null;//сообщение выписываем снаружи
		int numOfStars=matcher.group(2).length();//количество звёзд в аргументе
		int numOfEquals=matcher.group(3).length();//количество равенств в аргументе
		Integer num;
		if(matcher.group(5)!=null)
		{
			num=new Integer(matcher.group(5));
		}
		else
		{
			num=new Integer(matcher.group(6).charAt(1)); //charToInt
		}
		if((!argFormatStruct.primitivePermit&&(numOfEquals>0))||(!argFormatStruct.referencePermit&&(numOfStars>0))||((numOfStars>0)&&(numOfEquals>0))||(numOfEquals>1)||(num==null))//проверка на несовпадение аргумента и формата
		{
			return null;
		}
		int degree= (numOfEquals>0) ? -1 : numOfStars ;//генерация степени
		return new Argument(num,degree);
	}
	//генерация входной ленты из строки
	public static RAMInput generateInputTape(String input)
	{
		RAMInput ramInput=new RAMInput();
		Scanner scanner=new Scanner(input);
		while(scanner.hasNextLine())
		{
			String line=scanner.nextLine();
			Pattern argPattern=Pattern.compile("\\s*((-?[0-9]+)|('.')|(\"[^\"]*\"))\\s*(.*)");
			Matcher matcher=argPattern.matcher(line);
			while(matcher.matches())
			{
				String newElement=matcher.group(1);
				if(matcher.group(2)!=null)
				{
					Integer newElem=new Integer(newElement);
					ramInput.addLast(newElem);
				}
				if(matcher.group(3)!=null)
				{
					Integer newElem=new Integer(newElement.charAt(1));
					ramInput.addLast(newElem);
				}
				if(matcher.group(4)!=null)
				{
					for(int i=1;i<(newElement.length()-1);i++)
					{
						Integer newElem=new Integer(newElement.charAt(i));
						ramInput.addLast(newElem);
					}
				}
				matcher=argPattern.matcher(matcher.group(5));
			}
		}
		return ramInput;
	}
//генерит массив предкомпилированных строк(с разметкой и служебными данными)
	private ArrayList<PreCompiledLine> generatePreCompiledLines(String code)
	{
		ArrayList<PreCompiledLine> result=new ArrayList<PreCompiledLine>();
		Scanner lineScanner=new Scanner(code);
		int lineIndex=0;
		while(lineScanner.hasNextLine())
		{
			String line=lineScanner.nextLine();
			result.add(new PreCompiledLine(line,lineIndex));
			lineIndex+=1+line.length();
		}
		lineScanner.close();
		return result;
	}


	//генерит подсветки
	public ArrayList<PreCompiledLine> generateHighlightList(String code)
	{
		ArrayList<String> globalCommandMacroSet=new ArrayList<String>(ram.getValidCmdSet());
		ArrayList<PreCompiledLine> preCompiledLines=generatePreCompiledLines(code);
		ArrayList<String> globalLabelList=getLabelList(preCompiledLines);
		ArrayList<PreCompiledLine> preCompiledLinesForMacro=null;
		boolean seekMacroEnd=false;
		String newMacroName=null;

		for(int i=0;i<preCompiledLines.size();i++)
		{
			PreCompiledLine current=preCompiledLines.get(i);
			if(seekMacroEnd)
			{
				if(current.isMacroStart)
				{
					current.getPreString(CodeSubstringType.MACRO_DEFINITION_START).type= CodeSubstringType.INVALID_MACRO_START;
					continue;
				}
				if(current.isMacroEnd)
				{
					seekMacroEnd=false;
					for(PreCompiledLine item: preCompiledLinesForMacro)
					{
						this.getHighlightsForString(item,globalCommandMacroSet,this.getLabelList(preCompiledLinesForMacro));
					}
					continue;
				}
				preCompiledLinesForMacro.add(current);

			}
			else
			{
				if(current.isMacroEnd)
				{
					current.getPreString(CodeSubstringType.MACRO_DEFINITION_END).type= CodeSubstringType.INVALID_MACRO_END;
					continue;
				}
				if(current.isMacroStart)
				{
					seekMacroEnd=true;
					preCompiledLinesForMacro=new ArrayList<PreCompiledLine>();
					PreCompiledString comStr=current.getPreString(CodeSubstringType.MACRO_NAME_DEFINITION);
					if(comStr!=null)
					{
						newMacroName=comStr.str.toLowerCase();
						if(globalCommandMacroSet.contains(newMacroName))
						{
							current.getPreString(CodeSubstringType.MACRO_NAME_DEFINITION).type= CodeSubstringType.INVALID_MACRO_NAME;
						}
						else
						{
							globalCommandMacroSet.add(newMacroName);
						}
					}
					continue;
				}
				this.getHighlightsForString(current,globalCommandMacroSet,globalLabelList);

			}
		}
		if(seekMacroEnd)
		{
			for(PreCompiledLine item: preCompiledLinesForMacro)
			{
				this.getHighlightsForString(item,globalCommandMacroSet,this.getLabelList(preCompiledLinesForMacro));
			}
		}
		return preCompiledLines;
	}

	public ArrayList<String> getLabelList(ArrayList<PreCompiledLine> list)
	{
		ArrayList<String> result=new ArrayList<String>();
		Iterator<PreCompiledLine> it=list.iterator();
		while(it.hasNext())
		{
			PreCompiledLine item=it.next();
			if(item.isMacroStart)
			{
				while(it.hasNext())
				{
					PreCompiledLine itemToSeek=it.next();
					if(itemToSeek.isMacroEnd)
						break;
				}
				continue;
			}
			if(item.getNumberOf(CodeSubstringType.LABEL_DEFINITION)>0)
			{
				if(result.contains(item.getPreString(CodeSubstringType.LABEL_DEFINITION).str))
				{
					item.getPreString(CodeSubstringType.LABEL_DEFINITION).type= CodeSubstringType.INVALID_LABEL_DEFINITION;
				}
				else
				{
					result.add(item.getPreString(CodeSubstringType.LABEL_DEFINITION).str);
				}
			}
		}
		return result;
	}


	//меняет в нужных местах тип подсветки(valid->invalid)
	public void getHighlightsForString(PreCompiledLine line,ArrayList<String> globalCommandMacroSet,	ArrayList<String> labelList)
	{
		PreCompiledString commandStr=line.getPreString(CodeSubstringType.COMMAND);
		if(commandStr==null)
			return;
		String command=line.getPreString(CodeSubstringType.COMMAND).str.toLowerCase();
		if(command==null)
			return;
		ArrayList<ArgFormatStruct> argList=ram.getСmdFormat(command);	//считываем с машины формат команды
		if(argList==null)
		{
			if(globalCommandMacroSet.contains(command))	//значит макрос
			{
				line.getPreString(CodeSubstringType.COMMAND).type= CodeSubstringType.MACRO_CALL;
				argList=new ArrayList<ArgFormatStruct>();
			}
			else
			{
				line.getPreString(CodeSubstringType.COMMAND).type= CodeSubstringType.INVALID_COMMAND;
				return;
			}
		}
		if(line.numberOfArguments!=argList.size())
			line.invalidNumberOfParametres=true;
		if(line.getNumberOf(CodeSubstringType.ARGUMENT)>argList.size())
		{
			for(int i=line.getNumberOf(CodeSubstringType.ARGUMENT)-1;i>=argList.size();i--)
			{
				line.getPreString(CodeSubstringType.ARGUMENT,i).type=CodeSubstringType.INVALID_ARGUMENT;
			}
		}
		ArrayList<PreCompiledString> args=new ArrayList<PreCompiledString>();
		for(int i=0;i<line.getNumberOf(CodeSubstringType.ARGUMENT);i++)
		{
			args.add(line.getPreString(CodeSubstringType.ARGUMENT,i));
		}
		for(int i=0;i<args.size();i++)
		{
			line.switchArgument(args.get(i),this.constructArgumentHighlights(argList.get(i),args.get(i).str, labelList,args.get(i).startIndex));
		}
	}

	//генерит подсветки для конкретного аргумента
	private ArrayList<PreCompiledString> constructArgumentHighlights(ArgFormatStruct argFormatStruct, String argStr,ArrayList<String> labelMap,int initialOffset)
	{
		ArrayList<PreCompiledString> syntaxHighlightStructArrayList=new ArrayList<PreCompiledString>();
		if(argFormatStruct.labelPermit)
		{
			if(labelMap.contains(argStr))
			{
				syntaxHighlightStructArrayList.add(new PreCompiledString(argStr,initialOffset,initialOffset+argStr.length(), CodeSubstringType.LABEL_CALL));
				return syntaxHighlightStructArrayList;
			}
			else
			{
				syntaxHighlightStructArrayList.add(new PreCompiledString(argStr,initialOffset,initialOffset+argStr.length(), CodeSubstringType.INVALID_ARGUMENT));
				return syntaxHighlightStructArrayList;
			}

		}
		//((\**)(=?)((-?[0-9]+)|('.')))
		Pattern argPattern=Pattern.compile(argumentFormatPattern);
		Matcher matcher=argPattern.matcher(argStr);
		boolean m=matcher.matches();
		if(!m)
		{
			syntaxHighlightStructArrayList.add(new PreCompiledString(argStr,initialOffset,initialOffset+argStr.length(), CodeSubstringType.INVALID_ARGUMENT));
			return syntaxHighlightStructArrayList;
		}
		int numOfStars=matcher.group(2).length();//количество звёзд в аргументе
		int numOfEquals=matcher.group(3).length();//количество равенств в аргументе
		Integer numStart=initialOffset+matcher.start(4);
		Integer numEnd=initialOffset+matcher.end(4);
		if(matcher.group(5)!=null)
		{
			syntaxHighlightStructArrayList.add(new PreCompiledString(matcher.group(4),numStart,numEnd, CodeSubstringType.NUMBER));
		}
		else
		{
			syntaxHighlightStructArrayList.add(new PreCompiledString(matcher.group(4),numStart,numEnd, CodeSubstringType.SYMBOL));
		}
		//подсчитали кол-во звёзд и знаков равно
		if(((numOfStars>0)&&(numOfEquals>0))||(numOfEquals>1))//проверка на несовпадение аргумента и формата
		{
			syntaxHighlightStructArrayList.add(new PreCompiledString(matcher.group(4),initialOffset,initialOffset+matcher.end(4), CodeSubstringType.INVALID_ARGUMENT));
			return syntaxHighlightStructArrayList;
		}
		else
		{
			if (numOfStars > 0) {
				int starStart = initialOffset + matcher.start(2);
				int starEnd = initialOffset + matcher.end(2);

				if (argFormatStruct.referencePermit)
					syntaxHighlightStructArrayList.add(new PreCompiledString(matcher.group(2), starStart, starEnd, CodeSubstringType.STAR));
				else
					syntaxHighlightStructArrayList.add(new PreCompiledString(matcher.group(2), starStart, starEnd, CodeSubstringType.INVALID_ARGUMENT));
			}
			if (numOfEquals > 0) {
				int equalStart = initialOffset + matcher.start(3);
				int equalEnd = initialOffset + matcher.end(3);
				if (argFormatStruct.primitivePermit)
					syntaxHighlightStructArrayList.add(new PreCompiledString(matcher.group(3), equalStart, equalEnd, CodeSubstringType.EQUAL));
				else
					syntaxHighlightStructArrayList.add(new PreCompiledString(matcher.group(3), equalStart, equalEnd, CodeSubstringType.INVALID_ARGUMENT));
			}
		}
		return syntaxHighlightStructArrayList;
	}

	//генерит строку с табуляциями для смещений после нажатий энтера
	public static String generateTabStr(String input)
	{
		Pattern p=Pattern.compile("(\\s*)"+labelFormatPattern+":(\\s*)(([^\\s].*)|)");	//строка с меткой
		Matcher m=p.matcher(input);
		if(m.matches())
			return m.group(1)+m.group(3);
		Pattern p2=Pattern.compile("(\\s*)[^\\s].*");								//непустая строка без метки
		Matcher m2=p2.matcher(input);
		if(m2.matches())
			return m2.group(1);
		Pattern p3=Pattern.compile("(\\s*)");										//пустая строка с или без разделителей
		Matcher m3=p3.matcher(input);
		if(m3.matches())
			return m3.group(1);
		return "";
	}

}



//Trash

/**	//генерит коллекцию подсветок
	public ArrayList<SyntaxHighlightStruct> generateHighlightList(String code)
	{
		ArrayList<SyntaxHighlightStruct> result=new ArrayList<SyntaxHighlightStruct>();
		Scanner lineScanner=new Scanner(code);
		ArrayList<Integer> lineIndexes=new ArrayList<Integer>();
		ArrayList<String> labelMap=new ArrayList<String>();
		ArrayList<String> filteredCode=new ArrayList<String>();
		int lineIndex=0;
		while(lineScanner.hasNextLine())
		{
			String line=lineScanner.nextLine().toLowerCase();
			String lineWithoutComment;
			String lineWithoutCommentAndLabel;
			int indexOfFilteredStr=lineIndex;

			Matcher commentFilter=Pattern.compile("([^/]*)(//.*)").matcher(line);
			if(commentFilter.matches())
			{
				//надо коммент выделить и не учитывать его далее
				lineWithoutComment=commentFilter.group(1);
				int commentStart=lineIndex+lineWithoutComment.length();
				int commentEnd=lineIndex+line.length();
				result.add(new SyntaxHighlightStruct(commentStart,commentEnd,6));	//добавляем подсветку комментария
			}
			else
				lineWithoutComment=line;

			//Ищем лейбл
			Pattern p=Pattern.compile("(?:[\\s]*)"+labelFormatPattern+":([^:]*)");//проверяем, есть ли в строке лейбл
			Matcher matcher=p.matcher(lineWithoutComment);
			boolean m=matcher.matches();
			if(m)//Если есть - повесить подсветку и добавить в таблицу
			{
				String newLabel=matcher.toMatchResult().group(1);//найденный лейбл
				lineWithoutCommentAndLabel=matcher.group(2);//найденная строка без лейбла
				int labelStart=matcher.toMatchResult().start(1);
				if(labelMap.contains(newLabel))
				{
					//встретили объявление лейбла второй раз - выделяем кривым цветом
					result.add(new SyntaxHighlightStruct(indexOfFilteredStr+labelStart,indexOfFilteredStr+newLabel.length()+1,-1));
				}
				else
				{
					result.add(new SyntaxHighlightStruct(indexOfFilteredStr+labelStart,indexOfFilteredStr+newLabel.length()+1,1));
					labelMap.add(newLabel);
				}
				indexOfFilteredStr+=labelStart+newLabel.length()+1;
			}
			else
			{
				if(lineWithoutComment.matches(".*:.*"))
				{
					result.add(new SyntaxHighlightStruct(indexOfFilteredStr,indexOfFilteredStr+lineWithoutComment.length(),0));
					lineWithoutCommentAndLabel="";
				}
				else
					lineWithoutCommentAndLabel=lineWithoutComment;
			}
			filteredCode.add(lineWithoutCommentAndLabel);
			lineIndexes.add(indexOfFilteredStr);
			lineIndex+=line.length()+1;
		}
		//закончили генерить таблицу лейблов
		//начинаем идти по коду и читать команды, преобразуя на ходу лейблы в номера строк
		for(int i=0;i<filteredCode.size();i++)
		{
			String line=filteredCode.get(i);
			int lIndex=lineIndexes.get(i);
			Pattern p=Pattern.compile("(\\s*(([a-zA-Z0-9_]+)\\s+(.*))|(\\s*([a-zA-Z0-9_]+)\\s*)|(\\s*))");//команда с параметрами| команда без параметров| пустая строка
			Matcher m=p.matcher(line);
			String commandName="";
			String argumentStr="";
			Integer commandStart=0;
			Integer commandEnd=0;
			Integer argumentStart=null;
			Integer argumentEnd=null;
			if(!m.matches())	//если строка вся нафиг неправильная
			{
				result.add(new SyntaxHighlightStruct(lIndex,lIndex+line.length(),-666));
				continue;
			}
			if(m.group(7)!=null) //если строка пуста
			{
			//	result.add(null);
				continue;
			}
			if(m.group(5)!=null)//если встретили команду без параметров
			{
				commandName=m.group(6).toLowerCase();
				commandStart=lIndex+m.start(6);
				commandEnd=lIndex+m.end(6);
			}
			if(m.group(2)!=null)
			{
				commandName=m.group(3).toLowerCase();
				commandStart=lIndex+m.start(3);
				commandEnd=lIndex+m.end(3);
				argumentStr=m.group(4);
				argumentStart=lIndex+m.start(4);
				argumentEnd=lIndex+m.end(4);
			}
			ArrayList<ArgFormatStruct> argList=ram.getСmdFormat(commandName);//считываем с машины формат команды
			if(argList==null)//если такой команды нет...
			{
				result.add(new SyntaxHighlightStruct(commandStart,commandEnd,-2));
				if(argumentStart!=null)
				{
					result.add(new SyntaxHighlightStruct(argumentStart,argumentEnd,-666));
				}
				continue;
			}
			result.add(new SyntaxHighlightStruct(commandStart,commandEnd,2));
			if(m.group(5)!=null)//если встретили команду без параметров
				continue;
			ArrayList<Argument> arguments=new ArrayList<Argument>();
			int numberOfArguments=argList.size();
			//создание паттерна для аргументов
			String patternForArguments="\\s*";
			String StrForOneParameter="("+argumentFormatPattern+"|"+labelFormatPattern+")\\s*";
			int paramLengthInGroups=Pattern.compile(StrForOneParameter).matcher("").groupCount();
			for(int j=0;j<numberOfArguments;j++)
			{
				patternForArguments+=StrForOneParameter;
			}
			Pattern pForArgs=Pattern.compile(patternForArguments);
			Matcher matcherForArgs=pForArgs.matcher(argumentStr);
			if(!matcherForArgs.matches())//проверка аргументов на форматы
			{
				result.add(new SyntaxHighlightStruct(argumentStart,argumentEnd,-666));
				continue;
			}
			for(int j=0;j<numberOfArguments;j++)
			{
				ArgFormatStruct argFormatStruct=argList.get(j);
				String argStr=matcherForArgs.group(1+j*paramLengthInGroups);
				int initialOffset=argumentStart+matcherForArgs.start(1+j*paramLengthInGroups);
				result.addAll(constructArgumentHighlights(argFormatStruct,argStr,labelMap,initialOffset));
			}
		}

		lineScanner.close();
		return result;
	}
	*/

