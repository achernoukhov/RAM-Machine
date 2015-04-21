import ram_pack.ram_core.CmdStruct;
import ram_pack.ram_core.RAM;
import ram_pack.ram_exceptions.RAMException;
import ram_pack.RAMInput;
import ram_pack.RAMOutput;
import ram_pack.LanguageTranslator;
import ram_pack.ram_change_font.ChooseFontDialog;
import ram_pack.ram_UndoDocument.*;
import ram_pack.ram_choose_highlight_styles.ColoredFont;
import ram_pack.ram_choose_highlight_styles.ChooseHighlightStylesDialog;
import ram_pack.ram_check_and_convert.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.regex.MatchResult;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.*;
import javax.swing.event.*;
import javax.swing.undo.*;
import java.io.*;
import java.net.URL;

public class MainWindow extends JFrame
{
	private static boolean setRuLang=false;
    private boolean languageChanged=false;
    private static Color DEFAULT_ERROR_HIGHLIGHT_COLOR =new Color(255,150,150);
	private static Color DEFAULT_DEBUG_HIGHLIGHT_COLOR =new Color(50,255,0);
	private static String DEFAULT_HIGHLIGHT_SETTINGS = "highlight_settings.ini";
	private static String USER_SETTINGS_FILE_NAME = "user_settings.ini";
	private static String HIGHLIGHT_SETTINGS_PATTERN = "([^\\s]+)[\\s]+([1-9,0,-]+)[\\s]+([1-9,0,-]+)\\s*";
	private static String FONT_SETTINGS_PATTERN = "DEFAULT_FONT[\\s]+\\[(.+)\\][\\s]+([1-9,0]+)\\s*";
    private static String DIMENSIONS_SETTINGS_PATTERN = "DIMENSIONS[\\s]+\\[(.+)\\][\\s]+([1-90]+)\\s*";

    private static final String LANGUAGE_SETTINGS_PATTERN = "LANGUAGE[\\s]+((ru)|(en))\\s*";
	private JTextPane codeField;	//поле с кодом
	private JTextPane regsField;	//поле с регистрами
	private JTextPane inputField;	//поле с входной лентой
	private JTextPane outputField;	//поле с выходной лентой
	private JTextPane[] allTextPanes;
	private JScrollPane[] scrollPanes;	//прокручивающиеся панельки
	private	JSplitPane inputAndOutputSplitPane;
	private JSplitPane regsAndInputAndOutputSplitPane;
	private JSplitPane mainSplitPane;
	private MyDocument doc;	//документ, где хранятся данные(код)
    private MyDocumentListener docListener;
	private String newline = "\n";	//символ начала строки
	private HashMap actions;		//таблица с экшнами
	private boolean documentSaved=true;	//флаг "документ сохранен"
	private boolean debugModeOn=false;	//флаг "changeCodeTextвключен режим debug"
	private File openedFile=null;	//открытый файл
	private String applicationName="RAM";	//имя программы (пишется перед названием файла)
	private RAMInput inputQueue=new RAMInput();	//входная лента
	private RAMOutput outputQueue=new RAMOutput();	//выходная лента
	private RAM ram=new RAM(inputQueue,outputQueue);	//РАМ-машина для запуска
	private CaretListenerLabel caretListenerLabel =
				new CaretListenerLabel("");
    private MyDocument codeCopy;            //копия кода, используется при дебаггинге
	private JFileChooser fileChooser=null;

    protected SyntaxHighlightWorker syntaxHighlightWorker =null;
	protected boolean errorHighlighted=false;

	protected UndoAction undoAction;
	protected RedoAction redoAction;
	protected NewFileAction newFileAction;
	protected SaveFileAsAction saveFileAsAction;
	protected SaveFileAction saveFileAction;
	protected OpenFileAction openFileAction;
	protected RunAction runAction;
	protected RunStepByStepAction runStepByStepAction;
	protected RunToCursorAction runToCursorAction;
	protected NextLineAction nextLineAction;
	protected StopAction stopAction;
	protected ChangeHighlightStylesAction сhangeHighlightStylesAction;
	protected ChangeFontAction changeFontAction;
    protected ChangeLanguageAction changeLanguageAction;
	protected HelpAction helpAction;
    protected ContactsAction contactsAction;

    protected int[] dimensionParameters=new int[]{600,400,380,100};





	protected MyUndoManager undo = new MyUndoManager();
	private TreeMap<CodeSubstringType,ColoredFont> highlightStyles;

	// таймер для запуска подсветки
	private Timer timer=new Timer(100,new TimerActionListener());
	private boolean codeSwitched=false;



    //реализует добавление табуляций после нажатия на энтер
	protected class EnterKeyListener extends KeyAdapter
	{
			public void keyTyped(KeyEvent e)
			{
				if((e.getKeyChar()=='\n')&&codeField.isEditable())
				{
					try
					{
						//Юзер нажал на энтер - стоит посчитать табуляции и вывести правильное количество
						Element elem=doc.getDefaultRootElement();	//список строк после добавления новой
						int lineIndex=getLineAtCaret(codeField);	//смотрим номер строки, где находимся
						Element ourStringElement=elem.getElement(lineIndex-1);	//берем строку - строка с таким номером заведомо существует, тк мы нажали энтер => существует строка, где мы его нажимали
						try
						{
							String ourString=doc.getText(ourStringElement.getStartOffset(),ourStringElement.getEndOffset()-ourStringElement.getStartOffset()-1);	//ends by '\n'
							doc.insertString(codeField.getCaretPosition(), SyntaxChecker.generateTabStr(ourString),null);
						}
						catch(BadLocationException e1)
						{
							e1.printStackTrace();
						}
						//System.out.println(e.paramString());
					}
					catch(Exception ignore)
					{
                        System.out.println(LanguageTranslator.getString("caret.is.out.of.document"));
                        //игнорируем случай, когда после нажатия Enter каретка выходит за пределы документа
					}
				}
			}
	}

	// следит за перемещениями каретки
	protected class CaretListenerLabel extends JLabel
									   implements CaretListener
	{
		public String additionalStr="";
		public CaretListenerLabel(String label)
		{
			super(label);
		}

		//Might not be invoked from the event dispatching thread.
		public void caretUpdate(CaretEvent e)
		{
			displayCaretInfo();
		}

		//выставление новой строки в статус баре(другой поток)
		private void displayCaretInfo()
		{
			javax.swing.SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						String text=(LanguageTranslator.getString("html.body.p.line.t") +getLineAtCaret(codeField))+"</p>";
						if (debugModeOn)
						{
							text+= LanguageTranslator.getString("p.current.command.t") +ram.getCurrentCmd()+"</p>";
						}
						text+=additionalStr;
						text+="</body></html>";
						setText(text);
						mainSplitPane.revalidate();
						repaint();
						//revalidate();
					}
				});
		}

		//генерация сообщения об ошибке в статус баре
		private void displayErrorInfo(RAMException e)
		{
			additionalStr="<p><font color=\"#FF0000\">"+e.getMessage();
			int lineIndex=e.getErrorLine();
			if(lineIndex>=0)
			{
				additionalStr+= LanguageTranslator.getString("line") +e.getErrorLine()+")";
				changeLineHighlight(lineIndex, DEFAULT_ERROR_HIGHLIGHT_COLOR);
				errorHighlighted=true;
			}
			additionalStr+="</font></p>";
			codeField.repaint();
			displayCaretInfo();
		}

		//генерация сообщения об успехе в статус баре
		private void displaySuccessMessage(String info)
		{
			additionalStr="<p><font color=\"#20FF20\">"+info+"</font></p>";
			codeField.repaint();
			displayCaretInfo();
		}
	}

	// следит за изменениями, которые можно откатить (undo)
	protected class MyUndoableEditListener
					implements UndoableEditListener
	{
		public void undoableEditHappened(UndoableEditEvent e) {
			//Remember the edit and update the menus.
	//		String eventStr=e.getEdit().getPresentationName();
			undo.undoableEditHappened(e);
	//		if(!eventStr.equals("style change"))
            //if(eventStr.equals("addition")||eventStr.equals("deletion")||eventStr.equals("insert")||eventStr.equals("remove"))	//имхо криво, зато быстро делается и работает(отсев style changing - подсветки)
     //       {
	//			undo.undoableEditHappened(e);
				undoAction.updateUndoState();
				redoAction.updateRedoState();
     //      }
		}
	}

	// следит за изменениями в документе
	protected class MyDocumentListener
					implements DocumentListener
	{
		public boolean redoOrUndoMade =false;

		public void insertUpdate(DocumentEvent e)
		{
            //doc.splitTextCharacter(e.getOffset(),e.getLength());
            insertOrRemoveUpdate();
            if(!redoOrUndoMade)
            {
                javax.swing.SwingUtilities.invokeLater(new RunnableSplit(e.getOffset(),e.getLength(),doc));
            }

		}
		public void removeUpdate(DocumentEvent e)
		{
			insertOrRemoveUpdate();
		}
		public void changedUpdate(DocumentEvent e)
		{
		}
		private void insertOrRemoveUpdate()
		{
			documentSaved=false;
			timer.restart();
			if(syntaxHighlightWorker!=null)
				syntaxHighlightWorker.cancel(true);
			if(errorHighlighted)
			{
				codeField.getHighlighter().removeAllHighlights();
				errorHighlighted=false;
			}
		}
	}

	// следит за событиями таймера
	protected class TimerActionListener implements ActionListener
	{
		TimerActionListener()
		{
			super();
		}
		public void actionPerformed(ActionEvent event)
		{
			runSyntaxHighlight(doc);
		}
	}

	// запускает машину в новом потоке
	protected abstract class RAMWorker extends SwingWorker<RAMException,Object>
	{
		protected void done()
		{
			try
			{
				ramDoneAction(get());
			}
			catch (Exception ignore)
			{
				// ignoring
			}
		}
	}

//********************** Вспомогательные классы для менюшек и вообще действий над редактором кода и debug-а **************************//

	class NewFileAction extends AbstractAction
	{
		public NewFileAction() {
			super(LanguageTranslator.getString("create"));
			setEnabled(true);
		}

		public void actionPerformed(ActionEvent e)
		{
		   performNewFile();
		}
	}

	class OpenFileAction extends AbstractAction
	{
		public OpenFileAction()
		{
			super(LanguageTranslator.getString("open"));
			setEnabled(true);
		}

		public void actionPerformed(ActionEvent e)
		{
		   performOpenDocument();
		}
	}

	class SaveFileAction extends AbstractAction
	{
		public SaveFileAction() {
			super(LanguageTranslator.getString("save"));
			setEnabled(true);
		}

		public void actionPerformed(ActionEvent e)
		{
		   performSaveDocument();
		}
	}

	class SaveFileAsAction extends AbstractAction
	{
		public SaveFileAsAction()
		{
			super(LanguageTranslator.getString("save.as"));
			setEnabled(true);
		}

		public void actionPerformed(ActionEvent e)
		{
		   performSaveAsDocument();
		}
	}

	class UndoAction extends AbstractAction
	{
		public UndoAction() {
			super();
			setEnabled(false);
			updateUndoState();
		}

		public void actionPerformed(ActionEvent e)
		{
			try
			{
				docListener.redoOrUndoMade = true;
				undo.undo();
				docListener.redoOrUndoMade = false;
			} catch (CannotUndoException ex) {
				System.out.println(LanguageTranslator.getString("unable.to.undo") + ex);
				ex.printStackTrace();
			}
			updateUndoState();
			redoAction.updateRedoState();
		}

		protected void updateUndoState()
		{
			if (undo.canUndo())
			{
				setEnabled(true);
				putValue(Action.NAME, undo.getUndoPresentationName());
			}
			else
			{
				setEnabled(false);
				putValue(Action.NAME, UIManager.getString("AbstractUndoableEdit.undoText"));
			}
		}
	}

	class RedoAction extends AbstractAction
	{
		public RedoAction() {
			super();
			setEnabled(false);
			updateRedoState();
		}

		public void actionPerformed(ActionEvent e)
		{
			try
            {
                    docListener.redoOrUndoMade =true;
                    undo.redo();
                    docListener.redoOrUndoMade =false;
                    /*if(!undo.canRedo())
                    {
                        javax.swing.SwingUtilities.invokeLater(new RunnableSplit(0,doc.getLength(),doc));
                    }*/
			} catch (CannotRedoException ex) {
				System.out.println(LanguageTranslator.getString("unable.to.redo") + ex);
				ex.printStackTrace();
			}
			updateRedoState();
			undoAction.updateUndoState();
		}

		protected void updateRedoState()
		{
			if (undo.canRedo()) {
				setEnabled(true);
				putValue(Action.NAME, undo.getRedoPresentationName());
			} else {
				setEnabled(false);
				putValue(Action.NAME, UIManager.getString("AbstractUndoableEdit.redoText"));
			}
		}
	}

	class RunAction extends AbstractAction
	{
		public RunAction()
		{
			super(LanguageTranslator.getString("run"));
			setEnabled(true);
		}

		public void actionPerformed(ActionEvent e)
		{
			performRun();
		}
	}

	class RunStepByStepAction extends AbstractAction
	{
		public RunStepByStepAction()
		{
			super(LanguageTranslator.getString("run.step.by.step"));
			setEnabled(true);
		}

		public void actionPerformed(ActionEvent e)
		{
			performRunStepByStep();
		}
	}

	class NextLineAction extends AbstractAction
	{
		public NextLineAction()
		{
			super(LanguageTranslator.getString("next.line"));
			setEnabled(false);
		}

		public void actionPerformed(ActionEvent e)
		{
		   performNextLine();
		}
	}

	class RunToCursorAction extends AbstractAction
	{
		public RunToCursorAction()
		{
			super(LanguageTranslator.getString("run.to.cursor"));
			setEnabled(false);
		}

		public void actionPerformed(ActionEvent e)
		{
			performRunToCursor();
		}
	}

	class StopAction extends AbstractAction
	{
		public StopAction()
		{
			super(LanguageTranslator.getString("stop"));
			setEnabled(false);
		}

		public void actionPerformed(ActionEvent e)
		{
			performStop();
		}
	}

	class ChangeHighlightStylesAction extends AbstractAction
	{
		public ChangeHighlightStylesAction()
		{
			super(LanguageTranslator.getString("change.highlight.styles"));
			setEnabled(true);
		}

		public void actionPerformed(ActionEvent e)
		{
			performChooseHighlightStyles();
		}
	}

	class ChangeFontAction extends AbstractAction
	{
		public ChangeFontAction()
		{
			super(LanguageTranslator.getString("change.font"));
			setEnabled(true);
		}

		public void actionPerformed(ActionEvent e) {
			performChangeFont();
		}
	}

    class ChangeLanguageAction extends AbstractAction
	{
		public ChangeLanguageAction()
		{
			super(LanguageTranslator.getString("change.lang"));
			setEnabled(true);
		}

		public void actionPerformed(ActionEvent e) {
			performLanguageFont();
		}
	}

	class HelpAction extends AbstractAction
	{
		public HelpAction()
		{
			super(LanguageTranslator.getString("help"));
			setEnabled(true);
		}

		public void actionPerformed(ActionEvent e)
		{
			performHelp();
		}
	}

    class ContactsAction extends AbstractAction
	{
		public ContactsAction()
		{
			super(LanguageTranslator.getString("contacts"));
			setEnabled(true);
		}

		public void actionPerformed(ActionEvent e)
		{
            performContacts();
        }
    }

//********************** Инициализация интерфейса **************************//

	// Конструктор окна - инициализация всего интерфейса
	public MainWindow()
	{
		super("MainWindow");

		/*TODO get available memory size*/
		timer.setRepeats(false);
		codeField = new JTextPane();
		regsField = new JTextPane();
		inputField = new JTextPane();
		outputField = new JTextPane();
		allTextPanes = new JTextPane[]{codeField,regsField,inputField,outputField};

		codeField.setCaretPosition(0);
		codeField.setMargin(new Insets(5,5,5,5));
		JPanel codePanel=new JPanel(new BorderLayout());

		JLabel cLabel=new JLabel(LanguageTranslator.getString("source.code"));
		cLabel.setPreferredSize(new Dimension(50,15));
		codePanel.add(cLabel, BorderLayout.PAGE_START);
		codePanel.add(codeField, BorderLayout.CENTER);

		JPanel regsPanel=new JPanel(new BorderLayout());
		JLabel jLabel=new JLabel(LanguageTranslator.getString("registers"));
		jLabel.setPreferredSize(new Dimension(50,15));
		regsPanel.add(jLabel, BorderLayout.PAGE_START);
		regsPanel.add(regsField, BorderLayout.CENTER);

		JPanel inputPanel=new JPanel(new BorderLayout());
		JLabel jLabel1=new JLabel(LanguageTranslator.getString("input.tape"));
		jLabel1.setPreferredSize(new Dimension(50,15));
		inputPanel.add(jLabel1, BorderLayout.PAGE_START);
		inputPanel.add(inputField, BorderLayout.CENTER);

		JPanel outputPanel=new JPanel(new BorderLayout());
		JLabel label=new JLabel(LanguageTranslator.getString("output.tape"));
		label.setPreferredSize(new Dimension(50,15));
		outputPanel.add(label, BorderLayout.PAGE_START);
		outputPanel.add(outputField, BorderLayout.CENTER);

		scrollPanes=new JScrollPane[]{
				new JScrollPane(codePanel),
				new JScrollPane(regsPanel),
				new JScrollPane(inputPanel),
				new JScrollPane(outputPanel)
		};
		//scrollPanes[0].setPreferredSize(new Dimension(400, 570));
		//scrollPanes[1].setPreferredSize(new Dimension(100, 570));
		//scrollPanes[2].setPreferredSize(new Dimension(100, 570));
		//scrollPanes[3].setPreferredSize(new Dimension(100, 570));

		//mainScrollPane=new JScrollPane(mainPane);
		//mainScrollPane.setPreferredSize(new Dimension(820,620));

		scrollPanes[1].setVisible(false);
		regsField.setEditable(false);
		outputField.setEditable(false);

		//Create the status area.
		JPanel statusPane = new JPanel(new GridLayout(1, 1));
		statusPane.add(caretListenerLabel);

		//Split panes initialization
		inputAndOutputSplitPane=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,scrollPanes[2],scrollPanes[3]);
		regsAndInputAndOutputSplitPane=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,scrollPanes[1],inputAndOutputSplitPane);
		mainSplitPane=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,scrollPanes[0],inputAndOutputSplitPane);

        initUserDimensions();

		mainSplitPane.setResizeWeight(1);
		inputAndOutputSplitPane.setResizeWeight(1);
		regsAndInputAndOutputSplitPane.setResizeWeight(1);
		//mainSplitPane.setPreferredSize(new Dimension(600,400));
        mainSplitPane.setPreferredSize(new Dimension(dimensionParameters[0],dimensionParameters[1]));
		mainSplitPane.setDividerLocation(dimensionParameters[2]);
		inputAndOutputSplitPane.setDividerLocation(dimensionParameters[3]);
		scrollPanes[0].setMinimumSize(new Dimension(200,200));
		this.setMinimumSize(new Dimension(550,250));

		//Add the components.
		getContentPane().add(mainSplitPane,BorderLayout.CENTER);
		getContentPane().add(statusPane, BorderLayout.PAGE_END);

		//Set up the menu bar.
		createActionTable(codeField);
		JMenu fileMenu = createFileMenu();
		JMenu editMenu = createEditMenu();
		JMenu runMenu = createRunMenu();
		JMenu optionsMenu=createOptionsMenu();
		JMenu helpMenu = createHelpMenu();
		JMenuBar mb = new JMenuBar();
		mb.add(fileMenu);
		mb.add(editMenu);
		mb.add(runMenu);
		mb.add(optionsMenu);
		mb.add(helpMenu);
		setJMenuBar(mb);

		// добавляет некоторые горячие клавиши
		addBindings();

		//создание нового документа
		newFile();

		//Start watching for undoable edits and caret changes.
		//InputMethodEvent ev;
		codeField.addKeyListener(new EnterKeyListener());
		codeField.addCaretListener(caretListenerLabel);
		caretListenerLabel.displayCaretInfo();

		//инициализация цветов подсветки
		initUserSettings();
	}

	private void addBindings()
	{
		InputMap map=codeField.getInputMap();
		inputField.getInputMap().setParent(map);
		regsField.getInputMap().setParent(map);
		outputField.getInputMap().setParent(map);
		map.put(KeyStroke. getKeyStroke(java.awt.event.KeyEvent.VK_BACK_SPACE, Event.ALT_MASK),undoAction);
	}

	//Create the file menu.
	protected JMenu createFileMenu()
	{
		JMenu menu = new JMenu(LanguageTranslator.getString("file"));
		newFileAction=new NewFileAction();
		openFileAction=new OpenFileAction();
		saveFileAction=new SaveFileAction();
		saveFileAsAction=new SaveFileAsAction();
		JMenuItem item;
		item=menu.add(newFileAction);
		item.setAccelerator(KeyStroke. getKeyStroke(KeyEvent.VK_N, Event.CTRL_MASK));
		item=menu.add(openFileAction);
		item.setAccelerator(KeyStroke. getKeyStroke(KeyEvent.VK_O, Event.CTRL_MASK));
		item=menu.add(saveFileAction);
		item.setAccelerator(KeyStroke. getKeyStroke(KeyEvent.VK_S, Event.CTRL_MASK));
		menu.add(saveFileAsAction);
		return menu;
	}

	//Create the edit menu.
	protected JMenu createEditMenu()
	{
		JMenu menu = new JMenu(LanguageTranslator.getString("edit"));

		JMenuItem item;

		//Undo and redo are actions of our own creation.
		undoAction = new UndoAction();
		item=menu.add(undoAction);
		item.setAccelerator(KeyStroke. getKeyStroke(KeyEvent.VK_Z, Event.CTRL_MASK));
		redoAction = new RedoAction();
		item=menu.add(redoAction);
		item.setAccelerator(KeyStroke. getKeyStroke(KeyEvent.VK_Y, Event.CTRL_MASK));
		menu.addSeparator();

		//These actions come from the default editor kit.
		//Get the ones we want and stick them in the menu.
		item=menu.add(getActionByName(DefaultEditorKit.cutAction));
		item.setAccelerator(KeyStroke. getKeyStroke(KeyEvent.VK_X, Event.CTRL_MASK));
		item=menu.add(getActionByName(DefaultEditorKit.copyAction));
		item.setAccelerator(KeyStroke. getKeyStroke(KeyEvent.VK_C, Event.CTRL_MASK));
		item=menu.add(getActionByName(DefaultEditorKit.pasteAction));
		item.setAccelerator(KeyStroke. getKeyStroke(KeyEvent.VK_V, Event.CTRL_MASK));
		menu.addSeparator();

		item=menu.add(getActionByName(DefaultEditorKit.selectAllAction));
		item.setAccelerator(KeyStroke. getKeyStroke(KeyEvent.VK_A, Event.CTRL_MASK));
		return menu;
	}

	//Create the run menu.
	protected JMenu createRunMenu()
	{
		JMenu menu = new JMenu(LanguageTranslator.getString("run"));
		runAction=new RunAction();
		nextLineAction=new NextLineAction();
		runStepByStepAction=new RunStepByStepAction();
		runToCursorAction=new RunToCursorAction();
		stopAction=new StopAction();
		JMenuItem item;
		item=menu.add(runAction);
		item.setAccelerator(KeyStroke. getKeyStroke(KeyEvent.VK_F5, 0));
		item=menu.add(runStepByStepAction);
		item.setAccelerator(KeyStroke. getKeyStroke(KeyEvent.VK_F9, Event.SHIFT_MASK));
		item=menu.add(nextLineAction);
		item.setAccelerator(KeyStroke. getKeyStroke(KeyEvent.VK_F9, 0));
		item=menu.add(runToCursorAction);
		item.setAccelerator(KeyStroke. getKeyStroke(KeyEvent.VK_F7, 0));
		item=menu.add(stopAction);
		item.setAccelerator(KeyStroke. getKeyStroke(KeyEvent.VK_ESCAPE, 0));
		return menu;
	}

	protected JMenu createOptionsMenu()
	{
		JMenu menu=new JMenu(LanguageTranslator.getString("options"));
		сhangeHighlightStylesAction = new ChangeHighlightStylesAction();
		changeFontAction = new ChangeFontAction();
        changeLanguageAction=new ChangeLanguageAction();
		menu.add(сhangeHighlightStylesAction);
		//item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, Event.CTRL_MASK));
		menu.add(changeFontAction);
		//item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, Event.CTRL_MASK));
        menu.add(changeLanguageAction);
		return menu;
	}

	// Create help menu
	protected JMenu createHelpMenu()
	{

		JMenu menu=new JMenu(LanguageTranslator.getString("help"));
		helpAction=new HelpAction();
		JMenuItem item=menu.add(helpAction);
        item.setAccelerator(KeyStroke. getKeyStroke(KeyEvent.VK_F1, 0));
        contactsAction=new ContactsAction();
        menu.add(contactsAction);

		return menu;

	}

		//The following two methods allow us to find an
	//action provided by the editor kit by its name.
	private void createActionTable(JTextComponent textComponent)
	{
		actions = new HashMap();
        boolean ruLang=Locale.getDefault().getLanguage().equals("ru");
		Action[] actionsArray = textComponent.getActions();
		for (int i = 0; i < actionsArray.length; i++)
		{
			Action a = actionsArray[i];
            String engName=(String)a.getValue(Action.NAME);


            if(engName.equals(DefaultEditorKit.cutAction))
                a.putValue(Action.NAME,ruLang?"Вырезать":engName);
            if(engName.equals(DefaultEditorKit.copyAction))
                a.putValue(Action.NAME,ruLang?"Копировать":engName);
            if(engName.equals(DefaultEditorKit.pasteAction))
                a.putValue(Action.NAME,ruLang?"Вставить":engName);
            if(engName.equals(DefaultEditorKit.selectAllAction))
                a.putValue(Action.NAME,ruLang?"Выделить всё":engName);
            actions.put(engName,a);

		}
	}

	// Генерит таблицу стандартных action-ов для codeField
	private Action getActionByName(String name)
	{
		return (Action)actions.get(name);
	}


//********************** Методы, вызываемые на события из меню из соответствующих классов-обработчиков событий **************************//

	//Спрашивает юзера, хочет ли он сохранить несохраненный файл и создает новый
	private int performNewFile()
	{
		performStop();
		if(documentSaved)
		{
			return newFile();
		}
		else
		{
			Object[] options={LanguageTranslator.getString("yes"), LanguageTranslator.getString("no"), LanguageTranslator.getString("cancel")};
			int result = JOptionPane.showOptionDialog(this, LanguageTranslator.getString("would.you.like.to.save.opened.document"), LanguageTranslator.getString("confirm1"),JOptionPane.YES_NO_CANCEL_OPTION,JOptionPane.QUESTION_MESSAGE,null,options,options[2]);
			switch(result)
			{
				case 0://сейвим документ
					if(this.performSaveDocument()!=0)
					{
						System.out.println(LanguageTranslator.getString("can.t.save.document1"));
						return -1;
					}
				case 1://не сейвим документ
					return newFile();
				case 2://отмена
				default:
					return -1;
			}
		}


	}

	//Спрашивает юзера, хочет ли он сохранить несохраненный файл и открывает документ
	protected int performOpenDocument()
	{
		if (documentSaved)
		{
			return open();
		}
		else
		{
			Object[] options={LanguageTranslator.getString("yes"), LanguageTranslator.getString("no"), LanguageTranslator.getString("cancel")};
			int result = JOptionPane.showOptionDialog(this, LanguageTranslator.getString("would.you.like.to.save.opened.document"), LanguageTranslator.getString("confirm1"),JOptionPane.YES_NO_CANCEL_OPTION,JOptionPane.QUESTION_MESSAGE,null,options,options[2]);
			switch(result)
			{
				case 0://сейвим документ
					if(this.performSaveDocument()!=0)
					{
						System.out.println(LanguageTranslator.getString("can.t.save.document1"));
						return -1;
					}
				case 1://не сейвим документ
					return open();
				case 2://отмена
				default:
					return -1;
			}
		}
	}

	//сохранение документа 0 - success, -1 - не засейвили
	protected int performSaveDocument()
	{
		if(openedFile==null)
			return performSaveAsDocument();
		else
			return save();
	}

	//сохранение документа через диаложек 0 - success, -1 - не засейвили
	protected int performSaveAsDocument() //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! добавить отображение имени файла !!!!!!!!!!!!!!!!!!!!!!!!!!
	{
		if(fileChooser==null)
			initializeFileChooser();
		int result=fileChooser.showSaveDialog(this);
		switch(result)
		{
			case JFileChooser.APPROVE_OPTION://сохраняем документ
				this.openedFile=fileChooser.getSelectedFile();
				//НАДО НАПИСАТЬ ДОБАВЛЕНИЕ РАСШИРЕНИЯ!!!
				return save();
			case JFileChooser.ERROR_OPTION://какая-то ошибка:)
				System.err.println(LanguageTranslator.getString("error.while.opening.file"));
			case JFileChooser.CANCEL_OPTION://отмена
		}
		return -1;
	}

	// запуск кода до конца
	private void performRun()
	{
		run();
	}

	//переход в debug-режим. появление выходной ленты и просмотрщика регистров. запуск пошагового режима.
	private void performRunStepByStep()
	{
		if (compile()==-1)
			return;
		setDebugMode(true);
		RAMWorker ramWorker=new RAMWorker()
		{
			protected RAMException doInBackground()
			{
				try
				{
					ram.start();
				}
				catch (RAMException e)
				{
					return e;
				}
				return null;
			}
		};
		ramWorker.execute();
	}

	// команда перехода на следующую строку кода (debug-mode)
	private void performNextLine()
	{
		debugRun(false);
	}

	// запустить до курсора (debug-mode)
	private void performRunToCursor()
	{
		debugRun(true);
	}

	// остановка машины - завершение debug-режима
	private void performStop()
	{
		ram.stop();
		setRunMode(false);
		setDebugMode(false);
		if(codeSwitched)
		{
    		//switchDocuments();
		}
	}

	// отображение диалога выбора стилей подсветки
	private void performChooseHighlightStyles()
	{
		ChooseHighlightStylesDialog chooseHighlightStylesDialog =new ChooseHighlightStylesDialog(this, highlightStyles,codeField.getFont(),ram);
		chooseHighlightStylesDialog.showDialog();
		TreeMap<CodeSubstringType,ColoredFont> chosenHighlightStyles= chooseHighlightStylesDialog.getHighlightStyles();
		if (chosenHighlightStyles!=null)
			highlightStyles =chosenHighlightStyles;
		runSyntaxHighlight(doc);
	}

	private int performChangeFont()
	{
		Font oldFont = codeField.getFont();
		ChooseFontDialog chooseFontDialog = new ChooseFontDialog(oldFont);
		Font chosenFont = chooseFontDialog.showDialog();
		for (JTextPane tp:allTextPanes) {
			tp.setFont(chosenFont);
		}
		saveSettings();
		return 0;
	}
    private void performLanguageFont()
    {
        languageChanged=true;
        saveSettings();
        JOptionPane.showMessageDialog(this,LanguageTranslator.getString("change_language_message"));
    }


	// отображение справки
	private void performHelp()
	{
		JFrame helpFrame=new JFrame(LanguageTranslator.getString("help.on.ram"));
		java.net.URL helpURL = MainWindow.class.getResource(
                                LanguageTranslator.getString("helpname"));
		JScrollPane help;
		try
		{
			JEditorPane jEditorPane=new JEditorPane(helpURL);
			help=new JScrollPane(jEditorPane);
			jEditorPane.setMargin(new Insets(5,5,5,5));
		}
		catch(IOException e)
		{
			e.printStackTrace();
			return;
		}
		helpFrame.add(help);
		help.setPreferredSize(new Dimension(640,480));
		helpFrame.pack();
		helpFrame.setVisible(true);
	}
        //отображение контактов
    private void performContacts()
    {
        JOptionPane.showMessageDialog(this, "e-mail: Mikhail.Chernykh@phystech.edu\ne-mail: adalxx@gmail.com", LanguageTranslator.getString("contacts"),JOptionPane.INFORMATION_MESSAGE);
    }

//************************ Вспомогательные методы, запускаются из методов, описанных выше ******************************//

	// функция вызывается из performNewFile
	private int newFile()
	{
		doc=new MyDocument();
		codeField.setDocument(doc);
		openedFile=null;
		undo = new MyUndoManager();

        doc.addUndoableEditListener(new MyUndoableEditListener());
		docListener=new MyDocumentListener();
        doc.addDocumentListener(docListener);
		undoAction.updateUndoState();
		redoAction.updateRedoState();
		documentSaved=true;
		this.setTitle(applicationName+ LanguageTranslator.getString("unnamed.ram"));
		return 0;
	}

	//функция вызывается из performSaveDocument и performSaveAsDocument, сохраняет используя данные из openedFile!!!
	private int save()
	{
		if(openedFile==null)
			return -1;
		try
		{
			FileWriter fileWriter=new FileWriter(openedFile);
			fileWriter.write(codeField.getText());
			fileWriter.close();
			this.documentSaved=true;
		}
		catch(IOException e)
		{
			JOptionPane.showMessageDialog(this, LanguageTranslator.getString("can.t.save.document"));
			e.printStackTrace();
			return -1;
		}
		this.setTitle(applicationName+" - "+openedFile.getName());
		return 0;
	}
	// функция вызывается из performOpenDocument
	private int open()
	{
		performStop();

		if(fileChooser==null)
			initializeFileChooser();
		int result=fileChooser.showOpenDialog(this);
		switch(result)
		{
			case JFileChooser.APPROVE_OPTION://открываем файл!
				this.openedFile=fileChooser.getSelectedFile();
				try
				{
					Scanner inputFileScanner=new Scanner(openedFile);
					String newText="";
					while(inputFileScanner.hasNextLine())
						newText+=inputFileScanner.nextLine()+newline;
					inputFileScanner.close();
					undo = new MyUndoManager();
					doc=new MyDocument();					
					docListener=new MyDocumentListener();
					doc.addDocumentListener(docListener);
					undoAction.updateUndoState();
					redoAction.updateRedoState();

					codeField.setDocument(doc);
					codeField.setText(newText);
					doc.addUndoableEditListener(new MyUndoableEditListener());
					documentSaved=true;
				}
				catch(IOException e)
				{
					JOptionPane.showMessageDialog(this, LanguageTranslator.getString("can.t.open.document"));
					//e.printStackTrace();
					return -1;
				}
				this.setTitle(applicationName+" - "+openedFile.getName());
				//javax.swing.SwingUtilities.invokeLater(new RunnableSplit(0,doc.getLength(),doc));
				//runSyntaxHighlight();
				return 0;
			case JFileChooser.ERROR_OPTION://какая-то ошибка:)
				System.err.println(LanguageTranslator.getString("error.while.opening.file"));
			case JFileChooser.CANCEL_OPTION://отмена
			default:
				return -1;
		}
	}

	// компиляция кода
	private int compile()
	{
		String inputText=inputField.getText();
		inputQueue= SyntaxChecker.generateInputTape(inputText);
		outputQueue.clear();
		ram.setInput(inputQueue);
		SyntaxChecker syntaxChecker=new SyntaxChecker(ram);
		ArrayList<CmdStruct> code=null;
		try
		{
			//codeCopy=constructCorrectDocument(codeField.getText());
			code=syntaxChecker.compileCode(codeField.getText());
		}
		catch(RAMException e)
		{
			performStop();
			caretListenerLabel.displayErrorInfo(e);
		}
		if(code==null)
		{
			return -1;
		}
		else
		{
			//codeCopy=constructCorrectDocument(syntaxChecker.getCompiledCode());
			//switchDocuments();
        }
		ram.setCmdList(code);
		return 0;
	}

	// запуск машины (в новом потоке)
	private void run()
	{
		setRunMode(true);
		if (!debugModeOn)
			if (compile()==-1)
				return;
		RAMWorker ramWorker=new RAMWorker()
		{
			protected RAMException doInBackground()
			{
				try
				{
					ram.run();
				}
				catch (RAMException e)
				{
					return e;
				}
				return null;
			}
		};
		caretListenerLabel.displaySuccessMessage(LanguageTranslator.getString("in.run"));
		ramWorker.execute();
	}

	/* следующая комманда при дебаге (в новом потоке)
	 	toCursor==true - запуск до курсора
	 	toCursor==false - запуск следующей комманды
	 запускается из performRunToCursor и performNextLine */
	private void debugRun(boolean toCursor)
	{
		if (ram.isStarted())
		{
			setRunMode(true);
			RAMWorker ramWorker;
			if (toCursor)
			{
				ramWorker=new RAMWorker()
				{
					protected RAMException doInBackground()
					{
						try
						{
							ram.runToLine(getLineAtCaret(codeField));
						}
						catch (RAMException e)
						{
							return e;
						}
						return null;
					}
				};
				caretListenerLabel.displaySuccessMessage(LanguageTranslator.getString("in.run"));
			}
			else
			{
				ramWorker=new RAMWorker()
				{
					protected RAMException doInBackground()
					{
						try
						{
							ram.nextCommand();
						}
						catch (RAMException e)
						{
							return e;
						}
						return null;
					}
				};
			}
			ramWorker.execute();
		}
		else
			performStop();
	}

	// выполняется, если машина отработала действие корректно (вызывается из ramDoneAction
	private void ramOKAction()
	{
		caretListenerLabel.displayCaretInfo();
		if (!ram.isStarted())
			performStop();
		else
		{
			setRunMode(false);
			setDebugMode(true);
			printRegs();
			changeLineHighlight(ram.getCurrentCmd(), DEFAULT_DEBUG_HIGHLIGHT_COLOR);
		}
	}

	// обработка результатов выполнения действия машиной
	private void ramDoneAction(RAMException e)
	{
		printOutput();
		if (e==null)
		{
			ramOKAction();
			return;
		}
		if (e.getErrorLine()==-666) // возможно, бесконечный цикл (предупреждение)
			ramOKAction();
		else 						// ошибка
		{
			ram.stop();
			setRunMode(true);
		}
		caretListenerLabel.displayErrorInfo(e);
		setRegsVisible(true);
		printRegs();
	}


	// включение-отключение debug-режима
	private void setDebugMode(boolean mode)
	{
		if(!mode)
		{
			codeField.getHighlighter().removeAllHighlights();
		}
		runToCursorAction.setEnabled(mode);
		runStepByStepAction.setEnabled(!mode);
        if(mode)
        {
            undoAction.setEnabled(!mode);
		    redoAction.setEnabled(!mode);
        }
        else
        {
            undoAction.updateUndoState();
            redoAction.updateRedoState();
        }
        stopAction.setEnabled(mode);
		codeField.setEditable(!mode);
		inputField.setEditable(!mode);
		if (inputField.hasFocus())
			inputField.getCaret().setVisible(!mode);
        setRegsVisible(mode);
		caretListenerLabel.additionalStr="";
		caretListenerLabel.displayCaretInfo();
		nextLineAction.setEnabled(mode);
		debugModeOn=mode;
		this.validate();
		this.repaint();

	}

	// включение-отключение режима работы потока машины рам
	private void setRunMode(boolean mode)
	{
		if (mode)
		{
			codeField.getHighlighter().removeAllHighlights();
			codeField.setEditable(!mode);
			inputField.setEditable(!mode);
		}
		caretListenerLabel.additionalStr="";
		runToCursorAction.setEnabled(!mode);
		runStepByStepAction.setEnabled(!mode);
		stopAction.setEnabled(mode);
		runAction.setEnabled(!mode);
		nextLineAction.setEnabled(!mode);
	}

	// показывает/скрывает регистры
	private void setRegsVisible(boolean visible)
	{
		if (scrollPanes[1].isVisible()==visible)
			return;
		scrollPanes[1].setVisible(visible);
		int dividerLocation=mainSplitPane.getDividerLocation();
			final int regsWidth=visible?105:regsAndInputAndOutputSplitPane.getDividerLocation();
			final int effectiveRegsWidth=regsWidth+regsAndInputAndOutputSplitPane.getDividerSize();
			if(visible)
			{
				mainSplitPane.setRightComponent(regsAndInputAndOutputSplitPane);
				regsAndInputAndOutputSplitPane.setDividerLocation(regsWidth);
				regsAndInputAndOutputSplitPane.setRightComponent(inputAndOutputSplitPane);
			}
			else
			{
				mainSplitPane.setRightComponent(inputAndOutputSplitPane);
			}
			int newLocation=dividerLocation+(visible?-effectiveRegsWidth:effectiveRegsWidth);
			if((newLocation>5)&&(newLocation<mainSplitPane.getSize().getWidth()))
				mainSplitPane.setDividerLocation(dividerLocation+(visible?-effectiveRegsWidth:effectiveRegsWidth));
	}

	private void initUserSettings () {
		initHighlightStyles();
		initDefaultFont();
	}

    private static void initUserLanguage()
    {
        try
        {
            Scanner scanner;
            File userSettings = new File(USER_SETTINGS_FILE_NAME);
            if (userSettings.exists())
            {
                scanner = new Scanner(userSettings);

                while (scanner.hasNextLine())
                {
                    String line = scanner.nextLine();
                    Pattern p = Pattern.compile(LANGUAGE_SETTINGS_PATTERN);
                    Matcher m = p.matcher(line);
                    if (m.matches())
                    {
                        if(m.group(2)!=null)
                        {
                            setRuLang=true;
                        }
                    }
                }
                scanner.close();
            }
        }
        catch (FileNotFoundException e1)
        {
            e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        catch (IOException e)
        {
            // ignoring
        }
    }

	private void initDefaultFont() {
		try {
			Scanner scanner;
			File userSettings = new File(USER_SETTINGS_FILE_NAME);
			if (userSettings.exists())
			{
				scanner=new Scanner(userSettings);

				while (scanner.hasNextLine()) {
					String line = scanner.nextLine();
					Pattern p = Pattern.compile(FONT_SETTINGS_PATTERN);
					Matcher m = p.matcher(line);
					if (m.matches()) {
						MatchResult matchRes = m.toMatchResult();
						String fontName = matchRes.group(1);
						int fontSize = new Integer(matchRes.group(2));
						Font font = new Font(fontName,0, fontSize);
						if (font!=null) {
							for (JTextPane tp:allTextPanes) {
								tp.setFont(font);
							}
						}
						break;
					}
				}
				scanner.close();
			}
		} catch (IOException e) {
			// ignoring
		}

	}

	// заполняет таблицу подсветок
	private void initHighlightStyles()
	{
		highlightStyles =new TreeMap<CodeSubstringType,ColoredFont>();
		try
		{
			Scanner inputFileScanner;
			File userSettings = new File(USER_SETTINGS_FILE_NAME);
			if (userSettings.exists())
			{
				inputFileScanner=new Scanner(userSettings);
			}
			else
			{
				URL url = MainWindow.class.getResource(DEFAULT_HIGHLIGHT_SETTINGS);
				inputFileScanner=new Scanner(url.openStream());
			}
			while(inputFileScanner.hasNextLine())
			{
				String line=inputFileScanner.nextLine();
				Pattern p= Pattern.compile(HIGHLIGHT_SETTINGS_PATTERN);
				Matcher matcher=p.matcher(line);
				if (matcher.matches())
				{
					String type = matcher.toMatchResult().group(1);
                    String name;
                    try {
					    name = LanguageTranslator.getString(type);
                    } catch (MissingResourceException e) {
                        name = type;
                    }
					String styleStr=matcher.toMatchResult().group(2);
					String colorStr=matcher.toMatchResult().group(3);
					int style = new Integer(styleStr);
					int colorRGB = new Integer(colorStr);
					if (CodeSubstringType.valueOf(type)!=null)
					{
						highlightStyles.put(CodeSubstringType.valueOf(type),new ColoredFont(name,style,new Color(colorRGB)));
					}
				}
			}
		}
		catch(IOException e)
		{
			// ignore
		}
        for (CodeSubstringType type: CodeSubstringType.values())
		{
			if (!highlightStyles.containsKey(type))
			{
				highlightStyles.put(type,null);
			}
		}
	}
    //инициализирует запомненные с прошлого раза размеры основных трёх панелек
    private void initUserDimensions()
    {
        try
        {
            Scanner scanner;
            File userSettings = new File(USER_SETTINGS_FILE_NAME);
            if (userSettings.exists())
            {
                scanner = new Scanner(userSettings);

                while (scanner.hasNextLine())
                {
                    String line = scanner.nextLine();
                    Pattern p = Pattern.compile(DIMENSIONS_SETTINGS_PATTERN);
                    Matcher m = p.matcher(line);
                    if (m.matches())
                    {
                        int paramIndex=new Integer(m.group(1));
                        int dim=new Integer(m.group(2));
                        dimensionParameters[paramIndex]=dim;
                    }
                }
                scanner.close();
            }
        }
        catch (FileNotFoundException e1)
        {
            e1.printStackTrace();
        }
        catch (IOException e)
        {
            // ignoring
        }

    }

	private void initHighlightStylesManually()
	{
		highlightStyles.put(CodeSubstringType.LABEL_DEFINITION,new ColoredFont(LanguageTranslator.getString("label.definition"),Font.BOLD,new Color(102,14,122)));
		highlightStyles.put(CodeSubstringType.LABEL_CALL,new ColoredFont(LanguageTranslator.getString("label.call"),Font.PLAIN,new Color(102,14,122)));
		highlightStyles.put(CodeSubstringType.MACRO_DEFINITION_START,new ColoredFont(LanguageTranslator.getString("macro.start.definition"),Font.BOLD|Font.ITALIC,new Color(0,128,0)));
		highlightStyles.put(CodeSubstringType.MACRO_DEFINITION_END,new ColoredFont(LanguageTranslator.getString("macro.end.definition"),Font.BOLD|Font.ITALIC,new Color(0,128,0)));
		highlightStyles.put(CodeSubstringType.MACRO_NAME_DEFINITION,new ColoredFont(LanguageTranslator.getString("macro.name"),Font.ITALIC,new Color(0,0,0)));
		//highlightStyles.put(CodeSubstringType.MACRO_ARGUMENT_DEFINITION,new ColoredFont("Macro arguments definition",Font.PLAIN,new Color(0,0,0)));
		//highlightStyles.put(CodeSubstringType.MACRO_ARGUMENT,new ColoredFont("Macro arguments",Font.PLAIN,new Color(0,0,0)));
		highlightStyles.put(CodeSubstringType.MACRO_CALL,new ColoredFont(LanguageTranslator.getString("macro.call"),Font.BOLD|Font.ITALIC,new Color(0,0,128)));
		highlightStyles.put(CodeSubstringType.COMMAND,new ColoredFont(LanguageTranslator.getString("command.call"),Font.BOLD,new Color(0,0,128)));
		highlightStyles.put(CodeSubstringType.LABEL_TWO_POINTS,new ColoredFont(LanguageTranslator.getString("colon.after.label"),Font.PLAIN,new Color(0,0,0)));
		highlightStyles.put(CodeSubstringType.STAR,new ColoredFont(LanguageTranslator.getString("reference.symbol"),Font.PLAIN,new Color(0,170,60)));
		highlightStyles.put(CodeSubstringType.EQUAL,new ColoredFont(LanguageTranslator.getString("simple.number.simbol"),Font.PLAIN,new Color(0,128,255)));
		highlightStyles.put(CodeSubstringType.NUMBER,new ColoredFont(LanguageTranslator.getString("number.in.argument"),Font.PLAIN,new Color(0,0,255)));
		highlightStyles.put(CodeSubstringType.SYMBOL,new ColoredFont(LanguageTranslator.getString("symbol.in.argument"),Font.PLAIN,new Color(0,0,255)));
		highlightStyles.put(CodeSubstringType.COMMENT,new ColoredFont(LanguageTranslator.getString("comments"),Font.ITALIC,new Color(153,153,153)));
		highlightStyles.put(CodeSubstringType.INVALID_LABEL_DEFINITION,new ColoredFont(LanguageTranslator.getString("invalid.labeldefinition"),Font.PLAIN,new Color(255,0,0)));
		highlightStyles.put(CodeSubstringType.INVALID_MACRO_START,new ColoredFont(LanguageTranslator.getString("invalid.macro.start.definition"),Font.ITALIC,new Color(255,0,0)));
		highlightStyles.put(CodeSubstringType.INVALID_MACRO_END,new ColoredFont(LanguageTranslator.getString("invalid.macro.end.definition"),Font.ITALIC,new Color(255,0,0)));
		highlightStyles.put(CodeSubstringType.INVALID_MACRO_NAME,new ColoredFont(LanguageTranslator.getString("invalid.macro.name"),Font.ITALIC,new Color(255,0,0)));
		highlightStyles.put(CodeSubstringType.INVALID_COMMAND,new ColoredFont(LanguageTranslator.getString("unknown.command"),Font.PLAIN,new Color(255,0,0)));
		highlightStyles.put(CodeSubstringType.INVALID_ARGUMENT,new ColoredFont(LanguageTranslator.getString("invalid.argument"),Font.PLAIN,new Color(255,0,0)));
		highlightStyles.put(CodeSubstringType.TRASH,new ColoredFont(LanguageTranslator.getString("unrecognized.structure"),Font.PLAIN,new Color(255,0,0)));
		highlightStyles.put(CodeSubstringType.ARGUMENT,new ColoredFont(LanguageTranslator.getString("unknown.command.argument"),Font.PLAIN,new Color(255,0,0)));
	}

	// saves all highlight styles chosen by user to file
	private void saveSettings()
	{
		try
		{
			File settingsFile = new File(USER_SETTINGS_FILE_NAME);
			FileWriter fileWriter=new FileWriter(settingsFile);
			for (CodeSubstringType type: highlightStyles.keySet())
			{
				ColoredFont font = highlightStyles.get(type);
				if (font!=null)
					fileWriter.write(type+"\t"+font.getStyle()+"\t"+font.getColor().getRGB()+"\n");
			}
			Font font = codeField.getFont();
			fileWriter.write("DEFAULT_FONT\t["+font.getFontName()+"]\t"+font.getSize()+"\n");
            refreshDimensions();
            for(int i=0;i<dimensionParameters.length;i++)
            {
                fileWriter.write("DIMENSIONS\t["+i+"]\t"+dimensionParameters[i]+"\n");
            }
            fileWriter.write("LANGUAGE\t"+((languageChanged?!setRuLang:setRuLang)?"ru":"en"+"\n"));

			fileWriter.close();
		}
		catch (IOException e)
		{
			// do nothing
		}
	}

    private void refreshDimensions()
    {
        dimensionParameters[0] = mainSplitPane.getSize().width;
        dimensionParameters[1] = mainSplitPane.getSize().height;
        dimensionParameters[2] = mainSplitPane.getDividerLocation();
        dimensionParameters[3] = inputAndOutputSplitPane.getDividerLocation();
    }

    // Запускает подсветку синтаксиса
	private void runSyntaxHighlight(MyDocument docForHighLight)
	{
		syntaxHighlightWorker =new SyntaxHighlightWorker(ram, docForHighLight, highlightStyles);
		syntaxHighlightWorker.execute();
	}

	// смена подсвечиваемой строки (debug-mode)
	//возвращает true в случае успеха
	private boolean changeLineHighlight(int lineIndexToHighLight,Color clrToHighlight)
	{
		codeField.getHighlighter().removeAllHighlights();
		Element rootElement=doc.getDefaultRootElement();
		if((lineIndexToHighLight<0)||(lineIndexToHighLight>=rootElement.getElementCount()))
		{
			System.out.println(LanguageTranslator.getString("can.t.select.line.with.such.number"));
			return false;
		}
		int startOffset=rootElement.getElement(lineIndexToHighLight).getStartOffset();
		int endOffset=rootElement.getElement(lineIndexToHighLight).getEndOffset();
		try
		{
				codeField.getHighlighter().addHighlight(startOffset,endOffset,new DefaultHighlighter.DefaultHighlightPainter(clrToHighlight));
		}
		catch(BadLocationException e)
		{
			e.printStackTrace();
		}
		return true;
	}

	// отображение содержимого регистров
	private void printRegs()
	{
		SwingWorker<String,Object> swingWorker=new SwingWorker<String,Object>()
		{
			/*protected String doInBackgroundOld()
			{
				String regs="";
				Integer reg;
				TreeMap<Integer,Integer> allRegs=new TreeMap<Integer,Integer>(ram.getAllRegisters());
				for (Integer i: allRegs.keySet())
				{
					reg=allRegs.get(i);
					if (reg==null)
						regs+="["+i+"]\t"+"?"+newline;
					else
						regs+="["+i+"]\t"+reg+newline;
				}
				return regs;
			}*/

			protected String doInBackground()
			{
				Integer reg;
				TreeMap<Integer,Integer> allRegs=new TreeMap<Integer,Integer>(ram.getAllRegisters());
				int length=0;
				ArrayList<String> strings = new ArrayList<String> (allRegs.keySet().size());

				for (Integer i: allRegs.keySet())
				{
					String s;
					reg=allRegs.get(i);
					if (reg==null)
						s="["+i+"]\t"+"?"+newline;
					else
						s="["+i+"]\t"+reg+newline;
					strings.add(s);
					length+=s.length();
				}
				return makeStringLinear(strings,length);
			}

			protected void done()
			{
				try
				{
					regsField.setText(get());
				}
				catch (Exception ignore)
				{
					// ignoring
				}
			}
		};
		swingWorker.execute();
	}

	// отображение содержимого выходной ленты
	private void printOutput()
	{
		SwingWorker<String,Object> swingWorker=new SwingWorker<String,Object>()
		{
			protected String doInBackground()
			{
				int length=0;
				ArrayList<String> strings = new ArrayList<String>(outputQueue.size());
				for (String s:outputQueue)
				{
					String str=s+newline;
					strings.add(str);
					length+=str.length();
				}
				return makeStringLinear(strings,length);
			}

			protected void done()
			{
				try
				{
					outputField.setText(get());
				}
				catch (Exception ignore)
				{
					// ignoring
				}
			}
		};
		swingWorker.execute();
	}

	// function to make one long string from array list using linear algorithm
	private String makeStringLinear(ArrayList<String> strings,int length)
	{
		char [] regs = new char [length];
		int i=0;
		for (String s: strings)
		{
			for (char c:s.toCharArray())
			{
				regs[i]=c;
				i++;
			}
		}

		return String.copyValueOf(regs);
	}

	private void switchDocuments()
	{
		MyDocument s=doc;
		doc=codeCopy;
		codeCopy=s;
		codeSwitched=!codeSwitched;
		codeField.setDocument(doc);
		this.runSyntaxHighlight(doc);
	}

	private void initializeFileChooser()
	{
		fileChooser=new JFileChooser();
		fileChooser.setFileFilter(new FileFilter()
		{
			public String getDescription() { return LanguageTranslator.getString("ram.code");}
			public boolean accept(File f)
			{
				return f.getName().endsWith(".ram")||(f.isDirectory());
			}
		});
		fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
	}

	private static MyDocument constructCorrectDocument(String code)
	{
		MyDocument result=new MyDocument();
		try
		{
			result.insertString(0,code,null);
		}
		catch(BadLocationException ex)
		{
			//never used
		}
		result.splitTextCharacter(0,code.length());
		return result;
	}

	/*
	**  Return the current line number at the Caret position.
	*/
	public static int getLineAtCaret(JTextComponent component)
	{
		int caretPosition = component.getCaretPosition();
		Element root = component.getDocument().getDefaultRootElement();
		return root.getElementIndex( caretPosition );
	}

	private static void setRuLang()
		{
			//UIManager.get()
			UIManager.put("FileChooser.fileDescriptionText", "\u0424\u0430\u0439\u043b");
			UIManager.put("FileChooser.directoryDescriptionText", "\u041a\u0430\u0442\u0430\u043b\u043e\u0433");
			UIManager.put("FileChooser.newFolderErrorText", "\u041e\u0448\u0438\u0431\u043a\u0430 \u0441\u043e\u0437\u0434\u0430\u043d\u0438\u044f \u043d\u043e\u0432\u043e\u0439 \u043f\u0430\u043f\u043a\u0438");
			UIManager.put("FileChooser.newFolderErrorSeparator", ":");
			UIManager.put("FileChooser.acceptAllFileFilterText", "\u0412\u0441\u0435 \u0444\u0430\u0439\u043b\u044b");
			UIManager.put("FileChooser.cancelButtonText", "\u041e\u0442\u043c\u0435\u043d\u0430");
			UIManager.put("FileChooser.saveButtonText", "\u0421\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c");
			UIManager.put("FileChooser.openButtonText", "\u041e\u0442\u043a\u0440\u044b\u0442\u044c");
			UIManager.put("FileChooser.updateButtonText", "\u041e\u0431\u043d\u043e\u0432\u0438\u0442\u044c");
			UIManager.put("FileChooser.helpButtonText", "\u041f\u043e\u043c\u043e\u0449\u044c");
			UIManager.put("FileChooser.directoryOpenButtonText", "\u041e\u0442\u043a\u0440\u044b\u0442\u044c");
			UIManager.put("FileChooser.openDialogTitleText", "\u041e\u0442\u043a\u0440\u044b\u0442\u044c");
			UIManager.put("FileChooser.saveDialogTitleText", "\u0421\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c");

			UIManager.put("FileChooser.cancelButtonToolTipText", "\u0417\u0430\u043a\u0440\u044b\u0442\u044c \u0434\u0438\u0430\u043b\u043e\u0433 \u0432\u044b\u0431\u043e\u0440\u0430 \u0444\u0430\u0439\u043b\u0430");
			UIManager.put("FileChooser.saveButtonToolTipText", "\u0421\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c \u0432\u044b\u0431\u0440\u0430\u043d\u043d\u044b\u0439 \u0444\u0430\u0439\u043b");
			UIManager.put("FileChooser.openButtonToolTipText", "\u041e\u0442\u043a\u0440\u044b\u0442\u044c \u0432\u044b\u0431\u0440\u0430\u043d\u043d\u044b\u0439 \u0444\u0430\u0439\u043b");
			UIManager.put("FileChooser.directoryOpenButtonToolTipText", "\u041e\u0442\u043a\u0440\u044b\u0442\u044c \u0432\u044b\u0431\u0440\u0430\u043d\u043d\u044b\u0439 \u043a\u0430\u0442\u0430\u043b\u043e\u0433");
			UIManager.put("FileChooser.updateButtonToolTipText", "\u041e\u0431\u043d\u043e\u0432\u0438\u0442\u044c \u0441\u043e\u0434\u0435\u0440\u0436\u0438\u043c\u043e\u0435 \u043a\u0430\u0442\u0430\u043b\u043e\u0433\u0430");
			UIManager.put("FileChooser.helpButtonToolTipText", "\u0412\u044b\u0437\u043e\u0432 \u0441\u043f\u0440\u0430\u0432\u043a\u0438");
			UIManager.put("ColorChooser.previewText", "\u041f\u0440\u0438\u043c\u0435\u0440");
			UIManager.put("ColorChooser.okText", "OK");
			UIManager.put("ColorChooser.cancelText", "\u041e\u0442\u043c\u0435\u043d\u0430");
			UIManager.put("ColorChooser.resetText", "\u0421\u0431\u0440\u043e\u0441");
			UIManager.put("ColorChooser.resetMnemonic", 82);

			UIManager.put("ColorChooser.sampleText", "\u041f\u0440\u0438\u043c\u0435\u0440 \u0422\u0435\u043a\u0441\u0442\u0430  \u041f\u0440\u0438\u043c\u0435\u0440 \u0422\u0435\u043a\u0441\u0442\u0430");

			UIManager.put("ColorChooser.swatchesNameText", "\u041f\u0430\u043b\u0438\u0442\u0440\u0430");
			UIManager.put("ColorChooser.swatchesDisplayedMnemonicIndex", "0");
			UIManager.put("ColorChooser.swatchesRecentText", "\u041f\u043e\u0441\u043b\u0435\u0434\u043d\u0438\u0435:");

			UIManager.put("ColorChooser.hsbNameText", "HSB");
			UIManager.put("ColorChooser.hsbDisplayedMnemonicIndex", "0");
			UIManager.put("ColorChooser.hsbHueText", "\u0426\u0432\u0435\u0442");
			UIManager.put("ColorChooser.hsbSaturationText", "\u041d\u0430\u0441\u044b\u0449\u0435\u043d\u043e\u0441\u0442\u044c");
			UIManager.put("ColorChooser.hsbBrightnessText", "\u042f\u0440\u043a\u043e\u0441\u0442\u044c");
			UIManager.put("ColorChooser.hsbRedText", "\u041a\u0440\u0430\u0441\u043d\u044b\u0439");
			UIManager.put("ColorChooser.hsbGreenText", "\u0417\u0435\u043b\u0451\u043d\u044b\u0439");
			UIManager.put("ColorChooser.hsbBlueText", "\u0421\u0438\u043d\u0438\u0439");

			UIManager.put("ColorChooser.rgbNameText", "RGB");
			UIManager.put("ColorChooser.rgbDisplayedMnemonicIndex", "1");
			UIManager.put("ColorChooser.rgbRedText", "\u041a\u0440\u0430\u0441\u043d\u044b\u0439");
			UIManager.put("ColorChooser.rgbGreenText", "\u0417\u0435\u043b\u0451\u043d\u044b\u0439");
			UIManager.put("ColorChooser.rgbBlueText", "\u0421\u0438\u043d\u0438\u0439");

			UIManager.put("OptionPane.yesButtonText", "\u0414\u0430");
			UIManager.put("OptionPane.noButtonText", "\u041d\u0435\u0442");
			UIManager.put("OptionPane.okButtonText", "OK");
			UIManager.put("OptionPane.cancelButtonText", "\u041e\u0442\u043c\u0435\u043d\u0430");
			UIManager.put("OptionPane.titleText", "\u0412\u044b\u0431\u0435\u0440\u0438\u0442\u0435 \u0432\u0430\u0440\u0438\u0430\u043d\u0442");

			UIManager.put("AbstractDocument.additionText", "\u0434\u043e\u0431\u0430\u0432\u043b\u0435\u043d\u0438\u0435");
			UIManager.put("AbstractDocument.deletionText", "\u0443\u0434\u0430\u043b\u0435\u043d\u0438\u0435");
			UIManager.put("AbstractDocument.redoText", "\u0412\u0435\u0440\u043d\u0443\u0442\u044c");
			UIManager.put("AbstractDocument.styleChangeText", "\u0441\u043c\u0435\u043d\u0443 \u0441\u0442\u0438\u043b\u044f");
			UIManager.put("AbstractDocument.undoText", "\u041e\u0442\u043c\u0435\u043d\u0438\u0442\u044c");

			UIManager.put("AbstractUndoableEdit.redoText", "\u0412\u0435\u0440\u043d\u0443\u0442\u044c");
			UIManager.put("AbstractUndoableEdit.undoText", "\u041e\u0442\u043c\u0435\u043d\u0438\u0442\u044c");

			UIManager.put("FormView.browseFileButtonText", "\u0412\u044b\u0431\u0440\u0430\u0442\u044c...");
			UIManager.put("FormView.resetButtonText", "\u041e\u0447\u0438\u0441\u0442\u0438\u0442\u044c");
			UIManager.put("FormView.submitButtonText", "\u041e\u0442\u043f\u0440\u0430\u0432\u0438\u0442\u044c");


			UIManager.put("IsindexView.prompt", "\u041f\u043e \u044d\u0442\u043e\u043c\u0443 \u0438\u043d\u0434\u0435\u043a\u0441\u0443 \u0432\u043e\u0437\u043c\u043e\u0436\u0435\u043d \u043f\u043e\u0438\u0441\u043a.  \u0412\u0432\u0435\u0434\u0438\u0442\u0435 \u0441\u043b\u043e\u0432\u0430 \u0434\u043b\u044f \u043f\u043e\u0438\u0441\u043a\u0430:");

			UIManager.put("InternalFrame.closeButtonToolTip", "\u0417\u0430\u043a\u0440\u044b\u0442\u044c");
			UIManager.put("InternalFrame.iconButtonToolTip", "\u0421\u0432\u0435\u0440\u043d\u0443\u0442\u044c");
			UIManager.put("InternalFrame.maxButtonToolTip", "\u0420\u0430\u0437\u0432\u0435\u0440\u043d\u0443\u0442\u044c");
			UIManager.put("InternalFrame.restoreButtonToolTip", "\u0412\u043e\u0441\u0441\u0442\u0430\u043d\u043e\u0432\u0438\u0442\u044c \u043e\u0431\u0440\u0430\u0442\u043d\u043e");

			UIManager.put("InternalFrameTitlePane.closeButtonText", "\u0417\u0430\u043a\u0440\u044b\u0442\u044c");
			UIManager.put("InternalFrameTitlePane.maximizeButtonText", "\u0420\u0430\u0437\u0432\u0435\u0440\u043d\u0443\u0442\u044c");
			UIManager.put("InternalFrameTitlePane.minimizeButtonText", "\u0421\u0432\u0435\u0440\u043d\u0443\u0442\u044c");
			UIManager.put("InternalFrameTitlePane.moveButtonText", "\u041f\u0435\u0440\u0435\u043c\u0435\u0441\u0442\u0438\u0442\u044c");
			UIManager.put("InternalFrameTitlePane.restoreButtonText", "\u0412\u043e\u0441\u0441\u0442\u0430\u043d\u043e\u0432\u0438\u0442\u044c");
			UIManager.put("InternalFrameTitlePane.sizeButtonText", "\u0420\u0430\u0437\u043c\u0435\u0440");

			UIManager.put("ProgressMonitor.progressText", "\u041e\u0431\u0440\u0430\u0431\u043e\u0442\u043a\u0430...");

			UIManager.put("SplitPane.leftButtonText", "\u043b\u0435\u0432\u0430\u044f \u043a\u043d\u043e\u043f\u043a\u0430");
			UIManager.put("SplitPane.rightButtonText", "\u043f\u0440\u0430\u0432\u0430\u044f \u043a\u043d\u043e\u043f\u043a\u0430");

			UIManager.put("AbstractButton.clickText", "click");
			UIManager.put("ComboBox.togglePopupText", "togglePopup");
			UIManager.put("FileChooser.lookInLabelText", "\u0418\u0441\u043a\u0430\u0442\u044c \u0432:");
			UIManager.put("FileChooser.saveInLabelText", "\u0421\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c \u0432:");
			UIManager.put("FileChooser.fileNameLabelText", "\u0418\u043c\u044f \u0444\u0430\u0439\u043b\u0430:");
			UIManager.put("FileChooser.filesOfTypeLabelText", "\u0422\u0438\u043f \u0444\u0430\u0439\u043b\u043e\u0432:");
			UIManager.put("FileChooser.upFolderToolTipText", "\u041f\u0435\u0440\u0435\u0445\u043e\u0434 \u043d\u0430 \u043e\u0434\u0438\u043d \u0443\u0440\u043e\u0432\u0435\u043d\u044c \u0432\u0432\u0435\u0440\u0445");
			UIManager.put("FileChooser.upFolderAccessibleName", "\u0412\u0432\u0435\u0440\u0445");
			UIManager.put("FileChooser.homeFolderToolTipText", "\u041d\u0430\u0447\u0430\u043b\u044c\u043d\u0430\u044f \u043f\u0430\u043f\u043a\u0430");
			UIManager.put("FileChooser.homeFolderAccessibleName", "\u041d\u0430\u0447\u0430\u043b\u044c\u043d\u0430\u044f \u043f\u0430\u043f\u043a\u0430");
			UIManager.put("FileChooser.newFolderToolTipText", "\u0421\u043e\u0437\u0434\u0430\u043d\u0438\u0435 \u043d\u043e\u0432\u043e\u0439 \u043f\u0430\u043f\u043a\u0438");
			UIManager.put("FileChooser.newFolderAccessibleNam", "\u041d\u043e\u0432\u0430\u044f \u043f\u0430\u043f\u043a\u0430");
			UIManager.put("FileChooser.listViewButtonToolTipText", "\u0421\u043f\u0438\u0441\u043e\u043a");
			UIManager.put("FileChooser.listViewButtonAccessibleName", "\u0421\u043f\u0438\u0441\u043e\u043a");
			UIManager.put("FileChooser.detailsViewButtonToolTipText", "\u0422\u0430\u0431\u043b\u0438\u0446\u0430");
			UIManager.put("FileChooser.detailsViewButtonAccessibleName", "\u0422\u0430\u0431\u043b\u0438\u0446\u0430");
			UIManager.put("FileChooser.fileAttrHeaderText", "\u0410\u0442\u0440\u0438\u0431\u0443\u0442\u044b");
			UIManager.put("FileChooser.fileDateHeaderText", "\u0418\u0437\u043c\u0435\u043d\u0451\u043d");
			UIManager.put("FileChooser.fileNameHeaderText", "\u0418\u043c\u044f");
			UIManager.put("FileChooser.fileSizeHeaderText", "\u0420\u0430\u0437\u043c\u0435\u0440");
			UIManager.put("FileChooser.fileTypeHeaderText", "\u0422\u0438\u043f");
			UIManager.put("InternalFrameTitlePane.closeButtonAccessibleName", "\u0417\u0430\u043a\u0440\u044b\u0442\u044c");
			UIManager.put("InternalFrameTitlePane.iconifyButtonAccessibleName", "\u0421\u0432\u0435\u0440\u043d\u0443\u0442\u044c");
			UIManager.put("InternalFrameTitlePane.maximizeButtonAccessibleName", "\u0420\u0430\u0437\u043c\u0435\u0440\u043d\u0443\u0442\u044c");
			UIManager.put("MetalTitlePane.closeMnemonic", 67);
			UIManager.put("MetalTitlePane.closeTitle", "\u0417\u0430\u043a\u0440\u044b\u0442\u044c");
			UIManager.put("MetalTitlePane.iconifyMnemonic", 69);
			UIManager.put("MetalTitlePane.iconifyTitle", "\u0421\u0432\u0435\u0440\u043d\u0443\u0442\u044c");
			UIManager.put("MetalTitlePane.maximizeMnemonic", 88);
			UIManager.put("MetalTitlePane.maximizeTitle", "\u0420\u0430\u0437\u0432\u0435\u0440\u043d\u0443\u0442\u044c");
			UIManager.put("MetalTitlePane.restoreMnemonic", 82);
			UIManager.put("MetalTitlePane.restoreTitle", "\u0412\u043e\u0441\u0441\u0442\u0430\u043d\u043e\u0432\u0438\u0442\u044c");
			UIManager.put("FileChooser.acceptAllFileFilterText", "*");
			UIManager.put("FileChooser.cancelButtonText", "\u041e\u0442\u043c\u0435\u043d\u0430");
			UIManager.put("FileChooser.saveButtonText", "\u0421\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c");
			UIManager.put("FileChooser.openButtonText", "\u041e\u0442\u043a\u0440\u044b\u0442\u044c");
			UIManager.put("FileChooser.updateButtonText", "\u041e\u0431\u043d\u043e\u0432\u0438\u0442\u044c");
			UIManager.put("FileChooser.helpButtonText", "\u041f\u043e\u043c\u043e\u0449\u044c");
			UIManager.put("FileChooser.pathLabelText", "\u0412\u0432\u0435\u0434\u0438\u0442\u0435 \u043f\u0443\u0442\u044c \u0438\u043b\u0438 \u043d\u0430\u0437\u0432\u0430\u043d\u0438\u0435 \u043f\u0430\u043f\u043a\u0438:");
			UIManager.put("FileChooser.filterLabelText", "\u0422\u0438\u043f \u0444\u0430\u0439\u043b\u043e\u0432");
			UIManager.put("FileChooser.foldersLabelText", "\u041f\u0430\u043f\u043a\u0438");
			UIManager.put("FileChooser.filesLabelText", "\u0424\u0430\u0439\u043b\u044b");
			UIManager.put("FileChooser.enterFileNameLabelText", "\u0412\u0432\u0435\u0434\u0438\u0442\u0435 \u0438\u043c\u044f \u0444\u0430\u0439\u043b\u0430:");
			UIManager.put("FileChooser.cancelButtonToolTipText", "\u041e\u0442\u043c\u0435\u043d\u0438\u0442\u044c \u0432\u044b\u0431\u043e\u0440 \u0444\u0430\u0439\u043b\u0430.");
			UIManager.put("FileChooser.saveButtonToolTipText", "\u0421\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c \u0432\u044b\u0431\u0440\u0430\u043d\u043d\u044b\u0439 \u0444\u0430\u0439\u043b.");
			UIManager.put("FileChooser.openButtonToolTipText", "\u041e\u0442\u043a\u0440\u044b\u0442\u044c \u0432\u044b\u0431\u0440\u0430\u043d\u043d\u044b\u0439 \u0444\u0430\u0439\u043b.");
			UIManager.put("FileChooser.updateButtonToolTipText", "\u041e\u0431\u043d\u043e\u0432\u0438\u0442\u044c \u0441\u043e\u0434\u0435\u0440\u0436\u0438\u043c\u043e\u0435 \u043a\u0430\u0442\u0430\u043b\u043e\u0433\u0430.");
			UIManager.put("FileChooser.helpButtonToolTipText", "\u0412\u044b\u0437\u043e\u0432 \u0441\u043f\u0440\u0430\u0432\u043a\u0438.");
			UIManager.put("FileChooser.openDialogTitleText", "\u041e\u0442\u043a\u0440\u044b\u0442\u044c");
			UIManager.put("FileChooser.saveDialogTitleText", "\u0421\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c");

			UIManager.put("FileChooser.viewMenuLabelText", "Вид");
			UIManager.put("FileChooser.refreshActionLabelText", "Обновить");
			UIManager.put("FileChooser.newFolderActionLabelText", "Новая папка");
			UIManager.put("FileChooser.listViewActionLabelText", "Список");
			UIManager.put("FileChooser.detailsViewActionLabelText", "Таблица");
			UIManager.put("FileChooser.fileSizeKiloBytes", "КБ");
			UIManager.put("FileChooser.fileSizeMegaBytes", "МБ");
			UIManager.put("FileChooser.fileSizeGigaBytes", "ГБ");

			Locale.setDefault(new Locale("ru"));

		}


	/* Create the GUI and show it.  For thread safety,
		this method should be invoked from the
		event-dispatching thread.
	*/
	private static void createAndShowGUI() {
		//Create and set up the window.
		final MainWindow frame = new MainWindow();
		//frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		//Display the window.
		frame.pack();
		frame.setExtendedState(frame.getExtendedState()|JFrame.MAXIMIZED_BOTH);
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		/*and then your windowClosing method should look like this*/
		frame.addWindowListener(new WindowAdapter()
		{
				public void windowClosing(WindowEvent e)
				{

                    MainWindow main=(MainWindow)e.getSource();
                    main.saveSettings();
					if(!main.documentSaved)
					{
						Object[] options={LanguageTranslator.getString("yes"), LanguageTranslator.getString("no"), LanguageTranslator.getString("cancel")};
						int result = JOptionPane.showOptionDialog(main, LanguageTranslator.getString("save.opened.document"), LanguageTranslator.getString("confirm"),JOptionPane.YES_NO_CANCEL_OPTION,JOptionPane.QUESTION_MESSAGE,null,options,options[2]);
						switch(result)
						{
							case 0:	//сейвим документ
								if(main.performSaveDocument()!=0)
								{
									Object[] optionsConfirm={LanguageTranslator.getString("yes"), LanguageTranslator.getString("no")};
									if(1==JOptionPane.showOptionDialog(main, LanguageTranslator.getString("can.t.save.document.are.you.really.want.to.quit"), LanguageTranslator.getString("confirm"),JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE,null,optionsConfirm,optionsConfirm[1]))
										return; //решили, что выходить не стоит
								}
								break;
							case 1:	//не сейвим документ
								break;
							case 2:	//отмена
								return;	//решили, что выходить не стоит
							default:
						}
					}
			    	System.exit(0);
				}
		});
		frame.setVisible(true);
	}

	// main
	public static void main(String[] args)
	{
        initUserLanguage();
        if(setRuLang)
        {
            setRuLang();
            LanguageTranslator.setResourceBundle("ramBundle",new Locale("ru"));
        }
        else
        {
            LanguageTranslator.setResourceBundle("ramBundle",new Locale("en"));
            Locale.setDefault(new Locale("en"));
        }
		//Schedule a job for the event-dispatching thread:
		//creating and showing this application's GUI.
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI();
			}
		});
	}
}

