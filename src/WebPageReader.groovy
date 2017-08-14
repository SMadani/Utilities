import java.util.regex.Pattern

class WebPageReader
{
	private String raw
    private final URL site
	private static final def n = System.getProperty ("line.separator")
	public static Pattern WEBSITE_REGEX = ~/(https?:\/\/)?([\da-z\.-]+)\.([a-z\.]{2,6})([\/\w \.-]*)*\/?/
    public static Pattern EMAIL_REGEX = ~/[_A-Za-z0-9-]+(\.[_A-Za-z0-9-]+)*@[A-Za-z0-9\-_]+\.([A-Za-z]{2,3})(\.[A-Za-z]{2})?/
    public static Pattern NAME_REGEX = ~/([\u00c0-\u01ffa-zA-Z'\-])+/

    WebPageReader (dir) throws MalformedURLException
	{
        raw = "Page not read yet"
        if (dir instanceof String)
            site = dir.startsWith("http://") || dir.startsWith ("https://") ? new URL (dir) : new URL ("http://$dir")
        else if (dir instanceof URL)
            site = dir
        else
            site = new URL (dir.toString())
	}
	
	final def read() throws IOException
	{
        def bfr = new BufferedReader (new InputStreamReader(site.openStream()))
        def inputLine, buff = new StringBuilder()
        while ((inputLine = bfr.readLine()) != null)
            buff.append(inputLine).append(n)
        bfr.close()
        raw = buff.toString()
	}

    static boolean isWorkingURL (String url)
    {
        try {
            new URL (url).openConnection().connect()
            true
        }
        catch (Exception ignored) {
            false
        }
    }

    static boolean isValidURL (String urs)
    {
        urs ==~ WEBSITE_REGEX//~/(https?:\/\/)([\da-z\.-]+)\.([a-z\.]{2,6})([\/\w \.-]*)*\/?/
    }

    long getOptimalDelay (long step, int attempts)
    {
        def delay = 0, count = 0
        while (count < attempts)
        {
            try
            {
                read()
                count++
                sleep (delay)
            }
            catch (IOException ignored)
            {
                count = 0
                delay += step
            }
        }
        delay as long
    }

    TextConfig getEditor()
    {
        def editor = new TextConfig()
        editor.overwrite(raw)
        return editor
    }

    String getURL()
    {
        return site.toString();
    }

    private def crawlSites (int layers) //TODO: test multi-layer logic
    {
        def candidates, urls = []
        candidates = getEditor().find(websiteValidator())
        for (candidate in candidates)
            if (isWorkingURL(candidate))
                urls.add(candidate)
        if (layers < 2)
            return urls.toSet()
        else
        {
            def masterlist = []
            for (layer in layers-1)
            {
                def pagelist = []
                for (url in urls)
                {
                    def wpr = new WebPageReader(url)
                    wpr.read()
                    candidates = wpr.getEditor().find(websiteValidator())
                    for (candidate in candidates)
                        if (isWorkingURL(candidate))
                            pagelist.add(candidate)
                    masterlist.add(pagelist)
                }
                urls = pagelist
            }
            return masterlist.toSet()
        }
    }

    def getLinks()
    {
        crawlSites(0);
    }
}
