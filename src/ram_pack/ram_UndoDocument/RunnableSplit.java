package ram_pack.ram_UndoDocument;

/**
 * Created by IntelliJ IDEA.
 * User: Dragon
 * Date: 05.10.2009
 * Time: 10:07:11
 * To change this template use File | Settings | File Templates.
 */
public class RunnableSplit implements Runnable
{
    int offset;
    int length;
    MyDocument doc;
    public RunnableSplit(int offset,int length,MyDocument doc)
    {
        this.offset=offset;
        this.length=length;
        this.doc=doc;
    }

    public void run()
    {
        doc.splitTextCharacter(offset, length);
    }
}

