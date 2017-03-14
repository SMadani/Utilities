import javax.swing.JList
import javax.swing.JProgressBar
import java.text.DecimalFormat
import javax.swing.JFrame
import static javax.swing.JOptionPane.*
import static javax.swing.JOptionPane.showOptionDialog

class AssetMonitor implements ScheduledTask
{
	TextConfig record
	private def inspect, window, items, progress, delay, urlConverter
    private volatile boolean updating
	def TargetDelay, LowTarget, LowThreshold, HighTarget, HighThreshold, ErrorMessage, ItemList, VisibleAfterTrigger

    private final void constructor (final sources, sav, delay, appWindow, JList<String>list, boolean setVisible, JProgressBar progressBar, SiteSearchSyntax sq)
    {
        updating = false
        progress = progressBar
        VisibleAfterTrigger = setVisible
        urlConverter = sq
        ItemList = list
        this.delay = delay >= 0 ? delay : delay*-1
        TargetDelay = this.delay
        LowThreshold = -1.00; HighThreshold = -1.00
        LowTarget = []; HighTarget = []; inspect = []; items = []
        ErrorMessage = "The web page has changed. Please Remove this item and restart the app."
        record =  sav instanceof TextConfig ? sav : new TextConfig(sav)
        setItems (sources)

        if (appWindow == null)
        {
            window = new JFrame("Notification")
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
            window.setLocationRelativeTo(null)
        }
        else
            window = appWindow
    }

    AssetMonitor (final sources, sav, delay, appWindow, JList<String> list, boolean setVisible, JProgressBar progressBar, SiteSearchSyntax sq)
    {
        constructor (sources, sav, delay, appWindow, list, setVisible, progressBar, sq)
    }

    AssetMonitor (final sources, sav, delay, appWindow, JList<String> list, boolean setVisible, JProgressBar progressBar)
    {
        constructor (sources, sav, delay, appWindow, list, setVisible, progressBar, null)
    }

	AssetMonitor (final sources, sav, delay, appWindow, JList<String> list, boolean setVisible)
	{
        constructor (sources, sav, delay, appWindow, list, setVisible, null, null)
    }

    AssetMonitor (final sources, sav, delay, JList<String> list, boolean setVisible)
    {
        constructor (sources, sav, delay, null, list, setVisible, null, null)
    }

    AssetMonitor (final sources, sav, delay, JList<String> list)
    {
        constructor (sources, sav, delay, null, list, true, null, null)
    }

    AssetMonitor (final sources, sav, delay)
    {
        constructor (sources, sav, delay, null, null, true, null, null)
    }

    AssetMonitor (final sources, sav)
    {
        constructor (sources, sav, 0, null, null, true, null, null)
    }

    AssetMonitor (final sources)
    {
        constructor (sources, null, 0, null, null, true, null, null)
    }

    Tag[] getItems()
    {
        items.toArray (new Tag[items.size()])
    }

    synchronized def addItem (Tag item)
    {
        while (true)
            if (!updating)
            {
                items.add(item)
                LowTarget.add(-1.00)
                HighTarget.add(-1.00)
                def url = WebPageReader.isValidURL(item.getName()) ? item.getName() : urlConverter != null ? urlConverter.query(item.getName()) : item.getName()
                inspect.add(new WebPageReader(url))
                record.writeValue("${items.size()+1}", item.toString("ID,Description,Properties"))
                break
            }
    }

    synchronized def addItem (Tag item, lowTarg, highTarg)
    {
        while (true)
            if (!updating)
            {
                items.add(item)
                LowTarget.add(-1.00)
                HighTarget.add(-1.00)
                def url = WebPageReader.isValidURL(item.getName()) ? item.getName() : urlConverter != null ? urlConverter.query(item.getName()) : item.getName()
                inspect.add(new WebPageReader(url))
                record.writeValue("${items.size()+1}", item.toString("ID,Description,Properties"))
                setLowTarget(LowTarget.size(), lowTarg)
                setHighTarget(HighTarget.size(), highTarg)
                break
            }
    }

    synchronized def removeItem (Tag item)
    {
        while (true)
            if (!updating)
            {
                def index = items.indexOf(item)
                inspect.remove(index)
                LowTarget.remove(index)
                HighTarget.remove(index)
                items.remove(item)
                record = new TextConfig(record)
                break
            }
    }

    synchronized def setItems (assets)
    {
        while (true)
            if (!updating)
            {
                record = new TextConfig(record)
                items = []; LowTarget = []; HighTarget = []

                if (assets instanceof Tag[])
                    Collections.addAll(items, assets)
                else if (assets instanceof Collection)
                    items = new ArrayList(assets)
                else
                    items = assets

                assert items.get(0) instanceof Tag
                for (it in (0..<items.size()))
                {
                    def item = items.get(it)
                    def value = item.getValue() != null ? item.getValue() : 1
                    LowTarget.add(LowThreshold*value)
                    HighTarget.add(HighThreshold*value)
                    record.writeValue("Item ${it+1}", (String) item.toString("ID,Description,Properties"))
                    def url = WebPageReader.isValidURL(item.getName()) ? item.getName() : urlConverter != null ? urlConverter.query(item.getName()) : item.getName()
                    if (it >= inspect.size())
                        inspect.add(new WebPageReader(url))
                    else
                        inspect.set(it, new WebPageReader(url))
                }
                break
            }
    }

    def setHighTarget (int index, double target)
    {
        while (true)
            if (!updating && index <= HighTarget.size() && index >= 0)
            {
                HighTarget.set(index, target)
                break
            }
    }

    double getHighTarget (int index)
    {
        while (true)
            if (!updating && index < HighTarget.size() && index >= 0)
                return HighTarget.get(index) as double
        else
            return -1.00
    }

    Double[] getHighTarget()
    {
        while (true)
            if (!updating)
            {
                Double[] arr = new Double[HighTarget.size()]
                for (int i in (0..<arr.length))
                    arr[i] = getHighTarget(i)
                return arr
            }
    }

    def setLowTarget (int index, double target)
    {
        while (true)
            if (!updating && index <= LowTarget.size() && index >= 0)
            {
                LowTarget.set(index, target)
                break
            }
    }

    double getLowTarget (int index)
    {
        while (true)
            if (!updating && index < LowTarget.size() && index >= 0)
                return LowTarget.get(index) as double
        else
            return -1.00
    }

    Double[] getLowTarget()
    {
        while (true)
            if (!updating)
            {
                Double[] arr = new Double[LowTarget.size()]
                for (int i in (0..<arr.length))
                    arr[i] = getLowTarget(i)
                return arr
            }
    }

    @Override
	synchronized void Trigger (String message)
    {
        updating = true
        record?.update()
        for (int x in 0..<items.size())
        {
            progress?.setString ("Updating items: ${x+1} of ${items.size()}...")
            def price, value, item = items.get(x), page = inspect.get(x)
            try {
                page?.read()
                if (delay > TargetDelay)
                    delay -= 100
            }
            catch (IOException rcode)
            {
                progress?.setString (rcode.getMessage())
                delay += 1000
                sleep (delay*2)
                try
                {
                    page?.read()
                    progress.setString ("Resuming update of item ${x+1}...")
                }
                catch (IOException ignored)
                {
                    progress.setString ("Could not update item ${x+1}. Will increase delay and attempt remaining ${items.size()-(x+1)}")
                    delay += 500
                    continue
                }
            }
            def from = item?.getDescription()?.substring(0, 3)
            def to = item?.getDescription()?.substring(4, 7)
            String params = item?.getProperties()
            String keyword = params.substring(1, params.indexOf("\",", 1))
            def sofar = keyword.length()+3
            def occurrence = Integer.parseInt(params.substring(sofar, params.indexOf(',', sofar)))
            sofar += "$occurrence".length()+1
            def endIndex = Integer.parseInt(params.substring(sofar))
            try {
                price = page?.getEditor().find(keyword, occurrence, endIndex)
            }
            catch (StringIndexOutOfBoundsException | NullPointerException nf) {
                onError(nf)
            }
            if (price == null)
                onError(new NullPointerException())
            else
            {
                value = getPrice(price, from, to)
                item.setValue(value)
                item.setTimestamp()
                if (x < LowTarget.size() && LowTarget.get(x) < 0 && LowThreshold > 0)
                    LowTarget.set(x, LowThreshold*value)
                else if (LowThreshold > 0 && x >= LowTarget.size())
                    LowTarget.add(LowThreshold*value)
                if (x < HighTarget.size() && HighTarget.get(x) < 0 && HighThreshold > 0)
                    HighTarget.set(x, HighThreshold*value)
                else if (HighThreshold > 0 && x >= HighTarget.size())
                    HighTarget.add(HighThreshold*value)

                if (LowTarget.size() > x && LowTarget.get(x) >= 0 && value <= LowTarget.get(x) && LowThreshold >= 0)
                {
                    window.setVisible(true)
                    def retval = showOptionDialog(window, "Would you like to update the lower bound?", "${urlConverter.getItemName(item.getName())} is now ${item.getValue()}!", YES_NO_OPTION, INFORMATION_MESSAGE, null, null, null)
                    if (retval == YES_OPTION)
                        LowTarget.set(x, value*LowThreshold)
                    else
                        LowTarget.set(x, value)
                }
                else if (HighTarget.size() > x && HighTarget.get(x) >= 0 && value >= HighTarget.get(x) && HighThreshold > 0)
                {
                    window.setVisible(true)
                    def retval = showOptionDialog(window, "Would you like to update the upper bound?", "${urlConverter.getItemName(item.getName())} is now ${item.getValue()}!", YES_NO_OPTION, INFORMATION_MESSAGE, null, null, null)
                    if (retval == YES_OPTION)
                        HighTarget.set(x, value*HighThreshold)
                    else
                        HighTarget.set(x, value)
                }
                String formatted = item.toString("ID,Description,Properties")
                if (urlConverter != null)
                {
                    def name, iurl = formatted.substring(formatted.indexOf("Name: \"")+("Name: \"").length(), formatted.indexOf("\","))
                    name = WebPageReader.isValidURL(iurl) ? urlConverter.getItemName (new URL(iurl)) : iurl
                    if (!name.equals (iurl))
                        formatted = formatted.replace (iurl, name)
                }
                record?.writeValue("Item ${x+1}", formatted)
                if (x < items.size()-1)
                    sleep(delay)
            }
        }
        record?.save()
        def assets = []
        for (i in 1..items.size())
            assets.add(record.readValue("Item "+i))
        ItemList?.setListData(assets.toArray())
        window.setVisible(VisibleAfterTrigger)
        updating = false
    }

    private static def getPrice (pstring, from, to)
    {
        def price, pval = pstring.find (~/\d+\.\d{1,2}/)
        if (!(from == null || to == null))
        {
            from = from.toUpperCase()
            to = to.toUpperCase()
            def currency = new WebPageReader("https://www.google.co.uk/finance/converter?a=$pval&from=$from&to=$to")
            currency.read()
            price = currency.getEditor().find("<span class=bld>", 1, 6)
        }
        else
            price = pval
        DecimalFormat td = new DecimalFormat("#.##")
        price = Double.parseDouble(td.format(price as Double))
        return price
    }

    @Override
    void onError(Exception ex)
    {
        showMessageDialog (window,ErrorMessage,"Couldn't get price",ERROR_MESSAGE)
    }

}
