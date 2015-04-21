package ram_pack.ram_choose_highlight_styles;

import ram_pack.ram_check_and_convert.SyntaxHighlightWorker;
import ram_pack.ram_check_and_convert.CodeSubstringType;
import ram_pack.ram_core.RAM;
import ram_pack.ram_UndoDocument.MyDocument;
import ram_pack.LanguageTranslator;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.awt.event.*;
import java.util.TreeMap;
import java.util.Scanner;
import java.io.*;
import java.net.URL;

public class ChooseHighlightStylesPanel extends JPanel
{
	private String SAMPLE_CODE_FILE_NAME = LanguageTranslator.getString("sample_code_address");
	private JColorChooser colorChooser;
	private TreeMap<String, ColoredFont> stylesMap =new TreeMap<String,ColoredFont>();
	private TreeMap<CodeSubstringType, ColoredFont> highlightStyles;
	private TreeMap<String, ColoredFont> oldStylesMap;
	private JComboBox highlightComboBox;

	private JPanel chooserPanel;
	private JCheckBox cbBold=new JCheckBox(LanguageTranslator.getString("bold"));
	private JCheckBox cbItalic=new JCheckBox(LanguageTranslator.getString("italic"));
	private ColorChangeListener colorChangeListener=new ColorChangeListener();
	private JDialog parentDialog ;
	private RAM ram;
    private MyDocument doc;

	protected class ColorChangeListener implements ChangeListener
	{
		ColorChangeListener()
		{
			super();
		}

		public void stateChanged(ChangeEvent e)
		{
			changeColor();
		}
	}

	protected class StyleChangeListener implements ItemListener
	{
		public StyleChangeListener()
		{
			super();
		}

		public void itemStateChanged(ItemEvent e)
		{
			JCheckBox cb=(JCheckBox)e.getItemSelectable();
			if (cb==cbBold)
			{
				changeBold();
			}
			if (cb==cbItalic)
			{
				changeItalic();
			}
		}
	}

	protected class ComboChangeListener implements ActionListener
	{
		ComboChangeListener()
		{
			super();
		}

		public void actionPerformed(ActionEvent e)
		{
			resetChoosers();
		}
	}
	
	public ChooseHighlightStylesPanel(JDialog parent,TreeMap<CodeSubstringType,ColoredFont> initialHighlightStyles,Font font,RAM ram)
	{
		super(new BorderLayout());
		this.ram = ram;
		parentDialog=parent;
		parentDialog.addWindowListener(new WindowListener()
		{
			public void windowOpened(WindowEvent e)
			{
				// nothing to do
			}

			public void windowIconified(WindowEvent e)
			{
				// nothing to do
			}

			public void windowDeiconified(WindowEvent e)
			{
				// nothing to do
			}

			public void windowActivated(WindowEvent e)
			{
				// nothing to do
			}

			public void windowDeactivated(WindowEvent e)
			{
				// nothing to do
			}

			public void windowClosing(WindowEvent e)
			{
				performCancel();
			}

			public void windowClosed(WindowEvent e)
			{
				// nothing to do
			}
		});
		highlightStyles =initialHighlightStyles;
		oldStylesMap = convertToStylesMap(initialHighlightStyles);
		setStylesMap(oldStylesMap);
		JPanel bigPanel=new JPanel(new BorderLayout());
		setLayout(new BoxLayout(this,BoxLayout.PAGE_AXIS));
		JPanel samplePanel=new JPanel();
		samplePanel.setLayout(new BorderLayout());
		highlightComboBox =new JComboBox(stylesMap.keySet().toArray());
		highlightComboBox.addActionListener(new ComboChangeListener());
		samplePanel.add(highlightComboBox,BorderLayout.PAGE_START);
        doc= new MyDocument();
		String sampleCode = readSampleCode(SAMPLE_CODE_FILE_NAME);
		JTextPane textPane=new JTextPane(doc);
		textPane.setFont(font);
		textPane.setText(sampleCode);
		textPane.setEditable(false);
        doc.splitTextCharacter(0,sampleCode.length()); // for highlights
		JPanel textPanel=new JPanel(new BorderLayout());
		textPanel.add(textPane,BorderLayout.CENTER);
		JScrollPane textScrollPane=new JScrollPane(textPanel);
		samplePanel.add(textScrollPane,BorderLayout.CENTER);
		samplePanel.setPreferredSize(new Dimension(500,500));
		bigPanel.add(samplePanel, BorderLayout.CENTER);
		chooserPanel=new JPanel();
		chooserPanel.setLayout(new BoxLayout(chooserPanel,BoxLayout.PAGE_AXIS));
		JPanel checkPanel=new JPanel();
		checkPanel.setLayout(new BoxLayout(checkPanel,BoxLayout.PAGE_AXIS));
		cbBold.addItemListener(new StyleChangeListener());
		cbItalic.addItemListener(new StyleChangeListener());
		checkPanel.add(cbBold);
		checkPanel.add(cbItalic);
		chooserPanel.add(checkPanel);
		bigPanel.add(chooserPanel,BorderLayout.EAST);
		add(bigPanel,BorderLayout.CENTER);
		JPanel buttonPanel=new JPanel(new FlowLayout());
		JButton btOK=new JButton(LanguageTranslator.getString("ok"));
		btOK.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				performOK();
			}
		});
		JButton btCancel=new JButton(LanguageTranslator.getString("cancel"));
		btCancel.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				performCancel();
			}
		});
		JButton btReset=new JButton(LanguageTranslator.getString("reset"));
		btReset.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				performReset();
			}
		});
		buttonPanel.add(btOK);
		buttonPanel.add(btCancel);
		buttonPanel.add(btReset);
		add(buttonPanel, BorderLayout.PAGE_END);
		newColorChooser();
		runHighlights();
		resetChoosers();
	}

	// the action on "Bold checkbox state changed" event
	private void changeBold()
	{
		String key=(String) highlightComboBox.getSelectedItem();
		ColoredFont coloredFont= stylesMap.get(key);
		coloredFont.setBold(cbBold.isSelected());
		//colorChooser.getPreviewPanel().setFont(coloredFont);
		runHighlights();
	}

	// the action on "Italic checkbox state changed" event
	private void changeItalic()
	{
		String key=(String) highlightComboBox.getSelectedItem();
		ColoredFont coloredFont= stylesMap.get(key);
		coloredFont.setItalic(cbItalic.isSelected());
		//colorChooser.getPreviewPanel().setFont(coloredFont);
		runHighlights();
	}

	// the action on "Color changed" event
	private void changeColor()
	{
		String key=(String) highlightComboBox.getSelectedItem();
		ColoredFont coloredFont= stylesMap.get(key);
		coloredFont.setColor(colorChooser.getColor());
		runHighlights();
	}

	// creates new color chooser
	private void newColorChooser()
	{
		String item=(String) highlightComboBox.getSelectedItem();
		ColoredFont coloredFont= stylesMap.get(item);
		colorChooser = new JColorChooser(coloredFont.getColor());
		colorChooser.setBorder(BorderFactory.createTitledBorder(LanguageTranslator.getString("choose.color.for") +item+"]"));
		colorChooser.getSelectionModel().addChangeListener(colorChangeListener);
		colorChooser.getSelectionModel();
		//colorChooser.getPreviewPanel().setFont(coloredFont);
		chooserPanel.add(colorChooser,0);
		this.validate();
	}

	// refreshes the state of colorChooser
	private void resetColorChooser()
	{
		chooserPanel.remove(colorChooser);
		newColorChooser();
	}

	// refreshes the state of styleChooser
	private void resetStyleChooser()
	{
		String item=(String) highlightComboBox.getSelectedItem();
		cbBold.setSelected(stylesMap.get(item).isBold());
		cbItalic.setSelected(stylesMap.get(item).isItalic());
		this.validate();
	}

	// refreshes the state of choosers
	private void resetChoosers()
	{
		resetStyleChooser();
		resetColorChooser();
	}

	// the action on "OK button pressed" event
	private void performOK()
	{
		parentDialog.dispose();
	}

	// the action on "Cancel button pressed" event
	private void performCancel()
	{
		stylesMap =null;
		parentDialog.dispose();
	}

	// the action on "Reset button pressed" event
	private void performReset()
	{
		setStylesMap(oldStylesMap);
		resetChoosers();
		runHighlights();
	}

	// runs highlighting of the sample code with the styles currently chosen by user
	private void runHighlights()
	{
		SyntaxHighlightWorker syntaxHighlightWorker;
		syntaxHighlightWorker=new SyntaxHighlightWorker(ram,doc, convertToHighlightStyles(stylesMap));
		syntaxHighlightWorker.execute();
	}

	// public getter for chosen by user styles
	public TreeMap<CodeSubstringType,ColoredFont> getHighlightStyles()
	{
		return convertToHighlightStyles(stylesMap);
	}

	private void setStylesMap(TreeMap<String, ColoredFont> initialAttributeMap)
	{
		stylesMap =new TreeMap<String,ColoredFont>();
		for (String key: initialAttributeMap.keySet())
		{
			stylesMap.put(key,new ColoredFont(initialAttributeMap.get(key))); // копируем, но создавая новые ссылки
		}
	}

	// converts initialAttributeTypes into format used only in this panel
	private TreeMap<String,ColoredFont> convertToStylesMap(TreeMap<CodeSubstringType,ColoredFont> initialAttributeTypes)
	{
		TreeMap<String,ColoredFont> attributeMap=new TreeMap<String,ColoredFont>();
		for (CodeSubstringType type: initialAttributeTypes.keySet())
		{
			ColoredFont font=initialAttributeTypes.get(type);
			if (font!=null)
				attributeMap.put(font.getName(),initialAttributeTypes.get(type));
		}
		return attributeMap;
	}

	// converts attributeMap into format, which is used for highlighting
	private TreeMap<CodeSubstringType,ColoredFont> convertToHighlightStyles(TreeMap<String,ColoredFont> attributeMap)
	{
		if (attributeMap==null)
			return null;
		TreeMap<CodeSubstringType,ColoredFont> tempHighlightStyles=new TreeMap<CodeSubstringType,ColoredFont>(highlightStyles);
		for (CodeSubstringType type: highlightStyles.keySet())
		{
			ColoredFont font=tempHighlightStyles.get(type);
			if (font!=null)
			{
				tempHighlightStyles.remove(type);
				tempHighlightStyles.put(type,attributeMap.get(font.getName()));
			}
		}
		return tempHighlightStyles;
	}

	// reads sample code to display on the left of this panel from file
	private String readSampleCode(String fileName)
	{
		String absoluteFileName="";
		String sampleCode="";
		try
		{
			URL url=ChooseHighlightStylesPanel.class.getResource(fileName);
			absoluteFileName=url.getFile();
			Scanner inputFileScanner;
			inputFileScanner=new Scanner(url.openStream());
			while(inputFileScanner.hasNextLine())
				sampleCode+=inputFileScanner.nextLine()+"\n";
			inputFileScanner.close();
		}
		catch (IOException e)
		{
			sampleCode= LanguageTranslator.getString("error.can.t.open.sample.code.file") +absoluteFileName+">";
		}
		return sampleCode;
	}
}
