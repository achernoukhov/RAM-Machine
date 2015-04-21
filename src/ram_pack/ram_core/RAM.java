package ram_pack.ram_core;

import ram_pack.ram_exceptions.*;
import ram_pack.LanguageTranslator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class RAM
{
	public abstract class Runner // класс, запускающий комманду
	{
		protected RAM execClass; // класс, в котором Runner будет выполнять функцию
		public abstract void run() throws RAMException; // метод, выполняющий комманду (нужно переопределять)
		public Runner(RAM exec)
		{
			execClass=exec;
		}
	}

	private HashMap<String,CmdFormatStruct> cmdFormats=new HashMap<String,CmdFormatStruct>(); /* таблица разрешенных комманд и их аргументов:
								   															String имя -> CmdFormatStruct формат аргументов */
	private HashMap<Integer,Integer> regs=new HashMap<Integer,Integer>(); /* контейнер регистров:
							 												Integer номер -> Integer значение */
	private Readable inputTape; // входная лента
	private Writeable outputTape; // выходная лента
	private ArrayList<CmdStruct> cmdList=new ArrayList<CmdStruct>(); // список выполняемых комманд CmdStruct
	private int currentCmd=0; // номер текущей комманды в списке комманд на исполнение
	private boolean started; // активирована/не активирована машина RAM
	private boolean fillNullRegs=false; /* заполнять или не заполнять неиинициированные регистры нулями
 											true - неинициированные регистры заполняются нулями
											false - при попытке обращения к неинициированному регистру бросается ошибка */
	private boolean fillNullInput=true; /* считывать ли нули, если входная лента закончилась
											true - если лента закончилась, то считывается нуль
	 										false - при попытке считать с закончившейся ленты бросается ошибка */

	public static int criticalRegs=50000;
	public static int criticalOutput=50000;
	private boolean criticalReached=false;

	public RAM(Readable input,Writeable output)
	/** конструктор, в качестве параметров
		очереди для чтения (входная лента) и записи (выходная лента) */
	{
		inputTape=input;
		outputTape=output;
		fillStdCmdFormats();
	}

	public RAM(Readable input,Writeable output,ArrayList<CmdStruct> cmds)
	/** конструктор, в качестве параметров
	 	очереди для чтения и записи и список комманд на выполнение */
	{
		this(input,output);
		cmdList=cmds;
	}

	public RAM(Readable input, Writeable output, boolean fillNullRegs, boolean fillNullInput)
	/** конструктор, в качестве параметров
	 	очереди для чтения и записи и флаги заполнения нулями */
	{
		this(input,output);
		setFillNull(fillNullRegs,fillNullInput);
	}

	public RAM(Readable input, Writeable output,ArrayList<CmdStruct> cmds, boolean fillNullRegs, boolean fillNullInput)
	/** конструктор, в качестве параметров
		очереди для чтения и записи, список комманд на выполнение и флаги заполнения нулями */
	{
		this(input,output);
		cmdList=cmds;
		setFillNull(fillNullRegs,fillNullInput);
	}

	public void setCmdFormats(HashMap<String,CmdFormatStruct> formats) // заполнить таблицу допустимых комманд и аргументов форматами из formats
	{
		stop();
		cmdFormats=new HashMap<String,CmdFormatStruct>(formats);
	}

	public void addCmdFormat(String name, CmdFormatStruct format) // добавить новую комманду в таблицу допустимых комманд и аргументов
	{
		stop();
		cmdFormats.put(name,format);
	}

	public void clearCmdFormats() // очистить таблицу допустимых комманд и аргументов
	{
		stop();
		cmdFormats.clear();
	}

	public void setInput(Readable input) // установить очередь для чтения (входная лента)
	{
		stop();
		inputTape=input;
	}

	public void setOutput(Writeable output) // установить очередь для записи (выходная лента)
	{
		stop();
		outputTape=output;
	}

	public void setFillNull(boolean fillNullRegs, boolean fillNullInput) // установить флаги заполнения нулями
	{
		stop();
		this.fillNullRegs=fillNullRegs;
		this.fillNullInput=fillNullInput;
	}

	public boolean getFillNullRegs() // возвращает значение флага fillNullRegs
	{
		return fillNullRegs;
	}

	public boolean getFillNullInput() // возвращает значение флага fillNullInput
	{
		return fillNullInput;
	}

	public void setCmdList(ArrayList<CmdStruct> cmds) // заполнить список комманд для выполнения
	{
		stop();
		cmdList=new ArrayList<CmdStruct>(cmds);
	}

	public void addCmdList(ArrayList<CmdStruct> cmds)
	{
		stop();
		cmdList.addAll(cmds);
	}

	public void addCmd(CmdStruct cmd) // добавить новую команду в список комманд для выполнения
	{
		stop();
		cmdList.add(cmd);
	}

	public void clear() // очистить список комманд и регистры
	{
		stop();
		cmdList.clear();
	}

	public int getCurrentCmd() // возвращает номер текущей комманды
	{
		return currentCmd;
	}

	public ArrayList<ArgFormatStruct> getСmdFormat(String name) // возвращает список форматов аргументов для комманды
	{
		CmdFormatStruct cmdFormat=cmdFormats.get(name);
		if (cmdFormat==null)
			return null;
		return cmdFormat.getArgFormats();
	}

	public Set<String> getValidCmdSet() // возвращает набор известных машине (разрешенных) комманд
	{
		return cmdFormats.keySet();
	}

	public Integer getRegister(Integer n) // возвращает значение регистра по его номеру
	{
		Integer result=regs.get(n);
		if (result==null)
			if (fillNullRegs) // заполняем неинициированные регистры нулями
			{
				regs.put(n,0);
				return regs.get(n);
			}
			else
				return null;
		else
			return result;
	}

	public Integer[] getRegistersFromTo(int from, int to) // возвращает значения регистров с from по to включительно
	{
		if (to<from)
				return null;
		Integer result []=new Integer[to-from+1];
		for (int i=from;i<=to;i++)
		{
			result[i]=getRegister(i);
		}
		return result;
	}

	public Integer[] getRegistersFrom(int from, int n) // возвращает значения n регистров, начиная с from
	{
		return getRegistersFromTo(from,from+n-1);
	}

	public HashMap<Integer,Integer> getAllRegisters()
	{
		return (HashMap<Integer,Integer>)regs.clone();
	}

	public boolean isStarted() // возвращает текущее состояние машины (запущена/не запущена)
	{
		return started;
	}

	public void run() throws RAMException // запускает машину на выполнение
	{
		if (!isStarted())
			start();
		while (isStarted())
			nextCommand();
	}

	public void runToLine(int n) throws RAMException // запускает машину на выполнение до n-й строчки (n-я не выполняется)
	{
		while (isStarted()&&(currentCmd!=n))
			nextCommand();
	}

	public void start() throws RAMException// активирует машину для выполнения комманд
	{
		regs.clear();
		currentCmd=0;
		criticalReached=false;
		started=true;
		skipEmptyCommands();
	}

	public void stop() // останавливает машину RAM (попутно очищает регистры)
	{
		started=false;
	}

	public void nextCommand() throws RAMException // выполняет следующую комманду из списка cmdList
	{
		skipEmptyCommands();
		if (!isStarted())
			return;
		if (!criticalReached)
			if ((regs.size()>=criticalRegs)||(outputTape.size()>=criticalOutput))
			{
				criticalReached=true;
				throw new InvalException(LanguageTranslator.getString("your.program.uses.too.much.memory.maybe.it.has.infinit.loop.continuing.may.cause.memory.overflow.and.crash.of.the.application"),-666);
			}
		CmdStruct cmd=cmdList.get(currentCmd); // следующая комманда из списка
		CmdFormatStruct cmdFormat=cmdFormats.get(cmd.cmdName); // ищем комманду в списке разрешенных комманд
		if (cmdFormat==null)
		{
			stop();
			throw new NoCmdException(LanguageTranslator.getString("unknown.command1") +cmd.cmdName,currentCmd);
		}
		cmdFormat.runCmd(); // запускаем соответствующий обработчик
		skipEmptyCommands();
	}

	private void skipEmptyCommands() throws RAMException // пропускает пустые комманды
	{
		if (!started)
			return;
		if (currentCmd>=cmdList.size())
		{
			stop();
			throw new NoCmdException(LanguageTranslator.getString("going.out.of.command.space"),currentCmd);
		}
		while (cmdList.get(currentCmd)==null) // пропускаем пустые комманды
		{
			currentCmd++;
			if (currentCmd>=cmdList.size())
			{
				stop();
				throw new NoCmdException(LanguageTranslator.getString("going.out.of.command.space"),currentCmd);
			}
		}
	}

	private Integer getReference(Argument arg) throws RAMException // возвращает номер регистра по косвенной ссылке,
	{
		if (arg.referenceDegree<0) // не ссылка
			throw new InvalException(LanguageTranslator.getString("invalid.argument"),currentCmd);
		Integer l=arg.num;
		if (l==null) // пустой аргумент
			throw new InvalException(LanguageTranslator.getString("invalid.argument"),currentCmd);
		for (int i=arg.referenceDegree;i>0;i--)
		{
			l=getRegisterEx(l);
		}
		return l;
	}

	private Integer getRegisterEx(Integer reg) throws RAMException
	{
		Integer result=getRegister(reg);
		if (result==null)
			throw new InvalException(LanguageTranslator.getString("can.t.read.from.register") +reg+ LanguageTranslator.getString("register.isn.t.initialized.yet"),currentCmd);
		return result;
	}

	private Integer getRegisterArg(Argument arg) throws RAMException // возвращает значение регистра по косвенной ссылке
	{
		if (arg.referenceDegree<0)
			return arg.num;
		Integer ref=getReference(arg);
		return getRegisterEx(ref);
	}

	private Argument getArgument(int n) throws RAMException // возвращает n-й аргумент текущей комманды
	{
		if (currentCmd>=cmdList.size())
		{
			stop();
			throw new NoCmdException(LanguageTranslator.getString("going.out.of.command.space"),currentCmd);
		}
		CmdStruct cmd=cmdList.get(currentCmd);
		if (n>=cmd.argList.size())
		{
			stop();
			throw new InvalException(LanguageTranslator.getString("not.enough.arguments"),currentCmd);
		}
		Argument arg=cmd.argList.get(n);
		if (arg==null)
		{
			stop();
			throw new InvalException(LanguageTranslator.getString("not.enough.arguments"),currentCmd);
		}
		return arg;
	}

	private void load() throws RAMException // выполняется комманда load
	{
		Argument arg=getArgument(0);
		Integer source=getRegisterArg(arg);
		regs.remove(0);
		regs.put(0,source);
		currentCmd++;
	}

	private void store() throws RAMException // выполняется комманда store
	{
		Integer zero=getRegisterEx(0);
		Argument arg=getArgument(0);
		Integer modifyReg=getReference(arg);
		regs.remove(modifyReg);
		regs.put(modifyReg,zero);
		currentCmd++;
	}

	private void add() throws RAMException // выполняется комманда add
	{
		Integer zero=getRegisterEx(0);
		Argument arg=getArgument(0);
		Integer newZero=zero+getRegisterArg(arg);
		regs.remove(0);
		regs.put(0,newZero);
		currentCmd++;
	}

	private void sub() throws RAMException // выполняется комманда sub
	{
		Integer zero=getRegisterEx(0);
		Argument arg=getArgument(0);
		Integer newZero=zero-getRegisterArg(arg);
		regs.remove(0);
		regs.put(0,newZero);
		currentCmd++;
	}

	private void mult() throws RAMException // выполняется комманда mult
	{
		Integer zero=getRegisterEx(0);
		Argument arg=getArgument(0);
		Integer newZero=zero*getRegisterArg(arg);
		regs.remove(0);
		regs.put(0,newZero);
		currentCmd++;
	}

	private void div() throws RAMException // выполняется комманда div !!!возможно деление на ноль!!!
	{
		Integer zero=getRegisterEx(0);
		Argument arg=getArgument(0);
		Integer reg=getRegisterArg(arg);
		if (reg==0)
			throw new ZeroException(LanguageTranslator.getString("division.by.0"),currentCmd);
		Integer newZero=zero/reg;
		regs.remove(0);
		regs.put(0,newZero);
		currentCmd++;
	}

	private void mod() throws RAMException // выполняется комманда mod !!!возможно деление на ноль!!!
	{
		Integer zero=getRegisterEx(0);
		Argument arg=getArgument(0);
		Integer reg=getRegisterArg(arg);
		if (reg==0)
			throw new ZeroException(LanguageTranslator.getString("division.by.0"),currentCmd);
		Integer newZero=zero%reg;
		regs.remove(0);
		regs.put(0,newZero);
		currentCmd++;
	}

	private void jump() throws RAMException // выполняется комманда jump
	{
		Argument arg=getArgument(0);
		Integer ref=getReference(arg);
		if ((ref>=cmdList.size())||(ref<0))
		{
			stop();
			throw new InvalException(LanguageTranslator.getString("attempt.to.jump.to.line") +ref+ LanguageTranslator.getString("which.doesn.t.exist"),currentCmd);
		}
		currentCmd=ref;
	}

	private void jgtz() throws RAMException // выполняется комманда jgtz
	{
		if (getRegisterEx(0)>0)
		{
			jump();
		}
		else
			currentCmd++;
	}

	private void jzero() throws RAMException // выполняется комманда jzero
	{
		if (getRegisterEx(0)==0)
		{
			jump();
		}
		else
			currentCmd++;
	}

	private void write() throws RAMException // выполняется комманда write
	{
		Argument arg=getArgument(0);
		outputTape.writeNext(getRegisterArg(arg));
		currentCmd++;
	}

	private void writeSymbol() throws RAMException // выполняется комманда write
	{
		Argument arg=getArgument(0);
		int val = getRegisterArg(arg);
		if ((val<Character.MIN_VALUE)||(val>Character.MAX_VALUE))
		{
			throw new InvalException(LanguageTranslator.getString("invalid.argument"),currentCmd);
		}
		outputTape.writeNext((char)val);
		currentCmd++;
	}

	private void read() throws RAMException // выполняется комманда read
	{
		Argument arg=getArgument(0);
		Integer readval;
		if (inputTape.isEmpty())
			if (fillNullInput)
				readval=0;
			else
			{
				stop();
				throw new InputException(LanguageTranslator.getString("input.tape.is.over"),currentCmd);
			}
		else
			readval=inputTape.readNext();
		Integer modifyReg=getReference(arg);
		regs.remove(modifyReg);
		regs.put(modifyReg,readval);
		currentCmd++;
	}

	protected void halt() // выполняется комманда halt
	{
		stop();
	}

	private void fillStdCmdFormats() // заполняет таблицу разрешенных комманд и их аргументов стандартными значениями
	{
		Runner run; // будет передаваться в конструктор CmdFormatStruct
		ArrayList<ArgFormatStruct> format; // будет передаваться в конструктор CmdFormatStruct

		stop();

		format=new ArrayList<ArgFormatStruct>();
		format.add(ArgFormatStruct.argPrimitiveReferenceInt());
		run=new Runner(this){public void run() throws RAMException {execClass.load();}}; // создаем анонимный класс
		cmdFormats.put("load",new CmdFormatStruct(run,format));

		format=new ArrayList<ArgFormatStruct>();
		format.add(ArgFormatStruct.argReferenceInt());
		run=new Runner(this){public void run() throws RAMException {execClass.store();}}; // создаем анонимный класс
		cmdFormats.put("store",new CmdFormatStruct(run,format));

		format=new ArrayList<ArgFormatStruct>();
		format.add(ArgFormatStruct.argPrimitiveReferenceInt());
		run=new Runner(this){public void run() throws RAMException {execClass.add();}}; // создаем анонимный класс
		cmdFormats.put("add",new CmdFormatStruct(run,format));

		format=new ArrayList<ArgFormatStruct>();
		format.add(ArgFormatStruct.argPrimitiveReferenceInt());
		run=new Runner(this){public void run() throws RAMException {execClass.sub();}}; // создаем анонимный класс
		cmdFormats.put("sub",new CmdFormatStruct(run,format));

		format=new ArrayList<ArgFormatStruct>();
		format.add(ArgFormatStruct.argPrimitiveReferenceInt());
		run=new Runner(this){public void run() throws RAMException {execClass.mult();}}; // создаем анонимный класс
		cmdFormats.put("mult",new CmdFormatStruct(run,format));

		format=new ArrayList<ArgFormatStruct>();
		format.add(ArgFormatStruct.argPrimitiveReferenceInt());
		run=new Runner(this){public void run() throws RAMException {execClass.div();}}; // создаем анонимный класс
		cmdFormats.put("div",new CmdFormatStruct(run,format));

		format=new ArrayList<ArgFormatStruct>();
		format.add(ArgFormatStruct.argPrimitiveReferenceInt());
		run=new Runner(this){public void run() throws RAMException {execClass.mod();}}; // создаем анонимный класс
		cmdFormats.put("mod",new CmdFormatStruct(run,format));

		format=new ArrayList<ArgFormatStruct>();
		format.add(ArgFormatStruct.argLabelInt());
		run=new Runner(this){public void run() throws RAMException {execClass.jump();}}; // создаем анонимный класс
		cmdFormats.put("jump",new CmdFormatStruct(run,format));

		format=new ArrayList<ArgFormatStruct>();
		format.add(ArgFormatStruct.argLabelInt());
		run=new Runner(this){public void run() throws RAMException {execClass.jgtz();}}; // создаем анонимный класс
		cmdFormats.put("jgtz",new CmdFormatStruct(run,format));

		format=new ArrayList<ArgFormatStruct>();
		format.add(ArgFormatStruct.argLabelInt());
		run=new Runner(this){public void run() throws RAMException {execClass.jzero();}}; // создаем анонимный класс
		cmdFormats.put("jzero",new CmdFormatStruct(run,format));

		format=new ArrayList<ArgFormatStruct>();
		format.add(ArgFormatStruct.argPrimitiveReferenceInt());
		run=new Runner(this){public void run() throws RAMException {execClass.write();}}; // создаем анонимный класс
		cmdFormats.put("write",new CmdFormatStruct(run,format));

		format=new ArrayList<ArgFormatStruct>();
		format.add(ArgFormatStruct.argPrimitiveReferenceInt());
		run=new Runner(this){public void run() throws RAMException {execClass.writeSymbol();}}; // создаем анонимный класс
		cmdFormats.put("writesym",new CmdFormatStruct(run,format));

		format=new ArrayList<ArgFormatStruct>();
		format.add(ArgFormatStruct.argReferenceInt());
		run=new Runner(this){public void run() throws RAMException {execClass.read();}}; // создаем анонимный класс
		cmdFormats.put("read",new CmdFormatStruct(run,format));

		format=new ArrayList<ArgFormatStruct>();
		run=new Runner(this){public void run() {execClass.halt();}}; // создаем анонимный класс
		cmdFormats.put("halt",new CmdFormatStruct(run,format));
	}
}
