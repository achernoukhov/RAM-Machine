package ram_pack.ram_core;

/**
 * Created by IntelliJ IDEA.
 * User: ����
 * Date: 23.03.2008
 * Time: 20:46:31
 * To change this template use File | Settings | File Templates.
 */

public class Argument // �������� ��������
{
	public Integer num; // �������� �������� (����� ���� "=" � "*")
	//public boolean consistEqual; // ���� �� "="
	public int referenceDegree; // ������� ��������� ������: <0 - "=", 0 - ��� �����, >0 - referenceDegree �����
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
