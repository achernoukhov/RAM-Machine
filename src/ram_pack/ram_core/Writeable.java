package ram_pack.ram_core;

public interface Writeable
{
	void writeNext(Integer n);
	void writeNext(char c);
	int size();
}
