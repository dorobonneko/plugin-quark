package moe;
import java.util.Map;

public interface HookCallback{
	void download(String url,String name,Map<String,String> header);
}
