package ram_pack.ram_core;

import ram_pack.ram_exceptions.RAMException;

import java.util.ArrayList;

public class CmdFormatStruct // формат комманды
{
	private RAM.Runner runner; /* класс, выполн€ющий комманду: нужно создать анонимный класс
									типа Runner с переопределенным методом run() */
	private ArrayList<ArgFormatStruct> argFormats; // список форматов аргументов ArgFormatStruct

	public CmdFormatStruct(RAM.Runner newRunner, ArrayList<ArgFormatStruct> formats)
	{
		runner=newRunner;
		argFormats=new ArrayList<ArgFormatStruct>(formats);
	}

	public ArrayList<ArgFormatStruct> getArgFormats() // возвращает форматы аргументов дл€ данной комманды
	{
		return (ArrayList<ArgFormatStruct>)argFormats.clone();
	}
	
	public void runCmd() throws RAMException // запускает обработчик комманд (runner)
	{
		runner.run();
	}
}
