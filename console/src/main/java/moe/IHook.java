package moe;
import android.webkit.WebResourceResponse;
import android.webkit.WebResourceRequest;
import java.io.IOException;

public interface IHook{
	void onCreate(HookCallback callback);
	WebResourceResponse hook(WebResourceRequest request) throws IOException;
	
}
