package ram_pack.ram_UndoDocument;

import javax.swing.text.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.UndoableEditEvent;

/**
 * Created by IntelliJ IDEA.
 * User: Dragon
 * Date: 04.10.2009
 * Time: 16:45:56
 * To change this template use File | Settings | File Templates.
 */
public class MyDocument extends DefaultStyledDocument
{
    public void splitTextCharacter(int offset, int length)
    {
		try
        {
            writeLock();
            for (int i = offset; i < (offset + length); i++)
            {

                DefaultDocumentEvent changes =
                        new DefaultDocumentEvent(offset, length, DocumentEvent.EventType.CHANGE)
                        {
                            public boolean isSignificant()
                            {
                                return false;
                            }
                        };
                // split elements that need it
                buffer.change(i, 1, changes);
                changes.end();
                fireChangedUpdate(changes);
                fireUndoableEditUpdate(new UndoableEditEvent(this, changes)
                {
                    public boolean isSignificant()
                    {
                        return false;
                    }
                });
            }
        }
        catch(Exception ex)
        {
            //ignore
        }
        finally
        {
            writeUnlock();
        }
	}

    public void directHighlight(int offset, int length, AttributeSet s)
    {
        if (length == 0)
        {
            return;
        }
        try
        {
            writeLock();

            AttributeSet sCopy = s.copyAttributes();
            int lastEnd;
            for (int pos = offset; pos < (offset + length); pos = lastEnd)
            {
                DefaultDocumentEvent changes =
                                   new DefaultDocumentEvent(offset, length, DocumentEvent.EventType.CHANGE);
                
                Element run = getCharacterElement(pos);
                lastEnd = run.getEndOffset();
                if (pos == lastEnd)
                {
                    // offset + length beyond length of document, bail.
                    break;
                }
                MutableAttributeSet attr = (MutableAttributeSet) run.getAttributes();
                changes.addEdit(new AttributeUndoableEdit(run, sCopy, true));
                attr.removeAttributes(attr);
                attr.addAttributes(s);
                changes.end();
                fireChangedUpdate(changes);
            }

        }
        finally
        {
            writeUnlock();
        }

    }
     public void setCharacterAttributesA(int offset, int length, AttributeSet s, boolean replace) {
        if (length == 0) {
            return;
        }
	try {
	    //writeLock();
	    DefaultDocumentEvent changes =
		new DefaultDocumentEvent(offset, length, DocumentEvent.EventType.CHANGE);

	    // split elements that need it
	    buffer.change(offset, length, changes);

	    AttributeSet sCopy = s.copyAttributes();

	    // PENDING(prinz) - this isn't a very efficient way to iterate
	    int lastEnd = Integer.MAX_VALUE;
	    for (int pos = offset; pos < (offset + length); pos = lastEnd) {
		Element run = getCharacterElement(pos);
		lastEnd = run.getEndOffset();
                if (pos == lastEnd) {
                    // offset + length beyond length of document, bail.
                    break;
                }
		MutableAttributeSet attr = (MutableAttributeSet) run.getAttributes();
		changes.addEdit(new AttributeUndoableEdit(run, sCopy, replace));
		if (replace) {
		    attr.removeAttributes(attr);
		}
		attr.addAttributes(s);
	    }
	    changes.end();
	    fireChangedUpdate(changes);
	    fireUndoableEditUpdate(new UndoableEditEvent(this, changes));
	} finally {
	   //writeUnlock();
	}

    }
}
