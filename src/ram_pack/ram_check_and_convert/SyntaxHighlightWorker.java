package ram_pack.ram_check_and_convert;

import ram_pack.ram_choose_highlight_styles.ColoredFont;
import ram_pack.ram_core.RAM;
import ram_pack.ram_UndoDocument.MyDocument;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import ram_pack.ram_UndoDocument.MyDocument;

public class SyntaxHighlightWorker extends SwingWorker<ArrayList<PreCompiledLine>,Object>
{
	private MyDocument doc;
	private HashMap<Object,SimpleAttributeSet> attributeSets;
	private RAM ram=null;

	//конструктор

	public SyntaxHighlightWorker(RAM initRAM, MyDocument document, TreeMap<CodeSubstringType, ColoredFont> highlightStyles)
	{
		ram=initRAM;
		doc=document;
		setHighlights(highlightStyles);
	}

	private void setHighlights(TreeMap<CodeSubstringType,ColoredFont> highlightStyles)
	{
		attributeSets=new HashMap<Object,SimpleAttributeSet>();
		for(CodeSubstringType type: CodeSubstringType.values())
		{
			ColoredFont coloredFont=highlightStyles.get(type);
			if (coloredFont==null)
				continue;
			SimpleAttributeSet set=new SimpleAttributeSet();
			StyleConstants.setForeground(set,coloredFont.getColor());
			StyleConstants.setBold(set,coloredFont.isBold());
			StyleConstants.setItalic(set,coloredFont.isItalic());
			attributeSets.put(type,set);
		}
	}

	//генерит подсветки и применяет их к тексту
	public ArrayList<PreCompiledLine> doInBackground()
	{
		if(attributeSets==null)
			return null;
		if(ram==null)
			return null;
		String codeString="";
		try
		{
			codeString=doc.getText(0,doc.getLength());
		}
		catch(BadLocationException e)
		{
			e.printStackTrace();
		}

        ArrayList<PreCompiledLine> preCompiledLineArrayList = new SyntaxChecker(ram).generateHighlightList(codeString);
        return preCompiledLineArrayList;
	}
	
	public void done()
	{
 		try
		{
			ArrayList<PreCompiledLine> syntaxPreCompiledLineList=get(); 
			for(PreCompiledLine line:syntaxPreCompiledLineList)
			{
				for(PreCompiledString highlight:line.getStringData())
				{
					if(highlight.type == CodeSubstringType.SPACES_AND_TABULATIONS)
						continue;
					if(attributeSets.containsKey(highlight.type))
					{
						//doc.setCharacterAttributes(highlight.startIndex+line.startOfLineOffset,highlight.endIndex-highlight.startIndex,attributeSets.get(highlight.type),true);
                        doc.directHighlight(highlight.startIndex+line.startOfLineOffset,highlight.endIndex-highlight.startIndex,attributeSets.get(highlight.type));
					}
				}
		}
		}
		catch (InterruptedException ignore)
		{
			// ignoring
		}
		catch (ExecutionException ignore)
		{
			// ignoring
		}
		catch (CancellationException ignore)
		{
			// ignoring
		}

    }
}
