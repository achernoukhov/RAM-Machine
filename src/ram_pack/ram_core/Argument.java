package ram_pack.ram_core;

/**
 * Created by IntelliJ IDEA.
 * User: Саня
 * Date: 23.03.2008
 * Time: 20:46:31
 * To change this template use File | Settings | File Templates.
 */

public class Argument // аргумент комманды
{
	public Integer num; // числовой аргумент (после всех "=" и "*")
	//public boolean consistEqual; // есть ли "="
	public int referenceDegree; // порядок косвенной ссылки: <0 - "=", 0 - нет звезд, >0 - referenceDegree звезд
	public Argument(){}
	public Argument(Integer n, int degree)
	{
		num=n;
		referenceDegree=degree;
	}
	public Argument(int n, int degree)
	{
		num=n;
		referenceDegree=degree;
	}
}
