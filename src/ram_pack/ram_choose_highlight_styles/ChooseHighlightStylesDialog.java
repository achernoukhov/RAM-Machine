package ram_pack.ram_choose_highlight_styles;

import ram_pack.ram_check_and_convert.CodeSubstringType;
import ram_pack.ram_core.RAM;
import ram_pack.LanguageTranslator;

import javax.swing.*;
import java.awt.*;
import java.util.TreeMap;

public class ChooseHighlightStylesDialog extends JDialog
{
	private ChooseHighlightStylesPanel changeHighlightStylesPanel;

	public ChooseHighlightStylesDialog(Frame owner,TreeMap<CodeSubstringType,ColoredFont> initialHighlightStyles,Font font, RAM ram)
	{
		super(owner,true);
		this.setTitle(LanguageTranslator.getString("highlight.settings"));
		changeHighlightStylesPanel =new ChooseHighlightStylesPanel(this,initialHighlightStyles,font,ram);
		setContentPane(changeHighlightStylesPanel);
	}

	public void showDialog()
	{
		this.pack();
		this.setResizable(false);
		this.setLocationByPlatform(true);
		this.setVisible(true);
	}

	public TreeMap<CodeSubstringType,ColoredFont> getHighlightStyles()
	{
		return changeHighlightStylesPanel.getHighlightStyles();
	}
}
