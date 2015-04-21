import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;
import javax.swing.undo.CannotRedoException;

/**
 * Created by IntelliJ IDEA.
 * User: Саня
 * Date: 10.11.2008
 * Time: 20:06:22
 * To change this template use File | Settings | File Templates.
 */
public class MyUndoManager extends UndoManager
{
	public MyUndoManager()
	{
		super();	//To change body of overridden methods use File | Settings | File Templates.
		setLimit(20000);
	}

	/**
     * Redoes the appropriate edits.  If <code>end</code> has been
     * invoked this calls through to the superclass.  Otherwise
     * this invokes <code>redo</code> on all edits between the
     * index of the next edit and the next significant edit, updating
     * the index of the next edit appropriately.
     *
     * @throws javax.swing.undo.CannotRedoException if one of the edits throws
     *         <code>CannotRedoException</code> or there are no edits
     *         to be redone
     * @see javax.swing.undo.CompoundEdit#end
     * @see #canRedo
     * @see #editToBeRedone
     */
	@Override
    public synchronized void redo() throws CannotRedoException
	{
    	if(canRedo())
			super.redo();
		if(canRedo())
		{
			super.redo();
			undo();
		}

    }
}
