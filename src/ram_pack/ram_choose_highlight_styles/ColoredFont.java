package ram_pack.ram_choose_highlight_styles;

import java.awt.*;

public class ColoredFont extends Font
{
	public static final int DEFAULT_SIZE=12;
	public static final Color DEFAULT_COLOR=Color.BLACK;

	private Color color=DEFAULT_COLOR;

	public ColoredFont(String name)
	{
		super(name,Font.PLAIN,DEFAULT_SIZE);
	}

	public ColoredFont(String name, int style)
	{
		super(name,style,DEFAULT_SIZE);
	}

	public ColoredFont(String name, int style, Color color)
	{
		super(name,style,DEFAULT_SIZE);
		this.color = color;
	}

	public ColoredFont(ColoredFont font)
	{
		super(font);
		color=font.color;
	}

	public Color getColor()
	{
		return color;
	}

	public void setColor(Color color)
	{
		this.color=color;
	}

	public void setStyle(int style)
	{
		this.style=style;
	}

	public void setStyle(boolean bold,boolean italic)
	{
		style=(getIntMode(bold)*BOLD)|(getIntMode(italic)*ITALIC);
	}

	public void setBold(boolean mode)
	{
		setStyle(mode,isItalic());
	}

	public void setItalic(boolean mode)
	{
		setStyle(isBold(),mode);
	}

	private int getIntMode(boolean mode)
	{
		return mode?1:0;
	}
}
