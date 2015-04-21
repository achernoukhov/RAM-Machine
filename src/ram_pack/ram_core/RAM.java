package ram_pack.ram_core;

import ram_pack.ram_exceptions.*;
import ram_pack.LanguageTranslator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class RAM
{
	public abstract class Runner // �����, ����������� ��������
	{
		protected RAM execClass; // �����, � ������� Runner ����� ��������� �������
		public abstract void run() throws RAMException; // �����, ����������� �������� (����� ��������������)
		public Runner(RAM exec)
		{
			execClass=exec;
		}
	}

	private HashMap<String,CmdFormatStruct> cmdFormats=new HashMap<String,CmdFormatStruct>(); /* ������� ����������� ������� � �� ����������:
								   															String ��� -> CmdFormatStruct ������ ���������� */
	private HashMap<Integer,Integer> regs=new HashMap<Integer,Integer>(); /* ��������� ���������:
							 												Integer ����� -> Integer �������� */
	private Readable inputTape; // ������� �����
	private Writeable outputTape; // �������� �����
	private ArrayList<CmdStruct> cmdList=new ArrayList<CmdStruct>(); // ������ ����������� ������� CmdStruct
	private int currentCmd=0; // ����� ������� �������� � ������ ������� �� ����������
	private boolean started; // ������������/�� ������������ ������ RAM
	private boolean fillNullRegs=false; /* ��������� ��� �� ��������� ����������������� �������� ������
 											true - ���������������� �������� ����������� ������
											false - ��� ������� ��������� � ����������������� �������� ��������� ������ */
	private boolean fillNullInput=true; /* ��������� �� ����, ���� ������� ����� �����������
											true - ���� ����� �����������, �� ����������� ����
	 										false - ��� ������� ������� � ������������� ����� ��������� ������ */

	public static int criticalRegs=50000;
	public static int criticalOutput=50000;
	private boolean criticalReached=false;

	public RAM(Readable input,Writeable output)
	/** �����������, � �������� ����������
		������� ��� ������ (������� �����) � ������ (�������� �����) */
	{
		inputTape=input;
		outputTape=output;
		fillStdCmdFormats();
	}

	public RAM(Readable input,Writeable output,ArrayList<CmdStruct> cmds)
	/** �����������, � �������� ����������
	 	������� ��� ������ � ������ � ������ ������� �� ���������� */
	{
		this(input,output);
		cmdList=cmds;
	}

	public RAM(Readable input, Writeable output, boolean fillNullRegs, boolean fillNullInput)
	/** �����������, � �������� ����������
	 	������� ��� ������ � ������ � ����� ���������� ������ */
	{
		this(input,output);
		setFillNull(fillNullRegs,fillNullInput);
	}

	public RAM(Readable input, Writeable output,ArrayList<CmdStruct> cmds, boolean fillNullRegs, boolean fillNullInput)
	/** �����������, � �������� ����������
		������� ��� ������ � ������, ������ ������� �� ���������� � ����� ���������� ������ */
	{
		this(input,output);
		cmdList=cmds;
		setFillNull(fillNullRegs,fillNullInput);
	}

	public void setCmdFormats(HashMap<String,CmdFormatStruct> formats) // ��������� ������� ���������� ������� � ���������� ��������� �� formats
	{
		stop();
		cmdFormats=new HashMap<String,CmdFormatStruct>(formats);
	}

	public void addCmdFormat(String name, CmdFormatStruct format) // �������� ����� �������� � ������� ���������� ������� � ����������
	{
		stop();
		cmdFormats.put(name,format);
	}

	public void clearCmdFormats() // �������� ������� ���������� ������� � ����������
	{
		stop();
		cmdFormats.clear();
	}

	public void setInput(Readable input) // ���������� ������� ��� ������ (������� �����)
	{
		stop();
		inputTape=input;
	}

	public void setOutput(Writeable output) // ���������� ������� ��� ������ (�������� �����)
	{
		stop();
		outputTape=output;
	}

	public void setFillNull(boolean fillNullRegs, boolean fillNullInput) // ���������� ����� ���������� ������
	{
		stop();
		this.fillNullRegs=fillNullRegs;
		this.fillNullInput=fillNullInput;
	}

	public boolean getFillNullRegs() // ���������� �������� ����� fillNullRegs
	{
		return fillNullRegs;
	}

	public boolean getFillNullInput() // ���������� �������� ����� fillNullInput
	{
		return fillNullInput;
	}

	public void setCmdList(ArrayList<CmdStruct> cmds) // ��������� ������ ������� ��� ����������
	{
		stop();
		cmdList=new ArrayList<CmdStruct>(cmds);
	}

	public void addCmdList(ArrayList<CmdStruct> cmds)
	{
		stop();
		cmdList.addAll(cmds);
	}

	public void addCmd(CmdStruct cmd) // �������� ����� ������� � ������ ������� ��� ����������
	{
		stop();
		cmdList.add(cmd);
	}

	public void clear() // �������� ������ ������� � ��������
	{
		stop();
		cmdList.clear();
	}

	public int getCurrentCmd() // ���������� ����� ������� ��������
	{
		return currentCmd;
	}

	public ArrayList<ArgFormatStruct> get�mdFormat(String name) // ���������� ������ �������� ���������� ��� ��������
	{
		CmdFormatStruct cmdFormat=cmdFormats.get(name);
		if (cmdFormat==null)
			return null;
		return cmdFormat.getArgFormats();
	}

	public Set<String> getValidCmdSet() // ���������� ����� ��������� ������ (�����������) �������
	{
		return cmdFormats.keySet();
	}

	public Integer getRegister(Integer n) // ���������� �������� �������� �� ��� ������
	{
		Integer result=regs.get(n);
		if (result==null)
			if (fillNullRegs) // ��������� ���������������� �������� ������
			{
				regs.put(n,0);
				return regs.get(n);
			}
			else
				return null;
		else
			return result;
	}

	public Integer[] getRegistersFromTo(int from, int to) // ���������� �������� ��������� � from �� to ������������
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

	public Integer[] getRegistersFrom(int from, int n) // ���������� �������� n ���������, ������� � from
	{
		return getRegistersFromTo(from,from+n-1);
	}

	public HashMap<Integer,Integer> getAllRegisters()
	{
		return (HashMap<Integer,Integer>)regs.clone();
	}

	public boolean isStarted() // ���������� ������� ��������� ������ (��������/�� ��������)
	{
		return started;
	}

	public void run() throws RAMException // ��������� ������ �� ����������
	{
		if (!isStarted())
			start();
		while (isStarted())
			nextCommand();
	}

	public void runToLine(int n) throws RAMException // ��������� ������ �� ���������� �� n-� ������� (n-� �� �����������)
	{
		while (isStarted()&&(currentCmd!=n))
			nextCommand();
	}

	public void start() throws RAMException// ���������� ������ ��� ���������� �������
	{
		regs.clear();
		currentCmd=0;
		criticalReached=false;
		started=true;
		skipEmptyCommands();
	}

	public void stop() // ������������� ������ RAM (������� ������� ��������)
	{
		started=false;
	}

	public void nextCommand() throws RAMException // ��������� ��������� �������� �� ������ cmdList
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
		CmdStruct cmd=cmdList.get(currentCmd); // ��������� �������� �� ������
		CmdFormatStruct cmdFormat=cmdFormats.get(cmd.cmdName); // ���� �������� � ������ ����������� �������
		if (cmdFormat==null)
		{
			stop();
			throw new NoCmdException(LanguageTranslator.getString("unknown.command1") +cmd.cmdName,currentCmd);
		}
		cmdFormat.runCmd(); // ��������� ��������������� ����������
		skipEmptyCommands();
	}

	private void skipEmptyCommands() throws RAMException // ���������� ������ ��������
	{
		if (!started)
			return;
		if (currentCmd>=cmdList.size())
		{
			stop();
			throw new NoCmdException(LanguageTranslator.getString("going.out.of.command.space"),currentCmd);
		}
		while (cmdList.get(currentCmd)==null) // ���������� ������ ��������
		{
			currentCmd++;
			if (currentCmd>=cmdList.size())
			{
				stop();
				throw new NoCmdException(LanguageTranslator.getString("going.out.of.command.space"),currentCmd);
			}
		}
	}

	private Integer getReference(Argument arg) throws RAMException // ���������� ����� �������� �� ��������� ������,
	{
		if (arg.referenceDegree<0) // �� ������
			throw new InvalException(LanguageTranslator.getString("invalid.argument"),currentCmd);
		Integer l=arg.num;
		if (l==null) // ������ ��������
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

	private Integer getRegisterArg(Argument arg) throws RAMException // ���������� �������� �������� �� ��������� ������
	{
		if (arg.referenceDegree<0)
			return arg.num;
		Integer ref=getReference(arg);
		return getRegisterEx(ref);
	}

	private Argument getArgument(int n) throws RAMException // ���������� n-� �������� ������� ��������
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

	private void load() throws RAMException // ����������� �������� load
	{
		Argument arg=getArgument(0);
		Integer source=getRegisterArg(arg);
		regs.remove(0);
		regs.put(0,source);
		currentCmd++;
	}

	private void store() throws RAMException // ����������� �������� store
	{
		Integer zero=getRegisterEx(0);
		Argument arg=getArgument(0);
		Integer modifyReg=getReference(arg);
		regs.remove(modifyReg);
		regs.put(modifyReg,zero);
		currentCmd++;
	}

	private void add() throws RAMException // ����������� �������� add
	{
		Integer zero=getRegisterEx(0);
		Argument arg=getArgument(0);
		Integer newZero=zero+getRegisterArg(arg);
		regs.remove(0);
		regs.put(0,newZero);
		currentCmd++;
	}

	private void sub() throws RAMException // ����������� �������� sub
	{
		Integer zero=getRegisterEx(0);
		Argument arg=getArgument(0);
		Integer newZero=zero-getRegisterArg(arg);
		regs.remove(0);
		regs.put(0,newZero);
		currentCmd++;
	}

	private void mult() throws RAMException // ����������� �������� mult
	{
		Integer zero=getRegisterEx(0);
		Argument arg=getArgument(0);
		Integer newZero=zero*getRegisterArg(arg);
		regs.remove(0);
		regs.put(0,newZero);
		currentCmd++;
	}

	private void div() throws RAMException // ����������� �������� div !!!�������� ������� �� ����!!!
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

	private void mod() throws RAMException // ����������� �������� mod !!!�������� ������� �� ����!!!
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

	private void jump() throws RAMException // ����������� �������� jump
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

	private void jgtz() throws RAMException // ����������� �������� jgtz
	{
		if (getRegisterEx(0)>0)
		{
			jump();
		}
		else
			currentCmd++;
	}

	private void jzero() throws RAMException // ����������� �������� jzero
	{
		if (getRegisterEx(0)==0)
		{
			jump();
		}
		else
			currentCmd++;
	}

	private void write() throws RAMException // ����������� �������� write
	{
		Argument arg=getArgument(0);
		outputTape.writeNext(getRegisterArg(arg));
		currentCmd++;
	}

	private void writeSymbol() throws RAMException // ����������� �������� write
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

	private void read() throws RAMException // ����������� �������� read
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

	protected void halt() // ����������� �������� halt
	{
		stop();
	}

	private void fillStdCmdFormats() // ��������� ������� ����������� ������� � �� ���������� ������������ ����������
	{
		Runner run; // ����� ������������ � ����������� CmdFormatStruct
		ArrayList<ArgFormatStruct> format; // ����� ������������ � ����������� CmdFormatStruct

		stop();

		format=new ArrayList<ArgFormatStruct>();
		format.add(ArgFormatStruct.argPrimitiveReferenceInt());
		run=new Runner(this){public void run() throws RAMException {execClass.load();}}; // ������� ��������� �����
		cmdFormats.put("load",new CmdFormatStruct(run,format));

		format=new ArrayList<ArgFormatStruct>();
		format.add(ArgFormatStruct.argReferenceInt());
		run=new Runner(this){public void run() throws RAMException {execClass.store();}}; // ������� ��������� �����
		cmdFormats.put("store",new CmdFormatStruct(run,format));

		format=new ArrayList<ArgFormatStruct>();
		format.add(ArgFormatStruct.argPrimitiveReferenceInt());
		run=new Runner(this){public void run() throws RAMException {execClass.add();}}; // ������� ��������� �����
		cmdFormats.put("add",new CmdFormatStruct(run,format));

		format=new ArrayList<ArgFormatStruct>();
		format.add(ArgFormatStruct.argPrimitiveReferenceInt());
		run=new Runner(this){public void run() throws RAMException {execClass.sub();}}; // ������� ��������� �����
		cmdFormats.put("sub",new CmdFormatStruct(run,format));

		format=new ArrayList<ArgFormatStruct>();
		format.add(ArgFormatStruct.argPrimitiveReferenceInt());
		run=new Runner(this){public void run() throws RAMException {execClass.mult();}}; // ������� ��������� �����
		cmdFormats.put("mult",new CmdFormatStruct(run,format));

		format=new ArrayList<ArgFormatStruct>();
		format.add(ArgFormatStruct.argPrimitiveReferenceInt());
		run=new Runner(this){public void run() throws RAMException {execClass.div();}}; // ������� ��������� �����
		cmdFormats.put("div",new CmdFormatStruct(run,format));

		format=new ArrayList<ArgFormatStruct>();
		format.add(ArgFormatStruct.argPrimitiveReferenceInt());
		run=new Runner(this){public void run() throws RAMException {execClass.mod();}}; // ������� ��������� �����
		cmdFormats.put("mod",new CmdFormatStruct(run,format));

		format=new ArrayList<ArgFormatStruct>();
		format.add(ArgFormatStruct.argLabelInt());
		run=new Runner(this){public void run() throws RAMException {execClass.jump();}}; // ������� ��������� �����
		cmdFormats.put("jump",new CmdFormatStruct(run,format));

		format=new ArrayList<ArgFormatStruct>();
		format.add(ArgFormatStruct.argLabelInt());
		run=new Runner(this){public void run() throws RAMException {execClass.jgtz();}}; // ������� ��������� �����
		cmdFormats.put("jgtz",new CmdFormatStruct(run,format));

		format=new ArrayList<ArgFormatStruct>();
		format.add(ArgFormatStruct.argLabelInt());
		run=new Runner(this){public void run() throws RAMException {execClass.jzero();}}; // ������� ��������� �����
		cmdFormats.put("jzero",new CmdFormatStruct(run,format));

		format=new ArrayList<ArgFormatStruct>();
		format.add(ArgFormatStruct.argPrimitiveReferenceInt());
		run=new Runner(this){public void run() throws RAMException {execClass.write();}}; // ������� ��������� �����
		cmdFormats.put("write",new CmdFormatStruct(run,format));

		format=new ArrayList<ArgFormatStruct>();
		format.add(ArgFormatStruct.argPrimitiveReferenceInt());
		run=new Runner(this){public void run() throws RAMException {execClass.writeSymbol();}}; // ������� ��������� �����
		cmdFormats.put("writesym",new CmdFormatStruct(run,format));

		format=new ArrayList<ArgFormatStruct>();
		format.add(ArgFormatStruct.argReferenceInt());
		run=new Runner(this){public void run() throws RAMException {execClass.read();}}; // ������� ��������� �����
		cmdFormats.put("read",new CmdFormatStruct(run,format));

		format=new ArrayList<ArgFormatStruct>();
		run=new Runner(this){public void run() {execClass.halt();}}; // ������� ��������� �����
		cmdFormats.put("halt",new CmdFormatStruct(run,format));
	}
}
